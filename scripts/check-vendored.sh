#!/bin/sh
# Check that a plugin's vendored toolkit matches the tag recorded in its VENDORED file.
#
# Usage: sh scripts/check-vendored.sh <path-to-plugin-repo>
#
# Exits 0 when the copy matches, 1 on drift (and lists the differing files), 2 on usage errors.
set -eu

[ $# -eq 1 ] || { echo "usage: sh scripts/check-vendored.sh <path-to-plugin-repo>" >&2; exit 2; }
PLUGIN=$1

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
PKG=src/main/java/ws/yume/toolkit
TEST_PKG=src/test/java/ws/yume/toolkit
STAMP="$PLUGIN/$PKG/VENDORED"

[ -f "$STAMP" ] || { echo "check-vendored: no VENDORED file at $STAMP" >&2; exit 2; }

value() {
    sed -n "s/^$1=//p" "$STAMP" | tr -d '\r' | head -n 1
}
TAG=$(value tag)
RECORDED=$(value commit)
TESTS=$(value tests)
[ -n "$TAG" ] || { echo "check-vendored: VENDORED has no tag=" >&2; exit 2; }

COMMIT=$(git -C "$ROOT" rev-parse --verify --quiet "refs/tags/$TAG^{commit}") || {
    echo "check-vendored: unknown tag: $TAG (try: git -C \"$ROOT\" fetch --tags)" >&2
    exit 2
}

DRIFT=0
if [ -n "$RECORDED" ] && [ "$RECORDED" != "$COMMIT" ]; then
    echo "check-vendored: tag $TAG is $COMMIT but VENDORED records $RECORDED" >&2
    DRIFT=1
fi

WORK=$(mktemp -d)
cleanup() {
    git -C "$ROOT" worktree remove --force "$WORK/tree" >/dev/null 2>&1 || true
    rm -rf "$WORK"
}
trap cleanup EXIT INT TERM

git -C "$ROOT" worktree add --detach --quiet "$WORK/tree" "$COMMIT"

compare_tree() {
    # $1 = path relative to a repo root. Lists files that differ, are missing, or are extra.
    want="$WORK/tree/$1"
    have="$PLUGIN/$1"
    (cd "$want" && find . -type f) | sort > "$WORK/want.txt"
    if [ -d "$have" ]; then
        (cd "$have" && find . -type f ! -path ./VENDORED) | sort > "$WORK/have.txt"
    else
        : > "$WORK/have.txt"
    fi
    sort -u "$WORK/want.txt" "$WORK/have.txt" | while IFS= read -r f; do
        rel="$1/${f#./}"
        if [ ! -f "$have/$f" ]; then
            echo "missing  $rel"
        elif [ ! -f "$want/$f" ]; then
            echo "extra    $rel"
        elif ! cmp -s "$want/$f" "$have/$f"; then
            echo "changed  $rel"
        fi
    done
}

REPORT="$WORK/report.txt"
compare_tree "$PKG" > "$REPORT"
if [ "$TESTS" = true ]; then
    compare_tree "$TEST_PKG" >> "$REPORT"
fi

if [ -s "$REPORT" ]; then
    echo "check-vendored: $PLUGIN differs from toolkit $TAG:" >&2
    cat "$REPORT" >&2
    DRIFT=1
fi

if [ "$DRIFT" -ne 0 ]; then
    exit 1
fi
echo "check-vendored: OK, $PLUGIN matches toolkit $TAG ($(echo "$COMMIT" | cut -c1-7))$( [ "$TESTS" = true ] && echo ', tests included')"
