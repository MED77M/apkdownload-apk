package com.example.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.audio.AudioHelper
import com.example.data.firebase.FirebaseManager
import com.example.data.model.ChatMessage
import com.example.data.model.Role
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    conversationId: String,
    conversationTitle: String,
    isGroup: Boolean,
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = firebaseManager.currentUser ?: return

    val audioHelper = remember { AudioHelper(context) }
    DisposableEffect(Unit) {
        onDispose { audioHelper.releaseAll() }
    }

    val messages by firebaseManager.observeMessages(conversationId).collectAsState(initial = emptyList())

    var inputText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    var isUploading by remember { mutableStateOf(false) }
    var currentlyPlayingAudioUrl by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo picker launcher (Zero-permission Android standard)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            scope.launch {
                val uploadRes = firebaseManager.uploadFile(
                    fileUri = uri,
                    storagePath = "chat_images/${conversationId}/${System.currentTimeMillis()}.jpg"
                )
                isUploading = false
                uploadRes.onSuccess { downloadUrl ->
                    val msg = ChatMessage(
                        conversationId = conversationId,
                        senderId = currentUser.id,
                        senderName = currentUser.fullName,
                        senderRole = currentUser.role.name,
                        imageUrl = downloadUrl,
                        timestamp = System.currentTimeMillis()
                    )
                    firebaseManager.sendMessage(conversationId, msg)
                }.onFailure {
                    Toast.makeText(context, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Audio recording permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            audioHelper.startRecording(
                onSuccess = { isRecording = true },
                onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            )
        } else {
            Toast.makeText(context, "Microphone permission is required to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleSendText() {
        if (inputText.isBlank()) return
        val textToSend = inputText.trim()
        inputText = ""
        val msg = ChatMessage(
            conversationId = conversationId,
            senderId = currentUser.id,
            senderName = currentUser.fullName,
            senderRole = currentUser.role.name,
            messageText = textToSend,
            timestamp = System.currentTimeMillis()
        )
        scope.launch {
            firebaseManager.sendMessage(conversationId, msg)
        }
    }

    fun stopAndSendAudio() {
        val audioFile = audioHelper.stopRecording()
        isRecording = false
        if (audioFile != null && audioFile.exists()) {
            isUploading = true
            scope.launch {
                val uploadRes = firebaseManager.uploadLocalFile(
                    file = audioFile,
                    storagePath = "chat_audio/${conversationId}/${audioFile.name}"
                )
                isUploading = false
                uploadRes.onSuccess { downloadUrl ->
                    val msg = ChatMessage(
                        conversationId = conversationId,
                        senderId = currentUser.id,
                        senderName = currentUser.fullName,
                        senderRole = currentUser.role.name,
                        audioUrl = downloadUrl,
                        timestamp = System.currentTimeMillis()
                    )
                    firebaseManager.sendMessage(conversationId, msg)
                }.onFailure {
                    Toast.makeText(context, "Audio upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = conversationTitle,
                subtitle = if (isGroup) "Group Chat" else "Direct Chat",
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
                .imePadding()
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == currentUser.id
                    val isAdmin = currentUser.role == Role.ADMIN

                    MessageBubble(
                        message = msg,
                        isMe = isMe,
                        isAdmin = isAdmin,
                        isPlaying = currentlyPlayingAudioUrl == msg.audioUrl && msg.audioUrl.isNotEmpty(),
                        onPlayAudio = {
                            if (currentlyPlayingAudioUrl == msg.audioUrl) {
                                audioHelper.stopPlayback()
                                currentlyPlayingAudioUrl = null
                            } else {
                                currentlyPlayingAudioUrl = msg.audioUrl
                                audioHelper.playAudio(msg.audioUrl) {
                                    currentlyPlayingAudioUrl = null
                                }
                            }
                        },
                        onDeleteMessage = {
                            scope.launch {
                                firebaseManager.deleteMessage(conversationId, msg.id)
                            }
                        }
                    )
                }
            }

            if (isUploading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = SchoolSecondary
                )
            }

            // Recording Banner if active
            if (isRecording) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.recording,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = { stopAndSendAudio() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(strings.stopAndSend)
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo Attachment Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(48.dp).testTag("btn_attach_image")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = strings.sendImage, tint = SchoolPrimary)
                    }

                    // Text Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(strings.typeMessage) },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text")
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    if (inputText.isNotBlank()) {
                        IconButton(
                            onClick = { handleSendText() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SchoolPrimary)
                                .testTag("btn_send_message")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                    } else {
                        // Voice Record Button
                        IconButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    audioHelper.startRecording(
                                        onSuccess = { isRecording = true },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                    )
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SchoolSecondary)
                                .testTag("btn_record_voice")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = strings.recordVoice, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    isAdmin: Boolean,
    isPlaying: Boolean,
    onPlayAudio: () -> Unit,
    onDeleteMessage: () -> Unit
) {
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(
                text = "${message.senderName} (${message.senderRole})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }

        Surface(
            color = if (isMe) SchoolPrimary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Image message
                if (message.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Voice note message
                if (message.audioUrl.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = onPlayAudio,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isMe) Color.White.copy(alpha = 0.2f) else SchoolPrimary.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play voice note",
                                tint = if (isMe) Color.White else SchoolPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPlaying) "Playing..." else "Voice Note",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Text message
                if (message.messageText.isNotEmpty()) {
                    Text(
                        text = message.messageText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Sent",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onDeleteMessage,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete as Admin",
                                tint = if (isMe) Color.White else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
