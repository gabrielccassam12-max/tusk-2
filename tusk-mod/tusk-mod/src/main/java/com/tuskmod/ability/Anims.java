package com.tuskmod.ability;

/** Pose ids shared by the server (which triggers them) and the client model (which plays them). */
public final class Anims {
    private Anims() {}

    public static final int IDLE = 0;
    public static final int SHOOT = 1;
    public static final int FAN = 2;
    public static final int RAPID = 3;
    public static final int RAISE = 4;
    public static final int SPIRAL = 5;
    public static final int GUARD = 6;
    public static final int SLAM = 7;
    public static final int DASH = 8;
    public static final int VOID = 9;

    /** Duration of each pose in ticks. */
    private static final int[] DURATION = {1, 10, 14, 20, 28, 24, 50, 22, 12, 70};

    public static int duration(int anim) {
        return anim >= 0 && anim < DURATION.length ? DURATION[anim] : 1;
    }
}
