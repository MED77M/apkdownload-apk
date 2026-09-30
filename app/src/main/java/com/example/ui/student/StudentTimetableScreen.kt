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
import com.example.data.firebase.FirebaseManager
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.admin.TimetableSlotCard
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary

@Composable
fun StudentTimetableScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val currentStudent = firebaseManager.currentUser
    val slots by firebaseManager.observeTimetable(groupId = currentStudent?.groupIds?.firstOrNull()).collectAsState(initial = emptyList())

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
                    Tab(
                        selected = selectedDay == dayNum,
                        onClick = { selectedDay = dayNum },
                        text = { Text(dayName, fontWeight = FontWeight.Bold) },
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

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = slot.subjectName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "👨‍🏫 ${slot.teacherName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "📍 Room: ${slot.roomName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SchoolSecondary,
                                        fontWeight = FontWeight.SemiBold
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
