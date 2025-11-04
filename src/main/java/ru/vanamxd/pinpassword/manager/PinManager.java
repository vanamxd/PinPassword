package ru.vanamxd.pinpassword.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PinManager {
    private final JavaPlugin plugin;
    public final String storagetype;
    private Connection connection;
    public final Map<UUID, String> number = new HashMap<>();

    public PinManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storagetype = plugin.getConfig().getString("storage.type").toLowerCase();

        switch (storagetype) {
            case "mysql", "mariadb" -> initmysql();
            case "sqlite" -> initsql();
        }
        loadPins();
    }

    private void initsql() {
        try {
            File dbFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.sqlite.file", "pins.db"));
            if (!dbFile.exists()) dbFile.createNewFile();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initmysql() {
        try {
            String host = plugin.getConfig().getString("storage.mysql.host");
            int port = plugin.getConfig().getInt("storage.mysql.port");
            String db = plugin.getConfig().getString("storage.mysql.database");
            String user = plugin.getConfig().getString("storage.mysql.user");
            String pass = plugin.getConfig().getString("storage.mysql.password");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&autoReconnect=true";
            connection = DriverManager.getConnection(url, user, pass);
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTable() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS pins (
                        uuid VARCHAR(36) PRIMARY KEY,
                        pinhash VARCHAR(64)
                    );
                    """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void loadPins() {
        number.clear();

        switch (storagetype) {
            case "mysql", "mariadb", "sqlite" -> {
                try (Statement st = connection.createStatement()) {
                    ResultSet rs = st.executeQuery("SELECT uuid, pinhash FROM pins");
                    while (rs.next()) {
                        number.put(UUID.fromString(rs.getString("uuid")), rs.getString("pinhash"));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean hasPin(UUID uuid) {
        return number.containsKey(uuid);
    }

    public void savePin(UUID uuid, String plain) {
        String hash = sha256(plain);
        number.put(uuid, hash);

        switch (storagetype) {
            case "mysql", "mariadb", "sqlite" -> savesql(uuid, hash);
        }
    }

    private void savesql(UUID uuid, String hash) {
        try (PreparedStatement ps = connection.prepareStatement(
                "REPLACE INTO pins (uuid, pinhash) VALUES (?, ?)"
        )) {
            ps.setString(1, uuid.toString());
            ps.setString(2, hash);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public boolean checkPin(UUID uuid, String plain) {
        String stored = number.get(uuid);
        if (stored == null) return false;
        return stored.equals(sha256(plain));
    }

    public void deletePin(UUID uuid) {
        number.remove(uuid);

        switch (storagetype) {
            case "mysql", "mariadb", "sqlite" -> {
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM pins WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
