package com.LingTH.fridge.Notification

import ExpiryCheckWorker
import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.LingTH.fridge.sortandfilter.Setting.DefaultSettings
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

// Highest "Repeat Alert" option in Settings
private const val MAX_ALERTS_PER_DAY = 8

private fun workName(slot: Int) = "expiry_check_$slot"

/**
 * Runs ExpiryCheckWorker once a day at each of [alertsPerDay] time slots.
 * [replace] = true reschedules existing slots (after Settings change); false keeps them.
 */
fun scheduleExpiryChecks(context: Context, alertsPerDay: Int, replace: Boolean) {
    val count = alertsPerDay.coerceIn(1, MAX_ALERTS_PER_DAY)
    val workManager = WorkManager.getInstance(context)
    val policy = if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP
    val now = LocalDateTime.now()

    AlertTimeManager(count).timeSlots.forEachIndexed { slot, time ->
        val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNext(time, now), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(workName(slot), policy, request)
    }
    for (slot in count until MAX_ALERTS_PER_DAY) {
        workManager.cancelUniqueWork(workName(slot))
    }
    Log.d("ExpiryScheduler", "⏰ $count daily check(s) scheduled (replace=$replace)")
}

suspend fun scheduleExpiryChecksFromSettings(context: Context, replace: Boolean) {
    val settings = InventoryDatabase.getDatabase(context).settingsDao().getSettings() ?: DefaultSettings
    scheduleExpiryChecks(context, settings.repeatAlert.toIntOrNull() ?: 1, replace)
}

/** Milliseconds from [now] to the next [time] today, or tomorrow if it has passed. */
fun millisUntilNext(time: LocalTime, now: LocalDateTime): Long {
    var target = now.toLocalDate().atTime(time)
    if (!target.isAfter(now)) target = target.plusDays(1)
    return Duration.between(now, target).toMillis()
}
