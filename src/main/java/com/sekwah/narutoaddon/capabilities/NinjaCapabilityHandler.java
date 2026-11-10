package com.sekwah.narutoaddon.capabilities;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutoaddon.abilities.jutsus.SenriganFlightAbility;
import com.sekwah.narutoaddon.abilities.jutsus.SenriganSightAbility;
import com.sekwah.narutoaddon.entity.ShadowCloneEntity;
import com.sekwah.narutoaddon.item.ChakraBatteryItem;
import com.sekwah.narutoaddon.registries.NarutoRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "narutoaddon")
public class NinjaCapabilityHandler {

    public static final Capability<INinjaData> NINJA_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("narutoaddon");
            boolean basePresent = player.getCapability(
                    com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA).isPresent();
            logger.debug("narutoaddon diag: attach player, base present={}", basePresent);
            // Attach unconditionally: resolving the base capability lazily inside the provider
            // removes the ordering dependency on the base mod's own attach handler.
            event.addCapability(new ResourceLocation("narutoaddon", "ninja_data"),
                    new AddonNinjaDataProvider(player));
            logger.debug("narutoaddon diag: attached");
        }
    }

    /**
     * Capability provider that resolves the base mod's ninja data lazily, so the addon capability
     * is attached even when the base mod's own {@code AttachCapabilitiesEvent} handler has not run
     * yet (handler order is not guaranteed). The adapter is created on first
     * {@link #getCapability} call once the base capability is available.
     */
    public static class AddonNinjaDataProvider implements net.minecraftforge.common.capabilities.ICapabilityProvider,
            net.minecraftforge.common.capabilities.ICapabilitySerializable<net.minecraft.nbt.Tag> {
        private final Player player;
        private AddonNinjaData data;

        public AddonNinjaDataProvider(Player player) {
            this.player = player;
        }

        @Override
        public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
                net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.core.Direction side) {
            if (cap != NinjaCapabilityHandler.NINJA_DATA) {
                return net.minecraftforge.common.util.LazyOptional.empty();
            }
            AddonNinjaData resolved = this.resolve();
            if (resolved == null) {
                return net.minecraftforge.common.util.LazyOptional.empty();
            }
            return net.minecraftforge.common.util.LazyOptional.of(() -> resolved).cast();
        }

        /**
         * Builds the adapter on first use, once the base capability can be resolved. Returns null while
         * the base mod's own data is not attached yet, which lets the dispatcher ask again later
         * instead of caching an empty answer for the life of the player.
         */
        private AddonNinjaData resolve() {
            if (this.data == null) {
                var baseOpt = this.player.getCapability(
                        com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA);
                if (baseOpt.isPresent()) {
                    var baseData = baseOpt.orElseThrow(() -> new IllegalStateException("base ninja data missing"));
                    this.data = new AddonNinjaData(baseData, !this.player.level().isClientSide());
                    com.sekwah.narutoaddon.bridge.AbilityProxy.registerAdapter(baseData, this.data);
                }
            }
            return this.data;
        }

        // The provider, not the adapter, is what Forge keeps in the capability dispatcher, so the NBT
        // half has to live here: without it nothing would be written for this capability on save.
        @Override
        public net.minecraft.nbt.Tag serializeNBT() {
            AddonNinjaData resolved = this.resolve();
            return resolved == null ? new net.minecraft.nbt.CompoundTag() : resolved.serializeNBT();
        }

        @Override
        public void deserializeNBT(net.minecraft.nbt.Tag nbt) {
            AddonNinjaData resolved = this.resolve();
            if (resolved != null) {
                resolved.deserializeNBT(nbt);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerUpdate(TickEvent.PlayerTickEvent event) {
        if(event.phase.equals(TickEvent.Phase.END)) {
            Player player = event.player;
            player.getCapability(NINJA_DATA).ifPresent(data -> {
                if(event.side.isServer()) {
                    // Creative no longer refills the pool every tick: it regenerates ten times as
                    // fast instead (see NinjaData#updateDataServer), so a creative caster still has
                    // to pay for what they cast and gets the bar back quickly rather than instantly.
                    data.updateDataServer(player);
                    if (event.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        com.sekwah.narutoaddon.network.AddonDataSync.tick(serverPlayer);
                    }
                    if(!player.isSpectator()) {
                        // Toggles are paid for tick by tick, so a battery in the ninja's inventory
                        // gets the chance to keep the stance up once their own chakra runs low. The
                        // channeled cast, if there is one, has already had the same chance inside
                        // updateDataServer, and a pool that is already full enough is left alone.
                        if(!data.getToggleAbilityData().getAbilitiesHashSet().isEmpty()) {
                            ChakraBatteryItem.fuelUpkeep(player, data);
                        }
                        // Walk a snapshot: an ability that can no longer pay is removed right here, and
                        // removing from the live set while walking it would throw
                        // ConcurrentModificationException. Anything that has gone missing from the
                        // registry is dropped from the set rather than crashing on a null.
                        for (ResourceLocation abilityName : new ArrayList<>(data.getToggleAbilityData().getAbilitiesHashSet())) {
                            Ability ability = NarutoAbilities.byId(abilityName);
                            if (ability == null) {
                                data.getToggleAbilityData().removeAbility(abilityName);
                                continue;
                            }
                            if(ability.handleCost(player, data)) {
                                ability.performServer(player, data);
                            } else {
                                // Silent: handleCost has already shown the reason on the action bar.
                                data.getToggleAbilityData().removeAbilityEndedSilently(player, data, ability);
                            }
                        }
                    }
                } else {
                    data.updateDataClient(player);
                    if(!player.isSpectator()) {
                        data.getToggleAbilityData().getAbilitiesHashSet().forEach(abilityName -> {
                            Ability ability = NarutoAbilities.byId(abilityName);
                            if(ability instanceof Ability.Toggled toggleAbility) toggleAbility.performToggleClient(player, data);
                        });
                    }
                }
            });
        }

    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        player.getCapability(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA).ifPresent(com.sekwah.narutoaddon.bridge.AbilityProxy::unregisterAdapter);
    }

    @SubscribeEvent
    public static void playerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(NINJA_DATA).ifPresent(original -> {
            event.getEntity().getCapability(NINJA_DATA).ifPresent(future -> {
                future.deserializeNBT(original.serializeNBT());
                if (event.isWasDeath()) {
                    // Mastery is never taken away by dying: the whole cap is copied above and nothing
                    // here touches it. What death does clear is everything that was borrowed for the
                    // body that just died - the caster's meditations and their links.
                    dispelMeditationClones(event.getEntity(), original);
                    future.getMeditationData().clear();
                    future.setChakraCapacityBonus(0f);
                    // The Senrigan's borrowed flight must not be inherited by the new body: the
                    // toggle set is copied over, and without this a respawned caster would still be
                    // marked as flying.
                    SenriganFlightAbility.clearOnDeath(event.getEntity(), future);
                    SenriganSightAbility.clear(event.getEntity());
                }
            });
        });
    }

    /**
     * Pops whatever meditating clones of a dead caster can still be reached, so nothing keeps
     * feeding them from the grave. Their links go with them; the rest is cleared by the caller.
     */
    private static void dispelMeditationClones(Player caster, INinjaData original) {
        MinecraftServer server = caster.getServer();
        if (server == null) {
            return;
        }
        for (UUID cloneId : new ArrayList<>(original.getMeditationData().getLinks().keySet())) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity clone = level.getEntity(cloneId);
                if (clone instanceof ShadowCloneEntity shadowClone) {
                    shadowClone.dispel();
                    break;
                }
            }
        }
    }

    //public static final EntitySize STANDING_SIZE = EntitySize.flexible(0.1F, 0.1F);

    // For things like the transformation jutsus (make sure to trigger it manually when you change)
/*    @SubscribeEvent
    public static void playerSize(EntityEvent.Size event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            event.setNewEyeHeight(5f);
            event.setNewSize(EntityDimensions.scalable(10F, 10F));
        }
    }*/
}
