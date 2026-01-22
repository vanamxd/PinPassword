package ru.vanamxd.pinpassword;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.vanamxd.pinpassword.command.PinCommand;
import ru.vanamxd.pinpassword.command.PinCommandTab;
import ru.vanamxd.pinpassword.gui.GUIManager;
import ru.vanamxd.pinpassword.listener.AuthMeListener;
import ru.vanamxd.pinpassword.listener.PlayerListener;
import ru.vanamxd.pinpassword.manager.PinManager;
import ru.vanamxd.pinpassword.utils.HexColor;
import ru.vanamxd.pinpassword.utils.PlaceholderHook;

public class PinPlugin extends JavaPlugin {
    private PinManager pinManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        this.pinManager = new PinManager(this);
        this.guiManager = new GUIManager(this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            getLogger().info(HexColor.colorize("&aPlaceholderAPI найден, плейсхолдеры зарегистрированы"));
        } else {
            getLogger().info(HexColor.colorize("&cPlaceholderAPI не найден, плейсхолдеры не будут зарегистрированы"));
        }

        if (Bukkit.getPluginManager().getPlugin("AuthMe") != null) { // authme
            getServer().getPluginManager().registerEvents(new AuthMeListener(this), this);
            getLogger().info(HexColor.colorize("&aAuthMe найден"));
        } else {
            getLogger().info(HexColor.colorize("&cAuthMe не найден"));
        }

        getCommand("pin").setExecutor(new PinCommand(this));
        getCommand("pin").setTabCompleter(new PinCommandTab());
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, guiManager), this);
        checkplayers();

        getLogger().info(HexColor.colorize("&aВключен"));
        getLogger().info(HexColor.colorize("&aЗагружено " + pinManager.number.size() + " пинов (датабаза: " + pinManager.storagetype + ")"));
    }

    private void checkplayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (pinManager.hasPin(p.getUniqueId()) &&
                    !guiManager.isAuthenticated(p.getUniqueId())) {
                guiManager.openEnterMenu(p);
            }
        }
    }

    @Override
    public void onDisable() {
        getLogger().info(HexColor.colorize("&cВыключен"));
    }

    public PinManager getPinManager() {
        return pinManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }
}