#!/usr/bin/env python3
"""
Tusk mod asset generator.

Single source of truth for the Tusk stand model. Running this script writes:
  - src/main/resources/assets/tuskmod/textures/entity/tusk_stand.png       (base texture)
  - src/main/resources/assets/tuskmod/textures/entity/tusk_stand_glow.png  (emissive layer)
  - src/main/java/com/tuskmod/client/TuskStandParts.java                   (model geometry, same UVs)
  - item icons for Act 1..4 and the nail texture

Edit the PARTS list below to change the model, then re-run:  python3 tools/generate_assets.py
Requires Pillow (pip install pillow).
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
RES = os.path.join(ROOT, "src/main/resources/assets/tuskmod")
JAVA = os.path.join(ROOT, "src/main/java/com/tuskmod/client/TuskStandParts.java")
TEX = 128
random.seed(1234)

# ------------------------------------------------------------------ palette
BONE = (232, 224, 205); BONE_L = (246, 240, 224); BONE_D = (190, 178, 150)
GOLD = (236, 176, 38);  GOLD_L = (255, 226, 120); GOLD_D = (158, 100, 18)
SUIT = (62, 46, 42);    SUIT_L = (88, 68, 60);    SUIT_D = (34, 26, 24)
GLOW = (255, 214, 90);  GLOW_L = (255, 245, 180); GLOW_D = (255, 180, 40)
PAL = {
    "bone": (BONE, BONE_L, BONE_D),
    "gold": (GOLD, GOLD_L, GOLD_D),
    "suit": (SUIT, SUIT_L, SUIT_D),
    "glow": (GLOW, GLOW_L, GLOW_D),
}

# ------------------------------------------------------------------ model spec
# Coordinates follow the vanilla biped convention (Y points DOWN, front is -Z, units = 1/16 block).
# Box sizes must be integers (UV unwrapping); positions may be fractional.
PARTS = []


def P(name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0), box=None, mat="bone", tag=None, decor=None):
    PARTS.append(dict(name=name, parent=parent, pivot=pivot, rot=rot, box=box, mat=mat, tag=tag, decor=decor))


# torso & head
P("body", None, box=(-4, 0, -2, 8, 12, 4), mat="suit")
P("chest", "body", box=(-4.5, 0, -2.5, 9, 7, 5), mat="bone", decor="chest")
P("belt", "body", box=(-4.5, 10, -2.5, 9, 2, 5), mat="gold")
P("head", None, box=(-4, -8, -4, 8, 8, 8), mat="suit", decor="face")
P("helmet", "head", box=(-4.5, -8.5, -4.5, 9, 3, 9), mat="bone", decor="helmet")
P("crest", "head", box=(-1, -11.5, -5, 2, 3, 10), mat="gold")

# the three-segment curved tusks
P("tusk_r1", "head", pivot=(-4, -3, 0), box=(-4, -1, -1.5, 4, 2, 3), mat="bone")
P("tusk_r2", "tusk_r1", pivot=(-4, 0, 0), box=(-1.5, -1, -7, 3, 2, 7), mat="bone")
P("tusk_r3", "tusk_r2", pivot=(0, 0, -7), rot=(-0.8, 0, 0), box=(-1, -1, -5, 2, 2, 5), mat="gold")
P("tusk_l1", "head", pivot=(4, -3, 0), box=(0, -1, -1.5, 4, 2, 3), mat="bone")
P("tusk_l2", "tusk_l1", pivot=(4, 0, 0), box=(-1.5, -1, -7, 3, 2, 7), mat="bone")
P("tusk_l3", "tusk_l2", pivot=(0, 0, -7), rot=(-0.8, 0, 0), box=(-1, -1, -5, 2, 2, 5), mat="gold")

# arms with pointing fingers + glowing nails
for s, px, bx in (("r", -5, -3), ("l", 5, -1)):
    P(f"{s}_arm", None, pivot=(px, 2, 0), box=(bx, -2, -2, 4, 12, 4), mat="suit")
    P(f"{s}_shoulder", f"{s}_arm", box=(bx - 0.5, -2.5, -2.5, 5, 4, 5), mat="bone")
    P(f"{s}_gauntlet", f"{s}_arm", box=(bx - 0.5, 5, -2.5, 5, 5, 5), mat="gold", decor="gauntlet")
    P(f"{s}_hand", f"{s}_arm", box=(bx, 10, -2, 4, 2, 4), mat="bone")
    for i, fx in enumerate((bx + 0.1, bx + 1.5, bx + 2.9)):
        P(f"{s}_f{i}", f"{s}_arm", box=(fx, 12, -0.5, 1, 3, 1), mat="bone")
        P(f"{s}_n{i}", f"{s}_arm", box=(fx, 15, -0.5, 1, 2, 1), mat="glow")

# legs
for s, px in (("r", -1.9), ("l", 1.9)):
    P(f"{s}_leg", None, pivot=(px, 12, 0), box=(-2, 0, -2, 4, 12, 4), mat="suit")
    P(f"{s}_knee", f"{s}_leg", box=(-2.5, 2, -2.5, 5, 3, 5), mat="gold")
    P(f"{s}_boot", f"{s}_leg", box=(-2.5, 7, -2.5, 5, 5, 5), mat="bone")

# ACT 2: shoulder spikes
P("spike_r", "body", pivot=(-5.5, 0, 0), rot=(0, 0, -0.6), box=(-1, -6, -1, 2, 6, 2), mat="gold", tag="act2")
P("spike_l", "body", pivot=(5.5, 0, 0), rot=(0, 0, 0.6), box=(-1, -6, -1, 2, 6, 2), mat="gold", tag="act2")
P("spike_r2", "body", pivot=(-5.5, 1, 0), rot=(0, 0, -1.15), box=(-1, -4, -1, 2, 4, 2), mat="gold", tag="act2")
P("spike_l2", "body", pivot=(5.5, 1, 0), rot=(0, 0, 1.15), box=(-1, -4, -1, 2, 4, 2), mat="gold", tag="act2")

# ACT 3: back plate + fan of thorns
P("backplate", "body", pivot=(0, 0, 2), box=(-3, 1, 0, 6, 9, 1), mat="gold", tag="act3")
for i, (y, pitch) in enumerate(((2, 0.95), (5, 0.55), (8, 0.15))):
    P(f"thorn_r{i}", "body", pivot=(-2.5, y, 2.5), rot=(pitch, -0.45, 0), box=(-0.5, -0.5, 0, 1, 1, 9), mat="gold", tag="act3")
    P(f"thorn_l{i}", "body", pivot=(2.5, y, 2.5), rot=(pitch, 0.45, 0), box=(-0.5, -0.5, 0, 1, 1, 9), mat="gold", tag="act3")

# ACT 4: spinning golden halo (octagon)
P("halo", None, pivot=(0, 4, 8), mat="glow", tag="act4")
for k in range(8):
    a = k * math.pi / 4
    P(f"halo_{k}", "halo", pivot=(9 * math.cos(a), 9 * math.sin(a), 0), rot=(0, 0, a + math.pi / 2),
      box=(-4, -1, -0.5, 8, 2, 1), mat="glow")

# ------------------------------------------------------------------ UV packing
def footprint(box):
    _, _, _, w, h, d = box
    return 2 * (d + w), d + h


def pack():
    items = [p for p in PARTS if p["box"]]
    items.sort(key=lambda p: -footprint(p["box"])[1])
    x = y = shelf = 0
    for p in items:
        fw, fh = footprint(p["box"])
        if x + fw > TEX:
            x, y, shelf = 0, y + shelf, 0
        assert y + fh <= TEX, "texture atlas overflow"
        p["uv"] = (x, y)
        x += fw
        shelf = max(shelf, fh)


def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }


# ------------------------------------------------------------------ painting
def jit(c, amt=7):
    return tuple(max(0, min(255, v + random.randint(-amt, amt))) for v in c)


def paint_face(img, rect, mat, kind, alpha_img=None):
    x, y, w, h = rect
    base, light, dark = PAL[mat]
    for j in range(h):
        for i in range(w):
            c = base
            if kind == "top":
                c = light
            elif kind == "bottom":
                c = dark
            else:
                if j == 0 or i == 0:
                    c = light
                elif j == h - 1 or i == w - 1:
                    c = dark
            img.putpixel((x + i, y + j), jit(c, 5 if mat != "glow" else 3) + (255,))


def px(img, x, y, c):
    img.putpixel((x, y), c + (255,))


def decorate(kind, f, main, glow):
    fx, fy, fw, fh = f["front"]
    if kind == "chest":
        for i in range(fw):
            px(main, fx + i, fy, GOLD_L)
            px(main, fx + i, fy + fh - 1, GOLD_D)
        cx, cy = fx + fw // 2, fy + fh // 2
        pts = set()
        for i in range(0, 34):
            ang = i * 0.62
            r = 0.1 * i
            pts.add((cx + round(math.cos(ang) * r * 1.3), cy + round(math.sin(ang) * r * 0.95)))
        for (x, y) in pts:
            if fx < x < fx + fw - 1 and fy < y < fy + fh - 1:
                px(main, x, y, GOLD_L)
                px(glow, x, y, GLOW)
    elif kind == "face":
        for j in range(fh):
            for i in range(fw):
                if j <= 2:
                    px(main, fx + i, fy + j, SUIT_D)
                elif j in (3, 4):
                    px(main, fx + i, fy + j, SUIT_D)
                else:
                    c = BONE if i not in (2, 5) else BONE_D
                    if j == fh - 1:
                        c = BONE_D
                    px(main, fx + i, fy + j, c)
        for i in (1, 2, 5, 6):  # glowing eye slits
            px(main, fx + i, fy + 3, GLOW_L)
            px(glow, fx + i, fy + 3, GLOW_L)
        for i in (0, 3, 4, 7):
            px(main, fx + i, fy + 4, SUIT)
        # gold cheek marks on the side faces
        for key in ("left", "right"):
            sx, sy, sw, sh = f[key]
            for i in range(sw):
                px(main, sx + i, sy + 3, GOLD)
    elif kind == "helmet":
        for i in range(fw):
            px(main, fx + i, fy + fh - 1, GOLD)
        px(main, fx + fw // 2, fy + 1, GOLD_L)
    elif kind == "gauntlet":
        for key in ("front", "back", "left", "right"):
            sx, sy, sw, sh = f[key]
            for i in range(sw):
                px(main, sx + i, sy + 1, GOLD_L)
                px(main, sx + i, sy + sh - 2, GOLD_D)


def build_textures():
    main = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    for p in PARTS:
        if not p["box"]:
            continue
        u, v = p["uv"]
        _, _, _, w, h, d = p["box"]
        f = faces(u, v, w, h, d)
        for name, rect in f.items():
            kind = name if name in ("top", "bottom") else "side"
            paint_face(main, rect, p["mat"], kind)
            if p["mat"] == "glow":
                paint_face(glow, rect, "glow", kind)
        if p["decor"]:
            decorate(p["decor"], f, main, glow)
    out = os.path.join(RES, "textures/entity")
    os.makedirs(out, exist_ok=True)
    main.save(os.path.join(out, "tusk_stand.png"))
    glow.save(os.path.join(out, "tusk_stand_glow.png"))


# ------------------------------------------------------------------ java emission
def fl(x):
    return f"{float(x):.4f}f"


def chain(name):
    by = {p["name"]: p for p in PARTS}
    out = []
    cur = by[name]
    while cur:
        out.append(cur["name"])
        cur = by[cur["parent"]] if cur["parent"] else None
    return "/".join(reversed(out))


def emit_java():
    lines = []
    lines.append("// GENERATED by tools/generate_assets.py - do not edit by hand (edit the PARTS list and re-run).")
    lines.append("package com.tuskmod.client;")
    lines.append("")
    lines.append("import net.minecraft.client.model.ModelPartBuilder;")
    lines.append("import net.minecraft.client.model.ModelPartData;")
    lines.append("import net.minecraft.client.model.ModelTransform;")
    lines.append("")
    lines.append("final class TuskStandParts {")
    lines.append("    private TuskStandParts() {}")
    lines.append("")
    for tag in ("act2", "act3", "act4"):
        paths = [chain(p["name"]) for p in PARTS if p["tag"] == tag]
        if tag == "act4":
            paths = ["halo"]
        arr = ", ".join(f'"{s}"' for s in paths)
        lines.append(f"    static final String[] {tag.upper()} = {{{arr}}};")
    thorns = ", ".join(f'"{chain(p["name"])}"' for p in PARTS if p["name"].startswith("thorn_"))
    lines.append(f"    static final String[] THORNS = {{{thorns}}};")
    lines.append("")
    lines.append("    static void build(ModelPartData root) {")
    for p in PARTS:
        parent = "root" if not p["parent"] else f"v_{p['parent']}"
        b = "ModelPartBuilder.create()"
        if p["box"]:
            x, y, z, w, h, d = p["box"]
            u, v = p["uv"]
            b += f".uv({u}, {v}).cuboid({fl(x)}, {fl(y)}, {fl(z)}, {fl(w)}, {fl(h)}, {fl(d)})"
        px_, py_, pz_ = p["pivot"]
        rx, ry, rz = p["rot"]
        tr = f"ModelTransform.of({fl(px_)}, {fl(py_)}, {fl(pz_)}, {fl(rx)}, {fl(ry)}, {fl(rz)})"
        lines.append(f'        ModelPartData v_{p["name"]} = {parent}.addChild("{p["name"]}", {b}, {tr});')
    lines.append("    }")
    lines.append("}")
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, "w") as fh:
        fh.write("\n".join(lines) + "\n")


# ------------------------------------------------------------------ item icons + nail
def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def draw_icon(act):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    dist = {}
    steps = 90
    for s in range(steps + 1):
        t = s / steps
        cx = 2.2 + 11.4 * t
        cy = 13.6 - 11.2 * (math.sin(t * math.pi * 0.5) ** 1.15)
        th = 3.4 * (1 - t) + 0.9
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d <= th / 2 + 0.25:
                    key = (x, y)
                    if key not in dist or d / (th / 2) < dist[key][0]:
                        dist[key] = (d / (th / 2), t)
    for (x, y), (rd, t) in dist.items():
        if t > 0.86:
            c = lerp(GOLD, GLOW_L, (t - 0.86) / 0.14)
        elif t < 0.12 and act >= 2:
            c = GOLD
        else:
            shade = min(1.0, rd)
            c = lerp(BONE_L, BONE_D, shade * 0.9)
            if (t * 13) % 1.0 < 0.22:
                c = lerp(c, GOLD_D, 0.55)
        if (x + y) % 2 == 0 and t < 0.86:
            c = tuple(max(0, v - 4) for v in c)
        img.putpixel((x, y), c + (255,))
    # outline
    base = img.copy()
    for y in range(16):
        for x in range(16):
            if base.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and base.getpixel((nx, ny))[3] > 0:
                        img.putpixel((x, y), (60, 38, 14, 255))
                        break
    # act specific flair
    if act >= 2:
        for (x, y) in ((11, 1), (13, 3)):
            img.putpixel((x, y), GLOW + (255,))
    if act >= 3:
        for (x, y) in ((8, 3), (14, 7), (6, 6), (10, 5)):
            img.putpixel((x, y), GLOW_L + (255,))
    if act >= 4:
        cx, cy, r = 5.0, 5.0, 4.6
        for a in range(0, 360, 12):
            x = int(round(cx + r * math.cos(math.radians(a))))
            y = int(round(cy + r * math.sin(math.radians(a))))
            if 0 <= x < 16 and 0 <= y < 16 and img.getpixel((x, y))[3] == 0:
                img.putpixel((x, y), GLOW + (255,))
    return img


def build_items():
    out = os.path.join(RES, "textures/item")
    os.makedirs(out, exist_ok=True)
    for act in range(1, 5):
        draw_icon(act).save(os.path.join(out, f"tusk_act{act}.png"))
    nail = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            t = y / 15
            c = lerp(GOLD_L, GOLD_D, t * 0.7)
            c = lerp(c, GOLD_L, 0.35 if x < 6 else 0.0)
            nail.putpixel((x, y), c + (255,))
    nail.save(os.path.join(out, "tusk_nail.png"))


if __name__ == "__main__":
    pack()
    build_textures()
    emit_java()
    build_items()
    print(f"ok: {len(PARTS)} parts")
