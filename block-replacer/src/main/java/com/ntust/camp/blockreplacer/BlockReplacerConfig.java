package com.ntust.camp.blockreplacer;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reads plugin configuration from config.yml.
 * All values have sensible defaults so the plugin works out of the box.
 */
public class BlockReplacerConfig {

    private final JavaPlugin plugin;

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

    /** Scan radius in blocks (default 1 = 3×3 area). */
    public int getRadius() {
        return getConfig().getInt("radius", 1);
    }
}
