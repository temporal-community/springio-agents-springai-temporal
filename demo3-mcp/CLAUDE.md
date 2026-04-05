# Demo 3 - MCP Integration

## Architecture

This demo builds on demo2 (temporal-spring-ai library) and adds MCP tool integration via the library's MCP support classes.

### How MCP tools become Temporal activities

The temporal-spring-ai library wraps MCP operations as Temporal activities:

1. `McpClientActivityImpl` (runs on worker) — holds real `McpSyncClient` instances, handles listTools/callTool
2. `McpClientActivity` — `@ActivityInterface` for MCP operations
3. `ActivityMcpClient` (used in workflow) — wraps the activity stub
4. `McpToolCallback` — implements `ToolCallback`, dispatches tool calls through the activity

When the LLM calls an F1 tool, the execution path is:
`ChatClient` → `ToolCallingManager` → `McpToolCallback.call()` → `ActivityMcpClient.callTool()` → `McpClientActivity` (Temporal activity) → `McpClientActivityImpl` → `McpSyncClient` → F1 MCP server (stdio)

### Key design decisions

**Combined tools array.** `TemporalChatClient.builder().defaultTools()` replaces rather than appends, so we combine activity-backed tools and MCP tool callbacks into a single `Object[]` array before calling it.

**Library jar version 0.0.2-SNAPSHOT.** Demo2 uses a core-only jar (0.0.1-SNAPSHOT) that strips the MCP package. Demo3 uses 0.0.2-SNAPSHOT which includes the `io.temporal.ai.mcp` package.

**MCP server config via JSON file.** Spring AI's MCP client starter reads `mcp-servers.json` from the classpath. The F1 server path must be an absolute path — users edit this file to point to their local install.

**`McpSyncClient` auto-wiring.** Spring AI's `spring-ai-starter-mcp-client` auto-creates `McpSyncClient` beans from the JSON config. `McpClientActivityImpl` (a `@Component` from the library) takes `List<McpSyncClient>` in its constructor.

**Tool name prefixing.** `McpToolCallback` uses `McpToolUtils.prefixedToolName()` which prefixes tool names with the MCP client name (e.g., `f1-data_get_event_schedule`). The LLM sees these prefixed names.

## File layout

```
io/temporal/ai/workshop/
├── AgentWorkflow.java           Workflow interface (same as demo2)
├── AgentWorkflowImpl.java       Adds MCP activity stub + McpToolCallback
├── Starter.java                 Plain Java client (same as demo2)
├── WorkerApplication.java       Spring Boot entry point (same as demo2)
└── tools/
    ├── HttpHelper.java           Shared HTTP GET utility (same as demo2)
    ├── ToolActivities.java       Weather tool activity interface (same as demo2)
    └── ToolActivitiesImpl.java   Weather tool implementation (same as demo2)

resources/
├── application.yaml              Adds MCP client config + mcpClientActivityImpl bean
└── mcp-servers.json              F1 MCP server stdio config
```

## Dependencies added over demo2

- `spring-ai-mcp` — Spring AI MCP client support
- `spring-ai-starter-mcp-client` — Auto-configures MCP client connections
- `temporal-spring-ai:0.0.2-SNAPSHOT` — Library jar with MCP package included
