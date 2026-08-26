# mojito-mcp architecture: CLI-backed MCP server

## Goal

Expose curated Mojito REST capabilities to AI agents (Cursor MCP) by shelling out to the **Mojito CLI `api` command**, not by reimplementing HTTP or auth in Node.

If `mojito-prod api …` (or `mojito-dev api …`) works in a developer’s shell, the MCP tools for that environment work the same way.

## Non-goals

- Reimplementing Mojito authentication, cookies, MSAL, or host configuration in the MCP process.
- Exposing a generic “raw API” tool or `mojito api --spec` as an MCP tool (`--spec` remains a human/dev aid when authoring tools and skills).
- Auto-waiting on pollable tasks for the initial tool set (see [Wait / timeout](#wait--timeout)).

## Architecture

```
Cursor (MCP host)
  └─ mojito-mcp (stdio transport)
       └─ MojitoCliClient
            └─ spawn(MOJITO_CLI, ["api", …])  // stdout/stderr captured
                 └─ Mojito CLI (AuthenticatedRestTemplate + local CLI config)
                      └─ Mojito REST (/api/…)
```

Tool registration (`register-tools.ts`) stays as the MCP surface. Handlers call a CLI-backed client instead of a stub HTTP client.

### Why the CLI

- Auth and instance URL live in the engineer’s existing CLI setup (e.g. Spring config under `~/.l10n/…`).
- No `MOJITO_AUTH_TOKEN` or base URL secrets in Cursor MCP env.
- Pagination, JSON field encoding, and pollable-task helpers already exist on `mojito api`.
- Prod vs dev is the same dual-script pattern developers already use.

## Prod vs dev (dual script + dual MCP server)

Developers maintain two CLI entry points on `PATH`:

| Script        | Typical use                          |
|---------------|--------------------------------------|
| `mojito-prod` | Real data (linguistic bugs, prod)    |
| `mojito-dev`  | Feature development / experimentation |

The MCP process does **not** know about “prod” or “dev” as first-class concepts. It only runs whatever binary `MOJITO_CLI` names.

**Defaults**

| Variable / concept | Default        |
|--------------------|----------------|
| `MOJITO_CLI`       | `mojito-prod`  |
| Dev script name    | `mojito-dev` (documented; set explicitly in Cursor) |

**Cursor configuration:** two MCP server entries, same Node package, different `MOJITO_CLI`:

```json
{
  "mcpServers": {
    "mojito-prod": {
      "command": "node",
      "args": ["/absolute/path/to/mojito/mojito-mcp/dist/index.js"],
      "env": {
        "MOJITO_CLI": "mojito-prod",
        "MOJITO_CLI_TIMEOUT_MS": "600000"
      }
    },
    "mojito-dev": {
      "command": "node",
      "args": ["/absolute/path/to/mojito/mojito-mcp/dist/index.js"],
      "env": {
        "MOJITO_CLI": "mojito-dev",
        "MOJITO_CLI_TIMEOUT_MS": "600000"
      }
    }
  }
}
```

Skill guidance: prefer **mojito-dev** while building features; use **mojito-prod** for real-data / linguistic work. Treat write tools on prod with care.

## Configuration

| Variable               | Required | Default       | Meaning |
|------------------------|----------|---------------|---------|
| `MOJITO_CLI`           | No       | `mojito-prod` | Executable name or path of the Mojito CLI script |
| `MOJITO_CLI_TIMEOUT_MS`| No       | `600000` (10m)| Hard kill timeout for each CLI invocation |

Removed relative to the stub design: `MOJITO_BASE_URL`, `MOJITO_AUTH_TOKEN` (auth/host are CLI concerns).

### Startup probe

On process start:

1. Resolve `MOJITO_CLI` (default `mojito-prod`).
2. Run `{MOJITO_CLI} --help` with captured stdout/stderr and a short timeout.
3. If the process cannot be spawned or exits non-zero, exit the MCP server with a clear stderr message (CLI missing or broken). Do **not** call the network / `api` during startup.

## CLI runner contract

Module responsibility (e.g. `cli-runner.ts`):

- `execFile` / `spawn` with an argv array (no shell), program = `MOJITO_CLI`.
- **Always capture** child stdout and stderr (never inherit stdout onto the MCP stdio transport).
- Enforce `MOJITO_CLI_TIMEOUT_MS`; on timeout, kill the child and fail the tool call.
- Return `{ exitCode, stdout, stderr }` to the client layer.

MCP stdio uses the server’s stdin/stdout for the protocol. Child stdout must not be mixed into that stream. Child stderr is captured and only surfaced via tool error payloads / MCP logging—not streamed live into the protocol.

## `mojito api` usage conventions

Follow the CLI output contract:

| Channel   | Content |
|-----------|---------|
| stdout    | Response body only (JSON when applicable) |
| stderr    | Diagnostics (`mojito: …`), including HTTP error summaries |
| exit code | Non-zero on HTTP ≥ 400 (and CLI validation failures) |

Client behavior:

1. Build `api` argv for the tool.
2. Run via the CLI runner.
3. If `exitCode !== 0` (or timeout): treat as tool failure. Prefer stderr text for the AI-visible error; include stdout body when present (often JSON error details).
4. If success: `JSON.parse(stdout)` (or handle empty body) and return as MCP tool text content (pretty-printed JSON is fine for readability).

### Pagination

For list/search-style tools that can return multiple pages, always pass:

- `--paginate`
- `--slurp`
- `--max-pages` `0` (no limit; avoid the CLI default of 10)

Pass caller `limit` / `offset` / page-size fields through as `-f`/`-F` when the tool schema includes them; they influence page size / starting position per CLI semantics. Do not rely on the AI to loop pages manually for v1.

### Wait / timeout

**v1 tools:** do **not** pass `-w` / `--wait`. None of them need to block on pollable tasks.

`MOJITO_CLI_TIMEOUT_MS` still applies to every spawn (network hangs, slow searches). Default **10 minutes** so future tools that *do* use `--wait` (e.g. drop export/import, which can take ~5+ minutes per project) work without a too-aggressive default. Operators raise the env var for larger jobs.

When adding long-running tools later: pass `--wait` and document raising `MOJITO_CLI_TIMEOUT_MS` as needed. Keep `mojito_pollabletask_get` as a non-waiting status GET.

### `--spec`

Not registered as an MCP tool. Developers may run `mojito-prod api --spec` (or dev) locally when designing new tools or skills.

## Tool naming

All tools use **`mojito_<object>_<action>`**, where `<object>` is the resource the action applies to.

| Object | Actions (v1) |
|--------|----------------|
| `repo` | `list`, `view`, `create`, `delete` |
| `textunit` | `search`, `info`, `history`, `translation_add` |
| `review` | `update` |
| `pollabletask` | `get` |

Later (not v1): `mojito_textunit_translation_update` and related translation tools.

## Tool → CLI mapping (v1)

### Repository

| MCP tool | CLI shape |
|----------|-----------|
| `mojito_repo_list` | `api /api/repositories --paginate --slurp --max-pages 0` optional `-f name=…` |
| `mojito_repo_view` | `api /api/repositories/{repositoryId}` |
| `mojito_repo_create` | `api /api/repositories -X POST` + body fields (`name`, optional `description`, `sourceLocale`, `checkSLA`, `repositoryLocales`, `assetIntegrityCheckers`) |
| `mojito_repo_delete` | `api /api/repositories/{repositoryId} -X DELETE` |

`mojito_repo_create` body mirrors `POST /api/repositories` (`Repository` JSON). Prefer `--input` with JSON when nested `repositoryLocales` / integrity checkers are present; simple creates can use `-F`/`-f`.

### Text units

| MCP tool | CLI shape |
|----------|-----------|
| `mojito_textunit_search` | `api /api/textunits/search -X POST --paginate --slurp --max-pages 0` + search body (see below) |
| `mojito_textunit_info` | `api /api/textunits/search -X POST` with `tmTextUnitIds[]=…` (and optional `localeTags`) — returns `TextUnitDTO` rows (created date, status, used, target, asset path, etc.). There is no dedicated GET-by-id endpoint. |
| `mojito_textunit_history` | `api /api/textunits/{tmTextUnitId}/history` + required `-f bcp47Tag=…` (API requires BCP-47 tag, e.g. `fr-FR`) |
| `mojito_textunit_translation_add` | `api /api/textunits -X POST` with `tmTextUnitId`, `localeId`, `target`, optional `targetComment`, `status`, `includedInLocalizedFile` |

#### `mojito_textunit_search` parameters

Map the workbench / `TextUnitSearchBody` surface so agents can filter the same way the UI does:

| Parameter | Type | Notes |
|-----------|------|--------|
| `repositoryIds` | `number[]` | If set, restrict to these repos; **omit for all repos** |
| `repositoryNames` | `string[]` | Same scoping as ids (alternate); omit for all |
| `tmTextUnitIds` | `number[]` | Pin to specific TM text units (repo lists not required) |
| `localeTags` | `string[]` | If set, restrict to these BCP-47 tags; **omit for all locales** |
| `name` | `string` | Match string **id** / name |
| `source` | `string` | Match **source** text |
| `target` | `string` | Match **target** (translation) text |
| `assetPath` | `string` | Asset path filter |
| `pluralFormOther` | `string` | Plural “other” form filter |
| `searchType` | enum | How `name` / `source` / `target` match: `EXACT`, `CONTAINS`, `ILIKE` (default API: `EXACT`) |
| `statusFilter` | enum | Workbench status bucket — see below |
| `usedFilter` | enum | `USED` \| `UNUSED` (omit = both) |
| `doNotTranslateFilter` | `boolean` | DNT filter |
| `tmTextUnitCreatedAfter` | datetime string | Date range start (ISO-8601) |
| `tmTextUnitCreatedBefore` | datetime string | Date range end (ISO-8601) |
| `branchId` | `number` | Branch scope |
| `pluralFormFiltered` | `boolean` | Default true on API |
| `pluralFormExcluded` | `boolean` | Default false on API |
| `limit` / `offset` | `number` | Page size / start; still use CLI `--paginate --slurp --max-pages 0` to collect all pages |

**`statusFilter` values** (from `StatusFilter`): `ALL`, `TRANSLATED`, `UNTRANSLATED`, `TRANSLATED_AND_NOT_REJECTED`, `APPROVED_OR_NEEDS_REVIEW_AND_NOT_REJECTED`, `APPROVED_AND_NOT_REJECTED`, `FOR_TRANSLATION`, `REVIEW_NEEDED`, `REVIEW_NEEDED_OR_REJECTED`, `REVIEW_NOT_NEEDED`, `TRANSLATION_NEEDED`, `REJECTED`, `NOT_REJECTED`.

Search matches **source**, **name (id)**, and/or **target** independently: set any combination of those string fields plus `searchType`.

#### All repos / all locales (omit = all)

No extra flags. Presence of a list **restricts**; absence means **all**.

| Intent | Tool input | Client behavior |
|--------|------------|-----------------|
| All locales | Omit `localeTags` | Do not send `localeTags` (API already means all locales) |
| All repositories | Omit `repositoryIds` and `repositoryNames` | Mojito’s search API still requires a repo (or `tmTextUnitIds`) scope. Client expands “all” by listing repos (`mojito_repo_list` path), then POSTs search with every repository `id`. Transparent to the AI. |
| Specific repos | Non-empty `repositoryIds` and/or `repositoryNames` | Pass through |
| Specific text units | Non-empty `tmTextUnitIds` | Pass through; no repo expansion |

Skill note: prefer scoping by repo when possible; all-repo search can be large even with `--paginate --slurp`.

### Review

| MCP tool | CLI shape |
|----------|-----------|
| `mojito_review_update` | `api /api/textunits -X POST` — same endpoint as `mojito_textunit_translation_add`; sets review outcome like the workbench modal |

Workbench actions → fields (same as UI `TextUnitStore.onReviewTextUnits`):

| Action (tool param) | `status` | `includedInLocalizedFile` |
|---------------------|----------|---------------------------|
| `accept` | `APPROVED` | `true` |
| `review` | `REVIEW_NEEDED` | `true` |
| `translate` | `TRANSLATION_NEEDED` | `true` |
| `reject` | `TRANSLATION_NEEDED` | `false` |

Required inputs: `tmTextUnitId`, `localeId`, `target` (current translation text; API requires it), `action` (or explicit `status` + `includedInLocalizedFile`), optional `targetComment`.

### Pollable tasks

| MCP tool | CLI shape |
|----------|-----------|
| `mojito_pollabletask_get` | `api /api/pollableTasks/{pollableTaskId}` |

### Field encoding helpers

- Prefer `-F` for typed values (`true`/`false`/`null`/integers) and `-f` when the value must stay a string.
- Arrays: repeated `name[]=value` (CLI convention).
- Nested create payloads: `--input` + `-X POST`.
- Always require explicit `-X` for methods other than GET (CLI safety rule).

Tool ids and zod schemas in `register-tools.ts` / `tool-metadata.ts` must match this table (rename away from the stub names).

## Module plan

| Path | Change |
|------|--------|
| `src/config.ts` | `cliBinary` (`MOJITO_CLI`), `timeoutMs` (`MOJITO_CLI_TIMEOUT_MS`); drop base URL / auth token |
| `src/cli-runner.ts` | **New** — spawn, capture, timeout |
| `src/mojito-client.ts` | CLI-backed methods for every tool above |
| `src/tool-metadata.ts` | Canonical renamed tool id list |
| `src/register-tools.ts` | Register renamed tools + full search / review schemas |
| `src/index.ts` | Load config, startup `--help` probe, construct client |
| `README.md` / `SKILL.md` | Dual MCP servers, naming, CLI auth, search filter guidance |
| `tests/` | Mock runner; argv + error mapping; tool-id parity |

## Testing strategy

1. **Unit tests** with a fake CLI runner: each client method builds the expected argv (`api`, path, `--paginate`, `--slurp`, `--max-pages`, `0`, `-X`, fields).
2. **Error paths:** non-zero exit + stderr → thrown/returned error message suitable for MCP tool errors; timeout → distinct message.
3. **Startup:** `--help` failure fails process boot (test with injectable runner if practical).
4. **Optional integration** (manual or gated env): real `mojito-dev` / `mojito-prod` on the developer machine—not required for CI if Java CLI isn’t available.

## Implementation order

1. Config + CLI runner + unit tests.
2. Startup `--help` probe in `index.ts`.
3. Rename tool surface; implement repo + textunit + review + pollabletask client methods (paginate/slurp/max-pages where applicable; no `--wait`).
4. Expand `mojito_textunit_search` schema to full `TextUnitSearchBody` filters.
5. Update README and SKILL (dual-server, naming, search/review workflows).
6. Manual smoke: Cursor → `mojito-dev` `mojito_repo_list` → `mojito-prod` `mojito_textunit_search` (read-only).

## Future extensions (out of scope for v1)

- Drop export/import tools with `--wait` and documented long timeouts.
- Optional `--paginate` toggles only if unbounded slurps become a problem (not expected).
- Escape-hatch raw `api` tool (deliberately deferred; curated tools are safer for agents).
- Using `api --spec` offline when authoring new mappings.
- `mojito_repo_update` (`PATCH /api/repositories/{id}`) if needed later.
