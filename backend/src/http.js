import { GENERIC_ERROR_MESSAGE } from "./config.js";

export class ClientRequestError extends Error {
  constructor(status, message) {
    super(message);
    this.name = "ClientRequestError";
    this.status = status;
  }
}

export async function readJsonBody(request, maxBytes) {
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

export async function readTextBody(response, maxBytes) {
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

// Marks an Error whose message was deliberately written to be shown to the end user (Turkish, no
// internal detail). Anything else -- D1/driver errors, unexpected exceptions, raw fetch failures --
// is a bug or an infra hiccup, not something the client should see verbatim, so safeError() below
// collapses it to a generic message. The real message still reaches the logs via logError().
export class UserFacingError extends Error {
  constructor(message) {
    super(message);
    this.name = "UserFacingError";
  }
}

export function redactSecrets(message) {
  return String(message ?? "").replace(/nvapi-[A-Za-z0-9_-]+/g, "[secret]").slice(0, 240);
}

export function safeError(error) {
  if (error instanceof ClientRequestError || error instanceof UserFacingError) {
    return redactSecrets(error.message);
  }
  return GENERIC_ERROR_MESSAGE;
}

export function logError(operation, error) {
  const rawMessage = error instanceof Error ? error.message : String(error);
  console.error(JSON.stringify({
    level: "error",
    operation,
    message: redactSecrets(rawMessage)
  }));
}

export function json(value, status = 200, cacheControl = "no-store", extraHeaders = {}) {
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
