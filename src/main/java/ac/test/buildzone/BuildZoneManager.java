package ac.test.buildzone;

import ac.test.PluginLoader;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BuildZoneManager {

    private final PluginLoader plugin;
    private final Map<String, BuildZone> zones = new HashMap<>();
    private BukkitRunnable tickTask;
    private int tickCounter = 0;

    public BuildZoneManager(PluginLoader plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    private void startTickTask() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickCounter++;
                tickAllZones();
            }
        };
        tickTask.runTaskTimer(plugin, 20L, 20L); // 每秒执行一次
    }

    public void stopTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
        }
    }

    private void tickAllZones() {
        for (BuildZone zone : zones.values()) {
            try {
                // 检查是否需要开始清理倒计时
                if (!zone.isPendingClear()) {
                    if (tickCounter % zone.getClearInterval() == 0) {
                        // 到达清理时间
                        if (zone.hasPlayersInside()) {
                            // 有玩家在区域内，延迟清理
                            zone.setPendingClear(true);
                            zone.setClearDelay(60); // 延迟60秒
                            plugin.getLogger().info("搭路区 " + zone.getName() + " 清理延迟（有玩家在内）");
                        } else {
                            // 没有玩家，直接清理
                            clearZone(zone);
                        }
                    }
                } else {
                    // 正在等待清理
                    zone.decrementClearDelay();
                    
                    if (zone.shouldClear()) {
                        if (zone.hasPlayersInside()) {
                            // 还有玩家，继续延迟
                            zone.setClearDelay(60);
                            plugin.getLogger().info("搭路区 " + zone.getName() + " 清理再次延迟（仍有玩家在内）");
                        } else {
                            // 可以清理了
                            clearZone(zone);
                            zone.setPendingClear(false);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Tick build zone " + zone.getName() + " failed: " + e.getMessage());
            }
        }
    }

    public void clearZone(BuildZone zone) {
        plugin.getLogger().info("正在清理搭路区: " + zone.getName());
        
        Location corner1 = zone.getCorner1();
        Location corner2 = zone.getCorner2();
        World world = zone.getWorld();

        int minX = (int) Math.min(corner1.getX(), corner2.getX());
        int maxX = (int) Math.max(corner1.getX(), corner2.getX());
        int minY = (int) Math.min(corner1.getY(), corner2.getY());
        int maxY = (int) Math.max(corner1.getY(), corner2.getY());
        int minZ = (int) Math.min(corner1.getZ(), corner2.getZ());
        int maxZ = (int) Math.max(corner1.getZ(), corner2.getZ());

        int blockCount = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        block.setType(Material.AIR);
                        blockCount++;
                    }
                }
            }
        }

        plugin.getLogger().info("搭路区 " + zone.getName() + " 清理完成，共清理 " + blockCount + " 个方块");
    }

    public BuildZone createZone(String name, Location corner1, Location corner2) {
        return createZone(name, corner1, corner2, 300);
    }

    public BuildZone createZone(String name, Location corner1, Location corner2, int clearInterval) {
        if (zones.containsKey(name)) {
            return null;
        }

        BuildZone zone = new BuildZone(name, corner1, corner2);
        zone.setClearInterval(clearInterval);
        zones.put(name, zone);
        saveZoneToConfig(name, corner1, corner2, clearInterval);
        plugin.getLogger().info("创建搭路区: " + name + " (清理间隔: " + clearInterval + "秒)");
        return zone;
    }

    public void setZoneInterval(String name, int interval) {
        BuildZone zone = zones.get(name);
        if (zone != null) {
            zone.setClearInterval(interval);
            updateZoneIntervalInConfig(name, interval);
            plugin.getLogger().info("更新搭路区 " + name + " 的清理间隔为 " + interval + "秒");
        }
    }

    public boolean removeZone(String name) {
        BuildZone zone = zones.remove(name);
        if (zone != null) {
            removeZoneFromConfig(name);
            plugin.getLogger().info("删除搭路区: " + name);
            return true;
        }
        return false;
    }

    public BuildZone getZone(String name) {
        return zones.get(name);
    }

    public Collection<BuildZone> getAllZones() {
        return Collections.unmodifiableCollection(zones.values());
    }

    public BuildZone getZoneAtLocation(Location location) {
        for (BuildZone zone : zones.values()) {
            if (zone.isInside(location)) {
                return zone;
            }
        }
        return null;
    }

    public void loadZonesFromConfig() {
        ConfigurationSection zonesSection = plugin.getConfig().getConfigurationSection("buildzones");
        if (zonesSection == null) {
            return;
        }

        for (String zoneName : zonesSection.getKeys(false)) {
            ConfigurationSection zoneSection = zonesSection.getConfigurationSection(zoneName);
            if (zoneSection == null) continue;

            String worldName = zoneSection.getString("world");
            if (worldName == null) continue;

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World not found for build zone: " + worldName);
                continue;
            }

            double x1 = zoneSection.getDouble("corner1.x");
            double y1 = zoneSection.getDouble("corner1.y");
            double z1 = zoneSection.getDouble("corner1.z");
            double x2 = zoneSection.getDouble("corner2.x");
            double y2 = zoneSection.getDouble("corner2.y");
            double z2 = zoneSection.getDouble("corner2.z");

            Location corner1 = new Location(world, x1, y1, z1);
            Location corner2 = new Location(world, x2, y2, z2);

            BuildZone zone = new BuildZone(zoneName, corner1, corner2);
            zone.setClearInterval(zoneSection.getInt("clearInterval", 300));
            zones.put(zoneName, zone);
            plugin.getLogger().info("加载搭路区: " + zoneName);
        }
    }

    public void saveZoneToConfig(String name, Location corner1, Location corner2, int clearInterval) {
        String path = "buildzones." + name;
        plugin.getConfig().set(path + ".world", corner1.getWorld().getName());
        plugin.getConfig().set(path + ".corner1.x", corner1.getX());
        plugin.getConfig().set(path + ".corner1.y", corner1.getY());
        plugin.getConfig().set(path + ".corner1.z", corner1.getZ());
        plugin.getConfig().set(path + ".corner2.x", corner2.getX());
        plugin.getConfig().set(path + ".corner2.y", corner2.getY());
        plugin.getConfig().set(path + ".corner2.z", corner2.getZ());
        plugin.getConfig().set(path + ".clearInterval", clearInterval);
        plugin.saveConfig();
    }

    public void updateZoneIntervalInConfig(String name, int interval) {
        String path = "buildzones." + name + ".clearInterval";
        plugin.getConfig().set(path, interval);
        plugin.saveConfig();
    }

    public void removeZoneFromConfig(String name) {
        plugin.getConfig().set("buildzones." + name, null);
        plugin.saveConfig();
    }
}