#!/usr/bin/env python3
"""Disposable Paper runtime tests with a separate, never-distributed probe plugin.

Two normal boots exercise registered items, metadata, menus and recipe logic;
a third boot verifies malformed progress data fails closed. This does not
simulate a connected player or certify Folia's region scheduler.
"""
from __future__ import annotations

import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

from build_runtime_probe import build_probe

TEST_PLAYER = "00000000-0000-4000-8000-000000000042"
DATA_ROOT = Path("plugins/Cultivation")
EXPERIENCE = (
    f"{TEST_PLAYER}:\n"
    "  HORTICULTURALIST:\n    EXP: 274\n"
    "  ORCHARDIST:\n    EXP: 117\n"
    "  legacy-unknown:\n    setting: keep-me\n"
)
CODEX = (
    f"{TEST_PLAYER}:\n"
    "  PLANT:\n    BREEDING:\n"
    "      CLT_PLANT_LEMON:\n        UNLOCKED: true\n"
    "      CLT_PLANT_OTHER:\n        UNLOCKED: false\n"
    "  legacy-custom-flag: leave-intact\n"
)
EXPECTED = {
    "exp.yml": [TEST_PLAYER, "HORTICULTURALIST:", "EXP: 274", "ORCHARDIST:", "EXP: 117",
                "legacy-unknown:", "setting: keep-me"],
    "codex.yml": [TEST_PLAYER, "BREEDING:", "CLT_PLANT_LEMON:", "UNLOCKED: true",
                  "CLT_PLANT_OTHER:", "UNLOCKED: false", "legacy-custom-flag: leave-intact"],
}
READY = re.compile(r"Done \([\d.]+s\)! For help")
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


def run_boot(directory: Path, number: int, timeout_seconds: int = 200,
             expected_invalid_codex: bool = False) -> None:
    output = directory / f"cultivation-boot-{number}.stdout.log"
    with output.open("w", encoding="utf-8") as log:
        process = subprocess.Popen(
            ["java", "-Xms512M", "-Xmx2G", "-jar", "server.jar", "--nogui"],
            cwd=directory, stdin=subprocess.PIPE, stdout=log,
            stderr=subprocess.STDOUT, text=True,
        )
        try:
            deadline = time.monotonic() + timeout_seconds
            ready = False
            content = ""
            while time.monotonic() < deadline:
                content = output.read_text(encoding="utf-8", errors="replace")
                if "CULTIVATION_PROBE_FAIL" in content:
                    raise AssertionError("Packaged addon integration probe failed:\n" + content[-12000:])
                probe_done = expected_invalid_codex or "CULTIVATION_PROBE_PASS" in content
                if READY.search(content) and probe_done:
                    ready = True
                    break
                if process.poll() is not None:
                    break
                time.sleep(1)
            if not ready:
                raise AssertionError(f"Boot {number} did not pass startup/probe within {timeout_seconds}s:\n"
                                     + content[-12000:])
            if process.stdin is None:
                raise AssertionError("The Paper process has no console input")
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
    if not READY.search(content) or not re.search(r"Enabling Slimefun\b", content):
        raise AssertionError(f"Paper boot {number} did not reach readiness with Slimefun")
    if expected_invalid_codex:
        if "Unable to load codex.yml" not in content:
            raise AssertionError("Malformed codex did not trigger the strict data-load failure")
        if not re.search(r"Error occurred while enabling Cultivation", content, re.IGNORECASE):
            raise AssertionError("Cultivation did not fail closed on malformed saved codex")
        if re.search(r"Error occurred while enabling Slimefun", content, re.IGNORECASE):
            raise AssertionError("Slimefun unexpectedly failed during the negative test")
        if "CULTIVATION_PROBE_PASS" in content:
            raise AssertionError("Probe must not pass when its Cultivation dependency is disabled")
        print(f"Paper boot {number}: malformed codex rejected; server stopped cleanly", flush=True)
        return
    if not re.search(r"Enabling Cultivation\b", content):
        raise AssertionError(f"Paper boot {number} did not enable Cultivation")
    for fatal in FATAL:
        if fatal.search(content):
            raise AssertionError(f"Paper boot {number} reported a fatal plugin error: {fatal.pattern}")
    for line in content.splitlines():
        if "CULTIVATION_PROBE_PASS" in line:
            print(f"Boot {number}: {line}", flush=True)
    print(f"Paper boot {number}: both plugins enabled, runtime probe passed, clean shutdown", flush=True)


def main(argv: list[str]) -> int:
    if len(argv) != 4:
        print("Usage: paper_startup_smoke.py <paper.jar> <slimefun.jar> <cultivation.jar> <empty-work-dir>",
              file=sys.stderr)
        return 2
    paper, slimefun, cultivation, root = map(Path, argv)
    for jar in (paper, slimefun, cultivation):
        if not jar.is_file() or jar.stat().st_size < 1024:
            raise FileNotFoundError(f"Missing or invalid JAR: {jar}")
    # Never overwrite a previously existing world/server, even if invoked manually.
    if root.exists() and any(root.iterdir()):
        raise FileExistsError(f"Refusing to modify a non-empty server directory: {root}")
    root.mkdir(parents=True, exist_ok=True)
    probe = build_probe(cultivation, slimefun, root.parent / "probe-build")
    (root / "plugins").mkdir(exist_ok=True)
    (root / DATA_ROOT).mkdir(parents=True, exist_ok=True)
    shutil.copyfile(paper, root / "server.jar")
    shutil.copyfile(slimefun, root / "plugins/Slimefun-Legacy.jar")
    shutil.copyfile(cultivation, root / "plugins/SF_Cultivation.jar")
    shutil.copyfile(probe, root / "plugins/CultivationRuntimeProbe.jar")
    (root / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (root / "server.properties").write_text(
        "server-ip=127.0.0.1\nonline-mode=false\nmax-players=1\nview-distance=2\nsimulation-distance=2\n"
        "spawn-protection=0\nenable-query=false\nenable-rcon=false\n",
        encoding="utf-8",
    )
    (root / DATA_ROOT / "exp.yml").write_text(EXPERIENCE, encoding="utf-8")
    (root / DATA_ROOT / "codex.yml").write_text(CODEX, encoding="utf-8")
    for boot in (1, 2):
        run_boot(root, boot)
        verify_saved_data(root)

    exp_path = root / DATA_ROOT / "exp.yml"
    codex_path = root / DATA_ROOT / "codex.yml"
    exp_before = exp_path.read_bytes()
    invalid_codex = b"legacy-player:\n  PLANT: [unfinished\n"
    codex_path.write_bytes(invalid_codex)
    # The negative test deliberately disables Cultivation. Remove the CI-only
    # consumer before that boot, rather than testing it against a disabled API.
    # All positive integration assertions ran on both preceding real boots.
    (root / "plugins/CultivationRuntimeProbe.jar").unlink()
    run_boot(root, 3, expected_invalid_codex=True)
    if codex_path.read_bytes() != invalid_codex:
        raise AssertionError("Malformed codex changed during failed plugin startup/shutdown")
    if exp_path.read_bytes() != exp_before:
        raise AssertionError("Unrelated experience file changed during failed codex startup")
    print("Cultivation: runtime probes and restarts passed; malformed codex failed closed without data loss")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
