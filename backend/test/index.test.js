import assert from "node:assert/strict";
import test from "node:test";
import worker from "../src/index.js";

const installationId = "test_installation_1234567890";
const syncSecret = "abcdefghijklmnopqrstuvwxyzABCDEFGH1234567890_-";
const allowLimiter = { limit: async () => ({ success: true }) };
const denyLimiter = { limit: async () => ({ success: false }) };

if (!globalThis.crypto.subtle.timingSafeEqual) {
  globalThis.crypto.subtle.timingSafeEqual = (left, right) => {
    const a = new Uint8Array(left.buffer || left, left.byteOffset || 0, left.byteLength);
    const b = new Uint8Array(right.buffer || right, right.byteOffset || 0, right.byteLength);
    if (a.length !== b.length) return false;
    let difference = 0;
    for (let index = 0; index < a.length; index += 1) difference |= a[index] ^ b[index];
    return difference === 0;
  };
}

function createMemoryDb() {
  const rows = new Map();
  return {
    prepare(sql) {
      let values = [];
      return {
        bind(...bound) {
          values = bound;
          return this;
        },
        async first() {
          if (!sql.startsWith("SELECT")) throw new Error(`Unexpected first query: ${sql}`);
          return rows.get(values[0]) || null;
        },
        async run() {
          if (sql.startsWith("INSERT")) {
            rows.set(values[0], {
              auth_hash: values[1],
              session_json: values[2],
              session_version: 1,
              updated_at: values[4]
            });
          } else if (sql.startsWith("UPDATE")) {
            const current = rows.get(values[2]);
            rows.set(values[2], {
              ...current,
              session_json: values[0],
              session_version: current.session_version + 1,
              updated_at: values[1]
            });
          } else if (sql.startsWith("DELETE")) {
            rows.delete(values[0]);
          } else {
            throw new Error(`Unexpected run query: ${sql}`);
          }
          return { success: true, results: [], meta: {} };
        }
      };
    }
  };
}

function env(overrides = {}) {
  return {
    NVIDIA_API_KEY: "test-key",
    CLIENT_AI_RATE_LIMITER: allowLimiter,
    NETWORK_AI_RATE_LIMITER: allowLimiter,
    SYNC_RATE_LIMITER: allowLimiter,
    DB: createMemoryDb(),
    ...overrides
  };
}

function syncRequest(method, body, secret = syncSecret) {
  return new Request("https://migo.test/v1/sync/session", {
    method,
    headers: {
      "content-type": "application/json",
      "x-migo-installation": installationId,
      authorization: `Bearer ${secret}`
    },
    body
  });
}

function post(path, body, headers = {}) {
  return new Request(`https://migo.test${path}`, {
    method: "POST",
    headers: { "x-migo-installation": installationId, ...headers },
    body
  });
}

test("health exposes the current API contract", async () => {
  const response = await worker.fetch(new Request("https://migo.test/health"), {}, {});
  const payload = await response.json();

  assert.equal(response.status, 200);
  assert.equal(response.headers.get("x-migo-api-version"), "3");
  assert.equal(payload.service, "migo-cloudflare");
  assert.equal(payload.apiVersion, 3);
  assert.ok(payload.features.includes("cv_analysis"));
  assert.ok(payload.features.includes("cloud_sync"));
});

test("AI endpoints require an installation identity", async () => {
  const request = new Request("https://migo.test/v1/coach", { method: "POST", body: "{}" });
  const response = await worker.fetch(request, env(), {});
  assert.equal(response.status, 401);
});

test("invalid and oversized bodies are rejected before provider calls", async () => {
  const invalid = await worker.fetch(post("/v1/coach", "{"), env(), {});
  const oversized = await worker.fetch(post("/v1/coach", "x".repeat(50_001)), env(), {});

  assert.equal(invalid.status, 400);
  assert.equal(oversized.status, 413);
});

test("unknown endpoints and empty coach questions use client errors", async () => {
  const unknown = await worker.fetch(post("/v1/missing", "{}"), env(), {});
  const emptyQuestion = await worker.fetch(post("/v1/coach", "{}"), env(), {});

  assert.equal(unknown.status, 404);
  assert.equal(emptyQuestion.status, 400);
});

test("rate limits fail closed with retry guidance", async () => {
  const response = await worker.fetch(
    post("/v1/coach", JSON.stringify({ question: "Test" })),
    env({ CLIENT_AI_RATE_LIMITER: denyLimiter }),
    {}
  );

  assert.equal(response.status, 429);
  assert.equal(response.headers.get("retry-after"), "60");
});

test("cloud vault creates, reads, updates and deletes a session", async () => {
  const database = createMemoryDb();
  const environment = env({ DB: database });
  const firstSession = { targetCareer: "Android Geliştirici", coachMessages: [] };
  const updatedSession = {
    targetCareer: "Android Geliştirici",
    coachMessages: [{ text: "Merhaba", isFromUser: false }]
  };

  const created = await worker.fetch(
    syncRequest("PUT", JSON.stringify({ session: firstSession })), environment, {}
  );
  const read = await worker.fetch(syncRequest("GET"), environment, {});
  const updated = await worker.fetch(
    syncRequest("PUT", JSON.stringify({ session: updatedSession })), environment, {}
  );
  const readUpdated = await worker.fetch(syncRequest("GET"), environment, {});
  const deleted = await worker.fetch(syncRequest("DELETE"), environment, {});
  const missing = await worker.fetch(syncRequest("GET"), environment, {});

  assert.equal(created.status, 200);
  assert.deepEqual((await read.json()).session, firstSession);
  assert.equal(updated.status, 200);
  assert.deepEqual((await readUpdated.json()).session, updatedSession);
  assert.equal(deleted.status, 200);
  assert.equal(missing.status, 404);
});

test("cloud vault rejects a different secret and enforces its own quota", async () => {
  const database = createMemoryDb();
  const environment = env({ DB: database });
  await worker.fetch(
    syncRequest("PUT", JSON.stringify({ session: { targetCareer: "Test" } })), environment, {}
  );

  const forbidden = await worker.fetch(
    syncRequest("GET", undefined, "ZYXWVUTSRQPONMLKJIHGFEDCBA9876543210_-abcdef"), environment, {}
  );
  const limited = await worker.fetch(
    syncRequest("GET"), env({ DB: database, SYNC_RATE_LIMITER: denyLimiter }), {}
  );

  assert.equal(forbidden.status, 403);
  assert.equal(limited.status, 429);
  assert.equal(limited.headers.get("retry-after"), "60");
});
