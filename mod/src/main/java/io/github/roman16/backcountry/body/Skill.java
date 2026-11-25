package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public enum Skill {
    FIRE_MAKING,
    WOODCUTTING,
    BUTCHERING,
    ARCHERY;

    public static final int HIGHEST = 5;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public MutableComponent title() {
        return Component.translatable("skill.backcountry." + id());
    }

    public static int level(int uses) {
        int level = 1;
        for (double needed : Balance.get().skills().usesToLevel()) {
            if (uses >= needed) {
                level++;
            }
        }
        return level;
    }

    public static double effort(int level) {
        return 1 - Balance.get().skills().levelShare() * (level - 1);
    }

    public int level(Body body) {
        return level(body.skills.getOrDefault(this, 0));
    }

    public double effort(Body body) {
        return effort(level(body));
    }

    public double speed(Body body) {
        return 1 / effort(body);
    }

    public void use(ServerPlayer player, int count) {
        if (!Metabolism.lives(player) || count <= 0) {
            return;
        }
        Body body = Metabolism.body(player);
        int before = level(body);
        body.skills.merge(this, count, Integer::sum);
        int after = level(body);
        if (after > before) {
            player.displayClientMessage(Component.translatable("message.backcountry.skill_up", title(), after), true);
        }
    }

    public static List<Integer> levels(Body body) {
        return Arrays.stream(values()).map(skill -> skill.level(body)).toList();
    }
}
