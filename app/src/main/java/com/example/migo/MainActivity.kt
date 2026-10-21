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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
private fun MigoApp(viewModel: MigoViewModel = viewModel()) {
    val screen = viewModel.screen

    BackHandler(enabled = screen in SCREEN_CONSENT..SCREEN_ANALYSIS) {
        viewModel.onBack()
    }

    AnimatedContent(
        targetState = screen,
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
    ) { target ->
        when (target) {
            SCREEN_CONSENT -> PrivacyConsentScreen(
                onBack = { viewModel.navigate(SCREEN_WELCOME) },
                onAccept = viewModel::acceptPrivacy
            )
            SCREEN_PROFILE -> ProfileSetupScreen(
                initialProfile = viewModel.profile,
                onBackClick = { viewModel.navigate(SCREEN_WELCOME) },
                onContinueClick = viewModel::submitProfile
            )
            SCREEN_AI_DISCOVERY -> {
                val completedProfile = viewModel.profile
                if (completedProfile == null) {
                    viewModel.navigate(SCREEN_PROFILE)
                } else {
                    AiDiscoveryScreen(
                        profile = completedProfile,
                        onBackClick = { viewModel.navigate(SCREEN_PROFILE) },
                        onAnalysisReady = viewModel::onAnalysisReady
                    )
                }
            }
            SCREEN_ANALYSIS -> {
                val analysis = viewModel.careerAnalysis
                val completedProfile = viewModel.profile
                if (analysis == null || completedProfile == null) {
                    viewModel.navigate(SCREEN_PROFILE)
                } else {
                    CareerAnalysisScreen(
                        profile = completedProfile,
                        analysis = analysis,
                        onBackClick = { viewModel.navigate(SCREEN_PROFILE) },
                        onTargetSelected = viewModel::selectTarget
                    )
                }
            }
            SCREEN_MAIN -> {
                val session = viewModel.careerSession
                if (session == null) {
                    viewModel.navigate(SCREEN_WELCOME)
                } else {
                    MainShell(
                        session = session,
                        onSessionChange = viewModel::updateSession,
                        onReset = viewModel::reset
                    )
                }
            }
            else -> WelcomeScreen(onStartClick = viewModel::start)
        }
    }
}

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
