#!/usr/bin/env python3
"""Reject every deprecated/for-removal source diagnostic after the Theme migration.

Old binary bridges remain explicitly deprecated declarations, not warning
suppressions. The compiler must report zero deprecated API uses in main source.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ANSI = re.compile(r"\x1b\[[0-9;]*m")


def audit_log(log_text: str) -> tuple[int, list[str]]:
    unexpected: set[str] = set()
    for raw in log_text.splitlines():
        line = ANSI.sub("", raw).replace("\\", "/")
        if "[WARNING]" not in line and "warning:" not in line.lower():
            continue
        if "src/main/java/" not in line and ".java:" not in line:
            continue
        if any(token in line.lower() for token in ("deprecated", "deprecation", "removal")):
            unexpected.add(line.strip())
    return 0, sorted(unexpected)


def main(paths: list[str]) -> int:
    if not paths:
        print("Usage: verify_deprecation_baseline.py <maven-log> [...]", file=sys.stderr)
        return 2
    failures = 0
    for name in paths:
        try:
            log = Path(name).read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            print(f"Cannot read {name}: {exc}", file=sys.stderr)
            failures += 1
            continue
        # Do not mistake an empty, truncated or failed build log for zero warnings.
        if "BUILD SUCCESS" not in log:
            print(f"{name}: missing successful Maven build evidence", file=sys.stderr)
            failures += 1
        _, unexpected = audit_log(log)
        print(f"{name}: {len(unexpected)} deprecated/removal source diagnostics (required: 0)")
        for line in unexpected:
            print(line, file=sys.stderr)
        failures += len(unexpected)
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
