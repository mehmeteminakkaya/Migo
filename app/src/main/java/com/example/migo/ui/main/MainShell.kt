package com.example.migo.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.migo.data.local.PdfCvExtractor
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.AiContentSanitizer
import com.example.migo.data.model.CareerActionPlan
import com.example.migo.data.model.CareerPlanAction
import com.example.migo.data.model.CareerPlanWeek
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CareerTask
import com.example.migo.data.model.CareerTaskPlanner
import com.example.migo.data.model.CoachMessage
import com.example.migo.data.model.CvAnalysis
import com.example.migo.data.model.DailyCareerInsight
import com.example.migo.data.model.UserProfile
import com.example.migo.data.remote.NvidiaCareerService
import com.example.migo.data.remote.MigoLegalLinks
import com.example.migo.ui.theme.GradientHeroEnd
import com.example.migo.ui.theme.GradientHeroMid
import com.example.migo.ui.theme.GradientHeroStart
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
    var planIsLoading by remember { mutableStateOf(false) }
    var planError by remember { mutableStateOf<String?>(null) }
    var insightIsLoading by remember { mutableStateOf(false) }
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

    val createActionPlan: () -> Unit = createPlan@{
        if (planIsLoading) return@createPlan
        planIsLoading = true
        planError = null
        shellScope.launch {
            NvidiaCareerService.createActionPlan(latestSession)
                .onSuccess { plan ->
                    onSessionChange(latestSession.copy(actionPlan = plan))
                    selectedTab = MainTab.PLAN
                }
                .onFailure { error ->
                    planError = error.message ?: "30 günlük plan şu anda hazırlanamadı."
                }
            planIsLoading = false
        }
    }

    val today = remember { dateKey(Calendar.getInstance()) }
    LaunchedEffect(session.targetCareer, session.dailyInsight?.dateKey, today) {
        if (session.dailyInsight?.dateKey == today || insightIsLoading) return@LaunchedEffect
        insightIsLoading = true
        NvidiaCareerService.createDailyInsight(session, today)
            .onSuccess { insight ->
                onSessionChange(latestSession.copy(dailyInsight = insight))
            }
        insightIsLoading = false
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
                    onOpenTools = { selectedTab = MainTab.TOOLS },
                    onOpenCoach = { selectedTab = MainTab.COACH },
                    isPlanLoading = planIsLoading,
                    planError = planError,
                    insightIsLoading = insightIsLoading,
                    onCreatePlan = createActionPlan,
                    onAskCoach = { question ->
                        selectedTab = MainTab.COACH
                        askCoach(question)
                    }
                )
                MainTab.PLAN -> RoadmapScreen(
                    session = session,
                    isPlanLoading = planIsLoading,
                    errorMessage = planError,
                    onGeneratePlan = createActionPlan,
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
            if (session.streakCount > 0) {
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
                        "${session.streakCount} gün",
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

        Spacer(modifier = Modifier.height(24.dp))
        CalmInsightCard(
            insight = session.dailyInsight,
            isLoading = insightIsLoading,
            fallbackAction = nextTask?.title.orEmpty(),
            onAskCoach = onAskCoach
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onOpenPlan) { Text("Tüm planı gör") }
            TextButton(onClick = onOpenCoach) { Text("AI koça sor") }
        }
        Spacer(modifier = Modifier.height(22.dp))
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

@Composable
private fun CareerCompassHero(
    session: CareerSession,
    progress: Float,
    milestonePeriod: String,
    milestoneTitle: String,
    onOpenPlan: () -> Unit
) {
    val gradient = Brush.linearGradient(
        listOf(GradientHeroStart, GradientHeroMid, GradientHeroEnd)
    )
    Card(onClick = onOpenPlan, shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(18.dp)
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
                            "MIGO KARİYER GPS",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
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
                "Rotan hazır",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Genel tavsiye değil; bölümün, sınıfın ve hedefin için sıralanmış rota.",
                color = Color.White.copy(alpha = 0.76f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(13.dp))
            Surface(
                color = Color.White.copy(alpha = 0.11f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 330.dp) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            CareerRoutePoint(
                                icon = Icons.Rounded.School,
                                label = "ŞU AN",
                                value = "${session.profile.classLevel} • ${session.profile.department}"
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 11.dp),
                                color = Color.White.copy(alpha = 0.18f)
                            )
                            CareerRoutePoint(
                                icon = Icons.Rounded.Flag,
                                label = "HEDEF",
                                value = session.targetCareer
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CareerRoutePoint(
                                icon = Icons.Rounded.School,
                                label = "ŞU AN",
                                value = "${session.profile.classLevel}\n${session.profile.department}",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.64f),
                                modifier = Modifier.padding(horizontal = 9.dp).size(19.dp)
                            )
                            CareerRoutePoint(
                                icon = Icons.Rounded.Flag,
                                label = "HEDEF",
                                value = session.targetCareer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Rota ilerlemesi",
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

            if (milestoneTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Rounded.RocketLaunch,
                        contentDescription = null,
                        tint = MigoGold,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (milestonePeriod.isBlank()) "SONRAKİ DURAK" else "SONRAKİ DURAK • ${milestonePeriod.uppercase()}",
                            color = MigoGold,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            milestoneTitle,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun CareerRoutePoint(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = Color.White.copy(alpha = 0.13f),
            shape = RoundedCornerShape(11.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(8.dp).size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.labelSmall)
            Text(
                value,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DailyInsightCard(
    insight: DailyCareerInsight?,
    isLoading: Boolean,
    fallbackAction: String,
    onAskCoach: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(GradientHeroMid, GradientHeroEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading && insight == null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "MIGO AI • BUGÜNÜN İÇGÖRÜSÜ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        insight?.title ?: "Kişisel yönlendirmen hazırlanıyor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(11.dp))
            Text(
                insight?.message
                    ?: "Bölümün, sınıfın ve sıradaki adımın birlikte değerlendiriliyor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 21.sp
            )
            val rawActionLabel = insight?.actionLabel.orEmpty().ifBlank { fallbackAction }
            val actionLabel = AiContentSanitizer.taskText(rawActionLabel).title
            if (actionLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BUGÜN YAP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(actionLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (insight?.coachPrompt?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(onClick = { onAskCoach(insight.coachPrompt) }) {
                    Text("Bunu AI koçla planla")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun NextBestActionCard(
    task: CareerTask,
    stepNumber: Int,
    focusTitle: String,
    focusDescription: String,
    onOpenPlan: () -> Unit,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "AKTİF ADIM $stepNumber",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                shape = RoundedCornerShape(15.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Rounded.AutoGraph,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Neden şimdi? • $focusTitle",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            focusDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenPlan, modifier = Modifier.weight(1f)) {
                    Text("Detayı gör")
                }
                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1.35f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tamamladım", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WeeklySprintCard(
    tasks: List<CareerTask>,
    completedTaskIds: Set<String>,
    onToggleTask: (String) -> Unit
) {
    val completed = tasks.count { it.id in completedTaskIds }
    val progress = if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (completed == tasks.size && tasks.isNotEmpty()) "Sprint tamamlandı" else "Küçük adımlar, görünür ilerleme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$completed görev bitti • ${tasks.sumOf { it.durationMinutes }} dk toplam",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(11.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            tasks.forEachIndexed { index, task ->
                WeeklyMissionRow(
                    task = task,
                    completed = task.id in completedTaskIds,
                    onToggle = { onToggleTask(task.id) }
                )
                if (index != tasks.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 44.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyMissionRow(task: CareerTask, completed: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    if (completed) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            } else {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (completed) FontWeight.Normal else FontWeight.SemiBold,
                color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${task.category} • ${task.durationMinutes} dk",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (task.description.isNotBlank()) {
                Text(
                    task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CareerRealityCard(
    career: CareerRecommendation,
    targetCareer: String,
    isPlanLoading: Boolean,
    errorMessage: String?,
    onCreatePlan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(13.dp)) {
                    Icon(
                        Icons.Rounded.WorkOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(10.dp).size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        targetCareer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        career.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CareerMarketMetric(
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    label = "İşe alım görünümü",
                    value = career.hiringOutlook.ifBlank { "Analiz ediliyor" },
                    modifier = Modifier.weight(1f)
                )
                CareerMarketMetric(
                    icon = Icons.Rounded.AutoGraph,
                    label = "Maaş referansı",
                    value = compactHomeSalary(career.salaryRange),
                    modifier = Modifier.weight(1f)
                )
            }
            if (career.roles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(13.dp))
                Text("Yakın roller", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(7.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    career.roles.take(3).forEach { role ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.74f),
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            Text(
                                role,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onCreatePlan,
                enabled = !isPlanLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isPlanLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    if (isPlanLoading) "Plan hazırlanıyor…" else "AI ile 30 günlük plan oluştur",
                    fontWeight = FontWeight.Bold
                )
            }
            if (errorMessage != null) {
                Text(
                    errorMessage,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                "Maaş rakamı yalnızca doğrulanabilen dış kaynak verisi varsa gösterilir; kesin teklif veya iş garantisi değildir.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CareerMarketMetric(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AiStrategyCard(
    session: CareerSession,
    onOpenCoach: () -> Unit,
    onAskCoach: (String) -> Unit
) {
    val prompt = "${session.profile.classLevel} seviyesinde ${session.targetCareer} hedefim için " +
        "bu ay hangi somut çıktıyı üretmeliyim?"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
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
                .padding(17.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Migo AI stratejistin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Bölümünü ve sınıfını bilen koç",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onOpenCoach) { Text("Sohbet") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                    "“$prompt”",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(11.dp))
            Button(
                onClick = { onAskCoach(prompt) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Bu soruyu Migo'ya sor", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(7.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun QuickCareerActions(onOpenPlan: () -> Unit, onOpenTools: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickCareerAction(
            icon = Icons.Rounded.Map,
            title = "Yol haritam",
            subtitle = "Tüm adımlar",
            onClick = onOpenPlan,
            modifier = Modifier.weight(1f)
        )
        QuickCareerAction(
            icon = Icons.Rounded.PictureAsPdf,
            title = "CV laboratuvarı",
            subtitle = "AI ile güçlendir",
            onClick = onOpenTools,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickCareerAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(11.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp).size(19.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun CareerSession.activeCareerRecommendation(): CareerRecommendation? {
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

@Composable
private fun RoadmapScreen(
    session: CareerSession,
    isPlanLoading: Boolean,
    errorMessage: String?,
    onGeneratePlan: () -> Unit,
    onToggleTask: (String) -> Unit
) {
    val tasks = session.effectiveTasks()
    val completedTaskIds = session.effectiveCompletedTaskIds()
    val planActions = session.actionPlan?.weeks?.flatMap { it.actions }.orEmpty()
    val progressIds = if (planActions.isNotEmpty()) planActions.map { it.id } else tasks.map { it.id }
    val completedCount = progressIds.count { it in completedTaskIds }
    val total = progressIds.size.coerceAtLeast(1)
    val progress = completedCount.toFloat() / total
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
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
                color = Color.Transparent,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "HEDEF ROTA",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.68f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            session.targetCareer,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "${(progress * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "$completedCount tamamlandı • ${(progressIds.size - completedCount).coerceAtLeast(0)} adım kaldı",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        if (session.actionPlan != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("30 günlük AI rotan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        session.actionPlan.objective,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        "4 HAFTA",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            session.actionPlan.weeks.forEach { week ->
                ActionPlanWeekCard(
                    week = week,
                    completedTaskIds = completedTaskIds,
                    onToggleAction = onToggleTask
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            ActionPlanEmptyState(
                isLoading = isPlanLoading,
                errorMessage = errorMessage,
                onGeneratePlan = onGeneratePlan
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        Text(
            if (session.actionPlan == null) "Başlangıç adımları" else "Destek görevleri",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        val supportingTasks = if (session.actionPlan == null) tasks else tasks.take(4)
        supportingTasks.forEachIndexed { index, task ->
            val completed = task.id in completedTaskIds
            RoadmapTaskCard(
                step = index + 1,
                task = task,
                completed = completed,
                onClick = { onToggleTask(task.id) }
            )
            if (index != supportingTasks.lastIndex) Spacer(modifier = Modifier.height(10.dp))
        }
        if (progress == 1f) {
            Spacer(modifier = Modifier.height(12.dp))
            CompletedPlanCard()
        }
        Spacer(modifier = Modifier.height(22.dp))
    }
}

@Composable
private fun ActionPlanEmptyState(
    isLoading: Boolean,
    errorMessage: String?,
    onGeneratePlan: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        Icons.Rounded.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(13.dp))
            Text(
                if (isLoading) "Rotan hazırlanıyor" else "30 gününü görünür bir rotaya çevir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Migo AI bölümünü, sınıfını ve hedef branşını dört haftalık ölçülebilir çıktılara dönüştürür.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onGeneratePlan,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(if (isLoading) "AI planlıyor…" else "30 günlük planı oluştur", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActionPlanWeekCard(
    week: CareerPlanWeek,
    completedTaskIds: Set<String>,
    onToggleAction: (String) -> Unit
) {
    val completed = week.actions.count { it.id in completedTaskIds }
    val progress = if (week.actions.isEmpty()) 0f else completed.toFloat() / week.actions.size
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HAFTA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 8.sp)
                        Text(week.number.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(week.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(week.focus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Text(
                    "$completed/${week.actions.size}",
                    color = accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = accent,
                trackColor = Color.Transparent
            )
            Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 7.dp)) {
                week.actions.forEachIndexed { index, action ->
                    PlanActionRow(
                        action = action,
                        number = index + 1,
                        completed = action.id in completedTaskIds,
                        accent = accent,
                        onToggle = { onToggleAction(action.id) }
                    )
                    if (index != week.actions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 42.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Rounded.Flag, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("HAFTA ÇIKTISI", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                        Text(week.outcome, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanActionRow(
    action: CareerPlanAction,
    number: Int,
    completed: Boolean,
    accent: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 11.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (completed) accent else accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(Icons.Rounded.Check, contentDescription = "Tamamlandı", tint = Color.White, modifier = Modifier.size(17.dp))
            } else {
                Text(number.toString(), color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    action.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${action.durationMinutes} dk",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (action.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(action.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    "Çıktı: ${action.deliverable}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RoadmapTaskCard(
    step: Int,
    task: CareerTask,
    completed: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (completed) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            1.dp,
            if (completed) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (completed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (completed) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Tamamlandı",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(19.dp)
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
                        color = MaterialTheme.colorScheme.primary
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    task.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
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
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Migo AI Koç",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
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
            color = Color.Transparent,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 4.dp,
                bottomEnd = 0.dp
            ),
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
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Hepsini birden değil, bugün yalnızca ilk adımı tamamla.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    val accent = MaterialTheme.colorScheme.primary

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
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(70.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Spacer(modifier = Modifier.width(11.dp))
        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 4.dp else 12.dp),
            color = Color.Transparent,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
        CoachPrompt(Icons.Rounded.Map, "Yol haritam", "Nereden başlayacağını netleştir", "Bana adım adım kariyer yol haritası çıkar."),
        CoachPrompt(Icons.Rounded.Bolt, "Bugünün görevi", "Hemen yapabileceğin tek adım", "Bugün hedefim için ne yapmalıyım?"),
        CoachPrompt(Icons.Rounded.Description, "CV güçlendir", "Profilini daha etkili yap", "CV'mi güçlendirmek için öneriler ver."),
        CoachPrompt(Icons.Rounded.RecordVoiceOver, "Mülakat provası", "Sorulara birlikte hazırlan", "Hedef rolüm için mülakat provası yapalım.")
    )

    Column {
        Text(
            "Ne üzerinde çalışalım?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
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
    Surface(
        modifier = modifier
            .heightIn(min = 126.dp)
            .clickable(enabled = enabled, onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                prompt.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
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
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
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
                shape = RoundedCornerShape(4.dp),
                label = { Text("7 günlük plan") }
            )
            AssistChip(
                onClick = { onQuestion("İlk adımı daha detaylı anlat.") },
                shape = RoundedCornerShape(4.dp),
                label = { Text("İlk adımı aç") }
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
                shape = RoundedCornerShape(8.dp),
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
    val icon: ImageVector,
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
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
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
                color = Color.Transparent,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI profil", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        session.profile.fullName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.profile.fullName,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        session.profile.department,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        session.profile.university,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.62f),
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
        Text("Bölümündeki branşlar", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            "Program haritasında inceleyebileceğin kariyer yönleri",
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
        Text("Bu aşamanın gelişim alanları", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            "Sınıf seviyene ve seçtiğin branşa göre önerilen yetkinlikler",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            session.skills.forEach { SkillProfileRow(it) }
        }

        Spacer(modifier = Modifier.height(22.dp))
        Text("Gizlilik ve verilerin", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            "Migo'da paylaştığın bilgiler senin kontrolünde.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                PrivacySummaryRow(
                    icon = Icons.Rounded.CloudDone,
                    text = "Profilin, ilerlemen ve son 50 AI koç mesajın anonim bulut kasasında eşitlenir."
                )
                PrivacySummaryRow(
                    icon = Icons.Rounded.PictureAsPdf,
                    text = "CV dosyan saklanmaz; cihazda çıkarılan metin yalnızca analiz için gönderilir."
                )
                PrivacySummaryRow(
                    icon = Icons.Rounded.PrivacyTip,
                    text = "Etkin olmayan bulut kasaları 180 gün sonra otomatik silinir."
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                )
                PrivacyLinkRow(
                    title = "Gizlilik Bildirimi",
                    subtitle = "Hangi verinin neden işlendiğini gör",
                    onClick = { uriHandler.openUri(MigoLegalLinks.privacy) }
                )
                PrivacyLinkRow(
                    title = "Kullanım Koşulları",
                    subtitle = "Migo'nun kullanım kapsamını incele",
                    onClick = { uriHandler.openUri(MigoLegalLinks.terms) }
                )
                PrivacyLinkRow(
                    title = "Destek",
                    subtitle = "Soru, hata ve veri talebi gönder",
                    onClick = { uriHandler.openUri(MigoLegalLinks.support) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showResetDialog = true },
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Tüm verilerimi sil",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Cihazdaki ve buluttaki Migo verilerini kaldır",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Tüm verilerini sil?") },
            text = {
                Text(
                    "Profilin, hedefin, ilerlemen ve koç konuşmaların bu cihazdan hemen silinecek. " +
                        "Bulut kasası için de silme isteği gönderilecek; çevrimdışıysan sonraki açılışta yeniden denenecek."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onReset()
                }) { Text("Verilerimi sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun PrivacySummaryRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(11.dp))
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PrivacyLinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape
        ) {
            Icon(
                Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp).size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = "Tarayıcıda aç",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun ProfileInfoCard(icon: ImageVector, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        color = Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SkillProfileRow(label: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "Harika! Mevcut yol haritandaki tüm adımları tamamladın.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun CareerSession.effectiveTasks(): List<CareerTask> {
    val planned = CareerTaskPlanner.build(
        targetCareer = targetCareer,
        nextSteps = analysis.nextSteps,
        recommendation = activeCareerRecommendation(),
        classLevel = profile.classLevel
    )
    return (planned + tasks.map(CareerTaskPlanner::normalize))
        .distinctBy { it.title.lowercase(Locale.forLanguageTag("tr-TR")) }
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
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                val tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .heightIn(min = 44.dp)
                        .widthIn(min = 44.dp)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        tint     = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    )
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
                profile = UserProfile("Deniz Yılmaz", "İstanbul Üniversitesi", "Bilgisayar Mühendisliği", "3. sınıf"),
                interests = listOf("Mobil geliştirme", "Veri ve yapay zekâ", "Siber güvenlik"),
                skills = listOf("Programlama temelleri", "Proje üretme", "Teknik İngilizce"),
                analysis = CareerAnalysis(
                    "Bilgisayar Mühendisliği program haritan",
                    "Bu harita bölümün ve sınıf seviyen temel alınarak hazırlandı.",
                    listOf("Temel algoritmalar", "İlk portföy", "Topluluk deneyimi"),
                    listOf(CareerRecommendation("Mobil Geliştirme", "Mobil ürünlerin yazılım geliştirme sürecine odaklanır.")),
                    listOf("Temel bir kursu tamamla.", "Portföy projesi oluştur.", "Bir profesyonelle görüş.", "CV'ni güncelle.")
                ),
                targetCareer = "Ürün Yöneticisi"
            ),
            onSessionChange = { },
            onReset = { }
        )
    }
}
