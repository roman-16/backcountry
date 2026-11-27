package io.github.roman16.backcountry.coast;

import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.Things;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GooseNestBlock extends Block {
    public static final MapCodec<GooseNestBlock> CODEC = simpleCodec(GooseNestBlock::new);
    public static final IntegerProperty EGGS = IntegerProperty.create("eggs", 1, 4);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 4, 14);

    public GooseNestBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(EGGS, 1));
    }

    @Override
    protected MapCodec<GooseNestBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EGGS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            ItemStack egg = new ItemStack(Things.GOOSE_EGG.get());
            if (!player.getInventory().add(egg)) {
                player.drop(egg, false);
            }
            int eggs = state.getValue(EGGS);
            level.setBlockAndUpdate(pos, eggs > 1 ? state.setValue(EGGS, eggs - 1) : Blocks.AIR.defaultBlockState());
            level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
