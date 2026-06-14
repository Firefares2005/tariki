package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TarikiLightColorScheme = lightColorScheme(
    primary = TarikiColors.DarkGreen,
    onPrimary = Color.White,
    primaryContainer = TarikiColors.KeyGreen,
    onPrimaryContainer = Color(0xFF002204),
    secondary = TarikiColors.DarkGreen,
    onSecondary = Color.White,
    background = TarikiColors.Cream,
    onBackground = TarikiColors.TextPrimary,
    surface = TarikiColors.White,
    onSurface = TarikiColors.TextPrimary,
    surfaceVariant = TarikiColors.LightGray,
    onSurfaceVariant = TarikiColors.TextMuted,
    outline = TarikiColors.Border,
    error = TarikiColors.Error,
    onError = Color.White
)

private val TarikiDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CD69B),
    onPrimary = Color(0xFF02390E),
    primaryContainer = Color(0xFF215026),
    onPrimaryContainer = Color(0xFFBCF0B3),
    secondary = Color(0xFF9CD69B),
    onSecondary = Color(0xFF02390E),
    background = TarikiColors.BlackSurface,
    onBackground = Color(0xFFE2E3DC),
    surface = Color(0xFF1A1C16),
    onSurface = Color(0xFFE2E3DC),
    surfaceVariant = Color(0xFF3F443B),
    onSurfaceVariant = Color(0xFFBEC4B7),
    outline = Color(0xFF8B9285),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun TarikiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        TarikiDarkColorScheme
    } else {
        TarikiLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep MyApplicationTheme for backward-compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // disabled dynamicColor to preserve Tariki's exact brand colors
    content: @Composable () -> Unit
) {
    TarikiTheme(darkTheme = darkTheme, content = content)
}
