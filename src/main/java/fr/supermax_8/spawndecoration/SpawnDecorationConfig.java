package fr.supermax_8.spawndecoration;

import fr.supermax_8.spawndecoration.blueprint.TrackDecoration;
import fr.supermax_8.spawndecoration.manager.DecorationManager;
import fr.supermax_8.spawndecoration.manager.RecordLocationManager;
import fr.supermax_8.spawndecoration.utils.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class SpawnDecorationConfig {


    /**
     * Simple load will be change if I add other decoration type
     */
    public static void load() {
        RecordLocationManager.load();
        File pluginDir = SpawnDecorationPlugin.getInstance().getDataFolder();
        File decorationsDir = new File(pluginDir, "decorations");
        decorationsDir.mkdirs();
        for (File f : FileUtils.getFilesRecursively(decorationsDir)) {
            if (!f.getName().endsWith(".yml")) return;
            FileConfiguration fc = YamlConfiguration.loadConfiguration(f);
            try {
                for (String key : fc.getKeys(false)) {
                    ConfigurationSection section = fc.getConfigurationSection(key);
                    DecorationManager.loadDecoration(key, section.getString("model"), section.getString("record"));
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error with file " + f.getName() + " !");
                e.printStackTrace();
            }
        }
    }

    public static void unLoad() {
        RecordLocationManager.records.clear();
        DecorationManager.map.values().forEach(TrackDecoration::end);
        DecorationManager.map.clear();
    }

    public static void reload() {
        unLoad();
        load();
    }

}