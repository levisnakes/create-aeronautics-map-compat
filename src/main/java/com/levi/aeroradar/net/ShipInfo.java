package com.levi.aeroradar.net;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/** One ship as seen by the server: stable id, name (may be empty), position, and whether it's currently loaded. */
public record ShipInfo(UUID id, String name, double x, double y, double z, boolean loaded) {

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name == null ? "" : name, 64);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeBoolean(loaded);
    }

    public static ShipInfo read(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String name = buf.readUtf(64);
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        boolean loaded = buf.readBoolean();
        return new ShipInfo(id, name.isEmpty() ? null : name, x, y, z, loaded);
    }
}
