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
 * Test scenarios: spawning the Mojito CLI as a child process.
 *
 * This is the only place mojito-mcp touches the operating system, and it runs underneath
 * an MCP stdio transport where the server's own stdout *is* the protocol channel. The
 * tests use `process.execPath` (the Node binary already running Jest) as a stand-in CLI,
 * so they need neither a Mojito install nor network access.
 *
 * 1. A command that succeeds. The two output streams are captured separately and handed
 *    back alongside exit code 0. Capturing rather than inheriting is the point of the
 *    case: child output written onto the parent's stdout would corrupt the MCP stream.
 * 2. A command that exits non-zero. The runner reports the real exit code and does not
 *    throw, because deciding what a failure means belongs to the client layer, which
 *    needs the captured stderr to build a message worth showing the agent.
 * 3. A command that never exits. Given a short timeout, the runner kills the child and
 *    rejects with a MojitoCliError flagged `timedOut`, so a hung or unreachable Mojito
 *    instance cannot wedge the MCP server indefinitely.
 * 4. A program that does not exist. A missing or misnamed CLI wrapper is the most common
 *    setup mistake, and it has to arrive as a MojitoCliError like every other failure
 *    rather than as an unhandled spawn error that takes the process down.
 * 5. The child receives environment variables from the MCP process. Mojito authentication
 *    wrappers often rely on HOME, PATH, profiles, or access-token helper variables.
 * 6. A child that ignores SIGTERM is escalated to SIGKILL. The timeout must remain a hard
 *    bound even when a broken wrapper installs its own signal handler.
 */

import { describe, expect, test } from "@jest/globals";
import { DefaultCliRunner } from "../src/cli-runner.js";
import { MojitoCliError } from "../src/errors.js";

/**
 * Contract tests for DefaultCliRunner.
 * These will fail until the runner is implemented (TDD red phase).
 */
describe("DefaultCliRunner", () => {
    test("run executes the configured binary with argv and returns exitCode/stdout/stderr", async () => {
        // Use a portable command: node -e prints and exits 0
        const runner = new DefaultCliRunner({
            cliBinary: process.execPath,
            timeoutMs: 10_000,
        });

        const result = await runner.run([
            "-e",
            "process.stdout.write('out'); process.stderr.write('err');",
        ]);

        expect(result.exitCode).toBe(0);
        expect(result.stdout).toBe("out");
        expect(result.stderr).toBe("err");
    });

    test("run returns non-zero exitCode without throwing when the child exits failed", async () => {
        const runner = new DefaultCliRunner({
            cliBinary: process.execPath,
            timeoutMs: 10_000,
        });

        const result = await runner.run(["-e", "process.exit(7)"]);

        expect(result.exitCode).toBe(7);
    });

    test("run times out and throws MojitoCliError with timedOut=true", async () => {
        const runner = new DefaultCliRunner({
            cliBinary: process.execPath,
            timeoutMs: 200,
        });

        await expect(runner.run(["-e", "setTimeout(() => {}, 10_000)"])).rejects.toMatchObject({
            name: "MojitoCliError",
            timedOut: true,
        } satisfies Partial<MojitoCliError>);
    });

    test("run throws MojitoCliError when the binary cannot be spawned", async () => {
        const runner = new DefaultCliRunner({
            cliBinary: "/nonexistent/mojito-binary-for-tests",
            timeoutMs: 5_000,
        });

        await expect(runner.run(["--help"])).rejects.toBeInstanceOf(MojitoCliError);
    });

    test("run passes the parent environment to the child", async () => {
        const runner = new DefaultCliRunner({
            cliBinary: process.execPath,
            timeoutMs: 10_000,
        });
        const original = process.env.MOJITO_MCP_RUNNER_TEST;
        process.env.MOJITO_MCP_RUNNER_TEST = "available";
        try {
            const result = await runner.run([
                "-e",
                "process.stdout.write(process.env.MOJITO_MCP_RUNNER_TEST ?? 'missing')",
            ]);
            expect(result.stdout).toBe("available");
        } finally {
            if (original === undefined) {
                delete process.env.MOJITO_MCP_RUNNER_TEST;
            } else {
                process.env.MOJITO_MCP_RUNNER_TEST = original;
            }
        }
    });

    test("run escalates to SIGKILL when a timed-out child ignores SIGTERM", async () => {
        const runner = new DefaultCliRunner({
            cliBinary: process.execPath,
            timeoutMs: 100,
        });
        const startedAt = Date.now();

        await expect(
            runner.run(["-e", "process.on('SIGTERM', () => {}); setInterval(() => {}, 1000)"]),
        ).rejects.toMatchObject({
            name: "MojitoCliError",
            timedOut: true,
        });
        expect(Date.now() - startedAt).toBeLessThan(5_000);
    }, 10_000);
});
