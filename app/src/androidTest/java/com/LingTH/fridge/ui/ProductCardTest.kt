package com.LingTH.fridge.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.LingTH.fridge.ProductCard
import com.LingTH.fridge.UiTags
import com.LingTH.fridge.testutil.product
import com.LingTH.fridge.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductCardTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(product: Databases.ProductData, onClick: () -> Unit = {}) {
        compose.setContent {
            MyApplicationTheme { ProductCard(product = product, onClick = onClick) }
        }
    }

    @Test
    fun showsNameAndPrimaryCategory() {
        show(product(name = "Fresh Milk", categories = "en:dairies, en:snacks"))

        compose.onNodeWithText("Fresh Milk").assertIsDisplayed()
        // getPrimaryCategory prefers water/snack/food tags
        compose.onNodeWithText("en:snacks").assertIsDisplayed()
    }

    @Test
    fun blankNameShowsPlaceholder() {
        show(product(name = ""))
        compose.onNodeWithText("Unnamed item").assertIsDisplayed()
    }

    @Test
    fun expiredBadge() {
        show(product(expiresInDays = -2))
        compose.onNodeWithTag(UiTags.EXPIRY_BADGE, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Expired").assertIsDisplayed()
    }

    @Test
    fun expiresTodayBadge() {
        show(product(expiresInDays = 0))
        compose.onNodeWithText("Expires today").assertIsDisplayed()
    }

    @Test
    fun freshBadgeShowsDaysLeft() {
        show(product(expiresInDays = 12))
        compose.onNodeWithText("12 day(s) left").assertIsDisplayed()
    }

    @Test
    fun noDateBadge() {
        show(product(expiresInDays = null))
        compose.onNodeWithText("No date").assertIsDisplayed()
    }

    @Test
    fun clickInvokesCallback() {
        var clicked = false
        show(product(name = "Tap me")) { clicked = true }

        compose.onNodeWithTag(UiTags.PRODUCT_CARD).performClick()

        assertTrue(clicked)
    }
}
