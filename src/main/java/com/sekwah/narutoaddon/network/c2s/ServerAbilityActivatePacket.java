package com.sekwah.narutoaddon.network.c2s;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutoaddon.abilities.AbilityUnlocks;
import com.sekwah.narutoaddon.abilities.ComboRequirement;
import com.sekwah.narutoaddon.abilities.EyeRequirement;
import com.sekwah.narutoaddon.abilities.ModeBus;
import com.sekwah.narutoaddon.abilities.ToggleState;
import com.sekwah.narutoaddon.abilities.jutsus.AbstractDojutsuAbility;
import com.sekwah.narutoaddon.abilities.jutsus.ChakraCloakAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutoaddon.events.ByakuganEvents;
import com.sekwah.narutoaddon.events.GenjutsuEvents;
import com.sekwah.narutoaddon.gameevents.NarutoGameEvents;
import com.sekwah.narutoaddon.registries.NarutoRegistries;
import com.sekwah.narutoaddon.util.FrogState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.function.Supplier;

/**
 * Tell the server that the user wants to cast a specific ability.
 */
public class ServerAbilityActivatePacket {

    private final int abilityId;

    public ServerAbilityActivatePacket(ResourceLocation ability) {
        this(resolveRegistryId(ability));
    }

    /**
     * Registry id of an ability, tolerating the two namespaces in use.
     *
     * <p>The packet carries a numeric registry id, and the menu builds its rows from
     * {@code Ability#ID} constants, which live in the {@code narutomod} namespace while the addon
     * registers its abilities under {@code narutoaddon}. A plain lookup therefore answers -1, so
     * the other namespace is tried before giving up.</p>
     */
    private static int resolveRegistryId(ResourceLocation ability) {
        int id = NarutoRegistries.ABILITIES.getID(ability);
        if (id >= 0) {
            return id;
        }
        if ("narutomod".equals(ability.getNamespace())) {
            return NarutoRegistries.ABILITIES.getID(new ResourceLocation("narutoaddon", ability.getPath()));
        }
        return id;
    }

    public ServerAbilityActivatePacket(int abilityId) {
        this.abilityId = abilityId;
    }

    public static void encode(ServerAbilityActivatePacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeInt(msg.abilityId);
    }

    public static ServerAbilityActivatePacket decode(FriendlyByteBuf inBuffer) {
        int abilityId = inBuffer.readInt();
        return new ServerAbilityActivatePacket(abilityId);
    }

    /**
     * @return whether this cast only stops a stance the caster already has running, which is always
     *         allowed so a seal can never trap a body in an upkeep.
     */
    private static boolean closesRunningToggle(INinjaData ninjaData, Ability ability) {
        if (ability.activationType() != Ability.ActivationType.TOGGLE) {
            return false;
        }
        ResourceLocation id = NarutoRegistries.ABILITIES.getKey(ability);
        return id != null && ToggleState.isActive(ninjaData, id);
    }

