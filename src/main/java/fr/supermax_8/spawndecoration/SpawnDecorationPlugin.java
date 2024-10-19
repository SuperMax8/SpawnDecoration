package fr.supermax_8.spawndecoration;

import com.github.retrooper.packetevents.PacketEvents;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import fr.supermax_8.spawndecoration.blueprint.DriverManager;
import fr.supermax_8.spawndecoration.commands.MegDecorationCommand;
import fr.supermax_8.spawndecoration.listeners.InteractListener;
import fr.supermax_8.spawndecoration.manager.DecorationManager;
import fr.supermax_8.spawndecoration.utils.TemporaryListener;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

public final class SpawnDecorationPlugin extends JavaPlugin {

    @Getter
    private static SpawnDecorationPlugin instance;

    public static String version;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();

        SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(this);
        APIConfig settings = new APIConfig(PacketEvents.getAPI())
                .usePlatformLogger();

        EntityLib.init(platform, settings);

        instance = this;
        version = getDescription().getVersion();
        Metrics metrics = new Metrics(this, 17158);
        metrics.addCustomChart(new Metrics.SingleLineChart("numberofdecoration", () -> DecorationManager.getInstance().getDecorations().size()));


        if (Bukkit.getPluginManager().getPlugin("ModelEngine") == null) {
            Bukkit.getLogger().warning("ModelEngineDecoration | Plugin turn OFF, ModelEngine is not on the server ! You should have ModelEngine to use this plugin !");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!ModelEngineAPI.getAPI().getModelGenerator().isInitialized())
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

        DriverManager.getInstance().initTask();

        Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(this).build();
        lamp.register(new MegDecorationCommand());

        getServer().getPluginManager().registerEvents(new InteractListener(), this);
    }

    @Override
    public void onDisable() {
        SpawnDecorationConfig.unLoad();
        PacketEvents.getAPI().terminate();
    }

    public void loadModelEngineUsers() {
        try {
            SpawnDecorationConfig.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}