# AGENTS.md — Naruto Mod Addon (Minecraft 1.20.1 / Forge)

Это **аддон `narutoaddon`**, который переносит контент форка `StandarterDF/Naruto-Mod` 1.11.0 поверх официального
`NarutoMod-1.20-1.8.1`. Аддон **не содержит кода Sekwah**: базовый мод подключается как jar
(`libs/NarutoMod-1.20-1.8.1-deobf.jar`, `implementation fg.deobf(files(...))` в `build.gradle`), sekclib —
`com.sekwah:sekclib-forge:1.0.2-SNAPSHOT`. Цель — паритет поведения форка 1.11.0 поверх базы 1.8.1.
`origin` — форк `StandarterDF/Naruto-Mod` (пушим туда). Оригинал 1.7.10 лежит в `upstream/release-forge-1-7-10` —
читать при портировании.

Это **карта проекта, а не энциклопедия**: здесь «где что лежит» и правила работы. Числа техник, точные
механики, история решений и грабли — в **[`AGENT_NOTES.md`](AGENT_NOTES.md)** (открывать по теме, не целиком).
Вики — в `docs/`, но агент её **не правит** (см. правило про `docs/` ниже).

## Доска задач

Актуальный список работ ведётся в **Kanban MCP**: проект `narutomod-addon`,
адрес `http://127.0.0.1:34535/mcp?project=narutomod-addon`. Используй инструменты MCP (get_tasks, get_work_queue и т.д.),
а не REST API.

## Сборка и запуск

```powershell
# Gradle 8.1.1 не работает на системной Java 25 — только JDK 17
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
.\gradlew.bat build        # или compileJava
.\gradlew.bat compileJava --offline   # проверка компиляции аддона
.\run-dev-client.bat       # дев-клиент, сам находит JDK 17
.\run-dev-server.bat       # дев-сервер
```

* Скрипты запускают Gradle с `--offline`: зависимости, Minecraft/Forge, ассеты и нативы уже в кэше
  (`~/.gradle`, `build/fg_cache`, `run/`). Ручной `.\gradlew.bat ...` без сети тоже зови с `--offline`.
  После правки зависимостей — один раз `set NARUTO_ONLINE=1`, дальше снова офлайн.
* База подключается компиляционным слоем: если в `libs/` лежит `NarutoMod-1.20-1.8.1-deobf.jar` —
  берётся он (офлайн), иначе `build.gradle` тянет официальный файл через `curse.maven`
  (`sekwah41s-naruto-mod-225298:4684253`). `libs/` в `.gitignore`: чужие jar в репозиторий не попадают.
* **Java не читает `HTTP_PROXY` из окружения**, поэтому Gradle без сети важных мавенов падает таймаутом
  (`Connection timed out` на `maven.neoforged.net` / `api.modrinth.com`). Если в системе задан прокси
  (`$env:HTTP_PROXY`, у нас `http://192.168.0.250:2081`), передай его JVM явно:
  `.\gradlew.bat build -Dhttp.proxyHost=192.168.0.250 -Dhttp.proxyPort=2081 -Dhttps.proxyHost=192.168.0.250 -Dhttps.proxyPort=2081`.
  Проверить прокси до сборки: `Invoke-WebRequest -Uri https://maven.neoforged.net/releases/ -Proxy $env:HTTP_PROXY -Method Head`.
* Релизный jar (без суффикса `-SNAPSHOT`) собирается с `$env:IS_RELEASE='true'` — иначе `build.gradle:55`
  добавляет к версии `-SNAPSHOT`. Результат: `build/libs/NarutoModAddon-1.20-<version>-universal.jar`.
* Дев-клиент идёт с JEI 15.71.0.224 (`runtimeOnly`, CDN Modrinth): это только dev-удобство, код мода на
  его API не опирается.
