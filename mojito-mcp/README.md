# mojito-mcp

MCP (Model Context Protocol) server that lets Cursor and other AI hosts work with [Mojito](https://github.com/box/mojito) — search strings, inspect repositories, add translations, update review status, and more.

**Authentication and server URL are not configured in this package.** The server shells out to your existing Mojito CLI (`mojito api …`). If the CLI can reach Mojito, the MCP tools can too.

This directory is a **standalone npm package** (not a Maven module). You need **Node 18+** and a working Mojito CLI on your `PATH`.

Internal design notes for implementers: see [DESIGN.md](./DESIGN.md).

## Prerequisites: Mojito CLI

Before installing the MCP server, install and configure the Mojito CLI so it authenticates to your Mojito instance(s).

### Official Mojito documentation

| Topic | Doc |
|-------|-----|
| Install the CLI (jar, Homebrew, install script from the server) | [Installation and Setup](https://www.mojito.global/docs/guides/install-springboot3/) |
| CLI host, credentials, and auth modes (`l10n.resttemplate.*`) | [Configurations](https://www.mojito.global/docs/refs/configurations/) |
| Contributor / local alias examples | [Open source contributors](https://www.mojito.global/docs/guides/open-source-contributors/) |

Typical pieces:

1. A CLI wrapper on your `PATH` (install script from the Mojito server, Homebrew `mojito`, or a `java -jar …` wrapper).
2. CLI `application.properties` with host/scheme/port and authentication (form login, MSAL, header/CF Access, etc.) — see the configurations guide above.
3. Confirm in a terminal: `mojito --help` and a simple call such as `mojito api /api/repositories`.

The MCP server does **not** replace that setup. It only invokes the script you point it at.

### Prod and dev (dual scripts)

Many teams run two Mojito servers (for example **prod** with real linguistic data and **dev** for feature work). Use **two CLI entry points**, each with its own config (host + auth), both on your `PATH`:

| Script (convention) | Use for |
|---------------------|---------|
| `mojito-prod` | Real data — linguistic bugs, production lookups |
| `mojito-dev` | Developing against a non-prod Mojito |

Name the scripts whatever you like; the MCP server only cares about the value of `MOJITO_CLI`. The conventions above match the defaults and docs in this package.

Verify each script independently:

```bash
mojito-prod --help
mojito-prod api /api/repositories

mojito-dev --help
mojito-dev api /api/repositories
```

Then register **two MCP servers** in Cursor (same package, different `MOJITO_CLI`) — see [Configure Cursor](#configure-cursor).

**Default:** if `MOJITO_CLI` is unset, the MCP server uses `mojito-prod`. Prefer `mojito-dev` while building features; use prod when you need real data. Be careful with write tools (`mojito_repo_create`, `mojito_repo_delete`, `mojito_textunit_translation_add`, `mojito_review_update`) on prod.

## Install

From this directory (development / local checkout):

```bash
cd mojito-mcp
npm install
npm test
npm run build
```

The compiled entrypoint is `dist/index.js`. Cursor should run that file with Node (stdio transport). Do not pipe JSON to stdin yourself; the MCP host owns the protocol.

After an internal publish you can switch to your registry’s `npx` / package install pattern; configuration below stays the same.

## Configure Cursor

In **Cursor → Settings → MCP**, or in `~/.cursor/mcp.json`, add one entry per Mojito environment. Use **absolute paths** to `dist/index.js`.

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

If you only use one environment, a single server entry is enough. Set `MOJITO_CLI` to that script’s name (or omit it to default to `mojito-prod`).

### Environment variables

| Variable | Required | Default | Meaning |
|----------|----------|---------|---------|
| `MOJITO_CLI` | No | `mojito-prod` | Mojito CLI executable name or path on `PATH` |
| `MOJITO_CLI_TIMEOUT_MS` | No | `600000` (10 minutes) | Hard timeout per CLI invocation |

There is **no** `MOJITO_BASE_URL` or auth token in the MCP env. Host and credentials live in the CLI configuration only.

On startup the server runs `{MOJITO_CLI} --help`. If that fails (missing script, not executable), the MCP process exits with an error — fix the CLI wrapper first.

### Timeouts

Long CLI work (especially future drop export/import with wait) can exceed a few minutes. Raise `MOJITO_CLI_TIMEOUT_MS` in the MCP env for that server if needed. The default (10 minutes) covers typical slow jobs; large projects may need more.

## Tools

Tool ids follow `mojito_<object>_<action>`.

### Repository

| Tool | Purpose |
|------|---------|
| `mojito_repo_list` | List repositories (optional name filter) |
| `mojito_repo_view` | Get one repository by id |
| `mojito_repo_create` | Create a repository |
| `mojito_repo_delete` | Delete a repository by id |

### Text units

| Tool | Purpose |
|------|---------|
| `mojito_textunit_search` | Workbench-style search (repos, locales, source/name/target, status, used/unused, date range, …) |
| `mojito_textunit_info` | General info for a text unit (created date, status, current translation fields, …) |
| `mojito_textunit_history` | Translation history for a text unit + locale (`bcp47Tag`) |
| `mojito_textunit_translation_add` | Add / set the current translation for a locale |

**Search scoping:** omit `repositoryIds` / `repositoryNames` to search **all repositories**; omit `localeTags` to include **all locales**. Providing a list restricts the search to those values. (The client expands “all repos” for the Mojito API under the hood.)

Prefer scoping by repository when you can — all-repo search can return a lot of data.

### Review and async tasks

| Tool | Purpose |
|------|---------|
| `mojito_review_update` | Accept / needs-review / needs-translation / reject (workbench review actions) |
| `mojito_pollabletask_get` | Fetch status of a pollable task by id |

## How it works (brief)

```
Cursor → mojito-mcp → mojito-prod|mojito-dev api … → Mojito REST
```

The server captures CLI stdout/stderr. Successful JSON responses are returned to the model; CLI failures (non-zero exit, stderr diagnostics) are surfaced as normal MCP tool errors. List/search tools use the CLI’s pagination helpers (`--paginate --slurp`, unbounded page cap) so callers do not page manually.

## Optional: Cursor skill

[SKILL.md](./SKILL.md) has workflow hints (e.g. matching git repo names to Mojito repository names). Copy or symlink it into your Cursor skills directory if you want that guidance in every chat; tool descriptions alone are enough to call the APIs. The skill file is also included in the npm package `files` list.

## Develop / test this package

```bash
cd mojito-mcp
npm install
npm test
npm run test:watch
npm run build
```

Unit tests use Jest + `ts-jest` (ESM) under `tests/`. Implementation should follow [DESIGN.md](./DESIGN.md).

### Integration tests (real Mojito CLI / dev server)

Live tests live in `tests-integration/` and are **not** part of `npm test`. They require a local config file pointing at your CLI scripts and a disposable `testRepositoryName`; they run against **dev** only and will create/view/delete that repository:

```bash
cp tests-integration/integration-config.template.json tests-integration/integration-config.json
# edit integration-config.json — set devCli, prodCli, testRepositoryName
npm run test:integration
```

See [tests-integration/README.md](./tests-integration/README.md).

## License

Apache-2.0, consistent with Mojito.
