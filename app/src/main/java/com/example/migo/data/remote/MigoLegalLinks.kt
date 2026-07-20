package com.example.migo.data.remote

import com.example.migo.BuildConfig

object MigoLegalLinks {
    private val backendUrl: String
        get() = BuildConfig.MIGO_BACKEND_URL.trim().trimEnd('/')

    val privacy: String
        get() = "$backendUrl/privacy"

    val terms: String
        get() = "$backendUrl/terms"

    val support: String
        get() = "$backendUrl/support"
}
