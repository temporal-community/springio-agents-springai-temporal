# Spring AI + Temporal Agent Workshop

A series of demos showing how to build AI agents using Spring AI and Temporal. Each demo builds on the previous one, progressively adding capabilities. See individual demo READMEs for prerequisites and setup instructions.

## Demos

| Demo | Description |
|------|-------------|
| [demo1-agentic-loop](demo1-agentic-loop/) | An agentic loop as a Temporal workflow — calls an LLM with tools and loops until the goal is achieved |
| [demo2-springai-temporal-integration](demo2-springai-temporal-integration/) | Same agentic loop using the [temporal-spring-ai](https://github.com/temporal-community/temporal-spring-ai) library — Spring AI handles the loop, Temporal provides durability |
| [demo3-mcp](demo3-mcp/) | Adds F1 race data via an MCP server — the agent chains F1 schedule tools with weather tools |
| [demo4-hitl](demo4-hitl/) | Human-in-the-loop — the agent can ask the user questions mid-execution via signals and queries |

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
