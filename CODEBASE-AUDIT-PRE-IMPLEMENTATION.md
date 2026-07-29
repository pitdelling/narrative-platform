# Narrative Platform — Pre-Implementation Codebase Audit

Read-only audit performed before implementing upcoming features (voting, light/dark theme, expanded invitation channels, etc.). No files were changed. No Maven/Gradle/Java/backend CLI commands were run. All findings below come from reading `backend/CLAUDE.md`, `README.md` (root, backend, frontend), the two Flyway migrations, and the full source of the `auth`, `party`, `invitation`, `chronicle`, and `aijob` bounded contexts plus the entire frontend (`frontend/app`, `frontend/components`, `frontend/lib`).

Base API path is `/api` (`NEXT_PUBLIC_API_URL` defaults to `http://localhost:8080/api`; all backend controllers are mapped under `/api/...`).

> **Product decision (post-audit, supersedes §4 below):** WhatsApp Business API sending will **not** be built. Delivery of invitations/notifications will instead go through Web Push, per the separate `PWA-PUSH-AUDIT.md` audit already on disk. The **email invitation feature is also deprioritized**, and both existing integrations — the Resend email client and the `wa.me` WhatsApp deep-link — are to be **removed** from the codebase, not extended. See the updated §4 and the revised implementation order at the end of this document for the concrete scope of that removal.

---

## 1. Account registration and user-facing registration copy

**Endpoints** (`AuthController`, `/api/auth`):
- `POST /api/auth/register-narrator` — public. Body: `{username, displayName, password}`. Creates **only a `UserEntity`** — no party is auto-created. Returns `201` with `AuthResponse{token, userId, username, displayName}`.
- `POST /api/auth/register-from-invite` — public. Body: `{token, username, displayName, password}`. Validates the invite (`InvitationService.validateForRegistration`), creates the user, then consumes the invite (`InvitationService.acceptForUser`), creating `PLAYER` membership in one flow.
- `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/change-password` (auth required; validates current password, requires new ≠ current).

**Validation**: `username` — not blank, ≤40 chars, `^[a-zA-Z0-9_-]+$`, uniqueness checked case-insensitively (`existsByUsernameIgnoreCase`) and enforced at the DB level by both a `UNIQUE` constraint and a case-insensitive unique index. `displayName` ≤80. `password` 8–200 chars, Argon2id hashing via Spring Security.

**User-facing copy** lives entirely in the frontend, in Portuguese, hardcoded inline (no i18n layer):
- `frontend/app/page.tsx` — the combined login/register screen. Headline: *"Seu segundo arquivo para mundos que ainda estão sendo escritos."* Toggle between "Entrar" / "Criar conta de narrador". Footer note: *"Contas de jogador são criadas apenas por um convite individual."*
- `frontend/app/invite/[token]/page.tsx` — invite-driven registration/login screen, copy such as *"Você foi convidado para {party}"*, single-use link notice.

**Reusable services**: `AuthService` (`registerNarrator`, `registerFromInvite`, `changePassword`, `login`), `CurrentUserService.require()` (never trusts a client-supplied user ID — identity always comes from the JWT via `SecurityContextHolder`), `JwtService`.

**Backward-compatibility risks**: username uniqueness is enforced at three layers that must stay in sync; there is no endpoint to disable/enable a `UserEntity` itself (only party membership status) despite the `enabled` column existing; `SecurityConfiguration` hardcodes the public-endpoint allowlist by exact path, so any new public route (e.g. a public shareable chronicle page) must be added there explicitly.

---

## 2. Light/dark theme infrastructure

**Not implemented.** Confirmed by full-repo case-insensitive search for `theme`, `dark`, `light` in the frontend — zero matches outside unrelated words.

What exists instead: a single fixed palette ("Arcane Editorial Linen") hardcoded as CSS custom properties on `:root` in `frontend/app/globals.css` (`--navy`, `--linen`, `--parchment`, `--ivory`, `--gold`, etc.), with no `prefers-color-scheme` media query, no theme toggle, no `data-theme` attribute usage, and no per-user preference storage anywhere in `lib/auth.ts` or the API. `PROJECT-STATUS.md` explicitly lists "campaign-specific themes" under **Deliberately deferred**, but does not mention light/dark mode specifically — it was simply never started.

A future theme feature would need: a token layer to replace hardcoded hex values in `globals.css` with light/dark variants, a persistence mechanism (likely `localStorage`, mirroring the existing `narrative-platform-token` pattern in `lib/auth.ts`), and no backend involvement is required unless the preference should sync across devices (in which case it would be a new `users` column/endpoint).

