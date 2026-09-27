package com.LingTH.fridge.notification

import buildNotificationMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationMessageTest {

    private val styles = listOf("Normal", "E-Girlfriend", "Aggressive", "Friendly")

    @Test
    fun `every style mentions the product and the day count`() {
        styles.forEach { style ->
            val expiry = buildNotificationMessage("Milk", 3, "expiry", style)
            assertTrue("$style expiry: $expiry", expiry.contains("Milk") && expiry.contains("3"))

            val expired = buildNotificationMessage("Milk", -2, "expired", style)
            assertTrue("$style expired: $expired", expired.contains("Milk") && expired.contains("2"))
        }
    }

    @Test
    fun `expired messages show a positive number of days`() {
        styles.forEach { style ->
            val message = buildNotificationMessage("Eggs", -4, "expired", style)
            assertFalse("$style: $message", message.contains("-4"))
        }
    }

    @Test
    fun `normal style wording`() {
        assertEquals("Product: Milk will expire in 3 day(s)", buildNotificationMessage("Milk", 3, "expiry", "Normal"))
        assertEquals("Product: Milk expired 2 day(s) ago", buildNotificationMessage("Milk", -2, "expired", "Normal"))
    }

    @Test
    fun `unknown style falls back to normal wording`() {
        assertEquals(
            buildNotificationMessage("Milk", 3, "expiry", "Normal"),
            buildNotificationMessage("Milk", 3, "expiry", "ปกติ")
        )
    }

    @Test
    fun `unknown status still produces a message`() {
        styles.forEach { style ->
            assertTrue(buildNotificationMessage("Milk", 0, "other", style).contains("Milk"))
        }
    }
}
