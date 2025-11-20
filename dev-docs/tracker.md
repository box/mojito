Product & Tech Tracker
======================

Purpose
- Capture improvement ideas and technical debt so we can prioritize intentionally instead of chasing every idea immediately.
- Treat this as a lightweight backlog: write down the idea, rationale, and any quick notes. Pull items into workstreams as capacity allows.

How to use
- Add new entries to the bottom with a unique identifier (e.g., `FRONTEND-01`).
- Keep notes concise: what problem it solves, rough scope/impact, and dependencies.
- When an item becomes active, move it to a dedicated design doc (see `dev-docs/design/`) or ticket system.

Backlog
- FRONTEND-01 — Add ESLint/Prettier/Vitest to `webapp/new-frontend` for consistent code quality.
- DEV-01 — Evaluate upgrading repository-wide Node version to 22 LTS and update Maven/legacy UI accordingly.
- DEV-02 — Replace `npm install` with `npm ci` in Maven plugin executions for reproducible builds.
- AUTH-01 — Figure out OSS-friendly auth story for static assets + APIs without Cloudflare: likely a tiny reverse proxy that terminates auth and sets a secure cookie for `/` and `/n/**`; dev can mimic edge by injecting `CF-Access-Jwt-Assertion`/`Authorization` headers via Vite proxy; document requirements (covers assets, avoids CSRF if cookies used) so we don’t restart from scratch.
