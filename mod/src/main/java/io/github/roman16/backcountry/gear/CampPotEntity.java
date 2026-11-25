package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CampPotEntity extends BlockEntity {
    private static final int STEAM_TICKS = 10;
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
        water = Water.of(false, water.litres());
        setChanged();
        return true;
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
        BlockState fire = level.getBlockState(pos.below());
        boolean syrup = pot.water.drink() == Water.Drink.BIRCH_SAP;
        if (pot.water.empty() || pot.water.boiled() && !syrup || !CampfireBlock.isLitCampfire(fire)) {
            return;
        }
        pot.boilTicks++;
        if (pot.boilTicks % STEAM_TICKS == 0 && level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.1, 0.05, 0.1, 0.01);
        }
        Balance balance = Balance.get();
        if (!pot.water.boiled() && pot.boilTicks >= balance.water().boilSeconds() * TICKS_A_SECOND) {
            pot.water = pot.needles ? new Water(Water.Drink.SPRUCE_TEA, true, pot.water.litres()) : pot.water.boil();
            pot.needles = false;
        }
        if (syrup && pot.boilTicks >= balance.forage().syrupSeconds() * TICKS_A_SECOND) {
            int made = (int) Math.floor(pot.water.litres() / balance.forage().syrupL() + 1e-6);
            if (made > 0) {
                Block.popResource(level, pos, new ItemStack(Things.BIRCH_SYRUP.get(), made));
            }
            pot.water = Water.EMPTY;
            pot.boilTicks = 0;
        }
        pot.setChanged();
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
