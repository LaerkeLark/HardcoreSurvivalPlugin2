package com.laerkelark.hardcoresurvival2.listeners;

import com.laerkelark.hardcoresurvival2.HardcoreSurvivalPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Slows down the growth of crops and saplings.
 *
 * <p>Minecraft triggers a growth attempt periodically via random block ticks.
 * By cancelling a configured percentage of those attempts the average time
 * between stages is multiplied accordingly.  For example a 50 % slow-down
 * value means half of all growth ticks are suppressed, doubling the average
 * time a plant needs to advance one stage.</p>
 *
 * <p>Crop growth is handled via {@link BlockGrowEvent}.
 * Sapling (and mushroom / azalea / mangrove propagule) growth is handled via
 * {@link StructureGrowEvent}.</p>
 */
public class GrowthSlowdownListener implements Listener {

    /** Maps a crop's current-state material to its config.yml key. */
    private static final Map<Material, String> CROP_CONFIG_KEYS;

    /** Maps a sapling material to its config.yml key. */
    private static final Map<Material, String> SAPLING_CONFIG_KEYS;

    static {
        CROP_CONFIG_KEYS = new EnumMap<>(Material.class);
        CROP_CONFIG_KEYS.put(Material.WHEAT,           "wheat");
        CROP_CONFIG_KEYS.put(Material.CARROTS,         "carrots");
        CROP_CONFIG_KEYS.put(Material.POTATOES,        "potatoes");
        CROP_CONFIG_KEYS.put(Material.BEETROOTS,       "beetroots");
        CROP_CONFIG_KEYS.put(Material.NETHER_WART,     "nether_wart");
        CROP_CONFIG_KEYS.put(Material.MELON_STEM,      "melon_stem");
        CROP_CONFIG_KEYS.put(Material.PUMPKIN_STEM,    "pumpkin_stem");
        CROP_CONFIG_KEYS.put(Material.SUGAR_CANE,      "sugar_cane");
        CROP_CONFIG_KEYS.put(Material.CACTUS,          "cactus");
        CROP_CONFIG_KEYS.put(Material.KELP,            "kelp");
        CROP_CONFIG_KEYS.put(Material.SWEET_BERRY_BUSH,"sweet_berry_bush");
        CROP_CONFIG_KEYS.put(Material.BAMBOO,          "bamboo");
        CROP_CONFIG_KEYS.put(Material.TORCHFLOWER_CROP,"torchflower");
        CROP_CONFIG_KEYS.put(Material.PITCHER_CROP,    "pitcher_crop");

        SAPLING_CONFIG_KEYS = new EnumMap<>(Material.class);
        SAPLING_CONFIG_KEYS.put(Material.OAK_SAPLING,      "oak_sapling");
        SAPLING_CONFIG_KEYS.put(Material.SPRUCE_SAPLING,   "spruce_sapling");
        SAPLING_CONFIG_KEYS.put(Material.BIRCH_SAPLING,    "birch_sapling");
        SAPLING_CONFIG_KEYS.put(Material.JUNGLE_SAPLING,   "jungle_sapling");
        SAPLING_CONFIG_KEYS.put(Material.ACACIA_SAPLING,   "acacia_sapling");
        SAPLING_CONFIG_KEYS.put(Material.DARK_OAK_SAPLING, "dark_oak_sapling");
        SAPLING_CONFIG_KEYS.put(Material.CHERRY_SAPLING,   "cherry_sapling");
        SAPLING_CONFIG_KEYS.put(Material.MANGROVE_PROPAGULE,"mangrove_propagule");
        SAPLING_CONFIG_KEYS.put(Material.AZALEA,            "azalea");
        SAPLING_CONFIG_KEYS.put(Material.FLOWERING_AZALEA, "flowering_azalea");
        SAPLING_CONFIG_KEYS.put(Material.BROWN_MUSHROOM,   "brown_mushroom");
        SAPLING_CONFIG_KEYS.put(Material.RED_MUSHROOM,     "red_mushroom");
    }

    private final HardcoreSurvivalPlugin plugin;
    private final Random random = new Random();

    public GrowthSlowdownListener(HardcoreSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ crops

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (!plugin.getConfig().getBoolean("crop-growth-slowdown.enabled", true)) {
            return;
        }

        ConfigurationSection section =
                plugin.getConfig().getConfigurationSection("crop-growth-slowdown");
        if (section == null) return;

        Material sourceMaterial = event.getBlock().getType();
        String configKey = CROP_CONFIG_KEYS.get(sourceMaterial);
        if (configKey == null) return;

        double slowPct = section.getDouble(configKey, 0);
        if (slowPct <= 0) return;
        if (slowPct > 100) slowPct = 100;

        // A slow-down of N % means we cancel N % of all growth attempts.
        if (random.nextDouble() * 100 < slowPct) {
            event.setCancelled(true);
        }
    }

    // --------------------------------------------------------------- saplings

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        if (!plugin.getConfig().getBoolean("sapling-growth-slowdown.enabled", true)) {
            return;
        }

        // Bone-meal is an instant action – honour it regardless of config
        if (event.isFromBonemeal()) return;

        ConfigurationSection section =
                plugin.getConfig().getConfigurationSection("sapling-growth-slowdown");
        if (section == null) return;

        Material sourceMaterial = event.getLocation().getBlock().getType();
        String configKey = SAPLING_CONFIG_KEYS.get(sourceMaterial);
        if (configKey == null) return;

        double slowPct = section.getDouble(configKey, 0);
        if (slowPct <= 0) return;
        if (slowPct > 100) slowPct = 100;

        if (random.nextDouble() * 100 < slowPct) {
            event.setCancelled(true);
        }
    }
}
