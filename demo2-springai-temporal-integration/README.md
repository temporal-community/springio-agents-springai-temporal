# Demo 2 - Spring AI Temporal Integration

The same agentic loop as demo1, reimplemented using the [temporal-spring-ai](https://github.com/temporal-community/temporal-spring-ai) integration library. The library makes Spring AI's built-in tool calling loop durable by routing LLM calls and tool executions through Temporal activities transparently.

## What's different from demo1

In demo1, we manually built the agentic loop: an explicit for-loop in the workflow that calls an LLM activity, checks for tool calls, dispatches them via dynamic activities, and repeats. We also wrote our own serializable DTOs and tool dispatch infrastructure.

In demo2, the library handles all of that. The workflow's `run` method is one line:

```java
return chatClient.prompt().user(goal).call().content();
```

Spring AI's `ChatClient` drives the tool calling loop. Under the hood, `ActivityChatModel` routes each LLM call to a Temporal activity, and tool activity stubs route each tool execution to its own Temporal activity. The developer writes standard Spring AI code; Temporal durability is automatic.

## Architecture

- **`ActivityChatModel`** — Implements Spring AI's `ChatModel` interface, but dispatches to `ChatModelActivity` (a Temporal activity) for the actual LLM call. Handles the tool calling loop internally.
- **`TemporalChatClient`** — A Temporal-aware `ChatClient` that routes tools through `TemporalToolUtil`, which detects activity stubs and wraps them as `ActivityToolCallback` instances.
- **`ToolActivities`** — A single `@ActivityInterface` with both `@ActivityMethod` and `@Tool` on each method. This serves as both the Temporal activity contract and the Spring AI tool definition.
- **`temporal-spring-boot-starter`** — Auto-configures the Temporal client, worker, and factory from `application.yaml`. No manual `TemporalConfig` needed.

### Trade-off: tool coupling

In demo1, the `tools/` directory contains plain Java classes with `@Tool` annotations — no Temporal imports. The `DynamicToolActivity` and `ToolRegistry` bridge them to Temporal, keeping tool logic completely decoupled from infrastructure.

In demo2, the library requires tools to be Temporal activity stubs (with both `@ActivityMethod` and `@Tool` on the same interface). The library's `TemporalToolUtil` only accepts activity stubs, local activity stubs, Nexus service stubs, or `@DeterministicTool` classes — plain Java objects are rejected. This means the tools directory now contains Temporal-specific code. You get a simpler workflow, but tools are no longer portable outside of Temporal.

### Tools

Same tools as demo1:

| Tool | API | Purpose |
|------|-----|---------|
| `getIpAddress` | icanhazip.com | Get the caller's public IP address |
| `getLocationInfo` | ip-api.com | Get city, country, lat/lon for an IP address |
| `getCoordinates` | Open-Meteo Geocoding | Get lat/lon for a city name |
| `getWeather` | Open-Meteo Forecast | Get current temperature, weather code, and wind speed |

## Prerequisites

- **Java 21+**
- **Maven 3.9+**
- **Temporal CLI** — `brew install temporal` (macOS) or see [Temporal CLI docs](https://docs.temporal.io/cli)
- **OpenAI API key** — set as `OPENAI_API_KEY` environment variable
- **temporal-spring-ai library** — a pre-built jar is included in the repo. From the project root, run:

```bash
./scripts/install-libs.sh
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
mvn -pl demo2-springai-temporal-integration spring-boot:run
```

The `temporal-spring-boot-starter` auto-configures the worker from `application.yaml`, registers the workflow and activity beans, and starts polling the `agent-task-queue` task queue. Leave this running.

### 4. Start a workflow

In a second terminal:

```bash
mvn -pl demo2-springai-temporal-integration compile exec:java -Dexec.args="What is the weather in Barcelona?"
```

### Example prompts

```bash
# Weather by city name
mvn -pl demo2-springai-temporal-integration compile exec:java -Dexec.args="What is the weather in Tokyo?"

# Weather at current location
mvn -pl demo2-springai-temporal-integration compile exec:java -Dexec.args="What is the weather where I am?"

# Multi-city comparison
mvn -pl demo2-springai-temporal-integration compile exec:java -Dexec.args="Compare the weather in London and Sydney right now"
```

### Observing the workflow

View running workflows in the Temporal Web UI at [http://localhost:8233](http://localhost:8233). Each LLM call and tool execution appears as a separate activity in the workflow history, just like demo1.
