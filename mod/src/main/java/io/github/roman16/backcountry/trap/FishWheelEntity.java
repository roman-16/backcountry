package io.github.roman16.backcountry.trap;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Inspectable;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.food.Fish;
import io.github.roman16.backcountry.food.Seasons;
import io.github.roman16.backcountry.hunt.Hunting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FishWheelEntity extends BlockEntity implements Inspectable {
    private static final int STEP_TICKS = 100;
    private static final double TICKS_AN_HOUR = 1000;
    private static final double HOURS_A_DAY = 24;
    private static final List<Fish.Catch> RUN = List.of(Fish.SOCKEYE, Fish.COHO, Fish.PINK);
    private static final List<Fish.Catch> OTHERS = List.of(Fish.WHITEFISH, Fish.GRAYLING);
    private final List<ItemStack> caught = new ArrayList<>();
    private long clock;
    private boolean awake;

    public FishWheelEntity(BlockPos pos, BlockState state) {
        super(Things.FISH_WHEEL_ENTITY.get(), pos, state);
    }

    public List<ItemStack> caught() {
        return caught;
    }

    public List<ItemStack> take() {
        List<ItemStack> taken = List.copyOf(caught);
        caught.clear();
        setChanged();
        return taken;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FishWheelEntity wheel) {
        long now = level.getDayTime();
        if (!wheel.awake) {
            wheel.awake = true;
            wheel.clock = now;
        }
        if ((level.getGameTime() + pos.asLong()) % STEP_TICKS != 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        double hours = Math.max(0, now - wheel.clock) / TICKS_AN_HOUR;
        wheel.clock = now;
        Balance.FishWheel rules = Balance.get().fishWheel();
        if (!FishWheelBlock.inStream(server, pos) || wheel.caught.size() >= rules.most()) {
            return;
        }
        boolean run = Hunting.months(Seasons.month(server), Balance.get().fish().runMonths());
        double perDay = run ? rules.runPerDay() : rules.othersPerDay();
        if (server.random.nextDouble() < perDay * hours / HOURS_A_DAY) {
            List<Fish.Catch> kinds = run ? RUN : OTHERS;
            wheel.caught.add(kinds.get(server.random.nextInt(kinds.size())).stack(server.random));
            wheel.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ListTag fish = new ListTag();
        caught.forEach(stack -> fish.add(stack.save(provider)));
        tag.put("caught", fish);
        tag.putLong("clock", clock);
        tag.putBoolean("awake", awake);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        caught.clear();
        for (Tag fish : tag.getList("caught", Tag.TAG_COMPOUND)) {
            ItemStack.parse(provider, fish).ifPresent(caught::add);
        }
        clock = tag.getLong("clock");
        awake = tag.getBoolean("awake");
    }

    @Override
    public List<Component> inspect() {
        int fish = caught().stream().mapToInt(ItemStack::getCount).sum();
        return List.of(fish == 0 ? Component.translatable("message.backcountry.fish_wheel_empty") : Component.translatable("fish_wheel.backcountry.inspect", fish));
    }
}
