package com.example.ui.setup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppButton
import com.example.ui.common.AppHeader
import com.example.ui.common.StatusBadge
import com.example.ui.theme.SchoolAccentRed
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseSetupScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var activeProjectId by remember { mutableStateOf(firebaseManager.getActiveProjectId()) }
    var isCustomDb by remember { mutableStateOf(firebaseManager.isUsingCustomDatabase()) }
    val isConnected = firebaseManager.isFirebaseInitialized

    var selectedTab by remember { mutableStateOf(0) } // 0: Paste JSON, 1: Manual
    var jsonText by remember { mutableStateOf("") }

    var manualProjectId by remember { mutableStateOf("") }
    var manualApiKey by remember { mutableStateOf("") }
    var manualAppId by remember { mutableStateOf("") }
    var manualStorageBucket by remember { mutableStateOf("") }

    var isConnecting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.connectNewDatabaseTitle,
                subtitle = "Firebase Cloud Integration",
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Current Live Status Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isConnected) Color(0xFF16A34A).copy(alpha = 0.12f) else Color(0xFFDC2626).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isConnected) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.firebaseStatus,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Project: $activeProjectId",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(
                            text = if (isCustomDb) "CUSTOM" else "DEFAULT",
                            color = if (isCustomDb) SchoolPrimary else Color(0xFF64748B)
                        )
                    }

                    if (isCustomDb) {
                        OutlinedButton(
                            onClick = {
                                isConnecting = true
                                scope.launch {
                                    val res = firebaseManager.resetDatabaseToDefaultConfig()
                                    isConnecting = false
                                    res.onSuccess {
                                        activeProjectId = firebaseManager.getActiveProjectId()
                                        isCustomDb = firebaseManager.isUsingCustomDatabase()
                                        statusMessage = "تمت العودة إلى قاعدة البيانات الافتراضية"
                                        isError = false
                                    }.onFailure { err ->
                                        statusMessage = err.localizedMessage ?: err.message
                                        isError = true
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.resetToDefaultDatabase, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 2. Connect New Database Form Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SchoolPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = SchoolPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.connectNewDatabaseTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SchoolPrimary
                            )
                            Text(
                                text = strings.connectNewDatabaseDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tabs: Paste JSON vs Manual Entry
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(strings.pasteJsonTab) },
                            icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(strings.manualEntryTab) },
                            icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    if (selectedTab == 0) {
                        // Paste JSON content
                        OutlinedTextField(
                            value = jsonText,
                            onValueChange = {
                                jsonText = it
                                statusMessage = null
                            },
                            placeholder = { Text(strings.pasteJsonPlaceholder) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("input_google_services_json"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        // Manual Entry
                        OutlinedTextField(
                            value = manualProjectId,
                            onValueChange = { manualProjectId = it; statusMessage = null },
                            label = { Text("Project ID (معرف المشروع)") },
                            placeholder = { Text("my-school-12345") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = manualApiKey,
                            onValueChange = { manualApiKey = it; statusMessage = null },
                            label = { Text("API Key (مفتاح الـ API)") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = manualAppId,
                            onValueChange = { manualAppId = it; statusMessage = null },
                            label = { Text("App ID / Mobile SDK App ID") },
                            placeholder = { Text("1:123456789:android:abc123def456") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = manualStorageBucket,
                            onValueChange = { manualStorageBucket = it },
                            label = { Text("Storage Bucket (اختياري)") },
                            placeholder = { Text("my-school-12345.firebasestorage.app") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (statusMessage != null) {
                        Surface(
                            color = if (isError) MaterialTheme.colorScheme.errorContainer else Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isError) MaterialTheme.colorScheme.error else Color(0xFF16A34A)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = statusMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF15803D),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (!isError) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = onBack,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تسجيل الدخول بقاعدة البيانات الجديدة (admin / admin)", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Apply Button
                    Button(
                        onClick = {
                            isConnecting = true
                            statusMessage = null
                            scope.launch {
                                val result = if (selectedTab == 0) {
                                    if (jsonText.isBlank()) {
                                        isConnecting = false
                                        statusMessage = "يرجى لصق محتوى ملف google-services.json"
                                        isError = true
                                        return@launch
                                    }
                                    firebaseManager.connectWithGoogleServicesJson(jsonText)
                                } else {
                                    if (manualProjectId.isBlank() || manualApiKey.isBlank() || manualAppId.isBlank()) {
                                        isConnecting = false
                                        statusMessage = "يرجى ملء جميع الحقول المطلوبة (Project ID, API Key, App ID)"
                                        isError = true
                                        return@launch
                                    }
                                    firebaseManager.connectNewDatabase(
                                        projectId = manualProjectId,
                                        apiKey = manualApiKey,
                                        appId = manualAppId,
                                        storageBucket = manualStorageBucket
                                    )
                                }
                                isConnecting = false
                                result.onSuccess {
                                    activeProjectId = firebaseManager.getActiveProjectId()
                                    isCustomDb = firebaseManager.isUsingCustomDatabase()
                                    statusMessage = "${strings.databaseConnectedSuccess} (Project: $activeProjectId)"
                                    isError = false
                                    Toast.makeText(context, strings.databaseConnectedSuccess, Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    statusMessage = "خطأ في الاتصال: ${err.localizedMessage ?: err.message}"
                                    isError = true
                                }
                            }
                        },
                        enabled = !isConnecting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_apply_new_database")
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.loading)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.applyAndConnectButton, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isCustomDb) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    firebaseManager.resetDatabaseToDefaultConfig()
                                    isCustomDb = false
                                    activeProjectId = firebaseManager.getActiveProjectId()
                                    statusMessage = "تمت العودة بنجاح إلى وضع التجربة المحلي (Offline Mode)"
                                    isError = false
                                    Toast.makeText(context, "تم التبديل لوضع التجربة بدون إنترنت", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_reset_to_offline")
                        ) {
                            Icon(Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("العودة إلى وضع التجربة المحلي (Offline Mode)")
                        }
                    }
                }
            }

            // Step 1: Create Firebase Project Instructions
            SetupStepCard(
                step = "1",
                title = "1. خطوات إنشاء وتفعيل المشروع الجديد في Firebase",
                instructions = """
1. افتح منصة Firebase Console:
   https://console.firebase.google.com
2. أنشئ مشروعاً جديداً (Create a project).
3. فعّل خدمة الدخول (Authentication):
   - انتقل إلى Build > Authentication > Sign-in method
   - فعّل خيار 'Email/Password' واحفظ.
4. فعّل قاعدة بيانات Firestore:
   - انتقل إلى Build > Firestore Database > Create Database
   - اختر وضع البدء (Test Mode).
5. فعّل التخزين (Cloud Storage):
   - انتقل إلى Build > Storage > Get Started.
                """.trimIndent(),
                onCopy = { copyToClipboard("Step 1 Instructions", it) }
            )

            // Step 2: Download & Connect
            SetupStepCard(
                step = "2",
                title = "2. استخراج ملف google-services.json أو البيانات",
                instructions = """
1. في إعدادات المشروع (Project Settings ⚙️):
2. أضف تطبيق Android بحزمة التطبيق التالية:
   Package Name: com.aistudio.scienceest.kmvyza
3. حمّل ملف google-services.json وافتحه في أي قارئ نصوص.
4. انسخ المحتوى والصقه في خانة "لصق google-services.json" أعلاه واضغط زر الحفظ!
                """.trimIndent(),
                onCopy = { copyToClipboard("Step 2 Instructions", it) }
            )

            // Step 3: Rules
            val firestoreRules = """
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
            """.trimIndent()

            SetupStepCard(
                step = "3",
                title = "3. قواعد الأمان (Firestore Rules)",
                instructions = """
انسخ القواعد التالية والصقها في Firebase Console > Firestore Database > Rules للسماح للعمليات بالعمل فورياً:

$firestoreRules
                """.trimIndent(),
                onCopy = { copyToClipboard("Firestore Rules", firestoreRules) }
            )
        }
    }
}

@Composable
fun SetupStepCard(
    step: String,
    title: String,
    instructions: String,
    onCopy: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SchoolPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = step, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = instructions,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { onCopy(instructions) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("نسخ التعليمات")
            }
        }
    }
}
