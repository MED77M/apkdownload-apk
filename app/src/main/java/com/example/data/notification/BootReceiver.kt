package com.example.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("BootReceiver", "Received broadcast action: $action")
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = context.getSharedPreferences("school_prefs", Context.MODE_PRIVATE)
            val cachedUserId = prefs.getString("cached_user_id", null)
            if (!cachedUserId.isNullOrBlank()) {
                Log.d("BootReceiver", "User session active, starting background notification service...")
                SchoolBackgroundNotificationService.startService(context)
            }
        }
    }
}
