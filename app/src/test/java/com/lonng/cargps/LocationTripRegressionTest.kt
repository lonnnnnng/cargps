package com.lonng.cargps

import android.location.Location
import com.lonng.cargps.domain.TripPoint
import com.lonng.cargps.storage.ActiveTripRecord
import com.lonng.cargps.storage.CompletedTripRecord
import com.lonng.cargps.storage.TripStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27])
class LocationTripRegressionTest {
    @Test fun pauseResumeAndProcessRestoreKeepSameMovingTime() {
        val storage = MemoryStorage()
        val model = DashboardViewModel(storage, nowProvider = { 30_000L })
        model.toggleTrip(0L)
        model.onLocation(location(1_000L, 0.0))
        model.onLocation(location(2_000L, 0.0001))
        model.toggleTrip(3_000L)
        model.onTick(20_000L)
        assertEquals(3_000L, model.state.value.tripStats.elapsedMillis)
        assertEquals(2_000L, model.state.value.tripStats.movingMillis)
        model.toggleTrip(23_000L)
        model.onLocation(location(24_000L, 0.01))
        model.onLocation(location(25_000L, 0.0101))
        val before = model.state.value.tripStats
        assertEquals(3_000L, before.movingMillis)
        assertTrue("恢复前后的有效路段合计为 ${before.distanceMeters} 米", before.distanceMeters in 18.0..20.0)
        val restored = DashboardViewModel(storage, nowProvider = { 25_000L })
        assertEquals(before, restored.state.value.tripStats)
    }

    @Test fun staleGapAndEndDoNotAddDistanceOrMovingTime() {
        val model = DashboardViewModel(nowProvider = { 1_000L })
        model.toggleTrip(0L)
        model.onLocation(location(1_000L, 0.0))
        model.onLocation(location(2_000L, 0.0001))
        val distance = model.state.value.tripStats.distanceMeters
        model.onTick(6_001L)
        model.onLocation(location(7_000L, 0.01))
        assertEquals(distance, model.state.value.tripStats.distanceMeters, 0.001)
        assertEquals(1_000L, model.state.value.tripStats.movingMillis)
        model.onTick(20_000L)
        val frozen = model.state.value.tripStats
        model.endTrip(20_000L)
        assertEquals(frozen, model.state.value.tripStats)
    }

    @Test fun returningFromBackgroundDoesNotConnectLocations() {
        val model = DashboardViewModel()
        model.toggleTrip(0L)
        model.onLocation(location(1_000L, 0.0))
        model.onLocationMonitoringStopped(1_500L)
        model.onLocation(location(2_000L, 0.01))
        assertEquals(0.0, model.state.value.tripStats.distanceMeters, 0.001)
        assertEquals(500L, model.state.value.tripStats.movingMillis)
    }

    private fun location(at: Long, lon: Double) = Location("gps").apply {
        time = at
        latitude = 31.0
        longitude = lon
        accuracy = 4f
        speed = 10f
    }

    private class MemoryStorage : TripStorage {
        private var active: ActiveTripRecord? = null
        override fun loadActiveTrip() = active
        override fun startTrip(startedAtMillis: Long) {
            active = ActiveTripRecord(TripMode.RECORDING, startedAtMillis, null, 0L, emptyList())
        }
        override fun appendPoint(point: TripPoint) {
            active = active?.let { it.copy(points = it.points + point) }
        }
        override fun updateActiveTrip(mode: TripMode, pausedAtMillis: Long?, totalPausedMillis: Long) {
            active = active?.copy(mode = mode, pausedAtMillis = pausedAtMillis, totalPausedMillis = totalPausedMillis)
        }
        override fun completeTrip(record: CompletedTripRecord) { active = null }
        override fun recentTrips(limit: Int) = emptyList<CompletedTripRecord>()
        override fun completedTripPoints(tripId: Long) = emptyList<TripPoint>()
    }
}
