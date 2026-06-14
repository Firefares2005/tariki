package com.example.presentation.driver

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.R
import com.example.core.config.SessionManager
import com.example.data.models.DriverOffer
import com.example.data.models.RideRequest
import com.example.data.repository.TarikiRepository
import com.example.presentation.components.TarikiMapView
import com.example.ui.theme.TarikiColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

sealed class DriverFlowState {
    object Offline : DriverFlowState()
    object IdleOnline : DriverFlowState()
    data class NegotiationOffer(val rideRequest: RideRequest) : DriverFlowState()
    data class WaitingForPassenger(val rideRequest: RideRequest, val offer: DriverOffer) : DriverFlowState()
    data class RideAccepted(val rideRequest: RideRequest) : DriverFlowState()
    data class DriveInProgress(val rideRequest: RideRequest) : DriverFlowState()
    data class DriveCompleted(val rideRequest: RideRequest) : DriverFlowState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverHomeScreen(
    phone: String,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var flowState by remember { mutableStateOf<DriverFlowState>(DriverFlowState.Offline) }

    // Session states
    var driverName by remember { mutableStateOf("") }
    var driverRating by remember { mutableStateOf("5.0") }
    var driverId by remember { mutableStateOf("") }

    val context = LocalContext.current
    val fusedLocationClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }

