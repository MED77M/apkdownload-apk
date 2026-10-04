package com.example.data.local

import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object LocalDataStore {

    val adminUser = SchoolUser(
        id = "admin_fixed",
        username = "admin",
        fullName = "مدير المركز (Admin)",
        role = Role.ADMIN,
        phone = "0600000000",
        isActive = true,
        isPrimaryAdmin = true,
        needsPasswordChange = false,
        recoveryEmail = "admin@scienceest.ma"
    )

    // Clean, production-ready Reactive StateFlows (starts fresh without dummy/sample data)
    val usersFlow = MutableStateFlow<List<SchoolUser>>(listOf(adminUser))
    val groupsFlow = MutableStateFlow<List<SchoolGroup>>(emptyList())
    val subjectsFlow = MutableStateFlow<List<Subject>>(emptyList())
    val roomsFlow = MutableStateFlow<List<Room>>(emptyList())
    val timetableFlow = MutableStateFlow<List<TimetableSlot>>(emptyList())
    val attendanceFlow = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val resourcesFlow = MutableStateFlow<List<LearningResource>>(emptyList())
    val homeworkFlow = MutableStateFlow<List<Homework>>(emptyList())
    val submissionsFlow = MutableStateFlow<List<HomeworkSubmission>>(emptyList())
    val gradesFlow = MutableStateFlow<List<GradeItem>>(emptyList())
    val enrollmentsFlow = MutableStateFlow<List<Enrollment>>(emptyList())
    val teacherSharesFlow = MutableStateFlow<List<TeacherSubjectShare>>(emptyList())
    val paymentsFlow = MutableStateFlow<List<PaymentRecord>>(emptyList())
    val auditLogsFlow = MutableStateFlow<List<AuditLog>>(emptyList())
    val announcementsFlow = MutableStateFlow<List<Announcement>>(emptyList())
    val conversationsFlow = MutableStateFlow<List<ChatConversation>>(emptyList())
    val messagesMapFlow = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val qaPostsFlow = MutableStateFlow<List<SubjectQaPost>>(emptyList())
    val qaRepliesMapFlow = MutableStateFlow<Map<String, List<SubjectQaReply>>>(emptyMap())

    fun resetToDefaults() {
        usersFlow.value = listOf(adminUser)
        groupsFlow.value = emptyList()
        subjectsFlow.value = emptyList()
        roomsFlow.value = emptyList()
        timetableFlow.value = emptyList()
        attendanceFlow.value = emptyList()
        resourcesFlow.value = emptyList()
        homeworkFlow.value = emptyList()
        submissionsFlow.value = emptyList()
        gradesFlow.value = emptyList()
        enrollmentsFlow.value = emptyList()
        teacherSharesFlow.value = emptyList()
        paymentsFlow.value = emptyList()
        auditLogsFlow.value = emptyList()
        announcementsFlow.value = emptyList()
        conversationsFlow.value = emptyList()
        messagesMapFlow.value = emptyMap()
        qaPostsFlow.value = emptyList()
        qaRepliesMapFlow.value = emptyMap()
    }
}
