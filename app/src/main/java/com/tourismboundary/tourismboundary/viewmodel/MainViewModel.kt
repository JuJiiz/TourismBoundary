package com.tourismboundary.tourismboundary.viewmodel

import android.app.Activity
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.startActivity
import android.support.v4.content.FileProvider
import com.google.android.gms.maps.model.LatLng
import com.tourismboundary.tourismboundary.BuildConfig
import com.tourismboundary.tourismboundary.Constant.LOCATION_PERMISSION_REQUEST_CODE
import com.tourismboundary.tourismboundary.R
import io.reactivex.subjects.BehaviorSubject
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import javax.inject.Inject


class MainViewModel @Inject constructor() : ViewModel() {
    val mPermissionState: BehaviorSubject<ResultPermission> by lazy { BehaviorSubject.create<ResultPermission>() }

    enum class PermissionState { READY, PERMISSION_OFF, GPS_OFF }
    object ResultPermission {
        var permissionState: PermissionState = MainViewModel.PermissionState.PERMISSION_OFF
        var message = "no permission granted."
    }

    fun requestPermission(activity: Activity, code: Int) {
        if (code == 0) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else if (code == 1) {
            val gpsOptionsIntent = Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
            )
            startActivity(activity, gpsOptionsIntent, null)
        }
    }

    fun setResultPermission(
        permissionState: PermissionState = PermissionState.PERMISSION_OFF,
        message: String = "no permission granted."
    ) {
        val result = ResultPermission
        result.message = message
        result.permissionState = permissionState
        mPermissionState.apply { onNext(result) }
    }

    fun checkGPS(activity: Activity) {
        val locationManager by lazy { activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
        val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (ActivityCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            setResultPermission(PermissionState.PERMISSION_OFF, "no permission granted.")
        } else if (!gps) {
            setResultPermission(PermissionState.GPS_OFF, "gps is disable.")
        } else {
            setResultPermission(PermissionState.READY, "ready.")
        }
    }

    fun saveImage(context: Context, bitmap: Bitmap) {
        val dir = ContextCompat.getExternalFilesDirs(context, null)
        val file = File(dir.first().absolutePath, "map.jpeg")
        if (file.exists()) file.delete()
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()
    }

    fun sendToEmail(context: Context, email: String, dimension: String, locationList: List<LatLng>) {
        val dir = ContextCompat.getExternalFilesDirs(context, null)
        val text = File(dir.first().absolutePath, "location.txt")
        val screen = File(dir.first().absolutePath, "map.jpeg")
        val writer = FileWriter(text)

        locationList.forEachIndexed { index, latLng ->
            writer.append("${latLng.latitude}:${latLng.longitude}")
            if (index != locationList.size - 1) writer.append(",")
        }
        writer.flush()
        writer.close()

        val textUri = FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".provider",
            text
        )

        val screenUri = FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".provider",
            screen
        )
        val totalDimension =
            "${context.resources.getString(R.string.total_dimension)} $dimension ${context.resources.getString(R.string.square_meters)}"

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND_MULTIPLE
        sendIntent.putExtra(Intent.EXTRA_STREAM, arrayListOf(textUri, screenUri))
        sendIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        sendIntent.putExtra(Intent.EXTRA_TEXT, totalDimension)
        sendIntent.type = "text/plain"
        context.startActivity(sendIntent)
    }
}