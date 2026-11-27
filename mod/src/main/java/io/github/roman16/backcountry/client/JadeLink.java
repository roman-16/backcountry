package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Inspect;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.fire.FireBlock;
import io.github.roman16.backcountry.gear.CampPotBlock;
import io.github.roman16.backcountry.gear.CampPotEntity;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.gear.WaterVesselItem;
import io.github.roman16.backcountry.guide.FieldNotes;
import io.github.roman16.backcountry.hunt.CarcassBlock;
import io.github.roman16.backcountry.hunt.CarcassEntity;
import io.github.roman16.backcountry.hunt.TrackBlock;
import io.github.roman16.backcountry.hunt.TrackEntity;
import io.github.roman16.backcountry.hunt.Tracks;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

@WailaPlugin
public final class JadeLink implements IWailaPlugin {
    private static final int LAST = Integer.MAX_VALUE;
    private static final int TICKS_A_SECOND = 20;
    private static final int BELOW_BAR = 2;

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Inspect.POT_DATA, CampPotEntity.class);
        registration.registerBlockDataProvider(Inspect.CAMPFIRE_DATA, CampfireBlockEntity.class);
        registration.registerBlockDataProvider(Inspect.SIGN_DATA, TrackEntity.class);
        registration.registerBlockDataProvider(Inspect.CARCASS_DATA, CarcassEntity.class);
        registration.registerBlockDataProvider(Inspect.LINES_DATA, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new Lines(Inspect.POT, JadeLink::pot), CampPotBlock.class);
        registration.registerBlockComponent(new Lines(Inspect.CAMPFIRE, JadeLink::campfire), FireBlock.class);
        registration.registerBlockComponent(new Lines(Inspect.SIGN, JadeLink::sign), TrackBlock.class);
        registration.registerBlockComponent(new Lines(Inspect.CARCASS, JadeLink::carcass), CarcassBlock.class);
        registration.registerBlockComponent(new WaterName(), LiquidBlock.class);
        registration.registerBlockComponent(new Lines(Inspect.LINES, JadeLink::lines), Block.class);
        registration.addBeforeRenderCallback((box, rect, graphics, accessor) -> {
            if (Minecraft.getInstance().player == null || !Minecraft.getInstance().player.isCrouching()) {
                return true;
            }
            rect.rect.setY(Math.max(rect.rect.getY(), LocatorBar.bottom() + BELOW_BAR));
            return false;
        });
        registration.addTooltipCollectedCallback(LAST, (box, accessor) -> {
            ITooltip tooltip = box.getTooltip();
            boolean named = !(accessor instanceof BlockAccessor block && (block.getBlock() instanceof TrackBlock || block.getBlock() instanceof LiquidBlock));
            List<IElement> name = named ? unknown(accessor).<List<IElement>>map(unknown -> List.of(IElementHelper.get().text(unknown)))
                    .orElseGet(() -> List.copyOf(tooltip.get(JadeIds.CORE_OBJECT_NAME))) : List.of();
            List<IElement> state = List.of(Inspect.POT, Inspect.CAMPFIRE, Inspect.SIGN, Inspect.CARCASS, Inspect.WATER_KIND).stream().flatMap(id -> tooltip.get(id).stream()).toList();
            List<IElement> mod = List.copyOf(tooltip.get(JadeIds.CORE_MOD_NAME));
            tooltip.clear();
            tooltip.add(name);
            state.forEach(tooltip::add);
            tooltip.add(mod);
            box.setIcon(null);
        });
    }

    private static Optional<Component> unknown(Accessor<?> accessor) {
        Optional<String> subject = switch (accessor) {
            case EntityAccessor entity -> Optional.of(FieldNotes.animal(entity.getEntity().getType())).filter(FieldNotes.catalogue()::contains);
            case BlockAccessor block -> FieldNotes.plant(block.getBlock());
            default -> Optional.empty();
        };
        return subject.filter(id -> !ClientState.noted(id)).map(FieldNotes::unknown);
    }

    private static void pot(ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
        Water water = Water.CODEC.parse(NbtOps.INSTANCE, data.get(Inspect.WATER)).result().orElse(Water.EMPTY);
        if (water.empty()) {
            tooltip.add(Component.translatable("jade.backcountry.pot.empty"));
            return;
        }
        String left = seconds(data.getInt(Inspect.TICKS_LEFT));
        boolean started = data.getBoolean(Inspect.STARTED);
        boolean lit = data.getBoolean(Inspect.LIT);
        boolean needles = data.getBoolean(Inspect.NEEDLES);
        if (water.drink() == Water.Drink.SEA_WATER) {
            tooltip.add(lit ? Component.translatable("jade.backcountry.pot.salt", left)
                    : started ? Component.translatable("jade.backcountry.pot.fire_out", left) : WaterVesselItem.describe(water));
            return;
        }
        if (water.drink() == Water.Drink.BIRCH_SAP) {
            tooltip.add(lit ? Component.translatable("jade.backcountry.pot.syrup", left)
                    : started ? Component.translatable("jade.backcountry.pot.fire_out", left) : WaterVesselItem.describe(water));
            return;
        }
        if (needles) {
            tooltip.add(lit ? Component.translatable("jade.backcountry.pot.steeping", left) : Component.translatable("jade.backcountry.pot.fire_out", left));
        } else if (water.boiled()) {
            tooltip.add(water.drink() == Water.Drink.WATER ? Component.translatable("jade.backcountry.pot.boiled", WaterVesselItem.litres(water))
                    : WaterVesselItem.describe(water));
        } else {
            tooltip.add(lit ? Component.translatable("jade.backcountry.pot.boiling", left)
                    : started ? Component.translatable("jade.backcountry.pot.fire_out", left) : WaterVesselItem.describe(water));
        }
    }

    private static void campfire(ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
        tooltip.add(FireScreen.times(data.getBoolean(Inspect.LIT), data.getInt(Inspect.BURNING), data.getInt(Inspect.QUEUED), data.getInt(Inspect.COALS)));
        for (Tag piece : data.getList(Inspect.COOKING, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) piece;
            Component item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getString(Inspect.ITEM))).getDescription();
            tooltip.add(Component.translatable("jade.backcountry.cooking", item, seconds(entry.getInt(Inspect.TICKS_LEFT))));
        }
    }

    private static void sign(ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
        BlockState state = accessor.getBlockState();
        Component sign = state.getValue(TrackBlock.KIND).sign(((TrackBlock) state.getBlock()).droppings());
        tooltip.add(Component.translatable("jade.backcountry.sign", sign, Component.translatable(Tracks.Age.of(data.getLong(Inspect.AGE)).key())));
    }

    private static void carcass(ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
        tooltip.add(Component.translatable("jade.backcountry.carcass", Words.lifetime(data.getLong(Inspect.TICKS_LEFT))));
    }

    private static String seconds(int ticks) {
        return Words.seconds((Math.max(0, ticks) + TICKS_A_SECOND - 1) / TICKS_A_SECOND);
    }

    private interface Describe {
        void lines(ITooltip tooltip, BlockAccessor accessor, CompoundTag data);
    }

    private static final class WaterName implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!accessor.getLevel().getFluidState(accessor.getPosition()).is(FluidTags.WATER)) {
                return;
            }
            boolean salt = accessor.getLevel().getBiome(accessor.getPosition()).is(Things.SALT_WATER);
            boolean moving = Water.Source.at(accessor.getLevel(), accessor.getPosition()) == Water.Source.STREAM;
            tooltip.add(Component.translatable(salt ? "jade.backcountry.water.sea" : moving ? "jade.backcountry.water.stream" : "jade.backcountry.water.lake"));
        }

        @Override
        public ResourceLocation getUid() {
            return Inspect.WATER_KIND;
        }
    }

    private static void lines(ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
        ListTag lines = data.getList(Inspect.TEXT, Tag.TAG_STRING);
        for (int i = 0; i < lines.size(); i++) {
            Component line = Component.Serializer.fromJson(lines.getString(i), accessor.getLevel().registryAccess());
            if (line != null) {
                tooltip.add(line);
            }
        }
    }

    private record Lines(ResourceLocation uid, Describe describe) implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            describe.lines(tooltip, accessor, accessor.getServerData());
        }

        @Override
        public ResourceLocation getUid() {
            return uid;
        }
    }
}
