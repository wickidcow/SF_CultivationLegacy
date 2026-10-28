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

## CI deprecation-regression contract (2026-10-28)

`scripts/verify_deprecation_baseline.py` checks the full compiler output separately for Paper 1.21.11, 26.1, 26.2, and 26.3. It accepts **at most 38** unique diagnostics, and only when they are the two explicitly documented Bungee ChatColor warnings within `CultivationThemes.java`. A new deprecated method, deprecated-for-removal API, unexpected warning in another source file, or growth above the current ceiling fails CI. Warnings are not hidden, suppressed, or downgraded. Retained logs remain available as a CI artifact. A future *reduction* below 38 is accepted.

`CultivationThemesTest` snapshots all **19 named theme color values**, plus public labels (including the existing `DISCOVEREY` spelling) so a future SefiLib replacement must maintain the existing hex colors, player-visible names, and API compatibility. Tests do not grant a live-server release gate.

## Disposable Paper startup/restart CI

PR builds also run an **independent** two-boot smoke using the newest official **stable Paper 26.3** server JAR returned by PaperMC Fill v3. This download requires a project-specific User-Agent, verifies the official SHA-256 and refuses pre-release builds. CI copies only the built Cultivation JAR and pinned Slimefun Legacy 4.1.78 JAR into an ephemeral temporary server, starts Paper on Java 25, waits for both plugins to enable and Paper to report readiness, shuts down cleanly, and starts it again. Old-format disposable `exp.yml` and `codex.yml` fixtures must retain their known player UUID, EXP, discovery bits and unknown legacy values across both boots.

The smoke never contacts or alters a live server. It **does not** assert full 1.21.11/Purpur/Leaf/Folia runtime support, simulate real player clicks/breeding/crafting, or prove threading safety. Those remain manual or more advanced integration gates.
