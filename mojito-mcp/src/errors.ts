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
 * Error raised when a Mojito CLI invocation fails (non-zero exit, timeout, or spawn failure).
 * Intended to be surfaced to the AI as a normal MCP tool error.
 */
export class MojitoCliError extends Error {
    readonly exitCode: number | null;
    readonly stdout: string;
    readonly stderr: string;
    readonly timedOut: boolean;

    constructor(
        message: string,
        options: {
            exitCode?: number | null;
            stdout?: string;
            stderr?: string;
            timedOut?: boolean;
            cause?: unknown;
        } = {},
    ) {
        super(message, options.cause !== undefined ? { cause: options.cause } : undefined);
        this.name = "MojitoCliError";
        this.exitCode = options.exitCode ?? null;
        this.stdout = options.stdout ?? "";
        this.stderr = options.stderr ?? "";
        this.timedOut = options.timedOut ?? false;
    }
}

export function notImplemented(what: string): never {
    throw new Error(`${what}: not implemented`);
}
