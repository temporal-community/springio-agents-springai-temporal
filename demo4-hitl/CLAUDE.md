# Demo 4 - Human in the Loop

## Architecture

This demo builds on demo3 (temporal-spring-ai + MCP) and adds a human-in-the-loop pattern. The agent can ask the user questions mid-execution using a `@DeterministicTool` that blocks on `Workflow.await()`.

### Key design decisions

**`AskUserTool` is a `@DeterministicTool`, not an activity.** It runs directly in the workflow thread. Setting state variables and calling `Workflow.await()` are deterministic operations. Making it an activity would be wrong — we need it to block the workflow until user input arrives via signal.

**State lives in `AskUserTool`, not the workflow.** The `inputNeeded`, `question`, and `userInput` fields are on the tool instance. The workflow delegates signal/query handlers to the tool. This keeps the tool self-contained.

**ChatClient built in `run()`, not the constructor.** `McpToolCallback.fromMcpTools()` calls `listTools()` which executes an activity. In `@WorkflowInit`, this blocks the constructor and delays signal/query handler registration, causing `Unknown query type` errors. Moving it to `run()` ensures handlers are registered immediately at workflow start.

**Starter uses background thread for result.** `untypedStub.getResult(String.class)` is blocking. We run it on a `CompletableFuture.supplyAsync()` thread so the main thread can poll for questions. `resultFuture.isDone()` on the main thread detects completion.

**`WorkflowQueryException` handling.** During the first few seconds after workflow start, queries may fail because the workflow is still initializing (MCP tool discovery activity). The Starter catches `WorkflowQueryException` and silently retries on the next poll.

**`Workflow.await()` is durable.** While waiting for user input, the workflow consumes no worker resources. The workflow task completes, the worker slot is freed. The workflow state is held by the Temporal server. When a signal arrives, a new workflow task is scheduled and execution resumes.

## File layout

```
io/temporal/ai/workshop/
├── AgentWorkflow.java           Workflow interface — adds signal + query methods
├── AgentWorkflowImpl.java       Stubs in constructor, ChatClient in run()
├── AskUserTool.java             @DeterministicTool — askUser blocks on Workflow.await()
├── Starter.java                 Async start + poll loop + background result thread
├── WorkerApplication.java       Spring Boot entry point (same as demo3)
└── tools/
    ├── HttpHelper.java           Shared HTTP GET utility (same as demo3)
    ├── ToolActivities.java       Weather tool activity interface (same as demo3)
    └── ToolActivitiesImpl.java   Weather tool implementation (same as demo3)
```

### What changed from demo3

| File | Change |
|---|---|
| `AgentWorkflow.java` | Added `@SignalMethod provideUserInput`, `@QueryMethod isInputNeeded`, `@QueryMethod getPendingQuestion` |
| `AgentWorkflowImpl.java` | Moved ChatClient construction from `@WorkflowInit` to `run()`. Added `AskUserTool` to `defaultTools()`. Delegates signal/query to tool. |
| `AskUserTool.java` | New — `@DeterministicTool` with `askUser()` that blocks on `Workflow.await()` |
| `Starter.java` | Rewritten — async workflow start, background result thread, polling loop for HITL |
