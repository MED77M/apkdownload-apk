package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.FirebaseManager
import com.example.data.model.*
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatusBadge
import com.example.ui.theme.SchoolAccentRed
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    // 0: Timetable Sessions, 1: Rooms & Facilities
    var activeMainTab by remember { mutableStateOf(0) }
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
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var editingRoom by remember { mutableStateOf<Room?>(null) }
    var roomToDelete by remember { mutableStateOf<Room?>(null) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

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
                subtitle = if (activeMainTab == 0) strings.weeklySchedule else strings.roomsTitle,
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
                    // Search icon for finding any student or teacher
                    IconButton(
                        onClick = { showSearchDialog = true },
                        modifier = Modifier.size(48.dp).testTag("btn_timetable_search_users")
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = strings.searchUsersTitle, tint = Color.White)
                    }
                    IconButton(
                        onClick = { showAddGroupDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Class, contentDescription = strings.addGroup, tint = Color.White)
                    }
                    IconButton(
                        onClick = { showAddSubjectDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Book, contentDescription = strings.addSubject, tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (activeMainTab == 0) showAddSlotDialog = true
                    else showAddRoomDialog = true
                },
                containerColor = SchoolPrimary,
                contentColor = Color.White,
                icon = {
                    Icon(
                        imageVector = if (activeMainTab == 0) Icons.Default.AddAlarm else Icons.Default.AddBusiness,
                        contentDescription = null
                    )
                },
                text = {
                    Text(if (activeMainTab == 0) strings.addSlot else strings.addRoom)
                },
                modifier = Modifier.testTag(if (activeMainTab == 0) "fab_add_slot" else "fab_add_room")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Section Tabs: [جدول الحصص] | [إدارة القاعات]
            PrimaryTabRow(
                selectedTabIndex = activeMainTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SchoolPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeMainTab == 0,
                    onClick = { activeMainTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${strings.navTimetable} (${slots.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("tab_timetable")
                )
                Tab(
                    selected = activeMainTab == 1,
                    onClick = { activeMainTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${strings.roomsTitle} (${rooms.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("tab_rooms")
                )
            }

            if (activeMainTab == 0) {
                // ==========================================
                // VIEW 1: WEEKLY TIMETABLE SESSIONS
                // ==========================================

                // Day Selector Tabs with session count badge
                ScrollableTabRow(
                    selectedTabIndex = selectedDay - 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = SchoolPrimary,
                    edgePadding = 12.dp
                ) {
                    daysOfWeek.forEach { (dayNum, dayName) ->
                        val count = slots.count { it.dayOfWeek == dayNum }
                        Tab(
                            selected = selectedDay == dayNum,
                            onClick = { selectedDay = dayNum },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(dayName, fontWeight = if (selectedDay == dayNum) FontWeight.Bold else FontWeight.Normal)
                                    if (count > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (selectedDay == dayNum) SchoolPrimary else Color.Gray.copy(alpha = 0.3f),
                                            contentColor = if (selectedDay == dayNum) Color.White else MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = count.toString(),
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
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
            } else {
                // ==========================================
                // VIEW 2: ROOMS & FACILITIES MANAGEMENT
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Card with Stats and Add Room button
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = strings.roomsTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SchoolPrimary
                                )
                                Text(
                                    text = "إجمالي: ${rooms.size} قاعات • الطاقة: ${rooms.sumOf { it.capacity }} طالب",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { showAddRoomDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SchoolPrimary),
                                modifier = Modifier.testTag("btn_add_room_header")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.addRoom, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (rooms.isEmpty()) {
                        EmptyStateView(
                            message = strings.noRoomsYet,
                            icon = Icons.Default.MeetingRoom,
                            actionText = strings.addRoom,
                            onActionClick = { showAddRoomDialog = true }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(rooms, key = { it.id }) { room ->
                                val roomSlotsCount = slots.count {
                                    it.roomId == room.id || it.roomName.equals(room.name, ignoreCase = true)
                                }
                                RoomCardItem(
                                    room = room,
                                    scheduledCount = roomSlotsCount,
                                    strings = strings,
                                    onEdit = { editingRoom = room },
                                    onDelete = { roomToDelete = room }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // DIALOGS: ADD / EDIT ROOM (مرتب وواضح)
    // ----------------------------------------------------
    if (showAddRoomDialog || editingRoom != null) {
        val targetRoom = editingRoom
        RoomFormDialog(
            initialRoom = targetRoom,
            strings = strings,
            onDismiss = {
                showAddRoomDialog = false
                editingRoom = null
            },
            onSave = { name, capacity ->
                scope.launch {
                    if (targetRoom != null) {
                        firebaseManager.updateRoom(targetRoom.copy(name = name, capacity = capacity))
                    } else {
                        firebaseManager.addRoom(Room(name = name, capacity = capacity))
                    }
                    showAddRoomDialog = false
                    editingRoom = null
                }
            }
        )
    }

    // Delete Room Confirmation Dialog
    if (roomToDelete != null) {
        AlertDialog(
            onDismissRequest = { roomToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = SchoolAccentRed) },
            title = { Text(strings.deleteRoomConfirm, fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف القاعة: ${roomToDelete!!.name} (${roomToDelete!!.capacity} مقعد)") },
            confirmButton = {
                Button(
                    onClick = {
                        val rid = roomToDelete!!.id
                        roomToDelete = null
                        scope.launch {
                            firebaseManager.deleteRoom(rid)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolAccentRed)
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { roomToDelete = null }) { Text(strings.cancel) }
            }
        )
    }

    // ----------------------------------------------------
    // DIALOGS: ADD / EDIT TIMETABLE SLOT (مرتب وواضح جداً)
    // ----------------------------------------------------
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

    editingSlot?.let { slot ->
        EditSlotDialog(
            slot = slot,
            strings = strings,
            groups = groups,
            subjects = subjects,
            rooms = rooms,
            teachers = teachers,
            existingSlots = slots,
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

    // Dialogs to add Group, Subject
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

    // Search dialog to find any student or teacher
    if (showSearchDialog) {
        UserSearchDialog(
            firebaseManager = firebaseManager,
            strings = strings,
            onDismiss = { showSearchDialog = false }
        )
    }
}

// ----------------------------------------------------
// ROOM CARD COMPONENT (مرتب وواضح)
// ----------------------------------------------------
@Composable
fun RoomCardItem(
    room: Room,
    scheduledCount: Int,
    strings: AppStrings,
    onEdit: () -> Unit,
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SchoolPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = null,
                    tint = SchoolPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        text = "👥 ${room.capacity} طالب",
                        color = Color(0xFF16A34A)
                    )
                    StatusBadge(
                        text = "📅 $scheduledCount حصص",
                        color = SchoolPrimary
                    )
                }
            }

            // Edit & Delete actions
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SchoolPrimary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SchoolAccentRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ----------------------------------------------------
// ROOM FORM DIALOG (تصميم مرتب ومنظم ومريح وواضح جداً)
// ----------------------------------------------------
@Composable
fun RoomFormDialog(
    initialRoom: Room?,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (name: String, capacity: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialRoom?.name ?: "") }
    var capacityText by remember { mutableStateOf(initialRoom?.capacity?.toString() ?: "30") }
    var selectedFacilities by remember {
        mutableStateOf(setOf("مسلاط ضوئي Data Show", "سبورة ذكية تفاعلية"))
    }

    val capacityPresets = listOf(15, 20, 25, 30, 40, 50)
    val roomTypeSuggestions = listOf(
        "قاعة تدريس" to "🏫",
        "مختبر علوم" to "🔬",
        "إعلام آلي" to "💻",
        "مدرج محاضرات" to "🏛️",
        "ورشة أنشطة" to "🎨",
        "قاعة مطالعة" to "📚"
    )

    val availableFacilities = listOf(
        "مسلاط ضوئي Data Show" to "📽️",
        "سبورة ذكية تفاعلية" to "🖥️",
        "مكيف هواء" to "❄️",
        "شبكة Wi-Fi" to "📶",
        "نظام صوتي ومكبرات" to "🔊"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SchoolPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (initialRoom != null) Icons.Default.EditLocationAlt else Icons.Default.AddBusiness,
                                contentDescription = null,
                                tint = SchoolPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (initialRoom != null) strings.editRoom else strings.addRoom,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = SchoolPrimary
                            )
                            Text(
                                text = "تحديد اسم القاعة وطاقتها وتجهيزاتها",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Form Sections
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SECTION 1: Room Identity & Type (بطاقة بيانات القاعة)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = SchoolPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "1. اسم ونوع القاعة",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // Quick Category selection chips
                            Text(
                                text = "اختيار نوع القاعة سريعاً:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                roomTypeSuggestions.take(3).forEach { (type, emoji) ->
                                    FilterChip(
                                        selected = name.contains(type),
                                        onClick = {
                                            name = if (name.isBlank()) type else "$type $name".trim()
                                        },
                                        label = { Text("$emoji $type", fontSize = 11.sp) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                roomTypeSuggestions.drop(3).forEach { (type, emoji) ->
                                    FilterChip(
                                        selected = name.contains(type),
                                        onClick = {
                                            name = if (name.isBlank()) type else "$type $name".trim()
                                        },
                                        label = { Text("$emoji $type", fontSize = 11.sp) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(strings.roomName + " *") },
                                placeholder = { Text("مثال: قاعة 1، مختبر الفيزياء، مدرج ابن خلدون") },
                                leadingIcon = { Icon(Icons.Default.DoorSliding, contentDescription = null, tint = SchoolPrimary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_room_name")
                            )
                        }
                    }

                    // SECTION 2: Capacity & Seating (بطاقة الطاقة الاستيعابية)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "2. الطاقة الاستيعابية (عدد المقاعد)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // Stepper Controls (- / + 5)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        val cur = capacityText.toIntOrNull() ?: 30
                                        if (cur > 5) capacityText = (cur - 5).toString()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(46.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF16A34A).copy(alpha = 0.12f),
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Chair, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$capacityText مقعداً",
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }

                                FilledTonalButton(
                                    onClick = {
                                        val cur = capacityText.toIntOrNull() ?: 30
                                        capacityText = (cur + 5).toString()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(46.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }

                            // Quick Capacity Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                capacityPresets.forEach { preset ->
                                    FilterChip(
                                        selected = capacityText == preset.toString(),
                                        onClick = { capacityText = preset.toString() },
                                        label = { Text("$preset", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 3: Facilities & Equipment (تجهيزات القاعة)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Devices, contentDescription = null, tint = SchoolSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "3. التجهيزات والمرافق المتوفرة (اختياري)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                availableFacilities.forEach { (facility, emoji) ->
                                    val isSelected = selectedFacilities.contains(facility)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedFacilities = if (isSelected) {
                                                selectedFacilities - facility
                                            } else {
                                                selectedFacilities + facility
                                            }
                                        },
                                        label = { Text("$emoji $facility", fontSize = 12.sp) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Buttons: Save & Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(strings.cancel)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val cap = capacityText.toIntOrNull() ?: 30
                                onSave(name.trim(), cap)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolPrimary),
                        modifier = Modifier
                            .weight(1.5f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_save_room")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.save, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TIMETABLE SLOT CARD COMPONENT (مرتب وواضح)
// ----------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableSlotCard(
    slot: TimetableSlot,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Time Badge + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Range Badge
                Surface(
                    color = SchoolPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = SchoolPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${slot.startTime} - ${slot.endTime}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SchoolPrimary
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Slot", tint = SchoolPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Slot", tint = SchoolAccentRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subject Title
            Text(
                text = slot.subjectName.ifEmpty { "مادة دراسية" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Details Row: Level • Teacher • Room • Group
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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

// ----------------------------------------------------
// CREATE TIMETABLE SLOT DIALOG (اختيار المواد والمستويات المسجلة والربط التلقائي)
// ----------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateSlotDialog(
    strings: AppStrings,
    groups: List<SchoolGroup>,
    subjects: List<Subject>,
    rooms: List<Room>,
    teachers: List<SchoolUser>,
    existingSlots: List<TimetableSlot>,
    initialDay: Int,
    onDismiss: () -> Unit,
    onCreate: (TimetableSlot) -> Unit
) {
    var dayOfWeek by remember { mutableStateOf(initialDay) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("10:00") }

    // Collect all entered educational levels from subjects and groups
    val enteredLevels = remember(subjects, groups) {
        val lvls = (subjects.map { it.level } + groups.map { it.level }).filter { it.isNotBlank() }.distinct()
        if (lvls.isNotEmpty()) lvls else listOf("الابتدائي", "الأولى إعدادي", "الثانية إعدادي", "الثالثة إعدادي", "الجذع المشترك", "الأولى باكالوريا", "الثانية باكالوريا")
    }

    var selectedLevelFilter by remember { mutableStateOf("الكل") }

    var selectedSubjectObj by remember { mutableStateOf(subjects.firstOrNull()) }
    var subjectText by remember { mutableStateOf(selectedSubjectObj?.name ?: "") }
    var levelText by remember { mutableStateOf(selectedSubjectObj?.level ?: "") }

    var selectedGroupObj by remember { mutableStateOf(groups.firstOrNull()) }
    var groupText by remember { mutableStateOf(selectedGroupObj?.name ?: "") }

    var selectedTeacherObj by remember {
        mutableStateOf(
            selectedSubjectObj?.teacherId?.let { tId -> teachers.firstOrNull { it.id == tId } }
                ?: teachers.firstOrNull()
        )
    }
    var teacherText by remember { mutableStateOf(selectedTeacherObj?.fullName ?: "") }

    var selectedRoomObj by remember { mutableStateOf(rooms.firstOrNull()) }
    var roomText by remember { mutableStateOf(selectedRoomObj?.name ?: "") }

    // Filtered subjects based on selected level
    val filteredSubjects = remember(subjects, selectedLevelFilter) {
        if (selectedLevelFilter == "الكل" || selectedLevelFilter.isBlank()) {
            subjects
        } else {
            subjects.filter { it.level.equals(selectedLevelFilter, ignoreCase = true) }
        }
    }

    // Filtered groups based on selected level
    val filteredGroups = remember(groups, selectedLevelFilter) {
        if (selectedLevelFilter == "الكل" || selectedLevelFilter.isBlank()) {
            groups
        } else {
            groups.filter { it.level.isBlank() || it.level.equals(selectedLevelFilter, ignoreCase = true) }
        }
    }

    val quickTimePresets = listOf(
        "08:00" to "10:00",
        "10:00" to "12:00",
        "14:00" to "16:00",
        "16:00" to "18:00",
        "18:00" to "20:00"
    )

    val daysList = listOf(
        1 to strings.monday,
        2 to strings.tuesday,
        3 to strings.wednesday,
        4 to strings.thursday,
        5 to strings.friday,
        6 to strings.saturday,
        7 to strings.sunday
    )

    // Intelligent Conflict Check
    val conflict = remember(dayOfWeek, startTime, endTime, roomText, teacherText, existingSlots) {
        val candidate = TimetableSlot(
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime,
            roomName = roomText.trim(),
            teacherName = teacherText.trim()
        )
        val roomConflict = roomText.isNotBlank() && existingSlots.any { other ->
            other.dayOfWeek == dayOfWeek && other.roomName.equals(candidate.roomName, ignoreCase = true) && candidate.overlapsWith(other)
        }
        val teacherConflict = teacherText.isNotBlank() && existingSlots.any { other ->
            other.dayOfWeek == dayOfWeek && other.teacherName.equals(candidate.teacherName, ignoreCase = true) && candidate.overlapsWith(other)
        }

        when {
            roomConflict -> strings.conflictRoom
            teacherConflict -> strings.conflictTeacher
            else -> null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with icon, title, and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SchoolPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddAlarm,
                                contentDescription = null,
                                tint = SchoolPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.addSlot,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = SchoolPrimary
                            )
                            Text(
                                text = "تحديد المادة والمستوى والتوقيت والقاعة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form Sections
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Conflict Warning Banner if detected
                    if (conflict != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

                    // SECTION 1: 🕒 Day & Time Schedule (توقيت ويوم الحصة)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SchoolPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "1. اختيار اليوم والتوقيت",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // Day selector chips
                            Text(
                                text = "اختر يوم الحصة:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                daysList.take(4).forEach { (dNum, dName) ->
                                    FilterChip(
                                        selected = dayOfWeek == dNum,
                                        onClick = { dayOfWeek = dNum },
                                        label = { Text(dName, fontSize = 11.sp, fontWeight = if (dayOfWeek == dNum) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                daysList.drop(4).forEach { (dNum, dName) ->
                                    FilterChip(
                                        selected = dayOfWeek == dNum,
                                        onClick = { dayOfWeek = dNum },
                                        label = { Text(dName, fontSize = 11.sp, fontWeight = if (dayOfWeek == dNum) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }

                            // Time Range: Start and End time fields side by side
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = { startTime = it },
                                    label = { Text(strings.startTime) },
                                    placeholder = { Text("08:00") },
                                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = SchoolPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = endTime,
                                    onValueChange = { endTime = it },
                                    label = { Text(strings.endTime) },
                                    placeholder = { Text("10:00") },
                                    leadingIcon = { Icon(Icons.Default.AccessTimeFilled, contentDescription = null, tint = SchoolPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Quick Time Presets
                            Text(
                                text = "أوقات الحصص المعتادة:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickTimePresets.forEach { (s, e) ->
                                    val isSelected = startTime == s && endTime == e
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            startTime = s
                                            endTime = e
                                        },
                                        label = { Text("$s - $e", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 2: 🎓 Educational Level & Subject (المستوى والمادة المسجلة)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "2. اختيار المستوى الدراسي والمادة",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // 1. Level Filter Chips (المستويات المسجلة)
                            Text(
                                text = "المستوى الدراسي المسجل:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SchoolPrimary
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val allLevelOptions = listOf("الكل") + enteredLevels
                                allLevelOptions.forEach { lvl ->
                                    val isSelected = selectedLevelFilter.equals(lvl, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedLevelFilter = lvl
                                            if (lvl != "الكل") {
                                                levelText = lvl
                                            }
                                        },
                                        label = { Text(lvl, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = levelText,
                                onValueChange = {
                                    levelText = it
                                },
                                label = { Text("المستوى الدراسي (أو اكتب مخصص)") },
                                placeholder = { Text("مثال: الأولى باكالوريا، الثالثة إعدادي") },
                                leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFFD97706)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // 2. Subject Choices (المواد المسجلة في هذا المستوى)
                            Text(
                                text = "المواد المسجلة المتاحة ${if (selectedLevelFilter != "الكل") "($selectedLevelFilter)" else ""}:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SchoolPrimary
                            )

                            if (filteredSubjects.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    filteredSubjects.forEach { sb ->
                                        val isSelected = selectedSubjectObj?.id == sb.id || (subjectText.equals(sb.name, ignoreCase = true) && (levelText.isBlank() || sb.level.equals(levelText, ignoreCase = true)))
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedSubjectObj = sb
                                                subjectText = sb.name
                                                levelText = sb.level
                                                if (sb.level.isNotBlank() && selectedLevelFilter == "الكل") {
                                                    selectedLevelFilter = sb.level
                                                }
                                                if (sb.teacherId.isNotBlank()) {
                                                    val matchedTeacher = teachers.firstOrNull { it.id == sb.teacherId || it.fullName.equals(sb.teacherName, ignoreCase = true) }
                                                    if (matchedTeacher != null) {
                                                        selectedTeacherObj = matchedTeacher
                                                        teacherText = matchedTeacher.fullName
                                                    } else if (sb.teacherName.isNotBlank()) {
                                                        teacherText = sb.teacherName
                                                    }
                                                }
                                            },
                                            label = {
                                                val labelText = if (selectedLevelFilter == "الكل" && sb.level.isNotBlank()) "${sb.name} (${sb.level})" else sb.name
                                                Text(labelText, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = subjectText,
                                onValueChange = {
                                    subjectText = it
                                    selectedSubjectObj = subjects.firstOrNull { s -> s.name.equals(it.trim(), ignoreCase = true) && (levelText.isBlank() || s.level.equals(levelText.trim(), ignoreCase = true)) }
                                },
                                label = { Text(strings.subjectName + " *") },
                                placeholder = { Text("اختر أو اكتب اسم المادة (مثال: رياضيات، فيزياء)") },
                                leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFFD97706)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_slot_subject")
                            )
                        }
                    }

                    // SECTION 3: 👥 Group (الفوج / القسم)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = SchoolPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "3. الفوج والقسم",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // Group Quick Chips if available
                            if (filteredGroups.isNotEmpty()) {
                                Text(
                                    text = "الأفواج المسجلة:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    filteredGroups.forEach { gr ->
                                        val isSelected = selectedGroupObj?.id == gr.id || groupText.equals(gr.name, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedGroupObj = gr
                                                groupText = gr.name
                                            },
                                            label = { Text(gr.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = groupText,
                                onValueChange = {
                                    groupText = it
                                    selectedGroupObj = groups.firstOrNull { g -> g.name.equals(it.trim(), ignoreCase = true) }
                                },
                                label = { Text(strings.groupsTitle) },
                                placeholder = { Text("اختر أو اكتب الفوج (مثال: الفوج 1، السنة أولى)") },
                                leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, tint = SchoolPrimary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SECTION 4: 👨‍🏫 Teacher Assignment (الأستاذ المسؤول)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "4. الأستاذ المشرف على الحصة",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // Teachers chips
                            if (teachers.isNotEmpty()) {
                                Text(
                                    text = "الأساتذة المسجلون في المدرسة:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    teachers.forEach { tc ->
                                        val isSelected = selectedTeacherObj?.id == tc.id || teacherText.equals(tc.fullName, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                teacherText = tc.fullName
                                                selectedTeacherObj = tc
                                            },
                                            label = { Text("👨‍🏫 ${tc.fullName}", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = teacherText,
                                onValueChange = {
                                    teacherText = it
                                    selectedTeacherObj = teachers.firstOrNull { t -> t.fullName.equals(it.trim(), ignoreCase = true) }
                                },
                                label = { Text(strings.teacherName) },
                                placeholder = { Text("اختر أو اكتب اسم الأستاذ المسؤول") },
                                leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF2563EB)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SECTION 5: 🚪 Room & Seating (حجز القاعة الدراسية)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "5. القاعة الدراسية والمكان",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // Rooms chips with capacity
                            if (rooms.isNotEmpty()) {
                                Text(
                                    text = "القاعات المتاحة في المدرسة:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rooms.forEach { rm ->
                                        val isSelected = selectedRoomObj?.id == rm.id || roomText.equals(rm.name, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                roomText = rm.name
                                                selectedRoomObj = rm
                                            },
                                            label = { Text("🚪 ${rm.name} (${rm.capacity} مقعد)", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = roomText,
                                onValueChange = {
                                    roomText = it
                                    selectedRoomObj = rooms.firstOrNull { r -> r.name.equals(it.trim(), ignoreCase = true) }
                                },
                                label = { Text(strings.roomName) },
                                placeholder = { Text("اختر أو اكتب القاعة (مثال: قاعة 1، مختبر)") },
                                leadingIcon = { Icon(Icons.Default.DoorSliding, contentDescription = null, tint = Color(0xFF16A34A)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Buttons: Save & Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(strings.cancel)
                    }

                    Button(
                        onClick = {
                            if (subjectText.isNotBlank()) {
                                val resolvedSubjId = selectedSubjectObj?.id
                                    ?: subjects.firstOrNull { it.name.equals(subjectText.trim(), ignoreCase = true) && (levelText.isBlank() || it.level.equals(levelText.trim(), ignoreCase = true)) }?.id
                                    ?: subjects.firstOrNull { it.name.equals(subjectText.trim(), ignoreCase = true) }?.id
                                    ?: ""

                                val resolvedGroupId = selectedGroupObj?.id
                                    ?: groups.firstOrNull { it.name.equals(groupText.trim(), ignoreCase = true) }?.id
                                    ?: ""

                                val resolvedTeacherId = selectedTeacherObj?.id
                                    ?: teachers.firstOrNull { it.fullName.equals(teacherText.trim(), ignoreCase = true) }?.id
                                    ?: ""

                                val resolvedRoomId = selectedRoomObj?.id
                                    ?: rooms.firstOrNull { it.name.equals(roomText.trim(), ignoreCase = true) }?.id
                                    ?: ""

                                onCreate(
                                    TimetableSlot(
                                        dayOfWeek = dayOfWeek,
                                        startTime = startTime.trim(),
                                        endTime = endTime.trim(),
                                        subjectId = resolvedSubjId,
                                        subjectName = subjectText.trim(),
                                        level = levelText.trim(),
                                        teacherName = teacherText.trim(),
                                        teacherId = resolvedTeacherId,
                                        roomName = roomText.trim(),
                                        roomId = resolvedRoomId,
                                        groupId = resolvedGroupId,
                                        groupName = groupText.trim()
                                    )
                                )
                            }
                        },
                        enabled = subjectText.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolPrimary),
                        modifier = Modifier
                            .weight(1.6f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_confirm_add_slot")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وإضافة الحصة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// EDIT TIMETABLE SLOT DIALOG (تعديل كامل مع اختيار المواد والمستويات)
// ----------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditSlotDialog(
    slot: TimetableSlot,
    strings: AppStrings,
    groups: List<SchoolGroup>,
    subjects: List<Subject>,
    rooms: List<Room>,
    teachers: List<SchoolUser>,
    existingSlots: List<TimetableSlot>,
    onDismiss: () -> Unit,
    onSave: (TimetableSlot) -> Unit
) {
    var dayOfWeek by remember { mutableStateOf(slot.dayOfWeek) }
    var startTime by remember { mutableStateOf(slot.startTime) }
    var endTime by remember { mutableStateOf(slot.endTime) }

    val enteredLevels = remember(subjects, groups) {
        val lvls = (subjects.map { it.level } + groups.map { it.level }).filter { it.isNotBlank() }.distinct()
        if (lvls.isNotEmpty()) lvls else listOf("الابتدائي", "الأولى إعدادي", "الثانية إعدادي", "الثالثة إعدادي", "الجذع المشترك", "الأولى باكالوريا", "الثانية باكالوريا")
    }

    var selectedLevelFilter by remember {
        mutableStateOf(if (slot.level.isNotBlank()) slot.level else "الكل")
    }

    var selectedSubjectObj by remember {
        mutableStateOf(subjects.firstOrNull { it.id == slot.subjectId || (it.name == slot.subjectName && it.level == slot.level) })
    }
    var subjectText by remember { mutableStateOf(slot.subjectName) }
    var levelText by remember { mutableStateOf(slot.level) }

    var selectedGroupObj by remember {
        mutableStateOf(groups.firstOrNull { it.id == slot.groupId || it.name == slot.groupName })
    }
    var groupText by remember { mutableStateOf(slot.groupName) }

    var selectedTeacherObj by remember {
        mutableStateOf(teachers.firstOrNull { it.id == slot.teacherId || it.fullName == slot.teacherName })
    }
    var teacherText by remember { mutableStateOf(slot.teacherName) }

    var selectedRoomObj by remember {
        mutableStateOf(rooms.firstOrNull { it.id == slot.roomId || it.name == slot.roomName })
    }
    var roomText by remember { mutableStateOf(slot.roomName) }

    val filteredSubjects = remember(subjects, selectedLevelFilter) {
        if (selectedLevelFilter == "الكل" || selectedLevelFilter.isBlank()) {
            subjects
        } else {
            subjects.filter { it.level.equals(selectedLevelFilter, ignoreCase = true) }
        }
    }

    val filteredGroups = remember(groups, selectedLevelFilter) {
        if (selectedLevelFilter == "الكل" || selectedLevelFilter.isBlank()) {
            groups
        } else {
            groups.filter { it.level.isBlank() || it.level.equals(selectedLevelFilter, ignoreCase = true) }
        }
    }

    val quickTimePresets = listOf(
        "08:00" to "10:00",
        "10:00" to "12:00",
        "14:00" to "16:00",
        "16:00" to "18:00",
        "18:00" to "20:00"
    )

    val daysList = listOf(
        1 to strings.monday,
        2 to strings.tuesday,
        3 to strings.wednesday,
        4 to strings.thursday,
        5 to strings.friday,
        6 to strings.saturday,
        7 to strings.sunday
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SchoolPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = SchoolPrimary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.editTimetableSlot,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = SchoolPrimary
                            )
                            Text(
                                text = "تعديل تفاصيل المادة والمستوى وتوقيت الحصة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form Sections
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SECTION 1: Time
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SchoolPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("1. اليوم والتوقيت", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                daysList.take(4).forEach { (dNum, dName) ->
                                    FilterChip(
                                        selected = dayOfWeek == dNum,
                                        onClick = { dayOfWeek = dNum },
                                        label = { Text(dName, fontSize = 11.sp, fontWeight = if (dayOfWeek == dNum) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                daysList.drop(4).forEach { (dNum, dName) ->
                                    FilterChip(
                                        selected = dayOfWeek == dNum,
                                        onClick = { dayOfWeek = dNum },
                                        label = { Text(dName, fontSize = 11.sp, fontWeight = if (dayOfWeek == dNum) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = { startTime = it },
                                    label = { Text(strings.startTime) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = endTime,
                                    onValueChange = { endTime = it },
                                    label = { Text(strings.endTime) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickTimePresets.forEach { (s, e) ->
                                    FilterChip(
                                        selected = startTime == s && endTime == e,
                                        onClick = {
                                            startTime = s
                                            endTime = e
                                        },
                                        label = { Text("$s - $e", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 2: Level & Subject
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("2. المستوى الدراسي والمادة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }

                            // Level Chips
                            Text("المستوى الدراسي المسجل:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SchoolPrimary)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val allLevelOptions = listOf("الكل") + enteredLevels
                                allLevelOptions.forEach { lvl ->
                                    val isSelected = selectedLevelFilter.equals(lvl, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedLevelFilter = lvl
                                            if (lvl != "الكل") levelText = lvl
                                        },
                                        label = { Text(lvl, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = levelText,
                                onValueChange = { levelText = it },
                                label = { Text("المستوى الدراسي") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Subject Chips
                            Text("المواد المسجلة المتاحة:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SchoolPrimary)
                            if (filteredSubjects.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    filteredSubjects.forEach { sb ->
                                        val isSelected = selectedSubjectObj?.id == sb.id || (subjectText.equals(sb.name, ignoreCase = true) && (levelText.isBlank() || sb.level.equals(levelText, ignoreCase = true)))
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedSubjectObj = sb
                                                subjectText = sb.name
                                                levelText = sb.level
                                                if (sb.teacherId.isNotBlank()) {
                                                    val matchedTeacher = teachers.firstOrNull { it.id == sb.teacherId || it.fullName.equals(sb.teacherName, ignoreCase = true) }
                                                    if (matchedTeacher != null) {
                                                        selectedTeacherObj = matchedTeacher
                                                        teacherText = matchedTeacher.fullName
                                                    }
                                                }
                                            },
                                            label = { Text(if (selectedLevelFilter == "الكل" && sb.level.isNotBlank()) "${sb.name} (${sb.level})" else sb.name, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = subjectText,
                                onValueChange = {
                                    subjectText = it
                                    selectedSubjectObj = subjects.firstOrNull { s -> s.name.equals(it.trim(), ignoreCase = true) && (levelText.isBlank() || s.level.equals(levelText.trim(), ignoreCase = true)) }
                                },
                                label = { Text(strings.subjectName + " *") },
                                leadingIcon = { Icon(Icons.Default.Book, contentDescription = null, tint = Color(0xFFD97706)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SECTION 3: Group, Teacher, Room
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Class, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("3. الفوج والأستاذ والقاعة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }

                            if (filteredGroups.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    filteredGroups.forEach { gr ->
                                        val isSelected = selectedGroupObj?.id == gr.id || groupText.equals(gr.name, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedGroupObj = gr
                                                groupText = gr.name
                                            },
                                            label = { Text(gr.name, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = groupText,
                                onValueChange = {
                                    groupText = it
                                    selectedGroupObj = groups.firstOrNull { g -> g.name.equals(it.trim(), ignoreCase = true) }
                                },
                                label = { Text(strings.groupsTitle) },
                                leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, tint = SchoolPrimary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (teachers.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    teachers.forEach { tc ->
                                        val isSelected = selectedTeacherObj?.id == tc.id || teacherText.equals(tc.fullName, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                teacherText = tc.fullName
                                                selectedTeacherObj = tc
                                            },
                                            label = { Text("👨‍🏫 ${tc.fullName}", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = teacherText,
                                onValueChange = {
                                    teacherText = it
                                    selectedTeacherObj = teachers.firstOrNull { t -> t.fullName.equals(it.trim(), ignoreCase = true) }
                                },
                                label = { Text(strings.teacherName) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2563EB)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (rooms.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rooms.forEach { rm ->
                                        val isSelected = selectedRoomObj?.id == rm.id || roomText.equals(rm.name, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                roomText = rm.name
                                                selectedRoomObj = rm
                                            },
                                            label = { Text("🚪 ${rm.name} (${rm.capacity})", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = roomText,
                                onValueChange = {
                                    roomText = it
                                    selectedRoomObj = rooms.firstOrNull { r -> r.name.equals(it.trim(), ignoreCase = true) }
                                },
                                label = { Text(strings.roomName) },
                                leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = Color(0xFF16A34A)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(strings.cancel)
                    }

                    Button(
                        onClick = {
                            val resolvedSubjId = selectedSubjectObj?.id
                                ?: subjects.firstOrNull { it.name.equals(subjectText.trim(), ignoreCase = true) && (levelText.isBlank() || it.level.equals(levelText.trim(), ignoreCase = true)) }?.id
                                ?: subjects.firstOrNull { it.name.equals(subjectText.trim(), ignoreCase = true) }?.id
                                ?: slot.subjectId

                            val resolvedGroupId = selectedGroupObj?.id
                                ?: groups.firstOrNull { it.name.equals(groupText.trim(), ignoreCase = true) }?.id
                                ?: slot.groupId

                            val resolvedTeacherId = selectedTeacherObj?.id
                                ?: teachers.firstOrNull { it.fullName.equals(teacherText.trim(), ignoreCase = true) }?.id
                                ?: slot.teacherId

                            val resolvedRoomId = selectedRoomObj?.id
                                ?: rooms.firstOrNull { it.name.equals(roomText.trim(), ignoreCase = true) }?.id
                                ?: slot.roomId

                            onSave(
                                slot.copy(
                                    dayOfWeek = dayOfWeek,
                                    startTime = startTime.trim(),
                                    endTime = endTime.trim(),
                                    subjectId = resolvedSubjId,
                                    subjectName = subjectText.trim(),
                                    level = levelText.trim(),
                                    teacherId = resolvedTeacherId,
                                    teacherName = teacherText.trim(),
                                    groupId = resolvedGroupId,
                                    groupName = groupText.trim(),
                                    roomId = resolvedRoomId,
                                    roomName = roomText.trim()
                                )
                            )
                        },
                        enabled = subjectText.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolPrimary),
                        modifier = Modifier
                            .weight(1.5f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.save, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownSelector(
    items: List<T>,
    selectedItem: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedItem?.let(label) ?: "",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp)
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
