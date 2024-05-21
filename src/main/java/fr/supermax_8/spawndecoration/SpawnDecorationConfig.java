package fr.supermax_8.spawndecoration;

import com.google.gson.Gson;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import fr.supermax_8.spawndecoration.blueprint.DriverManager;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoList;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoration;
import fr.supermax_8.spawndecoration.blueprint.TrackDecoration;
import fr.supermax_8.spawndecoration.manager.DecorationManager;
import fr.supermax_8.spawndecoration.manager.RecordLocationManager;
import fr.supermax_8.spawndecoration.utils.FileUtils;
import fr.supermax_8.spawndecoration.utils.SerializationMethods;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.List;

public class SpawnDecorationConfig {

    @Getter
    private static int renderRadius;


    /**
     * Simple load will be change if I add other decoration type
     */
    public static void load() {
        RecordLocationManager.load();
        File pluginDir = SpawnDecorationPlugin.getInstance().getDataFolder();

        try {
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
                    DecorationManager.loadTrackedDecoration(key, section.getString("model"), section.getString("record"));
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
                DecorationManager.loadStaticDecoration(staticDeco.getModelId(), SerializationMethods.deserializedLocation(staticDeco.getLocation()));
            });
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        DriverManager.clear();
        RecordLocationManager.records.clear();
        DecorationManager.trackedDecoMap.values().forEach(TrackDecoration::end);
        DecorationManager.staticDecoMap.values().forEach(l -> l.forEach(StaticDecoration::end));
        DecorationManager.trackedDecoMap.clear();
        DecorationManager.staticDecoMap.clear();
    }

    public static void reload() {
        unLoad();
        load();
    }

}