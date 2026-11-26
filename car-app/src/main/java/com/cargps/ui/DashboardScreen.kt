package com.cargps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargps.DashboardState
import com.cargps.DashboardViewModel
import com.cargps.FixStatus
import com.cargps.TripMode
import java.util.Locale

@Composable
fun DashboardScreen(
    state: DashboardState,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onToggleTrip: () -> Unit,
    onEndTrip: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    var showEndConfirmation by rememberSaveable { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }


    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            val compact = maxHeight < 460.dp || LocalDensity.current.fontScale > 1.15f
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CockpitHeader(state, onToggleTheme)
                // 作者：long｜系统栏由 Activity 窗口预留；这里只分配实际剩余高度，避免旧版固定顶栏压住文字。
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    SpeedReadout(state, Modifier.weight(1.05f).fillMaxHeight())
                    Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.outline))
                    ClockReadout(state.nowMillis, Modifier.weight(1f).fillMaxHeight())
                }
                TripStrip(state, compact)
                ControlDock(
                    state = state,
                    onRequestPermission = onRequestPermission,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onToggleTrip = onToggleTrip,
                    onRequestEnd = { showEndConfirmation = true },
                    onDetails = { showDetails = true },
                )
            }
        }
    }

    if (showEndConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndConfirmation = false },
            title = { Text("结束当前行程？") },
            text = { Text("本次里程、时间和轨迹将保存到本机，统计结果继续保留在仪表上。") },
            confirmButton = {
                TextButton(
                    modifier = Modifier.heightIn(min = 56.dp),
                    onClick = { showEndConfirmation = false; onEndTrip() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("确认结束") }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.heightIn(min = 56.dp),
                    onClick = { showEndConfirmation = false },
                ) { Text("返回仪表") }
            },
        )
    }
    if (showDetails) {
        LocationDetails(state, onDismiss = { showDetails = false })
    }
}

