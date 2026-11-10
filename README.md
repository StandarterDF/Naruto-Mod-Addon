# Naruto Mod Addon

An addon for **Sekwah41's Naruto Mod** on **Minecraft 1.20.1 / Forge 47.x**.
It adds extra techniques, items, clans/ranks content and a set of balance tweaks on top of the
base mod, re-created from the 1.11.0 feature set.

The addon does **not** contain the base mod. It is a normal Forge mod that requires NarutoMod and
SekCLib to be installed next to it.

## Requirements

| Mod | Version | Note |
| --- | --- | --- |
| Minecraft | 1.20.1 | Forge 46–47 |
| [Sekwah41's Naruto Mod](https://www.curseforge.com/minecraft/mc-mods/sekwah41s-naruto-mod) | 1.20-1.8.1 | required, not bundled |
| [SekCLib](https://www.curseforge.com/minecraft/mc-mods/sekclib) | 1.0.x | required, not bundled |

Drop the three jars into `mods/` and start the game. Client and server use the same jar
(`NarutoModAddon-1.20-<version>-universal.jar`).

## Building

JDK 17 is required (Gradle 8.1.1 does not run on newer JDKs).

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
.\gradlew.bat build
```

The output is `build/libs/NarutoModAddon-1.20-<version>-universal.jar`.

The base mod is used only as a compile dependency. `build.gradle` takes it from
`libs/NarutoMod-1.20-1.8.1-deobf.jar` when that file exists (offline development), and otherwise
pulls the official CurseForge file through `curse.maven`. Repositories under `libs/` are never
committed.

Development runs: `run-dev-client.bat` / `run-dev-server.bat` (they find JDK 17 themselves and work
offline).

## Third-party content

This is not a standalone mod and cannot be used without NarutoMod. All game content it references -
item ids, entity ids, registries, the chakra/stamina capability, the ability registry - lives in the
base mod.

`ASSETS.md` documents exactly which files in `src/main/resources` are byte-identical to files from
the base mod, how that list was produced (`tools/check_base_asset_copies.ps1`) and how it is kept
small. In short:

* Textures that the base mod already ships are **not** copied here any more. Item models and
  renderers point at `narutomod:` directly; the 1.8.1 jar provides them at runtime.
* Files that only exist in the 1.11.0 content set (items such as food, bone weapons, scrolls, the
  headband item icons and the fork-only `assets/narutomod/**` models/recipes/blockstates that the
  addon back-fills into the base mod's namespace) are still shipped, because the 1.8.1 jar does not
  contain them and an addon cannot draw what is not there.
* NarutoMod itself is licensed **All rights reserved** by Sekwah41. This repository's MIT license
  covers the addon's own code only. If you plan to redistribute a build, keep the base mod as a
  dependency (never bundle it) and credit Sekwah41.

## License

MIT - see [LICENSE](LICENSE). The license covers the addon's own source code and does not extend to
NarutoMod, SekCLib, Minecraft or Forge.
