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
