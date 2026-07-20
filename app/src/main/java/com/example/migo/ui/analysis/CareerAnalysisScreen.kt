package com.example.migo.ui.analysis

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.migo.R
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.ui.theme.GradientCyanEnd
import com.example.migo.ui.theme.GradientCyanStart
import com.example.migo.ui.theme.GradientHeroEnd
import com.example.migo.ui.theme.GradientHeroMid
import com.example.migo.ui.theme.MigoGold
import com.example.migo.ui.theme.MigoSuccess

@Composable
fun CareerAnalysisScreen(
    analysis: CareerAnalysis,
    onBackClick: () -> Unit,
    onTargetSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCareer by remember { mutableStateOf<String?>(null) }

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
            // ── Top bar ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(
                    stringResource(R.string.career_analysis),
                    modifier   = Modifier.weight(1f),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
            LinearProgressIndicator(
                progress   = { 1f },
                modifier   = Modifier.fillMaxWidth().height(3.dp),
                color      = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // ── AI analysis step badge ─────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            stringResource(R.string.ai_analysis_label),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                // ── Hero headline card ─────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(24.dp),
                    color    = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(GradientHeroMid, GradientHeroEnd))
                            )
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint     = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Migo AI değerlendirmesi",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                analysis.headline,
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color.White
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                analysis.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.88f),
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Strengths ──────────────────────────────────────────────
                SectionTitle(
                    icon  = Icons.Rounded.Star,
                    title = stringResource(R.string.potential_strengths),
                    color = MigoGold
                )
                Spacer(modifier = Modifier.height(10.dp))
                analysis.strengths.forEach { strength ->
                    StrengthRow(strength)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(22.dp))

                // ── Career recommendations ─────────────────────────────────
                SectionTitle(
                    icon  = Icons.Rounded.Lightbulb,
                    title = stringResource(R.string.recommended_careers),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                analysis.careers.forEachIndexed { index, career ->
                    CareerOptionCard(
                        career      = career,
                        rank        = index + 1,
                        isSelected  = selectedCareer == career.title,
                        onSelected  = { selectedCareer = career.title }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // ── Next steps ─────────────────────────────────────────────
                Spacer(modifier = Modifier.height(14.dp))
                SectionTitle(
                    icon  = Icons.Rounded.ChevronRight,
                    title = stringResource(R.string.first_steps),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(20.dp),
                    color    = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        analysis.nextSteps.forEachIndexed { index, step ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(GradientCyanStart, GradientCyanEnd))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        (index + 1).toString(),
                                        color      = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style      = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    step,
                                    modifier   = Modifier.weight(1f),
                                    style      = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp
                                )
                            }
                            if (index < analysis.nextSteps.lastIndex) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }
                    }
                }

                // ── Disclaimer ─────────────────────────────────────────────
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.ai_disclaimer),
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(28.dp))

                // ── CTA ───────────────────────────────────────────────────
                selectedCareer?.let { target ->
                    Button(
                        onClick = { onTargetSelected(target) },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape    = RoundedCornerShape(20.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor   = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(GradientHeroMid, GradientHeroEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "$target — hedefim bu",
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } ?: run {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(20.dp),
                        color    = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.select_target_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StrengthRow(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        color    = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MigoSuccess.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint     = MigoSuccess,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CareerOptionCard(
    career: CareerRecommendation,
    rank: Int,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.02f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "career_scale"
    )

    val rankColors = listOf(
        Color(0xFFFFB347), // gold
        Color(0xFF9FA8DA), // silver
        Color(0xFFBCAAA4)  // bronze
    )
    val rankColor = rankColors.getOrElse(rank - 1) { MaterialTheme.colorScheme.primary }

    Surface(
        onClick         = onSelected,
        modifier        = Modifier.fillMaxWidth().scale(scale),
        shape           = RoundedCornerShape(20.dp),
        color           = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                          else MaterialTheme.colorScheme.surface,
        border          = if (isSelected) androidx.compose.foundation.BorderStroke(
            2.dp, MaterialTheme.colorScheme.primary
        ) else null,
        shadowElevation = if (isSelected) 6.dp else 3.dp
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(rankColor.copy(alpha = if (isSelected) 1f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#$rank",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 15.sp,
                    color      = if (isSelected) Color.White else rankColor
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    career.title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = if (isSelected) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    career.reason,
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Seçildi",
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
