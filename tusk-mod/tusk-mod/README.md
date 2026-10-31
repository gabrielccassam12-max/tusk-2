# Tusk - JoJo's Bizarre Adventure: Steel Ball Run (Fabric 1.21.1)

Tusk, Stand of Johnny Joestar, as a Minecraft mod. An animated stand floats beside you while you hold the
**Tusk** item, with 16 attacks spread over 4 Acts.

## Build
Requires **JDK 21**. This zip has no Gradle wrapper jar, so generate one first:

    gradle wrapper --gradle-version 8.10.2     # needs a local Gradle install, or copy gradlew/gradle/ from the Fabric example mod
    ./gradlew build                            # jar appears in build/libs/
    ./gradlew runClient                        # test in-game

Install the jar in a Fabric 1.21.1 instance with Fabric API 0.105.0+1.21.1 (or newer for 1.21.1).

## Getting it
Creative: Combat tab. Survival: gold ingots + bone blocks + eye of ender (see `data/tuskmod/recipe/tusk.json`).

## Controls
| Input | Action |
|---|---|
| Right click | Use selected attack |
| **Z** | Switch Act (rebindable) |
| **X** | Switch attack inside the Act (rebindable) |

## Progression
Acts unlock by XP level: Act 2 = 5, Act 3 = 15, Act 4 = 30 (creative: all). Change `ACT_LEVELS`/`UNLOCK_ALL`
in `TuskMod.java`. Act 4 attacks need food level > 6 and cost a lot of hunger.
`BREAK_BLOCKS` in the same file turns off all block destruction (drill / wormhole nails).

## Attacks
**Act 1 - Nails:** Nail Shot, Nail Fan (7 nails), Ricochet Nail (4 bounces, gains damage), Rapid Fire (8 re-aimed nails)
**Act 2 - Spin:** Spinning Nail (bleed), Drill Shot (pierces 5, breaks glass/leaves), Nail Rain (area barrage), Spiral Volley (corkscrew)
**Act 3 - Wormhole:** Wormhole Nail (digs a hole, slows/weakens), Homing Spiral (5 hunting nails), Tusk Guard (deflects projectiles and counter-fires), Rotation Shockwave (slam + ring of nails)
**Act 4 - Infinite Rotation:** Infinite Rotation (homing nail + 10 pulses of damage), Wormhole Dash (blink through enemies, burst of nails), Golden Encirclement (16 portals surround the target), Infinite Void (black hole, then 24 hunting nails)

## Model & textures
The stand model is defined once in `tools/generate_assets.py`; running it regenerates the textures AND
`TuskStandParts.java` so UVs never drift. Needs Pillow:

    pip install pillow && python3 tools/generate_assets.py

Armour grows per Act: shoulder spikes (2), back plate + thorn fan (3), spinning golden halo (4).
Poses (in `TuskStandModel.java`) are triggered by each attack. Eyes, nails, chest spiral and halo use an emissive layer.

## Important: not compiled by the author
This was written against Yarn mappings 1.21.1+build.3 without access to Minecraft, so it was only
syntax-checked. If Gradle reports a method/field name error, it is a mapping detail - fix that line.
Likeliest spots: `FabricEntityTypeBuilder` (deprecated but present), `timeUntilRegen`, `ProjectileUtil.getCollision`.
