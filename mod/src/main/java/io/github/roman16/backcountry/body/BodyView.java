package io.github.roman16.backcountry.body;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record BodyView(float energy, float water, float waterL, float needL, List<Condition> conditions, float weightKg, float coreC, int comfort, int trend, float wetness,
        float awakeHours, float kcalEaten, float kcalBurned, float protein, float carbs, float fat, float stomach, float loadKg, float capacityKg, int day, int blackout,
        boolean fire, boolean sheltered, float clo) {
    public static final BodyView NONE = new BodyView(1, 1, 0, 0, List.of(), 0, 37, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, 0);
    public static final StreamCodec<RegistryFriendlyByteBuf, BodyView> STREAM_CODEC = StreamCodec.of(BodyView::write, BodyView::read);

    public float bmi(double heightM) {
        return (float) (weightKg / (heightM * heightM));
    }

    private static void write(RegistryFriendlyByteBuf buffer, BodyView view) {
        for (float number : new float[] {view.energy, view.water, view.waterL, view.needL, view.weightKg, view.coreC, view.wetness, view.awakeHours, view.kcalEaten,
                view.kcalBurned, view.protein, view.carbs, view.fat, view.stomach, view.loadKg, view.capacityKg, view.clo}) {
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
    }

    private static BodyView read(RegistryFriendlyByteBuf buffer) {
        float[] n = new float[17];
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
        return new BodyView(n[0], n[1], n[2], n[3], List.copyOf(conditions), n[4], n[5], comfort, trend, n[6], n[7], n[8], n[9], n[10], n[11], n[12], n[13], n[14], n[15],
                day, blackout, fire, sheltered, n[16]);
    }
}
