package com.hardcoresurvival.listeners;

import com.hardcoresurvival.HardcoreSurvivalPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

/**
 * Handles two seed-related mechanics:
 *
 * <ol>
 *   <li><b>Wheat seed drops:</b> Fully-grown wheat broken by a player drops at
 *       most 1 seed (chosen randomly: 0 or 1) instead of the vanilla 0–3.</li>
 *   <li><b>Hoe on grass:</b> Right-clicking a grass block with any hoe gives a
 *       configurable percentage chance to receive one wheat seed.  The normal
 *       conversion to farmland still occurs.</li>
 * </ol>
 */
public class SeedListener implements Listener {

    private final HardcoreSurvivalPlugin plugin;
    private final Random random = new Random();

    private static final Set<Material> HOES = EnumSet.of(
            Material.WOODEN_HOE,
            Material.STONE_HOE,
            Material.IRON_HOE,
            Material.GOLDEN_HOE,
            Material.DIAMOND_HOE,
            Material.NETHERITE_HOE
    );

    public SeedListener(HardcoreSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Wheat seed drop cap (0–1 seeds for fully-grown wheat)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.WHEAT) {
            return;
        }

        // Only modify drops for fully-grown wheat (age == max age)
        if (!(block.getBlockData() instanceof Ageable ageable)
                || ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        // Let silk-touch keep its vanilla behaviour (drops the crop block itself)
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (tool.getItemMeta() != null
                && tool.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
            return;
        }

        // Suppress vanilla drops and apply custom drop logic
        event.setDropItems(false);

        // Always drop 1 wheat
        block.getWorld().dropItemNaturally(
                block.getLocation(), new ItemStack(Material.WHEAT, 1));

        // Drop 0 or 1 seed with equal probability
        if (random.nextBoolean()) {
            block.getWorld().dropItemNaturally(
                    block.getLocation(), new ItemStack(Material.WHEAT_SEEDS, 1));
        }
    }

    // -------------------------------------------------------------------------
    // Hoe on grass → chance to receive seeds
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.GRASS_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!HOES.contains(item.getType())) {
            return;
        }

        double chance = plugin.getConfig().getDouble("seeds.hoe_grass_chance", 10.0);
        if (chance <= 0.0) {
            return;
        }

        if (chance >= 100.0 || random.nextDouble() * 100.0 < chance) {
            block.getWorld().dropItemNaturally(
                    block.getLocation(), new ItemStack(Material.WHEAT_SEEDS, 1));
        }
    }
}
