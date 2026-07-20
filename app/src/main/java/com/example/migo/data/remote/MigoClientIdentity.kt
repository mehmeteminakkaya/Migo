package com.example.migo.data.remote

import java.net.HttpURLConnection

internal object MigoClientIdentity {
    @Volatile
    private var installationId: String = ""
    @Volatile
    private var syncSecret: String = ""

    fun configure(installationValue: String, syncSecretValue: String) {
        require(installationValue.length in 16..128) { "Geçersiz Migo kurulum kimliği." }
        require(syncSecretValue.length in 40..128) { "Geçersiz Migo eşitleme anahtarı." }
        installationId = installationValue
        syncSecret = syncSecretValue
    }

    fun applyTo(connection: HttpURLConnection) {
        check(installationId.isNotBlank()) { "Migo istemci kimliği hazırlanmadı." }
        connection.setRequestProperty("X-Migo-Installation", installationId)
    }

    fun applySyncAuth(connection: HttpURLConnection) {
        applyTo(connection)
        check(syncSecret.isNotBlank()) { "Migo eşitleme kimliği hazırlanmadı." }
        connection.setRequestProperty("Authorization", "Bearer $syncSecret")
    }
}
