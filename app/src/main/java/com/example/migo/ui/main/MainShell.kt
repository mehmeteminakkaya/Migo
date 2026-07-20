package com.example.migo.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.migo.data.local.PdfCvExtractor
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CareerTask
import com.example.migo.data.model.CareerTaskPlanner
import com.example.migo.data.model.CoachMessage
import com.example.migo.data.model.CvAnalysis
import com.example.migo.data.model.UserProfile
import com.example.migo.data.remote.NvidiaCareerService
import com.example.migo.ui.theme.GradientHeroEnd
import com.example.migo.ui.theme.GradientHeroMid
import com.example.migo.ui.theme.GradientHeroStart
import com.example.migo.ui.theme.MigoCoralAccent
import com.example.migo.ui.theme.MigoGold
import com.example.migo.ui.theme.MigoTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MainShell(
    session: CareerSession,
    onSessionChange: (CareerSession) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    val latestSession by rememberUpdatedState(session)
    val shellScope = rememberCoroutineScope()
    val context = LocalContext.current
    val coachGreeting = remember(session.profile.fullName, session.targetCareer) {
        val firstName = session.profile.fullName.substringBefore(" ")
        "Merhaba $firstName! ${session.targetCareer} hedefin için buradayım. " +
            "Bugün birlikte net ve uygulanabilir bir adım atalım."
    }
    val coachMessages = remember(session.targetCareer) {
        mutableStateListOf<CoachMessage>().apply {
            addAll(
                session.coachMessages.ifEmpty {
                    listOf(CoachMessage(coachGreeting, isFromUser = false))
                }
            )
        }
    }
    var coachIsLoading by remember { mutableStateOf(false) }
    var cvText by rememberSaveable { mutableStateOf("") }
    var cvFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var cvAnalysis by remember { mutableStateOf<CvAnalysis?>(null) }
    var cvError by remember { mutableStateOf<String?>(null) }
    var cvIsLoading by remember { mutableStateOf(false) }
    var cvIsReading by remember { mutableStateOf(false) }
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        cvIsReading = true
        cvError = null
        shellScope.launch {
            PdfCvExtractor.extract(context, uri)
                .onSuccess { extracted ->
                    cvText = extracted.text
                    cvFileName = "${extracted.fileName} • ${extracted.pageCount} sayfa"
                    cvAnalysis = null
                }
                .onFailure { error ->
                    cvError = error.message ?: "PDF dosyası okunamadı."
                }
            cvIsReading = false
        }
    }

    val persistCoachMessages: () -> Unit = {
        onSessionChange(
            latestSession.copy(coachMessages = coachMessages.takeLast(MAX_SAVED_COACH_MESSAGES))
        )
    }
    val askCoach: (String) -> Unit = ask@{ rawQuestion ->
        val question = rawQuestion.trim()
        if (question.isEmpty() || coachIsLoading) return@ask
        val previousHistory = coachMessages.toList()
        coachMessages.add(CoachMessage(question, isFromUser = true))
        persistCoachMessages()
        coachIsLoading = true
        shellScope.launch {
            NvidiaCareerService.askCoach(latestSession, previousHistory, question)
                .onSuccess { answer ->
                    coachMessages.add(CoachMessage(answer, isFromUser = false))
                }
                .onFailure { error ->
                    coachMessages.add(
                        CoachMessage(
                            error.message ?: "Şu an yanıt veremiyorum. Lütfen tekrar dene.",
                            isFromUser = false
                        )
                    )
                }
            coachIsLoading = false
            persistCoachMessages()
        }
    }
    val analyzeCv: () -> Unit = analyze@{
        val text = cvText.trim()
        if (text.length < 80 || cvIsLoading) return@analyze
        cvIsLoading = true
        cvError = null
        shellScope.launch {
            NvidiaCareerService.analyzeCv(latestSession, text)
                .onSuccess { cvAnalysis = it }
                .onFailure { error ->
                    cvError = error.message ?: "CV şu anda analiz edilemedi. Lütfen tekrar dene."
                }
            cvIsLoading = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { MigoFloatingNavBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it }) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.HOME -> HomeScreen(
                    session = session,
                    onToggleTask = { taskId ->
                        onSessionChange(session.toggleTask(taskId))
                    },
                    onOpenPlan = { selectedTab = MainTab.PLAN },
                    onOpenCoach = { selectedTab = MainTab.COACH }
                )
                MainTab.PLAN -> RoadmapScreen(
                    session = session,
                    onToggleTask = { taskId ->
                        onSessionChange(session.toggleTask(taskId))
                    }
                )
                MainTab.TOOLS -> CareerToolsScreen(
                    session = session,
                    cvText = cvText,
                    result = cvAnalysis,
                    isAnalyzing = cvIsLoading,
                    isReadingPdf = cvIsReading,
                    pdfFileName = cvFileName,
                    errorMessage = cvError,
                    onCvTextChange = { updatedText ->
                        cvText = updatedText
                        cvAnalysis = null
                        cvError = null
                    },
                    onPickPdf = { pdfPicker.launch(arrayOf("application/pdf")) },
                    onAnalyze = analyzeCv
                )
                MainTab.COACH -> CoachScreen(
                    session = session,
                    messages = coachMessages,
                    isLoading = coachIsLoading,
                    onQuestion = askCoach,
                    onNewConversation = {
                        if (!coachIsLoading) {
                            coachMessages.clear()
                            coachMessages.add(CoachMessage(coachGreeting, isFromUser = false))
                            persistCoachMessages()
                        }
                    }
                )
                MainTab.PROFILE -> ProfileOverviewScreen(session = session, onReset = onReset)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    session: CareerSession,
    onToggleTask: (String) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenCoach: () -> Unit
) {
    val tasks = session.effectiveTasks()
    val completedTaskIds = session.effectiveCompletedTaskIds()
    val totalSteps = tasks.size.coerceAtLeast(1)
    val progress = completedTaskIds.size.toFloat() / totalSteps
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
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        // ── Greeting header ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "$greeting, $firstName 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text  = "Hedefine bir adım daha yaklaş.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Streak badge
            if (session.streakCount > 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MigoGold.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint     = MigoGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${session.streakCount}",
                            fontWeight = FontWeight.ExtraBold,
                            color  = MigoGold,
                            style  = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            Brush.linearGradient(listOf(GradientHeroMid, GradientHeroEnd))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = firstName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        TargetHeroCard(session = session, progress = progress, onOpenPlan = onOpenPlan)

        Spacer(modifier = Modifier.height(22.dp))
        SectionHeader(title = "Bugünün odağı", action = "Planı gör", onAction = onOpenPlan)
        Spacer(modifier = Modifier.height(10.dp))

        if (nextTask != null) {
            DailyFocusCard(
                task = nextTask,
                stepNumber = tasks.indexOf(nextTask) + 1,
                onComplete = { onToggleTask(nextTask.id) }
            )
        } else {
            CompletedPlanCard()
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProgressSnapshotCard(
            progress  = progress,
            completed = completedTaskIds.size,
            total     = tasks.size,
            streak    = session.streakCount
        )

        Spacer(modifier = Modifier.height(16.dp))
        CoachNudgeCard(onClick = onOpenCoach)
        Spacer(modifier = Modifier.height(22.dp))
    }
}

@Composable
private fun TargetHeroCard(session: CareerSession, progress: Float, onOpenPlan: () -> Unit) {
    val gradient = Brush.linearGradient(
        listOf(GradientHeroStart, GradientHeroMid, GradientHeroEnd)
    )
    Card(onClick = onOpenPlan, shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Flag,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "AKTİF HEDEF",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Planı aç",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                session.targetCareer,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Yol haritası ilerlemesi",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.22f)
            )
        }
    }
}

