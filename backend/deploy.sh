#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POWERSHELL_SCRIPT="$(cygpath -w "$SCRIPT_DIR/deploy-auth.ps1")"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$POWERSHELL_SCRIPT"
