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
 * - `MOJITO_CLI` → `cliBinary` (default {@link DEFAULT_CLI_BINARY})
 * - `MOJITO_CLI_TIMEOUT_MS` → `timeoutMs` (default {@link DEFAULT_TIMEOUT_MS})
 */
export function loadConfigFromEnv(): MojitoMcpConfig {
    const cliBinary = process.env.MOJITO_CLI?.trim();
    const timeoutMs = Number(process.env.MOJITO_CLI_TIMEOUT_MS);

    return {
        cliBinary: cliBinary ? cliBinary : DEFAULT_CLI_BINARY,
        timeoutMs: Number.isFinite(timeoutMs) && timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS,
    };
}
