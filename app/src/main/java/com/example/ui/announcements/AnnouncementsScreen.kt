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
    var editingAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var deletingAnnouncement by remember { mutableStateOf<Announcement?>(null) }

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
                                        val editedText = if (item.updatedAt != null) " • ✏️ ${strings.editedBadge}" else ""
                                        Text(
                                            text = "By ${item.authorName} • 📅 $dateStr$editedText",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        if (canCreate) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { editingAnnouncement = item },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = strings.editAnnouncement,
                                                    tint = SchoolPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (currentUser?.role == Role.ADMIN || (currentUser?.role == Role.TEACHER && item.authorName == currentUser.fullName)) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                            IconButton(
                                                onClick = { deletingAnnouncement = item },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = strings.deleteAnnouncement,
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
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

    editingAnnouncement?.let { ann ->
        var editTitle by remember { mutableStateOf(ann.title) }
        var editBody by remember { mutableStateOf(ann.body) }
        var editAudience by remember { mutableStateOf(ann.targetAudience) }

        AlertDialog(
            onDismissRequest = { editingAnnouncement = null },
            title = { Text(strings.editAnnouncement) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBody,
                        onValueChange = { editBody = it },
                        label = { Text("Message Body") },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = editAudience == "ALL",
                            onClick = { editAudience = "ALL" },
                            label = { Text(strings.audienceAll) }
                        )
                        FilterChip(
                            selected = editAudience == "TEACHERS",
                            onClick = { editAudience = "TEACHERS" },
                            label = { Text(strings.audienceTeachers) }
                        )
                        FilterChip(
                            selected = editAudience == "STUDENTS",
                            onClick = { editAudience = "STUDENTS" },
                            label = { Text(strings.audienceStudents) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank() && editBody.isNotBlank()) {
                            scope.launch {
                                val res = firebaseManager.updateAnnouncement(
                                    ann.copy(
                                        title = editTitle.trim(),
                                        body = editBody.trim(),
                                        targetAudience = editAudience
                                    )
                                )
                                if (res.isSuccess) {
                                    Toast.makeText(context, strings.save, Toast.LENGTH_SHORT).show()
                                    editingAnnouncement = null
                                } else {
                                    Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = editTitle.isNotBlank() && editBody.isNotBlank()
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAnnouncement = null }) { Text(strings.cancel) }
            }
        )
    }

    deletingAnnouncement?.let { ann ->
        AlertDialog(
            onDismissRequest = { deletingAnnouncement = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text(strings.deleteAnnouncement) },
            text = { Text(strings.deleteAnnouncementConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val res = firebaseManager.deleteAnnouncement(ann.id)
                            if (res.isSuccess) {
                                Toast.makeText(context, strings.success, Toast.LENGTH_SHORT).show()
                                deletingAnnouncement = null
                            } else {
                                Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(strings.delete, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAnnouncement = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
