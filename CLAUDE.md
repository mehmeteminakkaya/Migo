# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Migo (Turkish UI, application ID `com.migo.kariyer`) is an Android career-planning app for university
students, paired with a Cloudflare Worker backend. The app uses YÖK Atlas program data and an
NVIDIA-NIM-backed AI to build a personalized career map, 30-day action plans, a daily coach, and CV
analysis. All AI/network logic is server-side — the Android app never talks to NVIDIA directly, and no
API key is embedded in the APK.

Two independently versioned projects live in this repo:
- `/` — the Android app (Kotlin + Jetpack Compose), Gradle module `:app`.
- `backend/` — the Cloudflare Worker API (`migo-api`), plain JS + Wrangler, pnpm-managed.

## Commands

### Android (run from repo root)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug              # debug build
.\gradlew.bat :app:testDebugUnitTest           # JVM unit tests
.\gradlew.bat :app:lintDebug                   # lint
.\gradlew.bat :app:connectedDebugAndroidTest   # instrumented tests (needs device/emulator)
```

Run a single JVM unit test class:
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.migo.CareerTaskPlannerTest"
```

Build outputs are redirected outside the repo (to `%LOCALAPPDATA%\MigoAndroidBuild\app`, see
`app/build.gradle.kts`) specifically to avoid OneDrive file-lock errors on Windows — don't assume
`app/build/` exists or holds anything.

Release (signed AAB) builds require a one-time signing setup and enforce an HTTPS
`MIGO_BACKEND_URL` at build time (`preReleaseBuild` check in `app/build.gradle.kts`):
```powershell
.\setup-release-signing.cmd     # once, generates upload keystore
.\build-play-release.cmd        # produces signed AAB + SHA-256 in release-output/ (gitignored)
```
Bump `versionCode` in `app/build.gradle.kts` for every Play Store update.

`MIGO_BACKEND_URL` is read from `local.properties` (or the env var of the same name) and injected
into `BuildConfig`. CI sets it via env var (see `.github/workflows/ci.yml`); locally it's normally
written by `backend/deploy-auth.cmd`/`deploy.sh` after a Worker deploy.

### Backend (run from `backend/`)

```powershell
pnpm install
pnpm dev                          # wrangler dev (local Worker)
pnpm test                         # node --test (backend/test/*.test.js)
pnpm check                        # wrangler deploy --dry-run && wrangler check startup
.\deploy-auth.cmd                 # interactive: prompts for CF API token, deploys, applies D1 migrations, sets NVIDIA_API_KEY secret if missing, verifies live endpoints, writes MIGO_BACKEND_URL into ../local.properties
```
Git Bash: use `bash ./deploy.sh` instead of `deploy-auth.cmd` (same flow, different shell).

Run a single backend test file directly: `node --test test/index.test.js`.

### CI

`.github/workflows/ci.yml` runs on every PR and push to `main`: Android job runs
`:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`; backend job runs `pnpm test && pnpm check`.
Both must pass before merging.

## Architecture

### Android app (`app/src/main/java/com/example/migo/`)

Single-Activity, no navigation library — `MainActivity.kt` holds one `@Composable` state machine
(`SCREEN_WELCOME` → `SCREEN_CONSENT` → `SCREEN_PROFILE` → `SCREEN_AI_DISCOVERY` → `SCREEN_ANALYSIS` →
`SCREEN_MAIN`) driven by an `Int` in `rememberSaveable`. Screen composables live under `ui/*` by
feature (`ui/legal`, `ui/profile`, `ui/analysis`, `ui/main`), not by screen name.

- `data/model/CareerModels.kt` — all domain data classes (`UserProfile`, `CareerSession`,
  `CareerAnalysis`, `CareerRecommendation`, `CareerActionPlan`, `CoachMessage`, `CvAnalysis`,
  `DailyCareerInsight`, ...) plus their JSON (de)serialization (`toJson()` / `toCareerSession()`
  extensions) and `AiContentSanitizer` for cleaning AI-generated text before display.
- `data/remote/NvidiaCareerService.kt` — the only place that calls the backend. Every AI-backed
  operation (`createCareerAnalysis`, `enrichCareer`, `createActionPlan`, `createDailyInsight`,
  `askCoach`, `analyzeCv`) checks `BuildConfig.MIGO_BACKEND_URL` first and short-circuits to a
  `missingBackend()` failure or a local fallback (`fallbackActionPlan`, `fallbackDailyInsight`) when
  it's blank, so the app degrades gracefully without a configured backend.
- `data/remote/MigoClientIdentity.kt` — holds the per-install `installationId` and cloud `syncSecret`
  used as request identity/auth for the backend (sent as `x-migo-installation` header /
  `Authorization: Bearer`), configured once from `MigoPreferences` in `MainActivity`.
