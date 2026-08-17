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

import { describe, expect, test } from "@jest/globals";
import { readFileSync } from "node:fs";
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

    test("empty parent segment after -> becomes an empty-tag parent node", () => {
        expect(parseEncodedRepositoryLocale("fr-FR->")).toEqual({
            locale: { bcp47Tag: "fr-FR" },
            toBeFullyTranslated: true,
            parentLocale: {
                locale: { bcp47Tag: "" },
            },
        });
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

    test("successful JSON stdout is parsed and returned", async () => {
        const runner = mockRunner(() => okJson([{ id: 1 }]));
        const client = new MojitoCliClient(config, runner);

        await expect(client.repoList()).resolves.toEqual([{ id: 1 }]);
    });
});
