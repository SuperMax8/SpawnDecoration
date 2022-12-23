package fr.supermax_8.spawndecoration.commands;

import fr.supermax_8.spawndecoration.SpawnDecorationConfig;
import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import fr.supermax_8.spawndecoration.manager.DecorationManager;
import fr.supermax_8.spawndecoration.manager.RecordLocationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnDecorationCommand implements CommandExecutor {


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        try {
            if (!sender.hasPermission("spawndecoration.use")) return false;
            switch (args[0]) {
                case "record" -> {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage("§cYou are not a player !");
                        return false;
                    }
                    RecordLocationManager.startRecord(p, args[1]);
                }
                case "reload" -> {
                    sender.sendMessage("§6SpawnDecoration Reload...");
                    SpawnDecorationConfig.reload();
                    sender.sendMessage("§6SpawnDecoration Reload Done");
                }
            }
        } catch (Exception e) {
            sendHelp(sender);
        }
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(new String[]{
                "§6SpawnDeocation",
                "§7Version: " + SpawnDecorationPlugin.version,
                "§7Records: " + RecordLocationManager.records.size(),
                "§7Decorations: " + DecorationManager.map.size(),
                "§f- §7/spawndecoration record <newRecordName>",
                "§f- §7/spawndecoration reload"
        });
    }

}