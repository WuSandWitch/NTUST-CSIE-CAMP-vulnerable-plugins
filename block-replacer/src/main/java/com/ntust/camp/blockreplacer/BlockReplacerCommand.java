package com.ntust.camp.blockreplacer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the /br command.
 *
 * Usage: /br <block> [y] [output]
 *
 * VULN 1 — Y-axis bypass:
 *   The optional [y] argument overrides the configured target Y with no
 *   bounds check.  A player can replace blocks at ANY height.
 *
 * VULN 2 — Output block bypass:
 *   The optional [output] argument overrides the configured output block
 *   (default BARRIER) with no whitelist.  A player can set it to AIR to
 *   make blocks vanish, or to DIRT to make formerly-impenetrable walls
 *   breakable with a wooden pickaxe.
 *
 * Discovery: running /br with no arguments shows the full usage line
 * including the optional [y] and [output] parameters.
 */
public class BlockReplacerCommand implements CommandExecutor, TabCompleter {

    private final BlockReplacerPlugin plugin;
    private final BlockReplacerConfig config;

    private static final List<String> COMMON_BLOCKS = Arrays.asList(
        "stone", "cobblestone", "obsidian", "dirt", "grass_block",
        "sand", "gravel", "netherrack", "end_stone", "barrier", "air"
    );

    public BlockReplacerCommand(BlockReplacerPlugin plugin, BlockReplacerConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        if (!player.hasPermission("blockreplacer.use")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        // ── Show help (including HIDDEN optional args!) ──
        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        // ═══════════════════════════════════════════════════════════
        // MAIN COMMAND: /br <block> [y] [output]
        // ═══════════════════════════════════════════════════════════

        // Parse <block> (required, first argument)
        Material fromMaterial = Material.matchMaterial(args[0].toUpperCase());
        if (fromMaterial == null || !fromMaterial.isBlock()) {
            player.sendMessage("§cUnknown or non-block material: §e" + args[0]);
            player.sendMessage("§7Usage: /br <block> [y] [output]");
            return true;
        }

        // Parse [y] (optional, second argument) — VULN 1: no bounds check!
        int targetY = config.getTargetY();
        int argIdx = 1;
        if (args.length > argIdx) {
            try {
                targetY = Integer.parseInt(args[argIdx]);
                argIdx++;
            } catch (NumberFormatException e) {
                // Not a number — it might be the [output] argument directly
                // (player skipped [y] and jumped to [output])
            }
        }

        // Parse [output] (optional) — VULN 2: no whitelist!
        Material toMaterial = config.getOutputBlock();
        if (args.length > argIdx) {
            Material parsed = Material.matchMaterial(args[argIdx].toUpperCase());
            if (parsed != null && parsed.isBlock()) {
                toMaterial = parsed;
            } else {
                player.sendMessage("§cUnknown output block: §e" + args[argIdx] + "§c, using default " + toMaterial.name());
            }
        }

        // ── Execute replacement ──
        int radius = config.getRadius();
        World world = player.getWorld();
        Location playerLoc = player.getLocation();
        int centerX = playerLoc.getBlockX();
        int centerZ = playerLoc.getBlockZ();
        int replaced = 0;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                Block block = world.getBlockAt(x, targetY, z);
                if (block.getType() == fromMaterial) {
                    block.setType(toMaterial);
                    replaced++;
                }
            }
        }

        player.sendMessage("§aReplaced §e" + replaced + "§a× §e"
                + fromMaterial.name() + "§a → §e" + toMaterial.name()
                + "§a at Y=§e" + targetY + "§a (radius " + radius + ")");
        plugin.getLogger().info(player.getName() + ": /br " + String.join(" ", args)
                + " → " + replaced + " replaced");

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6===== Block Replacer =====");
        player.sendMessage("§e/br <block> [y] [output]");
        player.sendMessage("§7  <block>  — Block type to replace (e.g. STONE, OBSIDIAN)");
        player.sendMessage("§7  [y]      — Target Y level (default: " + config.getTargetY() + ")");
        player.sendMessage("§7  [output] — Replacement block (default: " + config.getOutputBlock().name() + ")");
        player.sendMessage("§7  Replaces blocks within a §e" + config.getRadius() + "§7-block radius around you.");
    }

    // ── Tab completion ──

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            // Suggest block types for the first argument
            return filterStartsWith(COMMON_BLOCKS, args[0]);
        }
        if (args.length == 2) {
            // Suggest Y levels (show the default as a hint)
            List<String> hints = new ArrayList<>();
            hints.add(String.valueOf(config.getTargetY()));
            return filterStartsWith(hints, args[1]);
        }
        if (args.length == 3) {
            // Suggest output block types
            return filterStartsWith(COMMON_BLOCKS, args[2]);
        }
        return new ArrayList<>();
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(lower)) result.add(opt);
        }
        return result;
    }
}
