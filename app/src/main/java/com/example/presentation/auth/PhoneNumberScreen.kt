package com.example.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.core.utils.HapticUtils
import com.example.core.utils.LanguageUtils
import com.example.core.utils.SUPPORTED_LANGUAGES
import com.example.ui.theme.TarikiColors

@Composable
fun PhoneNumberScreen(
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onNavigateToProfile: (phone: String, userType: String) -> Unit,
    onNavigateToHome: (phone: String, userType: String) -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var phoneDigits by remember { mutableStateOf("") }
    var selectedUserType by remember { mutableStateOf("passenger") } // "passenger" | "driver"
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMsg by authViewModel.error.collectAsState()

    var dropdownExpanded by remember { mutableStateOf(false) }

    val isRtl = LanguageUtils.isRtl(currentLanguageCode)
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TarikiColors.Cream)
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                // ── Language Selector Button (Top Row) ─────────────────────────────
                Box(
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    val currentLang = SUPPORTED_LANGUAGES.find { it.code == currentLanguageCode } ?: SUPPORTED_LANGUAGES[0]
                    Button(
                        onClick = { dropdownExpanded = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TarikiColors.DarkGreen,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        val flag = when (currentLang.code) {
                            "ar" -> "🇩🇿 "
                            "fr" -> "🇫🇷 "
                            else -> "🇬🇧 "
                        }
                        Text(text = "$flag${currentLang.displayName}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(TarikiColors.White)
                    ) {
                        SUPPORTED_LANGUAGES.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(text = lang.displayName, color = TarikiColors.TextPrimary) },
                                onClick = {
                                    dropdownExpanded = false
                                    onLanguageSelected(lang.code)
                                }
                            )
                        }
                    }
                }

                // ── Logo ──────────────────────────────────────────
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "طريقي",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = TarikiColors.TextPrimary
                )
                Text(
                    text = "TARIKI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 6.sp,
                    color = TarikiColors.TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.enter_phone),
                    fontSize = 15.sp,
                    color = TarikiColors.TextMuted
                )

                // Error banner if any
                errorMsg?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(text = it, color = TarikiColors.Error, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // ── Phone Display Field ───────────────────────────
                Spacer(Modifier.height(24.dp))
                PhoneDisplayField(digits = phoneDigits)

                // ── Passenger / Driver Toggle ─────────────────────
                Spacer(Modifier.height(20.dp))
                UserTypeSelector(
                    selected = selectedUserType,
                    onSelect = { selectedUserType = it }
                )

                Spacer(Modifier.height(32.dp))

                // ── Loading Indicator ─────────────────────────────
                if (isLoading) {
                    CircularProgressIndicator(color = TarikiColors.DarkGreen)
                    Spacer(Modifier.height(16.dp))
                }

                // ── Numeric Keypad (RTL aware arrangement) ───────
                NumericKeypad(
                    onDigit = { digit ->
                        if (phoneDigits.length < 9) {
                            if (digit == "0" && phoneDigits.isEmpty()) return@NumericKeypad
                            phoneDigits += digit
                            HapticUtils.keyPress(context) // VIBRATE on every press
                        }
                    },
                    onBackspace = {
                        if (phoneDigits.isNotEmpty()) {
                            phoneDigits = phoneDigits.dropLast(1)
                            HapticUtils.keyPress(context)
                        }
                    },
                    onBackspaceLongPress = {
                        phoneDigits = ""
                        HapticUtils.keyPress(context)
                    },
                    onContinue = {
                        if (phoneDigits.length == 9 && !isLoading) {
                            val formattedPhone = "+213$phoneDigits"
                            authViewModel.loginOrRegister(
                                phone = formattedPhone,
                                userType = selectedUserType,
                                onExistingUser = {
                                    onNavigateToHome(formattedPhone, selectedUserType)
                                },
                                onNewUser = {
                                    onNavigateToProfile(formattedPhone, selectedUserType)
                                },
                                onError = {}
                            )
                        }
                    },
                    canContinue = phoneDigits.length == 9 && !isLoading,
                    isRtl = isRtl
                )
            }
        }
    }
}

@Composable
fun PhoneDisplayField(digits: String) {
    val placeholder = "  _ _ _  _ _ _  _ _ _  "
    val formattedDigits = buildString {
        digits.forEachIndexed { i, c ->
            if (i == 3 || i == 6) append("  ")
            append(c)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(TarikiColors.White, RoundedCornerShape(14.dp))
            .border(1.dp, TarikiColors.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (digits.isEmpty()) placeholder else formattedDigits,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = if (digits.isEmpty()) TarikiColors.TextMuted else TarikiColors.TextPrimary,
            letterSpacing = 2.sp,
            modifier = Modifier.weight(1f)
        )
        // +213 badge — vertical divider + label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(TarikiColors.Border)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "+213",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TarikiColors.TextPrimary
            )
        }
    }
}

@Composable
fun UserTypeSelector(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UserTypeButton(
            label = stringResource(R.string.passenger),
            icon = Icons.Default.Person,
            isSelected = selected == "passenger",
            onClick = { onSelect("passenger") },
            modifier = Modifier.weight(1f)
        )
        UserTypeButton(
            label = stringResource(R.string.driver),
            icon = Icons.Default.DirectionsCar,
            isSelected = selected == "driver",
            onClick = { onSelect("driver") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun UserTypeButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) TarikiColors.DarkGreen else TarikiColors.White,
            contentColor   = if (isSelected) Color.White           else TarikiColors.DarkGreen
        ),
        border = if (!isSelected) BorderStroke(1.5.dp, TarikiColors.DarkGreen) else null,
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
fun NumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackspaceLongPress: () -> Unit,
    onContinue: () -> Unit,
    canContinue: Boolean,
    isRtl: Boolean
) {
    // Key layout: Arabic order when RTL (3,2,1 etc.)
    val rows = if (isRtl) {
        listOf(
            listOf("3", "2", "1"),
            listOf("6", "5", "4"),
            listOf("9", "8", "7"),
            listOf("*", "0", "⌫")
        )
    } else {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "⌫")
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    KeypadKey(
                        label = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (key) {
                                "⌫" -> onBackspace()
                                "*" -> { /* no-op */ }
                                else -> onDigit(key)
                            }
                        },
                        onLongClick = if (key == "⌫") onBackspaceLongPress else null
                    )
                }
            }
        }

        AnimatedVisibility(visible = canContinue) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TarikiColors.DarkGreen,
                    contentColor   = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(
                    text = stringResource(R.string.continue_btn),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .background(TarikiColors.KeyGreen, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (label == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "backspace",
                tint = TarikiColors.TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TarikiColors.TextPrimary
            )
        }
    }
}
