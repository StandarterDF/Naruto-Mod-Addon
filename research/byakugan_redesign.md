# Переработка Бьякугана: 3 ветки-глаза (контракт для исполнителей)

Статус: **реализовано в коде** (см. раздел «Статус» в конце). Источник правды по именам, комбо, веткам и гейтам.
Джоуган удаляется полностью, кроме трёх техник: `dimension_step` (переписывается в порталы),
`sewing_pocket`, `sewing_lock`.

## Ветки дерева

В `SkillBranch` остаются/добавляются:

| enum | id глава | ветка | базовый глаз (`ability:`) |
| --- | --- | --- | --- |
| `BYAKUGAN` (есть) | `naruto.gui.skill.branch.byakugan` | 1. Бьякуган — тайдзюцу и контроль тела | `byakugan` |
| `DHARMA` (новая) | `naruto.gui.skill.branch.dharma` | 2. Додзюцу Дхармы — власть над материей | `dharma_eye` |
| `TENSEIGAN` (новая) | `naruto.gui.skill.branch.tenseigan` | 3. Тенсейган — бог гравитации | `tenseigan` |

`SkillBranch.JOUGAN` удаляется. Тенсейган в дереве гейтится узлом `tenseigan_eye`, у которого
в `AbilityUnlocks.BASE_REQUIREMENTS` база — `ability:byakugan`; Дхарма независима.

Все техники ветки гейтятся базовым глазом через `AbilityUnlocks.BASE_REQUIREMENTS`
(`ability:<path>` → `ability:<base>`), как у существующих додзюцу.

## Комбо: 4 базовых комбо × 3 модификатора на ветку (12 групп)

Вместо «одно комбо — одна техника» у каждой ветки-глаза теперь **4 базовых комбо**, и каждое отвечает
за **три** техники в зависимости от способа нажатия: `click` = обычное прожатие, `shift` = Shift+прожатие,
`hold` = удержание последней клавиши. Базовый мод умеет только «комбо → одна техника», поэтому каждая
группа регистрируется в `NarutoAbilities.COMBO_MAP` как канальный **carrier**
(`narutoaddon:byakugan_farsight`), чтобы базовый обработчик ждал удержание; `mixin/client/ComboModifierMixin`
перехватывает `NarutoAbilities#triggerAbility(long)` и `#handleCharging(long, ChannelStatus)` и выбирает
вариант по модификатору. `NarutoAddonMod#commonSetup` регистрирует группы, `JutsuScreen` показывает
по три строки на комбо с пометками Shift/Hold.

Глаз ветки — отдельная стойка, а не одна из двенадцати техник: у Бьякугана это click-слот группы 3111,
у Тенсейгана — click-слот 3311, у Дхармы — собственное комбо **3223** (три группы 32xx заняты
техниками). Размер — **одна** техника-тоггл `size_shift` на комбо `3211`: включил, крутишь колесо от 0.5
до 5.0, выключил — обычный рост; отдельные «микро/макро/плавный» убраны, всё описано в одном описании.
Техники Дхармы работают только при открытом глазе: `abilities/EyeRequirement` проверяет
это в обоих путях каста (`ServerAbilityActivatePacket`, `ServerAbilityChannelPacket`), а закрытие
глаза обрывает уже запущенный канал ветки.

| комбо | прожатие (click) | Shift+прожатие (shift) | удержание (hold) |
| --- | --- | --- | --- |
| 3111 | byakugan | byakugan_sonar | byakugan_farsight |
| 3112 | juken_stance | juken_purge | sixty_four_palms |
| 3113 | circular_block | palm_lance | rotation |
| 3121 | air_palm | eight_trigrams_collapse | chakra_grip |
| 3211 | size_shift (стойка «Размер», без модификаторов) | — | — |
| 3212 | dimension_step | dimension_banish | sewing_pocket |
| 3221 | sewing_lock (отдельное комбо) | — | — |
| 3222 | armor_collapse (отдельное комбо) | — | — |
| 3132 | space_drill (отдельное комбо) | — | — |
| 3133 | black_rod_throw (отдельное комбо) | — | — |
| 3333 | cube_summon (отдельное комбо) | — | — |
| 3223 | dharma_eye (стойка, без модификаторов) | — | — |
| 3311 | tenseigan | gravity_hook | telekinesis_loot |
| 3312 | projectile_reflect | gravity_press | zero_gravity |
| 3321 | orb_shot | orb_cocoon | golden_wheel |
| 3322 | flesh_rupture | gravity_well | otsutsuki_will |

