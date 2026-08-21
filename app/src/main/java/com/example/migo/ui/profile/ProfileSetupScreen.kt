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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.migo.R
import com.example.migo.data.model.EducationOption
import com.example.migo.data.model.UserProfile
import com.example.migo.data.remote.EducationCatalogService
import com.example.migo.ui.theme.MigoSuccess
import kotlinx.coroutines.delay
import java.util.Locale

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

    LaunchedEffect(department, programId, universityId) {
        if (universityId.isBlank() || programId.isNotBlank() || department.trim().length < 2) {
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
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "Eğitim bilgilerin",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
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
                Text(
                    "1 / 3 • EĞİTİM PROFİLİ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Eğitim yolunu seç",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Üniversiteni, ÖSYM programını ve sınıfını seç. Migo bölümünün branşlarını ve bu yıl için yol haritanı hazırlayacak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(28.dp))

                // ── Full name ─────────────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    TextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.full_name)) },
                        placeholder = { Text(stringResource(R.string.full_name_hint)) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.PersonOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // ── University search ─────────────────────────────────────────
                EducationSearchField(
                    value = university,
                    onValueChange = {
                        university = it
                        universityId = ""
                        department = ""
                        programId = ""
                        scoreType = ""
                        universityError = null
                    },
                    step = "01",
                    label = "Üniversite",
                    placeholder = "Üniversite adıyla ara",
                    icon = Icons.Rounded.AccountBalance,
                    results = universityResults,
                    isLoading = universityLoading,
                    errorMessage = universityError,
                    selected = universityId.isNotBlank(),
                    enabled = true,
                    onOptionSelected = { option ->
                        university = option.name
                        universityId = option.id
                        department = ""
                        programId = ""
                        scoreType = ""
                        universityResults = emptyList()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // ── Program search ────────────────────────────────────────────
                EducationSearchField(
                    value = department,
                    onValueChange = { department = it; programId = ""; scoreType = ""; programError = null },
                    step = "02",
                    label = "Bölüm / program",
                    placeholder = if (universityId.isBlank()) "Önce üniversiteni seç" else "Bölüm adıyla ara",
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    results = programResults,
                    isLoading = programLoading,
                    errorMessage = programError,
                    selected = programId.isNotBlank(),
                    enabled = universityId.isNotBlank(),
                    onOptionSelected = { option ->
                        department = option.name; programId = option.id; scoreType = option.meta; programResults = emptyList()
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "YÖK Atlas kataloğu • Lisans ve önlisans programları",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))

                // ── Class level ───────────────────────────────────────────────
                Text(
                    "Sınıfın",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (universityId.isNotBlank() && programId.isNotBlank() && selectedClass.isBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "Son adım: Yol haritanı sınıf seviyene göre hazırlayabilmemiz için sınıfını seç.",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
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
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
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
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        if (universityId.isNotBlank() && programId.isNotBlank() && selectedClass.isBlank()) {
                            "Devam etmek için sınıfını seç"
                        } else {
                            "Program haritamı oluştur"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Seçimler YÖK Atlas sonuçlarından doğrulanır. Yol haritan sınıf seviyene göre değişir.",
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
    step: String,
    label: String,
    placeholder: String,
    icon: ImageVector,
    results: List<EducationOption>,
    isLoading: Boolean,
    errorMessage: String?,
    selected: Boolean,
    enabled: Boolean,
    onOptionSelected: (EducationOption) -> Unit
) {
    if (selected) {
        SelectedEducationCard(
            value = value,
            step = step,
            label = label,
            icon = icon,
            onEdit = { onValueChange("") }
        )
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "$step  •  ${label.uppercase(Locale.forLanguageTag("tr-TR"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (enabled) "Yazarak ara ve listeden seç" else "Önce ilk seçimi tamamla",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                placeholder = { Text(placeholder) },
                singleLine = true,
                shape = RoundedCornerShape(6.dp),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            if (value.length == 1) {
                Text(
                    "Aramak için en az 2 harf yaz.",
                    modifier = Modifier.padding(start = 6.dp, top = 7.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (errorMessage != null) {
                Text(
                    errorMessage,
                    modifier = Modifier.padding(start = 6.dp, top = 7.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (value.trim().length >= 2 && !isLoading && results.isEmpty() && errorMessage == null) {
                Text(
                    "Eşleşme bulunamadı. Daha kısa veya farklı bir ifade dene.",
                    modifier = Modifier.padding(start = 6.dp, top = 7.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = results.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    results.take(5).forEachIndexed { index, option ->
                        Surface(
                            onClick = { onOptionSelected(option) },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        option.name.take(1).uppercase(Locale.forLanguageTag("tr-TR")),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(11.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(option.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    if (option.meta.isNotBlank() && option.meta != "Üniversite") {
                                        Text(
                                            option.meta,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = "Seç",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        if (index < minOf(results.size, 5) - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                        }
                    }
                    if (results.size > 5) {
                        Text(
                            "${results.size - 5} sonuç daha var • Aramanı netleştir",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedEducationCard(
    value: String,
    step: String,
    label: String,
    icon: ImageVector,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "$step • ${label.uppercase(Locale.forLanguageTag("tr-TR"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "YÖK Atlas kaydı doğrulandı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MigoSuccess.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MigoSuccess,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "Seçildi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MigoSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(11.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
            ) {
                Row(
                    modifier = Modifier.padding(start = 13.dp, end = 5.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        value,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Değiştir")
                    }
                }
            }
        }
    }
}
