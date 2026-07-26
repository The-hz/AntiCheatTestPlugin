package ac.test.listener;

import ac.test.PluginLoader;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerListener implements Listener {

    private final PluginLoader plugin;

    public PlayerListener(PluginLoader plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 玩家加入时的处理
        Player player = event.getPlayer();
        plugin.getLogger().info("玩家加入: " + player.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 玩家离开时的处理
        Player player = event.getPlayer();
        plugin.getScoreboardManager().removePlayer(player.getUniqueId());
        plugin.getLogger().info("玩家离开: " + player.getName());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // 记录CPS
        if (event.getHand() == EquipmentSlot.HAND) {
            if (event.getAction().name().contains("LEFT")) {
                plugin.getScoreboardManager().recordLeftClick(player.getUniqueId());
            } else if (event.getAction().name().contains("RIGHT")) {
                plugin.getScoreboardManager().recordRightClick(player.getUniqueId());
            }
        }
    }
}