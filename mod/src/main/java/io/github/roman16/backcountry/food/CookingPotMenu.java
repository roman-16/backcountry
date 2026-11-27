package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.BlockMenu;
import io.github.roman16.backcountry.Things;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CookingPotMenu extends BlockMenu {
    public static final int FOODS_X = 62;
    public static final int FOODS_Y = 20;
    public static final int STEW_X = 152;
    public static final int STEW_Y = 44;
    private static final int TENTHS = 10;
    private static final double MINUTES_AN_HOUR = 60;
    private static final int WATER = 0;
    private static final int COOKED = 1;
    private static final int LIT = 2;
    private static final int VALUES = 3;
    private final ContainerData data;
    private final Level level;

    public CookingPotMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(CookingPotEntity.SLOTS), new SimpleContainerData(VALUES));
        buffer.readBlockPos();
    }

    public CookingPotMenu(int id, Inventory inventory, CookingPotEntity pot) {
        this(id, inventory, pot, new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case WATER -> (int) Math.round(pot.waterL() * TENTHS);
                    case COOKED -> (int) Math.round(pot.cookedHours() * MINUTES_AN_HOUR);
                    case LIT -> pot.lit() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return VALUES;
            }
        });
    }

    private CookingPotMenu(int id, Inventory inventory, Container pot, ContainerData data) {
        super(Things.COOKING_POT_MENU.get(), id, pot);
        this.data = data;
        this.level = inventory.player.level();
        for (int slot = 0; slot < CookingPotEntity.FOODS; slot++) {
            addSlot(new StoreSlot(pot, slot, FOODS_X + slot * SLOT, FOODS_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack(CookingPotEntity.STEW).isEmpty() && CookingPotEntity.cooks(level, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addSlot(new StoreSlot(pot, CookingPotEntity.STEW, STEW_X, STEW_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !CookingPotEntity.servedInBowls(getItem());
            }
        });
        inventory(inventory);
        addDataSlots(data);
    }

    private ItemStack stack(int slot) {
        return slots.get(slot).getItem();
    }

    public double waterL() {
        return data.get(WATER) / (double) TENTHS;
    }

    public Component status() {
        List<ItemStack> foods = slots.subList(0, CookingPotEntity.FOODS).stream().map(slot -> slot.getItem()).toList();
        return CookingPotEntity.status(level, foods, stack(CookingPotEntity.STEW), waterL(), data.get(COOKED) / MINUTES_AN_HOUR, data.get(LIT) == 1);
    }
}
