package com.tuskmod.item;

import com.tuskmod.ModRegistry;
import com.tuskmod.TuskMod;
import com.tuskmod.ability.TuskAbilities;
import com.tuskmod.ability.TuskAttack;
import com.tuskmod.entity.TuskStandEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class TuskItem extends Item {
    public TuskItem(Settings settings) {
        super(settings);
    }

    // ------------------------------------------------------------ state helpers
    public static int getAct(ItemStack stack) {
        return Math.max(1, Math.min(4, stack.getOrDefault(ModRegistry.ACT, 1)));
    }

    public static TuskAttack getAttack(ItemStack stack) {
        List<TuskAttack> list = TuskAttack.forAct(getAct(stack));
        int idx = Math.floorMod(stack.getOrDefault(ModRegistry.ATTACK, 0), list.size());
        return list.get(idx);
    }

    /** Main hand first, then off hand. Returns ItemStack.EMPTY if the player holds no Tusk. */
    public static ItemStack heldTusk(PlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof TuskItem) return player.getMainHandStack();
        if (player.getOffHandStack().getItem() instanceof TuskItem) return player.getOffHandStack();
        return ItemStack.EMPTY;
    }

    public static int unlockedActs(PlayerEntity player) {
        if (player.isCreative() || TuskMod.UNLOCK_ALL) return 4;
        int acts = 1;
        for (int a = 2; a <= 4; a++) {
            if (player.experienceLevel >= TuskMod.ACT_LEVELS[a - 1]) acts = a;
        }
        return acts;
    }

    public static void cycleAct(ServerPlayerEntity player, ItemStack stack) {
        int act = getAct(stack);
        int unlocked = unlockedActs(player);
        int next = act % 4 + 1;
        if (next > unlocked) {
            if (unlocked == 1) { // nothing else to switch to: explain what is needed
                player.sendMessage(Text.translatable("message.tuskmod.locked", next, TuskMod.ACT_LEVELS[next - 1])
                        .formatted(Formatting.RED), true);
                return;
            }
            next = 1;
        }
        stack.set(ModRegistry.ACT, next);
        stack.set(ModRegistry.ATTACK, 0);
        announce(player, stack);
        TuskStandEntity.ensure(player);
    }

    public static void cycleAttack(ServerPlayerEntity player, ItemStack stack) {
        int size = TuskAttack.forAct(getAct(stack)).size();
        stack.set(ModRegistry.ATTACK, (stack.getOrDefault(ModRegistry.ATTACK, 0) + 1) % size);
        announce(player, stack);
    }

    private static void announce(ServerPlayerEntity player, ItemStack stack) {
        player.sendMessage(Text.translatable("message.tuskmod.attack", getAct(stack), getAttack(stack).displayName())
                .formatted(Formatting.GOLD), true);
    }

    // ------------------------------------------------------------ behaviour
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);

        if (player instanceof ServerPlayerEntity sp) {
            TuskAttack attack = getAttack(stack);
            if (attack.act == 4 && !sp.isCreative() && sp.getHungerManager().getFoodLevel() <= 6) {
                sp.sendMessage(Text.translatable("message.tuskmod.exhausted").formatted(Formatting.RED), true);
                return TypedActionResult.fail(stack);
            }
            TuskStandEntity.ensure(sp);
            TuskAbilities.execute(sp, attack);
            sp.addExhaustion(attack.exhaustion);
            sp.getItemCooldownManager().set(this, attack.cooldown);
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) return;
        if (player.age % 10 != 0) return;
        boolean inOffhand = player.getOffHandStack() == stack;
        if (selected || inOffhand) {
            TuskStandEntity.ensure(player);
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return getAct(stack) >= 4;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.tuskmod.act", getAct(stack)).formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("tooltip.tuskmod.attack", getAttack(stack).displayName()).formatted(Formatting.YELLOW));
        tooltip.add(Text.translatable("tooltip.tuskmod.use").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.tuskmod.keys", "Z", "X").formatted(Formatting.DARK_GRAY));
    }
}
