package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Things;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TrackEntity extends BlockEntity {
    private long laid;
    private long wear;
    private long checked;

    public TrackEntity(BlockPos pos, BlockState state) {
        super(Things.TRACK_ENTITY.get(), pos, state);
    }

    public void laid(long at) {
        laid = at;
        wear = 0;
        checked = now();
        setChanged();
    }

    public long age() {
        return Math.max(0, now() - laid) + wear;
    }

    public void weather(double wearShare) {
        long at = now();
        wear += (long) (Math.max(0, at - checked) * wearShare);
        checked = at;
        setChanged();
    }

    private long now() {
        return level == null ? laid : level.getDayTime();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("laid", laid);
        tag.putLong("wear", wear);
        tag.putLong("checked", checked);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        laid = tag.getLong("laid");
        wear = tag.getLong("wear");
        checked = tag.getLong("checked");
    }
}
