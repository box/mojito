---
name: mojito-mcp
description: Use when working with the Mojito MCP server — translation search, history, submitting corrections, review status, and pollable tasks. Apply when the user mentions Mojito, TM text units, mojito-mcp tools, or translation status in Cursor with MCP. Infer Mojito repository from the open git repo name when the user does not name one; match names case-insensitively and ignoring hyphens.
---

# Mojito MCP (mojito-mcp)

Tools are named `mojito_<object>_<action>`. Most setups use a single MCP server with **mojito-prod**. A second **mojito-dev** server is optional and only useful if the user has a local or non-prod Mojito. Prefer **mojito-dev** while experimenting when it exists. Treat write tools on prod with care: `mojito_repo_create`, `mojito_repo_delete`, `mojito_textunit_translation_add`, `mojito_review_update`.

Auth and host live in the engineer’s Mojito CLI (`MOJITO_CLI`), not in MCP env. If tools fail at startup or with login/HTTP errors, the CLI config is wrong — do not invent tokens.

## Default repository (workspace git)

The user is usually already inside a product repo in Cursor (e.g. **`EndUserApp`**). At Box, **Mojito repository names often align with the git repository name**, but spelling can differ in **letter case** and **hyphens** (e.g. git folder **`PreviewClient`** vs Mojito repo **`preview-client`**).

### Normalized name equality

When comparing **git repo identity** to a Mojito repository **`name`** (from **`mojito_repo_list`** or search results), treat two strings as the **same repo** if they are equal after:

1. **ASCII lowercasing** (case-insensitive), and
2. **Removing all `-` (hyphen) characters** from both sides,

then comparing the results character-for-character.

Example: `PreviewClient` → `previewclient`; `preview-client` → `previewclient` → **match**.

`mojito_repo_list`’s optional `name` filter is **exact** on the Mojito string. A filtered call may return nothing even when a normalized match exists — **list without that filter** (or with a broader filter), then **choose the row whose `name` matches under this normalization**. If **zero** or **several** Mojito repos tie, **ask the user** which repo to use.

When the user **does not** name a Mojito repository:

1. Determine the **current workspace’s git repo identity** (e.g. root folder name, or `origin` URL basename without `.git`).
2. Fetch Mojito repositories (**`mojito_repo_list`**) and compare **`name`** fields using **normalized name equality**.
3. If there is **no** match, or **multiple** plausible matches, **stop and ask** — do not guess silently.
4. Once resolved, pass the **canonical Mojito `name`** and/or **`repositoryIds`** into **`mojito_textunit_search`** and later tools — use Mojito’s exact spelling for API fields.

## When to use which tool

1. **Resolve context** — **`mojito_repo_list`** (optional exact `name`) for ids and names; **`mojito_repo_view`** when you already have a numeric `repositoryId`. If the user gave a repo name, still use **normalized name equality**. If not, follow **Default repository** above before searching.
2. **Find strings or translations** — **`mojito_textunit_search`** with `repositoryNames` or `repositoryIds` plus optional `source`, `name` (string id), `target`, `assetPath`, `branchId`, `statusFilter`, `localeTags`, `searchType` (`EXACT` / `CONTAINS` / `ILIKE`), `usedFilter`, date range, `limit`. Omit `repositoryIds`/`repositoryNames` for all repos; omit `localeTags` for all locales. Prefer scoping by repo — all-repo search can be large.
3. **Inspect one unit** — **`mojito_textunit_info`** with `tmTextUnitId` (optional `localeTags`) for created date, status, current translation, asset path, used/DNT, etc.
4. **Explain changes over time** — **`mojito_textunit_history`** with `tmTextUnitId` and **`bcp47Tag`** (required; e.g. `fr-FR`, not locale id).
5. **Submit a translation** — **`mojito_textunit_translation_add`** with `tmTextUnitId`, **numeric `localeId`** (from search/info, not the BCP-47 tag), and `target`. Optional `targetComment`, `status`, `includedInLocalizedFile`.
6. **Workbench review** — **`mojito_review_update`** with `tmTextUnitId`, `localeId`, current `target`, and `action`: `accept` / `review` / `translate` / `reject`. Prefer this over raw status fields for accept/reject/needs-review flows.
7. **Create/delete a repository** — **`mojito_repo_create`** / **`mojito_repo_delete`**. Confirm environment and id/name with the user first, especially on prod.
8. **Async jobs** — **`mojito_pollabletask_get`** with a pollable task id. This tool does **not** wait; poll until finished (backoff). v1 tools do not auto-wait on CLI `--wait`.

## Id and status pitfalls

- Distinguish **tm text unit id** vs **variant id** vs **current variant id**; the UI and APIs use different ids. Prefer values from **`mojito_textunit_search`** / **`mojito_textunit_info`**.
- Translation write tools need **`localeId`** (number). History needs **`bcp47Tag`** (string).
- **`statusFilter`** on search is a workbench bucket (`UNTRANSLATED`, `FOR_TRANSLATION`, `REVIEW_NEEDED`, …), not the same as a variant’s `status` (`APPROVED` / `REVIEW_NEEDED` / `TRANSLATION_NEEDED`).
- Vendor / TMS queues may **not** live in Mojito; say so if that data is missing.

## Errors

CLI failures (non-zero `mojito api`, auth, HTTP ≥ 400) surface as MCP tool errors, usually from CLI stderr. Do not leak credentials. If the MCP server never starts, `{MOJITO_CLI} --help` failed — the wrapper is missing or not on PATH.

## Repo location

In the Mojito monorepo, this server lives in **`mojito-mcp/`** (npm, not Maven). See [README.md](./README.md) for install and host config.
