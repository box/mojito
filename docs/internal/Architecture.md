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

| Concern | Notes |
|--------|--------|
| Assign / clear a type on a repository | Add optional FK from `repository` to `repo_type` |
| CLI commands | See [CLI → Repo Types](#repo-types-1) |
| Prompt / integrity UI | See [Frontend → Repo Types](#repo-types-2) |
| Layered prompt assembly (global → type → repo → request) | Prompt builder wiring type `aiPrompt` into AI runs |
| Runtime superset of type + repo checkers on push/import | Union by `(assetExtension, integrityCheckerType)` |
| Prompt content per stack | Authoring React/Android/etc. prompts in production data |

Until repositories can reference a type, deleting a type is an unconditional hard delete (no “type in use” check).

##### Architectural decisions

###### 1. One shared AI prompt per type (not translate vs review)

**Decision:** A single field `aiPrompt` (`ai_prompt` in the DB).

**Why:** The type layer describes the *stack* (ICU MessageFormat, Android `%1$s`, RESW `{{0}}`, markup rules). That information is useful for every AI translate *and* review mode. Two fields would mostly duplicate the same text.

**Not decided here:** Global or per-request layers may still distinguish translation vs review (follow-up work). Hardcoded base prompts remain in `AiTranslateType` / `AiReviewType` enums; the type prompt is an additional *layer*, not a replacement for those modes.

###### 2. Integrity checkers are a value set on the type, not their own entity

**Decision:**

- Same logical identity as repository checkers: `(assetExtension, IntegrityCheckerType)`.
- Same enum: `IntegrityCheckerType`.
- Multiple checkers per extension allowed (no unique constraint on extension alone — same as repos after V10).
- Persist as a JPA `@ElementCollection` of embeddable `RepoTypeIntegrityChecker` in table `repo_type_integrity_checker` (FK `repo_type_id`), not reuse of `AssetIntegrityChecker` (FK `repository_id`).
- The checker type has **only** `assetExtension` and `integrityCheckerType`. Parent and row ids are join-table housekeeping owned by `RepoType` and are **not** in JSON.

**Why not `AssetIntegrityChecker`:** JPA maps one parent FK per association. A row owned by a type cannot use `AssetIntegrityChecker`’s required `repository_id`.

**Why still “the same” for runtime:** Push/import will later build a **superset** by projecting both type-level and repo-level rows to `(assetExtension, integrityCheckerType)` and unioning. The Java class of the parent entity does not matter at apply time.

**JSON name:** The collection is exposed as `integrityCheckers` (not `assetIntegrityCheckers`) to keep the repo-type API clear. Element shape is `{ assetExtension, integrityCheckerType }` only — clients get and send de-duplicated sets of that pair.

###### 3. `AuditableEntity` without Hibernate Envers `@Audited`

**Decision:** Extend `AuditableEntity` for `created_date` / `last_modified_date`. Do **not** use `@Audited` / `*_aud` tables for repo types.

**Why:** Timestamps match other Mojito config entities and are cheap. Full revision history (Envers) is unused for this config unless product asks for it later; skipping it keeps the Flyway migration smaller.

###### 4. Entity-as-DTO REST, same pattern as repositories

**Decision:** `RepoTypeWS` accepts and returns the JPA `RepoType` entity. No separate request/response DTO classes in webapp.

**Why:** Consistent with `RepositoryWS` / `Repository` in this codebase. Client-side mirrors live in `restclient` (see [REST API client → Repo Types](#repo-types-3)).

###### 5. PATCH: `null` means leave unchanged

**Decision:** On update, every optional argument treats `null` as “do not change”:

| Argument | `null` | non-`null` |
|----------|--------|------------|
| `name`, `description`, `aiPrompt` | leave as-is | replace (empty string is a real value) |
| `integrityCheckers` | leave checkers as-is | replace **full** set; empty set clears all |

**Why:** Fixed in the contract before implementation so REST and service behave the same way. Omitted JSON fields deserialize to `null` and therefore leave values unchanged.

###### 6. Hard delete for now

**Decision:** `deleteRepoType` removes the type and all of its checker rows.

**Why:** No FK from `repository` yet. When repositories gain an optional `repo_type_id`, delete must gain a “type in use” guard (refuse or require clearing assignments first).

###### 7. Package layout (server)

| Layer | Location |
|-------|----------|
| Entities | `webapp/.../entity/RepoType.java`, `RepoTypeIntegrityChecker.java` |
| Service + Spring Data | `webapp/.../service/repotype/` |
| REST | `webapp/.../rest/repotype/` |
| Schema | Flyway `V69__Add_repo_type.sql` |

Follows existing Mojito layout (`Repository` / `RepositoryService` / `RepositoryWS`).

##### Data model

```
repo_type
  id
  created_date, last_modified_date
  name              UNIQUE
  description
  ai_prompt         longtext, default empty

repo_type_integrity_checker
  repo_type_id      FK → repo_type.id  (NOT NULL)
  asset_extension
  integrity_checker_type   (enum string, same values as asset_integrity_checker)
  PRIMARY KEY (repo_type_id, asset_extension, integrity_checker_type)
```

Uniqueness of a checker on a type is that primary key. Clients never see checker ids or parent.

##### Component flow

```
RepoTypeClient (restclient) / future CLI
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

Auth (existing `WebSecurityConfig`): authenticated GET; mutating `/api/**` requires PM or ADMIN.

##### REST API contract

Base path: `/api/repo-types`

| Method | Path | Success | Errors |
|--------|------|---------|--------|
| GET | `/{id}` | 200 + body | 404 if missing |
| GET | `/` | 200 + list (by name asc) | — |
| GET | `/?name=` | 200 + list (0 or 1); **not** 404 if unknown name | — |
| POST | `/` | 201 + body | 400 invalid name/description; 409 duplicate name |
| PATCH | `/{id}` | 200 + body | 400 invalid name/description; 404 missing; 409 name conflict |
| DELETE | `/{id}` | 204 (void) | 404 missing |

**Create body**

- Required: `name`
- Optional: `description`, `aiPrompt` (null → store `""`), `integrityCheckers` (null/omit → none)

**Example JSON**

```json
{
  "id": 1,
  "name": "React",
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

##### Service behavior

Implementations must match this section and the JavaDoc on `RepoTypeService` / `RepoTypeWS`.

###### `createRepoType`

- Rejects blank {@code name} (null, empty, whitespace) → `RepoTypeInvalidException` (HTTP 400).
- Rejects {@code name} / {@code description} longer than 255 characters → `RepoTypeInvalidException`.
- Rejects duplicate `name` → `RepoTypeNameAlreadyUsedException` (HTTP 409). Concurrent creates/renames that both pass `findByName` still fail on `UK__REPO_TYPE__NAME`; `saveAndFlush` surfaces that as `DataIntegrityViolationException`, which rolls back the service transaction and is mapped to 409 in `RepoTypeWS` (must not be caught inside `@Transactional`).
- Persists `description` as given (`null` allowed).
- Persists `aiPrompt`; `null` → `""`.
- Attaches checkers when the set is non-null and non-empty; otherwise no checker rows.
- Returns persisted entity with generated `id` and loaded checkers.

###### `getRepoTypeById`

- Returns type + checkers.
- Missing id → `RepoTypeWithIdNotFoundException`.

###### `getRepoTypes`

- `name` null or blank → all types, ordered by name ascending; never `null` (empty list OK).
- `name` set → exact match; empty list if none (no exception).

###### `updateRepoType` (PATCH)

- Missing id → `RepoTypeWithIdNotFoundException`.
- Non-null blank {@code name}, or over-length name/description → `RepoTypeInvalidException`.
- Rename to another type’s name → `RepoTypeNameAlreadyUsedException` (same flush-time catch as create).
- Rename to the **same** name → allowed (no conflict).
- Field-level `null` vs replace as in the table above.
- Non-null `integrityCheckers` → full replace via `updateIntegrityCheckers`. That also updates `last_modified_date` on `repo_type` (checker rows live in a join table, so a parent save alone may not dirty the type).

###### `deleteRepoType`

- Missing id → `RepoTypeWithIdNotFoundException`.
- Deletes all checker rows for the type, then the type.
- No soft-delete; no “in use” check until repositories can reference a type.

###### `updateIntegrityCheckers`

Replaces the type's checker collection (element collection on `RepoType`):

1. De-duplicate incoming pairs on `(assetExtension, integrityCheckerType)` (last occurrence wins).
2. Replace the collection with that set. Hibernate inserts/deletes join-table rows; there is no checker `id` to reuse or to accept from the client.
3. `null` or empty incoming set → delete all checkers for the type.

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
| `rest/repotype/RepoTypeWS.java` | HTTP API |
| `rest/repotype/RepoTypeWithIdNotFoundException.java` | → HTTP 404 |

Contracts live in JavaDoc and **this document**; implementations must match them.

---

### CLI (`cli`)

JCommander commands that talk to a running Mojito server through `restclient`.

*Module overview, command conventions, and auth/config for the CLI to be documented here.*

#### Repo Types

Not implemented yet. Follow-up work will add commands such as `repo-type-create`, `repo-type-update`, `repo-type-delete`, and `repo-type-view` that call `/api/repo-types` via `RepoTypeClient`. Until then, operators use the REST API (or tests use the client) directly.

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

No UI yet. Follow-up work may add management screens for creating types, editing `aiPrompt`, and configuring `integrityCheckers`. Until then, configuration is API-only (and later CLI).

---

## Related reading

- User guide (prompts / layers): `docs/_docs/guides/014-using-AI-translations.md`
- Integrity checkers (user): `docs/_docs/guides/009-integrity-checkers.md`
