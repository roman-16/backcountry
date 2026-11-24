package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.fire.FirewoodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SmokerEntity extends BaseContainerBlockEntity {
    public static final int HANGING = 4;
    public static final int FIREWOOD = 2;
    public static final int SLOTS = HANGING + FIREWOOD;
    private static final int STEP_TICKS = 20;
    private static final double TICKS_AN_HOUR = 1000;
    private static final double SPENT = 1e-6;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private double burnHours;
    private double smokeHours;
    private double batchHours;
    private long clock;
    private boolean awake;

    public enum Smoke {
        COLD,
        SMOULDERING,
        RISING
    }

    public SmokerEntity(BlockPos pos, BlockState state) {
        super(Things.SMOKER_ENTITY.get(), pos, state);
    }

    public static boolean hangs(ItemStack stack) {
        return Pantry.meaty(stack) && !stack.has(Things.SMOKED.get());
    }

    public Smoke smoke() {
        if (burnHours <= 0) {
            return Smoke.COLD;
        }
        return smokeHours >= Balance.get().smoker().risingHours() ? Smoke.RISING : Smoke.SMOULDERING;
    }

    public double batchHours() {
        return batchHours;
    }

    public double burnHours() {
        return burnHours;
    }

    private boolean hanging() {
        for (int slot = 0; slot < HANGING; slot++) {
            if (!items.get(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean kindle() {
        for (int slot = HANGING; slot < SLOTS; slot++) {
            ItemStack wood = items.get(slot);
            if (FirewoodItem.catches(wood) && wood.getItem() instanceof FirewoodItem firewood) {
                burnHours = firewood.hours() / Balance.get().smoker().burnRate();
                wood.shrink(1);
                return true;
            }
        }
        return false;
    }

    public void advance(double hours) {
        Balance.Smoker smoker = Balance.get().smoker();
        double left = hours;
        while (left > SPENT) {
            if (burnHours <= 0 && !kindle()) {
                smokeHours = 0;
                break;
            }
            double spent = Math.min(left, burnHours);
            burnHours -= spent;
            double warming = Math.max(0, Math.min(spent, smoker.risingHours() - smokeHours));
            smokeHours = Math.min(smoker.risingHours(), smokeHours + spent);
            if (hanging()) {
                batchHours += spent - warming;
                if (batchHours >= smoker.batchHours()) {
                    finish();
                }
            } else {
                batchHours = 0;
            }
            left -= spent;
            if (burnHours <= SPENT) {
                burnHours = 0;
            }
        }
        setChanged();
    }

    private void finish() {
        batchHours = 0;
        for (int slot = 0; slot < HANGING; slot++) {
            ItemStack hung = items.get(slot);
            if (hung.isEmpty()) {
                continue;
            }
            ItemStack smoked = level == null ? hung.copy()
                    : level.getRecipeManager().getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(hung), level)
                            .map(recipe -> recipe.value().getResultItem(level.registryAccess()).copyWithCount(hung.getCount())).orElse(hung.copy());
            smoked.remove(Things.AGE.get());
            smoked.set(Things.SMOKED.get(), Unit.INSTANCE);
            items.set(slot, smoked);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SmokerEntity smoker) {
        long now = level.getDayTime();
        if (!smoker.awake) {
            smoker.awake = true;
            smoker.clock = now;
        }
        if ((level.getGameTime() + pos.asLong()) % STEP_TICKS != 0) {
            return;
        }
        double hours = Math.max(0, now - smoker.clock) / TICKS_AN_HOUR;
        smoker.clock = now;
        smoker.advance(hours);
        boolean lit = smoker.burnHours > 0;
        if (state.getValue(SmokerBlock.LIT) != lit) {
            level.setBlockAndUpdate(pos, state.setValue(SmokerBlock.LIT, lit));
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.backcountry.smoker");
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
        return new SmokerMenu(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < HANGING ? hangs(stack) : FirewoodItem.catches(stack);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putDouble("burn_hours", burnHours);
        tag.putDouble("smoke_hours", smokeHours);
        tag.putDouble("batch_hours", batchHours);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        burnHours = tag.getDouble("burn_hours");
        smokeHours = tag.getDouble("smoke_hours");
        batchHours = tag.getDouble("batch_hours");
    }
}
