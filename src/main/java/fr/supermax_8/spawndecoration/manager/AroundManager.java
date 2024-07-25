package fr.supermax_8.spawndecoration.manager;

import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/*1.5*/
public class AroundManager implements Runnable, Listener {

    @Getter
    private static final AroundManager instance = new AroundManager();
    @Getter
    private final ConcurrentHashMap<ChunkCoord, ConcurrentHashMap<Location, List<Around>>> arounds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Around> aroundUUIDs = new ConcurrentHashMap<>();

    @Getter
    private ConcurrentHashMap<UUID, Set<UUID>> aroundsPlayerIsIn = new ConcurrentHashMap<>();
    private double biggestRadius = 0;

    public AroundManager() {
        Bukkit.getScheduler().runTaskTimer(SpawnDecorationPlugin.getInstance(), this, 0, 0);
        SpawnDecorationPlugin.getInstance().getServer().getPluginManager().registerEvents(this, SpawnDecorationPlugin.getInstance());
    }

    @Override
    public synchronized void run() {
        int lengthCheck = (int) Math.max(1, Math.ceil(biggestRadius / 16));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
            Location pLoc = p.getLocation();
            World world = pLoc.getWorld();
            ChunkCoord playerChunkCoord = new ChunkCoord(pLoc);
            UUID pId = p.getUniqueId();
            Set<UUID> playerArounds = aroundsPlayerIsIn.computeIfAbsent(pId, k -> new HashSet<>());
            Set<UUID> playerAroundsToTick = new HashSet<>(playerArounds);

            for (int x = playerChunkCoord.x - lengthCheck; x <= playerChunkCoord.x + lengthCheck; x++) {
                for (int z = playerChunkCoord.z - lengthCheck; z <= playerChunkCoord.z + lengthCheck; z++) {
                    ChunkCoord chunkCoord = playerChunkCoord.clone().set(x, z);
                    ConcurrentHashMap<Location, List<Around>> locations = arounds.get(chunkCoord);
                    if (locations == null) continue;

                    for (Map.Entry<Location, List<Around>> en : locations.entrySet()) {
                        Location location = en.getKey();
                        List<Around> chunkArounds = en.getValue();
                        if (chunkArounds == null) continue;
                        for (Around around : chunkArounds) {
                            UUID id = around.id;
                            playerAroundsToTick.remove(id);
                            if (location.distanceSquared(pLoc) > around.radiusSquared) {
                                if (playerArounds.contains(id)) {
                                    Consumer<Player> onLeave = around.onLeave;
                                    if (onLeave != null) onLeave.accept(p);
                                    playerArounds.remove(id);
                                }
                            } else {
                                if (around.detection != null && !around.detection.test(p)) continue;
                                if (!playerArounds.contains(id)) {
                                    Consumer<Player> onEnter = around.onEnter;
                                    if (onEnter != null) onEnter.accept(p);
                                    playerArounds.add(id);
                                }
                            }
                        }
                    }
                }
            }


            for (UUID id : playerAroundsToTick) {
                // Around may be removed
                Around around = aroundUUIDs.get(id);
                if (around == null) {
                    playerArounds.remove(id);
                    continue;
                }
                Location aroundLocation = around.location;
                if (!aroundLocation.getWorld().equals(world) || aroundLocation.distanceSquared(pLoc) > around.radiusSquared) {
                    Consumer<Player> onLeave = around.onLeave;
                    if (onLeave != null) onLeave.accept(p);
                    playerArounds.remove(id);
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        CompletableFuture.runAsync(() -> {
            Player p = e.getPlayer();
            Set<UUID> arounds = aroundsPlayerIsIn.remove(p.getUniqueId());
            for (UUID id : arounds) {
                Around around = aroundUUIDs.get(id);
                if (around.onLeave != null) around.onLeave.accept(p);
            }
        });
    }

    public synchronized Around addAround(Location location, double radius, Consumer<Player> onEnter, Consumer<Player> onLeave) {
        return addAround(location, radius, onEnter, onLeave, null);
    }

    public synchronized Around addAround(Location location, double radius, Consumer<Player> onEnter, Consumer<Player> onLeave, Predicate<Player> detection) {
        if (radius > biggestRadius) biggestRadius = radius;
        UUID id = UUID.randomUUID();
        Around around = new Around(id, location, onEnter, onLeave, radius, detection);

        addAround(around);
        return around;
    }

    public synchronized void addAround(Around around) {
        ConcurrentHashMap<Location, List<Around>> locations = arounds.computeIfAbsent(around.getChunkCoord(), k -> new ConcurrentHashMap<>());
        locations.computeIfAbsent(around.location, k -> new CopyOnWriteArrayList<>()).add(around);

        aroundUUIDs.put(around.id, around);
    }

    public synchronized boolean removeAround(Around around) {
        UUID id = around.id;
        if (aroundUUIDs.remove(id) == null) return false;
        Map<Location, List<Around>> l = arounds.get(around.getChunkCoord());
        if (l == null) return false;
        List<Around> aroundList = l.get(around.location);
        if (aroundList == null) return false;
        boolean removed = aroundList.remove(around);
        if (aroundList.isEmpty()) l.remove(around.location);
        return removed;
    }

    public synchronized void updateAround(Around around, Location newLoc) {
        removeAround(around);
        around.location = newLoc.clone();
        addAround(around);
    }

    @Getter
    public static class Around {

        private Location location;
        private final UUID id;
        private final Consumer<Player> onEnter;

        private final Consumer<Player> onLeave;
        private final Predicate<Player> detection;

        private final double radius;
        private final double radiusSquared;

        public Around(UUID id, Location location, Consumer<Player> onEnter, Consumer<Player> onLeave, double radius, Predicate<Player> detection) {
            this.id = id;
            this.location = location;
            this.onEnter = onEnter;
            this.onLeave = onLeave;
            this.radius = radius;
            radiusSquared = radius * radius;
            this.detection = detection;
        }

        public ChunkCoord getChunkCoord() {
            return new ChunkCoord(location);
        }

    }

}