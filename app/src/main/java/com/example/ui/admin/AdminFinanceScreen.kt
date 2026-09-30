package com.example.ui.admin

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
import com.example.data.model.PaymentRecord
import com.example.data.model.Role
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatCard
import com.example.ui.common.StatusBadge
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary
import com.example.ui.theme.SchoolWarning
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFinanceScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val payments by firebaseManager.observePayments().collectAsState(initial = emptyList())
    val students by firebaseManager.observeUsers(Role.STUDENT).collectAsState(initial = emptyList())

    var filterStatus by remember { mutableStateOf("ALL") }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }

    val filteredPayments = remember(payments, filterStatus) {
        if (filterStatus == "ALL") payments else payments.filter { it.status == filterStatus }
    }

    val totalCollected = remember(payments) {
        payments.filter { it.status == "PAID" }.sumOf { it.amount }
    }
    val totalOverdue = remember(payments) {
        payments.filter { it.status == "OVERDUE" }.sumOf { it.amount }
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
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRecordPaymentDialog = true },
                containerColor = SchoolPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddCard, contentDescription = null) },
                text = { Text(strings.recordPayment) },
                modifier = Modifier.testTag("fab_record_payment")
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
            // Stats summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = strings.totalCollected,
                    value = "${totalCollected.toInt()} DZD",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = strings.totalOverdue,
                    value = "${totalOverdue.toInt()} DZD",
                    icon = Icons.Default.Warning,
                    color = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
            }

            // Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterStatus == "ALL",
                    onClick = { filterStatus = "ALL" },
                    label = { Text("All (${payments.size})") }
                )
                FilterChip(
                    selected = filterStatus == "PAID",
                    onClick = { filterStatus = "PAID" },
                    label = { Text(strings.statusPaid) }
                )
                FilterChip(
                    selected = filterStatus == "OVERDUE",
                    onClick = { filterStatus = "OVERDUE" },
                    label = { Text(strings.statusOverdue) }
                )
            }

            // Payment Records List
            if (filteredPayments.isEmpty()) {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.ReceiptLong,
                    actionText = strings.recordPayment,
                    onActionClick = { showRecordPaymentDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredPayments, key = { it.id }) { payment ->
                        PaymentCard(payment = payment, strings = strings)
                    }
                }
            }
        }
    }

    if (showRecordPaymentDialog) {
        RecordPaymentDialog(
            strings = strings,
            students = students,
            onDismiss = { showRecordPaymentDialog = false },
            onSave = { payment ->
                scope.launch {
                    val res = firebaseManager.recordPayment(payment)
                    res.onSuccess {
                        showRecordPaymentDialog = false
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun PaymentCard(
    payment: PaymentRecord,
    strings: com.example.localization.AppStrings
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.studentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${strings.monthlyFees}: ${payment.month} • ${payment.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (payment.notes.isNotEmpty()) {
                    Text(
                        text = payment.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${payment.amount.toInt()} DZD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = SchoolPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(
                    text = when (payment.status) {
                        "PAID" -> strings.statusPaid
                        "OVERDUE" -> strings.statusOverdue
                        else -> strings.statusPending
                    },
                    color = when (payment.status) {
                        "PAID" -> Color(0xFF16A34A)
                        "OVERDUE" -> Color(0xFFDC2626)
                        else -> Color(0xFFD97706)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    strings: com.example.localization.AppStrings,
    students: List<com.example.data.model.SchoolUser>,
    onDismiss: () -> Unit,
    onSave: (PaymentRecord) -> Unit
) {
    var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
    var amount by remember { mutableStateOf("5000") }
    var month by remember { mutableStateOf("October") }
    var status by remember { mutableStateOf("PAID") }
    var notes by remember { mutableStateOf("") }

    val dateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.recordPayment) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.studentsTab, style = MaterialTheme.typography.labelMedium)
                DropdownSelector(
                    items = students,
                    selectedItem = selectedStudent,
                    label = { "${it.fullName} (@${it.username})" },
                    onSelect = { selectedStudent = it }
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(strings.amount) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = month,
                    onValueChange = { month = it },
                    label = { Text(strings.monthlyFees) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == "PAID",
                        onClick = { status = "PAID" },
                        label = { Text(strings.statusPaid) }
                    )
                    FilterChip(
                        selected = status == "OVERDUE",
                        onClick = { status = "OVERDUE" },
                        label = { Text(strings.statusOverdue) }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Remarques") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedStudent != null && amount.isNotBlank()) {
                        val payment = PaymentRecord(
                            studentId = selectedStudent!!.id,
                            studentName = selectedStudent!!.fullName,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            month = month.trim(),
                            status = status,
                            date = dateStr,
                            notes = notes.trim()
                        )
                        onSave(payment)
                    }
                },
                enabled = selectedStudent != null && amount.isNotBlank()
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
