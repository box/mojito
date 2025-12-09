Repositories Page Container (FRONTEND-02a)
=========================================

Context
- Repositories page currently renders mock data with a static layout (see `004-repositories-page-shell.md`).
- Next step is to add a container that owns data loading, filtering, selection, keyboard nav, and virtualization while keeping the view pure/presentational.

Goals
- Fetch repositories via React Query and expose a filtered list keyed by search text.
- Maintain selection state, handle click + keyboard navigation (↑/↓), and scroll the virtualized list to the selected row.
- Wire tanstack/react-virtual (or similar) for repositories + locales lists with size/measurement hooks configurable from the container.
- Keep the view dumb: pass props for repositories, selection, locale details, search value/handlers, and metric link callbacks.
- Provide loading/error/empty states to the view without coupling to query specifics.

Scope (02a)
- Implement container logic and data contracts; no visual restyle beyond what the view consumes.
- Add dependencies/config for React Query + virtualization if missing.
- Leave locales pane mocked in the view; container only wires the selected repo to locale details once available.

Architecture / Data Flow
- Container component (e.g., `RepositoriesPageContainer`) lives under `src/page/repositories/` and wraps the existing view.
- React Query client is assumed to be provided at app root; if absent, add a lightweight provider in `App.tsx`.
- Query: `useQuery(['repositories', searchValue], fetchRepositories)` with optional `name` filter; debounce search input in container to reduce network chatter.
- Repository shape: rely on API `/api/repositories?name=<search>` (existing REST client path "repositories"); expect counts either in payload or to be added later via an additional stats endpoint.
- Filtering: if API returns full list, apply client-side name substring filter as a fallback; prefer server-side name parameter when supported.
- Selection: keep `selectedRepositoryId` in state; default to first result when data loads; update on click/keyboard; expose `onSelectRepository(id)` to view.
- Locales: container derives `localeDetails` from selected repository data if present; otherwise stub empty array and expose an `onLoadLocales(repoId)` hook for 02c.

Virtualization Plan
- Use `@tanstack/react-virtual` `useVirtualizer` for both lists; container owns configs:
  - `estimateSize` (e.g., 48px rows), `overscan` (e.g., 8), and optional `measureElement` for dynamic heights.
  - Provide `virtualizer` results to the view as render data: total size + virtual items to map into rows.
- Scroll-to-selected: when `selectedRepositoryId` changes, compute its index in filtered data and call `virtualizer.scrollToIndex(index, { align: 'center' | 'nearest' })`.
- The view receives `virtualItems` and uses absolute positioning via inline styles from the virtualizer; container keeps DOM refs for measurement/scroll elements.

Keyboard Navigation
- Container attaches `onKeyDown` handler to the table wrapper or selected row:
  - ArrowUp/ArrowDown move selection within filtered list.
  - Enter triggers `onSelectRepository` no-op (selection already updates), reserved for future navigation.
  - Prevent default scrolling when handling arrows; ensure selected row has `tabIndex=0` passed to view so focus is stable.
- Keep focus management simple: when selection changes programmatically, focus the corresponding row element via ref map or a single ref + `data-selected` query.

State/Props Contract to View
- `repositories`: filtered array with `{ id, name, rejected, needsTranslation, needsReview, selected }` (selected derived in container).
- `selectedRepositoryId`: string/number.
- `onSelectRepository(id)`.
- `searchValue`, `onSearchChange(value)`, optional `isSearching` boolean for spinner.
- `loadingState`: `loading` | `error` | `empty` | `ready` for repositories list.
- `localeDetails`: array `{ tag, displayName, rejected, needsTranslation, needsReview }` derived from selection or empty.
- `virtualConfig`: refs + virtualizer outputs for repos/locales (estimate/measure fns passed through but defaulted in container).
- Metric link callbacks: placeholder callbacks supplied by container for now; real URLs wired later.

Edge Cases / Empty States
- Loading: show loading state on first fetch; retain previous data while refetching (React Query `keepPreviousData`).
- Error: surface `error` state with retry handler that calls `refetch`.
- Empty search result: `ready` but empty list; selection cleared; locales pane stays empty.

Risks / Open Questions
- Repository payload may not include locale stats; 02c will define `onLoadLocales` and caching strategy.
- Need to confirm REST API shape for counts; may require a dedicated stats endpoint to avoid N+1 locale fetches.
- Keyboard focus management should avoid fighting virtualization re-renders; may need a stable key-to-ref map.

Milestones
- 02a (this doc): land container + React Query + virtualization hooks and wire to existing view props.
- 02b: swap view to pure virtualized tables and styling adjustments.
- 02c: locale data loading, empty/error states, and accessibility polish.
