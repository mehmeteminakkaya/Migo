package com.example.migo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.migo.data.local.MigoPreferences
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CareerTaskPlanner
import com.example.migo.data.model.SmartProfileResult
import com.example.migo.data.model.UserProfile
import com.example.migo.data.remote.CloudSyncService
import com.example.migo.data.remote.MigoClientIdentity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val SCREEN_WELCOME = 0
internal const val SCREEN_CONSENT = 1
internal const val SCREEN_PROFILE = 2
internal const val SCREEN_AI_DISCOVERY = 3
internal const val SCREEN_ANALYSIS = 4
internal const val SCREEN_MAIN = 5

private const val SYNC_DEBOUNCE_MS = 900L

/**
 * Owns the top-level app state that used to live in `MigoApp()`'s `remember` blocks, so the onboarding
 * answers survive a configuration change (rotation) and the cloud-sync side effects aren't tied to
 * composition.
 */
class MigoViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = MigoPreferences(application)

    var screen by mutableIntStateOf(SCREEN_WELCOME)
        private set
    var hasPrivacyConsent by mutableStateOf(false)
        private set
    var profile by mutableStateOf<UserProfile?>(null)
        private set
    var careerAnalysis by mutableStateOf<CareerAnalysis?>(null)
        private set
    var careerSession by mutableStateOf<CareerSession?>(null)
        private set

    private var selectedInterests: List<String> = emptyList()
    private var selectedSkills: List<String> = emptyList()
    private var cloudSyncReady = false
    private var consentSyncJob: Job? = null
    private var pushJob: Job? = null

    init {
        MigoClientIdentity.configure(
            installationValue = preferences.installationId(),
            syncSecretValue = preferences.syncSecret(),
            tokenValue = preferences.installationToken(),
            onTokenUpdated = preferences::saveInstallationToken
        )
        val initialSession = preferences.loadSession()
        val initialConsent = preferences.hasAcceptedPrivacy()
        careerSession = initialSession
        hasPrivacyConsent = initialConsent
        screen = when {
            initialSession != null && initialConsent -> SCREEN_MAIN
            initialSession != null -> SCREEN_CONSENT
            else -> SCREEN_WELCOME
        }
        runConsentSync()
    }

    fun navigate(target: Int) {
        screen = target
    }

    /** System back for the onboarding screens; the main shell and welcome screen handle back themselves. */
    fun onBack(): Boolean {
        val target = when (screen) {
            SCREEN_ANALYSIS, SCREEN_AI_DISCOVERY -> SCREEN_PROFILE
            SCREEN_PROFILE, SCREEN_CONSENT -> SCREEN_WELCOME
            else -> return false
        }
        screen = target
        return true
    }

    fun start() {
        screen = if (hasPrivacyConsent) SCREEN_PROFILE else SCREEN_CONSENT
    }

    fun acceptPrivacy() {
        preferences.acceptPrivacy()
        hasPrivacyConsent = true
        screen = if (careerSession == null) SCREEN_PROFILE else SCREEN_MAIN
        runConsentSync()
    }

    fun submitProfile(completed: UserProfile) {
        profile = completed
        screen = SCREEN_AI_DISCOVERY
    }

    fun onAnalysisReady(result: SmartProfileResult) {
        selectedInterests = result.inferredInterests
        selectedSkills = result.inferredSkills
        careerAnalysis = result.analysis
        screen = SCREEN_ANALYSIS
    }

    fun selectTarget(selectedCareer: CareerRecommendation) {
        val analysis = careerAnalysis ?: return
        val completedProfile = profile ?: return
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
        screen = SCREEN_MAIN
        schedulePush()
    }

    fun updateSession(updated: CareerSession) {
        careerSession = updated
        preferences.saveSession(updated)
        schedulePush()
    }

    fun reset() {
        preferences.clear()
        hasPrivacyConsent = false
        profile = null
        selectedInterests = emptyList()
        selectedSkills = emptyList()
        careerAnalysis = null
        careerSession = null
        screen = SCREEN_WELCOME
        runConsentSync()
    }

    /** Flushes a pending cloud delete, then (with consent) pulls the vault and enables pushes. */
    private fun runConsentSync() {
        consentSyncJob?.cancel()
        pushJob?.cancel()
        consentSyncJob = viewModelScope.launch {
            val deleteWasPending = preferences.isCloudDeletePending()
            if (deleteWasPending) {
                CloudSyncService.deleteSession().onSuccess { preferences.markCloudDeleteComplete() }
            }

            if (!hasPrivacyConsent) {
                cloudSyncReady = false
                return@launch
            }

            if (!deleteWasPending) {
                CloudSyncService.pullSession().onSuccess { rawSession ->
                    rawSession
                        ?.let(preferences::decodeSession)
                        ?.let { restored ->
                            preferences.saveSession(restored)
                            careerSession = restored
                            screen = SCREEN_MAIN
                        }
                }
            }
            cloudSyncReady = true
            schedulePush()
        }
    }

    private fun schedulePush() {
        val session = careerSession ?: return
        if (!cloudSyncReady || !hasPrivacyConsent) return
        pushJob?.cancel()
        pushJob = viewModelScope.launch {
            delay(SYNC_DEBOUNCE_MS)
            CloudSyncService.pushSession(preferences.encodeSession(session))
        }
    }
}