    var driverLat by remember { mutableStateOf<Double?>(null) }
    var driverLng by remember { mutableStateOf<Double?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val fetchExactLocation = remember(fusedLocationClient, flowState, driverId) {
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
                            driverLat = loc.latitude
                            driverLng = loc.longitude
                            if (flowState !is DriverFlowState.Offline && driverId.isNotEmpty()) {
                                coroutineScope.launch {
                                    TarikiRepository.updateDriverLocationInDb(driverId, loc.latitude, loc.longitude)
                                }
                            }
                        } else {
                            try {
                                val fineCheckInner = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                                val coarseCheckInner = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (fineCheckInner == android.content.pm.PackageManager.PERMISSION_GRANTED || coarseCheckInner == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                        if (lastLoc != null) {
                                            driverLat = lastLoc.latitude
                                            driverLng = lastLoc.longitude
                                            if (flowState !is DriverFlowState.Offline && driverId.isNotEmpty()) {
                                                coroutineScope.launch {
                                                    TarikiRepository.updateDriverLocationInDb(driverId, lastLoc.latitude, lastLoc.longitude)
                                                }
                                            }
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

    // Automatically trigger GPS request dialog on app open
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

    // Periodically update online driver position on network/database
    LaunchedEffect(flowState, driverId) {
        if (driverId.isNotEmpty()) {
            fetchExactLocation() // instant fetch
        }
        while (flowState !is DriverFlowState.Offline && driverId.isNotEmpty()) {
            delay(10000)
            fetchExactLocation()
        }
    }

    // Available requests polled while online
    var availableRides by remember { mutableStateOf<List<RideRequest>>(emptyList()) }
    var activeOfferPrice by remember { mutableStateOf(200) }

    // Rating value
    var ratingGiven by remember { mutableStateOf(5) }

    // Fetch user details
    LaunchedEffect(Unit) {
        val details = SessionManager.getUserDetails()
        driverName = details["name"] ?: "السائق"
        driverId = details["id"] ?: "sim_driver"

        if (driverId.isNotEmpty() && driverId != "sim_driver") {
            android.util.Log.d("DEBUG_DRIVER", "Checking active ride/offer for driver: $driverId")
            
            // 1. Check active ride request accepted/in progress by driver
            val activeReq = TarikiRepository.getActiveRideRequestForDriver(driverId)
            if (activeReq != null) {
                android.util.Log.d("DEBUG_DRIVER", "Found active ride request: ${activeReq.status}")
                flowState = when (activeReq.status) {
                    "accepted", "driver_arrived" -> DriverFlowState.RideAccepted(activeReq)
                    "in_progress" -> DriverFlowState.DriveInProgress(activeReq)
                    else -> DriverFlowState.Offline
                }
            } else {
                // 2. Check pending offer made by driver
                val pendingOffer = TarikiRepository.getPendingOfferForDriver(driverId)
                if (pendingOffer != null) {
                    android.util.Log.d("DEBUG_DRIVER", "Found pending offer for ride: ${pendingOffer.rideRequestId}")
                    val rideReq = TarikiRepository.getRideRequestById(pendingOffer.rideRequestId)
                    if (rideReq != null && rideReq.status == "searching") {
                        flowState = DriverFlowState.WaitingForPassenger(rideReq, pendingOffer)
                    } else {
                        val profile = TarikiRepository.getDriverProfile(driverId).getOrNull()
                        if (profile?.isOnline == true) {
                            flowState = DriverFlowState.IdleOnline
                        }
                    }
                } else {
                    val profile = TarikiRepository.getDriverProfile(driverId).getOrNull()
                    if (profile?.isOnline == true) {
                        flowState = DriverFlowState.IdleOnline
                    }
                }
            }
        }
    }

    // Polling loop for active searching ride requests
    LaunchedEffect(flowState) {
        while (flowState is DriverFlowState.IdleOnline) {
            TarikiRepository.getAvailableRideRequests().onSuccess { requests ->
                availableRides = requests
            }
            delay(5000)
        }
    }

    // Checking if passenger accepts driver offer
    LaunchedEffect(flowState, driverId) {
        if (flowState is DriverFlowState.WaitingForPassenger) {
            val state = (flowState as DriverFlowState.WaitingForPassenger)
            val offerId = state.offer.id ?: ""
            while (true) {
                try {
                    val current = TarikiRepository.getRideRequestById(state.rideRequest.id ?: "")
                    android.util.Log.d("DEBUG_DRIVER", "Ride status: ${current?.status}, acceptedDriverId: ${current?.acceptedDriverId}, myId: $driverId")
                    if (current != null) {
                        when {
                            current.status == "accepted" && (
                                current.acceptedDriverId == driverId ||
                                current.acceptedDriverId?.trim() == driverId.trim()
                            ) -> {
                                flowState = DriverFlowState.RideAccepted(current)
                                break
                            }
                            current.status == "cancelled" -> {
                                flowState = DriverFlowState.IdleOnline
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DEBUG_DRIVER", "Error: ${e.message}")
                }
                delay(2000)
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
                                    text = driverName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = driverName.ifEmpty { "Driver" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TarikiColors.TextPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = TarikiColors.Warning, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(text = driverRating, fontSize = 12.sp, color = TarikiColors.TextMuted)
                                }
                            }
                        }

                        // Online/Offline switch & Logout
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (flowState !is DriverFlowState.Offline) stringResource(R.string.online) else stringResource(R.string.offline),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (flowState !is DriverFlowState.Offline) TarikiColors.Success else TarikiColors.TextMuted
                            )
                            Switch(
                                checked = flowState !is DriverFlowState.Offline,
                                onCheckedChange = { isChecked ->
                                    coroutineScope.launch {
                                        TarikiRepository.setDriverOnline(driverId, isChecked).onSuccess {
                                            flowState = if (isChecked) DriverFlowState.IdleOnline else DriverFlowState.Offline
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = TarikiColors.DarkGreen,
                                    uncheckedThumbColor = TarikiColors.TextMuted,
                                    uncheckedTrackColor = TarikiColors.Border
                                )
                            )
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    // Cleanly transition offline first
                                    TarikiRepository.setDriverOnline(driverId, false)
                                    SessionManager.clear()
                                    onLogout()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "تسجيل الخروج",
                                    tint = TarikiColors.Error
                                )
                            }
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
                // Main map element
                TarikiMapView(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    pickupLat = when (val s = flowState) {
                        is DriverFlowState.NegotiationOffer -> s.rideRequest.pickupLat
                        is DriverFlowState.WaitingForPassenger -> s.rideRequest.pickupLat
                        is DriverFlowState.RideAccepted -> s.rideRequest.pickupLat
                        is DriverFlowState.DriveInProgress -> s.rideRequest.pickupLat
                        is DriverFlowState.DriveCompleted -> s.rideRequest.pickupLat
                        else -> driverLat ?: 36.7538
                    },
                    pickupLng = when (val s = flowState) {
                        is DriverFlowState.NegotiationOffer -> s.rideRequest.pickupLng
                        is DriverFlowState.WaitingForPassenger -> s.rideRequest.pickupLng
                        is DriverFlowState.RideAccepted -> s.rideRequest.pickupLng
                        is DriverFlowState.DriveInProgress -> s.rideRequest.pickupLng
                        is DriverFlowState.DriveCompleted -> s.rideRequest.pickupLng
                        else -> driverLng ?: 3.0588
                    },
                    destLat = when (val s = flowState) {
                        is DriverFlowState.NegotiationOffer -> s.rideRequest.destinationLat
                        is DriverFlowState.WaitingForPassenger -> s.rideRequest.destinationLat
                        is DriverFlowState.RideAccepted -> s.rideRequest.destinationLat
                        is DriverFlowState.DriveInProgress -> s.rideRequest.destinationLat
                        is DriverFlowState.DriveCompleted -> s.rideRequest.destinationLat
                        else -> null
                    },
                    destLng = when (val s = flowState) {
                        is DriverFlowState.NegotiationOffer -> s.rideRequest.destinationLng
                        is DriverFlowState.WaitingForPassenger -> s.rideRequest.destinationLng
                        is DriverFlowState.RideAccepted -> s.rideRequest.destinationLng
                        is DriverFlowState.DriveInProgress -> s.rideRequest.destinationLng
                        is DriverFlowState.DriveCompleted -> s.rideRequest.destinationLng
                        else -> null
                    },
                    pickupAddress = when (val s = flowState) {
                        is DriverFlowState.NegotiationOffer -> s.rideRequest.pickupAddress
                        is DriverFlowState.WaitingForPassenger -> s.rideRequest.pickupAddress
                        is DriverFlowState.RideAccepted -> s.rideRequest.pickupAddress
                        is DriverFlowState.DriveInProgress -> s.rideRequest.pickupAddress
                        is DriverFlowState.DriveCompleted -> s.rideRequest.pickupAddress
                        else -> "موقعي الحالي"
                    },
                    destinationAddress = when (val s = flowState) {
                        is DriverFlowState.NegotiationOffer -> s.rideRequest.destinationAddress
                        is DriverFlowState.WaitingForPassenger -> s.rideRequest.destinationAddress
                        is DriverFlowState.RideAccepted -> s.rideRequest.destinationAddress
                        is DriverFlowState.DriveInProgress -> s.rideRequest.destinationAddress
                        is DriverFlowState.DriveCompleted -> s.rideRequest.destinationAddress
                        else -> null
                    },
                    showDrivers = flowState !is DriverFlowState.Offline,
                    hasLocationPermission = hasLocationPermission
                )

                // Bottom Panel dynamically responding to driver context
                AnimatedContent(
                    targetState = flowState,
                    transitionSpec = {
                        slideInVertically { h -> h } + fadeIn() togetherWith slideOutVertically { h -> -h } + fadeOut()
                    },
                    label = "driverBottomPanel"
                ) { state ->
                    when (state) {
                        is DriverFlowState.Offline -> {
                            DriverOfflineBottomCard()
                        }
                        is DriverFlowState.IdleOnline -> {
                            DriverIdleOnlineBottomCard(
                                rides = availableRides,
                                onSelect = { request ->
                                    activeOfferPrice = request.passengerProposedPrice
                                    flowState = DriverFlowState.NegotiationOffer(request)
                                }
                            )
                        }
                        is DriverFlowState.NegotiationOffer -> {
                            DriverNegotiationBottomCard(
                                ride = state.rideRequest,
                                proposedPrice = activeOfferPrice,
                                onPriceChange = { activeOfferPrice = it },
                                onSend = {
                                    coroutineScope.launch {
                                        val offer = DriverOffer(
                                            rideRequestId = state.rideRequest.id ?: "",
                                            driverId = driverId,
                                            offeredPrice = activeOfferPrice,
                                            status = "pending",
                                            driverLat = 36.75,
                                            driverLng = 3.05,
                                            etaMinutes = 3
                                        )
                                        TarikiRepository.createDriverOffer(offer).onSuccess { savedOffer ->
                                            flowState = DriverFlowState.WaitingForPassenger(state.rideRequest, savedOffer)
                                        }
                                    }
                                },
                                onDecline = {
                                    flowState = DriverFlowState.IdleOnline
                                }
                            )
                        }
                        is DriverFlowState.WaitingForPassenger -> {
                            DriverWaitingForResponseBottomCard(offerPrice = state.offer.offeredPrice)
                        }
                        is DriverFlowState.RideAccepted -> {
                            DriverTripAcceptedBottomCard(
                                ride = state.rideRequest,
                                onArrive = {
                                    coroutineScope.launch {
                                        TarikiRepository.updateRideStatus(state.rideRequest.id ?: "", "driver_arrived")
                                        flowState = DriverFlowState.DriveInProgress(state.rideRequest)
                                    }
                                }
                            )
                        }
                        is DriverFlowState.DriveInProgress -> {
                            DriverTripInProgressBottomCard(
                                ride = state.rideRequest,
                                onEnd = {
                                    coroutineScope.launch {
                                        TarikiRepository.updateRideStatus(state.rideRequest.id ?: "", "completed")
                                        flowState = DriverFlowState.DriveCompleted(state.rideRequest)
                                    }
                                }
                            )
                        }
                        is DriverFlowState.DriveCompleted -> {
                            DriverCompletedBottomCard(
                                rating = ratingGiven,
                                onRatingChange = { ratingGiven = it },
                                onDone = { flowState = DriverFlowState.IdleOnline }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────── DRIVER BOTTOM CARDS ────────────────

@Composable
fun DriverOfflineBottomCard() {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "أنت في وضع غير متصل 🔴",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "قم بالتمرير للاتصال وتلقي عروض الركاب القريبة منك فوراً.",
                fontSize = 13.sp,
                color = TarikiColors.TextMuted
            )
        }
    }
}

@Composable
fun DriverIdleOnlineBottomCard(
    rides: List<RideRequest>,
    onSelect: (RideRequest) -> Unit
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
            Text(
                text = "جاري تتبع الطلبات القريبة... 🟢",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.DarkGreen
            )

            Spacer(Modifier.height(12.dp))

            if (rides.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد طلبات حالية. يرجى الانتظار...",
                        color = TarikiColors.TextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rides) { ride ->
                        AvailableRideItem(ride = ride, onSelect = { onSelect(ride) })
                    }
                }
            }
        }
    }
}

@Composable
fun AvailableRideItem(
    ride: RideRequest,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TarikiColors.Cream, RoundedCornerShape(12.dp))
            .border(1.dp, TarikiColors.Border, RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "طالب الرحلة: ${ride.pickupAddress.take(20)}...",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TarikiColors.TextPrimary
            )
            Text(
                text = "إلى: ${ride.destinationAddress.take(20)}...",
                fontSize = 12.sp,
                color = TarikiColors.TextMuted
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${ride.passengerProposedPrice} دج",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.DarkGreen
            )
            Text(
                text = "تقديم عرض",
                fontSize = 11.sp,
                color = TarikiColors.KeyGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DriverNegotiationBottomCard(
    ride: RideRequest,
    proposedPrice: Int,
    onPriceChange: (Int) -> Unit,
    onSend: () -> Unit,
    onDecline: () -> Unit
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
                text = "مفاوضة السعر المقترح",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.DarkGreen
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "من: ${ride.pickupAddress}",
                fontSize = 13.sp,
                color = TarikiColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "إلى: ${ride.destinationAddress}",
                fontSize = 13.sp,
                color = TarikiColors.TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Counter biddings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { if (proposedPrice > 100) onPriceChange(proposedPrice - 50) }) {
                    Icon(Icons.Default.Remove, contentDescription = null, tint = TarikiColors.DarkGreen)
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "$proposedPrice دج",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TarikiColors.TextPrimary
                )
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = { onPriceChange(proposedPrice + 50) }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = TarikiColors.DarkGreen)
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onSend,
                    colors = ButtonDefaults.buttonColors(containerColor = TarikiColors.DarkGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = stringResource(R.string.send_offer), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TarikiColors.Error),
                    border = BorderStroke(1.dp, TarikiColors.Error),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "تجاهل")
                }
            }
        }
    }
}

