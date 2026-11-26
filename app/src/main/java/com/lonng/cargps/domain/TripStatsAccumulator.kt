package com.lonng.cargps.domain

class TripStatsAccumulator {
    private var distanceMeters = 0.0
    private var movingMillisBeforeLastPoint = 0L
    private var maxSpeedMps = 0.0
    private var lastPoint: TripPoint? = null

    fun clear() {
        distanceMeters = 0.0
        movingMillisBeforeLastPoint = 0L
        maxSpeedMps = 0.0
        lastPoint = null
    }

    fun append(point: TripPoint) {
        val previous = lastPoint
        distanceMeters += point.distanceFromPreviousMeters.coerceAtLeast(0.0)
        if (previous != null && point.moving) {
            // 作者：long｜路段时间归属终点样本；只在新样本到达时累加，避免每秒重扫整条轨迹。
            movingMillisBeforeLastPoint += (point.timestampMillis - previous.timestampMillis).coerceAtLeast(0L)
        }
        maxSpeedMps = maxOf(maxSpeedMps, point.speedMps.coerceAtLeast(0.0))
        lastPoint = point
    }

    fun closingPoints(atMillis: Long): List<TripPoint> {
        val last = lastPoint ?: return emptyList()
        if (!last.moving) return emptyList()
        val endAt = if (atMillis - last.timestampMillis in 0..3_000L) atMillis else last.timestampMillis
        // 作者：long｜用零距离边界保存移动尾段，再关闭移动标记；重建统计时同样不会跨越暂停间隔。
        return listOf(
            TripPoint(endAt, 0.0, 0.0, last.moving),
            TripPoint(endAt, 0.0, 0.0, false),
        )
    }

    fun rebuild(points: Iterable<TripPoint>) {
        clear()
        points.forEach(::append)
    }

    fun calculate(startAtMillis: Long, endAtMillis: Long, pausedMillis: Long): TripStats {
        val elapsedMillis = (endAtMillis - startAtMillis - pausedMillis).coerceAtLeast(0L)
        val last = lastPoint
        val movingTailMillis = if (last?.moving == true) {
            (endAtMillis - last.timestampMillis).coerceAtLeast(0L)
        } else {
            0L
        }
        val movingMillis = (movingMillisBeforeLastPoint + movingTailMillis).coerceAtMost(elapsedMillis)
        val stoppedMillis = (elapsedMillis - movingMillis).coerceAtLeast(0L)

        return TripStats(
            distanceMeters = distanceMeters,
            elapsedMillis = elapsedMillis,
            movingMillis = movingMillis,
            stoppedMillis = stoppedMillis,
            tripAverageMps = distanceMeters / elapsedMillis.secondsOrOne(),
            movingAverageMps = distanceMeters / movingMillis.secondsOrOne(),
            maxSpeedMps = maxSpeedMps,
        )
    }

    private fun Long.secondsOrOne(): Double = if (this == 0L) 1.0 else this / 1_000.0
}
