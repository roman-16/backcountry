package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Balance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class HuntArrowItem extends ArrowItem {
    public enum Head {
        BLUNT,
        BONE,
        BROADHEAD
    }

    private final Head head;

    public HuntArrowItem(Properties properties, Head head) {
        super(properties);
        this.head = head;
    }

    public boolean blunt() {
        return head == Head.BLUNT;
    }

    public double fromBlocks() {
        Balance.Arrows arrows = Balance.get().arrows();
        return switch (head) {
            case BLUNT -> arrows.bluntBlocks();
            case BONE -> arrows.boneBlocks();
            case BROADHEAD -> arrows.broadheadBlocks();
        };
    }

    public double fromAnimals() {
        Balance.Arrows arrows = Balance.get().arrows();
        return switch (head) {
            case BLUNT -> arrows.bluntAnimals();
            case BONE -> arrows.boneAnimals();
            case BROADHEAD -> arrows.broadheadAnimals();
        };
    }

    private double damage() {
        Balance.Arrows arrows = Balance.get().arrows();
        return switch (head) {
            case BLUNT -> arrows.bluntDamage();
            case BONE -> arrows.boneDamage();
            case BROADHEAD -> arrows.broadheadDamage();
        };
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, ItemStack weapon) {
        Arrow arrow = new Arrow(level, shooter, ammo.copyWithCount(1), weapon);
        arrow.setBaseDamage(damage());
        return arrow;
    }
}
