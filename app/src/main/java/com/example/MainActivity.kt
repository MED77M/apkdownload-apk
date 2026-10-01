package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseInitializer
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Role
import com.example.data.model.SchoolUser
import com.example.data.model.TimetableSlot
import com.example.data.notification.RealtimeNotificationObserver
import com.example.data.notification.SchoolNotificationManager
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.admin.*
import com.example.ui.announcements.AnnouncementsScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.SecureAccountScreen
import com.example.ui.chat.ChatConversationScreen
import com.example.ui.chat.ChatListScreen
import com.example.ui.chat.SubjectQaScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.setup.FirebaseSetupScreen
import com.example.ui.student.*
import com.example.ui.teacher.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SchoolPrimary
import kotlinx.coroutines.launch

sealed class Screen {
    data object Login : Screen()
    data object SecureAccount : Screen()
    data object FirebaseSetup : Screen()
    data class Main(val tabIndex: Int = 0) : Screen()
    data object AdminUsers : Screen()
    data object AdminTimetable : Screen()
    data object AdminFinance : Screen()
    data object AdminActivityLog : Screen()
    data object AdminReports : Screen()
    data object Announcements : Screen()
    data class TeacherAttendance(val slot: TimetableSlot? = null) : Screen()
    data object TeacherResources : Screen()
    data object TeacherHomework : Screen()
    data object TeacherGrades : Screen()
    data object TeacherFinance : Screen()
    data object StudentTimetable : Screen()
    data object StudentHomework : Screen()
    data object StudentGrades : Screen()
    data object StudentResources : Screen()
    data class ChatConversation(val convId: String, val title: String, val isGroup: Boolean) : Screen()
    data object SubjectQa : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseInitializer.initialize(applicationContext)
        val firebaseManager = FirebaseManager.getInstance(applicationContext)

