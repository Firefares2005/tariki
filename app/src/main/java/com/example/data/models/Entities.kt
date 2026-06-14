package com.example.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id") val id: String,
    @SerialName("phone") val phone: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    @SerialName("user_type") val userType: String, // "driver" | "passenger"
    @SerialName("rating") val rating: Double? = 5.0,
    @SerialName("total_rides") val totalRides: Int? = 0,
    @SerialName("is_active") val isActive: Boolean? = true,
    @SerialName("is_verified") val isVerified: Boolean? = false,
    @SerialName("preferred_language") val preferredLanguage: String? = "ar",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("device_id") val deviceId: String? = null
)

@Serializable
data class DriverProfile(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("car_brand") val carBrand: String,
    @SerialName("car_model") val carModel: String,
    @SerialName("car_year") val carYear: Int,
    @SerialName("car_color") val carColor: String,
    @SerialName("license_plate") val licensePlate: String,
    @SerialName("car_photo_url") val carPhotoUrl: String? = null,
    @SerialName("license_photo_url") val licensePhotoUrl: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("current_lat") val currentLat: Double? = null,
    @SerialName("current_lng") val currentLng: Double? = null,
    @SerialName("last_location_update") val lastLocationUpdate: String? = null
)

@Serializable
data class RideRequest(
    @SerialName("id") val id: String? = null,
    @SerialName("passenger_id") val passengerId: String,
    @SerialName("pickup_lat") val pickupLat: Double,
    @SerialName("pickup_lng") val pickupLng: Double,
    @SerialName("pickup_address") val pickupAddress: String,
    @SerialName("destination_lat") val destinationLat: Double,
    @SerialName("destination_lng") val destinationLng: Double,
    @SerialName("destination_address") val destinationAddress: String,
    @SerialName("passenger_proposed_price") val passengerProposedPrice: Int,
    @SerialName("final_price") val finalPrice: Int? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("estimated_minutes") val estimatedMinutes: Int? = null,
    @SerialName("status") val status: String = "searching", // "searching" | "negotiating" | "accepted" | "driver_arrived" | "in_progress" | "completed" | "cancelled"
    @SerialName("accepted_driver_id") val acceptedDriverId: String? = null,
    @SerialName("cancelled_by") val cancelledBy: String? = null,
    @SerialName("cancel_reason") val cancelReason: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class DriverOffer(
    @SerialName("id") val id: String? = null,
    @SerialName("ride_request_id") val rideRequestId: String,
    @SerialName("driver_id") val driverId: String,
    @SerialName("offered_price") val offeredPrice: Int,
    @SerialName("status") val status: String = "pending", // "pending" | "accepted" | "rejected" | "expired" | "withdrawn"
    @SerialName("driver_lat") val driverLat: Double? = null,
    @SerialName("driver_lng") val driverLng: Double? = null,
    @SerialName("eta_minutes") val etaMinutes: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ActiveRide(
    @SerialName("id") val id: String? = null,
    @SerialName("ride_request_id") val rideRequestId: String,
    @SerialName("driver_id") val driverId: String,
    @SerialName("passenger_id") val passengerId: String,
    @SerialName("current_driver_lat") val currentDriverLat: Double? = null,
    @SerialName("current_driver_lng") val currentDriverLng: Double? = null,
    @SerialName("last_update") val lastUpdate: String? = null,
    @SerialName("agreed_price") val agreedPrice: Int,
    @SerialName("created_at") val createdAt: String? = null
)
