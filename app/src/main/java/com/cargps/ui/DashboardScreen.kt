package com.cargps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargps.DashboardState
import com.cargps.DashboardViewModel
import com.cargps.FixStatus
import com.cargps.TripMode
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun DashboardScreen(
    state: DashboardState,
    onRequestPermission: () -> Unit,
    onToggleTrip: () -> Unit,
    onEndTrip: () -> Unit,
    onToggleTheme: () -> Unit,
    onTick: (Long) -> Unit,
) {
    LaunchedEffect(Unit) {
        while (true) {
            onTick(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Header(state = state, onToggleTheme = onToggleTheme)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SpeedPanel(state = state, modifier = Modifier.weight(1f).fillMaxHeight())
                StatsPanel(state = state, modifier = Modifier.width(360.dp).fillMaxHeight())
            }
            Spacer(Modifier.height(8.dp))
            Footer(
                state = state,
                onRequestPermission = onRequestPermission,
                onToggleTrip = onToggleTrip,
                onEndTrip = onEndTrip,
            )
        }
    }
}

@Composable
private fun Header(state: DashboardState, onToggleTheme: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusBadge(state.fixStatus)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${state.satellitesUsed}/${state.satellitesInView} 星",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            fontSize = 17.sp,
        )
        Text(
            text = state.accuracyMeters?.let { "  ·  ±${it.toInt()} m" } ?: "  ·  精度 --",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            fontSize = 17.sp,
        )
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(DashboardViewModel.formatTime(state.nowMillis), fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(DashboardViewModel.formatDate(state.nowMillis), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
        }
        IconButton(onClick = onToggleTheme, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = if (state.darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (state.darkTheme) "切换日间模式" else "切换夜间模式",
            )
        }
    }
}

@Composable
private fun StatusBadge(status: FixStatus) {
    val (text, color) = when (status) {
        FixStatus.PERMISSION_REQUIRED -> "需要定位权限" to MaterialTheme.colorScheme.error
        FixStatus.LOCATION_DISABLED -> "系统定位已关闭" to MaterialTheme.colorScheme.error
        FixStatus.SEARCHING -> "正在搜星" to Color(0xFFFFC857)
        FixStatus.FIXED -> "GPS 已定位" to MaterialTheme.colorScheme.primary
        FixStatus.POOR_ACCURACY -> "定位精度较差" to Color(0xFFFFC857)
        FixStatus.STALE -> "定位数据过期" to Color(0xFFFFC857)
        FixStatus.LOST -> "定位已丢失" to MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier.background(color.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).background(color, RoundedCornerShape(99.dp)))
        Spacer(Modifier.width(8.dp))
        Text(text, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SpeedPanel(state: DashboardState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = state.speedKmh?.let { String.format(Locale.US, "%.0f", it) } ?: "--",
                fontSize = 148.sp,
                lineHeight = 150.sp,
                fontWeight = FontWeight.Black,
                color = if (state.fixStatus == FixStatus.FIXED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text("km/h", fontSize = 25.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    append(state.bearingDegrees?.let { "航向 ${it.toInt()}°" } ?: "航向 --")
                    append("   ·   ")
                    append(state.lastFixAtMillis?.let { "更新 ${(state.nowMillis - it).coerceAtLeast(0L) / 1_000.0}s" } ?: "等待定位")
                },
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            )
        }
    }
}

@Composable
private fun StatsPanel(state: DashboardState, modifier: Modifier = Modifier) {
    val stats = state.tripStats
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("本次里程", formatDistance(stats.distanceMeters), Modifier.weight(1f))
            MetricCard("行程时间", DashboardViewModel.formatDuration(stats.elapsedMillis), Modifier.weight(1f))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("行程平均", "${DashboardViewModel.formatSpeed(stats.tripAverageMps)} km/h", Modifier.weight(1f))
            MetricCard("移动平均", "${DashboardViewModel.formatSpeed(stats.movingAverageMps)} km/h", Modifier.weight(1f))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("最高速度", "${DashboardViewModel.formatSpeed(stats.maxSpeedMps)} km/h", Modifier.weight(1f))
            MetricCard("海拔", state.altitudeMeters?.let { "${it.toInt()} m" } ?: "--", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
            Spacer(Modifier.height(3.dp))
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun Footer(
    state: DashboardState,
    onRequestPermission: () -> Unit,
    onToggleTrip: () -> Unit,
    onEndTrip: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (state.latitude != null && state.longitude != null) {
                    String.format(Locale.US, "%.6f, %.6f", state.latitude, state.longitude)
                } else {
                    "等待经纬度"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "HDOP ${state.hdop?.let { String.format(Locale.US, "%.1f", it) } ?: "--"}  ·  NMEA ${state.lastNmeaType ?: "--"}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            )
        }

        if (state.fixStatus == FixStatus.PERMISSION_REQUIRED) {
            Button(onClick = onRequestPermission, modifier = Modifier.height(52.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("授权定位", fontSize = 17.sp)
            }
        } else {
            Button(
                onClick = onToggleTrip,
                modifier = Modifier.height(52.dp).width(150.dp),
            ) {
                Icon(
                    imageVector = if (state.tripMode == TripMode.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when (state.tripMode) {
                        TripMode.IDLE -> "开始行程"
                        TripMode.RECORDING -> "暂停"
                        TripMode.PAUSED -> "继续"
                    },
                    fontSize = 17.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onEndTrip,
                enabled = state.tripMode != TripMode.IDLE,
                modifier = Modifier.height(52.dp).width(112.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("结束", fontSize = 17.sp)
            }
        }
    }
}

private fun formatDistance(meters: Double): String =
    if (meters < 1_000.0) "${meters.toInt()} m" else String.format(Locale.US, "%.2f km", meters / 1_000.0)
