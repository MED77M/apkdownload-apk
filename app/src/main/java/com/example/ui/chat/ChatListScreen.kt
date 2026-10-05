package com.example.ui.chat

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.ChatConversation
import com.example.data.model.Role
import com.example.data.model.SchoolUser
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatusBadge
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import kotlinx.coroutines.launch

@Composable
fun ChatListScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onOpenConversation: (String, String, Boolean) -> Unit,
    onOpenSubjectQa: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = firebaseManager.currentUser ?: return

    val conversations by firebaseManager.observeConversations(currentUser.id).collectAsState(initial = emptyList())
    val allUsers by firebaseManager.observeUsers().collectAsState(initial = emptyList())
    val enrollments by firebaseManager.observeEnrollments().collectAsState(initial = emptyList())
    val timetableSlots by firebaseManager.observeTimetable().collectAsState(initial = emptyList())
    val groups by firebaseManager.observeGroups().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Private, 2: Groups, 3: Requests
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    // Pending incoming requests for the current user
    val pendingIncomingRequests = remember(conversations, currentUser) {
        conversations.filter { it.status == "PENDING" && it.requestReceiverId == currentUser.id }
    }

    // Filter conversations by tab
    val filteredConversations = remember(conversations, selectedTab, currentUser) {
        when (selectedTab) {
            1 -> conversations.filter { !it.isGroup && (it.status == "ACCEPTED" || it.requestSenderId == currentUser.id) }
            2 -> conversations.filter { it.isGroup }
            3 -> conversations.filter { it.status == "PENDING" && it.requestReceiverId == currentUser.id }
            else -> conversations.filter { it.status == "ACCEPTED" || it.requestSenderId == currentUser.id || it.requestReceiverId == currentUser.id }
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.chatTitle,
                subtitle = if (currentUser.role == Role.ADMIN) strings.moderationNotice else null,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                } else null,
                actions = {
                    IconButton(
                        onClick = onOpenSubjectQa,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = strings.qaTab, tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentUser.role == Role.TEACHER || currentUser.role == Role.ADMIN) {
                    SmallFloatingActionButton(
                        onClick = { showCreateGroupDialog = true },
                        containerColor = SchoolSecondary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_create_group")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = strings.newGroupChat)
                    }
                }
                FloatingActionButton(
                    onClick = { showNewChatDialog = true },
                    containerColor = SchoolPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_new_chat")
                ) {
                    Icon(Icons.Default.ChatBubble, contentDescription = "New Message")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SchoolPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(strings.privateTab, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(strings.groupsTab, fontWeight = FontWeight.Bold) }
                )
                if (pendingIncomingRequests.isNotEmpty()) {
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            BadgedBox(badge = {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("${pendingIncomingRequests.size}")
                                }
                            }) {
                                Text("طلبات المراسلة", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }

            // Pending requests alert card at top of list
            if (pendingIncomingRequests.isNotEmpty() && selectedTab != 3) {
                Card(
                    onClick = { selectedTab = 3 },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "لديك ${pendingIncomingRequests.size} طلب محادثة بانتظار موافقتك",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "اضغط هنا لمراجعة الطلبات والقبول أو الرفض",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309)
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFD97706))
                    }
                }
            }

            // Subject Q&A Space shortcut banner
            Card(
                onClick = onOpenSubjectQa,
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SchoolSecondary.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = SchoolSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = strings.qaTab, fontWeight = FontWeight.Bold, color = SchoolSecondary)
                        Text(text = "فضاء طرح الأسئلة الدراسية وتلقي الإجابات", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SchoolSecondary)
                }
            }

            if (filteredConversations.isEmpty()) {
                EmptyStateView(
                    message = if (selectedTab == 3) "لا توجد طلبات محادثة واردة حالياً" else strings.noDataYet,
                    icon = Icons.Default.ChatBubbleOutline,
                    actionText = if (selectedTab != 3) "بدء محادثة جديدة" else null,
                    onActionClick = { showNewChatDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredConversations, key = { it.id }) { conv ->
                        val unreadCount = conv.unreadMap[currentUser.id] ?: 0
                        val isGroup = conv.isGroup
                        val isPending = conv.status == "PENDING"
                        val isIncomingRequest = isPending && conv.requestReceiverId == currentUser.id
                        val isSentRequest = isPending && conv.requestSenderId == currentUser.id

                        val displayName = if (isGroup) {
                            conv.name
                        } else {
                            val otherId = conv.participantIds.firstOrNull { it != currentUser.id }
                            allUsers.firstOrNull { it.id == otherId }?.fullName ?: conv.name.ifEmpty { "Direct Chat" }
                        }

                        Card(
                            onClick = { onOpenConversation(conv.id, displayName, isGroup) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isIncomingRequest) Color(0xFFFFFBEB) else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isGroup) SchoolSecondary.copy(alpha = 0.15f)
                                                else if (isPending) Color(0xFFFEF3C7)
                                                else SchoolPrimary.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isGroup) Icons.Default.Groups else if (isPending) Icons.Default.PersonAdd else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isGroup) SchoolSecondary else if (isPending) Color(0xFFD97706) else SchoolPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isGroup) {
                                                StatusBadge(text = "Group", color = SchoolSecondary)
                                            } else if (isSentRequest) {
                                                StatusBadge(text = "في انتظار الموافقة", color = Color(0xFFD97706))
                                            } else if (isIncomingRequest) {
                                                StatusBadge(text = "طلب وارد", color = Color(0xFF2563EB))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isIncomingRequest) "يرغب هذا الطالب في مراسلتك" else conv.lastMessage.ifEmpty { if (isPending) "طلب قيد الانتظار" else "لا توجد رسائل بعد" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (unreadCount > 0) SchoolPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }

                                    if (unreadCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Badge(containerColor = SchoolPrimary) {
                                            Text("$unreadCount")
                                        }
                                    }
                                }

                                // Quick Accept / Decline actions if this is an incoming request
                                if (isIncomingRequest) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    firebaseManager.acceptChatRequest(conv.id)
                                                    Toast.makeText(context, "تم قبول طلب المحادثة بنجاح", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("قبول المحادثة")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    firebaseManager.declineChatRequest(conv.id)
                                                    Toast.makeText(context, "تم رفض الطلب", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("رفض الطلب")
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

    // New Chat Dialog with strict Student Privacy and Teacher-Student Filtering
    if (showNewChatDialog) {
        NewChatModalDialog(
            currentUser = currentUser,
            allUsers = allUsers,
            enrollments = enrollments,
            timetableSlots = timetableSlots,
            groups = groups,
            strings = strings,
            firebaseManager = firebaseManager,
            onDismiss = { showNewChatDialog = false },
            onConversationCreated = { convId, name, isGroup ->
                showNewChatDialog = false
                onOpenConversation(convId, name, isGroup)
            }
        )
    }

    // Create Group Chat Dialog (Teacher / Admin)
    if (showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        
        // Strictly filter students for teachers to only enrolled students
        val selectableStudents = remember(allUsers, currentUser, enrollments, timetableSlots, groups) {
            val baseStudents = allUsers.filter { it.role == Role.STUDENT && it.isActive }
            if (currentUser.role == Role.TEACHER) {
                baseStudents.filter { s ->
                    firebaseManager.isStudentEnrolledWithTeacher(s, currentUser, enrollments, timetableSlots, groups)
                }
            } else {
                baseStudents
            }
        }
        val selectedStudents = remember { mutableStateListOf<String>() }

        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text(strings.newGroupChat) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (currentUser.role == Role.TEACHER) {
                        Surface(
                            color = SchoolSecondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔒 يظهر فقط الطلاب المسجلون في مادتك وفصولك.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SchoolSecondary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Text("Add Students (${selectedStudents.size} selected):", style = MaterialTheme.typography.labelMedium)
                    if (selectableStudents.isEmpty()) {
                        Text("لا يوجد طلاب مسجلون حالياً في مادتك لإضافتهم.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(selectableStudents) { s ->
                                val isChecked = selectedStudents.contains(s.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            if (it) selectedStudents.add(s.id) else selectedStudents.remove(s.id)
                                        }
                                    )
                                    Text(s.fullName, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            val participants = (selectedStudents + currentUser.id).distinct()
                            scope.launch {
                                val res = firebaseManager.createOrGetConversation(
                                    participantIds = participants,
                                    name = groupName.trim(),
                                    isGroup = true,
                                    creatorId = currentUser.id
                                )
                                res.onSuccess { convId ->
                                    showCreateGroupDialog = false
                                    onOpenConversation(convId, groupName.trim(), true)
                                }
                            }
                        }
                    },
                    enabled = groupName.isNotBlank()
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun NewChatModalDialog(
    currentUser: SchoolUser,
    allUsers: List<SchoolUser>,
    enrollments: List<com.example.data.model.Enrollment>,
    timetableSlots: List<com.example.data.model.TimetableSlot>,
    groups: List<com.example.data.model.SchoolGroup>,
    strings: com.example.localization.AppStrings,
    firebaseManager: FirebaseManager,
    onDismiss: () -> Unit,
    onConversationCreated: (String, String, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var studentSearchUsername by remember { mutableStateOf("") }
    var selectedStudentTab by remember { mutableStateOf(0) } // 0: Teachers & Admin, 1: Request Student Chat
    var isSendingRequest by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (currentUser.role) {
                    Role.STUDENT -> "بدء محادثة جديدة"
                    Role.TEACHER -> "مراسلة طالب أو زميل"
                    Role.ADMIN -> "مراسلة مستخدم"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentUser.role == Role.STUDENT) {
                    // Privacy tab row for students
                    TabRow(
                        selectedTabIndex = selectedStudentTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = SchoolPrimary
                    ) {
                        Tab(
                            selected = selectedStudentTab == 0,
                            onClick = { selectedStudentTab = 0 },
                            text = { Text("الأساتذة والإدارة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) }
                        )
                        Tab(
                            selected = selectedStudentTab == 1,
                            onClick = { selectedStudentTab = 1 },
                            text = { Text("طلب مراسلة طالب", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) }
                        )
                    }

                    if (selectedStudentTab == 0) {
                        // Directly list Teachers & Admin
                        val teachersAndAdmins = remember(allUsers) {
                            allUsers.filter { (it.role == Role.TEACHER || it.role == Role.ADMIN) && it.isActive }
                        }
                        if (teachersAndAdmins.isEmpty()) {
                            Text(
                                "لا يوجد أساتذة مسجلون حالياً",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                                items(teachersAndAdmins) { teacher ->
                                    ListItem(
                                        headlineContent = { Text(teacher.fullName, fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text(if (teacher.role == Role.ADMIN) "إدارة المدرسة" else "أستاذ • @${teacher.username}") },
                                        leadingContent = {
                                            Icon(
                                                imageVector = if (teacher.role == Role.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.School,
                                                contentDescription = null,
                                                tint = SchoolPrimary
                                            )
                                        },
                                        trailingContent = {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        val res = firebaseManager.createOrGetConversation(
                                                            participantIds = listOf(currentUser.id, teacher.id),
                                                            name = teacher.fullName,
                                                            isGroup = false,
                                                            creatorId = currentUser.id,
                                                            initialStatus = "ACCEPTED"
                                                        )
                                                        res.onSuccess { convId ->
                                                            onConversationCreated(convId, teacher.fullName, false)
                                                        }
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("محادثة")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Student-to-Student Request: NO PUBLIC STUDENT LIST EXPOSED!
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF2563EB))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🔒 حماية الخصوصية: لا تظهر معلومات الطلاب الشخصية. أدخل اسم المستخدم (@username) لإرسال طلب محادثة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1E40AF)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = studentSearchUsername,
                            onValueChange = { studentSearchUsername = it },
                            label = { Text("اسم مستخدم الطالب (Username)") },
                            placeholder = { Text("مثال: ahmed_2026") },
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val cleanTarget = studentSearchUsername.trim().lowercase().removePrefix("@")
                                if (cleanTarget.isBlank()) return@Button
                                if (cleanTarget == currentUser.username.trim().lowercase()) {
                                    Toast.makeText(context, "لا يمكنك مراسلة نفسك!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isSendingRequest = true
                                scope.launch {
                                    val targetUser = allUsers.firstOrNull { it.username.trim().lowercase() == cleanTarget && it.isActive }
                                        ?: firebaseManager.getUser(cleanTarget)

                                    if (targetUser == null) {
                                        isSendingRequest = false
                                        Toast.makeText(context, "لم يتم العثور على طالب بهذا الاسم.", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    val res = firebaseManager.createOrGetConversation(
                                        participantIds = listOf(currentUser.id, targetUser.id),
                                        name = targetUser.fullName,
                                        isGroup = false,
                                        creatorId = currentUser.id,
                                        initialStatus = if (targetUser.role == Role.STUDENT) "PENDING" else "ACCEPTED"
                                    )
                                    isSendingRequest = false
                                    res.onSuccess { convId ->
                                        Toast.makeText(context, "تم إرسال طلب المحادثة بنجاح", Toast.LENGTH_SHORT).show()
                                        onConversationCreated(convId, targetUser.fullName, false)
                                    }.onFailure {
                                        Toast.makeText(context, "فشل إرسال الطلب: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isSendingRequest && studentSearchUsername.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSendingRequest) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إرسال طلب محادثة")
                            }
                        }
                    }
                } else if (currentUser.role == Role.TEACHER) {
                    // TEACHER: Show ONLY enrolled students in teacher's subject & colleagues/admin
                    Surface(
                        color = SchoolSecondary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔒 يظهر فقط الطلاب المسجلون في مادتك وفصولك الدراسية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SchoolSecondary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    val eligibleContacts = remember(allUsers, currentUser, enrollments, timetableSlots, groups) {
                        allUsers.filter { user ->
                            user.id != currentUser.id && user.isActive && (
                                user.role == Role.ADMIN ||
                                user.role == Role.TEACHER ||
                                (user.role == Role.STUDENT && firebaseManager.isStudentEnrolledWithTeacher(user, currentUser, enrollments, timetableSlots, groups))
                            )
                        }
                    }

                    if (eligibleContacts.isEmpty()) {
                        Text(
                            "لا يوجد طلاب مسجلون في مادتك حالياً",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(eligibleContacts) { user ->
                                ListItem(
                                    headlineContent = { Text(user.fullName, fontWeight = FontWeight.Bold) },
                                    supportingContent = {
                                        Text(
                                            when (user.role) {
                                                Role.STUDENT -> "طالب مسجل في مادتك"
                                                Role.TEACHER -> "أستاذ زميل"
                                                Role.ADMIN -> "إدارة المدرسة"
                                            }
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            imageVector = when (user.role) {
                                                Role.STUDENT -> Icons.Default.Person
                                                Role.TEACHER -> Icons.Default.School
                                                Role.ADMIN -> Icons.Default.AdminPanelSettings
                                            },
                                            contentDescription = null,
                                            tint = SchoolPrimary
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingContent = {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val res = firebaseManager.createOrGetConversation(
                                                        participantIds = listOf(currentUser.id, user.id),
                                                        name = user.fullName,
                                                        isGroup = false,
                                                        creatorId = currentUser.id,
                                                        initialStatus = "ACCEPTED"
                                                    )
                                                    res.onSuccess { convId ->
                                                        onConversationCreated(convId, user.fullName, false)
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("مراسلة")
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // ADMIN: Can message any user
                    val contacts = remember(allUsers, currentUser) {
                        allUsers.filter { it.id != currentUser.id && it.isActive }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(contacts) { user ->
                            ListItem(
                                headlineContent = { Text(user.fullName, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text("${user.role.name} • @${user.username}") },
                                leadingContent = {
                                    Icon(
                                        imageVector = when (user.role) {
                                            Role.STUDENT -> Icons.Default.Person
                                            Role.TEACHER -> Icons.Default.School
                                            Role.ADMIN -> Icons.Default.AdminPanelSettings
                                        },
                                        contentDescription = null,
                                        tint = SchoolPrimary
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                trailingContent = {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val res = firebaseManager.createOrGetConversation(
                                                    participantIds = listOf(currentUser.id, user.id),
                                                    name = user.fullName,
                                                    isGroup = false,
                                                    creatorId = currentUser.id,
                                                    initialStatus = "ACCEPTED"
                                                )
                                                res.onSuccess { convId ->
                                                    onConversationCreated(convId, user.fullName, false)
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("مراسلة")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}
