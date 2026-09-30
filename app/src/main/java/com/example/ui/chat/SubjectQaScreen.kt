package com.example.ui.chat

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Subject
import com.example.data.model.SubjectQaPost
import com.example.data.model.SubjectQaReply
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
fun SubjectQaScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val scope = rememberCoroutineScope()
    val currentUser = firebaseManager.currentUser ?: return

    val subjects by firebaseManager.observeSubjects().collectAsState(initial = emptyList())
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }

    LaunchedEffect(subjects) {
        if (selectedSubject == null && subjects.isNotEmpty()) {
            selectedSubject = subjects.first()
        }
    }

    val posts by firebaseManager.observeQaPosts(selectedSubject?.id).collectAsState(initial = emptyList())

    var showAskDialog by remember { mutableStateOf(false) }
    var selectedPostForReplies by remember { mutableStateOf<SubjectQaPost?>(null) }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.qaTab,
                subtitle = "Subject Questions & Answers",
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
                onClick = { showAskDialog = true },
                containerColor = SchoolPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddComment, contentDescription = null) },
                text = { Text(strings.askQuestion) }
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
            // Subject Filter Bar
            if (subjects.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = subjects.indexOf(selectedSubject).coerceAtLeast(0),
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 0.dp
                ) {
                    subjects.forEach { subj ->
                        Tab(
                            selected = selectedSubject?.id == subj.id,
                            onClick = { selectedSubject = subj },
                            text = { Text(subj.name, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            if (posts.isEmpty()) {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.QuestionAnswer,
                    actionText = strings.askQuestion,
                    onActionClick = { showAskDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = post.authorName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "${post.authorRole} • ${post.subjectName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SchoolPrimary
                                        )
                                    }
                                    StatusBadge(
                                        text = "${post.repliesCount} ${strings.replies}",
                                        color = SchoolSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = post.questionText,
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { selectedPostForReplies = post },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View & Answer (${post.repliesCount})")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAskDialog && selectedSubject != null) {
        var questionText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAskDialog = false },
            title = { Text("${strings.askQuestion}: ${selectedSubject!!.name}") },
            text = {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("What is your question? (Text only)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (questionText.isNotBlank()) {
                            val newPost = SubjectQaPost(
                                subjectId = selectedSubject!!.id,
                                subjectName = selectedSubject!!.name,
                                authorId = currentUser.id,
                                authorName = currentUser.fullName,
                                authorRole = currentUser.role.name,
                                questionText = questionText.trim()
                            )
                            scope.launch {
                                firebaseManager.postQaQuestion(newPost)
                                showAskDialog = false
                            }
                        }
                    },
                    enabled = questionText.isNotBlank()
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAskDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (selectedPostForReplies != null) {
        QaRepliesSheet(
            post = selectedPostForReplies!!,
            firebaseManager = firebaseManager,
            strings = strings,
            onDismiss = { selectedPostForReplies = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaRepliesSheet(
    post: SubjectQaPost,
    firebaseManager: FirebaseManager,
    strings: com.example.localization.AppStrings,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = firebaseManager.currentUser ?: return
    val replies by firebaseManager.observeQaReplies(post.id).collectAsState(initial = emptyList())
    var replyInput by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding()
        ) {
            Text(
                text = "Question: ${post.questionText}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Asked by ${post.authorName} (${post.authorRole})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (replies.isEmpty()) {
                    item {
                        Text(
                            text = strings.noDataYet,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(replies, key = { it.id }) { r ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${r.authorName} (${r.authorRole})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SchoolPrimary
                                )
                                Text(text = r.replyText, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = replyInput,
                    onValueChange = { replyInput = it },
                    placeholder = { Text(strings.answerQuestion) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (replyInput.isNotBlank()) {
                            val reply = SubjectQaReply(
                                postId = post.id,
                                authorId = currentUser.id,
                                authorName = currentUser.fullName,
                                authorRole = currentUser.role.name,
                                replyText = replyInput.trim()
                            )
                            replyInput = ""
                            scope.launch {
                                firebaseManager.replyQaQuestion(reply)
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = SchoolPrimary)
                }
            }
        }
    }
}
