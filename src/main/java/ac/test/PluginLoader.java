package ac.test;

import ac.test.bot.BotManager;
import ac.test.buildzone.BuildZoneManager;
import ac.test.command.ACTestCommand;
import ac.test.command.BuildZoneCommand;
import ac.test.command.BotCommand;
import ac.test.listener.EntityListener;
import ac.test.listener.PlayerListener;
import ac.test.scoreboard.ScoreboardManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public final class PluginLoader extends JavaPlugin {

    //vibe coding

    private static PluginLoader instance;
    private BotManager botManager;
    private BuildZoneManager buildZoneManager;
    private ScoreboardManager scoreboardManager;
    private Thread shutdownHook;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        botManager = new BotManager(this);
        buildZoneManager = new BuildZoneManager(this);
        scoreboardManager = new ScoreboardManager(this);

        registerShutdownHook();
        
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
        if (scoreboardManager != null) {
            scoreboardManager.stop();
        }
        getLogger().info("ACTestPlugin 已禁用!");
    }
    
    private void registerShutdownHook() {
        shutdownHook = new Thread(() -> {
            getLogger().info("JVM关闭钩子执行: 清理Citizens saves.yml...");
            cleanupCitizensSavesYaml();
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        getLogger().info("已注册JVM关闭钩子");
    }
    
    private void cleanupCitizensSavesYaml() {
        try {
            Set<String> botNames = getConfig().getConfigurationSection("bots") != null 
                ? getConfig().getConfigurationSection("bots").getKeys(false) 
                : new HashSet<>();
            
            if (botNames.isEmpty()) {
                return;
            }

            File citizensFolder = new File(getDataFolder().getParentFile(), "Citizens");
            File savesFile = new File(citizensFolder, "saves.yml");
            
            if (!savesFile.exists()) {
                return;
            }

            YamlConfiguration savesConfig = new YamlConfiguration();
            try {
                savesConfig.load(savesFile);
            } catch (Exception e) {
                System.err.println("[ACTestPlugin] 无法读取Citizens saves.yml: " + e.getMessage());
                return;
            }

            org.bukkit.configuration.ConfigurationSection npcSection = savesConfig.getConfigurationSection("npc");
            if (npcSection == null) {
                return;
            }

            Set<String> toRemove = new HashSet<>();
            for (String npcId : npcSection.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection npcData = npcSection.getConfigurationSection(npcId);
                if (npcData != null) {
                    String name = npcData.getString("name");
                    if (name != null && botNames.contains(name)) {
                        toRemove.add(npcId);
                    }
                }
            }

            for (String npcId : toRemove) {
                npcSection.set(npcId, null);
                System.out.println("[ACTestPlugin] 从saves.yml移除NPC: " + npcId);
            }

            if (!toRemove.isEmpty()) {
                savesConfig.save(savesFile);
                System.out.println("[ACTestPlugin] 已清理 " + toRemove.size() + " 个NPC从Citizens saves.yml");
            }
            
        } catch (Exception e) {
            System.err.println("[ACTestPlugin] 清理Citizens saves.yml时出错: " + e.getMessage());
            e.printStackTrace();
        }
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