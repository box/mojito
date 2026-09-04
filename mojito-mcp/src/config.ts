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
 * Runtime configuration for mojito-mcp. Auth and host live in the Mojito CLI, not here.
 */
export type MojitoMcpConfig = {
    /** Executable name or path (default `mojito-prod`) */
    cliBinary: string;
    /** Hard kill timeout for each CLI spawn, in milliseconds */
    timeoutMs: number;
};

export const DEFAULT_CLI_BINARY = "mojito-prod";
export const DEFAULT_TIMEOUT_MS = 600_000;

/**
 * Reads {@link MojitoMcpConfig} from `process.env`.
 *
 * - `MOJITO_CLI` → `cliBinary` (default {@link DEFAULT_CLI_BINARY}). Trimmed; blank values
 *   fall back to the default. Names on `PATH` and filesystem paths are both accepted.
 * - `MOJITO_CLI_TIMEOUT_MS` → `timeoutMs` (default {@link DEFAULT_TIMEOUT_MS}). Must be a
 *   finite positive whole number of milliseconds; anything else falls back to the default.
 */
export function loadConfigFromEnv(): MojitoMcpConfig {
    const cliBinary = process.env.MOJITO_CLI?.trim();

    return {
        cliBinary: cliBinary ? cliBinary : DEFAULT_CLI_BINARY,
        timeoutMs: parseTimeoutMs(process.env.MOJITO_CLI_TIMEOUT_MS),
    };
}

function parseTimeoutMs(raw: string | undefined): number {
    const parsed = Number(raw);
    if (Number.isFinite(parsed) && Number.isInteger(parsed) && parsed > 0) {
        return parsed;
    }
    return DEFAULT_TIMEOUT_MS;
}
