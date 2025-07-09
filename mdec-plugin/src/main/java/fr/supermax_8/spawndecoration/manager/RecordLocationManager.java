package fr.supermax_8.spawndecoration.manager;

import fr.supermax_8.spawndecoration.ModelEngineDecorationPlugin;
import fr.supermax_8.spawndecoration.utils.BukkitListener;
import fr.supermax_8.spawndecoration.utils.FileUtils;
import fr.supermax_8.spawndecoration.utils.SerializationMethods;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
        File recordDir = new File(ModelEngineDecorationPlugin.getInstance().getDataFolder(), "recordlocation");
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

            Location lastpos = null;
            boolean a = false;

            @Override
            public void run() {
                Location pLoc = p.getLocation().clone();
                locs.add(pLoc);
            }
        }.runTaskTimer(ModelEngineDecorationPlugin.getInstance(), 0, 0);

        BukkitListener.registerPlayerListener(p, PlayerInteractEvent.class, e -> {
            try {
                File recordDir = new File(ModelEngineDecorationPlugin.getInstance().getDataFolder(), "recordlocation");
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

            /*new BukkitRunnable() {
                int i = 0;
                @Override
                public void run() {
                    if (i == 20 * 15) cancel();
                    for (Location loc : smoothed) {
                        loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 5, 0, 0, 0, 0);
                    }
                    i++;
                }
            }.runTaskTimer(SpawnDecorationPlugin.getInstance(), 0, 0);*/
            return true;
        });
    }


}