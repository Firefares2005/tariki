package com.example.presentation.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
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
import com.example.core.config.B2Config
import com.example.core.config.SessionManager
import com.example.data.repository.TarikiRepository
import com.example.ui.theme.TarikiColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    phone: String,
    userType: String,
    onNavigateToHome: (phone: String, userType: String) -> Unit,
    onNavigateToCarSetup: (phone: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    text = stringResource(R.string.profile_setup),
                    color = TarikiColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ) },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Circular Photo Upload ───────────────────────────
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(TarikiColors.White)
                    .border(2.dp, TarikiColors.Border, CircleShape)
                    .clickable {
                        coroutineScope.launch {
                            isUploading = true
                            // Simulates Camera photo capture and direct uploads to B2 Bucket
                            val url = B2Config.uploadToB2(phone.replace("+", ""), "users")
                            photoUrl = url
                            isUploading = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl != null) {
                    // Simulates showing the photo
                    Text(
                        text = "📸",
                        fontSize = 48.sp
                    )
                } else if (isUploading) {
                    CircularProgressIndicator(color = TarikiColors.DarkGreen)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = TarikiColors.TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.upload_photo),
                            fontSize = 11.sp,
                            color = TarikiColors.TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // ── Full Name Input ───────────────────────────────────
            Text(
                text = stringResource(R.string.full_name),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TarikiColors.TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TarikiColors.White,
                    unfocusedContainerColor = TarikiColors.White,
                    focusedBorderColor = TarikiColors.DarkGreen,
                    unfocusedBorderColor = TarikiColors.Border,
                    focusedTextColor = TarikiColors.TextPrimary,
                    unfocusedTextColor = TarikiColors.TextPrimary
                ),
                placeholder = {
                    Text(text = "Ali Benflis", color = TarikiColors.TextMuted)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Save/Continue Button ──────────────────────────────
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        coroutineScope.launch {
                            val detailsFlow = SessionManager.getUser()
                            val id = detailsFlow?.first ?: "sim_user_${System.currentTimeMillis()}"
                            
                            TarikiRepository.updateProfile(id, fullName, photoUrl)
                                .onSuccess { updatedUser ->
                                    SessionManager.saveUser(updatedUser)
                                    if (userType == "driver") {
                                        onNavigateToCarSetup(phone)
                                    } else {
                                        onNavigateToHome(phone, userType)
                                    }
                                }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = fullName.isNotBlank() && !isUploading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TarikiColors.DarkGreen,
                    contentColor = Color.White,
                    disabledContainerColor = TarikiColors.TextMuted,
                    disabledContentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
