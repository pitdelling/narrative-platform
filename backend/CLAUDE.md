# CLAUDE.md

This file defines the project-specific guidance for the Narrative Platform Spring Boot API. Read the parent/global `CLAUDE.md` first when one exists, then apply this file.

## Project Overview

This API powers a multi-party narrative RPG platform. The first module is `Arquivo do Cronista`. Authentication uses username and password. Roles are scoped to a party, each party has one reusable invitation link that a narrator can regenerate at will, game-story segments are hidden by the backend, and AI generation is performed only by this service.

## Package Structure

The mandatory root package is `com.narrativeplatform`.

```text
com.narrativeplatform
├── app
│   └── {boundedContext}
│       ├── controllers
│       ├── models
│       │   ├── dtos
│       │   ├── entities
│       │   ├── requests
│       │   ├── responses
│       │   ├── mappers
│       │   ├── commands
│       │   ├── projections
│       │   ├── enums
│       │   └── {additional domain grouping when justified}
│       ├── repositories
│       └── services
├── configuration
├── security
└── shared
    ├── constants
    ├── exceptions
    ├── integrations
    └── utils
```

Bounded contexts currently include `auth`, `party`, `invitation`, `chronicle`, and `aijob`.

## Implementation Standards

- All code, comments and this file must be in English.
- All parameters and local variables must be `final` unless reassigned.
- Use `var` for local variables when the type is obvious from the right-hand side. Never use `var` for fields.
- Prefer `record` for immutable requests, responses, DTOs, commands, results and projections.
- Use Lombok for entity boilerplate and non-record non-Spring classes.
- Use `@RequiredArgsConstructor` for constructor injection.
- Never place `@Value` directly on a field. Bind configuration through `@ConfigurationProperties` whenever possible. If `@Value` is unavoidable, inject it through a constructor into a `final` field.
- Private constructors must use `@NoArgsConstructor(access = AccessLevel.PRIVATE)`.
- All `@PathVariable` annotations must include the parameter name.
- Use `ProblemDetail` for API errors.
- Every `@Configuration` must use `@Configuration(proxyBeanMethods = false)`.
- A `@Bean` method must receive other beans as parameters instead of calling sibling `@Bean` methods.
- Avoid magic strings, numbers and booleans. Keep constants local unless shared semantically across classes.
- Follow KISS, guard clauses, early returns and flat control flow.
- Minimize N+1 queries. Prefer explicit fetch queries/projections for list and detail views.
- Transactions belong in repositories when a single persistence operation is sufficient. Use a service-level transaction when a use case coordinates multiple persistence operations.

## Mapping Rules — Project Override

Override of the generic parent mapper rule:

- Simple conversions belong as public instance methods on the source record/class whenever they need no injected dependency.
- Do not create a mapper merely to rename fields.
- Create a mapper in `models/mappers` only when the transformation is too large for the source type or requires injected Spring-managed collaborators.
- A mapper that has no dependency must be `final`, use `@NoArgsConstructor(access = AccessLevel.PRIVATE)`, and expose only public static conversion methods.
- A mapper that needs dependencies must be a Spring `@Component`, use `@RequiredArgsConstructor`, and expose public instance methods.
- Prefer one mapper per bounded context. Split it only when it becomes objectively large.

## Naming Conventions

- `responses/` classes end in `Response`.
- `requests/` classes end in `Request`.
- `dtos/` classes end in `DTO`.
- `commands/` input classes end in `Command`; outputs end in `Result`.
- `mappers/` classes end in `Mapper`.
- `projections/` classes/interfaces end in `Projection`.
- `enums/` classes end in `Enum` or `Type`.
- JPA classes end in `Entity`.

## Domain Invariants

