package fr.supermax_8.spawndecoration;

import com.github.retrooper.packetevents.PacketEvents;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.Hitbox;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import com.ticxo.modelengine.api.model.bone.behavior.BoneBehaviorRegistry;
import com.ticxo.modelengine.api.model.bone.behavior.BoneBehaviorType;
import com.ticxo.modelengine.core.model.bone.behavior.SubHitboxImpl;
import com.ticxo.modelengine.core.model.bone.render.SubHitboxRendererImpl;
import fr.supermax_8.spawndecoration.blueprint.DriverManager;
import fr.supermax_8.spawndecoration.commands.MegDecorationCommand;
import fr.supermax_8.spawndecoration.listeners.InteractListener;
import fr.supermax_8.spawndecoration.manager.DecorationManager;
import fr.supermax_8.spawndecoration.utils.BukkitListener;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3f;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.util.logging.Level;

public class ModelEngineDecoration {
    @Getter
    private static ModelEngineDecoration instance;

    public static String version;
    private ModelEngineDecorationPlugin plugin;

    public ModelEngineDecoration(ModelEngineDecorationPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }


    protected void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(false);
        PacketEvents.getAPI().load();
    }

    protected void onEnable() {
        BukkitListener.init(plugin);
        PacketEvents.getAPI().init();

        SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(plugin);
        APIConfig settings = new APIConfig(PacketEvents.getAPI())
                .usePlatformLogger();

        EntityLib.init(platform, settings);

        instance = this;
        version = plugin.getDescription().getVersion();
        Metrics metrics = new Metrics(plugin, 17158);
        metrics.addCustomChart(new Metrics.SingleLineChart("numberofdecoration", () -> DecorationManager.getInstance().getDecorations().size()));

        plugin.saveResource("generate_megcombined.py", true);

        if (Bukkit.getPluginManager().getPlugin("ModelEngine") == null) {
            Bukkit.getLogger().warning("ModelEngineDecoration | Plugin turn OFF, ModelEngine is not on the server ! You should have ModelEngine to use this plugin !");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        BoneBehaviorType<SubHitboxImpl> build = BoneBehaviorType.Builder.of(SubHitboxImpl::new, null, "sub_hitbox")
                .required("dimension", Hitbox.class)
                .optional("obb", Boolean.class)
                .optional("origin", Vector3f.class)
                .optional("rotation", Vector3f.class)
                .optional("cube_origin", Vector3f.class)
                .renderType(SubHitboxRendererImpl::new)
                .ignoreCubes()
                .build();

        BoneBehaviorRegistry boneBehavior = ModelEngineAPI.getAPI().getBoneBehaviorRegistry();


        if (!ModelEngineAPI.getAPI().getModelGenerator().isInitialized())
            BukkitListener.registerListener(ModelRegistrationEvent.class, e -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        loadModelEngineUsers();
                    }
                }.runTaskLater(plugin, 20);
                return true;
            });
        else loadModelEngineUsers();

        DriverManager.getInstance().initTask();

        Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(plugin)
                .build();
        lamp.register(new MegDecorationCommand());

        plugin.getServer().getPluginManager().registerEvents(new InteractListener(), plugin);
    }

    protected void onDisable() {
        SpawnDecorationConfig.unLoad();
        PacketEvents.getAPI().terminate();
    }

    private void loadModelEngineUsers() {
        try {
            SpawnDecorationConfig.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        Bukkit.getLogger().log(Level.INFO, "§8[§6§lModelEngineDecoration§8] §r§f" + message);
    }

}