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
