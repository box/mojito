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

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { MojitoCliClient } from "./mojito-client.js";
import { registerMojitoTools } from "./register-tools.js";
import { MOJITO_MCP_TOOL_IDS } from "./tool-metadata.js";

export { MOJITO_MCP_TOOL_IDS } from "./tool-metadata.js";

const SERVER_NAME = "mojito-mcp";
const SERVER_VERSION = "0.1.0-SNAPSHOT";

/**
 * Builds the stdio MCP server wired to the given Mojito CLI client.
 */
export function createMojitoMcpServer(client: MojitoCliClient): McpServer {
    const server = new McpServer({
        name: SERVER_NAME,
        version: SERVER_VERSION,
    });

    registerMojitoTools(server, client);

    return server;
}

/** @internal Exported for tests that assert documentation parity with registration. */
export function expectedToolCount(): number {
    return MOJITO_MCP_TOOL_IDS.length;
}
