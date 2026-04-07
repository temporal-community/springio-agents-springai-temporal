---
slug: human-in-the-loop
id: vtmkwcoaaygm
type: challenge
title: 'Demo 4: Human in the Loop'
teaser: The agent pauses mid-execution to ask you a question — durably waiting for
  a signal before it continues.
notes:
- type: text
  contents: |-
    **Demo 4: Human in the Loop**

    What if the agent doesn't have enough information to complete the task?

    In a stateless system, you'd need to restart the entire conversation.
    With Temporal, the agent can pause, ask you a question, and wait —
    consuming zero worker resources while it does — then resume exactly
    where it left off when you respond.

    The mechanism uses three Temporal primitives:

    - **A deterministic tool** (`askUser`) runs inside the workflow, sets a flag,
      and calls `Workflow.await()` — durably suspending execution
    - **A signal** (`provideUserInput`) delivers your response, unblocking the await
    - **Queries** (`isInputNeeded`, `getPendingQuestion`) let the Starter poll
      for pending questions without disturbing the workflow

    The Starter polls every 2 seconds, prints the agent's question when one appears,
    reads your response from stdin, and sends it as a signal.

    Hit **Start** when you're ready.
tabs:
- id: 99p6hpyoszqn
  title: Terminal 1 - Worker
  type: terminal
  hostname: workshop-host
  workdir: /workspace/workshop
- id: gmtgeq1y1z0v
  title: Terminal 2 - Starter
  type: terminal
  hostname: workshop-host
  workdir: /workspace/workshop
- id: 35mcppfz1be6
  title: VS Code
  type: service
  hostname: workshop-host
  path: ?folder=/workspace/workshop/demo4-hitl&openFile=/workspace/workshop/demo4-hitl/src/main/java/io/temporal/ai/workshop/AskUserTool.java
  port: 8443
- id: exz7w4vfyo29
  title: Temporal Web UI
  type: service
  hostname: workshop-host
  path: /
  port: 8080
- id: sqmspkxhukys
  title: Network Control Panel
  type: service
  hostname: workshop-host
  path: /
  port: 5000
difficulty: basic
timelimit: 1800
enhanced_loading: null
---

## Demo 4: Human in the Loop

### What to look at first

**VS Code** opens to `AskUserTool.java`. Read it before starting the demo — it's short and the whole mechanism is in here.

The key lines:

```java
this.inputNeeded = true;
Workflow.await(() -> !inputNeeded);   // durable suspend
return userInput;                     // resumed by signal
```

`@DeterministicTool` tells the `temporal-spring-ai` library to run this tool directly in the workflow thread (not as an activity), which is how `Workflow.await()` is legal here.

Then look at `AgentWorkflowImpl.java` — the signal handler and query handlers at the bottom are the other half of the contract:

```java
public void provideUserInput(String input) {
    askUserTool.provideInput(input);   // sets userInput, clears inputNeeded flag
}
```

---

### Run the worker

In **Terminal 1**:

```
cd demo4-hitl
mvn spring-boot:run
```

---

### Start an ambiguous workflow

In **Terminal 2**, give the agent something it will need to ask about:

```
cd demo4-hitl
mvn compile exec:java -Dexec.args="Should I bring rain gear to the F1 race?"
```

The agent doesn't know which race you mean. It will pause and ask. When it does:

1. The Starter prints the agent's question in your terminal
2. Type your answer and press Enter
3. The Starter sends it as a signal; the workflow resumes

---

### Watch it in the Temporal Web UI

Open the **Temporal Web UI** tab and find the running workflow. While the agent is waiting for your input:

- The workflow shows status **Running**
- No worker threads are consumed — the workflow task has completed
- When the signal arrives, a new workflow task is scheduled and execution continues

You'll see the `provideUserInput` signal event appear in the history timeline at the moment you respond.

---

### More prompts to try

```
# Ambiguous location — agent will ask which Portland
mvn compile exec:java -Dexec.args="What's the weather in Portland?"

# Multi-step with clarification
mvn compile exec:java -Dexec.args="Help me plan what to pack for the race"

# Clear enough that the agent may not ask at all
mvn compile exec:java -Dexec.args="What is the weather at the Monaco Grand Prix this year?"
```

---

### Kill the worker while it's waiting

Start an ambiguous prompt, but before you answer the agent's question, kill the worker (Ctrl+C in Terminal 1). Restart it. The workflow is still waiting — it never lost state. Answer the question; the workflow resumes and completes.
