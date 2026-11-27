package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.survivor.Team;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

public final class CacheEntity extends BaseContainerBlockEntity {
    public static final int SLOTS = 18;
    private static final int STEP_TICKS = 20;
    long raidedAt = Long.MIN_VALUE / 2;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public CacheEntity(BlockPos pos, BlockState state) {
        super(Things.FOOD_CACHE_ENTITY.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.backcountry.food_cache");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new CacheMenu(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    public List<ItemStack> stacks() {
        return items;
    }

    public double usableKcal() {
        return Pantry.usableKcal(items);
    }

    public boolean scented() {
        return Pantry.meatyKcal(items) >= Balance.get().cache().scentKcal();
    }

    public double airC() {
        return Spoilage.airC(level, worldPosition);
    }

    public enum Reach {
        GROUND,
        TALL,
        LADDER
    }

    public Reach reach() {
        if (level == null) {
            return Reach.GROUND;
        }
        int poles = 0;
        BlockPos below = worldPosition.below();
        boolean ladder = ladderBeside(worldPosition);
        while (poles < Balance.get().raids().tallPoles() && level.getBlockState(below).is(Tags.Blocks.STRIPPED_LOGS)) {
            ladder |= ladderBeside(below);
            poles++;
            below = below.below();
        }
        if (poles < Balance.get().raids().tallPoles()) {
            return Reach.GROUND;
        }
        return ladder ? Reach.LADDER : Reach.TALL;
    }

    private boolean ladderBeside(BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(BlockTags.CLIMBABLE)) {
                return true;
            }
        }
        return false;
    }

    public boolean raidedBy(EntityType<?> raider, RandomSource random) {
        Balance.Raids raids = Balance.get().raids();
        boolean fouls = raids.fouling().contains(BuiltInRegistries.ENTITY_TYPE.getKey(raider).toString());
        List<ItemStack> meat = new ArrayList<>(items.stream().filter(stack -> Pantry.kcal(stack) > 0 && Pantry.meaty(stack)).toList());
        if (meat.isEmpty()) {
            return false;
        }
        double hunger = fouls ? raids.wolverineKcal() : raids.bearKcal();
        while (hunger > 0 && !meat.isEmpty()) {
            ItemStack bitten = meat.get(random.nextInt(meat.size()));
            hunger -= Nutrition.of(bitten).map(Nutrition::kcal).orElse(hunger);
            bitten.shrink(1);
            if (bitten.isEmpty()) {
                meat.remove(bitten);
            }
        }
        if (fouls) {
            items.stream().filter(stack -> Pantry.kcal(stack) > 0 && !stack.has(Things.FOULED.get())).findAny()
                    .ifPresent(stack -> stack.set(Things.FOULED.get(), Unit.INSTANCE));
        }
        setChanged();
        return true;
    }

    public void report() {
        if (level instanceof ServerLevel server) {
            Team.get(server.getServer()).stocked(worldPosition, usableKcal());
        }
    }

    public boolean raid(RandomSource random) {
        List<ItemStack> food = new ArrayList<>(items.stream().filter(stack -> Pantry.kcal(stack) > 0).toList());
        if (food.isEmpty()) {
            return false;
        }
        double hunger = Balance.get().mice().kcal();
        while (hunger > 0 && !food.isEmpty()) {
            ItemStack bitten = food.get(random.nextInt(food.size()));
            hunger -= Nutrition.of(bitten).map(Nutrition::kcal).orElse(hunger);
            bitten.shrink(1);
            if (bitten.isEmpty()) {
                food.remove(bitten);
            }
        }
        if (!food.isEmpty()) {
            food.get(random.nextInt(food.size())).set(Things.CONTAMINATED.get(), Unit.INSTANCE);
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack held = items.get(slot);
            if (held.isEmpty()) {
                items.set(slot, new ItemStack(Things.MOUSE_DROPPINGS.get()));
                break;
            }
            if (held.is(Things.MOUSE_DROPPINGS.get()) && held.getCount() < held.getMaxStackSize()) {
                held.grow(1);
                break;
            }
        }
        setChanged();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CacheEntity cache) {
        if ((level.getGameTime() + pos.asLong()) % STEP_TICKS != 0) {
            return;
        }
        Spoilage.ageContainer(cache);
        cache.report();
        Raids.check((ServerLevel) level, pos, cache);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putLong("raided_at", raidedAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        raidedAt = tag.contains("raided_at") ? tag.getLong("raided_at") : Long.MIN_VALUE / 2;
    }
}
