import { legalDocumentResponse, legalMetadata } from "./legal-pages.js";

const NVIDIA_ENDPOINT = "https://integrate.api.nvidia.com/v1/chat/completions";
const PRIMARY_MODEL = "mistralai/mistral-small-4-119b-2603";
const FALLBACK_MODEL = "meta/llama-3.3-70b-instruct";
const MODEL_TIMEOUT_MS = 35_000;
const YOK_ATLAS_API = "https://yokatlas.yok.gov.tr/api/tercih-kilavuz";
const CATALOG_CACHE_SECONDS = 7_200;
const MAX_CATALOG_BYTES = 1_000_000;
const MAX_SYNC_BYTES = 250_000;
const VAULT_RETENTION_DAYS = 180;
const INSTALLATION_HEADER = "x-migo-installation";
const INSTALLATION_ID_PATTERN = /^[A-Za-z0-9_-]{16,128}$/;
const SYNC_SECRET_PATTERN = /^[A-Za-z0-9_-]{40,128}$/;
const SYNC_PATH = "/v1/sync/session";
const AI_PATHS = new Set(["/v1/career-analysis", "/v1/coach", "/v1/cv-analysis"]);

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const legalResponse = request.method === "GET" ? legalDocumentResponse(url.pathname) : null;
    if (legalResponse) return legalResponse;
    if (request.method === "GET" && url.pathname === "/v1/legal") {
      return json(legalMetadata(url.origin), 200, "public, max-age=3600");
    }
    if (request.method === "GET" && url.pathname === "/health") {
      return json({
        status: "ok",
        service: "migo-api",
        apiVersion: 4,
        features: [
          "education_catalog",
          "career_analysis",
          "coach",
          "cv_analysis",
          "cloud_sync",
          "legal_pages",
          "automatic_data_retention"
        ]
      });
    }
    if (request.method === "GET" && url.pathname === "/v1/education/universities") {
      try {
        return await educationSearch(request, ctx, "universities");
      } catch (error) {
        logError("education_universities", error);
        return json({ error: safeError(error) }, 502);
      }
    }
    if (request.method === "GET" && url.pathname === "/v1/education/programs") {
      try {
        return await educationSearch(request, ctx, "programs");
      } catch (error) {
        logError("education_programs", error);
        return json({ error: safeError(error) }, 502);
      }
    }
    if (url.pathname === SYNC_PATH) {
      try {
        return await syncSession(request, env);
      } catch (error) {
        const status = error instanceof ClientRequestError ? error.status : 500;
        logError("cloud_sync", error);
        return json({ error: safeError(error) }, status);
      }
    }
    if (request.method !== "POST") return json({ error: "Yalnızca POST desteklenir." }, 405);
    if (!AI_PATHS.has(url.pathname)) return json({ error: "Endpoint bulunamadı." }, 404);

    const installationId = clean(request.headers.get(INSTALLATION_HEADER), 128);
    if (!INSTALLATION_ID_PATTERN.test(installationId)) {
      return json({ error: "Geçerli Migo kurulum kimliği gerekli." }, 401);
    }

    const rateLimitResponse = await enforceAiRateLimits(request, env, url.pathname, installationId);
    if (rateLimitResponse) return rateLimitResponse;
    if (!env.NVIDIA_API_KEY) return json({ error: "Sunucu AI yapılandırması eksik." }, 503);

    let body;
    try {
      body = await readJsonBody(request, 50_000);
    } catch (error) {
      const status = error instanceof ClientRequestError ? error.status : 400;
      return json({ error: safeError(error) }, status);
    }

    try {
      const path = url.pathname;
      if (path === "/v1/career-analysis") return await careerAnalysis(body, env);
      if (path === "/v1/coach") return await coach(body, env);
      if (path === "/v1/cv-analysis") return await cvAnalysis(body, env);
      return json({ error: "Endpoint bulunamadı." }, 404);
    } catch (error) {
      logError(url.pathname, error);
      return json({ error: safeError(error) }, 502);
    }
  },
  async scheduled(_controller, env, ctx) {
    ctx.waitUntil(deleteExpiredVaults(env));
  }
};

