package com.example.migo.data.remote

import android.content.Context

object AiCoachRules {
    private const val ASSET_NAME = "MIGO_AI_KARIYER_KOCU_KURALLARI.md"
    private const val MAX_RULES_CHARACTERS = 24_000
    private const val FALLBACK_RULES =
        "Kariyer önerilerini öğrencinin bölümü, sınıfı ve hedefiyle ilişkilendir; " +
            "somut çıktı, ölçülebilir tamamlanma kriteri ve gerçekçi süre belirt."

    @Volatile
    private var cachedRules: String? = null

    fun load(context: Context): String = cachedRules ?: synchronized(this) {
        cachedRules ?: runCatching {
            context.applicationContext.assets
                .open(ASSET_NAME)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
                .trim()
                .take(MAX_RULES_CHARACTERS)
                .ifBlank { FALLBACK_RULES }
        }.getOrDefault(FALLBACK_RULES).also { cachedRules = it }
    }
}
