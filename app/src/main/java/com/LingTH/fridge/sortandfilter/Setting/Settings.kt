package com.LingTH.fridge.sortandfilter.Setting

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings_table")
data class Settings(
    @PrimaryKey val id: Int = 0,  // ใช้ id เดียวเสมอ เพื่อให้มีแค่แถวเดียว
    val alertBeforeExpiry: String,
    val alertMode: String,
    val repeatAlert: String,
    val email: String
)

// Used until the user saves Settings for the first time; matches the Settings screen defaults
val DefaultSettings = Settings(
    id = 0,
    alertBeforeExpiry = "3 days",
    alertMode = "Normal",
    repeatAlert = "4",
    email = ""
)
