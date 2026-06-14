package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TarikiColors
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Polyline
import com.example.core.utils.RouteUtils

@Composable
fun TarikiMapView(
    modifier: Modifier = Modifier,
    pickupLat: Double? = null,
    pickupLng: Double? = null,
    destLat: Double? = null,
    destLng: Double? = null,
    pickupAddress: String? = null,
    destinationAddress: String? = null,
    showDrivers: Boolean = false,
    activeDriverAngle: Float = 0f,
    hasLocationPermission: Boolean = false,
    driverLat: Double? = null,
    driverLng: Double? = null
) {
    // Default camera center (Algiers City Centre) if coordinates not provided
    val defaultLat = 36.7538
    val defaultLng = 3.0588

    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var driverRoutePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // Route from passenger pickup to passenger destination (Blue)
    LaunchedEffect(pickupLat, pickupLng, destLat, destLng) {
        if (pickupLat != null && pickupLng != null && destLat != null && destLng != null) {
            val route = RouteUtils.fetchRoute(pickupLat, pickupLng, destLat, destLng)
            routePoints = route
        } else {
            routePoints = emptyList()
        }
    }

    // Route from driver current location to passenger pickup (Red)
    LaunchedEffect(driverLat, driverLng, pickupLat, pickupLng) {
        if (driverLat != null && driverLng != null && pickupLat != null && pickupLng != null) {
            val route = RouteUtils.fetchRoute(driverLat, driverLng, pickupLat, pickupLng)
            driverRoutePoints = route
        } else {
            driverRoutePoints = emptyList()
        }
    }

    val cameraCenter = remember(pickupLat, pickupLng, destLat, destLng) {
        if (pickupLat != null && pickupLng != null) {
            LatLng(pickupLat, pickupLng)
        } else {
            LatLng(defaultLat, defaultLng)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cameraCenter, 14f)
    }

    // Smoothly animate/move camera to encompass all relevant coordinates
    LaunchedEffect(cameraCenter, routePoints, driverRoutePoints, driverLat, driverLng, pickupLat, pickupLng, destLat, destLng) {
        val boundsPoints = mutableListOf<LatLng>()
        if (pickupLat != null && pickupLng != null) boundsPoints.add(LatLng(pickupLat, pickupLng))
        if (destLat != null && destLng != null) boundsPoints.add(LatLng(destLat, destLng))
        if (driverLat != null && driverLng != null) boundsPoints.add(LatLng(driverLat, driverLng))
        boundsPoints.addAll(routePoints)
        boundsPoints.addAll(driverRoutePoints)

        if (boundsPoints.isNotEmpty()) {
            try {
                val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                boundsPoints.forEach { builder.include(it) }
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(builder.build(), 120),
                    1000
                )
            } catch (e: Exception) {
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(cameraCenter, 14f)
                )
            }
        } else {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(cameraCenter, 14f)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFEAEFE3)) // Background theme color matching Tariki aesthetic
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = hasLocationPermission,
                compassEnabled = true
            ),
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission
            )
        ) {
            // green marker for pickup
            if (pickupLat != null && pickupLng != null) {
                Marker(
                    state = com.google.maps.android.compose.MarkerState(position = LatLng(pickupLat, pickupLng)),
                    title = pickupAddress ?: "موقع الراكب",
                    snippet = "موقع الانطلاق",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)
                )
            }

            // red marker for destination
            if (destLat != null && destLng != null) {
                Marker(
                    state = com.google.maps.android.compose.MarkerState(position = LatLng(destLat, destLng)),
                    title = destinationAddress ?: "الوجهة",
                    snippet = "موقع الوصول"
                )
            }

            // marker for driver himself if coordinates passed
            if (driverLat != null && driverLng != null) {
                Marker(
                    state = com.google.maps.android.compose.MarkerState(position = LatLng(driverLat, driverLng)),
                    title = "موقعي الحالي كـ سائق 🚗",
                    snippet = "مكاني الآن",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN)
                )
            }

            // show surrounding driver markers if showDrivers is true & no specific driver is bound
            if (showDrivers && pickupLat != null && pickupLng != null && (driverLat == null || driverLng == null)) {
                Marker(
                    state = com.google.maps.android.compose.MarkerState(position = LatLng(pickupLat + 0.003, pickupLng - 0.002)),
                    title = "سائق طريقي 🚗",
                    snippet = "متوفر بالقرب منك",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN)
                )
                Marker(
                    state = com.google.maps.android.compose.MarkerState(position = LatLng(pickupLat - 0.002, pickupLng + 0.003)),
                    title = "سائق طريقي 🚗",
                    snippet = "متوفر بالقرب منك",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN)
                )
            }

            // Red route from Driver's location to Passenger's location
            if (driverRoutePoints.isNotEmpty()) {
                Polyline(
                    points = driverRoutePoints,
                    color = Color(0xFFE53935), // Beautiful Red for driver path to pickup
                    width = 14f
                )
            }

            // Blue route from Passenger's location to Destination
            if (routePoints.isNotEmpty()) {
                Polyline(
                    points = routePoints,
                    color = Color(0xFF2196F3), // Bright elegant Google Maps Blue
                    width = 14f
                )
            }
        }
    }
}
