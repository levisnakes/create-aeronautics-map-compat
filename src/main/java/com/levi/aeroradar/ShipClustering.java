package com.levi.aeroradar;

import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Groups Create: Aeronautics sub-levels into physical "ships". Sub-levels whose
 * bounding boxes (nearly) touch are one vehicle - bearing craft, trailers and
 * multi-part contraptions would otherwise each spawn their own map marker.
 *
 * Shared by the client tracker and the server registry so both derive the SAME
 * ship id for a given ship: the id is the smallest member UUID (deterministic,
 * geometry-independent), while name / bounds / centre come from the largest member
 * (the hull).
 */
public final class ShipClustering {

    public record Member(UUID id, UUID repId, BoundingBox3dc bounds) {}

    public record Cluster(UUID repId, String name, Vector3d center, BoundingBox3dc bounds) {}

    public record Result(List<Cluster> clusters, List<Member> members) {}

    private ShipClustering() {}

    public static Result cluster(List<? extends SubLevel> input, double margin) {
        List<SubLevel> raw = new ArrayList<>();
        for (SubLevel s : input) {
            if (s != null && !s.isRemoved() && s.getUniqueId() != null && s.boundingBox() != null) {
                raw.add(s);
            }
        }
        List<Cluster> clusters = new ArrayList<>();
        List<Member> members = new ArrayList<>();
        int n = raw.size();
        if (n == 0) return new Result(clusters, members);

        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (touches(raw.get(i).boundingBox(), raw.get(j).boundingBox(), margin)) {
                    parent[find(parent, i)] = find(parent, j);
                }
            }
        }

        // Per root: the largest member (hull) and the smallest UUID (stable id).
        Map<Integer, Integer> hullIndex = new LinkedHashMap<>();
        Map<Integer, UUID> minId = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            Integer hull = hullIndex.get(root);
            if (hull == null || volume(raw.get(i).boundingBox()) > volume(raw.get(hull).boundingBox())) {
                hullIndex.put(root, i);
            }
            UUID id = raw.get(i).getUniqueId();
            UUID cur = minId.get(root);
            if (cur == null || id.compareTo(cur) < 0) minId.put(root, id);
        }

        for (Map.Entry<Integer, Integer> e : hullIndex.entrySet()) {
            int root = e.getKey();
            SubLevel hull = raw.get(e.getValue());
            UUID repId = minId.get(root);
            BoundingBox3dc bb = hull.boundingBox();
            String name = hull.getName();
            if (name != null && name.isBlank()) name = null;
            clusters.add(new Cluster(repId, name, bb.center(), bb));
        }
        for (int i = 0; i < n; i++) {
            UUID repId = minId.get(find(parent, i));
            members.add(new Member(raw.get(i).getUniqueId(), repId, raw.get(i).boundingBox()));
        }
        return new Result(clusters, members);
    }

    private static boolean touches(BoundingBox3dc a, BoundingBox3dc b, double margin) {
        return a.minX() - margin <= b.maxX() && a.maxX() + margin >= b.minX()
                && a.minY() - margin <= b.maxY() && a.maxY() + margin >= b.minY()
                && a.minZ() - margin <= b.maxZ() && a.maxZ() + margin >= b.minZ();
    }

    private static double volume(BoundingBox3dc bb) {
        return (bb.maxX() - bb.minX()) * (bb.maxY() - bb.minY()) * (bb.maxZ() - bb.minZ());
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }
}
