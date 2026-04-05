# Demo 3 - MCP Integration

Extends demo2 by adding an MCP (Model Context Protocol) tool server for Formula 1 race data. The agent can now look up F1 race schedules, results, and standings, then chain those with the existing weather tools to answer questions like "What will the weather be at the next F1 race?"

## What's different from demo2

Demo2 has weather tools (geocoding, IP geolocation, weather forecast) as Temporal activity stubs. Demo3 adds F1 race data tools via the [F1 MCP server](https://github.com/rakeshgangwar/f1-mcp-server), integrated through the temporal-spring-ai library's MCP support.

### How the temporal-spring-ai library integrates MCP

The library provides a complete MCP-as-Temporal-activities pipeline. Here's the full flow:

**Worker startup — MCP server connection.** Spring AI's `spring-ai-starter-mcp-client` reads `mcp-servers.json`, spawns the F1 MCP server process (stdio), and creates `McpSyncClient` beans. The library's `McpClientActivityImpl` (a `@Component`) receives those clients via constructor injection.

**Workflow initialization — tool discovery.** In `@WorkflowInit`, the workflow creates an `McpClientActivity` activity stub, wraps it in `ActivityMcpClient`, and calls `McpToolCallback.fromMcpTools(mcpClient)`. This calls `mcpClient.listTools()` which dispatches through the activity stub to the worker, where `McpClientActivityImpl` calls each `McpSyncClient.listTools()` on the actual MCP server. The returned tool schemas become `McpToolCallback` instances (implementing Spring AI's `ToolCallback`).

**LLM sees all tools together.** The `TemporalChatClient` combines the activity-backed weather tools (via `defaultTools()`) and the MCP tool callbacks (via `defaultToolCallbacks()`) into one tool set. When calling the LLM, all tool schemas are sent together — the model doesn't know or care which are MCP vs activity-backed.

**MCP tool execution as a Temporal activity.** When the LLM requests an F1 tool call, Spring AI's `ToolCallingManager` finds the matching `McpToolCallback` and calls `callback.call(arguments)`. This dispatches through `ActivityMcpClient.callTool()` → the `McpClientActivity` activity stub — making it a Temporal activity execution, visible in the workflow history, retryable, with timeout protection. The activity impl routes to the right `McpSyncClient` and sends the request to the F1 MCP server via stdio.

**Result flows back through Spring AI's loop.** The MCP server returns the result, which flows back through the activity completion to `McpToolCallback` → `ToolCallingManager` → `ActivityChatModel`. If the model needs more tool calls, it loops. If done, the final text response returns from `chatClient.prompt().call().content()`.

**What the integration gives you for free:**
- MCP server lifecycle management (Spring AI starter handles spawn/connect)
- Tool discovery as a Temporal activity (durable, retryable)
- Each MCP tool invocation as a Temporal activity (visible in history, retryable, timeouts)
- MCP tools appear as standard Spring AI `ToolCallback` instances — no special handling
- The workflow code is just three lines: create stub, get callbacks, pass to builder

The workflow registers both tool types:
```java
this.chatClient = TemporalChatClient.builder(activityChatModel)
        .defaultTools(toolActivity)              // weather tools (activity stubs)
        .defaultToolCallbacks(mcpTools)           // F1 tools (MCP callbacks)
        .defaultSystem(systemPrompt)
        .build();
```

Note: activity stubs go through `defaultTools()` (processed by `TemporalToolUtil`), while MCP callbacks go through `defaultToolCallbacks()` (added directly, since they're already Temporal-safe via the `McpClientActivity`).

### F1 MCP server tools

| Tool | Parameters | Purpose |
|------|-----------|---------|
| `get_event_schedule` | year | F1 race calendar for a season |
| `get_event_info` | year, identifier | Details about a specific Grand Prix |
| `get_session_results` | year, event, session | Qualifying/race/practice results |
| `get_driver_info` | year, event, session, driver | Individual driver data |
| `analyze_driver_performance` | year, event, session, driver | Lap times and performance metrics |
| `compare_drivers` | year, event, session, drivers | Compare multiple drivers |
| `get_telemetry` | year, event, session, driver, lap | Vehicle telemetry data |
| `get_championship_standings` | year, round | Driver and constructor standings |

## Prerequisites

- **Java 21+**
- **Maven 3.9+**
- **Node.js 18+** (for the F1 MCP server)
- **Python 3.8+** and **uv** (for the F1 MCP server's FastF1 library)
- **Temporal CLI** — `brew install temporal` (macOS) or see [Temporal CLI docs](https://docs.temporal.io/cli)
- **OpenAI API key** — set as `OPENAI_API_KEY` environment variable

From the project root, run both setup scripts:

```bash
# Install pre-built temporal-spring-ai library jars
./scripts/install-libs.sh

# Clone, patch, and build the F1 MCP server (also writes mcp-servers.json)
./scripts/setup-f1-server.sh
```

## Running

### 1. Start the Temporal dev server

```bash
temporal server start-dev
```

### 2. Set your OpenAI API key

```bash
export OPENAI_API_KEY=sk-...
```

### 3. Start the worker

From the project root:

```bash
mvn -pl demo3-mcp spring-boot:run
```

### 4. Start a workflow

In a second terminal:

```bash
mvn -pl demo3-mcp compile exec:java -Dexec.args="When is the next F1 race and what will the weather be like?"
```

### Example prompts

```bash
# F1 + weather
mvn -pl demo3-mcp compile exec:java -Dexec.args="When is the next F1 race and what will the weather be like?"

# F1 schedule
mvn -pl demo3-mcp compile exec:java -Dexec.args="What is the 2025 F1 race calendar?"

# Weather only (existing tools still work)
mvn -pl demo3-mcp compile exec:java -Dexec.args="What is the weather in Barcelona?"

# Combined analysis
mvn -pl demo3-mcp compile exec:java -Dexec.args="What were the results of the last Monaco Grand Prix and what is the weather there right now?"
```

### Observing the workflow

View running workflows in the Temporal Web UI at [http://localhost:8233](http://localhost:8233). You'll see three types of activity entries:
- **ChatModel activities** — LLM calls
- **Tool activities** — Weather tool executions
- **MCP-Client activities** — F1 MCP tool calls (listTools, callTool)
