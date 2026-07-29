# Narrative Platform API

Spring Boot / Java 21 backend for party-scoped accounts, invitations, written chronicles, hidden game-story turns, moderation, edit locks and AI-generated final tales.

Read `CLAUDE.md` before changing code. Local execution and builds are performed through IntelliJ IDEA, not Maven or Java CLI commands.

## Main bounded contexts

- `auth`: username/password accounts and JWT issuance.
- `party`: parties, memberships and party-scoped roles.
- `invitation`: one-use invitation tokens and sharing integrations.
- `chronicle`: written and game chronicles, turn visibility, locks and moderation.
- `aijob`: idempotent OpenAI generation jobs.

Flyway owns the database schema in `src/main/resources/db/migration`.

## Mid-game party joins

Accepting an invitation or reactivating a disabled member inserts that person into the current cycle of every `IN_PROGRESS` game chronicle in that party (at the end of the cycle's turn order), and into the same relative position for any remaining cycles. Members who already had a turn in a cycle (e.g. skipped or expired earlier) are left untouched. See `GameChronicleService.insertPartyMemberIntoActiveRuns`.

## Optional AI configuration

`OPENAI_API_KEY` is optional. Without it, the API starts normally and automatic AI jobs remain pending. Explicit regeneration requests fail with HTTP 503 and the `ai_not_configured` problem code. The project uses the Spring Boot Flyway and RestClient starters so database migrations and `RestClient.Builder` auto-configuration are available on startup.
