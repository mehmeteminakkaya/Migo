package com.example.migo.data.local

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CareerTask
import com.example.migo.data.model.CoachMessage
import com.example.migo.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.UUID

class MigoPreferences(
    context: Context,
    preferencesName: String = PREFERENCES_NAME
) {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun saveSession(session: CareerSession) {
        preferences.edit {
            putString(KEY_SESSION, encodeSession(session))
            putBoolean(KEY_PENDING_CLOUD_DELETE, false)
        }
    }

    fun loadSession(): CareerSession? {
        val raw = preferences.getString(KEY_SESSION, null) ?: return null
        return decodeSession(raw)
    }

    fun encodeSession(session: CareerSession): String = session.toJson().toString()

    fun decodeSession(raw: String): CareerSession? =
        runCatching { JSONObject(raw).toCareerSession() }.getOrNull()

    fun clear() {
        preferences.edit {
            remove(KEY_SESSION)
            putBoolean(KEY_PENDING_CLOUD_DELETE, true)
        }
    }

    fun installationId(): String {
        preferences.getString(KEY_INSTALLATION_ID, null)
            ?.takeIf { it.length in 16..128 }
            ?.let { return it }

        val generated = UUID.randomUUID().toString().replace("-", "")
        preferences.edit(commit = true) { putString(KEY_INSTALLATION_ID, generated) }
        return generated
    }

    fun syncSecret(): String {
        preferences.getString(KEY_SYNC_SECRET, null)
            ?.takeIf { it.length in 40..128 }
            ?.let { return it }

        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        val generated = Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        preferences.edit(commit = true) { putString(KEY_SYNC_SECRET, generated) }
        return generated
    }

    fun isCloudDeletePending(): Boolean =
        preferences.getBoolean(KEY_PENDING_CLOUD_DELETE, false)

    fun markCloudDeleteComplete() {
        preferences.edit { putBoolean(KEY_PENDING_CLOUD_DELETE, false) }
    }

    private fun CareerSession.toJson(): JSONObject = JSONObject()
        .put(
            "profile",
            JSONObject()
                .put("fullName", profile.fullName)
                .put("university", profile.university)
                .put("department", profile.department)
                .put("classLevel", profile.classLevel)
                .put("universityId", profile.universityId)
                .put("programId", profile.programId)
                .put("scoreType", profile.scoreType)
        )
        .put("interests", JSONArray(interests))
        .put("skills", JSONArray(skills))
        .put(
            "analysis",
            JSONObject()
                .put("headline", analysis.headline)
                .put("summary", analysis.summary)
                .put("strengths", JSONArray(analysis.strengths))
                .put(
                    "careers",
                    JSONArray().apply {
                        analysis.careers.forEach { career ->
                            put(
                                JSONObject()
                                    .put("title", career.title)
                                    .put("reason", career.reason)
                            )
                        }
                    }
                )
                .put("nextSteps", JSONArray(analysis.nextSteps))
        )
        .put("targetCareer", targetCareer)
        .put("completedSteps", JSONArray(completedSteps.sorted()))
        .put(
            "tasks",
            JSONArray().apply {
                tasks.forEach { task ->
                    put(
                        JSONObject()
                            .put("id", task.id)
                            .put("title", task.title)
                            .put("category", task.category)
                            .put("durationMinutes", task.durationMinutes)
                    )
                }
            }
        )
        .put("completedTaskIds", JSONArray(completedTaskIds.sorted()))
        .put("streakCount", streakCount)
        .put("lastCompletionDate", lastCompletionDate)
        .put("isPremium", isPremium)
        .put(
            "coachMessages",
            JSONArray().apply {
                coachMessages.takeLast(MAX_COACH_MESSAGES).forEach { message ->
                    put(
                        JSONObject()
                            .put("text", message.text)
                            .put("isFromUser", message.isFromUser)
                    )
                }
            }
        )

    private fun JSONObject.toCareerSession(): CareerSession {
        val profileJson = getJSONObject("profile")
        val analysisJson = getJSONObject("analysis")
        return CareerSession(
            profile = UserProfile(
                fullName = profileJson.getString("fullName"),
                university = profileJson.getString("university"),
                department = profileJson.getString("department"),
                classLevel = profileJson.getString("classLevel"),
                universityId = profileJson.optString("universityId", ""),
                programId = profileJson.optString("programId", ""),
                scoreType = profileJson.optString("scoreType", "")
            ),
            interests = getJSONArray("interests").toStringList(),
            skills = getJSONArray("skills").toStringList(),
            analysis = CareerAnalysis(
                headline = analysisJson.getString("headline"),
                summary = analysisJson.getString("summary"),
                strengths = analysisJson.getJSONArray("strengths").toStringList(),
                careers = analysisJson.getJSONArray("careers").toObjectList { career ->
                    CareerRecommendation(
                        title = career.getString("title"),
                        reason = career.getString("reason")
                    )
                },
                nextSteps = analysisJson.getJSONArray("nextSteps").toStringList()
            ),
            targetCareer = getString("targetCareer"),
            completedSteps = optJSONArray("completedSteps")?.toIntSet().orEmpty(),
            tasks = optJSONArray("tasks")?.toObjectList { task ->
                CareerTask(
                    id = task.getString("id"),
                    title = task.getString("title"),
                    category = task.getString("category"),
                    durationMinutes = task.getInt("durationMinutes")
                )
            }.orEmpty(),
            completedTaskIds = optJSONArray("completedTaskIds")?.toStringList()?.toSet().orEmpty(),
            streakCount = optInt("streakCount", 0),
            lastCompletionDate = optString("lastCompletionDate", ""),
            isPremium = optBoolean("isPremium", false),
            coachMessages = optJSONArray("coachMessages")?.toObjectList { message ->
                CoachMessage(
                    text = message.getString("text"),
                    isFromUser = message.optBoolean("isFromUser", false)
                )
            }.orEmpty()
        )
    }

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (index in 0 until length()) add(getString(index))
    }

    private fun JSONArray.toIntSet(): Set<Int> = buildSet {
        for (index in 0 until length()) add(getInt(index))
    }

    private fun <T> JSONArray.toObjectList(transform: (JSONObject) -> T): List<T> = buildList {
        for (index in 0 until length()) add(transform(getJSONObject(index)))
    }

    private companion object {
        const val PREFERENCES_NAME = "migo_preferences"
        const val KEY_SESSION = "career_session"
        const val KEY_INSTALLATION_ID = "installation_id"
        const val KEY_SYNC_SECRET = "sync_secret"
        const val KEY_PENDING_CLOUD_DELETE = "pending_cloud_delete"
        const val MAX_COACH_MESSAGES = 50
    }
}
