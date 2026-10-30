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
 * Test scenarios: the failure payload that reaches the AI agent.
 *
 * Every error an agent sees from this server is a MojitoCliError, so the fields it
 * carries decide whether the agent can tell "your CLI is not installed" apart from
 * "Mojito rejected the request with HTTP 400". These tests pin that payload's shape.
 *
 * 1. A failure where the CLI actually ran. The error retains the exit code, both output
 *    streams, and the timeout flag, and reports a stable `name` that callers and tests
 *    can match on rather than parsing the message text.
 * 2. A failure with almost nothing known, such as a timeout. The optional fields default
 *    to empty string and null instead of `undefined`, so code that formats an MCP error
 *    message can never end up showing the agent the word "undefined".
 * 3. A lower-level exception is retained as `cause`. Spawn and JSON parse failures need
 *    their original exception for diagnostics without replacing the stable public error.
 */

import { describe, expect, test } from "@jest/globals";
import { MojitoCliError } from "../src/errors.js";

describe("MojitoCliError", () => {
    test("stores exit metadata for MCP error surfacing", () => {
        const err = new MojitoCliError("failed", {
            exitCode: 1,
            stdout: '{"message":"x"}',
            stderr: "mojito: x (HTTP 400)",
            timedOut: false,
        });

        expect(err.name).toBe("MojitoCliError");
        expect(err.message).toBe("failed");
        expect(err.exitCode).toBe(1);
        expect(err.stdout).toContain("message");
        expect(err.stderr).toContain("mojito:");
        expect(err.timedOut).toBe(false);
    });

    test("defaults optional fields", () => {
        const err = new MojitoCliError("timeout", { timedOut: true });
        expect(err.exitCode).toBeNull();
        expect(err.stdout).toBe("");
        expect(err.stderr).toBe("");
        expect(err.timedOut).toBe(true);
    });

    test("retains an underlying cause", () => {
        const cause = new Error("spawn failed");
        const err = new MojitoCliError("could not run CLI", { cause });

        expect(err.cause).toBe(cause);
    });
});
