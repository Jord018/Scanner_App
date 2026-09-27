package com.LingTH.fridge.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import parseAlertDays

class ParseAlertDaysTest {

    @Test
    fun `parses days weeks and months`() {
        assertEquals(listOf(1), parseAlertDays("1 day"))
        assertEquals(listOf(3), parseAlertDays("3 days"))
        assertEquals(listOf(14), parseAlertDays("2 weeks"))
        assertEquals(listOf(30), parseAlertDays("1 month"))
        assertEquals(listOf(180), parseAlertDays("6 months"))
    }

    @Test
    fun `zero days is allowed`() {
        assertEquals(listOf(0), parseAlertDays("0 days"))
    }

    @Test
    fun `parses a comma separated list and ignores case and spaces`() {
        assertEquals(listOf(1, 7, 30), parseAlertDays(" 1 Day, 1 WEEK ,1 month"))
    }

    @Test
    fun `every option shown in the Settings screen can be parsed`() {
        val settingsOptions = listOf(
            "0 days", "1 day", "2 days", "3 days", "4 days", "5 days", "1 week", "2 weeks",
            "3 weeks", "4 weeks", "1 month", "2 months", "3 months", "6 months"
        )
        settingsOptions.forEach { option ->
            assertEquals("option \"$option\"", 1, parseAlertDays(option).size)
        }
    }

    @Test
    fun `unknown formats are skipped`() {
        assertEquals(emptyList<Int>(), parseAlertDays("soon"))
        assertEquals(listOf(2), parseAlertDays("2 days, tomorrow"))
        assertEquals(emptyList<Int>(), parseAlertDays(""))
    }

    @Test
    fun `Thai value seeded by old builds is parsed`() {
        assertEquals(listOf(1), parseAlertDays("ก่อน 1 วัน"))
        assertEquals(listOf(14), parseAlertDays("2 สัปดาห์"))
    }
}
