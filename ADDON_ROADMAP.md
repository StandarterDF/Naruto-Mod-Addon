# План: полноценный аддон на базе официального NarutoMod 1.8.1

> Дорожная карта порта форка 1.11.0 в аддон `narutoaddon`, работающий поверх официального
> `NarutoMod-1.20-1.8.1`. Составлена по фактическому состоянию проекта.

---

## 0. Цель и главный принцип

**Цель:** игрок ставит официальный `NarutoMod-1.20-1.8.1` + наш `narutoaddon`, и получает весь контент
форка 1.11.0 (180+ техник, мастерство, дерево скилов, клоны-рабочие, HUD, меню J, предметы), при этом
аддон **не содержит кода Sekwah** — только наши добавления, написанные поверх его API.

**Принцип:** база владеет общими вещами (22 предмета, 7 базовых техник, чакра, стамина, режим ниндзя,
сеть, реестры). Аддон владеет только тем, чего в 1.8.1 нет, и **подключается** к базе через тонкий
слой-мост. Никаких повторных регистраций одного и того же id.

**Правило namespace:**

- `narutomod:*` — всё, что уже есть в 1.8.1 (кунай, катана, огненный шар, leap…). Аддон такие вещи
  не регистрирует, а ссылается на объекты базы.
- `narutoaddon:*` — только новое (чибаку, камуи, расенган, посох, свитки, мастерство…).

---

## 1. Состояние на сейчас (фактическое)

Проверено по коду и логам (`run/logs/latest.log`, `run/logs/debug.log`).

| Что | Факт |
| --- | --- |
| Java-файлов | аддон — 487, форк — 508 |
| Техники (`ABILITY.register`) | аддон — 164, форк — 165. Отсутствуют: `rinnegan_prison`, `senrigan_evasion`, `senrigan_guidance`. Добавлены: `heaven_weight`, `rinnegan_levitation` |
| Предметы | 33 своих (`ITEMS.register`) + 22 алиаса на `com.sekwah.narutomod.item.NarutoItems` = 55. Отсутствуют 9 предметов: `asura_missile`, `toneri_scarf`, `spinach_pill`, `curry_pill`, `chili_pill`, `hidan_scythe`, `nuibari`, `kiba`, `kabutowari` (предмет `minato_kunai` в аддоне есть, отсутствует сущность `minato_kunai` — ПРОВЕРИТЬ: в коде найдена `MinatoKunaiEntity`, возможно расхождение) |
| Отсутствуют | эффект `critical_exhaustion`, зачарования (`enchantment/NarutoEnchantments`) |
| Сборка | `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat compileJava --offline` → **BUILD SUCCESSFUL** |
| Мост | `bridge/AbilityProxy`, `bridge/NarutoAddonAbilities`, `capabilities/AddonNinjaData` реализованы и работают |
| Комбо | «Registered addon combos: 153 added, 5 overridden» (лог последнего запуска) |
| Клиент | `mixin/client/KeyHandlerMixin` перевешивает J на `client/gui/JutsuScreen` |
| Ресурсы | `src/main/resources/assets/narutoaddon/` и `data/narutoaddon/` |
| База | `libs/NarutoMod-1.20-1.8.1-deobf.jar`, sekclib `com.sekwah:sekclib-forge:1.0.2-SNAPSHOT` |
| Блоки (дефект) | `paper_bomb`, `bonsai_tree` пока регистрируются аддоном (задача 168) |

---

## 2. Архитектурная схема

```
narutomod-1.8.1.jar   (реестры, чакра, 7 техник, сеть, ключи, HUD)
        ▲  компиляция + рантайм
narutoaddon.jar       (мост + наш контент)
   ├─ bridge          прокси способностей, адаптер данных
   ├─ content         предметы/блоки/сущности/звуки (только новые)
   ├─ systems         мастерство, скиллы, клоны-рабочие, друзья, сундук
   └─ client          своё меню J, HUD, эфф., скилл-дерево
```

Ключевая идея моста — **не наследовать** классы базы, а **оборачивать**: 180 классов форка остаются
как есть, их никто не переписывает; рядом появляется маленький прокси-класс, который база видит как
обычную `Ability` и который внутри дёргает наш объект.

---

## 3. Этап A — гигиена среды (0.5 дня)

