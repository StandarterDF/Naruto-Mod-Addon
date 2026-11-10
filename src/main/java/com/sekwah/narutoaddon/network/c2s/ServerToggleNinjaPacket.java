package com.sekwah.narutoaddon.network.c2s;

import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.gameevents.NarutoGameEvents;
import com.sekwah.narutoaddon.item.NinjaHandbook;
import com.sekwah.narutoaddon.ranks.NinjaRanks;
import com.sekwah.narutoaddon.sounds.NarutoSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells the server that the user is entering jutsu keys
 */
public class ServerToggleNinjaPacket {

    private final boolean enableNinja;

    public ServerToggleNinjaPacket(boolean jutsuKey) {
        this.enableNinja = jutsuKey;
    }

    public static void encode(ServerToggleNinjaPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeBoolean(msg.enableNinja);
    }

    public static ServerToggleNinjaPacket decode(FriendlyByteBuf inBuffer) {
        boolean enableNinja = inBuffer.readBoolean();
        return new ServerToggleNinjaPacket(enableNinja);
    }

    public static class Handler {
        public static void handle(ServerToggleNinjaPacket msg, Supplier<NetworkEvent.Context> ctx) {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("narutoaddon");
            logger.debug("narutoaddon diag: toggle-ninja packet received, enable={}", msg.enableNinja);
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if(player == null || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    return;
                }
                var opt = player.getCapability(NinjaCapabilityHandler.NINJA_DATA);
                if (!opt.isPresent()) {
                    logger.warn("narutoaddon diag: ADDON NINJA DATA MISSING on server");
                    return;
                }
                var ninjaData = opt.orElseThrow(() -> new IllegalStateException("addon ninja data missing"));
                boolean wasNinja = ninjaData.isNinjaModeEnabled();
                logger.debug("narutoaddon diag: addon ninja data present, wasNinja={}", wasNinja);
                ninjaData.setIsNinja(msg.enableNinja);
                // Ninja mode can be turned off and on again, so "became a ninja just now" is not
                // the same as "never was one": without a saved flag every toggle would hand out
                // another handbook, which was a free book printer. The flag rides in the player
                // data, so a quick double click that sends two packets in one tick also only pays
                // once - the second packet sees it already set.
                if (!wasNinja && msg.enableNinja && !ninjaData.isHandbookGiven()) {
                    ninjaData.setHandbookGiven(true);
                    NinjaRanks.onAwakened(player, ninjaData);
                    ItemStack handbook = NinjaHandbook.create();
                    if (!player.getInventory().add(handbook)) {
                        player.drop(handbook, false);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