    public static class Handler {
        public static void handle(ServerAbilityActivatePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                final ServerPlayer player = ctx.get().getSender();
                if(player == null || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    return;
                }
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                    if(!ninjaData.isNinjaModeEnabled()) {
                        player.displayClientMessage(Component.translatable("jutsu.not_a_ninja").withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    // A frog has no hands for seals: no technique and no stance may start or switch
                    // while the body is worn as a toad.
                    if (FrogState.isFrog(ninjaData)) {
                        player.displayClientMessage(Component.translatable("jutsu.fail.frog").withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    Ability ability = NarutoAbilities.byId(msg.abilityId);
                    if (ability == null) {
                        return;
                    }
                    // A mind held by an illusion cannot draw on its chakra, except to break the illusion.
                    if (GenjutsuEvents.isUnderGenjutsu(player)
                            && !"KaiAbility".equals(ability.getClass().getSimpleName())) {
                        player.displayClientMessage(Component.translatable("jutsu.fail.genjutsu.held").withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    ResourceLocation abilityId = NarutoRegistries.ABILITIES.getResourceKey(ability)
                            .map(ResourceKey::location).orElse(null);
                    // A shared combo resolves to a technique of the running eye/mode; a hacked
                    // client must not cast the other branch's arm while its mode is closed.
                    if (!closesRunningToggle(ninjaData, ability) && !ComboRequirement.allows(player, ninjaData, abilityId)) {
                        return;
                    }
                    if (!AbilityUnlocks.isUnlocked(ninjaData, abilityId)) {
                        player.displayClientMessage(Component.translatable("jutsu.fail.notunlocked",
                                Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                                .withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    // Switched off in the jutsu menu: the row is grey there and the packet is skipped,
                    // but the server repeats the check so a hacked client cannot cast it anyway.
                    if (ninjaData.isAbilityDisabled(abilityId)) {
                        player.displayClientMessage(Component.translatable("jutsu.fail.techdisabled",
                                Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                                .withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    // A branch technique runs on its eye: a closed eye means no technique, however
                    // well the branch is learned. Checked on the server for both cast roads.
                    if (!EyeRequirement.allows(player, ninjaData, abilityId)) {
                        return;
                    }
                    // A Byakugan palm has closed the caster's channels: no technique may start.
                    // Closing an eye that is already burning is still allowed, or a sealed caster
                    // would be stuck paying an upkeep they cannot switch off.
                    if (ByakuganEvents.isSealed(player) && !closesRunningToggle(ninjaData, ability)) {
                        player.displayClientMessage(Component.translatable("jutsu.fail.tenketsu")
                                .withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    // The chakra cloak leaves no room in the body for another technique. Switching
                    // the cloak off (or pulling the tailed cloak over it) is still allowed, or the
                    // caster would be trapped inside their own shell.
                    if (!ChakraCloakAbility.leavesRoomFor(player, ability) && !closesRunningToggle(ninjaData, ability)) {
                        player.displayClientMessage(Component.translatable("jutsu.fail.chakra_cloak.locked",
                                Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                                .withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    if (ability.activationType() == Ability.ActivationType.INSTANT) {

                        boolean canTriggerJutsu = true;
                        if (ability  instanceof Ability.Cooldown) {
                            canTriggerJutsu = !((Ability.Cooldown) ability).checkCooldown(player, ninjaData, ability.getTranslationKey(ninjaData));
                        }

                        if(canTriggerJutsu && ability.handleCost(player, ninjaData)) {
                            if (ability.logInChat()) {
                                player.displayClientMessage(Component.translatable("jutsu.cast", Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN), true);
                            }
                            if(ability.castingSound() != null) {
                                player.level().playSound(null,
                                        player, ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                            }
                            ability.emitNoise(player);
                            ability.performServer(player, ninjaData);
                            ability.recordMastery(player, ninjaData);

                            if (ability  instanceof Ability.Cooldown) {
                               ((Ability.Cooldown) ability).registerCooldown(player, ninjaData, ability.getTranslationKey(ninjaData));
                            }
                        } else if(ability.castingFailSound() != null) {
                            player.playNotifySound( ability.castingFailSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                        }
                    } else if(ability.activationType() == Ability.ActivationType.TOGGLE) {
                        ToggleAbilityData abilityTracker = ninjaData.getToggleAbilityData();
                        HashSet<ResourceLocation> abilities = abilityTracker.getAbilitiesHashSet();
                        NarutoRegistries.ABILITIES.getResourceKey(ability).ifPresent(abilityResourceKey -> {
                            var location = abilityResourceKey.location();
                            // "Running" spans both toggle sets: an eye opened with its combo keys lives
                            // in the base layer's set, which this one never reads, so asking only this
                            // set would start a second copy instead of closing the eye.
                            if(abilities.contains(location) || ToggleState.isActive(ninjaData, location)) {
                                // A stage-based toggle may consume the re-press (the curse seal
                                // advances instead of closing) and stay in the set.
                                if (ability instanceof Ability.Retoggle retoggle
                                        && retoggle.onRetoggle(player, ninjaData)) {
                                    return;
                                }
                                // Toggle ability off
                                abilityTracker.removeAbilityEnded(player, ninjaData, ability);
                                // The entry may sit in the base layer's set; ending only this one would
                                // leave that set's tick loop paying for a stance nobody can see end.
                                ToggleState.clear(ninjaData, location);
                            } else {
                                // Toggle ability on. An eye takes the place of the one already
                                // burning in this same step; letting both sit in the set for a tick
                                // made their upkeeps close each other, sometimes leaving neither.
                                // A stance with an entry price (the Six Paths ritual, the cloaks
                                // built on another stance or on the vessel) is refused here, before
                                // anything else is touched: refusing after closing the other modes
                                // would leave the caster with neither the old stance nor the new one.
                                if (!ability.canStartToggle(player, ninjaData)) {
                                    return;
                                }
                                if (ability instanceof AbstractDojutsuAbility eye) {
                                    eye.switchFrom(player, ninjaData);
                                }
                                // One major mode at a time: the body cannot wear two full transformations.
                                ModeBus.closeOthers(player, ninjaData, abilityId);
                                abilityTracker.addAbilityStarted(player, ninjaData, ability);
                                ability.recordMastery(player, ninjaData);
                            }
                        });
                    }
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
