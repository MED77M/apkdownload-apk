package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.json.JSONObject

/**
 * Ensures Firebase connects immediately to the target project.
 * Supports dynamically switching to a new Firebase database at runtime
 * via custom options or by parsing google-services.json content.
 */
object FirebaseInitializer {

    private const val TAG = "FirebaseInitializer"
    private const val PREFS_NAME = "firebase_project_config"
    private const val KEY_CUSTOM_ACTIVE = "custom_active"
    private const val KEY_PROJECT_ID = "project_id"
    private const val KEY_APPLICATION_ID = "application_id"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_STORAGE_BUCKET = "storage_bucket"
    private const val KEY_GCM_SENDER_ID = "gcm_sender_id"

    // Default configuration (User's Firebase project: slcenter-ad48a)
    private const val DEFAULT_PROJECT_ID = "slcenter-ad48a"
    private const val DEFAULT_APPLICATION_ID = "1:642092785294:android:9b57a4b683c7b26115f3df"
    private const val DEFAULT_API_KEY = "AIzaSyDwglzdLjNZTIuyIGsxM3KQOYwwRC7mZTM"
    private const val DEFAULT_STORAGE_BUCKET = "slcenter-ad48a.firebasestorage.app"
    private const val DEFAULT_GCM_SENDER_ID = "642092785294"

