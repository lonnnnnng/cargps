package com.cargps.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF36E69A),
    onPrimary = Color(0xFF001E12),
    primaryContainer = Color(0xFF103C2B),
    onPrimaryContainer = Color(0xFFB8F8D7),
    secondary = Color(0xFF8EA79C),
    background = Color(0xFF030806),
    surface = Color(0xFF09110E),
    surfaceVariant = Color(0xFF0E1915),
    onBackground = Color(0xFFEAF6F0),
    onSurface = Color(0xFFEAF6F0),
    outline = Color(0xFF263A31),
    error = Color(0xFFFF736A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF007B4E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB5F3D1),
    onPrimaryContainer = Color(0xFF002114),
    background = Color(0xFFEDF4F0),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3ECE7),
    onBackground = Color(0xFF17201B),
    onSurface = Color(0xFF17201B),
    outline = Color(0xFFB6C6BD),
    error = Color(0xFFB3261E),
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
