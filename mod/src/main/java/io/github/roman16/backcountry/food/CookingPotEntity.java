package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CookingPotEntity extends BlockEntity {
    public static final int FOODS = 6;
    private static final int STEP_TICKS = 20;
    private static final double TICKS_AN_HOUR = 1000;
    private final NonNullList<ItemStack> foods = NonNullList.withSize(FOODS, ItemStack.EMPTY);
    private ItemStack stew = ItemStack.EMPTY;
    private double waterL;
    private double cookedHours;
    private long clock;
    private boolean awake;

    public CookingPotEntity(BlockPos pos, BlockState state) {
        super(Things.COOKING_POT_ENTITY.get(), pos, state);
    }

    public double waterL() {
        return waterL;
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

    public boolean add(ItemStack food) {
        if (!stew.isEmpty() || level == null || level.getRecipeManager().getAllRecipesFor(Things.STEWING.get()).stream().noneMatch(recipe -> recipe.value().takes(food))) {
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

    public ItemStack takeFood() {
        for (int slot = FOODS - 1; slot >= 0; slot--) {
            if (!foods.get(slot).isEmpty()) {
                ItemStack taken = foods.get(slot);
                foods.set(slot, ItemStack.EMPTY);
                cookedHours = 0;
                setChanged();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack serve() {
        if (stew.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack served = stew.split(1);
        setChanged();
        return served;
    }

    private Optional<RecipeHolder<StewRecipe>> recipe() {
        return level.getRecipeManager().getRecipeFor(Things.STEWING.get(), new StewRecipe.Pot(foods), level);
    }

    public void advance(double hours, boolean lit) {
        Balance.Cooking cooking = Balance.get().cooking();
        if (!lit || !stew.isEmpty() || waterL < cooking.servingL()) {
            return;
        }
        Optional<RecipeHolder<StewRecipe>> recipe = recipe();
        if (recipe.isEmpty()) {
            return;
        }
        cookedHours += hours;
        if (cookedHours >= cooking.stewHours()) {
            stew = recipe.get().value().assemble(new StewRecipe.Pot(foods), level.registryAccess());
            foods.clear();
            waterL -= cooking.servingL() * stew.getCount();
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
        boolean lit = CampfireBlock.isLitCampfire(level.getBlockState(pos.below()));
        pot.advance(hours, lit);
        if (lit && pot.waterL > 0 && level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 1, 0.1, 0.05, 0.1, 0.01);
        }
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
