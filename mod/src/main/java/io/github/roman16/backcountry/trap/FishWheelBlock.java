package io.github.roman16.backcountry.trap;

import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.guide.Deed;
import io.github.roman16.backcountry.guide.FieldNotes;
import io.github.roman16.backcountry.guide.Guide;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FishWheelBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<FishWheelBlock> CODEC = simpleCodec(FishWheelBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape ALONG_X = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape ALONG_Z = Block.box(6, 0, 0, 10, 16, 16);

    public FishWheelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<FishWheelBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, WATERLOGGED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? ALONG_X : ALONG_Z;
    }

    public static boolean inStream(Level level, BlockPos pos) {
        return level.getFluidState(pos.below()).is(FluidTags.WATER) && Water.Source.at(level, pos.below()) == Water.Source.STREAM;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getFluidState(pos).is(FluidTags.WATER) || !inStream(level, pos)) {
            if (context.getPlayer() != null && !level.isClientSide) {
                context.getPlayer().displayClientMessage(Component.translatable("message.backcountry.fish_wheel_stream"), true);
            }
            return null;
        }
        return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis()).setValue(WATERLOGGED, true);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FishWheelEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Things.FISH_WHEEL_ENTITY.get(), FishWheelEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FishWheelEntity wheel) {
            if (wheel.caught().isEmpty()) {
                player.displayClientMessage(Component.translatable(inStream(level, pos) ? "message.backcountry.fish_wheel_empty" : "message.backcountry.fish_wheel_stream"), true);
            }
            List<ItemStack> fish = wheel.take();
            if (!fish.isEmpty() && player instanceof ServerPlayer server) {
                Guide.did(server, Deed.FISH_WHEEL_CATCH);
                fish.forEach(caught -> FieldNotes.caught(server, caught));
            }
            for (ItemStack caught : fish) {
                if (!player.getInventory().add(caught)) {
                    player.drop(caught, false);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FishWheelEntity wheel) {
            wheel.take().forEach(fish -> Block.popResource(level, pos, fish));
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
