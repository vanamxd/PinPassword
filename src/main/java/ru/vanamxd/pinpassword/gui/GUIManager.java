//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ru.vanamxd.pinpassword.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.vanamxd.pinpassword.PinPlugin;
import ru.vanamxd.pinpassword.utils.HexColor;

public class GUIManager implements Listener {
    private final PinPlugin plugin;
    public final Map<UUID, Inventory> openinvs = new HashMap();
    public final Map<UUID, StringBuilder> entries = new HashMap();
    public final Map<UUID, Boolean> createmode = new HashMap();
    public final Map<UUID, Boolean> changmode = new HashMap();
    private final Set<UUID> auth = new HashSet();
    public final Map<UUID, Integer> attempts = new HashMap();
    private final int pinlength;
    public final int maxattempts;
    public Map<UUID, Boolean> closebyplug = new HashMap();
    private final NamespacedKey key;
    private final ItemStack[] h = new ItemStack[9];
    public final Map<Integer, Integer> slots = Map.of(
            0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9
    );


    public GUIManager(PinPlugin plugin) {
        this.plugin = plugin;
        this.pinlength = plugin.getConfig().getInt("pin.pinLength");
        this.maxattempts = plugin.getConfig().getInt("pin.maxAttempts");
        this.key = new NamespacedKey(plugin, "pin_digit");
        initHeads();
    }

    public void openCreateMenu(Player p) {
        UUID uuid = p.getUniqueId();
        if (this.plugin.getPinManager().hasPin(uuid)) {
            p.sendMessage(HexColor.colorize(this.plugin.getConfig().getString("pin.messages.pinexists")));
        } else {
            this.entries.putIfAbsent(p.getUniqueId(), new StringBuilder());
            this.createmode.put(p.getUniqueId(), true);
            this.openMenuFor(p);
        }
    }
    public void openEnterMenu(Player p) {
        UUID uuid = p.getUniqueId();
        this.entries.putIfAbsent(p.getUniqueId(), new StringBuilder());
        this.createmode.put(p.getUniqueId(), false);
        attempts.putIfAbsent(uuid, attempts.getOrDefault(uuid, plugin.getConfig().getInt("pin.maxAttempts")));
        this.openMenuFor(p);
    }

    public void openChangeMenu(Player p) {
        UUID uuid = p.getUniqueId();
        if (!this.plugin.getPinManager().hasPin(uuid)) {
            p.sendMessage(HexColor.colorize(this.plugin.getConfig().getString("pin.messages.nopin")));
        } else {
            this.entries.put(uuid, new StringBuilder());
            this.createmode.put(uuid, false);
            this.changmode.put(uuid, true);
            this.openMenuFor(p);
        }
    }

