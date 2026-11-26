package io.github.roman16.backcountry.trap;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.food.Seasons;
import io.github.roman16.backcountry.hunt.Animal;
import io.github.roman16.backcountry.hunt.Track;
import io.github.roman16.backcountry.hunt.TrackBlock;
import io.github.roman16.backcountry.survivor.Team;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TrapEntity extends BlockEntity {
    private static final int STEP_TICKS = 20;
    private static final double TICKS_AN_HOUR = 1000;
    private static final double HOURS_A_DAY = 24;
    private static final List<Catch> FISH = List.of(new Catch("grayling", 200, 1200), new Catch("northern_pike", 800, 6000), new Catch("lake_trout", 1000, 8000),
            new Catch("char", 500, 3000), new Catch("burbot", 500, 3000), new Catch("whitefish", 400, 2000), new Catch("sockeye_salmon", 1500, 4000),
            new Catch("coho_salmon", 2000, 5000), new Catch("pink_salmon", 1200, 2500));
    private ItemStack caught = ItemStack.EMPTY;
    private ItemStack bait = ItemStack.EMPTY;
    private long clock;
    private boolean awake;

    private record Catch(String species, int fromG, int toG) {}

    public TrapEntity(BlockPos pos, BlockState state) {
        super(Things.TRAP_ENTITY.get(), pos, state);
    }

    public Kind kind() {
        return ((TrapBlock) getBlockState().getBlock()).kind();
    }

    public ItemStack caught() {
        return caught;
    }

    public ItemStack bait() {
        return bait;
    }

    public void bait(ItemStack offered) {
        bait = offered.copyWithCount(1);
        setChanged();
    }

    public ItemStack take() {
        ItemStack taken = caught;
        caught = ItemStack.EMPTY;
        show();
        return taken;
    }

    public List<ItemStack> contents() {
        return List.of(caught, bait);
    }

    private void show() {
        setChanged();
        if (level != null && getBlockState().getValue(TrapBlock.SPRUNG) != !caught.isEmpty()) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(TrapBlock.SPRUNG, !caught.isEmpty()));
        }
    }

    public void watch(ServerLevel level, double hours) {
        if (!caught.isEmpty() || hours <= 0) {
            return;
        }
        Balance.Traps traps = Balance.get().traps();
        double share = hours / HOURS_A_DAY * (bait.isEmpty() ? 1 : traps.baitShare());
        if (kind().fish()) {
            if (level.random.nextDouble() < traps.fishPerDay() * traps.fishShare() * hours / HOURS_A_DAY) {
                caught = fish(level);
                show();
            }
            return;
        }
        int day = Calendar.day(level);
        Team team = Team.get(level.getServer());
        for (ResourceLocation id : kind().animals()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            Optional<Animal> animal = Animal.of(type);
            if (animal.isEmpty() || !animal.get().inSeason(Seasons.month(level)) || day - team.lastTaken(id.toString()) < animal.get().restDays()
                    || !level.getBiome(worldPosition).is(animal.get().habitat())) {
                continue;
            }
            double run = kind() == Kind.SMALL_SNARE && animal.get().track().map(track -> onRun(level, track)).orElse(false) ? traps.runShare() : 1;
            if (level.random.nextDouble() < animal.get().perDay() * traps.share() * share * run) {
                ItemStack carcass = new ItemStack(Things.CARCASS_ITEM.get());
                carcass.set(Things.ANIMAL_TYPE.get(), id);
                caught = carcass;
                bait = ItemStack.EMPTY;
                team.took(id.toString(), day);
                show();
                return;
            }
        }
    }

    public boolean onRun(ServerLevel level, Track track) {
        int reach = Balance.get().traps().runBlocks();
        for (BlockPos near : BlockPos.betweenClosed(worldPosition.offset(-reach, -1, -reach), worldPosition.offset(reach, 1, reach))) {
            BlockState state = level.getBlockState(near);
            if (state.is(Things.TRACKS.get()) && state.getValue(TrackBlock.KIND) == track) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack fish(ServerLevel level) {
        Catch kind = FISH.get(level.random.nextInt(FISH.size()));
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("riverfishing", kind.species()));
        ItemStack fish = new ItemStack(item);
        int grams = kind.fromG() + level.random.nextInt(kind.toG() - kind.fromG() + 1);
        CompoundTag data = new CompoundTag();
        data.putString("Species", "riverfishing:" + kind.species());
        data.putInt("WeightG", grams);
        data.putInt("LengthCm", (int) Math.round(Math.cbrt(grams) * 4));
        data.putBoolean("Legal", true);
        fish.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return fish;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TrapEntity trap) {
        long now = level.getDayTime();
        if (!trap.awake) {
            trap.awake = true;
            trap.clock = now;
        }
        if ((level.getGameTime() + pos.asLong()) % STEP_TICKS != 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        double hours = Math.max(0, now - trap.clock) / TICKS_AN_HOUR;
        trap.clock = now;
        trap.watch(server, hours);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (!caught.isEmpty()) {
            tag.put("caught", caught.save(provider));
        }
        if (!bait.isEmpty()) {
            tag.put("bait", bait.save(provider));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        caught = ItemStack.parseOptional(provider, tag.getCompound("caught"));
        bait = ItemStack.parseOptional(provider, tag.getCompound("bait"));
    }
}
