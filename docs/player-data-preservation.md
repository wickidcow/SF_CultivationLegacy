# Player data preservation

Cultivation previously used the forgiving Bukkit YAML convenience loader for exp.yml and codex.yml. A malformed file could therefore become an empty in-memory data set, later saved over the owner's file on shutdown. The default-configuration updater similarly accepted a failed load before copying defaults.

This candidate uses strict UTF-8/YAML reads for all three files. A failure throws before the ConfigManager is published or items/listeners/tasks are registered. The existing shutdown null guard prevents incomplete initialization from writing any player files. Fresh missing experience/codex files still start empty intentionally; existing malformed or dangling-symlink files are never replaced as fresh data.

Normal writes use complete serialization and staged sibling replacement with file flushing, atomic move where supported, an explicit unsupported-atomic fallback, preserved POSIX permissions and retained file symlinks. Unknown keys, player UUIDs, experience/discovery paths and values are preserved. No plant IDs, recipes, progression rates or mechanics change. The helper adds no new runtime dependency.

Thirteen real YAML/file tests cover malformed input, exact data round-trips, serialization failure, failed replacement, long keys, symlinks and permissions. Normal CI now executes them in the floor build. Real old-to-new server restarts and malformed-data startup must also pass before release. Version remains 1.1.23; this is not a stable-release or whole-world certification.
