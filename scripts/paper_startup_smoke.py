#!/usr/bin/env python3
"""Disposable Paper 26.3 two-boot Cultivation smoke with legacy-format progress data.

This script intentionally never connects to, edits, or deploys on a real server.
It verifies plugin enablement and player-data persistence across clean shutdowns;
interactive breeding, machine recipes and Folia require separate integration tests.
"""
from __future__ import annotations

import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

TEST_PLAYER = "00000000-0000-4000-8000-000000000042"
DATA_ROOT = Path("plugins/Cultivation")
EXPERIENCE = (
    f"{TEST_PLAYER}:\n"
    "  HORTICULTURALIST:\n"
    "    EXP: 274\n"
    "  ORCHARDIST:\n"
    "    EXP: 117\n"
    "  legacy-unknown:\n"
    "    setting: keep-me\n"
)
CODEX = (
    f"{TEST_PLAYER}:\n"
    "  PLANT:\n"
    "    BREEDING:\n"
    "      CLT_PLANT_LEMON:\n"
    "        UNLOCKED: true\n"
    "      CLT_PLANT_OTHER:\n"
    "        UNLOCKED: false\n"
    "  legacy-custom-flag: leave-intact\n"
)
EXPECTED = {
    "exp.yml": [TEST_PLAYER, "HORTICULTURALIST:", "EXP: 274", "ORCHARDIST:", "EXP: 117",
                "legacy-unknown:", "setting: keep-me"],
    "codex.yml": [TEST_PLAYER, "BREEDING:", "CLT_PLANT_LEMON:", "UNLOCKED: true",
                  "CLT_PLANT_OTHER:", "UNLOCKED: false", "legacy-custom-flag: leave-intact"],
}
READY = re.compile(r"Done \([\d.]+s\)! For help")
ENABLES = (
    re.compile(r"Enabling Slimefun\b"),
    re.compile(r"Enabling Cultivation\b"),
)
FATAL = (
    re.compile(r"Error occurred while enabling (Slimefun|Cultivation)", re.IGNORECASE),
    re.compile(r"Could not load .*?(Slimefun|Cultivation)", re.IGNORECASE),
    re.compile(r"Could not pass event .*? to Cultivation", re.IGNORECASE),
)


def verify_saved_data(directory: Path) -> None:
    for name, expected in EXPECTED.items():
        path = directory / DATA_ROOT / name
        if not path.is_file():
            raise AssertionError(f"Missing persisted player data: {path}")
        data = path.read_text(encoding="utf-8")
        for token in expected:
            if token not in data:
                raise AssertionError(f"Player data {path} lost the value {token!r}")


def run_boot(directory: Path, number: int, timeout_seconds: int = 200) -> None:
    output = directory / f"cultivation-boot-{number}.stdout.log"
    with output.open("w", encoding="utf-8") as log:
        process = subprocess.Popen(
            ["java", "-Xms512M", "-Xmx2G", "-jar", "server.jar", "--nogui"],
            cwd=directory,
            stdin=subprocess.PIPE,
            stdout=log,
            stderr=subprocess.STDOUT,
            text=True,
        )
        try:
            deadline = time.monotonic() + timeout_seconds
            ready = False
            while time.monotonic() < deadline:
                if output.exists() and READY.search(output.read_text(encoding="utf-8", errors="replace")):
                    ready = True
                    break
                if process.poll() is not None:
                    break
                time.sleep(1)
            if not ready:
                raise AssertionError(f"Paper did not finish boot {number} within {timeout_seconds}s")
            if process.stdin is None:
                raise AssertionError("The Paper process has no console input")
            # Commands are delivered in order, and stop flushes all enabled plugins' data.
            process.stdin.write("plugins\nstop\n")
            process.stdin.flush()
            try:
                returncode = process.wait(timeout=65)
            except subprocess.TimeoutExpired as exc:
                raise AssertionError("Paper failed to stop cleanly") from exc
            if returncode != 0:
                raise AssertionError(f"Paper exited with status {returncode}")
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=10)

    content = output.read_text(encoding="utf-8", errors="replace")
    if not READY.search(content):
        raise AssertionError(f"Paper boot {number} did not reach a ready state")
    for enable in ENABLES:
        if not enable.search(content):
            raise AssertionError(f"Paper boot {number} did not enable expected plugin: {enable.pattern}")
    for fatal in FATAL:
        if fatal.search(content):
            raise AssertionError(f"Paper boot {number} reported a fatal plugin error: {fatal.pattern}")
    print(f"Paper boot {number}: ready, both plugins enabled, clean shutdown")


def main(argv: list[str]) -> int:
    if len(argv) != 4:
        print("Usage: paper_startup_smoke.py <paper.jar> <slimefun.jar> <cultivation.jar> <work-dir>",
              file=sys.stderr)
        return 2
    paper, slimefun, cultivation, root = map(Path, argv)
    for jar in (paper, slimefun, cultivation):
        if not jar.is_file() or jar.stat().st_size < 1024:
            raise FileNotFoundError(f"Missing or invalid JAR: {jar}")
    root.mkdir(parents=True, exist_ok=True)
    (root / "plugins").mkdir(exist_ok=True)
    (root / DATA_ROOT).mkdir(parents=True, exist_ok=True)
    shutil.copyfile(paper, root / "server.jar")
    shutil.copyfile(slimefun, root / "plugins/Slimefun-Legacy.jar")
    shutil.copyfile(cultivation, root / "plugins/SF_Cultivation.jar")
    (root / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (root / "server.properties").write_text(
        "online-mode=false\nmax-players=1\nview-distance=2\nsimulation-distance=2\n"
        "spawn-protection=0\nenable-query=false\nenable-rcon=false\n",
        encoding="utf-8",
    )
    (root / DATA_ROOT / "exp.yml").write_text(EXPERIENCE, encoding="utf-8")
    (root / DATA_ROOT / "codex.yml").write_text(CODEX, encoding="utf-8")
    for boot in (1, 2):
        run_boot(root, boot)
        verify_saved_data(root)
    print("Cultivation: both startup cycles and legacy YAML values preserved")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
