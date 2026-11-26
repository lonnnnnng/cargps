package com.cargps

import android.location.Location
import androidx.lifecycle.ViewModel
import com.cargps.domain.LocationQualityGate
import com.cargps.domain.LocationSample
import com.cargps.domain.NmeaFrame
import com.cargps.domain.NmeaSentenceType
import com.cargps.domain.SpeedEstimator
import com.cargps.domain.TripPoint
import com.cargps.domain.TripStatsAccumulator
import com.cargps.domain.TripStats
import com.cargps.storage.CompletedTripRecord
import com.cargps.storage.NoOpTripStorage
import com.cargps.storage.TripStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class FixStatus {
    PERMISSION_REQUIRED,
    LOCATION_DISABLED,
    SEARCHING,
    FIXED,
    POOR_ACCURACY,
    STALE,
    LOST,
}

enum class TripMode {
    IDLE,
    RECORDING,
    PAUSED,
}

data class DashboardState(
    val nowMillis: Long = System.currentTimeMillis(),
    val fixStatus: FixStatus = FixStatus.SEARCHING,
    val speedKmh: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeMeters: Double? = null,
    val bearingDegrees: Float? = null,
    val accuracyMeters: Float? = null,
    val lastFixAtMillis: Long? = null,
    val satellitesInView: Int? = null,
    val satellitesUsed: Int? = null,
    val gpsSatellitesInView: Int? = null,
    val gpsSatellitesUsed: Int? = null,
    val beidouSatellitesInView: Int? = null,
    val beidouSatellitesUsed: Int? = null,
    val hdop: Double? = null,
    val pdop: Double? = null,
    val vdop: Double? = null,
    val lastNmeaType: NmeaSentenceType? = null,
    val tripMode: TripMode = TripMode.IDLE,
    val tripStats: TripStats = EMPTY_TRIP_STATS,
    val recentTrips: List<CompletedTripRecord> = emptyList(),
    val restoredTrip: Boolean = false,
    val darkTheme: Boolean = true,
)

