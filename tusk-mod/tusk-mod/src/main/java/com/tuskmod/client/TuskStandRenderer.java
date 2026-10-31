package com.tuskmod.client;

import com.tuskmod.TuskMod;
import com.tuskmod.entity.TuskStandEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class TuskStandRenderer extends EntityRenderer<TuskStandEntity> {
    private static final Identifier TEXTURE = TuskMod.id("textures/entity/tusk_stand.png");
    private static final Identifier GLOW = TuskMod.id("textures/entity/tusk_stand_glow.png");

    private final TuskStandModel model;

    public TuskStandRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new TuskStandModel(ctx.getPart(TuskStandModel.LAYER));
        this.shadowRadius = 0f;
    }

    @Override
    public void render(TuskStandEntity e, float yaw, float tickDelta, MatrixStack m,
                       VertexConsumerProvider vcp, int light) {
        m.push();
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, e.prevYaw, e.getYaw());
        float pitch = MathHelper.lerp(tickDelta, e.prevPitch, e.getPitch());
        float age = e.age + tickDelta;

        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - bodyYaw));
        float s = 0.92f;
        m.scale(-s, -s, s);
        float bob = MathHelper.sin(age * 0.1f) * 0.05f;
        m.translate(0f, -1.501f - bob - 0.15f, 0f); // hovers a little above the ground

        model.setAngles(e, 0f, 0f, age, 0f, pitch);
        model.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV, -1);
        // emissive layer: eyes, chest spiral, nails and halo shine in the dark
        model.render(m, vcp.getBuffer(RenderLayer.getEyes(GLOW)), LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV, -1);
        m.pop();
        super.render(e, yaw, tickDelta, m, vcp, light);
    }

    @Override
    public Identifier getTexture(TuskStandEntity entity) {
        return TEXTURE;
    }
}
