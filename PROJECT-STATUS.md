# MVP implementation status

## Implemented

- Public narrator account registration using username and password.
- Existing-account login and account creation through a one-use party invite.
- Party-scoped OWNER, NARRATOR and PLAYER memberships.
- Party creation, member disable/reactivate/remove and ownership transfer.
- Individual expiring invitations, manual link copy, prefilled WhatsApp sharing and optional Resend email delivery.
- Arquivo do Cronista dashboard with uniform published/in-progress cards.
- Written chronicles with editor grants/revocations, 24-hour exclusive edit locks, optimistic content versions, publish and archive.
- Game chronicles with all active party members, creator-first fixed shuffled order, one to three repeated cycles, 24-hour turns, private drafts, publish, skip, clear and automatic expiry.
- Backend-only hidden segment enforcement and visual size categories that account for text length and line breaks.
- Viewer visibility for own segments and their predecessor segments across cycles.
- Ten-second narrator reveal, narrator moderation, segment editing, disable reason, restore and gray removed blocks.
- Backend AI job queue with retries, stale-job recovery, token recording, regeneration and generated-story version history.
- Current generated story above the complete thread after publication.
- Arcane Editorial Linen visual system and configurable public app name.
- Flyway database migration, Docker backend deployment and Portuguese setup guide.

## Deliberately deferred

- Co-narrator promotion workflow. The role exists in the model, but promotion UI/API is reserved for the future requirement.
- Realtime notifications, push notifications and automatic WhatsApp Business sending.
- A full invitation history screen. The newly created invitation can be copied or revoked immediately.
- Billing, subscriptions, organization accounts and commercial administration.
- Rich-text or Markdown editors, image uploads and campaign-specific themes.
- Automated cost conversion for AI calls. Token usage and model are stored so a cost screen can be added without changing generation history.
- Production-grade refresh tokens, email recovery and multi-factor authentication.

## Validation note

The source tree received static syntax, structure, JSON, YAML and XML checks in the generation environment. Frontend dependency installation timed out against the npm registry, so `npm run lint` and `npm run build` must be run locally. In accordance with the requested project rules, Maven and Java CLI commands were not executed; compile and tests must be run from IntelliJ IDEA.

## Interaction and account refinements

- Mobile navigation now uses a permanent compact icon rail plus a hamburger drawer.
- Every party and chronicle screen includes a direct route back to the parent list.
- The signed-in `@username` is shown in the application shell.
- Users can change their password from the account dialog.
- Party owners can promote players to narrator and demote narrators to player.
- Ownership transfer now makes the former owner a player, preventing stale narrator authority.
- Disabled membership is temporary; removed membership requires a new invitation.
- Game chronicles are created with the opening fragment in the same form as title and cycle count.
- The active writer sees the composer first, followed by the immediately preceding visible fragment and then the full protected thread.
- Skipped and expired turns receive a fully gray visual treatment.
- Hidden thread cards keep the real content server-side; only an easter-egg message exists beneath the visual cover.
- The backend starts without OpenAI configured. Automatic AI work waits, and manual regeneration returns a clear 503 response.
