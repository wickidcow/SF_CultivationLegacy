#!/usr/bin/env python3
"""Protect strict reads and startup order; executable YAML tests remain separate."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
main = (root / 'src/main/java/dev/sefiraat/cultivation/Cultivation.java').read_text()
manager = (root / 'src/main/java/dev/sefiraat/cultivation/managers/ConfigManager.java').read_text()
startup = main.split('public void onEnable() {', 1)[1].split('private void setupItems()', 1)[0]
load = startup.index('this.configManager = new ConfigManager();')
for marker in ['new ListenerManager()', 'new TaskManager()', 'new Registry()', 'setupItems();']:
    assert load < startup.index(marker), marker
assert 'if (this.configManager != null)' in main
assert 'YamlConfiguration.loadConfiguration(' not in manager
assert 'PlayerDataFile.load(file.toPath())' in manager
assert 'PlayerDataFile.load(existingFile.toPath())' in manager
assert 'PlayerDataFile.save(config, file.toPath())' in manager
assert 'PlayerDataFile.save(existingConfig, existingFile.toPath())' in manager
assert 'throw new IllegalStateException(' in manager
print('Player experience, codex and configuration preservation boundaries passed.')
