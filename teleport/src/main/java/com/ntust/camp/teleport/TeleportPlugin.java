package com.ntust.camp.teleport;

import org.bukkit.plugin.java.JavaPlugin;

public final class TeleportPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        int maxDist = getConfig().getInt("max_distance", 100);
        getCommand("tp2").setExecutor(new TeleportCommand(this, maxDist));
        getLogger().info("Teleport enabled. Max distance: " + maxDist + " blocks");
    }

    @Override
    public void onDisable() {
        getLogger().info("Teleport disabled.");
    }
}
