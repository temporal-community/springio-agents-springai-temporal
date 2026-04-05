# Spring AI + Temporal Agent Workshop

A series of demos showing how to build AI agents using Spring AI and Temporal. Each demo builds on the previous one, progressively adding capabilities.

## Prerequisites

- **Java 21+**
- **Maven 3.9+**
- **Temporal CLI** - install with `brew install temporal` (macOS) or see [Temporal CLI docs](https://docs.temporal.io/cli)
- **OpenAI API key** - set as `OPENAI_API_KEY` environment variable

## Demos

| Demo | Description |
|------|-------------|
| [demo1-agentic-loop](demo1-agentic-loop/) | An agentic loop as a Temporal workflow — calls an LLM with tools and loops until the goal is achieved |
| [demo2-springai-temporal-integration](demo2-springai-temporal-integration/) | Same agentic loop using the [temporal-spring-ai](https://github.com/temporal-community/temporal-spring-ai) library — Spring AI handles the loop, Temporal provides durability |

## Building

Build all demos from the root:

```bash
mvn compile
```

Or build a specific demo:

```bash
mvn -pl demo1-agentic-loop compile
```
