package com.example.migo

import com.example.migo.data.model.CareerTaskPlanner
import org.junit.Assert.assertEquals
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
}
