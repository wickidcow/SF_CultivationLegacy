#!/usr/bin/env python3
"""Fail a Cultivation build if new Java API deprecations appear.

The approved remaining warnings come from the SefiLib 0.3.0 Theme ABI, whose
constructors require deprecated Bungee ChatColor. Do not suppress those warnings.
Each Maven compile target is audited independently to prevent accidental regressions.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

APPROVED_FILE = "dev/sefiraat/cultivation/api/utils/CultivationThemes.java"
APPROVED_DETAILS = frozenset({
    "net.md_5.bungee.api.ChatColor in net.md_5.bungee.api has been deprecated",
    "of(java.lang.String) in net.md_5.bungee.api.ChatColor has been deprecated",
})
MAX_APPROVED_WARNINGS = 38

# Maven compiler emits this when -Xlint:deprecation/-Xlint:removal are enabled.
JAVA_WARNING = re.compile(
    r"\\[WARNING\\]\\s+.*?/src/main/java/"
    r"(?P<file>[A-Za-z0-9_./-]+\\.java):"
    r"\\[(?P<line>\\d+),(?P<column>\\d+)\\]\\s+"
    r"(?P<detail>.+)"
)


def audit_log(log_text: str) -> tuple[int, list[str]]:
    """Return the unique approved warning count and all unapproved warnings."""
    distinct: set[tuple[str, int, int, str]] = set()
    unexpected: list[str] = []
    for line in log_text.splitlines():
        if "[WARNING]" not in line or "/src/main/java/" not in line:
            continue
        # Compilation warnings about non-deprecation matters are a separate audit.
        if "deprecated" not in line.lower() and "removal" not in line.lower():
            continue
        match = JAVA_WARNING.search(line)
        if match is None:
            unexpected.append("Unparseable Java deprecation warning: " + line.strip())
            continue
        file = match.group("file")
        reason = match.group("detail").strip()
        marker = (file, int(match.group("line")), int(match.group("column")), reason)
        if file != APPROVED_FILE or reason not in APPROVED_DETAILS:
            unexpected.append(f"New deprecated API: {file}:{marker[1]}:{marker[2]} {reason}")
        else:
            distinct.add(marker)

    if len(distinct) > MAX_APPROVED_WARNINGS:
        unexpected.append(
            f"Approved SefiLib warnings increased from {MAX_APPROVED_WARNINGS} to {len(distinct)}"
        )
    return len(distinct), unexpected


def main(paths: list[str]) -> int:
    if not paths:
        print("Usage: python3 scripts/verify_deprecation_baseline.py <maven-log> [...]", file=sys.stderr)
        return 2

    failures = 0
    for name in paths:
        try:
            log = Path(name).read_text(encoding="utf-8")
        except OSError as exc:
            print(f"Cannot read {name}: {exc}", file=sys.stderr)
            failures += 1
            continue
        count, unexpected = audit_log(log)
        print(f"{name}: {count}/{MAX_APPROVED_WARNINGS} approved SefiLib deprecations")
        for item in unexpected:
            print(item, file=sys.stderr)
        failures += len(unexpected)
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
