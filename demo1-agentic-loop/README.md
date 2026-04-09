# Demo 1 - Agentic Loop

An agentic loop implemented as a Temporal workflow, using Spring AI with OpenAI. The agent calls an LLM with a set of tools and loops until it has enough information to answer the user's goal.

## Architecture

The agent runs as a Temporal workflow that orchestrates two types of activities:

- **LLM Activity** (`LlmActivities`) - Calls OpenAI via Spring AI's `ChatModel`. Receives the conversation history, sends it to the model along with tool schemas, and returns the model's response (either a text answer or tool calls).
- **Tool Activities** (`ToolActivities`) - Execute the agent's tools, each of which makes external HTTP calls:

| Tool | API | Purpose |
|------|-----|---------|
| `getIpAddress` | icanhazip.com | Get the caller's public IP address |
| `getLocationInfo` | ip-api.com | Get city, country, lat/lon for an IP address |
| `getCoordinates` | Open-Meteo Geocoding | Get lat/lon for a city name |
| `getWeather` | Open-Meteo Forecast | Get current temperature, weather code, and wind speed |

Tools are dispatched via Temporal's dynamic activity mechanism — the workflow itself knows nothing about specific tools. It simply forwards the tool name and arguments chosen by the LLM to a generic `DynamicToolActivity`, which looks up the handler in a `ToolRegistry`.

The workflow loop:
1. Sends the conversation (system prompt + user goal + any prior tool results) to the LLM activity
2. If the LLM returns tool calls, dispatches each to a tool activity and adds the results to the conversation
3. If the LLM returns a text response, the agent is done

Temporal provides durable execution - if the worker crashes mid-loop, the workflow replays from history and resumes where it left off without re-executing completed activities.

## Prerequisites

- **Java 21+**
- **Maven 3.9+**
- **Temporal CLI** — `brew install temporal` (macOS) or see [Temporal CLI docs](https://docs.temporal.io/cli)
- **OpenAI API key** — set as `OPENAI_API_KEY` environment variable

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
mvn -pl demo1-agentic-loop spring-boot:run
```

This starts a Spring Boot application that registers the workflow and activities with Temporal and begins polling the `agent-task-queue` task queue. Leave this running.

### 4. Start a workflow

In a second terminal:

```bash
mvn -pl demo1-agentic-loop compile exec:java -Dexec.args="What is the weather in Barcelona?"
```

The starter creates a Temporal workflow, waits for the result, prints it, and exits.

### Example prompts

```bash
# Weather by city name (uses getCoordinates -> getWeather)
mvn -pl demo1-agentic-loop compile exec:java -Dexec.args="What is the weather in Tokyo?"

# Weather at current location (uses getIpAddress -> getLocationInfo -> getWeather)
mvn -pl demo1-agentic-loop compile exec:java -Dexec.args="What is the weather where I am?"

# Multi-city comparison
mvn -pl demo1-agentic-loop compile exec:java -Dexec.args="Compare the weather in London and Sydney right now"
```

## Try the durability

One of Temporal's key benefits is that workflows survive worker crashes. To see this in action:

1. Start a workflow with a multi-step prompt (so it takes a few seconds):
   ```bash
   mvn -pl demo1-agentic-loop compile exec:java -Dexec.args="Compare the weather in London and Sydney right now"
   ```

2. While the workflow is running (watch the worker terminal for activity logs), **kill the worker with Ctrl+C**.

3. In the Temporal Web UI at [http://localhost:8233](http://localhost:8233), the workflow shows as "Running" — it's waiting for the worker to come back.

4. Restart the worker:
   ```bash
   mvn -pl demo1-agentic-loop spring-boot:run
   ```

5. The workflow **resumes where it left off** — completed activities are not re-executed. You can confirm this in the Web UI: the event history shows which activities ran before the crash and which ran after the restart.

This is durable execution. Without Temporal, a crash mid-loop means starting over.

## Adding or swapping tools

Tools are fully decoupled from the agent workflow and activity infrastructure. Each tool is a plain Java method annotated with Spring AI's `@Tool` and `@ToolParam` — these annotations are the single source of truth for both the LLM schema and the execution logic.

**To add a new tool**, create a class in the `tools/` package:

```java
public class MyTools {
    @Tool(description = "Do something useful.")
    public String myTool(@ToolParam(description = "The input") String input) {
        // implementation
    }
}
```

Then register it in `TemporalConfig`:

```java
return new ToolRegistry(new LocationTools(), new WeatherTools(), new MyTools());
```

Nothing else changes — the workflow, dynamic activity, and LLM activity are all tool-agnostic.

**To swap the entire tool set**, just change the `ToolRegistry` constructor arguments. For example, to replace the weather tools with a completely different set:

```java
return new ToolRegistry(new DatabaseTools(), new SlackTools());
```

### Observing the workflow

While a workflow is running, you can view it in the Temporal Web UI at [http://localhost:8233](http://localhost:8233). You'll see each LLM call and tool execution as separate activity entries in the workflow history.
