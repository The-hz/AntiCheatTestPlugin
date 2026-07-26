package ac.test.command;

import ac.test.PluginLoader;
import ac.test.buildzone.BuildZone;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BuildZoneCommand implements CommandExecutor {

    private final PluginLoader plugin;
    private final Map<UUID, Location> corner1Map = new HashMap<>();

    public BuildZoneCommand(PluginLoader plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "该命令只能由玩家执行");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                return handleCreate(player, args);
            case "remove":
                return handleRemove(player, args);
            case "clear":
                return handleClear(player, args);
            case "list":
                return handleList(player);
            case "pos1":
                return handlePos1(player);
            case "pos2":
                return handlePos2(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "========== 搭路区管理 ==========");
        player.sendMessage(ChatColor.YELLOW + "/buildzone pos1" + ChatColor.WHITE + " - 设置第一个角落");
        player.sendMessage(ChatColor.YELLOW + "/buildzone pos2 <name>" + ChatColor.WHITE + " - 设置第二个角落并创建区域");
        player.sendMessage(ChatColor.YELLOW + "/buildzone remove <name>" + ChatColor.WHITE + " - 删除区域");
        player.sendMessage(ChatColor.YELLOW + "/buildzone clear <name>" + ChatColor.WHITE + " - 立即清理区域");
        player.sendMessage(ChatColor.YELLOW + "/buildzone list" + ChatColor.WHITE + " - 列出所有区域");
    }

    private boolean handlePos1(Player player) {
        corner1Map.put(player.getUniqueId(), player.getLocation());
        player.sendMessage(ChatColor.GREEN + "已设置第一个角落: " + formatLocation(player.getLocation()));
        player.sendMessage(ChatColor.YELLOW + "请使用 /buildzone pos2 <name> 设置第二个角落");
        return true;
    }

    private boolean handlePos2(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /buildzone pos2 <name>");
            return true;
        }

        Location corner1 = corner1Map.get(player.getUniqueId());
        if (corner1 == null) {
            player.sendMessage(ChatColor.RED + "请先使用 /buildzone pos1 设置第一个角落");
            return true;
        }

        if (!corner1.getWorld().equals(player.getWorld())) {
            player.sendMessage(ChatColor.RED + "两个角落必须在同一个世界");
            return true;
        }

        String name = args[1];
        Location corner2 = player.getLocation();

        BuildZone zone = plugin.getBuildZoneManager().createZone(name, corner1, corner2);
        if (zone != null) {
            player.sendMessage(ChatColor.GREEN + "成功创建搭路区: " + name);
            player.sendMessage(ChatColor.GRAY + "范围: " + formatLocation(corner1) + " 到 " + formatLocation(corner2));
            corner1Map.remove(player.getUniqueId());
        } else {
            player.sendMessage(ChatColor.RED + "创建失败，可能已存在同名区域");
        }

        return true;
    }

    private boolean handleCreate(Player player, String[] args) {
        player.sendMessage(ChatColor.YELLOW + "请使用以下步骤创建搭路区:");
        player.sendMessage(ChatColor.GRAY + "1. 站在第一个角落，执行 /buildzone pos1");
        player.sendMessage(ChatColor.GRAY + "2. 站在第二个角落，执行 /buildzone pos2 <name>");
        return true;
    }

    private boolean handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /buildzone remove <name>");
            return true;
        }

        String name = args[1];
        if (plugin.getBuildZoneManager().removeZone(name)) {
            player.sendMessage(ChatColor.GREEN + "已删除搭路区: " + name);
        } else {
            player.sendMessage(ChatColor.RED + "找不到搭路区: " + name);
        }

        return true;
    }

    private boolean handleClear(Player player, String[] args) {
        if (args.length < 2) {
            // 清理玩家所在的区域
            BuildZone zone = plugin.getBuildZoneManager().getZoneAtLocation(player.getLocation());
            if (zone == null) {
                player.sendMessage(ChatColor.RED + "你不在任何搭路区内，请使用 /buildzone clear <name>");
                return true;
            }
            plugin.getBuildZoneManager().clearZone(zone);
            player.sendMessage(ChatColor.GREEN + "已清理搭路区: " + zone.getName());
            return true;
        }

        String name = args[1];
        BuildZone zone = plugin.getBuildZoneManager().getZone(name);
        if (zone == null) {
            player.sendMessage(ChatColor.RED + "找不到搭路区: " + name);
            return true;
        }

        plugin.getBuildZoneManager().clearZone(zone);
        player.sendMessage(ChatColor.GREEN + "已清理搭路区: " + name);
        return true;
    }

    private boolean handleList(Player player) {
        player.sendMessage(ChatColor.GOLD + "========== 搭路区列表 ==========");
        
        if (plugin.getBuildZoneManager().getAllZones().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "暂无搭路区");
            return true;
        }

        for (BuildZone zone : plugin.getBuildZoneManager().getAllZones()) {
            player.sendMessage(ChatColor.YELLOW + zone.getName() + ChatColor.GRAY + 
                " - " + zone.getWorld().getName() + 
                " (清理间隔: " + zone.getClearInterval() + "秒)");
        }

        return true;
    }

    private String formatLocation(Location loc) {
        return String.format("[%d, %d, %d]", (int)loc.getX(), (int)loc.getY(), (int)loc.getZ());
    }
}