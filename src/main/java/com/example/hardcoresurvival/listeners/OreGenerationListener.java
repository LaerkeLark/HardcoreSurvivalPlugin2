package com.example.hardcoresurvival.listeners;

import com.example.hardcoresurvival.HardcoreSurvivalPlugin;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.Random;

/**
 * Reduces the number of ore blocks that generate in newly loaded chunks.
 *
 * <p>When a brand-new chunk is first loaded (i.e. freshly generated),
 * every ore block is subjected to a random keep-rate check.  If the random
 * roll fails, the ore block is replaced with the appropriate surrounding
 * stone-type material (stone, deepslate, or netherrack depending on the
 * ore).
 *
 * <p>Effective keep-rates per ore material are read from the {@code ores.*}
 * section of {@code config.yml}:
 * <ul>
 *   <li>100 % → vanilla generation (no blocks removed)</li>
 *   <li>50 %  → approximately half of each ore type is removed</li>
 *   <li>0 %   → all occurrences of that ore are removed</li>
 * </ul>
 *
 * <p>Processing is scheduled one tick after the {@link ChunkLoadEvent} so
 * that the chunk has been fully populated before scanning begins.
 */
public class OreGenerationListener implements Listener {

    private final HardcoreSurvivalPlugin plugin;
    private final Random random = new Random();

    public OreGenerationListener(HardcoreSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Event handler
    // -----------------------------------------------------------------------

    /**
     * Fired whenever a chunk is loaded.  Only newly generated chunks are
     * processed ({@link ChunkLoadEvent#isNewChunk()} must be {@code true}).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) {
            return;
        }
        Chunk chunk = event.getChunk();
        // Delay processing by 1 tick to ensure the chunk is fully populated
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> processNewChunk(chunk), 1L);
    }

    // -----------------------------------------------------------------------
    // Chunk scanning
    // -----------------------------------------------------------------------

    /**
     * Scans every block in the chunk, and for ore blocks that have a
     * configured keep-rate below 100 %, randomly replaces them with the
     * appropriate stone-type replacement.
     */
    private void processNewChunk(Chunk chunk) {
        int minY = chunk.getWorld().getMinHeight();
        int maxY = chunk.getWorld().getMaxHeight();

        for (int x = 0; x < 16; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = 0; z < 16; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material material = block.getType();

                    int rate = plugin.getPluginConfig2().getOreRate(material);
                    if (rate >= 100) {
                        continue; // vanilla – keep block
                    }
                    if (rate <= 0 || random.nextInt(100) >= rate) {
                        block.setType(getReplacementMaterial(material));
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the natural stone-type material that should replace a removed
     * ore block.  Deepslate ore variants → DEEPSLATE; Nether ores &
     * ANCIENT_DEBRIS → NETHERRACK; all others → STONE.
     */
    private Material getReplacementMaterial(Material ore) {
        switch (ore) {
            case DEEPSLATE_COAL_ORE:
            case DEEPSLATE_IRON_ORE:
            case DEEPSLATE_COPPER_ORE:
            case DEEPSLATE_GOLD_ORE:
            case DEEPSLATE_LAPIS_ORE:
            case DEEPSLATE_REDSTONE_ORE:
            case DEEPSLATE_DIAMOND_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return Material.DEEPSLATE;
            case NETHER_QUARTZ_ORE:
            case NETHER_GOLD_ORE:
            case ANCIENT_DEBRIS:
                return Material.NETHERRACK;
            default:
                return Material.STONE;
        }
    }
}
