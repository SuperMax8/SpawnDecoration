package fr.supermax_8.spawndecoration.manager;

import fr.supermax_8.spawndecoration.blueprint.TrackDecoration;
import org.bukkit.Location;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DecorationManager {

    public static ConcurrentHashMap<String, TrackDecoration> map = new ConcurrentHashMap<>();

    public static void loadDecoration(String name, String modelId, String recordName) {
        List<Location> locs = RecordLocationManager.records.get(recordName);
        TrackDecoration deco = new TrackDecoration(locs, modelId);

        map.put(name, deco);
    }


}