package com.example.localization

interface AppStrings {

    val appName: String
    val welcomeTitle: String
    val welcomeSubtitle: String
    val username: String
    val password: String
    val rememberMe: String
    val login: String
    val loggingIn: String
    val loginError: String
    val invalidCredentials: String
    val accountDisabled: String
    val selectLanguage: String
    val logout: String
    val roleAdmin: String
    val roleTeacher: String
    val roleStudent: String

    // Navigation
    val navDashboard: String
    val navUsers: String
    val navTimetable: String
    val navFinance: String
    val navReports: String
    val navAttendance: String
    val navResources: String
    val navHomework: String
    val navGrades: String
    val navChat: String
    val navAnnouncements: String
    val navSettings: String
    val navGroups: String

    // Users
    val usersTitle: String
    val studentsTab: String
    val teachersTab: String
    val addUser: String
    val addStudent: String
    val addTeacher: String
    val fullName: String
    val phone: String
    val autoGeneratePassword: String
    val credentialsTitle: String
    val credentialsDesc: String
    val copyCredentials: String
    val credentialsCopied: String
    val editUser: String
    val deleteUser: String
    val disableUser: String
    val enableUser: String
    val confirmDelete: String
    val teacherPermissions: String
    val permPublishResources: String
    val permEditGrades: String
    val permSendAnnouncements: String
    val statusActive: String
    val statusDisabled: String
    val searchPlaceholder: String

    // Academic / Timetable
    val groupsTitle: String
    val subjectsTitle: String
    val roomsTitle: String
    val addGroup: String
    val addSubject: String
    val addRoom: String
    val groupName: String
    val subjectName: String
    val teacherName: String
    val roomName: String
    val manualEntry: String
    val selectFromList: String
    val level: String
    val timetableTitle: String
    val addSlot: String
    val dayOfWeek: String
    val startTime: String
    val endTime: String
    val conflictDetected: String
    val conflictRoom: String
    val conflictTeacher: String
    val noConflict: String
    val weeklySchedule: String
    val nextClassReminder: String
    val noScheduleToday: String

    // Days
    val monday: String
    val tuesday: String
    val wednesday: String
    val thursday: String
    val friday: String
    val saturday: String
    val sunday: String

    // Attendance
    val attendanceTitle: String
    val markAttendance: String
    val present: String
    val absent: String
    val late: String
    val attendanceSaved: String
    val attendanceRate: String
    val myAttendanceRecord: String
    val totalSessions: String

    // Resources
    val resourcesTitle: String
    val addResource: String
    val resourceTitle: String
    val resourceType: String
    val typeSummary: String
    val typeExercise: String
    val typePastExam: String
    val fileOrUrl: String
    val downloadOrOpen: String

    // Homework
    val homeworkTitle: String
    val createHomework: String
    val deadline: String
    val submissions: String
    val submitHomework: String
    val yourSubmission: String
    val gradeSubmission: String
    val score: String
    val teacherComment: String
    val submittedStatus: String
    val notSubmittedStatus: String

    // Grades
    val gradesTitle: String
    val addGrade: String
    val gradeType: String
    val typeExam: String
    val typeQuiz: String
    val typeHomeworkGrade: String
    val gradeValue: String
    val overallAverage: String
    val progressChart: String

    // Finance
    val financeTitle: String
    val monthlyFees: String
    val paymentStatus: String
    val statusPaid: String
    val statusPending: String
    val statusOverdue: String
    val totalCollected: String
    val totalOverdue: String
    val recordPayment: String
    val amount: String
    val paymentDate: String

    // Reports
    val reportsTitle: String
    val attendanceReport: String
    val gradesReport: String
    val financeReport: String
    val exportPdf: String
    val exportExcel: String
    val reportExported: String

    // Announcements
    val announcementsTitle: String
    val createAnnouncement: String
    val announcementTitle: String
    val announcementBody: String
    val audienceAll: String
    val audienceTeachers: String
    val audienceStudents: String
    val unread: String
    val deleteAnnouncement: String
    val deleteAnnouncementConfirm: String
    val tapToDownloadImage: String

    // Chat
    val chatTitle: String
    val privateTab: String
    val groupsTab: String
    val qaTab: String
    val newGroupChat: String
    val recordVoice: String
    val recording: String
    val stopAndSend: String
    val playAudio: String
    val sendImage: String
    val sendDocument: String
    val downloadFile: String
    val downloadingFile: String
    val fileDownloaded: String
    val openFile: String
    val documentLabel: String
    val voiceNoteLabel: String
    val downloadFailed: String
    val downloadImage: String
    val typeMessage: String
    val askQuestion: String
    val answerQuestion: String
    val replies: String
    val moderationNotice: String
    val deleteMessage: String
    val chatRequestTitle: String
    val chatRequestDesc: String
    val chatRequestPending: String
    val acceptRequest: String
    val declineRequest: String
    val requestChat: String
    val studentPrivacyNotice: String
    val onlyRegisteredStudentsNotice: String

    // Common
    val save: String
    val cancel: String
    val delete: String
    val edit: String
    val confirm: String
    val close: String
    val copy: String
    val noDataYet: String
    val loading: String
    val success: String
    val error: String
    val settingsTitle: String
    val firebaseStatus: String
    val connected: String
    val disconnected: String
    val setupInstructions: String
    val securityRulesTitle: String
    val capacitorStepsTitle: String
    val initializeAdminButton: String
    val adminInitializedSuccess: String

    // Security & Multi-Admin
    val secureAccountTitle: String
    val secureAccountSubtitle: String
    val newPassword: String
    val confirmPassword: String
    val recoveryEmail: String
    val changeUsernameOptional: String
    val passwordCannotBeAdmin: String
    val passwordsDoNotMatch: String
    val passwordTooShort: String
    val invalidEmail: String
    val saveAndContinue: String
    val forgotPassword: String
    val forgotPasswordTitle: String
    val forgotPasswordDesc: String
    val sendResetLink: String
    val resetLinkSent: String
    val manageAdmins: String
    val adminsTab: String
    val addAdmin: String
    val primaryAdmin: String
    val you: String
    val cannotDeletePrimaryAdmin: String
    val changePassword: String
    val oldPassword: String
    val passwordChangedSuccess: String

    // Finance Feature 1 & 2
    val enrollmentsTitle: String
    val enrollStudent: String
    val enrolledSubjects: String
    val monthlyFeeAmount: String
    val amountDue: String
    val amountPaidLabel: String
    val amountRemainingLabel: String
    val selectSubjectForPayment: String
    val teacherShareLabel: String
    val schoolShareLabel: String
    val teacherPercentageLabel: String
    val requestedPercentageLabel: String
    val approvedPercentageLabel: String
    val setPercentage: String
    val requestPercentage: String
    val proposePercentage: String
    val statusPartiallyPaid: String
    val statusUnpaid: String
    val pauseEnrollment: String
    val resumeEnrollment: String
    val enrollmentActive: String
    val enrollmentPaused: String
    val perTeacherBreakdown: String
    val perStudentBreakdown: String
    val studentsTaught: String
    val totalCollectedForSubject: String
    val teacherShareOwed: String
    val schoolShareTotal: String
    val searchStudentPlaceholder: String
    val noEnrolledSubjects: String
    val myFeesTitle: String
    val paymentHistoryTitle: String
    val teacherFinanceTitle: String
    val myEarnedShare: String
    val myStudentsStatus: String
    val permViewFinance: String

    // Audit Log & Universal Edit
    val activityLogTitle: String
    val activityLogSubtitle: String
    val auditWarningDialogTitle: String
    val auditWarningMessage: String
    val auditReasonNote: String
    val auditReasonPlaceholder: String
    val editFinancialRecord: String
    val confirmAndLog: String
    val filterByCollection: String
    val filterByUser: String
    val filterAll: String
    val oldValueLabel: String
    val newValueLabel: String
    val recordLabel: String
    val actionLabel: String
    val editGrade: String
    val editAttendance: String
    val editHomework: String
    val editResource: String
    val editAnnouncement: String
    val editTimetableSlot: String
    val editedBadge: String
    val noAuditLogsYet: String
    val notificationsTitle: String
    val notificationsEnabled: String
    val notificationsDisabled: String
    val testNotification: String
    val enableNotifications: String
    val notificationsDesc: String
    val resetAdminButton: String
    val factoryResetTitle: String
    val factoryResetDesc: String
    val factoryResetConfirmTitle: String
    val factoryResetConfirmMessage: String
    val factoryResetSuccess: String
    val currencySymbol: String
    val connectNewDatabaseTitle: String
    val connectNewDatabaseDesc: String
    val pasteJsonTab: String
    val manualEntryTab: String
    val pasteJsonPlaceholder: String
    val applyAndConnectButton: String
    val databaseConnectedSuccess: String
    val resetToDefaultDatabase: String
    val searchUsersTitle: String
    val searchUsersPlaceholder: String
    val allUsersTab: String
    val roomCapacity: String
    val roomType: String
    val roomTypeGeneral: String
    val roomTypeLab: String
    val roomTypeComputer: String
    val roomTypeHall: String
    val editRoom: String
    val deleteRoomConfirm: String
    val noRoomsYet: String
    val quickTimeSlots: String
    val sessionDetails: String

    val selectSubject: String
    val teacherSubject: String
    val studentSubjects: String
    val orAddNewSubject: String
    val addSubjectPrompt: String
    val subjectCreatedDirectly: String
    val addAnotherSubject: String
    val noSubjectsAvailable: String
    val newSubjectNameInput: String
    val assignSubjectToTeacher: String
    val removeSubject: String
    val teacherSubjectsPrompt: String
    val addAnotherSubjectForTeacher: String

