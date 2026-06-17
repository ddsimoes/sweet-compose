#!/usr/bin/env bash
# Usage: tools/flake-runner.sh <N>
# Runs the full test suite N times, captures test result XML for later analysis.
set -u
N=${1:-20}
OUT=build/flake-report; mkdir -p "$OUT"
for i in $(seq 1 "$N"); do
  echo "=== Run $i/$N ==="
  xvfb-run -a ./gradlew cleanTest test --continue -q > "$OUT/run-$i.log" 2>&1
  for mod in sweet sample-sweet kotlinx-coroutines-swt; do
    cp -r "$mod/build/test-results/test" "$OUT/run-$i-$mod" 2>/dev/null || true
  done
done
echo "Done. Results in $OUT/"
