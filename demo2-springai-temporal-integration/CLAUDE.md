# Demo 2 - Spring AI Temporal Integration

## Architecture

This demo uses the `temporal-spring-ai` library to implement the same agentic loop as demo1. The library replaces our manual infrastructure with transparent Temporal-backed Spring AI abstractions.

### What the library provides (that we built manually in demo1)

| Concern | Demo1 (manual) | Demo2 (library) |
|---|---|---|
| LLM activity | `LlmActivities` / `LlmActivitiesImpl` | `ChatModelActivity` / `ChatModelActivityImpl` |
| Tool dispatch | `DynamicToolActivity` + `ToolRegistry` | Activity stubs with `@Tool` + `@ActivityMethod` |
| Serialization DTOs | `LlmMessage`, `LlmResponse`, `ToolCallInfo` | `ChatModelTypes` records |
| Agentic loop | Manual for-loop in workflow | Spring AI `ToolCallingManager` via `ActivityChatModel` |
| Worker config | Manual `TemporalConfig` bean | `temporal-spring-boot-starter` auto-config from `application.yaml` |

### Key design decisions

**Dual-annotated tool interface.** `ToolActivities` has both `@ActivityMethod` and `@Tool` on each method. This is the library's pattern — one interface serves as both the Temporal activity contract and the Spring AI tool definition. The library's `ActivityToolUtil` extracts `@Tool` schemas from activity stubs via reflection.

**No manual agentic loop.** `ActivityChatModel` implements Spring AI's `ChatModel` interface and handles the tool calling loop internally. When it gets tool calls back from the LLM, it uses `ToolCallingManager` to execute them (which invokes the activity stubs), then calls the LLM again. The workflow just calls `chatClient.prompt().user(goal).call().content()`.

**Auto-configured worker.** The `temporal-spring-boot-starter` reads `application.yaml` to set up the Temporal client, worker factory, and worker. Workflow classes and activity beans are declared in config, not in Java.

**`@Component` on ToolActivitiesImpl.** The starter discovers activity implementations by Spring bean name. `@Component("toolActivitiesImpl")` matches the `activity-beans` entry in `application.yaml`. The `chatModelActivity` bean is auto-registered by `ChatModelActivityBeanRegistrar`.

**`@WorkflowInit` on constructor.** The workflow creates activity stubs and builds the `TemporalChatClient` in its constructor, annotated with `@WorkflowInit`. This ensures the chat client is ready before `run()` is called.

**`scanBasePackages = "io.temporal.ai"` on WorkerApplication.** The library's `ChatModelActivityBeanRegistrar` lives in `io.temporal.ai.chat.model`, outside our default `io.temporal.ai.workshop` scan. Widening the scan to `io.temporal.ai` covers both the library's beans and our own.

### Library dependency

The `temporal-spring-ai` library is not published to Maven Central. It's a monolithic project that bundles library code with a sample app and optional features (vector store, MCP, Redis). We build a **core-only jar** by stripping out the optional packages (`vectorstore`, `mcp`, `chattools`, `workflows`, and the sample main class). This avoids `ClassNotFoundException` for optional dependencies we don't use (e.g., `VectorStore`). See README for the exact build steps.

The library uses Spring Boot 3.5.3, Spring AI 1.0.1, and Temporal SDK 1.31.0. This demo overrides the parent POM versions to match.

**`@WorkflowInit` constructor must match `@WorkflowMethod` parameters.** Temporal requires the `@WorkflowInit` constructor to have the same parameter list as the `@WorkflowMethod`. Since `run(String goal)` takes a String, the constructor is `AgentWorkflowImpl(String goal)` even though the goal isn't used during construction.

## File layout

```
io/temporal/ai/workshop/
├── AgentWorkflow.java           Workflow interface
├── AgentWorkflowImpl.java       One-line run() method using TemporalChatClient
├── Starter.java                 Plain Java client
├── WorkerApplication.java       Spring Boot entry point
└── tools/
    ├── HttpHelper.java           Shared HTTP GET utility
    ├── ToolActivities.java       @ActivityInterface + @Tool dual-annotated interface
    └── ToolActivitiesImpl.java   Activity implementation with HTTP calls
```

## Task queue

Uses `agent-task-queue`, configured in `application.yaml` under `spring.temporal.workers[0].task-queue`.
