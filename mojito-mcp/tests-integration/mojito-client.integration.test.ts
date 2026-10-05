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
 * Test scenarios: end-to-end against a real dev Mojito.
 *
 * The unit tests prove we build the right CLI arguments; only these tests prove the
 * arguments are ones Mojito accepts. They spawn a genuine CLI wrapper against a live
 * server, so they are excluded from the default `pnpm test` run and require a local
 * tests-integration/integration-config.json (see README.md in this directory). Point
 * them at a dev instance and a disposable repository name, never at production.
 *
 * 1. The configured dev CLI is usable. `probeHelp` is the same check the server performs
 *    at startup, so running it first means a broken wrapper or bad config fails with an
 *    obvious message instead of surfacing as a confusing failure in a later test.
 * 2. Listing repositories returns JSON. The cheapest read-only proof that authentication
 *    works, the host is reachable, and stdout parses as the shape we expect.
 * 3. The full repository lifecycle: create, view, search, delete. This is the one path
 *    that exercises writes against a real server, including the nested JSON body for
 *    derived locales (`fr-FR` plus `(fr-CA)->fr-FR`) that the CLI receives via a temp
 *    file. It first deletes any leftover repository of the same name so an interrupted
 *    earlier run cannot fail the suite, verifies a duplicate name is rejected, confirms
 *    the created repository can be read back and searched, then deletes it and confirms
 *    it is gone. An afterAll hook removes the repository if the test dies midway, so a
 *    failure does not litter the dev server.
 *
 * Not covered here: translation writes and review updates, text unit history, pollable
 * tasks, and the all-repositories search expansion. Those are argv-level unit tests only,
 * because asserting them live would need seeded translation data on the dev instance.
 */

import { afterAll, beforeAll, describe, expect, test } from "@jest/globals";
import { DefaultCliRunner } from "../src/cli-runner.js";
import { MojitoCliError } from "../src/errors.js";
import { MojitoCliClient } from "../src/mojito-client.js";
import { loadIntegrationConfig, type IntegrationConfig } from "./load-integration-config.js";

type RepoRow = { id?: number; name?: string };

function asRepoArray(value: unknown): RepoRow[] {
    return Array.isArray(value) ? (value as RepoRow[]) : [];
}

function requireRepoId(value: unknown, context: string): number {
    if (value && typeof value === "object" && "id" in value) {
        const id = (value as { id: unknown }).id;
        if (typeof id === "number" && Number.isFinite(id)) {
            return id;
        }
    }
    throw new Error(
        `${context}: expected repository JSON with numeric id, got ${JSON.stringify(value)}`,
    );
}

/**
 * Live integration tests against the **dev** Mojito CLI / server.
 * Requires tests-integration/integration-config.json (see README.md).
 */
describe("MojitoCliClient integration (dev)", () => {
    let integration: IntegrationConfig;
    let client: MojitoCliClient;
    /** Set when create succeeds so afterAll can clean up on failure mid-suite. */
    let createdRepositoryId: number | undefined;

    beforeAll(() => {
        integration = loadIntegrationConfig();
        const runner = new DefaultCliRunner({
            cliBinary: integration.devCli,
            timeoutMs: integration.timeoutMs ?? 600_000,
        });
        client = new MojitoCliClient(runner.config, runner);
    });

    afterAll(async () => {
        if (createdRepositoryId === undefined) {
            return;
        }
        try {
            await client.repoDelete(createdRepositoryId);
        } catch {
            // Best-effort cleanup if the delete test already removed it or the server rejected.
        }
    });

    test("probeHelp succeeds with the configured dev CLI", async () => {
        await expect(client.probeHelp()).resolves.toBeUndefined();
    });

    test("repoList returns a JSON array from the dev server", async () => {
        const repos = await client.repoList();
        expect(Array.isArray(repos)).toBe(true);
    });

    test("creates, views, and deletes the configured test repository on dev", async () => {
        const repoName = integration.testRepositoryName;

        // Leftover from a previous interrupted run
        const existing = asRepoArray(await client.repoList({ name: repoName }));
        for (const row of existing) {
            if (typeof row.id === "number") {
                await client.repoDelete(row.id);
            }
        }

        const created = await client.repoCreate({
            name: repoName,
            description: "Created by mojito-mcp integration tests; safe to delete",
            repositoryLocales: ["fr-FR", "(fr-CA)->fr-FR"],
        });
        const repositoryId = requireRepoId(created, "repoCreate");
        createdRepositoryId = repositoryId;

        expect(created).toMatchObject({
            id: repositoryId,
            name: repoName,
        });

        await expect(client.repoCreate({ name: repoName })).rejects.toBeInstanceOf(MojitoCliError);

        const viewed = await client.repoView(repositoryId);
        expect(viewed).toMatchObject({
            id: repositoryId,
            name: repoName,
        });

        const searchHits = await client.textunitSearch({
            repositoryNames: [repoName],
            searchType: "CONTAINS",
            limit: 5,
        });
        expect(Array.isArray(searchHits)).toBe(true);

        await client.repoDelete(repositoryId);
        createdRepositoryId = undefined;

        const afterDelete = asRepoArray(await client.repoList({ name: repoName }));
        expect(afterDelete.find((r) => r.id === repositoryId)).toBeUndefined();
    });
});
