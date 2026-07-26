package ac.test.listener;

import ac.test.PluginLoader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public class EntityListener implements Listener {

    private final PluginLoader plugin;

    public EntityListener(PluginLoader plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntitySpawn(EntitySpawnEvent event) {
        // 禁用除了玩家之外的所有生物生成
        if (!(event.getEntity() instanceof org.bukkit.entity.Player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // 禁用所有生物生成
        event.setCancelled(true);
    }
}