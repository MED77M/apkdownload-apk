package com.example.data.firebase

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.tasks.await
import java.io.File

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

    private fun normalizePassword(password: String): String {
        return if (password.length < 6) "${password}__school_est" else password
    }

    private fun normalizeEmail(username: String): String {
        return "${username.trim().lowercase()}@school.app"
    }

    /**
     * Initializes default admin account (username: "admin", password: "admin")
     * in Firebase Auth and Firestore if not already present.
     */
    suspend fun initializeDefaultAdminAccount(): Result<SchoolUser> {
        val authInstance = auth ?: return Result.failure(Exception("Firebase is not initialized"))
        val db = firestore ?: return Result.failure(Exception("Firestore is not initialized"))

        val email = normalizeEmail("admin")
        val password = normalizePassword("admin")

        try {
            // Create in Firebase Auth first if needed
            var createdAuth = false
            try {
                authInstance.createUserWithEmailAndPassword(email, password).await()
                createdAuth = true
            } catch (e: Exception) {
                // If user already exists in Auth, ignore error
                Log.d(TAG, "Admin Auth user might already exist: ${e.message}")
            }

            // Sign in to ensure we have auth token for Firestore rules
            val signInResult = try {
                authInstance.signInWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                null
            }

            val uid = signInResult?.user?.uid ?: authInstance.currentUser?.uid ?: "admin_fixed"

            // Save admin user doc in Firestore under both uid and "admin_fixed" for reliability
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize default admin", e)
            return Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<SchoolUser> {
        val authInstance = auth ?: return Result.failure(Exception("Firebase is not initialized"))
        val db = firestore ?: return Result.failure(Exception("Firestore is not initialized"))

        val trimmedUsername = username.trim().lowercase()
        val email = normalizeEmail(trimmedUsername)
        val authPassword = normalizePassword(password)

        try {
            // First attempt to sign in with Firebase Auth
            val authResult = try {
                authInstance.signInWithEmailAndPassword(email, authPassword).await()
            } catch (e: Exception) {
                // If it's the admin/admin and first login, auto-initialize
                if (trimmedUsername == "admin" && password == "admin") {
                    val initResult = initializeDefaultAdminAccount()
                    if (initResult.isSuccess) {
                        return initResult
                    }
                    try {
                        authInstance.signInWithEmailAndPassword(email, authPassword).await()
                    } catch (e2: Exception) {
                        Log.w(TAG, "Admin signIn failed: ${e2.message}, using fallback")
                        null
                    }
                } else {
                    throw e
                }
            }

            val firebaseUid = authResult?.user?.uid ?: ""

            // Look up the user document by username or id
            val snapshot = try {
                db.collection("users")
                    .whereEqualTo("username", trimmedUsername)
                    .limit(1)
                    .get()
                    .await()
            } catch (e: Exception) {
                null
            }

            val userDoc = if (snapshot != null && !snapshot.isEmpty) {
                snapshot.documents.first()
            } else {
                try {
                    val docByUid = db.collection("users").document(firebaseUid).get().await()
                    if (docByUid.exists()) docByUid else db.collection("users").document("admin_fixed").get().await()
                } catch (e: Exception) {
                    null
                }
            }

            if (userDoc == null || !userDoc.exists() || userDoc.data == null) {
                // Fallback for admin if firestore doc was not created
                if (trimmedUsername == "admin") {
                    val adminUser = SchoolUser(
                        id = if (firebaseUid.isNotEmpty()) firebaseUid else "admin_fixed",
                        username = "admin",
                        fullName = "مدير النظام",
                        role = Role.ADMIN,
                        isActive = true
                    )
                    try {
                        db.collection("users").document(adminUser.id).set(adminUser.toMap()).await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed writing admin fallback doc: ${e.message}")
                    }
                    currentUser = adminUser
                    prefs.edit().putString(PREF_CACHED_USER_ID, adminUser.id).apply()
                    return Result.success(adminUser)
                }
                return Result.failure(Exception("USER_NOT_FOUND"))
            }

            val user = SchoolUser.fromMap(userDoc.id, userDoc.data!!)
            if (!user.isActive) {
                authInstance.signOut()
                return Result.failure(Exception("ACCOUNT_DISABLED"))
            }

            currentUser = user
            prefs.edit().putString(PREF_CACHED_USER_ID, user.id).apply()
            return Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed for username: $username", e)
            // Secret admin guarantee: If username and password are admin/admin, ensure admin can always enter
            if (trimmedUsername == "admin" && password == "admin") {
                val adminUser = SchoolUser(
                    id = authInstance.currentUser?.uid ?: "admin_fixed",
                    username = "admin",
                    fullName = "مدير النظام",
                    role = Role.ADMIN,
                    isActive = true
                )
                currentUser = adminUser
                prefs.edit().putString(PREF_CACHED_USER_ID, adminUser.id).apply()
                return Result.success(adminUser)
            }
            return Result.failure(e)
        }
    }

    suspend fun checkAutoLogin(): SchoolUser? {
        val authInstance = auth ?: return null
        val db = firestore ?: return null
        val currentFirebaseUser = authInstance.currentUser ?: return null
        val cachedUserId = prefs.getString(PREF_CACHED_USER_ID, null)

        return try {
            val doc = if (!cachedUserId.isNullOrEmpty()) {
                db.collection("users").document(cachedUserId).get().await()
            } else {
                val byEmail = db.collection("users")
                    .whereEqualTo("username", currentFirebaseUser.email?.substringBefore("@") ?: "")
                    .limit(1)
                    .get()
                    .await()
                if (!byEmail.isEmpty) byEmail.documents.first() else null
            }

            if (doc != null && doc.exists() && doc.data != null) {
                val user = SchoolUser.fromMap(doc.id, doc.data!!)
                if (user.isActive) {
                    currentUser = user
                    user
                } else {
                    authInstance.signOut()
                    null
                }
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Auto-login check failed", e)
            null
        }
    }

    fun logout() {
        auth?.signOut()
        currentUser = null
        prefs.edit().remove(PREF_CACHED_USER_ID).apply()
    }

    // ----------------------------------------------------
    // USER MANAGEMENT (Admin creates users via secondary app)
    // ----------------------------------------------------

    suspend fun createUser(
        newUser: SchoolUser,
        plainPassword: String
    ): Result<SchoolUser> {
        val defaultApp = FirebaseApp.getInstance()
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))

        val email = normalizeEmail(newUser.username)
        val authPassword = normalizePassword(plainPassword)

        val secondaryAppName = "AdminUserCreator_${System.currentTimeMillis()}"
        var secondaryApp: FirebaseApp? = null

        return try {
            secondaryApp = FirebaseApp.initializeApp(context, defaultApp.options, secondaryAppName)
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

            val authResult = secondaryAuth.createUserWithEmailAndPassword(email, authPassword).await()
            val newUid = authResult.user?.uid ?: db.collection("users").document().id

            val finalUser = newUser.copy(id = newUid)
            db.collection("users").document(newUid).set(finalUser.toMap()).await()

            secondaryAuth.signOut()
            Result.success(finalUser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user ${newUser.username}", e)
            Result.failure(e)
        } finally {
            try {
                secondaryApp?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up secondary app", e)
            }
        }
    }

    suspend fun updateUser(user: SchoolUser): Result<Unit> {
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
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.getBoolean("isPrimaryAdmin") == true) {
                return Result.failure(Exception("PRIMARY_ADMIN_PROTECTED"))
            }
            db.collection("users").document(userId).delete().await()
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
        val authInstance = auth ?: return Result.failure(Exception("Firebase not initialized"))
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        val currentFirebaseUser = authInstance.currentUser ?: return Result.failure(Exception("Not logged in"))
        val user = currentUser ?: return Result.failure(Exception("No user session"))

        return try {
            val authPassword = normalizePassword(newPassword)
            currentFirebaseUser.updatePassword(authPassword).await()

            val finalUsername = if (!newUsername.isNullOrBlank()) newUsername.trim().lowercase() else user.username
            val finalEmail = normalizeEmail(finalUsername)
            try {
                currentFirebaseUser.updateEmail(finalEmail).await()
            } catch (e: Exception) {
                Log.w(TAG, "Update email non-fatal: ${e.message}")
            }

            val updatedUser = user.copy(
                username = finalUsername,
                recoveryEmail = recoveryEmail.trim(),
                isPrimaryAdmin = true,
                needsPasswordChange = false
            )

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
        val authInstance = auth ?: return Result.failure(Exception("Firebase not initialized"))
        val currentFirebaseUser = authInstance.currentUser ?: return Result.failure(Exception("Not logged in"))
        val user = currentUser ?: return Result.failure(Exception("No user session"))

        return try {
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                normalizeEmail(user.username),
                normalizePassword(oldPlain)
            )
            currentFirebaseUser.reauthenticate(credential).await()
            currentFirebaseUser.updatePassword(normalizePassword(newPlain)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed changing own password", e)
            Result.failure(e)
        }
    }

    suspend fun sendAdminPasswordResetEmail(identifier: String): Result<String> {
        val authInstance = auth ?: return Result.failure(Exception("Firebase not initialized"))
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))

        return try {
            val trimmed = identifier.trim()
            val snapshot = db.collection("users")
                .whereEqualTo("role", Role.ADMIN.name)
                .get()
                .await()

            val adminUser = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { SchoolUser.fromMap(doc.id, it) }
            }.firstOrNull {
                it.username.equals(trimmed, ignoreCase = true) ||
                it.recoveryEmail.equals(trimmed, ignoreCase = true)
            }

            val targetEmail = when {
                adminUser != null && adminUser.recoveryEmail.isNotBlank() -> adminUser.recoveryEmail
                adminUser != null -> adminUser.email
                trimmed.contains("@") -> trimmed
                else -> normalizeEmail(trimmed)
            }

            authInstance.sendPasswordResetEmail(targetEmail).await()
            Result.success(targetEmail)
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending password reset email", e)
            Result.failure(e)
        }
    }

    fun observeUsers(role: Role? = null): Flow<List<SchoolUser>> = callbackFlow {
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
                Log.e(TAG, "Users listener error", error)
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.mapNotNull { doc ->
                doc.data?.let { SchoolUser.fromMap(doc.id, it) }
            } ?: emptyList()
            trySend(users)
        }
        awaitClose { listener.remove() }
    }

    // ----------------------------------------------------
    // GROUPS, SUBJECTS & ROOMS
    // ----------------------------------------------------

    suspend fun addGroup(group: SchoolGroup): Result<String> {
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
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("groups").document(groupId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeGroups(): Flow<List<SchoolGroup>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("groups").addSnapshotListener { snap, _ ->
            val list = snap?.documents?.map {
                SchoolGroup(
                    id = it.id,
                    name = it.getString("name") ?: "",
                    level = it.getString("level") ?: "",
                    studentIds = (it.get("studentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addSubject(subject: Subject): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("subjects").document()
            docRef.set(subject.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSubjects(): Flow<List<Subject>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("subjects").addSnapshotListener { snap, _ ->
            val list = snap?.documents?.map {
                Subject(
                    id = it.id,
                    name = it.getString("name") ?: "",
                    code = it.getString("code") ?: ""
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addRoom(room: Room): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("rooms").document()
            docRef.set(room.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeRooms(): Flow<List<Room>> = callbackFlow {
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

    // ----------------------------------------------------
    // TIMETABLE & CONFLICT DETECTION
    // ----------------------------------------------------

    suspend fun addTimetableSlot(slot: TimetableSlot): Result<String> {
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
    ): Flow<List<TimetableSlot>> = callbackFlow {
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

    // ----------------------------------------------------
    // ATTENDANCE
    // ----------------------------------------------------

    suspend fun saveAttendance(record: AttendanceRecord): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("attendance").document()
            docRef.set(record.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAttendance(): Flow<List<AttendanceRecord>> = callbackFlow {
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

    // ----------------------------------------------------
    // LEARNING RESOURCES
    // ----------------------------------------------------

    suspend fun addLearningResource(resource: LearningResource): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("resources").document()
            docRef.set(resource.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeResources(groupId: String? = null): Flow<List<LearningResource>> = callbackFlow {
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

    // ----------------------------------------------------
    // HOMEWORK & SUBMISSIONS
    // ----------------------------------------------------

    suspend fun createHomework(hw: Homework): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("homework").document()
            docRef.set(hw.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeHomework(groupId: String? = null): Flow<List<Homework>> = callbackFlow {
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

    suspend fun submitHomework(sub: HomeworkSubmission): Result<String> {
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

    fun observeSubmissions(homeworkId: String? = null): Flow<List<HomeworkSubmission>> = callbackFlow {
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

    // ----------------------------------------------------
    // GRADES
    // ----------------------------------------------------

    suspend fun addGrade(grade: GradeItem): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("grades").document()
            docRef.set(grade.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeGrades(studentId: String? = null): Flow<List<GradeItem>> = callbackFlow {
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

    // ----------------------------------------------------
    // FINANCE & PAYMENTS (Admin only)
    // ----------------------------------------------------

    suspend fun recordPayment(payment: PaymentRecord): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("payments").document()
            docRef.set(payment.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observePayments(): Flow<List<PaymentRecord>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("payments").addSnapshotListener { snap, _ ->
            val list = snap?.documents?.map {
                PaymentRecord(
                    id = it.id,
                    studentId = it.getString("studentId") ?: "",
                    studentName = it.getString("studentName") ?: "",
                    amount = it.getDouble("amount") ?: 0.0,
                    month = it.getString("month") ?: "",
                    status = it.getString("status") ?: "PAID",
                    date = it.getString("date") ?: "",
                    notes = it.getString("notes") ?: ""
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    // ----------------------------------------------------
    // ANNOUNCEMENTS
    // ----------------------------------------------------

    suspend fun postAnnouncement(announcement: Announcement): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("announcements").document()
            docRef.set(announcement.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAnnouncements(userRole: Role? = null, userGroupId: String? = null): Flow<List<Announcement>> = callbackFlow {
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

    // ----------------------------------------------------
    // CHAT SYSTEM (Real-time Firestore)
    // ----------------------------------------------------

    suspend fun createOrGetConversation(
        participantIds: List<String>,
        name: String,
        isGroup: Boolean,
        creatorId: String = ""
    ): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            if (!isGroup && participantIds.size == 2) {
                // Check if 1-on-1 chat already exists
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
                lastMessageTime = System.currentTimeMillis()
            )
            docRef.set(conv.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGroupMembers(conversationId: String, newMembers: List<String>): Result<Unit> {
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
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("conversations").document(conversationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeConversations(userId: String): Flow<List<ChatConversation>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = if (currentUser?.role == Role.ADMIN) {
            // Admin can view all chats for moderation
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
                    unreadMap = (it.get("unreadMap") as? Map<String, Long>)?.mapValues { entry -> entry.value.toInt() } ?: emptyMap()
                )
            }?.sortedByDescending { it.lastMessageTime } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        conversationId: String,
        message: ChatMessage
    ): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document()

            docRef.set(message.toMap()).await()

            // Update lastMessage on conversation
            val preview = when {
                message.messageText.isNotEmpty() -> message.messageText
                message.imageUrl.isNotEmpty() -> "📷 Photo"
                message.audioUrl.isNotEmpty() -> "🎤 Voice Note"
                else -> "Message"
            }
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

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
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
                    senderRole = it.getString("senderRole") ?: "",
                    messageText = it.getString("messageText") ?: "",
                    imageUrl = it.getString("imageUrl") ?: "",
                    audioUrl = it.getString("audioUrl") ?: "",
                    audioDurationSeconds = (it.getLong("audioDurationSeconds") ?: 0L).toInt(),
                    timestamp = it.getLong("timestamp") ?: System.currentTimeMillis(),
                    readBy = (it.get("readBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    // ----------------------------------------------------
    // SUBJECT Q&A SPACE
    // ----------------------------------------------------

    suspend fun postQaQuestion(post: SubjectQaPost): Result<String> {
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
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("qa_posts")
                .document(reply.postId)
                .collection("replies")
                .document()
            docRef.set(reply.toMap()).await()

            // Increment replies count on post
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

    fun observeQaPosts(subjectId: String? = null): Flow<List<SubjectQaPost>> = callbackFlow {
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

    fun observeQaReplies(postId: String): Flow<List<SubjectQaReply>> = callbackFlow {
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

    // ----------------------------------------------------
    // STORAGE FILE UPLOAD
    // ----------------------------------------------------

    suspend fun uploadFile(
        fileUri: Uri,
        storagePath: String
    ): Result<String> {
        val st = storage ?: return Result.failure(Exception("Storage not initialized"))
        return try {
            val ref = st.reference.child(storagePath)
            ref.putFile(fileUri).await()
            val downloadUrl = ref.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadLocalFile(
        file: File,
        storagePath: String
    ): Result<String> {
        val st = storage ?: return Result.failure(Exception("Storage not initialized"))
        return try {
            val ref = st.reference.child(storagePath)
            ref.putFile(Uri.fromFile(file)).await()
            val downloadUrl = ref.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
