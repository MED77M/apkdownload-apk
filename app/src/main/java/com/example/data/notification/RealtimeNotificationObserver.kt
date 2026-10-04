package com.example.data.notification

import android.content.Context
import android.util.Log
import com.example.data.model.Role
import com.example.data.model.SchoolUser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.ConcurrentHashMap

object RealtimeNotificationObserver {

    private const val TAG = "RealtimeNotifObserver"
    private var announcementListener: ListenerRegistration? = null
    private val conversationListeners = ConcurrentHashMap<String, ListenerRegistration>()
    private var conversationsIndexListener: ListenerRegistration? = null
    private val notifiedMessageIds = ConcurrentHashMap.newKeySet<String>()

    private var appStartTime = System.currentTimeMillis()
    private var isStarted = false
    private var currentUserId: String? = null

    // Track which conversation is currently active on screen so we don't buzz the user while they are already viewing it
    @Volatile
    var activeConversationId: String? = null

    fun start(context: Context, currentUser: SchoolUser) {
        if (isStarted && currentUserId == currentUser.id) return
        stop()
        isStarted = true
        currentUserId = currentUser.id
        appStartTime = System.currentTimeMillis() - 1000 // Small buffer

        val db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}", e)
            null
        } ?: return

        Log.d(TAG, "Starting RealtimeNotificationObserver for user: ${currentUser.fullName} (${currentUser.role})")

        // 1. Observe Announcements
        observeAnnouncements(context, db, currentUser)

        // 2. Observe User's Conversations and Messages
        observeConversations(context, db, currentUser)
    }

    private fun observeAnnouncements(context: Context, db: FirebaseFirestore, currentUser: SchoolUser) {
        announcementListener?.remove()
        announcementListener = db.collection("announcements")
            .whereGreaterThan("createdAt", appStartTime)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                for (change in snapshot.documentChanges) {
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        if (!notifiedMessageIds.add(doc.id)) continue

                        val title = doc.getString("title") ?: "إعلان مدرسي جديد"
                        val body = doc.getString("body") ?: ""
                        val authorName = doc.getString("authorName") ?: ""
                        val targetAudience = doc.getString("targetAudience") ?: "ALL"
                        val targetGroupId = doc.getString("targetGroupId")

                        // Do not notify self
                        if (authorName.isNotBlank() && authorName.equals(currentUser.fullName, ignoreCase = true)) {
                            continue
                        }

                        // Check audience match
                        val shouldNotify = when (targetAudience) {
                            "ALL" -> true
                            "TEACHERS" -> currentUser.role == Role.TEACHER || currentUser.role == Role.ADMIN
                            "STUDENTS" -> currentUser.role == Role.STUDENT || currentUser.role == Role.ADMIN
                            "GROUP" -> targetGroupId != null && currentUser.groupIds.contains(targetGroupId)
                            else -> true
                        }

                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        if (shouldNotify) {
                            BadgeManager.onNewAnnouncementReceived(createdAt)
                            SchoolNotificationManager.showAnnouncementNotification(
                                context = context.applicationContext,
                                title = title,
                                body = body,
                                authorName = authorName,
                                announcementId = doc.id
                            )
                        }
                    }
                }
            }
    }

    private fun observeConversations(context: Context, db: FirebaseFirestore, currentUser: SchoolUser) {
        conversationsIndexListener?.remove()
        conversationListeners.values.forEach { it.remove() }
        conversationListeners.clear()

        val query = if (currentUser.role == Role.ADMIN) {
            db.collection("conversations")
        } else {
            db.collection("conversations").whereArrayContains("participantIds", currentUser.id)
        }

        conversationsIndexListener = query.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) {
                Log.w(TAG, "Conversations index listener error: ${e?.message}")
                return@addSnapshotListener
            }

            val hasAnyUnread = snapshot.documents.any { d ->
                val unreadMap = (d.get("unreadMap") as? Map<*, *>) ?: emptyMap<Any, Any>()
                val count = (unreadMap[currentUser.id] as? Number)?.toInt() ?: 0
                count > 0
            }
            if (hasAnyUnread) {
                BadgeManager.setUnreadMessages(true)
            }

            for (doc in snapshot.documents) {
                val convId = doc.id
                val convName = doc.getString("name") ?: "محادثة"
                val isGroup = doc.getBoolean("isGroup") ?: false

                if (!conversationListeners.containsKey(convId)) {
                    val msgListener = db.collection("conversations")
                        .document(convId)
                        .collection("messages")
                        .whereGreaterThan("timestamp", appStartTime)
                        .addSnapshotListener { msgSnap, msgErr ->
                            if (msgErr != null || msgSnap == null) return@addSnapshotListener

                            for (change in msgSnap.documentChanges) {
                                if (change.type == DocumentChange.Type.ADDED) {
                                    val mDoc = change.document
                                    val messageId = mDoc.id
                                    if (!notifiedMessageIds.add(messageId)) continue

                                    val senderId = mDoc.getString("senderId") ?: ""
                                    val senderName = mDoc.getString("senderName") ?: convName
                                    val messageText = mDoc.getString("messageText") ?: ""
                                    val imageUrl = mDoc.getString("imageUrl") ?: ""
                                    val audioUrl = mDoc.getString("audioUrl") ?: ""
                                    val documentUrl = mDoc.getString("documentUrl") ?: ""
                                    val documentName = mDoc.getString("documentName") ?: "مستند"

                                    // Don't notify self
                                    if (senderId.isNotBlank() && senderId == currentUser.id) {
                                        continue
                                    }

                                    // Trigger badge indicator
                                    BadgeManager.onNewMessageReceived(convId)

                                    // If user is currently looking at this exact chat screen, don't buzz with heads-up
                                    if (activeConversationId == convId) {
                                        continue
                                    }

                                    val displayBody = when {
                                        imageUrl.isNotEmpty() -> "📷 صورة جديدة"
                                        audioUrl.isNotEmpty() -> "🎙️ تسجيل صوتي"
                                        documentUrl.isNotEmpty() -> "📄 $documentName"
                                        else -> messageText.ifBlank { "رسالة جديدة" }
                                    }

                                    Log.d(TAG, "Showing notification for message from $senderName in $convId")
                                    SchoolNotificationManager.showMessageNotification(
                                        context = context.applicationContext,
                                        senderName = if (isGroup) "$convName: $senderName" else senderName,
                                        messageText = displayBody,
                                        conversationId = convId,
                                        isGroup = isGroup
                                    )
                                }
                            }
                        }
                    conversationListeners[convId] = msgListener
                }
            }
        }
    }

    fun stop() {
        isStarted = false
        currentUserId = null
        announcementListener?.remove()
        announcementListener = null
        conversationsIndexListener?.remove()
        conversationsIndexListener = null
        conversationListeners.values.forEach { it.remove() }
        conversationListeners.clear()
        notifiedMessageIds.clear()
    }
}
