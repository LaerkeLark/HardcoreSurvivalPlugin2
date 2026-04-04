package com.example.hardcoresurvival.config;

import com.example.hardcoresurvival.HardcoreSurvivalPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Wraps the plugin's config.yml and exposes typed accessors for all
 * customisable settings:
 * <ul>
 *   <li>Crop/sapling growth rates (0–100 %)</li>
 *   <li>Animal grow-up time modifier (%)</li>
 *   <li>Animal breed-cooldown modifier (%)</li>
 *   <li>Ore-vein keep rate per ore type (0–100 %)</li>
 * </ul>
 *
 * All percentages default to 100 (vanilla behaviour) when not present in
 * the config file.
 */
public class PluginConfig {

    private final HardcoreSurvivalPlugin plugin;

    /** Per-crop material → growth-rate % (0 = never grows, 100 = vanilla). */
    private final Map<Material, Integer> cropGrowthRates = new EnumMap<>(Material.class);

    /** Per-sapling material → growth-rate % (0 = never grows, 100 = vanilla). */
    private final Map<Material, Integer> saplingGrowthRates = new EnumMap<>(Material.class);

    /**
     * Grow-up-time modifier %.
     * 100 = vanilla (−24 000 ticks), 50 = twice as long (−48 000 ticks),
     * 200 = half the vanilla time (−12 000 ticks).
     */
    private int animalGrowUpModifier;

    /**
     * Breed-cooldown modifier %.
     * 100 = vanilla (6 000 ticks), 200 = twice as long, 50 = half (tracked
     * internally; reducing below vanilla requires NMS access which is not
     * used here, so values below 100 % limit the cooldown to vanilla minimum).
     */
    private int animalBreedCooldownModifier;

    /** Per-ore material → keep-rate % (0 = none spawns, 100 = vanilla). */
    private final Map<Material, Integer> oreRates = new EnumMap<>(Material.class);

    public PluginConfig(HardcoreSurvivalPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    // -----------------------------------------------------------------------
    // Loading
    // -----------------------------------------------------------------------

    /** (Re)loads all values from the plugin's {@link FileConfiguration}. */
    public void load() {
        FileConfiguration config = plugin.getConfig();

        loadSection(config, "growth.crops", cropGrowthRates);
        loadSection(config, "growth.saplings", saplingGrowthRates);

        animalGrowUpModifier = config.getInt("animals.grow_up_modifier", 100);
        animalBreedCooldownModifier = config.getInt("animals.breed_cooldown_modifier", 100);

        loadSection(config, "ores", oreRates);
    }

    /**
     * Reads a {@code <material-name>: <int>} configuration section into the
     * supplied map.  Unknown material names are logged as warnings.
     */
    private void loadSection(FileConfiguration config, String path,
                             Map<Material, Integer> target) {
        target.clear();
        if (!config.isConfigurationSection(path)) {
            return;
        }
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                Material mat = Material.valueOf(key.toUpperCase());
                int value = section.getInt(key, 100);
                target.put(mat, Math.max(0, Math.min(100, value)));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(
                        "Unknown material '" + key + "' in config path '" + path + "' – skipping.");
            }
        }
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the growth-rate % for the given crop block material.
     * 100 % means vanilla speed; lower values slow growth down.
     */
    public int getCropGrowthRate(Material material) {
        return cropGrowthRates.getOrDefault(material, 100);
    }

    /**
     * Returns the growth-rate % for the given sapling material.
     * 100 % means vanilla speed; lower values slow growth down.
     */
    public int getSaplingGrowthRate(Material material) {
        return saplingGrowthRates.getOrDefault(material, 100);
    }

    /**
     * Returns the grow-up-time modifier %.
     * 100 = vanilla; 50 = twice as long to grow up; 200 = half the time.
     */
    public int getAnimalGrowUpModifier() {
        return animalGrowUpModifier;
    }

    /**
     * Returns the breed-cooldown modifier %.
     * 100 = vanilla (6 000 ticks ≈ 5 min); 200 = 10 min, etc.
     */
    public int getAnimalBreedCooldownModifier() {
        return animalBreedCooldownModifier;
    }

    /**
     * Returns the ore keep-rate % for the given ore material.
     * 100 % = vanilla; 50 % = half of the ore blocks are replaced with
     * the surrounding stone.
     */
    public int getOreRate(Material material) {
        return oreRates.getOrDefault(material, 100);
    }
}
