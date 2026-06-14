package com.example.data.repository

import android.util.Log
import com.example.core.config.SupabaseConfig
import com.example.core.utils.LocationUtils
import com.example.data.models.ActiveRide
import com.example.data.models.DriverOffer
import com.example.data.models.DriverProfile
import com.example.data.models.RideRequest
import com.example.data.models.User
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.util.UUID

object TarikiRepository {
    private const val TAG = "TarikiRepository"
    private val supabase = SupabaseConfig.client

    // Real-time broadsheets simulated on fallback
    private val _offersStream = MutableSharedFlow<DriverOffer>()
    val offersStream: Flow<DriverOffer> = _offersStream

    private val _ridesStream = MutableSharedFlow<RideRequest>()
    val ridesStream: Flow<RideRequest> = _ridesStream

    // In-memory simulation states for offline prototype fallback
    private var simulatedUser: User? = null
    private val simulatedOffers = mutableListOf<DriverOffer>()
    private val simulatedRideRequests = mutableListOf<RideRequest>()
    private var simulatedDriverProfile: DriverProfile? = null
    private var simulatedActiveRide: ActiveRide? = null

    /**
     * Look up user by phone. Register if new.
     */
    suspend fun loginOrRegister(phone: String, userType: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.postgrest.from("users").select {
                filter { eq("phone", phone) }
            }.decodeSingleOrNull<User>()

            if (response != null) {
                Result.success(response)
            } else {
                Log.d(TAG, "User not found, registering new user phone=$phone")
                val newUser = User(
                    id = UUID.randomUUID().toString(),
                    phone = phone,
                    userType = userType,
                    fullName = "",
                    preferredLanguage = "ar",
                    rating = 5.0,
                    totalRides = 0
                )
                try {
                    supabase.postgrest.from("users").insert(newUser)
                } catch (e: Exception) {
                    Log.e(TAG, "SQL insert failed, using memory persistence", e)
                }
                simulatedUser = newUser
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "loginOrRegister network error, failing back to simulated user", e)
            val fallbackUser = User(
                id = simulatedUser?.id ?: UUID.randomUUID().toString(),
                phone = phone,
                userType = userType,
                fullName = simulatedUser?.fullName ?: "",
                preferredLanguage = "ar",
                rating = 5.0,
                totalRides = 0
            )
            simulatedUser = fallbackUser
            Result.success(fallbackUser)
        }
    }

    /**
     * Update user details (profile setup)
     */
    suspend fun updateProfile(userId: String, fullName: String, photoUrl: String?): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user = simulatedUser ?: supabase.postgrest.from("users").select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<User>() ?: User(id = userId, phone = "+213555123456", userType = "passenger")

            val updated = user.copy(fullName = fullName, profilePhotoUrl = photoUrl)
            try {
                supabase.postgrest.from("users").update(mapOf(
                    "full_name" to fullName,
                    "profile_photo_url" to (photoUrl ?: "")
                )) {
                    filter { eq("id", userId) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "SQL update failed, overriding local memory", e)
            }
            simulatedUser = updated
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Profile update error, updating local memory", e)
            val updated = (simulatedUser ?: User(userId, "+213555123456", fullName, photoUrl, "passenger")).copy(
                fullName = fullName,
                profilePhotoUrl = photoUrl
            )
            simulatedUser = updated
            Result.success(updated)
        }
    }

    /**
     * Update/insert driver profile
     */
    suspend fun saveDriverProfile(profile: DriverProfile): Result<DriverProfile> = withContext(Dispatchers.IO) {
        try {
            try {
                supabase.postgrest.from("driver_profiles").insert(profile)
            } catch (e: Exception) {
                // If exists, try update
                supabase.postgrest.from("driver_profiles").update(mapOf(
                    "car_brand" to profile.carBrand,
                    "car_model" to profile.carModel,
                    "car_year" to profile.carYear,
                    "car_color" to profile.carColor,
                    "license_plate" to profile.licensePlate,
                    "car_photo_url" to (profile.carPhotoUrl ?: ""),
                    "license_photo_url" to (profile.licensePhotoUrl ?: "")
                )) {
                    filter { eq("user_id", profile.userId) }
                }
            }
            simulatedDriverProfile = profile
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "saveDriverProfile error, failing back to local cache", e)
            simulatedDriverProfile = profile
            Result.success(profile)
        }
    }

    /**
     * Get driver profile
     */
    suspend fun getDriverProfile(userId: String): Result<DriverProfile?> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.postgrest.from("driver_profiles").select {
                filter { eq("user_id", userId) }
            }.decodeSingleOrNull<DriverProfile>()
            Result.success(response ?: simulatedDriverProfile)
        } catch (e: Exception) {
            Log.e(TAG, "getDriverProfile error", e)
            Result.success(simulatedDriverProfile)
        }
    }

    /**
     * Set driver online state
     */
    suspend fun setDriverOnline(userId: String, isOnline: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            supabase.postgrest.from("driver_profiles").update(mapOf(
                "is_online" to isOnline
            )) {
                filter { eq("user_id", userId) }
            }
            simulatedDriverProfile = simulatedDriverProfile?.copy(isOnline = isOnline)
            Result.success(isOnline)
        } catch (e: Exception) {
            Log.e(TAG, "setDriverOnline error", e)
            simulatedDriverProfile = simulatedDriverProfile?.copy(isOnline = isOnline)
            Result.success(isOnline)
        }
    }

    /**
     * Create ride request (Passenger)
     */
    suspend fun createRideRequest(request: RideRequest): Result<RideRequest> = withContext(Dispatchers.IO) {
        try {
            val newRequest = request.copy(id = UUID.randomUUID().toString())
            try {
                // Try complete insert first
                supabase.postgrest.from("ride_requests").insert(newRequest)
            } catch (e: Exception) {
                Log.e(TAG, "SQL full insert ride request failed, trying essential fields only", e)
                try {
                    // Fallback to essential fields only if some columns do not exist in database schema
                    val fallbackMap = mapOf(
                        "id" to (newRequest.id ?: UUID.randomUUID().toString()),
                        "passenger_id" to newRequest.passengerId,
                        "pickup_lat" to newRequest.pickupLat,
                        "pickup_lng" to newRequest.pickupLng,
                        "pickup_address" to newRequest.pickupAddress,
                        "destination_lat" to newRequest.destinationLat,
                        "destination_lng" to newRequest.destinationLng,
                        "destination_address" to newRequest.destinationAddress,
                        "passenger_proposed_price" to newRequest.passengerProposedPrice,
                        "status" to newRequest.status
                    )
                    supabase.postgrest.from("ride_requests").insert(fallbackMap)
                } catch (ex: Exception) {
                    Log.e(TAG, "SQL fallback insert ride request failed too", ex)
                }
            }
            simulatedRideRequests.add(newRequest)
            Result.success(newRequest)
        } catch (e: Exception) {
            Log.e(TAG, "createRideRequest error", e)
            val newRequest = request.copy(id = UUID.randomUUID().toString())
            simulatedRideRequests.add(newRequest)
            Result.success(newRequest)
        }
    }

    /**
     * Create driver offer
     */
    suspend fun createDriverOffer(offer: DriverOffer): Result<DriverOffer> = withContext(Dispatchers.IO) {
        try {
            val newOffer = offer.copy(id = UUID.randomUUID().toString())
            try {
                // Try complete insert first
                supabase.postgrest.from("driver_offers").insert(newOffer)
            } catch (e: Exception) {
                Log.e(TAG, "SQL full insert driver offer failed, trying essential fields fallback", e)
                try {
                    // Fallback to essential fields only if driver_lat, driver_lng or eta_minutes don't exist in database
                    val fallbackMap = mapOf(
                        "id" to (newOffer.id ?: UUID.randomUUID().toString()),
                        "ride_request_id" to newOffer.rideRequestId,
                        "driver_id" to newOffer.driverId,
                        "offered_price" to newOffer.offeredPrice,
                        "status" to newOffer.status
                    )
                    supabase.postgrest.from("driver_offers").insert(fallbackMap)
                } catch (ex: Exception) {
                    Log.e(TAG, "SQL fallback insert driver offer failed too", ex)
                }
            }
            simulatedOffers.add(newOffer)
            _offersStream.emit(newOffer)
            Result.success(newOffer)
        } catch (e: Exception) {
            Log.e(TAG, "createDriverOffer error", e)
            val newOffer = offer.copy(id = UUID.randomUUID().toString())
            simulatedOffers.add(newOffer)
            _offersStream.emit(newOffer)
            Result.success(newOffer)
        }
    }

    /**
     * Query all pending ride requests (Driver)
     */
    suspend fun getAvailableRideRequests(): Result<List<RideRequest>> = withContext(Dispatchers.IO) {
        try {
            val response = try {
                supabase.postgrest.from("ride_requests").select {
                    filter { eq("status", "searching") }
                }.decodeList<RideRequest>()
            } catch (e: Exception) {
                Log.e(TAG, "decodeList<RideRequest> failed, trying map fallback", e)
                val rawList = supabase.postgrest.from("ride_requests").select {
                    filter { eq("status", "searching") }
                }.decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()
                
                rawList.map { map ->
                    RideRequest(
                        id = map["id"]?.toString()?.replace("\"", ""),
                        passengerId = map["passenger_id"]?.toString()?.replace("\"", "") ?: "",
                        pickupLat = map["pickup_lat"]?.toString()?.toDoubleOrNull() ?: 36.75,
                        pickupLng = map["pickup_lng"]?.toString()?.toDoubleOrNull() ?: 3.05,
                        pickupAddress = map["pickup_address"]?.toString()?.replace("\"", "") ?: "",
                        destinationLat = map["destination_lat"]?.toString()?.toDoubleOrNull() ?: 36.75,
                        destinationLng = map["destination_lng"]?.toString()?.toDoubleOrNull() ?: 3.05,
                        destinationAddress = map["destination_address"]?.toString()?.replace("\"", "") ?: "",
                        passengerProposedPrice = map["passenger_proposed_price"]?.toString()?.toIntOrNull() ?: 0,
                        status = map["status"]?.toString()?.replace("\"", "") ?: "searching",
                        distanceKm = map["distance_km"]?.toString()?.toDoubleOrNull()
                    )
                }
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "getAvailableRideRequests error", e)
            Result.success(simulatedRideRequests.filter { it.status == "searching" })
        }
    }

    /**
     * Listen to offers for a ride request (Passenger listens to driver offers)
     */
    fun observeDriverOffers(rideId: String): Flow<List<DriverOffer>> = flow {
        while (true) {
            try {
                android.util.Log.d("DEBUG_OFFERS", "Fetching offers for rideId: $rideId")
                val dbOffers = supabase.postgrest.from("driver_offers").select {
                    filter {
                        eq("ride_request_id", rideId)
                        eq("status", "pending")
                    }
                }.decodeList<DriverOffer>()
                android.util.Log.d("DEBUG_OFFERS", "Found ${dbOffers.size} offers: $dbOffers")
                emit(dbOffers)
            } catch (e: Exception) {
                android.util.Log.e("DEBUG_OFFERS", "Error fetching offers: ${e.message}")
                val sim = simulatedOffers.filter { it.rideRequestId == rideId && it.status == "pending" }
                emit(sim)
            }
            delay(2000)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Accept a driver offer (updates ride_request status and other offers to expired)
     */
    suspend fun acceptDriverOffer(rideId: String, offer: DriverOffer): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 1. Try updating the full packet to ride_requests
            try {
                supabase.postgrest.from("ride_requests").update(mapOf(
                    "status" to "accepted",
                    "accepted_driver_id" to offer.driverId,
                    "final_price" to offer.offeredPrice
                )) {
                    filter { eq("id", rideId) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "SQL full update ride_requests failed, trying status & driver only", e)
                try {
                    // Fallback 1: Try status and accepted_driver_id only (final_price might be missing)
                    supabase.postgrest.from("ride_requests").update(mapOf(
                        "status" to "accepted",
                        "accepted_driver_id" to offer.driverId
                    )) {
                        filter { eq("id", rideId) }
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "SQL fallback 1 failed, trying status only", e2)
                    try {
                        // Fallback 2: Try status only (vital column!)
                        supabase.postgrest.from("ride_requests").update(mapOf(
                            "status" to "accepted"
                        )) {
                            filter { eq("id", rideId) }
                        }
                    } catch (e3: Exception) {
                        Log.e(TAG, "All SQL updates on ride_requests failed", e3)
                    }
                }
            }

            // 2. Try marking the specific offer as accepted
            try {
                supabase.postgrest.from("driver_offers").update(mapOf(
                    "status" to "accepted"
                )) {
                    filter { eq("id", offer.id ?: "") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "SQL update driver_offers accepted failed, trying fallback", e)
            }

            // 3. Try marking other offers for this ride as expired
            try {
                supabase.postgrest.from("driver_offers").update(mapOf(
                    "status" to "expired"
                )) {
                    filter {
                        eq("ride_request_id", rideId)
                        neq("id", offer.id ?: "")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "SQL expire other driver_offers failed", e)
            }

            // Keep local simulated memory in sync
            updateLocalRides(rideId, "accepted", offer.driverId, offer.offeredPrice)
            simulatedOffers.forEachIndexed { index, o ->
                if (o.id == offer.id) {
                    simulatedOffers[index] = o.copy(status = "accepted")
                } else if (o.rideRequestId == rideId) {
                    simulatedOffers[index] = o.copy(status = "expired")
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "acceptDriverOffer full catch error: ${e.message}", e)
            // Local fallback
            updateLocalRides(rideId, "accepted", offer.driverId, offer.offeredPrice)
            simulatedOffers.forEachIndexed { index, o ->
                if (o.id == offer.id) {
                    simulatedOffers[index] = o.copy(status = "accepted")
                } else if (o.rideRequestId == rideId) {
                    simulatedOffers[index] = o.copy(status = "expired")
                }
            }
            Result.success(true)
        }
    }

    private fun updateLocalRides(rideId: String, status: String, driverId: String?, price: Int?) {
        simulatedRideRequests.forEachIndexed { index, request ->
            if (request.id == rideId) {
                simulatedRideRequests[index] = request.copy(
                    status = status,
                    acceptedDriverId = driverId ?: request.acceptedDriverId,
                    finalPrice = price ?: request.finalPrice
                )
            }
        }
    }

    /**
     * Complete/rate ride
     */
    suspend fun updateRideStatus(rideId: String, status: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            supabase.postgrest.from("ride_requests").update(mapOf(
                "status" to status
            )) {
                filter { eq("id", rideId) }
            }
            updateLocalRides(rideId, status, null, null)
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "updateRideStatus error", e)
            updateLocalRides(rideId, status, null, null)
            Result.success(true)
        }
    }

    /**
     * Continuously poll a ride request (for status updates)
     */
    fun observeRideRequest(rideId: String): Flow<RideRequest?> = flow {
        while (true) {
            val local = simulatedRideRequests.firstOrNull { it.id == rideId }
            try {
                val dbRequest = supabase.postgrest.from("ride_requests").select {
                    filter { eq("id", rideId) }
                }.decodeSingleOrNull<RideRequest>()
                if (dbRequest != null) {
                    updateLocalRideRequestInSimulated(dbRequest)
                }
                emit(dbRequest ?: local)
            } catch (e: Exception) {
                emit(local)
            }
            delay(2000)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getOnlineDrivers(): Result<List<DriverProfile>> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.postgrest.from("driver_profiles").select {
                filter { eq("is_online", true) }
            }.decodeList<DriverProfile>()
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "getOnlineDrivers error", e)
            val list = mutableListOf<DriverProfile>()
            simulatedDriverProfile?.let {
                if (it.isOnline) {
                    list.add(it)
                }
            }
            Result.success(list)
        }
    }

    suspend fun getUserById(userId: String): Result<User?> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.postgrest.from("users").select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<User>()
            Result.success(response ?: (if (simulatedUser?.id == userId) simulatedUser else null))
        } catch (e: Exception) {
            Log.e(TAG, "getUserById error", e)
            Result.success(if (simulatedUser?.id == userId) simulatedUser else null)
        }
    }

    suspend fun updateDriverLocationInDb(userId: String, lat: Double, lng: Double): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            supabase.postgrest.from("driver_profiles").update(mapOf(
                "current_lat" to lat,
                "current_lng" to lng
            )) {
                filter { eq("user_id", userId) }
            }
            simulatedDriverProfile = simulatedDriverProfile?.copy(currentLat = lat, currentLng = lng)
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "updateDriverLocationInDb error", e)
            simulatedDriverProfile = simulatedDriverProfile?.copy(currentLat = lat, currentLng = lng)
            Result.success(true)
        }
    }

    suspend fun getOffersForRide(rideId: String): List<DriverOffer> = withContext(Dispatchers.IO) {
        return@withContext try {
            val dbOffers = supabase.postgrest.from("driver_offers").select {
                filter {
                    eq("ride_request_id", rideId)
                    eq("status", "pending")
                }
            }.decodeList<DriverOffer>()
            val merged = (dbOffers + simulatedOffers.filter { it.rideRequestId == rideId && it.status == "pending" }).distinctBy { it.id }
            merged
        } catch (e: Exception) {
            simulatedOffers.filter { it.rideRequestId == rideId && it.status == "pending" }
        }
    }

    private fun updateLocalRideRequestInSimulated(request: RideRequest) {
        val index = simulatedRideRequests.indexOfFirst { it.id == request.id }
        if (index != -1) {
            simulatedRideRequests[index] = request
        } else {
            simulatedRideRequests.add(request)
        }
    }

    private fun updateLocalOfferInSimulated(offer: DriverOffer) {
        val index = simulatedOffers.indexOfFirst { o -> o.id == offer.id }
        if (index != -1) {
            simulatedOffers[index] = offer
        } else {
            simulatedOffers.add(offer)
        }
    }

    suspend fun getRideRequestById(rideId: String): RideRequest? = withContext(Dispatchers.IO) {
        val local = simulatedRideRequests.firstOrNull { it.id == rideId }
        return@withContext try {
            val dbRequest = supabase.postgrest.from("ride_requests").select {
                filter { eq("id", rideId) }
            }.decodeSingleOrNull<RideRequest>()
            if (dbRequest != null) {
                updateLocalRideRequestInSimulated(dbRequest)
            }
            dbRequest ?: local
        } catch (e: Exception) {
            local
        }
    }

    suspend fun getActiveRideRequestForPassenger(passengerId: String): RideRequest? = withContext(Dispatchers.IO) {
        val local = simulatedRideRequests.firstOrNull { 
            it.passengerId == passengerId && (it.status == "searching" || it.status == "accepted" || it.status == "driver_arrived" || it.status == "in_progress") 
        }
        return@withContext try {
            val response = supabase.postgrest.from("ride_requests").select {
                filter {
                    eq("passenger_id", passengerId)
                }
            }.decodeList<RideRequest>()
            val active = response.firstOrNull { it.status == "searching" || it.status == "accepted" || it.status == "driver_arrived" || it.status == "in_progress" }
            if (active != null) {
                updateLocalRideRequestInSimulated(active)
            }
            active ?: local
        } catch (e: Exception) {
            Log.e(TAG, "getActiveRideRequestForPassenger error: ${e.message}", e)
            local
        }
    }

    suspend fun getAcceptedOfferForRide(rideId: String): DriverOffer? = withContext(Dispatchers.IO) {
        val local = simulatedOffers.firstOrNull { it.rideRequestId == rideId && it.status == "accepted" }
        return@withContext try {
            val response = supabase.postgrest.from("driver_offers").select {
                filter {
                    eq("ride_request_id", rideId)
                    eq("status", "accepted")
                }
            }.decodeList<DriverOffer>()
            val active = response.firstOrNull()
            if (active != null) {
                updateLocalOfferInSimulated(active)
            }
            active ?: local
        } catch (e: Exception) {
            Log.e(TAG, "getAcceptedOfferForRide error: ${e.message}", e)
            local
        }
    }

    suspend fun getActiveRideRequestForDriver(driverId: String): RideRequest? = withContext(Dispatchers.IO) {
        val local = simulatedRideRequests.firstOrNull {
            it.acceptedDriverId == driverId && (it.status == "accepted" || it.status == "driver_arrived" || it.status == "in_progress")
        }
        return@withContext try {
            val response = supabase.postgrest.from("ride_requests").select {
                filter {
                    eq("accepted_driver_id", driverId)
                }
            }.decodeList<RideRequest>()
            val active = response.firstOrNull { it.status == "accepted" || it.status == "driver_arrived" || it.status == "in_progress" }
            if (active != null) {
                updateLocalRideRequestInSimulated(active)
            }
            active ?: local
        } catch (e: Exception) {
            Log.e(TAG, "getActiveRideRequestForDriver error: ${e.message}", e)
            local
        }
    }

    suspend fun getPendingOfferForDriver(driverId: String): DriverOffer? = withContext(Dispatchers.IO) {
        val local = simulatedOffers.firstOrNull { it.driverId == driverId && it.status == "pending" }
        return@withContext try {
            val response = supabase.postgrest.from("driver_offers").select {
                filter {
                    eq("driver_id", driverId)
                    eq("status", "pending")
                }
            }.decodeList<DriverOffer>()
            val active = response.firstOrNull()
            if (active != null) {
                updateLocalOfferInSimulated(active)
                val ride = getRideRequestById(active.rideRequestId)
                if (ride != null) {
                    updateLocalRideRequestInSimulated(ride)
                }
            }
            active ?: local
        } catch (e: Exception) {
            Log.e(TAG, "getPendingOfferForDriver error: ${e.message}", e)
            local
        }
    }
}
