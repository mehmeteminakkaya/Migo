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
const INSTALLATION_TOKEN_HEADER = "x-migo-installation-token";
const INSTALLATION_TOKEN_TTL_SECONDS = 400 * 86_400; // ~13 months
const SYNC_SECRET_PATTERN = /^[A-Za-z0-9_-]{40,128}$/;
const SYNC_PATH = "/v1/sync/session";
const AI_PATHS = new Set([
  "/v1/career-analysis",
  "/v1/career-detail",
  "/v1/action-plan",
  "/v1/daily-insight",
  "/v1/coach",
  "/v1/cv-analysis"
]);

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
          "career_detail",
          "action_plan",
          "daily_insight",
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

    const { limited, token } = await enforceAiRateLimits(request, env, url.pathname);
    if (limited) return withInstallationToken(limited, token);
    if (!env.NVIDIA_API_KEY) {
      return withInstallationToken(json({ error: "Sunucu AI yapılandırması eksik." }, 503), token);
    }

    let body;
    try {
      body = await readJsonBody(request, 50_000);
    } catch (error) {
      const status = error instanceof ClientRequestError ? error.status : 400;
      return withInstallationToken(json({ error: safeError(error) }, status), token);
    }

    try {
      const path = url.pathname;
      let response;
      if (path === "/v1/career-analysis") response = await careerAnalysis(body, env);
      else if (path === "/v1/career-detail") response = await careerDetail(body, env);
      else if (path === "/v1/action-plan") response = await actionPlan(body, env);
      else if (path === "/v1/daily-insight") response = await dailyInsight(body, env);
      else if (path === "/v1/coach") response = await coach(body, env);
      else if (path === "/v1/cv-analysis") response = await cvAnalysis(body, env);
      else response = json({ error: "Endpoint bulunamadı." }, 404);
      return withInstallationToken(response, token);
    } catch (error) {
      logError(url.pathname, error);
      return withInstallationToken(json({ error: safeError(error) }, 502), token);
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

// The client-supplied `x-migo-installation` id is not trustworthy on its own: a caller can send a
// fresh random id on every request and start a brand-new rate-limit bucket each time. To stop that,
// the per-installation limiter keys off a server-signed opaque token (`resolveInstallationSubject`)
// instead. The token's subject is minted here (crypto.randomUUID, never client-chosen) and HMAC-signed
// with a Worker secret, so a client cannot forge one for an id it wants to impersonate, and discarding
// a token to get a new one still only buys a fresh per-installation bucket -- it does NOT evade the
// per-network-IP limiter below, which is the actual backstop against that "always mint a new one" case.
async function enforceAiRateLimits(request, env, path) {
  if (!env.CLIENT_AI_RATE_LIMITER || !env.NETWORK_AI_RATE_LIMITER) {
    logError("rate_limit", new Error("Rate limit binding eksik."));
    return { limited: json({ error: "Sunucu kota yapılandırması eksik." }, 503), token: null };
  }
  if (!env.INSTALLATION_TOKEN_SECRET) {
    logError("rate_limit", new Error("Kurulum imza anahtarı eksik."));
    return { limited: json({ error: "Sunucu kota yapılandırması eksik." }, 503), token: null };
  }

  const { subject, token } = await resolveInstallationSubject(request, env);
  const networkId = request.headers.get("cf-connecting-ip") || "local-network";
  const [clientResult, networkResult] = await Promise.all([
    env.CLIENT_AI_RATE_LIMITER.limit({ key: `${subject}:${path}` }),
    env.NETWORK_AI_RATE_LIMITER.limit({ key: `${networkId}:${path}` })
  ]);
  if (!clientResult.success || !networkResult.success) {
    return {
      limited: json(
        { error: "Çok fazla AI isteği gönderildi. Bir dakika sonra tekrar dene." },
        429,
        "no-store",
        { "retry-after": "60" }
      ),
      token
    };
  }
  return { limited: null, token };
}

// Verifies the token the client presented (if any) and returns its signed subject; otherwise mints and
// signs a brand-new one. The minted/verified token is always echoed back via `withInstallationToken` so
// a client that had none, or whose token failed verification (e.g. after a secret rotation), picks up a
// valid one from the very next response.
async function resolveInstallationSubject(request, env) {
  const presented = clean(request.headers.get(INSTALLATION_TOKEN_HEADER), 200);
  if (presented) {
    const verified = await verifyInstallationToken(presented, env);
    if (verified) return { subject: verified.subject, token: presented };
  }
  return issueInstallationToken(env);
}

async function issueInstallationToken(env) {
  const subject = crypto.randomUUID();
  const issuedAt = Math.floor(Date.now() / 1000);
  const signature = await signInstallationToken(subject, issuedAt, env);
  return { subject, token: `${subject}.${issuedAt}.${signature}` };
}

async function verifyInstallationToken(token, env) {
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  const [subject, issuedAtRaw, signature] = parts;
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(subject)) return null;
  const issuedAt = Number(issuedAtRaw);
  if (!Number.isInteger(issuedAt) || issuedAt <= 0) return null;
  if (Math.floor(Date.now() / 1000) - issuedAt > INSTALLATION_TOKEN_TTL_SECONDS) return null;

  const expected = await signInstallationToken(subject, issuedAtRaw, env);
  if (!safeHashEquals(expected, signature)) return null;
  return { subject, issuedAt };
}

