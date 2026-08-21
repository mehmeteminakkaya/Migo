package com.example.migo.data.local

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.example.migo.data.model.AiContentSanitizer
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerActionPlan
import com.example.migo.data.model.CareerPlanAction
import com.example.migo.data.model.CareerPlanWeek
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerRoadmapStep
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CareerTask
import com.example.migo.data.model.CareerTaskPlanner
import com.example.migo.data.model.CoachMessage
import com.example.migo.data.model.DailyCareerInsight
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
    private val localDataCipher = LocalDataCipher()

    fun saveSession(session: CareerSession) {
        preferences.edit {
            putString(KEY_SESSION, localDataCipher.encrypt(encodeSession(session)))
            putBoolean(KEY_PENDING_CLOUD_DELETE, false)
        }
    }

    fun loadSession(): CareerSession? {
        val raw = preferences.getString(KEY_SESSION, null) ?: return null
        val decrypted = runCatching { localDataCipher.decrypt(raw) }.getOrNull() ?: return null
        val session = decodeSession(decrypted) ?: return null
        migrateToEncryptedStorage(KEY_SESSION, raw, decrypted)
        return session
    }

    fun encodeSession(session: CareerSession): String = session.toJson().toString()

    fun decodeSession(raw: String): CareerSession? =
        runCatching { JSONObject(raw).toCareerSession() }.getOrNull()

    fun clear() {
        preferences.edit {
            remove(KEY_SESSION)
            remove(KEY_PRIVACY_CONSENT_VERSION)
            remove(KEY_PRIVACY_CONSENT_AT)
            putBoolean(KEY_PENDING_CLOUD_DELETE, true)
        }
    }

    fun hasAcceptedPrivacy(): Boolean =
        preferences.getInt(KEY_PRIVACY_CONSENT_VERSION, 0) >= CURRENT_PRIVACY_VERSION

    fun acceptPrivacy() {
        preferences.edit(commit = true) {
            putInt(KEY_PRIVACY_CONSENT_VERSION, CURRENT_PRIVACY_VERSION)
            putLong(KEY_PRIVACY_CONSENT_AT, System.currentTimeMillis())
        }
    }

    fun privacyConsentAcceptedAt(): Long =
        preferences.getLong(KEY_PRIVACY_CONSENT_AT, 0L)

    fun installationId(): String {
        preferences.getString(KEY_INSTALLATION_ID, null)
            ?.takeIf { it.length in 16..128 }
            ?.let { return it }

        val generated = UUID.randomUUID().toString().replace("-", "")
        preferences.edit(commit = true) { putString(KEY_INSTALLATION_ID, generated) }
        return generated
    }

    fun syncSecret(): String {
        preferences.getString(KEY_SYNC_SECRET, null)?.let { stored ->
            val decrypted = runCatching { localDataCipher.decrypt(stored) }.getOrNull()
            decrypted?.takeIf { it.length in 40..128 }?.let { validSecret ->
                migrateToEncryptedStorage(KEY_SYNC_SECRET, stored, validSecret)
                return validSecret
            }
        }

        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        val generated = Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        preferences.edit(commit = true) {
            putString(KEY_SYNC_SECRET, localDataCipher.encrypt(generated))
        }
        return generated
    }

    /** The Worker-issued, HMAC-signed installation token (see MigoClientIdentity), empty until first use. */
    fun installationToken(): String =
        preferences.getString(KEY_INSTALLATION_TOKEN, null).orEmpty()

    fun saveInstallationToken(token: String) {
        preferences.edit { putString(KEY_INSTALLATION_TOKEN, token) }
    }

    fun isCloudDeletePending(): Boolean =
        preferences.getBoolean(KEY_PENDING_CLOUD_DELETE, false)

    fun markCloudDeleteComplete() {
        preferences.edit { putBoolean(KEY_PENDING_CLOUD_DELETE, false) }
    }

    private fun migrateToEncryptedStorage(key: String, storedValue: String, plainText: String) {
        if (localDataCipher.isEncrypted(storedValue)) return
        runCatching {
            preferences.edit(commit = true) {
                putString(key, localDataCipher.encrypt(plainText))
            }
        }
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
                                    .put("roles", JSONArray(career.roles))
                                    .put("workAreas", JSONArray(career.workAreas))
                                    .put("exampleEmployers", JSONArray(career.exampleEmployers))
                                    .put("salaryRange", career.salaryRange)
                                    .put("salaryNote", career.salaryNote)
                                    .put("salarySourceName", career.salarySourceName)
                                    .put("salarySourceUrl", career.salarySourceUrl)
                                    .put("salarySourceDate", career.salarySourceDate)
                                    .put("salaryDataType", career.salaryDataType)
                                    .put("hiringOutlook", career.hiringOutlook)
                                    .put("demandNote", career.demandNote)
                                    .put("requirements", JSONArray(career.requirements))
                                    .put("dailyWork", JSONArray(career.dailyWork))
                                    .put("first90Days", JSONArray(career.first90Days))
                            )
                        }
                    }
                )
                .put("nextSteps", JSONArray(analysis.nextSteps))
                .put("levelFocusTitle", analysis.levelFocusTitle)
                .put("levelFocusDescription", analysis.levelFocusDescription)
                .put(
                    "roadmap",
                    JSONArray().apply {
                        analysis.roadmap.forEach { step ->
                            put(
                                JSONObject()
                                    .put("period", step.period)
                                    .put("title", step.title)
                                    .put("description", step.description)
                                    .put("actions", JSONArray(step.actions))
                            )
                        }
                    }
                )
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
                            .put("description", task.description)
                    )
                }
            }
        )
        .put("completedTaskIds", JSONArray(completedTaskIds.sorted()))
        .put("streakCount", streakCount)
        .put("lastCompletionDate", lastCompletionDate)
        .put("isPremium", isPremium)
        .put("actionPlan", actionPlan?.toJson() ?: JSONObject.NULL)
        .put("dailyInsight", dailyInsight?.toJson() ?: JSONObject.NULL)
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

    private fun CareerActionPlan.toJson(): JSONObject = JSONObject()
        .put("title", title)
        .put("objective", objective)
        .put("durationDays", durationDays)
        .put(
            "weeks",
            JSONArray().apply {
                weeks.forEach { week ->
                    put(
                        JSONObject()
                            .put("number", week.number)
                            .put("title", week.title)
                            .put("focus", week.focus)
                            .put("outcome", week.outcome)
                            .put(
                                "actions",
                                JSONArray().apply {
                                    week.actions.forEach { action ->
                                        put(
                                            JSONObject()
                                                .put("id", action.id)
                                                .put("title", action.title)
                                                .put("description", action.description)
                                                .put("durationMinutes", action.durationMinutes)
                                                .put("deliverable", action.deliverable)
                                        )
                                    }
                                }
                            )
                    )
                }
            }
        )

    private fun DailyCareerInsight.toJson(): JSONObject = JSONObject()
        .put("dateKey", dateKey)
        .put("title", title)
        .put("message", message)
        .put("actionLabel", actionLabel)
        .put("coachPrompt", coachPrompt)

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
                        reason = career.getString("reason"),
                        roles = career.optJSONArray("roles")?.toStringList().orEmpty(),
                        workAreas = career.optJSONArray("workAreas")?.toStringList().orEmpty(),
                        exampleEmployers = career.optJSONArray("exampleEmployers")?.toStringList().orEmpty(),
                        salaryRange = career.optString("salaryRange", ""),
                        salaryNote = career.optString("salaryNote", ""),
                        salarySourceName = career.optString("salarySourceName", ""),
                        salarySourceUrl = career.optString("salarySourceUrl", ""),
                        salarySourceDate = career.optString("salarySourceDate", ""),
                        salaryDataType = career.optString("salaryDataType", ""),
                        hiringOutlook = career.optString("hiringOutlook", ""),
                        demandNote = career.optString("demandNote", ""),
                        requirements = career.optJSONArray("requirements")?.toStringList().orEmpty(),
                        dailyWork = career.optJSONArray("dailyWork")?.toStringList().orEmpty(),
                        first90Days = career.optJSONArray("first90Days")?.toStringList().orEmpty()
                    )
                },
                nextSteps = analysisJson.getJSONArray("nextSteps").toStringList(),
                levelFocusTitle = analysisJson.optString("levelFocusTitle", ""),
                levelFocusDescription = analysisJson.optString("levelFocusDescription", ""),
                roadmap = analysisJson.optJSONArray("roadmap")?.toObjectList { step ->
                    CareerRoadmapStep(
                        period = step.optString("period", ""),
                        title = step.optString("title", ""),
                        description = step.optString("description", ""),
                        actions = step.optJSONArray("actions")?.toStringList().orEmpty()
                    )
                }.orEmpty()
            ),
            targetCareer = getString("targetCareer"),
            completedSteps = optJSONArray("completedSteps")?.toIntSet().orEmpty(),
            tasks = optJSONArray("tasks")?.toObjectList { task ->
                CareerTaskPlanner.normalize(CareerTask(
                    id = task.getString("id"),
                    title = task.getString("title"),
                    category = task.getString("category"),
                    durationMinutes = task.getInt("durationMinutes"),
                    description = task.optString("description", "")
                ))
            }.orEmpty(),
            completedTaskIds = optJSONArray("completedTaskIds")?.toStringList()?.toSet().orEmpty(),
            streakCount = optInt("streakCount", 0),
            lastCompletionDate = optString("lastCompletionDate", ""),
            isPremium = optBoolean("isPremium", false),
            actionPlan = optJSONObject("actionPlan")?.toCareerActionPlan(),
            dailyInsight = optJSONObject("dailyInsight")?.let { insight ->
                DailyCareerInsight(
                    dateKey = insight.optString("dateKey", ""),
                    title = insight.optString("title", ""),
                    message = insight.optString("message", ""),
                    actionLabel = AiContentSanitizer.taskText(
                        insight.optString("actionLabel", "")
                    ).title,
                    coachPrompt = insight.optString("coachPrompt", "")
                )
            },
            coachMessages = optJSONArray("coachMessages")?.toObjectList { message ->
                CoachMessage(
                    text = message.getString("text"),
                    isFromUser = message.optBoolean("isFromUser", false)
                )
            }.orEmpty()
        )
    }

    private fun JSONObject.toCareerActionPlan(): CareerActionPlan = CareerActionPlan(
        title = optString("title", ""),
        objective = optString("objective", ""),
        durationDays = optInt("durationDays", 30),
        weeks = optJSONArray("weeks")?.toObjectList { week ->
            CareerPlanWeek(
                number = week.optInt("number", 1),
                title = week.optString("title", ""),
                focus = week.optString("focus", ""),
                outcome = week.optString("outcome", ""),
                actions = week.optJSONArray("actions")?.toObjectList { action ->
                    CareerPlanAction(
                        id = action.optString("id", ""),
                        title = action.optString("title", ""),
                        description = action.optString("description", ""),
                        durationMinutes = action.optInt("durationMinutes", 30),
                        deliverable = action.optString("deliverable", "")
                    )
                }.orEmpty()
            )
        }.orEmpty()
    )

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (index in 0 until length()) add(getString(index))
    }

    private fun JSONArray.toIntSet(): Set<Int> = buildSet {
        for (index in 0 until length()) add(getInt(index))
    }

    private fun <T> JSONArray.toObjectList(transform: (JSONObject) -> T): List<T> = buildList {
        for (index in 0 until length()) add(transform(getJSONObject(index)))
    }

    companion object {
        const val CURRENT_PRIVACY_VERSION = 1
        private const val PREFERENCES_NAME = "migo_preferences"
        private const val KEY_SESSION = "career_session"
        private const val KEY_INSTALLATION_ID = "installation_id"
        private const val KEY_INSTALLATION_TOKEN = "installation_token"
        private const val KEY_SYNC_SECRET = "sync_secret"
        private const val KEY_PENDING_CLOUD_DELETE = "pending_cloud_delete"
        private const val KEY_PRIVACY_CONSENT_VERSION = "privacy_consent_version"
        private const val KEY_PRIVACY_CONSENT_AT = "privacy_consent_at"
        private const val MAX_COACH_MESSAGES = 50
    }
}
