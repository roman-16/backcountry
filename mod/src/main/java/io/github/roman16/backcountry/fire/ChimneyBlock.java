package io.github.roman16.backcountry.fire;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ChimneyBlock extends Block {
    public static final MapCodec<ChimneyBlock> CODEC = simpleCodec(ChimneyBlock::new);
    private static final VoxelShape SHAPE = Shapes.join(Block.box(2, 0, 2, 14, 16, 14), Block.box(5, 0, 5, 11, 16, 11), BooleanOp.ONLY_FIRST);

    public ChimneyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ChimneyBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
