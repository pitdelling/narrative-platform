# Audit: Narrative Platform readiness for PWA + Web Push notifications

Read-only audit. No files were changed during the audit itself. Nothing was implemented. No Maven/Gradle/Java/Spring Boot CLI command was run — findings come entirely from reading source, config and docs. No compile/test claim is made anywhere below.

## Governance check

- Root `CLAUDE.md`: does not exist. Only `backend/CLAUDE.md` exists. No `frontend/CLAUDE.md` either.
- `backend/README.md` and `frontend/README.md` read in full (both short: backend describes bounded contexts + recent features; frontend is 9 lines describing nav/account behavior).
- Root `README.md` is effectively empty (`# narrative-platform`, one line).
- `PROJECT-STATUS.md` (MVP status doc) explicitly lists, under "Deliberately deferred": *"Realtime notifications, push notifications and automatic WhatsApp Business sending."* — push was a known, intentionally-deferred gap, not an oversight.

---

## 1. Current architecture summary

**Backend** — Spring Boot 21 (Java 21), package root `com.narrativeplatform`, bounded contexts `auth`, `party`, `invitation`, `chronicle`, `aijob`, plus `configuration`/`security`/`shared`. Postgres via Flyway (2 migrations: `V1__initial_schema.sql`, `V2__game_turn_sequence_deferrable.sql`). JWT auth (stateless, HMAC-signed, `JwtAuthenticationFilter`), Argon2id passwords, CORS locked to a single `FRONTEND_URL` origin. `ProblemDetail`-based error responses via one `@RestControllerAdvice`. Two `@Scheduled` jobs total (turn-expiry sweep every 60s, AI-job poller every 15s). AI generation runs through a durable job-queue table (`ai_jobs`) with pessimistic-lock claim/complete/fail and stale-job recovery — the closest thing to an "outbox" pattern in the codebase, though it's poll-based, not event-driven (no Spring `ApplicationEventPublisher`/`@EventListener` anywhere in the project). No `Clock` bean — every timestamp is `Instant.now()` called directly at the use site. No generic reversible-encryption utility (only Argon2 password hashing and one-way SHA-256 for invite tokens); BouncyCastle (`bcprov-jdk18on`) is already a Maven dependency though unused for this purpose. No backend tests exist (`src/test/java` doesn't exist), but `spring-boot-starter-test` + `spring-security-test` are already on the classpath, unused.

**Frontend** — Next.js 16.2.11 (App Router) + React 19.2.0, no state library, a thin `fetch` wrapper (`lib/api.ts`) that attaches a `Bearer` token from `localStorage` and always uses `cache: "no-store"`. Auth token lives only in `localStorage` (`lib/auth.ts`), no cookies, no `middleware.ts`, no centralized route guard — each page independently redirects to `/` on an auth failure. `AppShell` (a client component, not a Next `layout.tsx`) is the shared authenticated chrome (sidebar/hamburger + an account modal with logout and password change). There is no `frontend/public/` directory at all — zero icons, zero manifest, zero service worker, zero favicon anywhere in the repo. `next.config.ts` and the root `layout.tsx` `metadata` export are both minimal (no `manifest`, `themeColor`, `icons`, or `viewport` fields; no `headers()` function; no CSP/HSTS anywhere in the whole repo, frontend or backend). Zero frontend test infrastructure (no jest/vitest/playwright, no config file, no `*.test.*` in project source).

