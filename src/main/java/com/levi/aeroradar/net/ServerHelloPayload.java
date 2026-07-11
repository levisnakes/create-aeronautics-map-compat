package com.levi.aeroradar.net;

import com.levi.aeroradar.AeroRadar;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client, sent once on join: "this server runs AeroRadar" (enables shared/global ship radar). */
public record ServerHelloPayload(int protocol) implements CustomPacketPayload {

    public static final int PROTOCOL = 1;

    public static final Type<ServerHelloPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AeroRadar.MODID, "hello"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerHelloPayload> CODEC =
            StreamCodec.ofMember(ServerHelloPayload::write, ServerHelloPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(protocol);
    }

    private static ServerHelloPayload read(RegistryFriendlyByteBuf buf) {
        return new ServerHelloPayload(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