    val subjectsPricingTitle: String
    val addSubjectLevel: String
    val subjectLevel: String
    val subjectPrice: String
    val assignedTeacher: String
    val selectTeacher: String
    val noTeacherAssigned: String
    val applyTeacherToAllLevels: String
    val editSubject: String
    val deleteSubjectConfirm: String
    val commonLevels: String
    val commonSubjects: String
    val perMonth: String

}

class ArabicStrings : AppStrings {

    override val appName: String = "science.est.center"
    override val welcomeTitle: String = "مرحباً بك في science.est.center"
    override val welcomeSubtitle: String = "منصة إدارة المدرسة الخاصة ودعم الطلاب المتكاملة"
    override val username: String = "اسم المستخدم"
    override val password: String = "كلمة المرور"
    override val rememberMe: String = "تذكرني"
    override val login: String = "تسجيل الدخول"
    override val loggingIn: String = "جارٍ تسجيل الدخول..."
    override val loginError: String = "خطأ في تسجيل الدخول. تحقق من اسم المستخدم وكلمة المرور."
    override val invalidCredentials: String = "اسم المستخدم أو كلمة المرور غير صحيحة"
    override val accountDisabled: String = "هذا الحساب معطل حالياً. يرجى مراجعة إدارة المدرسة."
    override val selectLanguage: String = "اختر اللغة"
    override val logout: String = "تسجيل الخروج"
    override val roleAdmin: String = "مدير النظام"
    override val roleTeacher: String = "أستاذ"
    override val roleStudent: String = "طالب"

    override val navDashboard: String = "الرئيسية"
    override val navUsers: String = "المستخدمون"
    override val navTimetable: String = "جدول الحصص"
    override val navFinance: String = "المالية"
    override val navReports: String = "التقارير"
    override val navAttendance: String = "الغياب والحضور"
    override val navResources: String = "المكتبة التعليمية"
    override val navHomework: String = "الواجبات"
    override val navGrades: String = "كشف النقاط"
    override val navChat: String = "المحادثات"
    override val navAnnouncements: String = "الإعلانات"
    override val navSettings: String = "الإعدادات"
    override val navGroups: String = "الأقسام والمواد"

    override val usersTitle: String = "إدارة الطلاب والأساتذة"
    override val studentsTab: String = "الطلاب"
    override val teachersTab: String = "الأساتذة"
    override val addUser: String = "إضافة مستخدم جديد"
    override val addStudent: String = "إضافة طالب"
    override val addTeacher: String = "إضافة أستاذ"
    override val fullName: String = "الاسم واللقب"
    override val phone: String = "رقم الهاتف"
    override val autoGeneratePassword: String = "توليد كلمة سر تلقائية"
    override val credentialsTitle: String = "بيانات الدخول للحساب"
    override val credentialsDesc: String = "يرجى نسخ هذه البيانات ومشاركتها مع المستخدم الآن. لن يتم عرض كلمة المرور مرة أخرى لدواعي الأمان."
    override val copyCredentials: String = "نسخ بيانات الدخول"
    override val credentialsCopied: String = "تم نسخ بيانات الدخول إلى الحافظة"
    override val editUser: String = "تعديل الحساب"
    override val deleteUser: String = "حذف الحساب"
    override val disableUser: String = "تعطيل الحساب"
    override val enableUser: String = "تفعيل الحساب"
    override val confirmDelete: String = "هل أنت متأكد من رغبتك في حذف هذا الحساب نهائياً؟"
    override val teacherPermissions: String = "صلاحيات الأستاذ"
    override val permPublishResources: String = "نشر الموارد التعليمية"
    override val permEditGrades: String = "إدخال وتعديل النقاط"
    override val permSendAnnouncements: String = "إرسال الإعلانات العامة"
    override val statusActive: String = "نشط"
    override val statusDisabled: String = "معطل"
    override val searchPlaceholder: String = "بحث بالاسم أو اسم المستخدم..."

    override val groupsTitle: String = "الأقسام والمستويات"
    override val subjectsTitle: String = "المواد الدراسية"
    override val roomsTitle: String = "القاعات"
    override val addGroup: String = "إضافة قسم"
    override val addSubject: String = "إضافة مادة"
    override val addRoom: String = "إضافة قاعة"
    override val groupName: String = "اسم القسم / الفوج"
    override val subjectName: String = "اسم المادة"
    override val teacherName: String = "اسم الأستاذ"
    override val roomName: String = "اسم أو رقم القاعة"
    override val manualEntry: String = "كتابة يدوية"
    override val selectFromList: String = "اختيار من القائمة"
    override val level: String = "المستوى الدراسي"
    override val timetableTitle: String = "منشئ جدول الحصص الأسبوعي"
    override val addSlot: String = "إضافة حصة دراسية"
    override val dayOfWeek: String = "اليوم"
    override val startTime: String = "وقت البداية"
    override val endTime: String = "وقت النهاية"
    override val conflictDetected: String = "تنبيه: تم رصد تعارض في جدول الحصص!"
    override val conflictRoom: String = "تعارض: القاعة محجوزة لحصة أخرى في نفس التوقيت!"
    override val conflictTeacher: String = "تعارض: الأستاذ مرتبط بحصة أخرى في نفس التوقيت!"
    override val noConflict: String = "لا توجد أي تعارضات"
    override val weeklySchedule: String = "الجدول الأسبوعي"
    override val nextClassReminder: String = "الحصة القادمة قريباً"
    override val noScheduleToday: String = "لا توجد حصص مبرمجة اليوم"

    override val monday: String = "الإثنين"
    override val tuesday: String = "الثلاثاء"
    override val wednesday: String = "الأربعاء"
    override val thursday: String = "الخميس"
    override val friday: String = "الجمعة"
    override val saturday: String = "السبت"
    override val sunday: String = "الأحد"

    override val attendanceTitle: String = "تسجيل الغياب والحضور"
    override val markAttendance: String = "أخذ الحضور للحصة"
    override val present: String = "حاضر"
    override val absent: String = "غائب"
    override val late: String = "متأخر"
    override val attendanceSaved: String = "تم حفظ قائمة الحضور بنجاح"
    override val attendanceRate: String = "نسبة الحضور"
    override val myAttendanceRecord: String = "سجل الحضور والغياب الخاص بي"
    override val totalSessions: String = "إجمالي الحصص"

    override val resourcesTitle: String = "المكتبة والموارد التعليمية"
    override val addResource: String = "إضافة مورد تعليمي"
    override val resourceTitle: String = "عنوان المورد"
    override val resourceType: String = "نوع المورد"
    override val typeSummary: String = "ملخص درس"
    override val typeExercise: String = "سلسلة تمارين"
    override val typePastExam: String = "امتحان سابق مع الحل"
    override val fileOrUrl: String = "رابط الملف أو المورد"
    override val downloadOrOpen: String = "تحميل / فتح المورد"

    override val homeworkTitle: String = "الواجبات المنزلية"
    override val createHomework: String = "إنشاء واجب جديد"
    override val deadline: String = "آخر أجل للتسليم"
    override val submissions: String = "تسليمات الطلاب"
    override val submitHomework: String = "تسليم الواجب"
    override val yourSubmission: String = "إجابتك / عملك"
    override val gradeSubmission: String = "تقييم التسليم"
    override val score: String = "العلامة"
    override val teacherComment: String = "ملاحظة الأستاذ"
    override val submittedStatus: String = "تم التسليم"
    override val notSubmittedStatus: String = "لم يتم التسليم بعد"

    override val gradesTitle: String = "النقاط والتقييمات"
    override val addGrade: String = "رصد نقطة جديدة"
    override val gradeType: String = "نوع التقييم"
    override val typeExam: String = "امتحان"
    override val typeQuiz: String = "فرض / اختبار سريع"
    override val typeHomeworkGrade: String = "واجب منزلي"
    override val gradeValue: String = "النقطة"
    override val overallAverage: String = "المعدل العام"
    override val progressChart: String = "مخطط التطور عبر الزمن"

    override val financeTitle: String = "المالية والاشتراكات الشهرية"
    override val monthlyFees: String = "المستحقات الشهرية"
    override val paymentStatus: String = "حالة الدفع"
    override val statusPaid: String = "مدفوع"
    override val statusPending: String = "قيد الانتظار"
    override val statusOverdue: String = "متأخر عن الدفع"
    override val totalCollected: String = "إجمالي المبالغ المحصلة"
    override val totalOverdue: String = "إجمالي المبالغ المتأخرة"
    override val recordPayment: String = "تسجيل دفعة جديدة"
    override val amount: String = "المبلغ (د.ج)"
    override val paymentDate: String = "تاريخ الدفع"

    override val reportsTitle: String = "التقارير الشهرية والسنوية"
    override val attendanceReport: String = "تقرير الحضور والغياب"
    override val gradesReport: String = "تقرير النتائج والنقاط"
    override val financeReport: String = "التقرير المالي"
    override val exportPdf: String = "تصدير بتنسيق PDF"
    override val exportExcel: String = "تصدير بتنسيق Excel"
    override val reportExported: String = "تم إنشاء التقرير بنجاح"

    override val announcementsTitle: String = "الإعلانات الرسمية"
    override val createAnnouncement: String = "نشر إعلان جديد"
    override val announcementTitle: String = "عنوان الإعلان"
    override val announcementBody: String = "نص الإعلان الرسمي"
    override val audienceAll: String = "الجميع (الكل)"
    override val audienceTeachers: String = "الأساتذة فقط"
    override val audienceStudents: String = "الطلاب فقط"
    override val unread: String = "جديد وغير مقروء"
    override val deleteAnnouncement: String = "حذف الإعلان"
    override val deleteAnnouncementConfirm: String = "هل أنت متأكد من رغبتك في حذف هذا الإعلان نهائياً؟"
    override val tapToDownloadImage: String = "اضغط للتنزيل وعرض الصورة"

