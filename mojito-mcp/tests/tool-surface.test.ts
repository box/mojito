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

import { describe, expect, test } from "@jest/globals";
import { MOJITO_MCP_TOOL_IDS } from "../src/tool-metadata.js";
import { expectedToolCount } from "../src/server.js";

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
});
