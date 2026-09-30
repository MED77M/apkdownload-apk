package com.example.ui.teacher

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.AttendanceRecord
import com.example.data.model.Role
import com.example.data.model.TimetableSlot
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppButton
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.theme.SchoolPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeacherAttendanceScreen(
    firebaseManager: FirebaseManager,
    initialSlot: TimetableSlot?,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentTeacher = firebaseManager.currentUser

    val slots by firebaseManager.observeTimetable(teacherId = currentTeacher?.id).collectAsState(initial = emptyList())
    var selectedSlot by remember { mutableStateOf(initialSlot ?: slots.firstOrNull()) }

    val allStudents by firebaseManager.observeUsers(Role.STUDENT).collectAsState(initial = emptyList())

    // Filter students by slot's group if defined, else all students
    val studentsInGroup = remember(allStudents, selectedSlot) {
        if (selectedSlot == null) allStudents
        else {
            val matched = allStudents.filter { it.groupIds.contains(selectedSlot?.groupId) }
            if (matched.isNotEmpty()) matched else allStudents
        }
    }

    // Attendance state map: studentId -> "PRESENT" | "ABSENT" | "LATE"
    val attendanceMap = remember { mutableStateMapOf<String, String>() }

    // Initialize all to PRESENT by default
    LaunchedEffect(studentsInGroup) {
        studentsInGroup.forEach { s ->
            if (!attendanceMap.containsKey(s.id)) {
                attendanceMap[s.id] = "PRESENT"
            }
        }
    }

    var isSaving by remember { mutableStateOf(false) }
    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.attendanceTitle,
                subtitle = selectedSlot?.let { "${it.subjectName} (${it.groupName})" } ?: strings.markAttendance,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    AppButton(
                        text = strings.save,
                        isLoading = isSaving,
                        icon = Icons.Default.Save,
                        onClick = {
                            if (selectedSlot != null) {
                                isSaving = true
                                val presentIds = attendanceMap.filter { it.value == "PRESENT" }.keys.toList()
                                val absentIds = attendanceMap.filter { it.value == "ABSENT" }.keys.toList()
                                val lateIds = attendanceMap.filter { it.value == "LATE" }.keys.toList()

                                val record = AttendanceRecord(
                                    slotId = selectedSlot!!.id,
                                    subjectName = selectedSlot!!.subjectName,
                                    groupName = selectedSlot!!.groupName,
                                    date = todayDate,
                                    presentStudentIds = presentIds,
                                    absentStudentIds = absentIds,
                                    lateStudentIds = lateIds,
                                    teacherId = currentTeacher?.id ?: ""
                                )

                                scope.launch {
                                    val res = firebaseManager.saveAttendance(record)
                                    isSaving = false
                                    res.onSuccess {
                                        Toast.makeText(context, strings.attendanceSaved, Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }.onFailure { err ->
                                        Toast.makeText(context, err.message ?: strings.error, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        testTag = "btn_save_attendance"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Slot Selector if teacher has multiple classes
            if (slots.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = slots.indexOf(selectedSlot).coerceAtLeast(0),
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 0.dp
                ) {
                    slots.forEach { slot ->
                        Tab(
                            selected = selectedSlot?.id == slot.id,
                            onClick = { selectedSlot = slot },
                            text = { Text("${slot.subjectName} • ${slot.groupName}") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Summary stats banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${strings.totalSessions}: ${studentsInGroup.size} students",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = todayDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = SchoolPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (studentsInGroup.isEmpty()) {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.PersonOff
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(studentsInGroup, key = { it.id }) { student ->
                        val currentStatus = attendanceMap[student.id] ?: "PRESENT"

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.fullName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "@${student.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // 3 One-Tap Status Buttons (Present, Absent, Late)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AttendanceStatusButton(
                                        label = strings.present,
                                        isSelected = currentStatus == "PRESENT",
                                        activeColor = Color(0xFF16A34A),
                                        onClick = { attendanceMap[student.id] = "PRESENT" }
                                    )
                                    AttendanceStatusButton(
                                        label = strings.late,
                                        isSelected = currentStatus == "LATE",
                                        activeColor = Color(0xFFD97706),
                                        onClick = { attendanceMap[student.id] = "LATE" }
                                    )
                                    AttendanceStatusButton(
                                        label = strings.absent,
                                        isSelected = currentStatus == "ABSENT",
                                        activeColor = Color(0xFFDC2626),
                                        onClick = { attendanceMap[student.id] = "ABSENT" }
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

@Composable
fun AttendanceStatusButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) activeColor else activeColor.copy(alpha = 0.1f),
            contentColor = if (isSelected) Color.White else activeColor
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