    override val chatTitle: String = "المحادثات والتواصل"
    override val privateTab: String = "محادثات فردية"
    override val groupsTab: String = "مجموعات الدردشة"
    override val qaTab: String = "فضاء الأسئلة والأجوبة"
    override val newGroupChat: String = "إنشاء مجموعة دردشة جديدة"
    override val recordVoice: String = "تسجيل رسالة صوتية"
    override val recording: String = "جارٍ التسجيل الصوتي..."
    override val stopAndSend: String = "إيقاف وإرسال التسجيل"
    override val playAudio: String = "تشغيل الصوت"
    override val sendImage: String = "إرسال صورة"
    override val sendDocument: String = "إرسال مستند (PDF أو ملف)"
    override val downloadFile: String = "تنزيل الملف"
    override val downloadingFile: String = "جارٍ التنزيل والحفظ..."
    override val fileDownloaded: String = "تم حفظ الملف بنجاح في مجلد التنزيلات"
    override val openFile: String = "فتح الملف"
    override val documentLabel: String = "مستند / وثيقة"
    override val voiceNoteLabel: String = "تسجيل صوتي"
    override val downloadFailed: String = "فشل تنزيل الملف، يرجى إعادة المحاولة"
    override val downloadImage: String = "حفظ الصورة في الهاتف"
    override val typeMessage: String = "اكتب رسالتك هنا..."
    override val askQuestion: String = "طرح سؤال دراسي جديد"
    override val answerQuestion: String = "كتابة إجابة"
    override val replies: String = "الإجابات"
    override val moderationNotice: String = "وضع مراقبة المحادثات من قبل الإدارة"
    override val deleteMessage: String = "حذف الرسالة المخالفة"
    override val chatRequestTitle: String = "طلب محادثة خاص"
    override val chatRequestDesc: String = "لحماية خصوصية الطلاب، يتطلب بدء المراسلة موافقة الطرف الآخر أولاً."
    override val chatRequestPending: String = "طلب المحادثة قيد الانتظار... لا يمكن إرسال الرسائل حتى تتم الموافقة."
    override val acceptRequest: String = "قبول المحادثة"
    override val declineRequest: String = "رفض الطلب"
    override val requestChat: String = "طلب مراسلة"
    override val studentPrivacyNotice: String = "🔒 خصوصية محمية: معلومات الاتصال الخاصة بالطلاب مخفية تماماً عن بقية الطلاب."
    override val onlyRegisteredStudentsNotice: String = "يظهر فقط الطلاب المسجلون في مادتك وفصولك الدراسية."

    override val save: String = "حفظ"
    override val cancel: String = "إلغاء"
    override val delete: String = "حذف"
    override val edit: String = "تعديل"
    override val confirm: String = "تأكيد"
    override val close: String = "إغلاق"
    override val copy: String = "نسخ"
    override val noDataYet: String = "لا توجد بيانات مسجلة حتى الآن"
    override val loading: String = "جارٍ التحميل..."
    override val success: String = "تمت العملية بنجاح"
    override val error: String = "حدث خطأ. يرجى المحاولة مرة أخرى"
    override val settingsTitle: String = "الإعدادات العامة"
    override val firebaseStatus: String = "حالة الاتصال بـ Firebase"
    override val connected: String = "متصل مباشرة بالسحابة"
    override val disconnected: String = "في انتظار الإعداد أو الربط"
    override val setupInstructions: String = "دليل ربط مشروع Firebase"
    override val securityRulesTitle: String = "قواعد حماية Firestore والتخزين"
    override val capacitorStepsTitle: String = "خطوات التحويل لتطبيق APK عبر Capacitor"
    override val initializeAdminButton: String = "تهيئة حساب الإدارة (admin/admin)"
    override val adminInitializedSuccess: String = "تم إعداد حساب الإدارة admin بنجاح!"

    override val secureAccountTitle: String = "تأمين حساب المشرف"
    override val secureAccountSubtitle: String = "يجب تغيير كلمة المرور وتعيين بريد استرداد رسمي لحماية الحساب من القفل الدائم."
    override val newPassword: String = "كلمة المرور الجديدة"
    override val confirmPassword: String = "تأكيد كلمة المرور"
    override val recoveryEmail: String = "بريد الاسترداد الإلكتروني (لإعادة التعيين)"
    override val changeUsernameOptional: String = "تغيير اسم المستخدم (اختياري، الافتراضي: admin)"
    override val passwordCannotBeAdmin: String = "لا يمكن أن تكون كلمة المرور 'admin' مرة أخرى"
    override val passwordsDoNotMatch: String = "كلمتا المرور غير متطابقتين"
    override val passwordTooShort: String = "يجب ألا تقل كلمة المرور عن 6 أحرف"
    override val invalidEmail: String = "يرجى إدخال عنوان بريد إلكتروني صحيح"
    override val saveAndContinue: String = "تأمين الحساب والمتابعة"
    override val forgotPassword: String = "نسيت كلمة المرور؟ (للمشرفين)"
    override val forgotPasswordTitle: String = "استعادة كلمة المرور للمشرفين"
    override val forgotPasswordDesc: String = "أدخل اسم المستخدم أو بريد الاسترداد لتلقي رابط إعادة تعيين كلمة المرور رسمياً من Firebase"
    override val sendResetLink: String = "إرسال رابط الاسترداد"
    override val resetLinkSent: String = "تم إرسال رابط إعادة تعيين كلمة المرور إلى بريد الاسترداد بنجاح!"
    override val manageAdmins: String = "إدارة المشرفين"
    override val adminsTab: String = "المشرفون"
    override val addAdmin: String = "إضافة مشرف جديد"
    override val primaryAdmin: String = "المشرف الأساسي"
    override val you: String = "حسابك الحالي"
    override val cannotDeletePrimaryAdmin: String = "لا يمكن حذف أو تعطيل المشرف الأساسي للمنظومة لضمان عدم القفل النهائي."
    override val changePassword: String = "تغيير كلمة المرور"
    override val oldPassword: String = "كلمة المرور الحالية"
    override val passwordChangedSuccess: String = "تم تحديث كلمة المرور بنجاح!"

    override val enrollmentsTitle: String = "تسجيلات المواد"
    override val enrollStudent: String = "تسجيل طالب في مادة"
    override val enrolledSubjects: String = "المواد المسجل بها"
    override val monthlyFeeAmount: String = "الرسوم الشهرية"
    override val amountDue: String = "المبلغ المطلوب"
    override val amountPaidLabel: String = "المبلغ المدفوع"
    override val amountRemainingLabel: String = "المبلغ المتبقي"
    override val selectSubjectForPayment: String = "اختر المادة المراد سدادها"
    override val teacherShareLabel: String = "حصة الأستاذ"
    override val schoolShareLabel: String = "حصة المركز"
    override val teacherPercentageLabel: String = "نسبة الأستاذ"
    override val requestedPercentageLabel: String = "النسبة المقترحة"
    override val approvedPercentageLabel: String = "النسبة المعتمدة"
    override val setPercentage: String = "تحديد نسبة الأستاذ"
    override val requestPercentage: String = "اقتراح نسبة للأستاذ"
    override val proposePercentage: String = "إرسال اقتراح النسبة"
    override val statusPartiallyPaid: String = "مدفوع جزئياً"
    override val statusUnpaid: String = "غير مدفوع"
    override val pauseEnrollment: String = "تجميد التسجيل"
    override val resumeEnrollment: String = "تنشيط التسجيل"
    override val enrollmentActive: String = "نشط"
    override val enrollmentPaused: String = "مجمّد"
    override val perTeacherBreakdown: String = "حسب الأساتذة"
    override val perStudentBreakdown: String = "حسب الطلاب"
    override val studentsTaught: String = "عدد الطلاب"
    override val totalCollectedForSubject: String = "إجمالي المحصل"
    override val teacherShareOwed: String = "مستحقات الأستاذ"
    override val schoolShareTotal: String = "عائد المركز"
    override val searchStudentPlaceholder: String = "ابحث بالاسم عن طالب..."
    override val noEnrolledSubjects: String = "لا توجد مواد مسجلة لهذا الطالب"
    override val myFeesTitle: String = "رسومي الدراسية"
    override val paymentHistoryTitle: String = "سجل المدفوعات"
    override val teacherFinanceTitle: String = "مستحقاتي المالية"
    override val myEarnedShare: String = "إجمالي أرباحي"
    override val myStudentsStatus: String = "حالة دفع طلابي"
    override val permViewFinance: String = "الاطلاع على مستحقاته المالية وحالة طلابه"

