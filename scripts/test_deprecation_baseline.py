"""Regression checks for the compile warning gate."""
import unittest
from verify_deprecation_baseline import audit_log


class DeprecationBaselineTests(unittest.TestCase):
    def test_existing_sefilib_color_warnings_are_tracked(self):
        old = (
            "[WARNING] /tmp/repo/src/main/java/dev/sefiraat/cultivation/api/utils/"
            "CultivationThemes.java:[12,57] of(java.lang.String) in "
            "net.md_5.bungee.api.ChatColor has been deprecated"
        )
        count, unexpected = audit_log(old + "\n" + old)
        self.assertEqual(1, count)
        self.assertEqual([], unexpected)

    def test_new_deprecated_itemmeta_calls_fail(self):
        current = (
            "[WARNING] /tmp/repo/src/main/java/dev/sefiraat/cultivation/api/"
            "slimefun/items/trees/CultivationTree.java:[42,17] "
            "getLore() in org.bukkit.inventory.meta.ItemMeta has been deprecated"
        )
        count, unexpected = audit_log(current)
        self.assertEqual(0, count)
        self.assertEqual(1, len(unexpected))

    def test_future_removal_warning_fails_even_in_allowlisted_file(self):
        future = (
            "[WARNING] /tmp/repo/src/main/java/dev/sefiraat/cultivation/api/utils/"
            "CultivationThemes.java:[12,48] "
            "net.md_5.bungee.api.ChatColor is deprecated and marked for removal"
        )
        count, unexpected = audit_log(future)
        self.assertEqual(0, count)
        self.assertEqual(1, len(unexpected))

    def test_new_warning_in_same_file_fails(self):
        current = (
            "[WARNING] /tmp/repo/src/main/java/dev/sefiraat/cultivation/api/utils/"
            "CultivationThemes.java:[35,20] "
            "getLore() in org.bukkit.inventory.meta.ItemMeta has been deprecated"
        )
        count, unexpected = audit_log(current)
        self.assertEqual(0, count)
        self.assertEqual(1, len(unexpected))

    def test_shade_and_repository_messages_are_not_java_deprecations(self):
        noise = (
            "[WARNING] bungeecord-chat:1.21-R0.2-deprecated was downloaded from a mirror\n"
            "[WARNING] maven-shade-plugin has detected overlapping classes\n"
            "[INFO] BUILD SUCCESS\n"
        )
        self.assertEqual((0, []), audit_log(noise))


if __name__ == "__main__":
    unittest.main()
