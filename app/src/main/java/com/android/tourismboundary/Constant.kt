package com.android.tourismboundary

import java.io.IOException

object Constant{
    const val ENDPOINT = "https://api.tourismdb.info/"
    const val TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzdXBlcl90b2tlbkB0b3VyaXNtLmNtdSJ9.oFjuV7HrcI1bQO62Vig_1dXY3MkdBA-ijXi_ew3ZPWY"
    const val LOADING_TIMEOUT_SECONDS = 10
    const val LOCATION_PERMISSION_REQUEST_CODE = 0x123

    fun isInternetConnectionAvailable(): Boolean {
        val command = "ping -c 1 8.8.8.8"
        return try {
            Runtime.getRuntime().exec(command).waitFor() == 0
        } catch (e: InterruptedException) {
            false
        } catch (e: IOException) {
            false
        }
    }

}