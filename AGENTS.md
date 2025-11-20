Agent Guide (Codex assistants)
==============================

Purpose
- Document conventions and expectations for assistants working in this repo.

General Guidance
- Default to minimal, purposeful changes; avoid gratuitous complexity.
- Keep docs and tracker entries in sync with work you land.

When you see "finalize commit"
- Review all touched code for the simplest implementation that meets the task; suggest simplifications if possible.
- Update relevant docs (e.g., design notes, README snippets) to reflect the change.
- Update `dev-docs/tracker.md`: add new items or remove items that are done/no longer relevant (no "Done" section).
- Propose a concise commit message that fits the change.

Notes
- For frontend tooling, reuse the Maven-managed Node/npm (see `webapp/use_local_npm.sh`).
- New frontend lives under `webapp/new-frontend` and is served at `/n/`.
