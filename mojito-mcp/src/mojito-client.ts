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

import { mkdtempSync, writeFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import type { CliRunner } from "./cli-runner.js";
import type { MojitoMcpConfig } from "./config.js";
import { MojitoCliError } from "./errors.js";
import type {
    EncodedRepositoryLocale,
    RepoCreateParams,
    ReviewAction,
    ReviewUpdateParams,
    TextUnitSearchParams,
    TextUnitTranslationAddParams,
    TextUnitStatus,
} from "./types.js";

const PAGINATE_FLAGS = ["--paginate", "--slurp", "--max-pages", "0"] as const;

/**
 * The CLI's offset-style pagination stops when a page comes back shorter than
 * `--page-size`, so the page size must be large enough to make a short final
 * page likely but small enough to keep each request cheap.
 */
const SEARCH_PAGE_SIZE = 500;

const REVIEW_ACTION_MAP: Record<
    ReviewAction,
    { status: TextUnitStatus; includedInLocalizedFile: boolean }
> = {
    accept: { status: "APPROVED", includedInLocalizedFile: true },
    review: { status: "REVIEW_NEEDED", includedInLocalizedFile: true },
    translate: { status: "TRANSLATION_NEEDED", includedInLocalizedFile: true },
    reject: { status: "TRANSLATION_NEEDED", includedInLocalizedFile: false },
};

/**
 * Mojito API access via the CLI `api` command.
 */
export class MojitoCliClient {
    constructor(
        private readonly _config: MojitoMcpConfig,
        private readonly _runner: CliRunner,
    ) {}

    get config(): MojitoMcpConfig {
        return this._config;
    }

    get runner(): CliRunner {
        return this._runner;
    }

    /** Startup probe: `{cli} --help`. Throws {@link MojitoCliError} if the CLI is missing or fails. */
    async probeHelp(): Promise<void> {
        await this.runChecked(["--help"]);
    }

    /**
     * GET /api/repositories — returns every repository in one response.
     *
     * Not paginated: the endpoint ignores offset/limit, so `--paginate` would
     * loop forever because each "page" comes back full.
     */
    async repoList(params: { name?: string } = {}): Promise<unknown> {
        const argv = ["api", "/api/repositories"];
        if (params.name !== undefined) {
            pushRaw(argv, "name", params.name);
        }
        return this.apiJson(argv);
    }

    /** GET /api/repositories/{repositoryId} */
    async repoView(repositoryId: number): Promise<unknown> {
        return this.apiJson(["api", `/api/repositories/${repositoryId}`]);
    }

    /** POST /api/repositories */
    async repoCreate(params: RepoCreateParams): Promise<unknown> {
        if (needsRepoCreateJsonBody(params)) {
            return this.apiJsonWithInput(
                ["api", "/api/repositories", "-X", "POST"],
                buildRepoCreateBody(params),
            );
        }

        const argv = ["api", "/api/repositories", "-X", "POST"];
        pushRaw(argv, "name", params.name);
        if (params.description !== undefined) {
            pushRaw(argv, "description", params.description);
        }
        if (params.checkSLA !== undefined) {
            pushTyped(argv, "checkSLA", params.checkSLA);
        }
        return this.apiJson(argv);
    }

    /** DELETE /api/repositories/{repositoryId} */
    async repoDelete(repositoryId: number): Promise<unknown> {
        return this.apiJson(["api", `/api/repositories/${repositoryId}`, "-X", "DELETE"]);
    }

    /**
     * POST /api/textunits/search — paginate + slurp + max-pages 0.
     * Omit repo lists → expand to all repo ids; omit localeTags → all locales.
     */
    async textunitSearch(params: TextUnitSearchParams = {}): Promise<unknown> {
        const effective = { ...params };

        const hasRepoScope =
            (effective.repositoryIds?.length ?? 0) > 0 ||
            (effective.repositoryNames?.length ?? 0) > 0;
        const hasTmIds = (effective.tmTextUnitIds?.length ?? 0) > 0;

        if (!hasRepoScope && !hasTmIds) {
            const repos = await this.repoList();
            effective.repositoryIds = extractRepositoryIds(repos);
            if (effective.repositoryIds.length === 0) {
                return [];
            }
        }

        const argv = ["api", "/api/textunits/search", "-X", "POST"];
        // `--paginate` overwrites offset/limit in the request body, so an explicit
        // limit only survives on a single unpaginated request.
        if (effective.limit === undefined) {
            argv.push(...PAGINATE_FLAGS, "--page-size", String(SEARCH_PAGE_SIZE));
        }
        appendSearchFields(argv, effective);
        return this.apiJson(argv);
    }

    /** Detail via search with tmTextUnitIds (optional localeTags). */
    async textunitInfo(params: { tmTextUnitId: number; localeTags?: string[] }): Promise<unknown> {
        const argv = ["api", "/api/textunits/search", "-X", "POST"];
        pushTypedArray(argv, "tmTextUnitIds", [params.tmTextUnitId]);
        if (params.localeTags?.length) {
            pushRawArray(argv, "localeTags", params.localeTags);
        }
        return this.apiJson(argv);
    }

    /** GET /api/textunits/{tmTextUnitId}/history?bcp47Tag=… */
    async textunitHistory(params: { tmTextUnitId: number; bcp47Tag: string }): Promise<unknown> {
        const argv = ["api", `/api/textunits/${params.tmTextUnitId}/history`];
        pushRaw(argv, "bcp47Tag", params.bcp47Tag);
        return this.apiJson(argv);
    }

    /** POST /api/textunits — add current translation */
    async textunitTranslationAdd(params: TextUnitTranslationAddParams): Promise<unknown> {
        const argv = ["api", "/api/textunits", "-X", "POST"];
        pushTyped(argv, "tmTextUnitId", params.tmTextUnitId);
        pushTyped(argv, "localeId", params.localeId);
        pushRaw(argv, "target", params.target);
        if (params.targetComment !== undefined) {
            pushRaw(argv, "targetComment", params.targetComment);
        }
        if (params.status !== undefined) {
            pushRaw(argv, "status", params.status);
        }
        if (params.includedInLocalizedFile !== undefined) {
            pushTyped(argv, "includedInLocalizedFile", params.includedInLocalizedFile);
        }
        return this.apiJson(argv);
    }

    /** POST /api/textunits — workbench review action */
    async reviewUpdate(params: ReviewUpdateParams): Promise<unknown> {
        const mapped = REVIEW_ACTION_MAP[params.action];
        const argv = ["api", "/api/textunits", "-X", "POST"];
        pushTyped(argv, "tmTextUnitId", params.tmTextUnitId);
        pushTyped(argv, "localeId", params.localeId);
        pushRaw(argv, "target", params.target);
        pushRaw(argv, "status", mapped.status);
        pushTyped(argv, "includedInLocalizedFile", mapped.includedInLocalizedFile);
        if (params.targetComment !== undefined) {
            pushRaw(argv, "targetComment", params.targetComment);
        }
        return this.apiJson(argv);
    }

    /** GET /api/pollableTasks/{pollableTaskId} */
    async pollabletaskGet(pollableTaskId: number): Promise<unknown> {
        return this.apiJson(["api", `/api/pollableTasks/${pollableTaskId}`]);
    }

    // --- internals ---

    private async runChecked(argv: string[]): Promise<{ stdout: string; stderr: string }> {
        const result = await this._runner.run(argv);
        if (result.exitCode !== 0) {
            const summary =
                result.stderr.trim() || `mojito CLI exited with code ${result.exitCode}`;
            throw new MojitoCliError(summary, {
                exitCode: result.exitCode,
                stdout: result.stdout,
                stderr: result.stderr,
            });
        }
        return { stdout: result.stdout, stderr: result.stderr };
    }

    private async apiJson(argv: string[]): Promise<unknown> {
        const { stdout } = await this.runChecked(argv);
        return parseStdoutJson(stdout);
    }

    private async apiJsonWithInput(argv: string[], body: unknown): Promise<unknown> {
        const dir = mkdtempSync(join(tmpdir(), "mojito-mcp-"));
        const file = join(dir, "body.json");
        try {
            writeFileSync(file, JSON.stringify(body), "utf8");
            return await this.apiJson([...argv, "--input", file]);
        } finally {
            rmSync(dir, { recursive: true, force: true });
        }
    }
}

function parseStdoutJson(stdout: string): unknown {
    const trimmed = stdout.trim();
    if (trimmed === "") {
        return null;
    }
    try {
        return JSON.parse(trimmed) as unknown;
    } catch (cause) {
        throw new MojitoCliError("Failed to parse mojito CLI JSON stdout", {
            stdout,
            cause,
        });
    }
}

function pushRaw(argv: string[], key: string, value: string): void {
    argv.push("-f", `${key}=${value}`);
}

function pushTyped(argv: string[], key: string, value: string | number | boolean): void {
    argv.push("-F", `${key}=${String(value)}`);
}

function pushRawArray(argv: string[], key: string, values: string[]): void {
    for (const value of values) {
        pushRaw(argv, `${key}[]`, value);
    }
}

function pushTypedArray(argv: string[], key: string, values: number[]): void {
    for (const value of values) {
        pushTyped(argv, `${key}[]`, value);
    }
}

function appendSearchFields(argv: string[], params: TextUnitSearchParams): void {
    if (params.repositoryIds?.length) {
        pushTypedArray(argv, "repositoryIds", params.repositoryIds);
    }
    if (params.repositoryNames?.length) {
        pushRawArray(argv, "repositoryNames", params.repositoryNames);
    }
    if (params.tmTextUnitIds?.length) {
        pushTypedArray(argv, "tmTextUnitIds", params.tmTextUnitIds);
    }
    if (params.localeTags?.length) {
        pushRawArray(argv, "localeTags", params.localeTags);
    }
    if (params.name !== undefined) {
        pushRaw(argv, "name", params.name);
    }
    if (params.source !== undefined) {
        pushRaw(argv, "source", params.source);
    }
    if (params.target !== undefined) {
        pushRaw(argv, "target", params.target);
    }
    if (params.assetPath !== undefined) {
        pushRaw(argv, "assetPath", params.assetPath);
    }
    if (params.pluralFormOther !== undefined) {
        pushRaw(argv, "pluralFormOther", params.pluralFormOther);
    }
    if (params.searchType !== undefined) {
        pushRaw(argv, "searchType", params.searchType);
    }
    if (params.statusFilter !== undefined) {
        pushRaw(argv, "statusFilter", params.statusFilter);
    }
    if (params.usedFilter !== undefined) {
        pushRaw(argv, "usedFilter", params.usedFilter);
    }
    if (params.doNotTranslateFilter !== undefined) {
        pushTyped(argv, "doNotTranslateFilter", params.doNotTranslateFilter);
    }
    if (params.tmTextUnitCreatedAfter !== undefined) {
        pushRaw(argv, "tmTextUnitCreatedAfter", params.tmTextUnitCreatedAfter);
    }
    if (params.tmTextUnitCreatedBefore !== undefined) {
        pushRaw(argv, "tmTextUnitCreatedBefore", params.tmTextUnitCreatedBefore);
    }
    if (params.branchId !== undefined) {
        pushTyped(argv, "branchId", params.branchId);
    }
    if (params.pluralFormFiltered !== undefined) {
        pushTyped(argv, "pluralFormFiltered", params.pluralFormFiltered);
    }
    if (params.pluralFormExcluded !== undefined) {
        pushTyped(argv, "pluralFormExcluded", params.pluralFormExcluded);
    }
    if (params.limit !== undefined) {
        pushTyped(argv, "limit", params.limit);
    }
    if (params.offset !== undefined) {
        pushTyped(argv, "offset", params.offset);
    }
}

function extractRepositoryIds(repos: unknown): number[] {
    if (!Array.isArray(repos)) {
        throw new MojitoCliError(
            "Expected repository list to be a JSON array when expanding all repositories",
            { stdout: JSON.stringify(repos) },
        );
    }
    const ids: number[] = [];
    for (const repo of repos) {
        const id =
            repo && typeof repo === "object" && "id" in repo
                ? (repo as { id: unknown }).id
                : undefined;
        if (typeof id !== "number" || !Number.isSafeInteger(id) || id <= 0) {
            throw new MojitoCliError(
                "Expected every repository to have a positive integer id when expanding all repositories",
                { stdout: JSON.stringify(repos) },
            );
        }
        ids.push(id);
    }
    return ids;
}

function needsRepoCreateJsonBody(params: RepoCreateParams): boolean {
    return (
        params.sourceLocale !== undefined ||
        (params.repositoryLocales?.length ?? 0) > 0 ||
        (params.assetIntegrityCheckers?.length ?? 0) > 0
    );
}

function buildRepoCreateBody(params: RepoCreateParams): Record<string, unknown> {
    const body: Record<string, unknown> = { name: params.name };
    if (params.description !== undefined) {
        body.description = params.description;
    }
    if (params.checkSLA !== undefined) {
        body.checkSLA = params.checkSLA;
    }
    if (params.sourceLocale !== undefined) {
        body.sourceLocale = { bcp47Tag: params.sourceLocale };
    }
    if (params.repositoryLocales?.length) {
        body.repositoryLocales = params.repositoryLocales.map(parseEncodedRepositoryLocale);
    }
    if (params.assetIntegrityCheckers?.length) {
        body.assetIntegrityCheckers = params.assetIntegrityCheckers;
    }
    return body;
}

/**
 * Parse Mojito CLI `-l` encoding into nested RepositoryLocale JSON.
 * Mirrors {@code LocaleHelper.getRepositoryLocaleFromEncodedBcp47Tag}.
 */
export function parseEncodedRepositoryLocale(
    encoded: EncodedRepositoryLocale,
): Record<string, unknown> {
    const parts = encoded.split("->").map((p) => p.trim());
    if (parts.some((part) => part === "")) {
        throw new MojitoCliError(`Invalid encoded repository locale: ${encoded}`);
    }

    let toBeFullyTranslated = true;
    const locales: string[] = [];

    for (const part of parts) {
        if (/^\(\s*\)$/.test(part)) {
            throw new MojitoCliError(`Invalid encoded repository locale: ${encoded}`);
        }
        const m = /^\((.+)\)$/.exec(part);
        if (m) {
            const locale = m[1].trim();
            if (locale === "") {
                throw new MojitoCliError(`Invalid encoded repository locale: ${encoded}`);
            }
            toBeFullyTranslated = false;
            locales.push(locale);
        } else {
            locales.push(part);
        }
    }

    const [child, ...parents] = locales;
    const repositoryLocale: Record<string, unknown> = {
        locale: { bcp47Tag: child },
        toBeFullyTranslated,
    };

    let cursor = repositoryLocale;
    for (const parent of parents) {
        const parentNode: Record<string, unknown> = {
            locale: { bcp47Tag: parent },
        };
        cursor.parentLocale = parentNode;
        cursor = parentNode;
    }

    return repositoryLocale;
}
