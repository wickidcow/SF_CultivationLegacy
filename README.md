# Cultivation Legacy

Cultivation Legacy is a maintained fork of **Cultivation**, the Slimefun addon originally created by **Sefiraat**, **J3fftw**, **JustAHuman**, and its contributors.

Cultivation is a spiritual successor to ExoticGarden and adds a large farming and food-production system to Slimefun, including custom plants, plant breeding, bushes, fruit trees, kitchen processing, foods, tools, and the Garden Cloche.

## Legacy fork goals

This fork focuses on keeping Cultivation usable on modern Minecraft and Slimefun Legacy while preserving the original gameplay, item IDs, recipes, and visual style wherever practical.

Current primary target:

- Minecraft 1.21.11+
- Paper 26.2+
- Slimefun Legacy
- Java 21 bytecode / modern server runtimes

## AlbionMC changes

The Legacy fork includes a few compatibility and quality-of-life changes:

- Cultivation plants may grow directly over vanilla farmland without requiring Crop Sticks.
- Crop Sticks remain supported and are still used for advanced crop and crossbreeding mechanics.
- Modern Slimefun block-data storage support is retained.
- Modern Minecraft material, particle, potion-effect, and display-entity compatibility fixes are incorporated where needed.
- Display entities are reused where possible instead of repeatedly removing and respawning them.
- GuizhanLibPlugin is not required.
- The maintained build is English-first.

## Dependencies

Required:

- Slimefun Legacy

Cultivation Legacy does **not** require ExoticGarden and does not require other Slimefun addons to run.

## Builds

GitHub Actions builds are named `SF_Cultivation1.1.x.jar`. The build workflow compiles against the released Slimefun Legacy JAR rather than a Gugu Slimefun dependency.

## Credits

All credit for the original Cultivation design and content belongs to the original authors and contributors, especially **Sefiraat**, **J3fftw**, and **JustAHuman**.

This repository exists as a compatibility and maintenance fork for modern Slimefun Legacy deployments and AlbionMC.com. It is not intended to erase or replace the work of the original project.

Additional compatibility fixes were reviewed from active community forks of Cultivation and selectively adapted where they improved modern Minecraft support without changing the addon's identity or saved-data formats.

## License

Cultivation is licensed under the GNU General Public License v3.0. See `LICENSE` for details.