## Пассивные механики (узлы `mechanic:`)

Ветка 1:
* `mechanic:anatomical_scale` — счётчики убийств по типу моба, урон Мягкого кулака до ×5.
* `mechanic:enemy_chakra_pool` — скрытый пул чакры у любой LivingEntity, пороги дебаффов.
* `mechanic:surgical_extraction` — копание голой рукой = шёлковое касание.

Ветка 2:
* `mechanic:dharma_wheel` — адаптация к типу урона, −50 % повторно.
* `mechanic:sukunahikona_aura` — снаряды (стрелы, фаерболы гаста/ифрита) исчезают.
* `mechanic:space_hack` — открытие контейнеров под цельным блоком.

Ветка 3:
* `tenseigan` (базовая стойка) — режим чакры: поглощение урона, сила, левитация, ходьба по воде/лаве.
* `mechanic:trajectory_distortion` — эндер-перлы и стрелы летят по прямой.
* `mechanic:infinite_jump` — бесконечный двойной прыжок.
* `mechanic:meteor_strike` — падение >15 блоков → АоЕ, урон = высота ×2, без урона себе.
* `mechanic:truth_orbs` — сферы за спиной блокируют атаки сзади.

## Общие системы (создаются как самостоятельные классы)

| система | файл | данные |
| --- | --- | --- |
| Пул чакры врагов | `capabilities/enemychakra/EnemyChakra.java` + `events/EnemyChakraEvents.java` | persistent data сущности, ключ `narutoaddon:enemy_chakra`; пороги 75/50/25/10/0 % |
| Анатомический скейл | `capabilities/scale/AnatomicalScale.java` + `events/AnatomicalScaleEvents.java` | persistent data игрока, `narutoaddon:anatomical_kills`; множитель 1 + 4·(kills/50), кап ×5 |
| Колесо Дхармы | `capabilities/dharma/DharmaWheel.java` + `events/DharmaWheelEvents.java` | карта «тип урона → адаптирован», −50 % |
| Размер | `abilities/jutsus/AbstractSizeAbility.java` + `events/SizeEvents.java` | `AttributeModifiers` scale/speed/reach, 0.5–5.0 |

Правила: клиенту видно только через `@Sync`/пакеты; NBT переживает перезаход; сервер — источник
правды; все строки — в `lang/en_us.json` и `lang/ru_ru.json`; цена и КД — через `masteryCost(...)`
и `Ability.Cooldown#registerCooldown`.

## Что удаляется вместе с Джоуганом

Классы: `JouganAbility`, `JouganPhaseAbility`, `JouganStasisAbility`, `JouganUnweaveAbility`,
`ChakraSeverAbility`, `ChakraShearAbility`, `StructureCutAbility`, `SealPortalAbility`.
События: `JouganEvents` целиком; `JouganExtraEvents` — вычистить всё, кроме кода
`SewingPocket`/`SewingLock`/`DimensionStep`.
Реестр, `AbilityUnlocks`, `SkillBranch.JOUGAN`, узлы `jougan_*` в `Skill.java`, lang-ключи
`jougan*`, `scroll_jougan` (предмет+лут), `NarutoConfig.jouganEnabled`, `naruto.gui.jutsu.category.jougan`,
`naruto.gui.mode.jougan`.

## Порядок работ

1. Удаление Джоугана + переписывание `dimension_step` в порталы.
2. Общие системы (пул чакры, скейл, колесо Дхармы, размер).
3. Ветка 1 (12 техник + 3 пассивки), затем ветка 2, затем ветка 3.
4. Дерево, гейты, lang, `AGENT_NOTES.md`, `WEBChanges.md`.

Каждая волна заканчивается `.\gradlew.bat compileJava --offline` на JDK 17.

