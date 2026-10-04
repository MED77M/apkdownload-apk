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

    val teacher1 = SchoolUser(
        id = "teacher_1",
        username = "mohamed",
        fullName = "أ. محمد العلمي",
        role = Role.TEACHER,
        phone = "0611223344",
        isActive = true,
        subjectIds = listOf("subj_math", "subj_phy"),
        groupIds = listOf("grp_1", "grp_2"),
        teacherPermissions = TeacherPermissions(
            canPublishResources = true,
            canEditGrades = true,
            canSendAnnouncements = true,
            canViewFinance = true
        )
    )

    val teacher2 = SchoolUser(
        id = "teacher_2",
        username = "sara",
        fullName = "أ. سارة المنصوري",
        role = Role.TEACHER,
        phone = "0622334455",
        isActive = true,
        subjectIds = listOf("subj_svt"),
        groupIds = listOf("grp_1"),
        teacherPermissions = TeacherPermissions(
            canPublishResources = true,
            canEditGrades = true,
            canSendAnnouncements = true,
            canViewFinance = false
        )
    )

    val student1 = SchoolUser(
        id = "student_1",
        username = "ahmed",
        fullName = "أحمد بن علي",
        role = Role.STUDENT,
        phone = "0633445566",
        isActive = true,
        groupIds = listOf("grp_1"),
        subjectIds = listOf("subj_math", "subj_phy", "subj_svt")
    )

    val student2 = SchoolUser(
        id = "student_2",
        username = "fatima",
        fullName = "فاطمة الزهراء الإدريسي",
        role = Role.STUDENT,
        phone = "0644556677",
        isActive = true,
        groupIds = listOf("grp_1"),
        subjectIds = listOf("subj_math", "subj_phy")
    )

    val sampleGroup1 = SchoolGroup(
        id = "grp_1",
        name = "الأولى بكالوريا - علوم تجريبية",
        level = "1Bac",
        studentIds = listOf("student_1", "student_2")
    )

    val sampleGroup2 = SchoolGroup(
        id = "grp_2",
        name = "الثانية بكالوريا - علوم رياضية",
        level = "2Bac",
        studentIds = listOf("student_1")
    )

    val sampleSubjectMath = Subject(id = "subj_math", name = "الرياضيات", code = "MATH")
    val sampleSubjectPhysics = Subject(id = "subj_phy", name = "الفيزياء والكيمياء", code = "PC")
    val sampleSubjectSvt = Subject(id = "subj_svt", name = "علوم الحياة والأرض", code = "SVT")

    val sampleRoom1 = Room(id = "room_1", name = "قاعة الخوارزمي (1)", capacity = 25)
    val sampleRoom2 = Room(id = "room_2", name = "مختبر ابن الهيثم", capacity = 20)

    val sampleSlot1 = TimetableSlot(
        id = "slot_1",
        subjectId = "subj_math",
        subjectName = "الرياضيات",
        teacherId = "teacher_1",
        teacherName = "أ. محمد العلمي",
        groupId = "grp_1",
        groupName = "الأولى بكالوريا - علوم تجريبية",
        roomId = "room_1",
        roomName = "قاعة الخوارزمي (1)",
        dayOfWeek = 1,
        startTime = "09:00",
        endTime = "11:00"
    )

    val sampleSlot2 = TimetableSlot(
        id = "slot_2",
        subjectId = "subj_phy",
        subjectName = "الفيزياء والكيمياء",
        teacherId = "teacher_1",
        teacherName = "أ. محمد العلمي",
        groupId = "grp_1",
        groupName = "الأولى بكالوريا - علوم تجريبية",
        roomId = "room_2",
        roomName = "مختبر ابن الهيثم",
        dayOfWeek = 3,
        startTime = "14:00",
        endTime = "16:00"
    )

    val sampleAnnouncement1 = Announcement(
        id = "ann_1",
        title = "مرحباً بكم في منصة Science EST التعليمية",
        body = "يسر إدارة مركز Science EST الترحيب بكافة التلاميذ والأساتذة في الموسم الدراسي الجديد. يمكنكم الاطلاع على الجداول والحصص والدروس مباشرة من التطبيق.",
        authorName = "مدير المركز",
        targetAudience = "ALL",
        createdAt = System.currentTimeMillis() - 86400000L
    )

    val sampleGrade1 = GradeItem(
        id = "grade_1",
        studentId = "student_1",
        studentName = "أحمد بن علي",
        subjectId = "subj_math",
        subjectName = "الرياضيات",
        type = "EXAM",
        score = 18.5f,
        maxScore = 20f,
        comment = "ممتاز ومتميز في التحليل الجبري",
        date = "2026-10-01",
        teacherId = "teacher_1"
    )

    val sampleGrade2 = GradeItem(
        id = "grade_2",
        studentId = "student_2",
        studentName = "فاطمة الزهراء الإدريسي",
        subjectId = "subj_phy",
        subjectName = "الفيزياء والكيمياء",
        type = "QUIZ",
        score = 17.0f,
        maxScore = 20f,
        comment = "إتقان رائع لقوانين نيوتن",
        date = "2026-10-02",
        teacherId = "teacher_1"
    )

    val sampleHomework1 = Homework(
        id = "hw_1",
        title = "تمارين نهاية المتتاليات العددية",
        description = "يرجى إنجاز التمارين 4 و 5 من السلسلة الأولى المرفقة.",
        deadline = "2026-10-10",
        subjectId = "subj_math",
        subjectName = "الرياضيات",
        groupId = "grp_1",
        authorTeacherId = "teacher_1",
        authorTeacherName = "أ. محمد العلمي"
    )

    val sampleResource1 = LearningResource(
        id = "res_1",
        title = "ملخص شامل: دراسة الدوال العددية وتطبيقاتها",
        type = "SUMMARY",
        subjectId = "subj_math",
        subjectName = "الرياضيات",
        level = "1Bac",
        targetGroupId = "grp_1",
        authorId = "teacher_1",
        authorName = "أ. محمد العلمي"
    )

    val sampleEnrollment1 = Enrollment(
        id = "enr_1",
        studentId = "student_1",
        studentName = "أحمد بن علي",
        subjectId = "subj_math",
        subjectName = "الرياضيات",
        teacherId = "teacher_1",
        teacherName = "أ. محمد العلمي",
        monthlyFee = 300.0,
        amountPaid = 300.0,
        amountRemaining = 0.0,
        status = "active",
        period = "أكتوبر 2026"
    )

    val samplePayment1 = PaymentRecord(
        id = "pay_1",
        studentId = "student_1",
        studentName = "أحمد بن علي",
        enrollmentId = "enr_1",
        subjectId = "subj_math",
        subjectName = "الرياضيات",
        teacherId = "teacher_1",
        teacherName = "أ. محمد العلمي",
        amount = 300.0,
        teacherShare = 180.0,
        schoolShare = 120.0,
        teacherPercentage = 60.0,
        month = "أكتوبر 2026",
        status = "PAID",
        date = "2026-10-01",
        notes = "دفع نقدي",
        recordedBy = "مدير المركز"
    )

    val sampleConversation1 = ChatConversation(
        id = "conv_1",
        name = "محادثة الإدارة والأستاذ",
        isGroup = false,
        participantIds = listOf("admin_fixed", "teacher_1"),
        creatorId = "admin_fixed",
        lastMessage = "مرحباً أستاذ، تم إعداد جدول الحصص بنجاح",
        lastMessageTime = System.currentTimeMillis() - 3600000L
    )

    val sampleMessage1 = ChatMessage(
        id = "msg_1",
        conversationId = "conv_1",
        senderId = "admin_fixed",
        senderName = "مدير المركز",
        senderRole = Role.ADMIN.name,
        messageText = "مرحباً أستاذ، تم إعداد جدول الحصص بنجاح",
        timestamp = System.currentTimeMillis() - 3600000L
    )

    // Reactive StateFlows
    val usersFlow = MutableStateFlow<List<SchoolUser>>(listOf(adminUser, teacher1, teacher2, student1, student2))
    val groupsFlow = MutableStateFlow<List<SchoolGroup>>(listOf(sampleGroup1, sampleGroup2))
    val subjectsFlow = MutableStateFlow<List<Subject>>(listOf(sampleSubjectMath, sampleSubjectPhysics, sampleSubjectSvt))
    val roomsFlow = MutableStateFlow<List<Room>>(listOf(sampleRoom1, sampleRoom2))
    val timetableFlow = MutableStateFlow<List<TimetableSlot>>(listOf(sampleSlot1, sampleSlot2))
    val attendanceFlow = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val resourcesFlow = MutableStateFlow<List<LearningResource>>(listOf(sampleResource1))
    val homeworkFlow = MutableStateFlow<List<Homework>>(listOf(sampleHomework1))
    val submissionsFlow = MutableStateFlow<List<HomeworkSubmission>>(emptyList())
    val gradesFlow = MutableStateFlow<List<GradeItem>>(listOf(sampleGrade1, sampleGrade2))
    val enrollmentsFlow = MutableStateFlow<List<Enrollment>>(listOf(sampleEnrollment1))
    val teacherSharesFlow = MutableStateFlow<List<TeacherSubjectShare>>(emptyList())
    val paymentsFlow = MutableStateFlow<List<PaymentRecord>>(listOf(samplePayment1))
    val auditLogsFlow = MutableStateFlow<List<AuditLog>>(emptyList())
    val announcementsFlow = MutableStateFlow<List<Announcement>>(listOf(sampleAnnouncement1))
    val conversationsFlow = MutableStateFlow<List<ChatConversation>>(listOf(sampleConversation1))
    val messagesMapFlow = MutableStateFlow<Map<String, List<ChatMessage>>>(mapOf("conv_1" to listOf(sampleMessage1)))
    val qaPostsFlow = MutableStateFlow<List<SubjectQaPost>>(emptyList())
    val qaRepliesMapFlow = MutableStateFlow<Map<String, List<SubjectQaReply>>>(emptyMap())

    fun resetToDefaults() {
        usersFlow.value = listOf(adminUser, teacher1, teacher2, student1, student2)
        groupsFlow.value = listOf(sampleGroup1, sampleGroup2)
        subjectsFlow.value = listOf(sampleSubjectMath, sampleSubjectPhysics, sampleSubjectSvt)
        roomsFlow.value = listOf(sampleRoom1, sampleRoom2)
        timetableFlow.value = listOf(sampleSlot1, sampleSlot2)
        attendanceFlow.value = emptyList()
        resourcesFlow.value = listOf(sampleResource1)
        homeworkFlow.value = listOf(sampleHomework1)
        submissionsFlow.value = emptyList()
        gradesFlow.value = listOf(sampleGrade1, sampleGrade2)
        enrollmentsFlow.value = listOf(sampleEnrollment1)
        teacherSharesFlow.value = emptyList()
        paymentsFlow.value = listOf(samplePayment1)
        auditLogsFlow.value = emptyList()
        announcementsFlow.value = listOf(sampleAnnouncement1)
        conversationsFlow.value = listOf(sampleConversation1)
        messagesMapFlow.value = mapOf("conv_1" to listOf(sampleMessage1))
        qaPostsFlow.value = emptyList()
        qaRepliesMapFlow.value = emptyMap()
    }
}
