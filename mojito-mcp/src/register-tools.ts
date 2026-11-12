/**
 * Copyright 2026 Box, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { MojitoCliClient } from "./mojito-client.js";

/**
 * AI-facing MCP tool surface for Mojito.
 *
 * The MCP host publishes each tool's name, description, and inputSchema (from these zod
 * definitions) to the model. Handlers only forward validated args to {@link MojitoCliClient}.
 *
 * Naming: mojito_<object>_<action>
 */

// --- Shared enums / nested shapes (descriptions become JSON Schema for the model) ---

const searchTypeSchema = z
    .enum(["EXACT", "CONTAINS", "ILIKE"])
    .describe(
        "How name/source/target filters match. EXACT = whole-string equality; CONTAINS = substring; ILIKE = case-insensitive SQL LIKE. Default on the Mojito API is EXACT.",
    );

const usedFilterSchema = z
    .enum(["USED", "UNUSED"])
    .describe(
        "USED = string still present in the latest successful asset extraction; UNUSED = no longer extracted (orphaned in TM). Omit to include both.",
    );

const statusFilterSchema = z
    .enum([
        "ALL",
        "TRANSLATED",
        "UNTRANSLATED",
        "TRANSLATED_AND_NOT_REJECTED",
        "APPROVED_OR_NEEDS_REVIEW_AND_NOT_REJECTED",
        "APPROVED_AND_NOT_REJECTED",
        "FOR_TRANSLATION",
        "REVIEW_NEEDED",
        "REVIEW_NEEDED_OR_REJECTED",
        "REVIEW_NOT_NEEDED",
        "TRANSLATION_NEEDED",
        "REJECTED",
        "NOT_REJECTED",
    ])
    .describe(
        [
            "Workbench status bucket (combines translation presence, variant status, and includedInLocalizedFile):",
            "ALL = everything;",
            "TRANSLATED / UNTRANSLATED = has or lacks a current translation;",
            "FOR_TRANSLATION = needs work (missing translation, TRANSLATION_NEEDED, or rejected);",
            "REVIEW_NEEDED = current status is needs review;",
            "TRANSLATION_NEEDED = current status is needs translation;",
            "REJECTED = not included in localized file;",
            "NOT_REJECTED = included in localized file;",
            "APPROVED_AND_NOT_REJECTED / APPROVED_OR_NEEDS_REVIEW_AND_NOT_REJECTED / TRANSLATED_AND_NOT_REJECTED = quality filters;",
            "REVIEW_NEEDED_OR_REJECTED / REVIEW_NOT_NEEDED = review-oriented buckets.",
        ].join(" "),
    );

const textUnitStatusSchema = z
    .enum(["APPROVED", "REVIEW_NEEDED", "TRANSLATION_NEEDED"])
    .describe(
        "Current translation status: APPROVED = accepted; REVIEW_NEEDED = needs linguistic review; TRANSLATION_NEEDED = needs (re)translation.",
    );

const reviewActionSchema = z
    .enum(["accept", "review", "translate", "reject"])
    .describe(
        [
            "Workbench review action (maps to status + includedInLocalizedFile):",
            "accept → APPROVED, included;",
            "review → REVIEW_NEEDED, included;",
            "translate → TRANSLATION_NEEDED, included;",
            "reject → TRANSLATION_NEEDED, NOT included (rejected from file).",
        ].join(" "),
    );

const bcp47TagSchema = z.string().describe("BCP-47 locale tag, e.g. en-US, fr-FR, ja-JP.");

/**
 * Same encoding as `mojito repo-create -l` / `repo-update -l` (see Mojito “Managing Locales” docs).
 */
const encodedRepositoryLocaleSchema = z
    .string()
    .describe(
        [
            "Encoded locale, Mojito CLI -l syntax:",
            "`fr-FR` = fully translated;",
            "`(en-GB)` = not fully translated (parentheses);",
            "`(fr-CA)->fr-FR` = fr-CA inherits from parent fr-FR (child is not fully translated).",
            "Parent on the right must also be listed as its own entry if it should exist in the repo (e.g. include both `(fr-CA)->fr-FR` and `fr-FR`).",
        ].join(" "),
    );