- A user has no global player role. Membership roles exist only inside a party. Roles are `OWNER`, `NARRATOR`, `PLAYER`, and `SPECTATOR` — a spectator can view every chronicle but is never selected as a game-chronicle participant and never receives turns.
- Public registration creates a user capable of creating narrator-owned parties.
- Party membership can only be created by accepting a valid invitation; the invitation link's target role determines the new member's initial role (`PLAYER` or `SPECTATOR`).
- Each party has exactly one reusable invitation link **per target role** (`PLAYER` and `SPECTATOR`), each identified by its own opaque bearer token, enforced by `UNIQUE(party_id, target_role)`. Only an active narrator or the owner may view or regenerate either link; regenerating one invalidates its previous token immediately and never leaves more than one active link per (party, target role). The raw token is persisted (not hash-only) so it can be redisplayed to authorized viewers on demand; `token_hash` is what the public resolution/acceptance endpoints actually query against, so that path never needs read access to the raw value. This is a deliberate trade-off — see `PartyInvitationLinkEntity`'s Javadoc.
- Game-story ordering is generated and persisted by the backend, from active non-spectator members only. The creator is first; remaining active participants are shuffled once. The same order repeats for every cycle.
- A game can have one, two or three cycles.
- Game creation requires the creator's opening segment. The opening turn is persisted as submitted during creation, and the next participant becomes active immediately.
- A participant can submit only when it is their active turn.
- Non-visible segment content must never be serialized by the backend. Placeholder metadata is allowed.
- During an active game, a viewer can see their own submitted segments and the predecessor of each of those segments. The current participant can additionally see the latest preceding submitted active segment.
- Narrator reveal is permission-based. The frontend hides it after ten seconds, but the backend treats revealed data as intentionally disclosed to the narrator.
- Published threads show disabled segments in gray, with `Removed by narrator` and an optional reason.
- `DISABLED` membership is temporary and can be reactivated. `REMOVED` membership is permanent for that membership lifecycle and requires a new invitation; historical attribution remains.
- Only the owner can change a member's role between `PLAYER`, `NARRATOR` and `SPECTATOR`. Transferring ownership demotes the former owner to player. Converting a member to `SPECTATOR` removes them from every in-progress game chronicle's remaining turns (past segments are preserved); converting a spectator back to `PLAYER`/`NARRATOR` reinserts them into those same chronicles' remaining cycles, exactly like a reactivation.
- AI input includes only active or edited segments, never disabled segments.
- Skip and expiry remove the participant only from that turn. They can participate in later cycles.
- Written chronicles use an exclusive 24-hour editing lock. Saves with an expired or mismatched lock token must be rejected.
- Story content (`game_segments.content`, `game_drafts.content`, `written_story_documents.content`) is sanitized HTML, not plain text — the frontend's rich-text editor produces a constrained subset (`b/strong/i/em/u/s/ul/ol/li/p/br`, plus `span`/`p` carrying only `color`, `background-color` and `margin-left` style declarations, and `ol` carrying only a `data-kind` of `decimal`/`alpha` for the alternating smart-list numbering). `color`/`background-color` values must be accepted in both `#rrggbb` hex and `rgb(r, g, b)` form — the browser re-serializes a hex value set through the editor's color picker into `rgb(...)` when the DOM round-trips it, so rejecting that form silently drops every color on save. `RichTextSanitizer`'s `style` policy validates each CSS declaration independently rather than the whole attribute value at once, so one disallowed declaration never drops a co-located valid one. Every write path must sanitize through `shared/utils/RichTextSanitizer.sanitize()` before persisting; nothing downstream may trust unsanitized input. AI prompt builders must use `RichTextSanitizer.toPlainText()` instead of the raw content, both to avoid feeding markup to the model and to avoid wasting tokens.
- AI generation is idempotent per job and performed only by the backend.
- The application must start without an OpenAI API key. Automatic jobs remain pending, while explicit regeneration requests return `503 Service Unavailable` with `ai_not_configured`.
- Authenticated users can change their own password by providing the current password and a different valid new password.

## Security Rules

- Passwords use Argon2id through Spring Security.
- JWT signing secrets must come from configuration and never be committed.
- OpenAI secrets are backend-only.
- Controllers must never accept a user ID as proof of identity; derive the current user from the security context.
- Every party resource access must validate active membership and role.
- Do not expose disabled users' protected content.
- Invitation tokens are never logged and never appear in error messages; exception messages on the resolution path stay generic (e.g. `"Invitation not found."`).

## Logging

- Use Lombok `@Slf4j` for logging (already the pattern in `GlobalExceptionHandler`); never use `System.out`/`System.err`.
- Key business flows (turn/run lifecycle in `GameChronicleService`, AI job lifecycle in `AiJobService`/`AiJobStateService`/`AiJobProcessor`/`AiJobWorker`) log at `debug`/`info` for normal flow and `warn`/`error` for failures. Never log secrets, tokens, or full request bodies.
- Production `application.yml` has no `logging.level` override, so these stay silent by default (Spring Boot's default root level is `INFO`, and none of the above use `info` for routine per-request noise).
- To see them locally, set `logging.level.com.narrativeplatform: DEBUG` in `application-local.yml` (already the developer's personal, git-ignored local profile — the line is present there by default) or export `LOGGING_LEVEL_COM_NARRATIVEPLATFORM=DEBUG` for any other environment.

## Build & Run

The developer's day-to-day workflow is IntelliJ IDEA:

- Maven panel → Lifecycle → clean/test/package.
- Build → Build Project.
- Run `NarrativePlatformApplication` through an IntelliJ Spring Boot run configuration.

For Claude Code console sessions, the developer has no globally-installed JDK or Maven. Portable, version-pinned toolchains are staged under `C:\Users\Philip Delling\.raftech\develop\` and must be used instead of installing anything system-wide:

- `develop\jdks\<name>` — portable JDKs, one folder per version (e.g. `jdk-21.0.8`, which matches this project's required Java 21). IntelliJ itself already uses this same folder as its JDK cache.
- `develop\mvns\mvn-<version>` — portable Maven distributions, one folder per version (e.g. `mvn-3.9.11`, matching the version pinned in `backend/Dockerfile`'s build stage).

Rules for using this toolchain:

- Never install a JDK/Maven system-wide and never modify the system or user `PATH`/`JAVA_HOME` permanently. Set `JAVA_HOME` and prepend the chosen Maven's `bin` directory to `PATH` for a single terminal session only, or invoke the executables by their full path directly.
- If the JDK or Maven version a task needs is not already present under `develop\jdks\` or `develop\mvns\`, stop and ask the user which version to fetch — never download one silently.
- **Always ask the user for confirmation before actually running any `mvn`/`java` command — including read-only ones like `mvn -v` — even when operating in auto-mode.** This is a hard exception to auto-mode's normal bias toward proceeding without stopping.
- This portable toolchain may be used both to run the test suite and to start the application locally when a console genuinely needs to execute or verify backend behavior, not just read the code — subject to the confirmation rule above.

Docker and hosting configuration may use Maven internally during remote builds; that is unaffected by any of the above.

## Documentation Maintenance

Update `README.md` and the root Portuguese setup guide whenever a meaningful architectural, behavioral, integration, scheduling or deployment change is made.
