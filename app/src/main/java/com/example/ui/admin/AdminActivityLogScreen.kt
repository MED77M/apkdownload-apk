package com.example.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.AuditLog
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatusBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActivityLogScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val auditLogs by firebaseManager.observeAuditLogs().collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(auditLogs, selectedFilter, searchQuery) {
        auditLogs.filter { log ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "PAYMENTS" -> log.targetCollection == "payments" || log.action.contains("PAYMENT")
                "FEES" -> log.targetCollection == "enrollments" || log.action.contains("FEE")
                "PERCENTAGE" -> log.targetCollection == "teacher_shares" || log.action.contains("PERCENTAGE")
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                log.userName.contains(searchQuery, ignoreCase = true) ||
                log.recordTitle.contains(searchQuery, ignoreCase = true) ||
                log.note.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.activityLogTitle,
                subtitle = strings.activityLogSubtitle,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Immutable Audit Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SchoolDeepNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "IMMUTABLE AUDIT TRAIL 🔒",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = strings.auditWarningMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.filterByUser) },
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
                modifier = Modifier.fillMaxWidth()
            )

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("${strings.filterAll} (${auditLogs.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "PAYMENTS",
                    onClick = { selectedFilter = "PAYMENTS" },
                    label = { Text(strings.navFinance) }
                )
                FilterChip(
                    selected = selectedFilter == "FEES",
                    onClick = { selectedFilter = "FEES" },
                    label = { Text(strings.monthlyFeeAmount) }
                )
                FilterChip(
                    selected = selectedFilter == "PERCENTAGE",
                    onClick = { selectedFilter = "PERCENTAGE" },
                    label = { Text(strings.teacherPercentageLabel) }
                )
            }

            // Audit Logs List
            if (filteredLogs.isEmpty()) {
                EmptyStateView(
                    message = strings.noAuditLogsYet,
                    icon = Icons.Default.History
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        AuditLogCard(log = log, strings = strings)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogCard(
    log: AuditLog,
    strings: com.example.localization.AppStrings
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Action + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HistoryEdu,
                        contentDescription = null,
                        tint = SchoolPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.action,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SchoolPrimary
                    )
                }

                Text(
                    text = log.dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = SchoolTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Actor & Record Target
            Text(
                text = "${log.userName} (${log.userRole})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (log.recordTitle.isNotBlank()) {
                Text(
                    text = "${strings.recordLabel}: ${log.recordTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SchoolDeepNavy
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Diff Box: Old Value -> New Value
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${strings.oldValueLabel}: ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SchoolAccentRed
                        )
                        Text(
                            text = log.oldValue,
                            style = MaterialTheme.typography.labelSmall,
                            color = SchoolTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${strings.newValueLabel}: ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            text = log.newValue,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SchoolTextPrimary
                        )
                    }
                }
            }

            // Reason / Note
            if (log.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📝 ${strings.auditReasonNote}: \"${log.note}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        }
    }
}