async function signInstallationToken(subject, issuedAt, env) {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(env.INSTALLATION_TOKEN_SECRET),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signatureBytes = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(`${subject}.${issuedAt}`)
  );
  return base64UrlEncode(new Uint8Array(signatureBytes));
}

function base64UrlEncode(bytes) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function withInstallationToken(response, token) {
  if (!token) return response;
  const headers = new Headers(response.headers);
  headers.set(INSTALLATION_TOKEN_HEADER, token);
  return new Response(response.body, { status: response.status, headers });
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

async function careerDetail(body, env) {
  const profile = body?.profile || {};
  const branch = body?.branch || {};
  const university = clean(profile.university, 160);
  const department = clean(profile.department, 160);
  const branchTitle = clean(branch.title, 120);
  if (!university || !department || !branchTitle) {
    return json({ error: "Üniversite, bölüm veya branş bilgisi eksik." }, 400);
  }

  const system = `Sen Migo adlı kariyer planlama uygulamasının Türkçe kariyer danışmanısın.
Öğrencinin üniversite/bölüm/sınıf bilgisiyle seçtiği kariyer dalı için ayrıntılı bir künye hazırla.
Kesin iş, maaş veya işe alım garantisi verme; maaş bir tahmindir, bunu salary_note alanında belirt.
Düzenlenmiş meslekler için gerçek yasal/lisans şartlarını yaz, uydurma.
Yalnızca geçerli JSON döndür. Şema:
{"title":"","overview":"","roles":[""],"work_areas":[""],"example_employers":[""],
"salary_range":"","salary_note":"","hiring_outlook":"","demand_note":"",
"requirements":[""],"daily_work":[""],"first_90_days":[""]}.
Tam olarak 3 rol, 3 çalışma alanı, 3 örnek işveren türü, 4 gereksinim, 3 günlük iş maddesi
ve 3 ilk 90 gün adımı üret. Yanıt tamamen Türkçe olsun.`;
  const student = {
    university,
    department,
    classLevel: clean(profile.classLevel, 30),
    branchTitle,
    branchReason: clean(branch.reason, 500)
  };
  const output = await callNvidia(env, [
    { role: "system", content: system },
    { role: "user", content: `Bu kariyer dalı için künye hazırla: ${JSON.stringify(student)}` }
  ], 900, 0.35);

  const parsed = parseJsonObject(output);
  const result = {
    title: clean(parsed?.title, 120) || branchTitle,
    overview: clean(parsed?.overview, 500),
    roles: stringList(parsed?.roles, 6),
    work_areas: stringList(parsed?.work_areas, 6),
    example_employers: stringList(parsed?.example_employers, 6),
    salary_range: clean(parsed?.salary_range, 120),
    salary_note: clean(parsed?.salary_note, 200),
    hiring_outlook: clean(parsed?.hiring_outlook, 40),
    demand_note: clean(parsed?.demand_note, 200),
    requirements: stringList(parsed?.requirements, 8),
    daily_work: stringList(parsed?.daily_work, 6),
    first_90_days: stringList(parsed?.first_90_days, 6)
  };
  if (!result.overview || result.roles.length < 2 || result.requirements.length < 2) {
    throw new UserFacingError("AI branş detayını eksik oluşturdu.");
  }
  return json(result);
}

async function actionPlan(body, env) {
  const targetCareer = clean(body?.targetCareer, 120);
  if (!targetCareer) {
    return json({ error: "Hedef kariyer bilgisi eksik." }, 400);
  }
  const profile = body?.profile || {};
  const branch = body?.branch || {};
  const completedTasks = stringList(body?.completedTasks, 20);

  const system = `Sen Migo uygulamasındaki Türkçe kariyer koçusun.
Öğrenciye hedef kariyeri için tam 4 haftalık, uygulanabilir bir eylem planı hazırla.
Bilgi uydurma; gerçekçi ve ölçülebilir görevler ver. İş veya başarı garantisi verme.

Yazım tonu: öğrenciye doğrudan "sen" diyerek, sıcak ama gösterişsiz bir dille yaz —
bir arkadaşının sana ne yapman gerektiğini anlattığı gibi. Kısa, somut cümleler kur.
"Katkı sağlayacak", "değerli bir fırsat sunacak", "potansiyelini keşfet" gibi şişirilmiş,
kalıp AI ifadelerinden kaçın. Her başlık ve açıklama okuyanın "bunu bugün yapabilirim"
demesini sağlamalı; soyut değil, iş tanımı kadar net olsun. objective ve outcome alanları
"neden önemli" sorusuna tek cümlede cevap versin.

Yalnızca geçerli JSON döndür. Şema:
{"title":"","objective":"","duration_days":30,"weeks":[
{"number":1,"title":"","focus":"","outcome":"","actions":[
{"title":"","description":"","duration_minutes":30,"deliverable":""}]}]}.
Tam olarak 4 hafta üret, her haftada tam olarak 4 eylem olsun. Her eylem süresi 15-90 dakika arasında olsun.
Yanıt tamamen Türkçe olsun.`;
  const student = {
    targetCareer,
    university: clean(profile.university, 160),
    department: clean(profile.department, 160),
    classLevel: clean(profile.classLevel, 30),
    branchReason: clean(branch.reason, 500),
    branchRequirements: stringList(branch.requirements, 8),
    completedTasks
  };
  const output = await callNvidia(env, [
    { role: "system", content: withAiCoachRules(system, body) },
    { role: "user", content: `Bu öğrenci için 30 günlük eylem planı hazırla: ${JSON.stringify(student)}` }
  ], 1700, 0.4);

  const parsed = parseJsonObject(output);
  const weeksRaw = Array.isArray(parsed?.weeks) ? parsed.weeks.slice(0, 4) : [];
  const weeks = weeksRaw.map((week, weekIndex) => {
    const actionsRaw = Array.isArray(week?.actions) ? week.actions.slice(0, 4) : [];
    return {
      number: weekIndex + 1,
      title: clean(week?.title, 120),
      focus: clean(week?.focus, 240),
      outcome: clean(week?.outcome, 220),
      actions: actionsRaw.map((action, actionIndex) => ({
        id: `w${weekIndex + 1}_a${actionIndex + 1}`,
        title: clean(action?.title, 140),
        description: clean(action?.description, 320),
        duration_minutes: clampInt(action?.duration_minutes, 15, 90, 30),
        deliverable: clean(action?.deliverable, 180)
      }))
    };
  });

  const result = {
    title: clean(parsed?.title, 160) || `${targetCareer} için 30 günlük rota`,
    objective: clean(parsed?.objective, 420),
    duration_days: clampInt(parsed?.duration_days, 7, 60, 30),
    weeks
  };
  if (
    !result.objective ||
    result.weeks.length !== 4 ||
    result.weeks.some((week) => week.actions.length < 4)
  ) {
    throw new UserFacingError("AI 30 günlük planı eksik oluşturdu.");
  }
  return json(result);
}

async function dailyInsight(body, env) {
  const targetCareer = clean(body?.targetCareer, 120);
  if (!targetCareer) {
    return json({ error: "Hedef kariyer bilgisi eksik." }, 400);
  }
  const profile = body?.profile || {};
  const dateKey = clean(body?.dateKey, 20);

  const system = `Sen Migo uygulamasındaki Türkçe günlük kariyer koçusun.
Öğrenciye bugün için kısa, motive edici ve uygulanabilir bir öneri ver. Kesin başarı garantisi verme.
Yalnızca geçerli JSON döndür. Şema: {"title":"","message":"","action_label":"","coach_prompt":""}.
title en fazla 6 kelime, message en fazla 2 cümle, action_label somut ve kısa bir eylem cümlesi,
coach_prompt bu eylemi küçük adımlara bölmesini isteyen tek cümlelik bir koç istemi olsun.
Yanıt tamamen Türkçe olsun.`;
  const student = {
    targetCareer,
    university: clean(profile.university, 160),
    department: clean(profile.department, 160),
    classLevel: clean(profile.classLevel, 30),
    nextTask: clean(body?.nextTask, 160),
    completionPercent: clampInt(body?.completionPercent, 0, 100, 0)
  };
  const output = await callNvidia(env, [
    { role: "system", content: system },
    { role: "user", content: `Bugün için kısa bir kariyer içgörüsü hazırla: ${JSON.stringify(student)}` }
  ], 350, 0.5);

  const parsed = parseJsonObject(output);
  const result = {
    date_key: dateKey,
    title: clean(parsed?.title, 80),
    message: clean(parsed?.message, 320),
    action_label: clean(parsed?.action_label, 120),
    coach_prompt: clean(parsed?.coach_prompt, 280)
  };
  if (!result.title || !result.message || !result.action_label) {
    throw new UserFacingError("AI bugünün içgörüsünü eksik oluşturdu.");
  }
  return json(result);
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
    if (!upstream.ok) throw new UserFacingError(`YÖK Atlas veri hatası: ${upstream.status}`);
    const contentLength = Number(upstream.headers.get("content-length") || 0);
    if (contentLength > MAX_CATALOG_BYTES) throw new UserFacingError("YÖK Atlas yanıtı beklenenden büyük.");

    response = new Response(upstream.body, {
      status: upstream.status,
      headers: upstream.headers
    });
    response.headers.set("cache-control", `public, max-age=${CATALOG_CACHE_SECONDS}`);
    ctx.waitUntil(cache.put(url, response.clone()));
  }

  const text = await readTextBody(response, MAX_CATALOG_BYTES);
  const parsed = JSON.parse(text);
  if (!Array.isArray(parsed)) throw new UserFacingError("YÖK Atlas yanıtı okunamadı.");
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
    { role: "system", content: withAiCoachRules(system, body) },
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
    throw new UserFacingError("AI CV analizini eksik oluşturdu.");
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
      if (!response.ok) throw new UserFacingError(`AI sağlayıcı hatası: ${response.status}`);
      const payload = await response.json();
      const content = payload?.choices?.[0]?.message?.content?.trim();
      if (!content) throw new UserFacingError("AI boş yanıt verdi.");
      return content;
    } catch (error) {
      lastError = error;
    } finally {
      clearTimeout(timeout);
    }
  }
  throw lastError || new UserFacingError("AI yanıt vermedi.");
}

