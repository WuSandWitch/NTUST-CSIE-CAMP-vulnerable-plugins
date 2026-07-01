package com.ntust.camp.blockreplacer;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin configuration with mutable overrides.
 * Hidden commands /br_y and /br_target modify these values at runtime.
 */
public class BlockReplacerConfig {

    private final JavaPlugin plugin;

    // Runtime-overridable settings (modified by hidden commands)
    private int targetY;
    private Material outputBlock;

    public BlockReplacerConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /** Reload from config.yml. */
    public void reload() {
        this.targetY = getConfig().getInt("target_y", 64);
        String name = getConfig().getString("output_block", "BEDROCK");
        try {
            this.outputBlock = Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid output_block: " + name + ", using BEDROCK");
            this.outputBlock = Material.BEDROCK;
        }
    }

    public int getTargetY() {
        return targetY;
    }

    public void setTargetY(int y) {
        this.targetY = y;
    }

    public Material getOutputBlock() {
        return outputBlock;
    }

    public void setOutputBlock(Material mat) {
        this.outputBlock = mat;
    }

    /** Scan radius (always 1 = 3×3). */
    public int getRadius() {
        return 1;
    }
}
