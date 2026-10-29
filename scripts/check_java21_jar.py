#!/usr/bin/env python3
"""Fail closed if a Cultivation distribution cannot load stable Java 21 bytecode.

Inspect class headers in the *packaged* raw JAR, including shaded libraries.
A successful inspection is not a runtime/ABI/storage compatibility guarantee.
"""
from __future__ import annotations

import argparse
import collections
import struct
import sys
import zipfile
from pathlib import Path

JAVA_21_MAJOR = 65
CLASS_MAGIC = b"\xca\xfe\xba\xbe"
OWNED_PREFIX = "dev/sefiraat/cultivation/"
LOCAL_THEME = "dev/sefiraat/sefilib/string/Theme.class"


def inspect_jar(path: Path) -> tuple[int, collections.Counter[int], list[str]]:
    """Return (owned classes, all-class major histogram, violations)."""
    errors: list[str] = []
    versions: collections.Counter[int] = collections.Counter()
    seen: set[str] = set()
    owned = 0
    try:
        with zipfile.ZipFile(path, "r") as jar:
            for entry in sorted(jar.infolist(), key=lambda item: item.filename):
                name = entry.filename
                if not name.endswith(".class"):
                    continue
                if name in seen:
                    errors.append(f"{name}: duplicate class archive member")
                    continue
                seen.add(name)
                if name.startswith(OWNED_PREFIX) or name == LOCAL_THEME:
                    owned += 1
                try:
                    with jar.open(entry, "r") as stream:
                        header = stream.read(8)
                except (OSError, RuntimeError, EOFError, zipfile.BadZipFile) as exc:
                    errors.append(f"{name}: cannot read class header ({type(exc).__name__})")
                    continue
                if len(header) != 8 or not header.startswith(CLASS_MAGIC):
                    errors.append(f"{name}: missing or invalid class-file header")
                    continue
                minor, major = struct.unpack(">HH", header[4:8])
                versions[major] += 1
                if major < 45 or major > JAVA_21_MAJOR:
                    errors.append(f"{name}: unsupported class-file version {major}.{minor} (Java 21 max {JAVA_21_MAJOR}.0)")
                elif major >= 56 and minor != 0:
                    reason = "preview bytecode" if minor == 65535 else "invalid modern minor version"
                    errors.append(f"{name}: {reason} {major}.{minor} is not stable Java 21 bytecode")
    except (OSError, EOFError, zipfile.BadZipFile, ValueError) as exc:
        errors.append(f"{path}: invalid or unreadable JAR ({type(exc).__name__})")
    if owned == 0:
        errors.append("No Cultivation-owned classes in the JAR (wrong or empty artifact)")
    return owned, versions, errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path, help="Packaged raw Cultivation .jar")
    args = parser.parse_args(argv)
    owned, histogram, problems = inspect_jar(args.jar)
    print(f"Java 21 packaged-bytecode audit: {sum(histogram.values())} classes ({owned} owned)")
    print("Class major histogram: " + ", ".join(f"{v}={n}" for v, n in sorted(histogram.items())))
    if problems:
        for problem in problems:
            print("FAIL: " + problem, file=sys.stderr)
        return 1
    print("PASS: all packaged classes use stable Java 21-compatible bytecode")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