        setContent {
            val context = LocalContext.current
            val savedLangCode = remember { firebaseManager.getSavedLanguage() }
            var currentLanguage by remember { mutableStateOf(AppLanguage.fromCode(savedLangCode)) }
            val strings = Translations.get(currentLanguage)
            val scope = rememberCoroutineScope()

            var currentUser by remember { mutableStateOf(firebaseManager.currentUser) }
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

            // Notification permission request for Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    currentUser?.let { user ->
                        SchoolNotificationManager.fetchAndSaveFcmToken(context, user)
                    }
                }
            }

            // Check auto login on launch or perform clean reset to brand new
            LaunchedEffect(Unit) {
                if (!firebaseManager.isBrandNewResetDone()) {
                    firebaseManager.resetDatabaseToBrandNew()
                    firebaseManager.setBrandNewResetDone()
                    currentUser = null
                    currentScreen = Screen.Login
                } else {
                    val autoUser = firebaseManager.checkAutoLogin()
                    if (autoUser != null) {
                        currentUser = autoUser
                        currentScreen = Screen.Main(0)
                    }
                }
            }

            // Start Realtime Notification Observer and request permission when user is logged in
            LaunchedEffect(currentUser) {
                currentUser?.let { user ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (!SchoolNotificationManager.hasNotificationPermission(context)) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    RealtimeNotificationObserver.start(context.applicationContext, user)
                    SchoolNotificationManager.fetchAndSaveFcmToken(context.applicationContext, user)
                }
            }

            // Handle incoming notification intent routing (e.g. tapping on a notification)
            LaunchedEffect(intent) {
                val route = intent?.getStringExtra("route")
                if (route == "announcements") {
                    currentScreen = Screen.Announcements
                } else if (route == "chat") {
                    val convId = intent?.getStringExtra("conversationId") ?: ""
                    val title = intent?.getStringExtra("conversationTitle") ?: "محادثة"
                    val isGroup = intent?.getBooleanExtra("isGroup", false) ?: false
                    if (convId.isNotBlank()) {
                        currentScreen = Screen.ChatConversation(convId, title, isGroup)
                    }
                }
            }

            fun logout() {
                RealtimeNotificationObserver.stop()
                firebaseManager.logout()
                currentUser = null
                currentScreen = Screen.Login
            }

            CompositionLocalProvider(LocalLayoutDirection provides currentLanguage.layoutDirection) {
                MyApplicationTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (val screen = currentScreen) {
                            is Screen.Login -> {
                                LoginScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onLoginSuccess = { user ->
                                        currentUser = user
                                        currentScreen = Screen.Main(0)
                                    }
                                )
                            }

                            is Screen.SecureAccount -> {
                                SecureAccountScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onSuccess = { updatedUser ->
                                        currentUser = updatedUser
                                        currentScreen = Screen.Main(0)
                                    },
                                    onCancel = {
                                        logout()
                                    }
                                )
                            }

                            is Screen.FirebaseSetup -> {
                                BackHandler {
                                    currentScreen = if (currentUser != null) Screen.Main(4) else Screen.Login
                                }
                                FirebaseSetupScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = {
                                        currentScreen = if (currentUser != null) Screen.Main(4) else Screen.Login
                                    }
                                )
                            }

                            is Screen.Main -> {
                                val role = currentUser?.role ?: Role.STUDENT
                                var activeTab by remember(screen.tabIndex) { mutableStateOf(screen.tabIndex) }

                                BackHandler(enabled = activeTab != 0) {
                                    activeTab = 0
                                }

                                Scaffold(
                                    bottomBar = {
                                        NavigationBar(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = SchoolPrimary,
                                            tonalElevation = 6.dp
                                        ) {
                                            when (role) {
                                                Role.ADMIN -> {
                                                    NavigationBarItem(
                                                        selected = activeTab == 0,
                                                        onClick = { activeTab = 0 },
                                                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                                        label = { Text(strings.navDashboard) },
                                                        modifier = Modifier.testTag("nav_tab_dashboard")
                                                    )
                                                    NavigationBarItem(
                                                        selected = activeTab == 1,
                                                        onClick = { activeTab = 1 },
                                                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                                                        label = { Text(strings.navUsers) },
                                                        modifier = Modifier.testTag("nav_tab_users")
                                                    )
                                                    NavigationBarItem(
                                                        selected = activeTab == 2,
                                                        onClick = { activeTab = 2 },
                                                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                                                        label = { Text(strings.navTimetable) },
                                                        modifier = Modifier.testTag("nav_tab_timetable")
                                                    )
                                                    NavigationBarItem(
                                                        selected = activeTab == 3,
                                                        onClick = { activeTab = 3 },
                                                        icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                                                        label = { Text(strings.navChat) },
                                                        modifier = Modifier.testTag("nav_tab_chat")
                                                    )
                                                    NavigationBarItem(
                                                        selected = activeTab == 4,
                                                        onClick = { activeTab = 4 },
                                                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                                        label = { Text(strings.navSettings) },
                                                        modifier = Modifier.testTag("nav_tab_settings")
                                                    )
                                                }

                                                Role.TEACHER -> {
                                                    NavigationBarItem(
                                                        selected = activeTab == 0,
                                                        onClick = { activeTab = 0 },
                                                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                                        label = { Text(strings.navDashboard) },
                                                        modifier = Modifier.testTag("nav_tab_dashboard")
                                                    )
                                                    NavigationBarItem(
                                                        selected = activeTab == 1,
                                                        onClick = { activeTab = 1 },
                                                        icon = { Icon(Icons.Default.ChecklistRtl, contentDescription = null) },
                                                        label = { Text(strings.navAttendance) },
                                                        modifier = Modifier.testTag("nav_tab_attendance")
                                                    )
                                                    NavigationBarItem(
                                                        selected = activeTab == 2,
                                                        onClick = { activeTab = 2 },
                                                        icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                                                        label = { Text(strings.navHomework) },
                                                        modifier = Modifier.testTag("nav_tab_homework")
                                                    )
                                                    NavigationBarItem(
                                                        selected = activeTab == 3,
                                                        onClick = { activeTab = 3 },
                                                        icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                                                        label = { Text(strings.navChat) },
                                                        modifier = Modifier.testTag("nav_tab_chat")
                                                    )
                                                    NavigationBarItem(
                                                        selected = activeTab == 4,
                                                        onClick = { activeTab = 4 },
                                                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                                        label = { Text(strings.navSettings) },
                                                        modifier = Modifier.testTag("nav_tab_settings")
                                                    )
                                                }

                                                Role.STUDENT -> {
                                                    NavigationBarItem(
                                                        selected = activeTab == 0,
                                                        onClick = { activeTab = 0 },
                                                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                                        label = { Text(strings.navDashboard) },
                                                        modifier = Modifier.testTag("nav_tab_dashboard")
                                                    )
                                                    NavigationBarItem(
                                                        selected = activeTab == 1,
                                                        onClick = { activeTab = 1 },
                                                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                                                        label = { Text(strings.navTimetable) },
                                                        modifier = Modifier.testTag("nav_tab_timetable")
                                                    )
                                                    NavigationBarItem(
                                                        selected = activeTab == 2,
                                                        onClick = { activeTab = 2 },
                                                        icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                                                        label = { Text(strings.navHomework) },
                                                        modifier = Modifier.testTag("nav_tab_homework")
                                                    )
                                                    NavigationBarItem(
                                                        selected = activeTab == 3,
                                                        onClick = { activeTab = 3 },
                                                        icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                                                        label = { Text(strings.navChat) },
                                                        modifier = Modifier.testTag("nav_tab_chat")
                                                    )
                                                    NavigationBarItem(
                                                        selected = activeTab == 4,
                                                        onClick = { activeTab = 4 },
                                                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                                        label = { Text(strings.navSettings) },
                                                        modifier = Modifier.testTag("nav_tab_settings")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                ) { innerPadding ->
                                    Box(modifier = Modifier.padding(innerPadding)) {
                                        when (role) {
                                            Role.ADMIN -> {
                                                when (activeTab) {
                                                    0 -> AdminDashboardScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onNavigateToUsers = { activeTab = 1 },
                                                        onNavigateToTimetable = { activeTab = 2 },
                                                        onNavigateToFinance = { currentScreen = Screen.AdminFinance },
                                                        onNavigateToReports = { currentScreen = Screen.AdminReports },
                                                        onNavigateToAnnouncements = { currentScreen = Screen.Announcements },
                                                        onLogout = { logout() }
                                                    )
                                                    1 -> AdminUsersScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onBack = { activeTab = 0 }
                                                    )
                                                    2 -> AdminTimetableScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onBack = { activeTab = 0 }
                                                    )
                                                    3 -> ChatListScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onOpenConversation = { id, title, isGroup ->
                                                            currentScreen = Screen.ChatConversation(id, title, isGroup)
                                                        },
                                                         onOpenSubjectQa = { currentScreen = Screen.SubjectQa }
                                                    )
                                                    4 -> SettingsScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onLogout = { logout() }
                                                    )
                                                }
                                            }

                                            Role.TEACHER -> {
                                                when (activeTab) {
                                                    0 -> TeacherDashboardScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onNavigateToAttendance = { slot ->
                                                            currentScreen = Screen.TeacherAttendance(slot)
                                                        },
                                                        onNavigateToResources = { currentScreen = Screen.TeacherResources },
                                                        onNavigateToHomework = { activeTab = 2 },
                                                        onNavigateToGrades = { currentScreen = Screen.TeacherGrades },
                                                        onNavigateToChat = { activeTab = 3 },
                                                        onNavigateToFinance = { currentScreen = Screen.TeacherFinance },
                                                        onLogout = { logout() }
                                                    )
                                                    1 -> TeacherAttendanceScreen(
                                                        firebaseManager = firebaseManager,
                                                        initialSlot = null,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onBack = { activeTab = 0 }
                                                    )
                                                    2 -> TeacherHomeworkScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onBack = { activeTab = 0 }
                                                    )
                                                    3 -> ChatListScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onOpenConversation = { id, title, isGroup ->
                                                            currentScreen = Screen.ChatConversation(id, title, isGroup)
                                                        },
                                                        onOpenSubjectQa = { currentScreen = Screen.SubjectQa }
                                                    )
                                                    4 -> SettingsScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onLogout = { logout() }
                                                    )
                                                }
                                            }

                                            Role.STUDENT -> {
                                                when (activeTab) {
                                                    0 -> StudentDashboardScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onNavigateToTimetable = { activeTab = 1 },
                                                        onNavigateToHomework = { activeTab = 2 },
                                                        onNavigateToGrades = { currentScreen = Screen.StudentGrades },
                                                        onNavigateToResources = { currentScreen = Screen.StudentResources },
                                                        onNavigateToChat = { activeTab = 3 },
                                                        onNavigateToAnnouncements = { currentScreen = Screen.Announcements },
                                                        onLogout = { logout() }
                                                    )
                                                    1 -> StudentTimetableScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onBack = { activeTab = 0 }
                                                    )
                                                    2 -> StudentHomeworkScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onBack = { activeTab = 0 }
                                                    )
                                                    3 -> ChatListScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onOpenConversation = { id, title, isGroup ->
                                                            currentScreen = Screen.ChatConversation(id, title, isGroup)
                                                        },
                                                        onOpenSubjectQa = { currentScreen = Screen.SubjectQa }
                                                    )
                                                    4 -> SettingsScreen(
                                                        firebaseManager = firebaseManager,
                                                        currentLanguage = currentLanguage,
                                                        onLanguageChange = { currentLanguage = it },
                                                        onLogout = { logout() }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            is Screen.AdminFinance -> {
                                BackHandler { currentScreen = Screen.Main(0) }
                                AdminFinanceScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.Main(0) },
                                    onNavigateToActivityLog = { currentScreen = Screen.AdminActivityLog }
                                )
                            }

                            is Screen.AdminActivityLog -> {
                                BackHandler { currentScreen = Screen.AdminFinance }
                                AdminActivityLogScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.AdminFinance }
                                )
                            }

                            is Screen.TeacherFinance -> {
                                BackHandler { currentScreen = Screen.Main(0) }
                                TeacherFinanceScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.Main(0) }
                                )
                            }

                            is Screen.AdminReports -> {
                                BackHandler { currentScreen = Screen.Main(0) }
                                AdminReportsScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.Main(0) }
                                )
                            }

                            is Screen.Announcements -> {
                                BackHandler { currentScreen = Screen.Main(0) }
                                AnnouncementsScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.Main(0) }
                                )
                            }

                            is Screen.TeacherAttendance -> {
                                BackHandler { currentScreen = Screen.Main(0) }
                                TeacherAttendanceScreen(
                                    firebaseManager = firebaseManager,
                                    initialSlot = screen.slot,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.Main(0) }
                                )
                            }

                            is Screen.TeacherResources -> {
                                BackHandler { currentScreen = Screen.Main(0) }
                                TeacherResourcesScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.Main(0) }
                                )
                            }

                            is Screen.TeacherGrades -> {
                                BackHandler { currentScreen = Screen.Main(0) }
                                TeacherGradesScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.Main(0) }
                                )
                            }

                            is Screen.StudentGrades -> {
                                BackHandler { currentScreen = Screen.Main(0) }
                                StudentGradesScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.Main(0) }
                                )
                            }

                            is Screen.StudentResources -> {
                                BackHandler { currentScreen = Screen.Main(0) }
                                StudentResourcesScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.Main(0) }
                                )
                            }

                            is Screen.ChatConversation -> {
                                BackHandler { currentScreen = Screen.Main(3) }
                                ChatConversationScreen(
                                    conversationId = screen.convId,
                                    conversationTitle = screen.title,
                                    isGroup = screen.isGroup,
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.Main(3) }
                                )
                            }

                            is Screen.SubjectQa -> {
                                BackHandler { currentScreen = Screen.Main(3) }
                                SubjectQaScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.Main(3) }
                                )
                            }

                            else -> {
                                currentScreen = Screen.Login
                            }
                        }
                    }
                }
            }
        }
    }
}
