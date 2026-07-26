package ac.test.bot;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BotData {
    private final String name;
    private final BotType type;
    private final Location spawnLocation;
    private final NPC npc;
    
    private boolean alive;
    private double health;
    private int respawnDelay;
    private Player target;
    private int attackCooldown;

    public BotData(String name, BotType type, Location spawnLocation, NPC npc) {
        this.name = name;
        this.type = type;
        this.spawnLocation = spawnLocation.clone();
        this.npc = npc;
        this.alive = true;
        this.health = 20.0;
        this.respawnDelay = 0;
        this.target = null;
        this.attackCooldown = 0;
    }

    public String getName() {
        return name;
    }

    public BotType getType() {
        return type;
    }

    public Location getSpawnLocation() {
        return spawnLocation.clone();
    }

    public NPC getNpc() {
        return npc;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = Math.max(0, health);
    }

    public int getRespawnDelay() {
        return respawnDelay;
    }

    public void setRespawnDelay(int respawnDelay) {
        this.respawnDelay = respawnDelay;
    }

    public boolean shouldRespawn() {
        return respawnDelay > 0;
    }

    public void decrementRespawnDelay() {
        if (respawnDelay > 0) {
            respawnDelay--;
        }
    }

    public Player getTarget() {
        return target;
    }

    public void setTarget(Player target) {
        this.target = target;
    }

    public int getAttackCooldown() {
        return attackCooldown;
    }

    public void setAttackCooldown(int attackCooldown) {
        this.attackCooldown = attackCooldown;
    }

    public void decrementAttackCooldown() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }
}