---

## 3. Party creation and party invitation links

### Party creation
`POST /api/parties` (auth required). Body: `{name ≤120, description ≤1000?, imageUrl ≤1000?}`. `PartyService.create` generates a collision-safe slug (`SlugUtils.slugify` + `-2`, `-3`... suffix loop against `PartyRepository.existsBySlug`), creates the `PartyEntity`, then creates the creator's `PartyMemberEntity(role=OWNER, status=ACTIVE)`. Any authenticated user can create a party.

### Party invitation links
- `POST /api/parties/{partyId}/invites` — narrator+ only. Generates a random raw token (`TokenUtils.randomToken()`), stores only its SHA-256 hash (`token_hash`, unique) — the raw token is never persisted, only returned once in the response as part of an `{publicUrl}/invite/{token}` URL. Expiry is configurable (`app.invite-expiration-hours`).
- `GET /api/invites/{token}` — public preview (party name, inviter, expiry) used by the frontend invite landing page.
- `POST /api/invites/accept` — authenticated; consumes the invite under a pessimistic write lock (`findForUpdateByTokenHash`) to avoid race conditions on concurrent acceptance.
- `DELETE /api/parties/{partyId}/invites/{inviteId}` — narrator+ only, sets `revoked_at`.

**Single-use enforcement**: `PartyInviteEntity.isAvailable(now)` = not consumed, not revoked, not expired.

**Accept semantics**: if the user has no `party_members` row, a new one is created as `PLAYER`/`ACTIVE`. If a row already exists (previously `DISABLED` or `REMOVED`), it is reactivated **in place** and the role is reset to `PLAYER` — a former narrator who left and re-joins via a fresh invite comes back as a plain player. If already `ACTIVE`, the accept throws a conflict. After acceptance, `GameChronicleService.insertPartyMemberIntoActiveRuns` runs synchronously in the same transaction (see §5).

**Roles/authorization** (`party_members`, enums `PartyRoleType{OWNER,NARRATOR,PLAYER}`, `MemberStatusType{ACTIVE,DISABLED,REMOVED}`): centralized in `PartyAccessService` as plain service-layer guard methods (`requireActiveMember`, `requireNarrator`, `requireOwner`) called explicitly by other services — there is no annotation/interceptor-based authorization layer. `DISABLED` is reversible (reactivate), `REMOVED` is terminal and requires a brand-new invite; both facts are load-bearing for the chronicle turn-insertion logic (§5), so any future change to the membership lifecycle must preserve this three-state model exactly. Owner-only actions: promote/demote (`PUT /api/parties/{partyId}/members/{userId}/role`, cannot target `OWNER`), ownership transfer (`POST /api/parties/{partyId}/transfer`, demotes the previous owner to `PLAYER`). Narrator+ actions: disable/reactivate/remove members.

**Table**: `party_invites` — `id, party_id, created_by, token_hash(64,unique), channel(20), recipient_contact(320), expires_at, consumed_at, consumed_by, revoked_at, created_at`. `channel` is `InviteChannelType{LINK, EMAIL, WHATSAPP}`, stored as `STRING`.

**Backward-compatibility risks**: `channel` is additive-safe (new enum values fit as long as they stay ≤20 chars); `recipient_contact` is shared between email addresses and (potentially) phone numbers with no phone-format validation today; the silent role-downgrade-on-reactivation behavior would be a breaking change to alter without an explicit product decision; no rate-limiting exists on invite creation.

---

## 4. Email and WhatsApp invitation features

> **Superseded by product decision.** Both sub-features below are documented as they exist *today* for completeness, but the forward plan is: do **not** build a real WhatsApp Business API sender, do **not** further invest in the Resend email sender, and **remove both existing integrations** from the codebase. Notification/invitation delivery going forward is Web Push, scoped separately in `PWA-PUSH-AUDIT.md`. See the removal scope at the end of this section.

### Email — fully implemented, optional, synchronous (to be removed)
`shared/integrations/ResendClient.java` is a real Resend API client (Spring `RestClient`, `POST {resend.base-url}/emails`, bearer auth), builds an HTML-escaped invite email. `sendInvite(...)` is a no-op returning `false` (no exception) when `RESEND_API_KEY`/`RESEND_FROM_EMAIL` are unset — same "optional integration" pattern as OpenAI. Triggered only when `channel == EMAIL` and `recipientContact` contains `@`. The call is synchronous inside the invite-creation transaction with no try/catch — a Resend HTTP failure rolls back invite creation (no retry/queue). The frontend surfaces this via `InviteResponse.emailSent` and the copy *"Se o Resend não estiver configurado, o link ainda será criado para cópia manual."*

