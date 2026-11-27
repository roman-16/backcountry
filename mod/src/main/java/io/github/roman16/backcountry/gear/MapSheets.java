package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import io.github.roman16.backcountry.guide.Deed;
import io.github.roman16.backcountry.guide.Guide;
import io.github.roman16.backcountry.net.Messages;
import io.github.roman16.backcountry.weather.Weather;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public final class MapSheets extends SavedData {
    public static final int SIZE = 128;
    private static final String NAME = "backcountry_maps";
    private static final double TICKS_A_MINUTE = 1000.0 / 60;
    private static final int SIGHT_STEP = 4;
    private static final int SIGHT_CLEAR = 8;

    private final Map<Integer, Sheet> sheets = new HashMap<>();
    private int next = 1;

    public record Sheet(int centreX, int centreZ, byte[] colours, List<Long> crosses) {}

    public static MapSheets get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(MapSheets::new, MapSheets::load, null), NAME);
    }

    public static int surveyTicks() {
        return (int) Math.round(Balance.get().survey().minutes() * TICKS_A_MINUTE);
    }

    public static Optional<Component> cannotSurvey(Player player) {
        Level level = player.level();
        if (!level.isDay()) {
            return Optional.of(Component.translatable("message.backcountry.survey.dark"));
        }
        if (Weather.day(level, Calendar.day(level)).falling(Climate.hour(level))) {
            return Optional.of(Component.translatable("message.backcountry.survey.weather"));
        }
        if (!player.isCreative() && Hotbar.count(player, MapSheets::charcoal) == 0) {
            return Optional.of(Component.translatable("message.backcountry.survey.charcoal"));
        }
        return Optional.empty();
    }

    private static boolean charcoal(ItemStack stack) {
        return stack.is(Items.CHARCOAL);
    }

    public static void survey(ServerPlayer player, ItemStack map) {
        Optional<Component> refused = cannotSurvey(player);
        if (refused.isPresent()) {
            player.displayClientMessage(refused.get(), true);
            return;
        }
        if (!player.isCreative()) {
            Hotbar.take(player, MapSheets::charcoal, 1);
        }
        ServerLevel level = player.serverLevel();
        MapSheets book = get(level);
        Balance.Survey rules = Balance.get().survey();
        Integer id = map.get(Things.MAP_SHEET.get());
        Sheet sheet = id == null ? null : book.sheets.get(id);
        if (sheet == null) {
            int step = rules.blocksAPixel();
            sheet = new Sheet(Math.floorDiv(player.getBlockX(), step) * step, Math.floorDiv(player.getBlockZ(), step) * step, new byte[SIZE * SIZE], new ArrayList<>());
            id = book.next++;
            book.sheets.put(id, sheet);
            map.set(Things.MAP_SHEET.get(), id);
        }
        draw(level, player.getEyePosition(), sheet);
        sheet.crosses().add(BlockPos.asLong(player.getBlockX(), 0, player.getBlockZ()));
        book.setDirty();
        player.displayClientMessage(Component.translatable("message.backcountry.survey.done"), true);
        Guide.did(player, Deed.SURVEYED);
        show(player, map);
    }

    private static void draw(ServerLevel level, Vec3 eye, Sheet sheet) {
        Balance.Survey rules = Balance.get().survey();
        int step = rules.blocksAPixel();
        double reach = Math.min(rules.mostBlocks(), rules.baseBlocks() + Math.max(0, eye.y - Balance.get().climate().seaLevel()) * rules.blocksPerHeight());
        for (int pz = 0; pz < SIZE; pz++) {
            for (int px = 0; px < SIZE; px++) {
                int x = sheet.centreX() + (px - SIZE / 2) * step + step / 2;
                int z = sheet.centreZ() + (pz - SIZE / 2) * step + step / 2;
                double dx = x - eye.x;
                double dz = z - eye.z;
                if (dx * dx + dz * dz > reach * reach || !loaded(level, x, z)) {
                    continue;
                }
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (!seen(level, eye, x, height, z)) {
                    continue;
                }
                BlockPos top = new BlockPos(x, height - 1, z);
                MapColor colour = level.getBlockState(top).getMapColor(level, top);
                if (colour == MapColor.NONE) {
                    continue;
                }
                int north = loaded(level, x, z - step) ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z - step) : height;
                MapColor.Brightness brightness = colour == MapColor.WATER ? MapColor.Brightness.NORMAL
                        : height > north ? MapColor.Brightness.HIGH : height < north ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                sheet.colours()[px + pz * SIZE] = colour.getPackedId(brightness);
            }
        }
    }

    private static boolean seen(ServerLevel level, Vec3 eye, int x, int height, int z) {
        Vec3 target = new Vec3(x + 0.5, height + 0.5, z + 0.5);
        double distance = Math.sqrt((target.x - eye.x) * (target.x - eye.x) + (target.z - eye.z) * (target.z - eye.z));
        for (double along = SIGHT_CLEAR; along < distance - SIGHT_CLEAR; along += SIGHT_STEP) {
            double share = along / distance;
            double px = eye.x + (target.x - eye.x) * share;
            double py = eye.y + (target.y - eye.y) * share;
            double pz = eye.z + (target.z - eye.z) * share;
            BlockPos at = BlockPos.containing(px, 0, pz);
            if (!loaded(level, at.getX(), at.getZ()) || level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ()) > py) {
                return false;
            }
        }
        return true;
    }

    private static boolean loaded(ServerLevel level, int x, int z) {
        return level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
    }

    public static void show(ServerPlayer player, ItemStack map) {
        Integer id = map.get(Things.MAP_SHEET.get());
        Sheet sheet = id == null ? null : get(player.serverLevel()).sheets.get(id);
        Messages.map(player, sheet == null ? new Sheet(0, 0, new byte[0], List.of()) : sheet);
    }

    public static int surveys(ServerLevel level, ItemStack map) {
        Integer id = map.get(Things.MAP_SHEET.get());
        Sheet sheet = id == null ? null : get(level).sheets.get(id);
        return sheet == null ? 0 : sheet.crosses().size();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("next", next);
        CompoundTag all = new CompoundTag();
        sheets.forEach((id, sheet) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", sheet.centreX());
            entry.putInt("z", sheet.centreZ());
            entry.putByteArray("colours", sheet.colours());
            entry.put("crosses", new LongArrayTag(sheet.crosses()));
            all.put(String.valueOf(id), entry);
        });
        tag.put("sheets", all);
        return tag;
    }

    private static MapSheets load(CompoundTag tag, HolderLookup.Provider provider) {
        MapSheets book = new MapSheets();
        book.next = Math.max(1, tag.getInt("next"));
        CompoundTag all = tag.getCompound("sheets");
        for (String id : all.getAllKeys()) {
            CompoundTag entry = all.getCompound(id);
            List<Long> crosses = new ArrayList<>();
            for (long cross : entry.getLongArray("crosses")) {
                crosses.add(cross);
            }
            byte[] colours = entry.getByteArray("colours");
            book.sheets.put(Integer.parseInt(id), new Sheet(entry.getInt("x"), entry.getInt("z"), colours, crosses));
        }
        return book;
    }
}