Статус: ЧАСТИЧНО (A1–A4 не верифицированы по пунктам; сборка зелёная).

**A1. `run/options.txt`.** Удалить целиком — ваниль пересоздаст дефолты. Задвоенные `key_naruto.keys.*`
ломают раннее окно Forge ещё до загрузки модов. В `AGENTS.md` дописать: «при смене набора кейбиндов
удалять `run/options.txt`».

**A2. Логи ресурсов.** Собрать полный список ошибок из `run/logs/latest.log` (`TagLoader`,
`RecipeManager`, `LootTables`), он станет чек-листом для этапа G. Сейчас там: теги `scroll_*`,
`samehada/kubikiribocho`, еда, лут-таблицы `chests/sacred_tree`, `chests/dungeon`, рецепты
`kubikiribocho`, `sentry_seal`, `chakra_battery`.

**A3. `mods.toml` и версия.** В dev версия базы `0.0NONE` — из-за отсутствия `Implementation-Version`
в deobf-джарнике. Для dev это не только шум в логе: хендшейк мода падает → синк данных базы не идёт!
Решение: либо в `build.gradle` подкладывать в run манифест с версией, либо собирать в `run/mods`
universal-джарник с версией 1.8.1 и в dev работать с ним. Проверить, что `PlayerEvents`/синк чакры
реально доходят до клиента.

**A4. Чистка мёртвых копий.** Удалить остатки `mixin/fix` (`LocalPlayerMixin`, `PlayerModelMixin`,
`TripWireBlockMixin` — это копии микшинов Sekwah, они не наши и не подключены), оставить пустой
`mixins.narutoaddon.json` до этапа E.

---

## 4. Этап B — мост способностей (ядро, 2–3 дня)

Статус: СДЕЛАНО (AbilityProxy, NarutoAddonAbilities, combo override 153 added / 5 overridden).

**B1. `abilities/AbilityProxy extends com.sekwah.narutomod.abilities.Ability`.**
Оборачивает один объект `com.sekwah.narutoaddon.abilities.Ability` (наш). Зачем прокси, а не
наследование: у нашего `Ability` тот же набор методов с **другими типами** (`ActivationType`,
`INinjaData`) — унаследовать нельзя, будет конфликт сигнатур. Прокси решает это без правки
180 классов.

Методы прокси:

- `activationType()` — маппинг наш enum → базовый по имени;
- `handleCost(Player, base INinjaData, int)` — достаёт наш адаптер данных (см. C1) и зовёт наш
  `handleCost(Player, AddonINinjaData, int)`;
- `performServer(...)`, `canStartToggle(...)`, `logInChat()`, `castingSound()`, `castingFailSound()` —
  делегирование с конверсией данных;
- `getTranslationKey(...)` — базовый механизм уже отдаёт `registryKey.location()`, т.е.
  `narutoaddon:chibaku_tensei`; значит ключ локализации = `narutoaddon:chibaku_tensei`;
- реализует **все четыре** вложенных интерфейса базы (`Toggled`, `Channeled`, `Cooldown`,
  `HandleEnded`) и внутри проверяет `delegate instanceof ...`; если наш класс интерфейс не реализует —
  отдаёт дефолтное поведение. Это нужно потому, что базовый конвейер каналов делает `instanceof`.

**B2. Регистрация в реестре базы.**
`NarutoRegistries.ABILITIES` — публичный `ForgeRegistry<Ability>` базы, локация `narutomod:abilities`.
Аддон создаёт `DeferredRegister<base Ability>(NarutoRegistries.ABILITY_REGISTRY_LOC, "narutoaddon")` и
регистрирует по одному прокси на каждую нашу способность. Никаких своих реестров способностей — вся
серверная логика базы (instant/toggle/channeled, кулдауны, сообщения, звук каста) работает сразу.

**B3. Комбо.**
`NarutoAbilities.COMBO_MAP` базы публична. На `FMLCommonSetupEvent` (аддон идёт `AFTER narutomod`)
аддон:

1. перебирает свои зарегистрированные записи и кладёт `combo → ResourceLocation(narutoaddon:...)`;
2. при конфликте комбо **перезаписывает** базовую запись (наш fireball с мастерством важнее базового),
   а не ругается в лог;
3. пишет отчёт в лог: сколько комбо добавлено/перекрыто.