### WhatsApp — link-only, no API integration (to be removed)
No WhatsApp Business API / Twilio / any provider client exists anywhere in the backend (confirmed by repo-wide search — "sms" has zero matches at all). What exists: for every invite, the backend builds a `wa.me` deep link with a URL-encoded, hardcoded-Portuguese prefilled message; the frontend renders it as **"Abrir no WhatsApp"**, which opens WhatsApp for the *inviter* to manually send. `InviteChannelType.WHATSAPP` is stored but triggers no server-side send — unlike `EMAIL`, no channel-specific behavior branches on it beyond message wording. `PROJECT-STATUS.md` explicitly confirms this: *"automatic WhatsApp Business sending"* is listed under **Deliberately deferred**. A real WhatsApp Business API sender was never started and will not be started — the decision is to skip straight past it to Web Push instead.

### Removal scope (not executed by this audit — planning only)
Since this audit is read-only, no code was deleted. When the removal is implemented, it will touch at minimum:
- Backend: `shared/integrations/ResendClient.java`, its `AppProperties.Resend` config record, the `EMAIL`-branch call in `InvitationService.create` (and the `RESEND_*` env vars in `application.yml`/deployment config), the `wa.me` link-building code in the same service, and the `InviteChannelType.WHATSAPP`/`EMAIL` enum values if the invite model is simplified down to `LINK` only (needs a product call — `channel VARCHAR(20)` and existing `party_invites` rows already carry these values, so removing the enum values outright is a **breaking change to historical data**, not purely additive; keeping the enum values but making them inert is the backward-compatible option).
- Frontend: the `InviteResponse.whatsappUrl`/`emailSent` fields and their usages in `frontend/app/app/party/[partyId]/page.tsx` (the "Abrir no WhatsApp" button, the `<details className="email-invite">` block and its copy).
- Any reference to Resend/WhatsApp in `README.md` (backend), `SETUP-PTBR.md`, and `PROJECT-STATUS.md`, per the backend `CLAUDE.md` documentation-maintenance rule.

This removal can be its own small, isolated PR — see the revised implementation order below.

---

## 5. Game chronicle completion

Completion is driven by `GameChronicleService.advance(run, completedSequence)`, called from all three ways a turn can end — `publish()` (normal submission), `skip()`, and the scheduled sweep `expireTurns()` (`@Scheduled(fixedDelay = 60_000)`) — so completion logic is consistent regardless of how the last turn ends. If no turn exists at `completedSequence + 1`, `completeRun()` runs: sets `game_runs.status = COMPLETED` + `completed_at`, sets the parent chronicle to `AI_PENDING`, and **immediately enqueues an AI job** (`aiJobService.enqueue(chronicle, chronicle.getCreator())`) — AI generation is auto-triggered, not something the narrator has to request. Removing a member who holds the currently-active turn can also trigger `completeRun()` if no `WAITING` turns remain.

Notable asymmetry: **AI final-tale generation currently applies only to GAME chronicles.** `WrittenChronicleService.publish()` sets status to `PUBLISHED` directly with no `aiJobService` call — written chronicles have no AI step at all.

---

## 6. Current-participant calculation

Not a denormalized field beyond `game_runs.current_sequence` (an int pointer) plus exactly one `game_turns` row in `ACTIVE` status. `requireCurrentTurn(run)` is the canonical getter: fetches the turn at `current_sequence`, validates the run is `IN_PROGRESS` and the turn is `ACTIVE`, and **lazily expires it** if `expires_at` has passed (sets `EXPIRED`, calls `advance()`, throws `TurnExpiredException`) — so a stale read still self-heals even before the 60-second sweep catches it. For display, `GameChronicleService.detail()` finds the active turn via a query-time `.filter(status == ACTIVE).findFirst()` over the loaded turns rather than trusting `current_sequence` directly, but the two are always consistent since only one turn is ever `ACTIVE`.

Skip/expiry mark only that turn (`SKIPPED`/`EXPIRED`) and call `advance()` — the participant is not removed from the run, only from that turn, matching the CLAUDE.md invariant.

