# Spring AI + Temporal Agent Workshop

A series of demos showing how to build AI agents using Spring AI and Temporal. Each demo builds on the previous one, progressively adding capabilities. See individual demo READMEs for prerequisites and setup instructions.

## Demos

| Demo | Description |
|------|-------------|
| [demo1-agentic-loop](demo1-agentic-loop/) | An agentic loop as a Temporal workflow — calls an LLM with tools and loops until the goal is achieved |
| [demo2-springai-temporal-integration](demo2-springai-temporal-integration/) | Same agentic loop using the [temporal-spring-ai](https://github.com/temporal-community/temporal-spring-ai) library — Spring AI handles the loop, Temporal provides durability |
| [demo3-mcp](demo3-mcp/) | Adds F1 race data via an MCP server — the agent chains F1 schedule tools with weather tools |
| [demo4-hitl](demo4-hitl/) | Human-in-the-loop — the agent can ask the user questions mid-execution via signals and queries |

## Running the workshop

**At the live Spring I/O workshop**, sandboxed environments are provided via Instruqt — all dependencies are pre-installed. You only need to bring your OpenAI API key.

**To run independently** (outside the live workshop), you need:

- Java 21+
- Maven 3.9+
- Temporal CLI (`brew install temporal` on macOS, or see [Temporal CLI docs](https://docs.temporal.io/cli))
- An `OPENAI_API_KEY` environment variable
- For demos 3 and 4: Node.js 18+, Python 3.8+, and `uv`

One-time setup before running demo 2, 3, or 4:

```bash
./scripts/install-libs.sh
```

For demos 3 and 4 only:

```bash
./scripts/setup-f1-server.sh
```

### Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Workflow starts but never completes | Temporal server not running | Run `temporal server start-dev` in a separate terminal |
| `401 Unauthorized` from OpenAI | Missing or invalid API key | `export OPENAI_API_KEY=sk-...` |
| Worker starts but exits immediately | Build error or missing dependency | Run `mvn -pl <demo> compile` and check for errors |
| Demo 3/4 worker fails on startup | F1 MCP server not built | Run `./scripts/setup-f1-server.sh` |
| `Could not find artifact io.temporal.ai:temporal-spring-ai` | Library jar not installed | Run `./scripts/install-libs.sh` |
| Port 7233 already in use | Another Temporal server running | Stop it or check with `lsof -i :7233` |

## Building

Build all demos from the root:

```bash
mvn compile
```

Or build a specific demo:

```bash
mvn -pl demo1-agentic-loop compile
```

---

## Maintainer notes: rebuilding the temporal-spring-ai jar

The jar in `lib/` is pre-built so workshop participants don't need to clone and build the [temporal-spring-ai](https://github.com/temporal-community/temporal-spring-ai) library themselves. **You do not need to follow these steps to use the demos** — just run `./scripts/install-libs.sh`.

If you need to rebuild the jar (e.g., after the library is updated), here's how it was created. The library is a monolithic Gradle project that bundles library code with a sample app and optional features (vector store, Redis). We build the full project, then strip out the packages we don't need to avoid pulling in optional dependencies.

### Build the library

```bash
cd /path/to/temporal-spring-ai
./gradlew build -x test
```

This produces `build/libs/springAI-0.0.1-SNAPSHOT-plain.jar` (the plain jar without Spring Boot's fat-jar packaging).

### Repackage as core + MCP jar

Strips out the sample app, vector store, and sample tools. Keeps the core integration and MCP support:

```bash
mkdir -p build/lib-repack && cd build/lib-repack
jar xf ../libs/springAI-0.0.1-SNAPSHOT-plain.jar
rm -rf io/temporal/ai/chattools io/temporal/ai/workflows io/temporal/ai/vectorstore io/temporal/ai/TemporalSpringAiChat.class META-INF
jar cf temporal-spring-ai-0.0.1-SNAPSHOT.jar io
```

Copy to `lib/temporal-spring-ai-0.0.1-SNAPSHOT.jar`. A single jar is used by all demos (demo2, demo3, demo4). The MCP classes are included but inert unless MCP client dependencies and config are present.

### What's included

| Package | Included | Purpose |
|---|---|---|
| `io.temporal.ai.chat.client` | Yes | `TemporalChatClient`, `TemporalToolUtil` |
| `io.temporal.ai.chat.model` | Yes | `ActivityChatModel`, `ChatModelActivity`, `ChatModelTypes` |
| `io.temporal.ai.tool` | Yes | `ActivityToolCallback`, `DeterministicTool`, local activity wrappers |
| `io.temporal.ai.reflection` | Yes | Stub type detection utilities |
| `io.temporal.ai.mcp` | Yes | `McpToolCallback`, `ActivityMcpClient`, `McpClientActivity` |
| `io.temporal.ai.vectorstore` | No | Not needed — would require Redis dependencies |
| `io.temporal.ai.chattools` | No | Sample tools from the library's demo app |
| `io.temporal.ai.workflows` | No | Sample workflow from the library's demo app |
