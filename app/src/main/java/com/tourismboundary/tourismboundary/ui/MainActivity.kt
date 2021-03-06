package com.tourismboundary.tourismboundary.ui

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil
import com.tourismboundary.tourismboundary.Constant.LOCATION_PERMISSION_REQUEST_CODE
import com.tourismboundary.tourismboundary.R
import com.tourismboundary.tourismboundary.ui.fragment.PermissionDialogFragment
import com.tourismboundary.tourismboundary.ui.fragment.SendEmailFragment
import com.tourismboundary.tourismboundary.viewmodel.MainViewModel
import com.tourismboundary.tourismboundary.viewmodel.ViewModelFactory
import dagger.android.AndroidInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import javax.inject.Inject

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    @Inject
    lateinit var mViewModelFactory: ViewModelFactory
    private lateinit var mViewModel: MainViewModel
    private var mPermissionStateDisposable: Disposable? = null
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var dialogFragment = PermissionDialogFragment()
    private var locationList: ArrayList<LatLng> = arrayListOf()
    private var markerList: ArrayList<Marker> = arrayListOf()
    private var polygonMap: Polygon? = null
    private var manualPin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(MainViewModel::class.java)
        setContentView(R.layout.activity_main)
        init()

    }

    private fun init() {


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnPin.setOnClickListener {
            if (manualPin) {
                val latLng = mMap.cameraPosition.target
                addMarker(latLng)
            }
        }

        fabMenu.setOnFloatingActionsMenuUpdateListener(object :
            FloatingActionsMenu.OnFloatingActionsMenuUpdateListener {
            override fun onMenuCollapsed() {
                imvPin.visibility = View.GONE
                manualPin = false
            }

            override fun onMenuExpanded() {
                imvPin.visibility = View.VISIBLE
                manualPin = true
            }

        })

        btnClearAll.setOnClickListener {
            showClearConfirmDialog()
        }

        btnUndo.setOnClickListener {
            removeMarker()
        }

        btnDone.setOnClickListener {
            if (locationList.size > 2) {
                mMap.snapshot { screen ->
                    mViewModel.saveImage(this, screen)
                }

                val dimension = SphericalUtil.computeArea(locationList).toInt()
                if (supportFragmentManager.findFragmentByTag(SendEmailFragment.Tag) == null) {
                    val sendEmailFragment = SendEmailFragment()
                    val bundle = Bundle()
                    bundle.putParcelableArrayList("LOCATION_LIST", locationList)
                    bundle.putInt("DIMENSION", dimension)
                    sendEmailFragment.arguments = bundle

                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down)
                        .replace(android.R.id.content, sendEmailFragment, SendEmailFragment.Tag)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun showClearConfirmDialog() {
        // setup the alert builder
        val builder = AlertDialog.Builder(this)
        builder.setTitle(this.resources.getString(R.string.message))
        builder.setMessage("${this.resources.getString(R.string.want_to_clear_pin)}?")

        builder.setPositiveButton(
            this.resources.getString(R.string.submit)
        ) { dialogInterface, i ->
            removeAllMarker()
        }
        builder.setNegativeButton(this.resources.getString(R.string.cancel)) { dialogInterface, i ->
            dialogInterface.dismiss()
        }

        // create and show the alert dialog
        val dialog = builder.create()
        dialog.show()
    }

    private fun addMarker(latLng: LatLng) {
        val marker = mMap.addMarker(MarkerOptions().position(latLng))
        locationList.add(latLng)
        markerList.add(marker)
        drawArea()
    }

    private fun drawArea() {
        polygonMap?.remove()
        if (locationList.size > 2) {
            val polygon = PolygonOptions()
            polygon.fillColor(Color.RED)
            polygon.strokeWidth(4F)
            polygon.addAll(locationList)
            polygonMap = mMap.addPolygon(polygon)
        }
    }

    private fun removeMarker() {
        if (locationList.isNotEmpty() && markerList.isNotEmpty()) {
            locationList.remove(locationList.last())
            markerList.last().remove()
            markerList.remove(markerList.last())
            drawArea()
        }
    }

    private fun removeAllMarker() {
        locationList.clear()
        markerList.forEach { marker -> marker.remove() }
        markerList.clear()
        drawArea()
    }

    override fun onResume() {
        super.onResume()


        disposeSessionState()
        mPermissionStateDisposable = mViewModel.mPermissionState.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                handleSessionState(it)
            }, {
                Timber.e("error on getting login result: ${it.message}")
            })
        mViewModel.checkGPS(this)
    }

    override fun onPause() {
        disposeSessionState()
        removeAllMarker()
        super.onPause()
    }

    private fun handleSessionState(it: MainViewModel.ResultPermission) {
        Timber.d("session_state: ${it.permissionState} message: ${it.message}")

        when (it.permissionState) {
            MainViewModel.PermissionState.PERMISSION_OFF -> {
                if (supportFragmentManager.findFragmentByTag(PermissionDialogFragment.TAG) == null) {
                    val bundle = Bundle()
                    bundle.putInt("REQUEST_CODE", 0)
                    dialogFragment.arguments = bundle
                    dialogFragment.show(supportFragmentManager, PermissionDialogFragment.TAG)
                }
            }
            MainViewModel.PermissionState.GPS_OFF -> {
                val bundle = Bundle()
                bundle.putInt("REQUEST_CODE", 1)
                dialogFragment.arguments = bundle
                if (supportFragmentManager.findFragmentByTag(PermissionDialogFragment.TAG) == null) {
                    dialogFragment.show(supportFragmentManager, PermissionDialogFragment.TAG)
                }
            }
            MainViewModel.PermissionState.READY -> {
                if (supportFragmentManager.findFragmentByTag(PermissionDialogFragment.TAG) != null) {
                    dialogFragment.dismiss()
                }
                getCurrentLocation()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            permissions.forEachIndexed { index, permission ->
                if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                    val showRationale = shouldShowRequestPermissionRationale(permission)
                    if (!showRationale) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivityForResult(intent, LOCATION_PERMISSION_REQUEST_CODE)
                    }
                } else {
                    mViewModel.setResultPermission(MainViewModel.PermissionState.READY, "permission granted.")
                }
            }
        }
    }

    private fun disposeSessionState() {
        mPermissionStateDisposable?.apply { if (!isDisposed) dispose() }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        mMap.clear()
        if (MainViewModel.ResultPermission.permissionState == MainViewModel.PermissionState.READY) {
            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                if (location != null) {
                    val centerLocation = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(
                        MarkerOptions().position(centerLocation).title("Here").icon(
                            bitmapDescriptorFromVector(this, R.drawable.ic_person_pin)
                        )
                    )
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(centerLocation, 16F))
                }
            }
        }

    }

    private fun bitmapDescriptorFromVector(context: Context, @DrawableRes vectorDrawableResourceId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId)
        vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap =
            Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