const assetIntegrityCheckerInputSchema = z.object({
    assetExtension: z
        .string()
        .describe("Resource file extension without a dot, e.g. properties, resw, xlf."),
    integrityCheckerType: z
        .string()
        .describe(
            "Integrity checker type name, e.g. COMPOSITE_FORMAT, PRINTF_LIKE. Validates placeholders/format in translations.",
        ),
});

const assetFilterConfigIdOverrideSchema = z
    .enum([
        "PROPERTIES_JAVA",
        "MACSTRINGSDICT_FILTER_KEY",
        "XCODE_XLIFF",
        "CSV_ADOBE_MAGENTO",
        "HTML_ALPHA",
    ])
    .describe(
        "Optional Okapi filter override. Omit to let Mojito infer the filter from the asset path.",
    );

const localizedAssetStatusSchema = z
    .enum(["ALL", "ACCEPTED_OR_NEEDS_REVIEW", "ACCEPTED"])
    .describe(
        [
            "Which translations are eligible when generating the localized file (not the workbench statusFilter):",
            "ALL = every translation except rejected ones (CLI pull default);",
            "ACCEPTED_OR_NEEDS_REVIEW = accepted plus needs-review;",
            "ACCEPTED = only accepted translations.",
        ].join(" "),
    );

const localizedAssetInheritanceModeSchema = z
    .enum(["USE_PARENT", "REMOVE_UNTRANSLATED"])
    .describe(
        [
            "When a string has no translation in the target locale:",
            "USE_PARENT = fall back through parent locales, then the source (CLI pull default);",
            "REMOVE_UNTRANSLATED = omit the text unit from the generated file.",
        ].join(" "),
    );

const pseudoSubstituteTypeSchema = z
    .enum(["RANDOM", "CONSISTENT"])
    .describe(
        [
            "How accented replacements are chosen (CLI `pseudo --substitute`):",
            "RANDOM = pick a diacritic at random each time (server/CLI default; repeats may differ);",
            "CONSISTENT = same ASCII letter maps to the same replacement within a given string.",
        ].join(" "),
    );

const assetListFilterSchema = {
    repositoryId: z
        .number()
        .int()
        .positive()
        .describe("Numeric Mojito repository id whose assets to list."),
    path: z
        .string()
        .optional()
        .describe(
            "Exact logical asset path filter, e.g. `src/main/resources/messages.properties`. Omit to include every path.",
        ),
    deleted: z
        .boolean()
        .optional()
        .describe("true = only deleted assets; false = only live assets; omit = both."),
    virtual: z
        .boolean()
        .optional()
        .describe("true = only virtual assets; false = only non-virtual; omit = both."),
    branchId: z
        .number()
        .int()
        .positive()
        .optional()
        .describe("Restrict to assets associated with this Mojito branch id."),
};

function jsonResult(data: unknown) {
    return {
        content: [{ type: "text" as const, text: JSON.stringify(data, null, 2) }],
    };
}

/**
 * Registers all Mojito MCP tools on the given server.
 */
