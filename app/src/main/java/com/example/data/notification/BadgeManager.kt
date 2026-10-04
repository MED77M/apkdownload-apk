package com.example.data.notification

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BadgeManager {

    private const val TAG = "BadgeManager"
    private const val PREFS_NAME = "badge_prefs"
    private const val KEY_LAST_SEEN_ANNOUNCEMENT = "last_seen_announcement_time"
    private const val KEY_HAS_UNREAD_MESSAGES = "has_unread_messages"

    private val _hasUnreadMessages = MutableStateFlow(false)
    val hasUnreadMessages: StateFlow<Boolean> = _hasUnreadMessages.asStateFlow()

    private val _hasUnreadAnnouncements = MutableStateFlow(false)
    val hasUnreadAnnouncements: StateFlow<Boolean> = _hasUnreadAnnouncements.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _hasUnreadMessages.value = prefs?.getBoolean(KEY_HAS_UNREAD_MESSAGES, false) ?: false
        }
    }

    fun onNewMessageReceived(conversationId: String) {
        // If the user is currently inside this specific chat conversation, don't flag unread
        if (RealtimeNotificationObserver.activeConversationId == conversationId) {
            return
        }
        Log.d(TAG, "New unread message received for conversation: $conversationId")
        _hasUnreadMessages.value = true
        prefs?.edit()?.putBoolean(KEY_HAS_UNREAD_MESSAGES, true)?.apply()
    }

    fun setUnreadMessages(hasUnread: Boolean) {
        _hasUnreadMessages.value = hasUnread
        prefs?.edit()?.putBoolean(KEY_HAS_UNREAD_MESSAGES, hasUnread)?.apply()
    }

    fun clearMessagesBadge() {
        Log.d(TAG, "Clearing messages badge indicator")
        _hasUnreadMessages.value = false
        prefs?.edit()?.putBoolean(KEY_HAS_UNREAD_MESSAGES, false)?.apply()
    }

    fun onNewAnnouncementReceived(createdAt: Long = System.currentTimeMillis()) {
        val lastSeen = prefs?.getLong(KEY_LAST_SEEN_ANNOUNCEMENT, 0L) ?: 0L
        if (createdAt > lastSeen) {
            Log.d(TAG, "New unread announcement received at $createdAt (last seen: $lastSeen)")
            _hasUnreadAnnouncements.value = true
        }
    }

    fun setUnreadAnnouncements(hasUnread: Boolean) {
        _hasUnreadAnnouncements.value = hasUnread
    }

    fun clearAnnouncementsBadge() {
        Log.d(TAG, "Clearing announcements badge indicator")
        _hasUnreadAnnouncements.value = false
        prefs?.edit()?.putLong(KEY_LAST_SEEN_ANNOUNCEMENT, System.currentTimeMillis())?.apply()
    }

    fun getLastSeenAnnouncementTime(): Long {
        return prefs?.getLong(KEY_LAST_SEEN_ANNOUNCEMENT, 0L) ?: 0L
    }
}