- `data/remote/CloudSyncService.kt` — pushes/pulls the encoded `CareerSession` to the Worker's D1-backed
  vault (`/v1/sync/session`); `MainActivity` pulls on privacy-consent grant and pushes (debounced by
  ~900ms) whenever the session changes.
- `data/remote/AiCoachRules.kt` — loads a bundled ruleset (from `assets/`) that's sent to the backend as
  `aiRules` and merged into system prompts server-side (see `withAiCoachRules` in `backend/src/index.js`)
  to steer content quality without needing a backend redeploy for prompt tweaks.
- `data/local/MigoPreferences.kt` — `SharedPreferences` wrapper; session JSON is encrypted at rest via
  `LocalDataCipher` before storage, with a one-time migration path for pre-encryption data
  (`migrateToEncryptedStorage`). Also owns privacy-consent versioning/timestamps and installation ID
  generation.
- `data/local/PdfCvExtractor.kt` — extracts text from a user-supplied CV PDF on-device (via
  `pdfbox-android`) so the raw PDF file itself is never uploaded — only extracted text goes to
  `/v1/cv-analysis`.
- `MigoApplication.kt` — initializes `NvidiaCareerService`'s bundled AI-coach rules and PDFBox at
  app startup.

State flows one level: `MainActivity`'s `MigoApp()` composable owns all top-level mutable state
(profile, session, analysis) and passes it down with callbacks; there's no ViewModel/repository layer.
`CareerSession` (the persisted unit) bundles profile + interests/skills + analysis + target career +
generated tasks (`CareerTaskPlanner.build(...)`).

Debug-only source set (`app/src/debug/`) adds `*PreviewActivity` launchers for individual screens and
a `StoreAssetSeedReceiver` for generating store screenshots — these are never part of release builds.

### Backend (`backend/src/`)

Single-file Cloudflare Worker (`index.js`, no framework) exporting `fetch` and a `scheduled` cron
handler. Routing is a manual `if` chain in `fetch`, not a router library.

- **Auth model**: every AI (`/v1/*` POST) and sync (`/v1/sync/session`) request must carry
  `x-migo-installation` (regex-validated installation ID). Sync additionally requires a bearer
  `syncSecret`, which is only ever stored server-side as a SHA-256 hash (`hashSyncSecret`) compared
  with `timingSafeEqual` — the plaintext secret never touches D1.
- **Rate limiting**: Cloudflare `ratelimits` bindings, both per-installation and per-network-IP
  (`CLIENT_AI_RATE_LIMITER` / `NETWORK_AI_RATE_LIMITER` for AI endpoints, `SYNC_RATE_LIMITER` for sync),
  declared in `wrangler.jsonc`. `enforceAiRateLimits` fails closed if a binding is missing.
  Configured limits: 12/min per install, 30/min per network IP for AI; 60/min per install for sync.
- **AI calls** (`callNvidiaJson`/`callNvidia`) always try `PRIMARY_MODEL`
  (`meta/llama-3.1-8b-instruct`) then fall back to `FALLBACK_MODEL`
  (`mistralai/ministral-14b-instruct-2512`). Structured endpoints pass a JSON Schema as an OpenAI-style
  forced tool call (`submit_migo_response`); the response is parsed with `jsonrepair` as a fallback for
  malformed JSON. Every AI system prompt is Turkish and encodes hard content rules directly in the
  prompt text (no personality/skill inference beyond declared profile data, internships framed as
  optional with equivalent alternatives, no fabricated job/salary guarantees, regulated professions must
  state real requirements) — when touching prompts, preserve these constraints, they're product/legal
  requirements, not incidental copy.
- **Salary data**: `fetchSalaryBenchmark` scrapes live kariyer.net pages (cached via Cache API) for a
  fixed set of engineering/psychology roles in `SALARY_REFERENCES`, with a hardcoded `snapshot` fallback
  per entry if the live fetch/parse fails — salary numbers are never AI-generated.
- **Education catalog** (`/v1/education/{universities,programs}`): proxies/caches the public YÖK Atlas
  API (`getOfficialCatalog`, cached via Cache API, `CATALOG_CACHE_SECONDS`).
- **Cloud sync vault** (`syncSession`): D1 table `user_vaults` (see `backend/migrations/`), one row per
  installation, `expires_at` extended 180 days (`VAULT_RETENTION_DAYS`) on every successful sync. The
  raw CV PDF is never stored — only the app's locally-extracted session JSON.
- **Retention cron**: `scheduled()` runs daily (`0 3 * * *` per `wrangler.jsonc`) and deletes vaults past
  `expires_at` (`deleteExpiredVaults`).
