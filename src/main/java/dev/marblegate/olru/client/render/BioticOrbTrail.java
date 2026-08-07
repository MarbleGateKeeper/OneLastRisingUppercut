package dev.marblegate.olru.client.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Per-entity ring buffer of recent Biotic Orb center positions, sampled once per entity tick from
 * {@link BioticOrbRenderer#submit} and drawn as a fading ribbon behind the orb.
 */
final class BioticOrbTrail {
    private static final int CAPACITY = 22;
    private static final Map<Integer, Trail> TRAILS = new HashMap<>();

    private BioticOrbTrail() {}

    /** Records the orb center, advancing the buffer only when the entity tick advances. */
    static void update(int entityId, float ageInTicks, double x, double y, double z) {
        Trail trail = TRAILS.computeIfAbsent(entityId, id -> new Trail());
        int tick = (int) ageInTicks;
        if (tick <= trail.lastTick) return;
        trail.lastTick = tick;
        int slot = (trail.head + trail.size) % CAPACITY;
        trail.points[slot * 3] = x;
        trail.points[slot * 3 + 1] = y;
        trail.points[slot * 3 + 2] = z;
        if (trail.size < CAPACITY) {
            trail.size++;
        } else {
            trail.head = (trail.head + 1) % CAPACITY;
        }
    }

    /** Drops trails whose entity no longer exists, and every trail once the level is gone. */
    static void prune() {
        ClientLevel level = Minecraft.getInstance().level;
        TRAILS.entrySet().removeIf(entry -> level == null || level.getEntity(entry.getKey()) == null);
    }

    /** Oldest-to-newest flat xyz snapshot, or null while fewer than two points are recorded. */
    static double[] snapshot(int entityId) {
        Trail trail = TRAILS.get(entityId);
        if (trail == null || trail.size < 2) return null;
        double[] result = new double[trail.size * 3];
        for (int i = 0; i < trail.size; i++) {
            int slot = (trail.head + i) % CAPACITY;
            result[i * 3] = trail.points[slot * 3];
            result[i * 3 + 1] = trail.points[slot * 3 + 1];
            result[i * 3 + 2] = trail.points[slot * 3 + 2];
        }
        return result;
    }

    /** Catmull-Rom resampling removes the one-tick corners visible during bounces and turns. */
    static double[] snapshotSmoothed(int entityId) {
        double[] raw = snapshot(entityId);
        if (raw == null || raw.length < 12) return raw;
        int count = raw.length / 3;
        int subdivisions = 3;
        double[] smooth = new double[((count - 1) * subdivisions + 1) * 3];
        int out = 0;
        for (int segment = 0; segment < count - 1; segment++) {
            int p0 = Math.max(0, segment - 1);
            int p1 = segment;
            int p2 = segment + 1;
            int p3 = Math.min(count - 1, segment + 2);
            for (int step = 0; step < subdivisions; step++) {
                double t = (double) step / subdivisions;
                for (int axis = 0; axis < 3; axis++) {
                    smooth[out++] = catmull(
                            raw[p0 * 3 + axis], raw[p1 * 3 + axis],
                            raw[p2 * 3 + axis], raw[p3 * 3 + axis], t);
                }
            }
        }
        smooth[out++] = raw[(count - 1) * 3];
        smooth[out++] = raw[(count - 1) * 3 + 1];
        smooth[out] = raw[(count - 1) * 3 + 2];
        return smooth;
    }

    private static double catmull(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2.0 * p1)
                + (-p0 + p2) * t
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }

    private static final class Trail {
        private final double[] points = new double[CAPACITY * 3];
        private int head;
        private int size;
        private int lastTick = Integer.MIN_VALUE;
    }
}
