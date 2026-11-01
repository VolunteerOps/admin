#!/usr/bin/env bash
set -euo pipefail
# Compatibility entry point: build the current Next.js portal.
exec bash "$(dirname "$0")/build-web.sh"
