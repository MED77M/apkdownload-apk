package com.example.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Role
import com.example.data.model.SchoolUser
import com.example.localization.AppStrings
import com.example.ui.common.StatusBadge
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchDialog(
    firebaseManager: FirebaseManager,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onOpenConversation: ((userId: String, title: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(0) } // 0: All, 1: Students, 2: Teachers

    val allUsers by firebaseManager.observeUsers().collectAsState(initial = emptyList())

    val totalStudents = remember(allUsers) { allUsers.count { it.role == Role.STUDENT } }
    val totalTeachers = remember(allUsers) { allUsers.count { it.role == Role.TEACHER } }

    val filteredList = remember(allUsers, query, selectedFilter) {
        allUsers.filter { user ->
            val matchesRole = when (selectedFilter) {
                1 -> user.role == Role.STUDENT
                2 -> user.role == Role.TEACHER
                else -> user.role == Role.STUDENT || user.role == Role.TEACHER || user.role == Role.ADMIN
            }

            val q = query.trim().lowercase()
            val matchesQuery = if (q.isBlank()) {
                true
            } else {
                user.fullName.lowercase().contains(q) ||
                user.username.lowercase().contains(q) ||
                user.phone.replace(" ", "").contains(q.replace(" ", "")) ||
                (user.role == Role.STUDENT && ("تلميذ".contains(q) || "طالب".contains(q) || "student".contains(q) || "eleve".contains(q))) ||
                (user.role == Role.TEACHER && ("أستاذ".contains(q) || "استاذ".contains(q) || "معلم".contains(q) || "teacher".contains(q) || "prof".contains(q)))
            }

            matchesRole && matchesQuery
        }.sortedWith(compareBy({ it.role != Role.STUDENT }, { it.fullName }))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with icon, title, and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SchoolPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = SchoolPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.searchUsersTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SchoolPrimary
                            )
                            Text(
                                text = "ابحث عن أي تلميذ أو أستاذ مسجل (${filteredList.size} نتيجة)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Input Field
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("ابحث بالاسم الكامل، اسم المستخدم، أو رقم الهاتف...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SchoolPrimary)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SchoolPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_users")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips (الكل | الطلاب | الأساتذة) with live counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == 0,
                        onClick = { selectedFilter = 0 },
                        label = { Text("الكل (${allUsers.size})") },
                        leadingIcon = if (selectedFilter == 0) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = selectedFilter == 1,
                        onClick = { selectedFilter = 1 },
                        label = { Text("👨‍🎓 التلاميذ ($totalStudents)") },
                        leadingIcon = if (selectedFilter == 1) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = selectedFilter == 2,
                        onClick = { selectedFilter = 2 },
                        label = { Text("👨‍🏫 الأساتذة ($totalTeachers)") },
                        leadingIcon = if (selectedFilter == 2) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Results List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Color.Gray.copy(alpha = 0.6f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (query.isBlank()) "لا يوجد أي أساتذة أو تلاميذ مسجلين حالياً" else "لم يتم العثور على أي تلميذ أو أستاذ يطابق: \"$query\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList, key = { it.id }) { user ->
                            SearchResultUserCard(
                                user = user,
                                strings = strings,
                                onCall = {
                                    if (user.phone.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone.trim()}"))
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, "لا يتوفر رقم هاتف لهذا الحساب", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onChat = {
                                    onOpenConversation?.invoke(user.id, user.fullName)
                                    onDismiss()
                                },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("معلومات المستخدم", "${user.fullName} (${user.username}) - ${user.phone}")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ بيانات ${user.fullName}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultUserCard(
    user: SchoolUser,
    strings: AppStrings,
    onCall: () -> Unit,
    onChat: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Role Avatar Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        when (user.role) {
                            Role.STUDENT -> SchoolPrimary
                            Role.TEACHER -> Color(0xFFD97706)
                            Role.ADMIN -> SchoolSecondary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (user.role) {
                        Role.STUDENT -> Icons.Default.School
                        Role.TEACHER -> Icons.Default.CoPresent
                        Role.ADMIN -> Icons.Default.AdminPanelSettings
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = user.fullName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusBadge(
                        text = when (user.role) {
                            Role.STUDENT -> "تلميذ"
                            Role.TEACHER -> "أستاذ"
                            Role.ADMIN -> "مدير"
                        },
                        color = when (user.role) {
                            Role.STUDENT -> SchoolPrimary
                            Role.TEACHER -> Color(0xFFD97706)
                            Role.ADMIN -> SchoolSecondary
                        }
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "@${user.username}" + if (user.phone.isNotBlank()) " • 📞 ${user.phone}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons: Call, Chat, Copy
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (user.phone.isNotBlank()) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Call",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onChat,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Message",
                        tint = SchoolPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
