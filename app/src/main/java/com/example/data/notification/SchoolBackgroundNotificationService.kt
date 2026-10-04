package com.example.data.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.firebase.FirebaseInitializer
import com.example.data.model.SchoolUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SchoolBackgroundNotificationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SchoolBackgroundNotificationService created")
        SchoolNotificationManager.initialize(applicationContext)
        BadgeManager.initialize(applicationContext)
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SchoolBackgroundNotificationService onStartCommand (startId: $startId)")
        startForegroundNotification()

        serviceScope.launch {
            try {
                FirebaseInitializer.initialize(applicationContext)
                val prefs = getSharedPreferences("school_prefs", Context.MODE_PRIVATE)
                val cachedUserId = prefs.getString("cached_user_id", null)

                if (cachedUserId.isNullOrBlank()) {
                    Log.d(TAG, "No cached user session found for background service")
                    return@launch
                }

                val db = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
                if (db != null) {
                    try {
                        val doc = db.collection("users").document(cachedUserId).get().await()
                        if (doc != null && doc.exists() && doc.data != null) {
                            val user = SchoolUser.fromMap(doc.id, doc.data!!)
                            if (user.isActive) {
                                RealtimeNotificationObserver.start(applicationContext, user)
                                Log.d(TAG, "RealtimeNotificationObserver successfully running in background for ${user.fullName}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to fetch user doc online in background: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in background service onStartCommand: ${e.message}", e)
            }
        }

        return START_STICKY
    }

    private fun startForegroundNotification() {
        try {
            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification: Notification = NotificationCompat.Builder(
                this,
                SchoolNotificationManager.CHANNEL_BACKGROUND_SERVICE_ID
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("science.est.center")
                .setContentText("نظام الإشعارات والرسائل قيد العمل لاستقبال التنبيهات في الخلفية")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setShowWhen(false)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start foreground notification: ${e.message}")
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "App task removed (swiped away). Scheduling service restart to keep notifications running...")
        try {
            val restartIntent = Intent(applicationContext, SchoolBackgroundNotificationService::class.java).apply {
                setPackage(packageName)
            }
            val pendingIntent = PendingIntent.getService(
                applicationContext,
                RESTART_REQUEST_CODE,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1500,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error scheduling restart alarm on task removed: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SchoolBackgroundNotificationService destroyed")
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "SchoolBgNotifService"
        private const val NOTIFICATION_ID = 9001
        private const val RESTART_REQUEST_CODE = 9002

        fun startService(context: Context) {
            try {
                val intent = Intent(context, SchoolBackgroundNotificationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start SchoolBackgroundNotificationService: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, SchoolBackgroundNotificationService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop SchoolBackgroundNotificationService: ${e.message}")
            }
        }
    }
}
