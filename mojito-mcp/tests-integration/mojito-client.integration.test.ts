import { afterAll, beforeAll, describe, expect, test } from "@jest/globals";
import { DefaultCliRunner } from "../src/cli-runner.js";
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