    override val activityLogTitle: String = "سجل العمليات والتدقيق"
    override val activityLogSubtitle: String = "سجل التعديلات والعمليات المالية والتحكم بالبيانات"
    override val auditWarningDialogTitle: String = "تأكيد تعديل السجل المالي"
    override val auditWarningMessage: String = "تنبيه أمني: سيتم توثيق هذا التعديل، الوقت الدقيق، والقيم السابقة والجديدة في سجل التدقيق المالي غير القابل للحذف."
    override val auditReasonNote: String = "سبب التعديل أو ملاحظة (اختياري)"
    override val auditReasonPlaceholder: String = "مثال: تصحيح خطأ إملائي في المبلغ من 500 إلى 50"
    override val editFinancialRecord: String = "تعديل السجل المالي"
    override val confirmAndLog: String = "تأكيد وتوثيق بالسجل"
    override val filterByCollection: String = "تصفية حسب النوع"
    override val filterByUser: String = "تصفية حسب المستخدم"
    override val filterAll: String = "الكل"
    override val oldValueLabel: String = "القيمة السابقة"
    override val newValueLabel: String = "القيمة الجديدة"
    override val recordLabel: String = "السجل"
    override val actionLabel: String = "العملية"
    override val editGrade: String = "تعديل النقطة"
    override val editAttendance: String = "تعديل الحضور"
    override val editHomework: String = "تعديل الواجب"
    override val editResource: String = "تعديل المورد"
    override val editAnnouncement: String = "تعديل الإعلان"
    override val editTimetableSlot: String = "تعديل الحصة"
    override val editedBadge: String = "مُعدّل"
    override val noAuditLogsYet: String = "لا توجد سجلات تدقيق حتى الآن"
    override val notificationsTitle: String = "الإشعارات والتنبيهات"
    override val notificationsEnabled: String = "الإشعارات مفعلة"
    override val notificationsDisabled: String = "الإشعارات معطلة"
    override val testNotification: String = "إرسال إشعار تجريبي"
    override val enableNotifications: String = "تفعيل الإشعارات"
    override val notificationsDesc: String = "استلام إشعارات فورية على الهاتف عند وصول رسالة جديدة أو إعلان مدرسي"
    override val resetAdminButton: String = "استعادة والدخول بحساب المدير (admin / admin)"
    override val factoryResetTitle: String = "تصفير التطبيق والبدء من جديد"
    override val factoryResetDesc: String = "حذف كافة الحسابات والبيانات والبدء في التطبيق كأنه جديد بحساب admin فقط"
    override val factoryResetConfirmTitle: String = "تأكيد تصفير كافة الحسابات والبيانات؟"
    override val factoryResetConfirmMessage: String = "تحذير: سيتم حذف جميع حسابات الطلاب والأساتذة والجداول والواجبات وكافة السجلات نهائياً، والبدء كنسخة جديدة تماماً بحساب admin / admin."
    override val factoryResetSuccess: String = "تم تصفير التطبيق بنجاح والبدء كنسخة جديدة!"
    override val currencySymbol: String = "درهم"
    override val connectNewDatabaseTitle: String = "ربط قاعدة بيانات Firebase جديدة"
    override val connectNewDatabaseDesc: String = "استبدال قاعدة البيانات الحالية بقاعدة بياناتك الخاصة والبدء من جديد"
    override val pasteJsonTab: String = "لصق google-services.json"
    override val manualEntryTab: String = "إدخال يدوي"
    override val pasteJsonPlaceholder: String = "الصق محتوى ملف google-services.json هنا..."
    override val applyAndConnectButton: String = "حفظ والاتصال بقاعدة البيانات الجديدة"
    override val databaseConnectedSuccess: String = "تم الاتصال بقاعدة البيانات الجديدة بنجاح!"
    override val resetToDefaultDatabase: String = "العودة لقاعدة البيانات الأصلية"
    override val searchUsersTitle: String = "البحث عن تلميذ أو أستاذ"
    override val searchUsersPlaceholder: String = "ابحث بالاسم، اللقب، اسم المستخدم، أو رقم الهاتف..."
    override val allUsersTab: String = "الكل"
    override val roomCapacity: String = "السعة الاستيعابية"
    override val roomType: String = "نوع القاعة وتجهيزاتها"
    override val roomTypeGeneral: String = "قاعة تدريس عادية"
    override val roomTypeLab: String = "مختبر علوم وفيزياء"
    override val roomTypeComputer: String = "قاعة إعلام آلي"
    override val roomTypeHall: String = "مدرج محاضرات"
    override val editRoom: String = "تعديل بيانات القاعة"
    override val deleteRoomConfirm: String = "هل أنت متأكد من حذف هذه القاعة؟"
    override val noRoomsYet: String = "لا توجد قاعات مضافة بعد. أضف قاعة لتنظيم الجداول والحصص."
    override val quickTimeSlots: String = "توقيتات سريعة شائعة"
    override val sessionDetails: String = "تفاصيل الحصة"

    override val selectSubject: String = "اختر المادة"
    override val teacherSubject: String = "المادة التي يدرسها الأستاذ"
    override val studentSubjects: String = "المواد المسجل بها التلميذ"
    override val orAddNewSubject: String = "أو كتابة مادة جديدة لإضافتها مباشرة"
    override val addSubjectPrompt: String = "اكتب اسم المادة ليتم إضافتها تلقائياً للنظام"
    override val subjectCreatedDirectly: String = "تمت إضافة المادة إلى البرنامج مباشرة"
    override val addAnotherSubject: String = "إضافة مادة أخرى للتلميذ"
    override val noSubjectsAvailable: String = "لا توجد مواد مسجلة حالياً، يمكنك كتابة مادة جديدة أدناه"
    override val newSubjectNameInput: String = "اسم المادة الجديدة"
    override val assignSubjectToTeacher: String = "تعيين المادة للأستاذ"
    override val removeSubject: String = "إزالة المادة"
    override val teacherSubjectsPrompt: String = "المواد التي يدرسها الأستاذ (يمكن اختيار أكثر من مادة)"
    override val addAnotherSubjectForTeacher: String = "إسناد مادة أخرى للأستاذ"

    override val subjectsPricingTitle: String = "المواد والتسعيرات والمستويات"
    override val addSubjectLevel: String = "إضافة مادة ومستوى"
    override val subjectLevel: String = "المستوى الدراسي"
    override val subjectPrice: String = "واجب المادة (درهم)"
    override val assignedTeacher: String = "الأستاذ المسند"
    override val selectTeacher: String = "اختر الأستاذ"
    override val noTeacherAssigned: String = "لم يتم تعيين أستاذ بعد"
    override val applyTeacherToAllLevels: String = "تعيين هذا الأستاذ لجميع مستويات هذه المادة"
    override val editSubject: String = "تعديل المادة والتسعيرة"
    override val deleteSubjectConfirm: String = "هل أنت متأكد من حذف هذه المادة؟"
    override val commonLevels: String = "المستويات الشائعة"
    override val commonSubjects: String = "المواد الشائعة"
    override val perMonth: String = "شهرياً"
}

class EnglishStrings : AppStrings {

    override val appName: String = "science.est.center"
    override val welcomeTitle: String = "Welcome to science.est.center"
    override val welcomeSubtitle: String = "Integrated Private School Management & Student Support"
    override val username: String = "Username"
    override val password: String = "Password"
    override val rememberMe: String = "Remember me"
    override val login: String = "Sign In"
    override val loggingIn: String = "Signing in..."
    override val loginError: String = "Sign in failed. Check your username and password."
    override val invalidCredentials: String = "Invalid username or password"
    override val accountDisabled: String = "This account is currently disabled. Please contact school administration."
    override val selectLanguage: String = "Select Language"
    override val logout: String = "Sign Out"
    override val roleAdmin: String = "Administrator"
    override val roleTeacher: String = "Teacher"
    override val roleStudent: String = "Student"

    override val navDashboard: String = "Dashboard"
    override val navUsers: String = "Users"
    override val navTimetable: String = "Timetable"
    override val navFinance: String = "Finance"
    override val navReports: String = "Reports"
    override val navAttendance: String = "Attendance"
    override val navResources: String = "Resources"
    override val navHomework: String = "Homework"
    override val navGrades: String = "Grades"
    override val navChat: String = "Messages"
    override val navAnnouncements: String = "Announcements"
    override val navSettings: String = "Settings"
    override val navGroups: String = "Classes & Subjects"

    override val usersTitle: String = "User Management"
    override val studentsTab: String = "Students"
    override val teachersTab: String = "Teachers"
    override val addUser: String = "Add New User"
    override val addStudent: String = "Add Student"
    override val addTeacher: String = "Add Teacher"
    override val fullName: String = "Full Name"
    override val phone: String = "Phone Number"
    override val autoGeneratePassword: String = "Auto-Generate Password"
    override val credentialsTitle: String = "Login Credentials"
    override val credentialsDesc: String = "Please copy and share these credentials now. For security, passwords will not be displayed again."
    override val copyCredentials: String = "Copy Credentials"
    override val credentialsCopied: String = "Credentials copied to clipboard"
    override val editUser: String = "Edit Account"
    override val deleteUser: String = "Delete Account"
    override val disableUser: String = "Disable Account"
    override val enableUser: String = "Enable Account"
    override val confirmDelete: String = "Are you sure you want to permanently delete this account?"
    override val teacherPermissions: String = "Teacher Permissions"
    override val permPublishResources: String = "Publish Learning Resources"
    override val permEditGrades: String = "Edit Grades"
    override val permSendAnnouncements: String = "Send Announcements"
    override val statusActive: String = "Active"
    override val statusDisabled: String = "Disabled"
    override val searchPlaceholder: String = "Search by name or username..."

    override val groupsTitle: String = "Classes & Groups"
    override val subjectsTitle: String = "Subjects"
    override val roomsTitle: String = "Rooms"
    override val addGroup: String = "Add Class"
    override val addSubject: String = "Add Subject"
    override val addRoom: String = "Add Room"
    override val groupName: String = "Class / Group Name"
    override val subjectName: String = "Subject Name"
    override val teacherName: String = "Teacher Name"
    override val roomName: String = "Room Name / Number"
    override val manualEntry: String = "Manual Entry"
    override val selectFromList: String = "Select from List"
    override val level: String = "Academic Level"
    override val timetableTitle: String = "Weekly Timetable Builder"
    override val addSlot: String = "Add Class Slot"
    override val dayOfWeek: String = "Day"
    override val startTime: String = "Start Time"
    override val endTime: String = "End Time"
    override val conflictDetected: String = "Warning: Timetable conflict detected!"
    override val conflictRoom: String = "Conflict: Room is already booked for another session at this time!"
    override val conflictTeacher: String = "Conflict: Teacher already has another session at this time!"
    override val noConflict: String = "No conflicts detected"
    override val weeklySchedule: String = "Weekly Schedule"
    override val nextClassReminder: String = "Upcoming class reminder"
    override val noScheduleToday: String = "No classes scheduled for today"

