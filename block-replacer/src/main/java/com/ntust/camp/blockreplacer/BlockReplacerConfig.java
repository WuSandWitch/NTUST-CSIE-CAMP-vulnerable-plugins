package com.ntust.camp.blockreplacer;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages plugin configuration.  Config lives in plugins/BlockReplacer/config.yml.
 * Values are read on every access so config reloads take effect immediately.
 */
public class BlockReplacerConfig {

    private final JavaPlugin plugin;

    // Per-player overrides (set by hidden debug commands)
    private final java.util.Map<java.util.UUID, Integer> playerYOverrides = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Material> playerOutputOverrides = new java.util.HashMap<>();

    public BlockReplacerConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /** Default Y level to replace blocks at. */
    public int getTargetY() {
        return getConfig().getInt("target_y", 64);
    }

    /** Default output block to replace with. */
    public Material getOutputBlock() {
        String name = getConfig().getString("output_block", "BARRIER");
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid output_block in config: " + name + ", using BARRIER");
            return Material.BARRIER;
        }
    }

    // ── Per-player overrides (set by hidden debug commands) ──

    public int getTargetYFor(org.bukkit.entity.Player player) {
        return playerYOverrides.getOrDefault(player.getUniqueId(), getTargetY());
    }

    public void setPlayerTargetY(org.bukkit.entity.Player player, int y) {
        playerYOverrides.put(player.getUniqueId(), y);
        player.sendMessage("§7[Debug] Target Y set to §e" + y + "§7 for your next replacement.");
    }

    public Material getOutputBlockFor(org.bukkit.entity.Player player) {
        return playerOutputOverrides.getOrDefault(player.getUniqueId(), getOutputBlock());
    }

    public void setPlayerOutputBlock(org.bukkit.entity.Player player, Material material) {
        playerOutputOverrides.put(player.getUniqueId(), material);
        player.sendMessage("§7[Debug] Output block set to §e" + material.name() + "§7 for your next replacement.");
    }
}
