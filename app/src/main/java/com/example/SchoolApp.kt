package com.example

import android.app.Application
import com.example.data.firebase.FirebaseInitializer
import com.example.data.notification.SchoolNotificationManager

class SchoolApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.initialize(this)
        SchoolNotificationManager.initialize(this)
    }
}
