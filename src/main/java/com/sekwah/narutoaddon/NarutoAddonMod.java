package com.sekwah.narutoaddon;

import com.sekwah.narutoaddon.bridge.NarutoAddonAbilities;
import com.sekwah.narutoaddon.registries.NarutoRegistries;
import com.sekwah.narutoaddon.abilities.NarutoAbilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("narutoaddon")
@Mod.EventBusSubscriber(modid = "narutoaddon")
public class NarutoAddonMod {

    public static final String MOD_ID = "narutoaddon";

    public NarutoAddonMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // Advancement criteria are a static map, so they have to exist before any datapack (and
        // therefore any advancement JSON that asks for them) is read.
        com.sekwah.narutoaddon.triggers.NarutoTriggers.register();
        modEventBus.addListener(NarutoRegistries::init);
        modEventBus.addListener(NarutoAddonMod::registerCapabilities);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(NarutoAddonMod::registerCommands);
        NarutoAbilities.register(modEventBus);
        NarutoAddonAbilities.register(modEventBus);
        com.sekwah.narutoaddon.item.NarutoItems.register(modEventBus);
        com.sekwah.narutoaddon.block.NarutoBlocks.register(modEventBus);
        com.sekwah.narutoaddon.world.NarutoWorldGen.register(modEventBus);
        com.sekwah.narutoaddon.effect.NarutoEffects.register(modEventBus);
        com.sekwah.narutoaddon.entity.NarutoDataSerialisers.register(modEventBus);
        com.sekwah.narutoaddon.entity.NarutoEntities.register(modEventBus);
        com.sekwah.narutoaddon.enchantment.NarutoEnchantments.register(modEventBus);
        com.sekwah.narutoaddon.gameevents.NarutoGameEvents.register(modEventBus);
        com.sekwah.narutoaddon.loot.NarutoLootModifiers.register(modEventBus);
        com.sekwah.narutoaddon.menu.NarutoMenus.register(modEventBus);
        com.sekwah.narutoaddon.sounds.NarutoSounds.register(modEventBus);
        com.sekwah.narutoaddon.world.NarutoPaintings.register(modEventBus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, com.sekwah.narutoaddon.config.NarutoConfig.MOD_CONFIG);
        modEventBus.addListener(NarutoAddonMod::registerCapabilitySync);
        modEventBus.addListener(NarutoAddonMod::commonSetup);
        modEventBus.addListener(NarutoAddonMod::clientSetup);
    }

    public static void registerCapabilitySync(com.sekwah.sekclib.capabilitysync.capabilitysync.RegisterCapabilitySyncEvent event) {
        // The class here is the capability's data class, not the provider: sekclib reflects over its
        // fields to build the @Sync trackers. It stays AddonNinjaData even though the capability is now
        // attached through AddonNinjaDataProvider, which merely resolves the base lazily.
        event.registerPlayerCap(new net.minecraft.resources.ResourceLocation("narutoaddon", "ninja_data"),
                com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler.NINJA_DATA,
                com.sekwah.narutoaddon.capabilities.AddonNinjaData.class);
    }

    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(com.sekwah.narutoaddon.client.NarutoClientItems::register);
        event.enqueueWork(com.sekwah.narutoaddon.client.NarutoClientScreens::register);
        event.enqueueWork(com.sekwah.narutoaddon.client.gui.NarutoInGameGUI::registerEvents);
    }

    /**
     * Our own player capability, so {@code player.getCapability(NINJA_DATA)} has something to find.
     * The base mod registers its own separately; the adapter in {@code AddonNinjaData} bridges the two.
     */
    public static void registerCapabilities(net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent event) {
        event.register(com.sekwah.narutoaddon.capabilities.INinjaData.class);
    }

    public static void registerCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        com.sekwah.narutoaddon.commands.NarutoCommands.register(event.getDispatcher());
    }

    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.sekwah.narutoaddon.network.PacketHandler.init();
            com.sekwah.narutoaddon.item.NarutoDispenseItemBehavior.register();
            // Armour bonuses of the pieces aliased from the base mod: they cannot ride on the item,
            // so they are registered once the registries are frozen, see item/NarutoItems.
            com.sekwah.narutoaddon.item.NarutoItems.registerArmorBonuses();
            int added = 0;
            int overridden = 0;
            for (var entry : NarutoAddonAbilities.ABILITY.getEntries()) {
                com.sekwah.narutomod.abilities.Ability ability = entry.get();
                if (ability == null) continue;
                long combo = ability.defaultCombo();
                if (combo == -1) continue;
                ResourceLocation id = entry.getId();
                if (com.sekwah.narutoaddon.abilities.ComboGroup.isMember(id)) {
                    continue;
                }
                if (com.sekwah.narutomod.abilities.NarutoAbilities.COMBO_MAP.containsKey(combo)) {
                    overridden++;
                } else {
                    added++;
                }
                com.sekwah.narutomod.abilities.NarutoAbilities.COMBO_MAP.put(combo, id);
            }
            // The base handler resolves one ability per combo; every group is registered as the
            // channeled carrier, and the client mixin picks click/shift/hold from there.
            for (var group : com.sekwah.narutoaddon.abilities.ComboGroup.groups()) {
                if (com.sekwah.narutomod.abilities.NarutoAbilities.COMBO_MAP.containsKey(group.combo())) {
                    overridden++;
                } else {
                    added++;
                }
                com.sekwah.narutomod.abilities.NarutoAbilities.COMBO_MAP.put(group.combo(),
                        com.sekwah.narutoaddon.abilities.ComboGroup.CARRIER);
            }
            org.slf4j.LoggerFactory.getLogger("narutoaddon").info(
                    "Registered addon combos: {} added, {} overridden", added, overridden);
        });
    }
}
