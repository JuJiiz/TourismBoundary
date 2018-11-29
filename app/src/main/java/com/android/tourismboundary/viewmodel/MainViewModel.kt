package com.android.tourismboundary.viewmodel

import android.app.Activity
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import com.android.tourismboundary.BuildConfig
import com.android.tourismboundary.Constant.LOCATION_PERMISSION_REQUEST_CODE
import com.android.tourismboundary.R
import com.google.android.gms.maps.model.LatLng
import io.reactivex.subjects.BehaviorSubject
import java.io.File
import java.io.FileWriter
import javax.inject.Inject


class MainViewModel @Inject constructor() : ViewModel() {
    val mPermissionState: BehaviorSubject<ResultPermission> by lazy { BehaviorSubject.create<ResultPermission>() }

    enum class PermissionState { PERMISSION_ON, PERMISSION_OFF }
    object ResultPermission {
        var permissionState: PermissionState = MainViewModel.PermissionState.PERMISSION_OFF
        var message = "no permission granted."
    }

    fun requestPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
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

    fun checkSession(activity: Activity) {
        if (ActivityCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            setResultPermission(PermissionState.PERMISSION_OFF, "no permission granted.")
        } else {
            setResultPermission(PermissionState.PERMISSION_ON, "permission granted.")
        }
    }

    fun sendToEmail(context: Context, email: String, dimension: String, locationList: List<LatLng>) {
        val dir = ContextCompat.getExternalFilesDirs(context, null)
        val text = File(dir.first().absolutePath, "LatLng.txt")
        val screen = File(dir.first().absolutePath, "ScreenShot.jpg")
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
        //context.startActivity(Intent.createChooser(sendIntent, "Choose an Email provider"));
    }
}