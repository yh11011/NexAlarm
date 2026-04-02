/**
 * Claude Desktop (stdio) 入口
 */
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { createServer } from "./nexalarm-server.js";

const server = createServer();
const transport = new StdioServerTransport();
await server.connect(transport);