export function registerMojitoTools(server: McpServer, client: MojitoCliClient): void {
    // --- Repository ---

    server.registerTool(
        "mojito_repo_list",
        {
            description: [
                "List Mojito repositories (undeleted), optionally filtered by exact repository name.",
                "Use this to discover repository ids/names before search, or to resolve a product git repo name to a Mojito repository.",
                "Returns JSON array of repository summaries (id, name, description, locales summary depending on API view).",
                "Talks to whichever Mojito instance this MCP server was configured for (mojito-prod vs mojito-dev via MOJITO_CLI).",
            ].join(" "),
            inputSchema: {
                name: z
                    .string()
                    .optional()
                    .describe(
                        "Exact Mojito repository name filter. Omit to list all undeleted repositories. Matching is server-side exact name, not fuzzy.",
                    ),
            },
        },
        async ({ name }) => jsonResult(await client.repoList({ name })),
    );

    server.registerTool(
        "mojito_repo_view",
        {
            description: [
                "Get full details for one Mojito repository by numeric id,",
                "including description, source locale, repository locales, and integrity checkers when present.",
                "Prefer mojito_repo_list first if you only know the name.",
            ].join(" "),
            inputSchema: {
                repositoryId: z
                    .number()
                    .int()
                    .positive()
                    .describe(
                        "Numeric Mojito repository id (from mojito_repo_list or prior search results).",
                    ),
            },
        },
        async ({ repositoryId }) => jsonResult(await client.repoView(repositoryId)),
    );

    server.registerTool(
        "mojito_repo_create",
        {
            description: [
                "Create a new Mojito repository (container for strings, locales, and localization config).",
                "Requires a unique name. Optionally set description, source locale, target locales, SLA flag, and integrity checkers.",
                "WARNING: Prefer mojito-dev while experimenting. Creating repos on prod affects shared production data.",
                "On name conflict the API returns HTTP 409.",
            ].join(" "),
            inputSchema: {
                name: z
                    .string()
                    .describe(
                        "Unique repository name (often aligned with the product/git project name).",
                    ),
                description: z
                    .string()
                    .optional()
                    .describe("Optional human-readable description of the project/repository."),
                checkSLA: z
                    .boolean()
                    .optional()
                    .describe("Whether SLA tracking is enabled for this repository."),
                sourceLocale: bcp47TagSchema
                    .optional()
                    .describe(
                        "Source/root locale of the repository as a BCP-47 tag string, e.g. en-US. If omitted, server defaults apply.",
                    ),
                repositoryLocales: z
                    .array(encodedRepositoryLocaleSchema)
                    .optional()
                    .describe(
                        'Target locales as Mojito CLI -l strings, e.g. ["de-DE", "fr-FR", "(fr-CA)->fr-FR", "(en-GB)"].',
                    ),
                assetIntegrityCheckers: z
                    .array(assetIntegrityCheckerInputSchema)
                    .optional()
                    .describe(
                        'Optional integrity checkers per file extension, e.g. [{ "assetExtension": "properties", "integrityCheckerType": "PRINTF_LIKE" }].',
                    ),
            },
        },
        async (args) => jsonResult(await client.repoCreate(args)),
    );

    server.registerTool(
        "mojito_repo_delete",
        {
            description: [
                "Soft-delete a Mojito repository by id so it no longer appears in normal listings.",
                "DESTRUCTIVE: confirm the repository id and environment (prod vs dev) with the user before calling.",
                "Prefer mojito-dev for tests. Deleting on prod removes visibility of real project data from the UI.",
            ].join(" "),
            inputSchema: {
                repositoryId: z
                    .number()
                    .int()
                    .positive()
                    .describe(
                        "Numeric id of the repository to delete. Resolve via mojito_repo_list if unsure.",
                    ),
            },
        },
        async ({ repositoryId }) => jsonResult(await client.repoDelete(repositoryId)),
    );

    // --- Assets ---

    server.registerTool(
        "mojito_asset_list",
        {
            description: [
                "List source-asset summaries in a repository (id, path, deleted/virtual flags, and related metadata).",
                "GET /api/assets. repositoryId is required. Optional path, deleted, virtual, and branchId restrict the set.",
                "Returns the full filtered list in one response (the endpoint is not paginated).",
                "There is no GET-by-id for a single asset; use path + repositoryId here, or mojito_asset_ids when you only need ids.",
            ].join(" "),
            inputSchema: assetListFilterSchema,
        },
        async (args) => jsonResult(await client.assetList(args)),
    );

    server.registerTool(
        "mojito_asset_ids",
        {
            description: [
                "List numeric source-asset ids in a repository (same filters as mojito_asset_list).",
                "GET /api/assets/ids. Use this when you only need ids (for example before delete), not the summary objects.",
            ].join(" "),
            inputSchema: assetListFilterSchema,
        },
        async (args) => jsonResult(await client.assetIds(args)),
    );

    server.registerTool(
        "mojito_asset_import",
        {
            description: [
                "Create or update one source asset and start asynchronous string extraction via POST /api/assets.",
                "This is the core upload operation used by `mojito push`, but it does not scan local files, wait for completion, or delete assets omitted from the upload.",
                "The response contains `addedAssetId` and a `pollableTask`; use mojito_pollabletask_get with the returned task id until it finishes.",
                "By default content is the complete raw resource-file content and Mojito selects a parser from path; extractedContent=true is an advanced mode for Mojito pre-extracted text-unit JSON.",
                "WARNING: Importing changes source strings in the selected repository. Confirm repository id and environment; prefer mojito-dev while experimenting.",
            ].join(" "),
            inputSchema: {
                repositoryId: z
                    .number()
                    .int()
                    .positive()
                    .describe(
                        "Numeric destination repository id. Resolve it with mojito_repo_list if needed.",
                    ),
                path: z
                    .string()
                    .min(1)
                    .describe(
                        "Logical asset path stored in Mojito, including a recognizable extension, e.g. `src/main/resources/messages.properties`. This is not a local filesystem path.",
                    ),
                content: z
                    .string()
                    .describe(
                        "Complete source resource-file content. When extractedContent=true, Mojito pre-extracted text-unit JSON instead.",
                    ),
                branch: z
                    .string()
                    .optional()
                    .describe(
                        "Optional Mojito branch name. Omit to import into the repository's default/null branch.",
                    ),
                branchCreatedByUsername: z
                    .string()
                    .optional()
                    .describe("Optional username recorded as the creator of a new branch."),
                branchNotifiers: z
                    .array(z.string())
                    .optional()
                    .describe(
                        "Optional usernames to notify about this branch's localization state.",
                    ),
                pushRunName: z
                    .string()
                    .optional()
                    .describe(
                        "Optional existing or new push-run name used to associate extracted text units with a push run.",
                    ),
                filterConfigIdOverride: assetFilterConfigIdOverrideSchema.optional(),
                filterOptions: z
                    .array(z.string())
                    .optional()
                    .describe(
                        'Optional parser-specific settings in `name=value` form, e.g. ["generateHeader=false"].',
                    ),
                extractedContent: z
                    .boolean()
                    .optional()
                    .describe(
                        "Advanced: true only when content is Mojito's pre-extracted text-unit JSON. False/omitted means a normal source resource file.",
                    ),
            },
        },
        async (args) => jsonResult(await client.assetImport(args)),
    );

    server.registerTool(
        "mojito_asset_localize",
        {
            description: [
                "Generate one localized resource file for an existing source asset and locale (POST /api/assets/{assetId}/localized/{localeId}).",
                "This is the synchronous server call used by default `mojito pull`; it does not scan a working tree, write files to disk, or wait on a pollable task.",
                "Pass the complete current source-file `content` (same role as the file `mojito pull` reads locally). Mojito applies translations for numeric `localeId` and returns a LocalizedAssetBody whose `content` is the localized file and `bcp47Tag` is the tag to use in the output path.",
                "Resolve `assetId` via mojito_asset_list and `localeId` via mojito_repo_view or text-unit search — do not pass a BCP-47 tag as localeId.",
                "Async `/localized` and parallel `/localized/parallel` endpoints are not exposed; omit pullRunName unless you intentionally want Mojito to record a pull run.",
            ].join(" "),
            inputSchema: {
                assetId: z
                    .number()
                    .int()
                    .positive()
                    .describe(
                        "Numeric source asset id. Resolve via mojito_asset_list (there is no GET-by-id).",
                    ),
                localeId: z
                    .number()
                    .int()
                    .positive()
                    .describe(
                        "Numeric Mojito locale id whose translations to apply. Not a BCP-47 tag; take it from mojito_repo_view or a text-unit row.",
                    ),
                content: z
                    .string()
                    .describe(
                        "Complete current source resource-file content to localize (typically the English/source file, possibly with local edits vs what is stored in Mojito).",
                    ),
                outputBcp47tag: z
                    .string()
                    .optional()
                    .describe(
                        "Optional output BCP-47 tag for the generated file, e.g. `fr` while translations are stored on locale `fr-FR`. Omit to use the repository locale tag.",
                    ),
                filterConfigIdOverride: assetFilterConfigIdOverrideSchema.optional(),
                filterOptions: z
                    .array(z.string())
                    .optional()
                    .describe(
                        'Optional parser-specific settings in `name=value` form, e.g. ["generateHeader=false"].',
                    ),
                inheritanceMode: localizedAssetInheritanceModeSchema.optional(),
                status: localizedAssetStatusSchema.optional(),
                pullRunName: z
                    .string()
                    .optional()
                    .describe(
                        "Optional name under which Mojito records a pull run of the text-unit variants used. Omit for a generate-only call.",
                    ),
            },
        },
        async (args) => jsonResult(await client.assetLocalize(args)),
    );

    server.registerTool(
        "mojito_asset_pseudo",
        {
            description: [
                "Generate one pseudolocalized resource file for an existing source asset (POST /api/assets/{assetId}/pseudo).",
                "This is the synchronous server call used by `mojito pseudo`; it does not scan a working tree, write files to disk, or wait on a pollable task.",
                "Pass the complete current source-file `content` (same role as the file `mojito pseudo` reads locally). Mojito replaces source characters with accented alternatives so missing translations and layout issues are visible before real locales exist.",
                "No localeId: output is a synthetic locale. When writing the returned `content` to disk, use the path tag `en-x-pseudo` (CLI convention). The generate pipeline uses `en-x-psaccent` internally; do not use that as the filename tag.",
                "Optional `substituteType`: RANDOM (default) or CONSISTENT. Optional filter override/options match the Java client; the server currently uses filterConfigIdOverride only.",
            ].join(" "),
            inputSchema: {
                assetId: z
                    .number()
                    .int()
                    .positive()
                    .describe(
                        "Numeric source asset id. Resolve via mojito_asset_list (there is no GET-by-id).",
                    ),
                content: z
                    .string()
                    .describe(
                        "Complete current source resource-file content to pseudolocalize (typically the English/source file).",
                    ),
                outputBcp47tag: z
                    .string()
                    .optional()
                    .describe(
                        "Optional output BCP-47 tag sent on the request (Java client always sends `en-x-pseudo`). Omit to send that CLI default. The server does not use this to fetch translations; still write files using `en-x-pseudo`.",
                    ),
                filterConfigIdOverride: assetFilterConfigIdOverrideSchema.optional(),
                filterOptions: z
                    .array(z.string())
                    .optional()
                    .describe(
                        'Optional parser-specific settings in `name=value` form, e.g. ["generateHeader=false"]. The Java client sends these; the current pseudo endpoint only applies filterConfigIdOverride.',
                    ),
                substituteType: pseudoSubstituteTypeSchema.optional(),
            },
        },
        async (args) => jsonResult(await client.assetPseudo(args)),
    );

    server.registerTool(
        "mojito_asset_delete",
        {
            description: [
                "Delete one source asset by numeric id (DELETE /api/assets/{assetId}).",
                "DESTRUCTIVE: confirm the asset id, repository, and environment (prod vs dev) with the user before calling.",
                "Prefer mojito-dev for tests. This is a single-asset delete; it does not bulk-delete unused assets the way `mojito push` cleanup does.",
            ].join(" "),
            inputSchema: {
                assetId: z
                    .number()
                    .int()
                    .positive()
                    .describe(
                        "Numeric asset id to delete. Resolve via mojito_asset_list or mojito_asset_ids.",
                    ),
            },
        },
        async ({ assetId }) => jsonResult(await client.assetDelete(assetId)),
    );

    // --- Text units ---

    server.registerTool(
        "mojito_textunit_search",
        {
            description: [
                "Search translation-memory / workbench text units — primary tool for “where is my string?” and listing translations.",
                "You can match against string id (name), source text, and/or target (translated) text via searchType.",
                "Scoping: omit repositoryIds and repositoryNames to search ALL repositories; omit localeTags to include ALL locales.",
                "Provide repositoryIds, repositoryNames, and/or localeTags to restrict. Prefer scoping by repo when possible (all-repo search can be large).",
                "You may also pass tmTextUnitIds to fetch specific units. Returns TextUnitDTO-like JSON (ids, source/target, status, asset path, dates, used flag, etc.).",
            ].join(" "),
            inputSchema: {
                repositoryIds: z
                    .array(z.number().int().positive())
                    .optional()
                    .describe(
                        "Restrict to these repository ids. Omit (with repositoryNames also omitted) to search all repositories.",
                    ),
                repositoryNames: z
                    .array(z.string())
                    .optional()
                    .describe(
                        "Restrict to these exact Mojito repository names. Omit (with repositoryIds also omitted) for all repos.",
                    ),
                tmTextUnitIds: z
                    .array(z.number().int().positive())
                    .optional()
                    .describe(
                        "Restrict to these TM text unit ids. When set, repository lists are not required.",
                    ),
                localeTags: z
                    .array(z.string())
                    .optional()
                    .describe(
                        "Restrict to these BCP-47 locale tags (e.g. fr-FR, ja-JP). Omit to include all locales.",
                    ),
                name: z
                    .string()
                    .optional()
                    .describe(
                        "Filter by string id / resource key (TextUnit name), matched according to searchType.",
                    ),
                source: z
                    .string()
                    .optional()
                    .describe(
                        "Filter by source (typically English) text, matched according to searchType.",
                    ),
                target: z
                    .string()
                    .optional()
                    .describe(
                        "Filter by target translation text for the selected locales, matched according to searchType.",
                    ),
                assetPath: z
                    .string()
                    .optional()
                    .describe("Filter by asset/path of the resource file containing the string."),
                pluralFormOther: z
                    .string()
                    .optional()
                    .describe(
                        "Filter related to plural “other” form content when working with plurals.",
                    ),
                searchType: searchTypeSchema.optional(),
                statusFilter: statusFilterSchema.optional(),
                usedFilter: usedFilterSchema.optional(),
                doNotTranslateFilter: z
                    .boolean()
                    .optional()
                    .describe(
                        "If true, only do-not-translate (DNT) strings; if false, only non-DNT; omit for both.",
                    ),
                tmTextUnitCreatedAfter: z
                    .string()
                    .optional()
                    .describe(
                        "Only text units created at/after this instant (ISO-8601 datetime, e.g. 2024-01-15T00:00:00Z).",
                    ),
                tmTextUnitCreatedBefore: z
                    .string()
                    .optional()
                    .describe(
                        "Only text units created at/before this instant (ISO-8601 datetime).",
                    ),
                branchId: z
                    .number()
                    .int()
                    .positive()
                    .optional()
                    .describe("Restrict search to strings associated with this Mojito branch id."),
                pluralFormFiltered: z
                    .boolean()
                    .optional()
                    .describe(
                        "API default true. Controls plural-form row filtering in search results.",
                    ),
                pluralFormExcluded: z
                    .boolean()
                    .optional()
                    .describe(
                        "API default false. When true, excludes plural forms per server rules.",
                    ),
                limit: z
                    .number()
                    .int()
                    .positive()
                    .optional()
                    .describe(
                        "Maximum rows to return. When set, the tool makes one unpaginated request so the limit is preserved.",
                    ),
                offset: z
                    .number()
                    .int()
                    .nonnegative()
                    .optional()
                    .describe(
                        "Starting offset for pagination (advanced; usually omit and let the tool paginate).",
                    ),
            },
        },
        async (args) => jsonResult(await client.textunitSearch(args)),
    );

    server.registerTool(
        "mojito_textunit_info",
        {
            description: [
                "Get general information about one TM text unit: created date, current status, source/target,",
                "asset path, repository name, used/unused, do-not-translate, plural fields, and related ids.",
                "Implemented via search by tmTextUnitId (there is no dedicated GET-by-id endpoint).",
                "Optional localeTags restrict which locale rows are returned; omit for all locales.",
            ].join(" "),
            inputSchema: {
                tmTextUnitId: z
                    .number()
                    .int()
                    .positive()
                    .describe(
                        "TM text unit id (from mojito_textunit_search results: tmTextUnitId).",
                    ),
                localeTags: z
                    .array(z.string())
                    .optional()
                    .describe(
                        "Optional BCP-47 tags to limit which locale variants are returned. Omit for all locales.",
                    ),
            },
        },
        async (args) => jsonResult(await client.textunitInfo(args)),
    );

    server.registerTool(
        "mojito_textunit_history",
        {
            description: [
                "Get translation change history for a TM text unit in one locale (who/what changed over time).",
                "Requires tmTextUnitId and bcp47Tag. Use after search/info when you need historical variants, not just the current translation.",
            ].join(" "),
            inputSchema: {
                tmTextUnitId: z
                    .number()
                    .int()
                    .positive()
                    .describe("TM text unit id whose history you want."),
                bcp47Tag: z
                    .string()
                    .describe(
                        "Locale BCP-47 tag for the history, e.g. fr-FR. Required by the Mojito API (not locale id).",
                    ),
            },
        },
        async (args) => jsonResult(await client.textunitHistory(args)),
    );

    server.registerTool(
        "mojito_textunit_translation_add",
        {
            description: [
                "Add or set the current translation for a text unit in a locale (creates a new current TMTextUnitVariant).",
                "Requires tmTextUnitId, localeId (numeric locale id from search results), and target text.",
                "Optional status and includedInLocalizedFile control review state; optional targetComment stores a translator/reviewer comment.",
                "WARNING: Prefer mojito-dev for experiments. On prod this changes live translations.",
                "For workbench-style accept/reject/needs-review flows, prefer mojito_review_update.",
            ].join(" "),
            inputSchema: {
                tmTextUnitId: z.number().int().positive().describe("TM text unit id to translate."),
                localeId: z
                    .number()
                    .int()
                    .positive()
                    .describe(
                        "Numeric Mojito locale id (from search/info results: localeId), not the BCP-47 tag.",
                    ),
                target: z.string().describe("Translation text to save as the current target."),
                targetComment: z
                    .string()
                    .optional()
                    .describe("Optional comment attached to this translation variant."),
                status: textUnitStatusSchema.optional(),
                includedInLocalizedFile: z
                    .boolean()
                    .optional()
                    .describe(
                        "If false, translation is treated as rejected (not included in localized files). Default/typical for good translations is true.",
                    ),
            },
        },
        async (args) => jsonResult(await client.textunitTranslationAdd(args)),
    );

    // --- Review ---

    server.registerTool(
        "mojito_review_update",
        {
            description: [
                "Update review outcome for a current translation, matching the Mojito workbench review modal.",
                "Actions: accept (APPROVED), review (REVIEW_NEEDED), translate (TRANSLATION_NEEDED), reject (TRANSLATION_NEEDED + excluded from file).",
                "Requires the current target text because the underlying API updates via POST /api/textunits.",
                "WARNING: Prefer mojito-dev for experiments. On prod this changes live review state.",
            ].join(" "),
            inputSchema: {
                tmTextUnitId: z
                    .number()
                    .int()
                    .positive()
                    .describe("TM text unit id of the string being reviewed."),
                localeId: z
                    .number()
                    .int()
                    .positive()
                    .describe("Numeric locale id for the translation being reviewed."),
                target: z
                    .string()
                    .describe(
                        "Current translation text to keep/save with the new review status (required by the API).",
                    ),
                action: reviewActionSchema,
                targetComment: z
                    .string()
                    .optional()
                    .describe("Optional review comment stored on the translation variant."),
            },
        },
        async (args) => jsonResult(await client.reviewUpdate(args)),
    );

    // --- Pollable tasks ---

    server.registerTool(
        "mojito_pollabletask_get",
        {
            description: [
                "Fetch status of an asynchronous Mojito pollable task by id",
                "(imports, batch jobs, and other long-running operations that return a PollableTask).",
                "Use when a previous operation returned a pollableTask id; poll until allFinished is true or an error appears.",
                "This tool does not wait/block; call again as needed.",
            ].join(" "),
            inputSchema: {
                pollableTaskId: z
                    .number()
                    .int()
                    .positive()
                    .describe("Pollable task id from a previous Mojito async response."),
            },
        },
        async ({ pollableTaskId }) => jsonResult(await client.pollabletaskGet(pollableTaskId)),
    );
}
