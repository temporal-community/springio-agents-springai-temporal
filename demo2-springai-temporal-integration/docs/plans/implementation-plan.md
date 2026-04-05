# Demo 2 - Implementation Plan

## Goal

Reimplement demo1's agentic loop using the [temporal-spring-ai](https://github.com/temporal-community/temporal-spring-ai) integration library. Same tools, same behavior, but the LLM activity wrapping and tool dispatch come from the library instead of our manual implementation.

## What the library provides

The `temporal-spring-ai` library makes Spring AI code run on Temporal transparently:

- **`ActivityChatModel`** — Implements Spring AI's `ChatModel` interface but dispatches to a Temporal activity under the hood. From the workflow's perspective, it's just calling `chatModel.call(prompt)`.
- **`ChatModelActivity` / `ChatModelActivityImpl`** — The Temporal activity that calls the real `ChatModel` (OpenAI, etc.).
- **Activity-backed tools** — Tools defined on `@ActivityInterface` with `@Tool` annotations serve double duty: they're both Temporal activity contracts and Spring AI tool definitions. The library's `ActivityToolUtil` extracts tool schemas from activity stubs.
- **Automatic tool loop** — `ActivityChatModel` handles the tool calling loop internally using Spring AI's `ToolCallingManager`. No manual loop needed.
- **`SandboxingAdvisor`** — Safety net that ensures non-activity tool callbacks get wrapped as local activities to preserve workflow determinism.
- **`@DeterministicTool`** — Annotation for tools safe to run directly in the workflow (no I/O).
- **`temporal-spring-boot-starter`** — Auto-configures Temporal client, worker factory, and workers via `application.properties`.

### Other interesting features (not for this demo, but worth noting)

- **Update-based conversation** (`ChatWorkflow`) — Uses `@UpdateMethod` for request/reply chat, enabling multi-turn conversations within a single workflow.
- **Chat memory** — Spring AI's `MessageWindowChatMemory` integrated with the workflow for durable conversation history.
- **`ActivityVectorStore`** — RAG retrieval as a Temporal activity.
- **MCP integration** — MCP server tools as Spring AI callbacks backed by activities.
- **Multi-model support** — Auto-registers activity beans for every `ChatModel` in the Spring context.

## Dependency situation

The library is **not published to Maven Central** — it's a `0.0.1-SNAPSHOT`. We need to build it locally:

```bash
git clone https://github.com/temporal-community/temporal-spring-ai.git
cd temporal-spring-ai
./gradlew publishToMavenLocal
```

This puts it in `~/.m2/repository/io.temporal.ai/temporal-spring-ai/0.0.1-SNAPSHOT/`. The demo2 pom.xml will reference it from there.

The library also pulls in `io.temporal:temporal-spring-boot-starter:1.31.0` which auto-configures the Temporal client and worker. This replaces our manual `TemporalConfig`.

**Version implications:** The library uses Spring Boot 3.5.3, Spring AI 1.0.1, and Temporal SDK 1.31.0. Demo1 uses Spring Boot 3.4.5, Spring AI 1.0.0, and Temporal SDK 1.27.0. We should align demo2 with the library's versions. The parent pom versions can stay as-is (demo2 will override where needed).

## What changes from demo1

### Removed (provided by the library)
| Demo1 file | Replaced by |
|---|---|
| `LlmActivities.java` | `ChatModelActivity` from the library |
| `LlmActivitiesImpl.java` | `ChatModelActivityImpl` from the library |
| `DynamicToolActivity.java` | Library's activity-backed tool mechanism |
| `ToolRegistry.java` | Library's `ActivityToolUtil` / `TemporalToolUtil` |
| `TemporalConfig.java` (worker setup) | `temporal-spring-boot-starter` auto-configuration |
| `model/LlmMessage.java` | Library's `ChatModelTypes` serializable records |
| `model/LlmResponse.java` | Library's `ChatModelTypes` serializable records |
| `model/ToolCallInfo.java` | Library's `ChatModelTypes` serializable records |

### Kept (same as demo1)
| File | Notes |
|---|---|
| `tools/HttpHelper.java` | Shared HTTP utility, unchanged |

