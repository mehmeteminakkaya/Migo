package com.example.migo

import com.example.migo.data.model.CareerTaskPlanner
import com.example.migo.data.model.CareerRecommendation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CareerTaskPlannerTest {
    @Test
    fun build_usesAtMostFourAiStepsAndAddsCoreTasks() {
        val tasks = CareerTaskPlanner.build(
            targetCareer = "Android Geliştirici",
            nextSteps = listOf("Kotlin", "Compose", "Test", "Portföy", "Fazladan")
        )

        assertEquals(7, tasks.size)
        assertEquals(listOf("ai_0", "ai_1", "ai_2", "ai_3"), tasks.take(4).map { it.id })
        assertTrue(tasks.any { it.id == "market" })
        assertTrue(tasks.any { it.id == "profile" })
        assertTrue(tasks.any { it.id == "practice" })
    }

    @Test
    fun build_removesDuplicateTaskTitlesIgnoringCase() {
        val tasks = CareerTaskPlanner.build(
            targetCareer = "Veri Analisti",
            nextSteps = listOf("SQL öğren", "sql öğren", "Dashboard oluştur")
        )

        assertEquals(tasks.size, tasks.map { it.title.lowercase() }.distinct().size)
    }

    @Test
    fun build_prioritizesCareerRequirementsAndClassLevel() {
        val tasks = CareerTaskPlanner.build(
            targetCareer = "Android Geliştirici",
            nextSteps = listOf("Bir portföy projesi yayınla"),
            recommendation = CareerRecommendation(
                title = "Mobil Uygulama Geliştirme",
                reason = "Mobil ürün geliştirme",
                requirements = listOf("Kotlin", "Mobil mimari", "Git")
            ),
            classLevel = "3. sınıf"
        )

        assertEquals(listOf("career_requirement_0", "career_requirement_1"), tasks.take(2).map { it.id })
        assertTrue(tasks.any { it.id == "level_focus" && "başlangıç rolünü" in it.title.lowercase() })
        assertTrue(tasks.any { it.category == "Portföy" })
    }

    @Test
    fun build_turnsMarkdownParagraphIntoReadableTask() {
        val tasks = CareerTaskPlanner.build(
            targetCareer = "Yazılım Geliştirici",
            nextSteps = listOf(
                "1. **Staj ve proje deneyimi**: 3. sınıftan itibaren yazılım, veri bilimi veya siber güvenlik alanlarında staj yaparak pratik deneyim kazan."
            )
        )

        val task = tasks.first()
        assertEquals("Opsiyonel staj veya eşdeğer uygulama", task.title)
        assertEquals("Uygulama", task.category)
        assertFalse(task.title.contains("**"))
        assertFalse(task.title.startsWith("1."))
        assertTrue(task.description.contains("yoksa aynı kazanımı"))
    }
}
