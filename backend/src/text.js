import { UserFacingError } from "./http.js";

export function base64UrlEncode(bytes) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export function parseJsonObject(value) {
  const cleaned = String(value).replace(/^```json/i, "").replace(/^```/, "").replace(/```$/, "").trim();
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start < 0 || end <= start) throw new UserFacingError("AI yanıtı okunamadı.");
  try {
    return JSON.parse(cleaned.slice(start, end + 1));
  } catch {
    // Çoğunlukla token sınırında kesilmiş/bozuk JSON; ham SyntaxError kullanıcıya "Beklenmeyen hata" olarak gitmesin.
    throw new UserFacingError("AI yanıtı okunamadı.");
  }
}

export function clean(value, maxLength) {
  return typeof value === "string" ? value.trim().slice(0, maxLength) : "";
}

export function stringList(value, maxItems) {
  return Array.isArray(value) ? value.slice(0, maxItems).map((item) => clean(item, 100)).filter(Boolean) : [];
}

export function clampInt(value, min, max, fallback) {
  const number = Math.round(Number(value));
  return Number.isFinite(number) ? Math.max(min, Math.min(max, number)) : fallback;
}

export function normalizeSearch(value) {
  return String(value ?? "")
    .toLocaleLowerCase("tr-TR")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/ı/g, "i")
    .replace(/[^a-z0-9çğıöşü ]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}
