#!/usr/bin/env bash
set -euo pipefail

project_root="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"

# Keep the check graph in Gradle so local PowerShell, Bash and CI cannot drift.
./gradlew verify --no-daemon --console=plain --max-workers=2 "$@"