    override val monday: String = "Monday"
    override val tuesday: String = "Tuesday"
    override val wednesday: String = "Wednesday"
    override val thursday: String = "Thursday"
    override val friday: String = "Friday"
    override val saturday: String = "Saturday"
    override val sunday: String = "Sunday"

    override val attendanceTitle: String = "Attendance Tracking"
    override val markAttendance: String = "Mark Session Attendance"
    override val present: String = "Present"
    override val absent: String = "Absent"
    override val late: String = "Late"
    override val attendanceSaved: String = "Attendance successfully saved"
    override val attendanceRate: String = "Attendance Rate"
    override val myAttendanceRecord: String = "My Attendance History"
    override val totalSessions: String = "Total Sessions"

    override val resourcesTitle: String = "Learning Resources"
    override val addResource: String = "Add Resource"
    override val resourceTitle: String = "Resource Title"
    override val resourceType: String = "Resource Type"
    override val typeSummary: String = "Lesson Summary"
    override val typeExercise: String = "Exercise Sheet"
    override val typePastExam: String = "Past Exam with Solution"
    override val fileOrUrl: String = "Resource File / Link"
    override val downloadOrOpen: String = "Download / Open Resource"

    override val homeworkTitle: String = "Homework & Assignments"
    override val createHomework: String = "Create Assignment"
    override val deadline: String = "Deadline"
    override val submissions: String = "Student Submissions"
    override val submitHomework: String = "Submit Homework"
    override val yourSubmission: String = "Your Submission"
    override val gradeSubmission: String = "Grade Submission"
    override val score: String = "Grade / Score"
    override val teacherComment: String = "Teacher Feedback"
    override val submittedStatus: String = "Submitted"
    override val notSubmittedStatus: String = "Not submitted yet"

    override val gradesTitle: String = "Student Grades"
    override val addGrade: String = "Record Grade"
    override val gradeType: String = "Grade Type"
    override val typeExam: String = "Exam"
    override val typeQuiz: String = "Quiz"
    override val typeHomeworkGrade: String = "Homework"
    override val gradeValue: String = "Grade Value"
    override val overallAverage: String = "Overall Average"
    override val progressChart: String = "Progress Over Time"

    override val financeTitle: String = "School Finance & Fees"
    override val monthlyFees: String = "Monthly Fees"
    override val paymentStatus: String = "Payment Status"
    override val statusPaid: String = "Paid"
    override val statusPending: String = "Pending"
    override val statusOverdue: String = "Overdue"
    override val totalCollected: String = "Total Collected"
    override val totalOverdue: String = "Total Overdue"
    override val recordPayment: String = "Record Payment"
    override val amount: String = "Amount"
    override val paymentDate: String = "Payment Date"

    override val reportsTitle: String = "Monthly Reports"
    override val attendanceReport: String = "Attendance Report"
    override val gradesReport: String = "Academic Grades Report"
    override val financeReport: String = "Financial Summary Report"
    override val exportPdf: String = "Export as PDF"
    override val exportExcel: String = "Export as Excel"
    override val reportExported: String = "Report generated successfully"

    override val announcementsTitle: String = "Official Announcements"
    override val createAnnouncement: String = "Post Announcement"
    override val announcementTitle: String = "Announcement Title"
    override val announcementBody: String = "Announcement Content"
    override val audienceAll: String = "Everyone"
    override val audienceTeachers: String = "Teachers Only"
    override val audienceStudents: String = "Students Only"
    override val unread: String = "Unread"
    override val deleteAnnouncement: String = "Delete Announcement"
    override val deleteAnnouncementConfirm: String = "Are you sure you want to permanently delete this announcement?"
    override val tapToDownloadImage: String = "Tap to download and view photo"

    override val chatTitle: String = "Messages & Discussions"
    override val privateTab: String = "Direct Messages"
    override val groupsTab: String = "Chat Groups"
    override val qaTab: String = "Subject Q&A"
    override val newGroupChat: String = "New Chat Group"
    override val recordVoice: String = "Record Voice Note"
    override val recording: String = "Recording voice..."
    override val stopAndSend: String = "Stop & Send Audio"
    override val playAudio: String = "Play Audio"
    override val sendImage: String = "Send Photo"
    override val sendDocument: String = "Send Document (PDF or File)"
    override val downloadFile: String = "Download File"
    override val downloadingFile: String = "Downloading & Saving..."
    override val fileDownloaded: String = "File successfully saved to Downloads"
    override val openFile: String = "Open File"
    override val documentLabel: String = "Document"
    override val voiceNoteLabel: String = "Voice Note"
    override val downloadFailed: String = "Failed to download file, please try again"
    override val downloadImage: String = "Save Photo to Phone"
    override val typeMessage: String = "Type a message..."
    override val askQuestion: String = "Ask a Question"
    override val answerQuestion: String = "Post Answer"
    override val replies: String = "Replies"
    override val moderationNotice: String = "Admin Moderation View Active"
    override val deleteMessage: String = "Delete Violating Message"
    override val chatRequestTitle: String = "Private Chat Request"
    override val chatRequestDesc: String = "To protect student privacy, messaging requires approval from the recipient first."
    override val chatRequestPending: String = "Chat request is pending approval... Messages cannot be sent until accepted."
    override val acceptRequest: String = "Accept Chat"
    override val declineRequest: String = "Decline Request"
    override val requestChat: String = "Request Chat"
    override val studentPrivacyNotice: String = "🔒 Protected Privacy: Student contact information is strictly hidden from other students."
    override val onlyRegisteredStudentsNotice: String = "Showing only students enrolled in your subjects and classes."

    override val save: String = "Save"
    override val cancel: String = "Cancel"
    override val delete: String = "Delete"
    override val edit: String = "Edit"
    override val confirm: String = "Confirm"
    override val close: String = "Close"
    override val copy: String = "Copy"
    override val noDataYet: String = "No data yet"
    override val loading: String = "Loading..."
    override val success: String = "Operation completed successfully"
    override val error: String = "An error occurred. Please try again."
    override val settingsTitle: String = "Settings"
    override val firebaseStatus: String = "Firebase Status"
    override val connected: String = "Live Cloud Connected"
    override val disconnected: String = "Awaiting Setup"
    override val setupInstructions: String = "Firebase Connection Guide"
    override val securityRulesTitle: String = "Firestore & Storage Security Rules"
    override val capacitorStepsTitle: String = "Capacitor APK Conversion Steps"
    override val initializeAdminButton: String = "Initialize Admin Account (admin/admin)"
    override val adminInitializedSuccess: String = "Admin account initialized successfully!"

    override val secureAccountTitle: String = "Secure Your Admin Account"
    override val secureAccountSubtitle: String = "You must change your password and provide a real recovery email address to protect this account from permanent lockout."
    override val newPassword: String = "New Password"
    override val confirmPassword: String = "Confirm New Password"
    override val recoveryEmail: String = "Recovery Email Address"
    override val changeUsernameOptional: String = "Change Username (optional, default: admin)"
    override val passwordCannotBeAdmin: String = "Password cannot be 'admin' again"
    override val passwordsDoNotMatch: String = "Passwords do not match"
    override val passwordTooShort: String = "Password must be at least 6 characters"
    override val invalidEmail: String = "Please enter a valid email address"
    override val saveAndContinue: String = "Secure Account & Continue"
    override val forgotPassword: String = "Forgot Password? (Admins)"
    override val forgotPasswordTitle: String = "Admin Password Recovery"
    override val forgotPasswordDesc: String = "Enter your admin username or registered recovery email to receive a password reset link via Firebase"
    override val sendResetLink: String = "Send Reset Link"
    override val resetLinkSent: String = "Password reset link sent to your recovery email successfully!"
    override val manageAdmins: String = "Manage Admins"
    override val adminsTab: String = "Admins"
    override val addAdmin: String = "Add New Admin"
    override val primaryAdmin: String = "Primary Admin"
    override val you: String = "You (current session)"
    override val cannotDeletePrimaryAdmin: String = "The primary admin cannot be disabled or deleted to prevent total lockout."
    override val changePassword: String = "Change Password"
    override val oldPassword: String = "Current Password"
    override val passwordChangedSuccess: String = "Password updated successfully!"

    override val enrollmentsTitle: String = "Subject Enrollments"
    override val enrollStudent: String = "Enroll Student in Subject"
    override val enrolledSubjects: String = "Enrolled Subjects"
    override val monthlyFeeAmount: String = "Monthly Fee"
    override val amountDue: String = "Amount Due"
    override val amountPaidLabel: String = "Amount Paid"
    override val amountRemainingLabel: String = "Amount Remaining"
    override val selectSubjectForPayment: String = "Select Subject to Pay For"
    override val teacherShareLabel: String = "Teacher's Share"
    override val schoolShareLabel: String = "School's Share"
    override val teacherPercentageLabel: String = "Teacher Percentage"
    override val requestedPercentageLabel: String = "Proposed Percentage"
    override val approvedPercentageLabel: String = "Approved Percentage"
    override val setPercentage: String = "Set Teacher Percentage"
    override val requestPercentage: String = "Propose Percentage"
    override val proposePercentage: String = "Submit Percentage Proposal"
    override val statusPartiallyPaid: String = "Partially Paid"
    override val statusUnpaid: String = "Unpaid"
    override val pauseEnrollment: String = "Pause Enrollment"
    override val resumeEnrollment: String = "Resume Enrollment"
    override val enrollmentActive: String = "Active"
    override val enrollmentPaused: String = "Paused"
    override val perTeacherBreakdown: String = "Per Teacher"
    override val perStudentBreakdown: String = "Per Student"
    override val studentsTaught: String = "Students Enrolled"
    override val totalCollectedForSubject: String = "Total Collected"
    override val teacherShareOwed: String = "Teacher Payout"
    override val schoolShareTotal: String = "School Revenue"
    override val searchStudentPlaceholder: String = "Search student by name..."
    override val noEnrolledSubjects: String = "No enrolled subjects for this student"
    override val myFeesTitle: String = "My Fees"
    override val paymentHistoryTitle: String = "Payment History"
    override val teacherFinanceTitle: String = "My Finances"
    override val myEarnedShare: String = "My Earned Share"
    override val myStudentsStatus: String = "My Students' Payment Status"
    override val permViewFinance: String = "View financial earnings & student payment status"

