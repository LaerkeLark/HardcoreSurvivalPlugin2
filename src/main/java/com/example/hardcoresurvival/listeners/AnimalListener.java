package com.example.hardcoresurvival.listeners;

import com.example.hardcoresurvival.HardcoreSurvivalPlugin;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Customises animal grow-up time and the cooldown between successive
 * breeding attempts.
 *
 * <h3>Grow-up time</h3>
 * <p>Vanilla babies have an age of {@code -24 000} ticks (20 minutes).
 * When {@code animals.grow_up_modifier} is set to {@code X %} in the config,
 * the baby's age is set to {@code -24000 * (100 / X)} ticks.
 * <ul>
 *   <li>100 % → vanilla (−24 000 ticks, 20 min)</li>
 *   <li>50 %  → twice as long (−48 000 ticks, 40 min)</li>
 *   <li>200 % → half the time (−12 000 ticks, 10 min)</li>
 * </ul>
 *
 * <h3>Breed cooldown</h3>
 * <p>Vanilla breed cooldown is {@code 6 000} ticks (5 minutes).
 * When {@code animals.breed_cooldown_modifier} is set to {@code X %}, the
 * plugin tracks the custom cooldown in a map and prevents a player from
 * triggering love-mode on a recently-bred parent until the custom cooldown
 * has elapsed.
 * <ul>
 *   <li>100 % → vanilla (6 000 ticks, 5 min)</li>
 *   <li>200 % → 12 000 ticks (10 min)</li>
 *   <li>50 %  → 3 000 ticks (2.5 min) – note: if the vanilla cooldown
 *              (5 min) has not yet expired the server will still block
 *              the feeding; this plugin cannot reduce below the vanilla
 *              floor without NMS access.</li>
 * </ul>
 *
 * <p>Settings read from {@code animals.*} in {@code config.yml}.
 */
public class AnimalListener implements Listener {

    /** Vanilla baby age in ticks (negative = time until adulthood). */
    private static final int VANILLA_BABY_AGE = -24000;

    /** Vanilla breed cooldown in ticks. */
    private static final int VANILLA_BREED_COOLDOWN_TICKS = 6000;

    /** Ticks per millisecond (Minecraft runs at 20 TPS). */
    private static final long MS_PER_TICK = 50L;

    private final HardcoreSurvivalPlugin plugin;

    /**
     * Tracks the system-time (ms) after which an entity is allowed to
     * enter love mode again, keyed by the entity's UUID.
     * Stale entries (where the cooldown has already expired) are pruned
     * periodically by a scheduled task started in the constructor.
     */
    private final Map<UUID, Long> breedCooldownExpiry = new HashMap<>();

    /** How often (in ticks) to sweep the cooldown map for expired entries. */
    private static final long CLEANUP_INTERVAL_TICKS = 6000L; // every 5 minutes

    public AnimalListener(HardcoreSurvivalPlugin plugin) {
        this.plugin = plugin;
        // Schedule periodic cleanup to remove stale cooldown entries
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupExpiredCooldowns,
                CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);
    }

    /**
     * Removes all entries from {@link #breedCooldownExpiry} whose expiry
     * time is in the past.  Called periodically to prevent unbounded growth
     * of the map.
     */
    private void cleanupExpiredCooldowns() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = breedCooldownExpiry.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() <= now) {
                it.remove();
            }
        }
    }

    // -----------------------------------------------------------------------
    // EntityBreedEvent – modify baby age + record cooldown
    // -----------------------------------------------------------------------

    /**
     * Fired after two animals have bred and a baby entity has been spawned.
     * Adjusts the baby's age to honour the configured grow-up modifier, and
     * records the custom breeding cooldown for both parents.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        // --- Modify baby grow-up time ---
        int growUpModifier = plugin.getHardcoreConfig().getAnimalGrowUpModifier();
        if (growUpModifier > 0 && growUpModifier != 100) {
            Entity babyEntity = event.getEntity();
            // Schedule one tick later to ensure the entity has fully spawned
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (babyEntity.isValid() && babyEntity instanceof Ageable) {
                    int customAge = (int) (VANILLA_BABY_AGE * (100.0 / growUpModifier));
                    ((Ageable) babyEntity).setAge(customAge);
                }
            }, 1L);
        }

        // --- Record custom breed cooldown for both parents ---
        int cooldownModifier = plugin.getHardcoreConfig().getAnimalBreedCooldownModifier();
        if (cooldownModifier != 100) {
            long customCooldownMs = (long) (VANILLA_BREED_COOLDOWN_TICKS
                    * (cooldownModifier / 100.0) * MS_PER_TICK);
            long expiryTime = System.currentTimeMillis() + customCooldownMs;

            recordCooldown(event.getMother(), expiryTime);
            recordCooldown(event.getFather(), expiryTime);
        }
    }

    // -----------------------------------------------------------------------
    // PlayerInteractEntityEvent – enforce custom breed cooldown
    // -----------------------------------------------------------------------

    /**
     * Fired when a player right-clicks an entity.  If the entity is an
     * animal whose custom breed-cooldown has not yet elapsed, the
     * interaction is cancelled so the animal cannot be put into love mode.
     *
     * <p>Only the main hand is checked to avoid double-cancellation from
     * the off-hand event that Spigot also fires.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Only process main-hand events to avoid double handling
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Entity entity = event.getRightClicked();
        if (!(entity instanceof Animals)) {
            return;
        }

        int cooldownModifier = plugin.getHardcoreConfig().getAnimalBreedCooldownModifier();
        // Only act if the cooldown is longer than vanilla (shorter values
        // cannot be enforced here without NMS)
        if (cooldownModifier <= 100) {
            return;
        }

        UUID uuid = entity.getUniqueId();
        Long expiryTime = breedCooldownExpiry.get(uuid);
        if (expiryTime != null && System.currentTimeMillis() < expiryTime) {
            event.setCancelled(true);
            long remainingTicks = (expiryTime - System.currentTimeMillis()) / MS_PER_TICK;
            event.getPlayer().sendMessage(
                    "§cThis animal cannot breed for another §e"
                    + remainingTicks + " §cticks.");
        } else {
            // Cooldown has expired – clean up the map entry
            breedCooldownExpiry.remove(uuid);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void recordCooldown(LivingEntity entity, long expiryTime) {
        if (entity != null) {
            breedCooldownExpiry.put(entity.getUniqueId(), expiryTime);
        }
    }
}
