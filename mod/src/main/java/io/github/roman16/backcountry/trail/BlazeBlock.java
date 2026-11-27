package io.github.roman16.backcountry.trail;

import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BlazeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<BlazeBlock> CODEC = simpleCodec(BlazeBlock::new);
    public static final BooleanProperty PITCH = BooleanProperty.create("pitch");
    private static final double RANDOM_TICKS_A_DAY = 24000.0 / 1365;
    private static final VoxelShape NORTH = Block.box(4, 3, 15, 12, 13, 16);
    private static final VoxelShape SOUTH = Block.box(4, 3, 0, 12, 13, 1);
    private static final VoxelShape EAST = Block.box(0, 3, 4, 1, 13, 12);
    private static final VoxelShape WEST = Block.box(15, 3, 4, 16, 13, 12);

    public BlazeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PITCH, false));
    }

    @Override
    protected MapCodec<BlazeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PITCH);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public static BlockPos log(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING).getOpposite());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(log(state, pos)).is(BlockTags.LOGS);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(PITCH);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(log(state, pos)).is(Blocks.SPRUCE_LOG) && random.nextDouble() < 1 / (RANDOM_TICKS_A_DAY * Balance.get().trail().pitchDays())) {
            level.setBlockAndUpdate(pos, state.setValue(PITCH, true));
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(PITCH) || !stack.is(Things.KNIVES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, state.setValue(PITCH, false));
            level.playSound(null, pos, SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 1, 1);
            ItemStack pitch = new ItemStack(Things.SPRUCE_PITCH.get());
            if (!player.getInventory().add(pitch)) {
                player.drop(pitch, false);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
