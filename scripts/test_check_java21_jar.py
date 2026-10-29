"""Real CLI tests of the raw Cultivation distributable bytecode gate."""
from __future__ import annotations

import hashlib
import struct
import subprocess
import sys
import tempfile
import unittest
import warnings
import zipfile
from pathlib import Path

CHECKER = Path(__file__).with_name("check_java21_jar.py")
OWNED = "dev/sefiraat/cultivation/Cultivation.class"
LOCAL_THEME = "dev/sefiraat/sefilib/string/Theme.class"


def class_bytes(major: int = 65, minor: int = 0, magic: bytes = b"\xca\xfe\xba\xbe") -> bytes:
    return magic + struct.pack(">HH", minor, major) + b"synthetic-fixture"


class Java21JarGateTests(unittest.TestCase):
    def check(self, entries: list[tuple[str, bytes]], *, expected: int) -> str:
        with tempfile.TemporaryDirectory() as directory:
            jar = Path(directory) / "candidate.jar"
            with zipfile.ZipFile(jar, "w") as archive:
                for name, content in entries:
                    with warnings.catch_warnings():
                        warnings.simplefilter("ignore", UserWarning)
                        archive.writestr(name, content)
            before = hashlib.sha256(jar.read_bytes()).hexdigest()
            completed = subprocess.run(
                [sys.executable, str(CHECKER), str(jar)],
                capture_output=True, text=True, check=False,
            )
            self.assertEqual(expected, completed.returncode, completed.stdout + completed.stderr)
            self.assertEqual(before, hashlib.sha256(jar.read_bytes()).hexdigest())
            return completed.stdout + completed.stderr

    def test_java21_and_shaded_java17_pass(self):
        output = self.check([(OWNED, class_bytes()), ("com/shaded/Library.class", class_bytes(61))], expected=0)
        self.assertIn("PASS", output)

    def test_java21_supported_previous_minor_legacy_class_passes(self):
        self.check([(OWNED, class_bytes()), ("com/shaded/Old.class", class_bytes(55, 123))], expected=0)

    def test_local_theme_counts_as_owned(self):
        self.check([(LOCAL_THEME, class_bytes())], expected=0)

    def test_java22_rejected(self):
        output = self.check([(OWNED, class_bytes(66))], expected=1)
        self.assertIn("66.0", output)
        self.assertNotIn("PASS", output)

    def test_java21_preview_rejected(self):
        output = self.check([(OWNED, class_bytes(65, 65535))], expected=1)
        self.assertIn("preview", output)

    def test_java17_preview_rejected(self):
        output = self.check([(OWNED, class_bytes(61, 65535))], expected=1)
        self.assertIn("preview", output)

    def test_invalid_modern_minor_rejected(self):
        output = self.check([(OWNED, class_bytes(65, 1))], expected=1)
        self.assertIn("invalid modern minor", output)

    def test_shaded_preview_rejected(self):
        output = self.check([(OWNED, class_bytes()), ("com/shaded/Lib.class", class_bytes(65, 65535))], expected=1)
        self.assertIn("Lib.class", output)

    def test_unsupported_old_major_rejected(self):
        self.check([(OWNED, class_bytes(44))], expected=1)

    def test_missing_owned_class_rejected(self):
        self.check([("com/shaded/Lib.class", class_bytes())], expected=1)

    def test_wrong_magic_rejected(self):
        self.check([(OWNED, class_bytes(magic=b"WRNG"))], expected=1)

    def test_truncated_header_rejected(self):
        self.check([(OWNED, b"\xca\xfe\xba\xbe\x00")], expected=1)

    def test_duplicate_class_member_rejected(self):
        self.check([(OWNED, class_bytes()), (OWNED, class_bytes())], expected=1)

    def test_mixed_bad_class_rejected_and_path_reported(self):
        output = self.check([(OWNED, class_bytes()), ("com/shaded/X.class", class_bytes(66))], expected=1)
        self.assertIn("com/shaded/X.class", output)

    def test_no_class_entries_rejected(self):
        self.check([("plugin.yml", b"name: Cultivation\n")], expected=1)

    def test_corrupt_zip_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            jar = Path(directory) / "invalid.jar"
            jar.write_bytes(b"not a jar")
            completed = subprocess.run([sys.executable, str(CHECKER), str(jar)], capture_output=True, text=True)
            self.assertEqual(1, completed.returncode)
            self.assertIn("invalid or unreadable JAR", completed.stderr)


if __name__ == "__main__":
    unittest.main()
