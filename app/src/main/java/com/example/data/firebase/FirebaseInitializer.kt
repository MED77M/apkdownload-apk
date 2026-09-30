package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Ensures Firebase connects immediately to the project defined in google-services.json
 * upon application startup.
 */
object FirebaseInitializer {

    private const val TAG = "FirebaseInitializer"

    // Configuration from google-services.json
    private const val PROJECT_ID = "testapp-3cd3b"
    private const val APPLICATION_ID = "1:261150960996:android:e875579248b48f7a501e71"
    private const val API_KEY = "AIzaSyDgX1lCRUI7qYuhPNKmShiZSHwDo4pSSXA"
    private const val STORAGE_BUCKET = "testapp-3cd3b.firebasestorage.app"
    private const val GCM_SENDER_ID = "261150960996"

    /**
     * Initializes Firebase immediately on startup with multiple fallback mechanisms
     * to guarantee connection to the target project.
     */
    fun initialize(context: Context): FirebaseApp? {
        val appContext = context.applicationContext ?: context

        // 1. Check if an instance is already initialized
        try {
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                val existingApp = FirebaseApp.getInstance()
                Log.d(TAG, "Firebase already initialized: ${existingApp.name}, project: ${existingApp.options.projectId}")
                return existingApp
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking existing Firebase apps: ${e.message}")
        }

        // 2. Try standard automatic initialization (Google Services plugin provider)
        try {
            val app = FirebaseApp.initializeApp(appContext)
            if (app != null) {
                Log.i(TAG, "Firebase initialized via default provider for project: ${app.options.projectId}")
                return app
            }
        } catch (e: Exception) {
            Log.w(TAG, "Default Firebase initialization did not succeed: ${e.message}")
        }

        // 3. Try resource-based options from generated strings.xml (google-services.json)
        try {
            val resOptions = FirebaseOptions.fromResource(appContext)
            if (resOptions != null) {
                val app = FirebaseApp.initializeApp(appContext, resOptions)
                Log.i(TAG, "Firebase initialized via resource options for project: ${resOptions.projectId}")
                return app
            }
        } catch (e: Exception) {
            Log.w(TAG, "Resource-based Firebase initialization failed: ${e.message}")
        }

        // 4. Fallback to explicit options from google-services.json
        return try {
            val options = FirebaseOptions.Builder()
                .setProjectId(PROJECT_ID)
                .setApplicationId(APPLICATION_ID)
                .setApiKey(API_KEY)
                .setStorageBucket(STORAGE_BUCKET)
                .setGcmSenderId(GCM_SENDER_ID)
                .build()

            val app = FirebaseApp.initializeApp(appContext, options)
            Log.i(TAG, "Firebase initialized successfully with explicit google-services.json options: $PROJECT_ID")
            app
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase with explicit options", e)
            null
        }
    }
}