    override val activityLogTitle: String = "Activity & Audit Log"
    override val activityLogSubtitle: String = "Immutable audit trail of financial changes and system events"
    override val auditWarningDialogTitle: String = "Confirm Financial Record Edit"
    override val auditWarningMessage: String = "Security Warning: This change, exact timestamp, and previous vs new values will be permanently logged in the immutable audit trail."
    override val auditReasonNote: String = "Reason / Audit Note (Optional)"
    override val auditReasonPlaceholder: String = "e.g., Typo, corrected amount from 500 to 50"
    override val editFinancialRecord: String = "Edit Financial Record"
    override val confirmAndLog: String = "Confirm & Log to Audit Trail"
    override val filterByCollection: String = "Filter by Collection"
    override val filterByUser: String = "Filter by User"
    override val filterAll: String = "All"
    override val oldValueLabel: String = "Old Value"
    override val newValueLabel: String = "New Value"
    override val recordLabel: String = "Record"
    override val actionLabel: String = "Action"
    override val editGrade: String = "Edit Grade"
    override val editAttendance: String = "Edit Attendance"
    override val editHomework: String = "Edit Assignment"
    override val editResource: String = "Edit Resource"
    override val editAnnouncement: String = "Edit Announcement"
    override val editTimetableSlot: String = "Edit Class Slot"
    override val editedBadge: String = "Edited"
    override val noAuditLogsYet: String = "No activity logs recorded yet"
    override val notificationsTitle: String = "Notifications & Alerts"
    override val notificationsEnabled: String = "Notifications Enabled"
    override val notificationsDisabled: String = "Notifications Disabled"
    override val testNotification: String = "Send Test Notification"
    override val enableNotifications: String = "Enable Notifications"
    override val notificationsDesc: String = "Receive instant phone notifications for new messages and school announcements"
    override val resetAdminButton: String = "Restore & Login as Admin (admin / admin)"
    override val factoryResetTitle: String = "Reset Application & Start Fresh"
    override val factoryResetDesc: String = "Delete all accounts and data to start the app fresh with only default admin"
    override val factoryResetConfirmTitle: String = "Confirm Complete Factory Reset?"
    override val factoryResetConfirmMessage: String = "Warning: All student, teacher, timetable, grade, homework, and chat data will be permanently wiped, keeping only the clean default admin / admin."
    override val factoryResetSuccess: String = "Application reset successfully! You can now log in as admin / admin."
    override val currencySymbol: String = "MAD"
    override val connectNewDatabaseTitle: String = "Connect New Firebase Database"
    override val connectNewDatabaseDesc: String = "Replace current database with your own Firebase project and start fresh"
    override val pasteJsonTab: String = "Paste google-services.json"
    override val manualEntryTab: String = "Manual Entry"
    override val pasteJsonPlaceholder: String = "Paste google-services.json content here..."
    override val applyAndConnectButton: String = "Save & Connect to New Database"
    override val databaseConnectedSuccess: String = "Successfully connected to new Firebase database!"
    override val resetToDefaultDatabase: String = "Reset to Default Database"
    override val searchUsersTitle: String = "Search Student or Teacher"
    override val searchUsersPlaceholder: String = "Search by name, username, or phone..."
    override val allUsersTab: String = "All"
    override val roomCapacity: String = "Capacity"
    override val roomType: String = "Room Type & Equipment"
    override val roomTypeGeneral: String = "Standard Classroom"
    override val roomTypeLab: String = "Science Lab"
    override val roomTypeComputer: String = "Computer Lab"
    override val roomTypeHall: String = "Lecture Hall"
    override val editRoom: String = "Edit Room"
    override val deleteRoomConfirm: String = "Are you sure you want to delete this room?"
    override val noRoomsYet: String = "No rooms added yet. Add rooms to organize classes and schedule."
    override val quickTimeSlots: String = "Common Quick Time Slots"
    override val sessionDetails: String = "Session Details"

    override val selectSubject: String = "Select Subject"
    override val teacherSubject: String = "Subject Taught by Teacher"
    override val studentSubjects: String = "Student's Enrolled Subjects"
    override val orAddNewSubject: String = "Or write a new subject to add directly"
    override val addSubjectPrompt: String = "Type subject name to add it automatically"
    override val subjectCreatedDirectly: String = "Subject added to the system directly"
    override val addAnotherSubject: String = "Add another subject for student"
    override val noSubjectsAvailable: String = "No subjects available yet, type a new subject below"
    override val newSubjectNameInput: String = "New Subject Name"
    override val assignSubjectToTeacher: String = "Assign Subject to Teacher"
    override val removeSubject: String = "Remove Subject"
    override val teacherSubjectsPrompt: String = "Subjects Taught by Teacher (select one or more)"
    override val addAnotherSubjectForTeacher: String = "Assign another subject to teacher"

    override val subjectsPricingTitle: String = "Subjects, Levels & Pricing"
    override val addSubjectLevel: String = "Add Subject & Level"
    override val subjectLevel: String = "Academic Level"
    override val subjectPrice: String = "Monthly Fee (DH)"
    override val assignedTeacher: String = "Assigned Teacher"
    override val selectTeacher: String = "Select Teacher"
    override val noTeacherAssigned: String = "No teacher assigned yet"
    override val applyTeacherToAllLevels: String = "Assign this teacher to all levels of this subject"
    override val editSubject: String = "Edit Subject & Pricing"
    override val deleteSubjectConfirm: String = "Are you sure you want to delete this subject?"
    override val commonLevels: String = "Common Levels"
    override val commonSubjects: String = "Common Subjects"
    override val perMonth: String = "/ month"
}

class FrenchStrings : AppStrings {

    override val appName: String = "science.est.center"
    override val welcomeTitle: String = "Bienvenue sur science.est.center"
    override val welcomeSubtitle: String = "Gestion d'école privée et soutien scolaire intégré"
    override val username: String = "Nom d'utilisateur"
    override val password: String = "Mot de passe"
    override val rememberMe: String = "Se souvenir de moi"
    override val login: String = "Connexion"
    override val loggingIn: String = "Connexion en cours..."
    override val loginError: String = "Échec de connexion. Vérifiez le nom d'utilisateur et le mot de passe."
    override val invalidCredentials: String = "Nom d'utilisateur ou mot de passe incorrect"
    override val accountDisabled: String = "Ce compte est actuellement désactivé. Veuillez contacter l'administration."
    override val selectLanguage: String = "Choisir la langue"
    override val logout: String = "Déconnexion"
    override val roleAdmin: String = "Administrateur"
    override val roleTeacher: String = "Enseignant"
    override val roleStudent: String = "Élève"

    override val navDashboard: String = "Tableau de bord"
    override val navUsers: String = "Utilisateurs"
    override val navTimetable: String = "Emploi du temps"
    override val navFinance: String = "Finances"
    override val navReports: String = "Rapports"
    override val navAttendance: String = "Présences"
    override val navResources: String = "Ressources"
    override val navHomework: String = "Devoirs"
    override val navGrades: String = "Notes"
    override val navChat: String = "Messagerie"
    override val navAnnouncements: String = "Annonces"
    override val navSettings: String = "Paramètres"
    override val navGroups: String = "Classes & Matières"

    override val usersTitle: String = "Gestion des utilisateurs"
    override val studentsTab: String = "Élèves"
    override val teachersTab: String = "Enseignants"
    override val addUser: String = "Ajouter un utilisateur"
    override val addStudent: String = "Ajouter un élève"
    override val addTeacher: String = "Ajouter un enseignant"
    override val fullName: String = "Nom complet"
    override val phone: String = "Téléphone"
    override val autoGeneratePassword: String = "Générer un mot de passe"
    override val credentialsTitle: String = "Identifiants de connexion"
    override val credentialsDesc: String = "Veuillez copier et transmettre ces identifiants dès maintenant. Le mot de passe ne sera plus affiché."
    override val copyCredentials: String = "Copier les identifiants"
    override val credentialsCopied: String = "Identifiants copiés dans le presse-papiers"
    override val editUser: String = "Modifier le compte"
    override val deleteUser: String = "Supprimer le compte"
    override val disableUser: String = "Désactiver le compte"
    override val enableUser: String = "Activer le compte"
    override val confirmDelete: String = "Voulez-vous vraiment supprimer définitivement ce compte ?"
    override val teacherPermissions: String = "Permissions de l'enseignant"
    override val permPublishResources: String = "Publier des ressources"
    override val permEditGrades: String = "Modifier les notes"
    override val permSendAnnouncements: String = "Envoyer des annonces"
    override val statusActive: String = "Actif"
    override val statusDisabled: String = "Désactivé"
    override val searchPlaceholder: String = "Rechercher par nom ou identifiant..."

