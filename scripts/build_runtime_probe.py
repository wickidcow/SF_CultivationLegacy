"""Build a CI-only plugin against the packaged addon, never include it in releases."""
from __future__ import annotations

import os
import subprocess
import zipfile
from collections import Counter
from pathlib import Path


def build_probe(cultivation: Path, slimefun: Path, directory: Path) -> Path:
    directory = directory.resolve()
    directory.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(cultivation) as jar:
        names = jar.namelist()
        counts = Counter(names)
        if counts['dev/sefiraat/sefilib/string/Theme.class'] != 1:
            raise AssertionError('Packaged JAR must contain exactly one modern Theme.class')
        for name in names:
            if name.startswith(('org/junit/', 'org/mockito/', 'org/opentest4j/', 'ci/cultivation/')):
                raise AssertionError(f'Test-only class leaked into the release JAR: {name}')
        for required in ('licenses/LICENSE', 'THIRD_PARTY_NOTICES.txt'):
            if required not in names:
                raise AssertionError(f'Missing third-party license/notice: {required}')
    classpath_file = directory / 'classpath.txt'
    subprocess.run(['mvn', '-B', '-q', 'dependency:build-classpath',
                    f'-Dmdep.outputFile={classpath_file}'], check=True, timeout=300)
    classpath = os.pathsep.join((str(cultivation.resolve()), str(slimefun.resolve()),
                                classpath_file.read_text(encoding='utf-8').strip()))
    classes = directory / 'classes'
    classes.mkdir(exist_ok=True)
    source = Path(__file__).resolve().parent / 'integration' / 'RuntimeProbe.java'
    subprocess.run(['javac', '--release', '21', '-encoding', 'UTF-8', '-cp', classpath,
                    '-d', str(classes), str(source)], check=True, timeout=120)
    target = directory / 'CultivationRuntimeProbe.jar'
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as jar:
        jar.writestr('plugin.yml', 'name: CultivationRuntimeProbe\nversion: 1.0\n'
                     'main: ci.cultivation.RuntimeProbe\napi-version: \'1.21.11\'\n'
                     'depend: [Slimefun, Cultivation]\n')
        for file in classes.rglob('*.class'):
            jar.write(file, str(file.relative_to(classes)))
    return target
