# Mojito internal architecture

This directory holds **internal technical documentation** for Mojito contributors (humans and coding agents). It is separate from the user-facing guides under `docs/_docs/`.

Internal docs serve three purposes:

1. **Explain the change** — what a PR introduced and how the pieces fit together.
2. **Record decisions** — why the design looks this way, so future work does not silently reverse it.
3. **Reference for implementers and tests** — documented behavior is the source of truth when filling in code or writing tests.

The document is organized by major product surface. Most sections are placeholders to be filled as work lands; **Repo Types** is documented in the backend sections below.

---

## Backend

The backend spans three Maven modules that work together: the Spring Boot **server** (`webapp`), the **CLI** (`cli`), and the shared **REST API client** (`restclient`) used by the CLI and tests.

```
CLI  ──►  restclient  ──►  webapp REST API  ──►  services / DB
```

### Server (`webapp`)

Home of JPA entities, services, REST controllers (`*WS`), Flyway migrations, security, and the embedded web UI assets.

*Module overview and cross-cutting server patterns (auth, persistence, migrations) to be documented here.*

#### Repo Types

##### Problem

Many Mojito repositories share the same tech stack (React + FormatJS, Android `strings.xml`, Windows RESW, etc.). Without a shared type:

- AI instructions about placeholders, plurals, and markup are duplicated per repo (or missing).
- Integrity checker configuration is duplicated per repo for the same file formats.

**Repo types** are named shared configs (e.g. `React`, `Android`) that repositories will later be assigned to. Multiple repos of the same kind inherit the same AI prompt layer and the same integrity-checker defaults.

##### Scope of this server slice

**In scope**

- Persist `RepoType` and its integrity checkers.
- REST CRUD under `/api/repo-types` (`RepoTypeWS`).
- Documented contracts below (implementation and tests follow this doc).

**Out of scope (follow-up work)**

