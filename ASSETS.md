# Third-party assets in `src/main/resources`

This addon extends **Sekwah41's Naruto Mod** (CurseForge project `sekwah41s-naruto-mod`, jar
`NarutoMod-1.20-1.8.1`). The base mod is licensed **All rights reserved**. This file records which
of the addon's resource files are byte-identical to files from the base mod, so the amount of
somebody else's content in a build is known and stays as small as possible.

Regenerate the table at any time:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/check_base_asset_copies.ps1
# -> summary + tmp/base_asset_copies.csv
```

The script compares every file under `src/main/resources` by MD5 against the entries of
`libs/NarutoMod-1.20-1.8.1-deobf.jar` (the official 1.8.1 release) and
`libs/maven/com/sekwah/narutomod/1.11.0/narutomod-1.11.0.jar` (the 1.11.0 content set this addon
re-creates). Neither jar is committed; see `build.gradle` for how they are obtained.

## Current state (after the cleanup in this commit)

| Metric | Files |
| --- | --- |
| Resource files in the addon | 552 |
| Content identical to a base-mod file | 193 |
| … of those, also present in the official 1.8.1 jar | 4 |
| … of those, present only in the 1.11.0 content set | 189 |
| Shipped under the same resource path as the base mod (i.e. they patch base resources) | 82 |

Where the 193 live:

| Path | Count | Why it is there |
| --- | --- | --- |
| `assets/narutomod/models/**` | 51 | item/weapon models the 1.8.1 jar does not have; the items are registered into the base mod's namespace |
| `data/narutoaddon/recipes/**` | 31 | crafting recipes for addon items (same shape as the base mod's recipes) |
| `assets/narutomod/textures/**` | 26 | art for items that exist only in the 1.11.0 content set (scrolls, food, bone weapons, seals) |
| `assets/narutoaddon/textures/**` | 26 | icons of addon items whose art is the 1.11.0 art |
| `assets/narutoaddon/models/**` | 23 | item models that now point at `narutomod:` textures and therefore equal the base ones |
| `data/narutomod/recipes/**` | 19 | balance overrides of base recipes (same path, different numbers than 1.8.1) |
| `data/narutoaddon/damage_type/**` | 9 | boilerplate damage-type JSON, identical by nature |
| other (`tags`, `META-INF/accesstransformer.cfg`, `sounds`, `blockstates`) | 8 | tags/AT/audio needed for the base mod's own content |

## What was removed

* **19 textures** that the 1.8.1 jar already ships: all headband item icons, `paper_bomb`,
  `paperbomb`, `paperbomb_trans`, `bonsaitree`, `senbon` and the kunai projectile texture. The
  models and renderers now reference `narutomod:` directly instead of re-shipping a copy
  (`MinatoKunaiRenderer` was the only code change).
* **`narutologo.png`** (the base mod's logo, never referenced by the addon).

## What cannot be removed

The remaining files are the 1.11.0 content set: the 1.8.1 jar simply does not contain those items,
models, recipes or icons, and an addon cannot reference art that does not exist in the game. They
are the same files the addon's own 1.11.0 fork ships.

## Rules for new content

1. Before adding a texture or model, check whether the base mod already has it - if it does, point
   the model at `narutomod:<path>` and do not copy the file.
2. Run `tools/check_base_asset_copies.ps1` before a release; the "identical to a base-mod file"
   count should only grow when genuinely new 1.11.0 content is ported.
3. Never bundle the NarutoMod or SekCLib jar, and never copy base-mod classes into this addon - it is
   a compile-time dependency only.
4. Texture paths use the legacy pluralised folders the base mod uses (`textures/items/...`), so a
   `narutomod:` reference and a local copy always have the same layout.
