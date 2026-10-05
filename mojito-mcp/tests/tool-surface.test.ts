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
 * Test scenarios: the published tool surface.
 *
 * 1. The exact list, in order, for the read tools in this slice.
 * 2. No duplicate ids.
 * 3. The count the server reports matches the list.
 * 4. Every id follows the `mojito_<object>_<action>` convention.
 * 5. The registrations made against McpServer exactly match the canonical list.
 * 6. Every registered tool has a non-empty description.
 * 7. Numeric identifiers reject zero, negative, and fractional values.
 * 8. README.md and SKILL.md mention every published id.
 */

import { describe, expect, test } from "@jest/globals";
import { readFileSync } from "node:fs";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { MojitoCliClient } from "../src/mojito-client.js";
import { registerMojitoTools } from "../src/register-tools.js";
import { MOJITO_MCP_TOOL_IDS } from "../src/tool-metadata.js";
import { expectedToolCount } from "../src/server.js";

type Schema = { safeParse(value: unknown): { success: boolean } };
type Registration = {
    name: string;
    options: {
        description?: string;
        inputSchema: Record<string, Schema>;
    };
};

function captureRegistrations(): Registration[] {
    const registrations: Registration[] = [];
    const server = {
        registerTool(name: string, options: Registration["options"]) {
            registrations.push({ name, options });
        },
    } as unknown as McpServer;

    registerMojitoTools(server, {} as MojitoCliClient);
    return registrations;
}

describe("MCP tool surface", () => {
    test("exports a stable ordered list of tool ids", () => {
        expect(MOJITO_MCP_TOOL_IDS).toEqual([
            "mojito_repo_list",
            "mojito_repo_view",
            "mojito_pollabletask_get",
        ]);
    });

    test("tool ids are unique", () => {
        expect(new Set(MOJITO_MCP_TOOL_IDS).size).toBe(MOJITO_MCP_TOOL_IDS.length);
    });

    test("expectedToolCount matches MOJITO_MCP_TOOL_IDS", () => {
        expect(expectedToolCount()).toBe(MOJITO_MCP_TOOL_IDS.length);
    });

    test("all tool ids follow mojito_<object>_<action> shape", () => {
        for (const id of MOJITO_MCP_TOOL_IDS) {
            expect(id.startsWith("mojito_")).toBe(true);
            const parts = id.split("_");
            expect(parts.length).toBeGreaterThanOrEqual(3);
        }
    });

    test("registered tool ids exactly match the canonical list", () => {
        expect(captureRegistrations().map(({ name }) => name)).toEqual(MOJITO_MCP_TOOL_IDS);
    });

    test("every registered tool has a non-empty description", () => {
        for (const { options } of captureRegistrations()) {
            expect(options.description?.trim()).not.toBe("");
        }
    });

    test("numeric id schemas enforce their boundaries", () => {
        const byName = new Map(captureRegistrations().map((entry) => [entry.name, entry]));
        const repositoryId = byName.get("mojito_repo_view")!.options.inputSchema.repositoryId;
        expect(repositoryId.safeParse(1).success).toBe(true);
        expect(repositoryId.safeParse(0).success).toBe(false);
        expect(repositoryId.safeParse(-1).success).toBe(false);
        expect(repositoryId.safeParse(1.5).success).toBe(false);
    });

    test("README and SKILL mention every published tool id", () => {
        const docs = [
            readFileSync(new URL("../README.md", import.meta.url), "utf8"),
            readFileSync(new URL("../SKILL.md", import.meta.url), "utf8"),
        ];

        for (const document of docs) {
            for (const toolId of MOJITO_MCP_TOOL_IDS) {
                expect(document).toContain(toolId);
            }
        }
    });
});
