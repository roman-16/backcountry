package io.github.roman16.backcountry.haul;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Load;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class Sled extends ChestBoat {
    private static final double PULL_REACH = 12;

    public Sled(EntityType<? extends Boat> type, Level level) {
        super(type, level);
    }

    @Override
    public Item getDropItem() {
        return Things.SLED.get();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    public float getGroundFriction() {
        Balance.Sled sled = Balance.get().sled();
        return (float) (gliding() ? sled.snowFriction() : sled.groundFriction());
    }

    public boolean gliding() {
        BlockPos at = blockPosition();
        BlockState here = level().getBlockState(at);
        BlockState below = level().getBlockState(at.below());
        return here.getBlock() instanceof SnowLayerBlock || below.is(Blocks.SNOW_BLOCK) || below.is(Blocks.POWDER_SNOW) || below.getBlock() instanceof SnowLayerBlock
                || below.is(BlockTags.ICE);
    }

    public double contentsKg() {
        double kg = 0;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            kg += Load.kg(getItem(slot));
        }
        return kg;
    }

    public boolean load(ItemStack stack) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (getItem(slot).isEmpty()) {
                setItem(slot, stack);
                return true;
            }
        }
        return false;
    }

    public static List<Sled> pulledBy(ServerPlayer player) {
        return player.level().getEntitiesOfClass(Sled.class, player.getBoundingBox().inflate(PULL_REACH), sled -> sled.getLeashHolder() == player);
    }

    public static double pulledKg(ServerPlayer player) {
        Balance.Sled rules = Balance.get().sled();
        return pulledBy(player).stream().mapToDouble(sled -> (rules.selfKg() + sled.contentsKg()) * (sled.gliding() ? rules.snowDrag() : rules.groundDrag())).sum();
    }
}
