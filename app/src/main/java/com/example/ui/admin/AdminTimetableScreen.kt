package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.*
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTimetableScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDay by remember { mutableStateOf(1) } // 1: Mon .. 7: Sun

    val slots by firebaseManager.observeTimetable().collectAsState(initial = emptyList())
    val groups by firebaseManager.observeGroups().collectAsState(initial = emptyList())
    val subjects by firebaseManager.observeSubjects().collectAsState(initial = emptyList())
    val rooms by firebaseManager.observeRooms().collectAsState(initial = emptyList())
    val teachers by firebaseManager.observeUsers(Role.TEACHER).collectAsState(initial = emptyList())

    val daySlots = remember(slots, selectedDay) {
        slots.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startTime }
    }

    var showAddSlotDialog by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<TimetableSlot?>(null) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddRoomDialog by remember { mutableStateOf(false) }

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
                title = strings.timetableTitle,
                subtitle = strings.weeklySchedule,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddGroupDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.AddHomeWork, contentDescription = strings.addGroup, tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSlotDialog = true },
                containerColor = SchoolPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(strings.addSlot) },
                modifier = Modifier.testTag("fab_add_slot")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Quick management chip row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { showAddGroupDialog = true },
                    label = { Text("${strings.groupsTitle} (${groups.size})") },
                    leadingIcon = { Icon(Icons.Default.Class, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = { showAddSubjectDialog = true },
                    label = { Text("${strings.subjectsTitle} (${subjects.size})") },
                    leadingIcon = { Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = { showAddRoomDialog = true },
                    label = { Text("${strings.roomsTitle} (${rooms.size})") },
                    leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Day Selector Tabs
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

            // Slots list for selected day
            if (daySlots.isEmpty()) {
                EmptyStateView(
                    message = strings.noScheduleToday,
                    icon = Icons.Default.CalendarToday,
                    actionText = strings.addSlot,
                    onActionClick = { showAddSlotDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(daySlots, key = { it.id }) { slot ->
                        TimetableSlotCard(
                            slot = slot,
                            onEdit = { editingSlot = slot },
                            onDelete = {
                                scope.launch {
                                    firebaseManager.deleteTimetableSlot(slot.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Timetable Slot Dialog with Conflict Detection
    if (showAddSlotDialog) {
        CreateSlotDialog(
            strings = strings,
            groups = groups,
            subjects = subjects,
            rooms = rooms,
            teachers = teachers,
            existingSlots = slots,
            initialDay = selectedDay,
            onDismiss = { showAddSlotDialog = false },
            onCreate = { newSlot ->
                scope.launch {
                    val res = firebaseManager.addTimetableSlot(newSlot)
                    res.onSuccess {
                        showAddSlotDialog = false
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: strings.error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Dialogs to add Group, Subject, Room
    if (showAddGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        var level by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text(strings.addGroup) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text(strings.groupName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = level,
                        onValueChange = { level = it },
                        label = { Text(strings.level) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (groupName.isNotBlank()) {
                        scope.launch {
                            firebaseManager.addGroup(SchoolGroup(name = groupName.trim(), level = level.trim()))
                            showAddGroupDialog = false
                        }
                    }
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showAddSubjectDialog) {
        var subjectName by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text(strings.addSubject) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text(strings.subjectName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (subjectName.isNotBlank()) {
                        scope.launch {
                            firebaseManager.addSubject(Subject(name = subjectName.trim(), code = code.trim()))
                            showAddSubjectDialog = false
                        }
                    }
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showAddRoomDialog) {
        var roomName by remember { mutableStateOf("") }
        var capacity by remember { mutableStateOf("30") }
        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            title = { Text(strings.addRoom) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = roomName,
                        onValueChange = { roomName = it },
                        label = { Text(strings.roomName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = { Text("Capacity") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (roomName.isNotBlank()) {
                        scope.launch {
                            firebaseManager.addRoom(Room(name = roomName.trim(), capacity = capacity.toIntOrNull() ?: 30))
                            showAddRoomDialog = false
                        }
                    }
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRoomDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    editingSlot?.let { slot ->
        EditSlotDialog(
            slot = slot,
            strings = strings,
            onDismiss = { editingSlot = null },
            onSave = { updatedSlot ->
                scope.launch {
                    val res = firebaseManager.updateTimetableSlot(updatedSlot)
                    if (res.isSuccess) {
                        Toast.makeText(context, strings.save, Toast.LENGTH_SHORT).show()
                        editingSlot = null
                    } else {
                        Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun EditSlotDialog(
    slot: TimetableSlot,
    strings: com.example.localization.AppStrings,
    onDismiss: () -> Unit,
    onSave: (TimetableSlot) -> Unit
) {
    var subjectName by remember { mutableStateOf(slot.subjectName) }
    var teacherName by remember { mutableStateOf(slot.teacherName) }
    var groupName by remember { mutableStateOf(slot.groupName) }
    var roomName by remember { mutableStateOf(slot.roomName) }
    var startTime by remember { mutableStateOf(slot.startTime) }
    var endTime by remember { mutableStateOf(slot.endTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editTimetableSlot) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text(strings.subjectsTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text(strings.roleTeacher) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text(strings.groupName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text(strings.roomsTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        slot.copy(
                            subjectName = subjectName.trim(),
                            teacherName = teacherName.trim(),
                            groupName = groupName.trim(),
                            roomName = roomName.trim(),
                            startTime = startTime.trim(),
                            endTime = endTime.trim()
                        )
                    )
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun TimetableSlotCard(
    slot: TimetableSlot,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit
) {
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = SchoolPrimary.copy(alpha = 0.1f),
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
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "👨‍🏫 ${slot.teacherName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "👥 ${slot.groupName} • 📍 ${slot.roomName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SchoolSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Slot", tint = SchoolPrimary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Slot", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSlotDialog(
    strings: com.example.localization.AppStrings,
    groups: List<SchoolGroup>,
    subjects: List<Subject>,
    rooms: List<Room>,
    teachers: List<SchoolUser>,
    existingSlots: List<TimetableSlot>,
    initialDay: Int,
    onDismiss: () -> Unit,
    onCreate: (TimetableSlot) -> Unit
) {
    var isManualMode by remember { mutableStateOf(true) } // Default to manual entry as requested

    var dayOfWeek by remember { mutableStateOf(initialDay) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("10:00") }

    // Manual Entry Fields
    var manualSubject by remember { mutableStateOf("") }
    var manualTeacher by remember { mutableStateOf("") }
    var manualGroup by remember { mutableStateOf("") }
    var manualRoom by remember { mutableStateOf("") }

    // List Selection Fields
    var selectedGroup by remember { mutableStateOf(groups.firstOrNull()) }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var selectedRoom by remember { mutableStateOf(rooms.firstOrNull()) }
    var selectedTeacher by remember { mutableStateOf(teachers.firstOrNull()) }

    // Conflict check for list mode
    val conflict = remember(dayOfWeek, startTime, endTime, selectedRoom, selectedTeacher, existingSlots, isManualMode, manualRoom, manualTeacher) {
        if (!isManualMode) {
            val candidate = TimetableSlot(
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                roomId = selectedRoom?.id ?: "",
                teacherId = selectedTeacher?.id ?: ""
            )

            val roomConflict = existingSlots.any { other ->
                other.dayOfWeek == dayOfWeek && other.roomId.isNotBlank() && other.roomId == candidate.roomId && candidate.overlapsWith(other)
            }
            val teacherConflict = existingSlots.any { other ->
                other.dayOfWeek == dayOfWeek && other.teacherId.isNotBlank() && other.teacherId == candidate.teacherId && candidate.overlapsWith(other)
            }

            when {
                roomConflict -> strings.conflictRoom
                teacherConflict -> strings.conflictTeacher
                else -> null
            }
        } else {
            // Check conflicts for manual mode by room name and teacher name
            val candidate = TimetableSlot(
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                roomName = manualRoom.trim(),
                teacherName = manualTeacher.trim()
            )
            val roomConflict = manualRoom.isNotBlank() && existingSlots.any { other ->
                other.dayOfWeek == dayOfWeek && other.roomName.equals(candidate.roomName, ignoreCase = true) && candidate.overlapsWith(other)
            }
            val teacherConflict = manualTeacher.isNotBlank() && existingSlots.any { other ->
                other.dayOfWeek == dayOfWeek && other.teacherName.equals(candidate.teacherName, ignoreCase = true) && candidate.overlapsWith(other)
            }

            when {
                roomConflict -> strings.conflictRoom
                teacherConflict -> strings.conflictTeacher
                else -> null
            }
        }
    }

    val daysList = listOf(
        1 to strings.monday,
        2 to strings.tuesday,
        3 to strings.wednesday,
        4 to strings.thursday,
        5 to strings.friday,
        6 to strings.saturday,
        7 to strings.sunday
    )

    val isFormValid = if (isManualMode) {
        manualSubject.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()
    } else {
        selectedSubject != null && selectedTeacher != null && selectedGroup != null && selectedRoom != null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = SchoolPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.addSlot, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mode Toggle: Manual Entry vs Select From Registered
                PrimaryTabRow(
                    selectedTabIndex = if (isManualMode) 0 else 1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = isManualMode,
                        onClick = { isManualMode = true },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.manualEntry, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    )
                    Tab(
                        selected = !isManualMode,
                        onClick = { isManualMode = false },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.selectFromList, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    )
                }

                if (conflict != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = conflict,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Day Selection
                Text(strings.dayOfWeek, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    var expandedDay by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedDay,
                        onExpandedChange = { expandedDay = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = daysList.firstOrNull { it.first == dayOfWeek }?.second ?: strings.monday,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.dayOfWeek) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDay) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDay,
                            onDismissRequest = { expandedDay = false }
                        ) {
                            daysList.forEach { (dNum, dName) ->
                                DropdownMenuItem(
                                    text = { Text(dName) },
                                    onClick = {
                                        dayOfWeek = dNum
                                        expandedDay = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Time Pickers
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text(strings.startTime) },
                        placeholder = { Text("08:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text(strings.endTime) },
                        placeholder = { Text("10:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isManualMode) {
                    // Manual Text Fields
                    OutlinedTextField(
                        value = manualSubject,
                        onValueChange = { manualSubject = it },
                        label = { Text(strings.subjectName + " *") },
                        placeholder = { Text("مثال: رياضيات / Physique") },
                        leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = SchoolPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualTeacher,
                        onValueChange = { manualTeacher = it },
                        label = { Text(strings.teacherName) },
                        placeholder = { Text("مثال: أستاذ بن علي / M. Ahmed") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SchoolSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualGroup,
                        onValueChange = { manualGroup = it },
                        label = { Text(strings.groupName) },
                        placeholder = { Text("مثال: فوج 1 / 3AS Sciences") },
                        leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualRoom,
                        onValueChange = { manualRoom = it },
                        label = { Text(strings.roomName) },
                        placeholder = { Text("مثال: قاعة 3 / Labo 1") },
                        leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Registered Dropdown Selectors
                    Text(strings.subjectName, style = MaterialTheme.typography.labelMedium)
                    DropdownSelector(
                        items = subjects,
                        selectedItem = selectedSubject,
                        label = { it.name },
                        onSelect = { selectedSubject = it }
                    )

                    Text(strings.roleTeacher, style = MaterialTheme.typography.labelMedium)
                    DropdownSelector(
                        items = teachers,
                        selectedItem = selectedTeacher,
                        label = { it.fullName },
                        onSelect = { selectedTeacher = it }
                    )

                    Text(strings.groupName, style = MaterialTheme.typography.labelMedium)
                    DropdownSelector(
                        items = groups,
                        selectedItem = selectedGroup,
                        label = { "${it.name} (${it.level})" },
                        onSelect = { selectedGroup = it }
                    )

                    Text(strings.roomName, style = MaterialTheme.typography.labelMedium)
                    DropdownSelector(
                        items = rooms,
                        selectedItem = selectedRoom,
                        label = { it.name },
                        onSelect = { selectedRoom = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isManualMode) {
                        val trimmedSub = manualSubject.trim()
                        val trimmedTeacher = manualTeacher.trim().ifEmpty { "-" }
                        val trimmedGroup = manualGroup.trim().ifEmpty { "-" }
                        val trimmedRoom = manualRoom.trim().ifEmpty { "-" }

                        // Check if matches existing items for cleaner linking
                        val matchedSub = subjects.firstOrNull { it.name.equals(trimmedSub, ignoreCase = true) }
                        val matchedTeacher = teachers.firstOrNull { it.fullName.equals(trimmedTeacher, ignoreCase = true) }
                        val matchedGroup = groups.firstOrNull { it.name.equals(trimmedGroup, ignoreCase = true) }
                        val matchedRoom = rooms.firstOrNull { it.name.equals(trimmedRoom, ignoreCase = true) }

                        val newSlot = TimetableSlot(
                            subjectId = matchedSub?.id ?: "sub_${System.currentTimeMillis()}",
                            subjectName = trimmedSub,
                            teacherId = matchedTeacher?.id ?: "teacher_${System.currentTimeMillis()}",
                            teacherName = trimmedTeacher,
                            groupId = matchedGroup?.id ?: "grp_${System.currentTimeMillis()}",
                            groupName = trimmedGroup,
                            roomId = matchedRoom?.id ?: "room_${System.currentTimeMillis()}",
                            roomName = trimmedRoom,
                            dayOfWeek = dayOfWeek,
                            startTime = startTime.trim(),
                            endTime = endTime.trim()
                        )
                        onCreate(newSlot)
                    } else if (selectedSubject != null && selectedTeacher != null && selectedGroup != null && selectedRoom != null) {
                        val newSlot = TimetableSlot(
                            subjectId = selectedSubject!!.id,
                            subjectName = selectedSubject!!.name,
                            teacherId = selectedTeacher!!.id,
                            teacherName = selectedTeacher!!.fullName,
                            groupId = selectedGroup!!.id,
                            groupName = selectedGroup!!.name,
                            roomId = selectedRoom!!.id,
                            roomName = selectedRoom!!.name,
                            dayOfWeek = dayOfWeek,
                            startTime = startTime.trim(),
                            endTime = endTime.trim()
                        )
                        onCreate(newSlot)
                    }
                },
                enabled = isFormValid && conflict == null
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownSelector(
    items: List<T>,
    selectedItem: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedItem?.let(label) ?: "Select...",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(label(item)) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
