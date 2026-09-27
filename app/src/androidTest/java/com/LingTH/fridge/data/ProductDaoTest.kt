package com.LingTH.fridge.data

import Databases.InspectionData
import Databases.ProductDao
import InventoryDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.LingTH.fridge.testutil.inMemoryDb
import com.LingTH.fridge.testutil.product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductDaoTest {

    private lateinit var db: InventoryDatabase
    private lateinit var dao: ProductDao

    @Before
    fun setUp() {
        db = inMemoryDb()
        dao = db.productDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertAndReadBack() = runTest {
        dao.insertProduct(product(name = "Milk", barcode = "111"))

        val all = dao.getAllProducts().first()
        assertEquals(1, all.size)
        assertEquals("Milk", all[0].product_name)
        assertTrue("id is auto-generated", all[0].id > 0)
    }

    @Test
    fun getProductByBarcode() = runTest {
        dao.insertProduct(product(name = "Milk", barcode = "111"))
        dao.insertProduct(product(name = "Water", barcode = "222"))

        assertEquals("Water", dao.getProductByBarcode("222")?.product_name)
        assertNull(dao.getProductByBarcode("999"))
    }

    @Test
    fun sameBarcodeCanBeSavedTwice() = runTest {
        // Scanning the same item twice should give two rows (id is the key, not barcode)
        dao.insertProduct(product(name = "Milk", barcode = "111"))
        dao.insertProduct(product(name = "Milk", barcode = "111"))

        assertEquals(2, dao.getAllProductsOnce().size)
    }

    @Test
    fun insertWithExistingIdIsSilentlyIgnored() = runTest {
        // OnConflictStrategy.IGNORE: no exception, nothing written, returns -1
        // (Addviewmodel.saveProduct() turns that into onError)
        assertEquals(7L, dao.insertProduct(product(id = 7, name = "Original")))
        assertEquals(-1L, dao.insertProduct(product(id = 7, name = "Duplicate")))

        val all = dao.getAllProductsOnce()
        assertEquals(1, all.size)
        assertEquals("Original", all[0].product_name)
    }

    @Test
    fun updateProduct() = runTest {
        dao.insertProduct(product(name = "Old"))
        val stored = dao.getAllProductsOnce().single()

        dao.updateProduct(stored.copy(product_name = "New", notes = "edited"))

        val updated = dao.getAllProductsOnce().single()
        assertEquals(stored.id, updated.id)
        assertEquals("New", updated.product_name)
        assertEquals("edited", updated.notes)
    }

    @Test
    fun deleteById() = runTest {
        dao.insertProduct(product(name = "A"))
        dao.insertProduct(product(name = "B"))
        val a = dao.getAllProductsOnce().first { it.product_name == "A" }

        dao.deleteById(a.id)

        assertEquals(listOf("B"), dao.getAllProductsOnce().map { it.product_name })
    }

    @Test
    fun categoriesAreDistinct() = runTest {
        dao.insertProduct(product(name = "A", categories = "dairy"))
        dao.insertProduct(product(name = "B", categories = "dairy"))
        dao.insertProduct(product(name = "C", categories = "snack"))

        assertEquals(setOf("dairy", "snack"), dao.getAllCategories().first().toSet())
    }

    @Test
    fun productNamesAreSortedAndDistinct() = runTest {
        dao.insertProduct(product(name = "Water"))
        dao.insertProduct(product(name = "Apple"))
        dao.insertProduct(product(name = "Water"))

        assertEquals(listOf("Apple", "Water"), dao.getAllProductNames().first())
    }

    @Test
    fun expirationDatesSkipProductsWithoutOne() = runTest {
        dao.insertProduct(product(name = "A", expiresInDays = 3))
        dao.insertProduct(product(name = "B", expiresInDays = null))

        assertEquals(1, dao.getAllExpirationDates().first().size)
    }

    @Test
    fun deletingProductCascadesToInspections() = runTest {
        dao.insertProduct(product(name = "A"))
        val a = dao.getAllProductsOnce().single()
        dao.insertInspection(InspectionData(productId = a.id, inspectionDate = 1L, inspectionNotes = null))
        assertNotNull(dao.getInspectionsByProductId(a.id).first().singleOrNull())

        dao.deleteById(a.id) // Room turns on foreign_keys for databases with FK entities

        assertTrue(dao.getInspectionsByProductId(a.id).first().isEmpty())
    }
}
