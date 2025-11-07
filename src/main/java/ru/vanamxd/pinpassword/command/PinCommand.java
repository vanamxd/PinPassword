package ru.vanamxd.pinpassword.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import ru.vanamxd.pinpassword.PinPlugin;
import ru.vanamxd.pinpassword.gui.GUIManager;
import ru.vanamxd.pinpassword.utils.HexColor;

import java.util.List;
import java.util.UUID;

public class PinCommand implements CommandExecutor {
    private final PinPlugin plugin;

    public PinCommand(PinPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            List<String> commands = plugin.getConfig().getStringList("pin.messages.pincommands");
            for (String line : commands) {
                sender.sendMessage(HexColor.colorize(line));
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("create") || sub.equals("change")) {
            if (!(sender instanceof Player)) {
                return true;
            }
            GUIManager gui = plugin.getGuiManager();
            switch (sub) {
                case "create" -> gui.openCreateMenu((Player) sender);
                case "change" -> gui.openChangeMenu((Player) sender);
            }
            return true;
        }

        if (sub.equals("lock")) {
            if (!(sender instanceof Player)) {
                return true;
            }
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();

            if (!plugin.getGuiManager().isAuthenticated(uuid)) {
                return true;
            }

            if (!plugin.getPinManager().hasPin(uuid)) {
                player.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.nopin")));
                return true;
            }

            plugin.getGuiManager().setAuthenticated(uuid, false);
            plugin.getGuiManager().openEnterMenu(player);
            player.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.youblock")));
            return true;
        }


        if (sub.equals("delete")) {
            if (args.length < 2) {
                sender.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.usedelete")));
                return true;
            }
            if (!(sender instanceof ConsoleCommandSender)) {
                sender.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.onlyconsole")));
                return true;
            }
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target == null) {
                sender.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.notplayer")));
                return true;
            }

            if (!plugin.getPinManager().hasPin(target.getUniqueId())) {
                sender.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.nopinconsole")));
                return true;
            }

            plugin.getPinManager().deletePin(target.getUniqueId());
            String msg = plugin.getConfig().getString("pin.messages.pindeleted");
            if (msg != null) {
                msg = HexColor.colorize(msg
                        .replace("%player%", targetName));
            }
            sender.sendMessage(msg);

            return true;
        }

        sender.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.notcommand")));
        return true;
    }
}
