package com.example.presentation.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.models.DriverProfile
import com.example.data.repository.TarikiRepository
import com.example.ui.theme.TarikiColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverCarSetupScreen(
    phone: String,
    onNavigateToHome: (phone: String, userType: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var carBrand by remember { mutableStateOf("") }
    var carModel by remember { mutableStateOf("") }
    var carYear by remember { mutableStateOf("") }
    var carColor by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }

    var carPhotoUrl by remember { mutableStateOf<String?>(null) }
    var licensePhotoUrl by remember { mutableStateOf<String?>(null) }

    var isUploadingCar by remember { mutableStateOf(false) }
    var isUploadingLicense by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    text = stringResource(R.string.car_setup),
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
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(8.dp))

            // Brand & Model inputs
            OutlinedCarInput(label = stringResource(R.string.car_brand), value = carBrand, onValueChange = { carBrand = it }, placeholder = "Renault, Peugeot, Hyundai")
            Spacer(Modifier.height(14.dp))
            OutlinedCarInput(label = stringResource(R.string.car_model), value = carModel, onValueChange = { carModel = it }, placeholder = "Symbol, Ibzia, Accent")
            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCarInput(label = stringResource(R.string.car_year), value = carYear, onValueChange = { carYear = it }, placeholder = "2021")
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCarInput(label = stringResource(R.string.car_color), value = carColor, onValueChange = { carColor = it }, placeholder = "Grey, White")
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedCarInput(label = stringResource(R.string.license_plate), value = licensePlate, onValueChange = { licensePlate = it }, placeholder = "12345 119 16")

            Spacer(Modifier.height(24.dp))

            // Document uploads
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Car Photo Box
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(R.string.car_photo), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TarikiColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TarikiColors.White)
                            .border(1.dp, TarikiColors.Border, RoundedCornerShape(12.dp))
                            .clickable {
                                coroutineScope.launch {
                                    isUploadingCar = true
                                    val url = B2Config.uploadToB2(phone.replace("+", ""), "cars", "car.jpg")
                                    carPhotoUrl = url
                                    isUploadingCar = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (carPhotoUrl != null) {
                            Text(text = "🚗", fontSize = 28.sp)
                        } else if (isUploadingCar) {
                            CircularProgressIndicator(color = TarikiColors.DarkGreen, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = TarikiColors.TextMuted)
                        }
                    }
                }

                // License Photo Box
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(R.string.license_photo), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TarikiColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TarikiColors.White)
                            .border(1.dp, TarikiColors.Border, RoundedCornerShape(12.dp))
                            .clickable {
                                coroutineScope.launch {
                                    isUploadingLicense = true
                                    val url = B2Config.uploadToB2(phone.replace("+", ""), "cars", "license.jpg")
                                    licensePhotoUrl = url
                                    isUploadingLicense = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (licensePhotoUrl != null) {
                            Text(text = "🪪", fontSize = 28.sp)
                        } else if (isUploadingLicense) {
                            CircularProgressIndicator(color = TarikiColors.DarkGreen, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = TarikiColors.TextMuted)
                        }
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            val canSave = carBrand.isNotBlank() && carModel.isNotBlank() && carYear.isNotBlank() &&
                    carColor.isNotBlank() && licensePlate.isNotBlank() && !isUploadingCar && !isUploadingLicense

            Button(
                onClick = {
                    if (canSave) {
                        coroutineScope.launch {
                            val details = SessionManager.getUser()
                            val id = details?.first ?: "sim_user_1"
                            
                            val profile = DriverProfile(
                                userId = id,
                                carBrand = carBrand,
                                carModel = carModel,
                                carYear = carYear.toIntOrNull() ?: 2020,
                                carColor = carColor,
                                licensePlate = licensePlate,
                                carPhotoUrl = carPhotoUrl,
                                licensePhotoUrl = licensePhotoUrl,
                                isOnline = true
                            )
                            
                            TarikiRepository.saveDriverProfile(profile).onSuccess {
                                onNavigateToHome(phone, "driver")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canSave,
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

@Composable
fun OutlinedCarInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TarikiColors.TextPrimary)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TarikiColors.White,
                unfocusedContainerColor = TarikiColors.White,
                focusedBorderColor = TarikiColors.DarkGreen,
                unfocusedBorderColor = TarikiColors.Border,
                focusedTextColor = TarikiColors.TextPrimary,
                unfocusedTextColor = TarikiColors.TextPrimary
            ),
            placeholder = { Text(text = placeholder, color = TarikiColors.TextMuted) }
        )
    }
}
