package com.example.ui.announcements

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Announcement
import com.example.data.model.Role
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatusBadge
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import com.example.ui.theme.SchoolWarning
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnnouncementsScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = firebaseManager.currentUser

    val announcements by firebaseManager.observeAnnouncements(
        userRole = currentUser?.role,
        userGroupId = currentUser?.groupIds?.firstOrNull()
    ).collectAsState(initial = emptyList())

    val canCreate = currentUser?.role == Role.ADMIN ||
            (currentUser?.role == Role.TEACHER && (currentUser.teacherPermissions.canSendAnnouncements))

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.announcementsTitle,
                subtitle = strings.appName,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            if (canCreate) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = SchoolPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                    text = { Text(strings.createAnnouncement) },
                    modifier = Modifier.testTag("fab_create_announcement")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (announcements.isEmpty()) {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.Campaign,
                    actionText = if (canCreate) strings.createAnnouncement else null,
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(announcements, key = { it.id }) { item ->
                        val dateStr = remember(item.createdAt) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.createdAt))
                        }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "By ${item.authorName} • 📅 $dateStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusBadge(
                                        text = when (item.targetAudience) {
                                            "TEACHERS" -> strings.audienceTeachers
                                            "STUDENTS" -> strings.audienceStudents
                                            else -> strings.audienceAll
                                        },
                                        color = when (item.targetAudience) {
                                            "TEACHERS" -> SchoolSecondary
                                            "STUDENTS" -> SchoolPrimary
                                            else -> SchoolWarning
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = item.body,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        var targetAudience by remember { mutableStateOf("ALL") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(strings.createAnnouncement) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(strings.announcementTitle) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text(strings.announcementBody) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = targetAudience == "ALL",
                            onClick = { targetAudience = "ALL" },
                            label = { Text(strings.audienceAll) }
                        )
                        FilterChip(
                            selected = targetAudience == "TEACHERS",
                            onClick = { targetAudience = "TEACHERS" },
                            label = { Text(strings.audienceTeachers) }
                        )
                        FilterChip(
                            selected = targetAudience == "STUDENTS",
                            onClick = { targetAudience = "STUDENTS" },
                            label = { Text(strings.audienceStudents) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && body.isNotBlank()) {
                            val announcement = Announcement(
                                title = title.trim(),
                                body = body.trim(),
                                targetAudience = targetAudience,
                                authorName = currentUser?.fullName ?: "Administration"
                            )
                            scope.launch {
                                val res = firebaseManager.postAnnouncement(announcement)
                                res.onSuccess { showCreateDialog = false }
                            }
                        }
                    },
                    enabled = title.isNotBlank() && body.isNotBlank()
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
