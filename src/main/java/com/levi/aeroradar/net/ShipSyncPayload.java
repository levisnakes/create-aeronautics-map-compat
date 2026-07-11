package com.levi.aeroradar.net;

import com.levi.aeroradar.AeroRadar;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Server -> client: the full set of ships in one dimension (loaded + last-known). */
public record ShipSyncPayload(String dimension, List<ShipInfo> ships) implements CustomPacketPayload {

    public static final Type<ShipSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AeroRadar.MODID, "ship_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipSyncPayload> CODEC =
            StreamCodec.ofMember(ShipSyncPayload::write, ShipSyncPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(dimension);
        buf.writeVarInt(ships.size());
        for (ShipInfo s : ships) s.write(buf);
    }

    private static ShipSyncPayload read(RegistryFriendlyByteBuf buf) {
        String dim = buf.readUtf();
        int count = buf.readVarInt();
        List<ShipInfo> ships = new ArrayList<>(count);
        for (int i = 0; i < count; i++) ships.add(ShipInfo.read(buf));
        return new ShipSyncPayload(dim, ships);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
