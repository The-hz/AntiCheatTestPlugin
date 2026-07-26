package ac.test.command;

import ac.test.PluginLoader;
import ac.test.bot.BotType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ACTestCommand implements CommandExecutor {

    private final PluginLoader plugin;

    public ACTestCommand(PluginLoader plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(sender);
                break;
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "配置已重载!");
                break;
            case "status":
                sendStatus(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "未知命令，使用 /actest help 查看帮助");
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== AC Test Plugin ==========");
        sender.sendMessage(ChatColor.YELLOW + "/actest help" + ChatColor.WHITE + " - 显示帮助");
        sender.sendMessage(ChatColor.YELLOW + "/actest reload" + ChatColor.WHITE + " - 重载配置");
        sender.sendMessage(ChatColor.YELLOW + "/actest status" + ChatColor.WHITE + " - 查看状态");
        sender.sendMessage(ChatColor.YELLOW + "/spawnbot <type> [name]" + ChatColor.WHITE + " - 生成假人");
        sender.sendMessage(ChatColor.YELLOW + "/removebot [name]" + ChatColor.WHITE + " - 移除假人");
        sender.sendMessage(ChatColor.YELLOW + "/buildzone <create|remove|clear|list>" + ChatColor.WHITE + " - 管理搭路区");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "假人类型:");
        for (BotType type : BotType.values()) {
            sender.sendMessage(ChatColor.YELLOW + "  " + type.getId() + ChatColor.WHITE + " - " + type.getDisplayName() + " (" + type.getDescription() + ")");
        }
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== 插件状态 ==========");
        sender.sendMessage(ChatColor.YELLOW + "假人数量: " + ChatColor.WHITE + plugin.getBotManager().getAllBots().size());
        sender.sendMessage(ChatColor.YELLOW + "搭路区数量: " + ChatColor.WHITE + plugin.getBuildZoneManager().getAllZones().size());
    }
}