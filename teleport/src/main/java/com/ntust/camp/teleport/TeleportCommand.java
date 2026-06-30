package com.ntust.camp.teleport;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles the /tp2 command.
 *
 * Usage: /tp2 <x> <z>
 *
 * VULN 1 — Y-axis bypass via comma-separated coordinates:
 *   The plugin splits arguments on both spaces AND commas (to support
 *   "100,200" as an alternative to "100 200").  If a player types
 *   "/tp2 100,64,200", the split produces THREE values, and the middle
 *   one is interpreted as the Y coordinate — enabling 3D teleport.
 *
 * VULN 2 — Integer overflow in distance check:
 *   The squared-distance calculation uses int arithmetic:
 *     int dx = targetX - playerX;
 *     int dz = targetZ - playerZ;
 *     int distSq = dx*dx + dz*dz;
 *   When dx or dz is large (~65536), dx² overflows the 32-bit signed
 *   int and wraps around.  This can produce a small (or negative) value
 *   that passes the max-distance check while the actual teleport uses
 *   the true (large) double-precision coordinates.
 *
 * Discovery hints:
 *   - Vuln 1: try using commas (many games use "x,y,z" format)
 *   - Vuln 2: rejected teleports show "Distance: NaN blocks" when
 *     overflow produces a negative distSq (Math.sqrt of negative = NaN)
 */
public class TeleportCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final int maxDistance;

    public TeleportCommand(JavaPlugin plugin, int maxDistance) {
        this.plugin = plugin;
        this.maxDistance = maxDistance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        if (!player.hasPermission("teleport.use")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6===== Teleport =====");
            player.sendMessage("§e/tp2 <x> <z>");
            player.sendMessage("§7  Teleports you to X,Z (same Y level)");
            player.sendMessage("§7  Max distance: §e" + maxDistance + "§7 blocks");
            player.sendMessage("§7  Coordinates can be space-separated or comma-separated.");
            return true;
        }

        // ── Flexible coordinate parsing ──
        // Join all args and split on spaces OR commas OR semicolons.
        // This allows: "100 200", "100,200", "100,64,200", etc.
        String joined = String.join(" ", args);
        String[] parts = joined.split("[\\s,;]+");

        if (parts.length < 2 || parts.length > 3) {
            player.sendMessage("§cInvalid coordinates. Use: /tp2 <x> <z>  or  /tp2 <x>,<z>");
            return true;
        }

        try {
            double targetX = Double.parseDouble(parts[0]);
            double targetZ;
            double targetY;

            if (parts.length == 3) {
                // ── VULN 1: 3 values → middle one becomes Y ──
                // The comma-split was intended only for "x,z" pairs,
                // but "x,y,z" also works because of the generic split.
                targetY = Double.parseDouble(parts[1]);
                targetZ = Double.parseDouble(parts[2]);
            } else {
                targetY = player.getLocation().getY();
                targetZ = Double.parseDouble(parts[1]);
            }

            Location playerLoc = player.getLocation();
            int playerX = playerLoc.getBlockX();
            int playerZ = playerLoc.getBlockZ();

            // ── VULN 2: Integer overflow in distance check ──
            int dx = (int) targetX - playerX;
            int dz = (int) targetZ - playerZ;
            int distSq = dx * dx + dz * dz;   // ← OVERFLOW HERE

            int maxDistSq = maxDistance * maxDistance;

            if (distSq < 0 || distSq > maxDistSq) {
                // Show the calculated distance — when overflow produces
                // a negative distSq, Math.sqrt returns NaN, which is a
                // big clue that the math is broken.
                double shownDist = Math.sqrt(Math.abs((double) dx * dx + (double) dz * dz));
                player.sendMessage("§cToo far! Distance: §e"
                        + String.format("%.1f", shownDist)
                        + "§c blocks (max " + maxDistance + ")");
                return true;
            }

            Location dest = new Location(player.getWorld(),
                    targetX + 0.5, targetY, targetZ + 0.5,
                    playerLoc.getYaw(), playerLoc.getPitch());

            player.teleport(dest);
            player.sendMessage("§aTeleported to §e"
                    + (int) targetX + " " + (int) targetZ
                    + (parts.length == 3 ? " " + (int) targetY : "") + "§a!");

        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number format. Use numbers like: /tp2 100 200");
        }

        return true;
    }
}
