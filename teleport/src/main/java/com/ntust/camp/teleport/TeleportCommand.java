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
 * Usage (actual):      /tp2 <x> <z> [y]
 *
 * VULN 1 — Y-axis bypass (discoverable via JAR decompilation):
 *   The command accepts an optional third argument.  If three
 *   arguments are given, the third one is used as the Y coordinate,
 *   bypassing the "flat plane only" restriction.
 *
 * VULN 2 — Integer overflow (discoverable via JAR decompilation):
 *   Distance check:
 *     int distSq = dx*dx + dz*dz;
 *   With maxDistance=3, maxDistSq=9.
 *   When dx=65536, dx² overflows 32-bit signed int → wraps to 0 ≤ 9
 *   → check passes.  Actual teleport uses double coordinates.
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
            player.sendMessage("§7  Max distance: §e" + maxDistance + "§7 blocks.");
            return true;
        }

        if (args.length < 2 || args.length > 3) {
            player.sendMessage("§cUsage: /tp2 <x> <z>");
            return true;
        }

        try {
            double targetX = Double.parseDouble(args[0]);
            double targetZ = Double.parseDouble(args[1]);
            double targetY = player.getLocation().getY();

            // VULN 1: hidden 3rd argument = Y coordinate
            if (args.length == 3) {
                targetY = Double.parseDouble(args[2]);
            }

            Location playerLoc = player.getLocation();
            int playerX = playerLoc.getBlockX();
            int playerZ = playerLoc.getBlockZ();

            // VULN 2: int overflow in distance check
            int dx = (int) targetX - playerX;
            int dz = (int) targetZ - playerZ;
            int distSq = dx * dx + dz * dz;

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
                    + (args.length == 3 ? " " + (int) targetY : "") + "§a!");

        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number format.");
        }

        return true;
    }
}
