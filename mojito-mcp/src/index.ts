#!/usr/bin/env node
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { DefaultCliRunner } from "./cli-runner.js";
import { loadConfigFromEnv } from "./config.js";
import { MojitoCliClient } from "./mojito-client.js";
import { createMojitoMcpServer } from "./server.js";

async function main(): Promise<void> {
    const config = loadConfigFromEnv();
    const runner = new DefaultCliRunner(config);
    const client = new MojitoCliClient(config, runner);

    // Skeleton: will call client.probeHelp() once implemented
    await client.probeHelp();

    const mcpServer = createMojitoMcpServer(client);
    const transport = new StdioServerTransport();
    await mcpServer.connect(transport);
}

main().catch((err: unknown) => {
    console.error(err);
    process.exit(1);
});
