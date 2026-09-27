package com.LingTH.fridge.data

import InventoryDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.LingTH.fridge.sortandfilter.Setting.Settings
import com.LingTH.fridge.testutil.inMemoryDb
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDaoTest {

    private lateinit var db: InventoryDatabase

    @Before
    fun setUp() {
        db = inMemoryDb()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun emptyUntilSaved() = runTest {
        assertNull(db.settingsDao().getSettings())
    }

    @Test
    fun saveAndOverwriteKeepsASingleRow() = runTest {
        val dao = db.settingsDao()
        dao.insertOrUpdate(Settings(0, "3 days", "Normal", "4", ""))
        dao.insertOrUpdate(Settings(0, "1 week", "Friendly", "2", "me@example.com"))

        val saved = dao.getSettings()!!
        assertEquals("1 week", saved.alertBeforeExpiry)
        assertEquals("Friendly", saved.alertMode)
        assertEquals("me@example.com", saved.email)
    }
}
