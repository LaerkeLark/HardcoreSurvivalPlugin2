package com.hardcoresurvival.listeners;

import com.hardcoresurvival.HardcoreSurvivalPlugin;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Adjusts ore spawn density in freshly generated chunks.
 *
 * <p>When a chunk is first populated by the world generator this listener
 * iterates every block.  Any ore whose configured rate is below 100 % has a
 * {@code (100 - rate) / 100} chance of being replaced by its surrounding
 * natural material (stone / deepslate / netherrack), effectively reducing
 * the average ore density.</p>
 *
 * <p>Configuration is read from {@code ores.overworld.*} and
 * {@code ores.nether.*} in config.yml.  Overworld entries cover both the
 * regular and the deepslate variant of each ore.</p>
 */
public class OreListener implements Listener {

    private final HardcoreSurvivalPlugin plugin;
    private final Random random = new Random();

    // All overworld ore materials that this listener tracks
    private static final Set<Material> OVERWORLD_ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE
    );

    // All nether ore materials that this listener tracks
    private static final Set<Material> NETHER_ORES = EnumSet.of(
            Material.NETHER_QUARTZ_ORE,
            Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS
    );

    // Maps each ore Material to the config key used in config.yml
    private static final Map<Material, String> ORE_CONFIG_KEY = new EnumMap<>(Material.class);

    static {
        // Overworld – regular and deepslate variants share the same config key
        ORE_CONFIG_KEY.put(Material.COAL_ORE,              "ores.overworld.coal");
        ORE_CONFIG_KEY.put(Material.DEEPSLATE_COAL_ORE,    "ores.overworld.coal");
        ORE_CONFIG_KEY.put(Material.IRON_ORE,              "ores.overworld.iron");
        ORE_CONFIG_KEY.put(Material.DEEPSLATE_IRON_ORE,    "ores.overworld.iron");
        ORE_CONFIG_KEY.put(Material.COPPER_ORE,            "ores.overworld.copper");
        ORE_CONFIG_KEY.put(Material.DEEPSLATE_COPPER_ORE,  "ores.overworld.copper");
        ORE_CONFIG_KEY.put(Material.GOLD_ORE,              "ores.overworld.gold");
        ORE_CONFIG_KEY.put(Material.DEEPSLATE_GOLD_ORE,    "ores.overworld.gold");
        ORE_CONFIG_KEY.put(Material.REDSTONE_ORE,          "ores.overworld.redstone");
        ORE_CONFIG_KEY.put(Material.DEEPSLATE_REDSTONE_ORE, "ores.overworld.redstone");
        ORE_CONFIG_KEY.put(Material.LAPIS_ORE,             "ores.overworld.lapis");
        ORE_CONFIG_KEY.put(Material.DEEPSLATE_LAPIS_ORE,   "ores.overworld.lapis");
        ORE_CONFIG_KEY.put(Material.DIAMOND_ORE,           "ores.overworld.diamond");
        ORE_CONFIG_KEY.put(Material.DEEPSLATE_DIAMOND_ORE, "ores.overworld.diamond");
        ORE_CONFIG_KEY.put(Material.EMERALD_ORE,           "ores.overworld.emerald");
        ORE_CONFIG_KEY.put(Material.DEEPSLATE_EMERALD_ORE, "ores.overworld.emerald");

        // Nether
        ORE_CONFIG_KEY.put(Material.NETHER_QUARTZ_ORE, "ores.nether.nether_quartz");
        ORE_CONFIG_KEY.put(Material.NETHER_GOLD_ORE,   "ores.nether.nether_gold");
        ORE_CONFIG_KEY.put(Material.ANCIENT_DEBRIS,    "ores.nether.ancient_debris");
    }

    public OreListener(HardcoreSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Fires once when a brand-new chunk is fully decorated by the world
     * generator.  We scan every block for tracked ores and probabilistically
     * remove them according to the configured rate.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChunkPopulate(ChunkPopulateEvent event) {
        Chunk chunk = event.getChunk();
        World.Environment environment = chunk.getWorld().getEnvironment();

        Set<Material> relevantOres;
        if (environment == World.Environment.NORMAL) {
            relevantOres = OVERWORLD_ORES;
        } else if (environment == World.Environment.NETHER) {
            relevantOres = NETHER_ORES;
        } else {
            return;
        }

        // Quick exit: if every configured rate is 100 %, skip the scan entirely
        if (allRatesNormal(relevantOres)) {
            return;
        }

        int minY = chunk.getWorld().getMinHeight();
        int maxY = chunk.getWorld().getMaxHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material material = block.getType();

                    if (!relevantOres.contains(material)) {
                        continue;
                    }

                    String configKey = ORE_CONFIG_KEY.get(material);
                    double rate = plugin.getConfig().getDouble(configKey, 100.0);

                    if (rate >= 100.0) {
                        continue;
                    }

                    if (rate <= 0.0 || random.nextDouble() * 100.0 >= rate) {
                        block.setType(replacementFor(environment, y), false);
                    }
                }
            }
        }
    }

    /** Returns {@code true} when no ore in the set has a rate below 100. */
    private boolean allRatesNormal(Set<Material> ores) {
        for (Material ore : ores) {
            String key = ORE_CONFIG_KEY.get(ore);
            if (key != null && plugin.getConfig().getDouble(key, 100.0) < 100.0) {
                return false;
            }
        }
        return true;
    }

    /** Returns the block type that should replace a removed ore. */
    private Material replacementFor(World.Environment environment, int y) {
        if (environment == World.Environment.NETHER) {
            return Material.NETHERRACK;
        }
        // In the overworld, use deepslate below y = 0 and stone above
        return y < 0 ? Material.DEEPSLATE : Material.STONE;
    }
}
