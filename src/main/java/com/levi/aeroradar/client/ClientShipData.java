package com.levi.aeroradar.client;

import com.levi.aeroradar.net.ServerHelloPayload;
import com.levi.aeroradar.net.ShipInfo;
import com.levi.aeroradar.net.ShipSyncPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side store of what the SERVER told us about ships. Empty (and
 * {@code serverEnhanced == false}) on servers that don't run AeroRadar, in which
 * case the client falls back to its own nearby-ship scan.
 */
public final class ClientShipData {

    private static volatile boolean serverEnhanced;
    private static volatile String dimension = "";
    private static final Map<UUID, ShipInfo> SHIPS = new HashMap<>();

    private ClientShipData() {}

    public static void acceptHello(ServerHelloPayload payload) {
        serverEnhanced = payload.protocol() > 0;
    }

    public static void acceptSync(ShipSyncPayload payload) {
        serverEnhanced = true;
        dimension = payload.dimension();
        synchronized (SHIPS) {
            SHIPS.clear();
            for (ShipInfo s : payload.ships()) SHIPS.put(s.id(), s);
        }
    }

    public static void reset() {
        serverEnhanced = false;
        dimension = "";
        synchronized (SHIPS) {
            SHIPS.clear();
        }
    }

    public static boolean serverEnhanced() {
        return serverEnhanced;
    }

    /** Server ships, but only if the snapshot is for the dimension the player is in. */
    public static Map<UUID, ShipInfo> shipsFor(String currentDim) {
        if (!serverEnhanced || !dimension.equals(currentDim)) return Map.of();
        synchronized (SHIPS) {
            return new HashMap<>(SHIPS);
        }
    }
}
