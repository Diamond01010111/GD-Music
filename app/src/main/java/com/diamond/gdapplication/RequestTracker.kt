package com.diamond.gdapplication

import android.os.SystemClock
import java.util.ArrayDeque

object RequestTracker {
    private const val WINDOW_MS = 5L * 60L * 1_000L
    private val timestamps = ArrayDeque<Long>()

    @JvmStatic
    @Synchronized
    fun record() {
        val now = SystemClock.elapsedRealtime()
        timestamps.addLast(now)
        trim(now)
    }

    @JvmStatic
    @Synchronized
    fun countLastFiveMinutes(): Int {
        trim(SystemClock.elapsedRealtime())
        return timestamps.size
    }

    private fun trim(now: Long) {
        while (timestamps.isNotEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
            timestamps.removeFirst()
        }
    }
}
