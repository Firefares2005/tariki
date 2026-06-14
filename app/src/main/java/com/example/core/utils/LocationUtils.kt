package com.example.core.utils

import kotlin.math.*

object LocationUtils {
    /**
     * Calculates the distance in kilometers between two points using the Haversine formula.
     */
    fun calculateDistanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
                
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Simple pricing algorithm
     * 30 DZD per km, 100 DZD base minimum
     */
    fun getSuggestedPrice(distanceKm: Double): Int {
        val baseRate = 30
        val baseFare = 100
        val suggested = baseFare + (distanceKm * baseRate).roundToInt()
        // Round to nearest 5 or 10 DZD for convenience
        return (suggested / 10) * 10
    }
}
