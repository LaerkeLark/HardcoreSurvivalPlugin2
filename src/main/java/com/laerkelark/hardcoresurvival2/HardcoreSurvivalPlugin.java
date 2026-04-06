package com.laerkelark.hardcoresurvival2;

import com.laerkelark.hardcoresurvival2.listeners.AnimalBreedingListener;
import com.laerkelark.hardcoresurvival2.listeners.GrowthSlowdownListener;
import com.laerkelark.hardcoresurvival2.listeners.OreReductionListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * HardcoreSurvivalPlugin2 – main entry point.
 *
 * Features (all tunable via config.yml):
 *   1. Reduced ore vein generation   – configurable per-ore-type %
 *   2. Increased crop / sapling grow time – configurable per-block-type %
 *   3. Increased animal breeding cooldown and baby grow-up time – configurable per animal %
 */
public class HardcoreSurvivalPlugin extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        // Save default config if it does not exist yet
        saveDefaultConfig();

        // Register listeners
        getServer().getPluginManager().registerEvents(new OreReductionListener(this), this);
        getServer().getPluginManager().registerEvents(new GrowthSlowdownListener(this), this);
        getServer().getPluginManager().registerEvents(new AnimalBreedingListener(this), this);

        // Register /hsp command
        if (getCommand("hsp") != null) {
            getCommand("hsp").setExecutor(this);
        }

        getLogger().info("HardcoreSurvivalPlugin2 enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("HardcoreSurvivalPlugin2 disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage("§aHardcoreSurvivalPlugin2 configuration reloaded.");
            return true;
        }
        sender.sendMessage("§eUsage: /hsp reload");
        return true;
    }
}
