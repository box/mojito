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
import { z } from "zod";
import type { MojitoCliClient } from "./mojito-client.js";

function jsonResult(data: unknown) {
    return {
        content: [{ type: "text" as const, text: JSON.stringify(data, null, 2) }],
    };
}

/**
 * Registers all Mojito MCP tools on the given server.
 */
export function registerMojitoTools(server: McpServer, client: MojitoCliClient): void {
    server.registerTool(
        "mojito_repo_list",
        {
            description: [
                "List Mojito repositories (undeleted), optionally filtered by exact repository name.",
                "Use this to discover repository ids/names before search, or to resolve a product git repo name to a Mojito repository.",
                "Returns JSON array of repository summaries (id, name, description, locales summary depending on API view).",
                "Talks to whichever Mojito instance this MCP server was configured for (mojito-prod vs mojito-dev via MOJITO_CLI).",
            ].join(" "),
            inputSchema: {
                name: z
                    .string()
                    .optional()
                    .describe(
                        "Exact Mojito repository name filter. Omit to list all undeleted repositories. Matching is server-side exact name, not fuzzy.",
                    ),
            },
        },
        async ({ name }) => jsonResult(await client.repoList({ name })),
    );

    server.registerTool(
        "mojito_repo_view",
        {
            description: [
                "Get full details for one Mojito repository by numeric id,",
                "including description, source locale, repository locales, and integrity checkers when present.",
                "Prefer mojito_repo_list first if you only know the name.",
            ].join(" "),
            inputSchema: {
                repositoryId: z
                    .number()
                    .int()
                    .positive()
                    .describe(
                        "Numeric Mojito repository id (from mojito_repo_list or prior search results).",
                    ),
            },
        },
        async ({ repositoryId }) => jsonResult(await client.repoView(repositoryId)),
    );

    server.registerTool(
        "mojito_pollabletask_get",
        {
            description: [
                "Fetch status of an asynchronous Mojito pollable task by id",
                "(imports, batch jobs, and other long-running operations that return a PollableTask).",
                "Use when a previous operation returned a pollableTask id; poll until allFinished is true or an error appears.",
                "This tool does not wait/block; call again as needed.",
            ].join(" "),
            inputSchema: {
                pollableTaskId: z
                    .number()
                    .int()
                    .positive()
                    .describe("Pollable task id from a previous Mojito async response."),
            },
        },
        async ({ pollableTaskId }) => jsonResult(await client.pollabletaskGet(pollableTaskId)),
    );
}
