package com.sekwah.narutoaddon.item;

import com.sekwah.narutoaddon.item.armor.NarutoArmorItem;
import com.sekwah.narutoaddon.item.armor.NarutoArmorMaterial;
import com.sekwah.narutoaddon.item.weapons.ExplosiveKunaiItem;
import com.sekwah.narutoaddon.item.weapons.MinatoKunaiItem;
import com.sekwah.narutoaddon.item.weapons.KunaiItem;
import com.sekwah.narutoaddon.item.weapons.SenbonItem;
import com.sekwah.narutoaddon.item.weapons.ShurikenItem;
import com.sekwah.narutoaddon.item.weapons.BoneBladeItem;
import com.sekwah.narutoaddon.item.weapons.BoneMultitoolItem;
import com.sekwah.narutoaddon.item.weapons.NuibariItem;
import com.sekwah.narutoaddon.item.weapons.SamehadaItem;
import com.sekwah.narutoaddon.item.weapons.KabutowariItem;
import com.sekwah.narutoaddon.item.weapons.KibaItem;
import com.sekwah.narutoaddon.item.weapons.TechniqueSwordItem;
import com.sekwah.narutoaddon.item.StatScrollItem;
import com.sekwah.narutoaddon.sounds.NarutoSounds;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class NarutoItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "narutoaddon");

    public static final RegistryObject<Item> MODEL_TEST = com.sekwah.narutomod.item.NarutoItems.MODEL_TEST;

    // Weapons
    public static final RegistryObject<Item> KUNAI = com.sekwah.narutomod.item.NarutoItems.KUNAI;

    public static final RegistryObject<Item> ASURA_MISSILE = ITEMS.register("asura_missile",
            () -> new AsuraMissileItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SENBON = com.sekwah.narutomod.item.NarutoItems.SENBON;

    public static final RegistryObject<Item> EXPLOSIVE_KUNAI = com.sekwah.narutomod.item.NarutoItems.EXPLOSIVE_KUNAI;

    public static final RegistryObject<Item> MINATO_KUNAI = ITEMS.register("minato_kunai", ()
            -> new MinatoKunaiItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SHURIKEN = com.sekwah.narutomod.item.NarutoItems.SHURIKEN;

    /**
     * A throwable smoke bomb: the smoke screen technique in the pocket, for anyone who has not
     * learned the combo or would rather keep the chakra.
     */
    public static final RegistryObject<Item> SMOKE_BOMB = ITEMS.register("smoke_bomb", ()
            -> new SmokeBombItem(new Item.Properties().stacksTo(16)));

    /**
     * A field ration: a full meal in a paper wrap, eaten cold and on the move.
     */
    public static final RegistryObject<Item> NINJA_RATION = ITEMS.register("ninja_ration",
            () -> new NinjaRationItem(new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder().nutrition(9).saturationMod(0.67f).build())));

    /**
     * Throwing needles dipped in poison, for the fights where chakra is too precious to spend.
     */
    public static final RegistryObject<Item> POISONED_NEEDLES = ITEMS.register("poisoned_needles",
            () -> new PoisonedNeedleItem(new Item.Properties().stacksTo(16)));

    /**
     * A crafting table folded into a scroll: the field kit of a ninja who cannot carry a bench.
     */
    public static final RegistryObject<Item> SCROLL_WORKBENCH = ITEMS.register("scroll_workbench",
            () -> new ScrollWorkbenchItem(new Item.Properties().stacksTo(1)));

    /** A scrap of lore found in chests: it tells a story and is then gone. */
    public static final RegistryObject<Item> NINJA_NOTE = ITEMS.register("ninja_note",
            () -> new NinjaNoteItem(new Item.Properties().stacksTo(16)));

    /** Training weights: heavier steps and a heavier stamina bill, for the body that wants the work. */
    public static final RegistryObject<Item> TRAINING_WEIGHTS = ITEMS.register("training_weights",
            () -> new TrainingWeightsItem(new Item.Properties().stacksTo(2)));

    /** Toneri's scarf: a keepsake that hardens the wearer and quickens passive nature-energy gathering. */
    public static final RegistryObject<Item> TONERI_SCARF = ITEMS.register("toneri_scarf",
            () -> new ToneriScarfItem(new Item.Properties().stacksTo(1)));

    /**
     * The material the better recipes are built from: a crystal that can be charged with chakra.
     */
    public static final RegistryObject<Item> CHAKRA_CRYSTAL = ITEMS.register("chakra_crystal", ()
            -> new ChakraCrystalItem(new Item.Properties().stacksTo(16)));

    /**
     * A compass that points at a place the ninja chose rather than at the world spawn.
     */
    public static final RegistryObject<Item> CHAKRA_COMPASS = ITEMS.register("chakra_compass", ()
            -> new ChakraCompassItem(new Item.Properties().stacksTo(1)));


    // Armor

    /**
     * Bonuses the armour pieces carry, see {@code events/NarutoArmorEvents}. They are ninja kit, so
     * what they give is chakra and the body, not armour points.
     */
    /** A village headband helps its ninja keep hold of their chakra. */
    private static final NarutoArmorItem.Bonus HEADBAND_BONUS = new NarutoArmorItem.Bonus(0.0f, 0.0f, 0.4f);
    /** An ANBU mask is worn by shinobi trained to hold far more than they show. */
    private static final NarutoArmorItem.Bonus ANBU_MASK_BONUS = new NarutoArmorItem.Bonus(30.0f, 0.0f, 0.0f);
    /** The standard flak jacket takes a hit so its wearer does not have to. */
    private static final NarutoArmorItem.Bonus FLAK_BONUS = new NarutoArmorItem.Bonus(0.0f, 2.0f, 0.0f);
    /** The jonin flak is built over the same idea with better plate. */
    private static final NarutoArmorItem.Bonus JONIN_FLAK_BONUS = new NarutoArmorItem.Bonus(0.0f, 4.0f, 0.0f);
    /** ANBU field armour: a little less plate than a flak, a little more chakra discipline. */
    private static final NarutoArmorItem.Bonus ANBU_ARMOR_BONUS = new NarutoArmorItem.Bonus(20.0f, 2.0f, 0.2f);
    /** The Akatsuki cloak: S-rank chakra, and plenty of it. */
    private static final NarutoArmorItem.Bonus AKATSUKI_BONUS = new NarutoArmorItem.Bonus(50.0f, 0.0f, 0.5f);

    public static final RegistryObject<Item> RED_ANBU_MASK = com.sekwah.narutomod.item.NarutoItems.RED_ANBU_MASK;

    public static final RegistryObject<Item> YELLOW_ANBU_MASK = com.sekwah.narutomod.item.NarutoItems.YELLOW_ANBU_MASK;

    public static final RegistryObject<Item> GREEN_ANBU_MASK = com.sekwah.narutomod.item.NarutoItems.GREEN_ANBU_MASK;

    public static final RegistryObject<Item> BLUE_ANBU_MASK = com.sekwah.narutomod.item.NarutoItems.BLUE_ANBU_MASK;

    public static final RegistryObject<Item> MIST_ANBU_MASK = com.sekwah.narutomod.item.NarutoItems.MIST_ANBU_MASK;

    public static final RegistryObject<Item> FLAK_JACKET_NEW = com.sekwah.narutomod.item.NarutoItems.FLAK_JACKET_NEW;

    public static final RegistryObject<Item> FLAK_JACKET = com.sekwah.narutomod.item.NarutoItems.FLAK_JACKET;

    public static final RegistryObject<Item> ANBU_ARMOR = com.sekwah.narutomod.item.NarutoItems.ANBU_ARMOR;

    public static final RegistryObject<Item> AKATSUKI_CLOAK = com.sekwah.narutomod.item.NarutoItems.AKATSUKI_CLOAK;

    public static final RegistryObject<Item> LONELY_MARCH = com.sekwah.narutomod.item.NarutoItems.LONELY_MARCH;

    public static final RegistryObject<Item> FABRIC = com.sekwah.narutomod.item.NarutoItems.FABRIC;

    public static final RegistryObject<Item> FABRIC_REINFORCED = com.sekwah.narutomod.item.NarutoItems.FABRIC_REINFORCED;

    public static final RegistryObject<Item> FABRIC_REINFORCED_GREEN = com.sekwah.narutomod.item.NarutoItems.FABRIC_REINFORCED_GREEN;

    public static final RegistryObject<Item> FABRIC_REINFORCED_BLACK = com.sekwah.narutomod.item.NarutoItems.FABRIC_REINFORCED_BLACK;

    public static final RegistryObject<Item> ARMOR_PLATE = com.sekwah.narutomod.item.NarutoItems.ARMOR_PLATE;

    public static final RegistryObject<Item> ARMOR_PLATE_GREEN = com.sekwah.narutomod.item.NarutoItems.ARMOR_PLATE_GREEN;

    /**
     * Hands the armour bonuses of the <em>aliased</em> pieces to {@code ArmorBonuses}.
     *
     * <p>Masks, flak jackets, the ANBU field armour and the Akatsuki cloak are the base mod's items,
     * so they cannot carry a {@link NarutoArmorItem.Bonus} on the item itself - their class is the
     * base one, which has no such field. Without this table those pieces would give nothing at all,
     * and the mod's armour would be plain plate. Headbands are registered here and keep their bonus
     * on the item, so they are not listed.</p>
     *
     * <p>Called from {@code NarutoAddonMod#commonSetup}, after the registries are frozen, because
     * {@code RegistryObject#get()} is only answerable by then.</p>
     */
    public static void registerArmorBonuses() {
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(RED_ANBU_MASK, ANBU_MASK_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(YELLOW_ANBU_MASK, ANBU_MASK_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(GREEN_ANBU_MASK, ANBU_MASK_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(BLUE_ANBU_MASK, ANBU_MASK_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(MIST_ANBU_MASK, ANBU_MASK_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(FLAK_JACKET, FLAK_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(FLAK_JACKET_NEW, JONIN_FLAK_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(ANBU_ARMOR, ANBU_ARMOR_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(AKATSUKI_CLOAK, AKATSUKI_BONUS);

        // The village headbands. Same bonuses the fork gives them: Leaf pours chakra back, Sand and
        // Rock teach the body to take a hit, Cloud and Sound store more, Mist and Waterfall and Rain
        // pour chakra back faster.
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_BLUE, HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_BLACK, HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_RED, HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_CUSTARD, HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_LEAF, LEAF_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_LEAF_SCRATCHED, LEAF_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_LEAF_BLACK, LEAF_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_LEAF_BLACK_SCRATCHED, LEAF_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_ROCK, ROCK_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_ROCK_SCRATCHED, ROCK_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_SAND, SAND_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_SAND_SCRATCHED, SAND_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_SOUND, SOUND_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_SOUND_SCRATCHED, SOUND_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_MIST, MIST_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_MIST_SCRATCHED, MIST_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_WATERFALL, MIST_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_WATERFALL_SCRATCHED, MIST_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_CLOUD, CLOUD_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_CLOUD_SCRATCHED, CLOUD_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_RAIN, MIST_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_RAIN_SCRATCHED, MIST_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_GRASS, LEAF_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_GRASS_SCRATCHED, LEAF_HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_PRIDE, HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_YOUTUBE, HEADBAND_BONUS);
        com.sekwah.narutoaddon.item.armor.ArmorBonuses.register(HEADBAND_LAVA, ROCK_HEADBAND_BONUS);
    }

    /**
     * Field rations: chakra and stamina back instantly, plus a short burst of speed and haste.
     */
    public static final RegistryObject<Item> MILITARY_PILLS = ITEMS.register("military_pills",
            () -> new MilitaryPillItem(new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder().nutrition(6).saturationMod(0.6f).build())));

    /**
     * The three Akimichi clan pills: spinach (green), curry (yellow) and chili (red).
     */
    public static final RegistryObject<Item> SPINACH_PILL = ITEMS.register("spinach_pill",
            () -> new AkimichiPillItem(AkimichiPillItem.PillType.SPINACH,
                    new Item.Properties().stacksTo(16)
                            .food(new FoodProperties.Builder().nutrition(1).saturationMod(0.1f).fast().alwaysEat().build())));
    public static final RegistryObject<Item> CURRY_PILL = ITEMS.register("curry_pill",
            () -> new AkimichiPillItem(AkimichiPillItem.PillType.CURRY,
                    new Item.Properties().stacksTo(16)
                            .food(new FoodProperties.Builder().nutrition(1).saturationMod(0.1f).fast().alwaysEat().build())));
    public static final RegistryObject<Item> CHILI_PILL = ITEMS.register("chili_pill",
            () -> new AkimichiPillItem(AkimichiPillItem.PillType.CHILI,
                    new Item.Properties().stacksTo(16)
                            .food(new FoodProperties.Builder().nutrition(1).saturationMod(0.1f).fast().alwaysEat().build())));

    /**
     * Ichiraku's finest. A proper meal rather than a snack: more than a full hunger bar's worth in
     * one bowl, and enough saturation to keep the caster topped up for a while afterwards.
     */
    public static final RegistryObject<Item> RAMEN = ITEMS.register("ramen",
            () -> new Item(new Item.Properties().stacksTo(16).craftRemainder(Items.BOWL)
                    .food(new FoodProperties.Builder().nutrition(8).saturationMod(1.2f).build())));

    // ------------------------------------------------------------------------------------------
    // Ninja cooking: food that puts a little back into the pools, see ChakraFoodItem.
    // ------------------------------------------------------------------------------------------

    /** Miso ramen: the gentle bowl, with a short regeneration to settle the stomach. */
    public static final RegistryObject<Item> RAMEN_MISO = ITEMS.register("ramen_miso",
            () -> new ChakraFoodItem(new Item.Properties().stacksTo(8).craftRemainder(Items.BOWL)
                    .food(new FoodProperties.Builder().nutrition(9).saturationMod(1.3f)
                            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1.0f)
                            .build()), 15.0f, 0.0f));

    /** Pork ramen: the heavy bowl, with a few seconds of strength behind it. */
    public static final RegistryObject<Item> RAMEN_PORK = ITEMS.register("ramen_pork",
            () -> new ChakraFoodItem(new Item.Properties().stacksTo(8).craftRemainder(Items.BOWL)
                    .food(new FoodProperties.Builder().nutrition(10).saturationMod(1.4f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0), 1.0f)
                            .build()), 20.0f, 10.0f));

    /** Seafood ramen: the light bowl, with a breath of water in it. */
    public static final RegistryObject<Item> RAMEN_SEAFOOD = ITEMS.register("ramen_seafood",
            () -> new ChakraFoodItem(new Item.Properties().stacksTo(8).craftRemainder(Items.BOWL)
                    .food(new FoodProperties.Builder().nutrition(9).saturationMod(1.2f)
                            .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0), 1.0f)
                            .build()), 15.0f, 5.0f));

    /** Onigiri: a pocket snack, eaten fast, for a runner out of stamina rather than chakra. */
    public static final RegistryObject<Item> ONIGIRI = ITEMS.register("onigiri",
            () -> new ChakraFoodItem(new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder().nutrition(4).saturationMod(0.4f).fast().build()),
                    5.0f, 20.0f));

    /** Dango: sweet, small, and mostly for closing a scratch between fights. */
    public static final RegistryObject<Item> DANGO = ITEMS.register("dango",
            () -> new ChakraFoodItem(new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder().nutrition(3).saturationMod(0.5f).fast()
                            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 80, 0), 1.0f)
                            .build()), 8.0f, 0.0f));

    /** Curry of life: a full meal that leaves the eater stronger and quicker for a short while. */
    public static final RegistryObject<Item> CURRY = ITEMS.register("curry",
            () -> new ChakraFoodItem(new Item.Properties().stacksTo(8).craftRemainder(Items.BOWL)
                    .food(new FoodProperties.Builder().nutrition(12).saturationMod(1.5f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0), 1.0f)
                            .build()), 30.0f, 20.0f));    /**
     * The talisman every clone technique burns, see
     * {@code ShadowCloneJutsuAbility#handleCost(Player, INinjaData, int)}. One per summon, no matter
     * how many clones come out of it.
     */
    public static final RegistryObject<Item> CHAKRA_SEAL = ITEMS.register("chakra_seal",
            () -> new Item(new Item.Properties()));

    /**
     * A store of chakra that binds itself to the first ninja to use it, see
     * {@link ChakraBatteryItem}.
     */
    public static final RegistryObject<Item> CHAKRA_BATTERY = ITEMS.register("chakra_battery",
            () -> new ChakraBatteryItem(new Item.Properties().stacksTo(1)));

    /**
     * Scrolls: the knowledge of the deepest techniques, found in the world rather than bought with
     * points. See {@link JutsuScrollItem} and {@code Skill#requiresScroll()}.
     */
    public static final RegistryObject<Item> SCROLL_RASENGAN = createScroll("rasengan");
    public static final RegistryObject<Item> SCROLL_RASENSHURIKEN = createScroll("rasenshuriken", "rasengan");
    public static final RegistryObject<Item> SCROLL_EIGHT_GATES = createScroll("eight_gates", null, "gate_peacock", "gate_tiger", "gate_elephant");
    public static final RegistryObject<Item> SCROLL_GATE_PEACOCK = createScroll("gate_peacock", "eight_gates");
    public static final RegistryObject<Item> SCROLL_GATE_TIGER = createScroll("gate_tiger", "eight_gates");
    public static final RegistryObject<Item> SCROLL_GATE_ELEPHANT = createScroll("gate_elephant", "eight_gates");
    public static final RegistryObject<Item> SCROLL_KAMUI = createScroll("kamui", "sharingan");
    public static final RegistryObject<Item> SCROLL_KAMUI_PHASING = createScroll("kamui_phasing", "sharingan");
    public static final RegistryObject<Item> SCROLL_AMATERASU = createScroll("amaterasu", "sharingan");
    public static final RegistryObject<Item> SCROLL_KAGUTSUCHI = createScroll("kagutsuchi", "sharingan");
    public static final RegistryObject<Item> SCROLL_SUSANOO = createScroll("susanoo", "sharingan");
    public static final RegistryObject<Item> SCROLL_TSUKUYOMI = createScroll("tsukuyomi", "sharingan");
    public static final RegistryObject<Item> SCROLL_IZANAGI = createScroll("izanagi", "sharingan");
    public static final RegistryObject<Item> SCROLL_IZANAMI = createScroll("izanami", "sharingan");
    public static final RegistryObject<Item> SCROLL_SAGE_MODE = createScroll("sage_mode", "senjutsu_charging", "curse_seal_two", "seal_vessel", "perfect_cloak", "tailed_cloak", "tailed_ball", "divided_cloak", "seal_break");
    public static final RegistryObject<Item> SCROLL_KIRIN = createScroll("kirin", "lightning_mode");
    public static final RegistryObject<Item> SCROLL_RINNEGAN = createScroll("rinnegan", "sharingan");
    public static final RegistryObject<Item> SCROLL_CHIBAKU_TENSEI = createScroll("chibaku_tensei", "rinnegan");
    public static final RegistryObject<Item> SCROLL_SHARINGAN = createScroll("sharingan", null, "kamui", "kamui_phasing", "amaterasu", "kagutsuchi", "susanoo", "tsukuyomi", "izanagi", "izanami", "kirin");
    public static final RegistryObject<Item> SCROLL_BYAKUGAN = createScroll("byakugan");
    public static final RegistryObject<Item> SCROLL_SENRIGAN = createScroll("senrigan", null, "senrigan_sight", "senrigan_flight", "senrigan_evasion", "senrigan_guidance", "senrigan_pulse", "senrigan_freecam", "senrigan_deathmark", "senrigan_deathstep", "senrigan_world");
    public static final RegistryObject<Item> SCROLL_SENRIGAN_SIGHT = createScroll("senrigan_sight", "senrigan");
    public static final RegistryObject<Item> SCROLL_SENRIGAN_FLIGHT = createScroll("senrigan_flight", "senrigan");
    public static final RegistryObject<Item> SCROLL_SENRIGAN_EVASION = createScroll("senrigan_evasion", "senrigan");
    public static final RegistryObject<Item> SCROLL_SENRIGAN_GUIDANCE = createScroll("senrigan_guidance", "senrigan");
    public static final RegistryObject<Item> SCROLL_SENRIGAN_PULSE = createScroll("senrigan_pulse", "senrigan");
    public static final RegistryObject<Item> SCROLL_SENRIGAN_FREECAM = createScroll("senrigan_freecam", "senrigan");
    public static final RegistryObject<Item> SCROLL_SENRIGAN_DEATHMARK = createScroll("senrigan_deathmark", "senrigan");
    public static final RegistryObject<Item> SCROLL_SENRIGAN_DEATHSTEP = createScroll("senrigan_deathstep", "senrigan");
    public static final RegistryObject<Item> SCROLL_SENRIGAN_WORLD = createScroll("senrigan_world", "senrigan");
    public static final RegistryObject<Item> SCROLL_KOTOAMATSUKAMI = createScroll("kotoamatsukami", "sharingan");
    public static final RegistryObject<Item> SCROLL_SIX_PATHS_MODE = createScroll("six_paths_mode", "rinnegan");
    public static final RegistryObject<Item> SCROLL_MULTI_SHADOW_CLONE = createScroll("multi_shadow_clone", "shadow_clone");
    public static final RegistryObject<Item> SCROLL_CURSE_SEAL_TWO = createScroll("curse_seal_two", "curse_seal_one");
    public static final RegistryObject<Item> SCROLL_SEAL_VESSEL = createScroll("seal_vessel", "perfect_cloak", "seal_break");
    public static final RegistryObject<Item> SCROLL_SEAL_BREAK = createScroll("seal_break", "seal_vessel");
    public static final RegistryObject<Item> SCROLL_PERFECT_CLOAK = createScroll("perfect_cloak", "tailed_cloak");
    public static final RegistryObject<Item> SCROLL_TAILED_CLOAK = createScroll("tailed_cloak", "chakra_cloak", "tailed_ball", "divided_cloak");
    public static final RegistryObject<Item> SCROLL_TAILED_BALL = createScroll("tailed_ball", "tailed_cloak");
    public static final RegistryObject<Item> SCROLL_DIVIDED_CLOAK = createScroll("divided_cloak", "tailed_cloak");

    // Stat scrolls: permanent passive bonuses from exploring the world.
    public static final RegistryObject<Item> SCROLL_CHAKRA = ITEMS.register("scroll_chakra",
            () -> new StatScrollItem(new Item.Properties().stacksTo(1), StatScrollItem.Stat.CHAKRA));
    public static final RegistryObject<Item> SCROLL_STAMINA = ITEMS.register("scroll_stamina",
            () -> new StatScrollItem(new Item.Properties().stacksTo(1), StatScrollItem.Stat.STAMINA));
    public static final RegistryObject<Item> SCROLL_CHAKRA_REGEN = ITEMS.register("scroll_chakra_regen",
            () -> new StatScrollItem(new Item.Properties().stacksTo(1), StatScrollItem.Stat.CHAKRA_REGEN));
    public static final RegistryObject<Item> SCROLL_STAMINA_REGEN = ITEMS.register("scroll_stamina_regen",
            () -> new StatScrollItem(new Item.Properties().stacksTo(1), StatScrollItem.Stat.STAMINA_REGEN));
    public static final RegistryObject<Item> SCROLL_VITALITY = ITEMS.register("scroll_vitality",
            () -> new StatScrollItem(new Item.Properties().stacksTo(1), StatScrollItem.Stat.VITALITY));
    public static final RegistryObject<Item> SCROLL_STRENGTH = ITEMS.register("scroll_strength",
            () -> new StatScrollItem(new Item.Properties().stacksTo(1), StatScrollItem.Stat.STRENGTH));

    /**
     * A scrap of somebody else's map: right-click to turn it into a real filled map pointing at the
     * nearest village, mineshaft, shipwreck, ruined portal or ocean ruin. See {@link TreasureMapItem}.
     */
    public static final RegistryObject<Item> TREASURE_MAP = ITEMS.register("treasure_map",
            () -> new TreasureMapItem(new Item.Properties()));

    public static final RegistryObject<Item> KATANA = com.sekwah.narutomod.item.NarutoItems.KATANA;

    /**
     * Samehada: quick enough for a chain to be kept alive, and it takes chakra off whatever it cuts.
     * It is a legendary blade, so it lasts far longer than the rank-and-file katanas.
     */
    public static final RegistryObject<Item> SAMEHADA = ITEMS.register("samehada",
            () -> new SamehadaItem(NinjaTier.LEGENDARY, 4, -2.4F, new Item.Properties().stacksTo(1),
                    0.5f, 20, 20 * 4));

    /**
     * Kubikiribocho: slow, heavy, and the chain is worth far more per step.
     */
    public static final RegistryObject<Item> KUBIKIRIBOCHO = ITEMS.register("kubikiribocho",
            () -> new ComboSwordItem(NinjaTier.KATANA, 6, -3.2F, new Item.Properties().stacksTo(1),
                    0.75f, 20, 20 * 5, 0.0f, 3, true));

    /** Hidan's scythe: a contract with Jashin, growing sharper with kills. */
    public static final RegistryObject<Item> HIDAN_SCYTHE = ITEMS.register("hidan_scythe", ()
            -> new HidanScytheItem(net.minecraft.world.item.Tiers.NETHERITE, 6, -2.8f,
                    new Item.Properties()));

    /** The bone blade grown by the Camellia Dance: a handful of cuts, no durability, no enchanting. */
    public static final RegistryObject<Item> BONE_BLADE = ITEMS.register("bone_blade",
            () -> new BoneBladeItem(new Item.Properties().stacksTo(1)));

    /** Nuibari: a thin needle blade that threads victims together and fires reinforced needles. */
    public static final RegistryObject<Item> NUIBARI = ITEMS.register("nuibari",
            () -> new NuibariItem(new Item.Properties().stacksTo(1)));

    /** Kiba: twin lightning blades that strike with thunder in a storm and keep their wearer quick. */
    public static final RegistryObject<Item> KIBA = ITEMS.register("kiba",
            () -> new KibaItem(new Item.Properties().stacksTo(1)));

    /** Kabutowari: a hammer-axe that shatters any guard, ignoring shields and most armour. */
    public static final RegistryObject<Item> KABUTOWARI = ITEMS.register("kabutowari",
            () -> new KabutowariItem(new Item.Properties().stacksTo(1)));

    /** The bone multitool grown by the Bone Tools technique: one implement that digs, fights,
     * tills, strips and flattens, with a short life of its own. */
    public static final RegistryObject<Item> BONE_MULTITOOL = ITEMS.register("bone_multitool",
            () -> new BoneMultitoolItem(NinjaTier.KATANA));
    /** Hiramekarei: right-click shoulders the hammer for ten seconds of heavier blows. */
    public static final RegistryObject<Item> HIRAMEKAREI = ITEMS.register("hiramekarei",
            () -> new TechniqueSwordItem(NinjaTier.KATANA, 5, -2.6F, new Item.Properties().stacksTo(1),
                    0.75f, 20, 20 * 4, TechniqueSwordItem.Art.HIRAMEKAREI));

    /** Shibuki: right-click sets off the blast scroll; sneak to dig a short tunnel instead. */
    public static final RegistryObject<Item> SHIBUKI = ITEMS.register("shibuki",
            () -> new TechniqueSwordItem(NinjaTier.KATANA, 4, -2.4F, new Item.Properties().stacksTo(1),
                    0.5f, 20, 20 * 4, TechniqueSwordItem.Art.SHIBUKI));

    /** A bottle of rice wine: the price of the Drunken Fist. */
    public static final RegistryObject<Item> SAKE = ITEMS.register("sake",
            () -> new SakeItem(new Item.Properties().stacksTo(16)));

    // Ninja Masks

    private static RegistryObject<Item> createScroll(String knowledge) {
        return createScroll(knowledge, null);
    }

    private static RegistryObject<Item> createScroll(String knowledge, String base, String... pool) {
        List<ResourceLocation> poolIds = new ArrayList<>();
        for (String id : pool) {
            poolIds.add(new ResourceLocation("narutomod", id));
        }
        ResourceLocation baseId = base == null ? null : new ResourceLocation("narutomod", base);
        return ITEMS.register("scroll_" + knowledge, () -> new JutsuScrollItem(
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                new ResourceLocation("narutomod", knowledge), baseId, poolIds));
    }

    /**
     * Every village trains its ninja differently, and the headband carries a little of that: Leaf
     * pours chakra back, Sand teaches endurance, Rock teaches taking a hit, Cloud stores more.
     */
    private static final NarutoArmorItem.Bonus LEAF_HEADBAND_BONUS = HEADBAND_BONUS;
    private static final NarutoArmorItem.Bonus SAND_HEADBAND_BONUS = new NarutoArmorItem.Bonus(0.0f, 2.0f, 0.0f);
    private static final NarutoArmorItem.Bonus ROCK_HEADBAND_BONUS = new NarutoArmorItem.Bonus(0.0f, 4.0f, 0.0f);
    private static final NarutoArmorItem.Bonus CLOUD_HEADBAND_BONUS = new NarutoArmorItem.Bonus(25.0f, 0.0f, 0.0f);
    private static final NarutoArmorItem.Bonus MIST_HEADBAND_BONUS = new NarutoArmorItem.Bonus(0.0f, 0.0f, 0.7f);
    private static final NarutoArmorItem.Bonus SOUND_HEADBAND_BONUS = new NarutoArmorItem.Bonus(40.0f, 0.0f, 0.0f);

    // Headbands are the base mod's items: it registers all 27 of them and fills its own headband tab,
    // so the addon aliases them instead of registering a second set (which used to show up as two
    // identical headbands everywhere). Their bonuses are handed over in registerArmorBonuses below.

    //    BLANK(0, "blankBlueHeadBand", "headband_blank);"),
    public static final RegistryObject<Item> HEADBAND_BLUE = com.sekwah.narutomod.item.NarutoItems.HEADBAND_BLUE;
    //    BLANK_BLACK(1, "blankBlackHeadBand", "headband_black);"),
    public static final RegistryObject<Item> HEADBAND_BLACK = com.sekwah.narutomod.item.NarutoItems.HEADBAND_BLACK;
    //    BLANK_RED(2, "blankRedHeadBand", "headband_kred);"),
    public static final RegistryObject<Item> HEADBAND_RED = com.sekwah.narutomod.item.NarutoItems.HEADBAND_RED;
    //    CUSTARD(4, "custardHeadband", "headband_custard);"),
    public static final RegistryObject<Item> HEADBAND_CUSTARD = com.sekwah.narutomod.item.NarutoItems.HEADBAND_CUSTARD;


    public static final RegistryObject<Item> HEADBAND_LEAF = com.sekwah.narutomod.item.NarutoItems.HEADBAND_LEAF;
    public static final RegistryObject<Item> HEADBAND_LEAF_SCRATCHED = com.sekwah.narutomod.item.NarutoItems.HEADBAND_LEAF_SCRATCHED;
    public static final RegistryObject<Item> HEADBAND_LEAF_BLACK = com.sekwah.narutomod.item.NarutoItems.HEADBAND_LEAF_BLACK;
    public static final RegistryObject<Item> HEADBAND_LEAF_BLACK_SCRATCHED = com.sekwah.narutomod.item.NarutoItems.HEADBAND_LEAF_BLACK_SCRATCHED;

    public static final RegistryObject<Item> HEADBAND_ROCK = com.sekwah.narutomod.item.NarutoItems.HEADBAND_ROCK;
    public static final RegistryObject<Item> HEADBAND_ROCK_SCRATCHED = com.sekwah.narutomod.item.NarutoItems.HEADBAND_ROCK_SCRATCHED;

    public static final RegistryObject<Item> HEADBAND_SAND = com.sekwah.narutomod.item.NarutoItems.HEADBAND_SAND;
    public static final RegistryObject<Item> HEADBAND_SAND_SCRATCHED = com.sekwah.narutomod.item.NarutoItems.HEADBAND_SAND_SCRATCHED;

    public static final RegistryObject<Item> HEADBAND_SOUND = com.sekwah.narutomod.item.NarutoItems.HEADBAND_SOUND;
    public static final RegistryObject<Item> HEADBAND_SOUND_SCRATCHED = com.sekwah.narutomod.item.NarutoItems.HEADBAND_SOUND_SCRATCHED;

    public static final RegistryObject<Item> HEADBAND_MIST = com.sekwah.narutomod.item.NarutoItems.HEADBAND_MIST;
    public static final RegistryObject<Item> HEADBAND_MIST_SCRATCHED = com.sekwah.narutomod.item.NarutoItems.HEADBAND_MIST_SCRATCHED;

    public static final RegistryObject<Item> HEADBAND_WATERFALL = com.sekwah.narutomod.item.NarutoItems.HEADBAND_WATERFALL;
    public static final RegistryObject<Item> HEADBAND_WATERFALL_SCRATCHED = com.sekwah.narutomod.item.NarutoItems.HEADBAND_WATERFALL_SCRATCHED;

    public static final RegistryObject<Item> HEADBAND_CLOUD = com.sekwah.narutomod.item.NarutoItems.HEADBAND_CLOUD;
    public static final RegistryObject<Item> HEADBAND_CLOUD_SCRATCHED = com.sekwah.narutomod.item.NarutoItems.HEADBAND_CLOUD_SCRATCHED;

    public static final RegistryObject<Item> HEADBAND_RAIN = com.sekwah.narutomod.item.NarutoItems.HEADBAND_RAIN;
    public static final RegistryObject<Item> HEADBAND_RAIN_SCRATCHED = com.sekwah.narutomod.item.NarutoItems.HEADBAND_RAIN_SCRATCHED;

    public static final RegistryObject<Item> HEADBAND_GRASS = com.sekwah.narutomod.item.NarutoItems.HEADBAND_GRASS;
    public static final RegistryObject<Item> HEADBAND_GRASS_SCRATCHED = com.sekwah.narutomod.item.NarutoItems.HEADBAND_GRASS_SCRATCHED;


    public static final RegistryObject<Item> HEADBAND_PRIDE = com.sekwah.narutomod.item.NarutoItems.HEADBAND_PRIDE;
    public static final RegistryObject<Item> HEADBAND_YOUTUBE = com.sekwah.narutomod.item.NarutoItems.HEADBAND_YOUTUBE;
    public static final RegistryObject<Item> HEADBAND_LAVA = com.sekwah.narutomod.item.NarutoItems.HEADBAND_LAVA;


    // Materials

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
