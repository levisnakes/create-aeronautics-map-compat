package com.levi.aeroradar.net;

import com.levi.aeroradar.AeroRadar;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Client -> server: name a ship for everyone (only sent if the server advertises the channel). */
public record RenameShipPayload(UUID id, String name) implements CustomPacketPayload {

    public static final Type<RenameShipPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AeroRadar.MODID, "rename"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameShipPayload> CODEC =
            StreamCodec.ofMember(RenameShipPayload::write, RenameShipPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name == null ? "" : name, 64);
    }

    private static RenameShipPayload read(RegistryFriendlyByteBuf buf) {
        return new RenameShipPayload(buf.readUUID(), buf.readUtf(64));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
