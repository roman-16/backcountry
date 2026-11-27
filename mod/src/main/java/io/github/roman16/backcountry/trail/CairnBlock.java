package io.github.roman16.backcountry.trail;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CairnBlock extends Block {
    public static final MapCodec<CairnBlock> CODEC = simpleCodec(CairnBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 6, 14), Block.box(3, 6, 3, 13, 11, 13), Block.box(4, 11, 4, 12, 16, 12));

    public CairnBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CairnBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) || below.isFaceSturdy(level, pos.below(), Direction.UP);
    }
}
