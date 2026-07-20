package com.example.migo.data.remote

import com.example.migo.BuildConfig
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CoachMessage
import com.example.migo.data.model.CvAnalysis
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

    private fun JSONObject.toCvAnalysis(): CvAnalysis = CvAnalysis(
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
                it.write(payload.toString())
            }
            val responseCode = connection.responseCode
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

    private fun parseJsonObject(rawOutput: String): JSONObject {
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
        return JSONObject(cleaned.substring(start, end + 1))
    }

    private fun JSONObject.toCareerAnalysis(): CareerAnalysis {
        val careers = getJSONArray("careers").toObjectList { item ->
            CareerRecommendation(
                title = item.getString("title"),
                reason = item.getString("reason")
            )
        }
        return CareerAnalysis(
            headline = getString("headline"),
            summary = getString("summary"),
            strengths = getJSONArray("strengths").toStringList(),
            careers = careers,
            nextSteps = getJSONArray("next_steps").toStringList()
        ).also { analysis ->
            if (analysis.headline.isBlank() || analysis.careers.isEmpty()) {
                throw IOException(
                    "Kariyer analizi eksik oluşturuldu. Lütfen tekrar dene."
                )
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (index in 0 until length()) add(getString(index))
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
