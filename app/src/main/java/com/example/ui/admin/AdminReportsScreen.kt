package com.example.ui.admin

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Role
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppButton
import com.example.ui.common.AppHeader
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import com.example.ui.theme.SchoolWarning

@Composable
fun AdminReportsScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current

    val users by firebaseManager.observeUsers().collectAsState(initial = emptyList())
    val attendance by firebaseManager.observeAttendance().collectAsState(initial = emptyList())
    val grades by firebaseManager.observeGrades().collectAsState(initial = emptyList())
    val payments by firebaseManager.observePayments().collectAsState(initial = emptyList())

    val totalStudents = users.count { it.role == Role.STUDENT }
    val totalTeachers = users.count { it.role == Role.TEACHER }

    fun shareReport(title: String, content: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, content)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, strings.exportPdf)
        context.startActivity(shareIntent)
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.reportsTitle,
                subtitle = strings.appName,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Attendance Report Card
            ReportCard(
                title = strings.attendanceReport,
                description = "Total Sessions: ${attendance.size} • Total Students: $totalStudents",
                icon = Icons.Default.EventAvailable,
                color = Color(0xFF16A34A),
                onExportText = {
                    val sb = StringBuilder()
                    sb.append("=== science.est.center - ${strings.attendanceReport} ===\n\n")
                    sb.append("Total Sessions Recorded: ${attendance.size}\n")
                    attendance.forEachIndexed { i, a ->
                        sb.append("${i + 1}. Date: ${a.date} | Subject: ${a.subjectName} | Group: ${a.groupName}\n")
                        sb.append("   Present: ${a.presentStudentIds.size}, Absent: ${a.absentStudentIds.size}, Late: ${a.lateStudentIds.size}\n")
                    }
                    shareReport(strings.attendanceReport, sb.toString())
                }
            )

            // 2. Grades Report Card
            ReportCard(
                title = strings.gradesReport,
                description = "Total Evaluations: ${grades.size} • Overall Avg: ${
                    if (grades.isNotEmpty()) String.format("%.1f", grades.map { it.score }.average()) else "0.0"
                } / 20",
                icon = Icons.Default.Grading,
                color = SchoolPrimary,
                onExportText = {
                    val sb = StringBuilder()
                    sb.append("=== science.est.center - ${strings.gradesReport} ===\n\n")
                    grades.forEachIndexed { i, g ->
                        sb.append("${i + 1}. Student: ${g.studentName} | Subject: ${g.subjectName} | Score: ${g.score}/${g.maxScore} (${g.type})\n")
                    }
                    shareReport(strings.gradesReport, sb.toString())
                }
            )

            // 3. Finance Report Card
            val totalPaid = payments.filter { it.status == "PAID" }.sumOf { it.amount }
            val totalOverdue = payments.filter { it.status == "OVERDUE" }.sumOf { it.amount }

            ReportCard(
                title = strings.financeReport,
                description = "${strings.totalCollected}: ${totalPaid.toInt()} ${strings.currencySymbol} • ${strings.totalOverdue}: ${totalOverdue.toInt()} ${strings.currencySymbol}",
                icon = Icons.Default.PriceCheck,
                color = SchoolSecondary,
                onExportText = {
                    val sb = StringBuilder()
                    sb.append("=== science.est.center - ${strings.financeReport} ===\n\n")
                    sb.append("${strings.financeTitle}: ${payments.size}\n")
                    sb.append("${strings.totalCollected}: ${totalPaid.toInt()} ${strings.currencySymbol}\n")
                    sb.append("${strings.totalOverdue}: ${totalOverdue.toInt()} ${strings.currencySymbol}\n\n")
                    payments.forEachIndexed { i, p ->
                        sb.append("${i + 1}. ${p.studentName} - Month: ${p.month} | Amount: ${p.amount.toInt()} ${strings.currencySymbol} | Status: ${p.status} (${p.date})\n")
                    }
                    shareReport(strings.financeReport, sb.toString())
                }
            )
        }
    }
}

@Composable
fun ReportCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onExportText: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExportText,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export / Share")
                }
            }
        }
    }
}
