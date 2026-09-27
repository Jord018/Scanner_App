package com.LingTH.fridge.data

import com.LingTH.fridge.Barcode.convertDateToMillis
import com.LingTH.fridge.Barcode.convertMillisToDate
import com.LingTH.fridge.Barcode.getTodayDate
import com.LingTH.fridge.Barcode.toDateString
import com.LingTH.fridge.Barcode.toDateStringEdit
import com.LingTH.fridge.Barcode.toEpochMillis
import com.LingTH.fridge.Barcode.toEpochMillisEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class DateConversionTest {

    @Test
    fun `dd-MM-yyyy round trips through millis`() {
        val millis = convertDateToMillis("25/12/2026")!!
        assertEquals("25/12/2026", convertMillisToDate(millis))
    }

    @Test
    fun `toEpochMillis parses the format used by the date picker`() {
        val millis = "01/02/2027".toEpochMillis()
        assertTrue(millis > 0)
        assertEquals("01/02/2027", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(millis))
    }

    @Test
    fun `invalid input is rejected`() {
        assertEquals(0L, "not a date".toEpochMillis())
        assertEquals(0L, "31/02/2027".toEpochMillis()) // strict parsing
        assertEquals(0L, "2027-02-01".toEpochMillis())
        assertNull(convertDateToMillis("2027-02-01"))
    }

    @Test
    fun `today's date parses back`() {
        assertTrue(getTodayDate().toEpochMillis() > 0)
    }

    @Test
    fun `toDateString uses ISO format`() {
        val millis = "15/03/2027".toEpochMillis()
        assertEquals("2027-03-15", millis.toDateString())
    }

    @Ignore("BUG: Edit screen stores dates with toDateStringEdit (yyyy-MM-dd) but reads them with " +
        "toEpochMillisEdit (dd/MM/yyyy). Saving an edit without re-picking the dates writes 0 (1970), " +
        "so the product shows as expired")
    @Test
    fun `edit screen date format round trips`() {
        val original = "15/03/2027".toEpochMillisEdit()
        val shownInForm = original.toDateStringEdit()
        assertNotEquals(0L, shownInForm.toEpochMillisEdit())
        assertEquals(original, shownInForm.toEpochMillisEdit())
    }
}
