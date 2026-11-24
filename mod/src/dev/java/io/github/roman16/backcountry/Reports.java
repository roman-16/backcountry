package io.github.roman16.backcountry;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

final class Reports {
    private Reports() {}

    static int write(CommandSourceStack source, String folder) {
        Path root = Path.of(folder);
        var gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
                entries(root, registry.key().location(), registry.keySet(), gson);
            }
            source.getServer().registryAccess().registries().forEach(entry -> {
                try {
                    entries(root, entry.key().location(), entry.value().keySet(), gson);
                } catch (IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            });
            JsonArray foods = new JsonArray();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item.components().get(DataComponents.FOOD) != null) {
                    foods.add(BuiltInRegistries.ITEM.getKey(item).toString());
                }
            }
            Files.writeString(root.resolve("foods.json"), gson.toJson(foods));
        } catch (IOException | java.io.UncheckedIOException exception) {
            source.sendFailure(Component.literal("registries failed: " + exception.getMessage()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("registries written to " + root.toAbsolutePath()), false);
        return 1;
    }

    private static void entries(Path root, ResourceLocation id, java.util.Set<ResourceLocation> keys, com.google.gson.Gson gson) throws IOException {
        Path file = root.resolve("registries").resolve(id.getNamespace()).resolve(id.getPath() + ".json");
        Files.createDirectories(file.getParent());
        JsonObject list = new JsonObject();
        JsonArray names = new JsonArray();
        keys.stream().map(ResourceLocation::toString).sorted().forEach(names::add);
        list.add("entries", names);
        Files.writeString(file, gson.toJson(list));
    }
}
