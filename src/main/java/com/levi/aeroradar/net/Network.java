package com.levi.aeroradar.net;

import com.levi.aeroradar.AeroRadar;
import com.levi.aeroradar.client.ClientShipData;
import com.levi.aeroradar.server.ServerShipRegistry;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers all AeroRadar payloads as OPTIONAL, so:
 *  - a modded client can join a server that lacks the mod (no disconnect),
 *  - a modded server can serve vanilla/other clients (no disconnect).
 *
 * Handlers live here (common) and only touch client/server classes lazily inside
 * the handler body, so the receiving-side class is never loaded on the wrong dist.
 */
public final class Network {

    private Network() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1").optional();
        reg.playToClient(ServerHelloPayload.TYPE, ServerHelloPayload.CODEC, Network::onHello);
        reg.playToClient(ShipSyncPayload.TYPE, ShipSyncPayload.CODEC, Network::onSync);
        reg.playToServer(RenameShipPayload.TYPE, RenameShipPayload.CODEC, Network::onRename);
    }

    // ---- received on the client ----

    private static void onHello(ServerHelloPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientShipData.acceptHello(payload));
    }

    private static void onSync(ShipSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientShipData.acceptSync(payload));
    }

    // ---- received on the server ----

    private static void onRename(RenameShipPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ServerShipRegistry.acceptRename(ctx, payload));
    }

    public static String modId() {
        return AeroRadar.MODID;
    }
}
