package com.LingTH.fridge.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Migration จากเวอร์ชัน 1 ไป 2
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS inspection_table (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                product_id INTEGER NOT NULL,
                inspection_date INTEGER NOT NULL,
                inspection_notes TEXT,
                FOREIGN KEY(product_id) REFERENCES product_table(id) ON DELETE CASCADE
            )
        """)
    }
}
// Room only keeps one migration per (start, end) pair, so 2 -> 3 must be a single object
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS settings_table (
                id INTEGER PRIMARY KEY NOT NULL,
                alertBeforeExpiry TEXT NOT NULL,
                alertMode TEXT NOT NULL,
                repeatAlert TEXT NOT NULL,
                email TEXT NOT NULL
            )
        """.trimIndent())

        // Same values as DefaultSettings / the Settings screen options
        database.execSQL("""
            INSERT OR IGNORE INTO settings_table (id, alertBeforeExpiry, alertMode, repeatAlert, email)
            VALUES (0, '3 days', 'Normal', '4', '')
        """.trimIndent())
    }
}
