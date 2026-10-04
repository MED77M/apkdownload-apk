package com.example.ui.teacher

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.LearningResource
import com.example.data.model.Role
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.admin.DropdownSelector
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import kotlinx.coroutines.launch

@Composable
fun TeacherResourcesScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentTeacher = firebaseManager.currentUser

    val resources by firebaseManager.observeResources().collectAsState(initial = emptyList())
    val groups by firebaseManager.observeGroups().collectAsState(initial = emptyList())
    val subjects by firebaseManager.observeSubjects().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var editingResource by remember { mutableStateOf<LearningResource?>(null) }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.resourcesTitle,
                subtitle = strings.appName,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentTeacher?.teacherPermissions?.canPublishResources != false) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = SchoolPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                    text = { Text(strings.addResource) },
                    modifier = Modifier.testTag("fab_add_resource")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (resources.isEmpty()) {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.FolderOpen,
                    actionText = strings.addResource,
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(resources, key = { it.id }) { res ->
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
                                Box(
                                    modifier = Modifier.size(42.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (res.type) {
                                            "SUMMARY" -> Icons.Default.MenuBook
                                            "EXERCISE" -> Icons.Default.FitnessCenter
                                            else -> Icons.Default.Quiz
                                        },
                                        contentDescription = null,
                                        tint = SchoolPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = res.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${res.subjectName} • ${res.type} • ${res.level}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SchoolSecondary
                                    )
                                    if (res.authorName.isNotEmpty()) {
                                        Text(
                                            text = "By ${res.authorName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentTeacher?.role == Role.ADMIN || currentTeacher?.id == res.authorId) {
                                        IconButton(
                                            onClick = { editingResource = res },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = strings.editResource,
                                                tint = SchoolPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    if (res.fileUrl.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    Toast.makeText(context, strings.downloadingFile, Toast.LENGTH_SHORT).show()
                                                    val dlRes = com.example.data.file.FileDownloadHelper.downloadAndSaveToPhone(
                                                        context = context,
                                                        urlOrData = res.fileUrl,
                                                        suggestedFileName = "${res.title.ifBlank { "Resource" }}.pdf",
                                                        mimeType = "application/pdf"
                                                    )
                                                    dlRes.onSuccess { file ->
                                                        Toast.makeText(context, strings.fileDownloaded, Toast.LENGTH_SHORT).show()
                                                        com.example.data.file.FileDownloadHelper.openFile(context, file, "application/pdf")
                                                    }.onFailure { err ->
                                                        Toast.makeText(context, "${strings.downloadFailed}: ${err.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = strings.downloadFile, tint = SchoolPrimary)
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

    if (showAddDialog) {
        AddResourceDialog(
            strings = strings,
            groups = groups,
            subjects = subjects,
            onDismiss = { showAddDialog = false },
            onAdd = { newRes ->
                scope.launch {
                    val res = firebaseManager.addLearningResource(newRes.copy(
                        authorId = currentTeacher?.id ?: "",
                        authorName = currentTeacher?.fullName ?: ""
                    ))
                    res.onSuccess {
                        showAddDialog = false
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    editingResource?.let { res ->
        EditResourceDialog(
            resource = res,
            strings = strings,
            onDismiss = { editingResource = null },
            onSave = { updatedRes ->
                scope.launch {
                    val r = firebaseManager.updateLearningResource(updatedRes)
                    if (r.isSuccess) {
                        Toast.makeText(context, strings.save, Toast.LENGTH_SHORT).show()
                        editingResource = null
                    } else {
                        Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun EditResourceDialog(
    resource: LearningResource,
    strings: com.example.localization.AppStrings,
    onDismiss: () -> Unit,
    onSave: (LearningResource) -> Unit
) {
    var title by remember { mutableStateOf(resource.title) }
    var level by remember { mutableStateOf(resource.level) }
    var fileUrl by remember { mutableStateOf(resource.fileUrl) }
    var selectedType by remember { mutableStateOf(resource.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editResource) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedType == "SUMMARY",
                        onClick = { selectedType = "SUMMARY" },
                        label = { Text(strings.typeSummary) }
                    )
                    FilterChip(
                        selected = selectedType == "EXERCISE",
                        onClick = { selectedType = "EXERCISE" },
                        label = { Text(strings.typeExercise) }
                    )
                    FilterChip(
                        selected = selectedType == "EXAM",
                        onClick = { selectedType = "EXAM" },
                        label = { Text(strings.typePastExam) }
                    )
                }

                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it },
                    label = { Text(strings.level) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fileUrl,
                    onValueChange = { fileUrl = it },
                    label = { Text("Google Drive / PDF / File URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            resource.copy(
                                title = title.trim(),
                                type = selectedType,
                                level = level.trim(),
                                fileUrl = fileUrl.trim()
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
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
fun AddResourceDialog(
    strings: com.example.localization.AppStrings,
    groups: List<com.example.data.model.SchoolGroup>,
    subjects: List<com.example.data.model.Subject>,
    onDismiss: () -> Unit,
    onAdd: (LearningResource) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("SUMMARY") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var selectedGroup by remember { mutableStateOf(groups.firstOrNull()) }
    var fileUrl by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addResource) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.resourceTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedType == "SUMMARY",
                        onClick = { selectedType = "SUMMARY" },
                        label = { Text(strings.typeSummary) }
                    )
                    FilterChip(
                        selected = selectedType == "EXERCISE",
                        onClick = { selectedType = "EXERCISE" },
                        label = { Text(strings.typeExercise) }
                    )
                    FilterChip(
                        selected = selectedType == "PAST_EXAM",
                        onClick = { selectedType = "PAST_EXAM" },
                        label = { Text(strings.typePastExam) }
                    )
                }

                DropdownSelector(
                    items = subjects,
                    selectedItem = selectedSubject,
                    label = { it.name },
                    onSelect = { selectedSubject = it }
                )

                DropdownSelector(
                    items = groups,
                    selectedItem = selectedGroup,
                    label = { it.name },
                    onSelect = { selectedGroup = it }
                )

                OutlinedTextField(
                    value = fileUrl,
                    onValueChange = { fileUrl = it },
                    label = { Text(strings.fileOrUrl) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedSubject != null) {
                        val res = LearningResource(
                            title = title.trim(),
                            type = selectedType,
                            subjectId = selectedSubject!!.id,
                            subjectName = selectedSubject!!.name,
                            level = level.ifEmpty { selectedGroup?.level ?: "" },
                            targetGroupId = selectedGroup?.id ?: "",
                            fileUrl = fileUrl.trim()
                        )
                        onAdd(res)
                    }
                },
                enabled = title.isNotBlank() && selectedSubject != null
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