* **Тесты в игре делает пользователь.** Агент доводит код до `build`/`compileJava` и останавливается;
  клиент, сервер и игровые сценарии не запускает. Автотест готовить только по отдельной просьбе:
  `runClient --args=--quickPlaySingleplayer=clonetest`, `runServer --args=--nogui`, проверка по маркеру
  `say <МАРКЕР>` в логе, артефакты — в `tmp/`/`run/` (системный temp не засорять).

## Карта репозитория

Базовый пакет аддона — `src/main/java/com/sekwah/narutoaddon/`.

| Путь | Что внутри |
| --- | --- |
| `abilities/` | `Ability` (база: цена, КД, каст, освоение), `NarutoAbilities` (реестр + `COMBO_MAP`), `AbilityUnlocks` (гейт дерева — единая точка правды), `JutsuEffects` |
| `abilities/jutsus/` | По классу на технику: клоны, стихии, дыхания, додзюцу, Камуи, Расенган, земляные техники… Плюс общие предки `AbstractBreathAbility`, `AbstractDojutsuAbility`, `BreathCone` |
| `abilities/utility/` | Простые стойки/утилиты: заряд чакры, рывок, кулак, двойной прыжок, прыжок, ходьба по воде |
| `abilities/mastery/` | `Mastery` — кривая освоения, скидки, КД (прогресс смертью не отнимается) |
| `capabilities/` | `INinjaData`/`NinjaData` (чакра, стамина, сенин, NBT, `@Sync`), `NinjaCapabilityHandler`, `AddonNinjaData` (адаптер к базовой капабилити); вложенные данные — `skills/`, `mastery/`, `absorption/`, `disabledabilitydata/`, `toggleabilitydata/`, `lingerabilitydata/` |
| `entity/` | `ShadowCloneEntity`, `ChakraMarkerEntity`, `SmokeCloudEntity`, `SubstitutionLogEntity`, `WorkerJob`, `NarutoEntities` (регистрация и атрибуты) |
| `entity/ai/` | Голы клонов (`Clone*Goal`) и рабочих (`Worker*Goal`) |
| `entity/jutsuprojectile/` | Снаряды и временные сущности техник: Расенсюрикен, Чибаку, земляная стена, огненный шар… |
| `entity/item/`, `entity/projectile/` | Предметы-сущности и обычные снаряды (кунай, сюрикен, бумажная бомба) |
| `events/` | Forge-обработчики: клоны, взрывы, щит, врата, Камуи, друзья, броня, скиллы… |
| `network/c2s/` | Пакеты клиент → сервер: каст, канал, скиллы, друзья, сундук, отключение техник |
| `network/s2c/` | Пакеты сервер → клиент: сенсорика, руда, друзья, содержимое сундука |
| `client/` | `gui/` (JutsuScreen, SkillTreeScreen, HUD-полоски), `keybinds/`, `renderer/`, `model/`, `events/` |
| `skills/` | `Skill` (таблица узлов), `SkillTree`, `SkillPoints`, `SkillBranch`, `SkillSection`, `SkillEffect` |
| `menu/` | Походный сундук: `SupplyChestContainer`, `SupplyChestMenu`, `SupplyChestWindow` |
| `item/` | Предметы, `armor/`, `weapons/` (великие мечи); `NarutoItems` — 33 своих + 22 алиаса на базу |
| `world/` | `KamuiChunkGenerator`, `EarthWallSavedData`, структуры/данжи, `NarutoWorldGen` |
| `mixin/common/`, `mixin/client/` | Миксины: аксессоры, подавление звука, бумажная бомба на нити, `KeyHandlerMixin` |
| `config/NarutoConfig` | Все опции мода (Forge config, `config/narutomod-*.toml`) |
| `commands/` | `/naruto`, `/naruto debug`, `/jutsu`, `menuclick` |
| `damagetypes/` | `NarutoDamageTypes` — ключи своих типов урона |
| `trackers/` | Синк-трекеры sekclib для полей `NinjaData` |
| `friends/`, `sounds/`, `registries/`, `loot/`, `ranks/`, `clans/`, `gameevents/`, `triggers/`, `anims/` | Друзья (SavedData), звуки, реестры, лут, ранги, кланы, игровые события, триггеры, анимации |
| `util/` | `Targeting` (лучи/цели), `GuiUtils`, `ExperienceUtils`, `ColorUtil`, `StateUtils` |
| `datagen/` | Код датагена есть, но **при сборке не запускается** — ресурсы лежат руками |
| `bridge/` | `AbilityProxy`, `NarutoAddonAbilities` — мост в базовый мод (см. раздел «Мост в базовый мод») |

