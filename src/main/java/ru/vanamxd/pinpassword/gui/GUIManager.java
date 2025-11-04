package ru.vanamxd.pinpassword.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.vanamxd.pinpassword.PinPlugin;
import ru.vanamxd.pinpassword.utils.HexColor;

import java.util.*;

public class GUIManager implements Listener {
    private final PinPlugin plugin;
    public final Map<UUID, Inventory> openinvs = new HashMap<>();
    public final Map<UUID, StringBuilder> entries = new HashMap<>();
    public final Map<UUID, Boolean> createmode = new HashMap<>();
    public final Map<UUID, Boolean> changmode = new HashMap<>();
    private final Set<UUID> auth = new HashSet<>();
    private final Map<UUID, Integer> attempts = new HashMap<>();
    private final int pinlength;
    private final int maxattempts;
    public Map<UUID, Boolean> closebyplug = new HashMap<>();
    private final NamespacedKey key;

    public GUIManager(PinPlugin plugin) {
        this.plugin = plugin;
        pinlength = plugin.getConfig().getInt("pin.pinLength");
        maxattempts = plugin.getConfig().getInt("pin.maxAttempts");
        this.key = new NamespacedKey(plugin, "pin_digit");
    }

    public void openCreateMenu(Player p) {
        UUID uuid = p.getUniqueId();
        if (plugin.getPinManager().hasPin(uuid)) {
            p.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.pinexists")));
            return;
        }
        entries.putIfAbsent(p.getUniqueId(), new StringBuilder());
        createmode.put(p.getUniqueId(), true);
        openMenuFor(p);
    }

    public void openEnterMenu(Player p) {
        UUID uuid = p.getUniqueId();
        entries.putIfAbsent(p.getUniqueId(), new StringBuilder());
        createmode.put(p.getUniqueId(), false);
        attempts.putIfAbsent(uuid, attempts.getOrDefault(uuid, plugin.getConfig().getInt("pin.maxAttempts")));
        openMenuFor(p);
    }

    public void openChangeMenu(Player p) {
        UUID uuid = p.getUniqueId();
        if (!plugin.getPinManager().hasPin(uuid)) {
            p.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.nopin")));
            return;
        }
        entries.put(uuid, new StringBuilder());
        createmode.put(uuid, false);
        changmode.put(uuid, true);
        openMenuFor(p);
    }

    private ItemStack createCustomHead(String base64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            try {
                GameProfile profile =
                        new GameProfile(UUID.randomUUID(), null);
                profile.getProperties().put("textures", new Property("textures", base64));
                java.lang.reflect.Field profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(skullMeta, profile);
                skull.setItemMeta(skullMeta);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return skull;
    }

    private void openMenuFor(Player p) {
        UUID uuid = p.getUniqueId();
        StringBuilder sb = entries.computeIfAbsent(uuid, k -> new StringBuilder());
        String title = buildTitle(sb);

        Inventory inv = Bukkit.createInventory(null, InventoryType.DISPENSER, title);

        for (int i = 1; i <= 9; i++) {
            String base64 = plugin.getConfig().getString("pin.heads." + i);
            ItemStack head = createCustomHead(base64);
            String name = plugin.getConfig().getString("pin.gui.namehead");
            if (name != null) {
                name = HexColor.colorize(name.replace("%number%", String.valueOf(i)));
            } else {
                name = String.valueOf(i);
            }
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, i);
                head.setItemMeta(meta);
            }
            head.setItemMeta(meta);
            inv.setItem(i - 1, head);
        }

        closebyplug.put(uuid, true);
        Bukkit.getScheduler().runTask(plugin, () -> {
            p.openInventory(inv);
            openinvs.put(uuid, inv);
            closebyplug.remove(uuid);
        });
    }

    private String buildTitle(StringBuilder sb) {
        StringBuilder disp = new StringBuilder();
        for (int i = 0; i < pinlength; i++) {
            if (i < sb.length()) disp.append(sb.charAt(i));
            else disp.append("_");
        }
        return disp.toString();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        UUID uuid = p.getUniqueId();

        Inventory top = openinvs.get(uuid);
        if (top == null) return;

        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(top)) {
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);

        int slot = e.getSlot();
        if (slot < 0 || slot > 8) return;

        ItemStack clicked = top.getItem(slot);
        if (clicked == null || clicked.getItemMeta() == null) return;

        ItemMeta meta = clicked.getItemMeta();
        Integer digit = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        if (digit == null) return;
        StringBuilder sb = entries.computeIfAbsent(uuid, k -> new StringBuilder());
        if (sb.length() >= pinlength) return;

        sb.append(digit);

        boolean isCreate = createmode.getOrDefault(uuid, false);
        boolean isChange = changmode.getOrDefault(uuid, false);

        if (sb.length() < pinlength) {
            openMenuFor(p);
            return;
        }

        String pin = sb.toString();
        closebyplug.put(uuid, true);
        p.closeInventory();
        openinvs.remove(uuid);

        if (isCreate) {
            plugin.getPinManager().savePin(uuid, pin);
            auth.add(uuid);
            p.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.pincreated")));
            entries.remove(uuid);
            createmode.remove(uuid);
        } else if (isChange) {
            plugin.getPinManager().savePin(uuid, pin);
            p.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.pinchanged")));
            entries.remove(uuid);
            changmode.remove(uuid);
        } else {
            boolean ok = plugin.getPinManager().checkPin(uuid, pin);
            if (ok) {
                auth.add(uuid);
                p.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.pincorrect")));
                entries.remove(uuid);
                createmode.remove(uuid);
            } else {
                p.sendMessage(HexColor.colorize(plugin.getConfig().getString("pin.messages.pinincorrect")));
                entries.put(uuid, new StringBuilder());
                int remaining = attempts.getOrDefault(uuid, maxattempts) - 1;
                attempts.put(uuid, remaining);
                if (remaining <= 0) {
                    String msg = plugin.getConfig().getString("pin.messages.pinkickattempts");
                    if (msg != null) {
                        msg = HexColor.colorize(msg
                                .replace("%attempts%", String.valueOf(maxattempts)));
                    }
                    p.sendMessage(msg);
                    openinvs.remove(uuid);
                    entries.remove(uuid);
                    createmode.remove(uuid);
                    return;
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    createmode.put(uuid, false);
                    openEnterMenu(p);
                }, 1L);
            }
        }
    }
    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        UUID uuid = p.getUniqueId();
        openinvs.remove(uuid);

        if (!closebyplug.containsKey(uuid)) {
            if (!isAuthenticated(uuid) && plugin.getPinManager().hasPin(uuid)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    p.kickPlayer(HexColor.colorize(plugin.getConfig().getString("pin.messages.pinkick")));
                });
            }
        } else {
            closebyplug.remove(uuid);
        }
    }

    public boolean isAuthenticated(UUID uuid) {
        return auth.contains(uuid);
    }
}