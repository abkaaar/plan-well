package com.planwell.app.domain.agenda

import java.util.Calendar
import java.util.TimeZone

/**
 * Inclusive local-calendar day window in epoch millis using the device default time zone.
 */
data class DayBounds(
    val startMs: Long,
    val endMsInclusive: Long,
) {
    fun contains(ms: Long): Boolean = ms in startMs..endMsInclusive

    fun isBefore(ms: Long): Boolean = ms < startMs

    companion object {
        fun forInstant(
            nowMs: Long,
            timeZone: TimeZone = TimeZone.getDefault(),
        ): DayBounds {
            val start = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowMs
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = Calendar.getInstance(timeZone).apply {
                timeInMillis = start
                add(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MILLISECOND, -1)
            }.timeInMillis
            return DayBounds(startMs = start, endMsInclusive = end)
        }
    }
}
