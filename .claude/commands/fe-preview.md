---
description: Regenerate the local, git-ignored frontend/app/dev-preview/page.tsx sandbox with mock data to visually check frontend changes without a backend.
---

Regenerate `frontend/app/dev-preview/page.tsx` as a standalone, backend-free preview route for visually validating frontend work in the browser.

Rules to follow:

- This route is permanently git-ignored (`frontend/app/dev-preview/` in the root `.gitignore`) — never remove it from `.gitignore`, and never `git add`/commit this file.
- No `AppShell`, no authentication/session logic, no `api()` calls from `frontend/lib/api.ts`. The page must render without a running backend and without a logged-in user.
- Reuse the real, current components relevant to what's being previewed (e.g. `ThemeToggle`, `DragonPanel`, or whatever component the user is currently working on) and feed them with local mock/fake data defined inline in the page.
- If `$ARGUMENTS` names specific components, screens, or recent changes, focus the regenerated page on demoing those; otherwise default to covering whatever frontend components changed most recently in the working tree.
- Keep it a single self-contained client component file (`"use client"`), same as the existing pattern: fake constants for data, small local demo sections with headings, and resize/width variants when layout behavior (truncation, wrapping, responsiveness) is relevant to what changed.
- After writing the file, tell the user to run the frontend dev server and open `/dev-preview` to check it — do not claim it was visually verified unless it was actually opened and inspected.
