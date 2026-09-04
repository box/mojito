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
 * Test scenarios: translating tool calls into Mojito CLI invocations.
 *
 * This is the contract between the MCP tool surface and the `mojito api` command line.
 * A fake CliRunner records the argv it is handed and returns canned output, so every
 * case below is an assertion about the command we would have run, with no Mojito
 * install, no authentication, and no network involved.
 *
 * The file covers two units, in this order.
 *
 * ## Encoded repository locales (`parseEncodedRepositoryLocale`)
 *
 * Repository locales reach us as a compact string so an agent can express a whole locale
 * tree without nested JSON: `fr-CA` is a locale, parentheses mean "not fully translated",
 * and `->` points at the locale it inherits from. The parser turns that into the nested
 * shape Mojito's API expects, and getting it wrong would silently misconfigure which
 * locales a repository translates.
 *
 * 1. A bare tag such as `fr-FR` is fully translated and has no parent, the common case.
 * 2. Parentheses, as in `(en-GB)`, mark the locale as not fully translated.
 * 3. `(fr-CA)->fr-FR` combines both features: inherits from a parent and is not fully
 *    translated, which is the standard shape for a regional variant.
 * 4. `fr-CA->fr-FR` inherits from a parent while staying fully translated, confirming the
 *    two features are independent rather than implied by each other.
 * 5. A three-level chain nests correctly, so inheritance is not capped at one hop.
 * 6. Parentheses on a later segment (`fr-CA->(fr-FR)`) still mark the child as not fully
 *    translated. This pins a deliberate decision: the flag is read from the whole string,
 *    not only from the leading segment.
 * 7. Whitespace around segments and around the arrow is trimmed, because these strings are
 *    typed by hand and by agents.
 * 8. A language-only tag such as `ja` is accepted; a region is not required.
 * 9. An empty string is rejected with a MojitoCliError naming the bad input, so a blank
 *    field fails loudly rather than creating a nameless locale.
 * 10. A whitespace-only string is rejected the same way.
 * 11. A missing child before the arrow, as in `->fr-FR`, is rejected: a parent with nothing
 *     inheriting from it is meaningless.
 * 12. Unbalanced parentheses like `(fr-FR` are *not* an error; they are kept as a literal
 *     tag and left for the server to reject. This documents that we only strip a matched
 *     pair rather than guessing at the author's intent.
 * 13. Nested parentheses strip only the outer pair, for the same reason.
 * 14. A trailing arrow (`fr-FR->`) is rejected rather than producing a parent with an
 *     empty tag that Mojito cannot use.
 * 15. Parentheses at more than one level of a chain apply to their own segment.
 * 16. A comma is not a separator: `de-DE,fr-FR` stays one literal tag. Agents are likely to
 *     try comma-separated lists, and this fixes what happens when they do.
 * 17. Commas inside a chain likewise stay part of each segment's tag.
 * 18. An empty middle segment and empty parentheses are rejected. Both otherwise produce
 *     malformed locale trees that fail later and less clearly at the Mojito API.
 *
 * ## CLI argument contracts (`MojitoCliClient`)
 *
 * One case per tool, asserting the exact `api` path, HTTP method, pagination flags, and
 * field encoding. `-f` sends a value as a string and `-F` sends it typed, so mixing them
 * up would send Mojito the wrong JSON types.
 *
 * 1. The startup probe runs `--help` and succeeds on exit 0. This is what the server does
 *    before serving traffic, and it must not touch the network.
 * 2. The startup probe fails on a non-zero exit, which is how a missing or broken CLI
 *    wrapper stops the server at boot instead of failing later on every tool call.
 * 3. Listing repositories deliberately does *not* paginate. The endpoint ignores offset and
 *    limit and returns everything at once, so `--paginate` would loop forever on pages that
 *    always look full. The most important case in this group.
 * 4. Listing with a name filter passes it as a string field.
 * 5. Viewing a repository is a plain GET on the id.
 * 6. Creating a simple repository POSTs with an explicit `-X POST` and string fields.
 * 7. Creating a repository with several derived locales switches to a JSON body written to a
 *    temp file and passed with `--input`, because the nested locale tree cannot be expressed
 *    as flat fields. The assertion reads that temp file inside the fake runner, since the
 *    file is deleted as soon as the call returns.
 * 8. Deleting a repository requires an explicit `-X DELETE`, never an implied method.
 * 9. Searching text units POSTs to the search endpoint with the full pagination flag set
 *    (`--paginate --slurp --max-pages 0`), and passes repository, source, search type, and
 *    locale filters through as fields.
 * 10. Searching with an explicit `limit` drops pagination and sends the limit as a typed
 *     field, so a caller who asked for a handful of rows gets one cheap request instead of a
 *     full slurp of every page.
 * 11. Searching with no repository scope first lists every repository and then searches with
 *     all of their ids. Mojito's search API demands a scope, so "all repositories" has to be
 *     expanded here rather than pushed onto the agent.
 * 12. Inspecting one text unit reuses the search endpoint filtered by id, because Mojito has
 *     no get-by-id route for text units.
 * 13. Fetching history sends the required BCP-47 tag as a query field. History takes a tag
 *     while the write tools take a numeric locale id, and confusing the two is an easy
 *     mistake for both humans and agents.
 * 14. Adding a translation POSTs numeric ids as typed fields and the target text as a string,
 *     so a translation that happens to look like a number is not coerced.
 * 15. The review action `accept` maps to status APPROVED and included-in-localized-file true.
 * 16. The review action `reject` maps to status TRANSLATION_NEEDED and included false. Reject
 *     is the case where a wrong mapping would quietly drop a string from shipped files.
 * 17. Fetching a pollable task is a plain GET on the id; it never waits on the task.
 * 18. Any non-zero CLI exit becomes a MojitoCliError that carries the exit code and stderr,
 *     which is how a Mojito HTTP error reaches the agent as a readable message.
 * 19. A successful call parses the JSON on stdout and returns it as data.
 *
 * 20. Empty descriptions and a false SLA flag survive a simple repository create. Checks
 *     based on truthiness would otherwise silently discard both valid values.
 * 21. A nested repository create writes every optional field and removes its temporary JSON
 *     file after success; a separate failure path proves cleanup also happens when CLI exits
 *     non-zero, preventing abandoned request bodies in the system temp directory.
 * 22. One exhaustive search encodes every supported filter with the right raw (`-f`) or
 *     typed (`-F`) flag, including false booleans and offset zero.
 * 23. An all-repository search on a server with no repositories returns an empty array
 *     without issuing an invalid unscoped search request.
 * 24. A malformed repository row is rejected during all-repository expansion instead of
 *     being silently omitted and producing incomplete search results.
 * 25. Text-unit info forwards multiple locale tags as repeated raw array fields.
 * 26. Translation add preserves empty target/comment strings and false inclusion instead of
 *     dropping them as falsy values.
 * 27. The `review` and `translate` actions are pinned in addition to accept and reject, and
 *     an empty review comment is preserved.
 * 28. A CLI failure with blank stderr falls back to an exit-code message, so the agent still
 *     receives a useful explanation.
 * 29. Successful empty stdout (the normal DELETE response) maps to null rather than causing
 *     a JSON parse error.
 * 30. Successful but malformed JSON becomes a MojitoCliError that preserves stdout and the
 *     SyntaxError cause for diagnosis.
 */

