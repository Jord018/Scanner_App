package com.LingTH.fridge.ui.theme

import androidx.compose.ui.graphics.Color

enum class ExpiryLevel { FRESH, SOON, EXPIRED, UNKNOWN }

data class ExpiryStatus(val level: ExpiryLevel, val label: String, val color: Color)

// Items expiring within this many days are shown as "soon"
const val EXPIRY_SOON_DAYS = 3L

fun expiryStatus(daysLeft: Long?): ExpiryStatus = when {
    daysLeft == null -> ExpiryStatus(ExpiryLevel.UNKNOWN, "No date", ExpiryUnknown)
    daysLeft < 0 -> ExpiryStatus(ExpiryLevel.EXPIRED, "Expired", ExpiryExpired)
    daysLeft == 0L -> ExpiryStatus(ExpiryLevel.SOON, "Expires today", ExpirySoon)
    daysLeft <= EXPIRY_SOON_DAYS -> ExpiryStatus(ExpiryLevel.SOON, "$daysLeft day(s) left", ExpirySoon)
    else -> ExpiryStatus(ExpiryLevel.FRESH, "$daysLeft day(s) left", ExpiryFresh)
}
