package com.example

import android.app.Application
import com.example.data.firebase.FirebaseInitializer

class SchoolApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.initialize(this)
    }
}
