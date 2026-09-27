package com.LingTH.fridge.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("ExpiryWorker", "✅ BootReceiver is running...")

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // เรียกใช้หลังจาก boot เสร็จ
            scheduleDailyAlerts(context)
        }
    }

    private fun scheduleDailyAlerts(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // WorkManager keeps periodic work across reboots; this only fills in missing slots
                scheduleExpiryChecksFromSettings(context, replace = false)
            } finally {
                pending.finish()
            }
        }
    }
}