async function syncSession(request, env) {
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
      throw new Error("Bulut oturumu okunamadı.");
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

async function deleteExpiredVaults(env) {
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

async function hashSyncSecret(value) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function safeHashEquals(left, right) {
  if (typeof left !== "string" || left.length !== right.length) return false;
  const encoder = new TextEncoder();
  return crypto.subtle.timingSafeEqual(encoder.encode(left), encoder.encode(right));
}

async function enforceAiRateLimits(request, env, path, installationId) {
  if (!env.CLIENT_AI_RATE_LIMITER || !env.NETWORK_AI_RATE_LIMITER) {
    logError("rate_limit", new Error("Rate limit binding eksik."));
    return json({ error: "Sunucu kota yapılandırması eksik." }, 503);
  }

  const networkId = request.headers.get("cf-connecting-ip") || "local-network";
  const [clientResult, networkResult] = await Promise.all([
    env.CLIENT_AI_RATE_LIMITER.limit({ key: `${installationId}:${path}` }),
    env.NETWORK_AI_RATE_LIMITER.limit({ key: `${networkId}:${path}` })
  ]);
  if (!clientResult.success || !networkResult.success) {
    return json(
      { error: "Çok fazla AI isteği gönderildi. Bir dakika sonra tekrar dene." },
      429,
      "no-store",
      { "retry-after": "60" }
    );
  }
  return null;
}

async function careerAnalysis(body, env) {
  const profile = body?.profile || {};
  const interests = stringList(body?.interests, 8);
  const skills = stringList(body?.skills, 8);
  if (!clean(profile.university, 160) || !clean(profile.department, 160)) {
    return json({ error: "Üniversite veya bölüm bilgisi eksik." }, 400);
  }

  const system = `Sen Migo adlı kariyer planlama uygulamasının Türkçe kariyer danışmanısın.
Öğrencinin üniversite, bölüm ve sınıf bilgisini analiz et. Bölümün akademik yapısını, ilişkili kariyerleri,
öğrencinin bu aşamada geliştirmesi mantıklı olan odak alanlarını ve yetkinlikleri çıkar.
Kullanıcının açıkça söylemediği bir beceriyi kazanılmış yetkinlik gibi sunma; bunları "önerilen gelişim alanı" olarak değerlendir.
Gerçekçi ve uygulanabilir öneriler ver. Kesin başarı, iş veya maaş garantisi verme.
Yalnızca geçerli JSON döndür. Şema:
{"headline":"", "summary":"", "inferred_interests":[""], "inferred_skills":[""], "strengths":[""], "careers":[{"title":"","reason":""}], "next_steps":[""]}.
Tam olarak 5 önerilen ilgi/odak alanı, 5 geliştirilmesi önerilen yetkinlik, 3 potansiyel güçlü yön,
3 kariyer ve 4 somut adım üret. Yanıt tamamen Türkçe olsun.`;
  const student = {
    fullName: clean(profile.fullName, 100),
    university: clean(profile.university, 160),
    department: clean(profile.department, 160),
    classLevel: clean(profile.classLevel, 30),
    selfReportedInterests: interests,
    selfReportedSkills: skills
  };
  const output = await callNvidia(env, [
    { role: "system", content: system },
    { role: "user", content: `Bu öğrenci için kariyer analizi hazırla: ${JSON.stringify(student)}` }
  ], 1400, 0.35);

  const parsed = parseJsonObject(output);
  return json(parsed);
}

async function educationSearch(request, ctx, type) {
  const query = clean(new URL(request.url).searchParams.get("q"), 80);
  if (query.length < 2) {
    return json({ items: [], source: "YÖK Atlas", total: 0 });
  }

  const isUniversity = type === "universities";
  const endpoint = isUniversity ? "universiteler" : "universite-programlar";
  const records = await getOfficialCatalog(`${YOK_ATLAS_API}/${endpoint}`, ctx);
  const nameKey = isUniversity ? "universiteAdi" : "birimGrupAdi";
  const idKey = isUniversity ? "universiteId" : "birimGrupId";
  const normalizedQuery = normalizeSearch(query);
  const matches = records
    .filter((item) => normalizeSearch(item?.[nameKey]).includes(normalizedQuery))
    .sort((left, right) => {
      const leftName = normalizeSearch(left?.[nameKey]);
      const rightName = normalizeSearch(right?.[nameKey]);
      const leftStarts = leftName.startsWith(normalizedQuery) ? 0 : 1;
      const rightStarts = rightName.startsWith(normalizedQuery) ? 0 : 1;
      return leftStarts - rightStarts || leftName.localeCompare(rightName, "tr-TR");
    });

  const items = matches.slice(0, 30).map((item) => ({
    id: String(item?.[idKey] ?? ""),
    name: clean(item?.[nameKey], 180),
    meta: isUniversity ? "Üniversite" : clean(item?.puanTuru, 20)
  })).filter((item) => item.id && item.name);

  return json(
    { items, source: "YÖK Atlas", total: matches.length },
    200,
    "public, max-age=300"
  );
}

async function getOfficialCatalog(url, ctx) {
  const cache = caches.default;
  let response = await cache.match(url);
  if (!response) {
    const upstream = await fetch(url, {
      headers: {
        Accept: "application/json",
        Referer: "https://yokatlas.yok.gov.tr/",
        "User-Agent": "MigoCareerApp/1.0"
      }
    });
    if (!upstream.ok) throw new Error(`YÖK Atlas veri hatası: ${upstream.status}`);
    const contentLength = Number(upstream.headers.get("content-length") || 0);
    if (contentLength > MAX_CATALOG_BYTES) throw new Error("YÖK Atlas yanıtı beklenenden büyük.");

    response = new Response(upstream.body, {
      status: upstream.status,
      headers: upstream.headers
    });
    response.headers.set("cache-control", `public, max-age=${CATALOG_CACHE_SECONDS}`);
    ctx.waitUntil(cache.put(url, response.clone()));
  }

  const text = await readTextBody(response, MAX_CATALOG_BYTES);
  const parsed = JSON.parse(text);
  if (!Array.isArray(parsed)) throw new Error("YÖK Atlas yanıtı okunamadı.");
  return parsed;
}

async function coach(body, env) {
  const question = clean(body?.question, 1000);
  if (!question) return json({ error: "Soru boş olamaz." }, 400);

  const targetCareer = clean(body?.targetCareer, 120);
  const department = clean(body?.department, 120);
  const skills = stringList(body?.skills, 8);
  const system = `Sen Migo uygulamasındaki Türkçe AI kariyer koçusun.
Kullanıcının hedefi ${targetCareer}; bölümü ${department}; yetkinlikleri ${skills.join(", ")}.
Kısa, somut ve uygulanabilir cevap ver. İş, maaş veya başarı garantisi verme. Güncel bilgi bilmiyorsan uydurma.
Uygunsa en fazla 4 maddelik eylem listesiyle bitir.`;
  const history = Array.isArray(body?.history) ? body.history.slice(-8).map((item) => ({
    role: item?.role === "assistant" ? "assistant" : "user",
    content: clean(item?.content, 2000)
  })).filter((item) => item.content) : [];

  const answer = await callNvidia(env, [
    { role: "system", content: system },
    ...history,
    { role: "user", content: question }
  ], 700, 0.45);
  return json({ answer });
}

async function cvAnalysis(body, env) {
  const cvText = clean(body?.cvText, 12_000);
  const targetCareer = clean(body?.targetCareer, 120);
  const department = clean(body?.department, 160);

  if (cvText.length < 80) {
    return json({ error: "CV metni analiz için çok kısa." }, 400);
  }
  if (!targetCareer) {
    return json({ error: "Hedef kariyer bilgisi eksik." }, 400);
  }

  const system = [
    "Sen Migo uygulamasındaki uzman Türkçe CV danışmanısın.",
    "CV'yi adayın hedeflediği role göre değerlendir. Sadece metinde bulunan bilgilere dayan;",
    "olmayan deneyim, teknoloji, başarı, sayı veya kurum uydurma. Ayrımcı değerlendirme yapma.",
    "Kişisel iletişim bilgilerini cevabında tekrar etme. Net, uygulanabilir ve dürüst ol.",
    "Yalnızca geçerli JSON döndür. Şema:",
    '{"score":0,"summary":"","strengths":[""],"improvements":[""],"missing_keywords":[""],"rewritten_bullets":[""]}.',
    "Score 0-100 arasında tam sayı olsun. Tam olarak 3 güçlü yön, 4 öncelikli geliştirme,",
    "en fazla 6 eksik anahtar kelime ve 3 yeniden yazım örneği üret.",
    "Yeniden yazım örneklerinde eksik ölçülebilir bilgiler için [sonuç], [oran] veya [süre] gibi yer tutucular kullan."
  ].join("\n");

  const candidate = {
    targetCareer,
    department,
    cvText
  };
  const output = await callNvidia(env, [
    { role: "system", content: system },
    { role: "user", content: "Bu CV metnini analiz et: " + JSON.stringify(candidate) }
  ], 1300, 0.25);

  const parsed = parseJsonObject(output);
  const score = Number.isFinite(Number(parsed?.score))
    ? Math.max(0, Math.min(100, Math.round(Number(parsed.score))))
    : 0;
  const result = {
    score,
    summary: clean(parsed?.summary, 600),
    strengths: stringList(parsed?.strengths, 3),
    improvements: stringList(parsed?.improvements, 4),
    missing_keywords: stringList(parsed?.missing_keywords, 6),
    rewritten_bullets: stringList(parsed?.rewritten_bullets, 3)
  };
  if (!result.summary || result.strengths.length === 0 || result.improvements.length === 0) {
    throw new Error("AI CV analizini eksik oluşturdu.");
  }
  return json(result);
}

async function callNvidia(env, messages, maxTokens, temperature) {
  let lastError;
  for (const model of [PRIMARY_MODEL, FALLBACK_MODEL]) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), MODEL_TIMEOUT_MS);
    try {
      const response = await fetch(NVIDIA_ENDPOINT, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${env.NVIDIA_API_KEY}`,
          "Content-Type": "application/json"
        },
        signal: controller.signal,
        body: JSON.stringify({
          model,
          messages,
          max_tokens: maxTokens,
          temperature,
          top_p: 0.85,
          stream: false
        })
      });
      if (!response.ok) throw new Error(`AI sağlayıcı hatası: ${response.status}`);
      const payload = await response.json();
      const content = payload?.choices?.[0]?.message?.content?.trim();
      if (!content) throw new Error("AI boş yanıt verdi.");
      return content;
    } catch (error) {
      lastError = error;
    } finally {
      clearTimeout(timeout);
    }
  }
  throw lastError || new Error("AI yanıt vermedi.");
}

function parseJsonObject(value) {
  const cleaned = String(value).replace(/^```json/i, "").replace(/^```/, "").replace(/```$/, "").trim();
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start < 0 || end <= start) throw new Error("AI yanıtı okunamadı.");
  return JSON.parse(cleaned.slice(start, end + 1));
}

