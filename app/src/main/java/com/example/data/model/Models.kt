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
    val canSendAnnouncements: Boolean = true,
    val canViewFinance: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "canPublishResources" to canPublishResources,
        "canEditGrades" to canEditGrades,
        "canSendAnnouncements" to canSendAnnouncements,
        "canViewFinance" to canViewFinance
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): TeacherPermissions {
            if (map == null) return TeacherPermissions()
            return TeacherPermissions(
                canPublishResources = map["canPublishResources"] as? Boolean ?: true,
                canEditGrades = map["canEditGrades"] as? Boolean ?: true,
                canSendAnnouncements = map["canSendAnnouncements"] as? Boolean ?: true,
                canViewFinance = map["canViewFinance"] as? Boolean ?: false
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
    val code: String = "",
    val level: String = "",
    val price: Double = 0.0,
    val teacherId: String = "",
    val teacherName: String = ""
) {
    val displayName: String
        get() = if (level.isNotBlank()) "$name - $level" else name

    val fullLabelWithPrice: String
        get() = buildString {
            append(name)
            if (level.isNotBlank()) append(" ($level)")
            if (price > 0) append(" • ${price.toInt()} DH")
            if (teacherName.isNotBlank()) append(" • $teacherName")
        }

    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "code" to code,
        "level" to level,
        "price" to price,
        "teacherId" to teacherId,
        "teacherName" to teacherName
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Subject {
            return Subject(
                id = id,
                name = map["name"] as? String ?: "",
                code = map["code"] as? String ?: "",
                level = map["level"] as? String ?: "",
                price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                teacherId = map["teacherId"] as? String ?: "",
                teacherName = map["teacherName"] as? String ?: ""
            )
        }
    }
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
    val level: String = "",
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
        "level" to level,
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
    val teacherId: String = "",
    val updatedAt: Long? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "slotId" to slotId,
        "subjectName" to subjectName,
        "groupName" to groupName,
        "date" to date,
        "presentStudentIds" to presentStudentIds,
        "absentStudentIds" to absentStudentIds,
        "lateStudentIds" to lateStudentIds,
        "teacherId" to teacherId,
        "updatedAt" to updatedAt
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "type" to type,
        "subjectId" to subjectId,
        "subjectName" to subjectName,
        "level" to level,
        "targetGroupId" to targetGroupId,
        "fileUrl" to fileUrl,
        "authorId" to authorId,
        "authorName" to authorName,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "description" to description,
        "deadline" to deadline,
        "subjectId" to subjectId,
        "subjectName" to subjectName,
        "groupId" to groupId,
        "attachmentUrl" to attachmentUrl,
        "authorTeacherId" to authorTeacherId,
        "authorTeacherName" to authorTeacherName,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
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
    val teacherId: String = "",
    val updatedAt: Long? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "studentId" to studentId,
        "studentName" to studentName,
        "subjectId" to subjectId,
        "subjectName" to subjectName,
        "type" to type,
        "score" to score,
        "maxScore" to maxScore,
        "comment" to comment,
        "date" to date,
        "teacherId" to teacherId,
        "updatedAt" to updatedAt
    )
}

