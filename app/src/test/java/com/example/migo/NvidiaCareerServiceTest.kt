package com.example.migo

import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.remote.NvidiaCareerService
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NvidiaCareerServiceTest {
    private val service = NvidiaCareerService

    // -- parseJsonObject --

    @Test
    fun parseJsonObject_extractsJsonFromMarkdownCodeFence() {
        val raw = "```json\n{\"headline\":\"Test\"}\n```"

        val result = service.parseJsonObject(raw)

        assertEquals("Test", result.getString("headline"))
    }

    @Test
    fun parseJsonObject_throwsWhenNoJsonObjectPresent() {
        assertThrows(IOException::class.java) {
            service.parseJsonObject("bu bir JSON değil")
        }
    }

    // -- toCareerAnalysis --

    private val validAnalysisJson = JSONObject(
        """
        {
          "headline": "Kariyer haritası",
          "summary": "Özet",
          "strengths": ["Güçlü yön"],
          "careers": [
            {"title": "Mobil Geliştirici", "reason": "İlgi alanına uygun", "roles": ["Android Developer"]}
          ],
          "next_steps": ["İlk adım"],
          "level_focus": {"title": "Odak", "description": "Açıklama"},
          "roadmap": [
            {"period": "Bu hafta", "title": "Başla", "description": "Açıklama", "actions": ["Adım"]}
          ]
        }
        """.trimIndent()
    )

    @Test
    fun toCareerAnalysis_mapsAllFieldsFromValidResponse() {
        with(service) {
            val analysis = validAnalysisJson.toCareerAnalysis()

            assertEquals("Kariyer haritası", analysis.headline)
            assertEquals(1, analysis.careers.size)
            assertEquals("Mobil Geliştirici", analysis.careers.first().title)
            assertEquals("Odak", analysis.levelFocusTitle)
            assertEquals(1, analysis.roadmap.size)
        }
    }

    @Test
    fun toCareerAnalysis_throwsWhenCareersListIsEmpty() {
        val json = JSONObject(
            """{"headline":"H","summary":"S","strengths":[],"careers":[],"next_steps":["x"]}"""
        )

        assertThrows(IOException::class.java) { with(service) { json.toCareerAnalysis() } }
    }

    // -- toCareerActionPlan --

    private fun actionPlanJsonWithWeeks(weekCount: Int, actionsPerWeek: Int): JSONObject {
        val weeks = (1..weekCount).joinToString(",") { weekNumber ->
            val actions = (1..actionsPerWeek).joinToString(",") { actionIndex ->
                """{"id":"w${weekNumber}_a$actionIndex","title":"Görev","description":"Açıklama","duration_minutes":30,"deliverable":"Kanıt"}"""
            }
            """{"number":$weekNumber,"title":"Hafta $weekNumber","focus":"Odak","outcome":"Sonuç","actions":[$actions]}"""
        }
        return JSONObject(
            """{"title":"Plan","objective":"Hedef","duration_days":30,"weeks":[$weeks]}"""
        )
    }

    @Test
    fun toCareerActionPlan_acceptsExactlyFourWeeksOfFourActions() {
        with(service) {
            val plan = actionPlanJsonWithWeeks(4, 4).toCareerActionPlan()

            assertEquals(4, plan.weeks.size)
            assertTrue(plan.weeks.all { it.actions.size == 4 })
        }
    }

    @Test
    fun toCareerActionPlan_throwsWhenFewerThanFourWeeks() {
        assertThrows(IOException::class.java) {
            with(service) { actionPlanJsonWithWeeks(3, 4).toCareerActionPlan() }
        }
    }

    @Test
    fun toCareerActionPlan_throwsWhenAWeekHasFewerThanFourActions() {
        assertThrows(IOException::class.java) {
            with(service) { actionPlanJsonWithWeeks(4, 2).toCareerActionPlan() }
        }
    }

    // -- toDailyCareerInsight --

    @Test
    fun toDailyCareerInsight_mapsFieldsAndFallsBackToRequestedDateKey() {
        with(service) {
            val json = JSONObject(
                """{"title":"Başlık","message":"Mesaj","action_label":"Eylem","coach_prompt":"Prompt"}"""
            )

            val insight = json.toDailyCareerInsight("2026-08-21")

            assertEquals("2026-08-21", insight.dateKey)
            assertEquals("Başlık", insight.title)
        }
    }

    @Test
    fun toDailyCareerInsight_throwsWhenMessageIsBlank() {
        val json = JSONObject("""{"title":"Başlık","message":"","action_label":"Eylem"}""")

        assertThrows(IOException::class.java) {
            with(service) { json.toDailyCareerInsight("2026-08-21") }
        }
    }

    // -- toCvAnalysis --

    @Test
    fun toCvAnalysis_mapsAndClampsScore() {
        with(service) {
            val json = JSONObject(
                """{"score":150,"summary":"Özet","strengths":["Güçlü yön"],"improvements":["Geliştir"]}"""
            )

            val analysis = json.toCvAnalysis()

            assertEquals(100, analysis.score)
            assertEquals("Özet", analysis.summary)
        }
    }

    @Test
    fun toCvAnalysis_throwsWhenStrengthsAreMissing() {
        val json = JSONObject("""{"score":80,"summary":"Özet","strengths":[]}""")

        assertThrows(IOException::class.java) { with(service) { json.toCvAnalysis() } }
    }

    // -- parseCareerDetail --

    private val fallbackBranch = CareerRecommendation(
        title = "Mobil Uygulama Geliştirme",
        reason = "Varsayılan sebep"
    )

    @Test
    fun parseCareerDetail_mapsRichResponseAndFallsBackForMissingOptionalFields() {
        val raw = """
            {"title":"Mobil Uygulama Geliştirme","overview":"Detaylı açıklama",
             "roles":["Android Developer","iOS Developer"],
             "requirements":["Kotlin","Git"]}
        """.trimIndent()

        val detail = service.parseCareerDetail(raw, fallbackBranch)

        assertEquals("Detaylı açıklama", detail.reason)
        assertEquals(2, detail.roles.size)
        assertEquals(fallbackBranch.workAreas, detail.workAreas)
    }

    @Test
    fun parseCareerDetail_throwsWhenRolesAreTooFewToBeUseful() {
        val raw = """{"title":"X","overview":"Kısa","roles":["Tek rol"],"requirements":["Tek gereksinim"]}"""

        assertThrows(IOException::class.java) {
            service.parseCareerDetail(raw, fallbackBranch)
        }
    }
}
