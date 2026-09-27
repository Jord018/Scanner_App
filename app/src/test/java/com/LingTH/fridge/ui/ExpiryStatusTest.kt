package com.LingTH.fridge.ui

import com.LingTH.fridge.ui.theme.EXPIRY_SOON_DAYS
import com.LingTH.fridge.ui.theme.ExpiryExpired
import com.LingTH.fridge.ui.theme.ExpiryFresh
import com.LingTH.fridge.ui.theme.ExpiryLevel
import com.LingTH.fridge.ui.theme.ExpirySoon
import com.LingTH.fridge.ui.theme.expiryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpiryStatusTest {

    @Test
    fun `levels by days left`() {
        assertEquals(ExpiryLevel.UNKNOWN, expiryStatus(null).level)
        assertEquals(ExpiryLevel.EXPIRED, expiryStatus(-1).level)
        assertEquals(ExpiryLevel.SOON, expiryStatus(0).level)
        assertEquals(ExpiryLevel.SOON, expiryStatus(EXPIRY_SOON_DAYS).level)
        assertEquals(ExpiryLevel.FRESH, expiryStatus(EXPIRY_SOON_DAYS + 1).level)
    }

    @Test
    fun `labels`() {
        assertEquals("Expired", expiryStatus(-5).label)
        assertEquals("Expires today", expiryStatus(0).label)
        assertEquals("2 day(s) left", expiryStatus(2).label)
        assertEquals("12 day(s) left", expiryStatus(12).label)
        assertEquals("No date", expiryStatus(null).label)
    }

    @Test
    fun `colors match level`() {
        assertEquals(ExpiryExpired, expiryStatus(-1).color)
        assertEquals(ExpirySoon, expiryStatus(1).color)
        assertEquals(ExpiryFresh, expiryStatus(10).color)
    }
}
