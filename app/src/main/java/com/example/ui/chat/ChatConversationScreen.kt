package com.example.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.audio.AudioHelper
import com.example.data.file.FileDownloadHelper
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
    DisposableEffect(conversationId) {
        com.example.data.notification.RealtimeNotificationObserver.activeConversationId = conversationId
        onDispose {
            com.example.data.notification.RealtimeNotificationObserver.activeConversationId = null
            audioHelper.releaseAll()
        }
    }

    val messages by firebaseManager.observeMessages(conversationId).collectAsState(initial = emptyList())

    var inputText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatusText by remember { mutableStateOf<String?>(null) }
    var currentlyPlayingAudioUrl by remember { mutableStateOf<String?>(null) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

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
            uploadStatusText = strings.sendImage
            scope.launch {
                val uploadRes = firebaseManager.uploadFile(
                    fileUri = uri,
                    storagePath = "chat_images/${conversationId}/${System.currentTimeMillis()}.jpg"
                )
                isUploading = false
                uploadStatusText = null
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

    // Document Picker launcher (PDF, Word, TXT, etc.)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            uploadStatusText = strings.sendDocument
            scope.launch {
                var fileName = "Document_${System.currentTimeMillis()}"
                var fileSize = 0L
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) cursor.getString(nameIndex)?.let { fileName = it }
                            if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("Chat", "Document metadata query notice", e)
                }

                val mimeType = context.contentResolver.getType(uri) ?: when {
                    fileName.endsWith(".pdf", true) -> "application/pdf"
                    fileName.endsWith(".doc", true) || fileName.endsWith(".docx", true) -> "application/msword"
                    else -> "application/octet-stream"
                }

                val uploadRes = firebaseManager.uploadDocument(
                    fileUri = uri,
                    fileName = fileName,
                    storagePath = "chat_docs/${conversationId}/${System.currentTimeMillis()}_$fileName"
                )
                isUploading = false
                uploadStatusText = null
                uploadRes.onSuccess { downloadUrl ->
                    val msg = ChatMessage(
                        conversationId = conversationId,
                        senderId = currentUser.id,
                        senderName = currentUser.fullName,
                        senderRole = currentUser.role.name,
                        documentUrl = downloadUrl,
                        documentName = fileName,
                        documentSize = fileSize,
                        documentType = mimeType,
                        timestamp = System.currentTimeMillis()
                    )
                    firebaseManager.sendMessage(conversationId, msg)
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: strings.error, Toast.LENGTH_LONG).show()
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
            uploadStatusText = strings.recording
            scope.launch {
                val uploadRes = firebaseManager.uploadLocalFile(
                    file = audioFile,
                    storagePath = "chat_audio/${conversationId}/${audioFile.name}"
                )
                isUploading = false
                uploadStatusText = null
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
                        strings = strings,
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
                        onImageClick = {
                            fullScreenImageUrl = msg.imageUrl
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
                Surface(
                    color = SchoolPrimary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SchoolPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uploadStatusText ?: strings.loading,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = SchoolPrimary
                        )
                    }
                }
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
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Document Attachment Button (PDF, Word, etc.)
                    IconButton(
                        onClick = {
                            documentPickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "text/plain",
                                    "application/*",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier.size(44.dp).testTag("btn_attach_document")
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = strings.sendDocument, tint = SchoolPrimary)
                    }

                    // Photo Attachment Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(44.dp).testTag("btn_attach_image")
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

    // Full Screen Image Viewer Modal
    fullScreenImageUrl?.let { imageUrl ->
        Dialog(
            onDismissRequest = { fullScreenImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = strings.downloadImage,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Top action bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { fullScreenImageUrl = null },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = strings.close, tint = Color.White)
                    }

                    // Download image to phone button
                    Button(
                        onClick = {
                            scope.launch {
                                Toast.makeText(context, strings.downloadingFile, Toast.LENGTH_SHORT).show()
                                val res = FileDownloadHelper.downloadAndSaveToPhone(
                                    context = context,
                                    urlOrData = imageUrl,
                                    suggestedFileName = "Photo_${System.currentTimeMillis()}.jpg",
                                    mimeType = "image/jpeg"
                                )
                                res.onSuccess {
                                    Toast.makeText(context, strings.fileDownloaded, Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, "${strings.downloadFailed}: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolPrimary),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.downloadImage, color = Color.White)
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
    strings: com.example.localization.AppStrings,
    isPlaying: Boolean,
    onPlayAudio: () -> Unit,
    onImageClick: () -> Unit,
    onDeleteMessage: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

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
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {

                // 1. IMAGE MESSAGE
                if (message.imageUrl.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onImageClick() }
                        )

                        // Download button overlay on image
                        IconButton(
                            onClick = {
                                scope.launch {
                                    Toast.makeText(context, strings.downloadingFile, Toast.LENGTH_SHORT).show()
                                    val res = FileDownloadHelper.downloadAndSaveToPhone(
                                        context = context,
                                        urlOrData = message.imageUrl,
                                        suggestedFileName = "Image_${message.id.take(6)}.jpg",
                                        mimeType = "image/jpeg"
                                    )
                                    res.onSuccess {
                                        Toast.makeText(context, strings.fileDownloaded, Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "${strings.downloadFailed}: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                        ) {
                            Icon(Icons.Default.Download, contentDescription = strings.downloadFile, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 2. DOCUMENT MESSAGE (PDF, Word, etc.)
                if (message.documentUrl.isNotEmpty()) {
                    val isPdf = message.documentName.endsWith(".pdf", true) || message.documentType.contains("pdf", true)
                    Surface(
                        color = if (isMe) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = if (isPdf) Color(0xFFEF4444).copy(alpha = 0.15f) else SchoolPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Description,
                                            contentDescription = null,
                                            tint = if (isPdf) Color(0xFFDC2626) else SchoolPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.documentName.ifBlank { "Document.pdf" },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${FileDownloadHelper.formatFileSize(message.documentSize)} • ${if (isPdf) "PDF" else "FILE"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action buttons: Download & Open
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = if (isMe) Color.White else SchoolPrimary
                                    )
                                } else {
                                    // Save to phone Downloads
                                    OutlinedButton(
                                        onClick = {
                                            isDownloading = true
                                            scope.launch {
                                                Toast.makeText(context, strings.downloadingFile, Toast.LENGTH_SHORT).show()
                                                val res = FileDownloadHelper.downloadAndSaveToPhone(
                                                    context = context,
                                                    urlOrData = message.documentUrl,
                                                    suggestedFileName = message.documentName.ifBlank { "Document_${message.id.take(6)}.pdf" },
                                                    mimeType = message.documentType.ifBlank { "application/pdf" }
                                                )
                                                isDownloading = false
                                                res.onSuccess {
                                                    Toast.makeText(context, strings.fileDownloaded, Toast.LENGTH_LONG).show()
                                                }.onFailure { err ->
                                                    Toast.makeText(context, "${strings.downloadFailed}: ${err.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (isMe) Color.White else SchoolPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.downloadFile, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Open directly
                                    Button(
                                        onClick = {
                                            isDownloading = true
                                            scope.launch {
                                                val res = FileDownloadHelper.downloadAndSaveToPhone(
                                                    context = context,
                                                    urlOrData = message.documentUrl,
                                                    suggestedFileName = message.documentName.ifBlank { "Document_${message.id.take(6)}.pdf" },
                                                    mimeType = message.documentType.ifBlank { "application/pdf" }
                                                )
                                                isDownloading = false
                                                res.onSuccess { downloadedFile ->
                                                    val openRes = FileDownloadHelper.openFile(
                                                        context = context,
                                                        file = downloadedFile,
                                                        mimeType = message.documentType.ifBlank { "application/pdf" }
                                                    )
                                                    if (openRes.isFailure) {
                                                        Toast.makeText(context, strings.fileDownloaded, Toast.LENGTH_LONG).show()
                                                    }
                                                }.onFailure { err ->
                                                    Toast.makeText(context, "${strings.downloadFailed}: ${err.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isMe) Color.White else SchoolPrimary,
                                            contentColor = if (isMe) SchoolPrimary else Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.openFile, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 3. VOICE NOTE MESSAGE
                if (message.audioUrl.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = onPlayAudio,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isMe) Color.White.copy(alpha = 0.25f) else SchoolPrimary.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play voice note",
                                tint = if (isMe) Color.White else SchoolPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPlaying) "Playing voice note..." else strings.voiceNoteLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Audio .m4a",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMe) Color.White.copy(alpha = 0.75f) else Color.Gray
                            )
                        }

                        // Save voice note to phone Downloads button
                        IconButton(
                            onClick = {
                                scope.launch {
                                    Toast.makeText(context, strings.downloadingFile, Toast.LENGTH_SHORT).show()
                                    val res = FileDownloadHelper.downloadAndSaveToPhone(
                                        context = context,
                                        urlOrData = message.audioUrl,
                                        suggestedFileName = "Voice_${message.id.take(6)}.m4a",
                                        mimeType = "audio/m4a"
                                    )
                                    res.onSuccess {
                                        Toast.makeText(context, strings.fileDownloaded, Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "${strings.downloadFailed}: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isMe) Color.White.copy(alpha = 0.18f) else SchoolSecondary.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = strings.downloadFile,
                                tint = if (isMe) Color.White else SchoolPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // 4. TEXT MESSAGE
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