    override val groupsTitle: String = "Classes et Groupes"
    override val subjectsTitle: String = "Matières"
    override val roomsTitle: String = "Salles de cours"
    override val addGroup: String = "Ajouter une classe"
    override val addSubject: String = "Ajouter une matière"
    override val addRoom: String = "Ajouter une salle"
    override val groupName: String = "Nom de la classe"
    override val subjectName: String = "Nom de la matière"
    override val teacherName: String = "Nom de l'enseignant"
    override val roomName: String = "Numéro de salle"
    override val manualEntry: String = "Saisie manuelle"
    override val selectFromList: String = "Choisir dans la liste"
    override val level: String = "Niveau scolaire"
    override val timetableTitle: String = "Générateur d'emploi du temps"
    override val addSlot: String = "Ajouter un créneau"
    override val dayOfWeek: String = "Jour"
    override val startTime: String = "Heure de début"
    override val endTime: String = "Heure de fin"
    override val conflictDetected: String = "Alerte : Conflit d'horaire détecté !"
    override val conflictRoom: String = "Conflit : La salle est déjà occupée sur ce créneau !"
    override val conflictTeacher: String = "Conflit : L'enseignant a déjà un cours sur ce créneau !"
    override val noConflict: String = "Aucun conflit détecté"
    override val weeklySchedule: String = "Emploi du temps hebdomadaire"
    override val nextClassReminder: String = "Rappel du prochain cours"
    override val noScheduleToday: String = "Aucun cours prévu aujourd'hui"

    override val monday: String = "Lundi"
    override val tuesday: String = "Mardi"
    override val wednesday: String = "Mercredi"
    override val thursday: String = "Jeudi"
    override val friday: String = "Vendredi"
    override val saturday: String = "Samedi"
    override val sunday: String = "Dimanche"

    override val attendanceTitle: String = "Feuille de présence"
    override val markAttendance: String = "Faire l'appel"
    override val present: String = "Présent"
    override val absent: String = "Absent"
    override val late: String = "En retard"
    override val attendanceSaved: String = "Présences enregistrées avec succès"
    override val attendanceRate: String = "Taux de présence"
    override val myAttendanceRecord: String = "Mon historique de présence"
    override val totalSessions: String = "Total des séances"

    override val resourcesTitle: String = "Ressources pédagogiques"
    override val addResource: String = "Ajouter une ressource"
    override val resourceTitle: String = "Titre de la ressource"
    override val resourceType: String = "Type de ressource"
    override val typeSummary: String = "Fiche de cours / Résumé"
    override val typeExercise: String = "Série d'exercices"
    override val typePastExam: String = "Ancien examen avec corrigé"
    override val fileOrUrl: String = "Fichier ou lien web"
    override val downloadOrOpen: String = "Télécharger / Consulter"

    override val homeworkTitle: String = "Devoirs et travaux"
    override val createHomework: String = "Créer un devoir"
    override val deadline: String = "Date limite de remise"
    override val submissions: String = "Travaux rendus"
    override val submitHomework: String = "Rendre le devoir"
    override val yourSubmission: String = "Votre rendu"
    override val gradeSubmission: String = "Évaluer le rendu"
    override val score: String = "Note / Barème"
    override val teacherComment: String = "Commentaire de l'enseignant"
    override val submittedStatus: String = "Rendu"
    override val notSubmittedStatus: String = "Non rendu"

    override val gradesTitle: String = "Notes et évaluations"
    override val addGrade: String = "Saisir une note"
    override val gradeType: String = "Type d'évaluation"
    override val typeExam: String = "Examen"
    override val typeQuiz: String = "Interrogation"
    override val typeHomeworkGrade: String = "Devoir maison"
    override val gradeValue: String = "Note"
    override val overallAverage: String = "Moyenne générale"
    override val progressChart: String = "Courbe de progression"

    override val financeTitle: String = "Finances & Frais de scolarité"
    override val monthlyFees: String = "Frais mensuels"
    override val paymentStatus: String = "Statut de paiement"
    override val statusPaid: String = "Payé"
    override val statusPending: String = "En attente"
    override val statusOverdue: String = "En retard"
    override val totalCollected: String = "Total encaissé"
    override val totalOverdue: String = "Total impayé"
    override val recordPayment: String = "Enregistrer un paiement"
    override val amount: String = "Montant"
    override val paymentDate: String = "Date du versement"

    override val reportsTitle: String = "Rapports d'activité"
    override val attendanceReport: String = "Rapport d'assiduité"
    override val gradesReport: String = "Relevé des notes"
    override val financeReport: String = "Bilan financier"
    override val exportPdf: String = "Exporter en PDF"
    override val exportExcel: String = "Exporter en Excel"
    override val reportExported: String = "Rapport généré avec succès"

    override val announcementsTitle: String = "Annonces officielles"
    override val createAnnouncement: String = "Publier une annonce"
    override val announcementTitle: String = "Titre de l'annonce"
    override val announcementBody: String = "Contenu de l'annonce"
    override val audienceAll: String = "Tout le monde"
    override val audienceTeachers: String = "Enseignants uniquement"
    override val audienceStudents: String = "Élèves uniquement"
    override val unread: String = "Non lu"
    override val deleteAnnouncement: String = "Supprimer l'annonce"
    override val deleteAnnouncementConfirm: String = "Voulez-vous vraiment supprimer cette annonce ?"
    override val tapToDownloadImage: String = "Appuyez pour télécharger et afficher la photo"

    override val chatTitle: String = "Messagerie & Échanges"
    override val privateTab: String = "Messages privés"
    override val groupsTab: String = "Groupes de discussion"
    override val qaTab: String = "Espace Questions / Réponses"
    override val newGroupChat: String = "Créer un groupe de discussion"
    override val recordVoice: String = "Enregistrer un message vocal"
    override val recording: String = "Enregistrement en cours..."
    override val stopAndSend: String = "Arrêter et envoyer"
    override val playAudio: String = "Écouter l'audio"
    override val sendImage: String = "Envoyer une photo"
    override val sendDocument: String = "Envoyer un document (PDF ou fichier)"
    override val downloadFile: String = "Télécharger le fichier"
    override val downloadingFile: String = "Téléchargement en cours..."
    override val fileDownloaded: String = "Fichier enregistré dans les Téléchargements"
    override val openFile: String = "Ouvrir le fichier"
    override val documentLabel: String = "Document"
    override val voiceNoteLabel: String = "Message vocal"
    override val downloadFailed: String = "Échec du téléchargement, veuillez réessayer"
    override val downloadImage: String = "Enregistrer la photo"
    override val typeMessage: String = "Écrivez votre message..."
    override val askQuestion: String = "Poser une question"
    override val answerQuestion: String = "Répondre"
    override val replies: String = "Réponses"
    override val moderationNotice: String = "Modération active par l'administration"
    override val deleteMessage: String = "Supprimer le message"
    override val chatRequestTitle: String = "Demande de discussion privée"
    override val chatRequestDesc: String = "Pour protéger la confidentialité des élèves, l'envoi de messages nécessite l'accord du destinataire."
    override val chatRequestPending: String = "Demande de discussion en attente... Impossible d'envoyer des messages avant acceptation."
    override val acceptRequest: String = "Accepter"
    override val declineRequest: String = "Refuser"
    override val requestChat: String = "Demander une discussion"
    override val studentPrivacyNotice: String = "🔒 Confidentialité protégée : Les coordonnées des élèves sont strictement masquées aux autres élèves."
    override val onlyRegisteredStudentsNotice: String = "Seuls les élèves inscrits dans vos matières et classes sont affichés."

    override val save: String = "Enregistrer"
    override val cancel: String = "Annuler"
    override val delete: String = "Supprimer"
    override val edit: String = "Modifier"
    override val confirm: String = "Confirmer"
    override val close: String = "Fermer"
    override val copy: String = "Copier"
    override val noDataYet: String = "Aucune donnée enregistrée pour le moment"
    override val loading: String = "Chargement en cours..."
    override val success: String = "Opération réussie"
    override val error: String = "Une erreur est survenue. Veuillez réessayer."
    override val settingsTitle: String = "Paramètres"
    override val firebaseStatus: String = "État de la connexion Firebase"
    override val connected: String = "Connecté au Cloud Firebase"
    override val disconnected: String = "En attente de configuration"
    override val setupInstructions: String = "Guide de connexion Firebase"
    override val securityRulesTitle: String = "Règles de sécurité Firestore et Storage"
    override val capacitorStepsTitle: String = "Étapes de conversion Capacitor APK"
    override val initializeAdminButton: String = "Initialiser le compte administrateur (admin/admin)"
    override val adminInitializedSuccess: String = "Compte administrateur initialisé avec succès !"

    override val secureAccountTitle: String = "Sécuriser votre compte administrateur"
    override val secureAccountSubtitle: String = "Vous devez changer votre mot de passe et définir une adresse e-mail de récupération pour éviter tout blocage permanent."
    override val newPassword: String = "Nouveau mot de passe"
    override val confirmPassword: String = "Confirmer le nouveau mot de passe"
    override val recoveryEmail: String = "E-mail de récupération"
    override val changeUsernameOptional: String = "Changer le nom d'utilisateur (facultatif, par défaut : admin)"
    override val passwordCannotBeAdmin: String = "Le mot de passe ne peut pas être 'admin' à nouveau"
    override val passwordsDoNotMatch: String = "Les mots de passe ne correspondent pas"
    override val passwordTooShort: String = "Le mot de passe doit comporter au moins 6 caractères"
    override val invalidEmail: String = "Veuillez saisir une adresse e-mail valide"
    override val saveAndContinue: String = "Sécuriser le compte et continuer"
    override val forgotPassword: String = "Mot de passe oublié ? (Admins)"
    override val forgotPasswordTitle: String = "Récupération du mot de passe"
    override val forgotPasswordDesc: String = "Entrez votre nom d'utilisateur ou e-mail de récupération pour recevoir un lien de réinitialisation"
    override val sendResetLink: String = "Envoyer le lien de réinitialisation"
    override val resetLinkSent: String = "Lien de réinitialisation envoyé avec succès à votre e-mail de récupération !"
    override val manageAdmins: String = "Gérer les administrateurs"
    override val adminsTab: String = "Admins"
    override val addAdmin: String = "Ajouter un administrateur"
    override val primaryAdmin: String = "Administrateur principal"
    override val you: String = "Vous (session actuelle)"
    override val cannotDeletePrimaryAdmin: String = "L'administrateur principal ne peut être ni supprimé ni désactivé."
    override val changePassword: String = "Changer le mot de passe"
    override val oldPassword: String = "Mot de passe actuel"
    override val passwordChangedSuccess: String = "Mot de passe mis à jour avec succès !"