Так базовый обработчик клавиш (`NarutoKeyHandler`: C/V/B + комбо) и базовые пакеты каста начинают
кастовать **наши** техники, без единой строки правки в базе.

**B4. Что делать с 7 общими техниками.**
Вариант принят: наш «улучшенный» вариант перекрывает комбо и реально используется, базовый остаётся
в реестре невидимым. Если для какой-то техники это ломает поведение — на этот случай в прокси остаётся
флаг «не перекрывать» (тогда используется база).

---

## 5. Этап C — данные игрока (2–3 дня)

Статус: СДЕЛАНО (AddonNinjaData, своя капабилити).

**C1. Адаптер `AddonNinjaData implements INinjaData` (наш интерфейс).**
Наш `INinjaData` — надмножество базового (мастерство, скиллы, сенин, поглощение, отключённые техники,
сундук, метки, хенге, ранги, природа…). Нужен экземпляр, у которого «общие» поля живут в базовой
капабилити, а «наши» — в своей.

Реализация: наш `NinjaData` (копия форка) получает ссылку на базовый `INinjaData` и **делегирует**
ему методы:
`getChakra/setChakra/addChakra/useChakra`, `getStamina/setStamina/addStamina/useStamina`,
`getMaxChakra/getMaxStamina`, `getSubstitutionCount/useSubstitution`, `setIsNinja/isNinjaModeEnabled`,
`getCooldownEvents`, `getToggleAbilityData`, `getCurrentlyChanneled*`, `getDoubleJumpData`,
`setInvisibleTicks/getInvisible`, `setSubstitutionLoc/getSubstitution*`, `scheduleDelayedTickEvent`,
`updateDataServer/Client`.

Итог: **одна полоска чакры** на экране (базовая), базовая регенерация и базовый HUD работают; наши
техники тратят ту же чакру.

**C2. Своя капабилити `narutoaddon:ninja_data`.**
Регистрируется в `RegisterCapabilitiesEvent` + провайдер на `Player`. Хранит только наши поля
(мастерство, скиллы, сенин, поглощение, disabled, сундук, ранги, природа, метки). NBT-сериализация уже
есть в `NinjaData.serializeNBT` — её оставляем, но из неё выкидываем продублированные базовые поля
(иначе рассинхрон после релога).

**C3. Синхронизация.**
База использует `sekclib` (`@Sync` + трекеры). Аддон подписывается на `RegisterCapabilitySyncEvent`
**отдельной записью** `narutoaddon:ninja_data` → наш `NinjaData` с нашими `@Sync`-полями. Базовые поля
не синкаем — их синкает база. Проверить, что `sekclib` корректно принимает вторую капабилити на игроке
(если нет — фолбэк: собственный пакет полного снапшота `ClientboundAddonDataPacket` при изменении и на
логин).

**C4. Совместимость.**
Адаптер создаётся при attach: находит базовую капабилити, если её нет (например, игрок ещё не «ниндзя»)
— дефолтные нули. При death/clone/возврате из Камуи — перепривязка (база пересоздаёт данные, наша
ссылка должна обновиться: вешать на `PlayerEvent.Clone` + `TickEvent`).

---

## 6. Этап D — сеть (1 день)

Статус: СДЕЛАНО (свои пакеты в network/).

**D1. Переиспользование канала базы.** Каст, каналы, тогглы — через базовые пакеты
(`ServerAbilityActivatePacket`, `ServerAbilityChannelPacket`, `ServerToggleNinjaPacket`): они принимают
`ResourceLocation`/id из общего реестра, значит наши способности работают «из коробки».

**D2. Свои пакеты** — только то, чего в базе нет: сенсорика/руда, друзья, содержимое сундука,
отключение техник, скилл-поинты, меню-клик. Свой `SimpleChannel` с id `narutoaddon:main` и
`networkProtocolVersion = "1"`. Не смешивать с каналом базы (там версия = версия мода, и в dev она
`0.0NONE`).

**D3. Все `sendToServer` форка,** которые сейчас идут в `PacketHandler` нашего пакета, перевести:
каст/канал/тоггл → базовый `PacketHandler`, остальное → наш канал.

---

## 7. Этап E — клиент, меню, HUD (2–3 дня)

