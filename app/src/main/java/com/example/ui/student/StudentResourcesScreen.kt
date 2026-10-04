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
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import kotlinx.coroutines.launch

@Composable
fun StudentResourcesScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentStudent = firebaseManager.currentUser

    val resources by firebaseManager.observeResources(groupId = currentStudent?.groupIds?.firstOrNull()).collectAsState(initial = emptyList())
    var selectedType by remember { mutableStateOf("ALL") }

    val filteredResources = remember(resources, selectedType) {
        if (selectedType == "ALL") resources else resources.filter { it.type == selectedType }
    }

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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedType == "ALL",
                    onClick = { selectedType = "ALL" },
                    label = { Text("All (${resources.size})") }
                )
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

            if (filteredResources.isEmpty()) {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.FolderOpen
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredResources, key = { it.id }) { res ->
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
                                        text = "${res.subjectName} • ${res.type}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SchoolSecondary
                                    )
                                    if (res.authorName.isNotEmpty()) {
                                        Text(
                                            text = "Teacher: ${res.authorName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
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
