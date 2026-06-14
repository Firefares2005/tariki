package com.example.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
import com.example.R
import com.example.core.config.SessionManager
import com.example.ui.theme.TarikiColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val details = SessionManager.getUserDetails()
        name = details["name"] ?: "مستخدم طريقي"
        userType = details["type"] ?: "passenger"
        phone = details["phone"] ?: "+213555123456"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "الملف الشخصي", fontWeight = FontWeight.Bold, color = TarikiColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TarikiColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TarikiColors.Cream)
            )
        },
        containerColor = TarikiColors.Cream
    ) { innerPadding ->
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

            Spacer(Modifier.height(40.dp))

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
                    ProfileFieldItem(label = "إجمالي الرحلات المكتملة", value = "21 رحلة")
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
