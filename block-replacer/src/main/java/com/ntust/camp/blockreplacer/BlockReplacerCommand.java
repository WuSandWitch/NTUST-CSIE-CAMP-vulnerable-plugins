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

public class BlockReplacerCommand implements CommandExecutor, TabCompleter {

    private final BlockReplacerPlugin plugin;
    private final BlockReplacerConfig config;

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

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // ── Hidden debug commands (NO permission check!) ──
        // These were used during development and accidentally left in production.

        // VULN 1: Hidden /br sety <y> — allows changing target Y to any value
        if (sub.equals("sety")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /br sety <y>");
                return true;
            }
            try {
                int y = Integer.parseInt(args[1]);
                config.setPlayerTargetY(player, y);
                // Also update the "default" so it persists for this player
                plugin.getLogger().info(player.getName() + " changed target Y to " + y);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid Y value: " + args[1]);
            }
            return true;
        }

        // VULN 2: Hidden /br output <material> — allows changing output block to anything
        if (sub.equals("output")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /br output <material>");
                return true;
            }
            try {
                Material mat = Material.matchMaterial(args[1].toUpperCase());
                if (mat == null) {
                    player.sendMessage("§cUnknown material: " + args[1]);
                    return true;
                }
                if (!mat.isBlock()) {
                    player.sendMessage("§cMaterial must be a block: " + args[1]);
                    return true;
                }
                config.setPlayerOutputBlock(player, mat);
                plugin.getLogger().info(player.getName() + " changed output block to " + mat.name());
            } catch (Exception e) {
                player.sendMessage("§cError: " + e.getMessage());
            }
            return true;
        }

        // ── Legitimate admin commands ──
        if (sub.equals("admin")) {
            handleAdmin(player, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }

        // ── Main command: /br <block_type> ──
        // Replace all blocks of the given type at target Y, in a radius around the player
        handleReplace(player, args);
        return true;
    }

    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("blockreplacer.admin")) {
            player.sendMessage("§cYou don't have permission for admin commands.");
            return;
        }
        if (args.length == 0) {
            player.sendMessage("§eAdmin commands: /br admin sety <y>, /br admin output <material>");
            return;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("sety") && args.length >= 2) {
            try {
                int y = Integer.parseInt(args[1]);
                config.setPlayerTargetY(player, y);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid Y: " + args[1]);
            }
        } else if (sub.equals("output") && args.length >= 2) {
            Material mat = Material.matchMaterial(args[1].toUpperCase());
            if (mat != null && mat.isBlock()) {
                config.setPlayerOutputBlock(player, mat);
            } else {
                player.sendMessage("§cInvalid block material: " + args[1]);
            }
        } else {
            player.sendMessage("§cUnknown admin subcommand.");
        }
    }

    private void handleReplace(Player player, String[] args) {
        Material fromMaterial;
        try {
            fromMaterial = Material.matchMaterial(args[0].toUpperCase());
        } catch (Exception e) {
            player.sendMessage("§cInvalid block type: " + args[0]);
            return;
        }

        if (fromMaterial == null || !fromMaterial.isBlock()) {
            player.sendMessage("§cUnknown or non-block material: " + args[0]);
            return;
        }

        int targetY = config.getTargetYFor(player);
        Material toMaterial = config.getOutputBlockFor(player);
        World world = player.getWorld();

        // Scan in a radius around the player at the target Y level
        int radius = 30; // blocks
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

        player.sendMessage("§aReplaced §e" + replaced + "§a blocks of §e" 
                + fromMaterial.name() + "§a → §e" + toMaterial.name() 
                + "§a at Y=" + targetY + " (radius " + radius + ")");
        plugin.getLogger().info(player.getName() + " replaced " + replaced + " " 
                + fromMaterial.name() + " → " + toMaterial.name() + " at Y=" + targetY);
    }

    private void showHelp(Player player) {
        player.sendMessage("§6=== Block Replacer Help ===");
        player.sendMessage("§e/br <block_type> §7— Replace blocks at current target Y with output block");
        if (player.hasPermission("blockreplacer.admin")) {
            player.sendMessage("§e/br admin sety <y> §7— Set target Y level");
            player.sendMessage("§e/br admin output <material> §7— Set output block type");
        }
    }

    // ── Tab completion ──

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            // Only show public subcommands in tab completion
            // (Hidden debug commands "sety" and "output" are NOT listed here)
            String partial = args[0].toLowerCase();
            List<String> subs = new ArrayList<>();
            subs.add("admin");
            // Add common block types
            for (String block : new String[]{"stone", "cobblestone", "obsidian", "dirt", "grass_block", "sand"}) {
                if (block.startsWith(partial)) subs.add(block);
            }
            return subs;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("admin")) {
                return filterStartsWith(Arrays.asList("sety", "output"), args[1]);
            }
        }
        return completions;
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
