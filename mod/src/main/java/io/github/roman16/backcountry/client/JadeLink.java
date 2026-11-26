package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Inspect;
import io.github.roman16.backcountry.fire.FireBlock;
import io.github.roman16.backcountry.gear.CampPotBlock;
import io.github.roman16.backcountry.gear.CampPotEntity;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.gear.WaterVesselItem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;

@WailaPlugin
public final class JadeLink implements IWailaPlugin {
    private static final int LAST = Integer.MAX_VALUE;
    private static final int TICKS_A_SECOND = 20;
    private static final int BELOW_BAR = 2;

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Inspect.POT_DATA, CampPotEntity.class);
        registration.registerBlockDataProvider(Inspect.CAMPFIRE_DATA, CampfireBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new Lines(Inspect.POT, JadeLink::pot), CampPotBlock.class);
        registration.registerBlockComponent(new Lines(Inspect.CAMPFIRE, JadeLink::campfire), FireBlock.class);
        registration.addBeforeRenderCallback((box, rect, graphics, accessor) -> {
            if (Minecraft.getInstance().player == null || !Minecraft.getInstance().player.isCrouching()) {
                return true;
            }
            rect.rect.setY(Math.max(rect.rect.getY(), LocatorBar.bottom() + BELOW_BAR));
            return false;
        });
        registration.addTooltipCollectedCallback(LAST, (box, accessor) -> {
            ITooltip tooltip = box.getTooltip();
            List<IElement> name = List.copyOf(tooltip.get(JadeIds.CORE_OBJECT_NAME));
            List<IElement> state = List.of(tooltip.get(Inspect.POT), tooltip.get(Inspect.CAMPFIRE)).stream().flatMap(List::stream).toList();
            List<IElement> mod = List.copyOf(tooltip.get(JadeIds.CORE_MOD_NAME));
            tooltip.clear();
            tooltip.add(name);
            state.forEach(tooltip::add);
            tooltip.add(mod);
            box.setIcon(null);
        });
    }

    private static void pot(ITooltip tooltip, CompoundTag data) {
        Water water = Water.CODEC.parse(NbtOps.INSTANCE, data.get(Inspect.WATER)).result().orElse(Water.EMPTY);
        if (water.empty()) {
            tooltip.add(Component.translatable("jade.backcountry.pot.empty"));
            return;
        }
        Balance balance = Balance.get();
        int ticks = data.getInt(Inspect.BOIL_TICKS);
        boolean lit = data.getBoolean(Inspect.LIT);
        boolean needles = data.getBoolean(Inspect.NEEDLES);
        if (water.drink() == Water.Drink.BIRCH_SAP) {
            String left = seconds(balance.forage().syrupSeconds() * TICKS_A_SECOND - ticks);
            tooltip.add(lit ? Component.translatable("jade.backcountry.pot.syrup", left)
                    : ticks > 0 ? Component.translatable("jade.backcountry.pot.fire_out", left) : WaterVesselItem.describe(water));
            return;
        }
        String left = seconds(balance.water().boilSeconds() * TICKS_A_SECOND - ticks);
        if (needles) {
            tooltip.add(lit ? Component.translatable("jade.backcountry.pot.steeping", left) : Component.translatable("jade.backcountry.pot.fire_out", left));
        } else if (water.boiled()) {
            tooltip.add(water.drink() == Water.Drink.WATER ? Component.translatable("jade.backcountry.pot.boiled", WaterVesselItem.litres(water))
                    : WaterVesselItem.describe(water));
        } else {
            tooltip.add(lit ? Component.translatable("jade.backcountry.pot.boiling", left)
                    : ticks > 0 ? Component.translatable("jade.backcountry.pot.fire_out", left) : WaterVesselItem.describe(water));
        }
    }

    private static void campfire(ITooltip tooltip, CompoundTag data) {
        tooltip.add(FireScreen.times(data.getBoolean(Inspect.LIT), data.getInt(Inspect.BURNING), data.getInt(Inspect.QUEUED), data.getInt(Inspect.COALS)));
        for (Tag piece : data.getList(Inspect.COOKING, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) piece;
            Component item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getString(Inspect.ITEM))).getDescription();
            tooltip.add(Component.translatable("jade.backcountry.cooking", item, seconds(entry.getInt(Inspect.TICKS_LEFT))));
        }
    }

    private static String seconds(int ticks) {
        return Words.seconds((Math.max(0, ticks) + TICKS_A_SECOND - 1) / TICKS_A_SECOND);
    }

    private interface Describe {
        void lines(ITooltip tooltip, CompoundTag data);
    }

    private record Lines(ResourceLocation uid, Describe describe) implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            describe.lines(tooltip, accessor.getServerData());
        }

        @Override
        public ResourceLocation getUid() {
            return uid;
        }
    }
}
