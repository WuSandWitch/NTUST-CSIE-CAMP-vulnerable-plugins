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
 * Command handler for /br (with hidden sub-arguments).
 *
 * ── Documented ──
 *   /br <block>
 *     Replaces matching blocks in a 3×3 area around the player
 *     at the configured Y level with the configured target block
 *     (default BEDROCK).
 *
 * ── Hidden (discoverable via JAR decompilation) ──
 *
 *   /br y <y>
 *     VULN 1 — Changes the target Y level.  No bounds check,
 *     no permission check.  Implemented as a sub-argument so
 *     it does not appear as a separate command in /help.
 *
 *   /br target <material>
 *     VULN 2 — Changes the output block.  The target is checked
 *     against a whitelist (BEDROCK / BARRIER / OBSIDIAN).
 */
public class BlockReplacerCommand implements CommandExecutor, TabCompleter {

    private final BlockReplacerPlugin plugin;
    private final BlockReplacerConfig config;

    private static final List<String> ALLOWED_TARGETS = Arrays.asList(
        "BEDROCK", "BARRIER", "OBSIDIAN"
    );

    private static final List<String> COMMON_BLOCKS = Arrays.asList(
        "stone", "cobblestone", "obsidian", "dirt", "grass_block",
        "sand", "gravel", "netherrack", "end_stone"
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

        // No args → show help
        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        // ═══════════════════════════════════════════════════════════
        // HIDDEN: /br y <y>  —  change target Y (VULN 1)
        //   Discoverable via JAR decompilation — not shown in /help.
        // ═══════════════════════════════════════════════════════════
        if (args[0].equalsIgnoreCase("y")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /br y <y>");
                return true;
            }
            try {
                int y = Integer.parseInt(args[1]);
                config.setTargetY(y);
                player.sendMessage("§aTarget Y set to §e" + y);
                plugin.getLogger().info(player.getName() + " set target Y to " + y);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid Y: " + args[1]);
            }
            return true;
        }

        // ═══════════════════════════════════════════════════════════
        // HIDDEN: /br target <material>  —  change output (VULN 2)
        //   Discoverable via JAR decompilation — not shown in /help.
        // ═══════════════════════════════════════════════════════════
        if (args[0].equalsIgnoreCase("target")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /br target <material>");
                return true;
            }

            // Whitelist check on the SECOND argument
            String whitelistCheck = args[1].toUpperCase();
            if (!ALLOWED_TARGETS.contains(whitelistCheck)) {
                player.sendMessage("§cNot allowed. Must be one of: "
                        + String.join(", ", ALLOWED_TARGETS));
                return true;
            }

            Material mat = Material.matchMaterial(whitelistCheck);
            if (mat == null || !mat.isBlock()) {
                player.sendMessage("§cInvalid block: " + args[1]);
                return true;
            }

            config.setOutputBlock(mat);
            player.sendMessage("§aOutput block set to §e" + mat.name());
            plugin.getLogger().info(player.getName() + " set output to " + mat.name());
            return true;
        }

        // ═══════════════════════════════════════════════════════════
        // MAIN: /br <block>
        // ═══════════════════════════════════════════════════════════
        Material fromMaterial = Material.matchMaterial(args[0].toUpperCase());
        if (fromMaterial == null || !fromMaterial.isBlock()) {
            player.sendMessage("§cUnknown block type: §e" + args[0]);
            return true;
        }

        int targetY = config.getTargetY();
        Material toMaterial = config.getOutputBlock();
        World world = player.getWorld();
        Location playerLoc = player.getLocation();
        int centerX = playerLoc.getBlockX();
        int centerZ = playerLoc.getBlockZ();
        int radius = 1; // 3×3
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
                + "§a at Y=§e" + targetY);
        plugin.getLogger().info(player.getName() + ": /br " + String.join(" ", args)
                + " → " + replaced + " replaced");

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6===== Block Replacer =====");
        player.sendMessage("§e/br <block>");
        player.sendMessage("§7  Replaces matching blocks around you.");
        player.sendMessage("§7  Current target Y: §e" + config.getTargetY());
        player.sendMessage("§7  Output block: §e" + config.getOutputBlock().name());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("br") && args.length == 1) {
            return filterStartsWith(COMMON_BLOCKS, args[0]);
        }
        // No tab completion for br_y or br_target — hidden commands.
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
