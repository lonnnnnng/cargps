package com.cargps.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val WarningAmber = Color(0xFFFFBA52)

@Composable
fun DashboardScreen(
    state: DashboardState,
    onRequestPermission: () -> Unit,
    onToggleTrip: () -> Unit,
    onEndTrip: () -> Unit,
    onToggleTheme: () -> Unit,
    onTick: (Long) -> Unit,
) {
    var showEndConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            onTick(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            CockpitHeader(state = state, onToggleTheme = onToggleTheme)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SpeedGauge(state = state, modifier = Modifier.weight(1f).fillMaxHeight())
                TripConsole(state = state, modifier = Modifier.width(330.dp).fillMaxHeight())
            }
            Spacer(Modifier.height(8.dp))
            ControlDock(
                state = state,
                onRequestPermission = onRequestPermission,
                onToggleTrip = onToggleTrip,
                onRequestEnd = { showEndConfirmation = true },
            )
        }
    }

    if (showEndConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndConfirmation = false },
            title = { Text("结束当前行程？") },
            text = { Text("当前统计会保留在仪表上。行程历史持久化将在下一阶段加入。") },
            confirmButton = {
                Button(
                    onClick = {
                        showEndConfirmation = false
                        onEndTrip()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("确认结束")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmation = false }) {
                    Text("继续记录")
                }
            },
        )
    }
}

@Composable
private fun CockpitHeader(state: DashboardState, onToggleTheme: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(128.dp)) {
            Text(
                text = "CAR GPS",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.2.sp,
            )
            Text(
                text = tripStatusText(state.tripMode),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                fontSize = 12.sp,
            )
        }

        StatusLine(state.fixStatus)
        HeaderSeparator()
        TechnicalLabel("卫星", satelliteText(state))
        HeaderSeparator()
        TechnicalLabel("精度", state.accuracyMeters?.let { "±${it.toInt()} m" } ?: "--")
        HeaderSeparator()
        TechnicalLabel("HDOP", state.hdop?.let { String.format(Locale.US, "%.1f", it) } ?: "--")

        Spacer(Modifier.weight(1f))
        Text(
            text = "${DashboardViewModel.formatDate(state.nowMillis)} ${DashboardViewModel.formatTime(state.nowMillis)}",
            fontSize = 21.sp,
            lineHeight = 24.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onToggleTheme, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = if (state.darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (state.darkTheme) "切换日间模式" else "切换夜间模式",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
            )
        }
    }
}

@Composable
private fun StatusLine(status: FixStatus) {
    val (text, color) = fixStatusPresentation(status)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(99.dp)))
        Spacer(Modifier.width(7.dp))
        Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeaderSeparator() {
    Box(
        Modifier.padding(horizontal = 12.dp)
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
    )
}

@Composable
private fun TechnicalLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f))
        Text(value, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SpeedGauge(state: DashboardState, modifier: Modifier = Modifier) {
    val speed = state.speedKmh
    val animatedProgress by animateFloatAsState(
        targetValue = ((speed ?: 0.0) / 200.0).coerceIn(0.0, 1.0).toFloat(),
        label = "speed-gauge",
    )
    val (_, statusColor) = fixStatusPresentation(state.fixStatus)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GaugeArc(progress = animatedProgress, accent = statusColor)

            Column(
                modifier = Modifier.align(Alignment.Center).padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = speed?.let { String.format(Locale.US, "%.0f", it) } ?: "--",
                    fontSize = 156.sp,
                    lineHeight = 150.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    color = when (state.fixStatus) {
                        FixStatus.FIXED -> MaterialTheme.colorScheme.primary
                        FixStatus.STALE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    "km/h",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                )
            }

            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("实时车速", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f))
            }

            Row(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GaugeDetail(
                    label = "航向",
                    value = state.bearingDegrees?.let { "${it.toInt()}°  ${bearingName(it)}" } ?: "--",
                )
                GaugeDetail(
                    label = "定位更新",
                    value = state.lastFixAtMillis?.let {
                        String.format(Locale.US, "%.1f s", (state.nowMillis - it).coerceAtLeast(0L) / 1_000.0)
                    } ?: "等待中",
                    alignEnd = true,
                )
            }
        }
    }
}

