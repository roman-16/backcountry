package io.github.roman16.backcountry.trap;

import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.guide.Guide;
import io.github.roman16.backcountry.guide.Task;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TrapBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty SPRUNG = BooleanProperty.create("sprung");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 8, 14);
    private static final int WATER_REACH = 2;
    private final Kind kind;

    public TrapBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(SPRUNG, false).setValue(WATERLOGGED, false));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new TrapBlock(properties, kind));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPRUNG, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(FluidTags.WATER));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
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
        BlockPos below = pos.below();
        return switch (kind) {
            case GILL_NET -> level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource();
            case TIP_UP -> level.getFluidState(below).is(FluidTags.WATER) && Direction.Plane.HORIZONTAL.stream().anyMatch(side -> level.getBlockState(below.relative(side)).is(BlockTags.ICE));
            case LARGE_SNARE -> sturdy(level, below) && nearWater(level, pos);
            default -> sturdy(level, below) && level.getFluidState(pos).isEmpty();
        };
    }

    private static boolean sturdy(LevelReader level, BlockPos below) {
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    private static boolean nearWater(LevelReader level, BlockPos pos) {
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-WATER_REACH, -1, -WATER_REACH), pos.offset(WATER_REACH, 0, WATER_REACH))) {
            if (level.getFluidState(at).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrapEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Things.TRAP_ENTITY.get(), TrapEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!kind.baited() || !stack.is(Things.CRICKET.get()) || !(level.getBlockEntity(pos) instanceof TrapEntity trap) || !trap.bait().isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            trap.bait(stack.split(1));
            player.displayClientMessage(Component.translatable("message.backcountry.trap_baited"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TrapEntity trap) {
            if (trap.caught().isEmpty()) {
                player.displayClientMessage(Component.translatable(trap.bait().isEmpty() ? "message.backcountry.trap_set" : "message.backcountry.trap_set_baited"), true);
            } else {
                ItemStack taken = trap.take();
                player.displayClientMessage(Component.translatable("message.backcountry.trap_took", taken.getHoverName()), true);
                if (player instanceof ServerPlayer server) {
                    Guide.done(server, Task.TRAP_CATCH);
                }
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TrapEntity trap) {
            trap.contents().forEach(stack -> Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack));
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    public static boolean guards(Level level, BlockPos cache, int reach) {
        for (BlockPos at : BlockPos.betweenClosed(cache.offset(-reach, -reach, -reach), cache.offset(reach, reach, reach))) {
            if (level.getBlockState(at).getBlock() instanceof TrapBlock trap && trap.kind().keepsMiceOff()) {
                return true;
            }
        }
        return false;
    }
}
