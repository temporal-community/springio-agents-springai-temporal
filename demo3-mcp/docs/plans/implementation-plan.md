# Plan: Demo 3 - MCP Integration with F1 Data

## Context

Demo3 adds an MCP tool to the agentic loop. We're integrating the F1 MCP server (https://github.com/rakeshgangwar/f1-mcp-server) alongside our existing weather tools. The demo narrative: "When's the next F1 race? What's the weather going to be at the circuit?" — the LLM chains F1 tools (from MCP) with our weather tools to answer.

Demo3 builds on demo2 (temporal-spring-ai library) because the library already has MCP-as-Temporal-activity support: `McpToolCallback`, `ActivityMcpClient`, `McpClientActivity`, `McpClientActivityImpl`.

## Key library MCP classes

- `McpClientActivity` — `@ActivityInterface` wrapping MCP operations (listTools, callTool)
- `McpClientActivityImpl` — Activity impl that holds real `McpSyncClient` instances, registered as `@Component`
- `ActivityMcpClient` — Workflow-side wrapper around the activity stub
- `McpToolCallback` — Implements `ToolCallback`, calls MCP tools via the activity. `McpToolCallback.fromMcpTools(client)` returns a list of ToolCallbacks for all tools from all connected MCP servers.

## What changes from demo2

### Library jar
The core-only jar we built for demo2 stripped out the `io.temporal.ai.mcp` package. For demo3, we need to build a new jar that includes it. Strip the same packages as before EXCEPT keep `mcp`:

```bash
rm -rf io/temporal/ai/chattools io/temporal/ai/workflows io/temporal/ai/vectorstore io/temporal/ai/TemporalSpringAiChat.class META-INF
```

### New dependencies
- `spring-ai-mcp` — Spring AI's MCP client support (provides `McpSyncClient`, `McpSchema`, etc.)
- `spring-ai-starter-mcp-client` — Auto-configures MCP client connections from application config
- `io.modelcontextprotocol:mcp` — MCP protocol SDK

### Workflow changes
`AgentWorkflowImpl` adds an `McpClientActivity` activity stub, wraps it in `ActivityMcpClient`, gets `McpToolCallback` instances, and passes them alongside the tool activity stub to `TemporalChatClient.builder().defaultTools(...)`.

### Worker configuration
`application.yaml` adds MCP client configuration pointing to the F1 MCP server (stdio transport via `node build/index.js`). The `McpClientActivityImpl` bean needs to be registered as an activity bean.

## Implementation steps

### Step 1: Rebuild the library jar with MCP classes

```bash
cd /path/to/temporal-spring-ai/build/lib-only
# Same as demo2 but keep the mcp package
rm -rf io/temporal/ai/chattools io/temporal/ai/workflows io/temporal/ai/vectorstore io/temporal/ai/TemporalSpringAiChat.class META-INF
jar cf temporal-spring-ai-mcp.jar io

mvn install:install-file \
  -Dfile=temporal-spring-ai-mcp.jar \
  -DgroupId=io.temporal.ai \
  -DartifactId=temporal-spring-ai \
  -Dversion=0.0.2-SNAPSHOT \
  -Dpackaging=jar
```

Use version `0.0.2-SNAPSHOT` to distinguish from the core-only jar used by demo2.

### Step 2: Create demo3 module

Copy demo2 as the starting point. New directory: `demo3-mcp/`

pom.xml changes from demo2:
- Change artifactId to `demo3-mcp`
- Update `temporal-spring-ai` version to `0.0.2-SNAPSHOT`
- Add MCP dependencies: `spring-ai-mcp`, `spring-ai-starter-mcp-client`

Add to parent POM `<modules>`.

### Step 3: Install the F1 MCP server

Prerequisites documented in README:
```bash
git clone https://github.com/rakeshgangwar/f1-mcp-server.git
cd f1-mcp-server
pip install fastf1 pandas numpy
npm install
npm run build
```

### Step 4: Configure MCP client in application.yaml

