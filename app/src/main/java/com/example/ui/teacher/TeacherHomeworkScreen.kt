package com.example.ui.teacher

import android.widget.Toast
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
import com.example.data.model.Homework
import com.example.data.model.HomeworkSubmission
import com.example.data.model.Role
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.admin.DropdownSelector
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatusBadge
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import kotlinx.coroutines.launch

@Composable
fun TeacherHomeworkScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentTeacher = firebaseManager.currentUser

    val homeworkList by firebaseManager.observeHomework().collectAsState(initial = emptyList())
    val groups by firebaseManager.observeGroups().collectAsState(initial = emptyList())
    val subjects by firebaseManager.observeSubjects().collectAsState(initial = emptyList())
    val allStudents by firebaseManager.observeUsers(Role.STUDENT).collectAsState(initial = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedHomeworkForSubmissions by remember { mutableStateOf<Homework?>(null) }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.homeworkTitle,
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
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = SchoolPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(strings.createHomework) },
                modifier = Modifier.testTag("fab_create_homework")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (homeworkList.isEmpty()) {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.AssignmentLate,
                    actionText = strings.createHomework,
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(homeworkList, key = { it.id }) { hw ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = hw.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${hw.subjectName} • ⏰ ${strings.deadline}: ${hw.deadline}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SchoolPrimary
                                        )
                                    }
                                    Button(
                                        onClick = { selectedHomeworkForSubmissions = hw },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(strings.submissions)
                                    }
                                }
                                if (hw.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = hw.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateHomeworkDialog(
            strings = strings,
            groups = groups,
            subjects = subjects,
            onDismiss = { showCreateDialog = false },
            onCreate = { newHw ->
                scope.launch {
                    val res = firebaseManager.createHomework(newHw.copy(
                        authorTeacherId = currentTeacher?.id ?: "",
                        authorTeacherName = currentTeacher?.fullName ?: ""
                    ))
                    res.onSuccess {
                        showCreateDialog = false
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (selectedHomeworkForSubmissions != null) {
        SubmissionsBottomSheet(
            homework = selectedHomeworkForSubmissions!!,
            firebaseManager = firebaseManager,
            strings = strings,
            allStudents = allStudents,
            onDismiss = { selectedHomeworkForSubmissions = null }
        )
    }
}

@Composable
fun CreateHomeworkDialog(
    strings: com.example.localization.AppStrings,
    groups: List<com.example.data.model.SchoolGroup>,
    subjects: List<com.example.data.model.Subject>,
    onDismiss: () -> Unit,
    onCreate: (Homework) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("Tomorrow 18:00") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var selectedGroup by remember { mutableStateOf(groups.firstOrNull()) }
    var attachmentUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.createHomework) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Consignes") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text(strings.deadline) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                    value = attachmentUrl,
                    onValueChange = { attachmentUrl = it },
                    label = { Text("Attachment Link / File URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedSubject != null && selectedGroup != null) {
                        val hw = Homework(
                            title = title.trim(),
                            description = description.trim(),
                            deadline = deadline.trim(),
                            subjectId = selectedSubject!!.id,
                            subjectName = selectedSubject!!.name,
                            groupId = selectedGroup!!.id,
                            attachmentUrl = attachmentUrl.trim()
                        )
                        onCreate(hw)
                    }
                },
                enabled = title.isNotBlank() && selectedSubject != null && selectedGroup != null
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
fun SubmissionsBottomSheet(
    homework: Homework,
    firebaseManager: FirebaseManager,
    strings: com.example.localization.AppStrings,
    allStudents: List<com.example.data.model.SchoolUser>,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val submissions by firebaseManager.observeSubmissions(homework.id).collectAsState(initial = emptyList())
    var submissionToGrade by remember { mutableStateOf<HomeworkSubmission?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "${strings.submissions}: ${homework.title}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Submitted: ${submissions.size} / ${allStudents.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = SchoolSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (submissions.isEmpty()) {
                EmptyStateView(
                    message = strings.notSubmittedStatus,
                    icon = Icons.Default.Inbox
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(submissions, key = { it.id }) { sub ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = sub.studentName, fontWeight = FontWeight.Bold)
                                        Text(text = sub.submissionText, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (sub.score != null) {
                                        StatusBadge(text = "${sub.score} / 20", color = Color(0xFF16A34A))
                                    } else {
                                        Button(
                                            onClick = { submissionToGrade = sub },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(strings.gradeSubmission)
                                        }
                                    }
                                }
                                if (sub.feedback.isNotEmpty()) {
                                    Text(
                                        text = "${strings.teacherComment}: ${sub.feedback}",
                                        style = MaterialTheme.typography.bodySmall,
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

    if (submissionToGrade != null) {
        var scoreStr by remember { mutableStateOf("15") }
        var feedbackStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { submissionToGrade = null },
            title = { Text(strings.gradeSubmission) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Student: ${submissionToGrade!!.studentName}")
                    OutlinedTextField(
                        value = scoreStr,
                        onValueChange = { scoreStr = it },
                        label = { Text("${strings.score} (0-20)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = feedbackStr,
                        onValueChange = { feedbackStr = it },
                        label = { Text(strings.teacherComment) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val score = scoreStr.toFloatOrNull() ?: 0f
                        scope.launch {
                            firebaseManager.gradeSubmission(submissionToGrade!!.id, score, feedbackStr.trim())
                            submissionToGrade = null
                        }
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { submissionToGrade = null }) { Text(strings.cancel) }
            }
        )
    }
}