Статус: СДЕЛАНО (KeyHandlerMixin, JutsuScreen).

**E1. Меню J (mixin).** Базовый `NarutoKeyHandler` держит `private static KeyBindingTickHeld
JUTSU_MENU_KEY` и вешает на него `new JutsuScreen()` (базовый). Миксин `KeyHandlerMixin`:

- `@Shadow` на `JUTSU_MENU_KEY`;
- `@Inject(at = TAIL, method = "registerKeyBinds")` → перевесить consumer на наш
  `com.sekwah.narutoaddon.client.gui.JutsuScreen`.

Один миксин, без правки keys/настроек, J остаётся J.

**E2. Наш `JutsuScreen`.** Сейчас он перечисляет способности из нашего `NarutoAbilities.ABILITY`
(своего `DeferredRegister`). После этапа B источник — записи нашего `DeferredRegister`,
зарегистрированные в реестре базы. Правки: список берём из нашего регистратора (id `narutoaddon:*`),
цены/КД/мастерство — из адаптера данных (C1), «отключено» — из нашей `DisabledAbilityData`. Проверить,
что нигде не осталось `NarutoAbilities.getAbilityFromCombo` в старом смысле (комбо теперь в карте базы).

**E3. HUD.** Базовый `NarutoInGameGUI` рисует чакру/стамину — оставляем. Наш `ChakraAndStaminaGUI`/
`SeninGUI`/`AbsorptionGUI`/`ModeHudOverlay`/`GateCounterOverlay` включаем только для того, чего у базы
нет (сенин, поглощение, врата, режимы, орбы). Двойных полосок быть не должно: чакра/стамина — только
база.

**E4. Остальные экраны.** `SkillTreeScreen`, `FriendsScreen`, `SupplyChestScreen`, `SharinganGUI`,
`SensingGUI` — регистрируются в `MenuScreens`/`RegisterClientScreens`; для них нужны свои кейбинды
(`narutoaddon.keys.*`, дефолты не пересекаются с C/V/B/J/X базы) и свои пакеты из D2.

---

## 8. Этап F — контент-реестры (2–3 дня)

Статус: СДЕЛАНО (33 предмета + 22 алиаса, блоки/сущности/звуки).

Правило: **общее — алиас, новое — регистрация под `narutoaddon:`**.

**F1. Предметы.** В форке 55, у базы 22. Для 22 общих (`kunai`, `shuriken`, `katana`, `senbon`,
`explosive_kunai`, броня ANBU, `flak_jacket`, `fabric*`, `armor_plate*`, `akatsuki_cloak`…) — не
регистрировать, а сделать поля-алиасы на `com.sekwah.narutomod.item.NarutoItems.X` (тип тот же
`RegistryObject<Item>`, весь остальной код форка продолжает компилироваться без правок). Для 33 новых
(`kubikiribocho`, `samehada`, свитки, `chakra_crystal`, `smoke_bomb`, еда-рамен и т.д.) —
`DeferredRegister` под `narutoaddon`.

**F2. Блоки/блок-сущности.** Сравнить `NarutoBlocks` (у базы есть `chakra_seal` и т.п.); общие — алиас,
новые (`sentry_seal` и др.) — регистрация + `BlockEntityType` + `BlockEntity` рендерер.

**F3. Сущности.** `NarutoEntities` форка (клоны, рабочие, снаряды, Чибаку, стена…). У базы есть базовый
набор; сверить по именам, общие — алиас, новые — `narutoaddon:`. Не забыть атрибуты
(`EntityAttributeCreationEvent`) и data-фиксеры/сериализаторы (`NarutoDataSerialisers`).

**F4. Звуки.** `sounds.json` форка объединить с отсутствующими у базы; регистрация
`DeferredRegister<SoundEvent>` под `narutoaddon`; в `sounds.json` — `narutoaddon:*`.

**F5. Прочее.** `NarutoEffects` (моб-эффекты), `NarutoMenus` (типы меню), `NarutoPaintings`,
`NarutoLootModifiers` — всё под `narutoaddon:`. Креативная вкладка — своя (`narutoaddon:main`), чтобы не
дублировать базовую.

