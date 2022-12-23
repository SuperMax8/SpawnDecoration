package fr.supermax_8.spawndecoration.manager;

import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import fr.supermax_8.spawndecoration.utils.FileUtils;
import fr.supermax_8.spawndecoration.utils.SerializationMethods;
import fr.supermax_8.spawndecoration.utils.TemporaryListener;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RecordLocationManager {

    public static ConcurrentHashMap<String, List<Location>> records = new ConcurrentHashMap<>();

    public static List<String> toStringList(List<Location> locs) {
        List<String> list = new ArrayList<>();
        for (Location loc : locs) {
            list.add(SerializationMethods.serializedLocation(loc));
        }
        return list;
    }

    public static List<Location> toLocationList(List<String> locs) {
        List<Location> list = new ArrayList<>();
        for (String loc : locs) {
            list.add(SerializationMethods.deserializedLocation(loc));
        }
        return list;
    }

    public static void load() {
        File recordDir = new File(SpawnDecorationPlugin.getInstance().getDataFolder(), "recordlocation");
        try {
            recordDir.mkdirs();
            for (File f : FileUtils.getFilesRecursively(recordDir)) {
                FileConfiguration fc = YamlConfiguration.loadConfiguration(f);
                records.put(f.getName().replace(".yml", ""), toLocationList(fc.getStringList("record")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void startRecord(Player p, String recordName) {
        p.sendMessage("§6Starting record " + recordName + " §7§lClick to end record !");
        List<Location> locs = new ArrayList<>();
        BukkitTask run = new BukkitRunnable() {
            @Override
            public void run() {
                locs.add(p.getLocation().clone());
                p.sendMessage("§6Adding new Position: §7" + p.getLocation());
            }
        }.runTaskTimer(SpawnDecorationPlugin.getInstance(), 0, 0);
        new TemporaryListener<PlayerInteractEvent>(PlayerInteractEvent.class, EventPriority.NORMAL, e -> {
            if (!p.equals(e.getPlayer())) return false;
            try {
                File recordDir = new File(SpawnDecorationPlugin.getInstance().getDataFolder(), "recordlocation");
                File record = new File(recordDir, recordName + ".yml");
                if (!record.exists()) record.createNewFile();
                FileConfiguration config = YamlConfiguration.loadConfiguration(record);
                config.set("record", RecordLocationManager.toStringList(locs));
                config.save(record);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            p.sendMessage("§6End record " + recordName);
            run.cancel();
            return true;
        });
    }

}