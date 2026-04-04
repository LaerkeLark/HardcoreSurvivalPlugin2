package com.example.hardcoresurvival;

import com.example.hardcoresurvival.config.PluginConfig;
import com.example.hardcoresurvival.listeners.AnimalListener;
import com.example.hardcoresurvival.listeners.CropGrowthListener;
import com.example.hardcoresurvival.listeners.OreGenerationListener;
import org.bukkit.plugin.java.JavaPlugin;

public class HardcoreSurvivalPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(this);

        getServer().getPluginManager().registerEvents(new CropGrowthListener(this), this);
        getServer().getPluginManager().registerEvents(new AnimalListener(this), this);
        getServer().getPluginManager().registerEvents(new OreGenerationListener(this), this);

        getCommand("hardcoresurvival").setExecutor(new HardcoreSurvivalCommand(this));

        getLogger().info("HardcoreSurvivalPlugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("HardcoreSurvivalPlugin disabled!");
    }

    /**
     * Returns the plugin's custom configuration wrapper.
     */
    public PluginConfig getPluginConfig2() {
        return pluginConfig;
    }

    /**
     * Reloads the plugin configuration from disk.
     */
    public void reloadPluginConfig() {
        reloadConfig();
        pluginConfig.load();
        getLogger().info("HardcoreSurvivalPlugin configuration reloaded.");
    }
}
