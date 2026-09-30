package com.example.data.model

enum class Role {
    ADMIN,
    TEACHER,
    STUDENT;

    companion object {
        fun fromString(value: String): Role {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: STUDENT
        }
    }
}

data class TeacherPermissions(
    val canPublishResources: Boolean = true,
    val canEditGrades: Boolean = true,
    val canSendAnnouncements: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "canPublishResources" to canPublishResources,
        "canEditGrades" to canEditGrades,
        "canSendAnnouncements" to canSendAnnouncements
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): TeacherPermissions {
            if (map == null) return TeacherPermissions()
            return TeacherPermissions(
                canPublishResources = map["canPublishResources"] as? Boolean ?: true,
                canEditGrades = map["canEditGrades"] as? Boolean ?: true,
                canSendAnnouncements = map["canSendAnnouncements"] as? Boolean ?: true
            )
        }
    }
}

data class SchoolUser(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val role: Role = Role.STUDENT,
    val phone: String = "",
    val isActive: Boolean = true,
    val groupIds: List<String> = emptyList(),
    val subjectIds: List<String> = emptyList(),
    val teacherPermissions: TeacherPermissions = TeacherPermissions(),
    val isPrimaryAdmin: Boolean = false,
    val recoveryEmail: String = "",
    val needsPasswordChange: Boolean = false
) {
    val email: String
        get() = "${username.trim().lowercase()}@school.app"

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "username" to username,
        "fullName" to fullName,
        "role" to role.name,
        "phone" to phone,
        "isActive" to isActive,
        "groupIds" to groupIds,
        "subjectIds" to subjectIds,
        "teacherPermissions" to teacherPermissions.toMap(),
        "isPrimaryAdmin" to isPrimaryAdmin,
        "recoveryEmail" to recoveryEmail,
        "needsPasswordChange" to needsPasswordChange
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): SchoolUser {
            val roleStr = map["role"] as? String ?: "STUDENT"
            val permsMap = map["teacherPermissions"] as? Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            return SchoolUser(
                id = id,
                username = map["username"] as? String ?: "",
                fullName = map["fullName"] as? String ?: "",
                role = Role.fromString(roleStr),
                phone = map["phone"] as? String ?: "",
                isActive = map["isActive"] as? Boolean ?: true,
                groupIds = (map["groupIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                subjectIds = (map["subjectIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                teacherPermissions = TeacherPermissions.fromMap(permsMap),
                isPrimaryAdmin = map["isPrimaryAdmin"] as? Boolean ?: false,
                recoveryEmail = map["recoveryEmail"] as? String ?: "",
                needsPasswordChange = map["needsPasswordChange"] as? Boolean ?: false
            )
        }
    }
}

data class SchoolGroup(
    val id: String = "",
    val name: String = "",
    val level: String = "",
    val studentIds: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "level" to level,
        "studentIds" to studentIds
    )
}

data class Subject(
    val id: String = "",
    val name: String = "",
    val code: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "code" to code
    )
}

data class Room(
    val id: String = "",
    val name: String = "",
    val capacity: Int = 30
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "capacity" to capacity
    )
}

data class TimetableSlot(
    val id: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val roomId: String = "",
    val roomName: String = "",
    val dayOfWeek: Int = 1, // 1: Monday, 7: Sunday
    val startTime: String = "08:00",
    val endTime: String = "10:00"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "subjectId" to subjectId,
        "subjectName" to subjectName,
        "teacherId" to teacherId,
        "teacherName" to teacherName,
        "groupId" to groupId,
        "groupName" to groupName,
        "roomId" to roomId,
        "roomName" to roomName,
        "dayOfWeek" to dayOfWeek,
        "startTime" to startTime,
        "endTime" to endTime
    )

    // Checks overlap between this slot and another
    fun overlapsWith(other: TimetableSlot): Boolean {
        if (this.dayOfWeek != other.dayOfWeek) return false
        // Simple time string comparison "HH:MM"
        return !(this.endTime <= other.startTime || this.startTime >= other.endTime)
    }
}

data class AttendanceRecord(
    val id: String = "",
    val slotId: String = "",
    val subjectName: String = "",
    val groupName: String = "",
    val date: String = "",
    val presentStudentIds: List<String> = emptyList(),
    val absentStudentIds: List<String> = emptyList(),
    val lateStudentIds: List<String> = emptyList(),
    val teacherId: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "slotId" to slotId,
        "subjectName" to subjectName,
        "groupName" to groupName,
        "date" to date,
        "presentStudentIds" to presentStudentIds,
        "absentStudentIds" to absentStudentIds,
        "lateStudentIds" to lateStudentIds,
        "teacherId" to teacherId
    )
}

