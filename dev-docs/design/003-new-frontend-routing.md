New Frontend Routing + Shell Layout
===================================

Context
- The new frontend only rendered a bare headline; we need routing scaffolding to grow feature pages.
- SPA lives under `/n`, so client routing must respect that base path.

Goals
- Adopt React Router (v7) with `BrowserRouter` basename `/n` to align with server routing and deep links.
- Introduce a shared shell layout with top navigation and routed content slots.
- Provide initial routes for Repositories and Workbench with sensible defaults/redirects.
- Establish baseline theming tokens and page structure CSS for light/dark support.

Non-goals
- No data fetching, auth, or real page content yet—routes are placeholders.
- No design system or component library decisions; styles are minimal tokens/layout only.
- No 404/500 UX polish beyond redirecting unknown paths.

Plan (narrow)
- Add `react-router-dom@^7` dependency.
- Wrap `App` in `BrowserRouter` (basename `/n`), define `Routes`/`Route` tree with a shared `AppLayout` using `Outlet`.
- Provide nav links for `/repositories` and `/workbench`; redirect `/` and fallthrough paths to `/repositories`.
- Add `app.css` with CSS variables (colors, spacing, radius) and shell layout styles, and import it in `App.tsx`.

Open questions
- Should navigation highlight active routes or adopt a design-system component?
- Do we want per-route code splitting once pages grow?
- Where should shared tokens live long-term (global CSS vs. theming system)?
