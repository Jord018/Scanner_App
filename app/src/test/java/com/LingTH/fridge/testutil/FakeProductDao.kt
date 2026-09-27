package com.LingTH.fridge.testutil

import Databases.InspectionData
import Databases.ProductDao
import Databases.ProductData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneOffset

/** In-memory ProductDao that mimics Room's IGNORE-on-conflict insert. */
class FakeProductDao(initial: List<ProductData> = emptyList()) : ProductDao {
    val products = MutableStateFlow(initial)
    var failWrites = false
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override suspend fun insertProduct(product: ProductData): Long {
        if (failWrites) throw IllegalStateException("write failed")
        val withId = if (product.id == 0) product.copy(id = nextId++) else product
        if (products.value.any { it.id == withId.id }) return -1L // IGNORE
        products.value = products.value + withId
        return withId.id.toLong()
    }

    override suspend fun insertInspection(inspection: InspectionData) = Unit
    override fun getAllProducts(): Flow<List<ProductData>> = products
    override fun getInspectionsByProductId(productId: Int): Flow<List<InspectionData>> =
        MutableStateFlow(emptyList())

    override fun getInspectionCount(): Int = 0
    override suspend fun getAllProductsOnce(): List<ProductData> = products.value
    override suspend fun getProductByBarcode(barcode: String): ProductData? =
        products.value.firstOrNull { it.barcode == barcode }

    override fun getAllCategories(): Flow<List<String>> =
        products.map { list -> list.map { it.categories }.distinct() }

    override fun getWithPhotos(): Flow<List<ProductData>> =
        products.map { list -> list.filter { it.image_url.isNotBlank() } }

    override fun getWithoutPhotos(): Flow<List<ProductData>> =
        products.map { list -> list.filter { it.image_url.isBlank() } }

    override suspend fun deleteById(id: Int) {
        if (failWrites) throw IllegalStateException("write failed")
        products.value = products.value.filterNot { it.id == id }
    }

    override fun getAllProductNames(): Flow<List<String>> =
        products.map { list -> list.map { it.product_name }.distinct().sorted() }

    override fun getAllExpirationDates(): Flow<List<Long>> =
        products.map { list -> list.mapNotNull { it.expiration_date } }

    override fun getAllAddedDates(): Flow<List<Long>> =
        products.map { list -> list.mapNotNull { it.add_day } }

    override suspend fun updateProduct(product: ProductData) {
        if (failWrites) throw IllegalStateException("write failed")
        products.value = products.value.map { if (it.id == product.id) product else it }
    }
}

/** Epoch millis for UTC midnight [days] from today, matching daysUntilExpiry()'s UTC math. */
fun utcDaysFromToday(days: Long): Long =
    LocalDate.now(ZoneOffset.UTC).plusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun product(
    id: Int = 0,
    name: String = "Milk",
    categories: String = "dairy",
    imageUrl: String = "",
    expiresInDays: Long? = 5,
    addedDaysAgo: Long? = 0,
    barcode: String = "885000000000$id",
) = ProductData(
    id = id,
    barcode = barcode,
    product_name = name,
    categories = categories,
    image_url = imageUrl,
    expiration_date = expiresInDays?.let { utcDaysFromToday(it) },
    add_day = addedDaysAgo?.let { System.currentTimeMillis() - it * 24 * 60 * 60 * 1000 },
    notes = ""
)
