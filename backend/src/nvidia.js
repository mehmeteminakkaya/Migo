import { FALLBACK_MODEL, MAX_AI_RULES_CHARS, MODEL_TIMEOUT_MS, NVIDIA_ENDPOINT, PRIMARY_MODEL } from "./config.js";
import { UserFacingError, logEvent } from "./http.js";
import { clean } from "./text.js";

export async function callNvidia(env, messages, maxTokens, temperature) {
  let lastError;
  for (const model of [PRIMARY_MODEL, FALLBACK_MODEL]) {
    const startedAt = Date.now();
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
      logEvent("ai_model_call", { model, fallback: model === FALLBACK_MODEL, outcome: "ok", ms: Date.now() - startedAt });
      return content;
    } catch (error) {
      lastError = error;
      logEvent("ai_model_call", {
        model,
        fallback: model === FALLBACK_MODEL,
        outcome: error?.name === "AbortError" ? "timeout" : "error",
        ms: Date.now() - startedAt
      });
    } finally {
      clearTimeout(timeout);
    }
  }
  throw lastError || new UserFacingError("AI yanıt vermedi.");
}

export function withAiCoachRules(system, body) {
  const rules = clean(body?.aiRules, MAX_AI_RULES_CHARS);
  if (!rules) return system;
  return `${system}\n\nAşağıdaki ek koçluk kuralları bu görevle çelişmediği sürece bağlayıcıdır:\n${rules}`;
}