| Concern | Notes                                                   |
|--------|---------------------------------------------------------|
| Assign / clear a type on a repository | Add optional FK from `repository` to `repo_type`        |
| CLI commands | See [CLI → Repo Types](#repo-types-1)                   |
| Prompt / integrity UI | See [Frontend → Repo Types](#repo-types-2)              |
| Layered prompt assembly (global → type → repo → request) | Prompt builder wiring type `aiPrompt` into AI runs      |
| Runtime superset of type + repo checkers on push/import | Union by `(assetExtension, integrityCheckerType)`       |
| Prompt content per stack | Authoring React/Android/etc. prompts in production data |

Until repositories can reference a type, deleting a type is an unconditional hard delete (no “type in use” check).

##### Architectural decisions

###### 1. One shared AI prompt per type (not translate vs review)

**Decision:** A single field `aiPrompt` (`ai_prompt` in the DB).

**Why:** The type layer describes the *stack* (ICU MessageFormat, Android `%1$s`, RESW `{{0}}`, markup rules). That information is useful for every AI translate *and* review mode. Two fields would mostly duplicate the same text.

**Not decided here:** Global or per-request layers may still distinguish translation vs review (follow-up work). Hardcoded base prompts remain in `AiTranslateType` / `AiReviewType` enums; the type prompt is an additional *layer*, not a replacement for those modes.

###### 2. `aiPrompt` has no application max length

**Decision:** Persist as unbounded long text. No service 400 for over-length `aiPrompt`.

- DB: V69 `longtext`. JPA: `@Column(name = "ai_prompt", length = Integer.MAX_VALUE)` only — **not** `@Lob`.
- `@Lob` in this codebase is for binary blobs (`Image`, `MBlob`, `ApplicationCache`). Long strings (`TMTextUnit.content`, `AssetContent`, `AiReviewProto`) use the same bare `@Column(..., length = Integer.MAX_VALUE)` pattern.

**Why no cap:** A Mojito instance has on the order of tens of types, not thousands. Operators keep prompts short themselves because this text is later injected into every AI call for every repo of that type — more text is more tokens and more cost. An arbitrary 64KiB / 1MiB product limit would not buy safety we need.

**Practical ceilings** (not Mojito 400s): HTTP body size, JVM heap, MySQL `max_allowed_packet`. Name and description stay capped at 255.

###### 3. Integrity checkers are a value set on the type, not their own entity

**Decision:**

- Same logical identity as repository checkers: `(assetExtension, IntegrityCheckerType)`.
- Same enum: `IntegrityCheckerType`.
- Multiple checkers per extension allowed (no unique constraint on extension alone — same as repos after V10).
- Persist as a JPA `@ElementCollection` of embeddable `RepoTypeIntegrityChecker` in table `repo_type_integrity_checker` (FK `repo_type_id`), not reuse of `AssetIntegrityChecker` (FK `repository_id`).
- Uniqueness is `(repo_type_id, asset_extension, integrity_checker_type)`: V69 primary key on MySQL, and `@CollectionTable` `UK__REPO_TYPE_INTEGRITY_CHECKER` so Hibernate's test schema (HSQL, Flyway off) enforces the same rule. Application de-dupe is not the only backstop.
- The checker type has **only** `assetExtension` and `integrityCheckerType`. Parent and row ids are join-table housekeeping owned by `RepoType` and are **not** in JSON.

**Why not `AssetIntegrityChecker`:** JPA maps one parent FK per association. A row owned by a type cannot use `AssetIntegrityChecker`’s required `repository_id`.

**Why still “the same” for runtime:** Push/import will later build a **superset** by projecting both type-level and repo-level rows to `(assetExtension, integrityCheckerType)` and unioning. The Java class of the parent entity does not matter at apply time.

**JSON name:** The collection is exposed as `integrityCheckers` (not `assetIntegrityCheckers`) to keep the repo-type API clear. Element shape is `{ assetExtension, integrityCheckerType }` only — clients get and send de-duplicated sets of that pair.

###### 4. `AuditableEntity` without Hibernate Envers `@Audited`

**Decision:** Extend `AuditableEntity` for `created_date` / `last_modified_date`. Do **not** use `@Audited` / `*_aud` tables for repo types.

**Why:** Timestamps match other Mojito config entities and are cheap. Full revision history (Envers) is unused for this config unless product asks for it later; skipping it keeps the Flyway migration smaller.

###### 5. Entity-as-DTO REST, same pattern as repositories

**Decision:** `RepoTypeWS` accepts and returns the JPA `RepoType` entity. No separate request/response DTO classes in webapp.

**Why:** Consistent with `RepositoryWS` / `Repository` in this codebase. Client-side mirrors live in `restclient` (see [REST API client → Repo Types](#repo-types-3)).

###### 6. PATCH: `null` means leave unchanged

**Decision:** On update, every optional argument treats `null` as “do not change”:

| Argument | `null` | non-`null` |
|----------|--------|------------|
| `name`, `description`, `aiPrompt` | leave as-is | replace (empty string is a real value) |
| `integrityCheckers` | leave checkers as-is | replace **full** set; empty set clears all |

**Why:** Fixed in the contract before implementation so REST and service behave the same way. Omitted JSON fields deserialize to `null` and therefore leave values unchanged.

###### 7. Hard delete for now

**Decision:** `deleteRepoType` removes the type and all of its checker rows.

**Why:** No FK from `repository` yet. When repositories gain an optional `repo_type_id`, delete must gain a “type in use” guard (refuse or require clearing assignments first).

###### 8. Package layout (server)

| Layer | Location |
|-------|----------|
| Entities | `webapp/.../entity/RepoType.java`, `RepoTypeIntegrityChecker.java` |
| Service + Spring Data | `webapp/.../service/repotype/` |
| REST | `webapp/.../rest/repotype/` |
| Schema | Flyway `V69__Add_repo_type.sql` |

Follows existing Mojito layout (`Repository` / `RepositoryService` / `RepositoryWS`).

###### 9. Name is an open string (same rules as repository names)

**Decision:** `name` is a free-form string, not a Java enum and not a closed catalog. Operators can add `Django`, `Flutter`, etc. without a code change. A recommended list can be seed data later; it is not part of this API.

Rules (same idea as repository names):

- Required: non-empty after trim
- Trim leading/trailing whitespace before uniqueness and persist (`"React "` → `"React"`)
- Max 255 characters
- Unique, **case-sensitive** (`React` and `react` are different types; the DB uses `utf8_bin`, same as repository names)
- No required charset, PascalCase rule, or separator

##### Data model

```
repo_type
  id
  created_date, last_modified_date
  name              UNIQUE
  description
  ai_prompt         longtext, default empty; no application max length

repo_type_integrity_checker
  repo_type_id      FK → repo_type.id  (NOT NULL)
  asset_extension
  integrity_checker_type   (enum string, same values as asset_integrity_checker)
  PRIMARY KEY (repo_type_id, asset_extension, integrity_checker_type)
```

Uniqueness of a checker on a type is that primary key (JPA `UK__REPO_TYPE_INTEGRITY_CHECKER` on
the collection table so tests match). Clients never see checker ids or parent.

##### Component flow

```
CLI (`repo-type-*`) / RepoTypeClient (restclient)
        │
        ▼
 RepoTypeWS  (/api/repo-types)
        │
        ▼
 RepoTypeService
        │
        └── RepoTypeRepository
                │
                ▼
         MySQL tables
```

Auth (existing `WebSecurityConfig`): authenticated GET (USER included → 200); mutating `/api/**` requires PM or ADMIN (USER → 403).

##### REST API contract

Base path: `/api/repo-types`

| Method | Path | Success | Errors |
|--------|------|---------|--------|
| GET | `/{id}` | 200 + body | 404 if missing |
| GET | `/` | 200 + list (by name asc) | — |
| GET | `/?name=` | 200 + list (0 or 1); **not** 404 if unknown name | — |
| POST | `/` | 201 + body | 400 invalid name/description/checkers; 403 USER; 409 duplicate name |
| PATCH | `/{id}` | 200 + body | 400 invalid name/description/checkers; 403 USER; 404 missing; 409 name conflict |
| DELETE | `/{id}` | 204 (void) | 403 USER; 404 missing |

**Error bodies**

- 409 name conflict: `RepoType with name [<trimmed name>] already exists`. Only
  `RepoTypeNameAlreadyUsedException` and `UK__REPO_TYPE__NAME`. Other
  `DataIntegrityViolationException`s are not labeled as a name conflict.
- 400 validation: the `RepoTypeInvalidException` message (e.g. `name is required`,
  `name must be at most 255 characters`, `description must be at most 255 characters`,
  `integrity checker must not be null`, `assetExtension is required`,
  `integrityCheckerType is required`). There is no 400 for over-length `aiPrompt`.

**Create body**

- Required: `name`
- Optional: `description`, `aiPrompt` (null → store `""`), `integrityCheckers` (null/omit → none)

**Example JSON**

```json
{
  "id": 1,
  "name": "React",
  "createdDate": "2026-08-20T23:00:00Z",
  "lastModifiedDate": "2026-08-20T23:00:00Z",
  "description": "FormatJS / react-intl apps",
  "aiPrompt": "This stack uses ICU MessageFormat. Preserve {placeholders} and plural/select skeletons.",
  "integrityCheckers": [
    {
      "assetExtension": "properties",
      "integrityCheckerType": "MESSAGE_FORMAT"
    },
    {
      "assetExtension": "properties",
      "integrityCheckerType": "TRAILING_WHITESPACE"
    }
  ]
}
```

GET/POST/PATCH serialize with {@code View.RepoType}: {@code id} and {@code name} stay on {@code View.IdAndName} so a nested summary can omit prompt and checkers; timestamps use {@code IdAndNameAndCreated} / {@code Modified}.

##### Service behavior

Implementations must match this section and the JavaDoc on `RepoTypeService` / `RepoTypeWS`.

###### `createRepoType`

- Rejects blank {@code name} (null, empty, whitespace) → `RepoTypeInvalidException` (HTTP 400).
  Names are trimmed before uniqueness and persist (`"React "` is stored as `"React"`).
- Rejects {@code name} / {@code description} longer than 255 characters → `RepoTypeInvalidException`.
- Rejects duplicate `name` → `RepoTypeNameAlreadyUsedException` (HTTP 409). Uniqueness is
  **case-sensitive** (`React` does not block `react`). Concurrent creates/renames that both pass
  `findByName` still fail on `UK__REPO_TYPE__NAME`; `saveAndFlush` surfaces that as
  `DataIntegrityViolationException`, which rolls back the service transaction. `RepoTypeWS` maps
  that to 409 **only** when the constraint is `UK__REPO_TYPE__NAME`, and the body quotes the
  trimmed name that was actually persisted (not a null request field). Other integrity failures
  (e.g. the checker table composite primary key) are not labeled as a name conflict (HTTP 500).
  Do not catch the exception inside `@Transactional`.
- Persists `description` as given (`null` allowed).
- Persists `aiPrompt`; `null` → `""`. No application max length (see decision 2).
- Attaches checkers when the set is non-null and non-empty; otherwise no checker rows. Each checker
  requires {@code assetExtension} and {@code integrityCheckerType} ({@code RepoTypeInvalidException}).
  Extensions are trimmed and a leading {@code .} is stripped.
- Returns persisted entity with generated `id` and loaded checkers.

###### `getRepoTypeById`

- Returns type + checkers.
- Missing id → `RepoTypeWithIdNotFoundException`.

###### `getRepoTypes`

- `name` null or blank → all types, ordered by name ascending; never `null` (empty list OK).
- `name` set → trim, then exact match; empty list if none (no exception).

###### `updateRepoType` (PATCH)

- Missing id → `RepoTypeWithIdNotFoundException`.
- Non-null blank {@code name} (after trim), or over-length name/description → `RepoTypeInvalidException`.
  Non-null names are trimmed before uniqueness and persist.
- Rename to another type’s name → `RepoTypeNameAlreadyUsedException` (same flush-time catch as create).
- Rename to the **same** name → allowed (no conflict).
- Field-level `null` vs replace as in the table above. Non-null {@code aiPrompt} is stored as
  given (including empty string); there is no application max length.
- Non-null `integrityCheckers` → full replace via `updateIntegrityCheckers`. That also updates `last_modified_date` on `repo_type` (checker rows live in a join table, so a parent save alone may not dirty the type).

###### `deleteRepoType`

- Missing id → `RepoTypeWithIdNotFoundException`.
- Deletes all checker rows for the type, then the type.
- No soft-delete; no “in use” check until repositories can reference a type.

###### `updateIntegrityCheckers`

Replaces the type's checker collection (element collection on `RepoType`):

1. Validate each checker: {@code integrityCheckerType} required; {@code assetExtension} required
   after trim and stripping one leading {@code .}. Missing fields → `RepoTypeInvalidException`.
2. De-duplicate incoming pairs on `(assetExtension, integrityCheckerType)` (last occurrence wins).
3. Replace the collection with that set. Hibernate inserts/deletes join-table rows; there is no checker `id` to reuse or to accept from the client.
4. `null` or empty incoming set → delete all checkers for the type.
5. Missing type id → `RepoTypeWithIdNotFoundException`. Never save the caller's instance when
   the row is gone (that would re-insert a deleted type).

Caller must pass a persisted `RepoType`.

##### Code map (server)

| File | Role |
|------|------|
| `entity/RepoType.java` | Aggregate root |
| `entity/RepoTypeIntegrityChecker.java` | Embeddable checker pair (extension + type) |
| `service/repotype/RepoTypeService.java` | Business rules (above) |
| `service/repotype/RepoTypeRepository.java` | `findByName`, `findAllByOrderByNameAsc` |
| `service/repotype/RepoTypeNameAlreadyUsedException.java` | → HTTP 409 |
| `service/repotype/RepoTypeInvalidException.java` | → HTTP 400 |
| `rest/View.java` | `View.RepoType` for `/api/repo-types` payloads |
| `rest/repotype/RepoTypeWithIdNotFoundException.java` | → HTTP 404 |

Contracts live in JavaDoc and **this document**; implementations must match them.

---

### CLI (`cli`)

JCommander commands that talk to a running Mojito server through `restclient`.

*Module overview, command conventions, and auth/config for the CLI to be documented here.*

#### Repo Types

Name/description CLI for repo types. Commands are JCommander `Command` beans in `cli/.../command/`, same pattern as `repo-create` / `repo-update` / `repo-delete` / `repo-view`. They talk to the server only through `RepoTypeClient` (never through `RepoTypeService` in production code). Integration tests extend `CLITestBase` and then assert via `RepoTypeRepository` / `RepoTypeService`.

**In scope**

- `repo-type-create`, `repo-type-update`, `repo-type-delete`, `repo-type-view`
- Setting **name** and **description** only

**Out of scope (follow-up)**

- CLI for `aiPrompt` and `integrityCheckers` (separate stories)
- Listing all types (no `repo-type-list`; view is by exact name)

##### Commands

| Command | Role | Required flags | Optional flags |
|---------|------|----------------|----------------|
| `repo-type-create` | POST `/api/repo-types` | `--name` / `-n` | `--description` / `-d` |
| `repo-type-update` | PATCH `/api/repo-types/{id}` | `--name` / `-n` (existing type) | `--new-name` / `-nn`, `--description` / `-d` |
| `repo-type-delete` | DELETE `/api/repo-types/{id}` | `--name` / `-n` | — |
| `repo-type-view` | GET list filtered by name | `--name` / `-n` | — |

Flag constants live in `cli/.../command/param/Param.java` (`REPO_TYPE_*`). Help text comes from those descriptions.

##### Lookup by name

Update, delete, and view resolve the type with `CommandHelper.findRepoTypeByName`. A blank `-n` is rejected with `Repo type name is required` before calling the API — the server treats a blank `?name=` as list-all, so a single existing type would otherwise be selected. Non-blank names are trimmed and looked up with `RepoTypeClient.getRepoTypes(name)`. If the list size is not exactly `1`, the command fails with `Repo type with name [<name>] is not found` (`CommandException`). There is no dedicated get-by-name client method.

##### Create

- Body: `name` required; `description` may be omitted (`null`).
- Does not send `aiPrompt` or `integrityCheckers` (server defaults: empty prompt, no checkers).
- HTTP 400, 404, and 409 → the response body as a `CommandException` (e.g. `name must be at most 255 characters`, `RepoType with name [<trimmed name>] already exists`). Empty body falls back to `Invalid repo type` (400), `Repo type is not found` (404), or `Repo type already exists` (409). HTTP 403 is **not** mapped: `AuthenticatedRestTemplate` treats 403 as a stale session, retries login, then throws `RestClientException` (`Tried to re-authenticate but the response remains to be unauthenticated`). That is the same dump as other mutating CLI commands (e.g. `repo-create`). Unmapped client errors still dump as `Unexpected error` in `L10nJCommander`.
- Success prints `created --> repo type id: <id>`.

##### Update

- At least one of `--new-name` or `--description` is required; otherwise `Must provide at least one of the following options: --new-name, --description`.
- PATCH body sets only the fields the user passed; omitted flags stay `null` so the server leaves those columns unchanged (see [PATCH semantics](#6-patch-null-means-leave-unchanged)).
- `integrityCheckers` is sent as `null` so checkers are not replaced. (A non-null empty set would clear them.) {@code aiPrompt} is never set (stays `null`). A description-only update must leave prompt and checkers as they were.
- HTTP 400, 404, and 409 → same mapping as create (response body, or the 400/404/409 fallbacks). HTTP 403 is the same session-retry dump as create.
- Success prints `updated --> repo type id: <id>`.

##### Delete

- Resolves by name, then `deleteRepoType(id)`. HTTP 404 on that call → same mapping as create (lookup-then-delete race is a short `CommandException`). HTTP 403 is the same session-retry dump as create (USER mutate never arrives as `HttpClientErrorException`).
- Success prints `deleted --> repo type name: <name>`.
- Until repositories can reference a type, delete is an unconditional hard delete (same as the server).

##### View

Prints three fields for an existing type:

- `Repo type id --> <id>`
- `Name --> <name>`
- `Description --> <description>` (`null` description prints as empty)

##### Package layout (CLI)

| File | Role |
|------|------|
| `cli/.../command/RepoTypeCreateCommand.java` | Create |
| `cli/.../command/RepoTypeUpdateCommand.java` | Update |
| `cli/.../command/RepoTypeDeleteCommand.java` | Delete |
| `cli/.../command/RepoTypeViewCommand.java` | View |
| `cli/.../command/param/Param.java` | `--name` / `--new-name` / `--description` constants |
| `cli/.../command/RepoType*CommandTest.java` | `CLITestBase` happy path and error cases (duplicate name, unknown type, update with no optional flags, description/rename must not clear prompt or checkers) |

---

### REST API client (`restclient`)

Java HTTP client and DTO mirrors of server JSON entities. Used by the CLI and by server integration tests against a live API.

*Module overview and `BaseClient` / auth patterns to be documented here.*

#### Repo Types

Skeleton client and DTOs mirror the server contract in [Server → Repo Types](#repo-types).

| File | Role |
|------|------|
| `rest/client/RepoTypeClient.java` | HTTP client; entity path `repo-types` → `/api/repo-types` |
| `rest/entity/RepoType.java` | DTO mirror of server `RepoType` JSON |
| `rest/entity/RepoTypeIntegrityChecker.java` | DTO for `integrityCheckers[]` elements |

`RepoTypeClient` methods (get by id, list, create, update, delete) correspond 1:1 with `RepoTypeWS`. Behavior must match the [REST API contract](#rest-api-contract) above (including 404 / 409 mapping via HTTP errors).

---

## Frontend

Web UI served from `webapp` (React / Flux-style JS under `webapp/src/main/resources/public`).

*Frontend architecture, SDK entities, and page patterns to be documented here.*

#### Repo Types

No UI yet. Follow-up work may add management screens for creating types, editing `aiPrompt`, and configuring `integrityCheckers`. Until then, configuration is REST API and CLI (name/description only on the CLI).

---

## Related reading

- User guide (prompts / layers): `docs/_docs/guides/014-using-AI-translations.md`
- Integrity checkers (user): `docs/_docs/guides/009-integrity-checkers.md`
