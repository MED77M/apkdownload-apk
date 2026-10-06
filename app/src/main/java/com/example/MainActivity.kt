package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.firebase.FirebaseInitializer
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Role
import com.example.data.model.SchoolUser
import com.example.data.model.TimetableSlot
import com.example.data.notification.BadgeManager
import com.example.data.notification.RealtimeNotificationObserver
import com.example.data.notification.SchoolBackgroundNotificationService
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
    data object AdminSubjects : Screen()
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
    private val pendingIntentState = mutableStateOf<Intent?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntentState.value = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingIntentState.value = intent

        FirebaseInitializer.initialize(applicationContext)
        val firebaseManager = FirebaseManager.getInstance(applicationContext)

        setContent {
            val context = LocalContext.current
            val savedLangCode = remember { firebaseManager.getSavedLanguage() }
            var currentLanguage by remember { mutableStateOf(AppLanguage.fromCode(savedLangCode)) }
            val strings = Translations.get(currentLanguage)
            val scope = rememberCoroutineScope()

            var currentUser by remember { mutableStateOf(firebaseManager.currentUser) }
            val liveUser by firebaseManager.observeUser(currentUser?.id ?: "").collectAsState(initial = currentUser)

            LaunchedEffect(liveUser) {
                if (liveUser != null && liveUser != currentUser) {
                    currentUser = liveUser
                    firebaseManager.currentUser = liveUser
                }
            }

            var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

            // Device permissions request on first launch (Microphone for voice notes + Notifications for Android 13+)
            val requiredPermissions = remember {
                val list = mutableListOf(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    list.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                list.toTypedArray()
            }

            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms[Manifest.permission.POST_NOTIFICATIONS] == true
                } else true

                if (notifGranted) {
                    currentUser?.let { user ->
                        RealtimeNotificationObserver.start(context.applicationContext, user)
                    }
                }
            }

            // Immediately ask permissions the first time the app is opened on the phone
            LaunchedEffect(Unit) {
                val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true

                if (!audioGranted || !notifGranted) {
                    permissionsLauncher.launch(requiredPermissions)
                }
            }

            // Check auto login on launch
            LaunchedEffect(Unit) {
                firebaseManager.setBrandNewResetDone()
                val autoUser = firebaseManager.checkAutoLogin()
                if (autoUser != null) {
                    currentUser = autoUser
                    currentScreen = Screen.Main(0)
                } else {
                    currentScreen = Screen.Login
                }
            }

            // Start Realtime Notification Observer and Background Service when user is logged in
            LaunchedEffect(currentUser?.id) {
                currentUser?.let { user ->
                    RealtimeNotificationObserver.start(context.applicationContext, user)
                    SchoolBackgroundNotificationService.startService(context.applicationContext)
                }
            }

            // Handle incoming notification intent routing (e.g. tapping on a notification)
            val currentIntent by pendingIntentState
            LaunchedEffect(currentIntent) {
                val target = currentIntent ?: return@LaunchedEffect
                val route = target.getStringExtra("route")
                if (route == "announcements") {
                    BadgeManager.clearAnnouncementsBadge()
                    currentScreen = Screen.Announcements
                    pendingIntentState.value = null
                } else if (route == "chat") {
                    BadgeManager.clearMessagesBadge()
                    val convId = target.getStringExtra("conversationId") ?: ""
                    val title = target.getStringExtra("conversationTitle") ?: "محادثة"
                    val isGroup = target.getBooleanExtra("isGroup", false)
                    if (convId.isNotBlank()) {
                        currentScreen = Screen.ChatConversation(convId, title, isGroup)
                        pendingIntentState.value = null
                    }
                }
            }

            fun logout() {
                SchoolBackgroundNotificationService.stopService(context.applicationContext)
                RealtimeNotificationObserver.stop()
                BadgeManager.clearMessagesBadge()
                BadgeManager.clearAnnouncementsBadge()
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
                                    },
                                    onOpenFirebaseSetup = {
                                        currentScreen = Screen.FirebaseSetup
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
                                val hasUnreadMessages by BadgeManager.hasUnreadMessages.collectAsState()

                                LaunchedEffect(activeTab) {
                                    if (activeTab == 3) {
                                        BadgeManager.clearMessagesBadge()
                                    }
                                }

                                BackHandler(enabled = activeTab != 0) {
                                    activeTab = 0
                                }

                                @Composable
                                fun CurrentTabContent() {
                                    when (role) {
                                        Role.ADMIN -> {
                                            when (activeTab) {
                                                0 -> AdminDashboardScreen(
                                                    firebaseManager = firebaseManager,
                                                    currentLanguage = currentLanguage,
                                                    onLanguageChange = { currentLanguage = it },
                                                    onNavigateToUsers = { activeTab = 1 },
                                                    onNavigateToSubjects = { currentScreen = Screen.AdminSubjects },
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
                                                    onBack = { activeTab = 0 },
                                                    onNavigateToSubjects = { currentScreen = Screen.AdminSubjects }
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
                                                    onOpenFirebaseSetup = {
                                                        currentScreen = Screen.FirebaseSetup
                                                    },
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
                                                    onNavigateToAnnouncements = { currentScreen = Screen.Announcements },
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

                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val isTablet = maxWidth >= 720.dp

                                    if (isTablet) {
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            NavigationRail(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                contentColor = SchoolPrimary,
                                                header = {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = SchoolPrimary.copy(alpha = 0.12f),
                                                        modifier = Modifier.size(44.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = when (role) {
                                                                    Role.ADMIN -> Icons.Default.AdminPanelSettings
                                                                    Role.TEACHER -> Icons.Default.School
                                                                    Role.STUDENT -> Icons.Default.Person
                                                                },
                                                                contentDescription = null,
                                                                tint = SchoolPrimary,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                },
                                                modifier = Modifier.fillMaxHeight()
                                            ) {
                                                when (role) {
                                                    Role.ADMIN -> {
                                                        NavigationRailItem(
                                                            selected = activeTab == 0,
                                                            onClick = { activeTab = 0 },
                                                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                                            label = { Text(strings.navDashboard) },
                                                            modifier = Modifier.testTag("nav_rail_dashboard")
                                                        )
                                                        NavigationRailItem(
                                                            selected = activeTab == 1,
                                                            onClick = { activeTab = 1 },
                                                            icon = { Icon(Icons.Default.People, contentDescription = null) },
                                                            label = { Text(strings.navUsers) },
                                                            modifier = Modifier.testTag("nav_rail_users")
                                                        )
                                                        NavigationRailItem(
                                                            selected = activeTab == 2,
                                                            onClick = { activeTab = 2 },
                                                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                                                            label = { Text(strings.navTimetable) },
                                                            modifier = Modifier.testTag("nav_rail_timetable")
                                                        )
                                                        NavigationRailItem(
                                                            selected = activeTab == 3,
                                                            onClick = {
                                                                activeTab = 3
                                                                BadgeManager.clearMessagesBadge()
                                                            },
                                                            icon = {
                                                                BadgedBox(badge = {
                                                                    if (hasUnreadMessages) {
                                                                        Badge(containerColor = Color(0xFFEF4444), modifier = Modifier.size(9.dp))
                                                                    }
                                                                }) {
                                                                    Icon(Icons.Default.Chat, contentDescription = strings.navChat)
                                                                }
                                                            },
                                                            label = { Text(strings.navChat) },
                                                            modifier = Modifier.testTag("nav_rail_chat")
                                                        )
                                                        NavigationRailItem(
                                                            selected = activeTab == 4,
                                                            onClick = { activeTab = 4 },
                                                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                                            label = { Text(strings.navSettings) },
                                                            modifier = Modifier.testTag("nav_rail_settings")
                                                        )
                                                    }

                                                    Role.TEACHER -> {
                                                        NavigationRailItem(
                                                            selected = activeTab == 0,
                                                            onClick = { activeTab = 0 },
                                                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                                            label = { Text(strings.navDashboard) },
                                                            modifier = Modifier.testTag("nav_rail_dashboard")
                                                        )
                                                        NavigationRailItem(
                                                            selected = activeTab == 1,
                                                            onClick = { activeTab = 1 },
                                                            icon = { Icon(Icons.Default.ChecklistRtl, contentDescription = null) },
                                                            label = { Text(strings.navAttendance) },
                                                            modifier = Modifier.testTag("nav_rail_attendance")
                                                        )
                                                        NavigationRailItem(
                                                            selected = activeTab == 2,
                                                            onClick = { activeTab = 2 },
                                                            icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                                                            label = { Text(strings.navHomework) },
                                                            modifier = Modifier.testTag("nav_rail_homework")
                                                        )
                                                        NavigationRailItem(
                                                            selected = activeTab == 3,
                                                            onClick = {
                                                                activeTab = 3
                                                                BadgeManager.clearMessagesBadge()
                                                            },
                                                            icon = {
                                                                BadgedBox(badge = {
                                                                    if (hasUnreadMessages) {
                                                                        Badge(containerColor = Color(0xFFEF4444), modifier = Modifier.size(9.dp))
                                                                    }
                                                                }) {
                                                                    Icon(Icons.Default.Chat, contentDescription = strings.navChat)
                                                                }
                                                            },
                                                            label = { Text(strings.navChat) },
                                                            modifier = Modifier.testTag("nav_rail_chat")
                                                        )
                                                        NavigationRailItem(
                                                            selected = activeTab == 4,
                                                            onClick = { activeTab = 4 },
                                                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                                            label = { Text(strings.navSettings) },
                                                            modifier = Modifier.testTag("nav_rail_settings")
                                                        )
                                                    }

                                                    Role.STUDENT -> {
                                                        NavigationRailItem(
                                                            selected = activeTab == 0,
                                                            onClick = { activeTab = 0 },
                                                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                                            label = { Text(strings.navDashboard) },
                                                            modifier = Modifier.testTag("nav_rail_dashboard")
                                                        )
                                                        NavigationRailItem(
                                                            selected = activeTab == 1,
                                                            onClick = { activeTab = 1 },
                                                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                                                            label = { Text(strings.navTimetable) },
                                                            modifier = Modifier.testTag("nav_rail_timetable")
                                                        )
                                                        NavigationRailItem(
                                                            selected = activeTab == 2,
                                                            onClick = { activeTab = 2 },
                                                            icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                                                            label = { Text(strings.navHomework) },
                                                            modifier = Modifier.testTag("nav_rail_homework")
                                                        )
                                                        NavigationRailItem(
                                                            selected = activeTab == 3,
                                                            onClick = {
                                                                activeTab = 3
                                                                BadgeManager.clearMessagesBadge()
                                                            },
                                                            icon = {
                                                                BadgedBox(badge = {
                                                                    if (hasUnreadMessages) {
                                                                        Badge(containerColor = Color(0xFFEF4444), modifier = Modifier.size(9.dp))
                                                                    }
                                                                }) {
                                                                    Icon(Icons.Default.Chat, contentDescription = strings.navChat)
                                                                }
                                                            },
                                                            label = { Text(strings.navChat) },
                                                            modifier = Modifier.testTag("nav_rail_chat")
                                                        )
                                                        NavigationRailItem(
                                                            selected = activeTab == 4,
                                                            onClick = { activeTab = 4 },
                                                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                                            label = { Text(strings.navSettings) },
                                                            modifier = Modifier.testTag("nav_rail_settings")
                                                        )
                                                    }
                                                }
                                            }
                                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                CurrentTabContent()
                                            }
                                        }
                                    } else {
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
                                                                onClick = {
                                                                    activeTab = 3
                                                                    BadgeManager.clearMessagesBadge()
                                                                },
                                                                icon = {
                                                                    BadgedBox(
                                                                        badge = {
                                                                            if (hasUnreadMessages) {
                                                                                Badge(
                                                                                    containerColor = Color(0xFFEF4444),
                                                                                    modifier = Modifier.size(9.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                    ) {
                                                                        Icon(Icons.Default.Chat, contentDescription = strings.navChat)
                                                                    }
                                                                },
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
                                                                onClick = {
                                                                    activeTab = 3
                                                                    BadgeManager.clearMessagesBadge()
                                                                },
                                                                icon = {
                                                                    BadgedBox(
                                                                        badge = {
                                                                            if (hasUnreadMessages) {
                                                                                Badge(
                                                                                    containerColor = Color(0xFFEF4444),
                                                                                    modifier = Modifier.size(9.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                    ) {
                                                                        Icon(Icons.Default.Chat, contentDescription = strings.navChat)
                                                                    }
                                                                },
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
                                                                onClick = {
                                                                    activeTab = 3
                                                                    BadgeManager.clearMessagesBadge()
                                                                },
                                                                icon = {
                                                                    BadgedBox(
                                                                        badge = {
                                                                            if (hasUnreadMessages) {
                                                                                Badge(
                                                                                    containerColor = Color(0xFFEF4444),
                                                                                    modifier = Modifier.size(9.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                    ) {
                                                                        Icon(Icons.Default.Chat, contentDescription = strings.navChat)
                                                                    }
                                                                },
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
                                                CurrentTabContent()
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

                            is Screen.AdminSubjects -> {
                                BackHandler { currentScreen = Screen.Main(0) }
                                AdminSubjectsScreen(
                                    firebaseManager = firebaseManager,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = { currentLanguage = it },
                                    onBack = { currentScreen = Screen.Main(0) }
                                )
                            }

                            is Screen.Announcements -> {
                                LaunchedEffect(Unit) {
                                    BadgeManager.clearAnnouncementsBadge()
                                }
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
                                LaunchedEffect(Unit) {
                                    BadgeManager.clearMessagesBadge()
                                }
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
                                LaunchedEffect(Unit) {
                                    BadgeManager.clearMessagesBadge()
                                }
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
