package com.LingTH.fridge.data

import Databases.daysUntilExpiry
import com.LingTH.fridge.testutil.product
import com.LingTH.fridge.testutil.utcDaysFromToday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DaysUntilExpiryTest {

    @Test
    fun `expires today is zero`() {
        assertEquals(0L, product(expiresInDays = 0).daysUntilExpiry())
    }

    @Test
    fun `future and past dates`() {
        assertEquals(1L, product(expiresInDays = 1).daysUntilExpiry())
        assertEquals(30L, product(expiresInDays = 30).daysUntilExpiry())
        assertEquals(-1L, product(expiresInDays = -1).daysUntilExpiry())
    }

    @Test
    fun `time of day does not change the count`() {
        val lateInDay = product(expiresInDays = null).copy(
            expiration_date = utcDaysFromToday(2) + 23 * 60 * 60 * 1000
        )
        assertEquals(2L, lateInDay.daysUntilExpiry())
    }

    @Test
    fun `no expiration date gives null`() {
        assertNull(product(expiresInDays = null).daysUntilExpiry())
    }
}
