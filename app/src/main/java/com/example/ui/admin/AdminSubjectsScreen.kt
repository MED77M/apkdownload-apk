package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Role
import com.example.data.model.SchoolUser
import com.example.data.model.Subject
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import androidx.activity.compose.BackHandler
import com.example.ui.common.EmptyStateView
import com.example.ui.theme.SchoolDeepNavy
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminSubjectsScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler { onBack() }

    val subjects by firebaseManager.observeSubjects().collectAsState(initial = emptyList())
    val users by firebaseManager.observeUsers().collectAsState(initial = emptyList())
    val enrollments by firebaseManager.observeEnrollments().collectAsState(initial = emptyList())

    val teachers = remember(users) { users.filter { it.role == Role.TEACHER } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedLevelFilter by remember { mutableStateOf("الكل") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

    // Distinct levels for filter bar
    val availableLevels = remember(subjects) {
        listOf("الكل") + subjects.map { it.level }.filter { it.isNotBlank() }.distinct()
    }

    val filteredSubjects = remember(subjects, searchQuery, selectedLevelFilter) {
        subjects.filter { subj ->
            val matchesLevel = selectedLevelFilter == "الكل" || subj.level == selectedLevelFilter
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isBlank() ||
                subj.name.lowercase().contains(q) ||
                subj.level.lowercase().contains(q) ||
                subj.teacherName.lowercase().contains(q)
            matchesLevel && matchesQuery
        }.sortedWith(compareBy({ it.name }, { it.level }))
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.subjectsPricingTitle,
                subtitle = strings.appName,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            subjectToEdit = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier.size(48.dp).testTag("btn_add_subject_header")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = strings.addSubjectLevel, tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    subjectToEdit = null
                    showAddEditDialog = true
                },
                containerColor = SchoolPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(strings.addSubjectLevel) },
                modifier = Modifier.testTag("fab_add_subject")
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
                // Search Input
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
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )

                // Level Filter Chips
                if (availableLevels.size > 1) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(availableLevels) { lvl ->
                            val isSelected = selectedLevelFilter == lvl
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLevelFilter = lvl },
                                label = { Text(lvl) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // List of Subjects
                if (filteredSubjects.isEmpty()) {
                    EmptyStateView(
                        message = strings.noDataYet,
                        icon = Icons.Default.MenuBook,
                        actionText = strings.addSubjectLevel,
                        onActionClick = {
                            subjectToEdit = null
                            showAddEditDialog = true
                        }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredSubjects, key = { it.id }) { subj ->
                            val studentCount = remember(enrollments, subj.id) {
                                enrollments.count { it.subjectId == subj.id && it.status == "active" }
                            }
                            SubjectPricingCard(
                                subject = subj,
                                studentCount = studentCount,
                                strings = strings,
                                onEdit = {
                                    subjectToEdit = subj
                                    showAddEditDialog = true
                                },
                                onDelete = { subjectToDelete = subj }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Subject Dialog
    if (showAddEditDialog) {
        AddEditSubjectDialog(
            subjectToEdit = subjectToEdit,
            existingSubjects = subjects,
            teachers = teachers,
            strings = strings,
            onDismiss = { showAddEditDialog = false },
            onSave = { name, level, price, teacher, applyToAllLevels ->
                scope.launch {
                    val code = if (subjectToEdit != null && subjectToEdit!!.code.isNotBlank()) {
                        subjectToEdit!!.code
                    } else {
                        val clean = name.trim().filter { it.isLetterOrDigit() }
                        if (clean.length >= 2) clean.take(4).uppercase() else "SUBJ_${UUID.randomUUID().toString().take(4).uppercase()}"
                    }

                    if (subjectToEdit == null) {
                        // Create New
                        val newSubj = Subject(
                            name = name.trim(),
                            code = code,
                            level = level.trim(),
                            price = price,
                            teacherId = teacher?.id ?: "",
                            teacherName = teacher?.fullName ?: ""
                        )
                        val res = firebaseManager.addSubject(newSubj)
                        val newId = res.getOrNull() ?: ""

                        if (teacher != null && applyToAllLevels) {
                            // Assign this teacher to all other levels of the same subject name
                            val sameNameSubjects = subjects.filter { it.name.equals(name.trim(), ignoreCase = true) }
                            for (s in sameNameSubjects) {
                                firebaseManager.updateSubject(s.copy(teacherId = teacher.id, teacherName = teacher.fullName))
                            }
                            val allIds = (teacher.subjectIds + sameNameSubjects.map { it.id } + newId).filter { it.isNotBlank() }.distinct()
                            firebaseManager.updateUser(teacher.copy(subjectIds = allIds))
                        } else if (teacher != null && newId.isNotBlank()) {
                            val allIds = (teacher.subjectIds + newId).distinct()
                            firebaseManager.updateUser(teacher.copy(subjectIds = allIds))
                        }
                        Toast.makeText(context, strings.success, Toast.LENGTH_SHORT).show()
                    } else {
                        // Update Existing
                        val updated = subjectToEdit!!.copy(
                            name = name.trim(),
                            code = code,
                            level = level.trim(),
                            price = price,
                            teacherId = teacher?.id ?: "",
                            teacherName = teacher?.fullName ?: ""
                        )
                        firebaseManager.updateSubject(updated)

                        if (teacher != null && applyToAllLevels) {
                            val sameNameSubjects = subjects.filter { it.name.equals(name.trim(), ignoreCase = true) && it.id != updated.id }
                            for (s in sameNameSubjects) {
                                firebaseManager.updateSubject(s.copy(teacherId = teacher.id, teacherName = teacher.fullName))
                            }
                            val allIds = (teacher.subjectIds + sameNameSubjects.map { it.id } + updated.id).filter { it.isNotBlank() }.distinct()
                            firebaseManager.updateUser(teacher.copy(subjectIds = allIds))
                        } else if (teacher != null) {
                            val allIds = (teacher.subjectIds + updated.id).distinct()
                            firebaseManager.updateUser(teacher.copy(subjectIds = allIds))
                        }
                        Toast.makeText(context, strings.success, Toast.LENGTH_SHORT).show()
                    }
                    showAddEditDialog = false
                }
            }
        )
    }

    // Delete Subject Confirmation
    if (subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            modifier = Modifier.widthIn(max = 500.dp),
            title = { Text(strings.delete) },
            text = { Text("${strings.deleteSubjectConfirm}\n(${subjectToDelete!!.fullLabelWithPrice})") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = subjectToDelete!!.id
                        subjectToDelete = null
                        scope.launch {
                            firebaseManager.deleteSubject(id)
                            Toast.makeText(context, strings.success, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun SubjectPricingCard(
    subject: Subject,
    studentCount: Int,
    strings: com.example.localization.AppStrings,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
            // Subject Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SchoolPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = SchoolPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subject.level.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SchoolSecondary.copy(alpha = 0.15f),
                            contentColor = SchoolSecondary
                        ) {
                            Text(
                                text = subject.level,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Price & Students info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Price Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (subject.price > 0) "${subject.price.toInt()} DH ${strings.perMonth}" else "مجانية",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF166534),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (studentCount > 0) {
                        Text(
                            text = "• $studentCount تلميذ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Assigned Teacher Badge
                if (subject.teacherName.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = SchoolPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${strings.assignedTeacher}: ${subject.teacherName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SchoolPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonOutline,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.noTeacherAssigned,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = strings.edit, tint = SchoolPrimary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditSubjectDialog(
    subjectToEdit: Subject?,
    existingSubjects: List<Subject>,
    teachers: List<SchoolUser>,
    strings: com.example.localization.AppStrings,
    onDismiss: () -> Unit,
    onSave: (name: String, level: String, price: Double, teacher: SchoolUser?, applyToAllLevels: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(subjectToEdit?.name ?: "") }
    var level by remember { mutableStateOf(subjectToEdit?.level ?: "") }
    var priceText by remember { mutableStateOf(if (subjectToEdit != null && subjectToEdit.price > 0) subjectToEdit.price.toInt().toString() else "400") }

    var selectedTeacher by remember {
        mutableStateOf<SchoolUser?>(teachers.firstOrNull { it.id == subjectToEdit?.teacherId })
    }
    var showTeacherDropdown by remember { mutableStateOf(false) }
    var applyToAllLevels by remember { mutableStateOf(false) }

    // Popular Subject Suggestions
    val popularSubjects = listOf(
        "الرياضيات", "الفيزياء والكيمياء", "علوم الحياة والأرض", "اللغة الفرنسية",
        "اللغة الإنجليزية", "اللغة العربية", "الفلسفة", "الإعلاميات"
    )

    // Popular Level Suggestions
    val popularLevels = listOf(
        "الجدع مشترك علمي", "الجدع مشترك آداب",
        "الأولى باكالوريا علوم تجريبية", "الأولى باكالوريا علوم رياضية", "الأولى باكالوريا آداب",
        "الثانية باكالوريا علوم فيزيائية", "الثانية باكالوريا علوم رياضية", "الثانية باكالوريا SVT",
        "الثانية باكالوريا آداب", "الثالثة إعدادي"
    )

    // Popular Price Suggestions
    val popularPrices = listOf("300", "350", "400", "500", "600")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        title = {
            Text(if (subjectToEdit == null) strings.addSubjectLevel else strings.editSubject)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Subject Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.subjectName) },
                    placeholder = { Text("مثال: الرياضيات") },
                    leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = SchoolPrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Subject suggestions
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    popularSubjects.forEach { sName ->
                        val isSelected = name.trim().equals(sName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { name = sName },
                            label = { Text(sName, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 2. Academic Level
                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it },
                    label = { Text(strings.subjectLevel) },
                    placeholder = { Text("مثال: الثانية باكالوريا علوم فيزيائية") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = SchoolSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Level suggestions
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    popularLevels.forEach { lName ->
                        val isSelected = level.trim().equals(lName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { level = lName },
                            label = { Text(lName, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 3. Price / Fee
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(strings.subjectPrice) },
                    placeholder = { Text("400") },
                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF16A34A)) },
                    trailingIcon = { Text("DH", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Price suggestions
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    popularPrices.forEach { p ->
                        val isSelected = priceText.trim() == p
                        FilterChip(
                            selected = isSelected,
                            onClick = { priceText = p },
                            label = { Text("$p DH", style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 4. Assigned Teacher Selection
                Text(
                    text = strings.assignedTeacher,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = SchoolPrimary
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showTeacherDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedTeacher?.fullName ?: strings.selectTeacher,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = showTeacherDropdown,
                        onDismissRequest = { showTeacherDropdown = false },
                        modifier = Modifier.widthIn(min = 280.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("— ${strings.noTeacherAssigned} —", color = Color.Gray) },
                            onClick = {
                                selectedTeacher = null
                                showTeacherDropdown = false
                            }
                        )
                        teachers.forEach { t ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(t.fullName, fontWeight = FontWeight.Bold)
                                        Text("@${t.username}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.School, contentDescription = null, tint = SchoolPrimary)
                                },
                                onClick = {
                                    selectedTeacher = t
                                    showTeacherDropdown = false
                                }
                            )
                        }
                    }
                }

                // Checkbox: Apply teacher to all levels of this subject
                if (selectedTeacher != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Checkbox(
                            checked = applyToAllLevels,
                            onCheckedChange = { applyToAllLevels = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.applyTeacherToAllLevels,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val parsedPrice = priceText.toDoubleOrNull() ?: 0.0
                        onSave(name, level, parsedPrice, selectedTeacher, applyToAllLevels)
                    }
                },
                enabled = name.isNotBlank(),
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
