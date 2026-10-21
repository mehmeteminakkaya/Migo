package com.example.migo.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CareerTask
import com.example.migo.data.model.DailyCareerInsight
import com.example.migo.ui.theme.MigoGold
import com.example.migo.ui.theme.MigoSuccess
import java.util.Calendar
import java.util.Locale

@Composable
internal fun HomeScreen(
    session: CareerSession,
    onToggleTask: (String) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenCoach: () -> Unit,
    isPlanLoading: Boolean,
    planError: String?,
    insightIsLoading: Boolean,
    onCreatePlan: () -> Unit,
    onAskCoach: (String) -> Unit
) {
    val tasks = session.effectiveTasks()
    val completedTaskIds = session.effectiveCompletedTaskIds()
    val totalSteps = tasks.size.coerceAtLeast(1)
    val progress = (completedTaskIds.size.toFloat() / totalSteps).coerceIn(0f, 1f)
    val nextTask = tasks.firstOrNull { it.id !in completedTaskIds }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 6  -> "İyi geceler"
        hour < 12 -> "Günaydın"
        hour < 18 -> "İyi günler"
        else      -> "İyi akşamlar"
    }
    val firstName = session.profile.fullName.substringBefore(" ")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "BUGÜN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text  = "$greeting, $firstName",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text  = "Bugün tek bir anlamlı adım yeter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val streak = session.currentStreak()
            if (streak > 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "$streak gün",
                        fontWeight = FontWeight.Bold,
                        color  = MaterialTheme.colorScheme.primary,
                        style  = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = firstName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            CareerRouteRail(hasNextTask = nextTask != null)

            Column(modifier = Modifier.weight(1f).padding(start = 18.dp)) {
                CareerRouteSummary(
                    session = session,
                    progress = progress,
                    onOpenPlan = if (session.actionPlan == null) onCreatePlan else onOpenPlan,
                    isPlanLoading = isPlanLoading
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                if (nextTask != null) {
                    TodayFocusBlock(
                        task = nextTask,
                        onOpenPlan = onOpenPlan,
                        onComplete = { onToggleTask(nextTask.id) }
                    )
                } else {
                    CompletedPlanCard()
                }

                if (!planError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        planError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        val outlookCareer = session.activeCareerRecommendation()
        if (outlookCareer != null &&
            (outlookCareer.hiringOutlook.isNotBlank() || outlookCareer.salaryRange.isNotBlank())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            CareerOutlookRow(outlookCareer)
        }

        Spacer(modifier = Modifier.height(24.dp))
        CalmInsightCard(
            insight = session.dailyInsight,
            isLoading = insightIsLoading,
            fallbackAction = nextTask?.title.orEmpty(),
            onAskCoach = onAskCoach
        )

        Spacer(modifier = Modifier.height(20.dp))
        HomeQuickActions(
            onOpenPlan = onOpenPlan,
            onOpenCoach = onOpenCoach,
            onOpenTools = onOpenTools
        )
        Spacer(modifier = Modifier.height(22.dp))
    }
}

@Composable
private fun CareerOutlookRow(career: CareerRecommendation) {
    Column {
        Text(
            "KARİYER GÖRÜNÜMÜ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (career.hiringOutlook.isNotBlank()) {
                OutlookMetricCard(
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    label = "İşe alım sinyali",
                    value = career.hiringOutlook,
                    color = MigoSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
            if (career.salaryRange.isNotBlank()) {
                OutlookMetricCard(
                    icon = Icons.Rounded.Payments,
                    label = "Maaş referansı",
                    value = compactHomeSalary(career.salaryRange),
                    color = MigoGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OutlookMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeQuickActions(
    onOpenPlan: () -> Unit,
    onOpenCoach: () -> Unit,
    onOpenTools: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeQuickActionButton(Icons.Rounded.Map, "Yol haritan", onOpenPlan, Modifier.weight(1f))
        HomeQuickActionButton(Icons.AutoMirrored.Rounded.Chat, "AI koç", onOpenCoach, Modifier.weight(1f))
        HomeQuickActionButton(Icons.Rounded.PictureAsPdf, "CV analizi", onOpenTools, Modifier.weight(1f))
    }
}

@Composable
private fun HomeQuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CareerRouteRail(hasNextTask: Boolean) {
    Column(
        modifier = Modifier.width(20.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .width(2.dp)
                .padding(vertical = 4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .border(
                    2.dp,
                    if (hasNextTask) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .weight(0.6f)
                .width(2.dp)
                .padding(top = 4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun CareerRouteSummary(
    session: CareerSession,
    progress: Float,
    onOpenPlan: () -> Unit,
    isPlanLoading: Boolean
) {
    Column {
        Text(
            session.targetCareer,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${session.profile.classLevel} · ${session.profile.department}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "%${(progress * 100).toInt()}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "plan ilerlemesi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
        if (session.actionPlan == null) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onOpenPlan,
                enabled = !isPlanLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isPlanLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("30 günlük planı oluştur", fontWeight = FontWeight.SemiBold)
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayFocusBlock(
    task: CareerTask,
    onOpenPlan: () -> Unit,
    onComplete: () -> Unit
) {
    Column {
        Text(
            "BUGÜNÜN ODAĞI · ${task.durationMinutes} DK",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            task.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 25.sp
        )
        if (task.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                task.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            TextButton(
                onClick = onOpenPlan,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text("Ayrıntı", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onComplete,
                modifier = Modifier.heightIn(min = 44.dp),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tamamlandı", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CalmInsightCard(
    insight: DailyCareerInsight?,
    isLoading: Boolean,
    fallbackAction: String,
    onAskCoach: (String) -> Unit
) {
    val prompt = insight?.coachPrompt.orEmpty()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = prompt.isNotBlank()) { onAskCoach(prompt) },
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            if (isLoading && insight == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    insight?.title ?: "Migo AI kısa notu",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    insight?.message ?: fallbackAction.ifBlank {
                        "Bölümün ve hedefin için bugünün önerisi hazırlanıyor."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (prompt.isNotBlank()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        "AI koçla aç →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

internal fun CareerSession.activeCareerRecommendation(): CareerRecommendation? {
    val locale = Locale.forLanguageTag("tr-TR")
    val normalizedTarget = targetCareer.trim().lowercase(locale)
    return analysis.careers.firstOrNull { career ->
        val title = career.title.trim().lowercase(locale)
        title == normalizedTarget || title.contains(normalizedTarget) || normalizedTarget.contains(title) ||
            career.roles.any { role -> role.trim().lowercase(locale) == normalizedTarget }
    } ?: analysis.careers.firstOrNull()
}

private fun compactHomeSalary(value: String): String = value
    .replace("Aylık brüt yaklaşık", "", ignoreCase = true)
    .replace("Aylık yaklaşık", "", ignoreCase = true)
    .replace("Yaklaşık", "", ignoreCase = true)
    .trim()
    .ifBlank { "Veri sınırlı" }