Ресурсы: `src/main/resources/`

| Путь | Что внутри |
| --- | --- |
| `assets/narutoaddon/lang/` | `en_us.json`, `ru_ru.json` (+ другие языки); все сообщения игроку — отсюда |
| `assets/narutoaddon/textures/items/`, `blocks/` | Легаси-папки (множественное число) + `assets/minecraft/atlases/blocks.json` — без него текстуры фиолетовые |
| `assets/narutoaddon/sounds.json`, `sounds/` | Звуки (`clone_poof.ogg` и др.) |
| `data/narutoaddon/damage_type/` | JSON своих типов урона — обязательны, иначе сервер падает |
| `data/narutoaddon/dimension/`, `dimension_type/` | Камуи и типы измерений — руками |
| `data/narutoaddon/recipes/`, `loot_modifiers/`, `loot_tables/`, `tags/`, `worldgen/`, `advancements/` | Рецепты, лут, теги, генерация — руками |

Корень: `tools/` — python-скрипты (редактор/линтер дерева скилов, сборка вики), `docs/` — вики,
`research/` — дизайн-доки, `run/` — дев-мир, `tmp/` — рабочие артефакты, `WEBChanges.md` — очередь
правок вики.

## Исходники Minecraft и Forge: `.mc_sources/`

В корне лежит `.mc_sources/` — **декомпилированные исходники ровно нашей сборки** (Forge
1.20.1-47.1.62, моджанг-имена, 5438 `.java`): `Player.java`, `LivingEntity.java`,
`PlayerRenderer.java`, `EntityDimensions.java`, `Attributes.java` и т.д. **Читать ванилу и Forge
только оттуда** через `Grep`/`Read` по `.mc_sources/net/minecraft/...`, не `javap` и не байт-код.

