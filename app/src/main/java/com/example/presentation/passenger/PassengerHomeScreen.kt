package com.example.presentation.passenger

import android.Manifest
import android.widget.Space
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.config.SessionManager
import com.example.core.utils.LocationUtils
import com.example.core.utils.RouteUtils
import com.example.data.models.DriverOffer
import com.example.data.models.RideRequest
import com.example.data.repository.TarikiRepository
import com.example.presentation.components.TarikiMapView
import com.example.ui.theme.TarikiColors
import com.google.android.gms.location.LocationServices
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class PassengerFlowState {
    object Idle : PassengerFlowState()
    object EnterLocations : PassengerFlowState()
    object ProposePrice : PassengerFlowState()
    data class WaitingForOffers(val rideRequest: RideRequest) : PassengerFlowState()
    data class RideAccepted(val rideRequest: RideRequest, val acceptedOffer: DriverOffer) : PassengerFlowState()
    data class ActiveRide(val rideRequest: RideRequest) : PassengerFlowState()
    data class Completed(val rideRequest: RideRequest) : PassengerFlowState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerHomeScreen(
    phone: String,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var flowState by remember { mutableStateOf<PassengerFlowState>(PassengerFlowState.Idle) }

    // Session Details
    var passengerName by remember { mutableStateOf("") }
    var passengerRating by remember { mutableStateOf("5.0") }
    var passengerAvatar by remember { mutableStateOf("") }

    // Form inputs
    var pickupAddress by remember { mutableStateOf("Place Audin, Alger Center") }
    var destinationAddress by remember { mutableStateOf("") }
    var proposedPrice by remember { mutableStateOf(100) }
    var distanceKm by remember { mutableStateOf(4.5) }

    var pickupLat by remember { mutableStateOf(36.7538) }
    var pickupLng by remember { mutableStateOf(3.0588) }
    var destinationLat by remember { mutableStateOf(36.76) }
    var destinationLng by remember { mutableStateOf(3.07) }

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasLocationPermission by remember { mutableStateOf(false) }

    val fetchExactLocation = remember(fusedLocationClient) {
        {
            try {
                val fineCheck = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                val coarseCheck = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                if (fineCheck == android.content.pm.PackageManager.PERMISSION_GRANTED || coarseCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { loc ->
                        if (loc != null) {
                            pickupLat = loc.latitude
                            pickupLng = loc.longitude
                            pickupAddress = "موقعي الحالي (GPS)"
                        } else {
                            try {
                                val fineCheckInner = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                                val coarseCheckInner = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (fineCheckInner == android.content.pm.PackageManager.PERMISSION_GRANTED || coarseCheckInner == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                        if (lastLoc != null) {
                                            pickupLat = lastLoc.latitude
                                            pickupLng = lastLoc.longitude
                                            pickupAddress = "موقعي الحالي (GPS)"
                                        }
                                    }
                                }
                            } catch (e: SecurityException) {}
                        }
                    }
                }
            } catch (e: SecurityException) {}
        }
    }

    val gpsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            fetchExactLocation()
        }
    }

    val checkAndRequestGps = remember(context) {
        {
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            val isGpsEnabled = try {
                locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
            } catch (e: Exception) {
                false
            }
            if (!isGpsEnabled) {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        5000
                    ).build()

                    val builder = com.google.android.gms.location.LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest)
                        .setAlwaysShow(true)

                    val client = com.google.android.gms.location.LocationServices.getSettingsClient(context)
                    val task = client.checkLocationSettings(builder.build())

                    task.addOnFailureListener { exception ->
                        if (exception is com.google.android.gms.common.api.ResolvableApiException) {
                            try {
                                val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                                gpsLauncher.launch(intentSenderRequest)
                            } catch (sendEx: Exception) {}
                        }
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fineGranted || coarseGranted
        if (hasLocationPermission) {
            checkAndRequestGps()
            fetchExactLocation()
        }
    }

    // Automatically trigger GPS request dialog like Yassir app
    LaunchedEffect(Unit) {
        val fineLocation = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        hasLocationPermission = fineLocation == android.content.pm.PackageManager.PERMISSION_GRANTED || coarseLocation == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasLocationPermission) {
            checkAndRequestGps()
            fetchExactLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Live state updates
    var driverOffers by remember { mutableStateOf<List<DriverOffer>>(emptyList()) }
    var activeOffer by remember { mutableStateOf<DriverOffer?>(null) }
    var ratingGiven by remember { mutableStateOf(5) }

    // Load user session
    LaunchedEffect(Unit) {
        val details = SessionManager.getUserDetails()
        passengerName = details["name"] ?: "رافي"
        passengerAvatar = details["avatar"] ?: ""
        
        val pId = details["id"] ?: ""
        if (pId.isNotEmpty()) {
            android.util.Log.d("DEBUG_PASSENGER", "Checking active ride for passenger: $pId")
            val activeReq = TarikiRepository.getActiveRideRequestForPassenger(pId)
            if (activeReq != null) {
                android.util.Log.d("DEBUG_PASSENGER", "Found active state: ${activeReq.status}")
                proposedPrice = activeReq.passengerProposedPrice
                distanceKm = activeReq.distanceKm ?: distanceKm
                pickupLat = activeReq.pickupLat
                pickupLng = activeReq.pickupLng
                pickupAddress = activeReq.pickupAddress
                destinationLat = activeReq.destinationLat
                destinationLng = activeReq.destinationLng
                destinationAddress = activeReq.destinationAddress

                when (activeReq.status) {
                    "searching" -> {
                        flowState = PassengerFlowState.WaitingForOffers(activeReq)
                    }
                    "accepted" -> {
                        val acceptedOffer = TarikiRepository.getAcceptedOfferForRide(activeReq.id ?: "")
                        if (acceptedOffer != null) {
                            activeOffer = acceptedOffer
                            flowState = PassengerFlowState.RideAccepted(activeReq, acceptedOffer)
                        } else {
                            val fallbackOffer = DriverOffer(
                                rideRequestId = activeReq.id ?: "",
                                driverId = activeReq.acceptedDriverId ?: "",
                                offeredPrice = activeReq.finalPrice ?: activeReq.passengerProposedPrice,
                                status = "accepted"
                            )
                            activeOffer = fallbackOffer
                            flowState = PassengerFlowState.RideAccepted(activeReq, fallbackOffer)
                        }
                    }
                    "in_progress" -> {
                        flowState = PassengerFlowState.ActiveRide(activeReq)
                    }
                }
            }
        }
    }

    // Live loop for checking driver offers when searching
    LaunchedEffect(flowState) {
        if (flowState is PassengerFlowState.WaitingForOffers) {
            val req = (flowState as PassengerFlowState.WaitingForOffers).rideRequest
            android.util.Log.d("DEBUG_PASSENGER", "Starting to observe offers for rideId: ${req.id}")
            TarikiRepository.observeDriverOffers(req.id ?: "").collectLatest { offers ->
                android.util.Log.d("DEBUG_PASSENGER", "Received ${offers.size} offers")
                driverOffers = offers
            }
        } else if (flowState is PassengerFlowState.RideAccepted) {
            val req = (flowState as PassengerFlowState.RideAccepted).rideRequest
            val acceptedOffer = (flowState as PassengerFlowState.RideAccepted).acceptedOffer
            TarikiRepository.observeRideRequest(req.id ?: "").collectLatest { updated ->
                if (updated != null) {
                    if (updated.status == "in_progress") {
                        flowState = PassengerFlowState.ActiveRide(updated)
                    } else if (updated.status == "cancelled") {
                        flowState = PassengerFlowState.Idle
                    } else {
                        flowState = PassengerFlowState.RideAccepted(updated, acceptedOffer)
                    }
                }
            }
        } else if (flowState is PassengerFlowState.ActiveRide) {
            val req = (flowState as PassengerFlowState.ActiveRide).rideRequest
            TarikiRepository.observeRideRequest(req.id ?: "").collectLatest { updated ->
                if (updated != null && updated.status == "completed") {
                    flowState = PassengerFlowState.Completed(updated)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onNavigateToProfile() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(TarikiColors.DarkGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = passengerName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = passengerName.ifEmpty { "Passenger" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TarikiColors.TextPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = TarikiColors.Warning, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(text = passengerRating, fontSize = 12.sp, color = TarikiColors.TextMuted)
                                }
                            }
                        }

                        // Logout Icon
                        IconButton(onClick = {
                            coroutineScope.launch {
                                SessionManager.clear()
                                onLogout()
                            }
                        }) {
                            Icon(Icons.Default.Logout, contentDescription = "Log Out", tint = TarikiColors.Error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TarikiColors.Cream)
            )
        },
        containerColor = TarikiColors.Cream
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Map Element
                val isWaiting = flowState is PassengerFlowState.WaitingForOffers
                val isAccepted = flowState is PassengerFlowState.RideAccepted || flowState is PassengerFlowState.ActiveRide
                
                TarikiMapView(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    pickupLat = pickupLat,
                    pickupLng = pickupLng,
                    destLat = if (destinationAddress.isNotBlank()) destinationLat else null,
                    destLng = if (destinationAddress.isNotBlank()) destinationLng else null,
                    pickupAddress = pickupAddress,
                    destinationAddress = if (destinationAddress.isNotBlank()) destinationAddress else null,
                    showDrivers = isWaiting || isAccepted,
                    hasLocationPermission = hasLocationPermission
                )

                // Bottom Panel dynamically reflecting the user state context
                AnimatedContent(
                    targetState = flowState,
                    transitionSpec = {
                        slideInVertically { h -> h } + fadeIn() togetherWith slideOutVertically { h -> -h } + fadeOut()
                    },
                    label = "flowBottomContent"
                ) { state ->
                    when (state) {
                        is PassengerFlowState.Idle -> {
                            BottomIdleCard(onSearchClick = { flowState = PassengerFlowState.EnterLocations })
                        }
                        is PassengerFlowState.EnterLocations -> {
                            BottomLocationsCard(
                                pickup = pickupAddress,
                                destination = destinationAddress,
                                onPickupChange = { pickupAddress = it },
                                onDestinationChange = { destinationAddress = it },
                                onSuggestionSelect = { name, lat, lng ->
                                    destinationAddress = name
                                    destinationLat = lat
                                    destinationLng = lng
                                    
                                    // Calculate precise distance from the user's current GPS position to the chosen spot
                                    val exactDist = LocationUtils.calculateDistanceKm(pickupLat, pickupLng, lat, lng)
                                    // Smooth formatting to 1 decimal place
                                    distanceKm = Math.round(exactDist * 10.0) / 10.0
                                    proposedPrice = LocationUtils.getSuggestedPrice(distanceKm)
                                },
                                onNext = {
                                    if (destinationAddress.isNotBlank()) {
                                        // If they typed manually instead of selection, we offer a realistic distance mapping
                                        if (distanceKm <= 0.1 || destinationLat == 36.76) {
                                            val randomDist = 3.0 + (Math.random() * 8.0)
                                            distanceKm = Math.round(randomDist * 10.0) / 10.0
                                            proposedPrice = LocationUtils.getSuggestedPrice(distanceKm)
                                        }
                                        flowState = PassengerFlowState.ProposePrice
                                    }
                                },
                                onBack = { flowState = PassengerFlowState.Idle }
                            )
                        }
                        is PassengerFlowState.ProposePrice -> {
                            BottomPriceProposalCard(
                                suggestedPrice = LocationUtils.getSuggestedPrice(distanceKm),
                                selectionPrice = proposedPrice,
                                distance = distanceKm,
                                onPriceChange = { proposedPrice = it },
                                onSubmit = {
                                    coroutineScope.launch {
                                        val session = SessionManager.getUser()
                                        val req = RideRequest(
                                            passengerId = session?.first ?: "sim_pass",
                                            pickupLat = pickupLat,
                                            pickupLng = pickupLng,
                                            pickupAddress = pickupAddress,
                                            destinationLat = destinationLat,
                                            destinationLng = destinationLng,
                                            destinationAddress = destinationAddress,
                                            passengerProposedPrice = proposedPrice,
                                            distanceKm = distanceKm,
                                            status = "searching"
                                        )
                                        TarikiRepository.createRideRequest(req).onSuccess { saved ->
                                            driverOffers = emptyList()
                                            flowState = PassengerFlowState.WaitingForOffers(saved)
                                        }
                                    }
                                },
                                onBack = { flowState = PassengerFlowState.EnterLocations }
                            )
                        }
                        is PassengerFlowState.WaitingForOffers -> {
                            BottomWaitingCard(
                                priceProposed = proposedPrice,
                                offers = driverOffers,
                                onAccept = { offer ->
                                    coroutineScope.launch {
                                        android.util.Log.d("DEBUG_ACCEPT", "Accepting offer: ${offer.id}, rideId: ${state.rideRequest.id}, driverId: ${offer.driverId}")
                                        val result = TarikiRepository.acceptDriverOffer(state.rideRequest.id ?: "", offer)
                                        android.util.Log.d("DEBUG_ACCEPT", "acceptDriverOffer result: $result")
                                        result.onSuccess {
                                            android.util.Log.d("DEBUG_ACCEPT", "Success! Moving to RideAccepted")
                                            activeOffer = offer
                                            flowState = PassengerFlowState.RideAccepted(state.rideRequest, offer)
                                        }.onFailure { error ->
                                            android.util.Log.e("DEBUG_ACCEPT", "Failed: ${error.message}")
                                        }
                                    }
                                },
                                onDecline = { offer ->
                                    driverOffers = driverOffers.filter { it.id != offer.id }
                                },
                                onCancel = {
                                    coroutineScope.launch {
                                        TarikiRepository.updateRideStatus(state.rideRequest.id ?: "", "cancelled")
                                        flowState = PassengerFlowState.Idle
                                    }
                                }
                            )
                        }
                        is PassengerFlowState.RideAccepted -> {
                            BottomAcceptedCard(
                                offer = state.acceptedOffer,
                                status = state.rideRequest.status,
                                onCancel = {
                                    coroutineScope.launch {
                                        TarikiRepository.updateRideStatus(state.rideRequest.id ?: "", "cancelled")
                                        flowState = PassengerFlowState.Idle
                                    }
                                }
                            )
                        }
                        is PassengerFlowState.ActiveRide -> {
                            BottomActiveCard(price = proposedPrice)
                        }
                        is PassengerFlowState.Completed -> {
                            BottomCompletedCard(
                                rating = ratingGiven,
                                onRatingChange = { ratingGiven = it },
                                onDone = {
                                    flowState = PassengerFlowState.Idle
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────── REUSABLE COMPOSABLE RENDERS FOR FLOW STATES ────────────────

@Composable
fun BottomIdleCard(onSearchClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TarikiColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.where_to),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.TextPrimary
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(TarikiColors.Cream, RoundedCornerShape(12.dp))
                    .clickable { onSearchClick() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "مثال: باب الزوار، حيدرة، الجزائر...",
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun BottomLocationsCard(
    pickup: String,
    destination: String,
    onPickupChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onSuggestionSelect: (String, Double, Double) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var geocoderSuggestions by remember { mutableStateOf<List<Triple<String, Double, Double>>>(emptyList()) }
    val geocoder = remember { Geocoder(context) }

    LaunchedEffect(destination) {
        if (destination.length > 2) {
            val results = RouteUtils.getSearchSuggestions(destination)
            if (results.isNotEmpty()) {
                geocoderSuggestions = results
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        val fallback = geocoder.getFromLocationName("$destination, الجزائر", 5)
                        if (!fallback.isNullOrEmpty()) {
                            val mapped = fallback.mapNotNull {
                                if (it.hasLatitude() && it.hasLongitude()) {
                                    val name = (it.featureName ?: it.locality ?: it.subAdminArea ?: it.countryName ?: destination).toString()
                                    Triple(name, it.latitude, it.longitude)
                                } else null
                            }.distinctBy { it.first }
                            geocoderSuggestions = mapped
                        } else {
                            geocoderSuggestions = emptyList()
                        }
                    } catch (e: Exception) {
                        geocoderSuggestions = emptyList()
                    }
                }
            }
        } else {
            geocoderSuggestions = emptyList()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .imePadding(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TarikiColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "حدد الرحلة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TarikiColors.TextPrimary
                )
            }

            Spacer(Modifier.height(14.dp))

            // Pickup input
            OutlinedTextField(
                value = pickup,
                onValueChange = onPickupChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = TarikiColors.DarkGreen) },
                label = { Text("موقع الانطلاق") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TarikiColors.DarkGreen,
                    unfocusedBorderColor = TarikiColors.Border,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black
                )
            )

            Spacer(Modifier.height(12.dp))

            // Destination input
            OutlinedTextField(
                value = destination,
                onValueChange = onDestinationChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.PinDrop, contentDescription = null, tint = TarikiColors.Error) },
                label = { Text(stringResource(R.string.where_to)) },
                placeholder = { Text("أدخل وجهتك") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TarikiColors.DarkGreen,
                    unfocusedBorderColor = TarikiColors.Border,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black
                )
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "اقتراحات وجهات عالمية و وطنية للرحلة:",
                fontSize = 13.sp,
                color = TarikiColors.TextMuted,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(6.dp))

            // Suggestions List
            val defaultSuggestions = listOf(
                Triple("بوغفالة المسجد العتيق 🇩🇿", 36.7538, 3.0588),
                Triple("مطار الجزائر 🇩🇿", 36.6910, 3.2154),
                Triple("مقام الشهيد 🇩🇿", 36.7458, 3.0594),
                Triple("محطة خروبة 🇩🇿", 36.7335, 3.0857),
                Triple("الجزائر الوسطى 🇩🇿", 36.7753, 3.0601),
                Triple("باب الزوار 🇩🇿", 36.7202, 3.1856),
                Triple("وهران وسط 🇩🇿", 35.7001, -0.6409),
                Triple("قسنطينة 🇩🇿", 36.3650, 6.6147)
            )

            if (destination.isNotBlank() && geocoderSuggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(TarikiColors.Cream, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    geocoderSuggestions.take(5).forEach { (name, lat, lng) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSuggestionSelect(name, lat, lng)
                                    geocoderSuggestions = emptyList() // dismiss
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TarikiColors.DarkGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    defaultSuggestions.forEach { (name, lat, lng) ->
                        val isSelected = destination == name
                        SuggestionChip(
                            onClick = { onSuggestionSelect(name, lat, lng) },
                            label = { Text(name) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) TarikiColors.DarkGreen else TarikiColors.Cream,
                                labelColor = if (isSelected) Color.White else TarikiColors.TextPrimary
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isSelected) TarikiColors.DarkGreen else TarikiColors.Border
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (destination.isNotBlank() && geocoderSuggestions.isNotEmpty()) {
                        val firstSug = geocoderSuggestions.first()
                        onSuggestionSelect(firstSug.first, firstSug.second, firstSug.third)
                    }
                    onNext()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TarikiColors.DarkGreen)
            ) {
                Text(text = stringResource(R.string.continue_btn), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BottomPriceProposalCard(
    suggestedPrice: Int,
    selectionPrice: Int,
    distance: Double,
    onPriceChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TarikiColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.propose_price),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TarikiColors.TextPrimary
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "${String.format("%.1f", distance)} km",
                fontSize = 14.sp,
                color = TarikiColors.TextMuted,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))
            // Large interactive proposed price input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$selectionPrice",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = TarikiColors.DarkGreen
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.dzd),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TarikiColors.TextPrimary
                )
            }

            Text(
                text = "السعر المقترح: $suggestedPrice ${stringResource(R.string.dzd)}",
                fontSize = 13.sp,
                color = TarikiColors.TextMuted
            )

            Spacer(Modifier.height(16.dp))

            // Slider to tweak price easily
            Slider(
                value = selectionPrice.toFloat(),
                onValueChange = { onPriceChange((it.toInt() / 50) * 50) },
                valueRange = (suggestedPrice * 0.7f)..(suggestedPrice * 1.5f),
                colors = SliderDefaults.colors(
                    thumbColor = TarikiColors.DarkGreen,
                    activeTrackColor = TarikiColors.DarkGreen,
                    inactiveTrackColor = TarikiColors.Border
                )
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TarikiColors.DarkGreen)
            ) {
                Text(text = "ابحث عن سائق 👤", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BottomWaitingCard(
    priceProposed: Int,
    offers: List<DriverOffer>,
    onAccept: (DriverOffer) -> Unit,
    onDecline: (DriverOffer) -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TarikiColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.searching_drivers),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TarikiColors.DarkGreen
                    )
                    Text(
                        text = "جاري تلقي العروض بقيمة $priceProposed دج",
                        fontSize = 12.sp,
                        color = TarikiColors.TextMuted
                    )
                }
                CircularProgressIndicator(color = TarikiColors.DarkGreen, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.height(14.dp))

            if (offers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "في انتظار عروض السائقين...",
                        color = TarikiColors.TextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(offers) { offer ->
                        OfferRowItem(offer = offer, onAccept = { onAccept(offer) }, onDecline = { onDecline(offer) })
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TarikiColors.Error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TarikiColors.Error)
            ) {
                Text(text = stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OfferRowItem(
    offer: DriverOffer,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var driverName by remember(offer.driverId) { mutableStateOf(if (offer.driverId == "sim_driver_ahmed") "أحمد" else if (offer.driverId == "sim_driver_kamal") "كمال" else "جاري التحميل...") }
    var carDetails by remember(offer.driverId) { mutableStateOf("سيارة طريقي • ⭐️ 4.9") }

    LaunchedEffect(offer.driverId) {
        if (offer.driverId != "sim_driver_ahmed" && offer.driverId != "sim_driver_kamal") {
            com.example.data.repository.TarikiRepository.getUserById(offer.driverId).onSuccess { user ->
                if (user != null && !user.fullName.isNullOrBlank()) {
                    driverName = user.fullName
                }
            }
            com.example.data.repository.TarikiRepository.getDriverProfile(offer.driverId).onSuccess { profile ->
                if (profile != null) {
                    carDetails = "${profile.carBrand} ${profile.carModel} • ${profile.carColor} • ⭐️ 5.0"
                }
            }
        } else {
            if (offer.driverId == "sim_driver_ahmed") {
                driverName = "أحمد البهجة"
                carDetails = "Dacia Logan • ⭐️ 4.8"
            } else {
                driverName = "كمال بن عيسى"
                carDetails = "Hyundai Atos • ⭐️ 4.9"
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TarikiColors.Cream, RoundedCornerShape(12.dp))
            .border(1.dp, TarikiColors.Border, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = driverName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.TextPrimary
            )
            Text(
                text = carDetails,
                fontSize = 12.sp,
                color = TarikiColors.TextMuted
            )
            Text(
                text = "يصل خلال ${offer.etaMinutes ?: 3} دقائق",
                fontSize = 11.sp,
                color = TarikiColors.Success,
                fontWeight = FontWeight.Bold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${offer.offeredPrice} دج",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.DarkGreen,
                modifier = Modifier.padding(end = 4.dp)
            )
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = TarikiColors.DarkGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(R.string.accept), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(
                onClick = onDecline,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Decline", tint = TarikiColors.Error)
            }
        }
    }
}

@Composable
fun BottomAcceptedCard(
    offer: DriverOffer?,
    status: String = "accepted",
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val driverId = offer?.driverId ?: ""
    var driverName by remember(driverId) { mutableStateOf(if (driverId == "sim_driver_ahmed") "أحمد" else if (driverId == "sim_driver_kamal") "كمال" else "جاري التحميل...") }
    var carDetails by remember(driverId) { mutableStateOf("سيارة رونو سامبول • 24890 119 16") }
    var driverPhone by remember(driverId) { mutableStateOf(if (driverId == "sim_driver_ahmed") "+213555001122" else if (driverId == "sim_driver_kamal") "+213666445566" else "") }

    LaunchedEffect(driverId) {
        if (driverId.isNotEmpty() && driverId != "sim_driver_ahmed" && driverId != "sim_driver_kamal") {
            com.example.data.repository.TarikiRepository.getUserById(driverId).onSuccess { user ->
                if (user != null) {
                    if (!user.fullName.isNullOrBlank()) {
                        driverName = user.fullName
                    }
                    driverPhone = user.phone
                }
            }
            com.example.data.repository.TarikiRepository.getDriverProfile(driverId).onSuccess { profile ->
                if (profile != null) {
                    carDetails = "${profile.carBrand} ${profile.carModel} • ${profile.licensePlate}"
                }
            }
        } else {
            if (driverId == "sim_driver_ahmed") {
                driverName = "أحمد بن علي"
                carDetails = "Dacia Logan • 14638 116 16"
                driverPhone = "+213555001122"
            } else if (driverId == "sim_driver_kamal") {
                driverName = "كمال بومدين"
                carDetails = "Hyundai Atos • 28931 112 16"
                driverPhone = "+213666445566"
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TarikiColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (status == "driver_arrived") "وصل السائق! 📍" else stringResource(R.string.ride_accepted),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.DarkGreen
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (status == "driver_arrived") "السائق في انتظارك الآن في نقطة الانطلاق" else stringResource(R.string.driver_arriving),
                fontSize = 14.sp,
                color = TarikiColors.Success,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(16.dp))

            // Driver Card details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TarikiColors.Cream, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TarikiColors.DarkGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🚗", fontSize = 22.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = driverName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TarikiColors.TextPrimary
                    )
                    Text(
                        text = carDetails,
                        fontSize = 12.sp,
                        color = TarikiColors.TextMuted
                    )
                }

                Text(
                    text = "${offer?.offeredPrice} دج",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TarikiColors.DarkGreen
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (driverPhone.isNotEmpty()) {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                    data = android.net.Uri.parse("tel:$driverPhone")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TarikiColors.DarkGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(text = "اتصال")
                }

                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TarikiColors.Error),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TarikiColors.Error),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
fun BottomActiveCard(price: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TarikiColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "أنت في الطريق الآن 🗺️",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.DarkGreen
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "رحلتك جارية إلى الوجهة المحددة",
                fontSize = 13.sp,
                color = TarikiColors.TextMuted
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "القيمة المتفق عليها:", fontSize = 14.sp, color = TarikiColors.TextPrimary)
                Text(text = "$price دج", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TarikiColors.DarkGreen)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { /* SOS trigger */ },
                colors = ButtonDefaults.buttonColors(containerColor = TarikiColors.Error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(text = "طلب نجدة SOS", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun BottomCompletedCard(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    onDone: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TarikiColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "وصلت بالسلامة! 🎉",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.DarkGreen
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.rate_experience),
                fontSize = 14.sp,
                color = TarikiColors.TextMuted
            )

            Spacer(Modifier.height(16.dp))

            // Star selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 1..5) {
                    val tint = if (i <= rating) TarikiColors.Warning else TarikiColors.Border
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onRatingChange(i) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TarikiColors.DarkGreen)
            ) {
                Text(text = "إنهاء", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
