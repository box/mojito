#!/usr/bin/env node
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

import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { DefaultCliRunner } from "./cli-runner.js";
import { loadConfigFromEnv } from "./config.js";
import { MojitoCliClient } from "./mojito-client.js";
import { createMojitoMcpServer } from "./server.js";

async function main(): Promise<void> {
    const config = loadConfigFromEnv();
    const runner = new DefaultCliRunner(config);
    const client = new MojitoCliClient(config, runner);

    // Skeleton: will call client.probeHelp() once implemented
    await client.probeHelp();

    const mcpServer = createMojitoMcpServer(client);
    const transport = new StdioServerTransport();
    await mcpServer.connect(transport);
}

main().catch((err: unknown) => {
    console.error(err);
    process.exit(1);
});
