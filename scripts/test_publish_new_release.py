"""Release gate regression: no automatic publishing or modification of old releases."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from zipfile import ZipFile

SCRIPT = Path(__file__).with_name("publish_new_release.sh")
SHA = "a" * 40

class ImmutableCultivationReleaseTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        root = Path(self.directory.name)
        self.log = root / "commands.log"
        dist = root / "dist"
        dist.mkdir()
        self.jar = dist / "SF_Cultivation1.1.24.jar"
        with ZipFile(self.jar, "w") as out:
            out.writestr("plugin.yml", "name: Cultivation\\nversion: '1.1.24'\\napi-version: '1.21.11'\\n")
        notes = root / "docs" / "releases"
        notes.mkdir(parents=True)
        (notes / "1.1.24.md").write_text("Test release notes.\\n", encoding="utf-8")
        gh = root / "gh"
        gh.write_text(
            "#!/usr/bin/env python3\\n"
            "import os, sys\\n"
            "args=sys.argv[1:]\\n"
            "with open(os.environ['TEST_LOG'], 'a') as f:f.write('gh '+ ' '.join(args)+'\\\\n')\\n"
            "if args[:2]==['api', 'repos/wickidcow/SF_CultivationLegacy/git/ref/heads/main']:\\n"
            " print(os.environ['GITHUB_SHA'] if os.environ.get('TEST_MOVED_MAIN')!='true' else 'b'*40);sys.exit(0)\\n"
            "if args[:2]==['release', 'view']:sys.exit(0 if os.environ.get('TEST_EXISTING_RELEASE')=='true' else 1)\\n"
            "if args[:2]==['release', 'create']:sys.exit(0)\\n"
            "sys.exit(98)\\n", encoding="utf-8")
        gh.chmod(0o755)
        git = root / "git"
        git.write_text(
            "#!/usr/bin/env python3\\n"
            "import os, sys\\n"
            "with open(os.environ['TEST_LOG'], 'a') as f:f.write('git '+' '.join(sys.argv[1:])+'\\\\n')\\n"
            "sys.exit(128 if os.environ.get('TEST_TAG_NETWORK_FAIL')=='true' else (0 if os.environ.get('TEST_EXISTING_TAG')=='true' else 2))\\n",
            encoding="utf-8")
        git.chmod(0o755)
        self.cwd = root
        self.env = {**os.environ, "PATH": str(root)+os.pathsep+os.environ.get("PATH", ""),
                    "TEST_LOG": str(self.log),
                    "GITHUB_REPOSITORY": "wickidcow/SF_CultivationLegacy",
                    "GITHUB_EVENT_NAME": "workflow_dispatch",
                    "GITHUB_REF": "refs/heads/main",
                    "GITHUB_REF_NAME": "main",
                    "RELEASE_CONFIRMED": "true",
                    "CONFIRM_VERSION": "1.1.24",
                    "VERSION": "1.1.24",
                    "OUTPUT_NAME": self.jar.name,
                    "JAR": "dist/"+self.jar.name,
                    "GITHUB_SHA": SHA,
                    "EXPECTED_SOURCE_SHA": SHA}

    def invoke(self, **overrides):
        env={**self.env,**overrides}
        result=subprocess.run(["bash",str(SCRIPT.resolve())],cwd=self.cwd,
                             env=env, capture_output=True,text=True,check=False)
        commands=self.log.read_text().splitlines() if self.log.exists() else []
        return result,commands

    def test_distinct_approved_version_creates_only_once(self):
        result, commands = self.invoke()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(1, len([x for x in commands if x.startswith("gh release create ")]))
        self.assertFalse(any("delete" in x or "--clobber" in x for x in commands))
        self.assertTrue(any("git ls-remote" in x for x in commands))

    def test_existing_tag_and_release_are_never_mutated(self):
        for option in ("TEST_EXISTING_RELEASE", "TEST_EXISTING_TAG"):
            with self.subTest(option=option):
                if self.log.exists(): self.log.unlink()
                result, commands=self.invoke(**{option:"true"})
                self.assertNotEqual(result.returncode,0)
                self.assertFalse(any("release create" in x for x in commands))

    def test_refusal_after_git_lookup_network_error(self):
        result, commands=self.invoke(TEST_TAG_NETWORK_FAIL="true")
        self.assertNotEqual(result.returncode,0)
        self.assertIn("Cannot verify tag absence",result.stderr)
        self.assertFalse(any("release create" in x for x in commands))

    def test_only_explicit_matching_main_dispatch_may_publish(self):
        cases=[
          {"GITHUB_EVENT_NAME":"push"},
          {"GITHUB_EVENT_NAME":"schedule"},
          {"GITHUB_REF":"refs/tags/v1.1.24"},
          {"GITHUB_REF_NAME":"topic"},
          {"RELEASE_CONFIRMED":"false"},
          {"CONFIRM_VERSION":"1.1.23"},
          {"EXPECTED_SOURCE_SHA":"b"*40},
          {"GITHUB_REPOSITORY":"someone/other"},
          {"GITHUB_SHA":"bad"},
        ]
        for case in cases:
            with self.subTest(case=case):
                if self.log.exists():self.log.unlink()
                result,commands=self.invoke(**case)
                self.assertNotEqual(result.returncode,0)
                self.assertFalse(any("release create" in x for x in commands))

    def test_stale_master_is_rejected(self):
        result, commands=self.invoke(TEST_MOVED_MAIN="true")
        self.assertNotEqual(result.returncode,0)
        self.assertFalse(any("release create" in x for x in commands))

    def test_missing_or_tampered_jar_is_rejected(self):
        with ZipFile(self.jar,"w") as out:
            out.writestr("plugin.yml","version: 1.1.23\\n")
        result, commands=self.invoke()
        self.assertNotEqual(result.returncode,0)
        self.assertFalse(any("release create" in x for x in commands))
        self.jar.unlink()
        result, commands=self.invoke()
        self.assertNotEqual(result.returncode,0)

if __name__=="__main__":
    unittest.main()
