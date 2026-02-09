Mojito New Frontend Shell (React + Vite)
=======================================

Context
- We are introducing a new frontend that coexists with the legacy UI.
- It should be production-ready, incrementally extensible, and deploy inside the existing Spring Boot app.

Decisions
- Stack: React + Vite + TypeScript.
- Served path: `/n/` (Vite `base: '/n/'`).
- Output path: `webapp/target/classes/public/n` so Spring Boot serves it from the classpath.
- Build integration: `frontend-maven-plugin` in `webapp/pom.xml` runs `npm install` and `npm run build` inside `webapp/new-frontend` during `compile`.
- Dev proxy: Vite proxies `/api/*` to `http://localhost:8080`.
- Server forwarding: `NewFrontendController` forwards `/n` and client-side `/n/**` (except assets/files) to `/n/index.html` so deep links load the SPA.
- Security: `/n/**` is allowlisted in `WebSecurityJWTConfig` so the SPA can load under stateless JWT mode.

Dev Workflow
- Backend: `cd webapp && mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.config.additional-location=optional:file://$HOME/.l10n/config/webapp/ -Dspring.profiles.active=$USER,npm -Duser.timezone=UTC"`
- Frontend: `cd webapp/new-frontend && npm install && npm run dev` (default `http://localhost:5173`).
- Packaged app: `http://localhost:8080/n/`.

Implementation Notes
- Location: `webapp/new-frontend` with `vite.config.ts` configured to write into `../target/classes/public/n`.
- Keep all new UI assets under this folder; do not mix with legacy webpack output.
- Ensure `.gitignore` excludes Node artifacts globally (`**/node_modules/`, `**/.vite/`, etc.).

Open Questions / Next Steps
- Routing strategy and auth integration for `/n/` paths.
- Add ESLint/Prettier/Vitest.
- Establish UI component conventions and theming.
