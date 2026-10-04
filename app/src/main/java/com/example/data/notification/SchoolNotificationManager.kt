package com.example.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Role
import com.example.data.model.SchoolUser
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.atomic.AtomicInteger

object SchoolNotificationManager {

    const val CHANNEL_MESSAGES_ID = "school_messages_channel"
    const val CHANNEL_ANNOUNCEMENTS_ID = "school_announcements_channel"
    const val CHANNEL_GENERAL_ID = "school_general_channel"

    private val notificationIdCounter = AtomicInteger(1000)

    fun initialize(context: Context) {
        createNotificationChannels(context)
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Messages Channel
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                "رسائل المحادثات / Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الرسائل الجديدة للطلاب والأساتذة"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }

            // 2. Announcements Channel
            val announcementsChannel = NotificationChannel(
                CHANNEL_ANNOUNCEMENTS_ID,
                "إعلانات المدرسة / School Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إعلانات وتنبيهات المدرسة الهامة"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }

            // 3. General Updates Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL_ID,
                "تحديثات النظام / General Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات عامة وتنبيهات الحصص"
            }

            notificationManager.createNotificationChannels(listOf(messagesChannel, announcementsChannel, generalChannel))
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showMessageNotification(
        context: Context,
        senderName: String,
        messageText: String,
        conversationId: String = "",
        isGroup: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "chat")
            putExtra("conversationId", conversationId)
            putExtra("conversationTitle", senderName)
            putExtra("isGroup", isGroup)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdCounter.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val title = if (isGroup) "💬 $senderName" else "💬 رسالة من $senderName"

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationIdCounter.incrementAndGet(), builder.build())
        } catch (_: SecurityException) {
            // Permission check covered above
        }
    }

    fun showAnnouncementNotification(
        context: Context,
        title: String,
        body: String,
        authorName: String = "",
        announcementId: String = ""
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "announcements")
            putExtra("announcementId", announcementId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdCounter.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationTitle = "📢 إعلان مدرسي: $title"
        val fullContent = if (authorName.isNotBlank()) "$body\n(نشر بواسطة: $authorName)" else body

        val builder = NotificationCompat.Builder(context, CHANNEL_ANNOUNCEMENTS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notificationTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullContent))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationIdCounter.incrementAndGet(), builder.build())
        } catch (_: SecurityException) {
            // Handled
        }
    }

    fun sendTestNotification(context: Context) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔔 science.est.center")
            .setContentText("خاصية الإشعارات مفعلة وتعمل بنجاح على هاتفك!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(9999, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun fetchAndSaveFcmToken(context: Context, currentUser: SchoolUser? = null) {
        // Realtime notifications are delivered locally via RealtimeNotificationObserver
    }
}
