package com.example.migo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.migo.data.local.MigoPreferences
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerActionPlan
import com.example.migo.data.model.CareerPlanAction
import com.example.migo.data.model.CareerPlanWeek
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerRoadmapStep
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CareerTask
import com.example.migo.data.model.CoachMessage
import com.example.migo.data.model.DailyCareerInsight
import com.example.migo.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigoPreferencesInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = MigoPreferences(context, "migo_test_preferences")

    @Test
    fun applicationId_isProductionId() {
        assertEquals("com.migo.kariyer", context.packageName)
    }

    @Test
    fun session_roundTripsWithoutLosingCoachOrProgress() {
        preferences.clear()
        val session = CareerSession(
            profile = UserProfile("Test Kullanıcı", "Test Üniversitesi", "Bilgisayar Mühendisliği", "3. sınıf"),
            interests = listOf("Mobil"),
            skills = listOf("Kotlin"),
            analysis = CareerAnalysis(
                headline = "Test analiz",
                summary = "Özet",
                strengths = listOf("Mobil temeller"),
                careers = listOf(
                    CareerRecommendation(
                        title = "Android Geliştirici",
                        reason = "Mobil ürün geliştirme branşı",
                        roles = listOf("Android Developer"),
                        workAreas = listOf("Mobil ürün ekipleri"),
                        exampleEmployers = listOf("Teknoloji şirketleri"),
                        salaryRange = "Aylık brüt yaklaşık aralık",
                        salaryNote = "AI tahmini",
                        hiringOutlook = "Orta",
                        demandNote = "Deneyime göre değişir",
                        requirements = listOf("Kotlin", "Compose"),
                        dailyWork = listOf("Kod geliştir", "Test yaz"),
                        first90Days = listOf("Mini proje", "Portföye ekle")
                    )
                ),
                nextSteps = listOf("Compose öğren"),
                levelFocusTitle = "Uzmanlaşma ve staj",
                levelFocusDescription = "Üçüncü sınıfta kanıtlanabilir proje üret.",
                roadmap = listOf(
                    CareerRoadmapStep(
                        period = "Bu dönem",
                        title = "Portföy oluştur",
                        description = "Bir mobil ürün geliştir.",
                        actions = listOf("İlk ekranı tamamla")
                    )
                )
            ),
            targetCareer = "Android Geliştirici",
            tasks = listOf(
                CareerTask(
                    "task-1",
                    "Compose öğren",
                    "Öğrenme",
                    30,
                    "Çalışan bir ekran üret."
                )
            ),
            completedTaskIds = setOf("task-1"),
            streakCount = 2,
            actionPlan = CareerActionPlan(
                title = "30 günlük rota",
                objective = "Bir mobil ürün kanıtı üret.",
                weeks = listOf(
                    CareerPlanWeek(
                        number = 1,
                        title = "Temel",
                        focus = "Compose",
                        outcome = "Çalışan ekran",
                        actions = listOf(
                            CareerPlanAction("plan-1", "Ekranı kur", "Ana akışı geliştir", 45, "Ekran görüntüsü")
                        )
                    )
                )
            ),
            dailyInsight = DailyCareerInsight(
                dateKey = "2026-07-20",
                title = "Bugünün odağı",
                message = "Tek bir görünür çıktı üret.",
                actionLabel = "Ana ekranı bitir",
                coachPrompt = "Ana ekranı üç adıma böl."
            ),
            coachMessages = listOf(CoachMessage("Merhaba", false), CoachMessage("Başlayalım", true))
        )

        preferences.saveSession(session)
        val storedSession = context
            .getSharedPreferences("migo_test_preferences", 0)
            .getString("career_session", null)
        assertTrue(storedSession?.startsWith("migo:v1:") == true)
        assertFalse(storedSession.orEmpty().contains("Test Kullanıcı"))
        val restored = preferences.loadSession()

        assertEquals(session, restored)
        preferences.clear()
        assertNull(preferences.loadSession())
    }

    @Test
    fun legacyPlainTextSession_isMigratedToEncryptedStorage() {
        preferences.clear()
        val session = CareerSession(
            profile = UserProfile("Deniz Yılmaz", "Örnek Üniversitesi", "Yazılım Mühendisliği", "2. sınıf"),
            interests = listOf("Mobil"),
            skills = listOf("Kotlin"),
            analysis = CareerAnalysis("Başlık", "Özet", emptyList(), emptyList(), emptyList()),
            targetCareer = "Android Geliştirici"
        )
        val rawSession = preferences.encodeSession(session)
        context.getSharedPreferences("migo_test_preferences", 0)
            .edit()
            .putString("career_session", rawSession)
            .commit()

        assertEquals(session, preferences.loadSession())
        val migrated = context.getSharedPreferences("migo_test_preferences", 0)
            .getString("career_session", null)
        assertTrue(migrated?.startsWith("migo:v1:") == true)
        assertFalse(migrated.orEmpty().contains("Deniz Yılmaz"))
    }

    @Test
    fun installationId_isStableAndSurvivesSessionClear() {
        val first = preferences.installationId()
        val firstSecret = preferences.syncSecret()
        preferences.clear()
        val second = preferences.installationId()
        val secondSecret = preferences.syncSecret()

        assertEquals(first, second)
        assertEquals(firstSecret, secondSecret)
        assertNotEquals("", first)
        assertTrue(firstSecret.length >= 40)
    }

    @Test
    fun cloudDeleteFlag_isClearedAfterRemoteDelete() {
        preferences.clear()
        assertTrue(preferences.isCloudDeletePending())

        preferences.markCloudDeleteComplete()

        assertFalse(preferences.isCloudDeletePending())
    }

    @Test
    fun privacyConsent_isVersionedAndRemovedWithUserData() {
        preferences.clear()
        assertFalse(preferences.hasAcceptedPrivacy())

        preferences.acceptPrivacy()

        assertTrue(preferences.hasAcceptedPrivacy())
        assertTrue(preferences.privacyConsentAcceptedAt() > 0L)

        preferences.clear()
        assertFalse(preferences.hasAcceptedPrivacy())
    }
}
