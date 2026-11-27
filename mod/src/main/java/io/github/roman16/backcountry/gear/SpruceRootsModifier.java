package io.github.roman16.backcountry.gear;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class SpruceRootsModifier extends LootModifier {
    public static final MapCodec<SpruceRootsModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, SpruceRootsModifier::new));
    private static final int REACH = 2;

    public SpruceRootsModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null || context.getRandom().nextDouble() >= Balance.get().forage().rootsChance()) {
            return loot;
        }
        BlockPos dug = BlockPos.containing(origin);
        for (BlockPos near : BlockPos.betweenClosed(dug.offset(-REACH, 0, -REACH), dug.offset(REACH, REACH, REACH))) {
            if (context.getLevel().getBlockState(near).is(Blocks.SPRUCE_LOG)) {
                loot.add(new ItemStack(Things.SPRUCE_ROOTS.get()));
                return loot;
            }
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
