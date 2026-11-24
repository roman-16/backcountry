package io.github.roman16.backcountry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.roman16.backcountry.body.Alert;
import io.github.roman16.backcountry.body.BodyView;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import io.github.roman16.backcountry.body.Condition;
import io.github.roman16.backcountry.client.ClientState;
import io.github.roman16.backcountry.mixin.dev.GuiAccessor;
import io.github.roman16.backcountry.weather.Outlook;
import io.github.roman16.backcountry.weather.WeatherView;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Backcountry.MODID, value = Dist.CLIENT)
public final class Driver {
    private static final Gson GSON = new Gson();
    private static final int STEP_TICKS = 1200;
    private static final int DAY_START_HOUR = 6;
    private static final int HOURS = 24;
    private static final String FRESH = "play";
    private static final Map<String, Integer> KEYS = Map.of("enter", GLFW.GLFW_KEY_ENTER, "escape", GLFW.GLFW_KEY_ESCAPE, "left", GLFW.GLFW_KEY_LEFT, "right",
            GLFW.GLFW_KEY_RIGHT, "space", GLFW.GLFW_KEY_SPACE, "tab", GLFW.GLFW_KEY_TAB);
    private static final double ARRIVED_BLOCKS = 1.5;
    private static final float FULL_TURN = 360;
    private static final int TICKS_A_SECOND = 20;
    private static final Deque<String> STEPS = new ArrayDeque<>();
    private static final List<String> HEARD = new ArrayList<>();
    private static boolean opened;
    private static boolean announced;
    private static boolean running;
    private static String step;
    private static JsonObject result;
    private static BooleanSupplier until = () -> true;
    private static Runnable each = () -> {};
    private static int ticks;
    private static int waited;
    private static long frames;
    private static int marks;
    private static long logRead;
    private static final StringBuilder OUT = new StringBuilder();

    private Driver() {}

