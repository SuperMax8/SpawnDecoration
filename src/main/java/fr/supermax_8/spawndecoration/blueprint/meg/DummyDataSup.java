package fr.supermax_8.spawndecoration.blueprint.meg;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.ticxo.modelengine.api.entity.CullType;
import com.ticxo.modelengine.api.entity.data.AbstractEntityData;
import com.ticxo.modelengine.api.nms.impl.DummyTrackedEntity;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.N;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class DummyDataSup extends AbstractEntityData {

    protected final DummySup<?> dummy;
    @Getter
    protected final DummyTrackedEntity tracked;

    protected final Set<UUID> syncTracking = new HashSet<>();
    protected final Map<UUID, CullType> asyncTracking = Maps.newConcurrentMap();

    protected final Queue<UUID> startTrackingQueue = new ConcurrentLinkedQueue<>();
    protected final Set<UUID> startTracking = new HashSet<>();
    protected final Queue<UUID> stopTrackingQueue = new ConcurrentLinkedQueue<>();
    protected final Set<UUID> stopTracking = new HashSet<>();
    @Setter
    protected Location location = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);

    public DummyDataSup(DummySup<?> dummy) {
        this.dummy = dummy;
        tracked = new DummyTrackedEntity();
        trackingUpdate();
        syncUpdate();
    }

    @Override
    public void asyncUpdate() {
        tickUpdate();
        trackingUpdate();
    }

    private void tickUpdate() {
        Supplier<Location> sup = dummy.getSup();
        if (sup == null) return;
        Location loc = sup.get();
        if (loc != null) location = loc;
    }

    private void trackingUpdate() {
        while (!startTrackingQueue.isEmpty()) {
            var player = startTrackingQueue.poll();
            startTracking.add(player);
            asyncTracking.put(player, CullType.NO_CULL);
        }
        while (!stopTrackingQueue.isEmpty()) {
            var player = stopTrackingQueue.poll();
            stopTracking.add(player);
            if (asyncTracking.get(player) != CullType.CULLED)
                asyncTracking.remove(player);
        }
    }

    @Override
    public void syncUpdate() {
        if (dummy.isDetectingPlayers() && location != null)
            tracked.detectPlayers(location);
        var updatedTracking = tracked.getTrackedPlayer(player -> asyncTracking.get(player.getUniqueId()) != CullType.CULLED);
        var all = new HashSet<>(syncTracking);
        all.addAll(updatedTracking);
        for (var player : all) {
            if (!syncTracking.contains(player)) {
                startTrackingQueue.add(player);
            } else if (!updatedTracking.contains(player)) {
                stopTrackingQueue.add(player);
            }
        }
        syncTracking.clear();
        syncTracking.addAll(updatedTracking);
    }

    @Override
    public void cullUpdate() {
    }

    @Override
    public void cleanup() {
        startTracking.clear();
        stopTracking.clear();
    }

    @Override
    public void destroy() {
    }

    @Override
    public boolean isDataValid() {
        return dummy.isAlive();
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public List<org.bukkit.entity.Entity> getPassengers() {
        return List.of();
    }

    @Override
    public Set<UUID> getStartTracking() {
        return ImmutableSet.copyOf(startTracking);
    }

    @Override
    public Map<UUID, CullType> getTracking() {
        return ImmutableMap.copyOf(asyncTracking);
    }

    @Override
    public Set<UUID> getStopTracking() {
        return ImmutableSet.copyOf(stopTracking);
    }

    public int getRenderRadius() {
        return tracked.getBaseRange();
    }

    public void setRenderRadius(int radius) {
        tracked.setBaseRange(radius);
    }

}