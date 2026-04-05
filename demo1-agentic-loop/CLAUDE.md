# Demo 1 - Agentic Loop

## Architecture

This is an agentic loop implemented as a Temporal workflow with Spring AI providing the LLM integration.

### Layer separation

The codebase has three distinct layers:

1. **Agent infrastructure** (`io.temporal.ai.workshop`) - The workflow, activities, DTOs, and tool dispatch mechanism. These are generic and tool-agnostic.
2. **Tool implementations** (`io.temporal.ai.workshop.tools`) - The actual tools (HTTP calls to external APIs). Only tool-specific code lives here.
3. **Spring Boot wiring** (`TemporalConfig`, `WorkerApplication`) - Connects everything together.

### Key design decisions

**Custom DTOs instead of Spring AI message types.** We use our own `LlmMessage`, `LlmResponse`, and `ToolCallInfo` records for data that crosses the Temporal workflow-activity boundary. Spring AI's message types (`AssistantMessage`, `ToolResponseMessage`, etc.) have complex internals that may not serialize cleanly through Temporal's Jackson-based `DataConverter`. The conversion between DTOs and Spring AI types happens entirely within `LlmActivitiesImpl`.

**LLM call is a Temporal activity.** The `ChatModel.call()` is non-deterministic I/O and must be an activity. Spring AI is only used inside `LlmActivitiesImpl` — the workflow has no Spring AI dependency.

**Tools are Temporal dynamic activities.** Tool execution uses Temporal's `DynamicActivity` and `Workflow.newUntypedActivityStub()`. The workflow forwards the LLM's tool name and raw JSON arguments directly — it has no knowledge of specific tools. This mirrors the pattern in the Python cookbook example (`agentic_loop_tool_call_openai_python`).

**`ToolRegistry` bridges Spring AI schemas and Temporal dispatch.** It accepts any objects with `@Tool` annotated methods, uses `ToolCallbacks.from()` to generate both the LLM schemas (sent to the model) and the execution handlers (called by the dynamic activity). The `@Tool` annotations are the single source of truth — schema and implementation live in the same method.

**`DynamicToolActivity` is completely generic.** It looks up a handler by name from the `ToolRegistry` and calls `callback.call(arguments)`. Adding or removing tools requires zero changes to the workflow or dynamic activity.

**Separate worker and starter.** `WorkerApplication` is a Spring Boot app (needs `ChatModel` auto-configuration) that runs the Temporal worker. `Starter` is plain Java that creates a `WorkflowClient`, starts the workflow, and exits. The worker needs Spring; the starter does not.

### Adding a new tool

1. Create a class in `tools/` with `@Tool` and `@ToolParam` annotated methods
2. Add it to the `ToolRegistry` constructor in `TemporalConfig`

Nothing else changes — the workflow, dynamic activity, and LLM activity are all tool-agnostic.

### Swapping the tool set

Replace the `ToolRegistry` constructor args in `TemporalConfig`:
```java
return new ToolRegistry(new MyNewTools());
```

### Provider-specific code

The only OpenAI-specific code is `OpenAiChatOptions.builder()` in `LlmActivitiesImpl`. Switching to another provider (Anthropic, Gemini, etc.) means changing that builder, the Maven dependency, and `application.properties`. Everything else is provider-agnostic through Spring AI's abstractions.

## File layout

```
io/temporal/ai/workshop/
├── AgentWorkflow.java           Workflow interface
├── AgentWorkflowImpl.java       Agentic loop - calls LLM activity, dispatches tools via untyped stub
├── DynamicToolActivity.java     Generic dynamic activity - dispatches to ToolRegistry by name
├── LlmActivities.java           Activity interface for LLM calls
├── LlmActivitiesImpl.java       Calls ChatModel, converts DTOs <-> Spring AI types
├── Starter.java                 Plain Java client - starts a workflow and waits for result
├── TemporalConfig.java          Spring config - creates ToolRegistry, worker, client
├── ToolRegistry.java            Collects @Tool objects, provides schemas and handlers
├── WorkerApplication.java       Spring Boot entry point for the worker
├── model/
│   ├── LlmMessage.java          Serializable conversation message DTO
│   ├── LlmResponse.java         Serializable LLM response DTO
│   └── ToolCallInfo.java        Serializable tool call DTO
└── tools/
    ├── HttpHelper.java           Shared HTTP GET utility
    ├── LocationTools.java        IP lookup, geolocation, geocoding tools
    └── WeatherTools.java         Weather forecast tool
```

## Task queue

All workflow and activity tasks use the `agent-task-queue` task queue, defined in `TemporalConfig.TASK_QUEUE`.