Получено Vineflower из
`%USERPROFILE%\.gradle\caches\forge_gradle\minecraft_user_repo\net\neoforged\forge\1.20.1-47.1.62_mapped_official_1.20.1\forge-1.20.1-47.1.62_mapped_official_1.20.1.jar`
(декомпилятор — `org.vineflower:vineflower:1.12.0` из кэша модулей). Пересоздать:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$vf="$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\org.vineflower\vineflower\1.12.0\85570609a0a5941a7d2918b6260b209de810f66f\vineflower-1.12.0.jar"
$mc="$env:USERPROFILE\.gradle\caches\forge_gradle\minecraft_user_repo\net\neoforged\forge\1.20.1-47.1.62_mapped_official_1.20.1\forge-1.20.1-47.1.62_mapped_official_1.20.1.jar"
& "$env:JAVA_HOME\bin\java.exe" -Xmx4G -jar $vf -dgs=1 -hdc=0 $mc .mc_sources
```

**Не брать** `sourcesAndCompiledWithNeoForge_*.jar` из `~/.gradle/caches/neoformruntime/` — там
исходники другой версии (1.21), и их API вводит в заблуждение (например, `Attributes.SCALE` есть в
1.21, но **нет** в 1.20.1). Папка в `.gitignore`, в сборку не входит (`sourceSets` — только
`src/main/java`).

### Исходники базового мода: `.base_sources/`

Рядом лежит `.base_sources/` — **настоящие исходники базы** из `libs/NarutoMod-1.20-1.8.1-sources.jar`
(111 `.java` + ресурсы). Вопросы про поведение базы (ключевой обработчик, `NinjaData`, пакеты,
`COMBO_MAP`, тогглы) решать чтением `.base_sources/com/sekwah/narutomod/...`, а не `javap` по
`*-deobf.jar`. Папка в `.gitignore`.

### Внешние справочники для 1.20.1 (без декомпиляции)

* **Javadoc ванилы и Forge 1.20.1** — https://lexxie.dev/forge/1.20.1/ (есть и `net/minecraft/...`,
  например `.../net/minecraft/world/entity/player/Player.html`). Быстрый ответ «какой метод/событие
  есть в этой версии».
* **Браузер моджанг-маппингов** — https://mappings.dev/1.20.1/ и https://linkie.shedaniel.dev/ :
  обфусцированное имя ↔ `official`/`mojmap`/`srg`, сравнение версий.
* **Документация Forge 1.20.1** — https://docs.minecraftforge.net/en/1.20.1/ (datagen:
  `/en/1.20.1/datagen/`). Смотреть именно ветку `1.20.1`, а не `1.21.x`.
* **Mixin** — https://github.com/SpongePowered/Mixin/wiki . Если инжект «не сработал», включить
  `-Dmixin.debug.export=true` (или `@Debug(export = true)` на миксине) и посмотреть готовый класс в
  `run/.mixin.out/class/` — видно, попал ли инжект и куда.
* **Ресурсы ванилы** (текстуры, модели, `lang`, звуки) — https://mcasset.cloud/1.20.1/
* **Генераторы JSON** (измерения, worldgen, лут, датапак) — https://misode.github.io/dimension/ ,
  https://misode.github.io/worldgen/
* **Parchment** (имена параметров вместо `p_36166_` + javadoc, опционально) —
  https://parchmentmc.org/docs/getting-started.html ; артефакт
  `org.parchmentmc.data:parchment-1.20.1:2023.09.03@zip`. Подключается в `build.gradle` слоем к
  `mappings channel: 'official', version: '1.20.1'`; требует одной онлайн-сборки. Если подключить —
  декомпилировать `.mc_sources` из parchment- jar, тогда исходники читаются с нормальными именами.

## Мост в базовый мод

База (`com.sekwah.narutomod.*`) владеет общими вещами (22 предмета, 7 базовых техник, чакра, стамина, режим
ниндзя, сеть, реестры). Аддон подключается к базе через тонкий слой-мост в `bridge/`:

* `bridge/AbilityProxy.java` — оборачивает наш `Ability` и отдаёт базовому конвейеру; реализует все 4
  вложенных интерфейса базы (`Toggled`, `Channeled`, `Cooldown`, `HandleEnded`), мапит `ActivationType`
  и адаптирует `INinjaData`.
* `bridge/NarutoAddonAbilities.java` — `DeferredRegister` в реестре базы
  (`NarutoRegistries.ABILITY_REGISTRY_LOC`) под namespace `narutoaddon`.
* `capabilities/AddonNinjaData.java` — адаптер: общие поля (чакра, стамина, кулдауны, тогглы) делегируются
  в базовую капабилити, наши поля (мастерство, скиллы, сенин, поглощение, отключённые техники, сундук)
  живут в аддоне.
* `NarutoAddonMod.commonSetup` перекрывает комбо в базовой `NarutoAbilities.COMBO_MAP` и пишет в лог
  «Registered addon combos: N added, M overridden» (в логе последнего запуска: 153 added, 5 overridden).
* Клиент: `mixin/client/KeyHandlerMixin.java` перевешивает клавишу J на наш `client/gui/JutsuScreen`.

Правило namespace: общее с базой — алиас на `com.sekwah.narutomod.*` (в `item/NarutoItems.java` 22 таких
поля, например `KUNAI = com.sekwah.narutomod.item.NarutoItems.KUNAI`); новое — регистрация под
`narutoaddon:`. Блоки `paper_bomb` и `bonsai_tree` пока регистрируются аддоном и это считается дефектом
(задача 168).

Правило: **аддон не копирует классы базового мода**. Если класс есть в jar базы — использовать
`com.sekwah.narutomod.*` (через алиас или прямой вызов), а не дублировать.

## Где искать механику

| Механика | Файлы |
| --- | --- |
| Тени-клоны | `entity/ShadowCloneEntity`, `entity/ai/Clone*Goal`, `abilities/jutsus/ShadowCloneJutsuAbility`, `client/renderer/entity/ShadowCloneRenderer` |
| Медитация и аура | `abilities/jutsus/ChakraMeditationJutsuAbility`, `capabilities/mastery/MeditationData` |
| Рабочие смены | `entity/WorkerJob`, `entity/ai/Worker*Goal`, `ShadowCloneJutsuAbility#workerJob`, `menu/SupplyChest*` |
| Техники | `abilities/jutsus/<Name>Ability`, регистрация в `NarutoAbilities`, гейт в `AbilityUnlocks` |
| Додзюцу | `AbstractDojutsuAbility`, `SharinganAbility`/`ByakuganAbility`/`JouganAbility`/`Senrigan*`/`RinneganAbility`, `events/*Events` |
| Освоение | `abilities/mastery/Mastery`, `capabilities/mastery/MasteryData`, `trackers/MasteryDataSyncTracker` |
| Дерево скилов | `skills/Skill`, `skills/SkillTree`, `client/gui/SkillTreeScreen`, `network/c2s/ServerUnlockSkillPacket` |
| Чакра, стамина, HUD | `capabilities/NinjaData`, `client/gui/ChakraAndStaminaGUI`, `NinjaCapabilityHandler` |
| Друзья | `friends/Friends`, `friends/FriendsSavedData`, `client/gui/FriendsScreen`, `network/*Friend*` |
| Походный сундук | `abilities/jutsus/StorageChestAbility`, `menu/SupplyChest*`, `network/s2c/ClientboundSupplyChestPacket` |
| Камуи | `abilities/jutsus/KamuiJutsuAbility`, `KamuiPhaseAbility`, `events/KamuiEvents`, `world/KamuiChunkGenerator` + JSON измерения |
| Земляная стена | `EarthWallJutsuAbility`, `events/EarthWallEvents`, `world/EarthWallSavedData`, `entity/jutsuprojectile/EarthWallEntity`, `client/renderer/entity/EarthWallBlockRenderer` |
| Чибаку Тенсэй | `abilities/jutsus/ChibakuTenseiAbility`, `entity/jutsuprojectile/ChibakuTenseiEntity`, `client/renderer/entity/ChibakuTenseiRenderer` |
| Режим Мудреца | `SenjutsuChargingAbility`, `SageModeAbility`, `client/gui/SeninGUI`, `events/PlayerEvents#onEntityUpdate` |
| Сенсорика и руда | `ChakraSensingAbility`, `OreSenseAbility`, `network/s2c/Clientbound*Packet`, `client/ClientSensingData`, `client/renderer/*Renderer` |
| Отладка | `commands/NarutoDebugCommand` (`/naruto debug`), `commands/MenuClickCommand` |

