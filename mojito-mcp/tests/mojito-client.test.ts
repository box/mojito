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
 * Test scenarios: translating tool calls into Mojito CLI invocations for repository
 * reads, pollable tasks, and text-unit search/info/history.
 *
 * Write tools (create/delete/translation/review) and locale encoding land in the next PR.
 */

import { describe, expect, test } from "@jest/globals";
import type { CliRunResult, CliRunner } from "../src/cli-runner.js";
import { MojitoCliError } from "../src/errors.js";
import { MojitoCliClient } from "../src/mojito-client.js";

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
