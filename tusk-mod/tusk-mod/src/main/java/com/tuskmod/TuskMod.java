package com.tuskmod;

import com.tuskmod.ability.Scheduler;
import com.tuskmod.item.TuskItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class TuskMod implements ModInitializer {
    public static final String MOD_ID = "tuskmod";

    // ---- simple "config" (edit and rebuild) ----
    /** Nails of Act 3/4 and the drill shot are allowed to destroy blocks. */
    public static final boolean BREAK_BLOCKS = true;
    /** If true every Act is available from the start (otherwise gated by XP level). */
    public static final boolean UNLOCK_ALL = false;
    /** XP levels needed for Act 2, 3 and 4 (creative mode ignores this). */
    public static final int[] ACT_LEVELS = {0, 5, 15, 30};

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModRegistry.init();
        Scheduler.init();

        PayloadTypeRegistry.playC2S().register(TuskPayload.ID, TuskPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TuskPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            ItemStack stack = TuskItem.heldTusk(player);
            if (stack.isEmpty()) return;
            if (payload.action() == TuskPayload.SWITCH_ACT) {
                TuskItem.cycleAct(player, stack);
            } else if (payload.action() == TuskPayload.SWITCH_ATTACK) {
                TuskItem.cycleAttack(player, stack);
            }
        });
    }
}
