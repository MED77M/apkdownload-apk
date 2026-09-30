package com.example.ui.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.TimetableSlot
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.admin.DashboardActionRow
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatCard
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import com.example.ui.theme.SchoolWarning

@Composable
fun TeacherDashboardScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onNavigateToAttendance: (TimetableSlot?) -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToHomework: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToChat: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val currentTeacher = firebaseManager.currentUser

    val slots by firebaseManager.observeTimetable(teacherId = currentTeacher?.id).collectAsState(initial = emptyList())
    val resources by firebaseManager.observeResources().collectAsState(initial = emptyList())
    val homeworks by firebaseManager.observeHomework().collectAsState(initial = emptyList())

    val canPublish = currentTeacher?.teacherPermissions?.canPublishResources ?: true
    val canEditGrades = currentTeacher?.teacherPermissions?.canEditGrades ?: true
    val canAnnounce = currentTeacher?.teacherPermissions?.canSendAnnouncements ?: true

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.appName,
                subtitle = "${strings.roleTeacher}: ${currentTeacher?.fullName ?: ""}",
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                onLogoutClick = onLogout
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = strings.timetableTitle,
                    value = "${slots.size} classes",
                    icon = Icons.Default.CalendarMonth,
                    color = SchoolPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = strings.homeworkTitle,
                    value = "${homeworks.size} assignments",
                    icon = Icons.Default.Assignment,
                    color = SchoolSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Actions Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    DashboardActionRow(
                        title = strings.markAttendance,
                        icon = Icons.Default.ChecklistRtl,
                        color = Color(0xFF16A34A),
                        onClick = { onNavigateToAttendance(slots.firstOrNull()) },
                        testTag = "teacher_action_attendance"
                    )

                    if (canPublish) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        DashboardActionRow(
                            title = strings.resourcesTitle,
                            icon = Icons.Default.LibraryBooks,
                            color = SchoolPrimary,
                            onClick = onNavigateToResources,
                            testTag = "teacher_action_resources"
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    DashboardActionRow(
                        title = strings.homeworkTitle,
                        icon = Icons.Default.AssignmentLate,
                        color = SchoolSecondary,
                        onClick = onNavigateToHomework,
                        testTag = "teacher_action_homework"
                    )

                    if (canEditGrades) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        DashboardActionRow(
                            title = strings.gradesTitle,
                            icon = Icons.Default.Grading,
                            color = SchoolWarning,
                            onClick = onNavigateToGrades,
                            testTag = "teacher_action_grades"
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    DashboardActionRow(
                        title = strings.chatTitle,
                        icon = Icons.Default.Chat,
                        color = Color(0xFF0284C7),
                        onClick = onNavigateToChat,
                        testTag = "teacher_action_chat"
                    )
                }
            }

            // Teacher's Schedule for quick session access
            Text(
                text = strings.weeklySchedule,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (slots.isEmpty()) {
                EmptyStateView(
                    message = strings.noScheduleToday,
                    icon = Icons.Default.EventBusy
                )
            } else {
                slots.forEach { slot ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = slot.subjectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "👥 ${slot.groupName} • 📍 ${slot.roomName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "⏰ ${slot.startTime} - ${slot.endTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SchoolPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(
                                onClick = { onNavigateToAttendance(slot) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(strings.markAttendance)
                            }
                        }
                    }
                }
            }
        }
    }
}
