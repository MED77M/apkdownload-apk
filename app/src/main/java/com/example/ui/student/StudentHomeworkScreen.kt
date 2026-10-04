package com.example.ui.student

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Homework
import com.example.data.model.HomeworkSubmission
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppButton
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatusBadge
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import kotlinx.coroutines.launch

@Composable
fun StudentHomeworkScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentStudent = firebaseManager.currentUser

    val homeworkList by firebaseManager.observeHomework().collectAsState(initial = emptyList())
    val mySubmissions by firebaseManager.observeSubmissions().collectAsState(initial = emptyList())

    val mySubmissionMap = remember(mySubmissions, currentStudent) {
        mySubmissions.filter { it.studentId == currentStudent?.id }.associateBy { it.homeworkId }
    }

    var selectedHomeworkToSubmit by remember { mutableStateOf<Homework?>(null) }

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
                    icon = Icons.Default.AssignmentTurnedIn
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(homeworkList, key = { it.id }) { hw ->
                        val submission = mySubmissionMap[hw.id]
                        val isSubmitted = submission != null

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
                                    StatusBadge(
                                        text = if (isSubmitted) strings.submittedStatus else strings.notSubmittedStatus,
                                        color = if (isSubmitted) Color(0xFF16A34A) else Color(0xFFD97706)
                                    )
                                }

                                if (hw.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = hw.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (hw.attachmentUrl.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                Toast.makeText(context, strings.downloadingFile, Toast.LENGTH_SHORT).show()
                                                val dlRes = com.example.data.file.FileDownloadHelper.downloadAndSaveToPhone(
                                                    context = context,
                                                    urlOrData = hw.attachmentUrl,
                                                    suggestedFileName = "${hw.title.ifBlank { "Homework" }}.pdf",
                                                    mimeType = "application/pdf"
                                                )
                                                dlRes.onSuccess { file ->
                                                    Toast.makeText(context, strings.fileDownloaded, Toast.LENGTH_SHORT).show()
                                                    com.example.data.file.FileDownloadHelper.openFile(context, file, "application/pdf")
                                                }.onFailure { err ->
                                                    Toast.makeText(context, "${strings.downloadFailed}: ${err.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.downloadFile, style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (submission != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "${strings.yourSubmission}: ${submission.submissionText}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (submission.score != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${strings.score}: ${submission.score} / 20",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF16A34A)
                                                )
                                                if (submission.feedback.isNotEmpty()) {
                                                    Text(
                                                        text = "${strings.teacherComment}: ${submission.feedback}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = SchoolPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { selectedHomeworkToSubmit = hw },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(strings.submitHomework)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedHomeworkToSubmit != null) {
        var submissionText by remember { mutableStateOf("") }
        var attachmentUrl by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedHomeworkToSubmit = null },
            title = { Text(strings.submitHomework) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = selectedHomeworkToSubmit!!.title,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = submissionText,
                        onValueChange = { submissionText = it },
                        label = { Text(strings.yourSubmission) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = attachmentUrl,
                        onValueChange = { attachmentUrl = it },
                        label = { Text("File URL / Attachment Link (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (submissionText.isNotBlank()) {
                            isSubmitting = true
                            val sub = HomeworkSubmission(
                                homeworkId = selectedHomeworkToSubmit!!.id,
                                studentId = currentStudent?.id ?: "",
                                studentName = currentStudent?.fullName ?: "",
                                submissionText = submissionText.trim(),
                                attachmentUrl = attachmentUrl.trim()
                            )
                            scope.launch {
                                val res = firebaseManager.submitHomework(sub)
                                isSubmitting = false
                                res.onSuccess {
                                    selectedHomeworkToSubmit = null
                                    Toast.makeText(context, strings.success, Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, err.message ?: strings.error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = submissionText.isNotBlank() && !isSubmitting
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedHomeworkToSubmit = null }) { Text(strings.cancel) }
            }
        )
    }
}