data class LearningResource(
    val id: String = "",
    val title: String = "",
    val type: String = "SUMMARY", // SUMMARY, EXERCISE, PAST_EXAM
    val subjectId: String = "",
    val subjectName: String = "",
    val level: String = "",
    val targetGroupId: String = "",
    val fileUrl: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "title" to title,
        "type" to type,
        "subjectId" to subjectId,
        "subjectName" to subjectName,
        "level" to level,
        "targetGroupId" to targetGroupId,
        "fileUrl" to fileUrl,
        "authorId" to authorId,
        "authorName" to authorName,
        "createdAt" to createdAt
    )
}

data class Homework(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val deadline: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val groupId: String = "",
    val attachmentUrl: String = "",
    val authorTeacherId: String = "",
    val authorTeacherName: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "title" to title,
        "description" to description,
        "deadline" to deadline,
        "subjectId" to subjectId,
        "subjectName" to subjectName,
        "groupId" to groupId,
        "attachmentUrl" to attachmentUrl,
        "authorTeacherId" to authorTeacherId,
        "authorTeacherName" to authorTeacherName,
        "createdAt" to createdAt
    )
}

data class HomeworkSubmission(
    val id: String = "",
    val homeworkId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val submissionText: String = "",
    val attachmentUrl: String = "",
    val submittedAt: Long = System.currentTimeMillis(),
    val score: Float? = null,
    val feedback: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "homeworkId" to homeworkId,
        "studentId" to studentId,
        "studentName" to studentName,
        "submissionText" to submissionText,
        "attachmentUrl" to attachmentUrl,
        "submittedAt" to submittedAt,
        "score" to score,
        "feedback" to feedback
    )
}

data class GradeItem(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val type: String = "EXAM", // EXAM, QUIZ, HOMEWORK
    val score: Float = 0f,
    val maxScore: Float = 20f,
    val comment: String = "",
    val date: String = "",
    val teacherId: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "studentId" to studentId,
        "studentName" to studentName,
        "subjectId" to subjectId,
        "subjectName" to subjectName,
        "type" to type,
        "score" to score,
        "maxScore" to maxScore,
        "comment" to comment,
        "date" to date,
        "teacherId" to teacherId
    )
}

data class PaymentRecord(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val amount: Double = 0.0,
    val month: String = "",
    val status: String = "PAID", // PAID, PENDING, OVERDUE
    val date: String = "",
    val notes: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "studentId" to studentId,
        "studentName" to studentName,
        "amount" to amount,
        "month" to month,
        "status" to status,
        "date" to date,
        "notes" to notes
    )
}

data class Announcement(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val targetAudience: String = "ALL", // ALL, TEACHERS, STUDENTS, GROUP
    val targetGroupId: String? = null,
    val authorName: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "body" to body,
        "targetAudience" to targetAudience,
        "targetGroupId" to targetGroupId,
        "authorName" to authorName,
        "createdAt" to createdAt
    )
}

data class ChatConversation(
    val id: String = "",
    val name: String = "",
    val isGroup: Boolean = false,
    val photoUrl: String = "",
    val participantIds: List<String> = emptyList(),
    val creatorId: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadMap: Map<String, Int> = emptyMap()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "isGroup" to isGroup,
        "photoUrl" to photoUrl,
        "participantIds" to participantIds,
        "creatorId" to creatorId,
        "lastMessage" to lastMessage,
        "lastMessageTime" to lastMessageTime,
        "unreadMap" to unreadMap
    )
}

data class ChatMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val messageText: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "",
    val audioDurationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val readBy: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "conversationId" to conversationId,
        "senderId" to senderId,
        "senderName" to senderName,
        "senderRole" to senderRole,
        "messageText" to messageText,
        "imageUrl" to imageUrl,
        "audioUrl" to audioUrl,
        "audioDurationSeconds" to audioDurationSeconds,
        "timestamp" to timestamp,
        "readBy" to readBy
    )
}

data class SubjectQaPost(
    val id: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorRole: String = "",
    val questionText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val repliesCount: Int = 0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "subjectId" to subjectId,
        "subjectName" to subjectName,
        "authorId" to authorId,
        "authorName" to authorName,
        "authorRole" to authorRole,
        "questionText" to questionText,
        "timestamp" to timestamp,
        "repliesCount" to repliesCount
    )
}

data class SubjectQaReply(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorRole: String = "",
    val replyText: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "postId" to postId,
        "authorId" to authorId,
        "authorName" to authorName,
        "authorRole" to authorRole,
        "replyText" to replyText,
        "timestamp" to timestamp
    )
}
