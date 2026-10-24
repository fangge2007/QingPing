#!/usr/bin/env bash
set -euo pipefail
project_root="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
configuration="$project_root/gradle/android-sdk.properties"
read_property() { sed -n "s/^$1=//p" "$configuration" | tr -d '\r'; }
compile_sdk="$(read_property compileSdk)"
build_tools="$(read_property buildToolsVersion)"
command_line_tools="$(read_property commandLineToolsVersion)"
[[ "$compile_sdk" =~ ^[0-9]+$ && "$build_tools" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ && "$command_line_tools" =~ ^[0-9]+$ ]] || {
  printf 'Invalid Android SDK version configuration\n' >&2
  exit 1
}
printf 'compile_sdk=%s\nbuild_tools=%s\ncommand_line_tools=%s\n' "$compile_sdk" "$build_tools" "$command_line_tools"
