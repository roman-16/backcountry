package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Things;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CarcassEntity extends BlockEntity {
    private EntityType<?> animal = EntityType.RABBIT;
    private final List<ItemStack> arrows = new ArrayList<>();
    private int dressed;

    public CarcassEntity(BlockPos pos, BlockState state) {
        super(Things.CARCASS_ENTITY.get(), pos, state);
    }

    public void hold(EntityType<?> type, List<ItemStack> embedded) {
        animal = type;
        arrows.clear();
        arrows.addAll(embedded);
        dressed = 0;
        setChanged();
    }

    public EntityType<?> animal() {
        return animal;
    }

    public List<ItemStack> arrows() {
        return arrows;
    }

    public Optional<Animal> data() {
        return Animal.of(animal);
    }

    public int dress() {
        dressed++;
        setChanged();
        return dressed;
    }

    public ItemStack item() {
        ItemStack stack = new ItemStack(Things.CARCASS_ITEM.get());
        stack.set(Things.ANIMAL_TYPE.get(), BuiltInRegistries.ENTITY_TYPE.getKey(animal));
        stack.set(Things.ARROWS.get(), List.copyOf(arrows));
        return stack;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("animal", BuiltInRegistries.ENTITY_TYPE.getKey(animal).toString());
        ListTag list = new ListTag();
        arrows.forEach(arrow -> list.add(arrow.save(provider)));
        tag.put("arrows", list);
        tag.putInt("dressed", dressed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        animal = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(tag.getString("animal")));
        arrows.clear();
        for (Tag arrow : tag.getList("arrows", Tag.TAG_COMPOUND)) {
            ItemStack.parse(provider, arrow).ifPresent(arrows::add);
        }
        dressed = tag.getInt("dressed");
    }
}
