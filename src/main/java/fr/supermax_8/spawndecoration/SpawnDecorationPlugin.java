package fr.supermax_8.spawndecoration;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import fr.supermax_8.spawndecoration.commands.SpawnDecorationCommand;
import fr.supermax_8.spawndecoration.manager.DecorationManager;
import fr.supermax_8.spawndecoration.utils.TemporaryListener;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class SpawnDecorationPlugin extends JavaPlugin {

    @Getter
    private static SpawnDecorationPlugin instance;

    public static String version;

    @Override
    public void onEnable() {
        instance = this;
        version = getDescription().getVersion();
        Metrics metrics = new Metrics(this, 17158);
        metrics.addCustomChart(new Metrics.SingleLineChart("numberofdecoration", () -> DecorationManager.map.size()));

        if (Bukkit.getPluginManager().getPlugin("ModelEngine") == null) {
            Bukkit.getLogger().warning("SpawnDecoration | Plugin turn OFF, ModelEngine is not on the server ! You should have ModelEngine to use this plugin !");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!ModelEngineAPI.api.getGenerator().isInitialized())
            new TemporaryListener<>(ModelRegistrationEvent.class, EventPriority.NORMAL, e -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        loadModelEngineUsers();
                    }
                }.runTaskLater(getInstance(), 20);
                return true;
            });
        else loadModelEngineUsers();

        getCommand("spawndecoration").setExecutor(new SpawnDecorationCommand());
    }

    @Override
    public void onDisable() {
        SpawnDecorationConfig.unLoad();
    }

    public void loadModelEngineUsers() {
        SpawnDecorationConfig.load();
    }

}