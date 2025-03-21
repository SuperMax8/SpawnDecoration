package fr.supermax_8.spawndecoration;

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
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnDecorationConfig {

    @Getter
    private static int renderRadius;

    @Getter
    private static ConcurrentHashMap<String, List<String>> particle = new ConcurrentHashMap<>();

    @Getter
    private static ConcurrentHashMap<String, Text> text = new ConcurrentHashMap<>();

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
            Section texts = bonebehaviorConfig.getSection("text");
            try {
                texts.getRoutes(false).forEach(r -> {
                    if (texts.isSection(r)) {
                        Section textSection = texts.getSection(r);
                        List<String> lines = textSection.getStringList("lines", List.of("Caelum aka SuperLion is the best mcmmorpg", "default text"));
                        TextDisplay.TextAlignment alignment = TextDisplay.TextAlignment.valueOf(textSection.getString("alignment", "CENTER").toUpperCase());
                        Display.Billboard billboard = Display.Billboard.valueOf(textSection.getString("billboard", "FIXED").toUpperCase());
                        boolean shadow = textSection.getBoolean("shadow", false);
                        boolean seeThroughBlocks = textSection.getBoolean("seeThroughBlocks", false);
                        String[] bgColor = textSection.getString("bgColor", "0;0;0;0").split(";");
                        int[] bgArgb = new int[4];
                        int i = 0;
                        for (String c : bgColor) {
                            bgArgb[i] = Integer.parseInt(c);
                            i++;
                        }
                        float scale = textSection.getDouble("scale", 1.0).floatValue();
                        text.put(textSection.getNameAsString(), new Text(lines, alignment, billboard, shadow, seeThroughBlocks, bgArgb, scale));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

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
                    DecorationManager.getInstance().loadTrackedDecoration(UUID.randomUUID(), key, section.getString("model"), section.getString("record"), section.getBoolean("smoothPath", true));
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error with file " + f.getName() + " !");
                e.printStackTrace();
            }
        }

        File staticDecorations = new File(pluginDir, "staticDecorations.json");
        if (!staticDecorations.exists()) return;

        StaticDecoList list = DecorationManager.getInstance().readStaticDecos();
        list.getList().forEach(staticDeco -> {
            DecorationManager.getInstance().loadStaticDecoration(staticDeco.getId(), staticDeco.getModelId(), SerializationMethods.deserializedLocation(staticDeco.getLocation()), staticDeco.getScale(), staticDeco.getRotation(), staticDeco.getTexts());
        });
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
        DecorationManager.getInstance().getDecorations().values().forEach(Decoration::remove);
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

    public record Text(List<String> lines, TextDisplay.TextAlignment alignment, Display.Billboard billboard,
                       boolean shadow, boolean seeThroughBlocks, int[] bgArgb, float scale) {

    }

}