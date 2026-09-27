package com.LingTH.fridge.ui

import InventoryDatabase
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.LingTH.fridge.UiTags
import com.LingTH.fridge.sortandfilter.Setting.SettingsScreen
import com.LingTH.fridge.sortandfilter.Setting.viewmodel.SettingsViewModel
import com.LingTH.fridge.testutil.inMemoryDb
import com.LingTH.fridge.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var db: InventoryDatabase

    @Before
    fun setUp() {
        db = inMemoryDb()
        val vm = SettingsViewModel(db.settingsDao())
        compose.setContent {
            MyApplicationTheme { SettingsScreen(navController = rememberNavController(), viewModel = vm) }
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun showsNotificationOptions() {
        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.onNodeWithText("Notifications").assertIsDisplayed()
        compose.onNodeWithText("Alert before expired:").assertIsDisplayed()
        compose.onNodeWithText("Alert Mode:").assertIsDisplayed()
    }

    @Test
    fun choosingAnOptionAndSavingWritesToTheDatabase() {
        compose.onNodeWithText("Alert before expired:").performClick()
        compose.onNodeWithText("1 week").performScrollTo().performClick()

        compose.onNodeWithText("Alert Mode:").performScrollTo().performClick()
        compose.onNodeWithText("Friendly").performScrollTo().performClick()

        compose.onNodeWithTag(UiTags.SAVE_BUTTON).performScrollTo().performClick()
        compose.waitForIdle()

        compose.waitUntil(timeoutMillis = 5_000) {
            runBlocking { db.settingsDao().getSettings() } != null
        }
        val saved = runBlocking { db.settingsDao().getSettings() }!!
        assertEquals("1 week", saved.alertBeforeExpiry)
        assertEquals("Friendly", saved.alertMode)
    }
}
