package com.example.migo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.migo.data.local.MigoPreferences
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CareerTaskPlanner
import com.example.migo.data.model.UserProfile
import com.example.migo.data.remote.CloudSyncService
import com.example.migo.data.remote.MigoClientIdentity
import com.example.migo.ui.analysis.AiDiscoveryScreen
import com.example.migo.ui.analysis.CareerAnalysisScreen
import com.example.migo.ui.legal.PrivacyConsentScreen
import com.example.migo.ui.main.MainShell
import com.example.migo.ui.profile.ProfileSetupScreen
import com.example.migo.ui.theme.MigoTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MigoTheme {
                MigoApp()
            }
        }
    }
}

@Composable
private fun MigoApp() {
    val context = LocalContext.current.applicationContext
    val preferences = remember { MigoPreferences(context) }
    remember(preferences) {
        MigoClientIdentity.configure(
            installationValue = preferences.installationId(),
            syncSecretValue = preferences.syncSecret(),
            tokenValue = preferences.installationToken(),
            onTokenUpdated = preferences::saveInstallationToken
        )
        true
    }
    val initialSession = remember { preferences.loadSession() }
    val initialPrivacyConsent = remember { preferences.hasAcceptedPrivacy() }
    var hasPrivacyConsent by remember { mutableStateOf(initialPrivacyConsent) }
    var currentScreen by rememberSaveable {
        mutableIntStateOf(
            when {
                initialSession != null && initialPrivacyConsent -> SCREEN_MAIN
                initialSession != null -> SCREEN_CONSENT
                else -> SCREEN_WELCOME
            }
        )
    }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var selectedInterests by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var careerAnalysis by remember { mutableStateOf<CareerAnalysis?>(null) }
    var careerSession by remember { mutableStateOf(initialSession) }
    var cloudSyncReady by remember { mutableStateOf(false) }

    LaunchedEffect(hasPrivacyConsent) {
        val deleteWasPending = preferences.isCloudDeletePending()
        if (deleteWasPending) {
            CloudSyncService.deleteSession().onSuccess {
                preferences.markCloudDeleteComplete()
            }
        }

        if (!hasPrivacyConsent) {
            cloudSyncReady = false
            return@LaunchedEffect
        }

        if (!deleteWasPending) {
            CloudSyncService.pullSession().onSuccess { rawSession ->
                rawSession
                    ?.let(preferences::decodeSession)
                    ?.let { restoredSession ->
                        preferences.saveSession(restoredSession)
                        careerSession = restoredSession
                        currentScreen = SCREEN_MAIN
                    }
            }
        }
        cloudSyncReady = true
    }

    LaunchedEffect(careerSession, cloudSyncReady, hasPrivacyConsent) {
        val session = careerSession ?: return@LaunchedEffect
        if (!cloudSyncReady || !hasPrivacyConsent) return@LaunchedEffect
        delay(900)
        CloudSyncService.pushSession(preferences.encodeSession(session))
    }

    BackHandler(enabled = currentScreen in SCREEN_PROFILE..SCREEN_ANALYSIS) {
        currentScreen = when (currentScreen) {
            SCREEN_ANALYSIS -> SCREEN_PROFILE
            SCREEN_AI_DISCOVERY -> SCREEN_PROFILE
            else -> SCREEN_WELCOME
        }
    }

    BackHandler(enabled = currentScreen == SCREEN_CONSENT) {
        currentScreen = SCREEN_WELCOME
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            val forward = targetState > initialState
            if (forward) {
                (slideInVertically { it / 4 } + fadeIn(tween(320))) togetherWith
                        (slideOutVertically { -it / 8 } + fadeOut(tween(200)))
            } else {
                (slideInVertically { -it / 8 } + fadeIn(tween(280))) togetherWith
                        (slideOutVertically { it / 4 } + fadeOut(tween(200)))
            }
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            SCREEN_CONSENT -> PrivacyConsentScreen(
                onBack = { currentScreen = SCREEN_WELCOME },
                onAccept = {
                    preferences.acceptPrivacy()
                    hasPrivacyConsent = true
                    currentScreen = if (careerSession == null) SCREEN_PROFILE else SCREEN_MAIN
                }
            )
            SCREEN_PROFILE -> ProfileSetupScreen(
                initialProfile = profile,
                onBackClick = { currentScreen = SCREEN_WELCOME },
                onContinueClick = { completedProfile ->
                    profile = completedProfile
                    currentScreen = SCREEN_AI_DISCOVERY
                }
            )
            SCREEN_AI_DISCOVERY -> {
                val completedProfile = profile
                if (completedProfile == null) {
                    currentScreen = SCREEN_PROFILE
                } else {
                    AiDiscoveryScreen(
                        profile = completedProfile,
                        onBackClick = { currentScreen = SCREEN_PROFILE },
                        onAnalysisReady = { result ->
                            selectedInterests = result.inferredInterests
                            selectedSkills = result.inferredSkills
                            careerAnalysis = result.analysis
                            currentScreen = SCREEN_ANALYSIS
                        }
                    )
                }
            }
            SCREEN_ANALYSIS -> {
                val analysis = careerAnalysis
                val completedProfile = profile
                if (analysis == null || completedProfile == null) {
                    currentScreen = SCREEN_PROFILE
                } else {
                    CareerAnalysisScreen(
                        profile = completedProfile,
                        analysis = analysis,
                        onBackClick = { currentScreen = SCREEN_PROFILE },
                        onTargetSelected = { selectedCareer ->
                            val updatedAnalysis = analysis.copy(
                                careers = analysis.careers.map { career ->
                                    if (career.title == selectedCareer.title) selectedCareer else career
                                }
                            )
                            val newSession = CareerSession(
                                profile = completedProfile,
                                interests = selectedInterests,
                                skills = selectedSkills,
                                analysis = updatedAnalysis,
                                targetCareer = selectedCareer.title,
                                tasks = CareerTaskPlanner.build(
                                    targetCareer = selectedCareer.title,
                                    nextSteps = updatedAnalysis.nextSteps,
                                    recommendation = selectedCareer,
                                    classLevel = completedProfile.classLevel
                                )
                            )
                            careerAnalysis = updatedAnalysis
                            preferences.saveSession(newSession)
                            careerSession = newSession
                            currentScreen = SCREEN_MAIN
                        }
                    )
                }
            }
            SCREEN_MAIN -> {
                val session = careerSession
                if (session == null) {
                    currentScreen = SCREEN_WELCOME
                } else {
                    MainShell(
                        session = session,
                        onSessionChange = { updatedSession ->
                            careerSession = updatedSession
                            preferences.saveSession(updatedSession)
                        },
                        onReset = {
                            preferences.clear()
                            hasPrivacyConsent = false
                            profile = null
                            selectedInterests = emptyList()
                            selectedSkills = emptyList()
                            careerAnalysis = null
                            careerSession = null
                            currentScreen = SCREEN_WELCOME
                        }
                    )
                }
            }
            else -> WelcomeScreen(
                onStartClick = {
                    currentScreen = if (hasPrivacyConsent) SCREEN_PROFILE else SCREEN_CONSENT
                }
            )
        }
    }
}

