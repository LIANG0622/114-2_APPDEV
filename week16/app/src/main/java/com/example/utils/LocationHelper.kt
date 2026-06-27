package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.*

object LocationHelper {
    const val TAICHUNG_PORT_LAT = 24.2889
    const val TAICHUNG_PORT_LON = 120.4797

    /**
     * Compute Haversine distance between two coordinates in kilometers.
     */
    fun calculateDistanceKm(
        lat1: Double, lon1: Double,
        lat2: Double = TAICHUNG_PORT_LAT, lon2: Double = TAICHUNG_PORT_LON
    ): Double {
        val earthRadius = 6371.0 // in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * Attempts to fetch the current location with High Accuracy using FusedLocationProviderClient.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        context: Context,
        onSuccess: (lat: Double, lon: Double) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        onSuccess(location.latitude, location.longitude)
                    } else {
                        // Fall back to last known location if current high accuracy is slow/null
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc: Location? ->
                                if (lastLoc != null) {
                                    onSuccess(lastLoc.latitude, lastLoc.longitude)
                                } else {
                                    onFailure("無法取得當前或最後儲存的 GPS 定位。請確認是否已開啟定位服務。")
                                }
                            }
                            .addOnFailureListener { e ->
                                onFailure("定位失敗：${e.localizedMessage ?: "未知錯誤"}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    onFailure("定位失敗：${e.localizedMessage ?: "未知錯誤"}")
                }
        } catch (e: Exception) {
            onFailure("呼叫定位 API 時發生錯誤：${e.localizedMessage ?: "未知錯誤"}")
        }
    }
}
