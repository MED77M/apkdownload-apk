package com.example.ui.chat

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
import java.text.SimpleDateFormat
import java.util.*

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
    val scope = rememberCoroutineScope()
    val currentUser = firebaseManager.currentUser ?: return

    val conversations by firebaseManager.observeConversations(currentUser.id).collectAsState(initial = emptyList())
    val allUsers by firebaseManager.observeUsers().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Private, 2: Groups
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    val filteredConversations = remember(conversations, selectedTab) {
        when (selectedTab) {
            1 -> conversations.filter { !it.isGroup }
            2 -> conversations.filter { it.isGroup }
            else -> conversations
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
            }

            // Subject Q&A Space shortcut banner
            Card(
                onClick = onOpenSubjectQa,
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SchoolSecondary.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = SchoolSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = strings.qaTab, fontWeight = FontWeight.Bold, color = SchoolSecondary)
                        Text(text = "Subject Questions & Answers Space", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SchoolSecondary)
                }
            }

            if (filteredConversations.isEmpty()) {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.ChatBubbleOutline,
                    actionText = "Start a Chat",
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

                        val displayName = if (isGroup) {
                            conv.name
                        } else {
                            val otherId = conv.participantIds.firstOrNull { it != currentUser.id }
                            allUsers.firstOrNull { it.id == otherId }?.fullName ?: conv.name.ifEmpty { "Direct Chat" }
                        }

                        Card(
                            onClick = { onOpenConversation(conv.id, displayName, isGroup) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (isGroup) SchoolSecondary.copy(alpha = 0.15f) else SchoolPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isGroup) Icons.Default.Groups else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isGroup) SchoolSecondary else SchoolPrimary,
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
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = conv.lastMessage.ifEmpty { "No messages yet" },
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
                        }
                    }
                }
            }
        }
    }

    // New Direct Chat Dialog (Select contact)
    if (showNewChatDialog) {
        val eligibleContacts = remember(allUsers, currentUser) {
            allUsers.filter { it.id != currentUser.id && it.isActive }
        }

        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Select User to Message") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(eligibleContacts) { user ->
                        ListItem(
                            headlineContent = { Text(user.fullName, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${user.role.name} • @${user.username}") },
                            leadingContent = {
                                Icon(
                                    imageVector = if (user.role == Role.TEACHER) Icons.Default.School else Icons.Default.Person,
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
                                                creatorId = currentUser.id
                                            )
                                            res.onSuccess { convId ->
                                                showNewChatDialog = false
                                                onOpenConversation(convId, user.fullName, false)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Chat")
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) { Text(strings.close) }
            }
        )
    }

    // Create Group Chat Dialog (Teacher)
    if (showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        val students = remember(allUsers) { allUsers.filter { it.role == Role.STUDENT && it.isActive } }
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

                    Text("Add Students (${selectedStudents.size} selected):", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(students) { s ->
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
