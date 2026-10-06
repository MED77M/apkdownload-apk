package com.example.ui.student

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.notification.BadgeManager
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.admin.DashboardActionRow
import com.example.ui.common.AppHeader
import com.example.ui.common.StatCard
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import com.example.ui.theme.SchoolAccentRed
import com.example.ui.theme.SchoolWarning

@Composable
fun StudentDashboardScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToHomework: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToAnnouncements: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val liveStudent by firebaseManager.observeUser(firebaseManager.currentUser?.id ?: "").collectAsState(initial = firebaseManager.currentUser)
    val currentStudent = liveStudent ?: firebaseManager.currentUser

    val allTimetableSlots by firebaseManager.observeTimetable().collectAsState(initial = emptyList())
    val allSubjects by firebaseManager.observeSubjects().collectAsState(initial = emptyList())
    val allGroups by firebaseManager.observeGroups().collectAsState(initial = emptyList())
    val myGrades by firebaseManager.observeGrades(studentId = currentStudent?.id).collectAsState(initial = emptyList())
    val attendanceRecords by firebaseManager.observeAttendance().collectAsState(initial = emptyList())
    val announcements by firebaseManager.observeAnnouncements().collectAsState(initial = emptyList())
    val myEnrollments by firebaseManager.observeEnrollments(studentId = currentStudent?.id).collectAsState(initial = emptyList())
    val myPayments by firebaseManager.observePayments(studentId = currentStudent?.id).collectAsState(initial = emptyList())

    val studentSubjectIds = remember(currentStudent, myEnrollments) {
        val fromUser = currentStudent?.subjectIds ?: emptyList()
        val fromEnrollments = myEnrollments.filter {
            it.studentId == currentStudent?.id && (it.status.equals("ACTIVE", ignoreCase = true) || it.status.isBlank())
        }.map { it.subjectId }
        (fromUser + fromEnrollments).filter { it.isNotBlank() }.distinct()
    }
    val studentGroupIds = remember(currentStudent) {
        currentStudent?.groupIds ?: emptyList()
    }

    val slots = remember(allTimetableSlots, studentSubjectIds, studentGroupIds, allSubjects, myEnrollments, allGroups, currentStudent) {
        val student = currentStudent
        if (student == null) {
            emptyList()
        } else {
            val studentSubjectsList = allSubjects.filter { studentSubjectIds.contains(it.id) }

            allTimetableSlots.filter { slot ->
                val matchesGroup = (slot.groupId.isNotBlank() && studentGroupIds.contains(slot.groupId)) ||
                    (slot.groupName.isNotBlank() && allGroups.any { grp ->
                        grp.name.equals(slot.groupName, ignoreCase = true) && (studentGroupIds.contains(grp.id) || grp.studentIds.contains(student.id))
                    })

                val matchesSubjectId = slot.subjectId.isNotBlank() && studentSubjectIds.contains(slot.subjectId)

                val matchesSubjectNameAndLevel = slot.subjectName.isNotBlank() && studentSubjectsList.any { s ->
                    s.name.equals(slot.subjectName, ignoreCase = true) &&
                    (slot.level.isBlank() || s.level.isBlank() || s.level.equals(slot.level, ignoreCase = true))
                }

                matchesGroup || matchesSubjectId || matchesSubjectNameAndLevel
            }
        }
    }

    // Student attendance rate
    val attendanceRate = remember(attendanceRecords, currentStudent) {
        val studentId = currentStudent?.id ?: ""
        var presentCount = 0
        var totalRecorded = 0
        attendanceRecords.forEach { rec ->
            if (rec.presentStudentIds.contains(studentId) || rec.absentStudentIds.contains(studentId) || rec.lateStudentIds.contains(studentId)) {
                totalRecorded++
                if (rec.presentStudentIds.contains(studentId) || rec.lateStudentIds.contains(studentId)) {
                    presentCount++
                }
            }
        }
        if (totalRecorded > 0) ((presentCount.toFloat() / totalRecorded.toFloat()) * 100).toInt() else 100
    }

    val overallAvg = remember(myGrades) {
        if (myGrades.isNotEmpty()) String.format("%.1f", myGrades.map { it.score }.average()) else "0.0"
    }

    val hasUnreadMessages by BadgeManager.hasUnreadMessages.collectAsState()
    val hasUnreadAnnouncements by BadgeManager.hasUnreadAnnouncements.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.appName,
                subtitle = "${strings.roleStudent}: ${currentStudent?.fullName ?: ""}",
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                onLogoutClick = onLogout,
                actions = {
                    IconButton(
                        onClick = {
                            BadgeManager.clearAnnouncementsBadge()
                            onNavigateToAnnouncements()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (hasUnreadAnnouncements) {
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Campaign,
                                contentDescription = strings.announcementsTitle,
                                tint = Color.White
                            )
                        }
                    }
                }
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
            // Next Class Reminder Banner
            if (slots.isNotEmpty()) {
                val nextSlot = slots.first()
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SchoolPrimary.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = SchoolPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.nextClassReminder,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SchoolPrimary
                            )
                            Text(
                                text = "${nextSlot.subjectName} • 📍 ${nextSlot.roomName} (${nextSlot.startTime})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = strings.overallAverage,
                    value = "$overallAvg / 20",
                    icon = Icons.Default.Grade,
                    color = SchoolSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = strings.attendanceRate,
                    value = "$attendanceRate%",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
            }

            // My Registered Subjects Section (المواد المسجلة)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.studentSubjects,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SchoolPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${myEnrollments.size} ${if (currentLanguage == AppLanguage.ARABIC) "مواد" else "subjects"}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SchoolPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (myEnrollments.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = strings.noEnrolledSubjects,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                myEnrollments.forEach { enroll ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SchoolPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = SchoolPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = enroll.subjectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (enroll.teacherName.isNotBlank()) {
                                    Text(
                                        text = "${strings.roleTeacher}: ${enroll.teacherName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SchoolPrimary
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF16A34A).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${enroll.monthlyFee.toInt()} ${strings.currencySymbol}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // My Fees Section (Multi-Subject Breakdown - STRICT PRIVACY)
            if (myEnrollments.isNotEmpty()) {
                Text(
                    text = strings.myFeesTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                myEnrollments.forEach { enroll ->
                    val isPaid = enroll.amountRemaining <= 0.0
                    val isPartial = enroll.amountPaid > 0.0 && enroll.amountRemaining > 0.0
                    val statusColor = when {
                        isPaid -> Color(0xFF16A34A)
                        isPartial -> SchoolPrimary
                        else -> SchoolAccentRed
                    }

                    val subjectPayments = remember(myPayments, enroll) {
                        myPayments.filter { it.enrollmentId == enroll.id || it.subjectId == enroll.subjectId }
                    }

                    var showHistory by remember { mutableStateOf(false) }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = enroll.subjectName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${strings.roleTeacher}: ${enroll.teacherName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SchoolPrimary
                                    )
                                }

                                com.example.ui.common.StatusBadge(
                                    text = when {
                                        isPaid -> strings.statusPaid
                                        isPartial -> strings.statusPartiallyPaid
                                        else -> strings.statusUnpaid
                                    },
                                    color = statusColor
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Three columns: Fee, Paid, Remaining (NO teacher/school shares ever shown)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(strings.monthlyFeeAmount, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("${enroll.monthlyFee.toInt()} ${strings.currencySymbol}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                                Column {
                                    Text(strings.amountPaidLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("${enroll.amountPaid.toInt()} ${strings.currencySymbol}", fontWeight = FontWeight.Bold, color = SchoolPrimary, style = MaterialTheme.typography.bodyMedium)
                                }
                                Column {
                                    Text(strings.amountRemainingLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(
                                        "${enroll.amountRemaining.toInt()} ${strings.currencySymbol}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (enroll.amountRemaining > 0) SchoolAccentRed else Color(0xFF16A34A),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            // Payment History Toggle
                            if (subjectPayments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { showHistory = !showHistory }) {
                                        Text(
                                            if (showHistory) "Hide History ▲" else "View Payment History (${subjectPayments.size}) ▼",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                if (showHistory) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        subjectPayments.forEach { p ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "🗓️ ${p.date} (${p.month})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.DarkGray
                                                )
                                                Text(
                                                    text = "${p.amount.toInt()} ${strings.currencySymbol}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SchoolPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Navigation Actions
            Text(
                text = strings.navDashboard,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    DashboardActionRow(
                        title = strings.timetableTitle,
                        icon = Icons.Default.CalendarMonth,
                        color = SchoolPrimary,
                        onClick = onNavigateToTimetable,
                        testTag = "student_action_timetable"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    DashboardActionRow(
                        title = strings.homeworkTitle,
                        icon = Icons.Default.Assignment,
                        color = SchoolSecondary,
                        onClick = onNavigateToHomework,
                        testTag = "student_action_homework"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    DashboardActionRow(
                        title = strings.gradesTitle,
                        icon = Icons.Default.Grading,
                        color = SchoolWarning,
                        onClick = onNavigateToGrades,
                        testTag = "student_action_grades"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    DashboardActionRow(
                        title = strings.resourcesTitle,
                        icon = Icons.Default.MenuBook,
                        color = Color(0xFF0284C7),
                        onClick = onNavigateToResources,
                        testTag = "student_action_resources"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    DashboardActionRow(
                        title = strings.chatTitle,
                        icon = Icons.Default.Chat,
                        color = Color(0xFF7C3AED),
                        onClick = {
                            BadgeManager.clearMessagesBadge()
                            onNavigateToChat()
                        },
                        testTag = "student_action_chat",
                        hasBadge = hasUnreadMessages
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    DashboardActionRow(
                        title = strings.announcementsTitle,
                        icon = Icons.Default.Campaign,
                        color = Color(0xFFD97706),
                        onClick = {
                            BadgeManager.clearAnnouncementsBadge()
                            onNavigateToAnnouncements()
                        },
                        testTag = "student_action_announcements",
                        hasBadge = hasUnreadAnnouncements
                    )
                }
            }
        }
    }
}
