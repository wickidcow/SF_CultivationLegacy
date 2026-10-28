# Cultivation deprecation audit — 2026-10-28

This is an addon-source audit of Java API deprecation warnings. Build warnings from Maven repository cache resolution and duplicate shaded metadata/classes are tracked separately; they are **not** counted as deprecated Java calls.

## Audit baseline

The Maven compiler now uses `-Xlint:deprecation` and `-Xlint:removal`. The initial audit against Slimefun Legacy 4.1.29 showed 76 distinct source diagnostics across 10 Java files, repeated by three compile target builds:

| Category | Distinct diagnostics | Treatment |
| --- | ---: | --- |
| SefiLib / Bungee hex ChatColor | 38 | Requires careful SefiLib Theme migration |
| Legacy ChestMenu and ClickAction references | 23 | Supported by the official Slimefun Legacy 4.1.78 API; update provided compile dependency |
| Bukkit string lore | 8 | Use Adventure component lore |
| Legacy int energy-capacity interface | 3 | The public interface remains supported in Slimefun Legacy 4.1.78 |
| Bukkit string display names | 4 | Use Adventure component display names |

The first updated CI build passed (16 JUnit tests, 0 failures, 0 errors) and reported **38 distinct remaining diagnostics**, all from `CultivationThemes.java` Bungee ChatColor references.

## Why the remaining warnings are not silently suppressed

Cultivation depends on SefiLib 0.3.0 `dev.sefiraat.sefilib.string.Theme`, whose public constructor takes `net.md_5.bungee.api.ChatColor` rather than Adventure `TextColor`. Replacing `ChatColor.of("#RRGGBB")` calls with Adventure `TextColor` directly does not compile. Replacing all Theme references or introducing an incompatible Theme class risks changing color output, item lore, and other addons' API expectations.

Next action: evaluate an API-compatible modernized SefiLib Theme or a carefully staged local theme adapter with parity tests. Do not remove item IDs, change lore contents, hide the warnings, or use reflection to evade the deprecated symbols.

## Preservation and validation requirements

- Preserve all existing Slimefun IDs, saved player XP, discovery IDs and keys, typed plant profiles, seed-pack contents, recipes and growth/production rates.
- Keep real player data on disk when parsing/serialization fails; do not treat a failed read as an empty file.
- Use source guard, Java 21 bytecode check, all 16+ unit tests, raw JAR checks and Paper API compilation targets before merge.
- CI compilation is **not** a substitute for real startup/restart tests. Verify with disposable old-format data and test: placing/breaking crops, breeding, catalog UI, seed-pack name/lore and storage persistence, recipe unlocks, garden-cloche visuals and output, powered kitchen recipes and network charge.
- Also validate Folia region scheduling and concurrent menu/machine behavior on an actual compatible Folia build before asserting Folia support.
- Keep unrelated Maven toolchain modernization PR #4 separate; reconcile its changes when merging.
- A passing draft PR is **not** a release authorization.