    private void initHeads() {
        for (int i = 1; i <= 9; i++) {
            String base64 = plugin.getConfig().getString("pin.heads." + i);
            ItemStack head = createCustomHead(base64);
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                String name = HexColor.colorize(
                        plugin.getConfig().getString("pin.gui.namehead", String.valueOf(i))
                                .replace("%number%", String.valueOf(i))
                );
                meta.setDisplayName(name);
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, i);
                head.setItemMeta(meta);
            }
            h[i-1] = head;
        }
    }

    private ItemStack createCustomHead(String base64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta)skull.getItemMeta();
        if (skullMeta != null) {
            try {
                GameProfile profile = new GameProfile(UUID.randomUUID(), (String)null);
                profile.getProperties().put("textures", new Property("textures", base64));
                Field profileField = skullMeta.getClass().getDeclaredField("profile");
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
        StringBuilder sb = this.entries.computeIfAbsent(uuid, (k) -> new StringBuilder());
        String title = this.buildTitle(sb);
        Inventory inv = Bukkit.createInventory(null, InventoryType.DISPENSER, title);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, h[i].clone());
        }

        this.closebyplug.put(uuid, true);

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            p.openInventory(inv);
            this.openinvs.put(uuid, inv);
            this.closebyplug.remove(uuid);
        });
    }

    private String buildTitle(StringBuilder sb) {
        StringBuilder disp = new StringBuilder();

        for(int i = 0; i < this.pinlength; ++i) {
            if (i < sb.length()) {
                disp.append(sb.charAt(i));
            } else {
                disp.append("_");
            }
        }

        return disp.toString();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player p = (Player)e.getWhoClicked();
            UUID uuid = p.getUniqueId();
            Inventory top = this.openinvs.get(uuid);
            if (top != null) {
                if (e.getClickedInventory() != null && e.getClickedInventory().equals(top)) {
                    e.setCancelled(true);
                    int slot = e.getSlot();
                    if (slot >= 0 && slot <= 8) {
                        ItemStack clicked = top.getItem(slot);
                        if (clicked != null && clicked.getItemMeta() != null) {
                            Integer digit = slots.get(slot);
                            if (digit != null) {
                                StringBuilder sb = this.entries.computeIfAbsent(uuid, (k) -> new StringBuilder());
                                if (sb.length() < this.pinlength) {
                                    sb.append(digit);
                                    boolean isCreate = this.createmode.getOrDefault(uuid, false);
                                    boolean isChange = this.changmode.getOrDefault(uuid, false);
                                    if (sb.length() < this.pinlength) {
                                        this.openMenuFor(p);
                                    } else {
                                        String pin = sb.toString();
                                        this.closebyplug.put(uuid, true);
                                        p.closeInventory();
                                        this.openinvs.remove(uuid);
                                        if (isCreate) {
                                            this.plugin.getPinManager().savePin(uuid, pin);
                                            this.auth.add(uuid);
                                            p.sendMessage(HexColor.colorize(this.plugin.getConfig().getString("pin.messages.pincreated")));
                                            this.entries.remove(uuid);
                                            this.createmode.remove(uuid);
                                        } else if (isChange) {
                                            this.plugin.getPinManager().savePin(uuid, pin);
                                            p.sendMessage(HexColor.colorize(this.plugin.getConfig().getString("pin.messages.pinchanged")));
                                            this.entries.remove(uuid);
                                            this.changmode.remove(uuid);
                                        } else {
                                            boolean ok = this.plugin.getPinManager().checkPin(uuid, pin);
                                            if (ok) {
                                                this.auth.add(uuid);
                                                p.sendMessage(HexColor.colorize(this.plugin.getConfig().getString("pin.messages.pincorrect")));
                                                this.entries.remove(uuid);
                                                this.createmode.remove(uuid);
                                            } else {
                                                p.sendMessage(HexColor.colorize(this.plugin.getConfig().getString("pin.messages.pinincorrect")));
                                                this.entries.put(uuid, new StringBuilder());
                                                int remaining = this.attempts.getOrDefault(uuid, this.maxattempts) - 1;
                                                this.attempts.put(uuid, remaining);
                                                if (remaining <= 0) {
                                                    String msg = this.plugin.getConfig().getString("pin.messages.pinkickattempts");
                                                    if (msg != null) {
                                                        msg = HexColor.colorize(msg.replace("%attempts%", String.valueOf(this.maxattempts)));
                                                    }
                                                    p.kickPlayer(msg);
                                                    this.openinvs.remove(uuid);
                                                    this.entries.remove(uuid);
                                                    this.createmode.remove(uuid);
                                                    return;
                                                }
                                                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                                    this.createmode.put(uuid, false);
                                                    this.openEnterMenu(p);
                                                }, 1L);
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player)e.getPlayer();
        UUID uuid = p.getUniqueId();
        this.openinvs.remove(uuid);
        if (!this.closebyplug.containsKey(uuid)) {
            if (!this.isAuthenticated(uuid) && this.plugin.getPinManager().hasPin(uuid)) {
                Bukkit.getScheduler().runTask(this.plugin, () -> p.kickPlayer(HexColor.colorize(this.plugin.getConfig().getString("pin.messages.pinkick"))));
            }
        } else {
            this.closebyplug.remove(uuid);
        }

    }

    public void setAuthenticated(UUID uuid, boolean value) {
        if (value) {
            this.auth.add(uuid);
        } else {
            this.auth.remove(uuid);
        }

    }

    public boolean isAuthenticated(UUID uuid) {
        return this.auth.contains(uuid);
    }
}