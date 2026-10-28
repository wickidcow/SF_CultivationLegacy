"""Tests of the zero-deprecation gate, including formerly allowed SefiLib calls."""
import tempfile
import unittest
from pathlib import Path
from verify_deprecation_baseline import audit_log, main


class DeprecationBaselineTests(unittest.TestCase):
    def test_old_color_warning_now_fails(self):
        warning = "[WARNING] /tmp/src/main/java/dev/sefiraat/cultivation/api/utils/CultivationThemes.java:[12,57] of(java.lang.String) in net.md_5.bungee.api.ChatColor has been deprecated"
        self.assertEqual(1, len(audit_log(warning + "\n" + warning)[1]))

    def test_itemmeta_warning_fails(self):
        warning = "[WARNING] /tmp/src/main/java/CultivationTree.java:[42,17] getLore() has been deprecated"
        self.assertEqual(1, len(audit_log(warning)[1]))

    def test_for_removal_fails(self):
        warning = "[WARNING] /tmp/src/main/java/Theme.java:[35,20] API deprecated and marked for removal"
        self.assertEqual(1, len(audit_log(warning)[1]))

    def test_ansi_windows_and_javac_forms_fail(self):
        warning = "\x1b[33mC:\\work\\src\\main\\java\\Theme.java:12: warning: [deprecation] old API\x1b[0m"
        self.assertEqual(1, len(audit_log(warning)[1]))

    def test_dependency_noise_not_confused_with_source(self):
        noise = "[WARNING] bungeecord-chat:1.21-R0.2-deprecated was downloaded from a mirror\n[WARNING] overlapping classes\n[INFO] BUILD SUCCESS"
        self.assertEqual((0, []), audit_log(noise))

    def test_empty_log_not_accepted_as_success(self):
        with tempfile.TemporaryDirectory() as temp:
            file = Path(temp) / "build.log"
            file.write_text("", encoding="utf-8")
            self.assertEqual(1, main([str(file)]))
            file.write_text("[INFO] BUILD SUCCESS", encoding="utf-8")
            self.assertEqual(0, main([str(file)]))


if __name__ == "__main__":
    unittest.main()
