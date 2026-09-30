package com.example.ui.settings

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
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Role
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.admin.DashboardActionRow
import com.example.ui.common.AppButton
import com.example.ui.common.AppHeader
import com.example.ui.common.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onOpenFirebaseSetup: () -> Unit = {},
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = firebaseManager.currentUser

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.settingsTitle,
                subtitle = strings.appName,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                } else null
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
            // User Profile Card
            if (currentUser != null) {
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
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(SchoolPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = SchoolPrimary, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${currentUser.username} • ${currentUser.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(
                            text = currentUser.role.name,
                            color = when (currentUser.role) {
                                Role.ADMIN -> SchoolPrimary
                                Role.TEACHER -> SchoolSecondary
                                Role.STUDENT -> Color(0xFF0284C7)
                            }
                        )
                    }
                }
            }

            // Change Password Card
            var showChangePasswordDialog by remember { mutableStateOf(false) }
            var oldPasswordInput by remember { mutableStateOf("") }
            var newPasswordInput by remember { mutableStateOf("") }
            var confirmPasswordInput by remember { mutableStateOf("") }
            var changePasswordLoading by remember { mutableStateOf(false) }
            var changePasswordError by remember { mutableStateOf<String?>(null) }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.changePassword,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${currentUser?.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                oldPasswordInput = ""
                                newPasswordInput = ""
                                confirmPasswordInput = ""
                                changePasswordError = null
                                showChangePasswordDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolPrimary)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.edit)
                        }
                    }
                }
            }

            if (showChangePasswordDialog) {
                AlertDialog(
                    onDismissRequest = { showChangePasswordDialog = false },
                    title = { Text(strings.changePassword, fontWeight = FontWeight.Bold, color = SchoolDeepNavy) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = oldPasswordInput,
                                onValueChange = {
                                    oldPasswordInput = it
                                    changePasswordError = null
                                },
                                label = { Text(strings.oldPassword) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newPasswordInput,
                                onValueChange = {
                                    newPasswordInput = it
                                    changePasswordError = null
                                },
                                label = { Text(strings.newPassword) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = confirmPasswordInput,
                                onValueChange = {
                                    confirmPasswordInput = it
                                    changePasswordError = null
                                },
                                label = { Text(strings.confirmPassword) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (changePasswordError != null) {
                                Text(
                                    text = changePasswordError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newPasswordInput.length < 6) {
                                    changePasswordError = strings.passwordTooShort
                                    return@Button
                                }
                                if (newPasswordInput != confirmPasswordInput) {
                                    changePasswordError = strings.passwordsDoNotMatch
                                    return@Button
                                }
                                changePasswordLoading = true
                                scope.launch {
                                    val res = firebaseManager.changeOwnPassword(oldPasswordInput, newPasswordInput)
                                    changePasswordLoading = false
                                    res.onSuccess {
                                        showChangePasswordDialog = false
                                        Toast.makeText(context, strings.passwordChangedSuccess, Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        changePasswordError = err.localizedMessage ?: err.message ?: strings.error
                                    }
                                }
                            },
                            enabled = !changePasswordLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolPrimary)
                        ) {
                            Text(if (changePasswordLoading) strings.loading else strings.save, color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showChangePasswordDialog = false }) {
                            Text(strings.cancel)
                        }
                    }
                )
            }

            // Language Selector Section
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.selectLanguage,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = lang == currentLanguage
                        Surface(
                            onClick = {
                                onLanguageChange(lang)
                                firebaseManager.saveLanguage(lang.code)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SchoolPrimary.copy(alpha = 0.12f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang.flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SchoolPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SchoolPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Logout Button
            AppButton(
                text = strings.logout,
                onClick = onLogout,
                color = MaterialTheme.colorScheme.error,
                icon = Icons.Default.Logout,
                testTag = "btn_settings_logout"
            )
        }
    }
}
