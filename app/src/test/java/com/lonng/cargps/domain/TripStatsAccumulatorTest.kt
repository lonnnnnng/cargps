package com.lonng.cargps.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TripStatsAccumulatorTest {
    @Test
    fun `incremental totals match batch calculation for a long trip`() {
        val points = (0 until 30_000).map { index ->
            TripPoint(
                timestampMillis = index * 1_000L,
                speedMps = if (index % 5 == 0) 0.0 else 12.5,
                distanceFromPreviousMeters = if (index == 0) 0.0 else 12.5,
                moving = index % 5 != 0,
            )
        }
        val accumulator = TripStatsAccumulator().apply { rebuild(points) }
        val expected = TripStatsCalculator.calculate(
            startAtMillis = 0L,
            endAtMillis = 31_000_000L,
            points = points,
            pausedMillis = 2_000_000L,
        )

        val actual = accumulator.calculate(0L, 31_000_000L, pausedMillis = 2_000_000L)

        assertEquals(expected.distanceMeters, actual.distanceMeters, 0.001)
        assertEquals(expected.elapsedMillis, actual.elapsedMillis)
        assertEquals(expected.movingMillis, actual.movingMillis)
        assertEquals(expected.stoppedMillis, actual.stoppedMillis)
        assertEquals(expected.tripAverageMps, actual.tripAverageMps, 0.001)
        assertEquals(expected.movingAverageMps, actual.movingAverageMps, 0.001)
        assertEquals(expected.maxSpeedMps, actual.maxSpeedMps, 0.001)
    }

    @Test
    fun `last moving segment advances with the end time`() {
        val accumulator = TripStatsAccumulator().apply {
            append(TripPoint(0L, 10.0, 0.0, moving = true))
            append(TripPoint(2_000L, 10.0, 20.0, moving = true))
        }

        assertEquals(5_000L, accumulator.calculate(0L, 5_000L, 0L).movingMillis)
        assertEquals(2_000L, accumulator.calculate(0L, 5_000L, 3_000L).elapsedMillis)
    }

    @Test
    fun `persisted boundaries preserve moving tail and exclude pause after rebuild`() {
        val points = mutableListOf(TripPoint(10_000L, 10.0, 0.0, true))
        val accumulator = TripStatsAccumulator().apply { rebuild(points) }
        val closing = accumulator.closingPoints(12_000L)
        closing.forEach(accumulator::append)
        points += closing
        val resumed = listOf(TripPoint(112_000L, 0.0, 0.0, false), TripPoint(113_000L, 10.0, 10.0, true))
        resumed.forEach(accumulator::append)
        points += resumed
        val expected = accumulator.calculate(0L, 113_000L, 100_000L)
        val restored = TripStatsAccumulator().apply { rebuild(points) }.calculate(0L, 113_000L, 100_000L)
        assertEquals(3_000L, expected.movingMillis)
        assertEquals(expected, restored)
    }
}
