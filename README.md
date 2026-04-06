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

## Maintainer notes: rebuilding the temporal-spring-ai jars

The jars in `lib/` are pre-built so workshop participants don't need to clone and build the [temporal-spring-ai](https://github.com/temporal-community/temporal-spring-ai) library themselves. **You do not need to follow these steps to use the demos** — just run `./scripts/install-libs.sh`.

If you need to rebuild the jars (e.g., after the library is updated), here's how they were created. The library is a monolithic Gradle project that bundles library code with a sample app and optional features (vector store, MCP, Redis). We build the full project, then strip out the packages we don't need to avoid pulling in optional dependencies.

### Build the library

```bash
cd /path/to/temporal-spring-ai
./gradlew build -x test
```

This produces `build/libs/springAI-0.0.1-SNAPSHOT-plain.jar` (the plain jar without Spring Boot's fat-jar packaging).

### 0.0.1-SNAPSHOT (core only, used by demo2)

Strips out the sample app, vector store, MCP, and sample tools:

```bash
mkdir -p build/lib-only && cd build/lib-only
jar xf ../libs/springAI-0.0.1-SNAPSHOT-plain.jar
rm -rf io/temporal/ai/chattools io/temporal/ai/workflows io/temporal/ai/vectorstore io/temporal/ai/mcp io/temporal/ai/TemporalSpringAiChat.class META-INF
jar cf temporal-spring-ai-core.jar io
```

Copy to `lib/temporal-spring-ai-0.0.1-SNAPSHOT.jar`.

### 0.0.2-SNAPSHOT (core + MCP, used by demo3)

Same as above but keeps the `io.temporal.ai.mcp` package:

```bash
mkdir -p build/lib-mcp && cd build/lib-mcp
jar xf ../libs/springAI-0.0.1-SNAPSHOT-plain.jar
rm -rf io/temporal/ai/chattools io/temporal/ai/workflows io/temporal/ai/vectorstore io/temporal/ai/TemporalSpringAiChat.class META-INF
jar cf temporal-spring-ai-mcp.jar io
```

Copy to `lib/temporal-spring-ai-0.0.2-SNAPSHOT.jar`.

### What's in each jar

| Package | 0.0.1 (core) | 0.0.2 (core + MCP) |
|---|---|---|
| `io.temporal.ai.chat.client` | Yes | Yes |
| `io.temporal.ai.chat.model` | Yes | Yes |
| `io.temporal.ai.tool` | Yes | Yes |
| `io.temporal.ai.reflection` | Yes | Yes |
| `io.temporal.ai.mcp` | No | Yes |
| `io.temporal.ai.vectorstore` | No | No |
| `io.temporal.ai.chattools` | No | No |
| `io.temporal.ai.workflows` | No | No |
