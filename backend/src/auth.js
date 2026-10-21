import { INSTALLATION_TOKEN_HEADER, INSTALLATION_TOKEN_TTL_SECONDS } from "./config.js";
import { json, logError } from "./http.js";
import { base64UrlEncode, clean } from "./text.js";

export async function hashSyncSecret(value) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

export function safeHashEquals(left, right) {
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
export async function enforceAiRateLimits(request, env, path) {
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
export async function resolveInstallationSubject(request, env) {
  const presented = clean(request.headers.get(INSTALLATION_TOKEN_HEADER), 200);
  if (presented) {
    const verified = await verifyInstallationToken(presented, env);
    if (verified) return { subject: verified.subject, token: presented };
  }
  return issueInstallationToken(env);
}

export async function issueInstallationToken(env) {
  const subject = crypto.randomUUID();
  const issuedAt = Math.floor(Date.now() / 1000);
  const signature = await signInstallationToken(subject, issuedAt, env);
  return { subject, token: `${subject}.${issuedAt}.${signature}` };
}

export async function verifyInstallationToken(token, env) {
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

export async function signInstallationToken(subject, issuedAt, env) {
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

export function withInstallationToken(response, token) {
  if (!token) return response;
  const headers = new Headers(response.headers);
  headers.set(INSTALLATION_TOKEN_HEADER, token);
  return new Response(response.body, { status: response.status, headers });
}
