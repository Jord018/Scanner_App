package com.LingTH.fridge.testutil

import Databases.ProductData
import InventoryDatabase
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import java.time.ZoneOffset

val appContext: Context get() = ApplicationProvider.getApplicationContext()

/** In-memory database so tests never touch the real "inventory_database" on the device. */
fun inMemoryDb(): InventoryDatabase =
    Room.inMemoryDatabaseBuilder(appContext, InventoryDatabase::class.java)
        .allowMainThreadQueries()
        .build()

fun utcDaysFromToday(days: Long): Long =
    LocalDate.now(ZoneOffset.UTC).plusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun product(
    id: Int = 0,
    name: String = "Milk",
    categories: String = "en:dairies",
    imageUrl: String = "",
    expiresInDays: Long? = 5,
    barcode: String = "885000000000$id",
) = ProductData(
    id = id,
    barcode = barcode,
    product_name = name,
    categories = categories,
    image_url = imageUrl,
    expiration_date = expiresInDays?.let { utcDaysFromToday(it) },
    add_day = System.currentTimeMillis(),
    notes = ""
)
