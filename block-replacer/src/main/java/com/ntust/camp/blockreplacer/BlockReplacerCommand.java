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
 * Command handler for /br, /br_y, and /br_target.
 *
 * ── Documented ──
 *   /br <block>
 *     Replaces matching blocks in a 3×3 area around the player
 *     at the configured Y level with the configured target block
 *     (default BEDROCK).
 *
 * ── Hidden (discoverable via JAR decompilation) ──
 *
 *   /br_y <y>
 *     VULN 1 — Changes the target Y level.  No bounds check,
 *     no permission check.  Registered as an alias of /br so
 *     it appears in plugin.yml but has no help entry.
 *
 *   /br_target <target>
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

        // ═══════════════════════════════════════════════════════════
        // HIDDEN: /br_y <y>  —  change target Y (VULN 1)
        // ═══════════════════════════════════════════════════════════
        if (label.equalsIgnoreCase("br_y")) {
            if (args.length < 1) {
                player.sendMessage("§cUsage: /br_y <y>");
                return true;
            }
            try {
                int y = Integer.parseInt(args[0]);
                config.setTargetY(y);
                player.sendMessage("§aTarget Y set to §e" + y);
                plugin.getLogger().info(player.getName() + " set target Y to " + y);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid Y: " + args[0]);
            }
            return true;
        }

        // ═══════════════════════════════════════════════════════════
        // HIDDEN: /br_target <allowed> [actual]  —  change output (VULN 2)
        // ═══════════════════════════════════════════════════════════
        if (label.equalsIgnoreCase("br_target")) {
            if (args.length < 1) {
                player.sendMessage("§cUsage: /br_target <material>");
                return true;
            }

            // Whitelist check on the FIRST argument
            String whitelistCheck = args[0].toUpperCase();
            if (!ALLOWED_TARGETS.contains(whitelistCheck)) {
                player.sendMessage("§cNot allowed. Must be one of: "
                        + String.join(", ", ALLOWED_TARGETS));
                return true;
            }

            Material mat = Material.matchMaterial(whitelistCheck);
            if (mat == null || !mat.isBlock()) {
                player.sendMessage("§cInvalid block: " + args[0]);
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
        if (args.length == 0) {
            showHelp(player);
            return true;
        }

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
