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

/**
 * Shared parameter / enum types for Mojito MCP tools and the CLI client.
 * Keep in sync with the zod schemas in {@link registerMojitoTools}.
 */

/** How name / source / target string filters match in text-unit search. */
export type SearchType = "EXACT" | "CONTAINS" | "ILIKE";

/** Whether the string is still present in the latest asset extraction. */
export type UsedFilter = "USED" | "UNUSED";

/**
 * Workbench status buckets for search (not the same as a single variant status).
 * Combines translation presence, TMTextUnitVariant.Status, and includedInLocalizedFile.
 */
export type StatusFilter =
    | "ALL"
    | "TRANSLATED"
    | "UNTRANSLATED"
    | "TRANSLATED_AND_NOT_REJECTED"
    | "APPROVED_OR_NEEDS_REVIEW_AND_NOT_REJECTED"
    | "APPROVED_AND_NOT_REJECTED"
    | "FOR_TRANSLATION"
    | "REVIEW_NEEDED"
    | "REVIEW_NEEDED_OR_REJECTED"
    | "REVIEW_NOT_NEEDED"
    | "TRANSLATION_NEEDED"
    | "REJECTED"
    | "NOT_REJECTED";

/** Workbench review modal actions for {@link ReviewUpdateParams}. */
export type ReviewAction = "accept" | "review" | "translate" | "reject";

/** Current-translation status written by translation_add / review_update. */
export type TextUnitStatus = "APPROVED" | "REVIEW_NEEDED" | "TRANSLATION_NEEDED";

/**
 * Parameters for text-unit search.
 * Omit repository lists = all repositories; omit localeTags = all locales.
 */
export type TextUnitSearchParams = {
    repositoryIds?: number[];
    repositoryNames?: string[];
    tmTextUnitIds?: number[];
    localeTags?: string[];
    name?: string;
    source?: string;
    target?: string;
    assetPath?: string;
    pluralFormOther?: string;
    searchType?: SearchType;
    statusFilter?: StatusFilter;
    usedFilter?: UsedFilter;
    doNotTranslateFilter?: boolean;
    tmTextUnitCreatedAfter?: string;
    tmTextUnitCreatedBefore?: string;
    branchId?: number;
    pluralFormFiltered?: boolean;
    pluralFormExcluded?: boolean;
    limit?: number;
    offset?: number;
};

/**
 * Encoded repository locale string, same syntax as `mojito repo-create -l`:
 * - `fr-FR` — fully translated locale
 * - `(en-GB)` — not fully translated (parentheses)
 * - `(fr-CA)->fr-FR` — fr-CA inherits from parent fr-FR (and is not fully translated)
 */
export type EncodedRepositoryLocale = string;

/** Integrity checker binding for an asset file extension. */
export type AssetIntegrityCheckerInput = {
    /** File extension without dot, e.g. `properties`, `resw`, `xlf`. */
    assetExtension: string;
    /**
     * Checker type name as configured in Mojito, e.g. `COMPOSITE_FORMAT`, `PRINTF_LIKE`.
     * See Mojito integrity-checker docs for valid values.
     */
    integrityCheckerType: string;
};

/** Okapi filter override accepted by the source-asset import API. */
export type AssetFilterConfigIdOverride =
    | "PROPERTIES_JAVA"
    | "MACSTRINGSDICT_FILTER_KEY"
    | "XCODE_XLIFF"
    | "CSV_ADOBE_MAGENTO"
    | "HTML_ALPHA";

/**
 * Shared query filters for GET /api/assets and GET /api/assets/ids.
 * `repositoryId` is required; other fields restrict the result when present.
 */
export type AssetListParams = {
    repositoryId: number;
    /** Exact logical asset path. Omit to include every path in the repository. */
    path?: string;
    /** true = only deleted assets; false = only live assets; omit = both. */
    deleted?: boolean;
    /** true = only virtual assets; false = only non-virtual; omit = both. */
    virtual?: boolean;
    /** Restrict to assets associated with this Mojito branch id. */
    branchId?: number;
};

/**
 * Parameters for POST /api/assets.
 *
 * The operation creates or updates the asset at `path`, then starts asynchronous
 * extraction. Its response contains the asset id and a pollable task.
 */
export type AssetImportParams = {
    repositoryId: number;
    path: string;
    content: string;
    branch?: string;
    branchCreatedByUsername?: string;
    branchNotifiers?: string[];
    pushRunName?: string;
    filterConfigIdOverride?: AssetFilterConfigIdOverride;
    filterOptions?: string[];
    /**
     * False/omitted means `content` is a source resource file. True means it is
     * Mojito's pre-extracted text-unit JSON format.
     */
    extractedContent?: boolean;
};

/** Which translations are eligible when generating a localized asset. */
export type LocalizedAssetStatus = "ALL" | "ACCEPTED_OR_NEEDS_REVIEW" | "ACCEPTED";

/**
 * When a string has no translation in the target locale, either fall back through
 * parent locales (and ultimately the source) or drop the text unit from the file.
 */
export type LocalizedAssetInheritanceMode = "USE_PARENT" | "REMOVE_UNTRANSLATED";

/**
 * Parameters for POST /api/assets/{assetId}/localized/{localeId}.
 *
 * This is the synchronous localize call used by default `mojito pull`: the request
 * body is the current source resource content; the response body's `content` is the
 * generated localized file. It does not scan a working tree or write files to disk.
 */
export type AssetLocalizeParams = {
    assetId: number;
    localeId: number;
    content: string;
    /** Generate the file tagged with this BCP-47 value while still fetching translations for localeId. */
    outputBcp47tag?: string;
    filterConfigIdOverride?: AssetFilterConfigIdOverride;
    filterOptions?: string[];
    inheritanceMode?: LocalizedAssetInheritanceMode;
    status?: LocalizedAssetStatus;
    /** If set, Mojito records a pull run under this name when generating the file. */
    pullRunName?: string;
};

export type RepoCreateParams = {
    name: string;
    description?: string;
    /** Source / root locale as a BCP-47 tag, e.g. `en-US`. */
    sourceLocale?: string;
    checkSLA?: boolean;
    /**
     * Target locales using Mojito CLI encoding (`fr-FR`, `(en-GB)`, `(fr-CA)->fr-FR`).
     * The client parses these into Mojito's nested RepositoryLocale JSON.
     */
    repositoryLocales?: EncodedRepositoryLocale[];
    assetIntegrityCheckers?: AssetIntegrityCheckerInput[];
};

export type TextUnitTranslationAddParams = {
    tmTextUnitId: number;
    localeId: number;
    target: string;
    targetComment?: string;
    status?: TextUnitStatus;
    includedInLocalizedFile?: boolean;
};

export type ReviewUpdateParams = {
    tmTextUnitId: number;
    localeId: number;
    target: string;
    action: ReviewAction;
    targetComment?: string;
};
