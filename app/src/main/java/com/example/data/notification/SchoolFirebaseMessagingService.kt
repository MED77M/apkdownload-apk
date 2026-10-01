package com.example.data.notification

import com.example.data.firebase.FirebaseManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SchoolFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val firebaseManager = FirebaseManager.getInstance(applicationContext)
        val currentUser = firebaseManager.currentUser
        if (currentUser != null && token.isNotBlank()) {
            SchoolNotificationManager.fetchAndSaveFcmToken(applicationContext, currentUser)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val type = data["type"] ?: "message"
        val title = notification?.title ?: data["title"] ?: "إشعار جديد"
        val body = notification?.body ?: data["body"] ?: ""

        when (type) {
            "announcement" -> {
                val author = data["authorName"] ?: ""
                val announcementId = data["announcementId"] ?: ""
                SchoolNotificationManager.showAnnouncementNotification(
                    context = applicationContext,
                    title = title,
                    body = body,
                    authorName = author,
                    announcementId = announcementId
                )
            }
            "message", "chat" -> {
                val senderName = data["senderName"] ?: title
                val conversationId = data["conversationId"] ?: ""
                val isGroup = data["isGroup"]?.toBoolean() ?: false
                SchoolNotificationManager.showMessageNotification(
                    context = applicationContext,
                    senderName = senderName,
                    messageText = body,
                    conversationId = conversationId,
                    isGroup = isGroup
                )
            }
            else -> {
                SchoolNotificationManager.showMessageNotification(
                    context = applicationContext,
                    senderName = title,
                    messageText = body
                )
            }
        }
    }
}
