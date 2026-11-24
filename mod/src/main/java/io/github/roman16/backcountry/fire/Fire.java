package io.github.roman16.backcountry.fire;

import com.teamtea.eclipticseasons.api.EclipticSeasonsApi;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class Fire implements INBTSerializable<CompoundTag> {
    public static final int QUEUE = 4;
    private static final int STEP_TICKS = 20;
    private static final double TICKS_AN_HOUR = 1000;
    private static final int SPARK_TRIES = 16;
    private static final double SPENT = 1e-6;

    public final NonNullList<ItemStack> queue = NonNullList.withSize(QUEUE, ItemStack.EMPTY);
    public Setting setting = Setting.STEADY;
    private double pieceHours;
    private double pieceHeat;
    private double coalHours;
    private long clock;
    private boolean awake;

    public enum Setting {
        LOW,
        STEADY,
        HOT;

        public double burn() {
            Balance.Fire fire = Balance.get().fire();
            return switch (this) {
                case LOW -> fire.lowBurn();
                case STEADY -> 1;
                case HOT -> fire.hotBurn();
            };
        }

        public double heatC() {
            Balance.Fire fire = Balance.get().fire();
            return switch (this) {
                case LOW -> fire.lowC();
                case STEADY -> fire.steadyC();
                case HOT -> fire.hotC();
            };
        }
    }

    public static Fire of(CampfireBlockEntity campfire) {
        return campfire.getData(Things.FIRE);
    }

    public boolean lit() {
        return pieceHours > 0;
    }

    public boolean coals() {
        return !lit() && coalHours > 0;
    }

    public double coalHours() {
        return coalHours;
    }

    public double burningHours(boolean wet) {
        return lit() ? pieceHours / rate(wet) : 0;
    }

    public double queuedHours(boolean wet) {
        double hours = 0;
        for (ItemStack piece : queue) {
            if (piece.getItem() instanceof FirewoodItem wood) {
                hours += wood.hours();
            }
        }
        return hours / rate(wet);
    }

    public double heatC() {
        if (lit()) {
            return setting.heatC() * pieceHeat;
        }
        return coals() ? Balance.get().fire().coalsC() : 0;
    }

    public static double hottestC() {
        Balance.Fire fire = Balance.get().fire();
        return fire.hotC() * fire.hardwoodHeat();
    }

    public boolean queued() {
        return queue.stream().anyMatch(piece -> !piece.isEmpty());
    }

    public int free() {
        for (int slot = 0; slot < QUEUE; slot++) {
            if (queue.get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    public boolean packed() {
        return lit() && setting == Setting.HOT && queue.stream().noneMatch(ItemStack::isEmpty);
    }

    public boolean banked() {
        return coalHours >= Balance.get().fire().bankedHours();
    }

    public void light() {
        Balance.Fire fire = Balance.get().fire();
        pieceHours = fire.kindlingHours();
        pieceHeat = fire.kindlingHeat();
    }

    public void putOut() {
        pieceHours = 0;
        coalHours = 0;
    }

    public void advance(double hours, boolean wet) {
        Balance.Fire fire = Balance.get().fire();
        double left = hours;
        while (left > SPENT) {
            if (lit()) {
                double rate = rate(wet);
                double spent = Math.min(left, pieceHours / rate);
                pieceHours -= spent * rate;
                coalHours = Math.min(fire.mostCoalsHours(), coalHours + spent);
                left -= spent;
                if (pieceHours <= SPENT) {
                    pieceHours = 0;
                    catchNext();
                }
            } else if (coalHours > 0) {
                if (catchNext()) {
                    continue;
                }
                double decay = wet ? fire.rainBurn() : 1;
                double spent = Math.min(left, coalHours / decay);
                coalHours -= spent * decay;
                left -= spent;
                if (coalHours <= SPENT) {
                    coalHours = 0;
                }
            } else {
                return;
            }
        }
        if (coals()) {
            catchNext();
        }
    }

    private boolean catchNext() {
        for (int slot = 0; slot < QUEUE; slot++) {
            ItemStack piece = queue.get(slot);
            if (piece.getItem() instanceof FirewoodItem wood) {
                pieceHours = wood.hours();
                pieceHeat = wood.heat(piece);
                for (int after = slot; after < QUEUE - 1; after++) {
                    queue.set(after, queue.get(after + 1));
                }
                queue.set(QUEUE - 1, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    private double rate(boolean wet) {
        return setting.burn() * (wet ? Balance.get().fire().rainBurn() : 1);
    }

    public static boolean rainedOn(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        return level.canSeeSky(above) && EclipticSeasonsApi.getInstance().isRainOrSnowAt(level, above);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        Fire fire = of(campfire);
        long now = level.getDayTime();
        if (!fire.awake) {
            fire.awake = true;
            fire.clock = now;
        }
        if ((level.getGameTime() + pos.asLong()) % STEP_TICKS != 0) {
            return;
        }
        if (fire.lit() != state.getValue(CampfireBlock.LIT) || state.getValue(CampfireBlock.WATERLOGGED)) {
            if (fire.lit() || state.getValue(CampfireBlock.WATERLOGGED)) {
                fire.putOut();
            } else {
                fire.catchNext();
            }
        }
        double hours = Math.max(0, now - fire.clock) / TICKS_AN_HOUR;
        fire.clock = now;
        fire.advance(hours, rainedOn(level, pos));
        if (fire.packed()) {
            spark(level, pos, hours);
        }
        show(level, pos, state, fire);
        campfire.setChanged();
    }

    public static void show(Level level, BlockPos pos, BlockState state, Fire fire) {
        BlockState shown = state.setValue(CampfireBlock.LIT, fire.lit()).setValue(FireBlock.COALS, fire.coals());
        if (shown != state) {
            level.setBlockAndUpdate(pos, shown);
        }
    }

    public static void spark(Level level, BlockPos pos, double hours) {
        Balance.Fire balance = Balance.get().fire();
        double chance = 1 - Math.pow(1 - balance.sparkChance(), hours);
        if (level.random.nextDouble() >= chance) {
            return;
        }
        int reach = balance.sparkBlocks();
        for (int attempt = 0; attempt < SPARK_TRIES; attempt++) {
            BlockPos at = pos.offset(level.random.nextInt(2 * reach + 1) - reach, level.random.nextInt(reach + 1), level.random.nextInt(2 * reach + 1) - reach);
            if (!at.equals(pos) && level.isEmptyBlock(at) && catches(level, at)) {
                level.setBlockAndUpdate(at, BaseFireBlock.getState(level, at));
                return;
            }
        }
    }

    private static boolean catches(Level level, BlockPos at) {
        for (Direction direction : Direction.values()) {
            BlockPos next = at.relative(direction);
            if (level.getBlockState(next).isFlammable(level, next, direction.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, queue, provider);
        tag.putString("setting", setting.name());
        tag.putDouble("piece_hours", pieceHours);
        tag.putDouble("piece_heat", pieceHeat);
        tag.putDouble("coal_hours", coalHours);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        ContainerHelper.loadAllItems(tag, queue, provider);
        setting = Setting.valueOf(tag.getString("setting"));
        pieceHours = tag.getDouble("piece_hours");
        pieceHeat = tag.getDouble("piece_heat");
        coalHours = tag.getDouble("coal_hours");
    }
}
