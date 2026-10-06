package com.example.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Enrollment
import com.example.data.model.Role
import com.example.data.model.SchoolUser
import com.example.data.model.Subject
import com.example.data.model.TeacherPermissions
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onNavigateToSubjects: (() -> Unit)? = null
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Students, 2: Teachers, 3: Admins
    var searchQuery by remember { mutableStateOf("") }

    val allUsers by firebaseManager.observeUsers().collectAsState(initial = emptyList())
    val allSubjects by firebaseManager.observeSubjects().collectAsState(initial = emptyList())
    val allEnrollments by firebaseManager.observeEnrollments().collectAsState(initial = emptyList())

    val allTeachers = remember(allUsers) { allUsers.filter { it.role == Role.TEACHER } }

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
                    if (onNavigateToSubjects != null) {
                        IconButton(
                            onClick = onNavigateToSubjects,
                            modifier = Modifier.size(48.dp).testTag("btn_users_header_subjects")
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = strings.subjectsPricingTitle,
                                tint = Color.White
                            )
                        }
                    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 960.dp)
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
                                allSubjects = allSubjects,
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
    }

    // Add User Dialog
    if (showAddDialog) {
        CreateUserDialog(
            role = currentRole,
            strings = strings,
            availableSubjects = allSubjects,
            onDismiss = { showAddDialog = false },
            onCreate = { newUser, password, selectedSubjectIds, customSubjectNamesToAdd, onComplete ->
                scope.launch {
                    val finalSubjectIds = selectedSubjectIds.toMutableList()
                    val addedDirectNames = mutableListOf<String>()

                    for (name in customSubjectNamesToAdd) {
                        val trimmedName = name.trim()
                        if (trimmedName.isNotBlank()) {
                            val parts = trimmedName.split("|")
                            val sName = parts[0].trim()
                            val sLevel = if (parts.size > 1) parts[1].trim() else ""
                            val sPrice = if (parts.size > 2) parts[2].toDoubleOrNull() ?: 0.0 else 0.0

                            val existing = allSubjects.firstOrNull {
                                it.name.equals(sName, ignoreCase = true) &&
                                (sLevel.isBlank() || it.level.equals(sLevel, ignoreCase = true))
                            }
                            val subjId = if (existing != null) {
                                existing.id
                            } else {
                                val code = generateSubjectCode(sName)
                                val addRes = firebaseManager.addSubject(
                                    Subject(
                                        name = sName,
                                        code = code,
                                        level = sLevel,
                                        price = sPrice
                                    )
                                )
                                addedDirectNames.add(if (sLevel.isNotBlank()) "$sName ($sLevel)" else sName)
                                addRes.getOrNull() ?: ""
                            }
                            if (subjId.isNotBlank() && !finalSubjectIds.contains(subjId)) {
                                finalSubjectIds.add(subjId)
                            }
                        }
                    }

                    val userWithSubjects = newUser.copy(subjectIds = finalSubjectIds)
                    val res = firebaseManager.createUser(userWithSubjects, password)
                    res.onSuccess { createdUser ->
                        // If student has subjects, create active Enrollment records
                        if (createdUser.role == Role.STUDENT && finalSubjectIds.isNotEmpty()) {
                            val currentPeriod = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
                            val currentSubjectsList = firebaseManager.observeSubjects().firstOrNull() ?: allSubjects
                            finalSubjectIds.forEach { sId ->
                                val subjObj = currentSubjectsList.firstOrNull { it.id == sId }
                                val teacherForSubj = allTeachers.firstOrNull { it.subjectIds.contains(sId) || (subjObj != null && it.id == subjObj.teacherId) }
                                val fee = if (subjObj != null && subjObj.price > 0) subjObj.price else 400.0
                                val displayName = subjObj?.displayName ?: subjObj?.name ?: ""
                                firebaseManager.createEnrollment(
                                    Enrollment(
                                        studentId = createdUser.id,
                                        studentName = createdUser.fullName,
                                        subjectId = sId,
                                        subjectName = displayName,
                                        teacherId = subjObj?.teacherId?.takeIf { it.isNotBlank() } ?: teacherForSubj?.id ?: "",
                                        teacherName = subjObj?.teacherName?.takeIf { it.isNotBlank() } ?: teacherForSubj?.fullName ?: "",
                                        monthlyFee = fee,
                                        amountPaid = 0.0,
                                        amountRemaining = fee,
                                        status = "active",
                                        period = currentPeriod
                                    )
                                )
                            }
                        }

                        onComplete(true)
                        showAddDialog = false
                        newlyCreatedCredentials = Pair(createdUser.username, password)
                        if (addedDirectNames.isNotEmpty()) {
                            Toast.makeText(context, "${strings.subjectCreatedDirectly}: ${addedDirectNames.joinToString(", ")}", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure { err ->
                        onComplete(false)
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
            availableSubjects = allSubjects,
            onDismiss = { userToEdit = null },
            onSave = { updatedUser, newlyAddedSubjectNames, onComplete ->
                scope.launch {
                    val finalSubjectIds = updatedUser.subjectIds.toMutableList()
                    val currentPeriod = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
                    val currentSubjectsList = allSubjects.toMutableList()
                    var newlyAddedDirectName: String? = null

                    for (subName in newlyAddedSubjectNames) {
                        val trimmed = subName.trim()
                        if (trimmed.isNotBlank()) {
                            val parts = trimmed.split("|")
                            val sName = parts[0].trim()
                            val sLevel = if (parts.size > 1) parts[1].trim() else ""
                            val sPrice = if (parts.size > 2) parts[2].toDoubleOrNull() ?: 0.0 else 0.0

                            val existing = currentSubjectsList.firstOrNull {
                                it.name.equals(sName, ignoreCase = true) &&
                                (sLevel.isBlank() || it.level.equals(sLevel, ignoreCase = true))
                            }
                            val subjId = if (existing != null) {
                                existing.id
                            } else {
                                val code = generateSubjectCode(sName)
                                val addRes = firebaseManager.addSubject(
                                    Subject(
                                        name = sName,
                                        code = code,
                                        level = sLevel,
                                        price = sPrice
                                    )
                                )
                                newlyAddedDirectName = if (sLevel.isNotBlank()) "$sName ($sLevel)" else sName
                                val newId = addRes.getOrNull() ?: ""
                                if (newId.isNotBlank()) {
                                    currentSubjectsList.add(Subject(id = newId, name = sName, code = code, level = sLevel, price = sPrice))
                                }
                                newId
                            }
                            if (subjId.isNotBlank() && !finalSubjectIds.contains(subjId)) {
                                finalSubjectIds.add(subjId)
                            }
                        }
                    }

                    val userToSave = updatedUser.copy(subjectIds = finalSubjectIds)
                    firebaseManager.updateUser(userToSave)

                    // If student, synchronize enrollments (create newly added, remove deleted)
                    if (userToSave.role == Role.STUDENT) {
                        val existingEnrollments = allEnrollments.filter { it.studentId == userToSave.id }
                        
                        // 1. Add enrollments for newly assigned subjects
                        finalSubjectIds.forEach { sId ->
                            if (existingEnrollments.none { it.subjectId == sId }) {
                                val subjObj = currentSubjectsList.firstOrNull { it.id == sId }
                                val teacherForSubj = allTeachers.firstOrNull { it.subjectIds.contains(sId) || (subjObj != null && it.id == subjObj.teacherId) }
                                val fee = if (subjObj != null && subjObj.price > 0) subjObj.price else 400.0
                                val displayName = subjObj?.displayName ?: subjObj?.name ?: ""
                                firebaseManager.createEnrollment(
                                    Enrollment(
                                        studentId = userToSave.id,
                                        studentName = userToSave.fullName,
                                        subjectId = sId,
                                        subjectName = displayName,
                                        teacherId = subjObj?.teacherId?.takeIf { it.isNotBlank() } ?: teacherForSubj?.id ?: "",
                                        teacherName = subjObj?.teacherName?.takeIf { it.isNotBlank() } ?: teacherForSubj?.fullName ?: "",
                                        monthlyFee = fee,
                                        amountPaid = 0.0,
                                        amountRemaining = fee,
                                        status = "active",
                                        period = currentPeriod
                                    )
                                )
                            }
                        }

                        // 2. Remove enrollments for removed subjects
                        val removedEnrollments = existingEnrollments.filter { !finalSubjectIds.contains(it.subjectId) }
                        for (rem in removedEnrollments) {
                            firebaseManager.deleteEnrollment(rem.id)
                        }

                        // 3. For each teacher of the removed subjects, check if student still has any other active subject with that teacher
                        val remainingEnrollments = existingEnrollments.filter { finalSubjectIds.contains(it.subjectId) }
                        val remainingTeacherIds = remainingEnrollments.map { it.teacherId }.filter { it.isNotBlank() }.toSet()
                        val remainingSubjectTeacherIds = finalSubjectIds.mapNotNull { sId ->
                            val subj = currentSubjectsList.firstOrNull { it.id == sId }
                            val teacher = allTeachers.firstOrNull { it.subjectIds.contains(sId) || (subj != null && it.id == subj.teacherId) }
                            subj?.teacherId?.takeIf { it.isNotBlank() } ?: teacher?.id
                        }.toSet()
                        val allActiveTeacherIdsForStudent = remainingTeacherIds + remainingSubjectTeacherIds

                        for (rem in removedEnrollments) {
                            val removedTeacherId = rem.teacherId.takeIf { it.isNotBlank() } ?: allTeachers.firstOrNull { it.subjectIds.contains(rem.subjectId) }?.id
                            if (!removedTeacherId.isNullOrBlank() && !allActiveTeacherIdsForStudent.contains(removedTeacherId)) {
                                firebaseManager.cleanupDirectConversationBetween(userToSave.id, removedTeacherId)
                            }
                        }
                    }

                    userToEdit = null
                    onComplete(true)
                    if (newlyAddedDirectName != null) {
                        Toast.makeText(context, "${strings.subjectCreatedDirectly}: $newlyAddedDirectName", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, strings.success, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog (Permanent Deletion)
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            modifier = Modifier.widthIn(max = 520.dp),
            title = { Text(strings.deleteUser) },
            text = {
                Text(
                    "هل أنت متأكد من حذف هذا الحساب نهائياً من قاعدة البيانات وسجلات المدرسة؟\n\n(${userToDelete!!.fullName} - @${userToDelete!!.username})\n\n⚠️ سيتم حذف الحساب وسجلاته بشكل نهائي ولا يمكن التراجع عن هذا الإجراء."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uid = userToDelete!!.id
                        val name = userToDelete!!.fullName
                        userToDelete = null
                        scope.launch {
                            val res = firebaseManager.deleteUser(uid)
                            if (res.isSuccess) {
                                Toast.makeText(context, "تم حذف حساب $name نهائياً من قاعدة البيانات بنجاح", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "${strings.error}: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
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
            modifier = Modifier.widthIn(max = 520.dp),
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
    allSubjects: List<Subject>,
    strings: com.example.localization.AppStrings,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isPrimary = user.isPrimaryAdmin
    val isCurrent = user.id == currentUserId

    val userSubjects = remember(user.subjectIds, allSubjects) {
        allSubjects.filter { user.subjectIds.contains(it.id) }
    }

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

                // Display assigned / enrolled subjects
                if (user.role == Role.TEACHER && userSubjects.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = SchoolPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${strings.teacherSubject}: ${userSubjects.joinToString(", ") { it.displayName }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SchoolPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (user.role == Role.STUDENT && userSubjects.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = SchoolSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${strings.studentSubjects}: ${userSubjects.joinToString(", ") { "${it.displayName}${if (it.price > 0) " (${it.price.toInt()} DH)" else ""}" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SchoolSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateUserDialog(
    role: Role,
    strings: com.example.localization.AppStrings,
    availableSubjects: List<Subject>,
    onDismiss: () -> Unit,
    onCreate: (SchoolUser, String, List<String>, List<String>, (Boolean) -> Unit) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf(generateRandomPassword()) }
    var phone by remember { mutableStateOf("") }
    var recoveryEmail by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Teacher permissions
    var canPublish by remember { mutableStateOf(true) }
    var canGrades by remember { mutableStateOf(true) }
    var canAnnounce by remember { mutableStateOf(true) }
    var canFinance by remember { mutableStateOf(false) }

    // Teacher subjects selection (supports multiple subjects)
    val teacherSelectedSubjectIds = remember { mutableStateListOf<String>() }
    var teacherNewSubjectInput by remember { mutableStateOf("") }
    val teacherCustomNewSubjects = remember { mutableStateListOf<String>() }

    // Student subjects selection
    val studentSelectedSubjectIds = remember { mutableStateListOf<String>() }
    var studentNewSubjectInput by remember { mutableStateOf("") }
    val studentCustomNewSubjects = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
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

                // ----------------------------------------------------
                // TEACHER: Multi-Subject Selection & Direct Creation
                // ----------------------------------------------------
                if (role == Role.TEACHER) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = strings.teacherSubjectsPrompt,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SchoolPrimary
                    )

                    // Current chosen subjects for teacher
                    val allTeacherChosen = remember(teacherSelectedSubjectIds.toList(), teacherCustomNewSubjects.toList()) {
                        val fromExisting = availableSubjects.filter { teacherSelectedSubjectIds.contains(it.id) }.map {
                            it.id to (if (it.level.isNotBlank()) "${it.name} (${it.level})" else it.name)
                        }
                        val fromCustom = teacherCustomNewSubjects.map { "" to it }
                        fromExisting + fromCustom
                    }

                    if (allTeacherChosen.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allTeacherChosen.forEach { (id, name) ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        if (id.isNotBlank()) teacherSelectedSubjectIds.remove(id)
                                        else teacherCustomNewSubjects.remove(name)
                                    },
                                    label = { Text(name) },
                                    leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = strings.removeSubject,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "اختر مادة أو أكثر من القائمة أدناه، أو اكتب مادة جديدة:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Available subjects quick toggle chips
                    if (availableSubjects.isNotEmpty()) {
                        Text(
                            text = strings.selectSubject,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Helper for multi-level subjects (e.g. Mathematics across 1st Bac, 2nd Bac, etc.)
                        val distinctTeacherSubjects = remember(availableSubjects) {
                            availableSubjects.groupBy { it.name }.filter { it.value.size > 1 }
                        }
                        if (distinctTeacherSubjects.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            ) {
                                distinctTeacherSubjects.forEach { (subName, subList) ->
                                    val allSelected = subList.all { teacherSelectedSubjectIds.contains(it.id) }
                                    AssistChip(
                                        onClick = {
                                            if (allSelected) {
                                                subList.forEach { teacherSelectedSubjectIds.remove(it.id) }
                                            } else {
                                                subList.forEach { if (!teacherSelectedSubjectIds.contains(it.id)) teacherSelectedSubjectIds.add(it.id) }
                                            }
                                        },
                                        label = { Text(if (allSelected) "إلغاء كل مستويات $subName" else "تحديد جميع مستويات $subName (${subList.size})", style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = { Icon(if (allSelected) Icons.Default.CheckCircle else Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableSubjects.forEach { subj ->
                                val isSelected = teacherSelectedSubjectIds.contains(subj.id)
                                val labelText = if (subj.level.isNotBlank()) "${subj.name} (${subj.level})" else subj.name
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) teacherSelectedSubjectIds.remove(subj.id)
                                        else teacherSelectedSubjectIds.add(subj.id)
                                    },
                                    label = { Text(labelText, style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Add new subject directly to teacher and program
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = teacherNewSubjectInput,
                            onValueChange = { teacherNewSubjectInput = it },
                            label = { Text(strings.orAddNewSubject) },
                            placeholder = { Text(strings.addSubjectPrompt) },
                            leadingIcon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = SchoolPrimary) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_teacher_subject")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalIconButton(
                            onClick = {
                                val trimmed = teacherNewSubjectInput.trim()
                                if (trimmed.isNotBlank()) {
                                    val existing = availableSubjects.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
                                    if (existing != null) {
                                        if (!teacherSelectedSubjectIds.contains(existing.id)) {
                                            teacherSelectedSubjectIds.add(existing.id)
                                        }
                                    } else {
                                        if (!teacherCustomNewSubjects.contains(trimmed)) {
                                            teacherCustomNewSubjects.add(trimmed)
                                        }
                                    }
                                    teacherNewSubjectInput = ""
                                }
                            },
                            enabled = teacherNewSubjectInput.isNotBlank(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = strings.addSubject)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
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

                // ----------------------------------------------------
                // STUDENT: Subject Selection & Enrolling in Subjects
                // ----------------------------------------------------
                if (role == Role.STUDENT) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = strings.studentSubjects,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SchoolPrimary
                    )

                    // Current chosen subjects chips
                    val allChosenNames = remember(studentSelectedSubjectIds.toList(), studentCustomNewSubjects.toList()) {
                        val fromExisting = availableSubjects.filter { studentSelectedSubjectIds.contains(it.id) }.map {
                            it.id to it.fullLabelWithPrice
                        }
                        val fromCustom = studentCustomNewSubjects.map { "" to it }
                        fromExisting + fromCustom
                    }

                    if (allChosenNames.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allChosenNames.forEach { (id, name) ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        if (id.isNotBlank()) studentSelectedSubjectIds.remove(id)
                                        else studentCustomNewSubjects.remove(name)
                                    },
                                    label = { Text(name, style = MaterialTheme.typography.bodySmall) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = strings.removeSubject,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = strings.noSubjectsAvailable,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Existing subjects quick selection chips
                    if (availableSubjects.isNotEmpty()) {
                        Text(
                            text = strings.selectSubject,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableSubjects.forEach { subj ->
                                val isSelected = studentSelectedSubjectIds.contains(subj.id)
                                val chipLabel = buildString {
                                    append(subj.name)
                                    if (subj.level.isNotBlank()) append(" (${subj.level})")
                                    if (subj.price > 0) append(" • ${subj.price.toInt()} DH")
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) studentSelectedSubjectIds.remove(subj.id)
                                        else studentSelectedSubjectIds.add(subj.id)
                                    },
                                    label = { Text(chipLabel, style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Add custom subject directly
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = studentNewSubjectInput,
                            onValueChange = { studentNewSubjectInput = it },
                            label = { Text(strings.orAddNewSubject) },
                            placeholder = { Text(strings.addSubjectPrompt) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalIconButton(
                            onClick = {
                                val trimmed = studentNewSubjectInput.trim()
                                if (trimmed.isNotBlank()) {
                                    val existing = availableSubjects.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
                                    if (existing != null) {
                                        if (!studentSelectedSubjectIds.contains(existing.id)) {
                                            studentSelectedSubjectIds.add(existing.id)
                                        }
                                    } else {
                                        if (!studentCustomNewSubjects.contains(trimmed)) {
                                            studentCustomNewSubjects.add(trimmed)
                                        }
                                    }
                                    studentNewSubjectInput = ""
                                }
                            },
                            enabled = studentNewSubjectInput.isNotBlank(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = strings.addSubject)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSubmitting && fullName.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                        isSubmitting = true
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

                        val finalSubjectIds = if (role == Role.TEACHER) {
                            teacherSelectedSubjectIds.toList()
                        } else {
                            studentSelectedSubjectIds.toList()
                        }

                        val customSubjectsToAdd = if (role == Role.TEACHER) {
                            val list = teacherCustomNewSubjects.toMutableList()
                            if (teacherNewSubjectInput.isNotBlank()) list.add(teacherNewSubjectInput.trim())
                            list
                        } else {
                            val list = studentCustomNewSubjects.toMutableList()
                            if (studentNewSubjectInput.isNotBlank()) list.add(studentNewSubjectInput.trim())
                            list
                        }

                        onCreate(newUser, password, finalSubjectIds, customSubjectsToAdd) { success ->
                            if (!success) {
                                isSubmitting = false
                            }
                        }
                    }
                },
                enabled = !isSubmitting && fullName.isNotBlank() && username.isNotBlank() && password.isNotBlank() && (role != Role.ADMIN || recoveryEmail.isNotBlank()),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text(strings.cancel)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditUserDialog(
    user: SchoolUser,
    strings: com.example.localization.AppStrings,
    availableSubjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (SchoolUser, List<String>, (Boolean) -> Unit) -> Unit
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var phone by remember { mutableStateOf(user.phone) }
    var isSaving by remember { mutableStateOf(false) }

    // Teacher permissions
    var canPublish by remember { mutableStateOf(user.teacherPermissions.canPublishResources) }
    var canGrades by remember { mutableStateOf(user.teacherPermissions.canEditGrades) }
    var canAnnounce by remember { mutableStateOf(user.teacherPermissions.canSendAnnouncements) }
    var canFinance by remember { mutableStateOf(user.teacherPermissions.canViewFinance) }

    // Teacher subjects (supports multiple subjects)
    val teacherSelectedSubjectIds = remember { mutableStateListOf<String>().apply { addAll(user.subjectIds) } }
    var teacherAdditionalSubjectInput by remember { mutableStateOf("") }
    val teacherNewCustomSubjects = remember { mutableStateListOf<String>() }

    // Student subjects
    val studentSelectedSubjectIds = remember { mutableStateListOf<String>().apply { addAll(user.subjectIds) } }
    var additionalSubjectInput by remember { mutableStateOf("") }
    val studentNewCustomSubjects = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        title = { Text(strings.editUser) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
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
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.phone) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ----------------------------------------------------
                // EDIT TEACHER: Multi-Subject Management & Permissions
                // ----------------------------------------------------
                if (user.role == Role.TEACHER) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = strings.teacherSubjectsPrompt,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SchoolPrimary
                    )

                    // Display current assigned subjects
                    val allTeacherAssigned = remember(teacherSelectedSubjectIds.toList(), teacherNewCustomSubjects.toList()) {
                        val fromExisting = availableSubjects.filter { teacherSelectedSubjectIds.contains(it.id) }.map {
                            it.id to (if (it.level.isNotBlank()) "${it.name} (${it.level})" else it.name)
                        }
                        val fromCustom = teacherNewCustomSubjects.map { "" to it }
                        fromExisting + fromCustom
                    }

                    if (allTeacherAssigned.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allTeacherAssigned.forEach { (id, name) ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        if (id.isNotBlank()) teacherSelectedSubjectIds.remove(id)
                                        else teacherNewCustomSubjects.remove(name)
                                    },
                                    label = { Text(name) },
                                    leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = strings.removeSubject,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "لا توجد مواد مسندة لهذا الأستاذ حالياً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Assign another subject from remaining available subjects
                    val remainingTeacherSubjects = availableSubjects.filter { !teacherSelectedSubjectIds.contains(it.id) }
                    if (remainingTeacherSubjects.isNotEmpty()) {
                        Text(
                            text = strings.addAnotherSubjectForTeacher,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Multi-level helper for teacher
                        val distinctTeacherSubjects = remember(remainingTeacherSubjects) {
                            remainingTeacherSubjects.groupBy { it.name }.filter { it.value.size > 1 }
                        }
                        if (distinctTeacherSubjects.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            ) {
                                distinctTeacherSubjects.forEach { (subName, subList) ->
                                    AssistChip(
                                        onClick = {
                                            subList.forEach { if (!teacherSelectedSubjectIds.contains(it.id)) teacherSelectedSubjectIds.add(it.id) }
                                        },
                                        label = { Text("إسناد كل مستويات $subName (${subList.size})", style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            remainingTeacherSubjects.forEach { subj ->
                                val labelText = if (subj.level.isNotBlank()) "+ ${subj.name} (${subj.level})" else "+ ${subj.name}"
                                SuggestionChip(
                                    onClick = {
                                        teacherSelectedSubjectIds.add(subj.id)
                                    },
                                    label = { Text(labelText, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }

                    // Add new custom subject to teacher and program
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = teacherAdditionalSubjectInput,
                            onValueChange = { teacherAdditionalSubjectInput = it },
                            label = { Text(strings.orAddNewSubject) },
                            placeholder = { Text(strings.addSubjectPrompt) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalIconButton(
                            onClick = {
                                val trimmed = teacherAdditionalSubjectInput.trim()
                                if (trimmed.isNotBlank()) {
                                    val existing = availableSubjects.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
                                    if (existing != null) {
                                        if (!teacherSelectedSubjectIds.contains(existing.id)) {
                                            teacherSelectedSubjectIds.add(existing.id)
                                        }
                                    } else {
                                        if (!teacherNewCustomSubjects.contains(trimmed)) {
                                            teacherNewCustomSubjects.add(trimmed)
                                        }
                                    }
                                    teacherAdditionalSubjectInput = ""
                                }
                            },
                            enabled = teacherAdditionalSubjectInput.isNotBlank(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = strings.addSubject)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
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

                // ----------------------------------------------------
                // EDIT STUDENT: Adding / Modifying Enrolled Subjects
                // ----------------------------------------------------
                if (user.role == Role.STUDENT) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = strings.studentSubjects,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SchoolPrimary
                    )

                    // Display current enrolled subjects
                    val allChosen = remember(studentSelectedSubjectIds.toList(), studentNewCustomSubjects.toList()) {
                        val fromExisting = availableSubjects.filter { studentSelectedSubjectIds.contains(it.id) }.map {
                            it.id to it.fullLabelWithPrice
                        }
                        val fromCustom = studentNewCustomSubjects.map { "" to it }
                        fromExisting + fromCustom
                    }

                    if (allChosen.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allChosen.forEach { (id, name) ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        if (id.isNotBlank()) studentSelectedSubjectIds.remove(id)
                                        else studentNewCustomSubjects.remove(name)
                                    },
                                    label = { Text(name, style = MaterialTheme.typography.bodySmall) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = strings.removeSubject,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = strings.noEnrolledSubjects,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Section to add another subject from existing subjects
                    val remainingSubjects = availableSubjects.filter { !studentSelectedSubjectIds.contains(it.id) }
                    if (remainingSubjects.isNotEmpty()) {
                        Text(
                            text = strings.addAnotherSubject,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            remainingSubjects.forEach { subj ->
                                val chipText = buildString {
                                    append("+ ${subj.name}")
                                    if (subj.level.isNotBlank()) append(" (${subj.level})")
                                    if (subj.price > 0) append(" • ${subj.price.toInt()} DH")
                                }
                                SuggestionChip(
                                    onClick = {
                                        studentSelectedSubjectIds.add(subj.id)
                                    },
                                    label = { Text(chipText, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }

                    // Text field to type a brand new subject name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = additionalSubjectInput,
                            onValueChange = { additionalSubjectInput = it },
                            label = { Text(strings.orAddNewSubject) },
                            placeholder = { Text(strings.addSubjectPrompt) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalIconButton(
                            onClick = {
                                val trimmed = additionalSubjectInput.trim()
                                if (trimmed.isNotBlank()) {
                                    val existing = availableSubjects.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
                                    if (existing != null) {
                                        if (!studentSelectedSubjectIds.contains(existing.id)) {
                                            studentSelectedSubjectIds.add(existing.id)
                                        }
                                    } else {
                                        if (!studentNewCustomSubjects.contains(trimmed)) {
                                            studentNewCustomSubjects.add(trimmed)
                                        }
                                    }
                                    additionalSubjectInput = ""
                                }
                            },
                            enabled = additionalSubjectInput.isNotBlank(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = strings.addSubject)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSaving && fullName.isNotBlank()) {
                        isSaving = true
                        val finalSubjectIds = if (user.role == Role.TEACHER) {
                            teacherSelectedSubjectIds.toList()
                        } else {
                            studentSelectedSubjectIds.toList()
                        }

                        val updated = user.copy(
                            fullName = fullName.trim(),
                            phone = phone.trim(),
                            subjectIds = finalSubjectIds,
                            teacherPermissions = TeacherPermissions(
                                canPublishResources = canPublish,
                                canEditGrades = canGrades,
                                canSendAnnouncements = canAnnounce,
                                canViewFinance = canFinance
                            )
                        )

                        val customNames = if (user.role == Role.TEACHER) {
                            val list = teacherNewCustomSubjects.toMutableList()
                            if (teacherAdditionalSubjectInput.isNotBlank()) list.add(teacherAdditionalSubjectInput.trim())
                            list
                        } else {
                            val list = studentNewCustomSubjects.toMutableList()
                            if (additionalSubjectInput.isNotBlank()) list.add(additionalSubjectInput.trim())
                            list
                        }

                        onSave(updated, customNames) {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving && fullName.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(strings.cancel)
            }
        }
    )
}

private fun generateRandomPassword(): String {
    val chars = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}

private fun generateSubjectCode(name: String): String {
    val clean = name.trim().filter { it.isLetterOrDigit() }
    return if (clean.length >= 2) clean.take(4).uppercase() else "SUBJ_${UUID.randomUUID().toString().take(4).uppercase()}"
}
