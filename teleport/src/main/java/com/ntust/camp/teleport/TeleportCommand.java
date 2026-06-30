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
 * Usage (documented):  /tp2 <x> <z>
 * Usage (actual):      /tp2 <x> <z>    also accepts comma-separated x,y,z
 *
 * VULN 1 — Y-axis bypass (discoverable via JAR decompilation):
 *   The coordinate parser splits on both spaces AND commas.  If a player
 *   types "/tp2 100,64,200" the split produces three values, and the
 *   middle one becomes the Y coordinate.
 *
 * VULN 2 — Integer overflow (discoverable via JAR decompilation):
 *   The distance check uses int arithmetic:
 *     int distSq = dx*dx + dz*dz;
 *   With maxDistance=3, maxDistSq=9.  But when dx is large (~65536),
 *   dx² overflows the 32-bit signed int and wraps to a small value
 *   that passes the check.  The actual teleport uses double-precision
 *   coordinates, so the player is sent to the true (large) location.
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
            player.sendMessage("§7  Teleports you to X,Z (same Y level).");
            player.sendMessage("§7  Max distance: §e" + maxDistance + "§7 blocks.");
            return true;
        }

        // ── Coordinate parsing (supports spaces AND commas) ──
        String joined = String.join(" ", args);
        String[] parts = joined.split("[\\s,;]+");

        if (parts.length < 2 || parts.length > 3) {
            player.sendMessage("§cInvalid coordinates. Use: /tp2 <x> <z>");
            return true;
        }

        try {
            double targetX = Double.parseDouble(parts[0]);
            double targetZ;
            double targetY;

            if (parts.length == 3) {
                // Three values → the middle one is Y (VULN 1)
                targetY = Double.parseDouble(parts[1]);
                targetZ = Double.parseDouble(parts[2]);
            } else {
                targetY = player.getLocation().getY();
                targetZ = Double.parseDouble(parts[1]);
            }

            Location playerLoc = player.getLocation();
            int playerX = playerLoc.getBlockX();
            int playerZ = playerLoc.getBlockZ();

            // ── Distance check (VULN 2: int overflow) ──
            int dx = (int) targetX - playerX;
            int dz = (int) targetZ - playerZ;
            int distSq = dx * dx + dz * dz;   // ← OVERFLOW

            int maxDistSq = maxDistance * maxDistance;

            if (distSq < 0 || distSq > maxDistSq) {
                player.sendMessage("§cToo far! Max teleport distance is §e"
                        + maxDistance + "§c blocks.");
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
            player.sendMessage("§cInvalid number format.");
        }

        return true;
    }
}