import { describe, expect, test } from "@jest/globals";
import { existsSync, readFileSync } from "node:fs";
import type { CliRunResult, CliRunner } from "../src/cli-runner.js";
import { MojitoCliError } from "../src/errors.js";
import { MojitoCliClient, parseEncodedRepositoryLocale } from "../src/mojito-client.js";

function mockRunner(
    handler: (argv: string[]) => Promise<CliRunResult> | CliRunResult,
): CliRunner & { calls: string[][] } {
    const calls: string[][] = [];
    return {
        calls,
        async run(argv: string[]) {
            calls.push([...argv]);
            return handler(argv);
        },
    };
}

function okJson(body: unknown): CliRunResult {
    return { exitCode: 0, stdout: JSON.stringify(body), stderr: "" };
}

const config = { cliBinary: "mojito-prod", timeoutMs: 60_000 };

describe("parseEncodedRepositoryLocale", () => {
    test("fully translated locale with no parent", () => {
        expect(parseEncodedRepositoryLocale("fr-FR")).toEqual({
            locale: { bcp47Tag: "fr-FR" },
            toBeFullyTranslated: true,
        });
    });

    test("not fully translated locale with parentheses", () => {
        expect(parseEncodedRepositoryLocale("(en-GB)")).toEqual({
            locale: { bcp47Tag: "en-GB" },
            toBeFullyTranslated: false,
        });
    });

    test("inherits from parent and is not fully translated", () => {
        expect(parseEncodedRepositoryLocale("(fr-CA)->fr-FR")).toEqual({
            locale: { bcp47Tag: "fr-CA" },
            toBeFullyTranslated: false,
            parentLocale: {
                locale: { bcp47Tag: "fr-FR" },
            },
        });
    });

    test("inherits from parent while remaining fully translated", () => {
        expect(parseEncodedRepositoryLocale("fr-CA->fr-FR")).toEqual({
            locale: { bcp47Tag: "fr-CA" },
            toBeFullyTranslated: true,
            parentLocale: {
                locale: { bcp47Tag: "fr-FR" },
            },
        });
    });

    test("builds a multi-level parent chain", () => {
        expect(parseEncodedRepositoryLocale("fr-CA->fr-FR->en-US")).toEqual({
            locale: { bcp47Tag: "fr-CA" },
            toBeFullyTranslated: true,
            parentLocale: {
                locale: { bcp47Tag: "fr-FR" },
                parentLocale: {
                    locale: { bcp47Tag: "en-US" },
                },
            },
        });
    });

    test("parentheses on any segment mark the locale as not fully translated", () => {
        expect(parseEncodedRepositoryLocale("fr-CA->(fr-FR)")).toEqual({
            locale: { bcp47Tag: "fr-CA" },
            toBeFullyTranslated: false,
            parentLocale: {
                locale: { bcp47Tag: "fr-FR" },
            },
        });
    });

    test("trims whitespace around segments and the arrow", () => {
        expect(parseEncodedRepositoryLocale("  (fr-CA)  ->  fr-FR  ")).toEqual({
            locale: { bcp47Tag: "fr-CA" },
            toBeFullyTranslated: false,
            parentLocale: {
                locale: { bcp47Tag: "fr-FR" },
            },
        });
    });

    test("accepts simple language tags without region", () => {
        expect(parseEncodedRepositoryLocale("ja")).toEqual({
            locale: { bcp47Tag: "ja" },
            toBeFullyTranslated: true,
        });
    });

    test("throws on empty string", () => {
        expect(() => parseEncodedRepositoryLocale("")).toThrow(MojitoCliError);
        expect(() => parseEncodedRepositoryLocale("")).toThrow(/Invalid encoded repository locale/);
    });

    test("throws on whitespace-only string", () => {
        expect(() => parseEncodedRepositoryLocale("   ")).toThrow(MojitoCliError);
    });

    test("throws when the child segment before -> is empty", () => {
        expect(() => parseEncodedRepositoryLocale("->fr-FR")).toThrow(MojitoCliError);
    });

    test("unbalanced parentheses are treated as a literal bcp47 tag", () => {
        expect(parseEncodedRepositoryLocale("(fr-FR")).toEqual({
            locale: { bcp47Tag: "(fr-FR" },
            toBeFullyTranslated: true,
        });
        expect(parseEncodedRepositoryLocale("fr-FR)")).toEqual({
            locale: { bcp47Tag: "fr-FR)" },
            toBeFullyTranslated: true,
        });
    });

    test("nested parentheses strip only the outer pair", () => {
        expect(parseEncodedRepositoryLocale("((fr-FR))")).toEqual({
            locale: { bcp47Tag: "(fr-FR)" },
            toBeFullyTranslated: false,
        });
    });

    test("throws when the parent segment after -> is empty", () => {
        expect(() => parseEncodedRepositoryLocale("fr-FR->")).toThrow(
            /Invalid encoded repository locale/,
        );
    });

    test("throws when a middle segment is empty", () => {
        expect(() => parseEncodedRepositoryLocale("fr-CA-> ->fr-FR")).toThrow(
            /Invalid encoded repository locale/,
        );
    });

    test("throws when a parenthesized locale is empty", () => {
        expect(() => parseEncodedRepositoryLocale("()")).toThrow(
            /Invalid encoded repository locale/,
        );
        expect(() => parseEncodedRepositoryLocale("(   )")).toThrow(
            /Invalid encoded repository locale/,
        );
    });

    test("parentheses at more than one level of a derived chain", () => {
        expect(parseEncodedRepositoryLocale("(es-MX)->(es-ES)->en-US")).toEqual({
            locale: { bcp47Tag: "es-MX" },
            toBeFullyTranslated: false,
            parentLocale: {
                locale: { bcp47Tag: "es-ES" },
                parentLocale: {
                    locale: { bcp47Tag: "en-US" },
                },
            },
        });
    });

    test("comma is not a separator: a comma-separated list stays one literal tag", () => {
        expect(parseEncodedRepositoryLocale("de-DE,fr-FR")).toEqual({
            locale: { bcp47Tag: "de-DE,fr-FR" },
            toBeFullyTranslated: true,
        });
    });

    test("commas inside a derived chain stay part of each segment's tag", () => {
        expect(parseEncodedRepositoryLocale("(fr-CA,fr-BE)->fr-FR")).toEqual({
            locale: { bcp47Tag: "fr-CA,fr-BE" },
            toBeFullyTranslated: false,
            parentLocale: {
                locale: { bcp47Tag: "fr-FR" },
            },
        });
    });
});

