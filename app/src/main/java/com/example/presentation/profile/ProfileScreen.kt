package com.example.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.config.SessionManager
import com.example.data.models.RideRequest
import com.example.data.repository.TarikiRepository
import com.example.ui.theme.TarikiColors
import kotlinx.coroutines.launch

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    
    val deviceId = remember(context) {
        try {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "simulated_device_id"
        } catch (e: Exception) {
            "simulated_device_id"
        }
    }

    var showHistory by remember { mutableStateOf(false) }
    var rideHistory by remember { mutableStateOf<List<RideRequest>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val details = SessionManager.getUserDetails()
        userId = details["id"] ?: ""
        name = details["name"] ?: "مستخدم طريقي"
        userType = details["type"] ?: "passenger"
        phone = details["phone"] ?: "+213555123456"
    }

    LaunchedEffect(showHistory, userId) {
        if (showHistory && userId.isNotEmpty()) {
            isLoadingHistory = true
            TarikiRepository.getRideHistory(userId).onSuccess { history ->
                rideHistory = history
            }
            isLoadingHistory = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (showHistory) "سجل الرحلات" else "الملف الشخصي", 
                        fontWeight = FontWeight.Bold, 
                        color = TarikiColors.TextPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showHistory) {
                            showHistory = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack, 
                            contentDescription = "Back", 
                            tint = TarikiColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TarikiColors.Cream)
            )
        },
        containerColor = TarikiColors.Cream
    ) { innerPadding ->
        if (showHistory) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (isLoadingHistory) {
                    CircularProgressIndicator(
                        color = TarikiColors.DarkGreen,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (rideHistory.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🚙",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "لا توجد رحلات مسجلة بعد",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TarikiColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ابدأ بطلب أو قبول الرحلات لتظهر سجلاتك هنا.",
                            fontSize = 14.sp,
                            color = TarikiColors.TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(rideHistory) { ride ->
                            HistoryRideCard(ride = ride, currentUserId = userId)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))

                // Avatar Ring in DarkGreen
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(TarikiColors.White)
                        .border(3.dp, TarikiColors.DarkGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👤", fontSize = 52.sp)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = name.ifEmpty { "مستخدم طريقي" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TarikiColors.TextPrimary
                )

                Spacer(Modifier.height(4.dp))

                // User type badge
                Row(
                    modifier = Modifier
                        .background(
                            color = TarikiColors.DarkGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (userType == "driver") Icons.Default.DirectionsCar else Icons.Default.Person,
                        contentDescription = null,
                        tint = TarikiColors.DarkGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (userType == "driver") stringResource(R.string.driver) else stringResource(R.string.passenger),
                        color = TarikiColors.DarkGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(30.dp))

                // Details list cards
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TarikiColors.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProfileFieldItem(label = "رقم الهاتف", value = phone)
                        Divider(color = TarikiColors.Cream, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                        ProfileFieldItem(label = "التقييم العام", value = "5.0 ★")
                        Divider(color = TarikiColors.Cream, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                        ProfileFieldItem(label = "دور العضوية الحالي", value = if (userType == "driver") "سائق معتمد 🚗" else "راكب مميز 👤")
                        Divider(color = TarikiColors.Cream, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(deviceId))
                                    android.widget.Toast.makeText(context, "تم نسخ معرف الجهاز بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "معرف الجهاز (ID)", color = TarikiColors.TextMuted, fontSize = 14.sp)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = deviceId, color = TarikiColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "(اضغط للنسخ)", color = TarikiColors.DarkGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 1. History Button
                Button(
                    onClick = { showHistory = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TarikiColors.White,
                        contentColor = TarikiColors.TextPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History, 
                                contentDescription = null, 
                                tint = TarikiColors.DarkGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "سجل رحلاتي", 
                                fontSize = 16.sp, 
                                fontWeight = FontWeight.Bold,
                                color = TarikiColors.TextPrimary
                            )
                        }
                        Text(
                            text = "‹", 
                            fontSize = 24.sp, 
                            fontWeight = FontWeight.Bold,
                            color = TarikiColors.TextMuted
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 2. Logout Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            SessionManager.clear()
                            onLogout()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TarikiColors.Error.copy(alpha = 0.1f),
                        contentColor = TarikiColors.Error
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout, 
                            contentDescription = "Logout", 
                            tint = TarikiColors.Error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "تسجيل الخروج", 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold,
                            color = TarikiColors.Error
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "طريقي طريقي - الجزائر © 2026",
                    fontSize = 11.sp,
                    color = TarikiColors.TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProfileFieldItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TarikiColors.TextMuted, fontSize = 14.sp)
        Text(text = value, color = TarikiColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun HistoryRideCard(ride: RideRequest, currentUserId: String) {
    val isDriver = ride.acceptedDriverId == currentUserId
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role Badge (Driver vs Passenger)
                Row(
                    modifier = Modifier
                        .background(
                            color = if (isDriver) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDriver) Icons.Default.DirectionsCar else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isDriver) Color(0xFF2E7D32) else Color(0xFF1565C0),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isDriver) "سائق" else "راكب",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDriver) Color(0xFF2E7D32) else Color(0xFF1565C0)
                    )
                }

                // Status Badge
                val (statusText, badgeColor, textColor) = when (ride.status) {
                    "completed" -> Triple("مكتملة 🎉", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                    "cancelled" -> Triple("ملغاة ❌", Color(0xFFFFEBEE), Color(0xFFC62828))
                    "in_progress" -> Triple("قيد التنفيذ 🛣️", Color(0xFFFFF3E0), Color(0xFFE65100))
                    "accepted", "driver_arrived" -> Triple("مقبولة 🚗", Color(0xFFE0F2F1), Color(0xFF00695C))
                    else -> Triple("جاري البحث 🔍", Color(0xFFECEFF1), Color(0xFF37474F))
                }

                Box(
                    modifier = Modifier
                        .background(color = badgeColor, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Location coordinates/addresses
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(TarikiColors.DarkGreen, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(TarikiColors.TextMuted.copy(alpha = 0.5f))
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFE53935), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "الانطلاق: ${ride.pickupAddress}",
                        fontSize = 13.sp,
                        color = TarikiColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "الوجهة: ${ride.destinationAddress}",
                        fontSize = 13.sp,
                        color = TarikiColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = TarikiColors.Cream)
            Spacer(modifier = Modifier.height(10.dp))

            // Final Price row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "سعر الرحلة",
                        fontSize = 12.sp,
                        color = TarikiColors.TextMuted
                    )
                    val agreedPrice = ride.finalPrice ?: ride.passengerProposedPrice
                    Text(
                        text = "$agreedPrice دج",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TarikiColors.DarkGreen
                    )
                }
                
                // Show relative distance or estimation
                ride.distanceKm?.let {
                    Text(
                        text = "المسافة: $it كم",
                        fontSize = 12.sp,
                        color = TarikiColors.TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
