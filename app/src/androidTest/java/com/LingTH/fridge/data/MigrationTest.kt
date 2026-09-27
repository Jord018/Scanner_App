package com.LingTH.fridge.data

import InventoryDatabase
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.LingTH.fridge.migration.MIGRATION_1_2
import com.LingTH.fridge.migration.MIGRATION_2_3
import com.LingTH.fridge.migration.MIGRATION_2_3_TO_3
import com.LingTH.fridge.testutil.appContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Builds old-version database files by hand (no exported schemas exist) and opens them
 * with the app's real migrations. Uses its own file name, never "inventory_database".
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @Before
    @After
    fun deleteFile() {
        appContext.deleteDatabase(dbName)
    }

    private val productTableSql = """
        CREATE TABLE IF NOT EXISTS product_table (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            barcode TEXT NOT NULL,
            product_name TEXT NOT NULL,
            categories TEXT NOT NULL,
            image_url TEXT NOT NULL,
            expiration_date INTEGER,
            add_day INTEGER,
            notes TEXT NOT NULL
        )
    """

    private val inspectionTableSql = """
        CREATE TABLE IF NOT EXISTS inspection_table (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            product_id INTEGER NOT NULL,
            inspection_date INTEGER NOT NULL,
            inspection_notes TEXT,
            FOREIGN KEY(product_id) REFERENCES product_table(id) ON DELETE CASCADE
        )
    """

    private fun createOldDatabase(version: Int, vararg sql: String) {
        val file = appContext.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            sql.forEach { db.execSQL(it) }
            db.execSQL(
                "INSERT INTO product_table (barcode, product_name, categories, image_url, expiration_date, add_day, notes) " +
                    "VALUES ('111', 'Old milk', 'dairy', '', 1000, 500, '')"
            )
            db.version = version
        }
    }

    // Same migration list as InventoryDatabase.getDatabase()
    private fun openWithAppMigrations(): InventoryDatabase =
        Room.databaseBuilder(appContext, InventoryDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_2_3_TO_3)
            .allowMainThreadQueries()
            .build()

    @Test
    fun migrateFromVersion2KeepsProductsAndSeedsSettings() = runTest {
        createOldDatabase(2, productTableSql, inspectionTableSql)

        val db = openWithAppMigrations()
        try {
            val products = db.productDao().getAllProducts().first()
            assertEquals(listOf("Old milk"), products.map { it.product_name })

            val settings = db.settingsDao().getSettings()!!
            assertEquals("ปกติ", settings.alertMode)
            assertEquals("ก่อน 1 วัน", settings.alertBeforeExpiry)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrateFromVersion1RunsBothMigrations() = runTest {
        createOldDatabase(1, productTableSql)

        val db = openWithAppMigrations()
        try {
            assertEquals(1, db.productDao().getAllProductsOnce().size)
            assertEquals(0, db.productDao().getInspectionCount())
        } finally {
            db.close()
        }
    }
}
