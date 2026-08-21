package com.example.migo.data.remote

import java.net.HttpURLConnection

/**
 * Holds the per-install request identity sent to the Worker.
 *
 * [installationId] is a value the app itself generates (see `MigoPreferences.installationId`), so the
 * backend can never treat it alone as a trustworthy rate-limit key -- a hostile client could just send a
 * new random one on every request. To close that gap the Worker mints and HMAC-signs an opaque
 * [installationToken] the first time it sees a client and echoes it back on every AI response
 * (`X-Migo-Installation-Token`); this class caches that token and resends it on subsequent requests, and
 * [captureTokenFrom] picks up a refreshed one from each response so it survives token rotation/expiry on
 * the server side. The server's per-installation rate limiter keys off the verified token subject, not
 * the raw [installationId], so rotating the self-chosen id no longer grants a fresh quota bucket.
 */
internal object MigoClientIdentity {
    private const val INSTALLATION_TOKEN_HEADER = "X-Migo-Installation-Token"

    @Volatile
    private var installationId: String = ""
    @Volatile
    private var syncSecret: String = ""
    @Volatile
    private var installationToken: String = ""
    @Volatile
    private var onTokenUpdated: (String) -> Unit = {}

    fun configure(
        installationValue: String,
        syncSecretValue: String,
        tokenValue: String = "",
        onTokenUpdated: (String) -> Unit = {}
    ) {
        require(installationValue.length in 16..128) { "Geçersiz Migo kurulum kimliği." }
        require(syncSecretValue.length in 40..128) { "Geçersiz Migo eşitleme anahtarı." }
        installationId = installationValue
        syncSecret = syncSecretValue
        installationToken = tokenValue
        this.onTokenUpdated = onTokenUpdated
    }

    fun applyTo(connection: HttpURLConnection) {
        check(installationId.isNotBlank()) { "Migo istemci kimliği hazırlanmadı." }
        connection.setRequestProperty("X-Migo-Installation", installationId)
        installationToken.takeIf(String::isNotBlank)?.let {
            connection.setRequestProperty(INSTALLATION_TOKEN_HEADER, it)
        }
    }

    fun applySyncAuth(connection: HttpURLConnection) {
        applyTo(connection)
        check(syncSecret.isNotBlank()) { "Migo eşitleme kimliği hazırlanmadı." }
        connection.setRequestProperty("Authorization", "Bearer $syncSecret")
    }

    /** Reads a (possibly refreshed) signed installation token off a completed response and caches it. */
    fun captureTokenFrom(connection: HttpURLConnection) {
        val header = runCatching { connection.getHeaderField(INSTALLATION_TOKEN_HEADER) }.getOrNull()
        if (!header.isNullOrBlank() && header != installationToken) {
            installationToken = header
            onTokenUpdated(header)
        }
    }
}
