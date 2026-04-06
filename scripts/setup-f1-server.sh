#!/usr/bin/env bash
# ABOUTME: Clones, patches, and builds the F1 MCP server for demos that use MCP.
# Run this once before running demo3 or demo4.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."
F1_DIR="$PROJECT_ROOT/f1-mcp-server"
MCP_CONFIG="$PROJECT_ROOT/demo3-mcp/src/main/resources/mcp-servers.json"

# Clone if not already present
if [ -d "$F1_DIR" ]; then
    echo "F1 MCP server already exists at $F1_DIR, skipping clone."
else
    echo "Cloning F1 MCP server..."
    git clone https://github.com/rakeshgangwar/f1-mcp-server.git "$F1_DIR"
fi

cd "$F1_DIR"

# Patch the hardcoded cache path if not already patched
if grep -q "Documents/Cline" python/f1_data.py 2>/dev/null; then
    echo "Patching FastF1 cache path..."
    sed -i.bak "s|fastf1.Cache.enable_cache.*|import os\\
_cache_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'cache')\\
os.makedirs(_cache_dir, exist_ok=True)\\
fastf1.Cache.enable_cache(_cache_dir)|" python/f1_data.py
    rm -f python/f1_data.py.bak
    echo "  Patched."
else
    echo "Cache path already patched, skipping."
fi

# Build Node side
echo "Installing Node dependencies..."
npm install --silent
echo "Building TypeScript..."
npm run build

# Set up Python venv
echo "Setting up Python venv with uv..."
uv venv -q
source .venv/bin/activate
uv pip install -q fastf1 pandas numpy

# Write mcp-servers.json for each demo that uses MCP
MCP_CONFIG_CONTENT=$(cat <<EOF
{
  "mcpServers": {
    "f1-data": {
      "command": "bash",
      "args": ["-c", "source $F1_DIR/.venv/bin/activate && node $F1_DIR/build/index.js"]
    }
  }
}
EOF
)

for demo in demo3-mcp demo4-hitl; do
    config="$PROJECT_ROOT/$demo/src/main/resources/mcp-servers.json"
    if [ -d "$PROJECT_ROOT/$demo" ]; then
        echo "Writing MCP server config to $config..."
        echo "$MCP_CONFIG_CONTENT" > "$config"
    fi
done

echo ""
echo "F1 MCP server setup complete."
echo "  Server location: $F1_DIR"
echo ""
echo "You can now run demo3 or demo4."
