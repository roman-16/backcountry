package io.github.roman16.backcountry.haul;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class Canoe extends Boat {
    private static final float PADDLE = 0.04F;
    private boolean forward;

    public Canoe(EntityType<? extends Boat> type, Level level) {
        super(type, level);
        setVariant(Boat.Type.BIRCH);
    }

    @Override
    public Item getDropItem() {
        return Things.CANOE.get();
    }

    @Override
    public void setInput(boolean left, boolean right, boolean up, boolean down) {
        super.setInput(left, right, up, down);
        forward = up && !down;
    }

    @Override
    public void tick() {
        super.tick();
        if (forward && isControlledByLocalInstance() && afloat()) {
            float yaw = getYRot() * Mth.DEG_TO_RAD;
            double push = PADDLE * Balance.get().canoe().speedShare();
            setDeltaMovement(getDeltaMovement().add(-Mth.sin(yaw) * push, 0, Mth.cos(yaw) * push));
        }
    }

    private boolean afloat() {
        return level().getFluidState(BlockPos.containing(getX(), getY() - 0.125, getZ())).is(FluidTags.WATER)
                || level().getFluidState(blockPosition()).is(FluidTags.WATER);
    }
}