function parseJsonObject(value) {
  const cleaned = String(value).replace(/^```json/i, "").replace(/^```/, "").replace(/```$/, "").trim();
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start < 0 || end <= start) throw new UserFacingError("AI yanıtı okunamadı.");
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
  if (!response.body) throw new UserFacingError("YÖK Atlas yanıt gövdesi eksik.");
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
        throw new UserFacingError("YÖK Atlas yanıtı beklenenden büyük.");
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

function clampInt(value, min, max, fallback) {
  const number = Math.round(Number(value));
  return Number.isFinite(number) ? Math.max(min, Math.min(max, number)) : fallback;
}

// The Android client bundles a bigger, product-owned coaching ruleset (see
// AiCoachRules.kt / assets/MIGO_AI_KARIYER_KOCU_KURALLARI.md) and sends it as
// `aiRules` on every AI request so prompt-quality tweaks ship without a
// Worker redeploy. Only endpoints that actually produce sprint-style
// coaching content (the weekly action plan, freeform coach Q&A) fold it in;
// daily-insight is a one-sentence nudge the sprint rules don't fit, and the
// analysis/detail/cv endpoints have their own narrower schemas.
const MAX_AI_RULES_CHARS = 12_000;

function withAiCoachRules(system, body) {
  const rules = clean(body?.aiRules, MAX_AI_RULES_CHARS);
  if (!rules) return system;
  return `${system}\n\nAşağıdaki ek koçluk kuralları bu görevle çelişmediği sürece bağlayıcıdır:\n${rules}`;
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

const GENERIC_ERROR_MESSAGE = "Beklenmeyen bir hata oluştu.";

// Marks an Error whose message was deliberately written to be shown to the end user (Turkish, no
// internal detail). Anything else -- D1/driver errors, unexpected exceptions, raw fetch failures --
// is a bug or an infra hiccup, not something the client should see verbatim, so safeError() below
// collapses it to a generic message. The real message still reaches the logs via logError().
class UserFacingError extends Error {
  constructor(message) {
    super(message);
    this.name = "UserFacingError";
  }
}

function redactSecrets(message) {
  return String(message ?? "").replace(/nvapi-[A-Za-z0-9_-]+/g, "[secret]").slice(0, 240);
}

function safeError(error) {
  if (error instanceof ClientRequestError || error instanceof UserFacingError) {
    return redactSecrets(error.message);
  }
  return GENERIC_ERROR_MESSAGE;
}

function logError(operation, error) {
  const rawMessage = error instanceof Error ? error.message : String(error);
  console.error(JSON.stringify({
    level: "error",
    operation,
    message: redactSecrets(rawMessage)
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
