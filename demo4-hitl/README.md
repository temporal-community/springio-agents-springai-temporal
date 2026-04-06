# Demo 4 - Human in the Loop

Extends demo3 by adding the ability for the agent to ask the user questions mid-execution. The LLM decides when it needs clarification and uses an `askUser` tool to pause the workflow, get input from the user, and continue.

## What's different from demo3

Demo3's agent runs to completion without any user interaction after the initial goal. Demo4 adds a human-in-the-loop pattern: the agent can pause, ask the user a question, wait for their response, and continue with that information.

### How it works

The HITL mechanism uses three Temporal primitives — a deterministic tool, a signal, and queries:

**1. The `askUser` tool** (`AskUserTool`) is a `@DeterministicTool` — it runs directly in the workflow, not as an activity. When the LLM decides it needs more information, it calls this tool with a question. The tool:
- Sets `inputNeeded = true` and `question = "Which race are you attending?"`
- Blocks on `Workflow.await(() -> !inputNeeded)` — this durably suspends the workflow

**2. The signal** (`provideUserInput`) delivers the user's response. When the signal arrives:
- Sets `userInput = "Monaco"` and `inputNeeded = false`
- The `Workflow.await()` unblocks, the tool returns the user's input, and the agentic loop continues

**3. The queries** (`isInputNeeded`, `getPendingQuestion`) let the Starter poll the workflow to detect when the agent needs input and what question to ask.

### What happens while waiting for the user

When `Workflow.await()` suspends the workflow, **no worker resources are consumed**. The workflow task completes, the worker is free to handle other work, and the workflow state is held durably by the Temporal server. The worker could even restart — when the signal arrives, the server schedules a new workflow task, the workflow replays, and execution resumes exactly where it left off.

### The Starter

The Starter starts the workflow asynchronously, then enters a polling loop:
- Every 2 seconds, it queries the workflow to check if input is needed
- If yes, it prints the agent's question, reads the user's response from stdin, and sends it as a signal
- A background thread waits for the workflow result; when it completes, the main loop exits and prints the result

## Prerequisites

- **Java 21+**
- **Maven 3.9+**
- **Node.js 18+** (for the F1 MCP server)
- **Python 3.8+** and **uv** (for the F1 MCP server's FastF1 library)
- **Temporal CLI** — `brew install temporal` (macOS) or see [Temporal CLI docs](https://docs.temporal.io/cli)
- **OpenAI API key** — set as `OPENAI_API_KEY` environment variable

From the project root, run both setup scripts:

```bash
# Install pre-built temporal-spring-ai library jars
./scripts/install-libs.sh

# Clone, patch, and build the F1 MCP server (also writes mcp-servers.json)
./scripts/setup-f1-server.sh
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
mvn -pl demo4-hitl spring-boot:run
```

### 4. Start a workflow

In a second terminal:

```bash
mvn -pl demo4-hitl compile exec:java -Dexec.args="Should I bring rain gear to the F1 race?"
```

The agent will ask which race you mean, wait for your response, then look up the weather.

### Example prompts

```bash
# Ambiguous — agent will ask which race
mvn -pl demo4-hitl compile exec:java -Dexec.args="Should I bring rain gear to the F1 race?"

# Ambiguous location
mvn -pl demo4-hitl compile exec:java -Dexec.args="What's the weather in Portland?"

# Clear enough — agent may not need to ask
mvn -pl demo4-hitl compile exec:java -Dexec.args="What is the weather at the Monaco Grand Prix this year?"

# Multi-step with clarification
mvn -pl demo4-hitl compile exec:java -Dexec.args="Help me plan what to pack for the race"
```

### Observing the workflow

View running workflows in the Temporal Web UI at [http://localhost:8233](http://localhost:8233). You'll see:
- **ChatModel activities** — LLM calls
- **Tool activities** — Weather tool executions
- **MCP-Client activities** — F1 MCP tool calls
- **Signal events** — User input delivered to the workflow
- The workflow will show as "Running" while waiting for user input, consuming no worker resources