@Composable
private fun DailyFocusCard(
    task: CareerTask,
    stepNumber: Int,
    onComplete: () -> Unit
) {
    Card(
        onClick = onComplete,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "ADIM $stepNumber",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = Color(0xFFA56500),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    "${task.durationMinutes} dk",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    task.category,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "Tamamla",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressSnapshotCard(
    progress: Float,
    completed: Int,
    total: Int,
    streak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 7.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Haftalık ritmin", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    MiniMetric(
                        icon = Icons.Rounded.CheckCircle,
                        value = "$completed/$total",
                        label = "görev"
                    )
                    MiniMetric(
                        icon = Icons.Rounded.EmojiEvents,
                        value = streak.toString(),
                        label = "gün seri"
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniMetric(icon: ImageVector, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(value, style = MaterialTheme.typography.labelLarge)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CoachNudgeCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Migo AI Koç", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Takıldığın yerde sana özel bir sonraki adımı sor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RoadmapScreen(session: CareerSession, onToggleTask: (String) -> Unit) {
    val tasks = session.effectiveTasks()
    val completedTaskIds = session.effectiveCompletedTaskIds()
    val total = tasks.size.coerceAtLeast(1)
    val progress = completedTaskIds.size.toFloat() / total
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Yol haritan", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Hedefine giden adımları kendi ritminde tamamla.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("AI planı", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF25215F), Color(0xFF4F46E5))
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "HEDEF ROTA",
                            color = Color.White.copy(alpha = 0.68f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            session.targetCareer,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "${(progress * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${completedTaskIds.size} tamamlandı • ${tasks.size - completedTaskIds.size} adım kaldı",
                    color = Color.White.copy(alpha = 0.74f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Adımlar", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(10.dp))

        tasks.forEachIndexed { index, task ->
            val completed = task.id in completedTaskIds
            RoadmapTaskCard(
                step = index + 1,
                task = task,
                completed = completed,
                onClick = { onToggleTask(task.id) }
            )
            if (index != tasks.lastIndex) Spacer(modifier = Modifier.height(10.dp))
        }
        if (progress == 1f) {
            Spacer(modifier = Modifier.height(12.dp))
            CompletedPlanCard()
        }
        Spacer(modifier = Modifier.height(22.dp))
    }
}

@Composable
private fun RoadmapTaskCard(
    step: Int,
    task: CareerTask,
    completed: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
            } else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (completed) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.26f)
            } else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.78f)
        )
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (completed) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (completed) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Tamamlandı",
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                } else {
                    Text(
                        step.toString().padStart(2, '0'),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (completed) "TAMAMLANDI" else "ADIM $step",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (completed) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${task.durationMinutes} dk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    task.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CoachScreen(
    session: CareerSession,
    messages: List<CoachMessage>,
    isLoading: Boolean,
    onQuestion: (String) -> Unit,
    onNewConversation: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    val sendQuestion: (String) -> Unit = send@{ rawQuestion ->
        val question = rawQuestion.trim()
        if (question.isEmpty() || isLoading) return@send
        input = ""
        onQuestion(question)
    }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        CoachHeader(
            targetCareer = session.targetCareer,
            onNewConversation = {
                if (!isLoading) {
                    input = ""
                    onNewConversation()
                }
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages) { message -> CoachMessageCard(message) }

            if (messages.size == 1) {
                item {
                    CoachQuickActions(
                        onQuestion = sendQuestion,
                        enabled = !isLoading
                    )
                }
            }

            if (isLoading) {
                item { CoachThinkingCard() }
            } else if (messages.size > 2 && !messages.last().isFromUser) {
                item {
                    CoachFollowUpActions(onQuestion = sendQuestion)
                }
            }
        }

        CoachComposer(
            input = input,
            onInputChange = { input = it },
            onSend = { sendQuestion(input) },
            enabled = !isLoading
        )
    }
}

@Composable
private fun CoachHeader(
    targetCareer: String,
    onNewConversation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3159C6), Color(0xFF7048E8))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Migo AI Koç",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF20B26B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Çevrimiçi • Kişisel kariyer koçun",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onNewConversation) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "Yeni sohbet",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(9.dp))
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Aktif hedef",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    targetCareer,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

