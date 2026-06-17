#!/usr/bin/env bash
# Fetch and checkout the pinned JetBrains compose-multiplatform-core revision.
# Run once to populate or update the vendored upstream checkout.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="$SCRIPT_DIR/compose-multiplatform-core"
REV_FILE="$SCRIPT_DIR/COMPOSE_REVISION.txt"
UPSTREAM="https://github.com/JetBrains/compose-multiplatform-core.git"

PINNED_REV=$(grep -v '^#' "$REV_FILE" | head -1 | tr -d '[:space:]')

if [ -z "$PINNED_REV" ]; then
    echo "ERROR: no pinned revision found in $REV_FILE" >&2
    exit 1
fi

if [ -d "$TARGET/.git" ]; then
    echo "Updating existing checkout to $PINNED_REV..."
    cd "$TARGET"
    git fetch origin
    git checkout "$PINNED_REV"
else
    echo "Cloning upstream (shallow at $PINNED_REV)..."
    git clone --depth 1 --branch "$PINNED_REV" "$UPSTREAM" "$TARGET" 2>/dev/null || {
        # If the pinned rev is not a branch head, do a full clone
        echo "Pinned revision is not a branch head; doing full clone..."
        rm -rf "$TARGET"
        git clone "$UPSTREAM" "$TARGET"
        cd "$TARGET"
        git checkout "$PINNED_REV"
    }
fi

echo "3rdparty/compose-multiplatform-core pinned at: $(cd "$TARGET" && git rev-parse HEAD)"
