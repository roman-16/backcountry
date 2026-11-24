package io.github.roman16.backcountry.food;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class Farmland extends SavedData {
    private static final String NAME = "backcountry_farmland";
    private final LongSet duds = new LongOpenHashSet();

    public static Farmland get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Farmland::new, Farmland::load, null), NAME);
    }

    private static Farmland load(CompoundTag tag, HolderLookup.Provider provider) {
        Farmland farmland = new Farmland();
        for (long pos : tag.getLongArray("duds")) {
            farmland.duds.add(pos);
        }
        return farmland;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("duds", new LongArrayTag(duds.toLongArray()));
        return tag;
    }

    public boolean dud(BlockPos pos) {
        return duds.contains(pos.asLong());
    }

    public void sow(BlockPos pos) {
        duds.add(pos.asLong());
        setDirty();
    }

    public void forget(BlockPos pos) {
        if (duds.remove(pos.asLong())) {
            setDirty();
        }
    }
}
