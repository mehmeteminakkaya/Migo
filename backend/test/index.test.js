import assert from "node:assert/strict";
import test from "node:test";
import worker from "../src/index.js";
import { safeHashEquals } from "../src/auth.js";
import { normalizeCoachHistory } from "../src/endpoints.js";

const installationId = "test_installation_1234567890";
const syncSecret = "abcdefghijklmnopqrstuvwxyzABCDEFGH1234567890_-";
const allowLimiter = { limit: async () => ({ success: true }) };
const denyLimiter = { limit: async () => ({ success: false }) };

function spyLimiter() {
  const calls = [];
  return {
    calls,
    limit: async ({ key }) => {
      calls.push(key);
      return { success: true };
    }
  };
}

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
          let changes = 0;
          if (sql.startsWith("INSERT")) {
            rows.set(values[0], {
              auth_hash: values[1],
              session_json: values[2],
              session_version: 1,
              updated_at: values[4],
              expires_at: values[5]
            });
            changes = 1;
          } else if (sql.startsWith("UPDATE")) {
            const current = rows.get(values[3]);
            rows.set(values[3], {
              ...current,
              session_json: values[0],
              session_version: current.session_version + 1,
              updated_at: values[1],
              expires_at: values[2]
            });
            changes = 1;
          } else if (sql.includes("WHERE expires_at")) {
            for (const [userId, row] of rows) {
              if (row.expires_at && row.expires_at <= values[0]) {
                rows.delete(userId);
                changes += 1;
              }
            }
          } else if (sql.startsWith("DELETE")) {
            changes = rows.delete(values[0]) ? 1 : 0;
          } else {
            throw new Error(`Unexpected run query: ${sql}`);
          }
          return { success: true, results: [], meta: { changes } };
        }
      };
    },
    expireAll() {
      for (const row of rows.values()) row.expires_at = "2000-01-01T00:00:00.000Z";
    },
    size() {
      return rows.size;
    }
  };
}

function env(overrides = {}) {
  return {
    NVIDIA_API_KEY: "test-key",
    INSTALLATION_TOKEN_SECRET: "test-installation-token-secret",
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
  assert.equal(response.headers.get("x-migo-api-version"), "4");
  assert.equal(payload.service, "migo-api");
  assert.equal(payload.apiVersion, 4);
  assert.ok(payload.features.includes("cv_analysis"));
  assert.ok(payload.features.includes("cloud_sync"));
  assert.ok(payload.features.includes("legal_pages"));
  assert.ok(payload.features.includes("automatic_data_retention"));
});

