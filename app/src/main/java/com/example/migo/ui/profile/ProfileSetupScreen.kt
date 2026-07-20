package com.example.migo.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.migo.R
import com.example.migo.data.model.EducationOption
import com.example.migo.data.model.UserProfile
import com.example.migo.data.remote.EducationCatalogService
import com.example.migo.ui.theme.GradientHeroEnd
import com.example.migo.ui.theme.GradientHeroMid
import com.example.migo.ui.theme.GradientHeroStart
import com.example.migo.ui.theme.MigoSuccess
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(
    onBackClick: () -> Unit,
    onContinueClick: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
    initialProfile: UserProfile? = null
) {
    var fullName      by rememberSaveable { mutableStateOf(initialProfile?.fullName.orEmpty()) }
    var university    by rememberSaveable { mutableStateOf(initialProfile?.university.orEmpty()) }
    var universityId  by rememberSaveable { mutableStateOf(initialProfile?.universityId.orEmpty()) }
    var department    by rememberSaveable { mutableStateOf(initialProfile?.department.orEmpty()) }
    var programId     by rememberSaveable { mutableStateOf(initialProfile?.programId.orEmpty()) }
    var scoreType     by rememberSaveable { mutableStateOf(initialProfile?.scoreType.orEmpty()) }
    var selectedClass by rememberSaveable {
        mutableStateOf(initialProfile?.classLevel.orEmpty().substringBefore(".").trim())
    }
    var universityResults by remember { mutableStateOf<List<EducationOption>>(emptyList()) }
    var programResults    by remember { mutableStateOf<List<EducationOption>>(emptyList()) }
    var universityLoading by remember { mutableStateOf(false) }
    var programLoading    by remember { mutableStateOf(false) }
    var universityError   by remember { mutableStateOf<String?>(null) }
    var programError      by remember { mutableStateOf<String?>(null) }

    val graduateLabel = stringResource(R.string.graduate)
    val classOptions  = listOf("1", "2", "3", "4", graduateLabel)
    val isFormValid   = fullName.isNotBlank() && universityId.isNotBlank() &&
            programId.isNotBlank() && selectedClass.isNotBlank()

    LaunchedEffect(university, universityId) {
        if (universityId.isNotBlank() || university.trim().length < 2) {
            universityResults = emptyList(); universityLoading = false; return@LaunchedEffect
        }
        universityResults = emptyList(); universityLoading = true; universityError = null
        delay(350)
        EducationCatalogService.searchUniversities(university)
            .onSuccess { universityResults = it }
            .onFailure { universityResults = emptyList(); universityError = it.message ?: "Üniversiteler alınamadı." }
        universityLoading = false
    }

    LaunchedEffect(department, programId) {
        if (programId.isNotBlank() || department.trim().length < 2) {
            programResults = emptyList(); programLoading = false; return@LaunchedEffect
        }
        programResults = emptyList(); programLoading = true; programError = null
        delay(350)
        EducationCatalogService.searchPrograms(department)
            .onSuccess { programResults = it }
            .onFailure { programResults = emptyList(); programError = it.message ?: "Programlar alınamadı." }
        programLoading = false
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
                .imePadding()
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
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
                    "Akıllı profil",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            // ── Step progress bar ─────────────────────────────────────────────
            LinearProgressIndicator(
                progress = { 0.33f },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                // Step label
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "1 / 3 • EĞİTİM BİLGİLERİN",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Seni doğru verilerle tanıyalım",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Üniversiteni ve ÖSYM'de kayıtlı programını seç. Migo kalan analizi AI ile hazırlayacak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(28.dp))

                // ── Full name ─────────────────────────────────────────────────
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.full_name)) },
                    placeholder = { Text(stringResource(R.string.full_name_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // ── University search ─────────────────────────────────────────
                EducationSearchField(
                    value = university,
                    onValueChange = { university = it; universityId = ""; universityError = null },
                    label = "Üniversite",
                    placeholder = "Örn. İstanbul, Boğaziçi…",
                    results = universityResults,
                    isLoading = universityLoading,
                    errorMessage = universityError,
                    selected = universityId.isNotBlank(),
                    onOptionSelected = { option ->
                        university = option.name; universityId = option.id; universityResults = emptyList()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // ── Program search ────────────────────────────────────────────
                EducationSearchField(
                    value = department,
                    onValueChange = { department = it; programId = ""; scoreType = ""; programError = null },
                    label = "Bölüm / program",
                    placeholder = "Örn. bilgisa, psikoloji, hukuk…",
                    results = programResults,
                    isLoading = programLoading,
                    errorMessage = programError,
                    selected = programId.isNotBlank(),
                    onOptionSelected = { option ->
                        department = option.name; programId = option.id; scoreType = option.meta; programResults = emptyList()
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Veri kaynağı: YÖK Atlas • Lisans ve önlisans programları",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))

                // ── Class level ───────────────────────────────────────────────
                Text("Sınıfın", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    classOptions.forEach { option ->
                        val isSelected = selectedClass == option
                        val label = if (option == graduateLabel) option
                        else stringResource(R.string.class_option, option)
                        Surface(
                            onClick = { selectedClass = option },
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shadowElevation = if (isSelected) 0.dp else 1.dp
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── CTA Button ────────────────────────────────────────────────
                Button(
                    onClick = {
                        onContinueClick(
                            UserProfile(
                                fullName    = fullName.trim(),
                                university  = university.trim(),
                                department  = department.trim(),
                                classLevel  = if (selectedClass == graduateLabel) selectedClass
                                              else "$selectedClass. sınıf",
                                universityId = universityId,
                                programId   = programId,
                                scoreType   = scoreType
                            )
                        )
                    },
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isFormValid) Modifier.background(
                                    Brush.linearGradient(listOf(GradientHeroMid, GradientHeroEnd))
                                ) else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "AI analizini başlat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isFormValid) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Listeden seçim yapman gerekir; böylece bölüm adı yanlış veya eksik kalmaz.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EducationSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    results: List<EducationOption>,
    isLoading: Boolean,
    errorMessage: String?,
    selected: Boolean,
    onOptionSelected: (EducationOption) -> Unit
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    selected  -> Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Seçildi",
                        tint = MigoSuccess
                    )
                }
            },
            supportingText = if (!selected && value.length in 1..1) {
                { Text("Aramak için en az 2 harf yaz.") }
            } else null
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        if (!selected && value.trim().length >= 2 && !isLoading && results.isEmpty() && errorMessage == null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Eşleşme bulunamadı. Yazımı kontrol et veya daha kısa ara.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = results.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit  = shrinkVertically() + fadeOut()
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 8.dp
            ) {
                Column {
                    results.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOptionSelected(option) }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    option.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (option.meta.isNotBlank()) {
                                    Text(
                                        option.meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        }
                        if (index < results.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }
}
