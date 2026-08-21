package com.example.migo.ui.analysis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.migo.R
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerRoadmapStep
import com.example.migo.data.model.UserProfile
import com.example.migo.data.remote.NvidiaCareerService
import com.example.migo.ui.theme.MigoGold
import com.example.migo.ui.theme.MigoSuccess

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CareerAnalysisScreen(
    profile: UserProfile,
    analysis: CareerAnalysis,
    onBackClick: () -> Unit,
    onTargetSelected: (CareerRecommendation) -> Unit,
    detailLoader: suspend (UserProfile, CareerRecommendation) -> Result<CareerRecommendation> =
        NvidiaCareerService::enrichCareer,
    modifier: Modifier = Modifier
) {
    var openedBranch by remember { mutableStateOf<CareerRecommendation?>(null) }
    var branchDetailLoading by remember { mutableStateOf(false) }
    var branchDetailError by remember { mutableStateOf<String?>(null) }
    var branchRetryKey by remember { mutableStateOf(0) }
    var selectedSection by remember { mutableStateOf(0) }
    val branchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(openedBranch?.title, branchRetryKey) {
        val branch = openedBranch ?: return@LaunchedEffect
        if (branch.hasRichDetail()) return@LaunchedEffect
        branchDetailLoading = true
        branchDetailError = null
        detailLoader(profile, branch)
            .onSuccess { openedBranch = it }
            .onFailure { error ->
                branchDetailError = error.message ?: "Branş ayrıntıları şu anda hazırlanamadı."
            }
        branchDetailLoading = false
    }

    openedBranch?.let { branch ->
        val displayBranch = if (branchDetailError != null) {
            branch.withFallbackDetail(profile)
        } else {
            branch
        }
        ModalBottomSheet(
            onDismissRequest = { openedBranch = null },
            sheetState = branchSheetState,
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            BranchDetailsSheet(
                branch = displayBranch,
                isLoading = branchDetailLoading,
                errorMessage = branchDetailError,
                onRetry = { branchRetryKey += 1 },
                onSelect = { onTargetSelected(displayBranch) }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "Program haritan",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "PROGRAM + SINIF ANALİZİ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                ProgramHero(analysis)

                if (analysis.levelFocusTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    LevelFocusCard(
                        title = analysis.levelFocusTitle,
                        description = analysis.levelFocusDescription
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                AnalysisSectionSwitcher(
                    selected = selectedSection,
                    onSelected = { selectedSection = it }
                )
                Spacer(modifier = Modifier.height(22.dp))

                if (selectedSection == 0) {
                    SectionTitle(
                        icon = Icons.Rounded.WorkOutline,
                        eyebrow = "${analysis.careers.size} KARİYER YÖNÜ",
                        title = "Bölümündeki branşlar",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Kartlar uygunluk sırası değildir. Bir branşa dokunup rollerini, koşullarını ve piyasa görünümünü incele.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 21.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    analysis.careers.forEach { branch ->
                        CareerBranchCard(
                            branch = branch,
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = {
                                branchDetailError = null
                                openedBranch = branch
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    MarketEstimateNotice()
                } else {
                    SectionTitle(
                        icon = Icons.Rounded.Route,
                        eyebrow = "ŞİMDİ NE YAPMALISIN?",
                        title = "Sınıfına özel yol haritası",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val roadmap = analysis.roadmap.ifEmpty {
                        analysis.nextSteps.mapIndexed { index, step ->
                            CareerRoadmapStep(
                                period = "Adım ${index + 1}",
                                title = step,
                                description = "Bu adımı tamamlayıp çıktını Migo'da takip et."
                            )
                        }
                    }
                    roadmap.forEachIndexed { index, step ->
                        RoadmapCard(
                            step = step,
                            index = index,
                            isLast = index == roadmap.lastIndex
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                    FocusAreasPanel(analysis.strengths)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Bir branşa dokunarak rolleri, çalışma alanlarını, gereksinimleri ve piyasa görünümünü inceleyebilirsin.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AnalysisSectionSwitcher(selected: Int, onSelected: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            AnalysisSectionOption(
                icon = Icons.Rounded.WorkOutline,
                label = "Kariyer branşları",
                selected = selected == 0,
                onClick = { onSelected(0) },
                modifier = Modifier.weight(1f)
            )
            AnalysisSectionOption(
                icon = Icons.Rounded.Route,
                label = "Yol haritam",
                selected = selected == 1,
                onClick = { onSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AnalysisSectionOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        border = if (selected) BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusAreasPanel(items: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.FactCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Bu aşamanın odakları",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(11.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { FocusChip(it) }
            }
        }
    }
}

@Composable
private fun MarketEstimateNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            Text(
                "Maaş rakamı yapay zekâ tarafından üretilmez. Yalnızca kaynak adı ve tarihiyle doğrulanabilen pozisyon verisi ayrıntı ekranında gösterilir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ProgramHero(analysis: CareerAnalysis) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                ) {
                    Icon(
                        Icons.Rounded.School,
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp).size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    "Migo bölüm rehberi",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                analysis.headline,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                lineHeight = 30.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                analysis.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
                lineHeight = 22.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LevelFocusCard(title: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    lineHeight = 19.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(11.dp))
        Column {
            Text(
                eyebrow,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun RoadmapCard(step: CareerRoadmapStep, index: Int, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (index + 1).toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(104.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(15.dp)) {
                if (step.period.isNotBlank()) {
                    Text(
                        step.period.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }
                Text(step.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                step.actions.take(2).forEach { action ->
                    Spacer(modifier = Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(action, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusChip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CareerBranchCard(
    branch: CareerRecommendation,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.WorkOutline,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    branch.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = "Detayı aç",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(11.dp))
            Text(
                branch.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (branch.roles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    branch.roles.take(2).forEach { role -> SmallTag(role, accent) }
                    if (branch.roles.size > 2) {
                        SmallTag("+${branch.roles.size - 2} rol", accent)
                    }
                }
            }
            if (branch.salaryRange.isNotBlank() || branch.hiringOutlook.isNotBlank()) {
                Spacer(modifier = Modifier.height(13.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (branch.salaryRange.isNotBlank()) {
                        InlineMetric(
                            icon = Icons.Rounded.Payments,
                            label = "Maaş referansı",
                            value = branch.salaryRange.compactSalary(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (branch.hiringOutlook.isNotBlank()) {
                        InlineMetric(
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            label = "Talep görünümü",
                            value = branch.hiringOutlook,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineMetric(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SmallTag(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.1f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BranchDetailsSheet(
    branch: CareerRecommendation,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onSelect: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Rounded.WorkOutline,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp).size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "KARİYER BRANŞI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Bölümündeki bir çalışma yönü",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    branch.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    branch.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Migo AI alanı araştırıyor", fontWeight = FontWeight.Bold)
                        Text(
                            "Günlük iş, roller, gereksinimler ve ilk 90 gün hazırlanıyor…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Temel rehber gösteriliyor", fontWeight = FontWeight.Bold)
                            Text(
                                "Canlı AI ayrıntısı alınamadı. Ekranı boş bırakmak yerine bölümün ve sınıfına göre güvenli başlangıç adımlarını hazırladık.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "Canlı ayrıntıyı yeniden dene",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (branch.salaryRange.isNotBlank() || branch.hiringOutlook.isNotBlank()) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (branch.salaryRange.isNotBlank()) {
                    DetailMetricCard(
                        icon = Icons.Rounded.Payments,
                        label = "Maaş referansı",
                        value = branch.salaryRange.compactSalary(),
                        color = MigoGold,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (branch.hiringOutlook.isNotBlank()) {
                    DetailMetricCard(
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        label = "İşe alım sinyali",
                        value = branch.hiringOutlook,
                        color = MigoSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (branch.salaryNote.isNotBlank() || branch.demandNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    listOf(branch.salaryNote, branch.demandNote).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
            if (branch.salarySourceUrl.isNotBlank()) {
                TextButton(onClick = { uriHandler.openUri(branch.salarySourceUrl) }) {
                    Text(
                        buildString {
                            append("Kaynağı aç: ")
                            append(branch.salarySourceName.ifBlank { "Maaş veri kaynağı" })
                            if (branch.salarySourceDate.isNotBlank()) append(" • ${branch.salarySourceDate}")
                        }
                    )
                }
            }
        }

        NumberedDetailTimeline(
            icon = Icons.Rounded.AutoAwesome,
            title = "Bu işte bir gün",
            subtitle = "Rolün günlük akışında karşılaşabileceğin işler",
            items = branch.dailyWork
        )
        DetailSection(Icons.Rounded.WorkOutline, "Çalışabileceğin roller", branch.roles)
        DetailSection(Icons.Rounded.Apartment, "Çalışma alanları", branch.workAreas)
        DetailSection(Icons.Rounded.School, "Öne çıkan gereksinimler", branch.requirements)
        NumberedDetailTimeline(
            icon = Icons.Rounded.Route,
            title = "İlk 90 gün rotası",
            subtitle = "Bu branşı denemek için düşük riskli başlangıç",
            items = branch.first90Days
        )
        DetailSection(Icons.Rounded.Apartment, "Örnek işveren ekosistemi", branch.exampleEmployers)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Bu içerik kişilik testi değil, bölüm içindeki bir kariyer yönü rehberidir. Maaş yalnızca adı ve tarihi gösterilen dış kaynaktan okunabildiğinde sunulur; teklif veya iş garantisi değildir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onSelect,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Bu branşa odaklan", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NumberedDetailTimeline(
    icon: ImageVector,
    title: String,
    subtitle: String,
    items: List<String>
) {
    if (items.isEmpty()) return
    Spacer(modifier = Modifier.height(20.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(modifier = Modifier.height(11.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (index + 1).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(item, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                }
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 38.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailSection(icon: ImageVector, title: String, items: List<String>) {
    if (items.isEmpty()) return
    Spacer(modifier = Modifier.height(20.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    item,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DetailMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

private fun String.compactSalary(): String =
    replace("Aylık brüt yaklaşık", "", ignoreCase = true)
        .replace("Aylık brüt", "", ignoreCase = true)
        .replace("Yaklaşık", "", ignoreCase = true)
        .trim()

private fun CareerRecommendation.hasRichDetail(): Boolean =
    roles.size >= 2 && requirements.size >= 2 && dailyWork.size >= 2 && first90Days.size >= 2

private fun CareerRecommendation.withFallbackDetail(profile: UserProfile): CareerRecommendation {
    val level = profile.classLevel.lowercase()
    val firstSteps = when {
        "1" in level || "hazırlık" in level -> listOf(
            "Bu branştaki üç rolü günlük işler ve eğitim koşullarıyla karşılaştır",
            "Bölümündeki temel derslerden bu alanla ilişkili olanları işaretle",
            "Bir uzman söyleşisi veya vaka üzerinden bir sayfalık gözlem notu üret"
        )
        "2" in level -> listOf(
            "Bir haftada tamamlanabilecek küçük bir uygulama veya vaka çalışması seç",
            "Alanla ilgili bir topluluk, laboratuvar veya öğrenci ekibini incele",
            "Ürettiğin çıktıyı bir kontrol listesiyle değerlendir ve geri bildirim al"
        )
        "3" in level -> listOf(
            "Beş başlangıç rolü veya proje örneğindeki ortak beklentileri çıkar",
            "En sık istenen beceriyi gösteren portföy ya da araştırma çıktısı üret",
            "Bir alan uzmanından geri bildirim alıp çıktını ikinci kez düzenle; stajı yalnızca erişimin varsa ek seçenek olarak değerlendir"
        )
        else -> listOf(
            "Hedef rol ilanlarını beceri, deneyim ve teslim beklentilerine göre karşılaştır",
            "İşe hazır olduğunu gösteren tek bir güçlü çalışma örneği tamamla",
            "CV ve başvuru metnini bu çıktının kanıtlarıyla güncelle"
        )
    }
    return copy(
        workAreas = workAreas.ifEmpty {
            listOf(
                "${profile.department} ile ilişkili kurumlar",
                "Proje ve araştırma ekipleri",
                "Özel sektör veya kamu uygulamaları"
            )
        },
        requirements = requirements.ifEmpty {
            listOf(
                "Bölüm temelinde güçlü akademik altyapı",
                "$title alanına uygun uygulama deneyimi",
                "Etik, iletişim ve düzenli dokümantasyon"
            )
        },
        dailyWork = dailyWork.ifEmpty {
            listOf(
                "Alanla ilgili problemleri ve ihtiyaçları analiz etmek",
                "Ekip veya paydaşlarla çözüm ve çıktı üretmek",
                "Çalışmayı belgelemek, kontrol etmek ve geliştirmek"
            )
        },
        first90Days = first90Days.ifEmpty { firstSteps }
    )
}
