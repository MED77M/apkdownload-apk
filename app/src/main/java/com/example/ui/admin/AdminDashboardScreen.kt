package com.example.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Role
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.StatCard
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import com.example.ui.theme.SchoolWarning

@Composable
fun AdminDashboardScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToAnnouncements: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = Translations.get(currentLanguage)

    val users by firebaseManager.observeUsers().collectAsState(initial = emptyList())
    val groups by firebaseManager.observeGroups().collectAsState(initial = emptyList())
    val attendanceRecords by firebaseManager.observeAttendance().collectAsState(initial = emptyList())
    val grades by firebaseManager.observeGrades().collectAsState(initial = emptyList())
    val payments by firebaseManager.observePayments().collectAsState(initial = emptyList())

    val totalStudents = users.count { it.role == Role.STUDENT }
    val totalTeachers = users.count { it.role == Role.TEACHER }

    // Attendance calculation
    val attendanceRate = remember(attendanceRecords) {
        var totalPresent = 0
        var totalMarked = 0
        attendanceRecords.forEach {
            totalPresent += it.presentStudentIds.size
            totalMarked += (it.presentStudentIds.size + it.absentStudentIds.size + it.lateStudentIds.size)
        }
        if (totalMarked > 0) ((totalPresent.toFloat() / totalMarked.toFloat()) * 100).toInt() else 100
    }

    // Average grade calculation
    val averageGrade = remember(grades) {
        if (grades.isNotEmpty()) {
            val sum = grades.sumOf { it.score.toDouble() }
            String.format("%.1f", sum / grades.size)
        } else "0.0"
    }

    val totalCollected = remember(payments) {
        payments.filter { it.status == "PAID" }.sumOf { it.amount }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.appName,
                subtitle = strings.roleAdmin,
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
            // Stat Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = strings.studentsTab,
                    value = totalStudents.toString(),
                    icon = Icons.Default.Groups,
                    color = SchoolPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = strings.teachersTab,
                    value = totalTeachers.toString(),
                    icon = Icons.Default.School,
                    color = SchoolSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = strings.attendanceRate,
                    value = "$attendanceRate%",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = strings.overallAverage,
                    value = "$averageGrade / 20",
                    icon = Icons.Default.Grade,
                    color = SchoolWarning,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Access Navigation Actions
            Text(
                text = strings.navDashboard,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    DashboardActionRow(
                        title = strings.usersTitle,
                        icon = Icons.Default.People,
                        color = SchoolPrimary,
                        onClick = onNavigateToUsers,
                        testTag = "admin_action_users"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    DashboardActionRow(
                        title = strings.timetableTitle,
                        icon = Icons.Default.CalendarMonth,
                        color = SchoolSecondary,
                        onClick = onNavigateToTimetable,
                        testTag = "admin_action_timetable"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    DashboardActionRow(
                        title = strings.financeTitle,
                        icon = Icons.Default.AccountBalanceWallet,
                        color = Color(0xFF16A34A),
                        onClick = onNavigateToFinance,
                        testTag = "admin_action_finance"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    DashboardActionRow(
                        title = strings.reportsTitle,
                        icon = Icons.Default.Assessment,
                        color = Color(0xFF0284C7),
                        onClick = onNavigateToReports,
                        testTag = "admin_action_reports"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    DashboardActionRow(
                        title = strings.announcementsTitle,
                        icon = Icons.Default.Campaign,
                        color = SchoolWarning,
                        onClick = onNavigateToAnnouncements,
                        testTag = "admin_action_announcements"
                    )
                }
            }

            // Academic Progress Visual Bars
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = strings.progressChart,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val subjects = grades.map { it.subjectName }.distinct().take(4)
                    if (subjects.isEmpty()) {
                        Text(
                            text = strings.noDataYet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        subjects.forEach { subjectName ->
                            val subGrades = grades.filter { it.subjectName == subjectName }
                            val subAvg = subGrades.map { it.score }.average().toFloat()
                            val progress = (subAvg / 20f).coerceIn(0f, 1f)

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = subjectName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${String.format("%.1f", subAvg)} / 20",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SchoolPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = SchoolSecondary,
                                    trackColor = SchoolSecondary.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardActionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
