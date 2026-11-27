package io.github.roman16.backcountry.guide;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Cuts;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Spirits;
import io.github.roman16.backcountry.food.BerryBushBlock;
import io.github.roman16.backcountry.food.Fish;
import io.github.roman16.backcountry.hunt.Animal;
import io.github.roman16.backcountry.net.Messages;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class FieldNotes extends SavedData {
    private static final String NAME = "backcountry_field_notes";
    private static final String SICKENING = "sickening";
    public static final List<String> PLANTS = List.of("backcountry:salmonberries", "backcountry:blueberries", "minecraft:sweet_berries", "backcountry:highbush_cranberries",
            "backcountry:baneberries", "minecraft:brown_mushroom", "minecraft:red_mushroom", "projectvibrantjourneys:bark_mushroom");

    private final Map<UUID, Map<String, Note>> notes = new HashMap<>();
    private final Map<UUID, Set<String>> sickening = new HashMap<>();

    public record Note(String id, int day, String biome) {
        public static final StreamCodec<ByteBuf, Note> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Note::id, ByteBufCodecs.VAR_INT, Note::day,
                ByteBufCodecs.STRING_UTF8, Note::biome, Note::new);
    }

    public enum Kind {
        ANIMAL,
        FISH,
        PLANT;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static FieldNotes get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(FieldNotes::new, FieldNotes::load, null), NAME);
    }

    public static String animal(EntityType<?> type) {
        return Kind.ANIMAL.id() + ":" + BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    public static String item(Kind kind, Item item) {
        return kind.id() + ":" + BuiltInRegistries.ITEM.getKey(item);
    }

    public static Kind kind(String id) {
        return Kind.valueOf(id.substring(0, id.indexOf(':')).toUpperCase(Locale.ROOT));
    }

    public static ResourceLocation subject(String id) {
        return ResourceLocation.parse(id.substring(id.indexOf(':') + 1));
    }

    public static List<String> catalogue() {
        List<String> all = new ArrayList<>();
        for (Map.Entry<ResourceKey<EntityType<?>>, Animal> entry : BuiltInRegistries.ENTITY_TYPE.getDataMap(Things.ANIMAL).entrySet()) {
            if (!Cuts.cut(BuiltInRegistries.ENTITY_TYPE.get(entry.getKey()))) {
                all.add(Kind.ANIMAL.id() + ":" + entry.getKey().location());
            }
        }
        Cuts.fish().forEach(fish -> all.add(item(Kind.FISH, fish.getItem())));
        PLANTS.stream().filter(id -> BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id))).forEach(id -> all.add(Kind.PLANT.id() + ":" + id));
        all.sort(null);
        return all;
    }

    public static Optional<String> plant(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return PLANTS.contains(id) ? Optional.of(Kind.PLANT.id() + ":" + id) : Optional.empty();
    }

    public static Optional<String> plant(Block block) {
        Item item = block.asItem();
        if (block instanceof SweetBerryBushBlock && !(block instanceof BerryBushBlock)) {
            item = Items.SWEET_BERRIES;
        } else if (block instanceof BerryBushBlock bush) {
            item = bush.berry().berries();
        }
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        return PLANTS.contains(id) ? Optional.of(Kind.PLANT.id() + ":" + id) : Optional.empty();
    }

    public static void note(ServerPlayer player, String id) {
        if (!Metabolism.lives(player) || !catalogue().contains(id)) {
            return;
        }
        FieldNotes book = get(player.server);
        Map<String, Note> own = book.notes.computeIfAbsent(player.getUUID(), uuid -> new TreeMap<>());
        if (own.containsKey(id)) {
            return;
        }
        String biome = player.level().getBiome(player.blockPosition()).unwrapKey().map(ResourceKey::location).map(ResourceLocation::toString).orElse("");
        own.put(id, new Note(id, Calendar.day(player.level()), biome));
        book.setDirty();
        Spirits.add(Metabolism.body(player), Balance.get().spirits().newFood() / 2);
        player.displayClientMessage(Component.translatable("message.backcountry.noted", name(id)), true);
        book.send(player);
    }

    public static void note(ServerPlayer player, Entity entity) {
        note(player, animal(entity.getType()));
    }

    public static void caught(ServerPlayer player, ItemStack stack) {
        ResourceLocation animal = stack.get(Things.ANIMAL_TYPE.get());
        if (animal != null) {
            note(player, Kind.ANIMAL.id() + ":" + animal);
        } else if (Fish.is(stack.getItem())) {
            note(player, item(Kind.FISH, stack.getItem()));
        }
    }

    public static void studied(ServerPlayer player, int entity) {
        Entity seen = player.level().getEntity(entity);
        if (seen != null && seen.distanceTo(player) <= Balance.get().binoculars().studyBlocks() + 1) {
            note(player, seen);
        }
    }

    public static Component unknown(String id) {
        String path = subject(id).getPath();
        String what = switch (kind(id)) {
            case ANIMAL -> "animal";
            case FISH -> "fish";
            case PLANT -> path.contains("berr") ? "berries" : "mushroom";
        };
        return Component.translatable("jade.backcountry.unknown." + what);
    }

    public static Component name(String id) {
        return switch (kind(id)) {
            case ANIMAL -> BuiltInRegistries.ENTITY_TYPE.get(subject(id)).getDescription();
            case FISH, PLANT -> new ItemStack(BuiltInRegistries.ITEM.get(subject(id))).getHoverName();
        };
    }

    public static void sickened(ServerPlayer player, Item food) {
        FieldNotes book = get(player.server);
        if (book.sickening.computeIfAbsent(player.getUUID(), uuid -> new TreeSet<>()).add(BuiltInRegistries.ITEM.getKey(food).toString())) {
            book.setDirty();
            book.send(player);
        }
    }

    private void send(ServerPlayer player) {
        Messages.notes(player, of(player), List.copyOf(sickening(player)));
    }

    public Set<String> sickening(ServerPlayer player) {
        return sickening.getOrDefault(player.getUUID(), Set.of());
    }

    public List<Note> of(ServerPlayer player) {
        return List.copyOf(notes.getOrDefault(player.getUUID(), Map.of()).values());
    }

    @SubscribeEvent
    static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            get(player.server).send(player);
        }
    }

    @SubscribeEvent
    static void picked(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            plant(event.getOriginalStack()).ifPresent(id -> note(player, id));
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        notes.forEach((uuid, own) -> {
            CompoundTag player = new CompoundTag();
            own.forEach((id, note) -> {
                CompoundTag entry = new CompoundTag();
                entry.putInt("day", note.day());
                entry.putString("biome", note.biome());
                player.put(id, entry);
            });
            tag.put(uuid.toString(), player);
        });
        CompoundTag sick = new CompoundTag();
        sickening.forEach((uuid, foods) -> {
            ListTag list = new ListTag();
            foods.forEach(food -> list.add(StringTag.valueOf(food)));
            sick.put(uuid.toString(), list);
        });
        tag.put(SICKENING, sick);
        return tag;
    }

    private static FieldNotes load(CompoundTag tag, HolderLookup.Provider provider) {
        FieldNotes book = new FieldNotes();
        CompoundTag sick = tag.getCompound(SICKENING);
        for (String uuid : sick.getAllKeys()) {
            Set<String> foods = new TreeSet<>();
            sick.getList(uuid, Tag.TAG_STRING).forEach(food -> foods.add(food.getAsString()));
            book.sickening.put(UUID.fromString(uuid), foods);
        }
        for (String uuid : tag.getAllKeys()) {
            if (uuid.equals(SICKENING)) {
                continue;
            }
            CompoundTag player = tag.getCompound(uuid);
            Map<String, Note> own = new TreeMap<>();
            for (String id : player.getAllKeys()) {
                CompoundTag entry = player.getCompound(id);
                own.put(id, new Note(id, entry.getInt("day"), entry.getString("biome")));
            }
            book.notes.put(UUID.fromString(uuid), own);
        }
        return book;
    }
}
