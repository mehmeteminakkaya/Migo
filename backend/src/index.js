import { legalDocumentResponse, legalMetadata } from "./legal-pages.js";
import { enforceAiRateLimits, withInstallationToken } from "./auth.js";
import { AI_PATHS, INSTALLATION_HEADER, INSTALLATION_ID_PATTERN, SYNC_PATH } from "./config.js";
import { educationSearch } from "./education.js";
import { actionPlan, careerAnalysis, careerDetail, coach, cvAnalysis, dailyInsight } from "./endpoints.js";
import { ClientRequestError, json, logError, logEvent, readJsonBody, safeError } from "./http.js";
import { deleteExpiredVaults, syncSession } from "./sync.js";
import { clean } from "./text.js";

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

    const startedAt = Date.now();
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
      logEvent("ai_request", { path, status: response.status, ms: Date.now() - startedAt });
      return withInstallationToken(response, token);
    } catch (error) {
      logError(url.pathname, error);
      logEvent("ai_request", { path: url.pathname, status: 502, ms: Date.now() - startedAt });
      return withInstallationToken(json({ error: safeError(error) }, 502), token);
    }
  },
  async scheduled(_controller, env, ctx) {
    ctx.waitUntil(deleteExpiredVaults(env));
  }
};
