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
import com.example.data.model.GradeItem
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeacherGradesScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentTeacher = firebaseManager.currentUser

    val canEditGrades = currentTeacher?.teacherPermissions?.canEditGrades ?: true

    val grades by firebaseManager.observeGrades().collectAsState(initial = emptyList())
    val students by firebaseManager.observeUsers(Role.STUDENT).collectAsState(initial = emptyList())
    val subjects by firebaseManager.observeSubjects().collectAsState(initial = emptyList())

    var showAddGradeDialog by remember { mutableStateOf(false) }
    var editingGrade by remember { mutableStateOf<GradeItem?>(null) }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.gradesTitle,
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
            if (canEditGrades) {
                ExtendedFloatingActionButton(
                    onClick = { showAddGradeDialog = true },
                    containerColor = SchoolPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(strings.addGrade) },
                    modifier = Modifier.testTag("fab_add_grade")
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
            if (!canEditGrades) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grade editing permission is restricted by administration.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (grades.isEmpty()) {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.Grading,
                    actionText = if (canEditGrades) strings.addGrade else null,
                    onActionClick = { showAddGradeDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(grades, key = { it.id }) { grade ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = grade.studentName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${grade.subjectName} • ${grade.type} • 📅 ${grade.date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (grade.comment.isNotEmpty()) {
                                        Text(
                                            text = "💬 ${grade.comment}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SchoolSecondary
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusBadge(
                                        text = "${grade.score} / ${grade.maxScore.toInt()}",
                                        color = if (grade.score >= 10f) Color(0xFF16A34A) else Color(0xFFDC2626)
                                    )
                                    if (grade.updatedAt != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "✏️ ${strings.editedBadge}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (canEditGrades) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        IconButton(
                                            onClick = { editingGrade = grade },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = strings.editGrade,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
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
    }

    if (showAddGradeDialog) {
        AddGradeDialog(
            strings = strings,
            students = students,
            subjects = subjects,
            onDismiss = { showAddGradeDialog = false },
            onAdd = { grade ->
                scope.launch {
                    val res = firebaseManager.addGrade(grade.copy(teacherId = currentTeacher?.id ?: ""))
                    res.onSuccess {
                        showAddGradeDialog = false
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    editingGrade?.let { grade ->
        EditGradeDialog(
            grade = grade,
            strings = strings,
            onDismiss = { editingGrade = null },
            onSave = { updatedGrade ->
                scope.launch {
                    val res = firebaseManager.updateGradeItem(updatedGrade)
                    if (res.isSuccess) {
                        Toast.makeText(context, strings.save, Toast.LENGTH_SHORT).show()
                        editingGrade = null
                    } else {
                        Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun EditGradeDialog(
    grade: GradeItem,
    strings: com.example.localization.AppStrings,
    onDismiss: () -> Unit,
    onSave: (GradeItem) -> Unit
) {
    var scoreStr by remember { mutableStateOf(grade.score.toString()) }
    var maxScoreStr by remember { mutableStateOf(grade.maxScore.toInt().toString()) }
    var comment by remember { mutableStateOf(grade.comment) }
    var selectedType by remember { mutableStateOf(grade.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editGrade) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${grade.studentName} • ${grade.subjectName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedType == "EXAM",
                        onClick = { selectedType = "EXAM" },
                        label = { Text(strings.typeExam) }
                    )
                    FilterChip(
                        selected = selectedType == "QUIZ",
                        onClick = { selectedType = "QUIZ" },
                        label = { Text(strings.typeQuiz) }
                    )
                    FilterChip(
                        selected = selectedType == "HOMEWORK",
                        onClick = { selectedType = "HOMEWORK" },
                        label = { Text(strings.typeHomeworkGrade) }
                    )
                }

                OutlinedTextField(
                    value = scoreStr,
                    onValueChange = { scoreStr = it },
                    label = { Text(strings.gradeValue) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = maxScoreStr,
                    onValueChange = { maxScoreStr = it },
                    label = { Text("Max Score (e.g. 20)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Feedback / Comment") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val score = scoreStr.toFloatOrNull() ?: grade.score
                    val maxScore = maxScoreStr.toFloatOrNull() ?: grade.maxScore
                    onSave(
                        grade.copy(
                            score = score,
                            maxScore = maxScore,
                            type = selectedType,
                            comment = comment.trim()
                        )
                    )
                },
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
fun AddGradeDialog(
    strings: com.example.localization.AppStrings,
    students: List<com.example.data.model.SchoolUser>,
    subjects: List<com.example.data.model.Subject>,
    onDismiss: () -> Unit,
    onAdd: (GradeItem) -> Unit
) {
    var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var scoreStr by remember { mutableStateOf("15") }
    var selectedType by remember { mutableStateOf("EXAM") }
    var comment by remember { mutableStateOf("") }

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addGrade) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownSelector(
                    items = students,
                    selectedItem = selectedStudent,
                    label = { it.fullName },
                    onSelect = { selectedStudent = it }
                )
                DropdownSelector(
                    items = subjects,
                    selectedItem = selectedSubject,
                    label = { it.name },
                    onSelect = { selectedSubject = it }
                )

                // Type Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedType == "EXAM",
                        onClick = { selectedType = "EXAM" },
                        label = { Text(strings.typeExam) }
                    )
                    FilterChip(
                        selected = selectedType == "QUIZ",
                        onClick = { selectedType = "QUIZ" },
                        label = { Text(strings.typeQuiz) }
                    )
                    FilterChip(
                        selected = selectedType == "HOMEWORK",
                        onClick = { selectedType = "HOMEWORK" },
                        label = { Text(strings.typeHomeworkGrade) }
                    )
                }

                OutlinedTextField(
                    value = scoreStr,
                    onValueChange = { scoreStr = it },
                    label = { Text("${strings.gradeValue} (0-20)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Commentaire / Feedback") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedStudent != null && selectedSubject != null && scoreStr.isNotBlank()) {
                        val score = scoreStr.toFloatOrNull() ?: 0f
                        val gradeItem = GradeItem(
                            studentId = selectedStudent!!.id,
                            studentName = selectedStudent!!.fullName,
                            subjectId = selectedSubject!!.id,
                            subjectName = selectedSubject!!.name,
                            type = selectedType,
                            score = score,
                            maxScore = 20f,
                            comment = comment.trim(),
                            date = todayDate
                        )
                        onAdd(gradeItem)
                    }
                },
                enabled = selectedStudent != null && selectedSubject != null && scoreStr.isNotBlank()
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
