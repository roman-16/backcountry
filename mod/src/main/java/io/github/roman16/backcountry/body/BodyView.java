package io.github.roman16.backcountry.body;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record BodyView(float energy, float water, float waterL, float needL, List<Condition> conditions, float weightKg, float coreC, int comfort, int trend, float wetness,
        float awakeHours, float kcalEaten, float kcalBurned, float protein, float carbs, float fat, float stomach, float loadKg, float capacityKg, int day, int blackout,
        boolean fire, boolean sheltered, boolean windy, float clo, List<Alert> alerts, boolean unread, int ration, float storesDays, List<Float> burned, float metresToday,
        List<Integer> skills, int room, float roomC, boolean smoky, float spirits, int spiritCauses) {
    public static final BodyView NONE = new BodyView(1, 1, 0, 0, List.of(), 0, 37, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, false, 0, List.of(), false, 1, 0, List.of(), 0, List.of(1, 1, 1, 1), 0, 0, false, 0.5F, 0);
    public static final StreamCodec<RegistryFriendlyByteBuf, BodyView> STREAM_CODEC = StreamCodec.of(BodyView::write, BodyView::read);
    private static final int FLOATS = 18;

    public float bmi(double heightM) {
        return (float) (weightKg / (heightM * heightM));
    }

    private static void write(RegistryFriendlyByteBuf buffer, BodyView view) {
        for (float number : new float[] {view.energy, view.water, view.waterL, view.needL, view.weightKg, view.coreC, view.wetness, view.awakeHours, view.kcalEaten,
                view.kcalBurned, view.protein, view.carbs, view.fat, view.stomach, view.loadKg, view.capacityKg, view.clo, view.metresToday}) {
            buffer.writeFloat(number);
        }
        buffer.writeVarInt(view.conditions.size());
        view.conditions.forEach(condition -> buffer.writeVarInt(condition.ordinal()));
        buffer.writeVarInt(view.comfort);
        buffer.writeVarInt(view.trend);
        buffer.writeVarInt(view.day);
        buffer.writeVarInt(view.blackout);
        buffer.writeBoolean(view.fire);
        buffer.writeBoolean(view.sheltered);
        buffer.writeBoolean(view.windy);
        buffer.writeVarInt(view.alerts.size());
        view.alerts.forEach(alert -> buffer.writeVarInt(alert.ordinal()));
        buffer.writeBoolean(view.unread);
        buffer.writeVarInt(view.ration);
        buffer.writeFloat(view.storesDays);
        buffer.writeVarInt(view.burned.size());
        view.burned.forEach(buffer::writeFloat);
        buffer.writeVarInt(view.skills.size());
        view.skills.forEach(buffer::writeVarInt);
        buffer.writeVarInt(view.room);
        buffer.writeFloat(view.roomC);
        buffer.writeBoolean(view.smoky);
        buffer.writeFloat(view.spirits);
        buffer.writeVarInt(view.spiritCauses);
    }

    private static BodyView read(RegistryFriendlyByteBuf buffer) {
        float[] n = new float[FLOATS];
        for (int i = 0; i < n.length; i++) {
            n[i] = buffer.readFloat();
        }
        int count = buffer.readVarInt();
        List<Condition> conditions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            conditions.add(Condition.values()[buffer.readVarInt()]);
        }
        int comfort = buffer.readVarInt();
        int trend = buffer.readVarInt();
        int day = buffer.readVarInt();
        int blackout = buffer.readVarInt();
        boolean fire = buffer.readBoolean();
        boolean sheltered = buffer.readBoolean();
        boolean windy = buffer.readBoolean();
        int raised = buffer.readVarInt();
        List<Alert> alerts = new ArrayList<>();
        for (int i = 0; i < raised; i++) {
            alerts.add(Alert.values()[buffer.readVarInt()]);
        }
        boolean unread = buffer.readBoolean();
        int ration = buffer.readVarInt();
        float storesDays = buffer.readFloat();
        int causes = buffer.readVarInt();
        List<Float> burned = new ArrayList<>();
        for (int i = 0; i < causes; i++) {
            burned.add(buffer.readFloat());
        }
        int practised = buffer.readVarInt();
        List<Integer> skills = new ArrayList<>();
        for (int i = 0; i < practised; i++) {
            skills.add(buffer.readVarInt());
        }
        int room = buffer.readVarInt();
        float roomC = buffer.readFloat();
        boolean smoky = buffer.readBoolean();
        float spirits = buffer.readFloat();
        int spiritCauses = buffer.readVarInt();
        return new BodyView(n[0], n[1], n[2], n[3], List.copyOf(conditions), n[4], n[5], comfort, trend, n[6], n[7], n[8], n[9], n[10], n[11], n[12], n[13], n[14], n[15],
                day, blackout, fire, sheltered, windy, n[16], List.copyOf(alerts), unread, ration, storesDays, List.copyOf(burned), n[17], List.copyOf(skills), room, roomC, smoky,
                spirits, spiritCauses);
    }
}
