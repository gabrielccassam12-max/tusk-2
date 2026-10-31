package com.tuskmod.client;

import com.tuskmod.ModRegistry;
import com.tuskmod.entity.TuskNailEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Draws the nail as a small 3D golden model, pointing along its flight path and spinning on its axis. */
public class TuskNailRenderer extends EntityRenderer<TuskNailEntity> {
    private final ItemRenderer itemRenderer;
    private final ItemStack stack = new ItemStack(ModRegistry.TUSK_NAIL);

    public TuskNailRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(TuskNailEntity e, float yaw, float tickDelta, MatrixStack m,
                       VertexConsumerProvider vcp, int light) {
        m.push();
        float ry = MathHelper.lerpAngleDegrees(tickDelta, e.prevYaw, e.getYaw());
        float rp = MathHelper.lerp(tickDelta, e.prevPitch, e.getPitch());
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f - rp));

        TuskNailEntity.Mode mode = e.getMode();
        float spinSpeed = switch (mode) {
            case SPIN, WORMHOLE, INFINITE, DRILL, HOMING -> 55f;
            default -> 8f;
        };
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((e.age + tickDelta) * spinSpeed));

        float scale = mode == TuskNailEntity.Mode.INFINITE ? 2.4f : mode == TuskNailEntity.Mode.WORMHOLE ? 1.9f : 1.4f;
        m.scale(scale, scale, scale);

        int l = (mode == TuskNailEntity.Mode.INFINITE || mode == TuskNailEntity.Mode.WORMHOLE
                || mode == TuskNailEntity.Mode.HOMING) ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;
        itemRenderer.renderItem(stack, ModelTransformationMode.FIXED, l, OverlayTexture.DEFAULT_UV, m, vcp,
                e.getWorld(), e.getId());
        m.pop();
        super.render(e, yaw, tickDelta, m, vcp, light);
    }

    @Override
    public Identifier getTexture(TuskNailEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
