import { hashSyncSecret, safeHashEquals } from "./auth.js";
import { INSTALLATION_HEADER, INSTALLATION_ID_PATTERN, MAX_SYNC_BYTES, SYNC_SECRET_PATTERN, VAULT_RETENTION_DAYS } from "./config.js";
import { ClientRequestError, UserFacingError, json, logError, readJsonBody } from "./http.js";
import { clean } from "./text.js";

export async function syncSession(request, env) {
  if (!env.DB) return json({ error: "Bulut veritabanı yapılandırması eksik." }, 503);

  const installationId = clean(request.headers.get(INSTALLATION_HEADER), 128);
  if (!INSTALLATION_ID_PATTERN.test(installationId)) {
    return json({ error: "Geçerli Migo kurulum kimliği gerekli." }, 401);
  }
  const bearer = request.headers.get("authorization") || "";
  const syncSecret = bearer.startsWith("Bearer ") ? bearer.slice(7).trim() : "";
  if (!SYNC_SECRET_PATTERN.test(syncSecret)) {
    return json({ error: "Geçerli bulut kasa anahtarı gerekli." }, 401);
  }
  if (!env.SYNC_RATE_LIMITER) {
    logError("sync_rate_limit", new Error("Sync rate limit binding eksik."));
    return json({ error: "Sunucu senkronizasyon kotası eksik." }, 503);
  }
  const limit = await env.SYNC_RATE_LIMITER.limit({ key: installationId });
  if (!limit.success) {
    return json(
      { error: "Çok fazla senkronizasyon isteği gönderildi. Bir dakika sonra tekrar dene." },
      429,
      "no-store",
      { "retry-after": "60" }
    );
  }

  const authHash = await hashSyncSecret(syncSecret);
  const row = await env.DB.prepare(
    "SELECT auth_hash, session_json, session_version, updated_at FROM user_vaults WHERE user_id = ? LIMIT 1"
  ).bind(installationId).first();

  if (request.method === "PUT") {
    const body = await readJsonBody(request, MAX_SYNC_BYTES);
    if (!body?.session || typeof body.session !== "object" || Array.isArray(body.session)) {
      throw new ClientRequestError(400, "Senkronize edilecek oturum eksik.");
    }
    const sessionJson = JSON.stringify(body.session);
    if (new TextEncoder().encode(sessionJson).byteLength > MAX_SYNC_BYTES) {
      throw new ClientRequestError(413, "Senkronizasyon verisi çok büyük.");
    }
    if (row && !safeHashEquals(row.auth_hash, authHash)) {
      return json({ error: "Bulut kasası doğrulanamadı." }, 403);
    }

    const updatedAt = new Date().toISOString();
    const expiresAt = new Date(Date.now() + VAULT_RETENTION_DAYS * 86_400_000).toISOString();
    if (row) {
      await env.DB.prepare(
        "UPDATE user_vaults SET session_json = ?, session_version = session_version + 1, updated_at = ?, expires_at = ? WHERE user_id = ?"
      ).bind(sessionJson, updatedAt, expiresAt, installationId).run();
    } else {
      await env.DB.prepare(
        "INSERT INTO user_vaults (user_id, auth_hash, session_json, session_version, created_at, updated_at, expires_at) VALUES (?, ?, ?, 1, ?, ?, ?)"
      ).bind(installationId, authHash, sessionJson, updatedAt, updatedAt, expiresAt).run();
    }
    return json({ synced: true, updatedAt, expiresAt });
  }

  if (!row) return json({ error: "Bulut yedeği bulunamadı." }, 404);
  if (!safeHashEquals(row.auth_hash, authHash)) {
    return json({ error: "Bulut kasası doğrulanamadı." }, 403);
  }

  if (request.method === "GET") {
    let session;
    try {
      session = JSON.parse(row.session_json);
    } catch {
      throw new UserFacingError("Bulut oturumu okunamadı.");
    }
    return json({ session, version: row.session_version, updatedAt: row.updated_at });
  }
  if (request.method === "DELETE") {
    await env.DB.prepare("DELETE FROM user_vaults WHERE user_id = ?")
      .bind(installationId).run();
    return json({ deleted: true });
  }
  return json({ error: "Bu uçta GET, PUT veya DELETE kullan." }, 405);
}

export async function deleteExpiredVaults(env) {
  if (!env.DB) {
    logError("vault_retention", new Error("D1 binding eksik."));
    return;
  }
  const result = await env.DB.prepare(
    "DELETE FROM user_vaults WHERE expires_at IS NOT NULL AND expires_at <= ?"
  ).bind(new Date().toISOString()).run();
  console.log(JSON.stringify({
    level: "info",
    operation: "vault_retention",
    deleted: Number(result?.meta?.changes || 0)
  }));
}