private const val SCREEN_WELCOME      = 0
private const val SCREEN_CONSENT      = 1
private const val SCREEN_PROFILE      = 2
private const val SCREEN_AI_DISCOVERY = 3
private const val SCREEN_ANALYSIS     = 4
private const val SCREEN_MAIN         = 5

// ─── Welcome Screen ────────────────────────────────────────────────────────────
@Composable
fun WelcomeScreen(onStartClick: () -> Unit, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); visible = true }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val landscapeLayout = maxWidth > maxHeight
            if (landscapeLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(0.9f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WelcomeBrand(visible)
                        Spacer(modifier = Modifier.height(22.dp))
                        WelcomePitch()
                    }
                    Column(
                        modifier = Modifier.weight(1.1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WelcomeFeatures()
                        Spacer(modifier = Modifier.height(22.dp))
                        WelcomeCta(onStartClick)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp))
                    WelcomeBrand(visible)
                    Spacer(modifier = Modifier.weight(1f))
                    WelcomePitch()
                    Spacer(modifier = Modifier.height(40.dp))
                    WelcomeFeatures()
                    Spacer(modifier = Modifier.weight(1f))
                    WelcomeCta(onStartClick)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun WelcomeBrand(visible: Boolean) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "logo_scale"
    )
    Surface(
        modifier = Modifier
            .scale(scaleAnim)
            .size(88.dp),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.migo_logo_foreground),
            contentDescription = "Migo",
            modifier = Modifier.padding(12.dp)
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(
            "AI destekli kariyer rehberi",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WelcomePitch() {
    Text(
        stringResource(R.string.welcome_title),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        lineHeight = 42.sp
    )
    Spacer(modifier = Modifier.height(18.dp))
    Text(
        stringResource(R.string.welcome_description),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 24.sp
    )
}

@Composable
private fun WelcomeFeatures() {
    WelcomeFeature(
        icon = Icons.Rounded.Explore,
        text = stringResource(R.string.feature_discover)
    )
    Spacer(modifier = Modifier.height(12.dp))
    WelcomeFeature(
        icon = Icons.Rounded.WorkOutline,
        text = stringResource(R.string.feature_target)
    )
    Spacer(modifier = Modifier.height(12.dp))
    WelcomeFeature(
        icon = Icons.Rounded.Route,
        text = stringResource(R.string.feature_progress)
    )
}

@Composable
private fun WelcomeCta(onStartClick: () -> Unit) {
    Button(
        onClick = onStartClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.start_button),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        stringResource(R.string.start_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun WelcomeFeature(icon: ImageVector, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
