package com.android.tourismboundary.viewmodel

import android.app.Activity
import android.arch.lifecycle.ViewModel
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import com.android.tourismboundary.Constant
import com.android.tourismboundary.Constant.LOCATION_PERMISSION_REQUEST_CODE
import io.reactivex.subjects.BehaviorSubject
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

    private fun checkInternet(): Boolean {
        return Constant.isInternetConnectionAvailable()
    }
}