package com.sekwah.narutoaddon.item;

import com.sekwah.narutoaddon.block.NarutoBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.MOD)
public class NarutoCreativeTabs {

    @SubscribeEvent
    public static void creativeModeBuildContent(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            acceptOwn(event, NarutoItems.LONELY_MARCH.get());
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            acceptOwn(event, NarutoBlocks.ITEM_BONSAI_TREE.get());
        } else if (event.getTabKey() == com.sekwah.narutomod.item.NarutoCreativeTabs.NINJA_WEAPONS.getKey()) {
            acceptOwn(event, NarutoItems.KUNAI.get());
            acceptOwn(event, NarutoItems.MINATO_KUNAI.get());
            acceptOwn(event, NarutoItems.SENBON.get());
            acceptOwn(event, NarutoItems.EXPLOSIVE_KUNAI.get());
            acceptOwn(event, NarutoItems.SHURIKEN.get());
            acceptOwn(event, NarutoBlocks.ITEM_PAPER_BOMB.get());
            acceptOwn(event, NarutoItems.SMOKE_BOMB.get());
            acceptOwn(event, NarutoItems.POISONED_NEEDLES.get());
            acceptOwn(event, NarutoItems.SCROLL_WORKBENCH.get());
            acceptOwn(event, NarutoItems.NINJA_NOTE.get());
            acceptOwn(event, NarutoItems.TRAINING_WEIGHTS.get());
            acceptOwn(event, NarutoItems.KATANA.get());
            acceptOwn(event, NarutoItems.SAMEHADA.get());
            acceptOwn(event, NarutoItems.KUBIKIRIBOCHO.get());
            acceptOwn(event, NarutoItems.HIDAN_SCYTHE.get());
            acceptOwn(event, NarutoItems.BONE_BLADE.get());
            acceptOwn(event, NarutoItems.NUIBARI.get());
            acceptOwn(event, NarutoItems.KIBA.get());
            acceptOwn(event, NarutoItems.KABUTOWARI.get());
            acceptOwn(event, NarutoItems.HIRAMEKAREI.get());
            acceptOwn(event, NarutoItems.SHIBUKI.get());
            acceptOwn(event, NarutoItems.ASURA_MISSILE.get());
            acceptOwn(event, NarutoItems.SAKE.get());
        } else if (event.getTabKey() == com.sekwah.narutomod.item.NarutoCreativeTabs.NINJA_ARMOR.getKey()) {
            acceptOwn(event, NarutoItems.RED_ANBU_MASK.get());
            acceptOwn(event, NarutoItems.YELLOW_ANBU_MASK.get());
            acceptOwn(event, NarutoItems.GREEN_ANBU_MASK.get());
            acceptOwn(event, NarutoItems.BLUE_ANBU_MASK.get());
            acceptOwn(event, NarutoItems.MIST_ANBU_MASK.get());
            acceptOwn(event, NarutoItems.FLAK_JACKET_NEW.get());
            acceptOwn(event, NarutoItems.FLAK_JACKET.get());
            acceptOwn(event, NarutoItems.ANBU_ARMOR.get());
            acceptOwn(event, NarutoItems.AKATSUKI_CLOAK.get());
            acceptOwn(event, NarutoItems.TONERI_SCARF.get());
        } else if (event.getTabKey() == com.sekwah.narutomod.item.NarutoCreativeTabs.HEADBANDS.getKey()) {
            acceptOwn(event, NarutoItems.HEADBAND_BLUE.get());
            acceptOwn(event, NarutoItems.HEADBAND_BLACK.get());
            acceptOwn(event, NarutoItems.HEADBAND_RED.get());
            acceptOwn(event, NarutoItems.HEADBAND_CUSTARD.get());
            acceptOwn(event, NarutoItems.HEADBAND_LEAF.get());
            acceptOwn(event, NarutoItems.HEADBAND_LEAF_SCRATCHED.get());
            acceptOwn(event, NarutoItems.HEADBAND_LEAF_BLACK.get());
            acceptOwn(event, NarutoItems.HEADBAND_LEAF_BLACK_SCRATCHED.get());
            acceptOwn(event, NarutoItems.HEADBAND_ROCK.get());
            acceptOwn(event, NarutoItems.HEADBAND_ROCK_SCRATCHED.get());
            acceptOwn(event, NarutoItems.HEADBAND_SAND.get());
            acceptOwn(event, NarutoItems.HEADBAND_SAND_SCRATCHED.get());
            acceptOwn(event, NarutoItems.HEADBAND_SOUND.get());
            acceptOwn(event, NarutoItems.HEADBAND_SOUND_SCRATCHED.get());
            acceptOwn(event, NarutoItems.HEADBAND_MIST.get());
            acceptOwn(event, NarutoItems.HEADBAND_MIST_SCRATCHED.get());
            acceptOwn(event, NarutoItems.HEADBAND_WATERFALL.get());
            acceptOwn(event, NarutoItems.HEADBAND_WATERFALL_SCRATCHED.get());
            acceptOwn(event, NarutoItems.HEADBAND_CLOUD.get());
            acceptOwn(event, NarutoItems.HEADBAND_CLOUD_SCRATCHED.get());
            acceptOwn(event, NarutoItems.HEADBAND_RAIN.get());
            acceptOwn(event, NarutoItems.HEADBAND_RAIN_SCRATCHED.get());
            acceptOwn(event, NarutoItems.HEADBAND_GRASS.get());
            acceptOwn(event, NarutoItems.HEADBAND_GRASS_SCRATCHED.get());
            acceptOwn(event, NarutoItems.HEADBAND_PRIDE.get());
            acceptOwn(event, NarutoItems.HEADBAND_YOUTUBE.get());
            acceptOwn(event, NarutoItems.HEADBAND_LAVA.get());
        } else if (event.getTabKey() == com.sekwah.narutomod.item.NarutoCreativeTabs.NINJA_MATERIALS.getKey()) {
            acceptOwn(event, NarutoItems.FABRIC.get());
            acceptOwn(event, NarutoItems.FABRIC_REINFORCED.get());
            acceptOwn(event, NarutoItems.FABRIC_REINFORCED_GREEN.get());
            acceptOwn(event, NarutoItems.FABRIC_REINFORCED_BLACK.get());
            acceptOwn(event, NarutoItems.ARMOR_PLATE.get());
            acceptOwn(event, NarutoItems.ARMOR_PLATE_GREEN.get());
            acceptOwn(event, NarutoItems.MILITARY_PILLS.get());
            acceptOwn(event, NarutoItems.SPINACH_PILL.get());
            acceptOwn(event, NarutoItems.CURRY_PILL.get());
            acceptOwn(event, NarutoItems.CHILI_PILL.get());
            acceptOwn(event, NarutoItems.RAMEN.get());
            acceptOwn(event, NarutoItems.RAMEN_MISO.get());
            acceptOwn(event, NarutoItems.RAMEN_PORK.get());
            acceptOwn(event, NarutoItems.RAMEN_SEAFOOD.get());
            acceptOwn(event, NarutoItems.NINJA_RATION.get());
            acceptOwn(event, NarutoItems.ONIGIRI.get());
            acceptOwn(event, NarutoItems.DANGO.get());
            acceptOwn(event, NarutoItems.CURRY.get());
            acceptOwn(event, NarutoItems.CHAKRA_SEAL.get());
            acceptOwn(event, NarutoBlocks.ITEM_SENTRY_SEAL.get());
            acceptOwn(event, NarutoItems.CHAKRA_BATTERY.get());
            acceptOwn(event, NarutoItems.CHAKRA_CRYSTAL.get());
            acceptOwn(event, NarutoItems.CHAKRA_COMPASS.get());
            acceptOwn(event, NarutoItems.SCROLL_RASENGAN.get());
            acceptOwn(event, NarutoItems.SCROLL_RASENSHURIKEN.get());
            acceptOwn(event, NarutoItems.SCROLL_EIGHT_GATES.get());
            acceptOwn(event, NarutoItems.SCROLL_KAMUI.get());
            acceptOwn(event, NarutoItems.SCROLL_SAGE_MODE.get());
            acceptOwn(event, NarutoItems.SCROLL_KIRIN.get());
            acceptOwn(event, NarutoItems.SCROLL_AMATERASU.get());
            acceptOwn(event, NarutoItems.SCROLL_IZANAGI.get());
            acceptOwn(event, NarutoItems.SCROLL_IZANAMI.get());
            acceptOwn(event, NarutoItems.SCROLL_KAGUTSUCHI.get());
            acceptOwn(event, NarutoItems.SCROLL_KAMUI_PHASING.get());
            acceptOwn(event, NarutoItems.SCROLL_KOTOAMATSUKAMI.get());
            acceptOwn(event, NarutoItems.SCROLL_SENRIGAN.get());
            acceptOwn(event, NarutoItems.SCROLL_SENRIGAN_EVASION.get());
            acceptOwn(event, NarutoItems.SCROLL_SENRIGAN_FLIGHT.get());
            acceptOwn(event, NarutoItems.SCROLL_SENRIGAN_GUIDANCE.get());
            acceptOwn(event, NarutoItems.SCROLL_SENRIGAN_SIGHT.get());
            acceptOwn(event, NarutoItems.SCROLL_SENRIGAN_PULSE.get());
            acceptOwn(event, NarutoItems.SCROLL_SENRIGAN_FREECAM.get());
            acceptOwn(event, NarutoItems.SCROLL_SENRIGAN_DEATHMARK.get());
            acceptOwn(event, NarutoItems.SCROLL_SENRIGAN_DEATHSTEP.get());
            acceptOwn(event, NarutoItems.SCROLL_SENRIGAN_WORLD.get());
            acceptOwn(event, NarutoItems.SCROLL_SUSANOO.get());
            acceptOwn(event, NarutoItems.SCROLL_TSUKUYOMI.get());
            acceptOwn(event, NarutoItems.SCROLL_RINNEGAN.get());
            acceptOwn(event, NarutoItems.SCROLL_SIX_PATHS_MODE.get());
            acceptOwn(event, NarutoItems.SCROLL_CHIBAKU_TENSEI.get());
            acceptOwn(event, NarutoItems.SCROLL_SHARINGAN.get());
            acceptOwn(event, NarutoItems.SCROLL_BYAKUGAN.get());
            acceptOwn(event, NarutoItems.SCROLL_MULTI_SHADOW_CLONE.get());
            acceptOwn(event, NarutoItems.SCROLL_CURSE_SEAL_TWO.get());
            acceptOwn(event, NarutoItems.SCROLL_SEAL_VESSEL.get());
            acceptOwn(event, NarutoItems.SCROLL_PERFECT_CLOAK.get());
            acceptOwn(event, NarutoItems.SCROLL_TAILED_BALL.get());
            acceptOwn(event, NarutoItems.SCROLL_TAILED_CLOAK.get());
            acceptOwn(event, NarutoItems.SCROLL_DIVIDED_CLOAK.get());
            acceptOwn(event, NarutoItems.SCROLL_SEAL_BREAK.get());
            acceptOwn(event, NarutoItems.SCROLL_GATE_PEACOCK.get());
            acceptOwn(event, NarutoItems.SCROLL_GATE_TIGER.get());
            acceptOwn(event, NarutoItems.SCROLL_GATE_ELEPHANT.get());
            acceptOwn(event, NarutoItems.TREASURE_MAP.get());
        }
    }

    /**
     * Adds an item to the tab only if this mod registered it.
     *
     * <p>The base 1.8.1 already fills its own tabs with everything it registers, and the addon only
     * aliases much of that content (kunai, ANBU masks, headbands, fabric, armour plates). Appending an
     * alias put a second, identical entry next to the base one, which is what read as duplicated
     * items. Items this mod registers itself - techniques, scrolls, the great swords, the sentry seal -
     * are still appended, because the base tabs cannot know about them.</p>
     */
    private static void acceptOwn(BuildCreativeModeTabContentsEvent event, net.minecraft.world.item.Item item) {
        net.minecraft.resources.ResourceLocation id =
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        if (id != null && "narutoaddon".equals(id.getNamespace())) {
            event.accept(item);
        }
    }
}