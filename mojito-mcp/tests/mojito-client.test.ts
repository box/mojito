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
