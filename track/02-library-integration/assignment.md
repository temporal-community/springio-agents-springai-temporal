---
slug: library-integration
id: epe7a2dw6dzb
type: challenge
title: 'Demo 2: The temporal-spring-ai Library'
teaser: The same durable agentic loop, collapsed to a single line — the library handles
  the loop, Temporal handles durability.
notes:
- type: text
  contents: |-
    **Demo 2: temporal-spring-ai Integration**

    In Demo 1, we wrote the agentic loop explicitly: a `for` loop in the workflow,
    manual tool dispatch, custom serializable DTOs so data could cross activity boundaries.

    The `temporal-spring-ai` library eliminates all of that boilerplate.
    The developer writes standard Spring AI `ChatClient` code.
    Under the hood, the library routes every LLM call and every tool execution
    through Temporal activities — without the developer doing anything extra.

    The workflow's `run` method goes from ~40 lines to one:

    ```java
    return chatClient.prompt().user(goal).call().content();
    ```

    Same durability guarantees. Same visibility in the Temporal UI.
    Hit **Start** when you're ready.
tabs:
- id: xg0m8j2fqqzy
  title: Terminal 1 - Worker
  type: terminal
  hostname: workshop-host
  workdir: /workspace/workshop
- id: lemxrjc7o1qg
  title: Terminal 2 - Starter
  type: terminal
  hostname: workshop-host
  workdir: /workspace/workshop
- id: dvjeiswwmoxh
  title: VS Code
  type: service
  hostname: workshop-host
  path: ?folder=/workspace/workshop/demo2-springai-temporal-integration
  port: 8443
- id: 7wdn8ehk7zsv
  title: Temporal Web UI
  type: service
  hostname: workshop-host
  path: /
  port: 8080
- id: fubh1dvhfz9e
  title: Network Control Panel
  type: service
  hostname: workshop-host
  path: /
  port: 5000
difficulty: basic
timelimit: 1800
enhanced_loading: null
---

## Demo 2: The temporal-spring-ai Library

### What changed

Open **VS Code** and compare `demo2-springai-temporal-integration/` with demo1.

Key differences:

- **`AgentWorkflowImpl.java`** — the `run` method is one line. `@WorkflowInit` creates the activity stubs and builds the `TemporalChatClient`; `run` just calls it.
- **No `LlmActivitiesImpl.java`** — the library provides `ActivityChatModel` and `ChatModelActivity`, which handle the LLM activity dispatch.
- **`tools/ToolActivities.java`** — tools are now Temporal `@ActivityInterface` methods that also carry `@Tool` annotations. The library's `TemporalToolUtil` only accepts activity stubs, local activity stubs, or `@DeterministicTool` classes — plain Java objects are rejected.
- **`application.yaml`** — the `temporal-spring-boot-starter` auto-configures the Temporal client and worker from YAML. No manual `TemporalConfig` needed.

**Trade-off:** In demo1, the `tools/` directory contains plain Java with no Temporal imports — tools are portable. In demo2, tools are Temporal activity interfaces. You get a simpler workflow, but tools are coupled to Temporal.

---

### Run the worker

In **Terminal 1**:

```
cd demo2-springai-temporal-integration
mvn spring-boot:run
```

---

### Start a workflow

In **Terminal 2**:

```
cd demo2-springai-temporal-integration
mvn compile exec:java -Dexec.args="What is the weather in Barcelona?"
```

---

### Compare in the Temporal Web UI

Open the **Temporal Web UI** tab. The workflow history looks identical to demo1: LLM calls and tool executions each appear as separate activities. The library didn't change the durability model — it just removed the boilerplate needed to achieve it.

---

### Try disrupting a service

Use the **Network Control Panel** to toggle off **Geolocation** or **Weather**, then run:

```
mvn compile exec:java -Dexec.args="What is the weather where I am?"
```

Watch the Temporal UI: the disabled activity fails and retries. Toggle it back on — the workflow continues from where it paused.