## Статус (03.11.2026)

### Реализовано

* **Удаление Джоугана.** `JouganAbility`, `JouganPhaseAbility`, `JouganStasisAbility`, `JouganUnweaveAbility`,
  `ChakraSeverAbility`, `ChakraShearAbility`, `StructureCutAbility`, `SealPortalAbility`,
  `events/JouganEvents` удалены; `events/JouganExtraEvents` вычищен и переименован в `events/SewingEvents`.
  Удалены `SkillBranch.JOUGAN`, узлы `jougan_*`, `scroll_jougan`, `NarutoConfig.jouganEnabled`,
  lang-ключи `jougan*`.
* **Общие системы.** `capabilities/enemychakra/EnemyChakra` + `events/EnemyChakraEvents`;
  `capabilities/scale/AnatomicalScale` + `events/AnatomicalScaleEvents`;
  `capabilities/dharma/DharmaWheel` + `events/DharmaWheelEvents`;
  `capabilities/size/EntitySize` + `network/s2c/ClientboundSizePacket` +
  `mixin/client/LivingEntityRendererScaleMixin` + `events/SizeEvents`/`SizeCombatEvents`;
  `abilities/jutsus/AbstractSizeAbility`.
* **Порталы «Врата».** `DimensionStepAbility` переписан; `world/DharmaCubeChunkGenerator` +
  `data/narutoaddon/dimension/dharma_cube.json`.
* **Ветка 1 — Бьякуган (12 техник).** `ByakuganAbility`, `ByakuganSonarAbility`,
  `ByakuganFarsightAbility`, `JukenStanceAbility`, `JukenPurgeAbility`, `SixtyFourPalmsAbility`,
  `CircularBlockAbility`, `PalmLanceAbility`, `RotationAbility`, `AirPalmAbility`,
  `MountainCollapseAbility`, `ChakraGripAbility`. Пассивки: `mechanic:anatomical_scale`,
  `mechanic:enemy_chakra_pool`, `mechanic:surgical_extraction`
  (`events/SurgicalExtractionEvents`).
* **Ветка 2 — Дхарма (12 техник).** `SizeMicroAbility`, `SizeMacroAbility`, `SizeShiftAbility`,
  `DimensionStepAbility`, `DimensionBanishAbility`, `SewingPocketAbility`, `SewingLockAbility`,
  `ArmorCollapseAbility`, `SpaceDrillAbility`, `BlackRodThrowAbility`, `DharmaLadderAbility`,
  `CubeSummonAbility`. Пассивки: `mechanic:dharma_wheel`, `mechanic:sukunahikona_aura`,
  `mechanic:space_hack` (`events/SpaceHackEvents`).
* **Ветка 3 — Тенсейган (12 техник).** `GravityHookAbility`, `GravityWellAbility`,
  `TelekinesisLootAbility`, `ProjectileReflectAbility`, `GravityPressAbility`,
  `ZeroGravityAbility`, `OrbShotAbility`, `OrbCocoonAbility`, `GoldenWheelAbility`,
  `FleshRuptureAbility`, `GravityDrillAbility`, `OtsutsukiWillAbility`. Пассивки: базовая стойка
  `tenseigan`, `mechanic:trajectory_distortion`, `mechanic:infinite_jump`,
  `mechanic:meteor_strike`, `mechanic:truth_orbs`
  (`events/TenseiganPassiveEvents`, `events/TruthOrbEvents`).
* **Дерево скилов.** Ветки `BYAKUGAN`, `DHARMA`, `TENSEIGAN` с центрами `byakugan_base`,
  `dharma_base`, `tenseigan_base`; гейты через `AbilityUnlocks.BASE_REQUIREMENTS`.
* **Документация.** Обновлены `AGENT_NOTES.md` и `WEBChanges.md`.

### Осталось

* Визуальный масштаб других игроков (сейчас синкается только владельцу).
* Баланс чисел (цены чакры/КД, пороги, множители) — требуется игровая проверка.
* Lang-строки и вики-страницы для 36 новых техник и трёх веток (очередь в `WEBChanges.md`).
* Игровые проверки всех техник и пассивок.