**Mid-game joins/removals** (already implemented — see backend README and `af29816`/`935f1d2` commits): `insertPartyMemberIntoActiveRuns` inserts a new `WAITING` turn at the end of the current cycle (and the same relative position in future cycles) for the joining user, then **renumbers every turn's `sequence_number` sequentially across all cycles**. This full renumbering is exactly why migration `V2__game_turn_sequence_deferrable.sql` made `uq_game_turn_sequence` `DEFERRABLE INITIALLY DEFERRED` — the unique constraint would otherwise collide mid-transaction. `removePartyMemberFromActiveRuns` deletes only not-yet-played turns (`WAITING`/`ACTIVE`) and renumbers similarly.

**Risk flagged for any future feature (e.g. voting) that references a specific turn/segment**: `sequence_number` is not stable across a party's lifetime — always reference `segment_id`/`turn_id` (stable UUIDs), never `sequence_number`, since it can be rewritten by joins/removals.

---

## 7. AI story generation

Prompt construction and orchestration: `AiJobStateService.claim()`/`buildPrompt()`. Input is `game_segments` for the run, ordered by `sequence_number`, **filtered to exclude `DISABLED` segments** — confirms the CLAUDE.md invariant. The prompt is a hardcoded template instructing the model to act as a "Chronicle Editor," treat fragments as untrusted content, preserve all events without inventing facts, and return strict JSON `{title, story}`.

`shared/integrations/OpenAiClient.java` calls OpenAI's Responses API (`POST {baseUrl}/responses`, model from `OPENAI_MODEL` env var, default `gpt-5-mini`) and extracts `output_text` plus token usage. `AiJobWorker.parseGeneratedText()` tries to parse the JSON response; on blank `story` or parse failure it **soft-fails**, falling back to the raw text as story content and the chronicle's original title — a malformed OpenAI response never fails the job outright.

Persistence on success (`AiJobStateService.complete()`): a new `generated_stories` row with `version_number = count+1`, set as `chronicles.current_generated_story_id`, `generated_preview` truncated to 600 chars, chronicle flipped to `PUBLISHED` with `published_at` stamped.

**Written chronicles have no AI generation path at all** (see §5) — relevant if an upcoming feature assumes AI generation is universal across chronicle types.

---

## 8. AI job persistence and execution

**Table `ai_jobs`**: `id, chronicle_id, requested_by, status, attempt_count, idempotency_key(unique,120), error_message, created_at, started_at, completed_at`. Enum `AiJobStatusType{PENDING, PROCESSING, COMPLETED, FAILED}` — no `CANCELLED`/`SKIPPED` state.

**Creation**: `AiJobService.enqueue()` (automatic, e.g. on game completion, does **not** check OpenAI configuration — jobs sit `PENDING` indefinitely without a key, matching CLAUDE.md) vs `enqueueRequested()` (explicit regeneration, calls `openAiClient.requireConfigured()` first, throws `AiNotConfiguredException` → HTTP 503 `ai_not_configured` if unset). Both reject if a `PENDING`/`PROCESSING` job already exists for the chronicle (`ConflictException`) — this existence check, not the `idempotency_key`, is what actually prevents duplicate jobs; the key itself is `chronicleId:randomUUID()`, non-deterministic, effectively just a DB collision guard rather than a true idempotency token.

**Execution**: `AiJobProcessor.processPending()` — genuine scheduled poller, `@Scheduled(fixedDelay = 15_000)`, `@EnableScheduling` on the application class. Each tick: recovers stale jobs first (`recoverStaleJobs()` — jobs stuck `PROCESSING` for >15 minutes are requeued or force-failed, handling crash/restart mid-call), then — only if `openAiClient.configured()` — claims up to 5 oldest `PENDING` jobs and processes them via `AiJobWorker`. `claim()`/`complete()`/`fail()` are all pessimistic-locked, state-guarded transitions (e.g. `complete()` only proceeds if current status is `PROCESSING`, protecting against stale/duplicate completion). `fail()` retries up to `MAX_ATTEMPTS = 3` before terminal `FAILED`.

**Endpoint**: only one, and it lives in `ChronicleController`, not a dedicated `aijob` controller (no `app/aijob/controllers` package exists) — `POST /api/parties/{partyId}/chronicles/{chronicleId}/regenerate`. There is **no endpoint to query AI job status directly**; clients infer progress from chronicle `status` (`AI_PENDING`/`AI_PROCESSING`/`PUBLISHED`/`FAILED`).