    fun getActiveProjectId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.getBoolean(KEY_CUSTOM_ACTIVE, false)) {
            prefs.getString(KEY_PROJECT_ID, DEFAULT_PROJECT_ID) ?: DEFAULT_PROJECT_ID
        } else {
            DEFAULT_PROJECT_ID
        }
    }

    fun isUsingCustomProject(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_CUSTOM_ACTIVE, false)
    }

    /**
     * Initializes Firebase immediately with custom options if configured,
     * or standard defaults.
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

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isCustom = prefs.getBoolean(KEY_CUSTOM_ACTIVE, false)

        val projectId = if (isCustom) prefs.getString(KEY_PROJECT_ID, DEFAULT_PROJECT_ID) ?: DEFAULT_PROJECT_ID else DEFAULT_PROJECT_ID
        val appId = if (isCustom) prefs.getString(KEY_APPLICATION_ID, DEFAULT_APPLICATION_ID) ?: DEFAULT_APPLICATION_ID else DEFAULT_APPLICATION_ID
        val apiKey = if (isCustom) prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY else DEFAULT_API_KEY
        val storageBucket = if (isCustom) prefs.getString(KEY_STORAGE_BUCKET, DEFAULT_STORAGE_BUCKET) ?: DEFAULT_STORAGE_BUCKET else DEFAULT_STORAGE_BUCKET
        val gcmSenderId = if (isCustom) prefs.getString(KEY_GCM_SENDER_ID, DEFAULT_GCM_SENDER_ID) ?: DEFAULT_GCM_SENDER_ID else DEFAULT_GCM_SENDER_ID

        return try {
            val options = FirebaseOptions.Builder()
                .setProjectId(projectId)
                .setApplicationId(appId)
                .setApiKey(apiKey)
                .setStorageBucket(storageBucket)
                .setGcmSenderId(gcmSenderId)
                .build()

            val app = FirebaseApp.initializeApp(appContext, options)
            Log.i(TAG, "Firebase initialized successfully for project: $projectId (custom=$isCustom)")
            app
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase with options for project $projectId", e)
            null
        }
    }

    /**
     * Switches the active Firebase project dynamically.
     * Deletes the current FirebaseApp instance, stores the new credentials,
     * and initializes the new connection immediately.
     */
    fun switchFirebaseProject(
        context: Context,
        projectId: String,
        apiKey: String,
        appId: String,
        storageBucket: String = "${projectId.trim()}.firebasestorage.app",
        gcmSenderId: String = ""
    ): Result<FirebaseApp> {
        val appContext = context.applicationContext ?: context
        return try {
            val cleanProjectId = projectId.trim()
            val cleanApiKey = apiKey.trim()
            val cleanAppId = appId.trim()
            val cleanStorageBucket = storageBucket.trim().ifEmpty { "${cleanProjectId}.firebasestorage.app" }
            val cleanGcmSenderId = gcmSenderId.trim()

            if (cleanProjectId.isEmpty() || cleanApiKey.isEmpty() || cleanAppId.isEmpty()) {
                return Result.failure(Exception("Project ID, API Key, and App ID cannot be empty."))
            }

            // Delete all current FirebaseApp instances to disconnect old database
            try {
                val apps = ArrayList(FirebaseApp.getApps(appContext))
                for (app in apps) {
                    app.delete()
                    Log.i(TAG, "Deleted old FirebaseApp instance: ${app.name}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting old FirebaseApp: ${e.message}")
            }

            // Save new configuration in SharedPreferences
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_CUSTOM_ACTIVE, true)
                .putString(KEY_PROJECT_ID, cleanProjectId)
                .putString(KEY_API_KEY, cleanApiKey)
                .putString(KEY_APPLICATION_ID, cleanAppId)
                .putString(KEY_STORAGE_BUCKET, cleanStorageBucket)
                .putString(KEY_GCM_SENDER_ID, cleanGcmSenderId)
                .apply()

            // Build new options
            val optionsBuilder = FirebaseOptions.Builder()
                .setProjectId(cleanProjectId)
                .setApiKey(cleanApiKey)
                .setApplicationId(cleanAppId)
                .setStorageBucket(cleanStorageBucket)

            if (cleanGcmSenderId.isNotEmpty()) {
                optionsBuilder.setGcmSenderId(cleanGcmSenderId)
            }

            val newApp = FirebaseApp.initializeApp(appContext, optionsBuilder.build())
            Log.i(TAG, "Successfully connected to new Firebase project: $cleanProjectId")
            Result.success(newApp)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch Firebase project", e)
            Result.failure(e)
        }
    }

    /**
     * Parses the JSON content of a google-services.json file and applies it immediately.
     */
    fun parseAndApplyGoogleServicesJson(context: Context, jsonStr: String): Result<String> {
        return try {
            val cleanJson = jsonStr.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val root = JSONObject(cleanJson)
            val projectInfo = root.getJSONObject("project_info")
            val projectId = projectInfo.getString("project_id")
            val projectNumber = projectInfo.optString("project_number", "")
            val storageBucket = projectInfo.optString("storage_bucket", "${projectId}.firebasestorage.app")

            val clients = root.getJSONArray("client")
            if (clients.length() == 0) {
                return Result.failure(Exception("No client configuration found in google-services.json"))
            }

            var selectedClient: JSONObject? = null
            val currentPkg = context.packageName

            for (i in 0 until clients.length()) {
                val c = clients.getJSONObject(i)
                val clientInfo = c.optJSONObject("client_info")
                val pkgName = clientInfo?.optJSONObject("android_client_info")?.optString("package_name", "")
                if (pkgName == currentPkg) {
                    selectedClient = c
                    break
                }
            }
            if (selectedClient == null) {
                selectedClient = clients.getJSONObject(0)
            }

            val clientInfo = selectedClient.getJSONObject("client_info")
            val appId = clientInfo.getString("mobilesdk_app_id")

            val apiKeys = selectedClient.getJSONArray("api_key")
            if (apiKeys.length() == 0) {
                return Result.failure(Exception("No API key found in google-services.json"))
            }
            val apiKey = apiKeys.getJSONObject(0).getString("current_key")

            val switchResult = switchFirebaseProject(
                context = context,
                projectId = projectId,
                apiKey = apiKey,
                appId = appId,
                storageBucket = storageBucket,
                gcmSenderId = projectNumber
            )

            if (switchResult.isSuccess) {
                Result.success(projectId)
            } else {
                Result.failure(switchResult.exceptionOrNull() ?: Exception("Unknown error initializing new project"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Invalid google-services.json format: ${e.message}"))
        }
    }

    /**
     * Resets the Firebase configuration back to the original default project.
     */
    fun resetToDefault(context: Context): Result<FirebaseApp> {
        val appContext = context.applicationContext ?: context
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        try {
            val apps = ArrayList(FirebaseApp.getApps(appContext))
            for (app in apps) {
                app.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting old apps during reset: ${e.message}")
        }

        val app = initialize(appContext)
        return if (app != null) Result.success(app) else Result.failure(Exception("Failed to re-initialize default project"))
    }
}
