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

import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

export type IntegrationConfig = {
    /** Prod Mojito CLI script name/path (not used by integration tests). */
    prodCli: string;
    /** Dev Mojito CLI script name/path — integration tests use this. */
    devCli: string;
    /**
     * Repository name created/viewed/deleted on the **dev** server during integration tests.
     * Use a dedicated disposable name (not a real project repo).
     */
    testRepositoryName: string;
    /** Optional spawn timeout in ms (default 600000). */
    timeoutMs?: number;
};

const CONFIG_FILE = "integration-config.json";
const TEMPLATE_FILE = "integration-config.template.json";

export function integrationConfigPath(): string {
    const dir = dirname(fileURLToPath(import.meta.url));
    return join(dir, CONFIG_FILE);
}

export function integrationConfigTemplatePath(): string {
    const dir = dirname(fileURLToPath(import.meta.url));
    return join(dir, TEMPLATE_FILE);
}

/**
 * Loads `tests-integration/integration-config.json`.
 * @throws Error with setup instructions if the file is missing or invalid.
 */
export function loadIntegrationConfig(): IntegrationConfig {
    const path = integrationConfigPath();
    if (!existsSync(path)) {
        throw new Error(
            [
                `Missing ${CONFIG_FILE}.`,
                `Copy the template and fill in your CLI script names:`,
                `  cp tests-integration/${TEMPLATE_FILE} tests-integration/${CONFIG_FILE}`,
                `See tests-integration/README.md for details.`,
            ].join("\n"),
        );
    }

    let parsed: unknown;
    try {
        parsed = JSON.parse(readFileSync(path, "utf8")) as unknown;
    } catch (cause) {
        throw new Error(`Invalid JSON in ${path}`, { cause });
    }

    return parseIntegrationConfig(parsed);
}

/** Parses and validates integration config independently of filesystem access. */
export function parseIntegrationConfig(parsed: unknown): IntegrationConfig {
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
        throw new Error(`${CONFIG_FILE} must be a JSON object`);
    }

    const { prodCli, devCli, testRepositoryName, timeoutMs } = parsed as Record<string, unknown>;

    if (typeof prodCli !== "string" || !prodCli.trim()) {
        throw new Error(`${CONFIG_FILE}: "prodCli" must be a non-empty string`);
    }
    if (typeof devCli !== "string" || !devCli.trim()) {
        throw new Error(`${CONFIG_FILE}: "devCli" must be a non-empty string`);
    }
    if (typeof testRepositoryName !== "string" || !testRepositoryName.trim()) {
        throw new Error(
            `${CONFIG_FILE}: "testRepositoryName" must be a non-empty string (disposable repo used for create/view/delete)`,
        );
    }
    if (
        timeoutMs !== undefined &&
        (typeof timeoutMs !== "number" || !Number.isSafeInteger(timeoutMs) || timeoutMs <= 0)
    ) {
        throw new Error(`${CONFIG_FILE}: "timeoutMs" must be a positive whole number when set`);
    }

    return {
        prodCli: prodCli.trim(),
        devCli: devCli.trim(),
        testRepositoryName: testRepositoryName.trim(),
        timeoutMs,
    };
}