## Правила работы

* Java 17. Комментарии и javadoc — **на английском**. Строки игроку — только через `lang/*.json`
  (`en_us` + `ru_ru`), хардкод в коде не оставлять.
* **Новая техника**: класс (наследник `Ability` или общего предка) → регистрация в `NarutoAbilities` →
  уникальное комбо → узел дерева и ключ `ability:<path>` в `AbilityUnlocks` → строки в оба lang-файла →
  опция в `NarutoConfig`, если технику можно выключить.
* **Новая «плюшка» клона**: гол в `entity/ai/`, регистрация в `ShadowCloneEntity#registerGoals`,
  копирование предмета в `copyEquipment`/`copyAmmo`, поза рук в `ShadowCloneRenderer`. Если плюшка
  занимает руки — её условие идёт в `ShadowCloneEntity#wantsToShoot`.
* **Сервер — источник правды**: проверка цены/условий в `handleCost`, эффект в `performServer`;
  клиент только просит пакетом (`network/c2s`), сервер перепроверяет.
* **Данные игрока**: клиенту видно — `@Sync` в `NinjaData` (+ трекер, если нужно `minTicks`);
  переживает перезаход — NBT в `serializeNBT`/`deserializeNBT`. Server-only поля не синкать.
* **Цена и КД** — через `masteryCost(...)` и `Ability.Cooldown#registerCooldown`, иначе освоение не даёт
  скидку. Учёт кастов — `recordMastery` в трёх путях пакетов; каналы с накоплением — `recordMasteryCredit`.
