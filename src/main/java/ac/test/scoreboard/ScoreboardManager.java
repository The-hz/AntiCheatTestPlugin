package ac.test.scoreboard;

import ac.test.PluginLoader;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final PluginLoader plugin;
    private BukkitRunnable updateTask;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final Map<UUID, org.bukkit.scoreboard.Scoreboard> playerScoreboards = new HashMap<>();

    public ScoreboardManager(PluginLoader plugin) {
        this.plugin = plugin;
    }

    public void start() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllScoreboards();
            }
        };
        updateTask.runTaskTimer(plugin, 0L, 5L); // 每5tick更新一次
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        for (org.bukkit.scoreboard.Scoreboard scoreboard : playerScoreboards.values()) {
            // 清理
        }
        playerScoreboards.clear();
    }

    private void updateAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                updatePlayerScoreboard(player);
            } catch (Exception e) {
                // 忽略错误
            }
        }
    }

    private void updatePlayerScoreboard(Player player) {
        PlayerData data = playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData());
        data.update(player);

        org.bukkit.scoreboard.Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            playerScoreboards.put(player.getUniqueId(), scoreboard);
            
            Objective objective = scoreboard.registerNewObjective("actest", Criteria.DUMMY, "§6§lAC Test");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        Objective objective = scoreboard.getObjective("actest");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("actest", Criteria.DUMMY, "§6§lAC Test");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // 清除旧的分数
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        int line = 15;

        // Ping
        objective.getScore("§ePing: §f" + data.getPing() + "ms").setScore(line--);
        
        // CPS Left
        objective.getScore("§eCPS L: §f" + data.getCpsLeft()).setScore(line--);
        
        // CPS Right
        objective.getScore("§eCPS R: §f" + data.getCpsRight()).setScore(line--);
        
        // BPS (Blocks Per Second)
        objective.getScore("§eBPS: §f" + String.format("%.2f", data.getBps())).setScore(line--);
        
        // Is Sprinting
        objective.getScore("§eSprint: §f" + (data.isSprinting() ? "§aYes" : "§cNo")).setScore(line--);
        
        // Is On Ground
        objective.getScore("§eGround: §f" + (data.isOnGround() ? "§aYes" : "§cNo")).setScore(line--);
        
        // Is Sneaking
        objective.getScore("§eSneak: §f" + (data.isSneaking() ? "§aYes" : "§cNo")).setScore(line--);

        player.setScoreboard(scoreboard);
    }

    public void removePlayer(UUID uuid) {
        playerDataMap.remove(uuid);
        playerScoreboards.remove(uuid);
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void recordLeftClick(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            data.recordLeftClick();
        }
    }

    public void recordRightClick(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            data.recordRightClick();
        }
    }

    public static class PlayerData {
        private int ping;
        private int cpsLeft;
        private int cpsRight;
        private double bps;
        private boolean sprinting;
        private boolean onGround;
        private boolean sneaking;

        // CPS计算
        private final java.util.Queue<Long> leftClicks = new java.util.LinkedList<>();
        private final java.util.Queue<Long> rightClicks = new java.util.LinkedList<>();

        // BPS计算
        private LocationData lastLocation;
        private long lastMoveTime;

        public void update(Player player) {
            // Ping
            this.ping = player.getPing();

            // CPS
            long now = System.currentTimeMillis();
            
            // 清理过期的点击记录（1秒前）
            while (!leftClicks.isEmpty() && now - leftClicks.peek() > 1000) {
                leftClicks.poll();
            }
            while (!rightClicks.isEmpty() && now - rightClicks.peek() > 1000) {
                rightClicks.poll();
            }
            
            this.cpsLeft = leftClicks.size();
            this.cpsRight = rightClicks.size();

            // BPS
            LocationData currentLoc = new LocationData(
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ()
            );
            
            if (lastLocation != null) {
                double distance = Math.sqrt(
                    Math.pow(currentLoc.x - lastLocation.x, 2) +
                    Math.pow(currentLoc.z - lastLocation.z, 2)
                );
                
                long timeDiff = now - lastMoveTime;
                if (timeDiff > 0) {
                    this.bps = (distance / timeDiff) * 1000; // 转换为每秒
                }
            }
            
            lastLocation = currentLoc;
            lastMoveTime = now;

            // 状态
            this.sprinting = player.isSprinting();
            this.onGround = player.isOnGround();
            this.sneaking = player.isSneaking();
        }

        public void recordLeftClick() {
            leftClicks.offer(System.currentTimeMillis());
        }

        public void recordRightClick() {
            rightClicks.offer(System.currentTimeMillis());
        }

        public int getPing() {
            return ping;
        }

        public int getCpsLeft() {
            return cpsLeft;
        }

        public int getCpsRight() {
            return cpsRight;
        }

        public double getBps() {
            return bps;
        }

        public boolean isSprinting() {
            return sprinting;
        }

        public boolean isOnGround() {
            return onGround;
        }

        public boolean isSneaking() {
            return sneaking;
        }
    }

    private static class LocationData {
        final double x, y, z;

        LocationData(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}