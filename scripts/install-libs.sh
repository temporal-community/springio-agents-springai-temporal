#!/usr/bin/env bash
# ABOUTME: Installs the pre-built temporal-spring-ai jar into the local Maven repository.
# Run this once before building demo2, demo3, or demo4.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LIB_DIR="$SCRIPT_DIR/../lib"

echo "Installing temporal-spring-ai jar to local Maven repository..."

mvn install:install-file \
  -Dfile="$LIB_DIR/temporal-spring-ai-1.35.0-SNAPSHOT.jar" \
  -DgroupId=io.temporal \
  -DartifactId=temporal-spring-ai \
  -Dversion=1.35.0-SNAPSHOT \
  -Dpackaging=jar \
  -q

echo "  Installed temporal-spring-ai-1.35.0-SNAPSHOT"
echo "Done."