* **Новый тип урона** — сразу JSON в `data/narutoaddon/damage_type/` + ключ в `NarutoDamageTypes`. Без JSON
  сервер падает при первом попадании.
* **Новое измерение** — JSON в `data/narutoaddon/dimension(_type)/`. Если файл битый, `getLevel` вернёт
  `null` (без падения) — проверяй и показывай игроку сообщение.
* **Рецепты, лут, теги, worldgen** — руками в `data/`, датаген не гоняется.
* **Текстуры** — в легаси-папки `textures/items|blocks`; атлас тянет их через `assets/minecraft/atlases/blocks.json`.
* **Дерево скилов** — правится через `tools/skilltree_preview.py`: правки в браузере → `--check` →
  `--apply` → `build`. Линтер ловит дубли, межветковые связи, технику на технике и пустые ключи.
* **Кейбинды**: при смене набора кейбиндов удалять `run/options.txt` (Forge пересоздаст дефолты; задвоенные
  `key_naruto.keys.*` ломают раннее окно).
* **Доки**: `docs/` не трогать и не собирать. Если правка меняет описанное на сайте — пункт в
  `WEBChanges.md` (страницы, секции, что поправить).
* **Локальные правила**: Python — только venv проекта; тестовые артефакты — в `tmp/`/`run/`, не в
  системном temp; глобальные npm/pip/winget не ставить.

## Хрупкие места (подробности — в `AGENT_NOTES.md`)

* `player.pick(...)` видит только блоки. Цель — `util/Targeting#lookedAtEntity`, блок — `#lookedAtBlock`.
* `Player` переопределяет `getDimensions(Pose)` и `getStandingEyeHeight(...)` **без `super`**, а
  `PlayerRenderer` — `scale(AbstractClientPlayer, ...)` без `super`. Миксин на `Entity#getDimensions` или
  `LivingEntityRenderer#scale` для игрока не сработает вообще: размер нужно вешать на переопределения
  (`mixin/common/PlayerDimensionsMixin`, `mixin/client/PlayerRendererScaleMixin`). Атрибута `SCALE` в
  1.20.1 нет — он появился в 1.20.2.
* Смена измерения пересоздаёт клиентское тело: масштаб надо отправлять заново
  (`SizeEvents#onChangedDimension`) и пересчитывать хитбокс на `ClientPlayerNetworkEvent.Clone`, иначе
  камера и хитбокс откатываются к обычному росту.
* `Player#tick` сбрасывает `noPhysics` каждый тик: флаги движения ставить в `LivingTickEvent`, на обеих
  сторонах (Камуи-фаза, сцепление).
* **Стойки живут в двух наборах.** Тоггл, включённый из меню, попадает в набор аддона
  (`AddonNinjaData.getToggleAbilityData()`), включённый клавишами — в набор базовой капабилити
  (`base.getToggleAbilityData()`), и тикают их разные циклы. Проверять «включено ли» только
  `abilities/ToggleState.isActive(ninjaData, id)`; `getToggleAbilityData()...contains(...)` видит лишь
  свой набор и молча врёт для стойки, включённой другим путём.
