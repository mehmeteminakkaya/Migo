import { FALLBACK_MODEL, MAX_AI_RULES_CHARS, MODEL_TIMEOUT_MS, NVIDIA_ENDPOINT, PRIMARY_MODEL } from "./config.js";
import { UserFacingError, logEvent } from "./http.js";
import { clean, parseJsonObject } from "./text.js";

export async function callNvidia(env, messages, maxTokens, temperature) {
  let lastError;
  const models = [env.NVIDIA_PRIMARY_MODEL || PRIMARY_MODEL, env.NVIDIA_FALLBACK_MODEL || FALLBACK_MODEL];
  for (const [attempt, model] of models.entries()) {
    const isFallback = attempt > 0;
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
          stream: false,
          // "Düşünen" Nemotron modelleri token bütçesini akıl yürütmeye harcayıp cevabı boş bırakabilir.
          ...(/nemotron/i.test(model) ? { chat_template_kwargs: { enable_thinking: false } } : {})
        })
      });
      if (!response.ok) throw new UserFacingError(`AI sağlayıcı hatası: ${response.status}`);
      const payload = await response.json();
      const content = payload?.choices?.[0]?.message?.content?.trim();
      if (!content) throw new UserFacingError("AI boş yanıt verdi.");
      logEvent("ai_model_call", { model, fallback: isFallback, outcome: "ok", ms: Date.now() - startedAt });
      return content;
    } catch (error) {
      lastError = error;
      logEvent("ai_model_call", {
        model,
        fallback: isFallback,
        outcome: error?.name === "AbortError" ? "timeout" : "error",
        ms: Date.now() - startedAt
      });
    } finally {
      clearTimeout(timeout);
    }
  }
  throw lastError || new UserFacingError("AI yanıt vermedi.");
}

// Modelin kesilmiş/bozuk JSON üretmesi olağandır; kullanıcıya hata göstermeden önce bir kez daha dene.
export async function callNvidiaJson(env, messages, maxTokens, temperature) {
  let lastError;
  for (let attempt = 0; attempt < 2; attempt += 1) {
    const output = await callNvidia(env, messages, maxTokens, temperature);
    try {
      return parseJsonObject(output);
    } catch (error) {
      lastError = error;
      logEvent("ai_json_unreadable", { attempt: attempt + 1 });
    }
  }
  throw lastError;
}

export function withAiCoachRules(system, body) {
  const rules = clean(body?.aiRules, MAX_AI_RULES_CHARS);
  if (!rules) return system;
  return `${system}\n\nAşağıdaki ek koçluk kuralları bu görevle çelişmediği sürece bağlayıcıdır:\n${rules}`;
}
