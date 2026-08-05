package com.cargps.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4BE99A),
    onPrimary = Color(0xFF002113),
    primaryContainer = Color(0xFF123E2C),
    onPrimaryContainer = Color(0xFFB7F7D3),
    secondary = Color(0xFF8FCDB0),
    background = Color(0xFF06100C),
    surface = Color(0xFF0C1914),
    surfaceVariant = Color(0xFF14251E),
    onBackground = Color(0xFFE6F4EC),
    onSurface = Color(0xFFE6F4EC),
    outline = Color(0xFF3F5D4F),
    error = Color(0xFFFFB4AB),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C46),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA2F2C5),
    onPrimaryContainer = Color(0xFF002114),
    background = Color(0xFFF4FBF6),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE1EEE6),
    onBackground = Color(0xFF17201B),
    onSurface = Color(0xFF17201B),
    outline = Color(0xFF66766C),
)

@Composable
fun CarGpsTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
