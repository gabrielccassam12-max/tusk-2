package com.tuskmod;

import com.mojang.serialization.Codec;
import com.tuskmod.entity.TuskNailEntity;
import com.tuskmod.entity.TuskStandEntity;
import com.tuskmod.item.TuskItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public final class ModRegistry {
    private ModRegistry() {}

    /** Current Act (1-4), stored on the item stack. */
    public static final ComponentType<Integer> ACT = Registry.register(Registries.DATA_COMPONENT_TYPE,
            TuskMod.id("act"),
            ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).build());

    /** Selected attack index inside the current Act. */
    public static final ComponentType<Integer> ATTACK = Registry.register(Registries.DATA_COMPONENT_TYPE,
            TuskMod.id("attack"),
            ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).build());

    public static final Item TUSK = Registry.register(Registries.ITEM, TuskMod.id("tusk"),
            new TuskItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).component(ACT, 1).component(ATTACK, 0)));

    /** Only used as the visual model of the flying nails. Not obtainable. */
    public static final Item TUSK_NAIL = Registry.register(Registries.ITEM, TuskMod.id("tusk_nail"),
            new Item(new Item.Settings()));

    public static final EntityType<TuskNailEntity> NAIL = Registry.register(Registries.ENTITY_TYPE,
            TuskMod.id("nail"),
            FabricEntityTypeBuilder.<TuskNailEntity>create(SpawnGroup.MISC, TuskNailEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                    .trackRangeBlocks(96)
                    .trackedUpdateRate(1)
                    .forceTrackedVelocityUpdates(true)
                    .disableSummon()
                    .build());

    public static final EntityType<TuskStandEntity> STAND = Registry.register(Registries.ENTITY_TYPE,
            TuskMod.id("stand"),
            FabricEntityTypeBuilder.<TuskStandEntity>create(SpawnGroup.MISC, TuskStandEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .trackRangeBlocks(96)
                    .trackedUpdateRate(1)
                    .disableSummon()
                    .build());

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(TUSK));
    }
}
