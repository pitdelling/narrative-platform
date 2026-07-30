# CLAUDE.md

This file defines project-specific guidance for the Narrative Platform Next.js frontend. Read the parent/global `CLAUDE.md` first, then apply this file.

## Local-only frontend preview sandbox

`frontend/app/dev-preview/page.tsx` is a standing, git-ignored sandbox route (see the root `.gitignore` entry `frontend/app/dev-preview/`). It renders real components (e.g. `ThemeToggle`, `DragonPanel`, the invite panel) with mock/fake data, without `AppShell` and without any `api()` call, so frontend changes can be checked visually in the browser without a running backend or a logged-in session.

Rules for this route:

- It must never be committed. It is intentionally excluded from git so it can be freely rewritten per session without polluting history or ever shipping to production.
- It is regenerated on demand through the `/fe-preview` slash command (`.claude/commands/fe-preview.md`), which rebuilds it to reflect whatever the current session needs to visually validate.
- It must keep mirroring current real components with mock data — never reintroduce `AppShell`, authentication, or real `api()` calls into it.
- If the file exists locally but is stale relative to the components it demos, prefer regenerating it via `/fe-preview` rather than hand-patching it.
