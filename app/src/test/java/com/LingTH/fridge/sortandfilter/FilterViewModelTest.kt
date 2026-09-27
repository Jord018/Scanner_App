package com.LingTH.fridge.sortandfilter

import com.LingTH.fridge.testutil.FakeProductDao
import com.LingTH.fridge.testutil.MainDispatcherRule
import com.LingTH.fridge.testutil.product
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class FilterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val milk = product(id = 1, name = "Milk", categories = "en:dairies, en:milks", expiresInDays = 2)
    private val apple = product(id = 2, name = "apple", categories = "en:fruits, food", imageUrl = "content://img/1", expiresInDays = 10)
    private val water = product(id = 3, name = "Water", categories = "en:waters, en:beverages", expiresInDays = 300)
    private val chips = product(id = 4, name = "Chips", categories = "en:snacks", imageUrl = "https://x/y.jpg", expiresInDays = 30)

    private fun viewModel(vararg items: Databases.ProductData) =
        FilterViewModel(FakeProductDao(items.toList()))

    private fun FilterViewModel.names() = filteredProducts.value.map { it.product_name }

    @Test
    fun `shows every product when no filter is set`() {
        val vm = viewModel(milk, apple, water, chips)
        assertEquals(listOf("Milk", "apple", "Water", "Chips"), vm.names())
    }

    @Test
    fun `list updates when the database changes`() {
        val dao = FakeProductDao(listOf(milk))
        val vm = FilterViewModel(dao)
        dao.products.value = listOf(milk, apple)
        assertEquals(listOf("Milk", "apple"), vm.names())
    }

    @Test
    fun `search matches name case-insensitively`() {
        val vm = viewModel(milk, apple, water, chips)
        vm.setSearchText("MIL")
        assertEquals(listOf("Milk"), vm.names())
    }

    @Test
    fun `search also matches categories`() {
        val vm = viewModel(milk, apple, water, chips)
        vm.setSearchText("beverages")
        assertEquals(listOf("Water"), vm.names())
    }

    @Test
    fun `clearing search shows everything again`() {
        val vm = viewModel(milk, apple, water)
        vm.setSearchText("milk")
        vm.setSearchText("")
        assertEquals(3, vm.filteredProducts.value.size)
    }

    @Test
    fun `category filter matches part of a tag`() {
        val vm = viewModel(milk, apple, water, chips)
        vm.setSelectedCategory(listOf("snack"))
        assertEquals(listOf("Chips"), vm.names())
    }

    @Test
    fun `several categories are combined with OR`() {
        val vm = viewModel(milk, apple, water, chips)
        vm.setSelectedCategory(listOf("water", "food"))
        assertEquals(listOf("apple", "Water"), vm.names())
    }

    @Test
    fun `photo filter`() {
        val vm = viewModel(milk, apple, water, chips)
        vm.setSelectedAddedPhoto(listOf("Added Photo"))
        assertEquals(listOf("apple", "Chips"), vm.names())
        vm.setSelectedAddedPhoto(listOf("NO Photo"))
        assertEquals(listOf("Milk", "Water"), vm.names())
    }

    @Test
    fun `filters and search combine with AND`() {
        val vm = viewModel(milk, apple, water, chips)
        vm.setSelectedAddedPhoto(listOf("Added Photo"))
        vm.setSearchText("chip")
        assertEquals(listOf("Chips"), vm.names())
    }

    @Test
    fun `exact expiration date filter`() {
        val vm = viewModel(milk, apple, water)
        vm.setSelectedExpirationDate(listOf(apple.expiration_date!!))
        assertEquals(listOf("apple"), vm.names())
    }

    @Test
    fun `sort by name A-Z and Z-A ignores case`() {
        val vm = viewModel(milk, apple, water, chips)
        vm.setSortByName("Name (A-Z)")
        assertEquals(listOf("apple", "Chips", "Milk", "Water"), vm.names())
        vm.setSortByName("Name (Z-A)")
        assertEquals(listOf("Water", "Milk", "Chips", "apple"), vm.names())
    }

    @Test
    fun `clearing the sort restores database order`() {
        val vm = viewModel(water, milk, apple)
        vm.setSortByName("Name (A-Z)")
        vm.setSortByName("")
        assertEquals(listOf("Water", "Milk", "apple"), vm.names())
    }

    @Ignore("BUG: 'Expiration Date (Soonest)' only drops expired items; it never sorts")
    @Test
    fun `sort by soonest expiration puts the nearest date first`() {
        val vm = viewModel(water, chips, milk, apple)
        vm.setSortByDate("Expiration Date (Soonest)")
        assertEquals(listOf("Milk", "apple", "Chips", "Water"), vm.names())
    }

    @Ignore("BUG: 'Expiration Date (Latest)' sorts ascending, i.e. soonest first")
    @Test
    fun `sort by latest expiration puts the furthest date first`() {
        val vm = viewModel(milk, water, apple, chips)
        vm.setSortByDate("Expiration Date (Latest)")
        assertEquals(listOf("Water", "Chips", "apple", "Milk"), vm.names())
    }

    @Ignore("BUG: filterProducts() uses add_day!!, so one product with add_day = null crashes the whole list")
    @Test
    fun `product without an added date does not crash filtering`() {
        val noAddDay = product(id = 9, name = "Mystery", addedDaysAgo = null)
        val vm = viewModel(milk, noAddDay)
        assertEquals(listOf("Milk", "Mystery"), vm.names())
    }
}
