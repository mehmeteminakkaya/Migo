package com.example.migo.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.example.migo.MainActivity
import com.example.migo.R
import com.example.migo.data.local.MigoPreferences
import com.example.migo.data.model.CareerSession
import com.example.migo.ui.main.dateKey
import com.example.migo.ui.main.effectiveCompletedTaskIds
import com.example.migo.ui.main.effectiveTasks
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Opt-in evening nudge ("today's step"). Everything happens on-device: the worker reads the locally
 * stored session and posts a local notification, so enabling it sends no data anywhere.
 */
object DailyReminder {
    private const val CHANNEL_ID = "daily_reminder"
    private const val WORK_NAME = "daily_reminder"
    private const val NOTIFICATION_ID = 1001
    const val REMINDER_HOUR = 19

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(millisUntilNext(Calendar.getInstance()), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    internal fun millisUntilNext(now: Calendar, hour: Int = REMINDER_HOUR): Long {
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!next.after(now)) next.add(Calendar.DAY_OF_YEAR, 1)
        return next.timeInMillis - now.timeInMillis
    }

    /** The next unfinished task's title, or null when there is nothing to nudge about today. */
    internal fun reminderTaskTitle(session: CareerSession, today: String): String? {
        if (session.lastCompletionDate == today) return null
        val done = session.effectiveCompletedTaskIds()
        return session.effectiveTasks().firstOrNull { it.id !in done }?.title
    }

    fun canPostNotifications(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    internal fun post(context: Context, taskTitle: String) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_migo)
            .setContentTitle("Migo")
            .setContentText("Bugünün adımı seni bekliyor.")
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_migo)
            .setContentTitle("Bugünün adımı")
            .setContentText(taskTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(taskTitle))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check above and posting; nothing to nudge about.
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Günlük hatırlatma",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Her akşam bugünün kariyer adımını hatırlatır." }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

class DailyReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val preferences = MigoPreferences(applicationContext)
        if (!preferences.isReminderEnabled() || !preferences.hasAcceptedPrivacy()) return Result.success()
        val session = preferences.loadSession() ?: return Result.success()
        val title = DailyReminder.reminderTaskTitle(session, dateKey(Calendar.getInstance()))
            ?: return Result.success()
        DailyReminder.post(applicationContext, title)
        return Result.success()
    }
}
