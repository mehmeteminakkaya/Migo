export const NVIDIA_ENDPOINT = "https://integrate.api.nvidia.com/v1/chat/completions";

// NVIDIA modelleri kısa sürede yayından kalkar (410 Gone): mistral-small-4 (2026-07-27) ve
// llama-3.3-70b-instruct (2026-08-26) kalktı. Yeniden dağıtım yapmadan değiştirmek için Worker
// değişkenleri NVIDIA_PRIMARY_MODEL / NVIDIA_FALLBACK_MODEL kullanılır (bkz. callNvidia).
// Yedek varsayılan, hesapta açık ve hızlı olan başka bir model bulunana kadar aynı model (yeniden deneme).
export const PRIMARY_MODEL = "nvidia/nemotron-3.5-lightning-30b-a3b";

export const FALLBACK_MODEL = "nvidia/nemotron-3.5-lightning-30b-a3b";

export const MODEL_TIMEOUT_MS = 35_000;

export const YOK_ATLAS_API = "https://yokatlas.yok.gov.tr/api/tercih-kilavuz";

export const CATALOG_CACHE_SECONDS = 7_200;

export const MAX_CATALOG_BYTES = 1_000_000;

export const MAX_SYNC_BYTES = 250_000;

export const VAULT_RETENTION_DAYS = 180;

export const INSTALLATION_HEADER = "x-migo-installation";

export const INSTALLATION_ID_PATTERN = /^[A-Za-z0-9_-]{16,128}$/;

export const INSTALLATION_TOKEN_HEADER = "x-migo-installation-token";

export const INSTALLATION_TOKEN_TTL_SECONDS = 400 * 86_400; // ~13 months

export const SYNC_SECRET_PATTERN = /^[A-Za-z0-9_-]{40,128}$/;

export const SYNC_PATH = "/v1/sync/session";

export const AI_PATHS = new Set([
  "/v1/career-analysis",
  "/v1/career-detail",
  "/v1/action-plan",
  "/v1/daily-insight",
  "/v1/coach",
  "/v1/cv-analysis"
]);

// The Android client bundles a bigger, product-owned coaching ruleset (see
// AiCoachRules.kt / assets/MIGO_AI_KARIYER_KOCU_KURALLARI.md) and sends it as
// `aiRules` on every AI request so prompt-quality tweaks ship without a
// Worker redeploy. Only endpoints that actually produce sprint-style
// coaching content (the weekly action plan, freeform coach Q&A) fold it in;
// daily-insight is a one-sentence nudge the sprint rules don't fit, and the
// analysis/detail/cv endpoints have their own narrower schemas.
// Must stay >= the client's own cap (AiCoachRules.MAX_RULES_CHARACTERS) or the ruleset's tail is cut off.
export const MAX_AI_RULES_CHARS = 24_000;

export const GENERIC_ERROR_MESSAGE = "Beklenmeyen bir hata oluştu.";
