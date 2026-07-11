package com.levi.aeroradar.server;

import com.levi.aeroradar.AeroRadar;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Drives the optional server-side ship radar. This only does anything when the mod
 * is installed on the server (or the integrated server in single-player); on servers
 * without it, the client simply never receives these packets and uses its local scan.
 */
@EventBusSubscriber(modid = AeroRadar.MODID)
public final class ServerEvents {

    private static int counter;

    private ServerEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++counter % ServerShipRegistry.SCAN_INTERVAL != 0) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            ServerShipRegistry.scanAndSync(level);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerShipRegistry.greet(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerShipRegistry.sendSnapshotTo(player);
        }
    }
}
