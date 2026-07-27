package ac.test.bot;

import ac.test.PluginLoader;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCCreateEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class BotManager implements Listener {

    private final PluginLoader plugin;
    private final Map<String, BotData> bots = new HashMap<>();
    private final Map<Integer, String> npcIdToBotMap = new HashMap<>();
    private BukkitRunnable tickTask;

    public BotManager(PluginLoader plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startTickTask();
    }

    private void startTickTask() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickAllBots();
            }
        };
        tickTask.runTaskTimer(plugin, 1L, 1L);
    }

    public void stopTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
        }
    }

    private void tickAllBots() {
        for (BotData bot : new ArrayList<>(bots.values())) {
            try {
                if (!bot.isAlive() && bot.shouldRespawn()) {
                    bot.decrementRespawnDelay();
                    if (!bot.shouldRespawn()) {
                        respawnBot(bot);
                    }
                } else if (bot.isAlive() && bot.getNpc() != null && bot.getNpc().isSpawned()) {
                    tickBot(bot);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Tick bot " + bot.getName() + " failed: " + e.getMessage());
            }
        }
    }

    private void tickBot(BotData bot) {
        switch (bot.getType()) {
            case STATIONARY:
                tickStationaryBot(bot);
                break;
            case COUNTER:
                tickCounterBot(bot);
                break;
            case PVP:
                tickPVPBot(bot);
                break;
            case PASSIVE:
                tickPassiveBot(bot);
                break;
        }
    }

    private void tickStationaryBot(BotData bot) {
        NPC npc = bot.getNpc();
        if (npc != null && npc.isSpawned()) {
            Location current = npc.getStoredLocation();
            if (current != null && current.distanceSquared(bot.getSpawnLocation()) > 0.01) {
                npc.teleport(bot.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }
    }

    private void tickCounterBot(BotData bot) {
        // 反击假人行为类似站桩假人，保持位置
        tickStationaryBot(bot);
        
        // 处理反击冷却
        if (bot.getAttackCooldown() > 0) {
            bot.decrementAttackCooldown();
        }
    }

    private void tickPVPBot(BotData bot) {
        NPC npc = bot.getNpc();
        if (npc == null || !npc.isSpawned()) return;

        if (bot.getAttackCooldown() > 0) {
            bot.decrementAttackCooldown();
        }

        // 检查目标
        if (bot.getTarget() == null || !bot.getTarget().isOnline()) {
            findNewTarget(bot);
        }

        Player target = bot.getTarget();
        if (target != null && target.isOnline()) {
            Location botLoc = npc.getStoredLocation();
            Location targetLoc = target.getLocation();

            if (!botLoc.getWorld().equals(targetLoc.getWorld())) {
                npc.teleport(bot.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                bot.setTarget(null);
                return;
            }

            double distanceSquared = botLoc.distanceSquared(targetLoc);
            
            // 如果玩家离开范围，返回出生点
            if (distanceSquared > 100.0) { // 10格范围
                npc.teleport(bot.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                bot.setTarget(null);
                return;
            }

            // 面向目标
            faceTarget(npc, target);

            // 攻击
            if (bot.getAttackCooldown() <= 0 && distanceSquared < 9.0) {
                attackPlayer(npc, target);
                bot.setAttackCooldown(12);
            }
        }
    }

    private void tickPassiveBot(BotData bot) {
        // 被动假人什么都不做，只是站着
        tickStationaryBot(bot);
    }

    private void findNewTarget(BotData bot) {
        NPC npc = bot.getNpc();
        if (npc == null || !npc.isSpawned()) return;

        Location botLoc = npc.getStoredLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(botLoc.getWorld())) {
                double distanceSquared = player.getLocation().distanceSquared(botLoc);
                if (distanceSquared <= 100.0) { // 10格范围
                    bot.setTarget(player);
                    return;
                }
            }
        }
    }

    private void faceTarget(NPC npc, Player target) {
        Location botLoc = npc.getStoredLocation();
        Location targetLoc = target.getLocation();

        double dx = targetLoc.getX() - botLoc.getX();
        double dz = targetLoc.getZ() - botLoc.getZ();
        double dy = targetLoc.getY() - botLoc.getY();

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        botLoc.setYaw(yaw);
        botLoc.setPitch(pitch);
        npc.faceLocation(targetLoc);
    }

    private void attackPlayer(NPC npc, Player target) {
        // 模拟攻击
        target.damage(1.0);
        
        // 添加击退效果
        applyKnockback(npc, target);
        
        // 播放挥动手臂动画
        if (npc.getEntity() instanceof Player) {
            Player botPlayer = (Player) npc.getEntity();
            botPlayer.swingMainHand();
        }
    }

    private void applyKnockback(NPC npc, Player target) {
        Location botLoc = npc.getStoredLocation();
        Location targetLoc = target.getLocation();
        
        // 计算击退方向（从假人指向目标）
        double dx = targetLoc.getX() - botLoc.getX();
        double dz = targetLoc.getZ() - botLoc.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        if (distance > 0) {
            // 归一化并设置击退强度
            double knockbackStrength = 0.4;
            double verticalStrength = 0.4;
            
            dx /= distance;
            dz /= distance;
            
            // 设置速度向量（水平击退 + 轻微向上）
            target.setVelocity(new org.bukkit.util.Vector(dx * knockbackStrength, verticalStrength, dz * knockbackStrength));
        }
    }

    public BotData createBot(String name, BotType type, Location location) {
        return createBot(name, type, location, null);
    }

    public BotData createBot(String name, BotType type, Location location, UUID storedUuid) {
        if (bots.containsKey(name)) {
            return null;
        }

        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        NPC npc = registry.createNPC(EntityType.PLAYER, name);
        
        npc.spawn(location);
        npc.setProtected(false);
        
        BotData botData = new BotData(name, type, location, npc);
        bots.put(name, botData);
        npcIdToBotMap.put(npc.getId(), name);

        // 根据类型设置属性
        setupBotByType(botData);

        // 保存到配置，同时存储UUID
        saveBotToConfig(name, type, location, npc.getUniqueId());

        plugin.getLogger().info("生成假人: " + name + " (" + type.getDisplayName() + "), UUID: " + npc.getUniqueId());
        return botData;
    }

    private void setupBotByType(BotData bot) {
        NPC npc = bot.getNpc();
        
        switch (bot.getType()) {
            case STATIONARY:
            case COUNTER:
                // 无敌设置
                npc.setProtected(true);
                break;
            case PVP:
            case PASSIVE:
                // 可死亡
                npc.setProtected(false);
                break;
        }
    }

    public boolean removeBot(String name) {
        BotData bot = bots.remove(name);
        if (bot != null) {
            npcIdToBotMap.remove(bot.getNpc().getId());
            bot.getNpc().destroy();
            return true;
        }
        return false;
    }

    public BotData getBot(String name) {
        return bots.get(name);
    }

    public BotData getBotByNpcId(int npcId) {
        String botName = npcIdToBotMap.get(npcId);
        return botName != null ? bots.get(botName) : null;
    }

    public Collection<BotData> getAllBots() {
        return Collections.unmodifiableCollection(bots.values());
    }

    private void respawnBot(BotData bot) {
        NPC npc = bot.getNpc();
        if (npc != null) {
            npc.spawn(bot.getSpawnLocation());
            bot.setAlive(true);
            bot.setHealth(20.0);
            bot.setTarget(null);
            plugin.getLogger().info("假人重生: " + bot.getName());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null) return;

        BotData bot = getBotByNpcId(npc.getId());
        if (bot == null) return;

        event.setCancelled(true);

        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }

        if (attacker == null) return;

        switch (bot.getType()) {
            case STATIONARY:
                // 无敌，只播放受击动画
                playHurtAnimation(npc);
                break;
                
            case COUNTER:
                // 无敌，播放受击动画，然后反击
                playHurtAnimation(npc);
                if (bot.getAttackCooldown() <= 0) {
                    counterAttack(npc, attacker);
                    bot.setAttackCooldown(10);
                }
                break;
                
            case PVP:
                // 受到伤害，有击退
                event.setCancelled(false);
                bot.setTarget(attacker);
                bot.setHealth(bot.getHealth() - event.getFinalDamage());
                if (bot.getHealth() <= 0) {
                    handleBotDeath(bot);
                }
                break;
                
            case PASSIVE:
                // 受到伤害，不还手
                event.setCancelled(false);
                bot.setHealth(bot.getHealth() - event.getFinalDamage());
                if (bot.getHealth() <= 0) {
                    handleBotDeath(bot);
                }
                break;
        }
    }

    private void counterAttack(NPC npc, Player target) {
        target.damage(1.0);

        applyKnockback(npc, target);

        faceTarget(npc, target);

        if (npc.getEntity() instanceof Player) {
            Player botPlayer = (Player) npc.getEntity();
            botPlayer.swingMainHand();
        }
    }

    private void playHurtAnimation(NPC npc) {
        if (npc.getEntity() instanceof Player) {
            Player botPlayer = (Player) npc.getEntity();
            botPlayer.playHurtAnimation(0);
        }
    }

    private void handleBotDeath(BotData bot) {
        bot.setAlive(false);
        bot.setRespawnDelay(100); // 5秒后重生
        
        NPC npc = bot.getNpc();
        if (npc != null && npc.isSpawned()) {
            // 播放死亡动画
            if (npc.getEntity() instanceof Player) {
                Player botPlayer = (Player) npc.getEntity();
                botPlayer.playHurtAnimation(0);
            }
            npc.despawn();
        }
        
        plugin.getLogger().info("假人死亡: " + bot.getName() + "，将在5秒后重生");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null) return;

        BotData bot = getBotByNpcId(npc.getId());
        if (bot != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    public void loadBotsFromConfig() {
        ConfigurationSection botsSection = plugin.getConfig().getConfigurationSection("bots");
        if (botsSection == null) {
            return;
        }

        for (String botName : botsSection.getKeys(false)) {
            ConfigurationSection botSection = botsSection.getConfigurationSection(botName);
            if (botSection == null) continue;

            String typeId = botSection.getString("type");
            if (typeId == null) continue;

            BotType type = BotType.fromId(typeId);
            if (type == null) {
                plugin.getLogger().warning("Unknown bot type: " + typeId);
                continue;
            }

            String worldName = botSection.getString("location.world");
            double x = botSection.getDouble("location.x");
            double y = botSection.getDouble("location.y");
            double z = botSection.getDouble("location.z");
            float yaw = (float) botSection.getDouble("location.yaw", 0);
            float pitch = (float) botSection.getDouble("location.pitch", 0);

            if (worldName == null) continue;

            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World not found: " + worldName);
                continue;
            }

            Location location = new Location(world, x, y, z, yaw, pitch);
            String uuidStr = botSection.getString("uuid");
            UUID storedUuid = uuidStr != null ? UUID.fromString(uuidStr) : null;
            createBot(botName, type, location, storedUuid);
        }
    }

    public void saveBotToConfig(String name, BotType type, Location location, UUID npcId) {
        String path = "bots." + name;
        plugin.getConfig().set(path + ".type", type.getId());
        plugin.getConfig().set(path + ".location.world", location.getWorld().getName());
        plugin.getConfig().set(path + ".location.x", location.getX());
        plugin.getConfig().set(path + ".location.y", location.getY());
        plugin.getConfig().set(path + ".location.z", location.getZ());
        plugin.getConfig().set(path + ".location.yaw", location.getYaw());
        plugin.getConfig().set(path + ".location.pitch", location.getPitch());
        plugin.getConfig().set(path + ".uuid", npcId.toString());
        plugin.saveConfig();
    }

    public void removeBotFromConfig(String name) {
        plugin.getConfig().set("bots." + name, null);
        plugin.saveConfig();
    }

    @EventHandler
    public void onNPCCreate(NPCCreateEvent event) {
        NPC npc = event.getNPC();
        if (npc == null || npc.getName() == null) {
            return;
        }

        ConfigurationSection botsSection = plugin.getConfig().getConfigurationSection("bots");
        if (botsSection == null) {
            return;
        }

        String npcName = npc.getName();
        ConfigurationSection botSection = botsSection.getConfigurationSection(npcName);
        if (botSection == null) {
            return;
        }

        // 获取配置中存储的UUID
        String storedUuidStr = botSection.getString("uuid");
        if (storedUuidStr == null) {
            // 没有UUID记录，可能是旧配置，直接移除
            plugin.getLogger().info("检测到无UUID记录的假人: " + npcName + "，正在销毁...");
            npc.destroy();
            return;
        }

        int storedUuidHash = UUID.fromString(storedUuidStr).hashCode();
        int npcUuidHash = npc.getUniqueId().hashCode();

        String npcUuidStr = npc.getUniqueId().toString();

        // 比对UUID
        if (storedUuidHash != npcUuidHash) {
            plugin.getLogger().info("检测到UUID不匹配的重复假人: " + npcName +
                " (配置UUID: " + storedUuidStr + ", Citizens UUID: " + npcUuidStr + ")，正在销毁...");
            npc.destroy();
        }
    }
}