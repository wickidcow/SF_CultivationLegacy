# Cultivation maintenance contract

Preserve existing item and research IDs, player UUIDs, experience values, discovery keys, typed persistent data, recipes, plant behavior, storage identity and output rates. Minecraft 1.21.11+ / Java 21 bytecode is the runtime floor, not an age limit for saved data.

Never accept an empty experience or codex file as recovery from a read/parse failure. Stop before registering listeners and items when persisted data cannot load. Strictly read an owner's configuration before adding missing defaults. Keep existing data files intact on serialization failures; staged replacement is best effort, not absolute crash/power-loss durability.

Run the source guard and complete Maven tests/package, and test real Paper startup/restart using disposable old-format data. Do not skip new tests, blanket-suppress warnings, change IDs or drop features just to get a green build. Keep test-only libraries out of the distributable JAR. Preserve concurrent repository work and publish raw JARs only after coordinated validation.