**Deployment** — Render (Docker, `backend/Dockerfile`, two-stage Maven→JRE build, no healthcheck directive beyond `render.yaml`'s `healthCheckPath`) for the backend; Vercel for the frontend (`SETUP-PTBR.md` §9–10, placeholder URLs only, no fixed production URL recorded anywhere). No Cloudflare Tunnel mention exists anywhere in the repository (grepped all docs and config, zero matches).

---

## 2. Detailed findings by requested topic

### Frontend
- **Next.js/App Router**: v16.2.11, App Router only (`app/` dir). Routes: `/` (login+register toggle, `frontend/app/page.tsx`), `/invite/[token]`, `/app` (party list dashboard), `/app/party/[partyId]`, `/app/party/[partyId]/chronicle/[chronicleId]`.
- **Layouts/authenticated shell**: single root `app/layout.tsx` (fonts + basic `metadata`); the "authenticated shell" is `components/AppShell.tsx`, a client component wrapped around page content (not a route-group layout), fetching `/auth/me` on mount and handling logout (`clearToken()` + `router.push("/")`) and password change (`POST /auth/change-password`).
- **Manifest/PWA metadata**: none. `layout.tsx` metadata has only `title`/`description`.
- **Service workers**: none.
- **Static assets/icons**: none — no `public/` directory exists at all.
- **Auth/logout flow**: login/register at `/` (`POST /auth/login` or `/auth/register-narrator`) → `setToken()` → `localStorage` → `router.push("/app")`. Logout is `clearToken()` + redirect, no server-side session/refresh-token invalidation call.
- **Account/profile page**: no dedicated route — it's a modal inside `AppShell` (`accountOpen` state), showing username/display name and a password-change form only. No notification-preference UI exists.
- **API-client conventions**: `lib/api.ts`, one function `api<T>(path, init)`, always `cache: "no-store"`, parses `ProblemDetail` JSON on error into a typed `ApiError` with `status`/`code`.

### Backend
- **Package structure**: `app/{auth,party,invitation,chronicle,aijob}` each with `controllers/models/repositories/services`; `configuration`, `security`, `shared/{constants,exceptions,integrations,utils}`.
- **Party membership/role authorization**: `PartyMemberEntity` (`role`: `OWNER|NARRATOR|PLAYER`; `status`: `ACTIVE|DISABLED|REMOVED`); `PartyAccessService.requireActiveMember/requireNarrator/requireOwner` is the single authorization chokepoint used by every party/chronicle service.
- **Game-chronicle creation**: `GameChronicleService.create` — fixed shuffled order (creator first), all turns for all cycles pre-materialized (`GameTurnEntity` rows), opening segment auto-submitted, next turn auto-activated.
- **Turn submission**: `GameChronicleService.publish` — validates current-turn ownership, saves `GameSegmentEntity`, marks turn `SUBMITTED`, calls `advance`.
- **Current-participant calculation**: the single authoritative source is `GameRunEntity.currentSequence`, resolved via `GameTurnRepository.findByRunIdAndSequenceNumber(runId, currentSequence)`. There is no separate "who's next" computation anywhere else.
- **Turn expiration**: the single authoritative source is `GameTurnEntity.expiresAt`, computed at turn-activation time in `activate()` as `startedAt + AppProperties.turnExpirationHours()`. Enforced two ways: lazily inside `requireCurrentTurn` and proactively by the `expireTurns()` scheduled sweep (every 60s).
- **Story completion**: `advance()`/`completeRun()` — when no next turn is found, `GameRunEntity.status = COMPLETED`, `ChronicleEntity.status = AI_PENDING`, `AiJobService.enqueue(...)` is called. This is the exact moment a "story finished" notification would need to originate from.
- **Scheduled tasks**: exactly two `@Scheduled` methods in the whole backend — `GameChronicleService.expireTurns` (60s) and `AiJobProcessor.processPending` (15s).
- **Asynchronous jobs**: `aijob` context — `AiJobEntity` (status `PENDING/PROCESSING/COMPLETED/FAILED`, `idempotencyKey` unique, `attemptCount`), `AiJobRepository.findForUpdate` (pessimistic-lock claim query), `AiJobStateService` (`claim`/`complete`/`fail`/`recoverStaleJobs`), `AiJobProcessor` (the poller). A genuine outbox/job-queue pattern, structurally reusable as a template, though poll-driven, not event-driven.
- **Notification/event infrastructure**: none exists. No `ApplicationEventPublisher`, no `@EventListener`, no notification entity/table, no web-push/subscription concept anywhere. A stray, unused `audit_events` table exists in `V1__initial_schema.sql` with `event_type`/`payload_json` columns but has zero corresponding Java code — dead schema, not usable event infra.
- **Database clock/UTC conventions**: `application.yml` sets `hibernate.jdbc.time_zone: UTC`; every persisted timestamp across the codebase is `java.time.Instant`. Consistent project-wide.
- **`ProblemDetail` exception handling**: `GlobalExceptionHandler` (`@RestControllerAdvice`) + `DomainException` base class (status + code). Existing codes: `bad_request` (400), `forbidden` (403), `not_found` (404), `conflict` (409), `ai_not_configured` (503, hardcoded in its own exception class — the established pattern any new problem code should copy).
- **Flyway migrations**: exactly two files, `V1__initial_schema.sql` and `V2__game_turn_sequence_deferrable.sql`. Next migration must be `V3__*.sql`.
- **Backend test infrastructure**: `src/test/java` does not exist. `spring-boot-starter-test` + `spring-security-test` are already Maven dependencies (unused). No Testcontainers.
- **Security headers**: none configured anywhere (no `.headers(...)` in `SecurityConfiguration`, no CSP/HSTS/X-Frame-Options in either frontend or backend).
- **Production frontend URL**: never hardcoded — `AppProperties.frontendUrl`/`publicUrl` come from `FRONTEND_URL`/`APP_PUBLIC_URL` env vars; `SETUP-PTBR.md` only documents placeholder Vercel/Render URLs, no fixed production domain is recorded in the repo.
- **Deployment**: Render (`render.yaml`, Docker) for backend, Vercel for frontend, per `SETUP-PTBR.md`. No Cloudflare Tunnel anywhere.

---

## 3. Reusable components

- **`AiJobEntity` / `AiJobRepository` / `AiJobStateService` / `AiJobProcessor`** (`backend/src/main/java/com/narrativeplatform/app/aijob/`) — the closest existing pattern to a notification/push outbox: status-enum state machine, unique idempotency key, `@Lock(PESSIMISTIC_WRITE)` claim query, attempt counting, stale-job recovery via a time cutoff, `@Scheduled` poller pulling a small batch.
- **`GlobalExceptionHandler` + `DomainException` hierarchy** (`backend/src/main/java/com/narrativeplatform/shared/exceptions/`) — new problem codes should be added as one more `DomainException` subclass, following `AiNotConfiguredException`'s exact shape.
- **`PartyAccessService`** (`backend/src/main/java/com/narrativeplatform/app/party/services/PartyAccessService.java`) — the one authorization chokepoint; any new party-scoped notification-settings endpoint should route through it.
- **`GameChronicleService.advance()`/`completeRun()`/`activate()`** — the three exact call sites where "turn became active" and "run completed" already happen; no new polling/derivation is needed, only a hook at these existing mutation points.
- **`lib/api.ts`** frontend fetch wrapper — any new frontend calls (subscribe/unsubscribe endpoints) should go through this.
- **`AppShell.tsx`** account modal — the natural existing place to add a notification toggle.
- **BouncyCastle (`bcprov-jdk18on`)** — already a Maven dependency; usable if push-subscription keys ever need reversible encryption at rest.
- **`spring-boot-starter-test`/`spring-security-test`** — already present; a future feature could be this project's first backend test without a new dependency.

---

## 4. Risks

- **No `Clock` bean anywhere.** Every timestamp is a direct `Instant.now()` call. Retrofitting existing call sites for testability would be an unrelated refactor and is out of scope for a notification feature.
- **No event/pub-sub infrastructure.** Notification triggers will have to be added as direct method calls inside existing services, the same "direct cross-context service call" style already used elsewhere in this codebase — increasing coupling between contexts, which is worth naming explicitly as a tradeoff.
- **No reversible encryption utility.** A Web Push subscription (endpoint URL + p256dh/auth keys) is sensitive; storing it in plaintext is the path of least resistance given current tooling, but that's a security tradeoff to flag explicitly.
- **Zero test coverage, backend and frontend.** Any notification logic added will be the first tested code in this repository if tests are written.
- **No security headers / no CSP anywhere.** A service worker + push requires careful `Service-Worker-Allowed`/scope and HTTPS; none of it can be "extended" from existing config — it would all be new.
- **CORS is single-origin and `allowCredentials(false)`.** Confirms there's no cookie/session mechanism to piggyback on.
- **No fixed production URL is recorded anywhere in the repo.** Manifest `start_url`/`scope` and any push-related absolute URLs will need real values supplied at deploy time.
- **Backward compatibility**: recent changes to `GameTurnEntity`/`GameRunEntity`/`PartyMemberEntity` were additive and don't change existing row shapes; a notification feature layering on top of `advance()`/`completeRun()` should be similarly additive.
- **Assumption conflict**: the audit request assumed a Cloudflare Tunnel deployment exists — it does not; only Render + Vercel are documented.

---

## 5. Recommended migration order (for a future implementation task — not started here)

1. A new Flyway migration `V3__*.sql` (schema only) introducing whatever push-subscription/notification-outbox tables are chosen.
2. Backend domain/entity/repository layer for the new tables, following the `aijob` package shape (own bounded context, e.g. `app/notification`), including a `DomainException` subclass for any new problem code.
3. Hook points inside existing services (`GameChronicleService.advance()`/`completeRun()`, `InvitationService.acceptForUser`, etc.) that enqueue a notification row — additive calls only.
4. A `@Scheduled` dispatcher (mirroring `AiJobProcessor`) to actually deliver queued notifications.
5. Frontend: `public/manifest.webmanifest` + icons + minimal service worker + subscribe/unsubscribe API calls through `lib/api.ts`, and a toggle inside `AppShell`'s account modal.
6. Security hardening (headers/CSP for the service worker) as an explicit, separate step, since none exists today to extend.

This ordering reflects only the dependency chain the audit surfaced; nothing above has been built.

---

## 6. Assumption conflicts found during the audit

- **Cloudflare Tunnel**: assumed as part of deployment but does not exist anywhere in this repository — deployment is Render (backend, Docker) + Vercel (frontend) only, per `render.yaml` and `SETUP-PTBR.md`.
- **"Outbox or job table"**: `ai_jobs` is job-queue-shaped but AI-specific; a separate, unused `audit_events` table exists in the schema but has zero application code behind it — the two should not be conflated.
- **Injectable Clock bean**: none exists — confirmed absent.
- **Encryption utility for sensitive DB values**: none exists beyond one-way hashing (Argon2 for passwords, SHA-256 for invite tokens).
