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
 * A fake CliRunner records the argv it is handed and returns canned output, so every
 * case below is an assertion about the command we would have run, with no Mojito
 * install, no authentication, and no network involved.
 *
 * 1. The startup probe runs `--help` and succeeds on exit 0. This is what the server does
 *    before serving traffic, and it must not touch the network.
 * 2. The startup probe fails on a non-zero exit, which is how a missing or broken CLI
 *    wrapper stops the server at boot instead of failing later on every tool call.
 * 3. Listing repositories deliberately does *not* paginate. The endpoint ignores offset and
 *    limit and returns everything at once, so `--paginate` would loop forever on pages that
 *    always look full.
 * 4. Listing with a name filter passes it as a string field.
 * 5. Viewing a repository is a plain GET on the id.
 * 6. Fetching a pollable task is a plain GET on the id; it never waits on the task.
 * 7. Any non-zero CLI exit becomes a MojitoCliError that carries the exit code and stderr.
 * 8. A CLI failure with blank stderr falls back to an exit-code message.
 * 9. A successful call parses the JSON on stdout and returns it as data.
 * 10. Successful empty stdout maps to null rather than causing a JSON parse error.
 * 11. Successful but malformed JSON becomes a MojitoCliError that preserves stdout and the
 *     SyntaxError cause for diagnosis.
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

        await expect(client.repoView(1)).resolves.toBeNull();
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