@Composable
private fun GaugeArc(progress: Float, accent: Color) {
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
    val tickColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 54.dp, vertical = 22.dp)) {
        val startAngle = 150f
        val sweepAngle = 240f
        val radius = min(size.width * 0.38f, size.height * 0.46f)
        val center = Offset(size.width / 2f, size.height * 0.53f)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)

        drawArc(
            color = trackColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
        )
        if (progress > 0f) {
            drawArc(
                color = accent,
                startAngle = startAngle,
                sweepAngle = sweepAngle * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        repeat(25) { index ->
            val angle = startAngle + sweepAngle * index / 24f
            val radians = angle / 180f * PI
            val major = index % 4 == 0
            val outerRadius = radius + 18.dp.toPx()
            val innerRadius = outerRadius - if (major) 13.dp.toPx() else 7.dp.toPx()
            drawLine(
                color = tickColor.copy(alpha = if (major) 0.42f else 0.18f),
                start = Offset(
                    center.x + cos(radians).toFloat() * innerRadius,
                    center.y + sin(radians).toFloat() * innerRadius,
                ),
                end = Offset(
                    center.x + cos(radians).toFloat() * outerRadius,
                    center.y + sin(radians).toFloat() * outerRadius,
                ),
                strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun GaugeDetail(label: String, value: String, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f))
        Text(value, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TripConsole(state: DashboardState, modifier: Modifier = Modifier) {
    val stats = state.tripStats
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("当前行程", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f))
                    Text(
                        tripStatusText(state.tripMode),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = tripStatusColor(state.tripMode),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    DashboardViewModel.formatDuration(stats.elapsedMillis),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(10.dp))
            Text("里程", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.44f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    distanceNumber(stats.distanceMeters),
                    fontSize = 48.sp,
                    lineHeight = 50.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    distanceUnit(stats.distanceMeters),
                    fontSize = 17.sp,
                    modifier = Modifier.padding(bottom = 7.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
            )

            MetricPair(
                leftLabel = "行程平均",
                leftValue = "${DashboardViewModel.formatSpeed(stats.tripAverageMps)} km/h",
                rightLabel = "移动平均",
                rightValue = "${DashboardViewModel.formatSpeed(stats.movingAverageMps)} km/h",
            )
            Spacer(Modifier.height(14.dp))
            MetricPair(
                leftLabel = "最高速度",
                leftValue = "${DashboardViewModel.formatSpeed(stats.maxSpeedMps)} km/h",
                rightLabel = "海拔",
                rightValue = state.altitudeMeters?.let { "${it.toInt()} m" } ?: "--",
            )
            Spacer(Modifier.weight(1f))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.48f))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "移动 ${DashboardViewModel.formatDuration(stats.movingMillis)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "停车 ${DashboardViewModel.formatDuration(stats.stoppedMillis)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                )
            }
        }
    }
}

@Composable
private fun MetricPair(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
) {
    Row(Modifier.fillMaxWidth()) {
        ConsoleMetric(leftLabel, leftValue, Modifier.weight(1f))
        Box(
            Modifier.padding(horizontal = 12.dp)
                .width(1.dp)
                .height(38.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.52f)),
        )
        ConsoleMetric(rightLabel, rightValue, Modifier.weight(1f))
    }
}

@Composable
private fun ConsoleMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.44f))
        Text(value, fontSize = 19.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ControlDock(
    state: DashboardState,
    onRequestPermission: () -> Unit,
    onToggleTrip: () -> Unit,
    onRequestEnd: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(68.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coordinateText(state),
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "NMEA ${state.lastNmeaType ?: "--"}  ·  PDOP ${state.pdop?.let { String.format(Locale.US, "%.1f", it) } ?: "--"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.46f),
                )
            }

            if (state.fixStatus == FixStatus.PERMISSION_REQUIRED) {
                Button(onClick = onRequestPermission, modifier = Modifier.height(50.dp).width(156.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("授权定位", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onToggleTrip,
                    modifier = Modifier.height(50.dp).width(156.dp),
                ) {
                    Icon(
                        imageVector = if (state.tripMode == TripMode.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        when (state.tripMode) {
                            TripMode.IDLE -> "开始行程"
                            TripMode.RECORDING -> "暂停行程"
                            TripMode.PAUSED -> "继续行程"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onRequestEnd,
                    enabled = state.tripMode != TripMode.IDLE,
                    modifier = Modifier.height(50.dp).width(116.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("结束", fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun fixStatusPresentation(status: FixStatus): Pair<String, Color> = when (status) {
    FixStatus.PERMISSION_REQUIRED -> "需要定位权限" to MaterialTheme.colorScheme.error
    FixStatus.LOCATION_DISABLED -> "系统定位关闭" to MaterialTheme.colorScheme.error
    FixStatus.SEARCHING -> "正在搜星" to WarningAmber
    FixStatus.FIXED -> "定位正常" to MaterialTheme.colorScheme.primary
    FixStatus.POOR_ACCURACY -> "精度较差" to WarningAmber
    FixStatus.STALE -> "数据过期" to WarningAmber
    FixStatus.LOST -> "定位丢失" to MaterialTheme.colorScheme.error
}

@Composable
private fun tripStatusColor(mode: TripMode): Color = when (mode) {
    TripMode.IDLE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
    TripMode.RECORDING -> MaterialTheme.colorScheme.primary
    TripMode.PAUSED -> WarningAmber
}

private fun tripStatusText(mode: TripMode): String = when (mode) {
    TripMode.IDLE -> "等待开始"
    TripMode.RECORDING -> "记录中"
    TripMode.PAUSED -> "已暂停"
}

private fun satelliteText(state: DashboardState): String = when {
    state.satellitesUsed == null && state.satellitesInView == null -> "--"
    else -> "${state.satellitesUsed ?: "-"}/${state.satellitesInView ?: "-"} 星"
}

private fun coordinateText(state: DashboardState): String =
    if (state.latitude != null && state.longitude != null) {
        String.format(Locale.US, "%.6f, %.6f", state.latitude, state.longitude)
    } else {
        "等待经纬度"
    }

private fun distanceNumber(meters: Double): String =
    if (meters < 1_000.0) meters.toInt().toString() else String.format(Locale.US, "%.2f", meters / 1_000.0)

private fun distanceUnit(meters: Double): String = if (meters < 1_000.0) "m" else "km"

private fun bearingName(degrees: Float): String {
    val directions = listOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")
    return directions[((degrees + 22.5f) / 45f).toInt() % directions.size]
}