data class Enrollment(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val monthlyFee: Double = 0.0,
    val amountPaid: Double = 0.0,
    val amountRemaining: Double = 0.0,
    val status: String = "active", // "active", "paused"
    val period: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "studentId" to studentId,
        "studentName" to studentName,
        "subjectId" to subjectId,
        "subjectName" to subjectName,
        "teacherId" to teacherId,
        "teacherName" to teacherName,
        "monthlyFee" to monthlyFee,
        "amountPaid" to amountPaid,
        "amountRemaining" to amountRemaining,
        "status" to status,
        "period" to period,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Enrollment {
            val fee = (map["monthlyFee"] as? Number)?.toDouble() ?: 0.0
            val paid = (map["amountPaid"] as? Number)?.toDouble() ?: 0.0
            val remaining = (map["amountRemaining"] as? Number)?.toDouble() ?: (fee - paid).coerceAtLeast(0.0)
            return Enrollment(
                id = id,
                studentId = map["studentId"] as? String ?: "",
                studentName = map["studentName"] as? String ?: "",
                subjectId = map["subjectId"] as? String ?: "",
                subjectName = map["subjectName"] as? String ?: "",
                teacherId = map["teacherId"] as? String ?: "",
                teacherName = map["teacherName"] as? String ?: "",
                monthlyFee = fee,
                amountPaid = paid,
                amountRemaining = remaining,
                status = map["status"] as? String ?: "active",
                period = map["period"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class TeacherSubjectShare(
    val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val requestedPercentage: Double = 50.0,
    val approvedPercentage: Double = 50.0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val schoolPercentage: Double
        get() = (100.0 - approvedPercentage).coerceIn(0.0, 100.0)

    fun toMap(): Map<String, Any> = mapOf(
        "teacherId" to teacherId,
        "teacherName" to teacherName,
        "subjectId" to subjectId,
        "subjectName" to subjectName,
        "requestedPercentage" to requestedPercentage,
        "approvedPercentage" to approvedPercentage,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): TeacherSubjectShare {
            return TeacherSubjectShare(
                id = id,
                teacherId = map["teacherId"] as? String ?: "",
                teacherName = map["teacherName"] as? String ?: "",
                subjectId = map["subjectId"] as? String ?: "",
                subjectName = map["subjectName"] as? String ?: "",
                requestedPercentage = (map["requestedPercentage"] as? Number)?.toDouble() ?: 50.0,
                approvedPercentage = (map["approvedPercentage"] as? Number)?.toDouble() ?: 50.0,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class PaymentRecord(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val enrollmentId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val amount: Double = 0.0,
    val teacherShare: Double = 0.0,
    val schoolShare: Double = 0.0,
    val teacherPercentage: Double = 0.0,
    val month: String = "",
    val status: String = "PAID", // PAID, PARTIALLY_PAID, PENDING, OVERDUE
    val date: String = "",
    val notes: String = "",
    val recordedBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "studentId" to studentId,
        "studentName" to studentName,
        "enrollmentId" to enrollmentId,
        "subjectId" to subjectId,
        "subjectName" to subjectName,
        "teacherId" to teacherId,
        "teacherName" to teacherName,
        "amount" to amount,
        "teacherShare" to teacherShare,
        "schoolShare" to schoolShare,
        "teacherPercentage" to teacherPercentage,
        "month" to month,
        "status" to status,
        "date" to date,
        "notes" to notes,
        "recordedBy" to recordedBy,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): PaymentRecord {
            return PaymentRecord(
                id = id,
                studentId = map["studentId"] as? String ?: "",
                studentName = map["studentName"] as? String ?: "",
                enrollmentId = map["enrollmentId"] as? String ?: "",
                subjectId = map["subjectId"] as? String ?: "",
                subjectName = map["subjectName"] as? String ?: "",
                teacherId = map["teacherId"] as? String ?: "",
                teacherName = map["teacherName"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                teacherShare = (map["teacherShare"] as? Number)?.toDouble() ?: 0.0,
                schoolShare = (map["schoolShare"] as? Number)?.toDouble() ?: 0.0,
                teacherPercentage = (map["teacherPercentage"] as? Number)?.toDouble() ?: 0.0,
                month = map["month"] as? String ?: "",
                status = map["status"] as? String ?: "PAID",
                date = map["date"] as? String ?: "",
                notes = map["notes"] as? String ?: "",
                recordedBy = map["recordedBy"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class AuditLog(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "",
    val action: String = "EDIT",
    val targetCollection: String = "",
    val targetRecordId: String = "",
    val recordTitle: String = "",
    val oldValue: String = "",
    val newValue: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateStr: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "userName" to userName,
        "userRole" to userRole,
        "action" to action,
        "targetCollection" to targetCollection,
        "targetRecordId" to targetRecordId,
        "recordTitle" to recordTitle,
        "oldValue" to oldValue,
        "newValue" to newValue,
        "note" to note,
        "timestamp" to timestamp,
        "dateStr" to dateStr
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AuditLog {
            return AuditLog(
                id = id,
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "",
                userRole = map["userRole"] as? String ?: "",
                action = map["action"] as? String ?: "EDIT",
                targetCollection = map["targetCollection"] as? String ?: "",
                targetRecordId = map["targetRecordId"] as? String ?: "",
                recordTitle = map["recordTitle"] as? String ?: "",
                oldValue = map["oldValue"] as? String ?: "",
                newValue = map["newValue"] as? String ?: "",
                note = map["note"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                dateStr = map["dateStr"] as? String ?: ""
            )
        }
    }
}

data class Announcement(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val targetAudience: String = "ALL", // ALL, TEACHERS, STUDENTS, GROUP
    val targetGroupId: String? = null,
    val authorName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "body" to body,
        "targetAudience" to targetAudience,
        "targetGroupId" to targetGroupId,
        "authorName" to authorName,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
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
    val unreadMap: Map<String, Int> = emptyMap(),
    val status: String = "ACCEPTED", // "PENDING", "ACCEPTED", "REJECTED"
    val requestSenderId: String = "",
    val requestReceiverId: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "isGroup" to isGroup,
        "photoUrl" to photoUrl,
        "participantIds" to participantIds,
        "creatorId" to creatorId,
        "lastMessage" to lastMessage,
        "lastMessageTime" to lastMessageTime,
        "unreadMap" to unreadMap,
        "status" to status,
        "requestSenderId" to requestSenderId,
        "requestReceiverId" to requestReceiverId
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
    val documentUrl: String = "",
    val documentName: String = "",
    val documentSize: Long = 0L,
    val documentType: String = "",
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
        "documentUrl" to documentUrl,
        "documentName" to documentName,
        "documentSize" to documentSize,
        "documentType" to documentType,
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
