package com.example.migo

import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.UserProfile
import com.example.migo.ui.main.dateKey
import com.example.migo.ui.main.effectiveTasks
import com.example.migo.ui.main.toggleTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TaskStreakTest {
    private fun session(streak: Int = 0, lastCompletion: String = "") = CareerSession(
        profile = UserProfile("Test", "Üniversite", "Bilgisayar Mühendisliği", "3. sınıf"),
        interests = emptyList(),
        skills = emptyList(),
        analysis = CareerAnalysis(
            headline = "",
            summary = "",
            strengths = emptyList(),
            careers = emptyList(),
            nextSteps = listOf("Portfolyo hazırla", "Mülakat sorularını çalış")
        ),
        targetCareer = "Mobil Geliştirici",
        streakCount = streak,
        lastCompletionDate = lastCompletion
    )

    private fun daysAgo(days: Int): String =
        dateKey(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) })

    @Test
    fun firstCompletionStartsStreakAtOne() {
        val base = session()
        val taskId = base.effectiveTasks().first().id

        val result = base.toggleTask(taskId)

        assertTrue(taskId in result.completedTaskIds)
        assertEquals(1, result.streakCount)
        assertEquals(daysAgo(0), result.lastCompletionDate)
    }

    @Test
    fun completingOnConsecutiveDayExtendsStreak() {
        val base = session(streak = 4, lastCompletion = daysAgo(1))

        val result = base.toggleTask(base.effectiveTasks().first().id)

        assertEquals(5, result.streakCount)
    }

    @Test
    fun completingAfterGapResetsStreakToOne() {
        val base = session(streak = 9, lastCompletion = daysAgo(3))

        val result = base.toggleTask(base.effectiveTasks().first().id)

        assertEquals(1, result.streakCount)
    }

    @Test
    fun secondCompletionSameDayDoesNotInflateStreak() {
        val base = session(streak = 2, lastCompletion = daysAgo(0))
        val tasks = base.effectiveTasks()

        val result = base.toggleTask(tasks[0].id).toggleTask(tasks[1].id)

        assertEquals(2, result.streakCount)
    }

    @Test
    fun uncheckingTaskKeepsStreakAndRemovesCompletion() {
        val base = session()
        val taskId = base.effectiveTasks().first().id
        val completed = base.toggleTask(taskId)

        val result = completed.toggleTask(taskId)

        assertFalse(taskId in result.completedTaskIds)
        assertEquals(completed.streakCount, result.streakCount)
    }
}
