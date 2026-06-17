#!/usr/bin/env bash
set -euo pipefail

# Relink sample files between sample-sweet and sample-jetbrains.
# For each pair, if they are not already hardlinked, hardlink the
# file with the older mtime to the file with the newer mtime.

SWEET_BASE="sample-sweet/src/main/kotlin/io/github/ddsimoes/sweet/sample"
JETBRAINS_BASE="sample-jetbrains/src/main/kotlin/io/github/ddsimoes/sweet/sample"

relink() {
  local a="$1"
  local b="$2"

  if [ ! -f "$a" ] || [ ! -f "$b" ]; then
    echo "SKIP (missing): $a | $b" >&2
    return
  fi

  local ino_a ino_b
  ino_a=$(stat -c '%i' "$a")
  ino_b=$(stat -c '%i' "$b")

  if [ "$ino_a" = "$ino_b" ]; then
    echo "OK   (already linked): $a" >&2
    return
  fi

  local mtime_a mtime_b
  mtime_a=$(stat -c '%Y' "$a")
  mtime_b=$(stat -c '%Y' "$b")

  if [ "$mtime_a" -le "$mtime_b" ]; then
    ln -f "$a" "$b"
    echo "LINK $a -> $b" >&2
  else
    ln -f "$b" "$a"
    echo "LINK $b -> $a" >&2
  fi
}

# Walk sample-sweet to discover files, then map to both sides.
find "$SWEET_BASE" -type f -name '*.kt' | while IFS= read -r sweet_file; do
  relpath="${sweet_file#"$SWEET_BASE/"}"
  jetbrains_file="$JETBRAINS_BASE/$relpath"
  relink "$sweet_file" "$jetbrains_file"
done