@Composable
fun DriverWaitingForResponseBottomCard(offerPrice: Int) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = TarikiColors.DarkGreen)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "جاري إرسال العرض بقيمة $offerPrice دج",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.DarkGreen
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "بانتظار موافقة الراكب على العرض المقدم.",
                fontSize = 13.sp,
                color = TarikiColors.TextMuted
            )
        }
    }
}

@Composable
fun DriverTripAcceptedBottomCard(
    ride: RideRequest,
    onArrive: () -> Unit
) {
    val context = LocalContext.current
    var passengerName by remember(ride.passengerId) { mutableStateOf("زبون طريقي") }
    var passengerPhone by remember(ride.passengerId) { mutableStateOf("") }

    LaunchedEffect(ride.passengerId) {
        TarikiRepository.getUserById(ride.passengerId).onSuccess { user ->
            if (user != null) {
                if (!user.fullName.isNullOrBlank()) {
                    passengerName = user.fullName
                }
                passengerPhone = user.phone
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
                text = "تم قبول عرضك للرحلة! 🎉",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.Success
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "الرجاء التوجه لنقطة الانطلاق لتلقي الراكب",
                fontSize = 13.sp,
                color = TarikiColors.TextMuted
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TarikiColors.Cream, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TarikiColors.DarkGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👤", fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(text = passengerName, fontWeight = FontWeight.Bold, color = TarikiColors.TextPrimary)
                    Text(text = "${ride.pickupAddress}", fontSize = 11.sp, color = TarikiColors.TextMuted)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onArrive,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TarikiColors.DarkGreen)
                ) {
                    Text(text = "وصلت لنقطة الانطلاق 📍", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                if (passengerPhone.isNotEmpty()) {
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                    data = android.net.Uri.parse("tel:$passengerPhone")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier
                            .height(52.dp)
                            .width(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TarikiColors.KeyGreen, contentColor = TarikiColors.DarkGreen),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "اتصال بالراكب"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DriverTripInProgressBottomCard(
    ride: RideRequest,
    onEnd: () -> Unit
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
                text = "الرحلة قيد التنفيذ حالياً 🛣️",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.DarkGreen
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "متوجه إلى: ${ride.destinationAddress}",
                fontSize = 13.sp,
                color = TarikiColors.TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TarikiColors.Error)
            ) {
                Text(text = "إنهاء الرحلة وتحصيل المبلغ 🏁", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DriverCompletedBottomCard(
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
                text = "تم إنهاء الرحلة بنجاح!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.DarkGreen
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "قيّم العميل لمجتمع طريقي",
                fontSize = 13.sp,
                color = TarikiColors.TextMuted
            )

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 1..5) {
                    val tint = if (i <= rating) TarikiColors.Warning else TarikiColors.Border
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onRatingChange(i) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TarikiColors.DarkGreen)
            ) {
                Text(text = "سجل متصل للرحلة التالية", fontWeight = FontWeight.Bold)
            }
        }
    }
}
