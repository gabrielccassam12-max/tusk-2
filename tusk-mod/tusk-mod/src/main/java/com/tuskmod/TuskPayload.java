package com.tuskmod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client -> server: 0 = switch act, 1 = switch attack. */
public record TuskPayload(int action) implements CustomPayload {
    public static final int SWITCH_ACT = 0;
    public static final int SWITCH_ATTACK = 1;

    public static final CustomPayload.Id<TuskPayload> ID = new CustomPayload.Id<>(TuskMod.id("action"));
    public static final PacketCodec<RegistryByteBuf, TuskPayload> CODEC =
            PacketCodecs.VAR_INT.xmap(TuskPayload::new, TuskPayload::action).cast();

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