- **Public pages**: `/privacy`, `/terms`, `/support`, and `/v1/legal` (policy version metadata for the
  app) are served from `backend/src/legal-pages.js`.
- All user-facing error strings are Turkish; `safeError()` redacts anything matching `nvapi-...` before
  it can leak into a response or log.

### Data flow between app and backend

`UserProfile` (+ interests/skills) → `POST /v1/career-analysis` → `CareerAnalysis` with 4 `branches`.
User picks one branch → `POST /v1/career-detail` (adds live/snapshot salary) → becomes the session's
`targetCareer`. From there: `POST /v1/action-plan` (30-day plan), `POST /v1/daily-insight` (daily nudge),
`POST /v1/coach` (freeform Q&A), `POST /v1/cv-analysis` (CV scoring). The full `CareerSession` is
persisted locally (encrypted) and optionally mirrored to the Worker's D1 vault for cross-device sync.

## Working conventions

- Backend and Android are versioned together but deployed independently — after changing the Worker's
  request/response shape for an endpoint, check both `NvidiaCareerService.kt` (request building) and
  `data/model/CareerModels.kt` (JSON parsing) for the matching contract, and keep old-field
  compatibility aliases (see the `strengths`/`careers` fields in `normalizeCareerAnalysis`) when you
  can't guarantee simultaneous rollout.
- Never hardcode or reintroduce the NVIDIA API key (or any secret) on the Android side; it is a
  Cloudflare Worker secret only, set via `wrangler secret` (or the `deploy-auth` script's prompt).
- When adding a new AI-backed endpoint or changing a prompt, keep the JSON-schema-forced tool-call
  pattern and add a `normalize*` function that clamps list lengths / truncates text and throws on
  clearly-incomplete AI output, matching the existing endpoints' defensive parsing.

## When to Use Plugins

Pick only the plugin/skill a task actually needs — don't run all of them on every change.

- **superpowers** — the default process layer for non-trivial work: `brainstorming` before
  creative/design work, `writing-plans` + `test-driven-development` before implementation,
  `systematic-debugging` before proposing a fix for a bug/test failure, `verification-before-completion`
  before claiming something is done.
- **feature-dev** — for new features or changes that touch the app/backend contract and need
  codebase understanding before writing code.
- **frontend-design** — for Compose UI work under `app/src/main/java/com/example/migo/ui/**`
  (new screens, layout/visual changes).
- **context7** — before writing code against an unfamiliar or version-sensitive third-party API used
  here (Jetpack Compose, Wrangler/Cloudflare Workers bindings, pdfbox-android), instead of relying on
  memory.
- **code-review** / **pr-review-toolkit** — after implementation, before calling a change done or
  opening a PR. `code-review` for a general diff pass; `pr-review-toolkit` for the specialized reviewer
  agents (test coverage, silent-failure hunting, type design).
- **code-simplifier** — after correctness is confirmed, when a change introduced avoidable
  duplication or complexity.
- **security-guidance** — its hook surfaces reminders automatically on security-sensitive diffs; run
  its `security-review` skill explicitly before merging changes to backend auth/rate-limit/sync code
  (`backend/src/index.js`) or to `LocalDataCipher`/`MigoClientIdentity`/`MigoPreferences`.
- **claude-md-management** — only when updating this file itself (`claude-md-improver` to audit,
  `revise-claude-md` to fold in new session learnings).
- **claude-code-setup** — only if asked to recommend or set up new Claude Code automations for this
  repo; not part of normal feature work.

## Claude Code Workflow

For non-trivial changes to Migo, follow: **Analyze → Plan → Implement → Test → Review → Security Check
→ Summarize.**

1. **Analyze** — read the relevant existing implementation in `app/` and/or `backend/` first; confirm
   which files already own the behavior (see Architecture above) before changing anything.
2. **Plan** — for features or anything touching the app/backend contract, use `feature-dev` or a
   Superpowers workflow (`brainstorming` → `writing-plans`) before writing code. Small, well-scoped
   fixes don't need a formal plan.
3. **Implement** — follow existing patterns (state flows one level in the app, `normalize*` defensive
   parsing in the backend); don't introduce new architecture, dependencies, or abstractions the codebase
   doesn't already use. Use `frontend-design` for UI work and `context7` for library-doc lookups as
   needed.
4. **Test** — run the Gradle/pnpm commands from the Commands section for whatever you touched. Never
   hide, skip, or paper over a failing test — fix the underlying cause.
5. **Review** — run `code-review` or `pr-review-toolkit` on the diff.
6. **Security Check** — for changes touching auth, secrets, rate limiting, sync, or stored user data,
   run the `security-review` skill before merging.
7. **Summarize** — report what changed and why, referencing files, and call out anything left out of
   scope.