class DashboardViewModel(
    private val storage: TripStorage = NoOpTripStorage,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val qualityGate = LocationQualityGate()
    private val speedEstimator = SpeedEstimator()
    private val tripStatsAccumulator = TripStatsAccumulator()
    private var previousSample: LocationSample? = null
    private var previousAndroidLocation: Location? = null
    private var tripStartedAtMillis: Long? = null
    private var pausedAtMillis: Long? = null
    private var totalPausedMillis: Long = 0L
    private var segmentStartPending = false
    private var lastNmeaAtMillis: Long? = null
    private var lastHdopAtMillis: Long? = null
    private var lastPdopAtMillis: Long? = null
    private var lastVdopAtMillis: Long? = null
    private var lastSatellitesInViewAtMillis: Long? = null
    private var lastSatellitesUsedAtMillis: Long? = null
    private var lastGnssStatusAtMillis: Long? = null

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        restorePersistedState()
    }

    fun onPermissionRequired() = update { copy(fixStatus = FixStatus.PERMISSION_REQUIRED, speedKmh = null) }

    fun onProviderDisabled() = update { copy(fixStatus = FixStatus.LOCATION_DISABLED, speedKmh = null) }

    fun onSearching() = update {
        if (fixStatus == FixStatus.FIXED || fixStatus == FixStatus.POOR_ACCURACY) this
        else copy(fixStatus = FixStatus.SEARCHING)
    }

    fun onLocation(location: Location) {
        val sample = LocationSample(
            timestampMillis = location.time,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy.takeIf { location.hasAccuracy() },
            altitudeMeters = location.altitude.takeIf { location.hasAltitude() },
            bearingDegrees = location.bearing.takeIf { location.hasBearing() },
            speedMps = location.speed.takeIf { location.hasSpeed() },
        )
        val decision = qualityGate.evaluate(previousSample, sample)
        if (!decision.displayable) return

        val distanceMeters = if (decision.connectsToPrevious && previousAndroidLocation != null) {
            previousAndroidLocation?.distanceTo(location)?.toDouble() ?: 0.0
        } else {
            0.0
        }
        val timeDeltaSeconds = previousSample?.let {
            (sample.timestampMillis - it.timestampMillis) / 1_000.0
        } ?: 0.0
        val derivedSpeedMps = if (timeDeltaSeconds > 0.0 && decision.connectsToPrevious) {
            distanceMeters / timeDeltaSeconds
        } else {
            null
        }
        val reading = speedEstimator.update(
            speedMps = sample.speedMps?.toDouble() ?: derivedSpeedMps,
            timestampMillis = sample.timestampMillis,
        )
        val statsSpeedMps = maxOf(reading.smoothedMps, derivedSpeedMps ?: 0.0)

        if (_state.value.tripMode == TripMode.RECORDING) {
            val startsNewSegment = segmentStartPending ||
                (previousSample != null && !decision.connectsToPrevious)
            if (startsNewSegment) {
                // 作者：long｜定位恢复后的首点单独起段，阻止暂停或失锁间隔进入移动时长。
                tripStatsAccumulator.closingPoints(sample.timestampMillis).forEach(::appendTripPoint)
                appendTripPoint(TripPoint(sample.timestampMillis, 0.0, 0.0, false))
                segmentStartPending = false
            }
            val point = TripPoint(
                timestampMillis = sample.timestampMillis,
                speedMps = statsSpeedMps,
                distanceFromPreviousMeters = distanceMeters,
                moving = statsSpeedMps >= MOVING_THRESHOLD_MPS,
            )
            appendTripPoint(point)
        }

        previousSample = sample
        previousAndroidLocation = Location(location)
        update {
            copy(
                fixStatus = if ((sample.accuracyMeters ?: Float.MAX_VALUE) <= 30f) {
                    FixStatus.FIXED
                } else {
                    FixStatus.POOR_ACCURACY
                },
                speedKmh = reading.smoothedMps * 3.6,
                latitude = sample.latitude,
                longitude = sample.longitude,
                altitudeMeters = sample.altitudeMeters,
                bearingDegrees = sample.bearingDegrees,
                accuracyMeters = sample.accuracyMeters,
                lastFixAtMillis = sample.timestampMillis,
                tripStats = calculateTripStats(sample.timestampMillis),
            )
        }
    }

    fun onSatellitesChanged(
        inView: Int,
        usedInFix: Int,
        gpsInView: Int,
        gpsUsedInFix: Int,
        beidouInView: Int,
        beidouUsedInFix: Int,
    ) {
        val receivedAtMillis = nowProvider()
        lastSatellitesInViewAtMillis = receivedAtMillis
        lastSatellitesUsedAtMillis = receivedAtMillis
        lastGnssStatusAtMillis = receivedAtMillis
        update {
            copy(
                satellitesInView = inView,
                satellitesUsed = usedInFix,
                gpsSatellitesInView = gpsInView,
                gpsSatellitesUsed = gpsUsedInFix,
                beidouSatellitesInView = beidouInView,
                beidouSatellitesUsed = beidouUsedInFix,
            )
        }
    }

    fun onNmea(frame: NmeaFrame) {
        val receivedAtMillis = nowProvider()
        val gnssCountsAreFresh = lastGnssStatusAtMillis.isFreshAt(
            receivedAtMillis,
            SATELLITE_STATS_STALE_AFTER_MILLIS,
        )
        lastNmeaAtMillis = receivedAtMillis
        // 作者：long｜不同语句只补充部分精度因子，各字段独立过期，防止 GGA 续期旧的 GSA 数据。
        if (frame.hdop != null) lastHdopAtMillis = receivedAtMillis
        if (frame.pdop != null) lastPdopAtMillis = receivedAtMillis
        if (frame.vdop != null) lastVdopAtMillis = receivedAtMillis
        if (!gnssCountsAreFresh) {
            if (frame.satellitesInView != null) lastSatellitesInViewAtMillis = receivedAtMillis
            if (frame.satellitesUsed != null) lastSatellitesUsedAtMillis = receivedAtMillis
        }
        update {
            copy(
                lastNmeaType = frame.type,
                hdop = frame.hdop ?: hdop,
                pdop = frame.pdop ?: pdop,
                vdop = frame.vdop ?: vdop,
                satellitesInView = if (gnssCountsAreFresh) satellitesInView else {
                    frame.satellitesInView ?: satellitesInView
                },
                satellitesUsed = if (gnssCountsAreFresh) satellitesUsed else {
                    frame.satellitesUsed ?: satellitesUsed
                },
            )
        }
    }

    fun onTick(nowMillis: Long) {
        val nmeaIsFresh = lastNmeaAtMillis.isFreshAt(nowMillis, NMEA_STALE_AFTER_MILLIS)
        val hdopIsFresh = lastHdopAtMillis.isFreshAt(nowMillis, NMEA_STALE_AFTER_MILLIS)
        val pdopIsFresh = lastPdopAtMillis.isFreshAt(nowMillis, NMEA_STALE_AFTER_MILLIS)
        val vdopIsFresh = lastVdopAtMillis.isFreshAt(nowMillis, NMEA_STALE_AFTER_MILLIS)
        val satellitesInViewAreFresh = lastSatellitesInViewAtMillis.isFreshAt(
            nowMillis,
            SATELLITE_STATS_STALE_AFTER_MILLIS,
        )
        val satellitesUsedAreFresh = lastSatellitesUsedAtMillis.isFreshAt(
            nowMillis,
            SATELLITE_STATS_STALE_AFTER_MILLIS,
        )
        val gnssStatusIsFresh = lastGnssStatusAtMillis.isFreshAt(
            nowMillis,
            SATELLITE_STATS_STALE_AFTER_MILLIS,
        )
        update {
            val ageMillis = lastFixAtMillis?.let(nowMillis::minus)
            val currentStatus = when {
                fixStatus == FixStatus.PERMISSION_REQUIRED || fixStatus == FixStatus.LOCATION_DISABLED -> fixStatus
                ageMillis == null -> FixStatus.SEARCHING
                ageMillis > 10_000L -> FixStatus.LOST
                ageMillis > 3_000L -> FixStatus.STALE
                else -> fixStatus
            }
            copy(
                nowMillis = nowMillis,
                fixStatus = currentStatus,
                speedKmh = when (currentStatus) {
                    FixStatus.STALE -> 0.0
                    FixStatus.LOST -> null
                    else -> speedKmh
                },
                lastNmeaType = if (nmeaIsFresh) lastNmeaType else null,
                hdop = if (hdopIsFresh) hdop else null,
                pdop = if (pdopIsFresh) pdop else null,
                vdop = if (vdopIsFresh) vdop else null,
                satellitesInView = if (satellitesInViewAreFresh) satellitesInView else null,
                satellitesUsed = if (satellitesUsedAreFresh) satellitesUsed else null,
                gpsSatellitesInView = if (gnssStatusIsFresh) gpsSatellitesInView else null,
                gpsSatellitesUsed = if (gnssStatusIsFresh) gpsSatellitesUsed else null,
                beidouSatellitesInView = if (gnssStatusIsFresh) beidouSatellitesInView else null,
                beidouSatellitesUsed = if (gnssStatusIsFresh) beidouSatellitesUsed else null,
                tripStats = calculateTripStats(
                    if (currentStatus == FixStatus.STALE || currentStatus == FixStatus.LOST) {
                        lastFixAtMillis ?: nowMillis
                    } else {
                        nowMillis
                    },
                ),
            )
        }
    }

    fun onLocationMonitoringStopped(nowMillis: Long) {
        if (_state.value.tripMode != TripMode.RECORDING) return
        tripStatsAccumulator.closingPoints(nowMillis).forEach(::appendTripPoint)
        segmentStartPending = true
        previousSample = null
        previousAndroidLocation = null
        update { copy(nowMillis = nowMillis, tripStats = calculateTripStats(nowMillis)) }
    }

    fun toggleTrip(nowMillis: Long) {
        when (_state.value.tripMode) {
            TripMode.IDLE -> {
                tripStatsAccumulator.clear()
                segmentStartPending = false
                tripStartedAtMillis = nowMillis
                pausedAtMillis = null
                totalPausedMillis = 0L
                previousSample = null
                previousAndroidLocation = null
                storage.startTrip(nowMillis)
                update {
                    copy(
                        tripMode = TripMode.RECORDING,
                        tripStats = EMPTY_TRIP_STATS,
                        restoredTrip = false,
                    )
                }
            }

            TripMode.RECORDING -> {
                tripStatsAccumulator.closingPoints(nowMillis).forEach(::appendTripPoint)
                pausedAtMillis = nowMillis
                storage.updateActiveTrip(TripMode.PAUSED, pausedAtMillis, totalPausedMillis)
                update {
                    val pausedState = copy(tripMode = TripMode.PAUSED)
                    pausedState.copy(tripStats = tripStatsAccumulator.calculate(
                        tripStartedAtMillis ?: nowMillis,
                        effectiveTripStatsEndAtMillis(pausedState, nowMillis, pausedAtMillis),
                        totalPausedMillis,
                    ))
                }
            }

            TripMode.PAUSED -> {
                // 作者：long｜兼容旧版保存的暂停行程：先补齐暂停边界，避免恢复后丢失移动尾段。
                tripStatsAccumulator.closingPoints(pausedAtMillis ?: nowMillis).forEach(::appendTripPoint)
                totalPausedMillis += nowMillis - (pausedAtMillis ?: nowMillis)
                pausedAtMillis = null
                // 作者：long｜暂停恢复后断开两个定位点，避免把暂停期间位移补算成直线里程。
                previousSample = null
                previousAndroidLocation = null
                segmentStartPending = true
                storage.updateActiveTrip(TripMode.RECORDING, null, totalPausedMillis)
                update { copy(tripMode = TripMode.RECORDING) }
            }
        }
    }

    fun endTrip(nowMillis: Long) {
        if (_state.value.tripMode == TripMode.IDLE) return
        val startedAtMillis = tripStartedAtMillis ?: return
        val stateAtEnd = _state.value
        val statsEndAtMillis = effectiveTripStatsEndAtMillis(stateAtEnd, nowMillis, pausedAtMillis)
        if (stateAtEnd.tripMode == TripMode.PAUSED) pausedAtMillis = null
        else tripStatsAccumulator.closingPoints(statsEndAtMillis).forEach(::appendTripPoint)
        val finalStats = tripStatsAccumulator.calculate(
            startAtMillis = startedAtMillis,
            endAtMillis = statsEndAtMillis,
            pausedMillis = totalPausedMillis,
        )
        storage.completeTrip(
            CompletedTripRecord(
                startedAtMillis = startedAtMillis,
                endedAtMillis = nowMillis,
                stats = finalStats,
            ),
        )
        tripStartedAtMillis = null
        segmentStartPending = false
        previousSample = null
        previousAndroidLocation = null
        update {
            copy(
                tripMode = TripMode.IDLE,
                tripStats = finalStats,
                recentTrips = storage.recentTrips(),
                restoredTrip = false,
            )
        }
    }

    fun toggleTheme() = update { copy(darkTheme = !darkTheme) }

    override fun onCleared() {
        storage.close()
        super.onCleared()
    }

    private fun calculateTripStats(nowMillis: Long): TripStats {
        val startedAt = tripStartedAtMillis ?: return _state.value.tripStats
        val effectiveEndAtMillis = if (_state.value.tripMode == TripMode.PAUSED) {
            effectiveTripStatsEndAtMillis(_state.value, nowMillis, pausedAtMillis)
        } else {
            nowMillis
        }
        return tripStatsAccumulator.calculate(startedAt, effectiveEndAtMillis, totalPausedMillis)
    }

    private fun restorePersistedState() {
        val recentTrips = storage.recentTrips()
        val activeTrip = storage.loadActiveTrip()
        if (activeTrip == null) {
            update { copy(recentTrips = recentTrips) }
            return
        }

        tripStatsAccumulator.rebuild(activeTrip.points)
        if (activeTrip.mode == TripMode.RECORDING) {
            activeTrip.points.lastOrNull()?.let {
                tripStatsAccumulator.closingPoints(it.timestampMillis).forEach(::appendTripPoint)
            }
            segmentStartPending = true
        }
        tripStartedAtMillis = activeTrip.startedAtMillis
        pausedAtMillis = activeTrip.pausedAtMillis
        totalPausedMillis = activeTrip.totalPausedMillis
        previousSample = null
        previousAndroidLocation = null

        val nowMillis = nowProvider()
        val statsEndAtMillis = if (activeTrip.mode == TripMode.PAUSED) {
            activeTrip.pausedAtMillis ?: nowMillis
        } else {
            nowMillis
        }
        update {
            copy(
                nowMillis = nowMillis,
                tripMode = activeTrip.mode,
                tripStats = tripStatsAccumulator.calculate(
                    startAtMillis = activeTrip.startedAtMillis,
                    endAtMillis = statsEndAtMillis,
                    pausedMillis = activeTrip.totalPausedMillis,
                ),
                recentTrips = recentTrips,
                restoredTrip = true,
            )
        }
    }

    private inline fun update(block: DashboardState.() -> DashboardState) {
        _state.value = _state.value.block()
    }

    private fun appendTripPoint(point: TripPoint) {
        tripStatsAccumulator.append(point)
        storage.appendPoint(point)
    }

    companion object {
        private const val MOVING_THRESHOLD_MPS = 2.0 / 3.6
        private const val NMEA_STALE_AFTER_MILLIS = 5_000L
        private const val SATELLITE_STATS_STALE_AFTER_MILLIS = 10_000L

        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
        private val CHINESE_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA).withZone(ZoneId.systemDefault())

        fun formatTime(millis: Long): String = TIME_FORMATTER.format(Instant.ofEpochMilli(millis))
        fun formatDate(millis: Long): String = DATE_FORMATTER.format(Instant.ofEpochMilli(millis))
        fun formatDateChinese(millis: Long): String = CHINESE_DATE_FORMATTER.format(Instant.ofEpochMilli(millis))
        fun formatDuration(millis: Long): String {
            val seconds = (millis / 1_000L).coerceAtLeast(0L)
            return "%02d:%02d:%02d".format(seconds / 3_600, (seconds % 3_600) / 60, seconds % 60)
        }

        fun formatSpeed(speedMps: Double): String = (speedMps * 3.6).roundToInt().toString()
    }
}

private fun Long?.isFreshAt(nowMillis: Long, maxAgeMillis: Long): Boolean =
    this?.let { nowMillis - it in 0..maxAgeMillis } == true

internal fun effectiveTripStatsEndAtMillis(
    state: DashboardState,
    nowMillis: Long,
    pausedAtMillis: Long?,
): Long {
    val endAtMillis = if (state.tripMode == TripMode.PAUSED) pausedAtMillis ?: nowMillis else nowMillis
    val lastFixAt = state.lastFixAtMillis ?: return endAtMillis
    // 作者：long｜按暂停当时的新鲜度结算；暂停之后再失锁不能把已经冻结的行程时间向前回退。
    return if (endAtMillis - lastFixAt > 3_000L) minOf(endAtMillis, lastFixAt) else endAtMillis
}

private val EMPTY_TRIP_STATS = TripStats(
    distanceMeters = 0.0,
    elapsedMillis = 0L,
    movingMillis = 0L,
    stoppedMillis = 0L,
    tripAverageMps = 0.0,
    movingAverageMps = 0.0,
    maxSpeedMps = 0.0,
)
