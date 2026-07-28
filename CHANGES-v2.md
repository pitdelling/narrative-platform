# Changes v2

## Frontend

- Added mobile hamburger drawer and compact shortcut rail.
- Added direct navigation back to party list and chronicle list.
- Added current `@username` display and password-change dialog.
- Clarified temporary disable versus permanent removal in member management.
- Added owner-only narrator promotion and demotion controls.
- Added opening-fragment field to game-chronicle creation and direct navigation to the created thread.
- Reworked game thread page: active composer first, previous visible message second, full thread afterward.
- Added covered easter egg inside hidden thread placeholders without sending protected content.
- Added full gray styling for skipped and expired turns.

## Backend

- Added authenticated password-change endpoint.
- Added owner-only party role update endpoint.
- Ownership transfer now demotes the previous owner to player.
- Removed members cannot be directly reactivated and must accept a new invitation.
- Game creation persists the creator's opening segment and activates the next turn immediately.
- Added optional-AI behavior with a dedicated `ai_not_configured` 503 response for explicit generation.
- Added Spring Boot Flyway and RestClient starters required by Spring Boot 4.1.
