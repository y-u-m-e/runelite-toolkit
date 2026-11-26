# Changelog

All notable changes to this toolkit are recorded here, one section per tag.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.2.0] - 2026-11-26

### Added

- `overlay.WrappedText`: lays fixed-size text out at a given width. Segments share a line when
  they fit, then each starts its own line and words wrap; a word is never split, and the width
  never goes below the widest word.
- `overlay.WrappedTextComponent`: an `OverlayPanel` child that renders `WrappedText` at the
  width the panel gives it, reports the height it needs, and gives the box's minimum width.
  The layout is cached until the segments, font or width change.

## [0.1.0] - 2026-10-04

First release: the toolkit as it stood in the RFL plugin repository at commit
`2237da8380931259ba13ab1681f32d1581f7c848`. Java sources and tests are imported unchanged.

### Added

- Areas `clipboard`, `concurrent`, `geom`, `io`, `model`, `overlay`, `scene`, `swing`, `text`
  and `time`, with their tests.
- `scripts/vendor.sh` and `scripts/check-vendored.sh` to copy a tagged version into a plugin and
  verify the copy.
- `scripts/compliance-scan.sh`, run in CI, which fails on APIs the Plugin Hub forbids.
- `ImportBoundaryTest`, which fails if toolkit sources import anything beyond the JDK, the
  RuneLite client and the libraries it ships.

### Changed

- The package README now points at the vendoring scripts instead of describing a manual copy.

[Unreleased]: https://github.com/y-u-m-e/runelite-toolkit/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/y-u-m-e/runelite-toolkit/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/y-u-m-e/runelite-toolkit/releases/tag/v0.1.0
