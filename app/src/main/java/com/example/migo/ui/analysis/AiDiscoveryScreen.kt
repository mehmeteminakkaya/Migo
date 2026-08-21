package com.example.migo.ui.analysis

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.migo.data.model.SmartProfileResult
import com.example.migo.data.model.UserProfile
import com.example.migo.data.remote.NvidiaCareerService
import com.example.migo.ui.theme.MigoSuccess
import kotlinx.coroutines.delay

@Composable
fun AiDiscoveryScreen(
    profile: UserProfile,
    onBackClick: () -> Unit,
    onAnalysisReady: (SmartProfileResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading    by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var stage        by remember { mutableIntStateOf(0) }
    var retryKey     by remember { mutableIntStateOf(0) }

    LaunchedEffect(profile, retryKey) {
        isLoading = true; errorMessage = null; stage = 0
        NvidiaCareerService.createSmartProfile(profile)
            .onSuccess(onAnalysisReady)
            .onFailure {
                errorMessage = it.message ?: "AI program haritası hazırlanamadı."
                isLoading    = false
            }
    }
    LaunchedEffect(isLoading, retryKey) {
        while (isLoading && stage < 2) { delay(2_000); stage += 1 }
    }

    // Pulsing orb animation
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val pulse by infiniteTransition.animateFloat(
        initialValue   = 0.92f,
        targetValue    = 1.08f,
        animationSpec  = infiniteRepeatable(tween(1_600, easing = LinearEasing), RepeatMode.Reverse),
        label          = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "Program haritası",
                    modifier   = Modifier.weight(1f),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    textAlign  = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
            LinearProgressIndicator(
                progress   = { 0.66f },
                modifier   = Modifier.fillMaxWidth().height(3.dp),
                color      = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(modifier = Modifier.height(52.dp))

            // ── Pulsing orb ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .scale(if (isLoading) pulse else 1f)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint   = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                "Migo program haritanı hazırlıyor",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
                textAlign  = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${profile.department} programının branşlarını ve ${profile.classLevel} için doğru ilerleme adımlarını inceliyoruz.",
                style     = MaterialTheme.typography.bodyLarge,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(36.dp))

            // ── Stage cards ────────────────────────────────────────────────
            AiStageCard(
                icon     = Icons.Rounded.School,
                title    = "Program verisi doğrulanıyor",
                detail   = profile.university,
                complete = stage >= 0
            )
            Spacer(modifier = Modifier.height(12.dp))
            AiStageCard(
                icon     = Icons.Rounded.Work,
                title    = "Bölüm branşları çıkarılıyor",
                detail   = "Programla ilişkili rol ve çalışma alanları hazırlanıyor",
                complete = stage >= 1
            )
            Spacer(modifier = Modifier.height(12.dp))
            AiStageCard(
                icon     = Icons.Rounded.Route,
                title    = "Sınıfına özel rota hazırlanıyor",
                detail   = "Bu yıl için gerçekçi adımlar sıralanıyor",
                complete = stage >= 2
            )

            // ── Error ──────────────────────────────────────────────────────
            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { retryKey += 1 }) { Text("Tekrar dene") }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Loading indicator ──────────────────────────────────────────
            if (isLoading) {
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Program ve sınıf analizi sürüyor…",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun AiStageCard(
    icon: ImageVector,
    title: String,
    detail: String,
    complete: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier          = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier          = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (complete) MigoSuccess.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    ),
                contentAlignment  = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (complete) MigoSuccess else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (complete) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MigoSuccess,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text("…", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
