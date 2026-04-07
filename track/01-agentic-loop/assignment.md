---
slug: agentic-loop
id: ikiozdzsqoz2
type: challenge
title: 'Demo 1: The Agentic Loop'
teaser: See how an AI agentic loop becomes a durable Temporal workflow — every LLM
  call and tool execution is a visible, retryable activity.
notes:
- type: text
  contents: |-
    **Demo 1: The Agentic Loop from Scratch**

    An AI agent is a loop: ask the LLM what to do, execute what it asks for,
    feed the result back, repeat until done.

    The fragile version of this lives in a single process: if it crashes mid-loop,
    the entire conversation is lost and the agent starts over.

    In this demo, we move each step of that loop into a Temporal workflow:
    - **LLM calls** become activities — non-deterministic I/O, tracked in history
    - **Tool executions** become activities — external HTTP calls, retried automatically on failure
    - **The loop state** lives in Temporal — a crash mid-loop replays from history and continues

    Hit **Start** when you're ready.
tabs:
- id: psvbfnfqdmza
  title: Terminal 1 - Worker
  type: terminal
  hostname: workshop-host
  workdir: /workspace/workshop
- id: f51q4emhj96t
  title: Terminal 2 - Starter
  type: terminal
  hostname: workshop-host
  workdir: /workspace/workshop
- id: knacglkx4ape
  title: VS Code
  type: service
  hostname: workshop-host
  path: ?folder=/workspace/workshop/demo1-agentic-loop
  port: 8443
- id: xxcmhunoiqs8
  title: Temporal Web UI
  type: service
  hostname: workshop-host
  path: /
  port: 8080
- id: jdezbgqex4nu
  title: Network Control Panel
  type: service
  hostname: workshop-host
  path: /
  port: 5000
difficulty: basic
timelimit: 1800
enhanced_loading: null
---

## Demo 1: The Agentic Loop

### What we're looking at

Open **VS Code** and navigate to `demo1-agentic-loop/src/main/java/io/temporal/ai/workshop/`.

The key files are:

- **`AgentWorkflowImpl.java`** — the agentic loop. An explicit `for` loop that calls the LLM activity, checks for tool calls, dispatches each one as a dynamic activity, and repeats until the model returns a text response.
- **`LlmActivitiesImpl.java`** — calls OpenAI via Spring AI's `ChatModel`. This is the only file that knows about Spring AI — the workflow has no Spring AI dependency.
- **`ToolRegistry.java`** — collects `@Tool`-annotated methods, generates LLM schemas and execution handlers from the same annotations. The workflow dispatches tools by name; it never knows which tools exist.
- **`tools/`** — plain Java classes with `@Tool` and `@ToolParam` annotations. No Temporal imports here.

---

### Run the worker

In **Terminal 1**, start the Spring Boot worker:

```
cd demo1-agentic-loop
mvn spring-boot:run
```

The worker registers the workflow and activities with Temporal and starts polling `agent-task-queue`. Leave this running.

---

### Start a workflow

In **Terminal 2**, start an agent:

```
cd demo1-agentic-loop
mvn compile exec:java -Dexec.args="What is the weather in Barcelona?"
```

---

### Watch it in the Temporal Web UI

Open the **Temporal Web UI** tab. Find the running workflow. Click into it and watch:

- Each LLM call appears as a `callLlm` activity
- Each tool execution appears as a separate activity (`getCoordinates`, `getWeather`, etc.)
- The input and output of every activity is recorded in the event history

**Try:** Kill the worker mid-execution (Ctrl+C in Terminal 1). Restart it. The workflow resumes exactly where it left off.

---

### Try the Network Control Panel

Open the **Network Control Panel** tab. Toggle **Weather** off while a workflow is running.

Watch what happens in the Temporal Web UI: the tool activity fails, Temporal retries it automatically. Toggle Weather back on — the next retry succeeds and the workflow continues.

---

### Try different prompts

```
mvn compile exec:java -Dexec.args="Compare the weather in Tokyo and Sydney right now"
```

```
mvn compile exec:java -Dexec.args="What is the weather where I am?"
```

The second prompt chains three tools: `getIpAddress` → `getLocationInfo` → `getWeather`.
