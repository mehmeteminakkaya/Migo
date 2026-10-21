package com.example.migo

import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.UserProfile
import com.example.migo.notifications.DailyReminder
import com.example.migo.ui.main.dateKey
import com.example.migo.ui.main.effectiveTasks
import com.example.migo.ui.main.toggleTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyReminderTest {
    private fun at(hour: Int, minute: Int = 0) = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 21, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun session() = CareerSession(
        profile = UserProfile("Test", "Üniversite", "Bilgisayar Mühendisliği", "3. sınıf"),
        interests = emptyList(),
        skills = emptyList(),
        analysis = CareerAnalysis("", "", emptyList(), emptyList(), listOf("Portfolyo hazırla")),
        targetCareer = "Mobil Geliştirici"
    )

    @Test
    fun beforeReminderHour_firesLaterToday() {
        assertEquals(TimeUnit.HOURS.toMillis(4), DailyReminder.millisUntilNext(at(15)))
    }

    @Test
    fun afterReminderHour_firesTomorrow() {
        assertEquals(TimeUnit.HOURS.toMillis(23), DailyReminder.millisUntilNext(at(20)))
    }

    @Test
    fun exactlyAtReminderHour_firesTomorrowNotImmediately() {
        assertEquals(TimeUnit.HOURS.toMillis(24), DailyReminder.millisUntilNext(at(DailyReminder.REMINDER_HOUR)))
    }

    @Test
    fun noReminder_whenSomethingWasAlreadyCompletedToday() {
        val base = session()
        val done = base.toggleTask(base.effectiveTasks().first().id)

        assertNull(DailyReminder.reminderTaskTitle(done, dateKey(Calendar.getInstance())))
    }

    @Test
    fun reminderNamesFirstUnfinishedTask_onADayWithNoProgress() {
        val base = session()

        val title = DailyReminder.reminderTaskTitle(base, "2099-01-01")

        assertNotNull(title)
        assertEquals(base.effectiveTasks().first().title, title)
    }
}
