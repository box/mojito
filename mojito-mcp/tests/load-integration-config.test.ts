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
 * Test scenarios: validating the opt-in live-test configuration.
 *
 * These tests exercise the data parser without reading a developer's machine-specific
 * `integration-config.json`. That keeps the default unit suite hermetic while pinning the
 * safety checks applied before live tests can create and delete a repository.
 *
 * 1. A complete valid object is accepted, surrounding whitespace is removed from every
 *    string, and a positive whole-number timeout is retained.
 * 2. The timeout is optional because the integration runner has a ten-minute default.
 * 3. Null, primitive values, and arrays are rejected; JSON config must be an object.
 * 4. Each required string rejects a missing, non-string, empty, or whitespace-only value.
 *    In particular, a blank test repository name must never reach a destructive live test.
 * 5. Timeout rejects strings, NaN, Infinity, zero, negatives, fractions, and unsafe integers.
 *    It must represent a finite positive whole number of milliseconds, matching runtime
 *    `MOJITO_CLI_TIMEOUT_MS` semantics.
 */

import { describe, expect, test } from "@jest/globals";
import { parseIntegrationConfig } from "../tests-integration/load-integration-config.js";

const valid = {
    prodCli: "mojito-prod",
    devCli: "mojito-dev",
    testRepositoryName: "mojito-mcp-integration-test",
};

describe("parseIntegrationConfig", () => {
    test("accepts and trims a complete valid config", () => {
        expect(
            parseIntegrationConfig({
                prodCli: "  mojito-prod  ",
                devCli: "  ./bin/mojito-dev  ",
                testRepositoryName: "  disposable-repo  ",
                timeoutMs: 120_000,
            }),
        ).toEqual({
            prodCli: "mojito-prod",
            devCli: "./bin/mojito-dev",
            testRepositoryName: "disposable-repo",
            timeoutMs: 120_000,
        });
    });

    test("accepts an omitted timeout", () => {
        expect(parseIntegrationConfig(valid)).toEqual({ ...valid, timeoutMs: undefined });
    });

    test.each([null, true, "config", 3, []])("rejects non-object config: %p", (value) => {
        expect(() => parseIntegrationConfig(value)).toThrow(/must be a JSON object/);
    });

    test.each(["prodCli", "devCli", "testRepositoryName"] as const)(
        "rejects an invalid %s",
        (field) => {
            for (const value of [undefined, null, 1, "", "   "]) {
                expect(() => parseIntegrationConfig({ ...valid, [field]: value })).toThrow();
            }
        },
    );

    test.each([
        "120000",
        Number.NaN,
        Number.POSITIVE_INFINITY,
        0,
        -1,
        1500.9,
        Number.MAX_SAFE_INTEGER + 1,
    ])("rejects invalid timeout %p", (timeoutMs) => {
        expect(() => parseIntegrationConfig({ ...valid, timeoutMs })).toThrow(
            /positive whole number/,
        );
    });
});