@Composable
private fun CoachMessageCard(message: CoachMessage) {
    if (message.isFromUser) {
        UserMessageBubble(message.text)
    } else {
        CoachAnswerCard(message.text)
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 5.dp
            ),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun CoachAnswerCard(text: String) {
    val content = remember(text) { parseCoachContent(text) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF3159C6), Color(0xFF7048E8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "Migo'nun önerisi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (content.steps.isEmpty()) "Sana özel yanıt" else "${content.steps.size} adımlık eylem planı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            if (content.intro.isNotBlank()) {
                Text(
                    content.intro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
                if (content.steps.isNotEmpty()) Spacer(modifier = Modifier.height(16.dp))
            }

            content.steps.forEachIndexed { index, step ->
                CoachTimelineStep(
                    step = step,
                    index = index,
                    isLast = index == content.steps.lastIndex
                )
            }

            if (content.steps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFFF3D6),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFE58A00),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Hepsini birden değil, bugün yalnızca ilk adımı tamamla.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6C4A00)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachTimelineStep(
    step: CoachStep,
    index: Int,
    isLast: Boolean
) {
    val accentColors = listOf(
        Color(0xFF3159C6),
        Color(0xFF7048E8),
        Color(0xFF008B7A),
        Color(0xFFE56A2C),
        Color(0xFFB53D73)
    )
    val accent = accentColors[index % accentColors.size]

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    step.number.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(70.dp)
                        .background(accent.copy(alpha = 0.25f))
                )
            }
        }
        Spacer(modifier = Modifier.width(11.dp))
        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 4.dp else 12.dp),
            color = accent.copy(alpha = 0.075f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    step.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                if (step.detail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        step.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachQuickActions(
    onQuestion: (String) -> Unit,
    enabled: Boolean
) {
    val prompts = listOf(
        CoachPrompt("🧭", "Yol haritam", "Nereden başlayacağını netleştir", "Bana adım adım kariyer yol haritası çıkar."),
        CoachPrompt("⚡", "Bugünün görevi", "Hemen yapabileceğin tek adım", "Bugün hedefim için ne yapmalıyım?"),
        CoachPrompt("📄", "CV güçlendir", "Profilini daha etkili yap", "CV'mi güçlendirmek için öneriler ver."),
        CoachPrompt("🎯", "Mülakat provası", "Sorulara birlikte hazırlan", "Hedef rolüm için mülakat provası yapalım.")
    )

    Column {
        Text(
            "Ne üzerinde çalışalım?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            "Bir kart seç, Migo senin için planlasın.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        prompts.chunked(2).forEach { rowPrompts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowPrompts.forEach { prompt ->
                    CoachPromptCard(
                        prompt = prompt,
                        onClick = { onQuestion(prompt.question) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CoachPromptCard(
    prompt: CoachPrompt,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .heightIn(min = 126.dp)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(prompt.emoji, fontSize = 25.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                prompt.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                prompt.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun CoachThinkingCard() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Planın hazırlanıyor", fontWeight = FontWeight.Bold)
                Text(
                    "Migo hedefini ve profilini değerlendiriyor…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoachFollowUpActions(onQuestion: (String) -> Unit) {
    Column {
        Text(
            "Devam etmek ister misin?",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { onQuestion("Bu önerileri 7 günlük uygulanabilir bir plana çevir.") },
                label = { Text("📅 7 günlük plan") }
            )
            AssistChip(
                onClick = { onQuestion("İlk adımı daha detaylı anlat.") },
                label = { Text("🔎 İlk adımı aç") }
            )
        }
    }
}

@Composable
private fun CoachComposer(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Hedefinle ilgili bir şey sor…") },
                shape = RoundedCornerShape(22.dp),
                maxLines = 4,
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = input.isNotBlank() && enabled,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (input.isNotBlank() && enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Gönder",
                    tint = if (input.isNotBlank() && enabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

private data class CoachPrompt(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val question: String
)

private data class CoachStep(
    val number: Int,
    val title: String,
    val detail: String
)

private data class ParsedCoachContent(
    val intro: String,
    val steps: List<CoachStep>
)

private fun parseCoachContent(text: String): ParsedCoachContent {
    val numberedItem = Regex("(?m)^\\s*(\\d+)[.)]\\s+")
    val matches = numberedItem.findAll(text).toList()
    if (matches.size < 2) {
        return ParsedCoachContent(intro = cleanCoachMarkdown(text), steps = emptyList())
    }

    val intro = cleanCoachMarkdown(text.substring(0, matches.first().range.first))
    val steps = matches.mapIndexed { index, match ->
        val contentStart = match.range.last + 1
        val contentEnd = matches.getOrNull(index + 1)?.range?.first ?: text.length
        val clean = cleanCoachMarkdown(text.substring(contentStart, contentEnd))
        val colon = clean.indexOf(':')
        val sentence = clean.indexOf('.')
        val splitAt = when {
            colon in 1..80 -> colon
            sentence in 1..70 -> sentence
            else -> -1
        }
        val title = if (splitAt > 0) clean.substring(0, splitAt).trim() else "Adım ${index + 1}"
        val detail = if (splitAt > 0) clean.substring(splitAt + 1).trim() else clean
        CoachStep(
            number = match.groupValues[1].toIntOrNull() ?: index + 1,
            title = title,
            detail = detail
        )
    }
    return ParsedCoachContent(intro = intro, steps = steps)
}

private fun cleanCoachMarkdown(text: String): String = text
    .replace(Regex("\\[([^]]+)]\\(https?://[^)]+\\)")) { match -> match.groupValues[1] }
    .replace("**", "")
    .replace("__", "")
    .replace("`", "")
    .replace(Regex("(?m)^\\s*[-•]\\s+"), "")
    .replace("*", "")
    .replace(Regex("[ \\t]+"), " ")
    .replace(Regex("\\n{3,}"), "\n\n")
    .trim()

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileOverviewScreen(session: CareerSession, onReset: () -> Unit) {
    var showResetDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Kariyer profilin", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Migo'nun sana özel çalıştığı merkez.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI profil", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF25215F), Color(0xFF4F46E5))
                        )
                    )
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        session.profile.fullName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.profile.fullName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        session.profile.department,
                        color = Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        session.profile.university,
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        ProfileInfoCard(Icons.Rounded.Flag, "Hedef meslek", session.targetCareer)
        Spacer(modifier = Modifier.height(10.dp))
        ProfileInfoCard(Icons.Rounded.CalendarMonth, "Eğitim aşaması", session.profile.classLevel)

        Spacer(modifier = Modifier.height(22.dp))
        Text("Kariyer odakların", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            "Profiline göre öne çıkan çalışma alanları",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            session.interests.forEach { ProfileTag(it) }
        }

        Spacer(modifier = Modifier.height(22.dp))
        Text("Gelişim alanların", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            "AI analizinin önerdiği yetkinlik seti",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            session.skills.forEach { SkillProfileRow(it) }
        }

        Spacer(modifier = Modifier.height(22.dp))
        TextButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Profili ve ilerlemeyi sıfırla",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Her şeyi sıfırla?") },
            text = { Text("Profilin, hedefin ve tamamladığın adımlar cihazdan silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onReset()
                }) { Text("Sıfırla", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun ProfileInfoCard(icon: ImageVector, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProfileTag(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.74f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SkillProfileRow(label: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun CompletedPlanCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Harika! Mevcut yol haritandaki tüm adımları tamamladın.", fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun CareerSession.effectiveTasks() = tasks.ifEmpty {
    CareerTaskPlanner.build(targetCareer, analysis.nextSteps)
}

private fun CareerSession.effectiveCompletedTaskIds(): Set<String> {
    if (completedTaskIds.isNotEmpty() || completedSteps.isEmpty()) return completedTaskIds
    val taskList = effectiveTasks()
    return completedSteps.mapNotNull { taskList.getOrNull(it)?.id }.toSet()
}

private fun CareerSession.toggleTask(taskId: String): CareerSession {
    val taskList = effectiveTasks()
    val updated = effectiveCompletedTaskIds().toMutableSet()
    val wasCompleted = !updated.add(taskId)
    if (wasCompleted) updated.remove(taskId)

    val today = dateKey(Calendar.getInstance())
    val streak = if (!wasCompleted && lastCompletionDate != today) {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        if (lastCompletionDate == dateKey(yesterday)) streakCount + 1 else 1
    } else streakCount

    return copy(
        tasks = taskList,
        completedTaskIds = updated,
        streakCount = streak,
        lastCompletionDate = if (!wasCompleted) today else lastCompletionDate
    )
}

private fun dateKey(calendar: Calendar): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

private const val MAX_SAVED_COACH_MESSAGES = 50

private enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Ana Sayfa", Icons.Rounded.Home),
    PLAN("Planım", Icons.Rounded.Map),
    TOOLS("Araçlar", Icons.Rounded.Star),
    COACH("AI Koç", Icons.AutoMirrored.Rounded.Chat),
    PROFILE("Profil", Icons.Rounded.Person)
}

@Composable
private fun MigoFloatingNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 20.dp,
            tonalElevation  = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    val scale by animateFloatAsState(
                        targetValue   = if (selected) 1.08f else 1f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy),
                        label         = "nav_scale"
                    )
                    val iconColor by animateColorAsState(
                        targetValue = if (selected) Color.White
                                      else MaterialTheme.colorScheme.onSurfaceVariant,
                        label       = "nav_icon_color"
                    )
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(20.dp))
                            .then(
                                if (selected) Modifier.background(
                                    Brush.linearGradient(listOf(GradientHeroMid, GradientHeroEnd))
                                ) else Modifier
                            )
                            .clickable { onTabSelected(tab) }
                            .padding(
                                horizontal = if (selected) 18.dp else 14.dp,
                                vertical = 10.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    tint     = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    tab.label,
                                    color  = Color.White,
                                    style  = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint     = iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainShellPreview() {
    MigoTheme {
        MainShell(
            session = CareerSession(
                profile = UserProfile("Mehmet Akkaya", "İstanbul Üniversitesi", "Bilgisayar Mühendisliği", "3. sınıf"),
                interests = listOf("Teknoloji", "Tasarım", "Veri"),
                skills = listOf("Problem çözme", "Analitik düşünme", "İletişim"),
                analysis = CareerAnalysis(
                    "Teknoloji ile insan ihtiyaçlarını buluşturabilirsin.",
                    "İlgi ve yetkinliklerin güçlü bir başlangıç sunuyor.",
                    listOf("Analitik düşünme", "Merak", "İletişim"),
                    listOf(CareerRecommendation("Ürün Yöneticisi", "Uygun bir hedef.")),
                    listOf("Temel bir kursu tamamla.", "Portföy projesi oluştur.", "Bir profesyonelle görüş.", "CV'ni güncelle.")
                ),
                targetCareer = "Ürün Yöneticisi"
            ),
            onSessionChange = { },
            onReset = { }
        )
    }
}
