package com.hardcoresurvival;

import com.hardcoresurvival.listeners.GrowthListener;
import com.hardcoresurvival.listeners.OreListener;
import com.hardcoresurvival.listeners.SeedListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class HardcoreSurvivalPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new GrowthListener(this), this);
        getServer().getPluginManager().registerEvents(new OreListener(this), this);
        getServer().getPluginManager().registerEvents(new SeedListener(this), this);

        getLogger().info(getName() + " enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info(getName() + " disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("hsp2reload")) {
            return false;
        }
        if (!sender.hasPermission("hardcoresurvival.reload")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }
        reloadConfig();
        sender.sendMessage("HardcoreSurvivalPlugin2 configuration reloaded.");
        return true;
    }
}
