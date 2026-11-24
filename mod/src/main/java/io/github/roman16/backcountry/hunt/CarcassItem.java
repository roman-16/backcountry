package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Things;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class CarcassItem extends Item {
    public CarcassItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static EntityType<?> animal(ItemStack stack) {
        ResourceLocation id = stack.get(Things.ANIMAL_TYPE.get());
        return id == null ? EntityType.RABBIT : BuiltInRegistries.ENTITY_TYPE.get(id);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.backcountry.carcass_of", animal(stack).getDescription());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPlaceContext place = new BlockPlaceContext(context);
        Level level = context.getLevel();
        BlockPos at = place.getClickedPos();
        if (!place.canPlace() || !level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide && level.setBlockAndUpdate(at, Things.CARCASS.get().defaultBlockState()) && level.getBlockEntity(at) instanceof CarcassEntity carcass) {
            ItemStack stack = context.getItemInHand();
            carcass.hold(animal(stack), stack.getOrDefault(Things.ARROWS.get(), List.of()));
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
