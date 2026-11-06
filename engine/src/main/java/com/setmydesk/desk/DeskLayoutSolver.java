package com.setmydesk.desk;

import com.setmydesk.desk.DeskModels.Handedness;
import com.setmydesk.desk.DeskModels.ItemRequest;
import com.setmydesk.desk.DeskModels.Layout;
import com.setmydesk.desk.DeskModels.LayoutRequest;
import com.setmydesk.desk.DeskModels.Placement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic desk layout solver. Same input, same layout, every time.
 * <p>
 * Coordinates are centimetres from the desk centre: x to the right, z towards the person sitting.
 * Each item has an ergonomic target; the solver puts it on the nearest free 1 cm grid cell (trying a
 * 90° rotation if needed) without overlapping anything or hanging off the edge. Pinned items are
 * placed first, exactly where asked.
 */
@Component
public class DeskLayoutSolver {

    static final double CLEARANCE = 1.0;
    static final double KEYBOARD_FRONT_GAP = 10.0;
    static final double MOUSE_GAP = 4.0;
    static final double MIN_VIEWING_DISTANCE = 50.0;

    private record Rect(double x, double z, double w, double d) {
        boolean collides(Rect o) {
            return Math.abs(x - o.x) < (w + o.w) / 2 + CLEARANCE && Math.abs(z - o.z) < (d + o.d) / 2 + CLEARANCE;
        }
    }

    private record Item(int index, ItemRequest req) {
    }

    public Layout solve(LayoutRequest req) {
        double W = req.deskWidthCm(), D = req.deskDepthCm();
        double mirror = req.hand() == Handedness.LEFT ? -1 : 1;

        List<Item> items = new ArrayList<>();
        for (int i = 0; i < req.items().size(); i++) items.add(new Item(i, req.items().get(i)));
        // Pinned first, then catalogue order (screens → inputs → accessories), then request order.
        items.sort(Comparator.<Item>comparingInt(it -> it.req().pinned() ? 0 : 1)
                .thenComparingInt(it -> it.req().type().ordinal())
                .thenComparingInt(Item::index));

        List<Placement> placed = new ArrayList<>();
        List<Rect> occupied = new ArrayList<>();
        List<String> unplaced = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean hasMonitor = req.items().stream().anyMatch(r -> r.type() == DeskItemType.MONITOR);
        int speakerCount = 0;

        for (Item it : items) {
            DeskItemType t = it.req().type();
            boolean preferRotated = Boolean.TRUE.equals(it.req().rotated());

            if (it.req().pinned()) {
                double w = preferRotated ? t.depthCm : t.widthCm, d = preferRotated ? t.widthCm : t.depthCm;
                Rect r = new Rect(it.req().x(), it.req().z(), w, d);
                if (inside(r, W, D) && occupied.stream().noneMatch(r::collides)) {
                    occupied.add(r);
                    placed.add(placement(it, r, preferRotated, true));
                    continue;
                }
                warnings.add(t.label + " could not go exactly where you pinned it, so the engine found the nearest good spot.");
            }

            double[] target = target(t, W, D, mirror, hasMonitor, placed, speakerCount);
            if (t == DeskItemType.SPEAKER) speakerCount++;

            Rect best = null;
            boolean rotated = false;
            for (boolean rot : preferRotated ? new boolean[]{true, false} : new boolean[]{false, true}) {
                double w = rot ? t.depthCm : t.widthCm, d = rot ? t.widthCm : t.depthCm;
                Rect r = nearestFree(target[0], target[1], w, d, W, D, occupied);
                if (r != null) {
                    best = r;
                    rotated = rot;
                    break;
                }
            }
            if (best == null) {
                unplaced.add(t.label);
                continue;
            }
            occupied.add(best);
            placed.add(placement(it, best, rotated, false));
        }

        centreMonitorGroup(placed, occupied, W, D);
        placed.sort(Comparator.comparingInt(Placement::index));
        List<String> ergo = ergonomics(placed, D);
        warnings.addAll(ergo);
        if (!unplaced.isEmpty()) warnings.add("Not enough room for: " + String.join(", ", unplaced) + ". Try a bigger desk or fewer items.");

        double used = occupied.stream().mapToDouble(r -> r.w() * r.d()).sum();
        int score = (int) Math.max(0, 100 - 12 * ergo.size() - 8 * unplaced.size());
        return new Layout(W, D, req.hand(), placed, unplaced, warnings, score, Math.round(1000 * used / (W * D)) / 10.0);
    }

