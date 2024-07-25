package fr.supermax_8.spawndecoration.manager;

import com.google.gson.Gson;
import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import fr.supermax_8.spawndecoration.blueprint.Decoration;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoList;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoration;
import fr.supermax_8.spawndecoration.blueprint.TrackDecoration;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DecorationManager {

    @Getter
    private static DecorationManager instance = new DecorationManager();

    @Getter
    private CopyOnWriteArrayList<Decoration> decorations = new CopyOnWriteArrayList<>();
    @Getter
    private ConcurrentHashMap<String, TrackDecoration> trackedDecoMap = new ConcurrentHashMap<>();
    @Getter
    private ConcurrentHashMap<String, List<StaticDecoration>> staticDecoMap = new ConcurrentHashMap<>();

    public DecorationManager() {
        instance = this;
        Bukkit.getScheduler().runTaskTimerAsynchronously(SpawnDecorationPlugin.getInstance(), () -> {
            for (Decoration decoration : decorations)
                decoration.tick();
        }, 0, 0);
    }

    public void loadTrackedDecoration(String name, String modelId, String recordName, boolean smoothPath) {
        List<Location> locs = RecordLocationManager.records.get(recordName);
        TrackDecoration deco = new TrackDecoration(locs, modelId, smoothPath);

        trackedDecoMap.put(name, deco);
        this.decorations.add(deco);
    }

    public void loadStaticDecoration(String modelId, Location loc) {
        List<StaticDecoration> decorations = staticDecoMap.computeIfAbsent(modelId, a -> new ArrayList<>());
        try {
            StaticDecoration d = new StaticDecoration(modelId, loc);
            decorations.add(d);
            this.decorations.add(d);
        } catch (Exception e) {
            System.out.println("Error while loading decoration: " + modelId + " at " + loc);
            e.printStackTrace();
        }
    }

    public void writeStaticDecos(StaticDecoList list) {
        File pluginDir = SpawnDecorationPlugin.getInstance().getDataFolder();
        File staticDecorations = new File(pluginDir, "staticDecorations.json");
        try (FileWriter writer = new FileWriter(staticDecorations)) {
            writer.write(new Gson().toJson(list));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public StaticDecoList readStaticDecos() {
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