**F6. Реестры форка, которых нет у базы.** `narutoaddon:abilities` — **не создавать** (используем
базовый, см. B2). `narutoaddon:clans` — свой (у базы кланы есть, но пустые; сверить и решить: алиас на
базовый или свой). Дамаг-типы (`NarutoDamageTypes`) — свои JSON в `data/narutoaddon/damage_type/`.

---

## 9. Этап G — ресурсы (2 дня, скриптом)

Статус: СДЕЛАНО (assets/data narutoaddon).

**G1. `assets/narutomod` → `assets/narutoaddon`.** Сейчас аддон перекрывает ресурсы базы (в dev
побеждает загрузившийся позже) — это может стереть модели/текстуры/локализацию базы. Переносим всё
наше в свой namespace.

**G2. `data/narutomod` → `data/narutoaddon`**, кроме того, что сознательно дополняет базу (теги базы —
через `data/narutomod/tags` с `"replace": false`, но только реально нужные).

**G3. Локализация.** `assets/narutoaddon/lang/en_us.json` и `ru_ru.json`: только наши ключи
(`narutoaddon:*`, `key.narutoaddon.*`, `naruto.gui.*` — проверить, что не дублируем базовые; при дубле
базовый текст не трогаем). Автопроверка: каждый ключ из кода (`Component.translatable`) существует в
обоих файлах.

**G4. Модели/текстуры.** Заменить в моделях `narutomod:` → `narutoaddon:` для наших предметов; общие
предметы (алиасы) — ссылки оставить на `narutomod:`. Атлас (`assets/minecraft/atlases/blocks.json`) —
один на мод, наш добавить как отдельный источник, не перекрывая базовый.

**G5. Рецепты/лут/теги.** Пройтись по ошибкам из A2 и заменить ссылки на новые id
(`narutomod:kubikiribocho` → `narutoaddon:kubikiribocho` и т.п.). Всё, что ссылается на базовые
предметы, оставить `narutomod:`. Полный прогон: 0 ошибок `TagLoader`/`RecipeManager`/`LootTables` в
логе.

**G6. Датаген.** Оставить руками (как в AGENTS.md), но добавить скрипт-линтер в `tools/`, который
сверяет «все `narutoaddon:` id в JSON существуют в коде».

---

## 10. Этап H — события и тик-логика (1–2 дня)

Статус: СДЕЛАНО (56 event-классов).

Форк несёт ~40 классов `events/*` (клоны, взрывы, щит, врата, Камуи, друзья, броня, скиллы, хенге,
сенсорика…). Подключить в `NarutoAddonMod`:

- `@Mod.EventBusSubscriber(modid="narutoaddon")` уже стоит на классах, но многие завязаны на
  `NarutoMod.MOD_ID` в аннотации (заменено на литерал `"narutomod"` — **проверить все**, иначе подписки
  уходят в базу и не работают);
- события с `bus = MOD` — регистрировать в конструкторе мода;
- тик-логика (`PlayerEvents#onEntityUpdate`, регенерация, сенин, врата) — не дублировать с базовой
  (база тоже тикает чакру: убедиться, что нет двойного расхода).

Отдельный чек-лист: всё, что в форке вызывалось из `NarutoMod` (единая точка входа), должно быть
вызвано из `NarutoAddonMod` — список составить механически (`grep` по `NarutoMod.` в форке) и закрыть
позиция за позицией.

---

## 11. Этап I — конфиг (0.5 дня)

Статус: ЧАСТИЧНО (NarutoConfig присутствует; проверка не проводилась).

Свой `narutoaddon-common.toml` / `narutoaddon-client.toml` (`ModLoadingContext.registerConfig`).
Разделить:

- параметры **наших** техник (стоимость, КД, mastery-кривая) — в аддон;
- параметры чакры/стамины/базовых техник — читать из конфига базы
  (`com.sekwah.narutomod.config.NarutoConfig`), не дублировать;
- `jsutsuKeybindHoldThreshold`/`jutsuCastDelay` — базовые, использовать их.

---

## 12. Этап J — измерения и worldgen (осторожно, 1 день)

Статус: ЧАСТИЧНО (dimension json присутствует; Kamui не проверялся в игре).

