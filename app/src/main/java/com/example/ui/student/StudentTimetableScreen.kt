package com.example.ui.student

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseManager
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatusBadge
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentTimetableScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val liveStudent by firebaseManager.observeUser(firebaseManager.currentUser?.id ?: "").collectAsState(initial = firebaseManager.currentUser)
    val currentStudent = liveStudent ?: firebaseManager.currentUser
    val allSlots by firebaseManager.observeTimetable().collectAsState(initial = emptyList())
    val allSubjects by firebaseManager.observeSubjects().collectAsState(initial = emptyList())
    val allEnrollments by firebaseManager.observeEnrollments(studentId = currentStudent?.id).collectAsState(initial = emptyList())
    val allGroups by firebaseManager.observeGroups().collectAsState(initial = emptyList())

    val studentSubjectIds = remember(currentStudent, allEnrollments) {
        val fromUser = currentStudent?.subjectIds ?: emptyList()
        val fromEnrollments = allEnrollments.filter {
            it.studentId == currentStudent?.id && (it.status.equals("ACTIVE", ignoreCase = true) || it.status.isBlank())
        }.map { it.subjectId }
        (fromUser + fromEnrollments).filter { it.isNotBlank() }.distinct()
    }
    val studentGroupIds = remember(currentStudent) {
        currentStudent?.groupIds ?: emptyList()
    }

    // STRICT FILTERING: Only show slots matching student's enrolled subjects or groups
    val slots = remember(allSlots, studentSubjectIds, studentGroupIds, allSubjects, allEnrollments, allGroups, currentStudent) {
        val student = currentStudent
        if (student == null) {
            emptyList()
        } else {
            val studentSubjectsList = allSubjects.filter { studentSubjectIds.contains(it.id) }

            allSlots.filter { slot ->
                // 1. Group matching
                val matchesGroup = (slot.groupId.isNotBlank() && studentGroupIds.contains(slot.groupId)) ||
                    (slot.groupName.isNotBlank() && allGroups.any { grp ->
                        grp.name.equals(slot.groupName, ignoreCase = true) && (studentGroupIds.contains(grp.id) || grp.studentIds.contains(student.id))
                    })

                // 2. Direct subject match by subjectId
                val matchesSubjectId = slot.subjectId.isNotBlank() && studentSubjectIds.contains(slot.subjectId)

                // 3. Subject match by name and level
                val matchesSubjectNameAndLevel = slot.subjectName.isNotBlank() && studentSubjectsList.any { s ->
                    s.name.equals(slot.subjectName, ignoreCase = true) &&
                    (slot.level.isBlank() || s.level.isBlank() || s.level.equals(slot.level, ignoreCase = true))
                }

                matchesGroup || matchesSubjectId || matchesSubjectNameAndLevel
            }
        }
    }

    var selectedDay by remember { mutableStateOf(1) } // 1: Mon .. 7: Sun

    val daySlots = remember(slots, selectedDay) {
        slots.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startTime }
    }

    val daysOfWeek = listOf(
        1 to strings.monday,
        2 to strings.tuesday,
        3 to strings.wednesday,
        4 to strings.thursday,
        5 to strings.friday,
        6 to strings.saturday,
        7 to strings.sunday
    )

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.weeklySchedule,
                subtitle = strings.timetableTitle,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedDay - 1,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SchoolPrimary,
                edgePadding = 16.dp
            ) {
                daysOfWeek.forEach { (dayNum, dayName) ->
                    val countForDay = slots.count { it.dayOfWeek == dayNum }
                    Tab(
                        selected = selectedDay == dayNum,
                        onClick = { selectedDay = dayNum },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dayName, fontWeight = if (selectedDay == dayNum) FontWeight.Bold else FontWeight.Normal)
                                if (countForDay > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (selectedDay == dayNum) SchoolPrimary else Color.Gray.copy(alpha = 0.25f),
                                        contentColor = if (selectedDay == dayNum) Color.White else MaterialTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = countForDay.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }

            if (daySlots.isEmpty()) {
                EmptyStateView(
                    message = strings.noScheduleToday,
                    icon = Icons.Default.EventBusy
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(daySlots, key = { it.id }) { slot ->
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
                                    color = SchoolPrimary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(slot.startTime, fontWeight = FontWeight.Bold, color = SchoolPrimary)
                                        Text(slot.endTime, style = MaterialTheme.typography.labelSmall, color = SchoolSecondary)
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = slot.subjectName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (slot.level.isNotBlank()) {
                                            StatusBadge(
                                                text = "🎓 ${slot.level}",
                                                color = SchoolSecondary
                                            )
                                        }

                                        if (slot.teacherName.isNotBlank()) {
                                            StatusBadge(
                                                text = "👨‍🏫 ${slot.teacherName}",
                                                color = Color(0xFFD97706)
                                            )
                                        }

                                        if (slot.roomName.isNotBlank()) {
                                            StatusBadge(
                                                text = "🚪 ${slot.roomName}",
                                                color = Color(0xFF16A34A)
                                            )
                                        }

                                        if (slot.groupName.isNotBlank()) {
                                            StatusBadge(
                                                text = "👥 ${slot.groupName}",
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
    }
}
