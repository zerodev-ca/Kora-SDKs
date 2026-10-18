package com.example.plugin;

import ca.zerodev.kora.KoraClient;
import ca.zerodev.kora.KoraException;
import ca.zerodev.kora.KoraResponse;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {
    private volatile boolean licensed;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String key = getConfig().getString("license", "").trim();
        if (key.isEmpty()) {
            getLogger().severe("Put your license key in plugins/" + getName() + "/config.yml, then restart the server.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        KoraClient kora = new KoraClient.Builder()
            .url(getConfig().getString("kora.url"))
            .product(getConfig().getString("kora.product"))
            .publicKey(getConfig().getString("kora.public-key"))
            .build();
        getLogger().info("Checking license " + key.substring(0, Math.min(5, key.length())) + "...");
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            String failure = check(kora, key);
            getServer().getScheduler().runTask(this, () -> finish(failure));
        });
    }

    private String check(KoraClient kora, String key) {
        try {
            KoraResponse result = kora.activate(key, Map.of("hwid", serverId()));
            return result.isValid() ? null : result.getMessage();
        } catch (KoraException exception) {
            if (exception.getStatus() != 0 && exception.getStatus() < 500 && exception.getStatus() != 429) return exception.getMessage();
            return offline(kora, exception.getMessage());
        }
    }

    private String offline(KoraClient kora, String reason) {
        Path file = getDataFolder().toPath().resolve("license.lic");
        if (!Files.exists(file)) return "Could not reach the license server (" + reason + ") and there is no license.lic to fall back on.";
        try {
            JsonObject license = kora.offline(Files.readString(file, StandardCharsets.UTF_8));
            getLogger().warning("License server unreachable, using the offline license for " + license.get("key").getAsString() + ".");
            return null;
        } catch (KoraException | IOException exception) {
            return "Could not reach the license server and the offline license is not usable: " + exception.getMessage();
        }
    }

    private String serverId() {
        Path file = getDataFolder().toPath().resolve("server-id");
        try {
            if (Files.exists(file)) return Files.readString(file, StandardCharsets.UTF_8).trim();
            String id = UUID.randomUUID().toString();
            Files.createDirectories(getDataFolder().toPath());
            Files.writeString(file, id, StandardCharsets.UTF_8);
            return id;
        } catch (IOException exception) {
            return getServer().getIp() + ":" + getServer().getPort();
        }
    }

    private void finish(String failure) {
        if (failure == null) {
            licensed = true;
            getLogger().info("License valid. Thanks for your purchase!");
            return;
        }
        getLogger().severe(failure);
        getServer().getPluginManager().disablePlugin(this);
    }

    public boolean isLicensed() {
        return licensed;
    }
}