`data/narutoaddon/dimension(_type)/kamui.json`: измерение Камуи нужно нашим техникам (`KamuiJutsuAbility`,
`KamuiPhaseAbility`, `KamuiEvents`, `KamuiChunkGenerator`). Сейчас файлы удалены (они ломали регистрацию).
Вернуть аккуратно: свой `narutoaddon:kamui` + чанк-генератор, зарегистрированный в `narutoaddon`. Без
структур/данжей форка (`worldgen/structure`) — их лут и биомы ссылаются на отсутствующие вещи; структуры
вернуть последними, отдельным этапом, после G5.

---

## 13. Этап K — сборка и релиз (0.5 дня)

Статус: ЧАСТИЧНО (compileJava BUILD SUCCESSFUL; релизный jar не проверялся).

- `build.gradle`: `implementation fg.deobf(files('libs/NarutoMod-1.20-1.8.1-deobf.jar'))` (для компиляции),
  плюс конфигурация, которая кладёт **universal 1.8.1** в `run/mods` для честного дев-прогона.
- Убрать устаревший комментарий про 1.11.0-deobf.
- `mods.toml`: зависимости `narutomod [1.8.1,)` + `sekclib [1.0.0,2)` (база требует sekclib — он должен
  быть у игрока).
- `pack.mcmeta` — свой (`narutoaddon resources`).
- `jar` собирается с `mixins.narutoaddon.json` в манифесте; проверить запуск не из dev-классов, а из
  готового jar в обычном профиле Forge.

---

## 14. Критерии готовности (приёмка)

1. Запуск: в списке модов `narutomod 1.8.1` + `narutoaddon`, лог без `Registry loading errors`, без
   `TagLoader/RecipeManager/LootTables` ошибок.
2. J открывает **наше** меню со всеми техниками аддона, цены и мастерство отображаются, «отключить»
   работает.
3. Комбо аддона кастуются (сервер подтверждает сообщением), тратят **базовую** чакру, полоска чакры
   одна.
4. Мастерство растёт от кастов, сохраняется после релога; дерево скилов открывается и покупается.
5. Предметы: 22 базовых — без дублей; 33 новых — в креатив-вкладке аддона, с текстурами и рецептами.
6. Клоны/рабочие/сундук, Камуи, Чибаку, стена — работают (по списку техник).
7. Нет двойных кейбиндов и двойных HUD-полос.
8. `gradlew build` зелёный; jar запускается на чистом Forge 47.1.62 + sekclib + база 1.8.1.

---

## 15. Риски и развилки

| Риск | Как снимаем |
| --- | --- |
| Прокси не покрывает какой-то путь базового конвейера (например, `HandleEnded`) | пройти по всем `instanceof` в базе (`grep` по `Ability.`) и закрыть каждый тестом |
| `sekclib` не умеет две капабилити на игроке | фолбэк: свой пакет снапшота данных |
| Базовый HUD/меню пересекается с нашим | жёсткое правило: чакра/стамина — база, экстра-состояния — аддон |
| Перекрытие комбо меняет ощущения базовых техник | список «не перекрывать» в прокси, решается поштучно |
| АП-риск: в аддоне остаются копии классов Sekwah | после этапов B–F вычистить из `narutoaddon` все 111 классов, совпадающих с базой (сейчас они там лежат копиями) — оставить только реально переиспользуемые через API базы |
| Долгая отладка в dev из-за `0.0NONE` | дев-прогон на universal-джарнике 1.8.1 в `run/mods` |

---

## 16. Порядок работ (спринты)

| Спринт | Содержание | Результат, который видно |
| --- | --- | --- |
| **S0** (0.5 д) | A1–A4 | стабильный запуск, чистый лог-чек-лист |
| **S1** (3 д) | B1–B4 + C1 (минимум) | комбо аддона кастуются в игре, тратят базовую чакру |
| **S2** (2 д) | E1–E2 | J открывает наше меню с нашими техниками |
| **S3** (3 д) | C2–C4 + D | мастерство/скиллы сохраняются и синкаются, свои пакеты |
| **S4** (3 д) | F1–F5 + G1–G5 | предметы/блоки/сущности/звуки и ресурсы без ошибок |
| **S5** (2 д) | H + E3–E4 | клоны, HUD, скилл-дерево, друзья, сундук |
| **S6** (1–2 д) | I, J, K | конфиг, Камуи, релизная сборка |

Итого ориентировочно **14–16 рабочих дней** агентской работы с постоянной проверкой `compileJava` после
каждого шага.
