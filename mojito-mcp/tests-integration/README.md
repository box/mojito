# Integration tests

These tests call a **real Mojito CLI** against a **real Mojito server**. They are separate from the unit tests under `tests/` and are **not** run by `npm test`.

They always use the **dev** CLI script from your config (never prod), so they talk to your non-production Mojito instance.

## Prerequisites

1. Working Mojito CLI wrappers on your `PATH` (or absolute paths), typically `mojito-dev` and `mojito-prod`, each configured to authenticate to the matching server. See the main [README.md](../README.md) and Mojito CLI docs.
2. Confirm in a shell:

```bash
mojito-dev --help
mojito-dev api /api/repositories
```

3. Permission on **dev** to create and delete repositories (the suite mutates data).

## Config file

1. Copy the template:

```bash
cp tests-integration/integration-config.template.json tests-integration/integration-config.json
```

2. Edit `integration-config.json`:

| Field | Meaning |
|-------|---------|
| `prodCli` | Name or path of the prod Mojito CLI script (documented for completeness; integration tests do **not** use it) |
| `devCli` | Name or path of the **dev** Mojito CLI script — used by all integration tests |
| `testRepositoryName` | Disposable repository name the suite will **create, view, and delete** on dev. Use something unique that is not a real project (e.g. `mojito-mcp-integration-test`). |
| `timeoutMs` | Optional per-invocation timeout as a positive whole number of milliseconds (default `600000`) |

Example:

```json
{
  "prodCli": "mojito-prod",
  "devCli": "mojito-dev",
  "testRepositoryName": "mojito-mcp-integration-test",
  "timeoutMs": 600000
}
```

`integration-config.json` is gitignored. Do not commit machine-specific paths or secrets (auth stays in the CLI config, not this file).

## Run

```bash
cd mojito-mcp
npm run test:integration
```

If `integration-config.json` is missing, the suite fails immediately with instructions to copy the template.

## What the tests do

Against **dev** only:

1. `probeHelp` — CLI is runnable  
2. `repoList` — can list repositories  
3. **Repository lifecycle** using `testRepositoryName`:
   - Delete any leftover repo with that name (from a prior interrupted run)
   - `repoCreate` (with sample locales including `(fr-CA)->fr-FR`)
   - Confirm creating the same repository name again is rejected
   - `repoView` by id
   - `textunitSearch` scoped to that new repo (empty results are fine)
   - `repoDelete` and confirm it no longer appears in a name-filtered list  

If a test fails after create, `afterAll` attempts to delete the created repository id.
