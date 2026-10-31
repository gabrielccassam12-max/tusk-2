package com.tuskmod.client;

import com.tuskmod.ModRegistry;
import com.tuskmod.TuskMod;
import com.tuskmod.TuskPayload;
import com.tuskmod.ability.TuskAttack;
import com.tuskmod.item.TuskItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class TuskModClient implements ClientModInitializer {
    public static KeyBinding switchAct;
    public static KeyBinding switchAttack;

    @Override
    public void onInitializeClient() {
        switchAct = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tuskmod.switch_act",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.tuskmod"));
        switchAttack = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tuskmod.switch_attack",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.tuskmod"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (switchAct.wasPressed()) ClientPlayNetworking.send(new TuskPayload(TuskPayload.SWITCH_ACT));
            while (switchAttack.wasPressed()) ClientPlayNetworking.send(new TuskPayload(TuskPayload.SWITCH_ATTACK));
        });

        EntityModelLayerRegistry.registerModelLayer(TuskStandModel.LAYER, TuskStandModel::getTexturedModelData);
        EntityRendererRegistry.register(ModRegistry.STAND, TuskStandRenderer::new);
        EntityRendererRegistry.register(ModRegistry.NAIL, TuskNailRenderer::new);

        // item icon changes with the Act (see models/item/tusk.json overrides)
        ModelPredicateProviderRegistry.register(ModRegistry.TUSK, TuskMod.id("act"),
                (stack, world, entity, seed) -> TuskItem.getAct(stack) / 10f);

        HudRenderCallback.EVENT.register(TuskModClient::renderHud);
    }

    private static void renderHud(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        ItemStack stack = TuskItem.heldTusk(mc.player);
        if (stack.isEmpty()) return;

        int act = TuskItem.getAct(stack);
        TuskAttack selected = TuskItem.getAttack(stack);
        List<TuskAttack> list = TuskAttack.forAct(act);

        int x = 10;
        int lineH = 11;
        int h = 18 + list.size() * lineH;
        int y = ctx.getScaledWindowHeight() - h - 40;
        int w = 128;

        ctx.fill(x - 4, y - 4, x + w, y + h - 2, 0x90000000);
        ctx.fill(x - 4, y - 4, x - 2, y + h - 2, 0xFFE8B02A);
        ctx.drawTextWithShadow(mc.textRenderer, Text.translatable("hud.tuskmod.act", act), x + 2, y, 0xFFE8B02A);
        for (int i = 0; i < list.size(); i++) {
            TuskAttack a = list.get(i);
            boolean sel = a == selected;
            Text name = Text.literal((sel ? "> " : "  ")).append(a.displayName());
            ctx.drawTextWithShadow(mc.textRenderer, name, x + 2, y + 14 + i * lineH, sel ? 0xFFFFE57A : 0xFF9A9A9A);
        }
    }
}
