package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CampPotEntity extends BlockEntity {
    private static final int TICKS_A_SECOND = 20;
    private Water water = Water.EMPTY;
    private int boilTicks;
    private boolean needles;
    private boolean pickedUp;

    public CampPotEntity(BlockPos pos, BlockState state) {
        super(Things.CAMP_POT_ENTITY.get(), pos, state);
    }

    public Water water() {
        return water;
    }

    public void hold(Water held) {
        water = held;
        boilTicks = 0;
        if (water.empty()) {
            needles = false;
        }
        setChanged();
    }

    public boolean steep() {
        if (needles || water.empty() || water.drink() != Water.Drink.WATER) {
            return false;
        }
        needles = true;
        boilTicks = 0;
        setChanged();
        return true;
    }

    public boolean needles() {
        return needles;
    }

    public int boilTicks() {
        return boilTicks;
    }

    public boolean heating() {
        return !water.empty() && (!water.boiled() || needles || water.drink() == Water.Drink.BIRCH_SAP);
    }

    public ItemStack item() {
        ItemStack stack = new ItemStack(Things.CAMP_POT.get());
        stack.set(Things.WATER.get(), water);
        return stack;
    }

    public void pickedUp() {
        pickedUp = true;
    }

    public boolean isPickedUp() {
        return pickedUp;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CampPotEntity pot) {
        boolean lit = CampfireBlock.isLitCampfire(level.getBlockState(pos.below()));
        if (lit && pot.heating()) {
            pot.boil(level, pos);
        }
        BlockState shown = state.setValue(CampPotBlock.CONTENT, pot.water.empty() ? CampPotBlock.Content.EMPTY
                : !lit ? CampPotBlock.Content.WATER : pot.heating() ? CampPotBlock.Content.HEATING : CampPotBlock.Content.BOILING);
        if (shown != state) {
            level.setBlock(pos, shown, Block.UPDATE_CLIENTS);
        }
    }

    private void boil(Level level, BlockPos pos) {
        Balance balance = Balance.get();
        boilTicks++;
        if ((!water.boiled() || needles) && boilTicks >= balance.water().boilSeconds() * TICKS_A_SECOND) {
            water = needles ? new Water(Water.Drink.SPRUCE_TEA, true, water.source(), water.litres()) : water.boil();
            needles = false;
        }
        if (water.drink() == Water.Drink.BIRCH_SAP && boilTicks >= balance.forage().syrupSeconds() * TICKS_A_SECOND) {
            int made = (int) Math.floor(water.litres() / balance.forage().syrupL() + 1e-6);
            if (made > 0) {
                Block.popResource(level, pos, new ItemStack(Things.BIRCH_SYRUP.get(), made));
            }
            water = Water.EMPTY;
            boilTicks = 0;
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        Water.CODEC.encodeStart(NbtOps.INSTANCE, water).ifSuccess(encoded -> tag.put("water", encoded));
        tag.putInt("boil_ticks", boilTicks);
        tag.putBoolean("needles", needles);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        water = Water.CODEC.parse(NbtOps.INSTANCE, tag.get("water")).result().orElse(Water.EMPTY);
        boilTicks = tag.getInt("boil_ticks");
        needles = tag.getBoolean("needles");
    }
}
