---
slug: environment-setup
type: challenge
title: "Environment Setup"
teaser: Verify your environment and set your OpenAI API key before the demos begin.
notes:
- type: text
  contents: |-
    Welcome to **Building Durable AI Agents with Spring AI + Temporal**.

    This environment has everything pre-installed:
    - Java 21, Maven 3.9
    - Temporal dev server (running in the background)
    - The workshop demo code, compiled and ready
    - A network control panel for disrupting external services mid-demo

    The one thing you need to provide is your **OpenAI API key**.

    Hit **Start** when you're ready.
tabs:
- title: Terminal
  type: terminal
  hostname: workshop-host
- title: Temporal Web UI
  type: service
  hostname: workshop-host
  path: /
  port: 8080
- title: Network Control Panel
  type: service
  hostname: workshop-host
  path: /
  port: 5000
difficulty: basic
timelimit: 600
---

## Environment Setup

Before the demos begin, set your OpenAI API key in this terminal.

```
export OPENAI_API_KEY=sk-...
```

Replace `sk-...` with your actual key.

> **Don't have a key yet?** Go to [platform.openai.com/api-keys](https://platform.openai.com/api-keys) and create one. A free-tier account with a small credit balance is enough for this workshop.

---

### Verify the environment

Confirm the Temporal server is running:

```
temporal operator cluster health
```

You should see `SERVING`.

Open the **Temporal Web UI** tab — you should see the Temporal dashboard with no workflows yet.

Open the **Network Control Panel** tab — you'll use this later to disrupt external services mid-demo and watch Temporal retry them.

---

When your key is set and both tabs are loading, click **Check** to continue.