Add Spring AI MCP client config for stdio transport:
```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          servers-configuration: classpath:mcp-servers.json
```

With `mcp-servers.json`:
```json
{
  "f1-data": {
    "command": "node",
    "args": ["/path/to/f1-mcp-server/build/index.js"]
  }
}
```

Note: The path to the F1 server will need to be configured per-environment. Could use an env var.

### Step 5: Update AgentWorkflowImpl

Add MCP activity stub and tool callbacks:
```java
McpClientActivity mcpClientActivity = Workflow.newActivityStub(
        McpClientActivity.class,
        ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .build());

ActivityMcpClient mcpClient = new ActivityMcpClient(mcpClientActivity);
List<ToolCallback> mcpTools = McpToolCallback.fromMcpTools(mcpClient);

this.chatClient = TemporalChatClient.builder(activityChatModel)
        .defaultTools(toolActivity)           // existing weather tools
        .defaultTools(mcpTools.toArray())      // F1 MCP tools
        .defaultSystem(SYSTEM_PROMPT)
        .build();
```

Need to verify: can `defaultTools()` be called multiple times, or do we need to combine into one array?

### Step 6: Update application.yaml worker config

Add `mcpClientActivityImpl` to the activity-beans list:
```yaml
activity-beans:
  - chatModelActivity
  - toolActivitiesImpl
  - mcpClientActivityImpl
```

### Step 7: Update system prompt

Add context about F1 tools being available so the LLM knows it can look up race information.

### Step 8: Write README, CLAUDE.md

Document setup (F1 server install, library rebuild), architecture, and example prompts like:
- "When is the next F1 race and what will the weather be like?"
- "What's the weather forecast for the Monaco Grand Prix?"

## File layout

```
demo3-mcp/
├── pom.xml
├── README.md
├── CLAUDE.md
└── src/main/
    ├── java/io/temporal/ai/workshop/
    │   ├── AgentWorkflow.java            (same as demo2)
    │   ├── AgentWorkflowImpl.java        (adds MCP activity stub + tools)
    │   ├── Starter.java                  (same as demo2)
    │   ├── WorkerApplication.java        (same as demo2)
    │   └── tools/
    │       ├── HttpHelper.java           (same as demo2)
    │       ├── ToolActivities.java       (same as demo2)
    │       └── ToolActivitiesImpl.java   (same as demo2)
    └── resources/
        ├── application.yaml              (adds MCP client config)
        └── mcp-servers.json              (F1 MCP server definition)
```

## Open risks

- **`defaultTools()` chaining** — Need to verify if `TemporalChatClient.builder()` supports calling `.defaultTools()` multiple times, or if we need to combine all tools into a single array/varargs call.
- **MCP dependency chain** — `spring-ai-mcp` and `spring-ai-starter-mcp-client` pull in additional transitive dependencies. May need to check for conflicts with existing deps.
- **F1 server path** — The stdio config needs an absolute path to the F1 server's `build/index.js`. Needs to be documented and possibly made configurable via env var.
- **McpSyncClient auto-configuration** — Spring AI's `spring-ai-starter-mcp-client` should auto-create `McpSyncClient` beans from the config. `McpClientActivityImpl` takes `List<McpSyncClient>` in its constructor. Need to verify the auto-wiring works.
- **Tool name prefixing** — `McpToolCallback.getToolDefinition()` uses `McpToolUtils.prefixedToolName()` which may prefix tool names with the client name. The LLM will see prefixed names. Need to check what they look like.

## Verification

1. Rebuild library jar with MCP classes and install
2. Install F1 MCP server (clone, pip install, npm install, npm run build)
3. `mvn -pl demo3-mcp compile`
4. Start Temporal dev server
5. Start worker: `mvn -pl demo3-mcp spring-boot:run`
6. Test prompts:
   - `"When is the next F1 race and what will the weather be like?"`
   - `"What's the weather forecast for the Monaco Grand Prix 2025?"`
   - `"Compare the weather at the next three F1 races"`
7. Verify in Temporal Web UI that MCP tool calls appear as separate activity entries
