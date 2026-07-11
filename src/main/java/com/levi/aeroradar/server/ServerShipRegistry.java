package com.levi.aeroradar.server;

import com.levi.aeroradar.ShipClustering;
import com.levi.aeroradar.net.RenameShipPayload;
import com.levi.aeroradar.net.ServerHelloPayload;
import com.levi.aeroradar.net.ShipInfo;
import com.levi.aeroradar.net.ShipSyncPayload;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative ship radar. Every {@link #SCAN_INTERVAL} ticks it scans
 * every dimension's loaded ships, records their positions in {@link AeroRadarSavedData}
 * (so unloaded ships keep a last-known marker), and pushes the per-dimension
 * snapshot to the players in that dimension.
 */
public final class ServerShipRegistry {

    public static final int SCAN_INTERVAL = 10; // ticks (~0.5s)
    private static final int MAX_NAME = 48;

    private ServerShipRegistry() {}

    public static void scanAndSync(ServerLevel level) {
        AeroRadarSavedData data = level.getDataStorage().computeIfAbsent(AeroRadarSavedData.factory(), AeroRadarSavedData.ID);

        Set<UUID> loaded = new HashSet<>();
        try {
            List<? extends SubLevel> subs = SubLevelContainer.getContainer(level).getAllSubLevels();
            ShipClustering.Result result = ShipClustering.cluster(subs, 1.0);
            for (ShipClustering.Cluster c : result.clusters()) {
                loaded.add(c.repId());
                data.updatePosition(c.repId(), c.name(), c.center().x, c.center().y, c.center().z);
            }
        } catch (Throwable t) {
            // Sable not ready for this level, or no ships - nothing to scan.
        }

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        String dim = level.dimension().location().toString();
        ShipSyncPayload payload = snapshot(dim, data, loaded);
        for (ServerPlayer p : players) {
            PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static ShipSyncPayload snapshot(String dim, AeroRadarSavedData data, Set<UUID> loaded) {
        List<ShipInfo> ships = new ArrayList<>();
        for (Map.Entry<UUID, AeroRadarSavedData.Rec> e : data.ships().entrySet()) {
            AeroRadarSavedData.Rec r = e.getValue();
            ships.add(new ShipInfo(e.getKey(), r.name, r.x, r.y, r.z, loaded.contains(e.getKey())));
        }
        return new ShipSyncPayload(dim, ships);
    }

    /** On join / dimension change: greet the client and send an immediate snapshot. */
    public static void greet(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ServerHelloPayload(ServerHelloPayload.PROTOCOL));
        sendSnapshotTo(player);
    }

    public static void sendSnapshotTo(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        AeroRadarSavedData data = level.getDataStorage().computeIfAbsent(AeroRadarSavedData.factory(), AeroRadarSavedData.ID);
        Set<UUID> loaded = currentlyLoaded(level);
        PacketDistributor.sendToPlayer(player, snapshot(level.dimension().location().toString(), data, loaded));
    }

    private static Set<UUID> currentlyLoaded(ServerLevel level) {
        Set<UUID> loaded = new HashSet<>();
        try {
            ShipClustering.Result result = ShipClustering.cluster(
                    SubLevelContainer.getContainer(level).getAllSubLevels(), 1.0);
            for (ShipClustering.Cluster c : result.clusters()) loaded.add(c.repId());
        } catch (Throwable ignored) {}
        return loaded;
    }

    /** Handle a client rename: store it and re-broadcast so everyone sees the new name. */
    public static void acceptRename(IPayloadContext ctx, RenameShipPayload payload) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        String name = payload.name() == null ? "" : payload.name().trim();
        if (name.length() > MAX_NAME) name = name.substring(0, MAX_NAME);
        ServerLevel level = player.serverLevel();
        AeroRadarSavedData data = level.getDataStorage().computeIfAbsent(AeroRadarSavedData.factory(), AeroRadarSavedData.ID);
        data.rename(payload.id(), name);
        // Push to everyone in the dimension right away.
        Set<UUID> loaded = currentlyLoaded(level);
        ShipSyncPayload snap = snapshot(level.dimension().location().toString(), data, loaded);
        for (ServerPlayer p : level.players()) PacketDistributor.sendToPlayer(p, snap);
    }
}
