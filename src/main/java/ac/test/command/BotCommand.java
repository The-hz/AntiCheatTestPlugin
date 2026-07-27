package ac.test.command;

import ac.test.PluginLoader;
import ac.test.bot.BotData;
import ac.test.bot.BotType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

public class BotCommand implements CommandExecutor {

    private final PluginLoader plugin;
    private final Random random = new Random();

    public BotCommand(PluginLoader plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "该命令只能由玩家执行");
            return true;
        }

        Player player = (Player) sender;
        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "spawnbot":
                return handleSpawnBot(player, args);
            case "removebot":
                return handleRemoveBot(player, args);
            default:
                return false;
        }
    }

    private boolean handleSpawnBot(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "用法: /spawnbot <type> [name]");
            player.sendMessage(ChatColor.YELLOW + "可用类型:");
            for (BotType type : BotType.values()) {
                player.sendMessage(ChatColor.GRAY + "  - " + type.getId() + ": " + type.getDisplayName());
            }
            return true;
        }

        BotType type = BotType.fromId(args[0]);
        if (type == null) {
            player.sendMessage(ChatColor.RED + "未知的假人类型: " + args[0]);
            return true;
        }

        String name;
        if (args.length >= 2) {
            name = args[1];
        } else {
            name = "Bot_" + random.nextInt(10000);
        }

        if (plugin.getBotManager().getBot(name) != null) {
            player.sendMessage(ChatColor.RED + "已存在名为 " + name + " 的假人");
            return true;
        }

        Location location = player.getLocation();
        BotData bot = plugin.getBotManager().createBot(name, type, location);
        
        if (bot != null) {
            plugin.getBotManager().saveBotToConfig(name, type, location, bot.getNpc().getUniqueId());
            player.sendMessage(ChatColor.GREEN + "成功生成 " + type.getDisplayName() + ": " + name);
        } else {
            player.sendMessage(ChatColor.RED + "生成假人失败");
        }

        return true;
    }

    private boolean handleRemoveBot(Player player, String[] args) {
        if (args.length < 1) {
            // 移除最近的假人
            BotData nearest = findNearestBot(player);
            if (nearest == null) {
                player.sendMessage(ChatColor.RED + "附近没有假人");
                return true;
            }
            
            if (plugin.getBotManager().removeBot(nearest.getName())) {
                plugin.getBotManager().removeBotFromConfig(nearest.getName());
                player.sendMessage(ChatColor.GREEN + "已移除假人: " + nearest.getName());
            }
            return true;
        }

        String name = args[0];
        if (plugin.getBotManager().removeBot(name)) {
            plugin.getBotManager().removeBotFromConfig(name);
            player.sendMessage(ChatColor.GREEN + "已移除假人: " + name);
        } else {
            player.sendMessage(ChatColor.RED + "找不到假人: " + name);
        }

        return true;
    }

    private BotData findNearestBot(Player player) {
        BotData nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        Location playerLoc = player.getLocation();

        for (BotData bot : plugin.getBotManager().getAllBots()) {
            if (bot.getNpc() != null && bot.getNpc().isSpawned()) {
                double distance = bot.getNpc().getStoredLocation().distanceSquared(playerLoc);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = bot;
                }
            }
        }

        return nearest;
    }
}