@Composable
private fun CockpitHeader(state: DashboardState, onToggleTheme: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("header"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("CarGPS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(20.dp))
        val (status, color) = fixStatusPresentation(state.fixStatus)
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(50)))
        Spacer(Modifier.width(8.dp))
        Text(status, color = color, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text(
            if (state.restoredTrip && state.tripMode != TripMode.IDLE) {
                "已恢复 · ${tripStatusText(state.tripMode)}"
            } else tripStatusText(state.tripMode),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = onToggleTheme, modifier = Modifier.size(56.dp)) {
            Icon(
                if (state.darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (state.darkTheme) "切换日间模式" else "切换夜间模式",
            )
        }
    }
}

@Composable
private fun SpeedReadout(state: DashboardState, modifier: Modifier) {
    Column(modifier.padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("瞬时速度", color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text("km/h", color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium)
        }
        FittedText(
            text = state.speedKmh?.let { String.format(Locale.US, "%.0f", it) } ?: "--",
            maxSize = 176.sp,
            color = if (state.fixStatus == FixStatus.STALE) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("speed"),
            weight = FontWeight.Bold,
        )
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.bearingDegrees?.let { "${bearingName(it)}  ${it.toInt()}°" } ?: "航向 --",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(state.altitudeMeters?.let { "海拔 ${it.toInt()} m" } ?: "海拔 --",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ClockReadout(nowMillis: Long, modifier: Modifier) {
    Column(
        modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 作者：long｜日期与秒级时钟独立排版并使用不同语义色，驾驶员先读时间，再读取日期。
        FittedText(
            DashboardViewModel.formatDateChinese(nowMillis),
            maxSize = 26.sp,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.fillMaxWidth().weight(0.7f).testTag("date"),
            weight = FontWeight.Medium,
        )
        FittedText(
            DashboardViewModel.formatTime(nowMillis),
            maxSize = 76.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().weight(1.6f).testTag("time"),
            weight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TripStrip(state: DashboardState, compact: Boolean) {
    val stats = state.tripStats
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TripMetric("本次里程", distanceText(stats.distanceMeters),
                "移动 ${DashboardViewModel.formatDuration(stats.movingMillis)}", Modifier.weight(1f))
            TripMetric("行程时长", DashboardViewModel.formatDuration(stats.elapsedMillis),
                "停车 ${DashboardViewModel.formatDuration(stats.stoppedMillis)}", Modifier.weight(1f))
            TripMetric("行程平均速度", "${DashboardViewModel.formatSpeed(stats.tripAverageMps)} km/h",
                "移动平均速度 ${DashboardViewModel.formatSpeed(stats.movingAverageMps)} km/h", Modifier.weight(1f))
            TripMetric("最高速度", "${DashboardViewModel.formatSpeed(stats.maxSpeedMps)} km/h",
                "仅前台记录", Modifier.weight(1f))
        }
    }
}

@Composable
private fun TripMetric(label: String, value: String, detail: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FittedText(value, 24.sp, MaterialTheme.colorScheme.onSurface,
            Modifier.fillMaxWidth().height(36.dp), alignment = Alignment.CenterStart)
        Text(detail, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun ControlDock(
    state: DashboardState,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onToggleTrip: () -> Unit,
    onRequestEnd: () -> Unit,
    onDetails: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("controls"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(coordinateText(state), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(fixAgeText(state), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        FilledTonalButton(onClick = onDetails, modifier = Modifier.heightIn(min = 56.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("定位详情")
        }
        val primaryAction = when (state.fixStatus) {
            FixStatus.PERMISSION_REQUIRED -> onRequestPermission
            FixStatus.LOCATION_DISABLED -> onOpenLocationSettings
            else -> onToggleTrip
        }
        val primaryLabel = when (state.fixStatus) {
            FixStatus.PERMISSION_REQUIRED -> "授权定位"
            FixStatus.LOCATION_DISABLED -> "开启定位"
            else -> when (state.tripMode) {
                TripMode.IDLE -> "开始行程"
                TripMode.RECORDING -> "暂停行程"
                TripMode.PAUSED -> "继续行程"
            }
        }
        Button(onClick = primaryAction, modifier = Modifier.heightIn(min = 56.dp)) {
            Icon(if (state.tripMode == TripMode.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null, Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(primaryLabel)
        }
        OutlinedButton(
            onClick = onRequestEnd,
            enabled = state.tripMode != TripMode.IDLE,
            modifier = Modifier.heightIn(min = 56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Default.Stop, contentDescription = null, Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text("结束")
        }
    }
}

@Composable
private fun LocationDetails(state: DashboardState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("定位详情") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${fixStatusPresentation(state.fixStatus).first} · ${fixAgeText(state)}")
                Text(coordinateText(state))
                HorizontalDivider()
                Text("卫星：参与定位 / 可见", fontWeight = FontWeight.Bold)
                Text("GPS    ${satellitePair(state.gpsSatellitesUsed, state.gpsSatellitesInView)}")
                Text("北斗    ${satellitePair(state.beidouSatellitesUsed, state.beidouSatellitesInView)}")
                Text("全部星座    ${satellitePair(state.satellitesUsed, state.satellitesInView)}")
                HorizontalDivider()
                Text(state.accuracyMeters?.let { "水平精度 ±${it.toInt()} m" } ?: "水平精度未提供")
                Text("HDOP ${decimal(state.hdop)}    PDOP ${decimal(state.pdop)}    VDOP ${decimal(state.vdop)}")
                Text("最近 NMEA 报文：${state.lastNmeaType?.name ?: "未提供或已过期"}")
                Text("-- 表示设备未提供或数据已过期；0 表示本次回调没有对应卫星。")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 56.dp)) { Text("返回仪表") }
        },
    )
}

@Composable
private fun FittedText(
    text: String,
    maxSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.SemiBold,
    alignment: Alignment = Alignment.Center,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier, contentAlignment = alignment) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
        val heightPx = with(density) { maxHeight.roundToPx().coerceAtLeast(1) }
        val baseStyle = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = weight,
            fontFeatureSettings = "tnum",
            letterSpacing = 0.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )
        // 作者：long｜固定横屏中的数字必须完整可读；使用真实字形测量，兼容车机字体和系统字号放大。
        // 作者：long｜等宽数字用统一样本测量，秒数变化可复用字号；单位或中文变化仍会重新测量。
        val measurementText = text.map { if (it in '0'..'9') '8' else it }.joinToString("")
        val fittedSize = remember(measurementText, widthPx, heightPx, maxSize, weight, density) {
            var low = 1f
            var high = maxSize.value
            repeat(10) {
                val candidate = (low + high) / 2f
                val result = measurer.measure(
                    measurementText,
                    style = baseStyle.copy(fontSize = candidate.sp, lineHeight = (candidate * 1.12f).sp),
                    softWrap = false,
                    maxLines = 1,
                    constraints = Constraints(),
                )
                if (result.size.width <= widthPx && result.size.height <= heightPx) low = candidate else high = candidate
            }
            low.sp
        }
        Text(
            text = text,
            color = color,
            style = baseStyle.copy(fontSize = fittedSize, lineHeight = (fittedSize.value * 1.12f).sp),
            maxLines = 1,
            softWrap = false,
            textAlign = if (alignment == Alignment.CenterStart) TextAlign.Start else TextAlign.Center,
        )
    }
}

@Composable
private fun fixStatusPresentation(status: FixStatus): Pair<String, Color> = when (status) {
    FixStatus.PERMISSION_REQUIRED -> "需要定位权限" to MaterialTheme.colorScheme.error
    FixStatus.LOCATION_DISABLED -> "系统定位关闭" to MaterialTheme.colorScheme.error
    FixStatus.SEARCHING -> "正在搜星" to MaterialTheme.colorScheme.tertiary
    FixStatus.FIXED -> "定位正常" to MaterialTheme.colorScheme.primary
    FixStatus.POOR_ACCURACY -> "精度较差" to MaterialTheme.colorScheme.tertiary
    FixStatus.STALE -> "定位数据过期" to MaterialTheme.colorScheme.tertiary
    FixStatus.LOST -> "定位丢失" to MaterialTheme.colorScheme.error
}

private fun tripStatusText(mode: TripMode): String = when (mode) {
    TripMode.IDLE -> "未开始行程"
    TripMode.RECORDING -> "行程记录中"
    TripMode.PAUSED -> "行程已暂停"
}

private fun satellitePair(used: Int?, visible: Int?): String = "${used ?: "--"} / ${visible ?: "--"}"
private fun decimal(value: Double?): String = value?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
private fun coordinateText(state: DashboardState): String =
    if (state.latitude != null && state.longitude != null) {
        String.format(Locale.US, "%.6f, %.6f", state.latitude, state.longitude)
    } else "等待经纬度"

private fun fixAgeText(state: DashboardState): String = state.lastFixAtMillis?.let {
    "定位更新 ${(state.nowMillis - it).coerceAtLeast(0L) / 1_000L} 秒前"
} ?: "等待有效定位"

private fun distanceText(meters: Double): String = if (meters < 1_000.0) "${meters.toInt()} m"
    else String.format(Locale.US, "%.2f km", meters / 1_000.0)

private fun bearingName(degrees: Float): String {
    val directions = listOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")
    return directions[(((degrees % 360f + 360f) % 360f + 22.5f) / 45f).toInt() % directions.size]
}
