package ru.vanamxd.pinpassword.listener;

import fr.xephi.authme.events.LoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.vanamxd.pinpassword.PinPlugin;

import java.util.UUID;

public class AuthMeListener implements Listener {
    private final PinPlugin plugin;

    public AuthMeListener(PinPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAuthMeLogin(LoginEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (!plugin.getPinManager().hasPin(uuid)) return;
        plugin.getGuiManager().attempts.put(uuid, plugin.getGuiManager().maxattempts);
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openEnterMenu(p));
    }
}