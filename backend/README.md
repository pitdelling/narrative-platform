# Narrative Platform API

Spring Boot / Java 21 backend for party-scoped accounts, invitations, written chronicles, hidden game-story turns, moderation, edit locks and AI-generated final tales.

Read `CLAUDE.md` before changing code. Local execution and builds are performed through IntelliJ IDEA, not Maven or Java CLI commands.

## Main bounded contexts

- `auth`: username/password accounts and JWT issuance.
- `party`: parties, memberships and party-scoped roles.
- `invitation`: one reusable invitation link per party, regenerated on demand by a narrator or the owner.
- `chronicle`: written and game chronicles, turn visibility, locks and moderation.
- `aijob`: idempotent OpenAI generation jobs.

Flyway owns the database schema in `src/main/resources/db/migration`.

## Mid-game party joins

Accepting an invitation or reactivating a disabled member inserts that person into the current cycle of every `IN_PROGRESS` game chronicle in that party (at the end of the cycle's turn order), and into the same relative position for any remaining cycles. Members who already had a turn in a cycle (e.g. skipped or expired earlier) are left untouched. See `GameChronicleService.insertPartyMemberIntoActiveRuns`.

## Reusable party invitation link

Each party has exactly one active invitation link (`GET /api/parties/{partyId}/invitation`), visible and regenerable only by an active narrator or the owner (`POST /api/parties/{partyId}/invitation/regenerate`). The link stays valid, and reusable by any number of people, until it is explicitly regenerated — regeneration invalidates the previous link immediately and atomically. Unauthenticated visitors resolve it through the existing public endpoints (`GET /api/invites/{token}` preview, `POST /api/invites/accept`, `POST /api/auth/register-from-invite`); re-accepting while already an active member is idempotent. The backend never sends email or WhatsApp messages — sharing the link is entirely the narrator's responsibility. See `InvitationService` and `PartyInvitationLinkEntity`'s Javadoc for the token-storage security trade-off.

## Optional AI configuration

`OPENAI_API_KEY` is optional. Without it, the API starts normally and automatic AI jobs remain pending. Explicit regeneration requests fail with HTTP 503 and the `ai_not_configured` problem code. The project uses the Spring Boot Flyway and RestClient starters so database migrations and `RestClient.Builder` auto-configuration are available on startup.
