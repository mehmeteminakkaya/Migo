package com.example.migo.data.remote

import com.example.migo.BuildConfig
import com.example.migo.data.model.EducationOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

object EducationCatalogService {
    private const val YOK_ATLAS_BASE_URL =
        "https://yokatlas.yok.gov.tr/api/tercih-kilavuz"
    private const val MAX_CATALOG_BYTES = 1_000_000
    private val turkishLocale = Locale.forLanguageTag("tr-TR")

    @Volatile
    private var universityCatalog: List<EducationOption>? = null

    @Volatile
    private var programCatalog: List<EducationOption>? = null

    suspend fun searchUniversities(query: String): Result<List<EducationOption>> =
        search(
            backendPath = "/v1/education/universities",
            officialPath = "/universiteler",
            query = query,
            isUniversity = true
        )

    suspend fun searchPrograms(query: String): Result<List<EducationOption>> =
        search(
            backendPath = "/v1/education/programs",
            officialPath = "/universite-programlar",
            query = query,
            isUniversity = false
        )

    private suspend fun search(
        backendPath: String,
        officialPath: String,
        query: String,
        isUniversity: Boolean
    ): Result<List<EducationOption>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext Result.success(emptyList())

        runCatching {
            val backendUrl = BuildConfig.MIGO_BACKEND_URL.trimEnd('/')
            if (backendUrl.isNotBlank()) {
                try {
                    val encodedQuery = URLEncoder.encode(cleanQuery, Charsets.UTF_8.name())
                    return@runCatching executeBackendGet(
                        backendUrl + backendPath + "?q=" + encodedQuery
                    )
                } catch (_: IOException) {
                    // The public YÖK Atlas catalog keeps onboarding usable while
                    // a Worker deployment is unavailable or temporarily stale.
                }
            }
            searchOfficialCatalog(officialPath, cleanQuery, isUniversity)
        }.recoverCatching {
            throw IOException(
                "Eğitim kataloğuna şu anda ulaşılamıyor. İnternet bağlantını kontrol edip tekrar dene."
            )
        }
    }

    private fun executeBackendGet(endpoint: String): List<EducationOption> {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Migo-Client", "android/" + BuildConfig.VERSION_NAME)
            MigoClientIdentity.applyTo(this)
        }

        try {
            val responseCode = connection.responseCode
            val response = (if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                val message = runCatching { JSONObject(response).optString("error") }
                    .getOrDefault("")
                throw IOException(
                    message.ifBlank {
                        "Migo eğitim servisi geçici olarak kullanılamıyor."
                    }
                )
            }

            val items = JSONObject(response).optJSONArray("items") ?: return emptyList()
            return buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val name = item.optString("name")
                    if (id.isNotBlank() && name.isNotBlank()) {
                        add(
                            EducationOption(
                                id = id,
                                name = name,
                                meta = item.optString("meta")
                            )
                        )
                    }
                }
            }
        } catch (_: SocketTimeoutException) {
            throw IOException("Migo eğitim servisi zaman aşımına uğradı.")
        } finally {
            connection.disconnect()
        }
    }

    private fun searchOfficialCatalog(
        officialPath: String,
        query: String,
        isUniversity: Boolean
    ): List<EducationOption> {
        val cached = if (isUniversity) universityCatalog else programCatalog
        val catalog = cached ?: loadOfficialCatalog(officialPath, isUniversity).also {
            if (isUniversity) universityCatalog = it else programCatalog = it
        }
        val normalizedQuery = normalizeSearch(query)
        return catalog
            .asSequence()
            .filter { normalizeSearch(it.name).contains(normalizedQuery) }
            .sortedWith(
                compareBy<EducationOption> {
                    !normalizeSearch(it.name).startsWith(normalizedQuery)
                }.thenBy { normalizeSearch(it.name) }
            )
            .take(30)
            .toList()
    }

    private fun loadOfficialCatalog(
        officialPath: String,
        isUniversity: Boolean
    ): List<EducationOption> {
        val connection = (
            URL(YOK_ATLAS_BASE_URL + officialPath).openConnection() as HttpURLConnection
            ).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 25_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Referer", "https://yokatlas.yok.gov.tr/")
            setRequestProperty("User-Agent", "MigoCareerApp/" + BuildConfig.VERSION_NAME)
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("YÖK Atlas veri servisi hata verdi.")
            }
            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_CATALOG_BYTES) {
                throw IOException("YÖK Atlas yanıtı beklenenden büyük.")
            }
            val raw = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            if (raw.length > MAX_CATALOG_BYTES) {
                throw IOException("YÖK Atlas yanıtı beklenenden büyük.")
            }

            val nameKey = if (isUniversity) "universiteAdi" else "birimGrupAdi"
            val idKey = if (isUniversity) "universiteId" else "birimGrupId"
            val records = JSONArray(raw)
            return buildList {
                for (index in 0 until records.length()) {
                    val item = records.optJSONObject(index) ?: continue
                    val id = item.optString(idKey)
                    val name = item.optString(nameKey)
                    if (id.isNotBlank() && name.isNotBlank()) {
                        add(
                            EducationOption(
                                id = id,
                                name = name,
                                meta = if (isUniversity) {
                                    "Üniversite"
                                } else {
                                    item.optString("puanTuru")
                                }
                            )
                        )
                    }
                }
            }
        } catch (_: SocketTimeoutException) {
            throw IOException("YÖK Atlas veri servisi zaman aşımına uğradı.")
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeSearch(value: String): String =
        Normalizer.normalize(value.lowercase(turkishLocale), Normalizer.Form.NFD)
            .replace(Regex("[\\u0300-\\u036f]"), "")
            .replace('ı', 'i')
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
