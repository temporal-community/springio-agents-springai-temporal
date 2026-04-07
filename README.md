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

## Running locally (non-Instruqt)

### Prerequisites

- Java 21+
- Maven 3.9+
- Temporal CLI (`brew install temporal` on macOS, or see [Temporal CLI docs](https://docs.temporal.io/cli))
- OpenAI API key: `export OPENAI_API_KEY=sk-...`
- For demo3 and demo4: Node.js 18+, Python 3.8+, and `uv` (see [uv install](https://docs.astral.sh/uv/getting-started/installation/))

### One-time setup

```bash
# Install the temporal-spring-ai jar into your local Maven repository
./scripts/install-libs.sh

# For demo3 and demo4: clone, patch, and build the F1 MCP server
./scripts/setup-f1-server.sh
```

### Starting the Temporal dev server

```bash
temporal server start-dev
```

Then follow the individual demo READMEs for worker and starter commands.

---

## Instruqt track

The `track/` directory contains an [Instruqt](https://instruqt.com) hands-on lab track for the Spring I/O 2025 workshop. The track provides a pre-configured VM environment so participants can run the demos without installing anything locally.

### Track structure

```
track/
├── track.yml                        # Track metadata (slug, title, tags, owner)
├── config.yml                       # Top-level VM specification
├── track_scripts/
│   └── setup-workshop-host          # Full VM bootstrap — runs once at track creation
├── 00-setup/                        # Challenge 0: environment check + API key
├── 01-agentic-loop/                 # Challenge 1: demo1
├── 02-library-integration/          # Challenge 2: demo2
├── 03-mcp/                          # Challenge 3: demo3
└── 04-human-in-the-loop/            # Challenge 4: demo4
```

Each challenge directory contains: `assignment.md` (the tab content participants see), `config.yml` (per-challenge VM spec), and `setup-workshop-host`, `check-workshop-host`, `cleanup-workshop-host` scripts.

### What the VM setup script installs

`track_scripts/setup-workshop-host` runs once when the Instruqt environment is created. It installs and configures:

- **Java 21** (Eclipse Temurin), **Maven 3.9**
- **Node.js 18** (required for the F1 MCP server)
- **uv** (Python package installer, used to build the FastF1 venv)
- **Temporal CLI** and a **Temporal dev server** running as a systemd service on port 8080 (UI) / 7233 (gRPC)
- **The workshop repo** cloned to `/workspace/workshop`, with the `temporal-spring-ai` jar installed and all Maven dependencies pre-warmed
- **The F1 MCP server** cloned to `/workspace/f1-mcp-server`, patched, built (Node + Python), and wired into `mcp-servers.json` for demo3 and demo4
- **mitmproxy** running as a systemd service on port 8888, with its CA cert trusted by both the system and the JVM truststore
- **A network control panel** (Flask app on port 5000) that lets participants toggle individual external services on and off via a browser UI — used to demonstrate Temporal's retry behavior by disrupting live API calls mid-workflow
- **code-server** (VS Code in browser) on port 8443, pointed at `/workspace/workshop`

### Network control panel and proxy

The workshop proxy (mitmproxy) sits between the VM and all external APIs. Every outbound HTTP/HTTPS call from Java, Python (FastF1), and Node.js routes through it. The control panel UI at port 5000 provides toggle switches for:

| Toggle | Intercepts |
|--------|------------|
| OpenAI | `api.openai.com` |
| Weather | `api.open-meteo.com` |
| Geolocation | `ip-api.com` |
| IP Info | `icanhazip.com` |
| F1 Data | `ergast.com`, `api.openf1.org`, `livetiming.formula1.com` |
| Kill Switch | All external traffic |

Toggling a service off returns HTTP 503 to the caller. Temporal retries the failed activity automatically. Toggling it back on allows the next retry to succeed. This makes retry behavior concrete and visible in the Temporal Web UI without requiring any artificial `if/then` failure injection in the code.

**How proxy routing is achieved per runtime:**
- **Java:** `JAVA_TOOL_OPTIONS` sets `-Dhttp.proxyHost` / `-Dhttps.proxyHost` system properties
- **Python (FastF1):** `HTTP_PROXY` / `HTTPS_PROXY` environment variables, respected natively by the `requests` library that FastF1 uses
- **Node.js:** `HTTP_PROXY` / `HTTPS_PROXY` environment variables plus `NODE_EXTRA_CA_CERTS` pointing to the mitmproxy CA cert (Node does not use the system truststore)

### Tabs available in each challenge

| Tab | Port | Purpose |
|-----|------|---------|
| Terminal(s) | — | Run worker and starter commands |
| VS Code | 8443 | Browse and read the demo source code |
| Temporal Web UI | 8080 | Watch workflow execution and activity history live |
| Network Control Panel | 5000 | Toggle external services to demonstrate retry behavior |

### Instruqt CLI workflow

```bash
# Authenticate
instruqt auth login

# Pull latest state from the platform (do this before any local edits)
instruqt track pull --track temporal/springio-agents-springai-temporal

# Push local changes
instruqt track push --track temporal/springio-agents-springai-temporal

# Always pull after push to populate id: fields that Instruqt generates
instruqt track pull --track temporal/springio-agents-springai-temporal
```

**Important:** do not edit the track via the Instruqt UI while also editing locally — this causes `.remote` file conflicts. Use the CLI exclusively.

### Creating the track for the first time

The track does not yet exist on Instruqt. Create it before the first push:

```bash
instruqt track create --title "Building Durable AI Agents with Spring AI + Temporal" --org temporal
```

Then push:

```bash
instruqt track push --track temporal/springio-agents-springai-temporal
instruqt track pull --track temporal/springio-agents-springai-temporal
```

---

## Known issues and action items

### ⚠️ demo3 requires `temporal-spring-ai:0.0.2-SNAPSHOT` — jar not yet in `lib/`

`demo3-mcp/pom.xml` references `io.temporal.ai:temporal-spring-ai:0.0.2-SNAPSHOT`. Only `0.0.1-SNAPSHOT` is committed to `lib/`. The VM setup script will fail to compile demo3 until this is resolved.

Options, in order of preference:

1. **Build and commit a `0.0.2-SNAPSHOT` jar** to `lib/` that includes the `io.temporal.ai.mcp` package, then update `scripts/install-libs.sh` to install it alongside the existing jar.
2. **Check whether `0.0.1-SNAPSHOT` already includes MCP classes.** If it does, update `demo3-mcp/pom.xml` to reference `0.0.1-SNAPSHOT` instead. (The `0.0.1-SNAPSHOT` jar in `lib/` is 62 KB and does include the `io.temporal.ai.mcp` package per the maintainer notes above — this may be the right fix.)
3. If neither is possible before the event, demo3 reverts to instructor-demo-only (run from Cornelia's machine, not the Instruqt VM).

### ⚠️ F1 MCP server commit hash is a placeholder

`track_scripts/setup-workshop-host` clones the `f1-mcp-server` repo and checks out commit `b8f5e2a` — this is a placeholder. Before the event, find the latest commit hash that builds cleanly, verify it with `setup-f1-server.sh` locally, and update the `F1_COMMIT` variable in `setup-workshop-host`.

```bash
cd /tmp && git clone https://github.com/rakeshgangwar/f1-mcp-server.git
cd f1-mcp-server && git log --oneline -5
```

Use the hash of the most recent commit that passes `npm run build` and produces working FastF1 output.

### Note: `mcp-servers.json` contains Cornelia's local paths

`demo3-mcp/src/main/resources/mcp-servers.json` and `demo4-hitl/src/main/resources/mcp-servers.json` both contain hardcoded local paths from Cornelia's development machine. This is expected: the `setup-f1-server.sh` script (locally) and the VM setup script (in Instruqt) both overwrite these files with the correct paths at setup time. The checked-in versions are intentionally left as-is to indicate which files need updating — do not rely on them to run the demos directly without running setup first.

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