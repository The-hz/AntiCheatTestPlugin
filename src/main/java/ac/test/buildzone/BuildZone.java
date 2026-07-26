package ac.test.buildzone;

import org.bukkit.Location;
import org.bukkit.World;

public class BuildZone {
    private final String name;
    private final Location corner1;
    private final Location corner2;
    private final World world;
    
    private int clearInterval; // 清理间隔（秒）
    private int clearDelay;    // 延迟清理计时器
    private boolean pendingClear;

    public BuildZone(String name, Location corner1, Location corner2) {
        this.name = name;
        this.corner1 = corner1.clone();
        this.corner2 = corner2.clone();
        this.world = corner1.getWorld();
        this.clearInterval = 300; // 默认5分钟
        this.clearDelay = 0;
        this.pendingClear = false;
    }

    public String getName() {
        return name;
    }

    public Location getCorner1() {
        return corner1.clone();
    }

    public Location getCorner2() {
        return corner2.clone();
    }

    public World getWorld() {
        return world;
    }

    public int getClearInterval() {
        return clearInterval;
    }

    public void setClearInterval(int clearInterval) {
        this.clearInterval = clearInterval;
    }

    public boolean isPendingClear() {
        return pendingClear;
    }

    public void setPendingClear(boolean pendingClear) {
        this.pendingClear = pendingClear;
    }

    public int getClearDelay() {
        return clearDelay;
    }

    public void setClearDelay(int clearDelay) {
        this.clearDelay = clearDelay;
    }

    public void decrementClearDelay() {
        if (clearDelay > 0) {
            clearDelay--;
        }
    }

    public boolean shouldClear() {
        return pendingClear && clearDelay <= 0;
    }

    public boolean isInside(Location location) {
        if (!location.getWorld().equals(world)) {
            return false;
        }

        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    public boolean hasPlayersInside() {
        for (org.bukkit.entity.Player player : world.getPlayers()) {
            if (isInside(player.getLocation())) {
                return true;
            }
        }
        return false;
    }
}