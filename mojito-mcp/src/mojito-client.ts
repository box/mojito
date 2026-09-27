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

import type { CliRunner } from "./cli-runner.js";
import type { MojitoMcpConfig } from "./config.js";
import { MojitoCliError } from "./errors.js";

/**
 * Mojito API access via the CLI `api` command.
 */
export class MojitoCliClient {
    constructor(
        private readonly _config: MojitoMcpConfig,
        private readonly _runner: CliRunner,
    ) {}

    get config(): MojitoMcpConfig {
        return this._config;
    }

    get runner(): CliRunner {
        return this._runner;
    }

    /** Startup probe: `{cli} --help`. Throws {@link MojitoCliError} if the CLI is missing or fails. */
    async probeHelp(): Promise<void> {
        await this.runChecked(["--help"]);
    }

    /**
     * GET /api/repositories — returns every repository in one response.
     *
     * Not paginated: the endpoint ignores offset/limit, so `--paginate` would
     * loop forever because each "page" comes back full.
     */
    async repoList(params: { name?: string } = {}): Promise<unknown> {
        const argv = ["api", "/api/repositories"];
        if (params.name !== undefined) {
            pushRaw(argv, "name", params.name);
        }
        return this.apiJson(argv);
    }

    /** GET /api/repositories/{repositoryId} */
    async repoView(repositoryId: number): Promise<unknown> {
        return this.apiJson(["api", `/api/repositories/${repositoryId}`]);
    }

    /** GET /api/pollableTasks/{pollableTaskId} */
    async pollabletaskGet(pollableTaskId: number): Promise<unknown> {
        return this.apiJson(["api", `/api/pollableTasks/${pollableTaskId}`]);
    }

    private async runChecked(argv: string[]): Promise<{ stdout: string; stderr: string }> {
        const result = await this._runner.run(argv);
        if (result.exitCode !== 0) {
            const summary =
                result.stderr.trim() || `mojito CLI exited with code ${result.exitCode}`;
            throw new MojitoCliError(summary, {
                exitCode: result.exitCode,
                stdout: result.stdout,
                stderr: result.stderr,
            });
        }
        return { stdout: result.stdout, stderr: result.stderr };
    }

    private async apiJson(argv: string[]): Promise<unknown> {
        const { stdout } = await this.runChecked(argv);
        return parseStdoutJson(stdout);
    }
}

function parseStdoutJson(stdout: string): unknown {
    const trimmed = stdout.trim();
    if (trimmed === "") {
        return null;
    }
    try {
        return JSON.parse(trimmed) as unknown;
    } catch (cause) {
        throw new MojitoCliError("Failed to parse mojito CLI JSON stdout", {
            stdout,
            cause,
        });
    }
}

function pushRaw(argv: string[], key: string, value: string): void {
    argv.push("-f", `${key}=${value}`);
}
