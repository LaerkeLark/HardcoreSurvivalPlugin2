package com.laerkelark.hardcoresurvival2.listeners;

import com.laerkelark.hardcoresurvival2.HardcoreSurvivalPlugin;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Reduces ore vein generation after a chunk is populated.
 *
 * For each ore in the chunk the listener uses the configured reduction
 * percentage to decide (randomly) whether to replace that ore block with its
 * natural background material (stone / deepslate / netherrack / end_stone).
 *
 * A 50 % reduction means, on average, half of the naturally generated ore
 * blocks will be removed, effectively halving vein sizes and density.
 */
public class OreReductionListener implements Listener {

    /** Maps every ore Material to the config.yml key used for its reduction %. */
    private static final Map<Material, String> ORE_CONFIG_KEYS;

    /** Replacement material for each ore depending on its host stone type. */
    private static final Map<Material, Material> ORE_REPLACEMENTS;

    static {
        ORE_CONFIG_KEYS = new EnumMap<>(Material.class);
        // Overworld stone ores
        ORE_CONFIG_KEYS.put(Material.COAL_ORE,             "coal_ore");
        ORE_CONFIG_KEYS.put(Material.IRON_ORE,             "iron_ore");
        ORE_CONFIG_KEYS.put(Material.GOLD_ORE,             "gold_ore");
        ORE_CONFIG_KEYS.put(Material.DIAMOND_ORE,          "diamond_ore");
        ORE_CONFIG_KEYS.put(Material.EMERALD_ORE,          "emerald_ore");
        ORE_CONFIG_KEYS.put(Material.LAPIS_ORE,            "lapis_ore");
        ORE_CONFIG_KEYS.put(Material.REDSTONE_ORE,         "redstone_ore");
        ORE_CONFIG_KEYS.put(Material.COPPER_ORE,           "copper_ore");
        // Overworld deepslate ores
        ORE_CONFIG_KEYS.put(Material.DEEPSLATE_COAL_ORE,    "coal_ore");
        ORE_CONFIG_KEYS.put(Material.DEEPSLATE_IRON_ORE,    "iron_ore");
        ORE_CONFIG_KEYS.put(Material.DEEPSLATE_GOLD_ORE,    "gold_ore");
        ORE_CONFIG_KEYS.put(Material.DEEPSLATE_DIAMOND_ORE, "diamond_ore");
        ORE_CONFIG_KEYS.put(Material.DEEPSLATE_EMERALD_ORE, "emerald_ore");
        ORE_CONFIG_KEYS.put(Material.DEEPSLATE_LAPIS_ORE,   "lapis_ore");
        ORE_CONFIG_KEYS.put(Material.DEEPSLATE_REDSTONE_ORE,"redstone_ore");
        ORE_CONFIG_KEYS.put(Material.DEEPSLATE_COPPER_ORE,  "copper_ore");
        // Nether ores
        ORE_CONFIG_KEYS.put(Material.NETHER_QUARTZ_ORE,    "nether_quartz_ore");
        ORE_CONFIG_KEYS.put(Material.NETHER_GOLD_ORE,      "nether_gold_ore");
        ORE_CONFIG_KEYS.put(Material.ANCIENT_DEBRIS,       "ancient_debris");

        ORE_REPLACEMENTS = new EnumMap<>(Material.class);
        // Stone ores → stone
        ORE_REPLACEMENTS.put(Material.COAL_ORE,              Material.STONE);
        ORE_REPLACEMENTS.put(Material.IRON_ORE,              Material.STONE);
        ORE_REPLACEMENTS.put(Material.GOLD_ORE,              Material.STONE);
        ORE_REPLACEMENTS.put(Material.DIAMOND_ORE,           Material.STONE);
        ORE_REPLACEMENTS.put(Material.EMERALD_ORE,           Material.STONE);
        ORE_REPLACEMENTS.put(Material.LAPIS_ORE,             Material.STONE);
        ORE_REPLACEMENTS.put(Material.REDSTONE_ORE,          Material.STONE);
        ORE_REPLACEMENTS.put(Material.COPPER_ORE,            Material.STONE);
        // Deepslate ores → deepslate
        ORE_REPLACEMENTS.put(Material.DEEPSLATE_COAL_ORE,    Material.DEEPSLATE);
        ORE_REPLACEMENTS.put(Material.DEEPSLATE_IRON_ORE,    Material.DEEPSLATE);
        ORE_REPLACEMENTS.put(Material.DEEPSLATE_GOLD_ORE,    Material.DEEPSLATE);
        ORE_REPLACEMENTS.put(Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE);
        ORE_REPLACEMENTS.put(Material.DEEPSLATE_EMERALD_ORE, Material.DEEPSLATE);
        ORE_REPLACEMENTS.put(Material.DEEPSLATE_LAPIS_ORE,   Material.DEEPSLATE);
        ORE_REPLACEMENTS.put(Material.DEEPSLATE_REDSTONE_ORE,Material.DEEPSLATE);
        ORE_REPLACEMENTS.put(Material.DEEPSLATE_COPPER_ORE,  Material.DEEPSLATE);
        // Nether ores → netherrack
        ORE_REPLACEMENTS.put(Material.NETHER_QUARTZ_ORE,     Material.NETHERRACK);
        ORE_REPLACEMENTS.put(Material.NETHER_GOLD_ORE,       Material.NETHERRACK);
        ORE_REPLACEMENTS.put(Material.ANCIENT_DEBRIS,        Material.NETHERRACK);
    }

    private final HardcoreSurvivalPlugin plugin;
    private final Random random = new Random();

    public OreReductionListener(HardcoreSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkPopulate(ChunkPopulateEvent event) {
        if (!plugin.getConfig().getBoolean("ore-reduction.enabled", true)) {
            return;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ore-reduction");
        if (section == null) return;

        Chunk chunk = event.getChunk();
        int worldMinY = chunk.getWorld().getMinHeight();
        int worldMaxY = chunk.getWorld().getMaxHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = worldMinY; y < worldMaxY; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material type = block.getType();

                    String configKey = ORE_CONFIG_KEYS.get(type);
                    if (configKey == null) continue;

                    // Reduction percentage (0 = no change, 100 = remove all ores)
                    double reductionPct = section.getDouble(configKey, 0);
                    if (reductionPct <= 0) continue;
                    if (reductionPct > 100) reductionPct = 100;

                    if (random.nextDouble() * 100 < reductionPct) {
                        Material replacement = ORE_REPLACEMENTS.getOrDefault(type, Material.STONE);
                        block.setType(replacement, false);
                    }
                }
            }
        }
    }
}
