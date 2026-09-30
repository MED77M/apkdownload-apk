package com.example.ui.student

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.GradeItem
import com.example.localization.AppLanguage
import com.example.localization.Translations
import com.example.ui.common.AppHeader
import com.example.ui.common.EmptyStateView
import com.example.ui.common.StatCard
import com.example.ui.common.StatusBadge
import com.example.ui.theme.SchoolPrimary
import com.example.ui.theme.SchoolSecondary

@Composable
fun StudentGradesScreen(
    firebaseManager: FirebaseManager,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = Translations.get(currentLanguage)
    val currentStudent = firebaseManager.currentUser

    val grades by firebaseManager.observeGrades(studentId = currentStudent?.id).collectAsState(initial = emptyList())

    val overallAvg = remember(grades) {
        if (grades.isNotEmpty()) String.format("%.1f", grades.map { it.score }.average()) else "0.0"
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = strings.gradesTitle,
                subtitle = "${strings.overallAverage}: $overallAvg / 20",
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
            // Overall Avg Stat Card
            StatCard(
                title = strings.overallAverage,
                value = "$overallAvg / 20",
                icon = Icons.Default.Grade,
                color = SchoolPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            // Progress Chart Over Time
            if (grades.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.progressChart,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom smooth line chart using Canvas
                        val scores = grades.map { it.score }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (scores.size == 1) {
                                    drawCircle(
                                        color = SchoolSecondary,
                                        radius = 8f,
                                        center = Offset(size.width / 2f, size.height * (1f - (scores[0] / 20f)))
                                    )
                                } else {
                                    val stepX = size.width / (scores.size - 1)
                                    val path = Path()
                                    scores.forEachIndexed { i, score ->
                                        val x = i * stepX
                                        val y = size.height * (1f - (score / 20f).coerceIn(0f, 1f))
                                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = SchoolSecondary,
                                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                                    )
                                    scores.forEachIndexed { i, score ->
                                        val x = i * stepX
                                        val y = size.height * (1f - (score / 20f).coerceIn(0f, 1f))
                                        drawCircle(color = SchoolPrimary, radius = 6f, center = Offset(x, y))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Grades List
            if (grades.isEmpty()) {
                EmptyStateView(
                    message = strings.noDataYet,
                    icon = Icons.Default.Grading
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(grades, key = { it.id }) { grade ->
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
                                        text = grade.subjectName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${grade.type} • 📅 ${grade.date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (grade.comment.isNotEmpty()) {
                                        Text(
                                            text = "💬 ${grade.comment}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SchoolSecondary
                                        )
                                    }
                                }
                                StatusBadge(
                                    text = "${grade.score} / ${grade.maxScore.toInt()}",
                                    color = if (grade.score >= 10f) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
