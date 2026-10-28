<div align="center">

# 🌾 Cultivation — Slimefun Legacy

**Plants, breeding, fruit trees, food production, and farming progression for Slimefun.**

![Slimefun Legacy](https://img.shields.io/badge/Slimefun-Legacy-6bd425?style=for-the-badge)
![Paper 1.21.11+](https://img.shields.io/badge/Paper-1.21.11%2B-blue?style=for-the-badge)
![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)
![Maintained for AlbionMC.com](https://img.shields.io/badge/Maintained%20for-albionmc.com-7b68ee?style=for-the-badge)

</div>

> [!IMPORTANT]
> Cultivation Legacy is an **unofficial community-maintained continuation**. It is maintained for **Slimefun Legacy** and developed for use on **albionmc.com**, while preserving the original project's gameplay, IDs, recipes, and history wherever practical.

## 🌱 What does Cultivation do?

Cultivation is a large farming and food-production addon and a spiritual successor to ExoticGarden. It expands Slimefun with a deeper agricultural progression system built around:

- custom plants and crops;
- plant breeding and crossbreeding;
- bushes and fruit trees;
- kitchen processing and custom foods;
- farming tools and resources;
- Crop Sticks and advanced crop mechanics;
- the **Garden Cloche** and related automation.

The goal is to make agriculture feel like a complete Slimefun progression path rather than a small collection of extra crops.

## 🧪 Slimefun Legacy maintenance

Primary maintenance target:

- **Minecraft/Paper minimum:** 1.21.11
- **API compilation checks:** 1.21.11, 26.1, 26.2 and 26.3
- **Runtime integration gates:** Paper 1.21.11 on Java 21 and Paper 26.3 on Java 25
- **Validated dependency:** Slimefun Legacy 4.1.78
- **Plugin bytecode:** Java 21; use the Java runtime required by the selected server version

Version **1.1.24** completes the Adventure-backed SefiLib Theme migration while retaining the old public binary API for existing consumers. It also preserves custom RGB colors in plant display items, Seed Packs and recipe scrolls. See [release notes](docs/releases/1.1.24.md) and the [migration and validation report](docs/deprecation-audit.md).

The runtime probes exercise packaged item metadata, breeding-pair logic, Seed Pack menus/persistence, kitchen recipe lookup and save-data recovery. They are not a claim of exhaustive connected-player gameplay testing. **Folia support is not newly enabled or certified by this release.** Purpur/Leaf-specific runtime behavior requires its own validation.

Legacy maintenance includes:

- allowing Cultivation plants to grow directly over vanilla farmland where supported;
- retaining Crop Sticks for advanced crop and crossbreeding mechanics;
- preserving modern Slimefun block-data storage behavior;
- updating materials, particles, potion effects, and display-entity handling for modern Paper;
- reusing display entities where practical instead of repeatedly respawning them;
- keeping the maintained build English-first;
- removing the need for GuizhanLibPlugin in this maintenance line;
- selectively adapting useful community-fork fixes without replacing Cultivation's identity or saved-data formats wholesale.

Cultivation Legacy does **not** require ExoticGarden to run.

## ❤️ Credits & project lineage

Cultivation exists because of the original developers and the community that continued maintaining it.

- **Sefiraat** — original Cultivation project creator and lead developer, and SefiLib author.
- **J3fftw / J3fftw1** — original project development and contribution.
- **JustAHuman** — original project development and contribution.
- **Sefiraat/Cultivation** — original source repository and gameplay foundation.
- **SlimefunGuguProject/Cultivation** — immediate upstream fork from which this repository was forked and a source of later compatibility work.
- **Cultivation and Slimefun community contributors** — fixes, maintenance, APIs, testing, and documentation over the project's lifetime.
- **wickidcow / Slimefun Legacy** — current compatibility and preservation work for modern servers and albionmc.com.

This maintenance fork is intended to **honor and preserve** the original work, not to take credit for it.

## 📜 GNU General Public License v3.0

Cultivation is licensed under the **GNU General Public License v3.0 (GPLv3)**. The complete license text is included in `LICENSE` and remains controlling if this summary differs from it.

If you distribute this GPL-covered project or a modified version, follow the GPLv3 requirements that apply to your distribution, including preserving applicable notices, licensing covered modified source under GPLv3, and making the required **Corresponding Source** available when distributing object-code builds. Modified versions should be clearly identified as modified.

The adapted SefiLib Theme source is included under `src/main/java/dev/sefiraat/sefilib/string/Theme.java`, with its origin and modifications recorded in `THIRD_PARTY_NOTICES.txt` inside the JAR.

This software is provided **without warranty** to the extent described by GPLv3.

## ⚖️ Independence & trademark notice

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

Cultivation Legacy, Slimefun Legacy, and this repository are independent community projects. They are not sponsored, endorsed, approved, or operated by Mojang Studios or Microsoft. Minecraft-related names, brands, and assets remain the property of their respective rights holders.

This fork is also not represented as an official release of Sefiraat, J3fftw, JustAHuman, the SlimefunGuguProject, the original Slimefun team, or other upstream contributors unless explicitly stated by those parties.

---

<div align="center">

**🌾 Grow it. Breed it. Cook it. Keep Cultivation alive. 🌱**

</div>
