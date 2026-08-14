/**
 * Canonical tool ids for this server. Keep in sync with {@link registerMojitoTools}.
 */
export const MOJITO_MCP_TOOL_IDS = [
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
] as const;

export type MojitoMcpToolId = (typeof MOJITO_MCP_TOOL_IDS)[number];
