#!/bin/sh
# Copy a tagged toolkit version into a plugin repository.
#
# Usage: sh scripts/vendor.sh [--with-tests | --only <paths>] <tag> <path-to-plugin-repo>
#
# Replaces <plugin>/src/main/java/ws/yume/toolkit/ with the tag's copy (and, with
# --with-tests, <plugin>/src/test/java/ws/yume/toolkit/), then writes
# <plugin>/src/main/java/ws/yume/toolkit/VENDORED. Also removes a pre-0.3.0 sh/yumekui copy.
#
# --only copies just the listed packages or classes, comma-separated and relative to the
# package root (e.g. overlay/WrappedText.java,overlay/WrappedTextComponent.java or time), so a
# plugin ships no unused toolkit code. Imports of toolkit classes left out fail the run;
# same-package references don't need an import, so build the plugin afterwards.
# Never commits in the plugin repo.
set -eu

usage() {
    echo "usage: sh scripts/vendor.sh [--with-tests | --only <paths>] <tag> <path-to-plugin-repo>" >&2
    exit 2
}

WITH_TESTS=false
ONLY=
if [ "${1:-}" = "--with-tests" ]; then
    WITH_TESTS=true
    shift
elif [ "${1:-}" = "--only" ]; then
    [ $# -ge 2 ] || usage
    ONLY=$2
    shift 2
fi
[ $# -eq 2 ] || usage
TAG=$1
PLUGIN=$2

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
PKG=src/main/java/ws/yume/toolkit
TEST_PKG=src/test/java/ws/yume/toolkit

[ -d "$PLUGIN" ] || { echo "vendor: plugin repo not found: $PLUGIN" >&2; exit 2; }
PLUGIN=$(CDPATH= cd -- "$PLUGIN" && pwd)
[ "$PLUGIN" != "$ROOT" ] || { echo "vendor: refusing to vendor into the toolkit itself" >&2; exit 2; }

COMMIT=$(git -C "$ROOT" rev-parse --verify --quiet "refs/tags/$TAG^{commit}") || {
    echo "vendor: unknown tag: $TAG (try: git -C \"$ROOT\" fetch --tags)" >&2
    exit 2
}

WORK=$(mktemp -d)
cleanup() {
    git -C "$ROOT" worktree remove --force "$WORK/tree" >/dev/null 2>&1 || true
    rm -rf "$WORK"
}
trap cleanup EXIT INT TERM

git -C "$ROOT" worktree add --detach --quiet "$WORK/tree" "$COMMIT"

copy_tree() {
    # $1 = path relative to a repo root
    rm -rf "$PLUGIN/$1"
    mkdir -p "$(dirname -- "$PLUGIN/$1")"
    cp -R "$WORK/tree/$1" "$PLUGIN/$1"
}

remove_legacy() {
    # pre-0.3.0 package root (sh/yumekui): drop it and its empty parents so re-vendoring migrates
    rm -rf "$PLUGIN/$1/sh/yumekui/toolkit"
    rmdir "$PLUGIN/$1/sh/yumekui" "$PLUGIN/$1/sh" 2>/dev/null || true
}

copy_only() {
    # Copy each listed path under $PKG, then check toolkit imports resolve inside the copy.
    rm -rf "$PLUGIN/$PKG"
    mkdir -p "$PLUGIN/$PKG"
    old_ifs=$IFS
    IFS=,
    for item in $ONLY; do
        item=${item%/}
        case $item in
            ""|/*|*..*) echo "vendor: bad --only path: '$item'" >&2; exit 2 ;;
        esac
        src="$WORK/tree/$PKG/$item"
        [ -e "$src" ] || { echo "vendor: not in $TAG: $PKG/$item" >&2; exit 2; }
        mkdir -p "$(dirname -- "$PLUGIN/$PKG/$item")"
        cp -R "$src" "$PLUGIN/$PKG/$item"
    done
    IFS=$old_ifs
    missing=$(grep -rhoE '^import ws\.yume\.toolkit\.[a-z]+\.[A-Za-z0-9_]+' "$PLUGIN/$PKG" --include='*.java' \
        | sed 's/^import ws\.yume\.toolkit\.//' | sort -u | while IFS= read -r ref; do
            f="$PLUGIN/$PKG/$(echo "$ref" | tr . /).java"
            [ -f "$f" ] || echo "$ref"
        done)
    if [ -n "$missing" ]; then
        echo "vendor: --only leaves out classes the copy imports; add them:" >&2
        echo "$missing" | sed 's/^/    /' >&2
        rm -rf "$PLUGIN/$PKG"
        exit 1
    fi
}

remove_legacy src/main/java
remove_legacy src/test/java
if [ -n "$ONLY" ]; then
    copy_only
else
    copy_tree "$PKG"
fi
if [ "$WITH_TESTS" = true ]; then
    copy_tree "$TEST_PKG"
fi

{
    echo "tag=$TAG"
    echo "commit=$COMMIT"
    echo "date=$(date -u +%Y-%m-%d)"
    echo "tests=$WITH_TESTS"
    [ -z "$ONLY" ] || echo "only=$ONLY"
} > "$PLUGIN/$PKG/VENDORED"

SHORT=$(echo "$COMMIT" | cut -c1-7)
echo "vendor: copied ws.yume.toolkit $TAG ($SHORT) into $PLUGIN"
[ "$WITH_TESTS" = true ] && echo "vendor: tests copied too"
[ -z "$ONLY" ] || echo "vendor: only $ONLY"
echo
echo "Next: build the plugin, review the diff, then commit, for example:"
echo
echo "    git -C \"$PLUGIN\" add $PKG$( [ "$WITH_TESTS" = true ] && printf ' %s' "$TEST_PKG" )"
echo "    git -C \"$PLUGIN\" commit -m \"Vendor ws.yume.toolkit $TAG ($SHORT)\""