    private double[] target(DeskItemType t, double W, double D, double mirror, boolean hasMonitor,
                            List<Placement> placed, int speakerCount) {
        double halfW = W / 2 - t.widthCm / 2.0, halfD = D / 2 - t.depthCm / 2.0;
        double tx = mirror * t.anchorX * halfW, tz = t.anchorZ * halfD;
        switch (t) {
            case KEYBOARD -> tz = D / 2 - KEYBOARD_FRONT_GAP - t.depthCm / 2.0;
            case LAPTOP -> {
                if (!hasMonitor) { tx = 0; tz = 0; }
            }
            default -> {
            }
        }
        if (t == DeskItemType.MOUSE) {
            var kb = placed.stream().filter(p -> p.type() == DeskItemType.KEYBOARD).findFirst();
            if (kb.isPresent()) {
                tx = kb.get().x() + mirror * (kb.get().widthCm() / 2 + MOUSE_GAP + t.widthCm / 2.0);
                tz = kb.get().z();
            }
        }
        if (t == DeskItemType.SPEAKER) {
            var screens = placed.stream().filter(p -> p.type() == DeskItemType.MONITOR || p.type() == DeskItemType.LAPTOP).toList();
            if (!screens.isEmpty()) {
                double left = screens.stream().mapToDouble(p -> p.x() - p.widthCm() / 2).min().orElse(0);
                double right = screens.stream().mapToDouble(p -> p.x() + p.widthCm() / 2).max().orElse(0);
                tx = speakerCount % 2 == 0 ? left - 3 - t.widthCm / 2.0 : right + 3 + t.widthCm / 2.0;
                tz = screens.get(0).z();
            }
        }
        return new double[]{tx, tz};
    }

    private static Rect nearestFree(double tx, double tz, double w, double d, double W, double D, List<Rect> occupied) {
        double minX = Math.ceil(-W / 2 + w / 2), maxX = Math.floor(W / 2 - w / 2);
        double minZ = Math.ceil(-D / 2 + d / 2), maxZ = Math.floor(D / 2 - d / 2);
        if (minX > maxX || minZ > maxZ) return null;
        double cx = clamp(Math.round(tx), minX, maxX), cz = clamp(Math.round(tz), minZ, maxZ);

        // Expanding square rings around the target; within a ring pick the closest free cell.
        int maxRing = (int) Math.max(maxX - minX, maxZ - minZ) + 1;
        for (int ring = 0; ring <= maxRing; ring++) {
            Rect best = null;
            double bestDist = Double.MAX_VALUE;
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    double x = cx + dx, z = cz + dz;
                    if (x < minX || x > maxX || z < minZ || z > maxZ) continue;
                    double dist = (x - tx) * (x - tx) + (z - tz) * (z - tz);
                    if (dist >= bestDist) continue;
                    Rect r = new Rect(x, z, w, d);
                    if (occupied.stream().noneMatch(r::collides)) {
                        best = r;
                        bestDist = dist;
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    /** Two or more monitors end up side by side; shift the group so it is centred if that is free. */
    private static void centreMonitorGroup(List<Placement> placed, List<Rect> occupied, double W, double D) {
        List<Placement> monitors = placed.stream().filter(p -> p.type() == DeskItemType.MONITOR && !p.pinned()).toList();
        if (monitors.size() < 2) return;
        double left = monitors.stream().mapToDouble(p -> p.x() - p.widthCm() / 2).min().orElse(0);
        double right = monitors.stream().mapToDouble(p -> p.x() + p.widthCm() / 2).max().orElse(0);
        double shift = -Math.round((left + right) / 2);
        if (shift == 0) return;

        List<Rect> others = new ArrayList<>(occupied);
        monitors.forEach(m -> others.removeIf(r -> r.x() == m.x() && r.z() == m.z() && r.w() == m.widthCm()));
        List<Rect> moved = monitors.stream().map(m -> new Rect(m.x() + shift, m.z(), m.widthCm(), m.depthCm())).toList();
        boolean ok = moved.stream().allMatch(r -> inside(r, W, D) && others.stream().noneMatch(r::collides));
        if (!ok) return;
        for (int i = 0; i < monitors.size(); i++) {
            Placement m = monitors.get(i);
            placed.set(placed.indexOf(m), new Placement(m.index(), m.type(), m.label(), m.x() + shift, m.z(),
                    m.widthCm(), m.depthCm(), m.heightCm(), m.rotated(), false, m.color()));
        }
        occupied.clear();
        occupied.addAll(others);
        occupied.addAll(moved);
    }

    private static List<String> ergonomics(List<Placement> placed, double D) {
        List<String> w = new ArrayList<>();
        for (Placement p : placed) {
            if (p.type() == DeskItemType.MONITOR) {
                double distance = D / 2 - (p.z() + p.depthCm() / 2);
                if (distance < MIN_VIEWING_DISTANCE) {
                    w.add("Monitor is about %.0f cm from the desk edge; 50–75 cm is easier on the eyes. A monitor arm or a deeper desk helps."
                            .formatted(distance));
                    break;
                }
            }
        }
        boolean laptopOnly = placed.stream().anyMatch(p -> p.type() == DeskItemType.LAPTOP)
                && placed.stream().noneMatch(p -> p.type() == DeskItemType.MONITOR);
        if (laptopOnly) w.add("Laptop is your only screen. Raise it to eye level with a stand and use an external keyboard to protect your neck.");
        if (D < 60) w.add("Desk is shallower than 60 cm, so screens will sit close to your eyes.");
        return w;
    }

    private static Placement placement(Item it, Rect r, boolean rotated, boolean pinned) {
        DeskItemType t = it.req().type();
        return new Placement(it.index(), t, t.label, r.x(), r.z(), r.w(), r.d(), t.heightCm, rotated, pinned, t.color);
    }

    private static boolean inside(Rect r, double W, double D) {
        return r.x() - r.w() / 2 >= -W / 2 && r.x() + r.w() / 2 <= W / 2 && r.z() - r.d() / 2 >= -D / 2 && r.z() + r.d() / 2 <= D / 2;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
