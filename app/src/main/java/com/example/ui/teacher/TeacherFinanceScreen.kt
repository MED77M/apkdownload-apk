package com.example.ui.teacher

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.Enrollment
import com.example.data.model.TeacherSubjectShare
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatCard
import com.example.ui.common.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherFinanceScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentTeacher = firebaseManager.currentUser

    // Observe only this teacher's enrollments and payments
    val myEnrollments by firebaseManager.observeEnrollments(teacherId = currentTeacher?.id).collectAsState(initial = emptyList())
    val myPayments by firebaseManager.observePayments(teacherId = currentTeacher?.id).collectAsState(initial = emptyList())
    val teacherShares by firebaseManager.observeTeacherShares(teacherId = currentTeacher?.id).collectAsState(initial = emptyList())

    val myShareAgreement = remember(teacherShares, currentTeacher) {
        teacherShares.firstOrNull { it.teacherId == currentTeacher?.id }
            ?: TeacherSubjectShare(
                teacherId = currentTeacher?.id ?: "",
                teacherName = currentTeacher?.fullName ?: "",
                approvedPercentage = 50.0,
                requestedPercentage = 50.0
            )
    }

    val totalEarned = remember(myPayments) {
        myPayments.sumOf { it.teacherShare }
    }
    val totalPaidStudents = remember(myEnrollments) {
        myEnrollments.count { it.amountRemaining <= 0.0 }
    }
    val totalUnpaidStudents = remember(myEnrollments) {
        myEnrollments.count { it.amountRemaining > 0.0 }
    }

    var showProposePercentageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.teacherFinanceTitle,
                subtitle = "${strings.roleTeacher}: ${currentTeacher?.fullName ?: ""}",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats Row: Total Earned Share & Total Enrolled Students
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = strings.myEarnedShare,
                    value = "${totalEarned.toInt()} ${strings.currencySymbol}",
                    icon = Icons.Default.AccountBalanceWallet,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = strings.studentsTaught,
                    value = "${myEnrollments.size} ${strings.studentsTab}",
                    icon = Icons.Default.Groups,
                    color = SchoolPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Percentage Agreement Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.teacherPercentageLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${strings.approvedPercentageLabel}: ${myShareAgreement.approvedPercentage.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SchoolPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            if (myShareAgreement.requestedPercentage != myShareAgreement.approvedPercentage) {
                                Text(
                                    text = "${strings.requestedPercentageLabel}: ${myShareAgreement.requestedPercentage.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }

                        Button(
                            onClick = { showProposePercentageDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolPrimaryContainer)
                        ) {
                            Text(
                                text = strings.requestPercentage,
                                color = SchoolPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Students Payment Status List
            Text(
                text = strings.myStudentsStatus,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (myEnrollments.isEmpty()) {
                EmptyStateView(
                    message = strings.noEnrolledSubjects,
                    icon = Icons.Default.PeopleOutline
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(myEnrollments, key = { it.id }) { enroll ->
                        val isPaid = enroll.amountRemaining <= 0.0
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = enroll.studentName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = enroll.subjectName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SchoolPrimary
                                    )
                                }

                                StatusBadge(
                                    text = if (isPaid) strings.statusPaid else "${strings.amountRemainingLabel}: ${enroll.amountRemaining.toInt()} ${strings.currencySymbol}",
                                    color = if (isPaid) Color(0xFF16A34A) else SchoolAccentRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProposePercentageDialog) {
        var proposedText by remember { mutableStateOf(myShareAgreement.requestedPercentage.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showProposePercentageDialog = false },
            title = { Text(strings.requestPercentage, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current approved share: ${myShareAgreement.approvedPercentage.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = SchoolTextSecondary
                    )
                    OutlinedTextField(
                        value = proposedText,
                        onValueChange = { proposedText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("${strings.requestedPercentageLabel} (%)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pct = proposedText.toDoubleOrNull() ?: myShareAgreement.requestedPercentage
                        scope.launch {
                            val res = firebaseManager.saveTeacherShare(
                                myShareAgreement.copy(requestedPercentage = pct)
                            )
                            if (res.isSuccess) {
                                Toast.makeText(context, strings.save, Toast.LENGTH_SHORT).show()
                                showProposePercentageDialog = false
                            } else {
                                Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.proposePercentage)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProposePercentageDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
