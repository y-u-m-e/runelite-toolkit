# runelite-toolkit

Small, plugin-neutral building blocks for [RuneLite](https://runelite.net) plugins, in the Java
package `sh.yumekui.toolkit`. Every piece follows the Plugin Hub rules, so it can ship inside a Hub
plugin unchanged.

## Why vendoring

The Plugin Hub builds each plugin from its own repository and does not allow extra dependencies,
so a plugin cannot pull this toolkit in as a library. Instead the toolkit is **vendored**: a tagged
version is copied into the plugin's source tree under the same package name. Each Hub plugin loads
in its own class loader, so two plugins carrying the same classes never clash.

Because it ships inside plugins, the toolkit keeps the same rules a plugin does:

- no reflection, JNI/JNA, process launching, Java serialization or dynamic code loading;
- no `new Gson()` (a `Gson` is passed in), no `HttpURLConnection`, no `Desktop` calls;
- no IO on the client thread;
- no dependency beyond what `net.runelite:client` already provides (JDK, client API, Gson,
  Guice/`javax.inject`, SLF4J; Lombok at compile time).

`scripts/compliance-scan.sh` and `ImportBoundaryTest` enforce this in CI.

## Areas

| Package | Types | What it's for |
|---|---|---|
| `clipboard` | `Clipboard` | Copy text without throwing when the clipboard is busy. |
| `concurrent` | `SerialQueue` | Run queued steps one at a time, in order, on any executor. |
| `geom` | `TriangleMesh` | Posed model triangles and exact triangle-triangle intersection. |
| `io` | `GzipNdjsonWriter`, `WriterState`, `DailyJsonlAppender`, `IoErrors` | Off-thread gzipped NDJSON and daily JSONL files, with readable error reasons. |
| `model` | `ModelCapture`, `Palette`, `RenderableModels`, `VertexSnapshot` | Copy and pose model geometry; the client's HSL16 colour table. |
| `overlay` | `TilePainter`, `MeshProjector`, `WrappedText`, `WrappedTextComponent` | Draw tiles and mesh triangles cheaply each frame; fixed-size overlay text that wraps instead of clipping. |
| `scene` | `PohDetector`, `SceneStamps` | Player-owned-house detection; tie stored tiles to the scene they came from. |
| `swing` | `SidebarWidgets`, `ConfirmHeader`, `SelectableCard`, `ToggleSelection`, `CopyFeedbackLabel` | Fixed-size side-panel widgets. |
| `text` | `FileNameTemplate`, `PlayerNames`, `Html` | Safe file names, RuneLite-style name matching, Swing HTML escaping. |
| `time` | `FrameBudget` | Spread a long job over frames within a time budget. |

Each type's javadoc says what it does, which thread it expects and where its numbers come from.
The full per-type index and the dependencies between packages are in
[`src/main/java/sh/yumekui/toolkit/README.md`](src/main/java/sh/yumekui/toolkit/README.md).

## Quick start

Vendor a tagged version into a plugin repository:

```sh
git clone https://github.com/y-u-m-e/runelite-toolkit.git
cd runelite-toolkit
sh scripts/vendor.sh v0.1.0 ../my-plugin            # main sources only
sh scripts/vendor.sh --with-tests v0.1.0 ../my-plugin  # also copy the tests
```

`vendor.sh` checks the tag out into a temporary worktree, replaces
`<plugin>/src/main/java/sh/yumekui/toolkit/` (and, with `--with-tests`,
`<plugin>/src/test/java/sh/yumekui/toolkit/`) with the tag's copy, and writes
`<plugin>/src/main/java/sh/yumekui/toolkit/VENDORED`:

```
tag=v0.1.0
commit=<full commit SHA>
date=<YYYY-MM-DD, UTC>
tests=<true|false>
```

It never commits in the plugin repository; it prints a suggested commit message instead.

Check that a plugin's copy still matches its recorded tag:

```sh
sh scripts/check-vendored.sh ../my-plugin
```

It exits non-zero on any drift and lists the files that changed, are missing, or are extra. A
plugin can run it in its own CI.

Both scripts are POSIX `sh` and run on Linux, macOS and Git Bash on Windows.

### Updating a plugin

1. Vendor the new tag: `sh scripts/vendor.sh <tag> <plugin>`.
2. Build the plugin (`./gradlew build`) and fix anything the new version needs.
3. Commit the vendored change in the plugin repository.
4. Bump the plugin's Plugin Hub manifest to that commit.

Don't edit a vendored copy in place. Fix it here, tag a release, and vendor the new tag.

## Building

Java 11, Gradle wrapper (8.10):

```sh
./gradlew build                  # compile and run the tests
sh scripts/compliance-scan.sh    # forbidden-API scan over src/main
```

The client is a `compileOnly` dependency at `latest.release`, as in the Plugin Hub template.

## Releases

Versions follow semantic versioning and are tagged `vX.Y.Z`. See [CHANGELOG.md](CHANGELOG.md).

## Licence

BSD 2-Clause, see [LICENSE](LICENSE).
