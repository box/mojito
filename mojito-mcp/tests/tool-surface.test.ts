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
 * Tool ids are the API this server exposes to AI agents, and they are also quoted by
 * name in README.md and SKILL.md. Renaming or reordering one silently breaks agent
 * prompts and skills that reference it, so these tests treat the list as a contract
 * rather than an implementation detail.
 *
 * 1. The exact list, in order. Spelling the ten ids out literally means any rename,
 *    removal, addition, or reshuffle shows up as a failing test and therefore as a
 *    deliberate diff a reviewer has to approve, not an accident.
 * 2. No duplicate ids. A repeated id would silently shadow one tool with another during
 *    registration, leaving the agent with a tool that quietly does the wrong thing.
 * 3. The count the server reports matches the list. `expectedToolCount()` is what other
 *    tests and docs parity checks lean on, so it must not drift from the ids themselves.
 * 4. Every id follows the `mojito_<object>_<action>` convention. The naming scheme is
 *    what lets an agent guess a tool's purpose from its name, so it is enforced rather
 *    than just documented.
 * 5. The registrations made against McpServer exactly match the canonical list. This
 *    closes the dangerous gap where metadata could advertise a tool that the server
 *    forgot to register, or registration could expose an undocumented extra tool.
 * 6. Every registered tool has a non-empty description. Tool descriptions are the model's
 *    primary instructions, so a nameless contract is not sufficient.
 * 7. Numeric identifiers reject zero, negative, and fractional values at the MCP boundary,
 *    while search offset deliberately accepts zero.
 * 8. README.md and SKILL.md mention every published id, keeping human setup docs and agent
 *    operating guidance synchronized with the server.
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
            "mojito_repo_create",
            "mojito_repo_delete",
            "mojito_textunit_search",
            "mojito_textunit_info",
            "mojito_textunit_history",
            "mojito_textunit_translation_add",
            "mojito_review_update",
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

    test("numeric id, limit, and offset schemas enforce their boundaries", () => {
        const byName = new Map(captureRegistrations().map((entry) => [entry.name, entry]));
        const repositoryId = byName.get("mojito_repo_view")!.options.inputSchema.repositoryId;
        expect(repositoryId.safeParse(1).success).toBe(true);
        expect(repositoryId.safeParse(0).success).toBe(false);
        expect(repositoryId.safeParse(-1).success).toBe(false);
        expect(repositoryId.safeParse(1.5).success).toBe(false);

        const search = byName.get("mojito_textunit_search")!.options.inputSchema;
        expect(search.limit.safeParse(1).success).toBe(true);
        expect(search.limit.safeParse(0).success).toBe(false);
        expect(search.offset.safeParse(0).success).toBe(true);
        expect(search.offset.safeParse(-1).success).toBe(false);
        expect(search.offset.safeParse(1.5).success).toBe(false);
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
