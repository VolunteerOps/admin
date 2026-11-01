#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
npm ci --prefix web
npm run business:sync --prefix web
npm run build --prefix web
