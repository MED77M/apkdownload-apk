package com.example.data.firebase

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.example.data.local.LocalDataStore
import com.example.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class FirebaseManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("science_est_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "FirebaseManager"
        private const val PREF_REMEMBER_ME = "remember_me"
        private const val PREF_LAST_USERNAME = "last_username"
        private const val PREF_CACHED_USER_ID = "cached_user_id"
        private const val PREF_LANGUAGE = "selected_language"

        @Volatile
        private var instance: FirebaseManager? = null

        fun getInstance(context: Context): FirebaseManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseManager(context.applicationContext).also { instance = it }
            }
        }
    }

    val isFirebaseInitialized: Boolean
        get() = ensureFirebaseInitialized()

    fun ensureFirebaseInitialized(): Boolean {
        if (try { FirebaseApp.getApps(context).isNotEmpty() } catch (e: Exception) { false }) {
            return true
        }
        return FirebaseInitializer.initialize(context) != null
    }

    private val auth: FirebaseAuth?
        get() = if (ensureFirebaseInitialized()) FirebaseAuth.getInstance() else null

    private val firestore: FirebaseFirestore?
        get() = if (ensureFirebaseInitialized()) FirebaseFirestore.getInstance() else null

    private val storage: FirebaseStorage?
        get() = if (ensureFirebaseInitialized()) FirebaseStorage.getInstance() else null

    var currentUser: SchoolUser? = null
        private set

    fun getSavedLanguage(): String {
        return prefs.getString(PREF_LANGUAGE, "ar") ?: "ar"
    }

    fun saveLanguage(code: String) {
        prefs.edit().putString(PREF_LANGUAGE, code).apply()
    }

    fun isRememberMe(): Boolean {
        return prefs.getBoolean(PREF_REMEMBER_ME, false)
    }

    fun getLastUsername(): String {
        return prefs.getString(PREF_LAST_USERNAME, "") ?: ""
    }

    fun setRememberMe(remember: Boolean, username: String) {
        prefs.edit()
            .putBoolean(PREF_REMEMBER_ME, remember)
            .putString(PREF_LAST_USERNAME, if (remember) username else "")
            .apply()
    }

    fun isBrandNewResetDone(): Boolean {
        return prefs.getBoolean("brand_new_reset_performed_v1", false)
    }

    fun setBrandNewResetDone() {
        prefs.edit().putBoolean("brand_new_reset_performed_v1", true).apply()
    }

    private fun normalizePassword(password: String): String {
        val trimmed = password.trim()
        return if (trimmed.length < 6) "${trimmed}__school_est" else trimmed
    }

    private fun normalizeEmail(username: String): String {
        val clean = username.trim().lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .ifBlank { "user_${System.currentTimeMillis()}" }
        return "${clean}_school@science.est.ma"
    }

    private suspend fun ensureAuthSession() {
        val a = auth ?: return
        if (a.currentUser == null) {
            try {
                a.signInAnonymously().await()
            } catch (e: Exception) {
                Log.w(TAG, "Anonymous session note: ${e.message}")
            }
        }
    }

    /**
     * Initializes default admin account (username: "admin", password: "admin")
     * in Firebase Auth and Firestore if not already present.
     */
    suspend fun initializeDefaultAdminAccount(): Result<SchoolUser> {
        val db = firestore ?: return Result.failure(Exception("Firestore is not initialized"))

        val uid = auth?.currentUser?.uid ?: "admin_fixed"
        val adminUser = SchoolUser(
            id = uid,
            username = "admin",
            fullName = "مدير النظام",
            role = Role.ADMIN,
            phone = "",
            isActive = true,
            isPrimaryAdmin = true,
            needsPasswordChange = true,
            recoveryEmail = ""
        )

        try {
            db.collection("users").document(uid).set(adminUser.toMap()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Could not write to users/$uid directly: ${e.message}")
        }

        try {
            if (uid != "admin_fixed") {
                db.collection("users").document("admin_fixed").set(adminUser.toMap()).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not write to users/admin_fixed: ${e.message}")
        }

        currentUser = adminUser
        prefs.edit().putString(PREF_CACHED_USER_ID, adminUser.id).apply()
        return Result.success(adminUser)
    }

    /**
     * Resets or restores admin credentials to (username: "admin", password: "admin").
     * Sets role = ADMIN in Firestore and logs the admin in immediately.
     */
    suspend fun resetAdminCredentials(): Result<SchoolUser> {
        val uid = auth?.currentUser?.uid ?: "admin_fixed"
        val adminUser = SchoolUser(
            id = uid,
            username = "admin",
            fullName = "مدير النظام",
            role = Role.ADMIN,
            phone = "0550000000",
            isActive = true,
            isPrimaryAdmin = true,
            needsPasswordChange = false,
            recoveryEmail = "admin@school.app"
        )

        // Asynchronous non-blocking Firestore write
        firestore?.let { db ->
            try {
                db.collection("users").document(uid).set(adminUser.toMap())
                if (uid != "admin_fixed") {
                    db.collection("users").document("admin_fixed").set(adminUser.toMap())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Non-blocking admin write notice: ${e.message}")
            }
        }

        currentUser = adminUser
        prefs.edit().putString(PREF_CACHED_USER_ID, adminUser.id).apply()
        return Result.success(adminUser)
    }

    fun getActiveProjectId(): String {
        return FirebaseInitializer.getActiveProjectId(context)
    }

    fun isUsingCustomDatabase(): Boolean {
        return ensureFirebaseInitialized()
    }

    /**
     * Connects to a new Firebase database project dynamically.
     * Deletes the old database connection and initializes the admin account in the new project.
     */
    suspend fun connectNewDatabase(
        projectId: String,
        apiKey: String,
        appId: String,
        storageBucket: String = "",
        gcmSenderId: String = ""
    ): Result<SchoolUser> {
        val switchRes = FirebaseInitializer.switchFirebaseProject(
            context = context,
            projectId = projectId,
            apiKey = apiKey,
            appId = appId,
            storageBucket = storageBucket,
            gcmSenderId = gcmSenderId
        )
        if (switchRes.isFailure) {
            return Result.failure(switchRes.exceptionOrNull() ?: Exception("Failed to switch database"))
        }

        // Clear session from old database
        currentUser = null
        prefs.edit().remove(PREF_CACHED_USER_ID).apply()

        // Create default admin account in the new database
        return resetAdminCredentials()
    }

    /**
     * Connects to a new Firebase project using google-services.json content.
     */
    suspend fun connectWithGoogleServicesJson(jsonContent: String): Result<SchoolUser> {
        val parseRes = FirebaseInitializer.parseAndApplyGoogleServicesJson(context, jsonContent)
        if (parseRes.isFailure) {
            return Result.failure(parseRes.exceptionOrNull() ?: Exception("Failed to apply google-services.json"))
        }

        // Clear session from old database
        currentUser = null
        prefs.edit().remove(PREF_CACHED_USER_ID).apply()

        // Create default admin account in the new database
        return resetAdminCredentials()
    }

    /**
     * Resets the active database connection back to the default project.
     */
    suspend fun resetDatabaseToDefaultConfig(): Result<SchoolUser> {
        val res = FirebaseInitializer.resetToDefault(context)
        if (res.isFailure) {
            return Result.failure(res.exceptionOrNull() ?: Exception("Failed to reset to default project"))
        }
        currentUser = null
        prefs.edit().remove(PREF_CACHED_USER_ID).apply()
        return resetAdminCredentials()
    }

    /**
     * Completely wipes all data collections and non-admin users, restoring
     * the app to a brand new factory state with only the default admin (admin / admin).
     */
    suspend fun resetDatabaseToBrandNew(): Result<Unit> {
        if (!isUsingCustomDatabase()) {
            resetAdminCredentials()
            prefs.edit()
                .remove(PREF_CACHED_USER_ID)
                .remove(PREF_LAST_USERNAME)
                .putBoolean(PREF_REMEMBER_ME, false)
                .apply()
            currentUser = null
            try { auth?.signOut() } catch (_: Exception) {}
            return Result.success(Unit)
        }

        val db = firestore ?: return Result.failure(Exception("Firestore is not initialized"))

        val collections = listOf(
            "announcements",
            "attendance",
            "auditLogs",
            "conversations",
            "enrollments",
            "grades",
            "groups",
            "homework",
            "homework_submissions",
            "payments",
            "qa_posts",
            "resources",
            "rooms",
            "subjects",
            "teacher_shares",
            "timetable"
        )

        try {
            // 1. Wipe all data collections
            for (colName in collections) {
                try {
                    val snap = db.collection(colName).get().await()
                    for (doc in snap.documents) {
                        try {
                            if (colName == "conversations") {
                                val msgSnap = doc.reference.collection("messages").get().await()
                                for (m in msgSnap.documents) {
                                    m.reference.delete().await()
                                }
                            }
                            doc.reference.delete().await()
                        } catch (e: Exception) {
                            Log.w(TAG, "Error deleting doc ${doc.id} in $colName: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error clearing $colName: ${e.message}")
                }
            }

            // 2. Wipe all non-admin users
            val usersSnap = db.collection("users").get().await()
            for (uDoc in usersSnap.documents) {
                val uname = uDoc.getString("username")?.lowercase()?.trim() ?: ""
                val isAdm = uDoc.getBoolean("isPrimaryAdmin") ?: false
                if (uname != "admin" && !isAdm && uDoc.id != "admin_fixed") {
                    try {
                        uDoc.reference.delete().await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error deleting user ${uDoc.id}: ${e.message}")
                    }
                }
            }

            // 3. Re-initialize clean default admin
            resetAdminCredentials()

            // 4. Clear local user preferences
            prefs.edit()
                .remove(PREF_CACHED_USER_ID)
                .remove(PREF_LAST_USERNAME)
                .putBoolean(PREF_REMEMBER_ME, false)
                .apply()
            currentUser = null
            auth?.signOut()

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset database", e)
            return Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<SchoolUser> {
        val trimmedUsername = username.trim().lowercase()
        val cleanPassword = password.trim()

        if (trimmedUsername.isBlank() || cleanPassword.isBlank()) {
            return Result.failure(Exception("USER_NOT_FOUND"))
        }

        val db = firestore
        val authInstance = auth

        if (db != null) {
            try {
                ensureAuthSession()
                val email = normalizeEmail(trimmedUsername)
                val authPassword = normalizePassword(cleanPassword)

                // 1. If trying to log in as admin, verify admin password from Firestore
                if (trimmedUsername == "admin") {
                    val adminDoc = try {
                        val fixedDoc = db.collection("users").document("admin_fixed").get().await()
                        if (fixedDoc != null && fixedDoc.exists()) fixedDoc else {
                            val uDoc = db.collection("users").document("admin").get().await()
                            if (uDoc != null && uDoc.exists()) uDoc else {
                                val snap = db.collection("users").whereEqualTo("role", "ADMIN").limit(1).get().await()
                                snap.documents.firstOrNull()
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Admin doc lookup note: ${e.message}")
                        null
                    }

                    if (adminDoc != null && adminDoc.exists() && adminDoc.data != null) {
                        val storedAdminPass = adminDoc.getString("password") ?: "admin"
                        if (cleanPassword != storedAdminPass) {
                            return Result.failure(Exception("INCORRECT_PASSWORD"))
                        }
                        val adminUser = SchoolUser.fromMap(adminDoc.id, adminDoc.data!!)
                        currentUser = adminUser
                        prefs.edit().putString(PREF_CACHED_USER_ID, adminUser.id).apply()
                        return Result.success(adminUser)
                    } else {
                        // First-time initialization of default admin if not yet in Firestore
                        if (cleanPassword == "admin") {
                            return resetAdminCredentials()
                        } else {
                            return Result.failure(Exception("USER_NOT_FOUND"))
                        }
                    }
                }

                // 2. Look up teacher or student in Firestore
                // Strategy A: Direct document lookup by username ID
                var matchedDoc = try {
                    val d = db.collection("users").document(trimmedUsername).get().await()
                    if (d != null && d.exists() && d.data != null) d else null
                } catch (e: Exception) {
                    null
                }

                // Strategy B: Query by username field
                if (matchedDoc == null) {
                    val snap = try {
                        db.collection("users")
                            .whereEqualTo("username", trimmedUsername)
                            .limit(1)
                            .get()
                            .await()
                    } catch (e: Exception) {
                        null
                    }
                    if (snap != null && !snap.isEmpty) {
                        matchedDoc = snap.documents.firstOrNull()
                    }
                }

                // Strategy C: Query by email field
                if (matchedDoc == null) {
                    val snap = try {
                        db.collection("users")
                            .whereEqualTo("email", email)
                            .limit(1)
                            .get()
                            .await()
                    } catch (e: Exception) {
                        null
                    }
                    if (snap != null && !snap.isEmpty) {
                        matchedDoc = snap.documents.firstOrNull()
                    }
                }

                // Strategy D: Query by raw username (case-preserving)
                if (matchedDoc == null && username.trim() != trimmedUsername) {
                    val snap = try {
                        db.collection("users")
                            .whereEqualTo("username", username.trim())
                            .limit(1)
                            .get()
                            .await()
                    } catch (e: Exception) {
                        null
                    }
                    if (snap != null && !snap.isEmpty) {
                        matchedDoc = snap.documents.firstOrNull()
                    }
                }

                // Strategy E: Query by phone
                if (matchedDoc == null) {
                    val snap = try {
                        db.collection("users")
                            .whereEqualTo("phone", username.trim())
                            .limit(1)
                            .get()
                            .await()
                    } catch (e: Exception) {
                        null
                    }
                    if (snap != null && !snap.isEmpty) {
                        matchedDoc = snap.documents.firstOrNull()
                    }
                }

                // Strategy F: Full scan fallback
                if (matchedDoc == null) {
                    val allUsersSnap = try { db.collection("users").get().await() } catch (e: Exception) { null }
                    if (allUsersSnap != null) {
                        matchedDoc = allUsersSnap.documents.firstOrNull { doc ->
                            val u = doc.getString("username") ?: ""
                            val e = doc.getString("email") ?: ""
                            val p = doc.getString("phone") ?: ""
                            doc.id.equals(trimmedUsername, ignoreCase = true) ||
                            u.equals(trimmedUsername, ignoreCase = true) ||
                            e.equals(email, ignoreCase = true) ||
                            p == username.trim()
                        }
                    }
                }

                if (matchedDoc != null && matchedDoc.exists() && matchedDoc.data != null) {
                    val data = matchedDoc.data!!
                    val isActive = matchedDoc.getBoolean("isActive") ?: (data["isActive"] as? Boolean) ?: true
                    if (!isActive) {
                        return Result.failure(Exception("ACCOUNT_DISABLED"))
                    }

                    val storedPass = matchedDoc.getString("password") ?: (data["password"] as? String) ?: ""
                    var passMatches = false

                    if (storedPass.isNotEmpty()) {
                        passMatches = (storedPass == cleanPassword)
                    } else {
                        try {
                            val authRes = authInstance?.signInWithEmailAndPassword(email, authPassword)?.await()
                            passMatches = (authRes != null)
                        } catch (e: Exception) {
                            Log.w(TAG, "Auth sign-in fallback note: ${e.message}")
                            passMatches = false
                        }
                    }

                    if (passMatches) {
                        val user = SchoolUser.fromMap(matchedDoc.id, data)
                        currentUser = user
                        prefs.edit().putString(PREF_CACHED_USER_ID, user.id).apply()
                        // Ensure local memory flow contains this user
                        LocalDataStore.usersFlow.value = LocalDataStore.usersFlow.value.filter { it.id != user.id } + user
                        return Result.success(user)
                    } else {
                        return Result.failure(Exception("INCORRECT_PASSWORD"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore login error: ${e.message}", e)
                if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                    return Result.failure(Exception("PERMISSION_DENIED"))
                }
            }
        }

        // Local cache lookup for users added locally or offline
        val localUser = LocalDataStore.usersFlow.value.firstOrNull {
            it.username.equals(trimmedUsername, ignoreCase = true)
        }

        if (localUser != null) {
            if (!localUser.isActive) {
                return Result.failure(Exception("ACCOUNT_DISABLED"))
            }
            currentUser = localUser
            prefs.edit().putString(PREF_CACHED_USER_ID, localUser.id).apply()
            return Result.success(localUser)
        }

        return Result.failure(Exception("USER_NOT_FOUND"))
    }

    suspend fun checkAutoLogin(): SchoolUser? {
        val cachedUserId = prefs.getString(PREF_CACHED_USER_ID, null) ?: return null

        val db = firestore
        if (db != null) {
            try {
                ensureAuthSession()
                val doc = db.collection("users").document(cachedUserId).get().await()
                if (doc != null && doc.exists() && doc.data != null) {
                    val user = SchoolUser.fromMap(doc.id, doc.data!!)
                    if (user.isActive) {
                        currentUser = user
                        return user
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Online auto-login lookup error: ${e.message}")
            }
        }

        val localUser = LocalDataStore.usersFlow.value.firstOrNull { it.id == cachedUserId }
        if (localUser != null && localUser.isActive) {
            currentUser = localUser
            return localUser
        }

        return null
    }

    fun logout() {
        auth?.signOut()
        currentUser = null
        prefs.edit().remove(PREF_CACHED_USER_ID).apply()
    }

    // ----------------------------------------------------
    // USER MANAGEMENT (Admin creates users via secondary app or local store)
    // ----------------------------------------------------

    suspend fun createUser(
        newUser: SchoolUser,
        plainPassword: String
    ): Result<SchoolUser> {
        val cleanUsername = newUser.username.trim().lowercase()
        val cleanPassword = plainPassword.trim()
        val cleanEmail = normalizeEmail(cleanUsername)

        val db = firestore
        if (db == null) {
            val newUid = "user_${UUID.randomUUID().toString().take(8)}"
            val finalUser = newUser.copy(id = newUid, username = cleanUsername, isActive = true)
            LocalDataStore.usersFlow.value = LocalDataStore.usersFlow.value.filter { it.id != newUid } + finalUser
            return Result.success(finalUser)
        }

        ensureAuthSession()

        val defaultApp = try { FirebaseApp.getInstance() } catch (e: Exception) { null }
        val secondaryAppName = "AdminUserCreator_${System.currentTimeMillis()}"
        var secondaryApp: FirebaseApp? = null
        var authUid: String? = null

        if (defaultApp != null) {
            try {
                secondaryApp = FirebaseApp.initializeApp(context, defaultApp.options, secondaryAppName)
                val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
                val authResult = secondaryAuth.createUserWithEmailAndPassword(cleanEmail, normalizePassword(cleanPassword)).await()
                authUid = authResult.user?.uid
                secondaryAuth.signOut()
            } catch (e: Exception) {
                Log.w(TAG, "Secondary auth creation note: ${e.message}")
            } finally {
                try { secondaryApp?.delete() } catch (_: Exception) {}
            }
        }

        return try {
            val newUid = cleanUsername
            val finalUser = newUser.copy(id = newUid, username = cleanUsername, isActive = true)

            val userMap = finalUser.toMap().toMutableMap()
            userMap["password"] = cleanPassword
            userMap["email"] = cleanEmail
            userMap["username"] = cleanUsername
            userMap["authUid"] = authUid ?: cleanUsername
            userMap["createdAt"] = com.google.firebase.Timestamp.now()

            // 1. Write EXACTLY ONE document with cleanUsername as key to strictly prevent duplicate entries
            db.collection("users").document(cleanUsername).set(userMap).await()

            // 2. Clean up any stale duplicate document that might have been saved by authUid
            if (authUid != null && authUid != cleanUsername) {
                try {
                    db.collection("users").document(authUid).delete().await()
                } catch (_: Exception) {}
            }

            // Update local memory flow for instant responsiveness (deduplicating by username and id)
            LocalDataStore.usersFlow.value = LocalDataStore.usersFlow.value.filter {
                it.id != cleanUsername && !it.username.equals(cleanUsername, ignoreCase = true)
            } + finalUser

            Result.success(finalUser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user $cleanUsername in Firestore", e)
            val isPermissionDenied = e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
            val friendlyMsg = if (isPermissionDenied) {
                "خطأ في صلاحيات قاعدة البيانات السحابية (PERMISSION_DENIED): يرجى تفعيل القواعد (Rules) في Firestore لتسمح بالقراءة والكتابة."
            } else {
                "فشل حفظ الحساب في قاعدة البيانات: ${e.localizedMessage ?: e.message}"
            }
            Result.failure(Exception(friendlyMsg))
        }
    }

    suspend fun updateUser(user: SchoolUser): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.usersFlow.value = LocalDataStore.usersFlow.value.map {
                if (it.id == user.id) user else it
            }
            if (currentUser?.id == user.id) {
                currentUser = user
            }
            return Result.success(Unit)
        }

        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("users").document(user.id).update(user.toMap()).await()
            if (currentUser?.id == user.id) {
                currentUser = user
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleUserActive(userId: String, isActive: Boolean): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val user = LocalDataStore.usersFlow.value.firstOrNull { it.id == userId }
            if (user?.isPrimaryAdmin == true) {
                return Result.failure(Exception("PRIMARY_ADMIN_PROTECTED"))
            }
            LocalDataStore.usersFlow.value = LocalDataStore.usersFlow.value.map {
                if (it.id == userId) it.copy(isActive = isActive) else it
            }
            return Result.success(Unit)
        }

        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.getBoolean("isPrimaryAdmin") == true) {
                return Result.failure(Exception("PRIMARY_ADMIN_PROTECTED"))
            }
            db.collection("users").document(userId).update("isActive", isActive).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: String): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val user = LocalDataStore.usersFlow.value.firstOrNull { it.id == userId }
            if (user?.isPrimaryAdmin == true) {
                return Result.failure(Exception("PRIMARY_ADMIN_PROTECTED"))
            }
            LocalDataStore.usersFlow.value = LocalDataStore.usersFlow.value.filter { it.id != userId }
            return Result.success(Unit)
        }

        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.getBoolean("isPrimaryAdmin") == true) {
                return Result.failure(Exception("PRIMARY_ADMIN_PROTECTED"))
            }
            db.collection("users").document(userId).delete().await()
            val username = doc.getString("username")
            if (!username.isNullOrBlank() && username != userId) {
                try { db.collection("users").document(username).delete().await() } catch (_: Exception) {}
            }
            val authUid = doc.getString("authUid")
            if (!authUid.isNullOrBlank() && authUid != userId) {
                try { db.collection("users").document(authUid).delete().await() } catch (_: Exception) {}
            }
            LocalDataStore.usersFlow.value = LocalDataStore.usersFlow.value.filter {
                it.id != userId && (username == null || !it.username.equals(username, ignoreCase = true))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun secureFirstAdminAccount(
        newPassword: String,
        recoveryEmail: String,
        newUsername: String? = null
    ): Result<SchoolUser> {
        val user = currentUser ?: return Result.failure(Exception("No user session"))
        val finalUsername = if (!newUsername.isNullOrBlank()) newUsername.trim().lowercase() else user.username
        val updatedUser = user.copy(
            username = finalUsername,
            recoveryEmail = recoveryEmail.trim(),
            isPrimaryAdmin = true,
            needsPasswordChange = false
        )

        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.usersFlow.value = LocalDataStore.usersFlow.value.map {
                if (it.id == user.id || it.id == "admin_fixed") updatedUser else it
            }
            currentUser = updatedUser
            prefs.edit().putString(PREF_CACHED_USER_ID, updatedUser.id).apply()
            return Result.success(updatedUser)
        }

        val authInstance = auth ?: return Result.failure(Exception("Firebase not initialized"))
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        val currentFirebaseUser = authInstance.currentUser ?: return Result.failure(Exception("Not logged in"))

        return try {
            val authPassword = normalizePassword(newPassword)
            currentFirebaseUser.updatePassword(authPassword).await()

            val finalEmail = normalizeEmail(finalUsername)
            try {
                currentFirebaseUser.updateEmail(finalEmail).await()
            } catch (e: Exception) {
                Log.w(TAG, "Update email non-fatal: ${e.message}")
            }

            db.collection("users").document(updatedUser.id).set(updatedUser.toMap()).await()
            try {
                db.collection("users").document("admin_fixed").set(updatedUser.toMap()).await()
            } catch (e: Exception) {
                Log.w(TAG, "admin_fixed doc sync non-fatal", e)
            }

            currentUser = updatedUser
            prefs.edit().putString(PREF_CACHED_USER_ID, updatedUser.id).apply()
            Result.success(updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed securing first admin account", e)
            Result.failure(e)
        }
    }

    suspend fun changeOwnPassword(oldPlain: String, newPlain: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("No user session"))
        val cleanOld = oldPlain.trim()
        val cleanNew = newPlain.trim()

        val db = firestore
        val authInstance = auth

        if (db != null) {
            try {
                // 1. Verify old password if available
                val userDoc = try {
                    val d1 = db.collection("users").document(user.id).get().await()
                    if (d1 != null && d1.exists()) d1 else db.collection("users").document(user.username).get().await()
                } catch (e: Exception) {
                    null
                }

                if (userDoc != null && userDoc.exists()) {
                    val currentStoredPass = userDoc.getString("password") ?: (if (user.role == Role.ADMIN) "admin" else "")
                    if (currentStoredPass.isNotEmpty() && currentStoredPass != cleanOld) {
                        return Result.failure(Exception("كلمة المرور الحالية غير صحيحة"))
                    }
                }

                // 2. Update Firestore documents
                val updateMap = mapOf<String, Any>(
                    "password" to cleanNew,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )

                try { db.collection("users").document(user.id).update(updateMap).await() } catch (_: Exception) {}
                if (user.username.isNotEmpty() && user.username != user.id) {
                    try { db.collection("users").document(user.username).update(updateMap).await() } catch (_: Exception) {}
                }
                if (user.role == Role.ADMIN) {
                    try { db.collection("users").document("admin_fixed").update(updateMap).await() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore password update error: ${e.message}")
            }
        }

        // 3. Update Firebase Auth if logged in
        if (authInstance != null && authInstance.currentUser != null) {
            try {
                val currentFirebaseUser = authInstance.currentUser
                if (currentFirebaseUser != null) {
                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                        normalizeEmail(user.username),
                        normalizePassword(cleanOld)
                    )
                    try { currentFirebaseUser.reauthenticate(credential).await() } catch (_: Exception) {}
                    currentFirebaseUser.updatePassword(normalizePassword(cleanNew)).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Auth password update notice: ${e.message}")
            }
        }

        return Result.success(Unit)
    }

    suspend fun sendAdminPasswordResetEmail(identifier: String): Result<String> {
        val trimmed = identifier.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(Exception("يرجى إدخال اسم المستخدم"))
        }

        val db = firestore
        if (db != null) {
            return try {
                val userSnapshot = try {
                    val s1 = db.collection("users").whereEqualTo("username", trimmed.lowercase()).limit(1).get().await()
                    if (s1 != null && !s1.isEmpty) s1 else db.collection("users").whereEqualTo("email", normalizeEmail(trimmed)).limit(1).get().await()
                } catch (e: Exception) {
                    null
                }

                val doc = userSnapshot?.documents?.firstOrNull()
                if (doc == null || !doc.exists()) {
                    return Result.failure(Exception("لم يتم العثور على حساب بهذا الاسم."))
                }

                val role = doc.getString("role") ?: "STUDENT"
                val fullName = doc.getString("fullName") ?: trimmed
                if (role != "ADMIN") {
                    val roleName = if (role == "TEACHER") "أستاذ" else "تلميذ"
                    return Result.failure(
                        Exception("هذا الحساب خاص بـ ($roleName: $fullName). يرجى مراجعة إدارة المركز لتعديل أو استعادة كلمة المرور الخاصة بك.")
                    )
                }

                val recoveryEmail = doc.getString("recoveryEmail")?.trim() ?: ""
                val authInstance = auth

                if (authInstance != null && recoveryEmail.contains("@") && !recoveryEmail.endsWith("@school.app")) {
                    authInstance.sendPasswordResetEmail(recoveryEmail).await()
                    Result.success(recoveryEmail)
                } else {
                    Result.failure(Exception("لم يتم تعيين بريد إلكتروني لاستعادة حساب المدير. يرجى تسجيل الدخول بكلمة المرور الحالية وتعيين بريد استعادة من الإعدادات."))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Password reset error: ${e.message}")
                Result.failure(e)
            }
        }

        return Result.failure(Exception("يرجى التواصل مع مسؤول النظام."))
    }

    suspend fun getUser(idOrUsername: String): SchoolUser? {
        val clean = idOrUsername.trim().lowercase().removePrefix("@")
        if (clean.isBlank()) return null
        val inMemory = LocalDataStore.usersFlow.value.firstOrNull {
            it.id == clean || it.username.trim().lowercase() == clean
        }
        if (inMemory != null) return inMemory

        val db = firestore ?: return null
        return try {
            // Try by document ID
            val doc = db.collection("users").document(clean).get().await()
            if (doc != null && doc.exists() && doc.data != null) {
                return SchoolUser.fromMap(doc.id, doc.data ?: emptyMap())
            }
            // Try by username field
            val snap = db.collection("users").whereEqualTo("username", clean).limit(1).get().await()
            if (snap != null && !snap.isEmpty) {
                val match = snap.documents.first()
                return SchoolUser.fromMap(match.id, match.data ?: emptyMap())
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "getUser error: ${e.message}")
            null
        }
    }

    fun observeUsers(role: Role? = null): Flow<List<SchoolUser>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.usersFlow.map { list ->
                if (role != null) list.filter { it.role == role } else list
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            var query: Query = db.collection("users")
            if (role != null) {
                query = query.whereEqualTo("role", role.name)
            }

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Users listener error: ${error.message}")
                    val fallback = if (role != null) {
                        LocalDataStore.usersFlow.value.filter { it.role == role }
                    } else {
                        LocalDataStore.usersFlow.value
                    }
                    trySend(fallback)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { SchoolUser.fromMap(doc.id, it) }
                }?.distinctBy { it.username.lowercase().trim() } ?: emptyList()
                LocalDataStore.usersFlow.value = if (role == null) users else (LocalDataStore.usersFlow.value.filter { it.role != role } + users)
                trySend(users)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // GROUPS, SUBJECTS & ROOMS
    // ----------------------------------------------------

    suspend fun addGroup(group: SchoolGroup): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = "grp_${UUID.randomUUID().toString().take(8)}"
            val created = group.copy(id = id)
            LocalDataStore.groupsFlow.value = LocalDataStore.groupsFlow.value + created
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("groups").document()
            docRef.set(group.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGroup(groupId: String): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.groupsFlow.value = LocalDataStore.groupsFlow.value.filter { it.id != groupId }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("groups").document(groupId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeGroups(): Flow<List<SchoolGroup>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.groupsFlow
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listener = db.collection("groups").addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    trySend(LocalDataStore.groupsFlow.value)
                    return@addSnapshotListener
                }
                val list = snap.documents.map {
                    SchoolGroup(
                        id = it.id,
                        name = it.getString("name") ?: "",
                        level = it.getString("level") ?: "",
                        studentIds = (it.get("studentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                }
                LocalDataStore.groupsFlow.value = list
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    suspend fun addSubject(subject: Subject): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = "subj_${UUID.randomUUID().toString().take(8)}"
            val created = subject.copy(id = id)
            LocalDataStore.subjectsFlow.value = LocalDataStore.subjectsFlow.value + created
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("subjects").document()
            docRef.set(subject.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSubjects(): Flow<List<Subject>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.subjectsFlow
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listener = db.collection("subjects").addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    trySend(LocalDataStore.subjectsFlow.value)
                    return@addSnapshotListener
                }
                val list = snap.documents.map {
                    Subject(
                        id = it.id,
                        name = it.getString("name") ?: "",
                        code = it.getString("code") ?: ""
                    )
                }
                LocalDataStore.subjectsFlow.value = list
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    suspend fun addRoom(room: Room): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = "room_${UUID.randomUUID().toString().take(8)}"
            val created = room.copy(id = id)
            LocalDataStore.roomsFlow.value = LocalDataStore.roomsFlow.value + created
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("rooms").document()
            docRef.set(room.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRoom(room: Room): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.roomsFlow.value = LocalDataStore.roomsFlow.value.map {
                if (it.id == room.id) room else it
            }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("rooms").document(room.id).set(room.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRoom(roomId: String): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.roomsFlow.value = LocalDataStore.roomsFlow.value.filter { it.id != roomId }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("rooms").document(roomId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeRooms(): Flow<List<Room>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.roomsFlow
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listener = db.collection("rooms").addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    Room(
                        id = it.id,
                        name = it.getString("name") ?: "",
                        capacity = (it.getLong("capacity") ?: 30L).toInt()
                    )
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // TIMETABLE & CONFLICT DETECTION
    // ----------------------------------------------------

    suspend fun addTimetableSlot(slot: TimetableSlot): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = "slot_${UUID.randomUUID().toString().take(8)}"
            val created = slot.copy(id = id)
            LocalDataStore.timetableFlow.value = LocalDataStore.timetableFlow.value + created
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("timetable").document()
            docRef.set(slot.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTimetableSlot(slotId: String): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.timetableFlow.value = LocalDataStore.timetableFlow.value.filter { it.id != slotId }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("timetable").document(slotId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeTimetable(
        groupId: String? = null,
        teacherId: String? = null
    ): Flow<List<TimetableSlot>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.timetableFlow.map { list ->
                list.filter { slot ->
                    (groupId == null || slot.groupId == groupId) &&
                    (teacherId == null || slot.teacherId == teacherId)
                }
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            var query: Query = db.collection("timetable")
            if (groupId != null) {
                query = query.whereEqualTo("groupId", groupId)
            } else if (teacherId != null) {
                query = query.whereEqualTo("teacherId", teacherId)
            }

            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    TimetableSlot(
                        id = it.id,
                        subjectId = it.getString("subjectId") ?: "",
                        subjectName = it.getString("subjectName") ?: "",
                        teacherId = it.getString("teacherId") ?: "",
                        teacherName = it.getString("teacherName") ?: "",
                        groupId = it.getString("groupId") ?: "",
                        groupName = it.getString("groupName") ?: "",
                        roomId = it.getString("roomId") ?: "",
                        roomName = it.getString("roomName") ?: "",
                        dayOfWeek = (it.getLong("dayOfWeek") ?: 1L).toInt(),
                        startTime = it.getString("startTime") ?: "08:00",
                        endTime = it.getString("endTime") ?: "10:00"
                    )
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // ATTENDANCE
    // ----------------------------------------------------

    suspend fun saveAttendance(record: AttendanceRecord): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = if (record.id.isNotBlank()) record.id else "att_${UUID.randomUUID().toString().take(8)}"
            val created = record.copy(id = id)
            LocalDataStore.attendanceFlow.value = LocalDataStore.attendanceFlow.value.filter { it.id != id } + created
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = if (record.id.isNotBlank()) db.collection("attendance").document(record.id) else db.collection("attendance").document()
            docRef.set(record.copy(id = docRef.id).toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAttendance(): Flow<List<AttendanceRecord>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.attendanceFlow
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listener = db.collection("attendance").addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    AttendanceRecord(
                        id = it.id,
                        slotId = it.getString("slotId") ?: "",
                        subjectName = it.getString("subjectName") ?: "",
                        groupName = it.getString("groupName") ?: "",
                        date = it.getString("date") ?: "",
                        presentStudentIds = (it.get("presentStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        absentStudentIds = (it.get("absentStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        lateStudentIds = (it.get("lateStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        teacherId = it.getString("teacherId") ?: ""
                    )
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // LEARNING RESOURCES
    // ----------------------------------------------------

    suspend fun addLearningResource(resource: LearningResource): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = "res_${UUID.randomUUID().toString().take(8)}"
            val created = resource.copy(id = id)
            LocalDataStore.resourcesFlow.value = listOf(created) + LocalDataStore.resourcesFlow.value
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("resources").document()
            docRef.set(resource.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeResources(groupId: String? = null): Flow<List<LearningResource>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.resourcesFlow.map { list ->
                list.filter { res ->
                    groupId == null || res.targetGroupId.isEmpty() || res.targetGroupId == groupId
                }
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val query = db.collection("resources").orderBy("createdAt", Query.Direction.DESCENDING)
            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    LearningResource(
                        id = it.id,
                        title = it.getString("title") ?: "",
                        type = it.getString("type") ?: "SUMMARY",
                        subjectId = it.getString("subjectId") ?: "",
                        subjectName = it.getString("subjectName") ?: "",
                        level = it.getString("level") ?: "",
                        targetGroupId = it.getString("targetGroupId") ?: "",
                        fileUrl = it.getString("fileUrl") ?: "",
                        authorId = it.getString("authorId") ?: "",
                        authorName = it.getString("authorName") ?: "",
                        createdAt = it.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }?.filter { res ->
                    groupId == null || res.targetGroupId.isEmpty() || res.targetGroupId == groupId
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // HOMEWORK & SUBMISSIONS
    // ----------------------------------------------------

    suspend fun createHomework(hw: Homework): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = "hw_${UUID.randomUUID().toString().take(8)}"
            val created = hw.copy(id = id)
            LocalDataStore.homeworkFlow.value = listOf(created) + LocalDataStore.homeworkFlow.value
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("homework").document()
            docRef.set(hw.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeHomework(groupId: String? = null): Flow<List<Homework>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.homeworkFlow.map { list ->
                list.filter { h -> groupId == null || h.groupId == groupId }
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val query = db.collection("homework").orderBy("createdAt", Query.Direction.DESCENDING)
            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    Homework(
                        id = it.id,
                        title = it.getString("title") ?: "",
                        description = it.getString("description") ?: "",
                        deadline = it.getString("deadline") ?: "",
                        subjectId = it.getString("subjectId") ?: "",
                        subjectName = it.getString("subjectName") ?: "",
                        groupId = it.getString("groupId") ?: "",
                        attachmentUrl = it.getString("attachmentUrl") ?: "",
                        authorTeacherId = it.getString("authorTeacherId") ?: "",
                        authorTeacherName = it.getString("authorTeacherName") ?: "",
                        createdAt = it.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }?.filter { h -> groupId == null || h.groupId == groupId } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    suspend fun submitHomework(sub: HomeworkSubmission): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = "sub_${UUID.randomUUID().toString().take(8)}"
            val created = sub.copy(id = id)
            LocalDataStore.submissionsFlow.value = LocalDataStore.submissionsFlow.value + created
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("homework_submissions").document()
            docRef.set(sub.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun gradeSubmission(subId: String, score: Float, feedback: String): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.submissionsFlow.value = LocalDataStore.submissionsFlow.value.map {
                if (it.id == subId) it.copy(score = score, feedback = feedback) else it
            }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("homework_submissions").document(subId).update(
                mapOf(
                    "score" to score,
                    "feedback" to feedback
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSubmissions(homeworkId: String? = null): Flow<List<HomeworkSubmission>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.submissionsFlow.map { list ->
                if (homeworkId != null) list.filter { it.homeworkId == homeworkId } else list
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            var query: Query = db.collection("homework_submissions")
            if (homeworkId != null) {
                query = query.whereEqualTo("homeworkId", homeworkId)
            }
            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    HomeworkSubmission(
                        id = it.id,
                        homeworkId = it.getString("homeworkId") ?: "",
                        studentId = it.getString("studentId") ?: "",
                        studentName = it.getString("studentName") ?: "",
                        submissionText = it.getString("submissionText") ?: "",
                        attachmentUrl = it.getString("attachmentUrl") ?: "",
                        submittedAt = it.getLong("submittedAt") ?: System.currentTimeMillis(),
                        score = it.getDouble("score")?.toFloat(),
                        feedback = it.getString("feedback") ?: ""
                    )
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // GRADES
    // ----------------------------------------------------

    suspend fun addGrade(grade: GradeItem): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = "grd_${UUID.randomUUID().toString().take(8)}"
            val created = grade.copy(id = id)
            LocalDataStore.gradesFlow.value = listOf(created) + LocalDataStore.gradesFlow.value
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("grades").document()
            docRef.set(grade.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeGrades(studentId: String? = null): Flow<List<GradeItem>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.gradesFlow.map { list ->
                if (studentId != null) list.filter { it.studentId == studentId } else list
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            var query: Query = db.collection("grades")
            if (studentId != null) {
                query = query.whereEqualTo("studentId", studentId)
            }
            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    GradeItem(
                        id = it.id,
                        studentId = it.getString("studentId") ?: "",
                        studentName = it.getString("studentName") ?: "",
                        subjectId = it.getString("subjectId") ?: "",
                        subjectName = it.getString("subjectName") ?: "",
                        type = it.getString("type") ?: "EXAM",
                        score = (it.getDouble("score") ?: 0.0).toFloat(),
                        maxScore = (it.getDouble("maxScore") ?: 20.0).toFloat(),
                        comment = it.getString("comment") ?: "",
                        date = it.getString("date") ?: "",
                        teacherId = it.getString("teacherId") ?: ""
                    )
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // GRADES - WITH EDIT SUPPORT
    // ----------------------------------------------------

    suspend fun updateGradeItem(grade: GradeItem): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.gradesFlow.value = LocalDataStore.gradesFlow.value.map {
                if (it.id == grade.id) grade.copy(updatedAt = System.currentTimeMillis()) else it
            }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val updated = grade.copy(updatedAt = System.currentTimeMillis())
            db.collection("grades").document(updated.id).set(updated.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // UNIVERSAL EDIT SUPPORT: ATTENDANCE, HOMEWORK, RESOURCES, ANNOUNCEMENTS, TIMETABLE
    // ----------------------------------------------------

    suspend fun updateAttendanceRecord(record: AttendanceRecord): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.attendanceFlow.value = LocalDataStore.attendanceFlow.value.map {
                if (it.id == record.id) record.copy(updatedAt = System.currentTimeMillis()) else it
            }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val updated = record.copy(updatedAt = System.currentTimeMillis())
            db.collection("attendance").document(updated.id).set(updated.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateHomework(homework: Homework): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.homeworkFlow.value = LocalDataStore.homeworkFlow.value.map {
                if (it.id == homework.id) homework.copy(updatedAt = System.currentTimeMillis()) else it
            }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val updated = homework.copy(updatedAt = System.currentTimeMillis())
            db.collection("homework").document(updated.id).set(updated.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLearningResource(resource: LearningResource): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.resourcesFlow.value = LocalDataStore.resourcesFlow.value.map {
                if (it.id == resource.id) resource.copy(updatedAt = System.currentTimeMillis()) else it
            }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val updated = resource.copy(updatedAt = System.currentTimeMillis())
            db.collection("resources").document(updated.id).set(updated.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAnnouncement(announcement: Announcement): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.announcementsFlow.value = LocalDataStore.announcementsFlow.value.map {
                if (it.id == announcement.id) announcement.copy(updatedAt = System.currentTimeMillis()) else it
            }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val updated = announcement.copy(updatedAt = System.currentTimeMillis())
            db.collection("announcements").document(updated.id).set(updated.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.announcementsFlow.value = LocalDataStore.announcementsFlow.value.filter {
                it.id != announcementId
            }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("announcements").document(announcementId).delete().await()
            LocalDataStore.announcementsFlow.value = LocalDataStore.announcementsFlow.value.filter {
                it.id != announcementId
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTimetableSlot(slot: TimetableSlot): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.timetableFlow.value = LocalDataStore.timetableFlow.value.map {
                if (it.id == slot.id) slot else it
            }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("timetable").document(slot.id).set(slot.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // FEATURE 1: MULTI-SUBJECT ENROLLMENTS
    // ----------------------------------------------------

    suspend fun createEnrollment(enrollment: Enrollment): Result<String> {
        val initialRemaining = (enrollment.monthlyFee - enrollment.amountPaid).coerceAtLeast(0.0)
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = "enr_${UUID.randomUUID().toString().take(8)}"
            val newEnrollment = enrollment.copy(
                id = id,
                amountRemaining = initialRemaining,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            LocalDataStore.enrollmentsFlow.value = listOf(newEnrollment) + LocalDataStore.enrollmentsFlow.value
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("enrollments").document()
            val newEnrollment = enrollment.copy(
                id = docRef.id,
                amountRemaining = initialRemaining,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            docRef.set(newEnrollment.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEnrollment(enrollment: Enrollment): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.enrollmentsFlow.value = LocalDataStore.enrollmentsFlow.value.map {
                if (it.id == enrollment.id) enrollment.copy(updatedAt = System.currentTimeMillis()) else it
            }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("enrollments").document(enrollment.id)
                .set(enrollment.copy(updatedAt = System.currentTimeMillis()).toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEnrollmentWithAudit(
        enrollment: Enrollment,
        oldEnrollment: Enrollment,
        note: String
    ): Result<Unit> {
        val res = updateEnrollment(enrollment)
        if (res.isSuccess) {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val user = currentUser
            logAudit(
                AuditLog(
                    userId = user?.id ?: "",
                    userName = user?.fullName ?: (user?.username ?: "Admin"),
                    userRole = user?.role?.name ?: "ADMIN",
                    action = "EDIT_ENROLLMENT_FEE",
                    targetCollection = "enrollments",
                    targetRecordId = enrollment.id,
                    recordTitle = "${enrollment.studentName} - ${enrollment.subjectName}",
                    oldValue = "Fee: ${oldEnrollment.monthlyFee}, Paid: ${oldEnrollment.amountPaid}, Rem: ${oldEnrollment.amountRemaining}",
                    newValue = "Fee: ${enrollment.monthlyFee}, Paid: ${enrollment.amountPaid}, Rem: ${enrollment.amountRemaining}",
                    note = note,
                    timestamp = System.currentTimeMillis(),
                    dateStr = dateStr
                )
            )
        }
        return res
    }

    fun observeEnrollments(studentId: String? = null, teacherId: String? = null): Flow<List<Enrollment>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.enrollmentsFlow.map { list ->
                list.filter { enr ->
                    (studentId == null || enr.studentId == studentId) &&
                    (teacherId == null || enr.teacherId == teacherId)
                }
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            var query: Query = db.collection("enrollments")
            if (studentId != null) {
                query = query.whereEqualTo("studentId", studentId)
            } else if (teacherId != null) {
                query = query.whereEqualTo("teacherId", teacherId)
            }
            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    Enrollment.fromMap(it.id, it.data ?: emptyMap())
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // FEATURE 1: TEACHER REVENUE SHARE PERCENTAGES
    // ----------------------------------------------------

    suspend fun saveTeacherShare(share: TeacherSubjectShare): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = if (share.id.isNotBlank()) share.id else "tshare_${UUID.randomUUID().toString().take(8)}"
            val created = share.copy(id = id, updatedAt = System.currentTimeMillis())
            LocalDataStore.teacherSharesFlow.value = LocalDataStore.teacherSharesFlow.value.filter { it.id != id } + created
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = if (share.id.isBlank()) db.collection("teacher_shares").document() else db.collection("teacher_shares").document(share.id)
            docRef.set(share.copy(id = docRef.id, updatedAt = System.currentTimeMillis()).toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTeacherShareWithAudit(
        share: TeacherSubjectShare,
        oldShare: TeacherSubjectShare,
        note: String
    ): Result<Unit> {
        val res = saveTeacherShare(share)
        if (res.isSuccess) {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val user = currentUser
            logAudit(
                AuditLog(
                    userId = user?.id ?: "",
                    userName = user?.fullName ?: (user?.username ?: "Admin"),
                    userRole = user?.role?.name ?: "ADMIN",
                    action = "EDIT_TEACHER_PERCENTAGE",
                    targetCollection = "teacher_shares",
                    targetRecordId = share.id,
                    recordTitle = "${share.teacherName} - ${share.subjectName}",
                    oldValue = "Approved: ${oldShare.approvedPercentage}%, Requested: ${oldShare.requestedPercentage}%",
                    newValue = "Approved: ${share.approvedPercentage}%, Requested: ${share.requestedPercentage}%",
                    note = note,
                    timestamp = System.currentTimeMillis(),
                    dateStr = dateStr
                )
            )
        }
        return res.map { }
    }

    fun observeTeacherShares(teacherId: String? = null): Flow<List<TeacherSubjectShare>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.teacherSharesFlow.map { list ->
                if (teacherId != null) list.filter { it.teacherId == teacherId } else list
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            var query: Query = db.collection("teacher_shares")
            if (teacherId != null) {
                query = query.whereEqualTo("teacherId", teacherId)
            }
            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    TeacherSubjectShare.fromMap(it.id, it.data ?: emptyMap())
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // FEATURE 1: MULTI-SUBJECT PAYMENTS & BALANCES
    // ----------------------------------------------------

    suspend fun recordMultiSubjectPayment(
        studentId: String,
        studentName: String,
        enrollmentId: String,
        subjectId: String,
        subjectName: String,
        teacherId: String,
        teacherName: String,
        amount: Double,
        approvedPercentage: Double,
        month: String,
        date: String,
        notes: String
    ): Result<String> {
        val teacherPct = approvedPercentage.coerceIn(0.0, 100.0)
        val teacherShare = amount * (teacherPct / 100.0)
        val schoolShare = amount * ((100.0 - teacherPct) / 100.0)

        if (!isUsingCustomDatabase() || firestore == null) {
            val payId = "pay_${UUID.randomUUID().toString().take(8)}"
            val existingEnrollment = LocalDataStore.enrollmentsFlow.value.firstOrNull { it.id == enrollmentId }
            val currentPaid = existingEnrollment?.amountPaid ?: 0.0
            val monthlyFee = existingEnrollment?.monthlyFee ?: amount
            val newAmountPaid = currentPaid + amount
            val newAmountRemaining = (monthlyFee - newAmountPaid).coerceAtLeast(0.0)
            val newPaymentStatus = if (newAmountRemaining <= 0.0) "PAID" else "PARTIALLY_PAID"

            val payment = PaymentRecord(
                id = payId,
                studentId = studentId,
                studentName = studentName,
                enrollmentId = enrollmentId,
                subjectId = subjectId,
                subjectName = subjectName,
                teacherId = teacherId,
                teacherName = teacherName,
                amount = amount,
                teacherShare = teacherShare,
                schoolShare = schoolShare,
                teacherPercentage = teacherPct,
                month = month,
                status = newPaymentStatus,
                date = date,
                notes = notes,
                recordedBy = currentUser?.fullName ?: "مدير المركز",
                createdAt = System.currentTimeMillis()
            )
            LocalDataStore.paymentsFlow.value = listOf(payment) + LocalDataStore.paymentsFlow.value

            if (existingEnrollment != null) {
                LocalDataStore.enrollmentsFlow.value = LocalDataStore.enrollmentsFlow.value.map {
                    if (it.id == enrollmentId) {
                        it.copy(amountPaid = newAmountPaid, amountRemaining = newAmountRemaining, updatedAt = System.currentTimeMillis())
                    } else it
                }
            }
            return Result.success(payId)
        }

        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val enrollmentRef = db.collection("enrollments").document(enrollmentId)
            val enrollmentSnap = enrollmentRef.get().await()
            val existingEnrollment = if (enrollmentSnap.exists()) {
                Enrollment.fromMap(enrollmentSnap.id, enrollmentSnap.data ?: emptyMap())
            } else null

            val currentPaid = existingEnrollment?.amountPaid ?: 0.0
            val monthlyFee = existingEnrollment?.monthlyFee ?: amount
            val newAmountPaid = currentPaid + amount
            val newAmountRemaining = (monthlyFee - newAmountPaid).coerceAtLeast(0.0)
            val newPaymentStatus = if (newAmountRemaining <= 0.0) "PAID" else "PARTIALLY_PAID"

            val paymentRef = db.collection("payments").document()
            val payment = PaymentRecord(
                id = paymentRef.id,
                studentId = studentId,
                studentName = studentName,
                enrollmentId = enrollmentId,
                subjectId = subjectId,
                subjectName = subjectName,
                teacherId = teacherId,
                teacherName = teacherName,
                amount = amount,
                teacherShare = teacherShare,
                schoolShare = schoolShare,
                teacherPercentage = teacherPct,
                month = month,
                status = newPaymentStatus,
                date = date,
                notes = notes,
                recordedBy = currentUser?.id ?: "admin",
                createdAt = System.currentTimeMillis()
            )
            paymentRef.set(payment.toMap()).await()

            if (existingEnrollment != null) {
                enrollmentRef.update(
                    mapOf(
                        "amountPaid" to newAmountPaid,
                        "amountRemaining" to newAmountRemaining,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            }

            Result.success(paymentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePaymentWithAudit(
        updatedPayment: PaymentRecord,
        oldPayment: PaymentRecord,
        note: String
    ): Result<Unit> {
        val teacherShare = updatedPayment.amount * (updatedPayment.teacherPercentage / 100.0)
        val schoolShare = updatedPayment.amount * ((100.0 - updatedPayment.teacherPercentage) / 100.0)
        val finalPayment = updatedPayment.copy(teacherShare = teacherShare, schoolShare = schoolShare)

        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val user = currentUser

        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.paymentsFlow.value = LocalDataStore.paymentsFlow.value.map {
                if (it.id == finalPayment.id) finalPayment else it
            }
            val diff = finalPayment.amount - oldPayment.amount
            if (diff != 0.0 && finalPayment.enrollmentId.isNotBlank()) {
                LocalDataStore.enrollmentsFlow.value = LocalDataStore.enrollmentsFlow.value.map { enr ->
                    if (enr.id == finalPayment.enrollmentId) {
                        val newPaid = (enr.amountPaid + diff).coerceAtLeast(0.0)
                        val newRem = (enr.monthlyFee - newPaid).coerceAtLeast(0.0)
                        enr.copy(amountPaid = newPaid, amountRemaining = newRem, updatedAt = System.currentTimeMillis())
                    } else enr
                }
            }
            logAudit(
                AuditLog(
                    userId = user?.id ?: "",
                    userName = user?.fullName ?: (user?.username ?: "Admin"),
                    userRole = user?.role?.name ?: "ADMIN",
                    action = "EDIT_PAYMENT",
                    targetCollection = "payments",
                    targetRecordId = finalPayment.id,
                    recordTitle = "${finalPayment.studentName} - ${finalPayment.subjectName}",
                    oldValue = "Amount: ${oldPayment.amount} MAD, Status: ${oldPayment.status}",
                    newValue = "Amount: ${finalPayment.amount} MAD, Status: ${finalPayment.status}",
                    note = note,
                    timestamp = System.currentTimeMillis(),
                    dateStr = dateStr
                )
            )
            return Result.success(Unit)
        }

        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("payments").document(finalPayment.id).set(finalPayment.toMap()).await()

            val diff = finalPayment.amount - oldPayment.amount
            if (diff != 0.0 && finalPayment.enrollmentId.isNotBlank()) {
                val enrollmentRef = db.collection("enrollments").document(finalPayment.enrollmentId)
                val snap = enrollmentRef.get().await()
                if (snap.exists()) {
                    val enroll = Enrollment.fromMap(snap.id, snap.data ?: emptyMap())
                    val newPaid = (enroll.amountPaid + diff).coerceAtLeast(0.0)
                    val newRem = (enroll.monthlyFee - newPaid).coerceAtLeast(0.0)
                    enrollmentRef.update(
                        mapOf(
                            "amountPaid" to newPaid,
                            "amountRemaining" to newRem,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()
                }
            }

            logAudit(
                AuditLog(
                    userId = user?.id ?: "",
                    userName = user?.fullName ?: (user?.username ?: "Admin"),
                    userRole = user?.role?.name ?: "ADMIN",
                    action = "EDIT_PAYMENT",
                    targetCollection = "payments",
                    targetRecordId = finalPayment.id,
                    recordTitle = "${finalPayment.studentName} - ${finalPayment.subjectName}",
                    oldValue = "Amount: ${oldPayment.amount} MAD, Status: ${oldPayment.status}",
                    newValue = "Amount: ${finalPayment.amount} MAD, Status: ${finalPayment.status}",
                    note = note,
                    timestamp = System.currentTimeMillis(),
                    dateStr = dateStr
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordPayment(payment: PaymentRecord): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = if (payment.id.isNotBlank()) payment.id else "pay_${UUID.randomUUID().toString().take(8)}"
            val created = payment.copy(id = id)
            LocalDataStore.paymentsFlow.value = listOf(created) + LocalDataStore.paymentsFlow.value
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("payments").document()
            docRef.set(payment.copy(id = docRef.id).toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observePayments(studentId: String? = null, teacherId: String? = null): Flow<List<PaymentRecord>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.paymentsFlow.map { list ->
                list.filter { p ->
                    (studentId == null || p.studentId == studentId) &&
                    (teacherId == null || p.teacherId == teacherId)
                }
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            var query: Query = db.collection("payments")
            if (studentId != null) {
                query = query.whereEqualTo("studentId", studentId)
            } else if (teacherId != null) {
                query = query.whereEqualTo("teacherId", teacherId)
            }
            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    PaymentRecord.fromMap(it.id, it.data ?: emptyMap())
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // FEATURE 2: AUDIT LOGS (Immutable History)
    // ----------------------------------------------------

    suspend fun logAudit(auditLog: AuditLog): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = "audit_${UUID.randomUUID().toString().take(8)}"
            val created = auditLog.copy(id = id)
            LocalDataStore.auditLogsFlow.value = listOf(created) + LocalDataStore.auditLogsFlow.value
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("auditLogs").document()
            docRef.set(auditLog.copy(id = docRef.id).toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAuditLogs(): Flow<List<AuditLog>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.auditLogsFlow
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listener = db.collection("auditLogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    val list = snap?.documents?.map {
                        AuditLog.fromMap(it.id, it.data ?: emptyMap())
                    } ?: emptyList()
                    trySend(list)
                }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // ANNOUNCEMENTS
    // ----------------------------------------------------

    suspend fun postAnnouncement(announcement: Announcement): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = "ann_${UUID.randomUUID().toString().take(8)}"
            val created = announcement.copy(id = id, createdAt = System.currentTimeMillis())
            LocalDataStore.announcementsFlow.value = listOf(created) + LocalDataStore.announcementsFlow.value
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("announcements").document()
            docRef.set(announcement.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAnnouncements(userRole: Role? = null, userGroupId: String? = null): Flow<List<Announcement>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.announcementsFlow.map { list ->
                list.filter { a ->
                    if (userRole == Role.ADMIN) true
                    else when (a.targetAudience) {
                        "ALL" -> true
                        "TEACHERS" -> userRole == Role.TEACHER
                        "STUDENTS" -> userRole == Role.STUDENT
                        "GROUP" -> a.targetGroupId == userGroupId
                        else -> true
                    }
                }
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val query = db.collection("announcements").orderBy("createdAt", Query.Direction.DESCENDING)
            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    Announcement(
                        id = it.id,
                        title = it.getString("title") ?: "",
                        body = it.getString("body") ?: "",
                        targetAudience = it.getString("targetAudience") ?: "ALL",
                        targetGroupId = it.getString("targetGroupId"),
                        authorName = it.getString("authorName") ?: "",
                        createdAt = it.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }?.filter { a ->
                    if (userRole == Role.ADMIN) true
                    else when (a.targetAudience) {
                        "ALL" -> true
                        "TEACHERS" -> userRole == Role.TEACHER
                        "STUDENTS" -> userRole == Role.STUDENT
                        "GROUP" -> a.targetGroupId == userGroupId
                        else -> true
                    }
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // CHAT SYSTEM & PRIVACY HELPERS
    // ----------------------------------------------------

    fun isStudentEnrolledWithTeacher(
        student: SchoolUser,
        teacher: SchoolUser,
        enrollments: List<Enrollment> = emptyList(),
        timetableSlots: List<TimetableSlot> = emptyList(),
        groups: List<SchoolGroup> = emptyList()
    ): Boolean {
        if (student.role != Role.STUDENT || teacher.role != Role.TEACHER) return false

        // 1. Direct active enrollment record with this teacher or teacher's subject
        if (enrollments.any { it.studentId == student.id && (it.teacherId == teacher.id || teacher.subjectIds.contains(it.subjectId)) && it.status == "active" }) {
            return true
        }
        // 2. Shared subject ID
        if (student.subjectIds.isNotEmpty() && teacher.subjectIds.isNotEmpty()) {
            if (student.subjectIds.any { teacher.subjectIds.contains(it) }) return true
        }
        // 3. Timetable slot match: teacher teaches a group that student belongs to
        val teacherTaughtGroupIds = timetableSlots.filter { it.teacherId == teacher.id }.map { it.groupId }.filter { it.isNotBlank() }
        if (teacherTaughtGroupIds.isNotEmpty() && student.groupIds.any { teacherTaughtGroupIds.contains(it) }) {
            return true
        }
        // 4. Group studentIds: student is in a group that teacher teaches
        val teacherTaughtGroups = groups.filter { teacherTaughtGroupIds.contains(it.id) }
        if (teacherTaughtGroups.any { it.studentIds.contains(student.id) }) {
            return true
        }
        // 5. Timetable slot subject match
        val teacherTimetableSubjectIds = timetableSlots.filter { it.teacherId == teacher.id }.map { it.subjectId }.filter { it.isNotBlank() }
        if (teacherTimetableSubjectIds.isNotEmpty() && student.subjectIds.any { teacherTimetableSubjectIds.contains(it) }) {
            return true
        }
        return false
    }

    suspend fun createOrGetConversation(
        participantIds: List<String>,
        name: String,
        isGroup: Boolean,
        creatorId: String = "",
        initialStatus: String? = null
    ): Result<String> {
        var targetUser = if (!isGroup && participantIds.size == 2) {
            val otherId = participantIds.firstOrNull { it != creatorId }
            LocalDataStore.usersFlow.value.firstOrNull { it.id == otherId }
        } else null

        if (targetUser == null && isUsingCustomDatabase() && firestore != null && !isGroup && participantIds.size == 2) {
            val otherId = participantIds.firstOrNull { it != creatorId } ?: ""
            if (otherId.isNotBlank()) {
                targetUser = getUser(otherId)
            }
        }

        val isStudentToStudent = !isGroup && participantIds.size == 2 &&
                currentUser?.role == Role.STUDENT &&
                (targetUser?.role == Role.STUDENT || (targetUser == null && !name.contains("أستاذ") && !name.contains("Teacher") && !name.contains("Admin") && !name.contains("مدير")))

        val effectiveStatus = initialStatus ?: if (isStudentToStudent) "PENDING" else "ACCEPTED"
        val reqSender = if (effectiveStatus == "PENDING") creatorId else ""
        val reqReceiver = if (effectiveStatus == "PENDING") (participantIds.firstOrNull { it != creatorId } ?: "") else ""

        if (!isUsingCustomDatabase() || firestore == null) {
            val existing = LocalDataStore.conversationsFlow.value.firstOrNull { conv ->
                !conv.isGroup && !isGroup && conv.participantIds.containsAll(participantIds) && participantIds.containsAll(conv.participantIds)
            }
            if (existing != null) return Result.success(existing.id)

            val id = "conv_${UUID.randomUUID().toString().take(8)}"
            val newConv = ChatConversation(
                id = id,
                name = name,
                isGroup = isGroup,
                participantIds = participantIds,
                creatorId = creatorId,
                lastMessage = "",
                lastMessageTime = System.currentTimeMillis(),
                status = effectiveStatus,
                requestSenderId = reqSender,
                requestReceiverId = reqReceiver
            )
            LocalDataStore.conversationsFlow.value = listOf(newConv) + LocalDataStore.conversationsFlow.value
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            if (!isGroup && participantIds.size == 2) {
                val existing = db.collection("conversations")
                    .whereEqualTo("isGroup", false)
                    .whereArrayContains("participantIds", participantIds[0])
                    .get()
                    .await()
                val match = existing.documents.firstOrNull { doc ->
                    val parts = (doc.get("participantIds") as? List<*>)?.filterIsInstance<String>()
                    parts != null && parts.contains(participantIds[1])
                }
                if (match != null) {
                    return Result.success(match.id)
                }
            }

            val docRef = db.collection("conversations").document()
            val conv = ChatConversation(
                id = docRef.id,
                name = name,
                isGroup = isGroup,
                participantIds = participantIds,
                creatorId = creatorId,
                lastMessage = "",
                lastMessageTime = System.currentTimeMillis(),
                status = effectiveStatus,
                requestSenderId = reqSender,
                requestReceiverId = reqReceiver
            )
            docRef.set(conv.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptChatRequest(conversationId: String): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.conversationsFlow.value = LocalDataStore.conversationsFlow.value.map {
                if (it.id == conversationId) it.copy(status = "ACCEPTED") else it
            }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("conversations").document(conversationId)
                .update("status", "ACCEPTED")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineChatRequest(conversationId: String): Result<Unit> {
        return deleteConversation(conversationId)
    }

    fun observeConversation(conversationId: String): Flow<ChatConversation?> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.conversationsFlow.map { list ->
                list.firstOrNull { it.id == conversationId }
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(null)
                close()
                return@callbackFlow
            }
            val listener = db.collection("conversations").document(conversationId)
                .addSnapshotListener { snap, _ ->
                    if (snap != null && snap.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val conv = ChatConversation(
                            id = snap.id,
                            name = snap.getString("name") ?: "",
                            isGroup = snap.getBoolean("isGroup") ?: false,
                            photoUrl = snap.getString("photoUrl") ?: "",
                            participantIds = (snap.get("participantIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            creatorId = snap.getString("creatorId") ?: "",
                            lastMessage = snap.getString("lastMessage") ?: "",
                            lastMessageTime = snap.getLong("lastMessageTime") ?: System.currentTimeMillis(),
                            unreadMap = (snap.get("unreadMap") as? Map<String, Long>)?.mapValues { entry -> entry.value.toInt() } ?: emptyMap(),
                            status = snap.getString("status") ?: "ACCEPTED",
                            requestSenderId = snap.getString("requestSenderId") ?: "",
                            requestReceiverId = snap.getString("requestReceiverId") ?: ""
                        )
                        trySend(conv)
                    } else {
                        trySend(null)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun updateGroupMembers(conversationId: String, newMembers: List<String>): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.conversationsFlow.value = LocalDataStore.conversationsFlow.value.map {
                if (it.id == conversationId) it.copy(participantIds = newMembers) else it
            }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("conversations").document(conversationId)
                .update("participantIds", newMembers)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            LocalDataStore.conversationsFlow.value = LocalDataStore.conversationsFlow.value.filter { it.id != conversationId }
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("conversations").document(conversationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeConversations(userId: String): Flow<List<ChatConversation>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.conversationsFlow.map { list ->
                if (currentUser?.role == Role.ADMIN) list
                else list.filter { it.participantIds.contains(userId) }
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val query = if (currentUser?.role == Role.ADMIN) {
                db.collection("conversations").orderBy("lastMessageTime", Query.Direction.DESCENDING)
            } else {
                db.collection("conversations")
                    .whereArrayContains("participantIds", userId)
            }

            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    @Suppress("UNCHECKED_CAST")
                    ChatConversation(
                        id = it.id,
                        name = it.getString("name") ?: "",
                        isGroup = it.getBoolean("isGroup") ?: false,
                        photoUrl = it.getString("photoUrl") ?: "",
                        participantIds = (it.get("participantIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        creatorId = it.getString("creatorId") ?: "",
                        lastMessage = it.getString("lastMessage") ?: "",
                        lastMessageTime = it.getLong("lastMessageTime") ?: System.currentTimeMillis(),
                        unreadMap = (it.get("unreadMap") as? Map<String, Long>)?.mapValues { entry -> entry.value.toInt() } ?: emptyMap(),
                        status = it.getString("status") ?: "ACCEPTED",
                        requestSenderId = it.getString("requestSenderId") ?: "",
                        requestReceiverId = it.getString("requestReceiverId") ?: ""
                    )
                }?.sortedByDescending { it.lastMessageTime } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        message: ChatMessage
    ): Result<String> {
        val preview = when {
            message.messageText.isNotEmpty() -> message.messageText
            message.imageUrl.isNotEmpty() -> "📷 Photo"
            message.audioUrl.isNotEmpty() -> "🎤 Voice Note"
            message.documentUrl.isNotEmpty() -> "📄 ${message.documentName.ifBlank { "Document" }}"
            else -> "Message"
        }

        if (!isUsingCustomDatabase() || firestore == null) {
            val msgId = "msg_${UUID.randomUUID().toString().take(8)}"
            val created = message.copy(id = msgId, conversationId = conversationId, timestamp = System.currentTimeMillis())
            val currentMap = LocalDataStore.messagesMapFlow.value.toMutableMap()
            val currentList = currentMap[conversationId] ?: emptyList()
            currentMap[conversationId] = currentList + created
            LocalDataStore.messagesMapFlow.value = currentMap

            LocalDataStore.conversationsFlow.value = LocalDataStore.conversationsFlow.value.map {
                if (it.id == conversationId) it.copy(lastMessage = preview, lastMessageTime = created.timestamp) else it
            }
            return Result.success(msgId)
        }

        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document()

            docRef.set(message.toMap()).await()

            db.collection("conversations").document(conversationId).update(
                mapOf(
                    "lastMessage" to preview,
                    "lastMessageTime" to message.timestamp
                )
            ).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val currentMap = LocalDataStore.messagesMapFlow.value.toMutableMap()
            val currentList = currentMap[conversationId] ?: emptyList()
            currentMap[conversationId] = currentList.filter { it.id != messageId }
            LocalDataStore.messagesMapFlow.value = currentMap
            return Result.success(Unit)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.messagesMapFlow.map { map ->
                map[conversationId] ?: emptyList()
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val query = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)

            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    ChatMessage(
                        id = it.id,
                        conversationId = conversationId,
                        senderId = it.getString("senderId") ?: "",
                        senderName = it.getString("senderName") ?: "",
                        senderRole = it.getString("senderRole") ?: "STUDENT",
                        messageText = it.getString("messageText") ?: "",
                        imageUrl = it.getString("imageUrl") ?: "",
                        audioUrl = it.getString("audioUrl") ?: "",
                        audioDurationSeconds = (it.getLong("audioDurationSeconds") ?: 0L).toInt(),
                        documentUrl = it.getString("documentUrl") ?: "",
                        documentName = it.getString("documentName") ?: "",
                        documentSize = it.getLong("documentSize") ?: 0L,
                        documentType = it.getString("documentType") ?: "",
                        timestamp = it.getLong("timestamp") ?: System.currentTimeMillis(),
                        readBy = (it.get("readBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // SUBJECT Q&A SPACE
    // ----------------------------------------------------

    suspend fun postQaQuestion(post: SubjectQaPost): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val id = "qa_${UUID.randomUUID().toString().take(8)}"
            val created = post.copy(id = id, timestamp = System.currentTimeMillis())
            LocalDataStore.qaPostsFlow.value = listOf(created) + LocalDataStore.qaPostsFlow.value
            return Result.success(id)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("qa_posts").document()
            docRef.set(post.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun replyQaQuestion(reply: SubjectQaReply): Result<String> {
        if (!isUsingCustomDatabase() || firestore == null) {
            val replyId = "rep_${UUID.randomUUID().toString().take(8)}"
            val created = reply.copy(id = replyId, timestamp = System.currentTimeMillis())
            val currentMap = LocalDataStore.qaRepliesMapFlow.value.toMutableMap()
            val list = currentMap[reply.postId] ?: emptyList()
            currentMap[reply.postId] = list + created
            LocalDataStore.qaRepliesMapFlow.value = currentMap

            LocalDataStore.qaPostsFlow.value = LocalDataStore.qaPostsFlow.value.map {
                if (it.id == reply.postId) it.copy(repliesCount = it.repliesCount + 1) else it
            }
            return Result.success(replyId)
        }
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("qa_posts")
                .document(reply.postId)
                .collection("replies")
                .document()
            docRef.set(reply.toMap()).await()

            val postRef = db.collection("qa_posts").document(reply.postId)
            db.runTransaction { tx ->
                val snap = tx.get(postRef)
                val currentCount = snap.getLong("repliesCount") ?: 0L
                tx.update(postRef, "repliesCount", currentCount + 1)
            }.await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeQaPosts(subjectId: String? = null): Flow<List<SubjectQaPost>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.qaPostsFlow.map { list ->
                if (subjectId != null) list.filter { it.subjectId == subjectId } else list
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            var query: Query = db.collection("qa_posts").orderBy("timestamp", Query.Direction.DESCENDING)
            if (subjectId != null) {
                query = query.whereEqualTo("subjectId", subjectId)
            }
            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    SubjectQaPost(
                        id = it.id,
                        subjectId = it.getString("subjectId") ?: "",
                        subjectName = it.getString("subjectName") ?: "",
                        authorId = it.getString("authorId") ?: "",
                        authorName = it.getString("authorName") ?: "",
                        authorRole = it.getString("authorRole") ?: "",
                        questionText = it.getString("questionText") ?: "",
                        timestamp = it.getLong("timestamp") ?: System.currentTimeMillis(),
                        repliesCount = (it.getLong("repliesCount") ?: 0L).toInt()
                    )
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    fun observeQaReplies(postId: String): Flow<List<SubjectQaReply>> {
        if (!isUsingCustomDatabase() || firestore == null) {
            return LocalDataStore.qaRepliesMapFlow.map { map ->
                map[postId] ?: emptyList()
            }
        }
        return callbackFlow {
            val db = firestore
            if (db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val query = db.collection("qa_posts")
                .document(postId)
                .collection("replies")
                .orderBy("timestamp", Query.Direction.ASCENDING)
            val listener = query.addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    SubjectQaReply(
                        id = it.id,
                        postId = postId,
                        authorId = it.getString("authorId") ?: "",
                        authorName = it.getString("authorName") ?: "",
                        authorRole = it.getString("authorRole") ?: "",
                        replyText = it.getString("replyText") ?: "",
                        timestamp = it.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
    }

    // ----------------------------------------------------
    // STORAGE FILE UPLOAD (Cloud Storage with resilient inline data fallback)
    // ----------------------------------------------------

    suspend fun uploadFile(
        fileUri: Uri,
        storagePath: String
    ): Result<String> {
        val st = storage
        if (st != null) {
            try {
                val ref = st.reference.child(storagePath)
                ref.putFile(fileUri).await()
                val downloadUrl = ref.downloadUrl.await()
                return Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Storage uploadFile note: ${e.message}, falling back to optimized inline data")
            }
        }

        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(fileUri) ?: "image/jpeg"
            val bytes = if (mimeType.startsWith("image/")) {
                val inputStream = contentResolver.openInputStream(fileUri)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val maxDimension = 1024
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val scale = if (width > maxDimension || height > maxDimension) {
                        maxDimension.toFloat() / maxOf(width, height)
                    } else {
                        1.0f
                    }

                    val scaledBitmap = if (scale < 1.0f) {
                        android.graphics.Bitmap.createScaledBitmap(
                            originalBitmap,
                            (width * scale).toInt(),
                            (height * scale).toInt(),
                            true
                        )
                    } else {
                        originalBitmap
                    }

                    var quality = 75
                    var stream = java.io.ByteArrayOutputStream()
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream)
                    var compressedBytes = stream.toByteArray()
                    while (compressedBytes.size > 400 * 1024 && quality > 30) {
                        quality -= 15
                        stream = java.io.ByteArrayOutputStream()
                        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream)
                        compressedBytes = stream.toByteArray()
                    }
                    compressedBytes
                } else {
                    contentResolver.openInputStream(fileUri)?.use { it.readBytes() } ?: ByteArray(0)
                }
            } else {
                contentResolver.openInputStream(fileUri)?.use { it.readBytes() } ?: ByteArray(0)
            }

            if (bytes.isNotEmpty()) {
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                Result.success("data:$mimeType;base64,$base64")
            } else {
                Result.failure(Exception("File is empty"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading file bytes: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadLocalFile(
        file: File,
        storagePath: String
    ): Result<String> {
        val st = storage
        if (st != null) {
            try {
                val ref = st.reference.child(storagePath)
                ref.putFile(Uri.fromFile(file)).await()
                val downloadUrl = ref.downloadUrl.await()
                return Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Storage uploadLocalFile note: ${e.message}, falling back to inline audio")
            }
        }

        return try {
            val bytes = file.readBytes()
            if (bytes.isNotEmpty()) {
                val mimeType = if (file.name.endsWith(".m4a", true)) "audio/m4a" else "audio/mp4"
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                Result.success("data:$mimeType;base64,$base64")
            } else {
                Result.failure(Exception("Audio file is empty"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading audio file: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadDocument(
        fileUri: Uri,
        fileName: String,
        storagePath: String
    ): Result<String> {
        val st = storage
        if (st != null) {
            try {
                val ref = st.reference.child(storagePath)
                ref.putFile(fileUri).await()
                val downloadUrl = ref.downloadUrl.await()
                return Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Storage uploadDocument note: ${e.message}, falling back to inline document")
            }
        }

        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(fileUri) ?: when {
                fileName.endsWith(".pdf", true) -> "application/pdf"
                fileName.endsWith(".doc", true) || fileName.endsWith(".docx", true) -> "application/msword"
                fileName.endsWith(".txt", true) -> "text/plain"
                else -> "application/octet-stream"
            }
            val bytes = contentResolver.openInputStream(fileUri)?.use { it.readBytes() } ?: ByteArray(0)
            if (bytes.size > 850 * 1024) {
                return Result.failure(Exception("File size (${bytes.size / 1024} KB) exceeds 850KB limit for inline sync. Please use a smaller file or ensure Firebase Storage is configured."))
            }
            if (bytes.isNotEmpty()) {
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                Result.success("data:$mimeType;base64,$base64")
            } else {
                Result.failure(Exception("Document is empty"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading document: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadDocumentFile(
        file: File,
        fileName: String,
        storagePath: String
    ): Result<String> {
        val st = storage
        if (st != null) {
            try {
                val ref = st.reference.child(storagePath)
                ref.putFile(Uri.fromFile(file)).await()
                val downloadUrl = ref.downloadUrl.await()
                return Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Storage uploadDocumentFile note: ${e.message}, falling back to inline document")
            }
        }

        return try {
            val bytes = file.readBytes()
            val mimeType = when {
                fileName.endsWith(".pdf", true) -> "application/pdf"
                fileName.endsWith(".doc", true) || fileName.endsWith(".docx", true) -> "application/msword"
                fileName.endsWith(".txt", true) -> "text/plain"
                else -> "application/octet-stream"
            }
            if (bytes.size > 850 * 1024) {
                return Result.failure(Exception("File size (${bytes.size / 1024} KB) exceeds 850KB limit for inline sync. Please use a smaller file or ensure Firebase Storage is configured."))
            }
            if (bytes.isNotEmpty()) {
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                Result.success("data:$mimeType;base64,$base64")
            } else {
                Result.failure(Exception("Document is empty"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading document file: ${e.message}", e)
            Result.failure(e)
        }
    }
}
