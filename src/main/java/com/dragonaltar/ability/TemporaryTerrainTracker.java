package com.dragonaltar.ability;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class TemporaryTerrainTracker {
    private final Map<Block, TemporaryTerrain> terrain = new HashMap<>();

    void track(Block block, BlockData original, Material expected) {
        terrain.putIfAbsent(block, new TemporaryTerrain(original.clone(), expected));
    }

    void revert(Map<Block, BlockData> originals, Material expected) {
        revert(originals, Set.of(expected));
    }

    void revert(Map<Block, BlockData> originals, Set<Material> expected) {
        for (var entry : originals.entrySet()) {
            if (expected.contains(entry.getKey().getType())) {
                entry.getKey().setBlockData(entry.getValue(), false);
            }
            terrain.remove(entry.getKey());
        }
    }

    void revertAll() {
        for (var entry : new ArrayList<>(terrain.entrySet())) {
            if (entry.getKey().getType() == entry.getValue().expected()) {
                entry.getKey().setBlockData(entry.getValue().original(), false);
            }
        }
        terrain.clear();
    }

    private record TemporaryTerrain(BlockData original, Material expected) {
    }
}