    private static Path folder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("driver");
    }

    @SubscribeEvent
    static void heard(ClientChatReceivedEvent event) {
        String text = event.getMessage().getString();
        if (text.startsWith(DevCommands.MARK)) {
            marks = Integer.parseInt(text.substring(DevCommands.MARK.length()).trim());
            event.setCanceled(true);
            return;
        }
        if (running) {
            HEARD.add(text);
        }
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Path folder = folder();
        try {
            Files.createDirectories(folder);
            if (!Files.exists(folder.resolve("game.pid"))) {
                Files.writeString(folder.resolve("game.pid"), Long.toString(ProcessHandle.current().pid()));
            }
            if (Files.deleteIfExists(folder.resolve("quit"))) {
                minecraft.stop();
                return;
            }
            open(minecraft, folder);
            if (!running && Files.exists(folder.resolve("commands.txt"))) {
                STEPS.addAll(Files.readAllLines(folder.resolve("commands.txt"), StandardCharsets.UTF_8).stream().filter(line -> !line.isBlank()).toList());
                Files.delete(folder.resolve("commands.txt"));
                OUT.setLength(0);
                running = true;
                step = null;
            }
            if (running) {
                advance(minecraft, folder);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void open(Minecraft minecraft, Path folder) throws IOException {
        if (!opened && minecraft.screen instanceof TitleScreen) {
            opened = true;
            String world = System.getProperty(Backcountry.MODID + ".world", "");
            String seed = System.getProperty(Backcountry.MODID + ".seed", "");
            if (!world.isEmpty() && !world.equals(FRESH)) {
                minecraft.createWorldOpenFlows().openWorld(world, () -> minecraft.setScreen(new TitleScreen()));
            } else if (world.equals(FRESH)) {
                LevelSettings settings = new LevelSettings(FRESH, GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                long chosen = seed.isEmpty() ? WorldOptions.randomSeed() : Long.parseLong(seed);
                minecraft.createWorldOpenFlows().createFreshLevel(FRESH, settings, new WorldOptions(chosen, true, false), WorldPresets::createNormalWorldDimensions,
                        new TitleScreen());
            } else {
                announce(folder, "title");
            }
        }
        if (!announced && minecraft.screen != null && minecraft.level == null && waited++ % 20 == 0) {
            Files.writeString(folder.resolve("screen.txt"), minecraft.screen.getClass().getName() + " \"" + minecraft.screen.getTitle().getString() + "\"");
        }
        if (minecraft.screen instanceof ConfirmScreen confirm && !announced) {
            Files.writeString(folder.resolve("failed.txt"), "start failed: a dialog stands: " + confirm.getTitle().getString());
        }
        if (!announced && minecraft.level != null && minecraft.player != null && minecraft.screen == null && ClientState.view != BodyView.NONE) {
            announce(folder, minecraft.getSingleplayerServer() == null ? "server" : minecraft.getSingleplayerServer().getWorldData().getLevelName());
        }
    }

    private static void announce(Path folder, String world) throws IOException {
        announced = true;
        JsonObject ready = new JsonObject();
        ready.addProperty("world", world);
        Files.writeString(folder.resolve("ready.json"), GSON.toJson(ready));
    }

    private static void advance(Minecraft minecraft, Path folder) throws IOException {
        if (step != null) {
            ticks++;
            each.run();
            if (until.getAsBoolean()) {
                finish();
            } else if (ticks > STEP_TICKS) {
                result.addProperty("error", step + " failed: it did not finish within " + STEP_TICKS / 20 + " s");
                finish();
                STEPS.clear();
            } else {
                return;
            }
        }
        if (STEPS.isEmpty()) {
            running = false;
            Path partial = folder.resolve("done.tmp");
            Files.writeString(partial, OUT.toString());
            Files.move(partial, folder.resolve("done.txt"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return;
        }
        step = STEPS.poll();
        result = new JsonObject();
        result.addProperty("step", step);
        HEARD.clear();
        ticks = 0;
        until = () -> true;
        each = () -> {};
        try {
            begin(minecraft, step);
        } catch (RuntimeException exception) {
            result.addProperty("error", step + " failed: " + exception.getMessage());
            STEPS.clear();
        }
        if (until.getAsBoolean()) {
            finish();
            advance(minecraft, folder);
        }
    }

    private static void finish() {
        if (!HEARD.isEmpty()) {
            JsonArray chat = new JsonArray();
            HEARD.forEach(chat::add);
            result.add("chat", chat);
        }
        OUT.append(GSON.toJson(result)).append('\n');
        step = null;
    }

    private static void begin(Minecraft minecraft, String line) {
        String[] words = line.split(" ", 2);
        String argument = words.length > 1 ? words[1] : "";
        if (line.startsWith("/")) {
            int mark = marks + 1;
            minecraft.player.connection.sendCommand(line.substring(1));
            minecraft.player.connection.sendCommand(Backcountry.MODID + " mark " + mark);
            until = () -> marks >= mark;
            return;
        }
        switch (words[0]) {
            case "seen" -> result.add("seen", seen(minecraft));
            case "screen" -> result.add("screen", screen(minecraft));
            case "pos" -> result.addProperty("pos", "%.1f %.1f %.1f facing %.0f %.0f".formatted(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(),
                    minecraft.player.getYRot(), minecraft.player.getXRot()));
            case "shot" -> shot(minecraft, argument.isEmpty() ? "shot" : argument);
            case "press" -> press(minecraft, argument);
            case "click" -> click(minecraft, argument.split(" "));
            case "key" -> key(minecraft, argument);
            case "hold" -> hold(minecraft, argument.split(" "));
            case "walk" -> walk(minecraft, argument.split(" "));
            case "bench" -> bench(minecraft, Integer.parseInt(argument));
            case "use" -> {
                KeyMapping.click(minecraft.options.keyUse.getKey());
                until = () -> ticks >= 1;
            }
            case "slot" -> minecraft.player.getInventory().selected = Integer.parseInt(argument) - 1;
            case "face" -> {
                String[] angles = argument.split(" ");
                minecraft.player.setYRot(Float.parseFloat(angles[0]));
                minecraft.player.setXRot(Float.parseFloat(angles[1]));
            }
            case "close" -> {
                if (minecraft.screen instanceof AbstractContainerScreen<?>) {
                    minecraft.player.closeContainer();
                } else {
                    minecraft.setScreen(null);
                }
            }
            case "wait" -> {
                int wait = Integer.parseInt(argument);
                until = () -> ticks >= wait;
            }
            case "gui" -> minecraft.options.hideGui = argument.equals("off");
            case "log" -> result.add("log", log(minecraft));
            case "join" -> join(minecraft, argument);
            case "quit" -> minecraft.stop();
            default -> throw new IllegalArgumentException("no step " + words[0]);
        }
    }

    private static void join(Minecraft minecraft, String address) {
        if (minecraft.level != null) {
            minecraft.level.disconnect();
            minecraft.disconnect();
        }
        ConnectScreen.startConnecting(new TitleScreen(), minecraft, ServerAddress.parseString(address), new ServerData(address, address, ServerData.Type.OTHER), false,
                null);
        until = () -> minecraft.level != null && minecraft.player != null && minecraft.screen == null;
    }

    private static JsonObject seen(Minecraft minecraft) {
        JsonObject seen = new JsonObject();
        BodyView view = ClientState.view;
        seen.addProperty("health", minecraft.player.getHealth());
        seen.addProperty("energy", Math.round(view.energy() * 100) + "%");
        seen.addProperty("water", "%.2f of %.1f L".formatted(view.waterL(), view.needL()));
        JsonArray conditions = new JsonArray();
        for (Condition condition : view.conditions()) {
            conditions.add(condition.title().getString());
        }
        seen.add("conditions", conditions);
        seen.addProperty("weight", "%.2f kg, BMI %.1f".formatted(view.weightKg(), view.bmi(Balance.get().body().heightM())));
        seen.addProperty("core", "%.2f °C, comfort %d".formatted(view.coreC(), view.comfort()));
        seen.addProperty("wetness", Math.round(view.wetness() * 100) + "%");
        seen.addProperty("stomach", Math.round(view.stomach() * 100) + "%");
        seen.addProperty("load", "%.1f of %.0f kg".formatted(view.loadKg(), view.capacityKg()));
        WeatherView weather = ClientState.weather;
        String falling = weather.falling().map(kind -> ", " + kind.title(weather.intensity()).getString().toLowerCase(Locale.ROOT) + (weather.now() ? " until " : " from ")
                + clock(weather.now() ? weather.toHour() : weather.fromHour())).orElse(", none");
        seen.addProperty("weather", "%d °C, feels like %d °C%s, wind %d km/h, cloud %d%%, high %d °C, low %d °C".formatted(Math.round(weather.airC()),
                Math.round(weather.feelsC()), falling, Math.round(weather.windKmh()), Math.round(weather.cloud() * 100), Math.round(weather.highC()),
                Math.round(weather.lowC())));
        JsonArray forecast = new JsonArray();
        for (Outlook outlook : weather.outlooks()) {
            forecast.add("%s: %s %d%%, %s, %d / %d °C, %d km/h".formatted(Calendar.label(outlook.day()).getString(), outlook.falling().id(), outlook.chance(),
                    outlook.sky().id(), Math.round(outlook.highC()), Math.round(outlook.lowC()), Math.round(outlook.windKmh())));
        }
        seen.add("forecast", forecast);
        BlockPos at = minecraft.player.blockPosition();
        seen.addProperty("sky", "%s at %d °C".formatted(minecraft.level.getBiome(at).value().getPrecipitationAt(at).getSerializedName(),
                Math.round(Climate.airC(minecraft.level, at, Calendar.day(minecraft.level)))));
        JsonArray alerts = new JsonArray();
        for (Alert alert : view.alerts()) {
            alerts.add(alert.title().getString());
        }
        seen.add("alerts", alerts);
        seen.addProperty("unread", view.unread());
        seen.addProperty("date", Calendar.label(view.day()).getString());
        seen.addProperty("time", minecraft.level.getDayTime() % 24000);
        GuiAccessor gui = (GuiAccessor) minecraft.gui;
        if (gui.backcountry$actionBarTime() > 0 && gui.backcountry$actionBar() != null) {
            seen.addProperty("action_bar", gui.backcountry$actionBar().getString());
        }
        JsonArray hotbar = new JsonArray();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            hotbar.add(stack.isEmpty() ? "" : stack.getCount() + " " + stack.getHoverName().getString());
        }
        seen.add("hotbar", hotbar);
        seen.addProperty("mode", minecraft.gameMode.getPlayerMode().getName());
        seen.addProperty("screen", minecraft.screen == null ? "" : minecraft.screen.getClass().getSimpleName() + " \"" + minecraft.screen.getTitle().getString() + "\"");
        return seen;
    }

    private static String clock(int hoursIntoDay) {
        return "%02d:00".formatted((hoursIntoDay + DAY_START_HOUR) % HOURS);
    }

    private static JsonObject screen(Minecraft minecraft) {
        JsonObject described = new JsonObject();
        if (minecraft.screen == null) {
            return described;
        }
        described.addProperty("class", minecraft.screen.getClass().getName());
        described.addProperty("title", minecraft.screen.getTitle().getString());
        JsonArray widgets = new JsonArray();
        minecraft.screen.children().forEach(child -> {
            if (child instanceof AbstractWidget widget) {
                widgets.add("%s at %d %d%s".formatted(widget.getMessage().getString(), widget.getX() + widget.getWidth() / 2, widget.getY() + widget.getHeight() / 2,
                        widget.active ? "" : " (inactive)"));
            }
        });
        described.add("widgets", widgets);
        return described;
    }

    private static void shot(Minecraft minecraft, String name) {
        boolean[] saved = {false, false};
        long from = frames;
        result.addProperty("screenshot", minecraft.gameDirectory.toPath().resolve("screenshots").resolve(name + ".png").toAbsolutePath().normalize().toString());
        each = () -> {
            if (!saved[1] && frames > from + 1) {
                saved[1] = true;
                Screenshot.grab(minecraft.gameDirectory, name + ".png", minecraft.getMainRenderTarget(), message -> saved[0] = true);
            }
        };
        until = () -> saved[0];
    }

    @SubscribeEvent
    static void drawn(RenderFrameEvent.Post event) {
        frames++;
    }

    private static void press(Minecraft minecraft, String text) {
        if (minecraft.screen == null) {
            throw new IllegalStateException("no screen is open");
        }
        String wanted = text.toLowerCase(Locale.ROOT);
        for (var child : minecraft.screen.children()) {
            if (child instanceof AbstractWidget widget && widget.active && widget.getMessage().getString().toLowerCase(Locale.ROOT).contains(wanted)) {
                double x = widget.getX() + widget.getWidth() / 2.0;
                double y = widget.getY() + widget.getHeight() / 2.0;
                var screen = minecraft.screen;
                screen.mouseClicked(x, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
                screen.mouseReleased(x, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
                return;
            }
        }
        throw new IllegalStateException("no active button reads " + text);
    }

    private static void click(Minecraft minecraft, String[] words) {
        double x = Double.parseDouble(words[0]);
        double y = Double.parseDouble(words[1]);
        int button = words.length > 2 && words[2].equals("right") ? GLFW.GLFW_MOUSE_BUTTON_RIGHT : GLFW.GLFW_MOUSE_BUTTON_LEFT;
        if (minecraft.screen == null) {
            throw new IllegalStateException("no screen is open");
        }
        var screen = minecraft.screen;
        screen.mouseClicked(x, y, button);
        screen.mouseReleased(x, y, button);
    }

    private static void key(Minecraft minecraft, String name) {
        if (name.equals("inventory") && minecraft.screen == null) {
            KeyMapping.click(minecraft.options.keyInventory.getKey());
            until = () -> ticks >= 1;
            return;
        }
        int code = KEYS.getOrDefault(name, name.length() == 1 ? InputConstants.getKey("key.keyboard." + name).getValue() : -1);
        if (code < 0 || minecraft.screen == null) {
            throw new IllegalArgumentException("no key " + name + (minecraft.screen == null ? " outside a screen" : ""));
        }
        minecraft.screen.keyPressed(code, 0, 0);
    }

    private static void hold(Minecraft minecraft, String[] words) {
        KeyMapping mapping = switch (words[0]) {
            case "forward" -> minecraft.options.keyUp;
            case "back" -> minecraft.options.keyDown;
            case "left" -> minecraft.options.keyLeft;
            case "right" -> minecraft.options.keyRight;
            case "jump" -> minecraft.options.keyJump;
            case "sneak" -> minecraft.options.keyShift;
            case "sprint" -> minecraft.options.keySprint;
            case "use" -> minecraft.options.keyUse;
            case "attack" -> minecraft.options.keyAttack;
            default -> throw new IllegalArgumentException("no key " + words[0] + " to hold");
        };
        int length = Integer.parseInt(words[1]);
        each = () -> mapping.setDown(ticks < length);
        until = () -> {
            if (ticks >= length) {
                mapping.setDown(false);
                return true;
            }
            return false;
        };
        mapping.setDown(true);
    }

    private static void walk(Minecraft minecraft, String[] target) {
        double x = Double.parseDouble(target[0]);
        double z = Double.parseDouble(target[1]);
        KeyMapping forward = minecraft.options.keyUp;
        KeyMapping jump = minecraft.options.keyJump;
        each = () -> {
            LocalPlayer player = minecraft.player;
            player.setYRot((float) Math.toDegrees(Math.atan2(-(x - player.getX()), z - player.getZ())));
            forward.setDown(true);
            jump.setDown(player.horizontalCollision || player.isInWater());
        };
        until = () -> {
            LocalPlayer player = minecraft.player;
            if (Math.hypot(x - player.getX(), z - player.getZ()) > ARRIVED_BLOCKS) {
                return false;
            }
            forward.setDown(false);
            jump.setDown(false);
            result.addProperty("arrived", "%.1f %.1f %.1f".formatted(player.getX(), player.getY(), player.getZ()));
            return true;
        };
    }

    private static void bench(Minecraft minecraft, int seconds) {
        List<Integer> samples = new ArrayList<>();
        float startYaw = minecraft.player.getYRot();
        each = () -> {
            minecraft.player.setYRot(startYaw + FULL_TURN * ticks / (seconds * TICKS_A_SECOND));
            if (ticks > 0 && ticks % TICKS_A_SECOND == 0) {
                samples.add(minecraft.getFps());
            }
        };
        until = () -> {
            if (ticks < seconds * TICKS_A_SECOND) {
                return false;
            }
            result.addProperty("fps", "average %.0f, lowest %d over %d s".formatted(samples.stream().mapToInt(Integer::intValue).average().orElse(0),
                    samples.stream().mapToInt(Integer::intValue).min().orElse(0), seconds));
            return true;
        };
    }

    private static JsonArray log(Minecraft minecraft) {
        JsonArray lines = new JsonArray();
        Path latest = minecraft.gameDirectory.toPath().resolve("logs/latest.log");
        try {
            byte[] bytes = Files.readAllBytes(latest);
            if (bytes.length < logRead) {
                logRead = 0;
            }
            String text = new String(bytes, (int) logRead, bytes.length - (int) logRead, StandardCharsets.UTF_8);
            logRead = bytes.length;
            text.lines().filter(line -> line.contains("/WARN]") || line.contains("/ERROR]")).forEach(lines::add);
        } catch (IOException exception) {
            lines.add("log failed: " + exception.getMessage());
        }
        return lines;
    }
}
