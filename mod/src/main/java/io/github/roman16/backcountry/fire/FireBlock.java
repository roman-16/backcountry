package io.github.roman16.backcountry.fire;

import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.guide.Deed;
import io.github.roman16.backcountry.guide.Guide;
import io.github.roman16.backcountry.net.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import io.github.roman16.backcountry.gear.CampPotItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;

public final class FireBlock extends CampfireBlock {
    public static final MapCodec<FireBlock> CODEC = simpleCodec(FireBlock::new);
    public static final BooleanProperty COALS = BooleanProperty.create("coals");
    private static final int FIRE_DAMAGE = 1;
    private static final int LIT_LIGHT = 15;
    private static final int COALS_LIGHT = 8;
    private static final int COALS_SMOKE_CHANCE = 8;

    public FireBlock(Properties properties) {
        super(true, FIRE_DAMAGE, properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(COALS, false).setValue(SIGNAL_FIRE, false).setValue(WATERLOGGED, false)
                .setValue(FACING, Direction.NORTH));
    }

    public static int light(BlockState state) {
        if (state.getValue(LIT)) {
            return LIT_LIGHT;
        }
        return state.getValue(COALS) ? COALS_LIGHT : 0;
    }

    public static boolean cold(BlockState state) {
        return !state.getValue(LIT) && !state.getValue(COALS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<CampfireBlock> codec() {
        return (MapCodec<CampfireBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COALS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LIT, false).setValue(COALS, false);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return state.getValue(LIT) ? createTickerHelper(type, BlockEntityType.CAMPFIRE, CampfireBlockEntity::particleTick) : null;
        }
        return createTickerHelper(type, BlockEntityType.CAMPFIRE, (tickLevel, pos, tickState, campfire) -> {
            if (tickState.getValue(LIT)) {
                CampfireBlockEntity.cookTick(tickLevel, pos, tickState, campfire);
            } else {
                CampfireBlockEntity.cooldownTick(tickLevel, pos, tickState, campfire);
            }
            Fire.tick(tickLevel, pos, tickState, campfire);
        });
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.canPerformAction(ItemAbilities.SHOVEL_DOUSE) && !cold(state)) {
            if (!level.isClientSide) {
                Fire fire = Fire.of(campfire);
                fire.putOut();
                Fire.show(level, pos, state, fire);
                campfire.setChanged();
                level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1, 1);
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            } else {
                dowse(player, level, pos, state);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FirewoodItem) {
            return feed(stack, level, pos, player, campfire);
        }
        if (stack.getItem() instanceof BlockItem || stack.getItem() instanceof CampPotItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (cold(state) && (stack.is(Things.BOW_DRILL.get()) || stack.is(Items.FLINT_AND_STEEL))) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            ServerPlayer survivor = (ServerPlayer) player;
            if (!Kindling.carried(survivor)) {
                survivor.displayClientMessage(Component.translatable("message.backcountry.needs_kindling", Balance.get().fire().kindlingSticks()), true);
            } else if (stack.is(Items.FLINT_AND_STEEL)) {
                Kindling.use(survivor);
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1, level.random.nextFloat() * 0.4F + 0.8F);
                light(level, pos, state, campfire);
            } else {
                Messages.bowDrill(survivor, pos, Kindling.wet(survivor, pos));
            }
            return ItemInteractionResult.CONSUME;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    private static ItemInteractionResult feed(ItemStack stack, Level level, BlockPos pos, Player player, CampfireBlockEntity campfire) {
        Fire fire = Fire.of(campfire);
        int free = fire.free();
        if (FirewoodItem.catches(stack) && free < 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (!FirewoodItem.catches(stack)) {
                player.displayClientMessage(Component.translatable("tooltip.backcountry.too_green"), true);
            } else {
                fire.queue.set(free, stack.split(1));
                campfire.setChanged();
                if (player instanceof ServerPlayer server) {
                    Guide.did(server, Deed.FED_FIRE);
                }
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1, 0.8F);
                player.displayClientMessage(Component.translatable("message.backcountry.queued", fire.queue.stream().filter(piece -> !piece.isEmpty()).count(), Fire.QUEUE),
                        true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
            player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new FireMenu(id, inventory, campfire), Component.translatable("block.backcountry.campfire")),
                    buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void light(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        Fire fire = Fire.of(campfire);
        fire.light();
        Fire.show(level, pos, state, fire);
        campfire.setChanged();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (state.getValue(COALS) && random.nextInt(COALS_SMOKE_CHANCE) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.25 + random.nextDouble() / 2, pos.getY() + 0.4, pos.getZ() + 0.25 + random.nextDouble() / 2, 0, 0.02, 0);
            if (random.nextBoolean()) {
                level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.3, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.005, 0);
            }
        }
    }
}