**Backward-compatibility risks**: `generated_stories(chronicle_id, version_number)` uniqueness relies on `count+1`, which is only race-safe because concurrent active jobs per chronicle are already blocked — a future feature allowing parallel regeneration must preserve that guard or introduce a DB sequence. `AiJobEntity.chronicle`/`requestedBy` are non-nullable — a future job type not tied 1:1 to a chronicle cannot reuse this table without a migration. The 15s/5-jobs/15-minute constants are hardcoded, not configuration-bound.

---

## 9. Published chronicle page

There is **no dedicated "published" endpoint or separate route** — the same detail endpoints serve every status:
- `GET /api/parties/{partyId}/chronicles/{chronicleId}/game?reveal=` — game chronicles.
- `GET /api/parties/{partyId}/chronicles/{chronicleId}/written` — written chronicles.

Frontend: `frontend/app/app/party/[partyId]/chronicle/[chronicleId]/page.tsx` branches on a `?type=` query param into `GameView`/`WrittenView`, and both branch internally on `data.status` (e.g., the generated-story block only renders `{data.generatedStory && ...}`). **There is no unauthenticated/public share route** — every chronicle view lives behind `/app/...` and requires a JWT (verified via `AppShell`'s `GET /auth/me` guard); only the invite-preview route (`/invite/[token]`) is public.

**Reveal rule for games**: `revealAll = gameCompleted || (narrator && reveal)`. Once `game_runs.status == COMPLETED`, all segments become visible to every viewer — this *is* the "published" reveal transition. Disabled segments remain visible but content-stripped for non-narrators (`Removido pelo Narrador` + optional reason), full content retained for narrators — matching the CLAUDE.md gray-removed-block invariant.

**Written chronicles**: no reveal logic at all — full content is always returned to any active member; "publish" only flips status, stamps `published_at`, computes a 600-char preview, and releases the edit lock.

---

## 10. Chronicle listing page

`GET /api/parties/{partyId}/chronicles` → `ChronicleService.list()` → everything except `ARCHIVED`, newest-`updated_at`-first. Returns `ChronicleCardResponse{id, type, status, title, preview, creatorName, updatedAt, published}` — **no pagination, no status/type filter params** at the API level.

Frontend: the "listing" is not a separate page but the `.chronicle-grid` section inside `frontend/app/app/party/[partyId]/page.tsx` (the party archive page) — cards show a status pill, preview text, creator name, and a distinct visual treatment (`.building` class) for anything not yet `PUBLISHED`.

---

## 11. Existing voting functionality

**Confirmed absent**, cross-checked three ways: (1) repo-wide case-insensitive search for "vote" across the entire backend returns zero matches, (2) the same search across the entire frontend returns zero matches, (3) a full read of every file in the `chronicle` bounded context (the only place it could plausibly live) found no entity, table, enum, field, endpoint, or service method resembling voting/polling/ballots. The nearest adjacent concept is narrator-only segment moderation (`disable`/`edit`/`restore`, audited via `segment_revisions`), which is a unilateral narrator action, not a multi-party vote. A voting feature is greenfield: new schema, new entities, new endpoints, no existing table to repurpose.

---

## 12. Party membership roles and authorization

Covered in depth in §3. Summary of the authorization *mechanism* specifically: no annotation-based (`@PreAuthorize`) or interceptor-based authorization exists anywhere audited — every bounded context calls explicit guard methods (`PartyAccessService.requireActiveMember/requireNarrator/requireOwner` for party-level checks; `ChronicleAccessService.requireMember/requireNarrator` for chronicle-scoped checks, which wraps `PartyAccessService`). Any new feature (voting, theme, etc.) that needs party-scoped authorization should call into these existing services rather than introducing a new pattern. Identity is always derived from `CurrentUserService.require()` (backed by the JWT `sub` claim via `SecurityContextHolder`) — controllers never accept a client-supplied user ID as proof of identity, per the CLAUDE.md security rule, and this was verified true across all four bounded contexts audited.

---

## 13. Flyway migrations related to these areas

Only two migrations exist under `backend/src/main/resources/db/migration/`:

- **`V1__initial_schema.sql`** — the entire schema in one file: `users`, `parties`, `party_members`, `party_invites`, `chronicles`, `written_story_documents`, `written_story_permissions`, `game_runs`, `game_turns`, `game_drafts`, `game_segments`, `segment_revisions`, `generated_stories`, `ai_jobs`, `audit_events`. Notably includes a defensive `DO $$ ... IF NOT EXISTS ... END $$` block to add the `chronicles.current_generated_story_id` FK idempotently — establishes a precedent any new migration touching `chronicles` should follow.
- **`V2__game_turn_sequence_deferrable.sql`** — drops and re-adds `uq_game_turn_sequence` on `game_turns` as `DEFERRABLE INITIALLY DEFERRED`, specifically to support the bulk turn-renumbering used by mid-game party joins/removals (§6).

**No migration exists yet for**: voting (no table), theme preference (no column), WhatsApp Business API delivery tracking (no column), or any AI-generation-for-written-chronicles path. All enums (`PartyRoleType`, `MemberStatusType`, `InviteChannelType`, `ChronicleStatusType`, `ChronicleType`, `GameRunStatusType`, `GameTurnStatusType`, `SegmentStatusType`, `AiJobStatusType`) are persisted as `VARCHAR` via `@Enumerated(EnumType.STRING)` — adding new enum values is additive-safe; renaming existing ones is not (breaks deserialization of existing rows) and would require a migration.

---

## Summary: reusable building blocks for future features

| Concern | Reusable entry point |
|---|---|
| Party-scoped authorization | `PartyAccessService.requireActiveMember/requireNarrator/requireOwner` |
| Chronicle-scoped authorization | `ChronicleAccessService.requireMember/requireNarrator` |
| Current-user identity | `CurrentUserService.require()` |
| Token generation/hashing | `TokenUtils.randomToken()` / `TokenUtils.sha256()` |
| Slug generation | `SlugUtils.slugify` + collision-suffix pattern in `PartyService` |
| Optional external integration pattern | `AppProperties.Resend`/`AppProperties.OpenAi` + `configured()` guard — template for a future `AppProperties.WhatsApp` |
| Background job idempotent enqueue | `AiJobService.enqueue`/`enqueueRequested` |
| Content-change audit trail | `SegmentRevisionEntity` — template for a future vote-audit or theme-change-audit entity |
| Scheduled sweep pattern | `GameChronicleService.expireTurns()` / `AiJobProcessor.processPending()` — colocated `@Scheduled` methods, no separate scheduler bean |

---

## Recommended implementation order (revised per product decision on email/WhatsApp)

Each feature below is scoped to be implemented and reviewed independently — none requires another to land first, but the order minimizes churn on shared code (turn/segment logic and the invitation/integration pattern are the two areas most likely to be touched by more than one feature).

1. **Light/dark theme infrastructure** — purely additive, frontend-only (CSS custom-property variants + a toggle + `localStorage` persistence mirroring `lib/auth.ts`). Zero backend/schema risk, zero interaction with the other features. Good first PR to de-risk nothing else.
2. **Remove the Resend email integration and the WhatsApp `wa.me` link feature** — scoped in the "Removal scope" box in §4 above. Small, isolated, backend + frontend cleanup PR. Do this *before* building push notifications so the new delivery mechanism isn't designed alongside dead code still in the diff.
3. **Web Push notifications** — per the separate `PWA-PUSH-AUDIT.md` audit (migration order already proposed there: `V3__*.sql` for subscription/outbox tables → new `app/notification` bounded context following the `aijob` shape → hook points inside `GameChronicleService.advance()`/`completeRun()` and `InvitationService.acceptForUser` → a `@Scheduled` dispatcher mirroring `AiJobProcessor` → frontend manifest/service worker/subscribe UI → security headers as a distinct step). This is now the sole channel for invitation and game-event delivery beyond the existing in-app link/preview flow.
4. **Voting functionality** — the largest lift: new entities/table(s), new enum(s), new endpoints, new authorization checks (reuse `ChronicleAccessService`/`PartyAccessService`), and must reference `segment_id`/`turn_id` rather than `sequence_number` given the renumbering behavior documented in §6. Design the schema and access rules before touching `GameChronicleService`, since it's the most invariant-heavy part of the codebase (CLAUDE.md's Domain Invariants section is almost entirely about this bounded context). If Web Push (item 3) has already landed, a "someone voted" notification is a natural additive hook into the same dispatcher rather than a new delivery mechanism.
5. **AI generation for written chronicles** (if desired, to close the game/written asymmetry noted in §7) — only after voting is settled, in case voting introduces new triggers into `WrittenChronicleService.publish()` that should be designed together with the AI-enqueue call rather than bolted on twice.

Registration-copy changes, if any are actually requested beyond what's audited in §1, are trivial frontend string edits and can ride along with whichever feature touches that screen — no dedicated slot needed.

**Explicitly out of scope going forward:** WhatsApp Business API sending and any further investment in the Resend email sender. Both are superseded by the Web Push plan above.
