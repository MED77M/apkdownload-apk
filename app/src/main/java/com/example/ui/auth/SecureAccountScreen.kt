package com.example.ui.auth

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.SchoolUser
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SecureAccountScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onSuccess: (SchoolUser) -> Unit,
    onCancel: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf(firebaseManager.currentUser?.username ?: "admin") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var recoveryEmail by remember { mutableStateOf("") }

    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Mandatory screen: BackHandler logs out to prevent bypassing security
    BackHandler {
        onCancel()
    }

    fun submitSecureAccount() {
        if (newPassword.length < 6) {
            errorMessage = strings.passwordTooShort
            return
        }
        if (newPassword.trim().lowercase() == "admin") {
            errorMessage = strings.passwordCannotBeAdmin
            return
        }
        if (newPassword != confirmPassword) {
            errorMessage = strings.passwordsDoNotMatch
            return
        }
        val trimmedEmail = recoveryEmail.trim()
        if (trimmedEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            errorMessage = strings.invalidEmail
            return
        }

        errorMessage = null
        isLoading = true
        scope.launch {
            val result = firebaseManager.secureFirstAdminAccount(
                newPassword = newPassword,
                recoveryEmail = trimmedEmail,
                newUsername = username.trim().ifBlank { "admin" }
            )
            isLoading = false
            result.onSuccess { updatedUser ->
                onSuccess(updatedUser)
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: err.message ?: strings.error
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SchoolBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Shield Icon in Navy
            Surface(
                shape = CircleShape,
                color = SchoolDeepNavy,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.secureAccountTitle,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = SchoolDeepNavy,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.secureAccountSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SchoolTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SchoolSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Optional Username Change
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = { Text(strings.changeUsernameOptional) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = SchoolPrimary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secure_username_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Recovery Email Input (Mandatory)
                    OutlinedTextField(
                        value = recoveryEmail,
                        onValueChange = {
                            recoveryEmail = it
                            errorMessage = null
                        },
                        label = { Text("${strings.recoveryEmail} *") },
                        placeholder = { Text("admin@example.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = SchoolPrimary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secure_recovery_email_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // New Password Input
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            errorMessage = null
                        },
                        label = { Text("${strings.newPassword} *") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SchoolPrimary)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { newPasswordVisible = !newPasswordVisible },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (newPasswordVisible) "Hide" else "Show"
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secure_new_password_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Confirm Password Input
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text("${strings.confirmPassword} *") },
                        leadingIcon = {
                            Icon(Icons.Default.LockReset, contentDescription = null, tint = SchoolPrimary)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Hide" else "Show"
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitSecureAccount() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secure_confirm_password_input")
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = SchoolSecondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = SchoolAccentRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = SchoolOnSecondaryContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    AppButton(
                        text = if (isLoading) strings.loading else strings.saveAndContinue,
                        onClick = { submitSecureAccount() },
                        isLoading = isLoading,
                        icon = Icons.Default.CheckCircle,
                        testTag = "secure_account_submit_button"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.logout,
                            color = SchoolTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