test("public legal pages expose policy metadata and hardened HTML", async () => {
  const privacy = await worker.fetch(new Request("https://migo.test/privacy"), {}, {});
  const terms = await worker.fetch(new Request("https://migo.test/terms"), {}, {});
  const support = await worker.fetch(new Request("https://migo.test/support"), {}, {});
  const metadata = await worker.fetch(new Request("https://migo.test/v1/legal"), {}, {});
  const policy = await metadata.json();

  assert.equal(privacy.status, 200);
  assert.match(privacy.headers.get("content-type"), /^text\/html/);
  assert.match(privacy.headers.get("content-security-policy"), /default-src 'none'/);
  assert.equal(privacy.headers.get("x-frame-options"), "DENY");
  assert.match(await privacy.text(), /180 gün sonra otomatik silinir/);
  assert.equal(terms.status, 200);
  assert.equal(support.status, 200);
  assert.equal(policy.policyVersion, 1);
  assert.equal(policy.privacyUrl, "https://migo.test/privacy");
  assert.equal(policy.retentionDays, 180);
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

test("a signed installation token pins the AI rate-limit identity even if the client-chosen id is rotated", async () => {
  const clientLimiter = spyLimiter();
  const environment = env({ CLIENT_AI_RATE_LIMITER: clientLimiter });

  const first = await worker.fetch(post("/v1/coach", "{}"), environment, {});
  assert.equal(first.status, 400); // empty question short-circuits before any provider call
  const issuedToken = first.headers.get("x-migo-installation-token");
  assert.ok(issuedToken, "worker should mint and return a signed installation token");
  assert.equal(clientLimiter.calls.length, 1);
  const firstSubject = clientLimiter.calls[0].split(":")[0];

  const second = await worker.fetch(
    post("/v1/coach", "{}", {
      "x-migo-installation": "rotated_installation_9876543210",
      "x-migo-installation-token": issuedToken
    }),
    environment,
    {}
  );
  assert.equal(second.status, 400);
  assert.equal(clientLimiter.calls.length, 2);
  const secondSubject = clientLimiter.calls[1].split(":")[0];

  assert.equal(
    secondSubject,
    firstSubject,
    "presenting the previously-issued token must reuse the same rate-limit subject even though x-migo-installation changed"
  );
  assert.equal(second.headers.get("x-migo-installation-token"), issuedToken);
});

test("omitting the token every request still yields a fresh rate-limit subject (network-IP limiter is the backstop)", async () => {
  const clientLimiter = spyLimiter();
  const environment = env({ CLIENT_AI_RATE_LIMITER: clientLimiter });

  await worker.fetch(post("/v1/coach", "{}"), environment, {});
  await worker.fetch(
    post("/v1/coach", "{}", { "x-migo-installation": "another_random_id_0000000000" }),
    environment,
    {}
  );

  const firstSubject = clientLimiter.calls[0].split(":")[0];
  const secondSubject = clientLimiter.calls[1].split(":")[0];
  assert.notEqual(
    firstSubject,
    secondSubject,
    "without a signed token the server cannot tell this is the same install, by design -- the per-IP limiter is what bounds this"
  );
});

test("a forged or tampered installation token is rejected and a fresh one is minted", async () => {
  const first = await worker.fetch(post("/v1/coach", "{}"), env(), {});
  const issuedToken = first.headers.get("x-migo-installation-token");
  const [subject, issuedAt] = issuedToken.split(".");
  const forged = `${subject}.${issuedAt}.not-a-real-signature`;

  const response = await worker.fetch(
    post("/v1/coach", "{}", { "x-migo-installation-token": forged }),
    env(),
    {}
  );

  assert.equal(response.status, 400);
  const refreshedToken = response.headers.get("x-migo-installation-token");
  assert.ok(refreshedToken);
  assert.notEqual(refreshedToken, forged, "a tampered token must not be honored");
});

test("AI rate limiting fails closed when the installation-token signing secret is missing", async () => {
  const response = await worker.fetch(
    post("/v1/coach", "{}"),
    env({ INSTALLATION_TOKEN_SECRET: undefined }),
    {}
  );
  assert.equal(response.status, 503);
});

test("unexpected backend errors (e.g. a raw DB failure) return a generic message instead of the driver string", async () => {
  const brokenDb = {
    prepare() {
      return {
        bind() {
          return this;
        },
        async first() {
          throw new Error("D1_ERROR: no such table: user_vaults: SQLITE_ERROR");
        },
        async run() {
          throw new Error("D1_ERROR: no such table: user_vaults: SQLITE_ERROR");
        }
      };
    }
  };

  const response = await worker.fetch(syncRequest("GET"), env({ DB: brokenDb }), {});
  const payload = await response.json();

  assert.equal(response.status, 500);
  assert.equal(payload.error, "Beklenmeyen bir hata oluştu.");
  assert.doesNotMatch(payload.error, /SQLITE|D1_ERROR|no such table/);
});

test("known, expected error cases still return their specific Turkish message", async () => {
  const oversized = await worker.fetch(post("/v1/coach", "x".repeat(50_001)), env(), {});
  const payload = await oversized.json();

  assert.equal(oversized.status, 413);
  assert.equal(payload.error, "İstek çok büyük.");
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

test("career-detail, action-plan and daily-insight reject requests missing required fields", async () => {
  const missingBranch = await worker.fetch(
    post("/v1/career-detail", JSON.stringify({ profile: { university: "ITU", department: "CS" } })),
    env(),
    {}
  );
  const missingTargetForPlan = await worker.fetch(post("/v1/action-plan", "{}"), env(), {});
  const missingTargetForInsight = await worker.fetch(post("/v1/daily-insight", "{}"), env(), {});

  assert.equal(missingBranch.status, 400);
  assert.equal(missingTargetForPlan.status, 400);
  assert.equal(missingTargetForInsight.status, 400);
});

function withMockedNvidiaResponse(payload, run) {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response(
    JSON.stringify({ choices: [{ message: { content: JSON.stringify(payload) } }] }),
    { status: 200, headers: { "content-type": "application/json" } }
  );
  return run().finally(() => {
    globalThis.fetch = originalFetch;
  });
}

test("career-detail normalizes a well-formed AI response into the shape the app expects", async () => {
  await withMockedNvidiaResponse(
    {
      title: "Mobil Uygulama Geliştirme",
      overview: "Android ve iOS ürünlerinin geliştirme sürecine odaklanır.",
      roles: ["Android Developer", "iOS Developer", "Mobile Engineer"],
      requirements: ["Kotlin", "Mobil mimari", "Git", "Portföy"]
    },
    async () => {
      const response = await worker.fetch(
        post("/v1/career-detail", JSON.stringify({
          profile: { university: "ITU", department: "Bilgisayar Mühendisliği" },
          branch: { title: "Mobil Uygulama Geliştirme", reason: "İlgi alanına uygun." }
        })),
        env(),
        {}
      );
      const payload = await response.json();

      assert.equal(response.status, 200);
      assert.equal(payload.title, "Mobil Uygulama Geliştirme");
      assert.equal(payload.roles.length, 3);
      assert.equal(payload.requirements.length, 4);
    }
  );
});

test("career-detail rejects an incomplete AI response instead of forwarding it", async () => {
  await withMockedNvidiaResponse(
    { title: "X", overview: "", roles: [], requirements: [] },
    async () => {
      const response = await worker.fetch(
        post("/v1/career-detail", JSON.stringify({
          profile: { university: "ITU", department: "Bilgisayar Mühendisliği" },
          branch: { title: "Mobil Uygulama Geliştirme", reason: "" }
        })),
        env(),
        {}
      );
      assert.equal(response.status, 502);
    }
  );
});

test("action-plan normalizes exactly 4 weeks of 4 actions each and assigns stable ids", async () => {
  const week = {
    title: "Hafta",
    focus: "Odak",
    outcome: "Sonuç",
    actions: Array.from({ length: 4 }, (_, index) => ({
      title: `Görev ${index + 1}`,
      description: "Açıklama",
      duration_minutes: 45,
      deliverable: "Kanıt"
    }))
  };
  await withMockedNvidiaResponse(
    {
      title: "30 Günlük Rota",
      objective: "Hedefe ulaş.",
      weeks: [week, week, week, week]
    },
    async () => {
      const response = await worker.fetch(
        post("/v1/action-plan", JSON.stringify({
          targetCareer: "Android Geliştirici",
          profile: { university: "ITU", department: "Bilgisayar Mühendisliği" }
        })),
        env(),
        {}
      );
      const payload = await response.json();

      assert.equal(response.status, 200);
      assert.equal(payload.weeks.length, 4);
      assert.ok(payload.weeks.every((w) => w.actions.length === 4));
      assert.equal(payload.weeks[0].actions[0].id, "w1_a1");
      assert.equal(payload.weeks[1].actions[2].id, "w2_a3");
    }
  );
});

test("action-plan rejects an AI response with fewer than 4 weeks", async () => {
  await withMockedNvidiaResponse(
    { title: "Plan", objective: "Hedef", weeks: [{ actions: [] }] },
    async () => {
      const response = await worker.fetch(
        post("/v1/action-plan", JSON.stringify({ targetCareer: "Android Geliştirici" })),
        env(),
        {}
      );
      assert.equal(response.status, 502);
    }
  );
});

function withCapturedNvidiaRequest(payload, run) {
  const originalFetch = globalThis.fetch;
  const requests = [];
  globalThis.fetch = async (url, init) => {
    requests.push(JSON.parse(init.body));
    return new Response(
      JSON.stringify({ choices: [{ message: { content: JSON.stringify(payload) } }] }),
      { status: 200, headers: { "content-type": "application/json" } }
    );
  };
  return run(requests).finally(() => {
    globalThis.fetch = originalFetch;
  });
}

test("action-plan folds the client-supplied aiRules into the system prompt sent upstream", async () => {
  const week = {
    title: "Hafta", focus: "Odak", outcome: "Sonuç",
    actions: Array.from({ length: 4 }, (_, i) => ({ title: `Görev ${i + 1}`, duration_minutes: 30 }))
  };
  await withCapturedNvidiaRequest(
    { title: "Plan", objective: "Hedef", weeks: [week, week, week, week] },
    async (requests) => {
      const response = await worker.fetch(
        post("/v1/action-plan", JSON.stringify({
          targetCareer: "Android Geliştirici",
          aiRules: "GitHub hesabı açmak tek başına haftalık çıktı olamaz."
        })),
        env(),
        {}
      );
      assert.equal(response.status, 200);
      const systemMessage = requests[0].messages.find((m) => m.role === "system").content;
      assert.match(systemMessage, /GitHub hesabı açmak tek başına haftalık çıktı olamaz\./);
    }
  );
});

test("daily-insight ignores aiRules — the sprint ruleset doesn't fit a one-line nudge", async () => {
  await withCapturedNvidiaRequest(
    { title: "T", message: "M", action_label: "A" },
    async (requests) => {
      await worker.fetch(
        post("/v1/daily-insight", JSON.stringify({
          targetCareer: "Android Geliştirici",
          aiRules: "Bu metin günlük içgörüde görünmemeli."
        })),
        env(),
        {}
      );
      const systemMessage = requests[0].messages.find((m) => m.role === "system").content;
      assert.doesNotMatch(systemMessage, /Bu metin günlük içgörüde görünmemeli\./);
    }
  );
});

test("daily-insight echoes the requested date key and normalizes the AI response", async () => {
  await withMockedNvidiaResponse(
    {
      title: "Kanıt üretme günü",
      message: "Bugün küçük bir adım at.",
      action_label: "Ana ekranın demosunu kaydet",
      coach_prompt: "Bunu üç adıma böl."
    },
    async () => {
      const response = await worker.fetch(
        post("/v1/daily-insight", JSON.stringify({
          targetCareer: "Android Geliştirici",
          dateKey: "2026-08-21"
        })),
        env(),
        {}
      );
      const payload = await response.json();

      assert.equal(response.status, 200);
      assert.equal(payload.date_key, "2026-08-21");
      assert.equal(payload.title, "Kanıt üretme günü");
    }
  );
});

test("a failed primary model is logged and the fallback model's success is flagged", async () => {
  const originalFetch = globalThis.fetch;
  const originalLog = console.log;
  const events = [];
  let calls = 0;
  console.log = (line) => events.push(JSON.parse(line));
  globalThis.fetch = async () => {
    calls += 1;
    if (calls === 1) return new Response("nope", { status: 500 });
    return new Response(JSON.stringify({
      choices: [{ message: { content: JSON.stringify({
        title: "Gün", message: "Küçük adım.", action_label: "Başla", coach_prompt: "Böl."
      }) } }]
    }), { status: 200 });
  };
  try {
    const response = await worker.fetch(
      post("/v1/daily-insight", JSON.stringify({ targetCareer: "Android Geliştirici", dateKey: "2026-08-21" })),
      env(),
      {}
    );
    assert.equal(response.status, 200);
  } finally {
    globalThis.fetch = originalFetch;
    console.log = originalLog;
  }
  const modelCalls = events.filter((event) => event.event === "ai_model_call");
  assert.deepEqual(modelCalls.map((event) => [event.fallback, event.outcome]), [[false, "error"], [true, "ok"]]);
  const request = events.find((event) => event.event === "ai_request");
  assert.equal(request.path, "/v1/daily-insight");
  assert.equal(request.status, 200);
});

test("coach history drops leading assistant turns, merges repeats and never ends on a user turn", () => {
  const history = normalizeCoachHistory([
    { role: "assistant", content: "Merhaba!" },
    { role: "user", content: "Soru 1" },
    { role: "user", content: "Soru 1b" },
    { role: "assistant", content: "Cevap 1" },
    { role: "user", content: "yarım kalan" }
  ]);
  assert.deepEqual(history, [
    { role: "user", content: "Soru 1\n\nSoru 1b" },
    { role: "assistant", content: "Cevap 1" }
  ]);
  assert.deepEqual(normalizeCoachHistory(undefined), []);
});

test("safeHashEquals returns false (not throws) for equal-length strings with different byte lengths", () => {
  assert.equal(safeHashEquals("abc", "abc"), true);
  assert.equal(safeHashEquals("abc", "abd"), false);
  assert.equal(safeHashEquals("aaa", "aaş"), false);
  assert.equal(safeHashEquals(undefined, "abc"), false);
});

test("model selection: Nemotron defaults disable thinking; NVIDIA_*_MODEL vars override without a redeploy", async () => {
  const originalFetch = globalThis.fetch;
  const bodies = [];
  globalThis.fetch = async (_url, init) => {
    bodies.push(JSON.parse(init.body));
    return new Response(JSON.stringify({
      choices: [{ message: { content: JSON.stringify({
        title: "Gün", message: "Küçük adım.", action_label: "Başla", coach_prompt: "Böl."
      }) } }]
    }), { status: 200 });
  };
  const call = (environment) => worker.fetch(
    post("/v1/daily-insight", JSON.stringify({ targetCareer: "Android Geliştirici", dateKey: "2026-08-21" })),
    environment,
    {}
  );
  try {
    assert.equal((await call(env())).status, 200);
    assert.match(bodies[0].model, /nemotron/i);
    assert.deepEqual(bodies[0].chat_template_kwargs, { enable_thinking: false });

    assert.equal((await call(env({ NVIDIA_PRIMARY_MODEL: "vendor/other-model" }))).status, 200);
    assert.equal(bodies[1].model, "vendor/other-model");
    assert.equal("chat_template_kwargs" in bodies[1], false);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("daily-insight retries once when the model returns truncated JSON, then succeeds", async () => {
  const originalFetch = globalThis.fetch;
  let calls = 0;
  globalThis.fetch = async () => {
    calls += 1;
    const content = calls === 1
      ? '{"title":"Gün","message":"Küçük adım.","action_label":"Başla","coach_prompt":"Böl'
      : JSON.stringify({ title: "Gün", message: "Küçük adım.", action_label: "Başla", coach_prompt: "Böl." });
    return new Response(JSON.stringify({ choices: [{ message: { content } }] }), { status: 200 });
  };
  try {
    const response = await worker.fetch(
      post("/v1/daily-insight", JSON.stringify({ targetCareer: "Android Geliştirici", dateKey: "2026-08-21" })),
      env(),
      {}
    );
    assert.equal(response.status, 200);
    assert.equal(calls, 2);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("unreadable JSON twice gives a Turkish user-facing error, not the generic one", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response(
    JSON.stringify({ choices: [{ message: { content: '{"title":"yarım' } }] }),
    { status: 200 }
  );
  try {
    const response = await worker.fetch(
      post("/v1/daily-insight", JSON.stringify({ targetCareer: "Android Geliştirici", dateKey: "2026-08-21" })),
      env(),
      {}
    );
    const payload = await response.json();
    assert.equal(response.status, 502);
    assert.equal(payload.error, "AI yanıtı okunamadı.");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("scheduled retention deletes expired anonymous vaults", async () => {
  const database = createMemoryDb();
  const environment = env({ DB: database });
  await worker.fetch(
    syncRequest("PUT", JSON.stringify({ session: { targetCareer: "Test" } })), environment, {}
  );
  assert.equal(database.size(), 1);
  database.expireAll();

  const pending = [];
  await worker.scheduled({}, environment, { waitUntil: (promise) => pending.push(promise) });
  await Promise.all(pending);

  assert.equal(database.size(), 0);
});
