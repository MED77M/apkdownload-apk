package com.example.data.notification

import android.content.Context
import com.example.data.model.Role
import com.example.data.model.SchoolUser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.ConcurrentHashMap

object RealtimeNotificationObserver {

    private var announcementListener: ListenerRegistration? = null
    private val conversationListeners = ConcurrentHashMap<String, ListenerRegistration>()
    private var conversationsIndexListener: ListenerRegistration? = null

    private var appStartTime = System.currentTimeMillis()
    private var isStarted = false

    fun start(context: Context, currentUser: SchoolUser) {
        if (isStarted) return
        isStarted = true
        appStartTime = System.currentTimeMillis()

        val db = FirebaseFirestore.getInstance()

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
                        val title = doc.getString("title") ?: "إعلان جديد"
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

                        if (shouldNotify) {
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

        conversationsIndexListener = db.collection("conversations")
            .whereArrayContains("participantIds", currentUser.id)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                for (doc in snapshot.documents) {
                    val convId = doc.id
                    val convName = doc.getString("name") ?: "محادثة"
                    val isGroup = doc.getBoolean("isGroup") ?: false

                    if (!conversationListeners.containsKey(convId)) {
                        val msgListener = db.collection("conversations")
                            .document(convId)
                            .collection("messages")
                            .whereGreaterThan("createdAt", appStartTime)
                            .addSnapshotListener { msgSnap, msgErr ->
                                if (msgErr != null || msgSnap == null) return@addSnapshotListener

                                for (change in msgSnap.documentChanges) {
                                    if (change.type == DocumentChange.Type.ADDED) {
                                        val mDoc = change.document
                                        val senderId = mDoc.getString("senderId") ?: ""
                                        val senderName = mDoc.getString("senderName") ?: convName
                                        val text = mDoc.getString("text") ?: ""
                                        val type = mDoc.getString("type") ?: "TEXT"

                                        if (senderId != currentUser.id) {
                                            val displayBody = when (type) {
                                                "VOICE" -> "🎙️ تسجيل صوتي"
                                                "IMAGE" -> "📷 صورة"
                                                else -> text
                                            }

                                            SchoolNotificationManager.showMessageNotification(
                                                context = context.applicationContext,
                                                senderName = if (isGroup) "$convName ($senderName)" else senderName,
                                                messageText = displayBody,
                                                conversationId = convId,
                                                isGroup = isGroup
                                            )
                                        }
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
        announcementListener?.remove()
        announcementListener = null
        conversationsIndexListener?.remove()
        conversationsIndexListener = null
        conversationListeners.values.forEach { it.remove() }
        conversationListeners.clear()
    }
}
