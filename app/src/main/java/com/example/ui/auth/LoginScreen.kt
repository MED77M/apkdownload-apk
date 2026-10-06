package com.example.ui.auth

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Role
import com.example.data.model.SchoolUser
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppButton
import com.example.ui.common.LanguageSelectorChip
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLoginSuccess: (SchoolUser) -> Unit,
    onOpenFirebaseSetup: () -> Unit = {}
) {
    val strings = Translations.get(currentLanguage)
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf(firebaseManager.getLastUsername()) }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(firebaseManager.isRememberMe()) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    fun performLogin() {
        val targetUser = username.trim()
        val targetPass = password.trim()

        if (targetUser.isBlank() || targetPass.isBlank()) {
            errorMessage = strings.invalidCredentials
            return
        }

        errorMessage = null
        isLoading = true
        scope.launch {
            val result = firebaseManager.login(targetUser, targetPass)
            isLoading = false
            result.onSuccess { user ->
                firebaseManager.setRememberMe(rememberMe, targetUser)
                onLoginSuccess(user)
            }.onFailure { err ->
                errorMessage = when (err.message) {
                    "ACCOUNT_DISABLED" -> strings.accountDisabled
                    "USER_NOT_FOUND" -> strings.invalidCredentials
                    "INCORRECT_PASSWORD" -> "كلمة المرور غير صحيحة، يرجى التأكد وإعادة المحاولة"
                    "PERMISSION_DENIED" -> "خطأ في صلاحيات السحابة (PERMISSION_DENIED). يرجى مراجعة إعداد قواعد Firestore في لوحة Firebase."
                    else -> err.localizedMessage ?: err.message ?: strings.invalidCredentials
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Language selector top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageSelectorChip(
                    currentLanguage = currentLanguage,
                    onLanguageChange = onLanguageChange
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // School Logo Crest & Branding
            Surface(
                modifier = Modifier.size(112.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(2.dp, SchoolSecondary.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_school_logo),
                        contentDescription = strings.appName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.appName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = SchoolPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = strings.welcomeSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Login Card Container
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = strings.login,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Username Input (No visible email field)
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = { Text(strings.username) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = SchoolPrimary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text(strings.password) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SchoolPrimary)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { performLogin() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Remember Me and Forgot Password Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = SchoolPrimary),
                                modifier = Modifier.testTag("remember_me_checkbox")
                            )
                            Text(
                                text = strings.rememberMe,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        var showForgotDialog by remember { mutableStateOf(false) }
                        var forgotIdentifier by remember { mutableStateOf("") }
                        var forgotLoading by remember { mutableStateOf(false) }
                        var forgotMessage by remember { mutableStateOf<String?>(null) }
                        var forgotIsError by remember { mutableStateOf(false) }

                        TextButton(
                            onClick = {
                                forgotIdentifier = username
                                forgotMessage = null
                                forgotIsError = false
                                showForgotDialog = true
                            },
                            modifier = Modifier.testTag("forgot_password_button")
                        ) {
                            Text(
                                text = strings.forgotPassword,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = SchoolPrimary
                            )
                        }

                        if (showForgotDialog) {
                            AlertDialog(
                                onDismissRequest = { showForgotDialog = false },
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LockReset,
                                            contentDescription = null,
                                            tint = SchoolPrimary
                                        )
                                        Text(
                                            text = strings.forgotPasswordTitle,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SchoolDeepNavy
                                        )
                                    }
                                },
                                text = {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "أدخل اسم المستخدم للتحقق من إمكانية استعادة الحساب:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SchoolTextSecondary
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        OutlinedTextField(
                                            value = forgotIdentifier,
                                            onValueChange = {
                                                forgotIdentifier = it
                                                forgotMessage = null
                                            },
                                            label = { Text(strings.username) },
                                            placeholder = { Text("اسم المستخدم") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("forgot_identifier_input")
                                        )

                                        if (forgotMessage != null) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Surface(
                                                color = if (forgotIsError) SchoolAccentRed.copy(alpha = 0.1f) else SchoolSuccess.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = forgotMessage!!,
                                                        color = if (forgotIsError) SchoolAccentRed else SchoolSuccess,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (forgotIdentifier.isBlank()) {
                                                forgotMessage = strings.invalidCredentials
                                                forgotIsError = true
                                                return@Button
                                            }
                                            forgotLoading = true
                                            scope.launch {
                                                val res = firebaseManager.sendAdminPasswordResetEmail(forgotIdentifier)
                                                forgotLoading = false
                                                res.onSuccess { resultVal ->
                                                    forgotMessage = "${strings.resetLinkSent} ($resultVal)"
                                                    forgotIsError = false
                                                }.onFailure { err ->
                                                    forgotMessage = err.localizedMessage ?: err.message ?: strings.error
                                                    forgotIsError = true
                                                }
                                            }
                                        },
                                        enabled = !forgotLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = SchoolPrimary)
                                    ) {
                                        Text(if (forgotLoading) strings.loading else strings.sendResetLink, color = Color.White)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showForgotDialog = false }) {
                                        Text(strings.close, color = SchoolTextSecondary)
                                    }
                                }
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
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
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Login Button (Large touch target min 52dp)
                    AppButton(
                        text = if (isLoading) strings.loggingIn else strings.login,
                        onClick = { performLogin() },
                        isLoading = isLoading,
                        icon = Icons.Default.Login,
                        testTag = "login_button"
                    )
                }
            }
        }
    }
}
