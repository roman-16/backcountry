package io.github.roman16.backcountry.food;

import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.gear.WaterVesselItem;
import io.github.roman16.backcountry.guide.Deed;
import io.github.roman16.backcountry.guide.Guide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CookingPotBlock extends BaseEntityBlock {
    public static final MapCodec<CookingPotBlock> CODEC = simpleCodec(CookingPotBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    public CookingPotBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof CampfireBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CookingPotEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Things.COOKING_POT_ENTITY.get(), CookingPotEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CookingPotEntity pot)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof WaterVesselItem) {
            if (!level.isClientSide) {
                Water held = WaterVesselItem.water(stack);
                float poured = (float) Math.min(pot.room(), held.litres());
                if (poured > 0) {
                    stack.set(Things.WATER.get(), held.take(poured));
                    pot.pour(poured);
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 1.2F);
                }
                tell(player, pot);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(Items.BOWL) && !pot.stew().isEmpty() && pot.stew().has(DataComponents.FOOD) && pot.stew().get(DataComponents.FOOD).usingConvertsTo().isPresent()) {
            if (!level.isClientSide) {
                ItemStack served = pot.serve();
                stack.shrink(1);
                if (!player.getInventory().add(served)) {
                    player.drop(served, false);
                }
                if (player instanceof ServerPlayer survivor) {
                    Guide.did(survivor, Deed.SERVED_STEW);
                }
                tell(player, pot);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && pot.add(stack)) {
            stack.shrink(1);
            tell(player, pot);
            return ItemInteractionResult.SUCCESS;
        }
        if (!level.isClientSide && Spoilage.spoiled(stack) && Nutrition.of(stack).isEmpty() && stack.is(Things.MOOSE_HEAD.get())) {
            player.displayClientMessage(Component.translatable("message.backcountry.pot_spoiled"), true);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CookingPotEntity pot) {
            player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new CookingPotMenu(id, inventory, pot), Component.translatable("block.backcountry.cooking_pot")),
                    buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void tell(Player player, CookingPotEntity pot) {
        player.displayClientMessage(CookingPotEntity.status(player.level(), pot.foods(), pot.stew(), pot.waterL(), pot.cookedHours(), pot.lit()), true);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CookingPotEntity pot) {
            pot.foods().forEach(food -> Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), food));
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), pot.stew());
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
