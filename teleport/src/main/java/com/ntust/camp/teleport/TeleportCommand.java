package com.ntust.camp.teleport;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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

        if (args.length < 2 || args.length > 3) {
            player.sendMessage("§eUsage: /tp2 <x> <z> §7— Teleport to X,Z (flat plane, max " + maxDistance + " blocks)");
            return true;
        }

        try {
            int targetX = Integer.parseInt(args[0]);
            int targetZ = Integer.parseInt(args[1]);

            Location playerLoc = player.getLocation();
            int playerX = playerLoc.getBlockX();
            int playerZ = playerLoc.getBlockZ();

            // ── VULN 2: Integer overflow in distance check ──
            // Using int for dx² + dz² can overflow when dx or dz are large.
            // Example: dx = 65536 → dx² = 4,294,967,296 → wraps to 0 in int
            // This bypasses the check while the actual teleport uses the true coords.
            int dx = targetX - playerX;
            int dz = targetZ - playerZ;
            int distSq = dx * dx + dz * dz;   // <-- INTEGER OVERFLOW HERE

            int maxDistSq = maxDistance * maxDistance;
            if (distSq < 0 || distSq > maxDistSq) {
                player.sendMessage("§cToo far! Max teleport distance is " + maxDistance + " blocks. "
                        + "You are trying to go §e" + (int) Math.sqrt(Math.abs(distSq)) + "§c blocks away.");
                return true;
            }

            // ── VULN 1: Hidden 3D teleport (Y-axis bypass) ──
            // If 3 arguments are provided, the third one is used as Y.
            // This was a debug feature for testing parkour maps — accidentally left in.
            double targetY = playerLoc.getY();
            if (args.length == 3) {
                targetY = Double.parseDouble(args[2]);
                player.sendMessage("§7[Debug] 3D teleport mode — using Y=" + targetY);
            }

            Location dest = new Location(player.getWorld(), targetX + 0.5, targetY, targetZ + 0.5,
                    playerLoc.getYaw(), playerLoc.getPitch());

            player.teleport(dest);
            player.sendMessage("§aTeleported to §e" + targetX + " " + targetZ
                    + (args.length == 3 ? " " + (int) targetY : "") + "§a!");

        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid coordinates. Use numbers like: /tp2 100 200");
        }

        return true;
    }
}
