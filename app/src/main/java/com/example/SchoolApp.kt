package com.example

import android.app.Application
import android.content.Context
import com.example.data.firebase.FirebaseInitializer
import com.example.data.notification.BadgeManager
import com.example.data.notification.SchoolBackgroundNotificationService
import com.example.data.notification.SchoolNotificationManager

class SchoolApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.initialize(this)
        SchoolNotificationManager.initialize(this)
        BadgeManager.initialize(this)

        // If user is already logged in, keep the background notification service running
        val prefs = getSharedPreferences("school_prefs", Context.MODE_PRIVATE)
        val cachedUserId = prefs.getString("cached_user_id", null)
        if (!cachedUserId.isNullOrBlank()) {
            SchoolBackgroundNotificationService.startService(this)
        }
    }
}

