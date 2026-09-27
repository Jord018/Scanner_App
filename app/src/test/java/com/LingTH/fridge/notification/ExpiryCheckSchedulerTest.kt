package com.LingTH.fridge.notification

import com.LingTH.fridge.Notification.AlertTimeManager
import com.LingTH.fridge.Notification.millisUntilNext
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ExpiryCheckSchedulerTest {

    private val morning = LocalDateTime.of(2026, 9, 28, 8, 30)

    @Test
    fun `next slot later today`() {
        assertEquals(TimeUnit.MINUTES.toMillis(30), millisUntilNext(LocalTime.of(9, 0), morning))
    }

    @Test
    fun `slot already passed moves to tomorrow`() {
        assertEquals(TimeUnit.HOURS.toMillis(23), millisUntilNext(LocalTime.of(7, 30), morning))
    }

    @Test
    fun `slot exactly now waits a full day`() {
        assertEquals(TimeUnit.DAYS.toMillis(1), millisUntilNext(LocalTime.of(8, 30), morning))
    }

    @Test
    fun `alert slots are spread from 9 to 21`() {
        assertEquals(listOf(LocalTime.of(9, 0)), AlertTimeManager(1).timeSlots)
        assertEquals(
            listOf("09:00", "12:00", "15:00", "18:00"),
            AlertTimeManager(4).timeSlots.map { it.toString() }
        )
    }
}
