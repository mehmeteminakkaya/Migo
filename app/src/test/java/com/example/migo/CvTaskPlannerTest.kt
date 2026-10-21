package com.example.migo

import com.example.migo.data.model.CareerTaskPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CvTaskPlannerTest {
    private val improvements = listOf(
        "Projelerine ölçülebilir sonuç ekle",
        "Teknik becerileri tek bölümde topla",
        "Özet paragrafını hedef role göre yaz",
        "Fazladan dördüncü öneri"
    )

    @Test
    fun takesOnlyTheTopThreeImprovements() {
        assertEquals(3, CareerTaskPlanner.fromCvImprovements(improvements).size)
    }

    @Test
    fun idsAreStableSoReAddingIsIdempotent() {
        val first = CareerTaskPlanner.fromCvImprovements(improvements).map { it.id }
        val second = CareerTaskPlanner.fromCvImprovements(improvements).map { it.id }

        assertEquals(first, second)
        assertEquals(first.size, first.toSet().size)
    }

    @Test
    fun tasksAreTaggedAsCvWork() {
        assertTrue(CareerTaskPlanner.fromCvImprovements(improvements).all { it.category == "CV" && it.id.startsWith("cv_") })
    }

    @Test
    fun blankImprovementsAreDropped() {
        assertTrue(CareerTaskPlanner.fromCvImprovements(listOf("", "   ")).isEmpty())
    }
}
