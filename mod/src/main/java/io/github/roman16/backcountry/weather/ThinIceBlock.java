package io.github.roman16.backcountry.weather;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class ThinIceBlock extends IceBlock {
    public static final int THICKEST = 2;
    public static final IntegerProperty THICKNESS = IntegerProperty.create("thickness", 0, THICKEST);
    private static final double RANDOM_TICKS_A_DAY = 24000.0 / 1365;

    public ThinIceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(THICKNESS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Balance.Ice ice = Balance.get().ice();
        double air = Climate.airC(level, pos, Calendar.day(level));
        if (air > ice.thawAboveC()) {
            melt(state, level, pos);
            return;
        }
        if (air <= ice.freezeBelowC() && random.nextDouble() < 1 / (RANDOM_TICKS_A_DAY * ice.thickenDays())) {
            int thickness = state.getValue(THICKNESS);
            level.setBlockAndUpdate(pos, thickness >= THICKEST ? Blocks.ICE.defaultBlockState() : state.setValue(THICKNESS, thickness + 1));
        }
    }
}
