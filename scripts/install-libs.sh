#!/usr/bin/env bash
# ABOUTME: Installs pre-built temporal-spring-ai jars into the local Maven repository.
# Run this once before building demo2 or demo3.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LIB_DIR="$SCRIPT_DIR/../lib"

echo "Installing temporal-spring-ai jars to local Maven repository..."

mvn install:install-file \
  -Dfile="$LIB_DIR/temporal-spring-ai-0.0.1-SNAPSHOT.jar" \
  -DgroupId=io.temporal.ai \
  -DartifactId=temporal-spring-ai \
  -Dversion=0.0.1-SNAPSHOT \
  -Dpackaging=jar \
  -q

echo "  Installed temporal-spring-ai-0.0.1-SNAPSHOT (core, for demo2)"

mvn install:install-file \
  -Dfile="$LIB_DIR/temporal-spring-ai-0.0.2-SNAPSHOT.jar" \
  -DgroupId=io.temporal.ai \
  -DartifactId=temporal-spring-ai \
  -Dversion=0.0.2-SNAPSHOT \
  -Dpackaging=jar \
  -q

echo "  Installed temporal-spring-ai-0.0.2-SNAPSHOT (core + MCP, for demo3)"
echo "Done."
