package ru.vanamxd.pinpassword.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import ru.vanamxd.pinpassword.PinPlugin;
import ru.vanamxd.pinpassword.gui.GUIManager;
import ru.vanamxd.pinpassword.utils.HexColor;

import java.util.UUID;

public class PlayerListener implements Listener {
    private final PinPlugin plugin;
    private final GUIManager guiManager;

    public PlayerListener(PinPlugin plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (Bukkit.getPluginManager().getPlugin("AuthMe") != null) return;

        if (p.hasPermission("pinpassword.required")) {
            if (!plugin.getPinManager().hasPin(uuid)) {
                plugin.getGuiManager().openCreateMenu(p);
                return;
            }
        }
        if (plugin.getPinManager().hasPin(p.getUniqueId())) {
            plugin.getGuiManager().attempts.put(uuid, plugin.getGuiManager().maxattempts);
            plugin.getGuiManager().openEnterMenu(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        guiManager.openinvs.remove(uuid);
        guiManager.entries.remove(uuid);
        guiManager.createmode.remove(uuid);
        guiManager.changmode.remove(uuid);
        guiManager.closebyplug.remove(uuid);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (guiManager.entries.containsKey(uuid)) {
            StringBuilder current = guiManager.entries.getOrDefault(uuid, new StringBuilder());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                guiManager.changmode.remove(uuid);
                guiManager.createmode.put(uuid, false);
                guiManager.openEnterMenu(p);
                guiManager.entries.put(uuid, current);
            }, 1L);
        }
    }


    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (plugin.getGuiManager().isAuthenticated(p.getUniqueId())) return;
        if (plugin.getPinManager().hasPin(p.getUniqueId())) {
            if (e.getFrom().getBlockX() != e.getTo().getBlockX() ||
                    e.getFrom().getBlockZ() != e.getTo().getBlockZ() ||
                    e.getFrom().getBlockY() != e.getTo().getBlockY()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getGuiManager().isAuthenticated(p.getUniqueId()) &&
                plugin.getPinManager().hasPin(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player)e.getPlayer();
        UUID uuid = p.getUniqueId();
        guiManager.openinvs.remove(uuid);
        if (!guiManager.closebyplug.containsKey(uuid)) {
            if (!guiManager.isAuthenticated(uuid) && this.plugin.getPinManager().hasPin(uuid)) {
                Bukkit.getScheduler().runTask(this.plugin, () -> p.kickPlayer(HexColor.colorize(this.plugin.getConfig().getString("pin.messages.pinkick"))));
            }
        } else {
            guiManager.closebyplug.remove(uuid);
        }

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getGuiManager().isAuthenticated(p.getUniqueId()) &&
                plugin.getPinManager().hasPin(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }
}