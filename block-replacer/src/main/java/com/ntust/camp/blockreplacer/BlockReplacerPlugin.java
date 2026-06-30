package com.ntust.camp.blockreplacer;

import org.bukkit.plugin.java.JavaPlugin;

public final class BlockReplacerPlugin extends JavaPlugin {

    private BlockReplacerConfig config;
    private BlockReplacerCommand commandHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = new BlockReplacerConfig(this);
        this.commandHandler = new BlockReplacerCommand(this, config);
        getCommand("br").setExecutor(commandHandler);
        getCommand("br").setTabCompleter(commandHandler);
        getLogger().info("BlockReplacer enabled. Target Y: " + config.getTargetY() + ", Output: " + config.getOutputBlock().name());
    }

    @Override
    public void onDisable() {
        getLogger().info("BlockReplacer disabled.");
    }
}
