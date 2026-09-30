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
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary

@Composable
fun FirebaseSetupScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isConnected = firebaseManager.isFirebaseInitialized

    fun copyToClipboard(label: String, text: String) {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.setupInstructions,
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
            // Live Status Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
                        Text(text = strings.firebaseStatus, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        val activeProjectId = try {
                            com.google.firebase.FirebaseApp.getInstance().options.projectId
                        } catch (e: Exception) {
                            "testapp-3cd3b"
                        }
                        Text(
                            text = if (isConnected) "${strings.connected} (Project: $activeProjectId)" else strings.disconnected,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConnected) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    }
                    StatusBadge(
                        text = if (isConnected) "ONLINE" else "SETUP NEEDED",
                        color = if (isConnected) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }
            }

            // Step 1: Connect Firebase
            SetupStepCard(
                step = "1",
                title = "Connect Your Real Firebase Project",
                instructions = """
1. Open the Firebase Console (console.firebase.google.com).
2. Create or select your project.
3. Enable Authentication:
   - Go to Build > Authentication > Sign-in method.
   - Enable 'Email/Password' provider.
4. Enable Cloud Firestore:
   - Go to Build > Firestore Database > Create Database.
5. Enable Cloud Storage:
   - Go to Build > Storage > Get Started.
6. Register Android App:
   - Package name: com.aistudio.scienceest.kmvyza
   - Download 'google-services.json' and place it in the '/app/' folder of this project!
                """.trimIndent(),
                onCopy = { copyToClipboard("Step 1 Instructions", it) }
            )

            // Step 2: Auto-create Admin Account
            SetupStepCard(
                step = "2",
                title = "Auto-Create Admin Account (admin / admin)",
                instructions = """
A setup script 'setup_admin.js' is provided in the root folder.
You can run it from any terminal with Node.js:
$ node setup_admin.js

OR simply tap the 'Initialize Admin Account' button inside the app on the Login screen or Settings screen!
It creates the admin account in Firebase Authentication with:
- Username: admin
- Password: admin
- Email: admin@school.app
- Role: ADMIN
                """.trimIndent(),
                onCopy = { copyToClipboard("Step 2 Instructions", it) }
            )

            // Step 3: Deploy Security Rules
            SetupStepCard(
                step = "3",
                title = "Deploy Security Rules",
                instructions = """
In Firebase Console > Firestore Database > Rules, paste the complete rules provided in 'FIREBASE_RULES.txt' or from the Settings menu in this app.

This enforces:
- Only Admins can manage users, classes, and rooms.
- Teachers can only mark attendance, publish resources, and grade for their groups.
- Students can only view their own groups, grades, and attendance.
- Private and group chat permissions strictly restricted to members.
- Admin moderation permissions across all chats.
                """.trimIndent(),
                onCopy = { copyToClipboard("Step 3 Instructions", it) }
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
                Text("Copy Instructions")
            }
        }
    }
}
