package io.github.roman16.backcountry.coast;

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

public class ClamModifier extends LootModifier {
    public static final MapCodec<ClamModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, ClamModifier::new));

    public ClamModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (context.getRandom().nextDouble() < Balance.get().coast().clamChance()) {
            loot.add(new ItemStack(Things.CLAMS.get()));
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
