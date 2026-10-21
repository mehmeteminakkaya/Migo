package com.example.migo.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
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
    var coachFailure by remember { mutableStateOf<CoachFailure?>(null) }
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
        coachFailure = null
        coachMessages.add(CoachMessage(question, isFromUser = true))
        persistCoachMessages()
        coachIsLoading = true
        shellScope.launch {
            NvidiaCareerService.askCoach(latestSession, previousHistory, question)
                .onSuccess { answer ->
                    coachMessages.add(CoachMessage(answer, isFromUser = false))
                }
                .onFailure { error ->
                    // Keep the thread clean: a failed turn is not history, so the question is pulled back
                    // out (never persisted or synced) and offered for retry instead.
                    coachMessages.removeLastOrNull()
                    coachFailure = CoachFailure(
                        question = question,
                        message = error.message ?: "Şu an yanıt veremiyorum."
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
                    failure = coachFailure,
                    onDismissFailure = { coachFailure = null },
                    onQuestion = askCoach,
                    onNewConversation = {
                        if (!coachIsLoading) {
                            coachFailure = null
                            coachMessages.clear()
                            coachMessages.add(CoachMessage(coachGreeting, isFromUser = false))
                            persistCoachMessages()
                        }
                    }
                )
                MainTab.PROFILE -> if (settingsOpen) {
                    SettingsScreen(onBack = { settingsOpen = false }, onReset = onReset)
                } else {
                    ProfileOverviewScreen(session = session, onOpenSettings = { settingsOpen = true })
                }
            }
        }
    }
}

internal fun CareerSession.effectiveTasks(): List<CareerTask> {
    val planned = CareerTaskPlanner.build(
        targetCareer = targetCareer,
        nextSteps = analysis.nextSteps,
        recommendation = activeCareerRecommendation(),
        classLevel = profile.classLevel
    )
    return (planned + tasks.map(CareerTaskPlanner::normalize))
        .distinctBy { it.title.lowercase(Locale.forLanguageTag("tr-TR")) }
}

internal fun CareerSession.effectiveCompletedTaskIds(): Set<String> {
    if (completedTaskIds.isNotEmpty() || completedSteps.isEmpty()) return completedTaskIds
    val taskList = effectiveTasks()
    return completedSteps.mapNotNull { taskList.getOrNull(it)?.id }.toSet()
}

internal fun CareerSession.toggleTask(taskId: String): CareerSession {
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

internal fun dateKey(calendar: Calendar): String =
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
