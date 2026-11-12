package id.kitabkuning.androidai.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the real formatDuration contract: milliseconds -> m:ss, switching
 * to h:mm:ss at one hour; non-positive and sub-second values render "0:00".
 */
class FormatTest {

    @Test
    fun zero_isZeroZero() {
        assertEquals("0:00", formatDuration(0L))
    }

    @Test
    fun negative_isZeroZero() {
        assertEquals("0:00", formatDuration(-1L))
        assertEquals("0:00", formatDuration(-60_000L))
    }

    @Test
    fun subSecond_truncatesToZero() {
        assertEquals("0:00", formatDuration(999L))
    }

    @Test
    fun seconds_underOneMinute() {
        assertEquals("0:01", formatDuration(1_000L))
        assertEquals("0:51", formatDuration(51_000L))
        assertEquals("0:59", formatDuration(59_999L))
    }

    @Test
    fun minutes_underOneHour() {
        assertEquals("1:00", formatDuration(60_000L))
        assertEquals("1:01", formatDuration(61_000L))
        assertEquals("59:59", formatDuration(3_599_000L))
    }

    @Test
    fun hours_useH_mm_ss() {
        assertEquals("1:00:00", formatDuration(3_600_000L))
        assertEquals("1:01:01", formatDuration(3_661_000L))
        assertEquals("10:00:00", formatDuration(36_000_000L))
    }
}