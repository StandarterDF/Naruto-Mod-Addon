package com.sekwah.narutoaddon.events;

import com.sekwah.narutoaddon.abilities.jutsus.ChakraTransferAbility;
import com.sekwah.narutoaddon.capabilities.INinjaData;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The seal itself: while it holds, every ally the thread reaches is fed from the caster's side.
 *
 * <p>The seal is stored on the caster alone as the list of allies it reaches, so a group is ticked
 * once from one side instead of once per member: threads that leave the radius, die, log out or
 * stop being allies are dropped one by one, and the last one leaving closes the seal.</p>
 *
 * <p>Pools are never averaged. The allies simply receive chakra every second; the caster keeps what
 * they had and recovers a smaller amount of their own. Averaging would make a full caster poorer for
 * helping a nearly empty ally, which is the opposite of what the seal is for.</p>
 */
@Mod.EventBusSubscriber(modid = "narutoaddon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChakraSealEvents {
    private ChakraSealEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player caster = event.player;
        ListTag stored = caster.getPersistentData().getList(ChakraTransferAbility.ALLIES_TAG, Tag.TAG_STRING);
        if (stored.isEmpty()) {
            return;
        }
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        INinjaData data = caster.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null) {
            return;
        }
        // The same reach the cast used, mastery bonus included: one formula, so a widened beam
        // never outlives the seal's own idea of how far it can hold.
        double radius = ChakraTransferAbility.range(data);

        // Every ally still in reach is kept; any thread that snapped is dropped from the seal.
        ListTag kept = new ListTag();
        List<INinjaData> allies = new ArrayList<>();
        for (int i = 0; i < stored.size(); i++) {
            UUID id;
            try {
                id = UUID.fromString(stored.getString(i));
            } catch (IllegalArgumentException malformed) {
                continue;
            }
            Player ally = level.getServer().getPlayerList().getPlayer(id);
            // A thread snaps when the ally logs out, dies, turns spectator, changes dimension,
            // walks out of reach, or stops being an ally at all (the team was left, the friendship
            // ended): the seal only ever feeds friends, and it is re-checked here rather than only
            // at the cast because the latch can outlive a whole session.
            if (ally == null || ally.level() != level || !ally.isAlive() || ally.isSpectator()
                    || caster.distanceTo(ally) > radius
                    || !ChakraTransferAbility.isAlly(caster, ally)) {
                continue;
            }
            INinjaData allyData = ally.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
            if (allyData == null) {
                continue;
            }
            kept.add(StringTag.valueOf(id.toString()));
            allies.add(allyData);
        }
        if (allies.isEmpty()) {
            // Nobody left to feed: the seal closes on its own.
            caster.getPersistentData().remove(ChakraTransferAbility.ALLIES_TAG);
            return;
        }
        if (kept.size() != stored.size()) {
            caster.getPersistentData().put(ChakraTransferAbility.ALLIES_TAG, kept);
        }

        // Additive feeding only: no pool levelling. Allies gain more than the caster, because the
        // caster is the one doing the work.
        if (caster.tickCount % 20 == 0) {
            for (INinjaData ally : allies) {
                ally.addChakra(ChakraTransferAbility.ALLY_REGEN_PER_SECOND);
            }
            data.addChakra(ChakraTransferAbility.CASTER_REGEN_PER_SECOND);
        }
    }
}
