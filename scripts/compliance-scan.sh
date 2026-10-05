#!/bin/sh
# Scan toolkit sources for APIs the Plugin Hub forbids. Exits non-zero on any hit.
#
# Usage: sh scripts/compliance-scan.sh [dir]
#   dir  tree to scan (default: src/main, relative to the repository root)
#
# Tests are not scanned: they may use java.lang.reflect.Proxy for fakes.
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DIR=${1:-"$ROOT/src/main"}

if [ ! -d "$DIR" ]; then
    echo "compliance-scan: no such directory: $DIR" >&2
    exit 2
fi

# One fixed string per line. Matched literally, anywhere in a .java file.
PATTERNS='java.lang.reflect
setAccessible
ProcessBuilder
Runtime.getRuntime().exec
ObjectInputStream
ObjectOutputStream
new Gson(
HttpURLConnection
Desktop.getDesktop
System.loadLibrary'

LIST=$(mktemp)
HITS=$(mktemp)
trap 'rm -f "$LIST" "$HITS"' EXIT INT TERM

find "$DIR" -type f -name '*.java' > "$LIST"
count=$(wc -l < "$LIST" | tr -d ' ')

if [ "$count" -eq 0 ]; then
    echo "compliance-scan: no .java files under $DIR" >&2
    exit 2
fi

# Read the file list line by line so paths with spaces survive.
while IFS= read -r file; do
    echo "$PATTERNS" | while IFS= read -r pattern; do
        grep -nF -- "$pattern" "$file" | sed "s|^|$file:|; s|\$|   [$pattern]|" || true
    done
done < "$LIST" > "$HITS"

if [ -s "$HITS" ]; then
    echo "compliance-scan: forbidden API use found:" >&2
    cat "$HITS" >&2
    exit 1
fi

echo "compliance-scan: OK ($count files, no forbidden APIs)"
