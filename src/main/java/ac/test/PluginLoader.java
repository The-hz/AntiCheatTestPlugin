package ac.test;

import ac.test.bot.BotManager;
import ac.test.buildzone.BuildZoneManager;
import ac.test.command.ACTestCommand;
import ac.test.command.BuildZoneCommand;
import ac.test.command.BotCommand;
import ac.test.listener.EntityListener;
import ac.test.listener.PlayerListener;
import ac.test.scoreboard.ScoreboardManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginLoader extends JavaPlugin {

    //vibe coding

    private static PluginLoader instance;
    private BotManager botManager;
    private BuildZoneManager buildZoneManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        botManager = new BotManager(this);
        buildZoneManager = new BuildZoneManager(this);
        scoreboardManager = new ScoreboardManager(this);
        
        getCommand("actest").setExecutor(new ACTestCommand(this));
        getCommand("spawnbot").setExecutor(new BotCommand(this));
        getCommand("removebot").setExecutor(new BotCommand(this));
        getCommand("buildzone").setExecutor(new BuildZoneCommand(this));
        
        getServer().getPluginManager().registerEvents(new EntityListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        botManager.loadBotsFromConfig();
        buildZoneManager.loadZonesFromConfig();
        scoreboardManager.start();
        
        getLogger().info("ACTestPlugin 已启用!");
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            botManager.removeAllBots();
        }
        if (scoreboardManager != null) {
            scoreboardManager.stop();
        }
        getLogger().info("ACTestPlugin 已禁用!");
    }

    public static PluginLoader getInstance() {
        return instance;
    }

    public BotManager getBotManager() {
        return botManager;
    }

    public BuildZoneManager getBuildZoneManager() {
        return buildZoneManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}