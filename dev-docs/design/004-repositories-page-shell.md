Repositories Page Shell
=======================

Context
- New frontend needs a repositories overview page with a locales pane; current implementation is mock-only but establishes layout and styling boundaries.
- CSS was leaking globally; we adopted page-scoped class naming and consolidated table styles for maintainability.

Goals
- Provide a two-pane layout (repositories table + locales table) with sticky headers and a divider.
- Keep styles page-scoped with a clear naming scheme and shared tokens (header height, spacing, colors).
- Include basic UX affordances: selectable rows, inline search input in the repositories header, numeric alignment for metrics.
- Add gentle scroll snapping tuned for the sticky header without clipping the first row.

Decisions
- Page-scoped BEM-ish classes (`repositories-page__*`) instead of wrapper-fenced selectors; drop unused modifiers.
- Shared header height via `--repositories-page-header-height` reused for sticky header height and `scroll-padding-top`.
- Scroll snapping per scrollable section (`scroll-snap-type: y mandatory`, rows `scroll-snap-align: start`).
- Consolidated table styles: common header/cell rules, `--searchable` header modifier, `--number` for numeric alignment, `--selected` row state.

Non-goals
- No data fetching, metric links, or keyboard navigation yet (mock data only).
- No virtualization or infinite loading; tables render all rows for now.
- No detail sidebar; locales are a separate table fed from mock selection.

Open Questions / Next Steps
- Replace mock data with real fetch (React Query) and selection-driven locale details; add loading/error/empty states.
- Introduce virtualization for large lists and scroll-to-index on selection.
- Decide whether locales live in a side pane versus an expandable row pattern; add metric links to workbench routes.
- Add accessibility polish (scope="col" on headers, focus/keyboard handling for row selection).
