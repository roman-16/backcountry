package io.github.roman16.backcountry.food;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class BerryBushBlock extends SweetBerryBushBlock {
    public static final MapCodec<BerryBushBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Berry.CODEC.fieldOf("berry").forGetter(BerryBushBlock::berry), propertiesCodec()).apply(instance, BerryBushBlock::new));
    private static final Vec3 BRUSHING = new Vec3(0.9, 0.9, 0.9);
    private static final int RIPE = 3;

    private final Berry berry;

    public BerryBushBlock(Berry berry, Properties properties) {
        super(properties);
        this.berry = berry;
    }

    public Berry berry() {
        return berry;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<SweetBerryBushBlock> codec() {
        return (MapCodec<SweetBerryBushBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(berry.berries());
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) > 1 && Seasons.past(level, berry.months())) {
            level.setBlock(pos, state.setValue(AGE, 1), 2);
            return;
        }
        super.randomTick(state, level, pos, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity) {
            entity.makeStuckInBlock(state, BRUSHING);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int age = state.getValue(AGE);
        if (age <= 1) {
            return InteractionResult.PASS;
        }
        popResource(level, pos, new ItemStack(berry.berries(), 1 + level.random.nextInt(2) + (age == RIPE ? 1 : 0)));
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1, 0.8F + level.random.nextFloat() * 0.4F);
        BlockState picked = state.setValue(AGE, 1);
        level.setBlock(pos, picked, 2);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
