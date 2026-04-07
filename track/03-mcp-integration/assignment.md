---
slug: mcp-integration
id: ir1uvq3cieus
type: challenge
title: 'Demo 3: MCP Tooling'
teaser: Chain F1 race data from an MCP server with live weather data — both sets of
  tools run as Temporal activities.
notes:
- type: text
  contents: |-
    **Demo 3: MCP Integration**

    So far, the agent's tools have been plain Java methods registered as Temporal activity stubs.
    What about tools that come from outside your codebase entirely?

    MCP (Model Context Protocol) is a standard for packaging tools as standalone servers.
    The F1 MCP server provides race schedules, results, standings, and telemetry as tools —
    and the `temporal-spring-ai` library makes each MCP tool call a Temporal activity automatically.

    When the LLM calls an F1 tool:
    - The call routes through `McpToolCallback` → `ActivityMcpClient`
    - That dispatches to a `McpClientActivity` Temporal activity
    - The activity calls the F1 MCP server via stdio
    - The result flows back through the activity into the Spring AI tool-calling loop

    Every F1 data fetch is visible in the Temporal workflow history, retryable, and protected by timeouts.
    Hit **Start** when you're ready.
tabs:
- id: rng3dkhigxsc
  title: Terminal 1 - Worker
  type: terminal
  hostname: workshop-host
  workdir: /workspace/workshop
- id: bywhcnxnfbxn
  title: Terminal 2 - Starter
  type: terminal
  hostname: workshop-host
  workdir: /workspace/workshop
- id: nfgj6wknyzbw
  title: VS Code
  type: service
  hostname: workshop-host
  path: ?folder=/workspace/workshop/demo3-mcp
  port: 8443
- id: c55vypprskrw
  title: Temporal Web UI
  type: service
  hostname: workshop-host
  path: /
  port: 8080
- id: cznvhzrttrdx
  title: Network Control Panel
  type: service
  hostname: workshop-host
  path: /
  port: 5000
difficulty: basic
timelimit: 1800
enhanced_loading: null
---

## Demo 3: MCP Integration

### What changed

Open **VS Code** and look at `demo3-mcp/src/main/java/io/temporal/ai/workshop/AgentWorkflowImpl.java`.

The `@WorkflowInit` constructor now creates an additional activity stub: `McpClientActivity`. In `run()`, three lines set up the MCP tools:

```java
ActivityMcpClient mcpClient = new ActivityMcpClient(mcpClientActivity);
List<ToolCallback> mcpTools = McpToolCallback.fromMcpTools(mcpClient);
```

`fromMcpTools()` calls `listTools()` through the `McpClientActivity` stub — so tool discovery itself is a Temporal activity, visible in the workflow history.

The `TemporalChatClient` then receives both tool sets: activity-backed weather tools via `defaultTools()`, and MCP tool callbacks via `defaultToolCallbacks()`. The LLM sees them all as a single flat list.

Also look at `resources/mcp-servers.json` — this tells Spring AI's MCP client starter how to spawn the F1 MCP server process (Node.js + Python venv, via stdio).

---

### Run the worker

In **Terminal 1**:

```
cd demo3-mcp
mvn spring-boot:run
```

The first startup takes a few extra seconds while the MCP client connects to the F1 server and discovers its tools.

---

### Start a workflow

In **Terminal 2**:

```
cd demo3-mcp
mvn compile exec:java -Dexec.args="When is the next F1 race and what will the weather be like there?"
```

---

### Watch the combined activity history

Open the **Temporal Web UI** tab. You should see three distinct activity types in the workflow history:

- **`ChatModelActivity`** — LLM calls (same as demos 1 and 2)
- **Tool activities** — weather tool executions (same as demos 1 and 2)
- **`McpClientActivity`** — F1 tool calls, routed through the MCP layer

---

### Disrupt F1 data mid-demo

Open the **Network Control Panel** and toggle **F1 Data** off while a workflow is running. The F1 tool activities fail; Temporal retries them automatically. Toggle F1 Data back on — the next retry succeeds and the workflow continues.

This works because the FastF1 Python library (which the F1 MCP server uses to fetch race data) respects the `HTTP_PROXY` environment variable set in the VM. Every F1 API request routes through the workshop proxy.

---

### More prompts to try

```
# Combined F1 + weather
mvn compile exec:java -Dexec.args="What is the 2025 F1 race calendar?"

# Weather tools still work independently
mvn compile exec:java -Dexec.args="What is the weather in Monaco right now?"

# Multi-step reasoning
mvn compile exec:java -Dexec.args="What were the results of the last Monaco Grand Prix and what is the weather there today?"
```
