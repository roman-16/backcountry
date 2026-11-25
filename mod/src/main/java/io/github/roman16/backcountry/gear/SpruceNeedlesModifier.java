package io.github.roman16.backcountry.gear;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class SpruceNeedlesModifier extends LootModifier {
    public static final MapCodec<SpruceNeedlesModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, SpruceNeedlesModifier::new));

    public SpruceNeedlesModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        int most = Balance.get().forage().needlesALeaf();
        loot.add(new ItemStack(Things.SPRUCE_NEEDLES.get(), 1 + context.getRandom().nextInt(most)));
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