### Changed
| File | What changes |
|---|---|
| `AgentWorkflow.java` | Same interface |
| `AgentWorkflowImpl.java` | Uses `ActivityChatModel` + `ChatClient` instead of manual loop. Tools are activity stubs passed to the chat client. The tool calling loop is handled by Spring AI automatically. |
| `Starter.java` | Likely unchanged or minor adjustments |
| `WorkerApplication.java` | Simpler — auto-configuration handles worker setup |
| Tool definitions | Restructured as `@ActivityInterface` with `@Tool` on methods (library's pattern) |

### New
| File | Purpose |
|---|---|
| `ToolActivities.java` | `@ActivityInterface` with `@Tool` + `@ActivityMethod` on each method |
| `ToolActivitiesImpl.java` | Implementation with HTTP calls (same logic as demo1's tool methods) |
| `application.properties` | Temporal connection config for the auto-starter + Spring AI config |

## Architecture comparison

### Demo1 (manual)
```
Workflow
  ├── calls LlmActivities.callLlm(messages)     [typed activity]
  ├── checks for tool calls
  ├── calls toolActivity.execute(name, args)     [dynamic activity]
  └── loops
```

### Demo2 (library)
```
Workflow
  ├── chatClient.prompt(goal).call()             [Spring AI ChatClient]
  │   ├── ActivityChatModel.call(prompt)          [dispatches to activity]
  │   ├── ToolCallingManager executes tools       [via activity stubs]
  │   └── loops automatically until done
  └── returns result
```

The workflow becomes much simpler — potentially just a few lines. The agentic loop is handled by Spring AI's internal tool calling mechanism, with Temporal providing durability for each step.

## Tool definition approach

In demo1, tools have `@Tool` annotations for schema generation, and execution is dispatched via `DynamicToolActivity`. In demo2, the library's pattern is to put `@Tool` and `@ActivityMethod` on the same interface methods:

```java
@ActivityInterface
public interface ToolActivities {
    @Tool(description = "Get the latitude and longitude for a city name.")
    @ActivityMethod
    String getCoordinates(@ToolParam(description = "The city name") String city);
}
```

The activity stub created by `Workflow.newActivityStub(ToolActivities.class, ...)` is then passed as a tool to the chat client. Spring AI sees the `@Tool` annotations for schema; Temporal sees the `@ActivityMethod` for activity dispatch. Both concerns on one interface.

## Implementation steps

1. **Build the library locally** — Clone and `./gradlew publishToMavenLocal`
2. **Create demo2 module** — pom.xml inheriting from parent, with library dependency and version overrides
3. **Define `ToolActivities` interface** — `@ActivityInterface` + `@Tool` on each method (getIpAddress, getLocationInfo, getCoordinates, getWeather)
4. **Implement `ToolActivitiesImpl`** — Same HTTP logic as demo1, using `HttpHelper`
5. **Implement `AgentWorkflowImpl`** — Use `ActivityChatModel` and `ChatClient` with tool activity stubs. The loop should be automatic.
6. **Configure `application.properties`** — Temporal connection, task queue, Spring AI / OpenAI settings
7. **Wire up `TemporalConfig`** or rely on auto-configuration — Register workflow and activity implementations. Determine how much the auto-starter handles vs what we configure manually.
8. **Create `Starter.java`** — Same pattern as demo1 (plain Java, starts workflow, waits for result)
9. **Create `WorkerApplication.java`** — Spring Boot main class
10. **Test** — Same prompts as demo1, verify same behavior
11. **Write README and CLAUDE.md** — Document the demo and design decisions

## Open questions

1. **Auto-configuration scope** — How much of the worker/client setup does `temporal-spring-boot-starter` handle? Do we still need any manual config for registering our workflow and activities? Need to check the auto-configuration.
2. **Tool loop visibility** — In demo1, each iteration of the loop is visible in the workflow history. With the library handling the loop inside `ActivityChatModel`, will tool executions still appear as separate activity entries? (They should, since tools are activity stubs.)
3. **HttpHelper reuse** — Can we share the `HttpHelper` class from demo1, or should demo2 have its own copy? For a workshop, a copy per demo is simpler (self-contained).
4. **Library stability** — The library is experimental with no tests and many TODOs. We may hit issues that require workarounds.
