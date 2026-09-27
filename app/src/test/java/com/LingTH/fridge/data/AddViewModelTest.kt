package com.LingTH.fridge.data

import Databases.Addviewmodel
import com.LingTH.fridge.testutil.FakeProductDao
import com.LingTH.fridge.testutil.MainDispatcherRule
import com.LingTH.fridge.testutil.product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `saveProduct writes to the database and calls onSaved`() {
        val dao = FakeProductDao()
        val vm = Addviewmodel(dao)
        var saved = false

        vm.saveProduct(
            barcode = "8850000000001", name = "Milk", categories = "dairy", imageUrl = "",
            add_day = 1_000L, expie_day = 2_000L, notes = "top shelf",
            onSaved = { saved = true }
        )

        assertTrue(saved)
        assertTrue(vm.saveCompleted.value)
        val stored = dao.products.value.single()
        assertEquals("Milk", stored.product_name)
        assertEquals(2_000L, stored.expiration_date)
        assertEquals(1_000L, stored.add_day)
        assertEquals("top shelf", stored.notes)
    }

    @Test
    fun `saveProduct does not call onSaved when the write fails`() {
        val dao = FakeProductDao().apply { failWrites = true }
        val vm = Addviewmodel(dao)
        var saved = false

        vm.saveProduct("1", "Milk", "dairy", "", 1L, 2L, "", onSaved = { saved = true })

        assertFalse(saved)
        assertFalse(vm.saveCompleted.value)
    }

    @Test
    fun `resetSaveFlag clears the completed flag`() {
        val vm = Addviewmodel(FakeProductDao())
        vm.saveProduct("1", "Milk", "dairy", "", 1L, 2L, "", onSaved = {})
        vm.resetSaveFlag()
        assertFalse(vm.saveCompleted.value)
    }

    @Test
    fun `updateProduct keeps the id and replaces fields`() {
        val dao = FakeProductDao(listOf(product(id = 5, name = "Old")))
        val vm = Addviewmodel(dao)
        var updated = false

        vm.updateProduct(
            id = 5, barcode = "b", name = "New", categories = "c", imageUrl = "",
            add_day = 10L, expie_day = 20L, notes = "n",
            onUpdated = { updated = true }, onError = {}
        )

        assertTrue(updated)
        val stored = dao.products.value.single()
        assertEquals(5, stored.id)
        assertEquals("New", stored.product_name)
        assertEquals(20L, stored.expiration_date)
    }

    @Test
    fun `updateProduct reports errors`() {
        val dao = FakeProductDao(listOf(product(id = 5))).apply { failWrites = true }
        val vm = Addviewmodel(dao)
        var error = false

        vm.updateProduct(5, "b", "New", "c", "", 1L, 2L, "", onUpdated = {}, onError = { error = true })

        assertTrue(error)
    }

    @Test
    fun `deleteProductById removes only that product`() {
        val dao = FakeProductDao(listOf(product(id = 1, name = "A"), product(id = 2, name = "B")))
        Addviewmodel(dao).deleteProductById(1)
        assertEquals(listOf("B"), dao.products.value.map { it.product_name })
    }
}
