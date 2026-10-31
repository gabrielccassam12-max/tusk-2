package com.tuskmod.ability;

import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** All 16 attacks. Four per Act. */
public enum TuskAttack {
    // ---- ACT 1: raw nails
    NAIL_SHOT(1, "nail_shot", 8, 0.1f, Anims.SHOOT),
    NAIL_FAN(1, "nail_fan", 26, 0.4f, Anims.FAN),
    RICOCHET(1, "ricochet", 20, 0.3f, Anims.SHOOT),
    RAPID_FIRE(1, "rapid_fire", 40, 0.8f, Anims.RAPID),

    // ---- ACT 2: spinning nails
    SPIN_SHOT(2, "spin_shot", 18, 0.6f, Anims.SPIRAL),
    DRILL_SHOT(2, "drill_shot", 30, 0.8f, Anims.SHOOT),
    NAIL_RAIN(2, "nail_rain", 70, 1.5f, Anims.RAISE),
    SPIRAL_VOLLEY(2, "spiral_volley", 55, 1.2f, Anims.SPIRAL),

    // ---- ACT 3: holes and pursuit
    WORMHOLE_NAIL(3, "wormhole_nail", 40, 1.5f, Anims.SHOOT),
    HOMING_SPIRAL(3, "homing_spiral", 60, 1.8f, Anims.RAISE),
    TUSK_GUARD(3, "tusk_guard", 120, 2.0f, Anims.GUARD),
    ROTATION_SHOCKWAVE(3, "rotation_shockwave", 90, 2.5f, Anims.SLAM),

    // ---- ACT 4: infinite rotation
    INFINITE_ROTATION(4, "infinite_rotation", 100, 4.0f, Anims.SPIRAL),
    WORMHOLE_DASH(4, "wormhole_dash", 80, 3.0f, Anims.DASH),
    GOLDEN_ENCIRCLEMENT(4, "golden_encirclement", 160, 5.0f, Anims.RAISE),
    INFINITE_VOID(4, "infinite_void", 260, 6.0f, Anims.VOID);

    public final int act;
    public final String key;
    public final int cooldown;
    public final float exhaustion;
    public final int anim;

    TuskAttack(int act, String key, int cooldown, float exhaustion, int anim) {
        this.act = act;
        this.key = key;
        this.cooldown = cooldown;
        this.exhaustion = exhaustion;
        this.anim = anim;
    }

    public Text displayName() {
        return Text.translatable("attack.tuskmod." + key);
    }

    public static List<TuskAttack> forAct(int act) {
        List<TuskAttack> list = new ArrayList<>();
        for (TuskAttack a : values()) {
            if (a.act == act) list.add(a);
        }
        return list;
    }
}
