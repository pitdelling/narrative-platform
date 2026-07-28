# CLAUDE.md

This file defines the project-specific guidance for the Narrative Platform Spring Boot API. Read the parent/global `CLAUDE.md` first when one exists, then apply this file.

## Project Overview

This API powers a multi-party narrative RPG platform. The first module is `Arquivo do Cronista`. Authentication uses username and password. Roles are scoped to a party, invitations are one-use and expiring, game-story segments are hidden by the backend, and AI generation is performed only by this service.

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

- A user has no global player role. Membership roles exist only inside a party.
- Public registration creates a user capable of creating narrator-owned parties.
- Player membership can only be created by accepting a valid invitation.
- Invitation tokens are stored only as hashes, are one-use, and expire.
- Game-story ordering is generated and persisted by the backend. The creator is first; remaining active participants are shuffled once. The same order repeats for every cycle.
- A game can have one, two or three cycles.
- Game creation requires the creator's opening segment. The opening turn is persisted as submitted during creation, and the next participant becomes active immediately.
- A participant can submit only when it is their active turn.
- Non-visible segment content must never be serialized by the backend. Placeholder metadata is allowed.
- During an active game, a viewer can see their own submitted segments and the predecessor of each of those segments. The current participant can additionally see the latest preceding submitted active segment.
- Narrator reveal is permission-based. The frontend hides it after ten seconds, but the backend treats revealed data as intentionally disclosed to the narrator.
- Published threads show disabled segments in gray, with `Removed by narrator` and an optional reason.
- `DISABLED` membership is temporary and can be reactivated. `REMOVED` membership is permanent for that membership lifecycle and requires a new invitation; historical attribution remains.
- Only the owner can promote a player to narrator or demote a narrator to player. Transferring ownership demotes the former owner to player.
- AI input includes only active or edited segments, never disabled segments.
- Skip and expiry remove the participant only from that turn. They can participate in later cycles.
- Written chronicles use an exclusive 24-hour editing lock. Saves with an expired or mismatched lock token must be rejected.
- AI generation is idempotent per job and performed only by the backend.
- The application must start without an OpenAI API key. Automatic jobs remain pending, while explicit regeneration requests return `503 Service Unavailable` with `ai_not_configured`.
- Authenticated users can change their own password by providing the current password and a different valid new password.

## Security Rules

- Passwords use Argon2id through Spring Security.
- JWT signing secrets must come from configuration and never be committed.
- OpenAI and Resend secrets are backend-only.
- Controllers must never accept a user ID as proof of identity; derive the current user from the security context.
- Every party resource access must validate active membership and role.
- Do not expose disabled users' protected content.

## Build & Run

Never suggest or run Maven or Java CLI commands. The developer uses IntelliJ IDEA:

- Maven panel → Lifecycle → clean/test/package.
- Build → Build Project.
- Run `NarrativePlatformApplication` through an IntelliJ Spring Boot run configuration.

Docker and hosting configuration may use Maven internally during remote builds; this restriction applies to local development instructions and Claude execution.

## Documentation Maintenance

Update `README.md` and the root Portuguese setup guide whenever a meaningful architectural, behavioral, integration, scheduling or deployment change is made.
