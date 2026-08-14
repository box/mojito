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
