package fr.supermax_8.spawndecoration.manager;

import com.google.gson.Gson;
import fr.supermax_8.spawndecoration.SpawnDecorationConfig;
import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import fr.supermax_8.spawndecoration.blueprint.Decoration;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoList;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoration;
import fr.supermax_8.spawndecoration.blueprint.TrackDecoration;
import fr.supermax_8.spawndecoration.utils.SerializationMethods;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.joml.Quaternionf;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DecorationManager {

    @Getter
    private static DecorationManager instance = new DecorationManager();

    @Getter
    private ConcurrentHashMap<UUID, Decoration> decorations = new ConcurrentHashMap<>();
    @Getter
    private ConcurrentHashMap<String, TrackDecoration> trackedDecoMap = new ConcurrentHashMap<>();
    @Getter
    private ConcurrentHashMap<String, List<StaticDecoration>> staticDecoMap = new ConcurrentHashMap<>();

    public DecorationManager() {
        instance = this;
        Bukkit.getScheduler().runTaskTimerAsynchronously(SpawnDecorationPlugin.getInstance(), () -> {
            for (Decoration decoration : decorations.values())
                decoration.tick();
        }, 0, 0);
    }

    public void loadTrackedDecoration(UUID uuid, String name, String modelId, String recordName, boolean smoothPath) {
        List<Location> locs = RecordLocationManager.records.get(recordName);
        TrackDecoration deco = new TrackDecoration(uuid, locs, modelId, smoothPath);

        trackedDecoMap.put(name, deco);
        this.decorations.put(deco.getDummy().getUUID(), deco);
    }

    public void loadStaticDecoration(UUID id, String modelId, Location loc, double scale, Quaternionf rotation, Map<String, List<String>> texts) {
        List<StaticDecoration> decorations = staticDecoMap.computeIfAbsent(modelId, a -> new ArrayList<>());
        try {
            StaticDecoration d = new StaticDecoration(id, modelId, loc, scale, rotation, texts);
            decorations.add(d);
            this.decorations.put(id, d);
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
            StaticDecoList list = new Gson().fromJson(reader, StaticDecoList.class);
            if (list == null) list = new StaticDecoList(new ArrayList<>());
            list.getList().forEach(staticDeco -> {
                if (staticDeco.getId() == null) staticDeco.setId(UUID.randomUUID());
                if (staticDeco.getRotation() == null) staticDeco.setRotation(new Quaternionf());
                if (staticDeco.getScale() <= 0) staticDeco.setScale(1);
            });
            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void editStaticDecos(Consumer<StaticDecoList> edit) {
        StaticDecoList decoList = DecorationManager.getInstance().readStaticDecos();
        edit.accept(decoList);
        DecorationManager.getInstance().writeStaticDecos(decoList);
        SpawnDecorationConfig.reload();
    }

    public void addStaticDeco(Location loc, String modelId, double scale, Quaternionf rotation, Map<String, List<String>> texts) {
        String serializedLocation = SerializationMethods.serializedLocation(loc);
        addStaticDecos(List.of(new StaticDecoList.StaticDeco(UUID.randomUUID(), serializedLocation, modelId, scale, rotation, texts)));
    }

    public void addStaticDecos(Collection<StaticDecoList.StaticDeco> decos) {
        editStaticDecos(decolist -> decolist.getList().addAll(decos));
    }

    public void removeStaticDeco(Location location) {
        String loc = SerializationMethods.serializedLocation(location);

        editStaticDecos(decoList -> {
            StaticDecoList.StaticDeco toRemove = null;
            for (StaticDecoList.StaticDeco deco : decoList.getList()) {
                if (deco.getLocation().equals(loc)) toRemove = deco;
            }
            if (toRemove == null) return;
            decoList.getList().remove(toRemove);
        });
    }

    public void removeStaticDeco(Collection<StaticDecoList.StaticDeco> decos) {
        editStaticDecos(decoList -> {
            for (StaticDecoList.StaticDeco d : decos)
                decoList.getList().removeIf(dec -> dec.getLocation().equals(d.getLocation()));
        });
    }

}