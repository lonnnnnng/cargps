package com.lonng.cargps.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4ED8CE),
    onPrimary = Color(0xFF003735),
    primaryContainer = Color(0xFF124B49),
    onPrimaryContainer = Color(0xFFB3F2EB),
    secondary = Color(0xFF9BBAB7),
    secondaryContainer = Color(0xFF2A4248),
    onSecondaryContainer = Color(0xFFD0E9E4),
    tertiary = Color(0xFFFFC15A),
    onTertiary = Color(0xFF402C00),
    background = Color(0xFF0B151A),
    surface = Color(0xFF132229),
    surfaceVariant = Color(0xFF1B2D34),
    onBackground = Color(0xFFF2F6F3),
    onSurface = Color(0xFFF2F6F3),
    onSurfaceVariant = Color(0xFFBACBC7),
    outline = Color(0xFF52696B),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5D0008),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A66),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F0EA),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF3D6562),
    secondaryContainer = Color(0xFFCDE3DE),
    onSecondaryContainer = Color(0xFF163B38),
    tertiary = Color(0xFF795600),
    onTertiary = Color.White,
    background = Color(0xFFEAF0ED),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDDE7E3),
    onBackground = Color(0xFF142525),
    onSurface = Color(0xFF142525),
    onSurfaceVariant = Color(0xFF344C4B),
    outline = Color(0xFF738985),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

@Composable
fun GpsTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
