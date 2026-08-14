---
name: mojito-mcp
description: Use when working with the Mojito MCP server — translation search, history, submitting corrections, and async pollable tasks. Apply when the user mentions Mojito, TM text units, mojito-mcp tools, or translation status in Cursor with MCP. Infer Mojito repository from the open git repo name when the user does not name one; match names case-insensitively and ignoring hyphens.
---

# Mojito MCP (mojito-mcp)

## Default repository (workspace git)

The user is usually already inside a product repo in Cursor (e.g. **`EndUserApp`**). At Box, **Mojito repository names often align with the git repository name**, but spelling can differ in **letter case** and **hyphens** (e.g. git folder **`PreviewClient`** vs Mojito repo **`preview-client`**).

### Normalized name equality

When comparing **git repo identity** to a Mojito repository **`name`** (from **`mojito_list_repositories`** or search results), treat two strings as the **same repo** if they are equal after:

1. **ASCII lowercasing** (case-insensitive), and  
2. **Removing all `-` (hyphen) characters** from both sides,

then comparing the results character-for-character.

Example: `PreviewClient` → `previewclient`; `preview-client` → `previewclient` → **match**.

If the HTTP `name` query parameter is **strict** (exact Mojito string), a single filtered call may return nothing even when a normalized match exists—**list repositories without that filter** (or with a broad enough filter), then **choose the row whose `name` matches under this normalization**. If **zero** or **several** Mojito repos tie under normalization, **ask the user** which repo to use.

When the user **does not** name a Mojito repository:

1. Determine the **current workspace’s git repo identity** (e.g. root folder name, or `origin` URL basename without `.git`).
2. Fetch Mojito repositories (**`mojito_list_repositories`**) so you can compare **`name`** fields using **normalized name equality** above.
3. If there is **no** repository that matches, or **multiple** plausible matches after normalization, **stop and ask** the user which Mojito repo to use—do not guess silently.
4. Once resolved, pass the **canonical Mojito `repository.name`** string (and/or **`repositoryIds`**) into **`mojito_search_text_units`** and later tools—use the name as returned by Mojito for API calls that require the server’s exact spelling.

## When to use which tool

1. **Resolve context** — Call **`mojito_list_repositories`** when you need ids or to confirm a repo exists. If the user gave a repo name, still use **normalized name equality** when pairing it to Mojito rows. If not, follow **Default repository (workspace git)** above before searching.
2. **Find strings or translations** — Use **`mojito_search_text_units`** with `repositoryNames` or `repositoryIds` plus optional `source`, `name`, `assetPath`, `branchId`, `statusFilter`, `localeTags`, `searchType`, pagination.
3. **Explain changes over time** — After you have a `tmTextUnitId`, use **`mojito_get_text_unit_history`**.
4. **Submit a corrected or shorter translation** — Use **`mojito_add_translation`** with `tmTextUnitId`, `localeId`, and `target` (and optional comment/status/`includedInLocalizedFile`). Requires correct ids from search results.
5. **Long-running jobs** — If another workflow returns a pollable task id, use **`mojito_get_pollable_task`** and poll until complete (respect backoff).

## Id and status pitfalls

- Distinguish **tm text unit id** vs **variant id** vs **current variant id**; the Mojito UI and APIs use different ids. Prefer values taken from **`mojito_search_text_units`** responses once implemented.
- **`statusFilter`** values must match Mojito’s `StatusFilter` enum strings (e.g. `UNTRANSLATED`, `FOR_TRANSLATION`, …) — verify against the webapp when in doubt.
- Vendor / TMS state may **not** live in Mojito; say so if the user asks about vendor queues and data is missing.

## Async and errors

- Until HTTP is implemented, tools will return MCP tool errors starting with `MojitoHttpClient.*: not implemented`.
- After implementation, expect **401/403** for auth issues; surface them clearly without leaking tokens.

## Repo location

In the Mojito monorepo, this server lives in **`mojito-mcp/`** (npm, not Maven).
