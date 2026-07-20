package com.example.migo.data.remote

import com.example.migo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

object CloudSyncService {
    suspend fun pullSession(): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val response = execute(method = "GET")
            if (response.status == HttpURLConnection.HTTP_NOT_FOUND) {
                null
            } else {
                response.requireSuccess()
                JSONObject(response.body).getJSONObject("session").toString()
            }
        }
    }

    suspend fun pushSession(sessionJson: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val session = JSONObject(sessionJson)
            execute(
                method = "PUT",
                body = JSONObject().put("session", session).toString()
            ).requireSuccess()
            Unit
        }
    }

    suspend fun deleteSession(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = execute(method = "DELETE")
            if (response.status != HttpURLConnection.HTTP_NOT_FOUND) {
                response.requireSuccess()
            }
            Unit
        }
    }

    private fun execute(method: String, body: String? = null): SyncResponse {
        val backendUrl = BuildConfig.MIGO_BACKEND_URL.trim().trimEnd('/')
        if (backendUrl.isBlank()) {
            throw IOException("Migo bulut bağlantısı yapılandırılmamış.")
        }

        val connection = (URL(backendUrl + SYNC_PATH).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = body != null
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Migo-Client", "android/" + BuildConfig.VERSION_NAME)
            if (body != null) {
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            MigoClientIdentity.applySyncAuth(this)
        }

        try {
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(body)
                }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                reader.readText().take(MAX_RESPONSE_CHARS)
            }.orEmpty()
            return SyncResponse(status, responseBody)
        } catch (_: SocketTimeoutException) {
            throw IOException("Migo bulut eşitlemesi zaman aşımına uğradı.")
        } finally {
            connection.disconnect()
        }
    }

    private fun SyncResponse.requireSuccess() {
        if (status in 200..299) return
        val serverMessage = runCatching { JSONObject(body).optString("error") }.getOrDefault("")
        throw IOException(
            serverMessage.ifBlank { "Migo bulut eşitlemesi tamamlanamadı (hata $status)." }
        )
    }

    private data class SyncResponse(val status: Int, val body: String)

    private const val SYNC_PATH = "/v1/sync/session"
    private const val MAX_RESPONSE_CHARS = 300_000
}