    override val enrollmentsTitle: String = "Inscriptions aux Matières"
    override val enrollStudent: String = "Inscrire l'étudiant à une matière"
    override val enrolledSubjects: String = "Matières inscrites"
    override val monthlyFeeAmount: String = "Frais mensuels"
    override val amountDue: String = "Montant dû"
    override val amountPaidLabel: String = "Montant payé"
    override val amountRemainingLabel: String = "Montant restant"
    override val selectSubjectForPayment: String = "Choisir la matière à régler"
    override val teacherShareLabel: String = "Part de l'enseignant"
    override val schoolShareLabel: String = "Part de l'école"
    override val teacherPercentageLabel: String = "Pourcentage enseignant"
    override val requestedPercentageLabel: String = "Pourcentage proposé"
    override val approvedPercentageLabel: String = "Pourcentage approuvé"
    override val setPercentage: String = "Définir le pourcentage enseignant"
    override val requestPercentage: String = "Proposer un pourcentage"
    override val proposePercentage: String = "Soumettre la proposition"
    override val statusPartiallyPaid: String = "Partiellement payé"
    override val statusUnpaid: String = "Non payé"
    override val pauseEnrollment: String = "Mettre en pause l'inscription"
    override val resumeEnrollment: String = "Reprendre l'inscription"
    override val enrollmentActive: String = "Actif"
    override val enrollmentPaused: String = "En pause"
    override val perTeacherBreakdown: String = "Par Enseignant"
    override val perStudentBreakdown: String = "Par Étudiant"
    override val studentsTaught: String = "Étudiants inscrits"
    override val totalCollectedForSubject: String = "Total collecté"
    override val teacherShareOwed: String = "Part enseignant due"
    override val schoolShareTotal: String = "Revenu de l'école"
    override val searchStudentPlaceholder: String = "Rechercher un étudiant par nom..."
    override val noEnrolledSubjects: String = "Aucune matière inscrite pour cet étudiant"
    override val myFeesTitle: String = "Mes Frais de Scolarité"
    override val paymentHistoryTitle: String = "Historique des Paiements"
    override val teacherFinanceTitle: String = "Mes Finances"
    override val myEarnedShare: String = "Ma part accumulée"
    override val myStudentsStatus: String = "Statut de paiement de mes étudiants"
    override val permViewFinance: String = "Consulter ses gains et le statut de paiement des étudiants"

    override val activityLogTitle: String = "Journal d'Activité et d'Audit"
    override val activityLogSubtitle: String = "Historique immuable des modifications financières et du système"
    override val auditWarningDialogTitle: String = "Confirmer la modification financière"
    override val auditWarningMessage: String = "Avertissement de sécurité : Cette modification, l'horodatage exact, ainsi que l'ancienne et nouvelle valeur seront enregistrés dans le journal d'audit immuable."
    override val auditReasonNote: String = "Motif de la modification (Optionnel)"
    override val auditReasonPlaceholder: String = "ex. Correction d'erreur de saisie de 500 à 50"
    override val editFinancialRecord: String = "Modifier l'enregistrement financier"
    override val confirmAndLog: String = "Confirmer et consigner dans l'audit"
    override val filterByCollection: String = "Filtrer par type"
    override val filterByUser: String = "Filtrer par utilisateur"
    override val filterAll: String = "Tous"
    override val oldValueLabel: String = "Ancienne valeur"
    override val newValueLabel: String = "Nouvelle valeur"
    override val recordLabel: String = "Enregistrement"
    override val actionLabel: String = "Action"
    override val editGrade: String = "Modifier la note"
    override val editAttendance: String = "Modifier la présence"
    override val editHomework: String = "Modifier le devoir"
    override val editResource: String = "Modifier la ressource"
    override val editAnnouncement: String = "Modifier l'annonce"
    override val editTimetableSlot: String = "Modifier le cours"
    override val editedBadge: String = "Modifié"
    override val noAuditLogsYet: String = "Aucun journal d'activité pour l'instant"
    override val notificationsTitle: String = "Notifications et Alertes"
    override val notificationsEnabled: String = "Notifications activées"
    override val notificationsDisabled: String = "Notifications désactivées"
    override val testNotification: String = "Tester la notification"
    override val enableNotifications: String = "Activer les notifications"
    override val notificationsDesc: String = "Recevez des notifications instantanées sur votre téléphone pour les messages et annonces scolaires"
    override val resetAdminButton: String = "Restaurer et se connecter Admin (admin / admin)"
    override val factoryResetTitle: String = "Réinitialiser et repartir à zéro"
    override val factoryResetDesc: String = "Supprimer tous les comptes et données pour redémarrer l'application à zéro avec admin par défaut"
    override val factoryResetConfirmTitle: String = "Confirmer la réinitialisation complète ?"
    override val factoryResetConfirmMessage: String = "Attention : toutes les données des élèves, enseignants, horaires, notes, devoirs et messages seront définitivement supprimées, ne laissant que le compte admin / admin."
    override val factoryResetSuccess: String = "Application réinitialisée avec succès ! Vous pouvez maintenant vous connecter en tant que admin / admin."
    override val currencySymbol: String = "DH"
    override val connectNewDatabaseTitle: String = "Connecter une nouvelle base Firebase"
    override val connectNewDatabaseDesc: String = "Remplacer la base actuelle par votre propre projet Firebase et repartir à zéro"
    override val pasteJsonTab: String = "Coller google-services.json"
    override val manualEntryTab: String = "Saisie manuelle"
    override val pasteJsonPlaceholder: String = "Collez le contenu de google-services.json ici..."
    override val applyAndConnectButton: String = "Enregistrer et connecter la nouvelle base"
    override val databaseConnectedSuccess: String = "Connexion à la nouvelle base Firebase réussie !"
    override val resetToDefaultDatabase: String = "Revenir à la base par défaut"
    override val searchUsersTitle: String = "Rechercher un élève ou enseignant"
    override val searchUsersPlaceholder: String = "Rechercher par nom, identifiant ou téléphone..."
    override val allUsersTab: String = "Tous"
    override val roomCapacity: String = "Capacité"
    override val roomType: String = "Type de salle et équipements"
    override val roomTypeGeneral: String = "Salle de classe standard"
    override val roomTypeLab: String = "Laboratoire de sciences"
    override val roomTypeComputer: String = "Salle informatique"
    override val roomTypeHall: String = "Amphithéâtre"
    override val editRoom: String = "Modifier la salle"
    override val deleteRoomConfirm: String = "Voulez-vous vraiment supprimer cette salle ?"
    override val noRoomsYet: String = "Aucune salle ajoutée pour l'instant. Ajoutez des salles pour organiser l'emploi du temps."
    override val quickTimeSlots: String = "Créneaux horaires rapides"
    override val sessionDetails: String = "Détails de la séance"

    override val selectSubject: String = "Sélectionner la matière"
    override val teacherSubject: String = "Matière enseignée par le professeur"
    override val studentSubjects: String = "Matières de l'élève"
    override val orAddNewSubject: String = "Ou écrire une nouvelle matière à ajouter directement"
    override val addSubjectPrompt: String = "Tapez le nom de la matière pour l'ajouter automatiquement"
    override val subjectCreatedDirectly: String = "Matière ajoutée directement au programme"
    override val addAnotherSubject: String = "Ajouter une autre matière pour l'élève"
    override val noSubjectsAvailable: String = "Aucune matière disponible, tapez une nouvelle matière ci-dessous"
    override val newSubjectNameInput: String = "Nom de la nouvelle matière"
    override val assignSubjectToTeacher: String = "Attribuer la matière au professeur"
    override val removeSubject: String = "Supprimer la matière"
    override val teacherSubjectsPrompt: String = "Matières enseignées (plusieurs choix possibles)"
    override val addAnotherSubjectForTeacher: String = "Attribuer une autre matière au professeur"

    override val subjectsPricingTitle: String = "Matières, Niveaux et Tarifs"
    override val addSubjectLevel: String = "Ajouter une matière et niveau"
    override val subjectLevel: String = "Niveau scolaire"
    override val subjectPrice: String = "Tarif mensuel (DH)"
    override val assignedTeacher: String = "Enseignant assigné"
    override val selectTeacher: String = "Sélectionner l'enseignant"
    override val noTeacherAssigned: String = "Aucun enseignant assigné"
    override val applyTeacherToAllLevels: String = "Assigner cet enseignant à tous les niveaux de cette matière"
    override val editSubject: String = "Modifier la matière et le tarif"
    override val deleteSubjectConfirm: String = "Voulez-vous supprimer cette matière ?"
    override val commonLevels: String = "Niveaux courants"
    override val commonSubjects: String = "Matières courantes"
    override val perMonth: String = "/ mois"
}

object Translations {
    val Arabic: AppStrings = ArabicStrings()
    val English: AppStrings = EnglishStrings()
    val French: AppStrings = FrenchStrings()

    fun get(language: AppLanguage): AppStrings {
        return when (language) {
            AppLanguage.ARABIC -> Arabic
            AppLanguage.ENGLISH -> English
            AppLanguage.FRENCH -> French
        }
    }
}
