package com.example.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.example.data.model.Role
import com.example.data.model.SchoolUser
import com.example.data.model.TeacherPermissions
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppButton
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Students, 2: Teachers, 3: Admins
    var searchQuery by remember { mutableStateOf("") }

    val allUsers by firebaseManager.observeUsers().collectAsState(initial = emptyList())

    val currentRole = when (selectedTab) {
        1 -> Role.STUDENT
        2 -> Role.TEACHER
        3 -> Role.ADMIN
        else -> Role.STUDENT
    }

    val users = remember(allUsers, selectedTab) {
        when (selectedTab) {
            1 -> allUsers.filter { it.role == Role.STUDENT }
            2 -> allUsers.filter { it.role == Role.TEACHER }
            3 -> allUsers.filter { it.role == Role.ADMIN }
            else -> allUsers
        }
    }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<SchoolUser?>(null) }
    var userToDelete by remember { mutableStateOf<SchoolUser?>(null) }
    var newlyCreatedCredentials by remember { mutableStateOf<Pair<String, String>?>(null) } // (username, password)
    var showSearchDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.usersTitle,
                subtitle = when (selectedTab) {
                    0 -> strings.allUsersTab
                    1 -> strings.studentsTab
                    2 -> strings.teachersTab
                    else -> strings.manageAdmins
                },
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
                        onClick = { showSearchDialog = true },
                        modifier = Modifier.size(48.dp).testTag("btn_users_header_search")
                    ) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = strings.searchUsersTitle,
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SchoolPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = {
                    Text(
                        when (selectedTab) {
                            1 -> strings.addStudent
                            2 -> strings.addTeacher
                            3 -> strings.addAdmin
                            else -> strings.addStudent
                        }
                    )
                },
                modifier = Modifier.testTag("fab_add_user")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Selector: All | Students | Teachers | Admins
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SchoolPrimary,
                edgePadding = 8.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("${strings.allUsersTab} (${allUsers.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("tab_all")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${strings.studentsTab} (${allUsers.count { it.role == Role.STUDENT }})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("tab_students")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("${strings.teachersTab} (${allUsers.count { it.role == Role.TEACHER }})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.School, contentDescription = null) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("tab_teachers")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("${strings.adminsTab} (${allUsers.count { it.role == Role.ADMIN }})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("tab_admins")
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("search_users_input")
            )

            // User List
            if (filteredUsers.isEmpty()) {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.PersonOff,
                    actionText = if (selectedTab == 0) strings.addStudent else strings.addTeacher,
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredUsers, key = { it.id }) { user ->
                        UserItemCard(
                            user = user,
                            currentUserId = firebaseManager.currentUser?.id ?: "",
                            strings = strings,
                            onEdit = { userToEdit = user },
                            onToggleStatus = {
                                if (user.isPrimaryAdmin) {
                                    Toast.makeText(context, strings.cannotDeletePrimaryAdmin, Toast.LENGTH_SHORT).show()
                                    return@UserItemCard
                                }
                                scope.launch {
                                    firebaseManager.toggleUserActive(user.id, !user.isActive)
                                }
                            },
                            onDelete = {
                                if (user.isPrimaryAdmin) {
                                    Toast.makeText(context, strings.cannotDeletePrimaryAdmin, Toast.LENGTH_SHORT).show()
                                    return@UserItemCard
                                }
                                userToDelete = user
                            }
                        )
                    }
                }
            }
        }
    }

    // Add User Dialog
    if (showAddDialog) {
        CreateUserDialog(
            role = currentRole,
            strings = strings,
            onDismiss = { showAddDialog = false },
            onCreate = { newUser, password ->
                scope.launch {
                    val res = firebaseManager.createUser(newUser, password)
                    res.onSuccess { createdUser ->
                        showAddDialog = false
                        newlyCreatedCredentials = Pair(createdUser.username, password)
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: strings.error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Edit User Dialog
    if (userToEdit != null) {
        EditUserDialog(
            user = userToEdit!!,
            strings = strings,
            onDismiss = { userToEdit = null },
            onSave = { updated ->
                scope.launch {
                    firebaseManager.updateUser(updated)
                    userToEdit = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text(strings.deleteUser) },
            text = { Text("${strings.confirmDelete}\n(${userToDelete!!.fullName} - @${userToDelete!!.username})") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uid = userToDelete!!.id
                        userToDelete = null
                        scope.launch {
                            firebaseManager.deleteUser(uid)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // One-Time Credentials Dialog
    if (newlyCreatedCredentials != null) {
        val (uname, pwd) = newlyCreatedCredentials!!
        AlertDialog(
            onDismissRequest = { newlyCreatedCredentials = null },
            icon = { Icon(Icons.Default.Key, contentDescription = null, tint = SchoolPrimary) },
            title = { Text(strings.credentialsTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.credentialsDesc, style = MaterialTheme.typography.bodySmall)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${strings.username}: $uname", fontWeight = FontWeight.Bold)
                            Text("${strings.password}: $pwd", fontWeight = FontWeight.Bold, color = SchoolPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Credentials", "Username: $uname\nPassword: $pwd")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, strings.credentialsCopied, Toast.LENGTH_SHORT).show()
                        newlyCreatedCredentials = null
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.copyCredentials)
                }
            },
            dismissButton = {
                TextButton(onClick = { newlyCreatedCredentials = null }) {
                    Text(strings.close)
                }
            }
        )
    }

    if (showSearchDialog) {
        UserSearchDialog(
            firebaseManager = firebaseManager,
            strings = strings,
            onDismiss = { showSearchDialog = false }
        )
    }
}

@Composable
fun UserItemCard(
    user: SchoolUser,
    currentUserId: String,
    strings: com.example.localization.AppStrings,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isPrimary = user.isPrimaryAdmin
    val isCurrent = user.id == currentUserId

    Card(
        shape = RoundedCornerShape(16.dp),
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
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !user.isActive -> Color.Gray.copy(alpha = 0.2f)
                            user.role == Role.ADMIN -> SchoolDeepNavy.copy(alpha = 0.15f)
                            user.role == Role.TEACHER -> SchoolPrimary.copy(alpha = 0.15f)
                            else -> SchoolPrimary.copy(alpha = 0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (user.role) {
                        Role.ADMIN -> Icons.Default.AdminPanelSettings
                        Role.TEACHER -> Icons.Default.School
                        Role.STUDENT -> Icons.Default.Person
                    },
                    contentDescription = null,
                    tint = if (user.isActive) {
                        if (user.role == Role.ADMIN) SchoolDeepNavy else SchoolPrimary
                    } else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isPrimary) {
                        StatusBadge(
                            text = strings.primaryAdmin,
                            color = SchoolDeepNavy
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (isCurrent) {
                        StatusBadge(
                            text = strings.you,
                            color = SchoolPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    StatusBadge(
                        text = if (user.isActive) strings.statusActive else strings.statusDisabled,
                        color = if (user.isActive) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }
                Text(
                    text = buildString {
                        append("@${user.username}")
                        if (user.recoveryEmail.isNotBlank()) {
                            append(" • ${user.recoveryEmail}")
                        } else if (user.phone.isNotEmpty()) {
                            append(" • ${user.phone}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                if (!isPrimary) {
                    IconButton(onClick = onToggleStatus) {
                        Icon(
                            imageVector = if (user.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = if (user.isActive) strings.disableUser else strings.enableUser,
                            tint = if (user.isActive) Color(0xFFD97706) else Color(0xFF16A34A)
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = strings.edit, tint = SchoolPrimary)
                }
                if (!isPrimary) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateUserDialog(
    role: Role,
    strings: com.example.localization.AppStrings,
    onDismiss: () -> Unit,
    onCreate: (SchoolUser, String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf(generateRandomPassword()) }
    var phone by remember { mutableStateOf("") }
    var recoveryEmail by remember { mutableStateOf("") }

    // Teacher permissions
    var canPublish by remember { mutableStateOf(true) }
    var canGrades by remember { mutableStateOf(true) }
    var canAnnounce by remember { mutableStateOf(true) }
    var canFinance by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (role) {
                    Role.STUDENT -> strings.addStudent
                    Role.TEACHER -> strings.addTeacher
                    Role.ADMIN -> strings.addAdmin
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(strings.fullName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(strings.username) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (role == Role.ADMIN) {
                    OutlinedTextField(
                        value = recoveryEmail,
                        onValueChange = { recoveryEmail = it },
                        label = { Text("${strings.recoveryEmail} *") },
                        placeholder = { Text("admin@example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(strings.password) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { password = generateRandomPassword() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = strings.autoGeneratePassword)
                    }
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.phone) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (role == Role.TEACHER) {
                    Text(
                        text = strings.teacherPermissions,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SchoolPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.permPublishResources, style = MaterialTheme.typography.bodySmall)
                        Switch(checked = canPublish, onCheckedChange = { canPublish = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.permEditGrades, style = MaterialTheme.typography.bodySmall)
                        Switch(checked = canGrades, onCheckedChange = { canGrades = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.permSendAnnouncements, style = MaterialTheme.typography.bodySmall)
                        Switch(checked = canAnnounce, onCheckedChange = { canAnnounce = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.permViewFinance, style = MaterialTheme.typography.bodySmall)
                        Switch(checked = canFinance, onCheckedChange = { canFinance = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                        val newUser = SchoolUser(
                            username = username.trim().lowercase(),
                            fullName = fullName.trim(),
                            role = role,
                            phone = phone.trim(),
                            recoveryEmail = recoveryEmail.trim(),
                            isPrimaryAdmin = false,
                            teacherPermissions = TeacherPermissions(
                                canPublishResources = canPublish,
                                canEditGrades = canGrades,
                                canSendAnnouncements = canAnnounce,
                                canViewFinance = canFinance
                            )
                        )
                        onCreate(newUser, password)
                    }
                },
                enabled = fullName.isNotBlank() && username.isNotBlank() && password.isNotBlank() && (role != Role.ADMIN || recoveryEmail.isNotBlank()),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun EditUserDialog(
    user: SchoolUser,
    strings: com.example.localization.AppStrings,
    onDismiss: () -> Unit,
    onSave: (SchoolUser) -> Unit
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var phone by remember { mutableStateOf(user.phone) }
    var canPublish by remember { mutableStateOf(user.teacherPermissions.canPublishResources) }
    var canGrades by remember { mutableStateOf(user.teacherPermissions.canEditGrades) }
    var canAnnounce by remember { mutableStateOf(user.teacherPermissions.canSendAnnouncements) }
    var canFinance by remember { mutableStateOf(user.teacherPermissions.canViewFinance) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editUser) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(strings.fullName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.phone) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (user.role == Role.TEACHER) {
                    Text(
                        text = strings.teacherPermissions,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SchoolPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.permPublishResources, style = MaterialTheme.typography.bodySmall)
                        Switch(checked = canPublish, onCheckedChange = { canPublish = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.permEditGrades, style = MaterialTheme.typography.bodySmall)
                        Switch(checked = canGrades, onCheckedChange = { canGrades = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.permSendAnnouncements, style = MaterialTheme.typography.bodySmall)
                        Switch(checked = canAnnounce, onCheckedChange = { canAnnounce = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.permViewFinance, style = MaterialTheme.typography.bodySmall)
                        Switch(checked = canFinance, onCheckedChange = { canFinance = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = user.copy(
                        fullName = fullName.trim(),
                        phone = phone.trim(),
                        teacherPermissions = TeacherPermissions(
                            canPublishResources = canPublish,
                            canEditGrades = canGrades,
                            canSendAnnouncements = canAnnounce,
                            canViewFinance = canFinance
                        )
                    )
                    onSave(updated)
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

private fun generateRandomPassword(): String {
    val chars = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}
