package fr.supermax_8.spawndecoration;

import com.google.gson.Gson;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import fr.supermax_8.spawndecoration.blueprint.Decoration;
import fr.supermax_8.spawndecoration.blueprint.DriverManager;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoList;
import fr.supermax_8.spawndecoration.manager.DecorationManager;
import fr.supermax_8.spawndecoration.manager.RecordLocationManager;
import fr.supermax_8.spawndecoration.utils.FileUtils;
import fr.supermax_8.spawndecoration.utils.SerializationMethods;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnDecorationConfig {

    @Getter
    private static int renderRadius;

    @Getter
    private static ConcurrentHashMap<String, List<String>> particle = new ConcurrentHashMap<>();

    @Getter
    private static ConcurrentHashMap<String, List<String>> text = new ConcurrentHashMap<>();

    /**
     * Simple load will be change if I add other decoration type
     */
    public static void load() {
        RecordLocationManager.load();
        File pluginDir = SpawnDecorationPlugin.getInstance().getDataFolder();

        particle.clear();
        text.clear();
        try {
            YamlDocument bonebehaviorConfig = YamlDocument.create(
                    new File(pluginDir, "bonebehavior.yml"),
                    getResourceAsStream("bonebehavior.yml"),
                    GeneralSettings.builder().build(),
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.builder().build(),
                    UpdaterSettings.builder()
                            .setVersioning(new BasicVersioning("config-version"))
                            .addIgnoredRoute("1", Route.fromString("particle"))
                            .addIgnoredRoute("1", Route.fromString("text"))
                            .build()
            );

            loadMapFromSection(particle, bonebehaviorConfig.getSection("particle"));
            loadMapFromSection(text, bonebehaviorConfig.getSection("text"));

            YamlDocument config = YamlDocument.create(
                    new File(pluginDir, "config.yml"),
                    getResourceAsStream("config.yml"),
                    GeneralSettings.builder().build(),
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.builder().build(),
                    UpdaterSettings.builder()
                            .setVersioning(new BasicVersioning("config-version"))
                            .build()
            );
            renderRadius = config.getInt("renderRadius");
        } catch (Exception e) {
            e.printStackTrace();
        }


        File decorationsDir = new File(pluginDir, "decorations");
        decorationsDir.mkdirs();
        for (File f : FileUtils.getFilesRecursively(decorationsDir)) {
            if (!f.getName().endsWith(".yml")) return;
            FileConfiguration fc = YamlConfiguration.loadConfiguration(f);
            try {
                for (String key : fc.getKeys(false)) {
                    ConfigurationSection section = fc.getConfigurationSection(key);
                    DecorationManager.getInstance().loadTrackedDecoration(key, section.getString("model"), section.getString("record"), section.getBoolean("smoothPath", true));
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error with file " + f.getName() + " !");
                e.printStackTrace();
            }
        }

        File staticDecorations = new File(pluginDir, "staticDecorations.json");
        if (!staticDecorations.exists()) return;
        try (FileReader reader = new FileReader(staticDecorations)) {
            StaticDecoList list = new Gson().fromJson(reader, StaticDecoList.class);
            list.getList().forEach(staticDeco -> {
                DecorationManager.getInstance().loadStaticDecoration(staticDeco.getModelId(), SerializationMethods.deserializedLocation(staticDeco.getLocation()), staticDeco.getTexts());
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static void loadMapFromSection(Map<String, List<String>> map, Section section) {
        try {
            section.getRoutes(false).forEach(r ->
                    map.put(r.join('.'), section.isList(r) ? section.getStringList(r) : new ArrayList<>() {{
                        add(section.getString(r));
                    }}));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static InputStream getResourceAsStream(String resourcePath) {
        InputStream inputStream = SpawnDecorationConfig.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            System.out.println("The specified resource was not found: " + resourcePath);
            return null;
        }
        return inputStream;
    }

    public static void unLoad() {
        DecorationManager.getInstance().getDecorations().forEach(Decoration::remove);
        RecordLocationManager.records.clear();
        DecorationManager.getInstance().getDecorations().clear();
        DecorationManager.getInstance().getTrackedDecoMap().clear();
        DecorationManager.getInstance().getStaticDecoMap().clear();
        DriverManager.clear();
    }

    public static void reload() {
        unLoad();
        load();
    }

}