* `GoalSelector` не сортирует по номеру: при равных приоритетах отобрать флаг нельзя, «всегда работающий»
  гол должен иметь номер меньше всех (подбор лута — 0).
* Access transformer не доезжает до compile-classpath: приватные поля открывать mixin-аксессором, не
  рефлексией по Mojang-имени.
* Аддон **переиспользует** контент базы через алиасы (`NarutoItems.X = com.sekwah.narutomod.item.NarutoItems.X`).
  Такой предмет/блок/снаряд — экземпляр **базового** класса, а не нашей копии, поэтому `instanceof <наш класс>`
  для него всегда `false`. Бонусы брони из-за этого не начислялись, имя под маской не скрывалось. Проверять
  оба класса (или держать таблицу «предмет → поведение»); готовый детектор — `tmp/check_alias_parity.ps1`.
* `LivingSpawnEvent.CheckSpawn` в 1.20.1 нет — `MobSpawnEvent.PositionCheck`.
* Свой `ChunkGenerator`: реализовать **все** абстрактные методы, включая `addDebugScreenInfo`;
  `applyBiomeDecoration` — no-op, иначе биомные фичи дорисуют «потолок». Параметр — `WorldGenLevel`.
* `Level#getHeight` на клиенте для незагруженного чанка отдаёт `minY` — подменять синканной высотой.
* `ItemStack` пишет count байтом: копии предметов клампить по 127, а сундук хранит количества отдельно.
* Ванильная регенерация от еды (до 2 HP/с) сильнее любого слива HP — гасить `LivingHealEvent`, если
  техника «сжигает» жизнь.
* `Component.keybind` зависит от раскладки — буквы комбо считать из кода клавиши (`JutsuScreen#keyLetter`).
* Forge зовёт `onExplosionDetonate` до урона и отдаёт тот же список — фильтр союзников делать там.
* `.gitattributes` (LF для `version.txt`, CRLF для `*.bat`) не откатывать: иначе ломается сборка jar.
* `Entity#lerpTo` у обычной сущности — это `setPos` без интерполяции: позиционные пакеты «шагают»,
  поэтому движение считать от возраста на обеих сторонах (Чибаку).
* `@Sync` и NBT — не одно и то же. `syncGlobally` нужен, если поле должны видеть все (Хенге), иначе его
  получает только владелец капабилити.
* Порядок регистрации голов и `MeleeAttackGoal#stop` (сбрасывает цель) — учитывать при голах, которые
  временно отбирают флаг (подбор лута).

## Подробности

`AGENT_NOTES.md` — полный справочник: механики клонов и рабочих, все техники и их числа, додзюцу,
друзья, освоение, дерево скилов, отладочные команды, устройство вики и развёрнутый список грабель.


## ��������� ������� (�����)
- ����������� ������� ��� Sekwah ��� ������� � ������ 1.8.1 (NarutoMod-1.20-1.8.1-universal.jar).
- ������ 1.11.0 (NarutoMod-1.20-1.11.0-deobf.jar) � ��� ����������� ���� (StandarterDF), ������ ����������� ����������.
- ����: ���������� ���� 1.11.0 � ����� � ������������ 1.8.1 ��-�� ������� � ��������� ������ (��).
- ���� ��������� ��� ����� (�����, �����-����, ����������, ����� � �.�.) ������ ���� �������� � ����� ��� ����� narutoaddon, � ����� ������ ������������ � �������� � ������ NarutoMod 1.8.1.
- �� ������� ������ ����� �� ��� ������� �� 1.11.0-deobf (�����), ��� ��� ������ ������� ������� ����������� �������� ������ ������� � ���� �� ��������.