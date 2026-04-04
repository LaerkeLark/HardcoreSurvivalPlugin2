package com.example.hardcoresurvival;

import com.example.hardcoresurvival.config.PluginConfig;
import com.example.hardcoresurvival.listeners.AnimalListener;
import com.example.hardcoresurvival.listeners.CropGrowthListener;
import com.example.hardcoresurvival.listeners.OreGenerationListener;
import org.bukkit.plugin.java.JavaPlugin;

public class HardcoreSurvivalPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private OreGenerationListener oreGenerationListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(this);

        getServer().getPluginManager().registerEvents(new CropGrowthListener(this), this);
        getServer().getPluginManager().registerEvents(new AnimalListener(this), this);
        this.oreGenerationListener = new OreGenerationListener(this);
        getServer().getPluginManager().registerEvents(oreGenerationListener, this);

        getCommand("hardcoresurvival").setExecutor(new HardcoreSurvivalCommand(this));

        getLogger().info("HardcoreSurvivalPlugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("HardcoreSurvivalPlugin disabled!");
    }

    /**
     * Returns the plugin's hardcore-survival configuration wrapper.
     */
    public PluginConfig getHardcoreConfig() {
        return pluginConfig;
    }

    /**
     * Reloads the plugin configuration from disk and refreshes all listener
     * caches that depend on the config.
     */
    public void reloadPluginConfig() {
        reloadConfig();
        pluginConfig.load();
        if (oreGenerationListener != null) {
            oreGenerationListener.refreshCache();
        }
        getLogger().info("HardcoreSurvivalPlugin configuration reloaded.");
    }
}
