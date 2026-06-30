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
import java.util.Set;

/**
 * Handles the /br command.
 *
 * Usage (documented):  /br <block>
 * Usage (actual):      /br <block> [y] [output]
 *
 * The plugin replaces blocks of the given type around the block the
 * player is looking at, in a small (3×3) area.
 *
 * VULN 1 — Y-axis bypass (discoverable via JAR decompilation):
 *   The optional [y] argument overrides the Y level of the focus block.
 *   It has no bounds check — any integer is accepted.
 *
 * VULN 2 — Output block bypass (discoverable via JAR decompilation):
 *   The optional [output] argument overrides the default replacement
 *   block (BARRIER) with no whitelist.  e.g. AIR makes walls vanish.
 */
public class BlockReplacerCommand implements CommandExecutor, TabCompleter {

    private final BlockReplacerPlugin plugin;
    private final BlockReplacerConfig config;

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

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        // ── Parse <block> (required) ──
        Material fromMaterial = Material.matchMaterial(args[0].toUpperCase());
        if (fromMaterial == null || !fromMaterial.isBlock()) {
            player.sendMessage("§cUnknown block type: §e" + args[0]);
            return true;
        }

        // ── Get the block the player is looking at ──
        Block focusBlock = player.getTargetBlockExact(50);
        if (focusBlock == null || focusBlock.getType() == Material.AIR) {
            player.sendMessage("§cYou must be looking at a block.");
            return true;
        }

        int centerX = focusBlock.getX();
        int centerZ = focusBlock.getZ();
        int targetY = focusBlock.getY();

        // ── Parse [y] (optional, UNDOCUMENTED) ──
        // VULN 1: overrides the Y level with no bounds check.
        int argIdx = 1;
        if (args.length > argIdx) {
            try {
                targetY = Integer.parseInt(args[argIdx]);
                argIdx++;
            } catch (NumberFormatException ignored) {
                // Not a number — maybe the player skipped [y] and gave [output] directly
            }
        }

        // ── Parse [output] (optional, UNDOCUMENTED) ──
        // VULN 2: overrides the output block with no whitelist.
        Material toMaterial = config.getOutputBlock();
        if (args.length > argIdx) {
            Material parsed = Material.matchMaterial(args[argIdx].toUpperCase());
            if (parsed != null && parsed.isBlock()) {
                toMaterial = parsed;
            }
        }

        // ── Replace in 3×3 area around the focus block (same Y) ──
        int radius = 1; // 3×3 = radius 1
        World world = player.getWorld();
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
        player.sendMessage("§7  Look at a block and run this command to replace");
        player.sendMessage("§7  matching blocks around it (" + (config.getRadius() * 2 + 1) + "×" + (config.getRadius() * 2 + 1) + " area).");
    }

    // ── Tab completion ──

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(COMMON_BLOCKS, args[0]);
        }
        // Deliberately no tab completion for [y] or [output] —
        // these are undocumented parameters students must discover.
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