describe("MojitoCliClient (CLI argv contracts)", () => {
    test("probeHelp runs --help and succeeds on exit 0", async () => {
        const runner = mockRunner(() => ({ exitCode: 0, stdout: "usage", stderr: "" }));
        const client = new MojitoCliClient(config, runner);

        await expect(client.probeHelp()).resolves.toBeUndefined();
        expect(runner.calls[0]).toEqual(["--help"]);
    });

    test("probeHelp throws MojitoCliError on non-zero exit", async () => {
        const runner = mockRunner(() => ({
            exitCode: 1,
            stdout: "",
            stderr: "command not found",
        }));
        const client = new MojitoCliClient(config, runner);

        await expect(client.probeHelp()).rejects.toBeInstanceOf(MojitoCliError);
    });

    test("repoList does not paginate: the endpoint returns every repository at once", async () => {
        const runner = mockRunner(() => okJson([]));
        const client = new MojitoCliClient(config, runner);

        await client.repoList();
        expect(runner.calls[0]).toEqual(["api", "/api/repositories"]);
    });

    test("repoList adds name filter as -f", async () => {
        const runner = mockRunner(() => okJson([]));
        const client = new MojitoCliClient(config, runner);

        await client.repoList({ name: "my-repo" });
        expect(runner.calls[0]).toEqual(expect.arrayContaining(["-f", "name=my-repo"]));
    });

    test("repoView GETs by id", async () => {
        const runner = mockRunner(() => okJson({ id: 42 }));
        const client = new MojitoCliClient(config, runner);

        await client.repoView(42);
        expect(runner.calls[0]).toEqual(["api", "/api/repositories/42"]);
    });

    test("repoCreate POSTs with -X POST", async () => {
        const runner = mockRunner(() => okJson({ id: 1, name: "r" }));
        const client = new MojitoCliClient(config, runner);

        await client.repoCreate({ name: "r", description: "d" });
        expect(runner.calls[0][0]).toBe("api");
        expect(runner.calls[0]).toEqual(
            expect.arrayContaining(["/api/repositories", "-X", "POST"]),
        );
        expect(runner.calls[0]).toEqual(expect.arrayContaining(["-f", "name=r"]));
    });

    test("repoCreate keeps false and empty optional fields in a flat request", async () => {
        const runner = mockRunner(() => okJson({ id: 1 }));
        const client = new MojitoCliClient(config, runner);

        await client.repoCreate({ name: "r", description: "", checkSLA: false });

        expect(runner.calls[0]).toEqual(expect.arrayContaining(["-f", "description="]));
        expect(runner.calls[0]).toEqual(expect.arrayContaining(["-F", "checkSLA=false"]));
    });

    test("repoCreate sends a list of locales with several derived locales as JSON body", async () => {
        let body: Record<string, unknown> | undefined;
        // The temp body file is deleted once the call returns, so read it inside the runner.
        const runner = mockRunner((argv) => {
            const file = argv[argv.indexOf("--input") + 1];
            body = JSON.parse(readFileSync(file, "utf8")) as Record<string, unknown>;
            return okJson({ id: 1, name: "multi" });
        });
        const client = new MojitoCliClient(config, runner);

        await client.repoCreate({
            name: "multi",
            sourceLocale: "en-US",
            repositoryLocales: [
                "de-DE",
                "(en-GB)",
                "(fr-CA)->fr-FR",
                "(es-MX)->es-ES",
                "(pt-BR)->pt-PT->en-US",
            ],
        });

        expect(runner.calls[0]).toEqual(expect.arrayContaining(["--input"]));
        expect(body?.sourceLocale).toEqual({ bcp47Tag: "en-US" });
        expect(body?.repositoryLocales).toEqual([
            { locale: { bcp47Tag: "de-DE" }, toBeFullyTranslated: true },
            { locale: { bcp47Tag: "en-GB" }, toBeFullyTranslated: false },
            {
                locale: { bcp47Tag: "fr-CA" },
                toBeFullyTranslated: false,
                parentLocale: { locale: { bcp47Tag: "fr-FR" } },
            },
            {
                locale: { bcp47Tag: "es-MX" },
                toBeFullyTranslated: false,
                parentLocale: { locale: { bcp47Tag: "es-ES" } },
            },
            {
                locale: { bcp47Tag: "pt-BR" },
                toBeFullyTranslated: false,
                parentLocale: {
                    locale: { bcp47Tag: "pt-PT" },
                    parentLocale: { locale: { bcp47Tag: "en-US" } },
                },
            },
        ]);
    });

    test("repoCreate JSON body includes every optional nested field and removes its temp file", async () => {
        let inputFile = "";
        let body: Record<string, unknown> | undefined;
        const runner = mockRunner((argv) => {
            inputFile = argv[argv.indexOf("--input") + 1];
            body = JSON.parse(readFileSync(inputFile, "utf8")) as Record<string, unknown>;
            return okJson({ id: 1 });
        });
        const client = new MojitoCliClient(config, runner);

        await client.repoCreate({
            name: "full",
            description: "",
            checkSLA: false,
            sourceLocale: "en-US",
            repositoryLocales: ["fr-FR"],
            assetIntegrityCheckers: [
                { assetExtension: "properties", integrityCheckerType: "PRINTF_LIKE" },
            ],
        });

        expect(body).toMatchObject({
            name: "full",
            description: "",
            checkSLA: false,
            sourceLocale: { bcp47Tag: "en-US" },
            assetIntegrityCheckers: [
                { assetExtension: "properties", integrityCheckerType: "PRINTF_LIKE" },
            ],
        });
        expect(inputFile).not.toBe("");
        expect(existsSync(inputFile)).toBe(false);
    });

    test("repoCreate removes its temp body when the CLI fails", async () => {
        let inputFile = "";
        const runner = mockRunner((argv) => {
            inputFile = argv[argv.indexOf("--input") + 1];
            return { exitCode: 1, stdout: "", stderr: "create failed" };
        });
        const client = new MojitoCliClient(config, runner);

        await expect(
            client.repoCreate({ name: "r", sourceLocale: "en-US" }),
        ).rejects.toBeInstanceOf(MojitoCliError);
        expect(inputFile).not.toBe("");
        expect(existsSync(inputFile)).toBe(false);
    });

    test("repoDelete uses -X DELETE", async () => {
        const runner = mockRunner(() => ({ exitCode: 0, stdout: "", stderr: "" }));
        const client = new MojitoCliClient(config, runner);

        await client.repoDelete(9);
        expect(runner.calls[0]).toEqual(["api", "/api/repositories/9", "-X", "DELETE"]);
    });

    test("textunitSearch POSTs search with pagination flags", async () => {
        const runner = mockRunner(() => okJson([]));
        const client = new MojitoCliClient(config, runner);

        await client.textunitSearch({
            repositoryNames: ["demo"],
            source: "Hello",
            searchType: "CONTAINS",
            localeTags: ["fr-FR"],
        });

        const argv = runner.calls[0];
        expect(argv).toEqual(
            expect.arrayContaining([
                "api",
                "/api/textunits/search",
                "-X",
                "POST",
                "--paginate",
                "--slurp",
                "--max-pages",
                "0",
            ]),
        );
        expect(argv).toEqual(expect.arrayContaining(["-f", "repositoryNames[]=demo"]));
        expect(argv).toEqual(expect.arrayContaining(["-f", "source=Hello"]));
        expect(argv).toEqual(expect.arrayContaining(["-f", "searchType=CONTAINS"]));
        expect(argv).toEqual(expect.arrayContaining(["-f", "localeTags[]=fr-FR"]));
    });

    test("textunitSearch with an explicit limit skips pagination", async () => {
        const runner = mockRunner(() => okJson([]));
        const client = new MojitoCliClient(config, runner);

        await client.textunitSearch({
            repositoryNames: ["demo"],
            searchType: "CONTAINS",
            limit: 5,
        });

        const argv = runner.calls[0];
        expect(argv).not.toContain("--paginate");
        expect(argv).toEqual(expect.arrayContaining(["-F", "limit=5"]));
    });

    test("textunitSearch with no repos lists all repos then searches with ids", async () => {
        const runner = mockRunner((argv) => {
            if (argv.includes("/api/repositories")) {
                return okJson([
                    { id: 1, name: "a" },
                    { id: 2, name: "b" },
                ]);
            }
            return okJson([]);
        });
        const client = new MojitoCliClient(config, runner);

        await client.textunitSearch({ source: "x", searchType: "CONTAINS" });

        expect(runner.calls.length).toBeGreaterThanOrEqual(2);
        expect(runner.calls[0]).toEqual(expect.arrayContaining(["api", "/api/repositories"]));
        const searchArgv = runner.calls.find((c) => c.includes("/api/textunits/search"));
        expect(searchArgv).toEqual(expect.arrayContaining(["-F", "repositoryIds[]=1"]));
        expect(searchArgv).toEqual(expect.arrayContaining(["-F", "repositoryIds[]=2"]));
    });

    test("textunitSearch encodes every optional search field with the correct type", async () => {
        const runner = mockRunner(() => okJson([]));
        const client = new MojitoCliClient(config, runner);

        await client.textunitSearch({
            repositoryIds: [1],
            repositoryNames: ["demo"],
            tmTextUnitIds: [2],
            localeTags: ["fr-FR"],
            name: "welcome",
            source: "Hello",
            target: "Bonjour",
            assetPath: "messages.properties",
            pluralFormOther: "items",
            searchType: "ILIKE",
            statusFilter: "REVIEW_NEEDED",
            usedFilter: "UNUSED",
            doNotTranslateFilter: false,
            tmTextUnitCreatedAfter: "2024-01-01T00:00:00Z",
            tmTextUnitCreatedBefore: "2024-02-01T00:00:00Z",
            branchId: 3,
            pluralFormFiltered: false,
            pluralFormExcluded: true,
            limit: 10,
            offset: 0,
        });

        expect(runner.calls[0]).toEqual([
            "api",
            "/api/textunits/search",
            "-X",
            "POST",
            "-F",
            "repositoryIds[]=1",
            "-f",
            "repositoryNames[]=demo",
            "-F",
            "tmTextUnitIds[]=2",
            "-f",
            "localeTags[]=fr-FR",
            "-f",
            "name=welcome",
            "-f",
            "source=Hello",
            "-f",
            "target=Bonjour",
            "-f",
            "assetPath=messages.properties",
            "-f",
            "pluralFormOther=items",
            "-f",
            "searchType=ILIKE",
            "-f",
            "statusFilter=REVIEW_NEEDED",
            "-f",
            "usedFilter=UNUSED",
            "-F",
            "doNotTranslateFilter=false",
            "-f",
            "tmTextUnitCreatedAfter=2024-01-01T00:00:00Z",
            "-f",
            "tmTextUnitCreatedBefore=2024-02-01T00:00:00Z",
            "-F",
            "branchId=3",
            "-F",
            "pluralFormFiltered=false",
            "-F",
            "pluralFormExcluded=true",
            "-F",
            "limit=10",
            "-F",
            "offset=0",
        ]);
    });

    test("textunitSearch returns an empty result without searching when there are no repositories", async () => {
        const runner = mockRunner(() => okJson([]));
        const client = new MojitoCliClient(config, runner);

        await expect(client.textunitSearch({ source: "x" })).resolves.toEqual([]);
        expect(runner.calls).toEqual([["api", "/api/repositories"]]);
    });

    test("textunitSearch rejects malformed repository rows during all-repository expansion", async () => {
        const runner = mockRunner(() => okJson([{ id: 1 }, { name: "missing-id" }]));
        const client = new MojitoCliClient(config, runner);

        await expect(client.textunitSearch({ source: "x" })).rejects.toThrow(/positive integer id/);
        expect(runner.calls).toEqual([["api", "/api/repositories"]]);
    });

    test("textunitInfo searches by tmTextUnitIds", async () => {
        const runner = mockRunner(() => okJson([]));
        const client = new MojitoCliClient(config, runner);

        await client.textunitInfo({ tmTextUnitId: 100 });
        expect(runner.calls[0]).toEqual(
            expect.arrayContaining([
                "api",
                "/api/textunits/search",
                "-X",
                "POST",
                "-F",
                "tmTextUnitIds[]=100",
            ]),
        );
    });

    test("textunitInfo passes every locale tag as a raw array field", async () => {
        const runner = mockRunner(() => okJson([]));
        const client = new MojitoCliClient(config, runner);

        await client.textunitInfo({ tmTextUnitId: 100, localeTags: ["fr-FR", "ja-JP"] });

        expect(runner.calls[0]).toEqual(
            expect.arrayContaining(["-f", "localeTags[]=fr-FR", "-f", "localeTags[]=ja-JP"]),
        );
    });

    test("textunitHistory requires bcp47Tag query field", async () => {
        const runner = mockRunner(() => okJson([]));
        const client = new MojitoCliClient(config, runner);

        await client.textunitHistory({ tmTextUnitId: 5, bcp47Tag: "ja-JP" });
        expect(runner.calls[0]).toEqual([
            "api",
            "/api/textunits/5/history",
            "-f",
            "bcp47Tag=ja-JP",
        ]);
    });

    test("textunitTranslationAdd POSTs typed fields", async () => {
        const runner = mockRunner(() => okJson({}));
        const client = new MojitoCliClient(config, runner);

        await client.textunitTranslationAdd({
            tmTextUnitId: 1,
            localeId: 2,
            target: "Bonjour",
            status: "APPROVED",
        });

        expect(runner.calls[0]).toEqual(
            expect.arrayContaining(["api", "/api/textunits", "-X", "POST"]),
        );
        expect(runner.calls[0]).toEqual(expect.arrayContaining(["-F", "tmTextUnitId=1"]));
        expect(runner.calls[0]).toEqual(expect.arrayContaining(["-F", "localeId=2"]));
        expect(runner.calls[0]).toEqual(expect.arrayContaining(["-f", "target=Bonjour"]));
    });

    test("textunitTranslationAdd preserves empty comments and false inclusion", async () => {
        const runner = mockRunner(() => okJson({}));
        const client = new MojitoCliClient(config, runner);

        await client.textunitTranslationAdd({
            tmTextUnitId: 1,
            localeId: 2,
            target: "",
            targetComment: "",
            includedInLocalizedFile: false,
        });

        expect(runner.calls[0]).toEqual(expect.arrayContaining(["-f", "target="]));
        expect(runner.calls[0]).toEqual(expect.arrayContaining(["-f", "targetComment="]));
        expect(runner.calls[0]).toEqual(
            expect.arrayContaining(["-F", "includedInLocalizedFile=false"]),
        );
    });

    test("reviewUpdate maps accept to APPROVED and includedInLocalizedFile true", async () => {
        const runner = mockRunner(() => okJson({}));
        const client = new MojitoCliClient(config, runner);

        await client.reviewUpdate({
            tmTextUnitId: 1,
            localeId: 2,
            target: "Hi",
            action: "accept",
        });

        expect(runner.calls[0]).toEqual(expect.arrayContaining(["-f", "status=APPROVED"]));
        expect(runner.calls[0]).toEqual(
            expect.arrayContaining(["-F", "includedInLocalizedFile=true"]),
        );
    });

    test("reviewUpdate maps reject to TRANSLATION_NEEDED and includedInLocalizedFile false", async () => {
        const runner = mockRunner(() => okJson({}));
        const client = new MojitoCliClient(config, runner);

        await client.reviewUpdate({
            tmTextUnitId: 1,
            localeId: 2,
            target: "Hi",
            action: "reject",
        });

        expect(runner.calls[0]).toEqual(
            expect.arrayContaining(["-f", "status=TRANSLATION_NEEDED"]),
        );
        expect(runner.calls[0]).toEqual(
            expect.arrayContaining(["-F", "includedInLocalizedFile=false"]),
        );
    });

    test.each([
        ["review", "REVIEW_NEEDED", true],
        ["translate", "TRANSLATION_NEEDED", true],
    ] as const)("reviewUpdate maps %s to %s and inclusion %s", async (action, status, included) => {
        const runner = mockRunner(() => okJson({}));
        const client = new MojitoCliClient(config, runner);

        await client.reviewUpdate({
            tmTextUnitId: 1,
            localeId: 2,
            target: "Hi",
            targetComment: "",
            action,
        });

        expect(runner.calls[0]).toEqual(expect.arrayContaining(["-f", `status=${status}`]));
        expect(runner.calls[0]).toEqual(
            expect.arrayContaining(["-F", `includedInLocalizedFile=${included}`]),
        );
        expect(runner.calls[0]).toEqual(expect.arrayContaining(["-f", "targetComment="]));
    });

    test("pollabletaskGet GETs by id", async () => {
        const runner = mockRunner(() => okJson({ id: 3 }));
        const client = new MojitoCliClient(config, runner);

        await client.pollabletaskGet(3);
        expect(runner.calls[0]).toEqual(["api", "/api/pollableTasks/3"]);
    });

    test("non-zero exit throws MojitoCliError including stderr", async () => {
        const runner = mockRunner(() => ({
            exitCode: 1,
            stdout: '{"message":"nope"}',
            stderr: "mojito: nope (HTTP 404)",
        }));
        const client = new MojitoCliClient(config, runner);

        await expect(client.repoView(999)).rejects.toMatchObject({
            name: "MojitoCliError",
            exitCode: 1,
            stderr: expect.stringContaining("mojito:"),
        });
    });

    test("non-zero exit with blank stderr uses an exit-code summary", async () => {
        const runner = mockRunner(() => ({ exitCode: 17, stdout: "", stderr: "   " }));
        const client = new MojitoCliClient(config, runner);

        await expect(client.repoView(999)).rejects.toMatchObject({
            message: "mojito CLI exited with code 17",
            exitCode: 17,
            stderr: "   ",
        });
    });

    test("successful JSON stdout is parsed and returned", async () => {
        const runner = mockRunner(() => okJson([{ id: 1 }]));
        const client = new MojitoCliClient(config, runner);

        await expect(client.repoList()).resolves.toEqual([{ id: 1 }]);
    });

    test("successful empty stdout returns null", async () => {
        const runner = mockRunner(() => ({ exitCode: 0, stdout: " \n ", stderr: "" }));
        const client = new MojitoCliClient(config, runner);

        await expect(client.repoDelete(1)).resolves.toBeNull();
    });

    test("successful non-JSON stdout throws MojitoCliError and preserves stdout", async () => {
        const runner = mockRunner(() => ({ exitCode: 0, stdout: "not json", stderr: "" }));
        const client = new MojitoCliClient(config, runner);

        await expect(client.repoList()).rejects.toMatchObject({
            name: "MojitoCliError",
            message: "Failed to parse mojito CLI JSON stdout",
            stdout: "not json",
            cause: expect.any(SyntaxError),
        });
    });
});
