# mojito-mcp

MCP (Model Context Protocol) server that lets Cursor and other AI hosts work with [Mojito](https://github.com/box/mojito) — search strings, inspect repositories, add translations, update review status, and more.

This directory is a **standalone npm package** (not a Maven module). You need **Node 18+** and a working Mojito CLI on your `PATH`.

Internal design notes for implementers: see [Architecture.md](./Architecture.md).

## Why it shells out to the Mojito CLI

Mojito’s REST APIs sit behind the same Java authentication the CLI already implements (form login, MSAL, Cloudflare Access headers, cookies, and per-instance Spring config). Reproducing that in Node for MCP would be fragile and would duplicate host/credential setup.

So this server does **not** talk to Mojito over HTTP itself and does **not** take `MOJITO_AUTH_TOKEN` or a base URL. It runs your existing CLI (`mojito api …`). If the CLI can reach Mojito, the MCP tools can too. Auth and instance URL stay in the CLI config you already maintain.

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

### Prod and optional dev

Most people only need **one** CLI wrapper, pointed at the Mojito they actually use (typically prod). That is enough for MCP.

**`mojito-dev` is optional.** Add it only if you run a **local or non-prod Mojito** (for example a laptop server or a shared dev instance) and want a second MCP server for experiments. If you do not have a dev server, skip the `mojito-dev` jar, wrapper, properties, and MCP entry.

| Script (convention) | Required? | Use for |
|---------------------|-----------|---------|
| `mojito-prod` | Yes (or any single name you set as `MOJITO_CLI`) | Real data — linguistic bugs, production lookups |
| `mojito-dev` | No — only with a local/non-prod Mojito | Feature work against that instance |

Name the scripts whatever you like; the MCP server only cares about the value of `MOJITO_CLI`. The conventions above match the defaults and docs in this package.

### Download `mojito-cli.jar` from your Mojito instance

Use the CLI jar **served by the Mojito webapp you will talk to**. Each instance publishes it at:

`https://<your-mojito-host>/cli/mojito-cli.jar`

Download the jar from **that** instance so the CLI version matches the server. Homebrew or a locally built `cli/target/mojito-cli-*-exec.jar` are alternatives; the instance URL is the usual path.

```bash
mkdir -p "$HOME/bin/mojito-files/prod"

# Replace the host with your Mojito origin (same host you set in l10n.resttemplate.host).
curl -fL -o "$HOME/bin/mojito-files/prod/mojito-cli.jar" \
  "https://mojito.example.com/cli/mojito-cli.jar"
```

**Optional — only if you have a local or non-prod Mojito:**

```bash
mkdir -p "$HOME/bin/mojito-files/dev"

curl -fL -o "$HOME/bin/mojito-files/dev/mojito-cli.jar" \
  "https://mojito-dev.example.com/cli/mojito-cli.jar"
```

Re-run the same `curl` commands to pick up a new CLI after the server is upgraded. If the instance is behind Cloudflare Access, pass the CF Access client id/secret headers (see [Installation and Setup](https://www.mojito.global/docs/guides/install-springboot3/) for `install.sh` with `authMode=CF_SERVICE_TOKEN`). The webapp also serves `/cli/install.sh` if you prefer the official installer over a raw jar.

### Example wrappers

Each script is a thin `java -jar` launcher: a **CLI jar**, a **Spring config directory**, and a **profile** that selects host + auth. Put the scripts on your `PATH` (for example `~/bin`) and `chmod +x` them.

`~/bin/mojito-prod`:

```bash
#!/bin/sh
exec java -jar "$HOME/bin/mojito-files/prod/mojito-cli.jar" "$@" \
  --spring.config.additional-location="file://${HOME}/.l10n/config/cli/" \
  --spring.profiles.active=cli,formlogincredentialprovider
```

**Optional `~/bin/mojito-dev`** (only if you have a local/non-prod Mojito). Use a **different jar and/or Spring profile** so it cannot share the prod host:

```bash
#!/bin/sh
exec java -jar "$HOME/bin/mojito-files/dev/mojito-cli.jar" "$@" \
  --spring.config.additional-location="file://${HOME}/.l10n/config/cli/" \
  --spring.profiles.active=dev,formlogincredentialprovider
```

`"$@"` must be passed through so `api` and other CLI args reach the jar. `formlogincredentialprovider` matches username/password in the properties files; other auth modes (MSAL, header/CF Access, console prompt) use different profiles — see [Configurations](https://www.mojito.global/docs/refs/configurations/).

Host and credentials live next to the CLI, not in MCP. With `--spring.profiles.active=cli,…` Spring loads `application-cli.properties`; `dev` loads `application-dev.properties`. Example (placeholders only):

`~/.l10n/config/cli/application-cli.properties` (prod):

```properties
l10n.resttemplate.host=mojito.example.com
l10n.resttemplate.port=443
l10n.resttemplate.scheme=https
l10n.resttemplate.authentication.credentialProvider=CONFIG
l10n.resttemplate.authentication.username=your-user
l10n.resttemplate.authentication.password=your-password
```

`~/.l10n/config/cli/application-dev.properties` (**optional**, local/non-prod Mojito): same keys, **dev host** and credentials.

Cursor’s GUI `PATH` may not include `~/bin`; if MCP startup cannot find the wrapper, set `MOJITO_CLI` to the script’s **absolute path**.

Verify the prod wrapper:

```bash
mojito-prod --help
mojito-prod api /api/repositories
```

If you set up **mojito-dev**, verify it the same way (`mojito-dev --help` and `mojito-dev api /api/repositories`).

Then register **one MCP server** for prod (and a second only if you have `mojito-dev`) — see [Install from npm](#install-from-npm-recommended) or [Install from a local checkout](#install-from-a-local-checkout).

**Default:** if `MOJITO_CLI` is unset, the MCP server uses `mojito-prod`. If you have a local/non-prod instance, prefer **mojito-dev** while building features and **mojito-prod** for real data. Be careful with write tools (`mojito_repo_create`, `mojito_repo_delete`, `mojito_textunit_translation_add`, `mojito_review_update`) on prod.

## Install from npm (recommended)

Install the published package **once, globally**. Cursor and Claude Code then spawn that binary on stdio. Do **not** use `npx` in a long-lived MCP config: every host start can hit the registry again.

```bash
npm install -g mojito-mcp
```

If the published name is scoped (for example `@box/mojito-mcp`), use that name in `npm install -g` and in the snippets below. You still need **Node 18+** and a working `mojito-prod` (or whatever you set as `MOJITO_CLI`). Add `mojito-dev` only if you have a local/non-prod Mojito. This package does not install the Mojito CLI.

Resolve the installed binary and use that **absolute path** in host config. GUI apps (Cursor) often do not inherit your shell `PATH` (nvm, fnm, asdf), so `"command": "mojito-mcp"` can fail even when the same name works in a terminal.

```bash
which mojito-mcp
# example: /Users/you/.nvm/versions/node/v22.14.0/bin/mojito-mcp
```

Upgrade later with `npm install -g mojito-mcp@<version>` (or the same command without a version for latest). After upgrading, `which mojito-mcp` should still be the same path unless you changed Node versions.

If you only use prod (typical), register a **single** MCP server with `MOJITO_CLI` set to `mojito-prod` (or omit it — that is the default). The `mojito-dev` blocks below are optional.

### Cursor

In **Cursor → Settings → MCP**, or in `~/.cursor/mcp.json` (all projects) / `.cursor/mcp.json` (this repo), add a **mojito-prod** entry. Merge into an existing `mcpServers` object; do not replace other servers. Replace the `command` path with your `which mojito-mcp` output. Add **mojito-dev** only if you have a local/non-prod Mojito.

```json
{
  "mcpServers": {
    "mojito-prod": {
      "command": "/absolute/path/to/mojito-mcp",
      "env": {
        "MOJITO_CLI": "mojito-prod",
        "MOJITO_CLI_TIMEOUT_MS": "600000"
      }
    }
  }
}
```

Optional second entry (local/non-prod Mojito only):

```json
"mojito-dev": {
  "command": "/absolute/path/to/mojito-mcp",
  "env": {
    "MOJITO_CLI": "mojito-dev",
    "MOJITO_CLI_TIMEOUT_MS": "600000"
  }
}
```

Reload MCP (or restart Cursor) and confirm the server(s) show as connected.

### Claude Code

Claude Code’s installer is `claude mcp add`. Everything after `--` is the process it spawns. Prefer the absolute path from `which mojito-mcp`. `--scope user` registers the server for all of your projects; use `--scope project` and a committed `.mcp.json` if the team should share the same launch command (no secrets belong there — auth stays in the Mojito CLI).

```bash
MCP_BIN="$(which mojito-mcp)"

claude mcp add --scope user mojito-prod \
  --env MOJITO_CLI=mojito-prod \
  --env MOJITO_CLI_TIMEOUT_MS=600000 \
  -- "$MCP_BIN"
```

Optional — only if you have a local/non-prod Mojito:

```bash
claude mcp add --scope user mojito-dev \
  --env MOJITO_CLI=mojito-dev \
  --env MOJITO_CLI_TIMEOUT_MS=600000 \
  -- "$MCP_BIN"
```

Check with `claude mcp list`, or `/mcp` inside a Claude Code session.

There is no `setup.sh` for this on purpose. Cursor wants a JSON merge into a file you already have; Claude Code already has a first-class add command. Copy the snippets above, or run `claude mcp add`.

## Install from a local checkout

For developing this package, or before it is published:

```bash
cd mojito-mcp
npm install
npm test
npm run build
```

The compiled entrypoint is `dist/index.js`. Point the host at that file with Node (stdio). Do not pipe JSON to stdin yourself; the MCP host owns the protocol.

### Cursor (local `dist/`)

Same as the npm install: **mojito-prod** is enough. Add **mojito-dev** only if you have a local/non-prod Mojito.

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
    }
  }
}
```

### Claude Code (local `dist/`)

```bash
claude mcp add --scope user mojito-prod \
  --env MOJITO_CLI=mojito-prod \
  --env MOJITO_CLI_TIMEOUT_MS=600000 \
  -- node /absolute/path/to/mojito/mojito-mcp/dist/index.js
```

Optional second add with `MOJITO_CLI=mojito-dev` if you have a local/non-prod Mojito.

## Configuration

### Environment variables

| Variable | Required | Default | Meaning |
|----------|----------|---------|---------|
| `MOJITO_CLI` | No | `mojito-prod` | Mojito CLI executable name or path on `PATH` |
| `MOJITO_CLI_TIMEOUT_MS` | No | `600000` (10 minutes) | Hard timeout per CLI invocation, as a positive whole number of milliseconds. Invalid values (including fractions) use the default. |

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

Unit tests use Jest + `ts-jest` (ESM) under `tests/`. Implementation should follow [Architecture.md](./Architecture.md).

### Integration tests (real Mojito CLI / dev server)

Live tests live in `tests-integration/` and are **not** part of `npm test`. They require a local config file pointing at your CLI scripts and a disposable `testRepositoryName`; they run against **dev** only and will create/view/delete that repository:

```bash
cp tests-integration/integration-config.template.json tests-integration/integration-config.json
# edit integration-config.json — set devCli, prodCli, testRepositoryName
npm run test:integration
```

See [tests-integration/README.md](./tests-integration/README.md).

## License

[Apache License 2.0](./LICENSE), consistent with Mojito.
