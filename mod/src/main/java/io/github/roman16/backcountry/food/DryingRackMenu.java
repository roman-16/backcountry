package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.BlockMenu;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.guide.Deed;
import io.github.roman16.backcountry.guide.Guide;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class DryingRackMenu extends BlockMenu {
    public static final int HANGING_X = 62;
    public static final int HANGING_Y = 20;
    private static final int PERCENT = 100;
    private static final int TENTHS = 10;
    private static final int PER_SLOT = 3;
    private static final int RAINED = DryingRackEntity.SLOTS * PER_SLOT;
    private static final int VALUES = RAINED + 1;
    private final ContainerData data;

    public DryingRackMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(DryingRackEntity.SLOTS), new SimpleContainerData(VALUES));
        buffer.readBlockPos();
    }

    public DryingRackMenu(int id, Inventory inventory, DryingRackEntity rack) {
        this(id, inventory, rack, new ContainerData() {
            @Override
            public int get(int index) {
                if (index == RAINED) {
                    return rack.rainedOn() ? 1 : 0;
                }
                int slot = index / PER_SLOT;
                return switch (index % PER_SLOT) {
                    case 0 -> (int) Math.round(rack.driedShare(slot) * PERCENT);
                    case 1 -> (int) Math.round(rack.daysToDry(slot) * TENTHS);
                    default -> rack.spoilsFirst(slot) ? 1 : 0;
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

    private DryingRackMenu(int id, Inventory inventory, Container rack, ContainerData data) {
        super(Things.DRYING_RACK_MENU.get(), id, rack);
        this.data = data;
        for (int slot = 0; slot < DryingRackEntity.SLOTS; slot++) {
            addSlot(new StoreSlot(rack, slot, HANGING_X + slot * SLOT, HANGING_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return DryingRackEntity.hangs(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public void onTake(Player player, ItemStack taken) {
                    if (player instanceof ServerPlayer survivor && taken.has(Things.DRIED.get()) && Fish.is(taken.getItem())) {
                        Guide.did(survivor, Deed.DRIED_FISH);
                    }
                    super.onTake(player, taken);
                }
            });
        }
        inventory(inventory);
        addDataSlots(data);
    }

    public Component line(int slot) {
        ItemStack piece = slots.get(slot).getItem();
        return DryingRackEntity.line(piece, data.get(slot * PER_SLOT), data.get(slot * PER_SLOT + 1) / (double) TENTHS, data.get(slot * PER_SLOT + 2) == 1);
    }

    public boolean rained() {
        return data.get(RAINED) == 1;
    }
}
