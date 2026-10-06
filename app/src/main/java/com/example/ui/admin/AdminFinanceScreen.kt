package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.firebase.FirebaseManager
import com.example.data.model.*
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatCard
import com.example.ui.common.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFinanceScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onNavigateToActivityLog: () -> Unit = {}
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: Students, 2: Teachers

    val payments by firebaseManager.observePayments().collectAsState(initial = emptyList())
    val enrollments by firebaseManager.observeEnrollments().collectAsState(initial = emptyList())
    val students by firebaseManager.observeUsers(Role.STUDENT).collectAsState(initial = emptyList())
    val teachers by firebaseManager.observeUsers(Role.TEACHER).collectAsState(initial = emptyList())
    val subjects by firebaseManager.observeSubjects().collectAsState(initial = emptyList())
    val teacherShares by firebaseManager.observeTeacherShares().collectAsState(initial = emptyList())

    // Dialog states
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var showEnrollStudentDialog by remember { mutableStateOf(false) }
    var preselectedStudentForPayment by remember { mutableStateOf<SchoolUser?>(null) }
    var preselectedEnrollmentForPayment by remember { mutableStateOf<Enrollment?>(null) }

    // Audit Edit Dialog states
    var editingPayment by remember { mutableStateOf<PaymentRecord?>(null) }
    var editingEnrollment by remember { mutableStateOf<Enrollment?>(null) }
    var editingTeacherShare by remember { mutableStateOf<TeacherSubjectShare?>(null) }

    // Summary calculations
    val totalCollected = remember(payments) {
        payments.sumOf { it.amount }
    }
    val totalSchoolShare = remember(payments) {
        payments.sumOf { it.schoolShare }
    }
    val totalTeacherPayouts = remember(payments) {
        payments.sumOf { it.teacherShare }
    }
    val totalRemainingFees = remember(enrollments) {
        enrollments.filter { it.status == "active" }.sumOf { it.amountRemaining }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.financeTitle,
                subtitle = strings.monthlyFees,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToActivityLog,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.HistoryEdu,
                            contentDescription = strings.activityLogTitle,
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { showEnrollStudentDialog = true },
                        modifier = Modifier.testTag("btn_enroll_student")
                    ) {
                        Icon(Icons.Default.PersonAddAlt1, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.enrollStudent)
                    }
                    ExtendedFloatingActionButton(
                        onClick = {
                            preselectedStudentForPayment = null
                            preselectedEnrollmentForPayment = null
                            showRecordPaymentDialog = true
                        },
                        containerColor = SchoolPrimary,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.AddCard, contentDescription = null) },
                        text = { Text(strings.recordPayment) },
                        modifier = Modifier.testTag("fab_record_payment")
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SchoolPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(strings.navFinance, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("tab_finance_overview")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(strings.perStudentBreakdown, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("tab_finance_students")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(strings.perTeacherBreakdown, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.School, contentDescription = null) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("tab_finance_teachers")
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> FinanceOverviewTab(
                    totalCollected = totalCollected,
                    totalSchoolShare = totalSchoolShare,
                    totalTeacherPayouts = totalTeacherPayouts,
                    totalRemainingFees = totalRemainingFees,
                    payments = payments,
                    strings = strings,
                    onEditPayment = { editingPayment = it }
                )
                1 -> FinanceStudentsTab(
                    students = students,
                    enrollments = enrollments,
                    payments = payments,
                    strings = strings,
                    onRecordPaymentForEnrollment = { student, enroll ->
                        preselectedStudentForPayment = student
                        preselectedEnrollmentForPayment = enroll
                        showRecordPaymentDialog = true
                    },
                    onEnrollStudent = { showEnrollStudentDialog = true },
                    onEditEnrollmentFee = { editingEnrollment = it },
                    onToggleEnrollmentStatus = { enroll ->
                        val nextStatus = if (enroll.status == "active") "paused" else "active"
                        scope.launch {
                            val res = firebaseManager.updateEnrollmentWithAudit(
                                enrollment = enroll.copy(status = nextStatus),
                                oldEnrollment = enroll,
                                note = "Status toggled to $nextStatus"
                            )
                            if (res.isFailure) {
                                Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                2 -> FinanceTeachersTab(
                    teachers = teachers,
                    subjects = subjects,
                    teacherShares = teacherShares,
                    enrollments = enrollments,
                    payments = payments,
                    strings = strings,
                    onEditPercentage = { editingTeacherShare = it }
                )
            }
        }
    }

    // Record Payment Dialog
    if (showRecordPaymentDialog) {
        MultiSubjectRecordPaymentDialog(
            strings = strings,
            students = students,
            enrollments = enrollments,
            teacherShares = teacherShares,
            preselectedStudent = preselectedStudentForPayment,
            preselectedEnrollment = preselectedEnrollmentForPayment,
            onDismiss = {
                showRecordPaymentDialog = false
                preselectedStudentForPayment = null
                preselectedEnrollmentForPayment = null
            },
            onSave = { studentId, studentName, enroll, amount, approvedPct, month, date, note ->
                scope.launch {
                    val res = firebaseManager.recordMultiSubjectPayment(
                        studentId = studentId,
                        studentName = studentName,
                        enrollmentId = enroll.id,
                        subjectId = enroll.subjectId,
                        subjectName = enroll.subjectName,
                        teacherId = enroll.teacherId,
                        teacherName = enroll.teacherName,
                        amount = amount,
                        approvedPercentage = approvedPct,
                        month = month,
                        date = date,
                        notes = note
                    )
                    if (res.isSuccess) {
                        Toast.makeText(context, strings.save, Toast.LENGTH_SHORT).show()
                        showRecordPaymentDialog = false
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Enroll Student Dialog
    if (showEnrollStudentDialog) {
        EnrollStudentDialog(
            strings = strings,
            students = students,
            teachers = teachers,
            subjects = subjects,
            onDismiss = { showEnrollStudentDialog = false },
            onSave = { enrollment ->
                scope.launch {
                    val res = firebaseManager.createEnrollment(enrollment)
                    if (res.isSuccess) {
                        val student = students.firstOrNull { it.id == enrollment.studentId }
                        if (student != null && !student.subjectIds.contains(enrollment.subjectId)) {
                            firebaseManager.updateUser(student.copy(subjectIds = student.subjectIds + enrollment.subjectId))
                        }
                        Toast.makeText(context, strings.save, Toast.LENGTH_SHORT).show()
                        showEnrollStudentDialog = false
                    } else {
                        Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Financial Audit Edit Confirmation Dialogs
    editingPayment?.let { p ->
        EditPaymentAuditDialog(
            payment = p,
            strings = strings,
            onDismiss = { editingPayment = null },
            onConfirm = { updatedAmount, note ->
                scope.launch {
                    val res = firebaseManager.updatePaymentWithAudit(
                        updatedPayment = p.copy(amount = updatedAmount),
                        oldPayment = p,
                        note = note
                    )
                    if (res.isSuccess) {
                        Toast.makeText(context, strings.save, Toast.LENGTH_SHORT).show()
                        editingPayment = null
                    } else {
                        Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    editingEnrollment?.let { enroll ->
        EditEnrollmentFeeAuditDialog(
            enrollment = enroll,
            strings = strings,
            onDismiss = { editingEnrollment = null },
            onConfirm = { newFee, note ->
                scope.launch {
                    val newRemaining = (newFee - enroll.amountPaid).coerceAtLeast(0.0)
                    val res = firebaseManager.updateEnrollmentWithAudit(
                        enrollment = enroll.copy(monthlyFee = newFee, amountRemaining = newRemaining),
                        oldEnrollment = enroll,
                        note = note
                    )
                    if (res.isSuccess) {
                        Toast.makeText(context, strings.save, Toast.LENGTH_SHORT).show()
                        editingEnrollment = null
                    } else {
                        Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    editingTeacherShare?.let { share ->
        EditTeacherShareAuditDialog(
            share = share,
            strings = strings,
            onDismiss = { editingTeacherShare = null },
            onConfirm = { approvedPct, note ->
                scope.launch {
                    val res = firebaseManager.updateTeacherShareWithAudit(
                        share = share.copy(approvedPercentage = approvedPct),
                        oldShare = share,
                        note = note
                    )
                    if (res.isSuccess) {
                        Toast.makeText(context, strings.save, Toast.LENGTH_SHORT).show()
                        editingTeacherShare = null
                    } else {
                        Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

// ----------------------------------------------------
// TAB 0: OVERVIEW & TOTALS
// ----------------------------------------------------

@Composable
fun FinanceOverviewTab(
    totalCollected: Double,
    totalSchoolShare: Double,
    totalTeacherPayouts: Double,
    totalRemainingFees: Double,
    payments: List<PaymentRecord>,
    strings: com.example.localization.AppStrings,
    onEditPayment: (PaymentRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Stats Row 1: Collected vs Remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = strings.totalCollected,
                    value = "${totalCollected.toInt()} ${strings.currencySymbol}",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = strings.amountRemainingLabel,
                    value = "${totalRemainingFees.toInt()} ${strings.currencySymbol}",
                    icon = Icons.Default.Warning,
                    color = SchoolAccentRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // Stats Row 2: School Revenue vs Teacher Payouts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = strings.schoolShareTotal,
                    value = "${totalSchoolShare.toInt()} ${strings.currencySymbol}",
                    icon = Icons.Default.AccountBalance,
                    color = SchoolPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = strings.teacherShareOwed,
                    value = "${totalTeacherPayouts.toInt()} ${strings.currencySymbol}",
                    icon = Icons.Default.School,
                    color = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // Visual Revenue Distribution Card
            val totalRevenue = (totalSchoolShare + totalTeacherPayouts).coerceAtLeast(1.0)
            val schoolRatio = (totalSchoolShare / totalRevenue).toFloat().coerceIn(0f, 1f)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.totalCollected,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Proportional Split Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                    ) {
                        if (schoolRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(schoolRatio)
                                    .background(SchoolPrimary)
                            )
                        }
                        if (1f - schoolRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f - schoolRatio)
                                    .background(SchoolAccentRed)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SchoolPrimary))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${strings.schoolShareLabel} (${(schoolRatio * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = SchoolTextSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SchoolAccentRed))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${strings.teacherShareLabel} (${((1f - schoolRatio) * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = SchoolTextSecondary
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = strings.paymentHistoryTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (payments.isEmpty()) {
            item {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.ReceiptLong
                )
            }
        } else {
            items(payments, key = { it.id }) { payment ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = payment.studentName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (payment.subjectName.isNotBlank()) {
                                Text(
                                    text = "${payment.subjectName} • ${payment.teacherName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SchoolPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${payment.date} • ${payment.month}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SchoolTextSecondary
                            )
                            // Admin-only breakdown
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${strings.schoolShareLabel}: ${payment.schoolShare.toInt()} ${strings.currencySymbol}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SchoolPrimary
                                )
                                Text(
                                    text = "${strings.teacherShareLabel}: ${payment.teacherShare.toInt()} ${strings.currencySymbol} (${payment.teacherPercentage.toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${payment.amount.toInt()} ${strings.currencySymbol}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = SchoolPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            IconButton(
                                onClick = { onEditPayment(payment) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = strings.edit,
                                    tint = SchoolTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 1: PER-STUDENT BREAKDOWN & RECORD PAYMENT
// ----------------------------------------------------

@Composable
fun FinanceStudentsTab(
    students: List<SchoolUser>,
    enrollments: List<Enrollment>,
    payments: List<PaymentRecord>,
    strings: com.example.localization.AppStrings,
    onRecordPaymentForEnrollment: (SchoolUser, Enrollment) -> Unit,
    onEnrollStudent: () -> Unit,
    onEditEnrollmentFee: (Enrollment) -> Unit,
    onToggleEnrollmentStatus: (Enrollment) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStudentId by remember { mutableStateOf<String?>(null) }

    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else students.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(strings.searchStudentPlaceholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("input_search_student_finance")
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filteredStudents, key = { it.id }) { student ->
                val studentEnrollments = enrollments.filter { it.studentId == student.id }
                val isExpanded = selectedStudentId == student.id

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedStudentId = if (isExpanded) null else student.id
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "@${student.username} • ${studentEnrollments.size} ${strings.enrolledSubjects}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SchoolTextSecondary
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SchoolTextSecondary
                            )
                        }

                        // Expanded View: Multi-subject line items
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = SchoolOutline)
                            Spacer(modifier = Modifier.height(10.dp))

                            if (studentEnrollments.isEmpty()) {
                                Text(
                                    text = strings.noEnrolledSubjects,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                studentEnrollments.forEach { enroll ->
                                    val isPaid = enroll.amountRemaining <= 0.0
                                    val isPartial = enroll.amountPaid > 0.0 && enroll.amountRemaining > 0.0
                                    val statusColor = when {
                                        enroll.status == "paused" -> Color.Gray
                                        isPaid -> Color(0xFF16A34A)
                                        isPartial -> SchoolPrimary
                                        else -> SchoolAccentRed
                                    }

                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (enroll.status == "paused") Color(0xFFF1F5F9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = enroll.subjectName,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.titleSmall
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        StatusBadge(
                                                            text = when {
                                                                enroll.status == "paused" -> strings.enrollmentPaused
                                                                isPaid -> strings.statusPaid
                                                                isPartial -> strings.statusPartiallyPaid
                                                                else -> strings.statusUnpaid
                                                            },
                                                            color = statusColor
                                                        )
                                                    }
                                                    Text(
                                                        text = "${strings.roleTeacher}: ${enroll.teacherName}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = SchoolTextSecondary
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { onEditEnrollmentFee(enroll) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = strings.edit, tint = SchoolTextSecondary, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            // Running balance row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${strings.monthlyFeeAmount}: ${enroll.monthlyFee.toInt()} ${strings.currencySymbol}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "${strings.amountPaidLabel}: ${enroll.amountPaid.toInt()} ${strings.currencySymbol}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = SchoolPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${strings.amountRemainingLabel}: ${enroll.amountRemaining.toInt()} ${strings.currencySymbol}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (enroll.amountRemaining > 0) SchoolAccentRed else Color(0xFF16A34A),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            // Actions Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = { onToggleEnrollmentStatus(enroll) }
                                                ) {
                                                    Text(
                                                        if (enroll.status == "active") strings.pauseEnrollment else strings.resumeEnrollment,
                                                        color = if (enroll.status == "active") Color(0xFFD97706) else Color(0xFF16A34A),
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Button(
                                                    onClick = { onRecordPaymentForEnrollment(student, enroll) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = SchoolPrimary),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(strings.recordPayment, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 2: PER-TEACHER BREAKDOWN & PERCENTAGE AGREEMENTS
// ----------------------------------------------------

@Composable
fun FinanceTeachersTab(
    teachers: List<SchoolUser>,
    subjects: List<Subject>,
    teacherShares: List<TeacherSubjectShare>,
    enrollments: List<Enrollment>,
    payments: List<PaymentRecord>,
    strings: com.example.localization.AppStrings,
    onEditPercentage: (TeacherSubjectShare) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        if (teachers.isEmpty()) {
            item {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.School
                )
            }
        } else {
            items(teachers, key = { it.id }) { teacher ->
                val teacherEnrollments = enrollments.filter { it.teacherId == teacher.id }
                val teacherPayments = payments.filter { it.teacherId == teacher.id }

                val totalCollectedForTeacher = teacherPayments.sumOf { it.amount }
                val teacherShareEarned = teacherPayments.sumOf { it.teacherShare }
                val schoolShareFromTeacher = teacherPayments.sumOf { it.schoolShare }

                // Find or create default share agreement record
                val existingShare = teacherShares.firstOrNull { it.teacherId == teacher.id }
                    ?: TeacherSubjectShare(
                        teacherId = teacher.id,
                        teacherName = teacher.fullName,
                        approvedPercentage = 50.0,
                        requestedPercentage = 50.0
                    )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = teacher.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${strings.studentsTaught}: ${teacherEnrollments.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SchoolTextSecondary
                                )
                            }

                            // Approved percentage pill
                            Button(
                                onClick = { onEditPercentage(existingShare) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SchoolPrimaryContainer),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = "${strings.approvedPercentageLabel}: ${existingShare.approvedPercentage.toInt()}%",
                                    color = SchoolPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Edit, contentDescription = null, tint = SchoolPrimary, modifier = Modifier.size(14.dp))
                            }
                        }

                        if (existingShare.requestedPercentage != existingShare.approvedPercentage) {
                            Text(
                                text = "ℹ️ ${strings.requestedPercentageLabel}: ${existingShare.requestedPercentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD97706),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SchoolOutline)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats breakdown for this teacher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(strings.totalCollectedForSubject, style = MaterialTheme.typography.labelSmall, color = SchoolTextSecondary)
                                Text("${totalCollectedForTeacher.toInt()} ${strings.currencySymbol}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text(strings.teacherShareOwed, style = MaterialTheme.typography.labelSmall, color = SchoolTextSecondary)
                                Text("${teacherShareEarned.toInt()} ${strings.currencySymbol}", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A), style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text(strings.schoolShareTotal, style = MaterialTheme.typography.labelSmall, color = SchoolTextSecondary)
                                Text("${schoolShareFromTeacher.toInt()} ${strings.currencySymbol}", fontWeight = FontWeight.Bold, color = SchoolPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        // Students Status preview
                        if (teacherEnrollments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${strings.myStudentsStatus} (${teacherEnrollments.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SchoolDeepNavy
                            )
                            teacherEnrollments.take(3).forEach { enroll ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${enroll.studentName} (${enroll.subjectName})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = if (enroll.amountRemaining <= 0) strings.statusPaid else "${strings.amountRemainingLabel}: ${enroll.amountRemaining.toInt()} ${strings.currencySymbol}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (enroll.amountRemaining <= 0) Color(0xFF16A34A) else SchoolAccentRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// MULTI-SUBJECT RECORD PAYMENT DIALOG
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSubjectRecordPaymentDialog(
    strings: com.example.localization.AppStrings,
    students: List<SchoolUser>,
    enrollments: List<Enrollment>,
    teacherShares: List<TeacherSubjectShare>,
    preselectedStudent: SchoolUser?,
    preselectedEnrollment: Enrollment?,
    onDismiss: () -> Unit,
    onSave: (studentId: String, studentName: String, enrollment: Enrollment, amount: Double, approvedPercentage: Double, month: String, date: String, note: String) -> Unit
) {
    var selectedStudent by remember { mutableStateOf(preselectedStudent ?: students.firstOrNull()) }
    val studentEnrollments = remember(selectedStudent, enrollments) {
        enrollments.filter { it.studentId == selectedStudent?.id }
    }
    var selectedEnrollment by remember {
        mutableStateOf(preselectedEnrollment ?: studentEnrollments.firstOrNull())
    }

    LaunchedEffect(studentEnrollments) {
        if (selectedEnrollment == null || studentEnrollments.none { it.id == selectedEnrollment?.id }) {
            selectedEnrollment = studentEnrollments.firstOrNull()
        }
    }

    // Active approved percentage agreement for this teacher
    val activeShare = remember(selectedEnrollment, teacherShares) {
        teacherShares.firstOrNull { it.teacherId == selectedEnrollment?.teacherId }
    }
    val approvedPct = activeShare?.approvedPercentage ?: 50.0

    var amountText by remember {
        mutableStateOf(selectedEnrollment?.amountRemaining?.toInt()?.toString() ?: "500")
    }
    LaunchedEffect(selectedEnrollment) {
        if (selectedEnrollment != null) {
            val rem = selectedEnrollment?.amountRemaining?.toInt() ?: 0
            amountText = if (rem > 0) rem.toString() else selectedEnrollment?.monthlyFee?.toInt()?.toString() ?: "500"
        }
    }

    val currentDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    val currentMonthStr = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    var paymentDate by remember { mutableStateOf(currentDateStr) }
    var month by remember { mutableStateOf(currentMonthStr) }
    var note by remember { mutableStateOf("") }

    val amountDouble = amountText.toDoubleOrNull() ?: 0.0
    val calculatedTeacherShare = amountDouble * (approvedPct / 100.0)
    val calculatedSchoolShare = amountDouble * ((100.0 - approvedPct) / 100.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.recordPayment, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Student Selector (if not preselected)
                Text(strings.roleStudent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                if (preselectedStudent != null) {
                    Text(
                        text = preselectedStudent.fullName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = SchoolPrimary
                    )
                } else {
                    var studentExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = studentExpanded,
                        onExpandedChange = { studentExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedStudent?.fullName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = studentExpanded,
                            onDismissRequest = { studentExpanded = false }
                        ) {
                            students.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.fullName) },
                                    onClick = {
                                        selectedStudent = s
                                        studentExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Subject Selector
                Text(strings.selectSubjectForPayment, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                if (studentEnrollments.isEmpty()) {
                    Text(
                        text = strings.noEnrolledSubjects,
                        style = MaterialTheme.typography.bodySmall,
                        color = SchoolAccentRed
                    )
                } else {
                    var subjectExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = subjectExpanded,
                        onExpandedChange = { subjectExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedEnrollment?.let { "${it.subjectName} (${strings.roleTeacher}: ${it.teacherName})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = subjectExpanded,
                            onDismissRequest = { subjectExpanded = false }
                        ) {
                            studentEnrollments.forEach { enroll ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(enroll.subjectName, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${strings.amountRemainingLabel}: ${enroll.amountRemaining.toInt()} ${strings.currencySymbol}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (enroll.amountRemaining > 0) SchoolAccentRed else Color(0xFF16A34A)
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedEnrollment = enroll
                                        subjectExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("${strings.amountPaidLabel} (${strings.currencySymbol})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_payment_amount")
                )

                // 4. Live Share Preview (Admin-only confidential calculation)
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SchoolPrimaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🔐 ${strings.securityRulesTitle} (Admin Only Preview)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SchoolPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${strings.teacherShareLabel} (${approvedPct.toInt()}%):",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${calculatedTeacherShare.toInt()} ${strings.currencySymbol}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${strings.schoolShareLabel} (${(100 - approvedPct).toInt()}%):",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${calculatedSchoolShare.toInt()} ${strings.currencySymbol}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SchoolPrimary
                            )
                        }
                    }
                }

                // 5. Month & Date
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it },
                        label = { Text(strings.monthlyFees) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = paymentDate,
                        onValueChange = { paymentDate = it },
                        label = { Text("Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 6. Notes
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.auditReasonNote) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val enroll = selectedEnrollment
                    val stud = selectedStudent
                    if (enroll != null && stud != null && amountDouble > 0) {
                        onSave(stud.id, stud.fullName, enroll, amountDouble, approvedPct, month, paymentDate, note)
                    }
                },
                enabled = selectedEnrollment != null && amountDouble > 0,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

// ----------------------------------------------------
// ENROLL STUDENT IN SUBJECT DIALOG
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollStudentDialog(
    strings: com.example.localization.AppStrings,
    students: List<SchoolUser>,
    teachers: List<SchoolUser>,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (Enrollment) -> Unit
) {
    var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var selectedTeacher by remember(selectedSubject) {
        mutableStateOf(teachers.firstOrNull { it.id == selectedSubject?.teacherId } ?: teachers.firstOrNull())
    }
    var feeText by remember(selectedSubject) {
        mutableStateOf(if (selectedSubject != null && selectedSubject!!.price > 0) selectedSubject!!.price.toInt().toString() else "400")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.enrollStudent, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Student Picker
                Text(strings.roleStudent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                var studentExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = studentExp, onExpandedChange = { studentExp = it }) {
                    OutlinedTextField(
                        value = selectedStudent?.fullName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentExp) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = studentExp, onDismissRequest = { studentExp = false }) {
                        students.forEach { s ->
                            DropdownMenuItem(text = { Text(s.fullName) }, onClick = { selectedStudent = s; studentExp = false })
                        }
                    }
                }

                // Subject Picker (Shows Level and Price)
                Text(strings.subjectsTitle, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                var subjExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = subjExp, onExpandedChange = { subjExp = it }) {
                    OutlinedTextField(
                        value = selectedSubject?.displayName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjExp) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = subjExp, onDismissRequest = { subjExp = false }) {
                        subjects.forEach { sb ->
                            DropdownMenuItem(
                                text = { Text(sb.fullLabelWithPrice) },
                                onClick = {
                                    selectedSubject = sb
                                    if (sb.price > 0) feeText = sb.price.toInt().toString()
                                    val assignedT = teachers.firstOrNull { it.id == sb.teacherId }
                                    if (assignedT != null) selectedTeacher = assignedT
                                    subjExp = false
                                }
                            )
                        }
                    }
                }

                // Teacher Picker
                Text(strings.roleTeacher, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                var teachExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = teachExp, onExpandedChange = { teachExp = it }) {
                    OutlinedTextField(
                        value = selectedTeacher?.fullName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teachExp) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = teachExp, onDismissRequest = { teachExp = false }) {
                        teachers.forEach { t ->
                            DropdownMenuItem(text = { Text(t.fullName) }, onClick = { selectedTeacher = t; teachExp = false })
                        }
                    }
                }

                // Monthly Fee
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("${strings.monthlyFeeAmount} (${strings.currencySymbol})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = selectedStudent
                    val sb = selectedSubject
                    val t = selectedTeacher
                    val fee = feeText.toDoubleOrNull() ?: 400.0
                    if (s != null && sb != null && t != null) {
                        onSave(
                            Enrollment(
                                studentId = s.id,
                                studentName = s.fullName,
                                subjectId = sb.id,
                                subjectName = sb.displayName,
                                teacherId = t.id,
                                teacherName = t.fullName,
                                monthlyFee = fee,
                                amountPaid = 0.0,
                                amountRemaining = fee,
                                status = "active"
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

// ----------------------------------------------------
// AUDIT CONFIRMATION EDIT DIALOGS (FINANCIAL RECORDS)
// ----------------------------------------------------

@Composable
fun EditPaymentAuditDialog(
    payment: PaymentRecord,
    strings: com.example.localization.AppStrings,
    onDismiss: () -> Unit,
    onConfirm: (newAmount: Double, note: String) -> Unit
) {
    var newAmountText by remember { mutableStateOf(payment.amount.toInt().toString()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Security, contentDescription = null, tint = SchoolAccentRed) },
        title = { Text(strings.auditWarningDialogTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.auditWarningMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = SchoolAccentRed
                )
                Text(
                    text = "${strings.recordLabel}: ${payment.studentName} - ${payment.subjectName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${strings.oldValueLabel}: ${payment.amount.toInt()} ${strings.currencySymbol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SchoolTextSecondary
                )
                OutlinedTextField(
                    value = newAmountText,
                    onValueChange = { newAmountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(strings.newValueLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.auditReasonNote) },
                    placeholder = { Text(strings.auditReasonPlaceholder) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = newAmountText.toDoubleOrNull() ?: payment.amount
                    onConfirm(amount, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolAccentRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(strings.confirmAndLog)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun EditEnrollmentFeeAuditDialog(
    enrollment: Enrollment,
    strings: com.example.localization.AppStrings,
    onDismiss: () -> Unit,
    onConfirm: (newFee: Double, note: String) -> Unit
) {
    var newFeeText by remember { mutableStateOf(enrollment.monthlyFee.toInt().toString()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Security, contentDescription = null, tint = SchoolAccentRed) },
        title = { Text(strings.auditWarningDialogTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.auditWarningMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = SchoolAccentRed
                )
                Text(
                    text = "${strings.recordLabel}: ${enrollment.studentName} - ${enrollment.subjectName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${strings.oldValueLabel}: ${enrollment.monthlyFee.toInt()} ${strings.currencySymbol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SchoolTextSecondary
                )
                OutlinedTextField(
                    value = newFeeText,
                    onValueChange = { newFeeText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("${strings.monthlyFeeAmount} (${strings.currencySymbol})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.auditReasonNote) },
                    placeholder = { Text(strings.auditReasonPlaceholder) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fee = newFeeText.toDoubleOrNull() ?: enrollment.monthlyFee
                    onConfirm(fee, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolAccentRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(strings.confirmAndLog)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun EditTeacherShareAuditDialog(
    share: TeacherSubjectShare,
    strings: com.example.localization.AppStrings,
    onDismiss: () -> Unit,
    onConfirm: (approvedPct: Double, note: String) -> Unit
) {
    var pctText by remember { mutableStateOf(share.approvedPercentage.toInt().toString()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Security, contentDescription = null, tint = SchoolAccentRed) },
        title = { Text(strings.auditWarningDialogTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.auditWarningMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = SchoolAccentRed
                )
                Text(
                    text = "${strings.roleTeacher}: ${share.teacherName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${strings.oldValueLabel}: ${share.approvedPercentage.toInt()}% (School: ${(100 - share.approvedPercentage).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = SchoolTextSecondary
                )
                OutlinedTextField(
                    value = pctText,
                    onValueChange = { pctText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("${strings.approvedPercentageLabel} (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.auditReasonNote) },
                    placeholder = { Text(strings.auditReasonPlaceholder) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pct = (pctText.toDoubleOrNull() ?: share.approvedPercentage).coerceIn(0.0, 100.0)
                    onConfirm(pct, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolAccentRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(strings.confirmAndLog)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
