package com.tuskmod.client;

import com.tuskmod.TuskMod;
import com.tuskmod.ability.Anims;
import com.tuskmod.entity.TuskStandEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/**
 * Geometry comes from the generated {@link TuskStandParts}. This class only animates it:
 *  - idle: breathing arms, floating halo, swaying thorns
 *  - one pose per {@link Anims} id, driven by how many ticks ago the attack started
 *  - extra armour appears with each Act (spikes, thorns + back plate, halo)
 */
public class TuskStandModel extends EntityModel<TuskStandEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(TuskMod.id("tusk_stand"), "main");

    private final ModelPart root, body, head, rArm, lArm, rLeg, lLeg, halo;
    private final ModelPart[] act2, act3, act4, thorns;

    public TuskStandModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.rArm = root.getChild("r_arm");
        this.lArm = root.getChild("l_arm");
        this.rLeg = root.getChild("r_leg");
        this.lLeg = root.getChild("l_leg");
        this.halo = root.getChild("halo");
        this.act2 = resolve(root, TuskStandParts.ACT2);
        this.act3 = resolve(root, TuskStandParts.ACT3);
        this.act4 = resolve(root, TuskStandParts.ACT4);
        this.thorns = resolve(root, TuskStandParts.THORNS);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        TuskStandParts.build(data.getRoot());
        return TexturedModelData.of(data, 128, 128);
    }

    private static ModelPart[] resolve(ModelPart root, String[] paths) {
        ModelPart[] out = new ModelPart[paths.length];
        for (int i = 0; i < paths.length; i++) {
            ModelPart p = root;
            for (String s : paths[i].split("/")) p = p.getChild(s);
            out[i] = p;
        }
        return out;
    }

    private static void show(ModelPart[] parts, boolean visible) {
        for (ModelPart p : parts) p.visible = visible;
    }

    private static float env(float t, float in, float out) {
        return MathHelper.clamp(Math.min(t / in, (1f - t) / out), 0f, 1f);
    }

    @Override
    public void setAngles(TuskStandEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
        for (ModelPart p : new ModelPart[]{root, body, head, rArm, lArm, rLeg, lLeg}) p.resetTransform();
        halo.resetTransform();

        int act = e.getAct();
        show(act2, act >= 2);
        show(act3, act >= 3);
        show(act4, act >= 4);

        // ---------------- idle
        float rad = (float) (Math.PI / 180.0);
        float breathe = MathHelper.sin(age * 0.09f);
        head.pitch = headPitch * rad * 0.6f;
        rArm.pitch = 0.08f * breathe;
        lArm.pitch = -0.08f * breathe;
        rArm.roll = 0.14f + 0.03f * breathe;
        lArm.roll = -0.14f - 0.03f * breathe;
        rLeg.pitch = 0.05f * breathe;
        lLeg.pitch = -0.05f * breathe;
        halo.roll = age * 0.08f;
        for (int i = 0; i < thorns.length; i++) {
            ModelTransform d = thorns[i].getDefaultTransform();
            thorns[i].pitch = d.pitch + MathHelper.sin(age * 0.07f + i) * 0.07f;
        }

        // ---------------- attack pose
        int anim = e.getAnim();
        if (anim == Anims.IDLE) return;
        float t = (age - e.animStartAge) / (float) Anims.duration(anim);
        if (t < 0f || t >= 1f) return;
        float bell = MathHelper.sin(t * (float) Math.PI);

        switch (anim) {
            case Anims.SHOOT -> {
                float k = env(t, 0.12f, 0.45f);
                rArm.pitch = -1.55f * k;
                rArm.yaw = 0.12f * k;
                rArm.roll = 0.14f * (1 - k);
                body.yaw = -0.18f * k;
                head.yaw = 0.1f * k;
                body.pitch = -0.05f * MathHelper.sin(t * 18f) * (1 - t);
            }
            case Anims.FAN -> {
                float k = env(t, 0.15f, 0.4f);
                rArm.pitch = lArm.pitch = -1.45f * k;
                rArm.yaw = 0.95f * k;
                lArm.yaw = -0.95f * k;
                rArm.roll = lArm.roll = 0f;
            }
            case Anims.RAPID -> {
                float k = env(t, 0.1f, 0.15f);
                float shake = MathHelper.sin(age * 3.2f) * 0.1f;
                rArm.pitch = -1.5f * k + shake;
                lArm.pitch = -1.5f * k - shake;
                rArm.yaw = 0.15f * k;
                lArm.yaw = -0.15f * k;
                rArm.roll = lArm.roll = 0f;
                body.pitch = -0.08f * k;
            }
            case Anims.RAISE -> {
                float k = env(t, 0.2f, 0.3f);
                rArm.pitch = lArm.pitch = -3.0f * k;
                rArm.roll = 0.35f * k;
                lArm.roll = -0.35f * k;
                head.pitch -= 0.5f * k;
                body.pitch = 0.1f * k;
                root.pivotY = -1.5f * k; // little hop
            }
            case Anims.SPIRAL -> {
                float k = env(t, 0.15f, 0.15f);
                rArm.pitch = -1.5f * k;
                lArm.pitch = -0.6f * k;
                rArm.roll = 0f;
                root.yaw = t * (float) Math.PI * 2f; // full pirouette
                rArm.yaw = 0.3f * MathHelper.sin(t * 25f) * k;
            }
            case Anims.GUARD -> {
                float k = env(t, 0.08f, 0.08f);
                rArm.pitch = lArm.pitch = -1.1f * k;
                rArm.yaw = -0.95f * k;
                lArm.yaw = 0.95f * k;
                rArm.roll = lArm.roll = 0f;
                body.pitch = 0.12f * k;
                head.pitch = 0.2f * k;
                root.roll = MathHelper.sin(age * 1.6f) * 0.015f * k; // shudder as it absorbs hits
            }
            case Anims.SLAM -> {
                float up = MathHelper.clamp(t / 0.4f, 0f, 1f);
                float down = MathHelper.clamp((t - 0.4f) / 0.12f, 0f, 1f);
                float rec = MathHelper.clamp((t - 0.55f) / 0.45f, 0f, 1f);
                float arm = -2.9f * up * (1 - down) + 0.5f * down * (1 - rec);
                rArm.pitch = lArm.pitch = arm;
                rArm.roll = 0.3f * (1 - rec);
                lArm.roll = -0.3f * (1 - rec);
                body.pitch = 0.5f * down * (1 - rec);
                head.pitch += 0.3f * down * (1 - rec);
                rLeg.pitch = -0.5f * down * (1 - rec);
                lLeg.pitch = 0.35f * down * (1 - rec);
                root.pivotY = 3f * down * (1 - rec);
            }
            case Anims.DASH -> {
                float k = bell;
                root.pitch = 0.75f * k;
                rArm.pitch = lArm.pitch = 1.1f * k;
                rArm.roll = 0.1f;
                lArm.roll = -0.1f;
                rLeg.pitch = 0.7f * k;
                lLeg.pitch = 0.2f * k;
            }
            case Anims.VOID -> {
                float k = env(t, 0.12f, 0.12f);
                rArm.roll = 1.45f * k;
                lArm.roll = -1.45f * k;
                rArm.pitch = lArm.pitch = -0.2f * k;
                head.pitch -= 0.55f * k;
                body.pitch = -0.1f * k;
                root.pivotY = -4f * k;
                root.yaw = MathHelper.sin(age * 0.2f) * 0.1f * k;
                halo.roll = age * (0.08f + 0.5f * k);
                rLeg.roll = 0.15f * k;
                lLeg.roll = -0.15f * k;
            }
            default -> {}
        }
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        root.render(matrices, vertices, light, overlay, color);
    }
}
