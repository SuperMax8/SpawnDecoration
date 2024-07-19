package fr.supermax_8.spawndecoration.manager;

import com.google.gson.Gson;
import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoList;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoration;
import fr.supermax_8.spawndecoration.blueprint.TrackDecoration;
import org.bukkit.Location;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DecorationManager {

    public static ConcurrentHashMap<String, TrackDecoration> trackedDecoMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, List<StaticDecoration>> staticDecoMap = new ConcurrentHashMap<>();

    public static void loadTrackedDecoration(String name, String modelId, String recordName) {
        List<Location> locs = RecordLocationManager.records.get(recordName);
        TrackDecoration deco = new TrackDecoration(locs, modelId);

        trackedDecoMap.put(name, deco);
    }

    public static void loadStaticDecoration(String modelId, Location loc) {
        List<StaticDecoration> decorations = staticDecoMap.computeIfAbsent(modelId, a -> new ArrayList<>());
        decorations.add(new StaticDecoration(modelId, loc));
    }

    public static void writeStaticDecos(StaticDecoList list) {
        File pluginDir = SpawnDecorationPlugin.getInstance().getDataFolder();
        File staticDecorations = new File(pluginDir, "staticDecorations.json");
        try (FileWriter writer = new FileWriter(staticDecorations)) {
            writer.write(new Gson().toJson(list));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static StaticDecoList readStaticDecos() {
        File pluginDir = SpawnDecorationPlugin.getInstance().getDataFolder();
        File staticDecorations = new File(pluginDir, "staticDecorations.json");
        if (!staticDecorations.exists()) return new StaticDecoList(new ArrayList<>());
        try (FileReader reader = new FileReader(staticDecorations)) {
            return new Gson().fromJson(reader, StaticDecoList.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}