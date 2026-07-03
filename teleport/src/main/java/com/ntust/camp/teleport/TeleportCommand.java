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
 * Usage (documented):  /tp2 <dx> <dz>
 * Usage (actual):      /tp2 <dx> <dz> [dy]
 *
 * Teleports the player by a relative offset from their current position.
 *
 * VULN 1 — Y-axis bypass (discoverable via JAR decompilation):
 *   The command accepts an optional third argument.  If three
 *   arguments are given, the third one is used as a vertical offset,
 *   bypassing the "flat plane only" restriction.
 *
 * VULN 2 — Integer overflow (discoverable via JAR decompilation):
 *   Distance check truncates each offset to a 16-bit short first:
 *     short sdx = (short) dx;
 *     int   distSq = sdx*sdx + sdz*sdz;
 *   With maxDistance=3, maxDistSq=9.
 *   Any dx whose low 16 bits are small slips through, e.g.
 *     dx=65537 → (short)65537 = 1     → distSq = 1  ≤ 9  → passes
 *     dx=65536 → (short)65536 = 0     → distSq = 0  ≤ 9  → passes
 *   The distance check sees the truncated value, but the actual
 *   teleport uses the full 32-bit dx/dz → real distance is unbounded.
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
            player.sendMessage("§e/tp2 <dx> <dz>");
            player.sendMessage("§7  Max distance: §e" + maxDistance + "§7 blocks.");
            return true;
        }

        if (args.length < 2 || args.length > 3) {
            player.sendMessage("§cUsage: /tp2 <dx> <dz>");
            return true;
        }

        try {
            int dx = Integer.parseInt(args[0]);
            int dz = Integer.parseInt(args[1]);
            double dy = 0;

            // VULN 1: hidden 3rd argument = Y offset
            if (args.length == 3) {
                dy = Double.parseDouble(args[2]);
            }

            // VULN 2: distance check truncates offsets to 16-bit short
            // → dx=65537 wraps to 1, passing the check while the real
            //   teleport below still moves the full 65537 blocks.
            short sdx = (short) dx;
            short sdz = (short) dz;
            int distSq = sdx * sdx + sdz * sdz;
            int maxDistSq = maxDistance * maxDistance;

            if (distSq < 0 || distSq > maxDistSq) {
                player.sendMessage("§cToo far! Max teleport distance is §e"
                        + maxDistance + "§c blocks.");
                return true;
            }

            Location playerLoc = player.getLocation();
            Location dest = new Location(player.getWorld(),
                    playerLoc.getX() + dx,
                    playerLoc.getY() + dy,
                    playerLoc.getZ() + dz,
                    playerLoc.getYaw(), playerLoc.getPitch());

            player.teleport(dest);
            player.sendMessage("§aTeleported §e" + dx + " " + dz
                    + (args.length == 3 ? " " + (int) dy : "") + "§a!");

        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number format.");
        }

        return true;
    }
}
