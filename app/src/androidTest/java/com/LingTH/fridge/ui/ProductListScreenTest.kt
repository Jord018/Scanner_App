package com.LingTH.fridge.ui

import InventoryDatabase
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.LingTH.fridge.ProductGrid
import com.LingTH.fridge.SearchField
import com.LingTH.fridge.UiTags
import com.LingTH.fridge.sortandfilter.FilterViewModel
import com.LingTH.fridge.testutil.inMemoryDb
import com.LingTH.fridge.testutil.product
import com.LingTH.fridge.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ProductListScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var db: InventoryDatabase? = null

    @After
    fun tearDown() {
        db?.close()
    }

    @Test
    fun emptyFridgeShowsEmptyState() {
        compose.setContent {
            MyApplicationTheme { ProductGrid(products = emptyList(), hasAnyProducts = false, onProductClick = {}) }
        }
        compose.onNodeWithTag(UiTags.EMPTY_STATE).assertIsDisplayed()
        compose.onNodeWithText("Your fridge is empty").assertIsDisplayed()
    }

    @Test
    fun noMatchesShowsFilterHint() {
        compose.setContent {
            MyApplicationTheme { ProductGrid(products = emptyList(), hasAnyProducts = true, onProductClick = {}) }
        }
        compose.onNodeWithText("No matching items").assertIsDisplayed()
    }

    @Test
    fun showsACardPerProduct() {
        val items = listOf(product(id = 1, name = "A"), product(id = 2, name = "B"), product(id = 3, name = "C"))
        compose.setContent {
            MyApplicationTheme { ProductGrid(products = items, hasAnyProducts = true, onProductClick = {}) }
        }
        compose.onAllNodesWithTag(UiTags.PRODUCT_CARD).assertCountEquals(3)
    }

    @Test
    fun clickingACardReportsThatProduct() {
        var clicked: String? = null
        val items = listOf(product(id = 1, name = "A"), product(id = 2, name = "B"))
        compose.setContent {
            MyApplicationTheme {
                ProductGrid(products = items, hasAnyProducts = true, onProductClick = { clicked = it.product_name })
            }
        }
        compose.onNodeWithText("B").performClick()
        assertEquals("B", clicked)
    }

    @Test
    fun searchingFiltersProductsFromTheDatabase() {
        val database = inMemoryDb().also { db = it }
        runBlocking {
            database.productDao().insertProduct(product(name = "Milk", categories = "en:dairies"))
            database.productDao().insertProduct(product(name = "Apple", categories = "en:fruits"))
            database.productDao().insertProduct(product(name = "Water", categories = "en:waters"))
        }
        val vm = FilterViewModel(database.productDao())

        compose.setContent {
            MyApplicationTheme {
                Column {
                    var text by remember { mutableStateOf("") }
                    SearchField(value = text, onValueChange = { text = it; vm.setSearchText(it) })
                    val products by vm.filteredProducts.collectAsState()
                    ProductGrid(products = products, hasAnyProducts = true, onProductClick = {})
                }
            }
        }

        compose.waitUntilNodeCount(hasTestTag(UiTags.PRODUCT_CARD), 3, timeoutMillis = 5_000)

        compose.onNodeWithTag(UiTags.SEARCH_FIELD).performTextInput("mil")
        compose.waitUntilNodeCount(hasTestTag(UiTags.PRODUCT_CARD), 1, timeoutMillis = 5_000)
        compose.onNodeWithText("Milk").assertIsDisplayed()

        compose.onNodeWithTag(UiTags.SEARCH_FIELD).performTextReplacement("zzz")
        compose.waitUntilExactlyOneExists(hasTestTag(UiTags.EMPTY_STATE), timeoutMillis = 5_000)
    }
}
