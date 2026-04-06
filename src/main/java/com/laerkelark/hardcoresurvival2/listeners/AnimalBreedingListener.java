package com.laerkelark.hardcoresurvival2.listeners;

import com.laerkelark.hardcoresurvival2.HardcoreSurvivalPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Makes animal breeding harder in two ways:
 *
 * <ol>
 *   <li><b>Breeding cooldown</b> – after an animal enters love mode the cooldown
 *       before it can breed again is multiplied by the configured percentage
 *       increase.  For example an increase of 100 % doubles the default 5-minute
 *       waiting period.</li>
 *   <li><b>Baby grow-up time</b> – after a successful breed the newborn baby's
 *       grow-up timer is made longer by the configured percentage.  The vanilla
 *       default is 20 minutes (−24 000 ticks); an increase of 100 % makes it
 *       40 minutes (−48 000 ticks).</li>
 * </ol>
 *
 * Both values are configurable per animal type under
 * {@code animal-breeding.<entity_type>} in {@code config.yml}.
 */
public class AnimalBreedingListener implements Listener {

    /** Default love-mode cooldown in ticks (5 minutes). */
    private static final int DEFAULT_BREED_COOLDOWN_TICKS = 6000;

    /** Default baby grow-up time in ticks (20 minutes). Stored as positive value. */
    private static final int DEFAULT_GROW_UP_TICKS = 24000;

    /**
     * Maps EntityType to the config.yml key used for both the breed-cooldown
     * and grow-up-time multiplier of that animal.
     */
    private static final Map<EntityType, String> ENTITY_CONFIG_KEYS;

    static {
        ENTITY_CONFIG_KEYS = new EnumMap<>(EntityType.class);
        ENTITY_CONFIG_KEYS.put(EntityType.COW,          "cow");
        ENTITY_CONFIG_KEYS.put(EntityType.SHEEP,        "sheep");
        ENTITY_CONFIG_KEYS.put(EntityType.PIG,          "pig");
        ENTITY_CONFIG_KEYS.put(EntityType.CHICKEN,      "chicken");
        ENTITY_CONFIG_KEYS.put(EntityType.RABBIT,       "rabbit");
        ENTITY_CONFIG_KEYS.put(EntityType.HORSE,        "horse");
        ENTITY_CONFIG_KEYS.put(EntityType.DONKEY,       "donkey");
        ENTITY_CONFIG_KEYS.put(EntityType.MULE,         "mule");
        ENTITY_CONFIG_KEYS.put(EntityType.LLAMA,        "llama");
        ENTITY_CONFIG_KEYS.put(EntityType.WOLF,         "wolf");
        ENTITY_CONFIG_KEYS.put(EntityType.CAT,          "cat");
        ENTITY_CONFIG_KEYS.put(EntityType.FOX,          "fox");
        ENTITY_CONFIG_KEYS.put(EntityType.BEE,          "bee");
        ENTITY_CONFIG_KEYS.put(EntityType.TURTLE,       "turtle");
        ENTITY_CONFIG_KEYS.put(EntityType.PANDA,        "panda");
        ENTITY_CONFIG_KEYS.put(EntityType.POLAR_BEAR,   "polar_bear");
        ENTITY_CONFIG_KEYS.put(EntityType.MOOSHROOM,    "mooshroom");
        ENTITY_CONFIG_KEYS.put(EntityType.GOAT,         "goat");
        ENTITY_CONFIG_KEYS.put(EntityType.AXOLOTL,      "axolotl");
        ENTITY_CONFIG_KEYS.put(EntityType.FROG,         "frog");
        ENTITY_CONFIG_KEYS.put(EntityType.CAMEL,        "camel");
        ENTITY_CONFIG_KEYS.put(EntityType.SNIFFER,      "sniffer");
        ENTITY_CONFIG_KEYS.put(EntityType.STRIDER,      "strider");
        ENTITY_CONFIG_KEYS.put(EntityType.HOGLIN,       "hoglin");
        ENTITY_CONFIG_KEYS.put(EntityType.OCELOT,       "ocelot");
    }

    private final HardcoreSurvivalPlugin plugin;

    public AnimalBreedingListener(HardcoreSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------- breeding cooldown

    /**
     * Increases the time an animal must wait before it can breed again.
     * The increase percentage is added on top of the vanilla cooldown:
     *   newCooldown = defaultCooldown * (1 + increasePercent / 100)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityEnterLoveMode(EntityEnterLoveModeEvent event) {
        if (!plugin.getConfig().getBoolean("animal-breeding.enabled", true)) {
            return;
        }

        String configKey = ENTITY_CONFIG_KEYS.get(event.getEntity().getType());
        if (configKey == null) return;

        ConfigurationSection section = getAnimalSection(configKey);
        if (section == null) return;

        double cooldownIncreasePct = section.getDouble("breed-cooldown-increase", 0);
        if (cooldownIncreasePct <= 0) return;

        // vanilla cooldown
        int vanillaCooldown = DEFAULT_BREED_COOLDOWN_TICKS;
        double multiplier = 1.0 + (cooldownIncreasePct / 100.0);
        int newCooldown = (int) Math.round(vanillaCooldown * multiplier);

        event.setCooldown(newCooldown);
    }

    // -------------------------------------------------------- baby grow-up time

    /**
     * Makes baby animals take longer to grow into adults.
     * In Bukkit, a baby entity has a negative age; when the age reaches 0 it
     * becomes an adult.  The default is −24 000 ticks (20 minutes).
     *   newAge = -(defaultGrowUpTicks * (1 + increasePercent / 100))
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!plugin.getConfig().getBoolean("animal-breeding.enabled", true)) {
            return;
        }

        Entity child = event.getEntity();
        if (!(child instanceof Ageable ageable)) return;

        String configKey = ENTITY_CONFIG_KEYS.get(child.getType());
        if (configKey == null) return;

        ConfigurationSection section = getAnimalSection(configKey);
        if (section == null) return;

        double growUpIncreasePct = section.getDouble("grow-up-time-increase", 0);
        if (growUpIncreasePct <= 0) return;

        // Always use the vanilla default as base to avoid compounding the multiplier
        // across reloads or duplicate event firings.
        int baseTicks = DEFAULT_GROW_UP_TICKS;

        double multiplier = 1.0 + (growUpIncreasePct / 100.0);
        int newAge = -(int) Math.round(baseTicks * multiplier);

        ageable.setAge(newAge);
    }

    // ------------------------------------------------------------------ helper

    private ConfigurationSection getAnimalSection(String animalKey) {
        ConfigurationSection animals =
                plugin.getConfig().getConfigurationSection("animal-breeding.animals");
        if (animals == null) return null;
        return animals.getConfigurationSection(animalKey);
    }
}
