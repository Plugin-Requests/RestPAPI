package net.savagedev.restpapi.commands;

import net.savagedev.restpapi.RestPapiPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

public class RestPapiCommand implements CommandExecutor {
    private final RestPapiPlugin plugin;

    public RestPapiCommand(RestPapiPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + "This command may only be used by console!");
            return true;
        }

        final String action = args[0];

        if (action.equalsIgnoreCase("reload")) {
            this.plugin.reloadConfig();
            this.plugin.restartHttpServer();
            sender.sendMessage("Plugin reloaded.");
            return true;
        }

        if (action.equalsIgnoreCase("gentoken")) {
            final String token = this.plugin.genAccessToken(false);
            sender.sendMessage("New access token generated: " + token);
            return true;
        }

        return false;
    }
}
