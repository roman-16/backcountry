package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Inspectable;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.client.Words;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CookingPotEntity extends BlockEntity implements Container, Inspectable {
    private static final double SPILL = 1e-6;
    public static final int FOODS = 6;
    public static final int STEW = FOODS;
    public static final int SLOTS = FOODS + 1;
    private static final int STEP_TICKS = 20;
    private static final double TICKS_AN_HOUR = 1000;
    private static final int MINUTES_AN_HOUR = 60;
    private final NonNullList<ItemStack> foods = NonNullList.withSize(FOODS, ItemStack.EMPTY);
    private ItemStack stew = ItemStack.EMPTY;
    private double waterL;
    private double cookedHours;
    private long clock;
    private boolean awake;

    public record Batch(RecipeHolder<StewRecipe> recipe, int batches) {}

    public CookingPotEntity(BlockPos pos, BlockState state) {
        super(Things.COOKING_POT_ENTITY.get(), pos, state);
    }

    public double waterL() {
        return waterL;
    }

    public double cookedHours() {
        return cookedHours;
    }

    public ItemStack stew() {
        return stew;
    }

    public NonNullList<ItemStack> foods() {
        return foods;
    }

    public double room() {
        return Balance.get().cooking().potL() - waterL;
    }

    public void pour(double litres) {
        waterL = Math.min(Balance.get().cooking().potL(), waterL + litres);
        setChanged();
    }

    public static boolean cooks(Level level, ItemStack food) {
        return !Spoilage.spoiled(food) && level.getRecipeManager().getAllRecipesFor(Things.STEWING.get()).stream().anyMatch(recipe -> recipe.value().takes(food));
    }

    public boolean add(ItemStack food) {
        if (!stew.isEmpty() || level == null || !cooks(level, food)) {
            return false;
        }
        for (int slot = 0; slot < FOODS; slot++) {
            if (foods.get(slot).isEmpty()) {
                foods.set(slot, food.copyWithCount(1));
                cookedHours = 0;
                setChanged();
                return true;
            }
        }
        return false;
    }

    public static boolean servedInBowls(ItemStack stew) {
        return stew.has(DataComponents.FOOD) && stew.get(DataComponents.FOOD).usingConvertsTo().isPresent();
    }

    public ItemStack serve() {
        if (stew.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack served = stew.split(1);
        setChanged();
        return served;
    }

    public static Optional<Batch> batch(Level level, List<ItemStack> foods) {
        StewRecipe.Pot pot = new StewRecipe.Pot(foods);
        for (RecipeHolder<StewRecipe> recipe : level.getRecipeManager().getAllRecipesFor(Things.STEWING.get())) {
            int batches = recipe.value().batches(pot);
            if (batches > 0) {
                return Optional.of(new Batch(recipe, batches));
            }
        }
        return Optional.empty();
    }

    public static Component status(Level level, List<ItemStack> foods, ItemStack stew, double waterL, double cookedHours, boolean lit) {
        if (!stew.isEmpty()) {
            return Component.translatable("cooking_pot.backcountry.ready", stew.getHoverName(), stew.getCount());
        }
        if (foods.stream().allMatch(ItemStack::isEmpty)) {
            return Component.translatable("cooking_pot.backcountry.empty");
        }
        Optional<Batch> batch = batch(level, foods);
        if (batch.isEmpty()) {
            StewRecipe.Pot pot = new StewRecipe.Pot(foods);
            return level.getRecipeManager().getAllRecipesFor(Things.STEWING.get()).stream().filter(recipe -> recipe.value().takesAll(pot)).findFirst()
                    .map(recipe -> Component.translatable("cooking_pot.backcountry.mix_short", recipe.value().result().getHoverName(), takes(recipe.value())))
                    .orElse(Component.translatable("cooking_pot.backcountry.mix"));
        }
        Component dish = batch.get().recipe().value().result().getHoverName();
        Balance.Cooking cooking = Balance.get().cooking();
        if (waterL < cooking.servingL()) {
            return Component.translatable("cooking_pot.backcountry.no_water", dish);
        }
        if (!lit) {
            return Component.translatable("cooking_pot.backcountry.fire_out", dish);
        }
        int left = (int) Math.ceil(Math.max(0, cooking.stewHours() - cookedHours) * MINUTES_AN_HOUR);
        return Component.translatable("cooking_pot.backcountry.cooking", dish, Words.duration(left));
    }

    private static Component takes(StewRecipe recipe) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Ingredient ingredient : recipe.ingredients()) {
            ItemStack[] options = ingredient.getItems();
            if (options.length > 0) {
                counts.merge(options[0].getItem(), 1, Integer::sum);
            }
        }
        List<String> parts = new ArrayList<>();
        counts.forEach((item, count) -> parts.add(count + " " + new ItemStack(item).getHoverName().getString()));
        return Component.literal(String.join(", ", parts));
    }

    public boolean lit() {
        return level != null && CampfireBlock.isLitCampfire(level.getBlockState(worldPosition.below()));
    }

    @Override
    public List<Component> inspect() {
        if (level == null) {
            return List.of();
        }
        return List.of(Component.translatable("cooking_pot.backcountry.water", Words.one(waterL)), status(level, foods, stew, waterL, cookedHours, lit()));
    }

    public void advance(double hours, boolean lit) {
        Balance.Cooking cooking = Balance.get().cooking();
        if (!lit || !stew.isEmpty() || waterL < cooking.servingL() || level == null) {
            return;
        }
        Optional<Batch> batch = batch(level, foods);
        if (batch.isEmpty()) {
            return;
        }
        cookedHours += hours;
        if (cookedHours >= cooking.stewHours()) {
            StewRecipe recipe = batch.get().recipe().value();
            stew = recipe.assemble(new StewRecipe.Pot(foods), level.registryAccess());
            stew.setCount(Math.min(recipe.servings() * batch.get().batches(), (int) Math.floor(waterL / cooking.servingL() + SPILL)));
            foods.clear();
            waterL = Math.max(0, waterL - cooking.servingL() * stew.getCount());
            cookedHours = 0;
        }
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CookingPotEntity pot) {
        long now = level.getDayTime();
        if (!pot.awake) {
            pot.awake = true;
            pot.clock = now;
        }
        if ((level.getGameTime() + pos.asLong()) % STEP_TICKS != 0) {
            return;
        }
        double hours = Math.max(0, now - pot.clock) / TICKS_AN_HOUR;
        pot.clock = now;
        boolean lit = pot.lit();
        pot.advance(hours, lit);
        if (lit && pot.waterL > 0 && level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 1, 0.1, 0.05, 0.1, 0.01);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return stew.isEmpty() && foods.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == STEW ? stew : foods.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot == STEW) {
            ItemStack taken = stew.split(count);
            setChanged();
            return taken;
        }
        ItemStack taken = ContainerHelper.removeItem(foods, slot, count);
        if (!taken.isEmpty()) {
            cookedHours = 0;
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == STEW) {
            ItemStack taken = stew;
            stew = ItemStack.EMPTY;
            return taken;
        }
        cookedHours = 0;
        return ContainerHelper.takeItem(foods, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == STEW) {
            stew = stack;
        } else {
            foods.set(slot, stack);
            cookedHours = 0;
        }
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < FOODS && stew.isEmpty() && level != null && cooks(level, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        foods.clear();
        stew = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, foods, provider);
        if (!stew.isEmpty()) {
            tag.put("stew", stew.save(provider));
        }
        tag.putDouble("water_l", waterL);
        tag.putDouble("cooked_hours", cookedHours);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        foods.clear();
        ContainerHelper.loadAllItems(tag, foods, provider);
        stew = ItemStack.parseOptional(provider, tag.getCompound("stew"));
        waterL = tag.getDouble("water_l");
        cookedHours = tag.getDouble("cooked_hours");
    }
}
