package com.example.migo.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CvAnalysis
import com.example.migo.ui.theme.GradientHeroEnd
import com.example.migo.ui.theme.GradientHeroMid
import com.example.migo.ui.theme.MigoGold

@Composable
fun CareerToolsScreen(
    session: CareerSession,
    cvText: String,
    result: CvAnalysis?,
    isAnalyzing: Boolean,
    isReadingPdf: Boolean,
    pdfFileName: String?,
    errorMessage: String?,
    onCvTextChange: (String) -> Unit,
    onPickPdf: () -> Unit,
    onAnalyze: () -> Unit
) {
    var showPlusDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Kariyer Stüdyosu",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text  = "Başvuru materyallerini ölç, geliştir, prova et.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Aktif araç",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kullanıma hazır", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI CV analizi", style = MaterialTheme.typography.titleMedium)
                        Text(
                            session.targetCareer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                    Text(
                        "AI",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "PDF CV'ni seç veya metni yapıştır; güçlü yönleri, eksik anahtar kelimeleri ve daha etkili cümleleri gör.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(11.dp))
                OutlinedButton(
                    onClick = onPickPdf,
                    enabled = !isAnalyzing && !isReadingPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    if (isReadingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(19.dp),
                            strokeWidth = 2.3.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PDF okunuyor…", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PDF CV seç", fontWeight = FontWeight.SemiBold)
                    }
                }
                pdfFileName?.let { selectedFile ->
                    Spacer(modifier = Modifier.height(9.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                selectedFile,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(9.dp))
                OutlinedTextField(
                    value = cvText,
                    onValueChange = { onCvTextChange(it.take(12_000)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("CV metnini buraya yapıştır…") },
                    supportingText = {
                        Text(
                            if (cvText.length < 80) {
                                "En az 80 karakter • ${cvText.length}/12.000"
                            } else {
                                "${cvText.length}/12.000 karakter"
                            }
                        )
                    },
                    minLines = 5,
                    maxLines = 10,
                    enabled = !isAnalyzing && !isReadingPdf,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        "PDF cihazda okunur; analiz için yalnızca çıkarılan metin Migo AI'a gönderilir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onAnalyze,
                    enabled = cvText.trim().length >= 80 && !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Text("Migo inceliyor…", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(modifier = Modifier.width(7.dp))
                        Text("CV'yi analiz et", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Analiz tamamlanamadı",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = onAnalyze, enabled = !isAnalyzing) {
                        Text("Tekrar dene")
                    }
                }
            }
        }

        result?.let { analysis ->
            Spacer(modifier = Modifier.height(16.dp))
            AiCvResultCard(analysis)
        }

        Spacer(modifier = Modifier.height(22.dp))
        Text("Sıradaki araçlar", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(10.dp))
        LockedToolCard(
            icon = Icons.Rounded.Work,
            title = "İlan uyumluluk analizi",
            description = "İlan ile CV'ni karşılaştır, eksik becerileri ve eşleşme puanını gör."
        )
        Spacer(modifier = Modifier.height(10.dp))
        LockedToolCard(
            icon = Icons.Rounded.RecordVoiceOver,
            title = "AI mülakat provası",
            description = "Hedef role özel sorularla pratik yap ve cevap puanını öğren."
        )
        Spacer(modifier = Modifier.height(16.dp))
        PlusCard(onClick = { showPlusDialog = true })
        Spacer(modifier = Modifier.height(22.dp))
    }

    if (showPlusDialog) {
        AlertDialog(
            onDismissRequest = { showPlusDialog = false },
            icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
            title = { Text("Migo Plus hazırlanıyor") },
            text = {
                Text(
                    "Dinamik plan, ilan eşleştirme ve mülakat provası Plus paketinde olacak. " +
                        "Satın alma açılmadan önce bu ekran Google Play Billing'e bağlanacak."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPlusDialog = false }) { Text("Anladım") }
            }
        )
    }
}

@Composable
private fun PlusCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Migo Plus",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tüm AI araçlarını tek pakette aç",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text("Yakında", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiCvResultCard(result: CvAnalysis) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(74.dp)) {
                    CircularProgressIndicator(
                        progress = { result.score / 100f },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 7.dp,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            result.score.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text("/100", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI CV puanın", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        scoreLabel(result.score),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(result.summary, style = MaterialTheme.typography.bodyLarge)

            AnalysisList(title = "Güçlü yönler", items = result.strengths)
            AnalysisList(title = "Öncelikli geliştirmeler", items = result.improvements)

            if (result.missingKeywords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text("Eksik olabilecek anahtar kelimeler", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    result.missingKeywords.forEach { keyword ->
                        AssistChip(onClick = { }, label = { Text(keyword) })
                    }
                }
            }

            if (result.rewrittenBullets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text("Daha güçlü ifade örnekleri", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                result.rewrittenBullets.forEachIndexed { index, bullet ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        color = Color.Transparent,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
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
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(bullet, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "AI önerisidir. Başvurudan önce tüm bilgileri doğrula.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalysisList(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Spacer(modifier = Modifier.height(18.dp))
    Text(title, fontWeight = FontWeight.Bold)
    items.forEach { item ->
        Row(
            modifier = Modifier.padding(top = 7.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                "•",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(item, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LockedToolCard(icon: ImageVector, title: String, description: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.Lock,
                contentDescription = "Yakında",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun scoreLabel(score: Int): String = when {
    score >= 85 -> "Başvuruya güçlü hazırlanmış"
    score >= 70 -> "İyi bir temel var"
    score >= 50 -> "Geliştirmeye açık"
    else -> "Önce temel yapıyı güçlendir"
}
