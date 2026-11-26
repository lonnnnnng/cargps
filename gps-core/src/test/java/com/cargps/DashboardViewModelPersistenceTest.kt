package com.cargps

import com.cargps.domain.TripPoint
import com.cargps.domain.TripStats
import com.cargps.domain.NmeaFrame
import com.cargps.domain.NmeaSentenceType
import com.cargps.storage.ActiveTripRecord
import com.cargps.storage.CompletedTripRecord
import com.cargps.storage.TripStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardViewModelPersistenceTest {
    @Test
    fun `进程重建后恢复活动行程和已有统计`() {
        val storage = FakeTripStorage(
            activeTrip = ActiveTripRecord(
                mode = TripMode.RECORDING,
                startedAtMillis = 1_000L,
                pausedAtMillis = null,
                totalPausedMillis = 0L,
                points = listOf(
                    TripPoint(
                        timestampMillis = 2_000L,
                        speedMps = 10.0,
                        distanceFromPreviousMeters = 20.0,
                        moving = true,
                    ),
                ),
            ),
        )

        val viewModel = DashboardViewModel(storage = storage, nowProvider = { 3_000L })

        assertEquals(TripMode.RECORDING, viewModel.state.value.tripMode)
        assertTrue(viewModel.state.value.restoredTrip)
        assertEquals(20.0, viewModel.state.value.tripStats.distanceMeters, 0.001)
        assertEquals(2_000L, viewModel.state.value.tripStats.elapsedMillis)
    }

    @Test
    fun `暂停状态结束行程时暂停时长只累计一次`() {
        val storage = FakeTripStorage(
            activeTrip = ActiveTripRecord(
                mode = TripMode.PAUSED,
                startedAtMillis = 1_000L,
                pausedAtMillis = 8_000L,
                totalPausedMillis = 1_000L,
                points = emptyList(),
            ),
        )
        val viewModel = DashboardViewModel(storage = storage, nowProvider = { 10_000L })

        viewModel.endTrip(10_000L)

        assertEquals(TripMode.IDLE, viewModel.state.value.tripMode)
        assertFalse(viewModel.state.value.restoredTrip)
        assertEquals(6_000L, storage.completed.single().stats.elapsedMillis)
    }

    @Test
    fun `恢复后的暂停行程不会把暂停等待时间算成移动时长`() {
        val storage = FakeTripStorage(
            activeTrip = ActiveTripRecord(
                mode = TripMode.PAUSED,
                startedAtMillis = 0L,
                pausedAtMillis = 10_000L,
                totalPausedMillis = 0L,
                points = listOf(
                    TripPoint(0L, 0.0, 0.0, moving = false),
                    TripPoint(5_000L, 0.0, 0.0, moving = false),
                    TripPoint(8_000L, 10.0, 20.0, moving = true),
                ),
            ),
        )
        val viewModel = DashboardViewModel(storage = storage, nowProvider = { 30_000L })

        viewModel.onTick(60_000L)

        assertEquals(10_000L, viewModel.state.value.tripStats.elapsedMillis)
        assertEquals(5_000L, viewModel.state.value.tripStats.movingMillis)
        assertEquals(5_000L, viewModel.state.value.tripStats.stoppedMillis)
    }

    @Test
    fun `NMEA 精度因子在没有新报文后过期`() {
        val viewModel = DashboardViewModel(nowProvider = { 1_000L })
        viewModel.onNmea(
            NmeaFrame(
                type = NmeaSentenceType.GSA,
                hdop = 0.8,
                pdop = 1.2,
                vdop = 0.9,
            ),
        )
        assertEquals(0.8, viewModel.state.value.hdop!!, 0.001)

        viewModel.onTick(6_001L)

        assertNull(viewModel.state.value.hdop)
        assertNull(viewModel.state.value.pdop)
        assertNull(viewModel.state.value.vdop)
        assertNull(viewModel.state.value.lastNmeaType)
    }

    @Test
    fun `新 HDOP 报文不会续期已经过期的 PDOP 和 VDOP`() {
        var nowMillis = 1_000L
        val viewModel = DashboardViewModel(nowProvider = { nowMillis })
        viewModel.onNmea(
            NmeaFrame(type = NmeaSentenceType.GSA, hdop = 0.8, pdop = 1.2, vdop = 0.9),
        )
        nowMillis = 4_000L
        viewModel.onNmea(NmeaFrame(type = NmeaSentenceType.GGA, hdop = 0.7))

        viewModel.onTick(6_001L)

        assertEquals(0.7, viewModel.state.value.hdop!!, 0.001)
        assertNull(viewModel.state.value.pdop)
        assertNull(viewModel.state.value.vdop)
    }

    @Test
    fun `每个 NMEA 卫星数量按自身更新时间过期`() {
        var nowMillis = 1_000L
        val viewModel = DashboardViewModel(nowProvider = { nowMillis })
        viewModel.onNmea(
            NmeaFrame(
                type = NmeaSentenceType.GGA,
                satellitesInView = 16,
                satellitesUsed = 9,
            ),
        )
        nowMillis = 6_000L
        viewModel.onNmea(NmeaFrame(type = NmeaSentenceType.GSV, satellitesInView = 12))

        viewModel.onTick(12_001L)

        assertEquals(12, viewModel.state.value.satellitesInView)
        assertNull(viewModel.state.value.satellitesUsed)
    }

    @Test
    fun `GNSS 回调分别保留 GPS 和北斗可见及使用数量并在超时后清空`() {
        val viewModel = DashboardViewModel(nowProvider = { 1_000L })

        viewModel.onSatellitesChanged(
            inView = 16,
            usedInFix = 9,
            gpsInView = 8,
            gpsUsedInFix = 5,
            beidouInView = 6,
            beidouUsedInFix = 4,
        )

        assertEquals(8, viewModel.state.value.gpsSatellitesInView)
        assertEquals(5, viewModel.state.value.gpsSatellitesUsed)
        assertEquals(6, viewModel.state.value.beidouSatellitesInView)
        assertEquals(4, viewModel.state.value.beidouSatellitesUsed)

        viewModel.onNmea(
            NmeaFrame(
                type = NmeaSentenceType.GSV,
                satellitesInView = 12,
            ),
        )
        assertEquals(16, viewModel.state.value.satellitesInView)

        viewModel.onTick(11_001L)

        assertNull(viewModel.state.value.gpsSatellitesInView)
        assertNull(viewModel.state.value.gpsSatellitesUsed)
        assertNull(viewModel.state.value.beidouSatellitesInView)
        assertNull(viewModel.state.value.beidouSatellitesUsed)
    }

    @Test
    fun `失锁或暂停结束行程时使用先发生的统计冻结时刻`() {
        val lost = DashboardState(
            tripMode = TripMode.RECORDING,
            fixStatus = FixStatus.LOST,
            lastFixAtMillis = 10_000L,
        )
        val pausedAndLost = lost.copy(tripMode = TripMode.PAUSED)

        assertEquals(10_000L, effectiveTripStatsEndAtMillis(lost, nowMillis = 20_000L, pausedAtMillis = null))
        assertEquals(12_000L, effectiveTripStatsEndAtMillis(
            pausedAndLost,
            nowMillis = 20_000L,
            pausedAtMillis = 12_000L,
        ))
        assertEquals(10_000L, effectiveTripStatsEndAtMillis(
            pausedAndLost,
            nowMillis = 20_000L,
            pausedAtMillis = 15_000L,
        ))
        assertEquals(12_000L, effectiveTripStatsEndAtMillis(
            pausedAndLost.copy(fixStatus = FixStatus.FIXED),
            nowMillis = 20_000L,
            pausedAtMillis = 12_000L,
        ))
    }

    private class FakeTripStorage(
        private var activeTrip: ActiveTripRecord? = null,
    ) : TripStorage {
        val completed = mutableListOf<CompletedTripRecord>()

        override fun loadActiveTrip(): ActiveTripRecord? = activeTrip

        override fun startTrip(startedAtMillis: Long) {
            activeTrip = ActiveTripRecord(
                mode = TripMode.RECORDING,
                startedAtMillis = startedAtMillis,
                pausedAtMillis = null,
                totalPausedMillis = 0L,
                points = emptyList(),
            )
        }

        override fun appendPoint(point: TripPoint) {
            activeTrip = activeTrip?.copy(points = activeTrip.orEmptyPoints() + point)
        }

        override fun updateActiveTrip(mode: TripMode, pausedAtMillis: Long?, totalPausedMillis: Long) {
            activeTrip = activeTrip?.copy(
                mode = mode,
                pausedAtMillis = pausedAtMillis,
                totalPausedMillis = totalPausedMillis,
            )
        }

        override fun completeTrip(record: CompletedTripRecord) {
            completed += record
            activeTrip = null
        }

        override fun recentTrips(limit: Int): List<CompletedTripRecord> = completed.takeLast(limit).reversed()

        override fun completedTripPoints(tripId: Long): List<TripPoint> = emptyList()

        private fun ActiveTripRecord?.orEmptyPoints(): List<TripPoint> = this?.points.orEmpty()
    }
}
