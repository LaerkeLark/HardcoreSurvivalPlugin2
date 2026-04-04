package com.example.hardcoresurvival.listeners;

import com.example.hardcoresurvival.HardcoreSurvivalPlugin;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.Random;

/**
 * Reduces the growth speed of crops and saplings by a configurable percentage.
 *
 * <p>When a growth event fires, there is a {@code (100 - rate) %} chance it
 * is cancelled, effectively slowing down growth.  A rate of 100 % leaves
 * vanilla behaviour unchanged; a rate of 0 % prevents all growth.
 *
 * <p>Affected by {@code growth.crops.*} and {@code growth.saplings.*} in
 * {@code config.yml}.
 */
public class CropGrowthListener implements Listener {

    private final HardcoreSurvivalPlugin plugin;
    private final Random random = new Random();

    public CropGrowthListener(HardcoreSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Crop growth (wheat, carrots, potatoes, etc.)
    // -----------------------------------------------------------------------

    /**
     * Fired whenever a crop block advances to its next growth stage.
     * Covers: WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART,
     * MELON_STEM, PUMPKIN_STEM, SWEET_BERRY_BUSH, BAMBOO, CACTUS,
     * SUGAR_CANE, KELP.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        Material material = event.getBlock().getType();
        int rate = plugin.getHardcoreConfig().getCropGrowthRate(material);
        if (shouldCancelGrowth(rate)) {
            event.setCancelled(true);
        }
    }

    // -----------------------------------------------------------------------
    // Sapling / tree growth
    // -----------------------------------------------------------------------

    /**
     * Fired when a sapling attempts to grow into a tree (or a mushroom/
     * bamboo/mangrove structure).  The sapling's {@link Material} is read
     * from the block at the event's location, which still holds the sapling
     * at the time the event is fired.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        Material saplingMaterial = event.getLocation().getBlock().getType();
        int rate = plugin.getHardcoreConfig().getSaplingGrowthRate(saplingMaterial);
        if (shouldCancelGrowth(rate)) {
            event.setCancelled(true);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} when a random roll determines that growth should
     * be suppressed based on the configured rate.
     *
     * @param rate percentage (0–100) of growth events that are allowed to proceed
     */
    private boolean shouldCancelGrowth(int rate) {
        if (rate >= 100) {
            return false; // vanilla – never cancel
        }
        if (rate <= 0) {
            return true; // growth disabled entirely
        }
        // Cancel with probability (100 - rate) / 100
        return random.nextInt(100) >= rate;
    }
}
