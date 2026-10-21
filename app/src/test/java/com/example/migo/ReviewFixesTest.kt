package com.example.migo

import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CareerTask
import com.example.migo.data.model.CareerTaskPlanner
import com.example.migo.data.model.UserProfile
import com.example.migo.ui.main.currentStreak
import com.example.migo.ui.main.dateKey
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ReviewFixesTest {
    private fun session(streak: Int, lastCompletion: String) = CareerSession(
        profile = UserProfile("Test", "Üniversite", "Bölüm", "3. sınıf"),
        interests = emptyList(),
        skills = emptyList(),
        analysis = CareerAnalysis("", "", emptyList(), emptyList(), emptyList()),
        targetCareer = "Hedef",
        streakCount = streak,
        lastCompletionDate = lastCompletion
    )

    private fun daysAgo(days: Int) = dateKey(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) })

    @Test
    fun streakIsShownWhileLastCompletionIsTodayOrYesterday() {
        assertEquals(4, session(4, daysAgo(0)).currentStreak())
        assertEquals(4, session(4, daysAgo(1)).currentStreak())
    }

    @Test
    fun brokenStreakDisplaysAsZero() {
        assertEquals(0, session(4, daysAgo(2)).currentStreak())
        assertEquals(0, session(4, "").currentStreak())
    }

    @Test
    fun normalizeKeepsWholeTitleWhenDescriptionExists() {
        val task = CareerTask("t", "Lisans: Bilgisayar Mühendisliği diploması", "Yetkinlik", 30, "Açıklama")

        assertEquals("Lisans: Bilgisayar Mühendisliği diploması", CareerTaskPlanner.normalize(task).title)
    }

    @Test
    fun normalizeStillSplitsTitleWhenThereIsNoDescription() {
        val task = CareerTask("t", "Portfolyo: üç proje seç ve README yaz", "Yetkinlik", 30, "")

        val result = CareerTaskPlanner.normalize(task)

        assertEquals("Portfolyo", result.title)
        assertEquals("üç proje seç ve README yaz", result.description)
    }
}
