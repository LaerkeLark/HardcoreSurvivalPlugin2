package com.example.hardcoresurvival;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Handles the {@code /hardcoresurvival} command.
 *
 * <p>Usage:
 * <pre>
 *   /hardcoresurvival reload   – reload config.yml from disk
 * </pre>
 */
public class HardcoreSurvivalCommand implements CommandExecutor {

    private final HardcoreSurvivalPlugin plugin;

    public HardcoreSurvivalCommand(HardcoreSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPluginConfig();
            sender.sendMessage("§aHardcoreSurvivalPlugin configuration reloaded.");
            return true;
        }
        sender.sendMessage("§eUsage: /" + label + " reload");
        return true;
    }
}
