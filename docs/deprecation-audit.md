# Cultivation 1.1.24 — deprecation migration and validation

## Outcome and scope

The original audit identified 76 distinct compiler diagnostics. Updating the provided Slimefun Legacy API to 4.1.78 and migrating ItemMeta text reduced that to 38. Version 1.1.24 removes the remaining active Bungee color calls by supplying an Adventure-backed, binary-compatible implementation of SefiLib's Theme class. CI now requires **zero deprecated/removal source diagnostics** on Paper API 1.21.11, 26.1, 26.2 and 26.3.

This is a focused Theme/API migration, not a claim that every class inside all bundled third-party libraries has been rewritten. Other Maven shading/unchecked-type notices are separate from API deprecation diagnostics.

## SefiLib compatibility boundary

Cultivation publicly exposes `dev.sefiraat.sefilib.string.Theme` in theme constants, PlantTheme and CultivationFlora. Changing that class name or its old public descriptors would break compiled consumers. The local GPL-3.0 adaptation therefore keeps every original public method, constructor and field descriptor, verified against the original SefiLib 0.3.0 JAR by automated tests.

Normal code now uses Adventure TextColor, getTextColor() and legacyCode(). Exactly three old public members remain explicitly deprecated, not marked for removal: the two ChatColor constructors and getColor(). They exist only as binary-compatibility bridges for old consumers. The implementation does not use blanket warning suppression or reflection to avoid deprecated symbols. Reflection in the tests verifies the original binary API, not production behavior.

Only the upstream dependency's Theme.class is excluded from shading. The packaged addon must contain exactly one replacement Theme.class. Other SefiLib classes are unchanged. THIRD_PARTY_NOTICES.txt and licenses/LICENSE ship in the JAR, and the modified source remains in this repository.

## Text and display preservation

All 19 Cultivation palette colors and existing public labels remain unchanged, including the historical DISCOVEREY field spelling. Legacy hexadecimal text retains its original lowercase encoding. RGB-preserving serialization prevents existing custom colors from being downsampled when generating plant displays, prefixing Seed Pack names, or replacing recipe-scroll placeholders.

The item-display 180-degree correction no longer relies on PaperLib parsing an old 1.x minor-version number: every supported server is newer than 1.20. Tests cover all stored display designs, both item/non-item modes, and repeated calls without accumulated rotation.

Item IDs, recipes, breeding probabilities, production rates, typed persistent-data keys and player-data formats are not migrated or reset.

## Automated gates

- 46 Java tests cover player-data safety, exact palette/labels, old/new Theme binary compatibility, legacy formatting tokens, RGB text/particle colors and display transformations.
- Six Python tests validate the zero-deprecation guard, including Windows/ANSI diagnostics and rejection of an empty build log.
- Full baseline Maven tests/package plus Java 21 bytecode checks; API compilation against 1.21.11, 26.1, 26.2 and stable 26.3.
- Packaged-JAR checks reject leaked test frameworks/probe classes and duplicate Theme.class entries and require license notices.
- Disposable official checksum-verified Paper 1.21.11 on Java 21 and Paper 26.3 on Java 25 run a separate integration probe on two consecutive boots. It exercises item registration/serialization, analyzed plant profiles, breeding-pair logic, Seed Pack GUI selectors and storage persistence, recipe scroll generation, kitchen recipe lookup and power capacity.
- Each runtime also receives a third deliberately malformed-codex boot, which must fail plugin initialization without changing the malformed codex bytes or the unrelated XP file. The CI-only probe is removed for this negative test because its dependency is deliberately disabled.
- These runtime gates apply before producing release assets on main/tag pushes, not only pull requests. Existing published release files are never overwritten by reruns.

The integration probe lives under scripts/integration and is built separately; it is never distributed with Cultivation. Test servers bind only to loopback, and the harness refuses to modify a non-empty server directory.

## Validation limits

Programmatic integration checks do not simulate a connected player's inventory drag/shift-click sequences, every protection-plugin combination, actual crop growth in loaded production chunks, or sustained multi-region concurrency. In particular, **Folia support is not certified or newly enabled by this release**; a region-scheduler migration and dedicated Folia gameplay tests are a separate compatibility task. Purpur/Leaf-specific runtime behavior is not inferred merely from Paper compile success.

Keep the independent Maven toolchain PR #4 separate. A toolchain update must retain these migration tests and release gates when it is reconciled.
