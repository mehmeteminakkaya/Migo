package com.example.migo.data.remote

import android.content.Context
import com.example.migo.BuildConfig
import com.example.migo.data.model.AiContentSanitizer
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerActionPlan
import com.example.migo.data.model.CareerPlanAction
import com.example.migo.data.model.CareerPlanWeek
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerRoadmapStep
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CoachMessage
import com.example.migo.data.model.CvAnalysis
import com.example.migo.data.model.DailyCareerInsight
import com.example.migo.data.model.SmartProfileResult
import com.example.migo.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

object NvidiaCareerService {
    @Volatile
    private var aiCoachRules: String = ""

    fun initialize(context: Context) {
        aiCoachRules = AiCoachRules.load(context)
    }

    suspend fun createSmartProfile(profile: UserProfile): Result<SmartProfileResult> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.MIGO_BACKEND_URL.isBlank()) {
                return@withContext missingBackend()
            }
            runCatching { requestBackendSmartProfile(profile) }
        }

    suspend fun createCareerAnalysis(
        profile: UserProfile,
        interests: List<String>,
        skills: List<String>
    ): Result<CareerAnalysis> = withContext(Dispatchers.IO) {
        if (BuildConfig.MIGO_BACKEND_URL.isBlank()) {
            return@withContext missingBackend()
        }
        runCatching { requestBackendAnalysis(profile, interests, skills) }
    }

    suspend fun askCoach(
        session: CareerSession,
        history: List<CoachMessage>,
        question: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (BuildConfig.MIGO_BACKEND_URL.isBlank()) {
            return@withContext missingBackend()
        }
        runCatching { requestBackendCoach(session, history, question) }
    }

    suspend fun analyzeCv(
        session: CareerSession,
        cvText: String
    ): Result<CvAnalysis> = withContext(Dispatchers.IO) {
        if (BuildConfig.MIGO_BACKEND_URL.isBlank()) {
            return@withContext missingBackend()
        }
        runCatching { requestBackendCvAnalysis(session, cvText) }
    }

    suspend fun enrichCareer(
        profile: UserProfile,
        branch: CareerRecommendation
    ): Result<CareerRecommendation> = withContext(Dispatchers.IO) {
        if (BuildConfig.MIGO_BACKEND_URL.isBlank()) {
            return@withContext missingBackend()
        }
        runCatching { requestBackendCareerDetail(profile, branch) }
    }

    suspend fun createActionPlan(session: CareerSession): Result<CareerActionPlan> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.MIGO_BACKEND_URL.isBlank()) {
                return@withContext Result.success(fallbackActionPlan(session))
            }
            runCatching { requestBackendActionPlan(session) }
        }

    suspend fun createDailyInsight(
        session: CareerSession,
        dateKey: String
    ): Result<DailyCareerInsight> = withContext(Dispatchers.IO) {
        if (BuildConfig.MIGO_BACKEND_URL.isBlank()) {
            return@withContext Result.success(fallbackDailyInsight(session, dateKey))
        }
        runCatching { requestBackendDailyInsight(session, dateKey) }
    }

    private fun requestBackendAnalysis(
        profile: UserProfile,
        interests: List<String>,
        skills: List<String>
    ): CareerAnalysis {
        val payload = JSONObject()
            .put("profile", profile.toJson())
            .put("interests", JSONArray(interests))
            .put("skills", JSONArray(skills))
        return parseAnalysis(
            executeBackendRequest("/v1/career-analysis", payload, 65_000)
        )
    }

    private fun requestBackendCareerDetail(
        profile: UserProfile,
        branch: CareerRecommendation
    ): CareerRecommendation {
        val payload = JSONObject()
            .put("profile", profile.toJson())
            .put(
                "branch",
                JSONObject()
                    .put("title", branch.title)
                    .put("reason", branch.reason)
            )
        val response = try {
            executeBackendRequest("/v1/career-detail", payload, 65_000)
        } catch (error: IOException) {
            if (error.isMissingEndpoint()) {
                throw IOException("Canlı branş ayrıntısı henüz etkin değil.")
            }
            throw error
        }
        return parseCareerDetail(response, branch)
    }

    private fun requestBackendActionPlan(session: CareerSession): CareerActionPlan {
        val branch = session.analysis.careers.firstOrNull { it.title == session.targetCareer }
        val payload = JSONObject()
            .put("profile", session.profile.toJson())
            .put("targetCareer", session.targetCareer)
            .put(
                "branch",
                JSONObject()
                    .put("reason", branch?.reason.orEmpty())
                    .put("requirements", JSONArray(branch?.requirements.orEmpty()))
            )
            .put(
                "completedTasks",
                JSONArray(
                    session.tasks.filter { it.id in session.completedTaskIds }.map { it.title }
                )
            )
        val response = try {
            executeBackendRequest("/v1/action-plan", payload, 65_000)
        } catch (error: IOException) {
            if (error.isMissingEndpoint()) return fallbackActionPlan(session)
            throw error
        }
        return parseJsonObject(response).toCareerActionPlan()
    }

    private fun requestBackendDailyInsight(
        session: CareerSession,
        dateKey: String
    ): DailyCareerInsight {
        val taskList = session.tasks
        val completed = session.completedTaskIds
        val nextTask = taskList.firstOrNull { it.id !in completed }?.title.orEmpty()
        val completion = if (taskList.isEmpty()) 0 else completed.size * 100 / taskList.size
        val payload = JSONObject()
            .put("profile", session.profile.toJson())
            .put("targetCareer", session.targetCareer)
            .put("dateKey", dateKey)
            .put("nextTask", nextTask)
            .put("completionPercent", completion)
        val response = try {
            executeBackendRequest("/v1/daily-insight", payload, 45_000)
        } catch (error: IOException) {
            if (error.isMissingEndpoint()) return fallbackDailyInsight(session, dateKey)
            throw error
        }
        return JSONObject(response).toDailyCareerInsight(dateKey)
    }

    private fun requestBackendSmartProfile(profile: UserProfile): SmartProfileResult {
        val payload = JSONObject()
            .put("profile", profile.toJson())
            .put("interests", JSONArray())
            .put("skills", JSONArray())
        val response = try {
            executeBackendRequest("/v1/career-analysis", payload, 65_000)
        } catch (error: IOException) {
            val isLegacyWorker = error.message
                ?.contains("ilgi alanı veya yetkinlik", ignoreCase = true) == true
            if (!isLegacyWorker) throw error

            payload
                .put(
                    "interests",
                    JSONArray(
                        listOf(
                            profile.department + " alanındaki kariyer yolları",
                            "Bölümle ilişkili sektörleri keşfetme",
                            "Kişisel gelişim ve öğrenme"
                        )
                    )
                )
                .put(
                    "skills",
                    JSONArray(
                        listOf(
                            "Geliştirilecek teknik yetkinlikleri keşfetme",
                            "Problem çözme potansiyeli",
                            "Yeni beceriler öğrenme isteği"
                        )
                    )
                )
            executeBackendRequest("/v1/career-analysis", payload, 65_000)
        }
        return parseSmartProfile(
            response
        )
    }

    private fun requestBackendCoach(
        session: CareerSession,
        history: List<CoachMessage>,
        question: String
    ): String {
        val payload = JSONObject()
            .put("targetCareer", session.targetCareer)
            .put("department", session.profile.department)
            .put("skills", JSONArray(session.skills))
            .put("question", question)
            .put(
                "history",
                JSONArray().apply {
                    history.takeLast(8).forEach { message ->
                        put(
                            JSONObject()
                                .put(
                                    "role",
                                    if (message.isFromUser) "user" else "assistant"
                                )
                                .put("content", message.text)
                        )
                    }
                }
            )
        val response = JSONObject(
            executeBackendRequest("/v1/coach", payload, 65_000)
        )
        return response.optString("answer").ifBlank {
            throw IOException("Migo AI boş yanıt verdi. Lütfen tekrar dene.")
        }
    }

    private fun requestBackendCvAnalysis(
        session: CareerSession,
        cvText: String
    ): CvAnalysis {
        val payload = JSONObject()
            .put("targetCareer", session.targetCareer)
            .put("department", session.profile.department)
            .put("cvText", cvText.take(12_000))
        val response = try {
            JSONObject(executeBackendRequest("/v1/cv-analysis", payload, 65_000))
        } catch (error: IOException) {
            val isLegacyWorker = error.message
                ?.contains("Endpoint bulunamadı", ignoreCase = true) == true
            if (!isLegacyWorker) throw error
            requestLegacyCvAnalysis(session, cvText)
        }
        return response.toCvAnalysis()
    }

    private fun requestLegacyCvAnalysis(
        session: CareerSession,
        cvText: String
    ): JSONObject {
        val question = """
            CV metnini hedef role göre değerlendir. Yalnızca JSON döndür:
            {"score":0,"summary":"","strengths":[""],"improvements":[""],
            "missing_keywords":[""],"rewritten_bullets":[""]}.
            Bilgi uydurma; eksik sayılar için [sonuç] yaz. CV:
        """.trimIndent() + "\n" + cvText.take(650)
        val payload = JSONObject()
            .put("targetCareer", session.targetCareer)
            .put("department", session.profile.department)
            .put("skills", JSONArray(session.skills))
            .put("question", question)
            .put("history", JSONArray())
        val wrapper = JSONObject(
            executeBackendRequest("/v1/coach", payload, 65_000)
        )
        val answer = wrapper.optString("answer").ifBlank {
            throw IOException("Migo AI CV analizi için boş yanıt verdi.")
        }
        return parseJsonObject(answer)
    }

    internal fun JSONObject.toCvAnalysis(): CvAnalysis = CvAnalysis(
        score = optInt("score", 0).coerceIn(0, 100),
        summary = optString("summary"),
        strengths = optJSONArray("strengths")?.toStringList().orEmpty(),
        improvements = optJSONArray("improvements")?.toStringList().orEmpty(),
        missingKeywords = optJSONArray("missing_keywords")?.toStringList().orEmpty(),
        rewrittenBullets = optJSONArray("rewritten_bullets")?.toStringList().orEmpty()
    ).also { analysis ->
        if (analysis.summary.isBlank() || analysis.strengths.isEmpty()) {
            throw IOException(
                "Migo AI CV analizini eksik oluşturdu. Lütfen tekrar dene."
            )
        }
    }

    internal fun parseCareerDetail(
        rawOutput: String,
        fallback: CareerRecommendation
    ): CareerRecommendation {
        val json = parseJsonObject(rawOutput)
        return CareerRecommendation(
            // The title is the join key for selection and plan lookups (`career.title == targetCareer`),
            // so the AI must not rename the branch the user picked.
            title = fallback.title,
            reason = AiContentSanitizer.inline(
                json.optString("overview").ifBlank { fallback.reason },
                500
            ),
            roles = json.optJSONArray("roles")?.toStringList().orEmpty().ifEmpty { fallback.roles },
            workAreas = json.optJSONArray("work_areas")?.toStringList().orEmpty().ifEmpty { fallback.workAreas },
            exampleEmployers = json.optJSONArray("example_employers")?.toStringList().orEmpty().ifEmpty { fallback.exampleEmployers },
            salaryRange = json.optString("salary_range").ifBlank { fallback.salaryRange },
            salaryNote = json.optString("salary_note").ifBlank { fallback.salaryNote },
            salarySourceName = json.optString("salary_source_name").ifBlank { fallback.salarySourceName },
            salarySourceUrl = json.optString("salary_source_url").ifBlank { fallback.salarySourceUrl },
            salarySourceDate = json.optString("salary_source_date").ifBlank { fallback.salarySourceDate },
            salaryDataType = json.optString("salary_data_type").ifBlank { fallback.salaryDataType },
            hiringOutlook = json.optString("hiring_outlook").ifBlank { fallback.hiringOutlook },
            demandNote = json.optString("demand_note").ifBlank { fallback.demandNote },
            requirements = json.optJSONArray("requirements")?.toStringList().orEmpty().ifEmpty { fallback.requirements },
            dailyWork = json.optJSONArray("daily_work")?.toStringList().orEmpty().ifEmpty { fallback.dailyWork },
            first90Days = json.optJSONArray("first_90_days")?.toStringList().orEmpty().ifEmpty { fallback.first90Days }
        ).also { detail ->
            if (detail.reason.isBlank() || detail.roles.size < 2 || detail.requirements.size < 2) {
                throw IOException("Migo AI branş detayını eksik oluşturdu. Lütfen tekrar dene.")
            }
        }
    }

    internal fun JSONObject.toCareerActionPlan(): CareerActionPlan = CareerActionPlan(
            title = AiContentSanitizer.inline(optString("title"), 160),
            objective = AiContentSanitizer.inline(optString("objective"), 420),
        durationDays = optInt("duration_days", 30),
        weeks = optJSONArray("weeks")?.toObjectList { week ->
            CareerPlanWeek(
                number = week.optInt("number", 1),
                title = AiContentSanitizer.inline(week.optString("title"), 120),
                focus = AiContentSanitizer.inline(week.optString("focus"), 240),
                outcome = AiContentSanitizer.inline(week.optString("outcome"), 220),
                actions = week.optJSONArray("actions")?.toObjectList { action ->
                    CareerPlanAction(
                        id = action.optString("id"),
                        title = AiContentSanitizer.taskText(action.optString("title")).title,
                        description = AiContentSanitizer.inline(
                            action.optString("description"),
                            320
                        ),
                        durationMinutes = action.optInt("duration_minutes", 30),
                        deliverable = AiContentSanitizer.inline(
                            action.optString("deliverable"),
                            180
                        )
                    )
                }.orEmpty()
            )
        }.orEmpty()
    ).also { plan ->
        if (plan.title.isBlank() || plan.objective.isBlank() || plan.weeks.size != 4 ||
            plan.weeks.any { it.actions.size < 4 }
        ) {
            throw IOException("Migo AI 30 günlük planı eksik oluşturdu. Lütfen tekrar dene.")
        }
    }

    internal fun JSONObject.toDailyCareerInsight(dateKey: String): DailyCareerInsight =
        DailyCareerInsight(
            dateKey = optString("date_key").ifBlank { dateKey },
            title = AiContentSanitizer.inline(optString("title"), 80),
            message = AiContentSanitizer.inline(optString("message"), 320),
            actionLabel = AiContentSanitizer.taskText(optString("action_label")).title,
            coachPrompt = AiContentSanitizer.inline(optString("coach_prompt"), 280)
        ).also { insight ->
            if (insight.title.isBlank() || insight.message.isBlank() || insight.actionLabel.isBlank()) {
                throw IOException("Migo AI bugünün içgörüsünü oluşturamadı.")
            }
        }

    private fun fallbackActionPlan(session: CareerSession): CareerActionPlan {
        val baseTasks = session.tasks.ifEmpty {
            listOf(
                com.example.migo.data.model.CareerTask("discover", "Alanındaki üç rolü karşılaştır", "Keşif", 30),
                com.example.migo.data.model.CareerTask("practice", "Küçük bir uygulama çıktısı üret", "Uygulama", 60),
                com.example.migo.data.model.CareerTask("market", "Üç güncel ilanı incele", "Araştırma", 30),
                com.example.migo.data.model.CareerTask("profile", "CV veya portföyünü güncelle", "Profil", 40)
            )
        }
        val weekTitles = listOf("Yönünü netleştir", "Temeli uygula", "Kanıt üret", "Görünür ol")
        val weeks = weekTitles.mapIndexed { weekIndex, title ->
            val primary = baseTasks[weekIndex % baseTasks.size]
            CareerPlanWeek(
                number = weekIndex + 1,
                title = title,
                focus = "${session.targetCareer} hedefinde ${primary.category.lowercase()} odağı",
                outcome = "Hafta sonunda paylaşılabilir bir kariyer kanıtı oluştur.",
                actions = listOf(
                    CareerPlanAction(
                        id = "plan_w${weekIndex + 1}_a1",
                        title = primary.title,
                        description = "Görevi tek oturumda tamamlanabilecek küçük bir parçaya böl.",
                        durationMinutes = primary.durationMinutes.coerceIn(15, 120),
                        deliverable = "Tamamlandığını gösteren kısa not veya bağlantı"
                    ),
                    CareerPlanAction(
                        id = "plan_w${weekIndex + 1}_a2",
                        title = "Çıktını değerlendir ve geliştir",
                        description = "Bir kontrol listesiyle eksikleri bul ve tek bir iyileştirme yap.",
                        durationMinutes = 30,
                        deliverable = "Önce/sonra notu"
                    ),
                    CareerPlanAction(
                        id = "plan_w${weekIndex + 1}_a3",
                        title = "Haftanın kanıtını kaydet",
                        description = "Ürettiğin sonucu CV, portföy veya çalışma günlüğüne ekle.",
                        durationMinutes = 20,
                        deliverable = "Portföy veya kariyer günlüğü kaydı"
                    ),
                    CareerPlanAction(
                        id = "plan_w${weekIndex + 1}_a4",
                        title = "Haftayı ölç ve sonraki adımı seç",
                        description = "Çıktını kabul kriterleriyle kontrol et; öğrendiğin bir şeyi ve geliştireceğin bir noktayı yaz.",
                        durationMinutes = 20,
                        deliverable = "Kısa değerlendirme ve sonraki hafta kararı"
                    )
                )
            )
        }
        return CareerActionPlan(
            title = "${session.targetCareer} için 30 günlük rota",
            objective = "Hedefini dört haftada öğrenme, uygulama ve görünür kariyer kanıtlarına dönüştür.",
            weeks = weeks
        )
    }

    private fun fallbackDailyInsight(session: CareerSession, dateKey: String): DailyCareerInsight {
        val nextTask = session.tasks.firstOrNull { it.id !in session.completedTaskIds }
        val action = nextTask?.title ?: "Hedef rolün için tek bir küçük çıktı üret"
        return DailyCareerInsight(
            dateKey = dateKey,
            title = "Bugünün küçük avantajı",
            message = "${session.profile.classLevel} döneminde hızdan çok görünür kanıt biriktirmek fark yaratır. Bugün tek bir adımı bitir.",
            actionLabel = action,
            coachPrompt = "$action görevini bugün bitirebileceğim küçük adımlara böl."
        )
    }

    private fun IOException.isMissingEndpoint(): Boolean =
        message?.contains("Endpoint bulunamadı", ignoreCase = true) == true

    private fun UserProfile.toJson(): JSONObject = JSONObject()
        .put("fullName", fullName)
        .put("university", university)
        .put("universityId", universityId)
        .put("department", department)
        .put("programId", programId)
        .put("scoreType", scoreType)
        .put("classLevel", classLevel)

    private fun executeBackendRequest(
        path: String,
        payload: JSONObject,
        timeoutMillis: Int
    ): String {
        val requestPayload = JSONObject(payload.toString()).apply {
            aiCoachRules.takeIf(String::isNotBlank)?.let { put("aiRules", it) }
        }
        val endpoint = BuildConfig.MIGO_BACKEND_URL.trimEnd('/') + path
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = timeoutMillis
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty(
                "X-Migo-Client",
                "android/" + BuildConfig.VERSION_NAME
            )
            MigoClientIdentity.applyTo(this)
        }
        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                it.write(requestPayload.toString())
            }
            val responseCode = connection.responseCode
            MigoClientIdentity.captureTokenFrom(connection)
            val response = (
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                )?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                val message = runCatching {
                    JSONObject(response).optString("error")
                }.getOrDefault("")
                throw IOException(
                    message.ifBlank {
                        "Migo sunucusuna ulaşılamadı (hata " + responseCode + ")."
                    }
                )
            }
            return response
        } catch (_: SocketTimeoutException) {
            throw IOException("Migo AI şu anda yoğun. Biraz sonra tekrar dene.")
        } finally {
            connection.disconnect()
        }
    }

    private fun parseAnalysis(rawOutput: String): CareerAnalysis =
        parseJsonObject(rawOutput).toCareerAnalysis()

    private fun parseSmartProfile(rawOutput: String): SmartProfileResult {
        val json = parseJsonObject(rawOutput)
        val analysis = json.toCareerAnalysis()
        val inferredInterests = json.optJSONArray("inferred_interests")
            ?.toStringList()
            .orEmpty()
            .ifEmpty { analysis.careers.map { it.title }.take(5) }
        val inferredSkills = json.optJSONArray("inferred_skills")
            ?.toStringList()
            .orEmpty()
            .ifEmpty { analysis.strengths.take(5) }
        return SmartProfileResult(
            analysis = analysis,
            inferredInterests = inferredInterests,
            inferredSkills = inferredSkills
        )
    }

    internal fun parseJsonObject(rawOutput: String): JSONObject {
        val fence = 96.toChar().toString().repeat(3)
        val cleaned = rawOutput
            .trim()
            .removePrefix(fence + "json")
            .removePrefix(fence)
            .removeSuffix(fence)
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start == -1 || end <= start) {
            throw IOException("Yapay zekâ yanıtı okunamadı. Lütfen tekrar dene.")
        }
        return try {
            JSONObject(cleaned.substring(start, end + 1))
        } catch (_: org.json.JSONException) {
            throw IOException("Yapay zekâ yanıtı okunamadı. Lütfen tekrar dene.")
        }
    }

    internal fun JSONObject.toCareerAnalysis(): CareerAnalysis {
        val careerItems = optJSONArray("branches") ?: getJSONArray("careers")
        val careers = careerItems.toObjectList { item ->
            CareerRecommendation(
                title = AiContentSanitizer.inline(item.getString("title"), 120),
                reason = AiContentSanitizer.inline(
                    item.optString("overview").ifBlank { item.optString("reason") },
                    500
                ),
                roles = item.optJSONArray("roles")?.toStringList().orEmpty(),
                workAreas = item.optJSONArray("work_areas")?.toStringList().orEmpty(),
                exampleEmployers = item.optJSONArray("example_employers")?.toStringList().orEmpty(),
                salaryRange = item.optString("salary_range"),
                salaryNote = item.optString("salary_note"),
                salarySourceName = item.optString("salary_source_name"),
                salarySourceUrl = item.optString("salary_source_url"),
                salarySourceDate = item.optString("salary_source_date"),
                salaryDataType = item.optString("salary_data_type"),
                hiringOutlook = item.optString("hiring_outlook"),
                demandNote = item.optString("demand_note"),
                requirements = item.optJSONArray("requirements")?.toStringList().orEmpty(),
                dailyWork = item.optJSONArray("daily_work")?.toStringList().orEmpty(),
                first90Days = item.optJSONArray("first_90_days")?.toStringList().orEmpty()
            )
        }
        val levelFocus = optJSONObject("level_focus")
        val roadmap = optJSONArray("roadmap")?.toObjectList { item ->
            CareerRoadmapStep(
                period = AiContentSanitizer.inline(item.optString("period"), 60),
                title = AiContentSanitizer.inline(item.optString("title"), 120),
                description = AiContentSanitizer.inline(item.optString("description"), 320),
                actions = item.optJSONArray("actions")?.toStringList().orEmpty()
            )
        }.orEmpty()
        return CareerAnalysis(
            headline = AiContentSanitizer.inline(getString("headline"), 180),
            summary = AiContentSanitizer.inline(getString("summary"), 700),
            strengths = (optJSONArray("focus_areas") ?: getJSONArray("strengths")).toStringList(),
            careers = careers,
            nextSteps = getJSONArray("next_steps").toStringList(),
            levelFocusTitle = AiContentSanitizer.inline(
                levelFocus?.optString("title").orEmpty(),
                120
            ),
            levelFocusDescription = AiContentSanitizer.inline(
                levelFocus?.optString("description").orEmpty(),
                360
            ),
            roadmap = roadmap
        ).also { analysis ->
            if (
                analysis.headline.isBlank() ||
                analysis.careers.isEmpty() ||
                analysis.nextSteps.isEmpty()
            ) {
                throw IOException(
                    "Kariyer analizi eksik oluşturuldu. Lütfen tekrar dene."
                )
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (index in 0 until length()) {
            AiContentSanitizer.inline(getString(index)).takeIf(String::isNotBlank)?.let(::add)
        }
    }

    private fun <T> JSONArray.toObjectList(
        transform: (JSONObject) -> T
    ): List<T> = buildList {
        for (index in 0 until length()) add(transform(getJSONObject(index)))
    }

    private fun <T> missingBackend(): Result<T> = Result.failure(
        IllegalStateException("Migo AI bağlantısı yapılandırılmamış.")
    )
}
