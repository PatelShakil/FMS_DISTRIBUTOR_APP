package com.shakilpatel.boloindia.distributor.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getCurrentLocation(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission(context)) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    continuation.resume(location)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (e: SecurityException) {
            continuation.resume(null)
        }
    }
}