class ClientRequestError extends Error {
  constructor(status, message) {
    super(message);
    this.name = "ClientRequestError";
    this.status = status;
  }
}

async function readJsonBody(request, maxBytes) {
  const declaredLength = Number(request.headers.get("content-length") || 0);
  if (declaredLength > maxBytes) {
    throw new ClientRequestError(413, "İstek çok büyük.");
  }
  if (!request.body) {
    throw new ClientRequestError(400, "İstek gövdesi eksik.");
  }

  const reader = request.body.getReader();
  const chunks = [];
  let received = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      received += value.byteLength;
      if (received > maxBytes) {
        await reader.cancel();
        throw new ClientRequestError(413, "İstek çok büyük.");
      }
      chunks.push(value);
    }
  } finally {
    reader.releaseLock();
  }

  const bytes = new Uint8Array(received);
  let offset = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, offset);
    offset += chunk.byteLength;
  }

  try {
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch {
    throw new ClientRequestError(400, "Geçersiz JSON.");
  }
}

async function readTextBody(response, maxBytes) {
  if (!response.body) throw new Error("YÖK Atlas yanıt gövdesi eksik.");
  const reader = response.body.getReader();
  const chunks = [];
  let received = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      received += value.byteLength;
      if (received > maxBytes) {
        await reader.cancel();
        throw new Error("YÖK Atlas yanıtı beklenenden büyük.");
      }
      chunks.push(value);
    }
  } finally {
    reader.releaseLock();
  }

  const bytes = new Uint8Array(received);
  let offset = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return new TextDecoder().decode(bytes);
}

function clean(value, maxLength) {
  return typeof value === "string" ? value.trim().slice(0, maxLength) : "";
}

function stringList(value, maxItems) {
  return Array.isArray(value) ? value.slice(0, maxItems).map((item) => clean(item, 100)).filter(Boolean) : [];
}

function normalizeSearch(value) {
  return String(value ?? "")
    .toLocaleLowerCase("tr-TR")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/ı/g, "i")
    .replace(/[^a-z0-9çğıöşü ]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function safeError(error) {
  const message = error instanceof Error ? error.message : "Beklenmeyen sunucu hatası.";
  return message.replace(/nvapi-[A-Za-z0-9_-]+/g, "[secret]").slice(0, 240);
}

function logError(operation, error) {
  console.error(JSON.stringify({
    level: "error",
    operation,
    message: safeError(error)
  }));
}

function json(value, status = 200, cacheControl = "no-store", extraHeaders = {}) {
  return new Response(JSON.stringify(value), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": cacheControl,
      "x-content-type-options": "nosniff",
      "x-migo-api-version": "4",
      ...extraHeaders
    }
  });
}
