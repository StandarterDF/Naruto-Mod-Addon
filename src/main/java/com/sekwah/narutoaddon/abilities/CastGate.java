package com.sekwah.narutoaddon.abilities;

import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.registries.NarutoRegistries;
import com.sekwah.narutoaddon.util.FrogState;
import com.sekwah.narutoaddon.events.GenjutsuEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * The rules every addon technique follows before it may start, wherever the request came from.
 *
 * <p>Cast requests reach the server by two different roads: the addon's own packets (the jutsu
 * menu) and the base mod's packets (a key combo, which the base resolves and casts through
 * {@code AbilityProxy}). The second road used to skip the addon's checks entirely, so the same
 * technique could be refused in the menu and cast anyway by its keys, or - worse - refused in one
 * place with a message that the other place never produced. This helper is called from both.</p>
 */
public final class CastGate {

    /** Persistent-data key of the last refusal, so a per-tick channel cannot flood the action bar. */
    private static final String DENIED_AT = "narutoaddon:cast_denied_at";

    /** How long between two refusal messages, in ticks. */
    private static final int MESSAGE_COOLDOWN = 20;

    private CastGate() {
    }

    /**
     * @return true when the technique may start. A refusal always tells the player why, at most once
     *         per second.
     */
    public static boolean allows(Player player, INinjaData ninjaData, Ability ability) {
        if (player == null || ninjaData == null || ability == null) {
            return true;
        }
        if (FrogState.isFrog(ninjaData)) {
            return deny(player, Component.translatable("jutsu.fail.frog"));
        }
        // A mind held by an illusion cannot draw on its chakra, except to break the illusion.
        if (GenjutsuEvents.isUnderGenjutsu(player)
                && !"KaiAbility".equals(ability.getClass().getSimpleName())) {
            return deny(player, Component.translatable("jutsu.fail.genjutsu.held"));
        }
        ResourceLocation id = NarutoRegistries.ABILITIES.getKey(ability);
        if (id == null) {
            return true;
        }
        if (ninjaData.isAbilityDisabled(id)) {
            return deny(player, Component.translatable("jutsu.fail.techdisabled",
                    Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)));
        }
        if (!AbilityUnlocks.isUnlocked(ninjaData, id)) {
            return deny(player, Component.translatable("jutsu.fail.notunlocked",
                    Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)));
        }
        // A branch technique runs on its eye. This is the road the key combos take (the base mod
        // casts through the proxy), so without the check here a closed eye only stopped the menu
        // and every learned technique still worked from the keys.
        if (!EyeRequirement.eyeIsOpen(ninjaData, id)) {
            return deny(player, EyeRequirement.refusalMessage(ninjaData, id));
        }
        return true;
    }

    private static boolean deny(Player player, Component message) {
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(DENIED_AT);
        if (now - last >= MESSAGE_COOLDOWN) {
            player.getPersistentData().putLong(DENIED_AT, now);
            player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), true);
        }
        return false;
    }
}
