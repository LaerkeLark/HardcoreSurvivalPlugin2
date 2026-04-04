package com.hardcoresurvival.listeners;

import com.hardcoresurvival.HardcoreSurvivalPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.Random;

/**
 * Reduces the natural growth speed of crops and saplings based on the
 * percentage values configured in config.yml under {@code growth.crops}
 * and {@code growth.saplings}.
 *
 * <p>A value of 100 means normal vanilla speed. A value of 50 cancels
 * 50 % of growth ticks, effectively halving the speed.</p>
 */
public class GrowthListener implements Listener {

    private final HardcoreSurvivalPlugin plugin;
    private final Random random = new Random();

    public GrowthListener(HardcoreSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    /** Called every time a crop block advances one growth stage. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        String key = "growth.crops." + block.getType().name().toLowerCase();

        if (!plugin.getConfig().contains(key)) {
            return;
        }

        double rate = plugin.getConfig().getDouble(key, 100.0);
        if (shouldCancel(rate)) {
            event.setCancelled(true);
        }
    }

    /** Called when a sapling attempts to grow into a full tree structure. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        Material saplingType = event.getLocation().getBlock().getType();
        String key = "growth.saplings." + saplingType.name().toLowerCase();

        if (!plugin.getConfig().contains(key)) {
            return;
        }

        double rate = plugin.getConfig().getDouble(key, 100.0);
        if (shouldCancel(rate)) {
            event.setCancelled(true);
        }
    }

    /**
     * Returns {@code true} if the growth event should be cancelled given the
     * configured {@code rate} (0–100).
     *
     * <ul>
     *   <li>rate &le; 0  → always cancel</li>
     *   <li>rate &ge; 100 → never cancel</li>
     *   <li>otherwise    → cancel with probability {@code (100 - rate) / 100}</li>
     * </ul>
     */
    private boolean shouldCancel(double rate) {
        if (rate <= 0.0) {
            return true;
        }
        if (rate >= 100.0) {
            return false;
        }
        // Cancel if the random roll falls outside the allowed rate window
        return random.nextDouble() * 100.0 >= rate;
    }
}
