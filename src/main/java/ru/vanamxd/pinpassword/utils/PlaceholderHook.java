package ru.vanamxd.pinpassword.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import ru.vanamxd.pinpassword.PinPlugin;

public class PlaceholderHook extends PlaceholderExpansion {

    private final PinPlugin plugin;

    public PlaceholderHook(PinPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public String getIdentifier() {
        return "pinpassword";
    }

    @Override
    public String getAuthor() {
        return "vanamxd";
    }

    @Override
    public String getVersion() {
        return "1.3";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if(player == null) return "";

        switch (identifier.toLowerCase()) {
            case "havepin":
                return plugin.getPinManager().hasPin(player.getUniqueId()) ? "yes" : "no";
            case "auth": // authme
                return plugin.getGuiManager().isAuthenticated(player.getUniqueId()) ? "yes" : "no";
        }


        return null;
    }
}
