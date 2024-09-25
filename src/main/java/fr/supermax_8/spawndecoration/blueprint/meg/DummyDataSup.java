package fr.supermax_8.spawndecoration.blueprint.meg;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ticxo.modelengine.api.entity.CullType;
import com.ticxo.modelengine.api.entity.data.AbstractEntityData;
import com.ticxo.modelengine.api.nms.impl.DummyTrackedEntity;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DummyDataSup extends AbstractEntityData {

    protected final DummySup dummy;
    @Getter
    protected final DummyTrackedEntity tracked;

    protected final Set<Player> syncTracking = new HashSet<>();
    protected final Map<Player, CullType> asyncTracking = new HashMap<>();

    protected final Queue<Player> startTrackingQueue = new ConcurrentLinkedQueue<>();
    protected final Set<Player> startTracking = new HashSet<>();
    protected final Queue<Player> stopTrackingQueue = new ConcurrentLinkedQueue<>();
    protected final Set<Player> stopTracking = new HashSet<>();
    @Setter
    protected Location location = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);

    public DummyDataSup(DummySup dummy) {
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
        if (dummy.getSup() == null) return;
        Location loc = dummy.getSup().get();
        if (loc != null) location = loc;
    }

    private void trackingUpdate() {
        while (!this.startTrackingQueue.isEmpty()) {
            Player player = this.startTrackingQueue.poll();
            this.startTracking.add(player);
            this.asyncTracking.put(player, CullType.NO_CULL);
        }

        while (!this.stopTrackingQueue.isEmpty()) {
            Player player = this.stopTrackingQueue.poll();
            this.stopTracking.add(player);
            if (this.asyncTracking.get(player) != CullType.CULLED) {
                this.asyncTracking.remove(player);
            }
        }
    }

    @Override
    public void syncUpdate() {
        if (this.dummy.isDetectingPlayers() && this.location != null) {
            this.tracked.detectPlayers(this.location);
        }

        Set<Player> updatedTracking = this.tracked.getTrackedPlayer(playerx -> this.asyncTracking.get(playerx) != CullType.CULLED);
        HashSet<Player> all = new HashSet<>(this.syncTracking);
        all.addAll(updatedTracking);

        for (Player player : all) {
            if (!this.syncTracking.contains(player)) {
                this.startTrackingQueue.add(player);
            } else if (!updatedTracking.contains(player)) {
                this.stopTrackingQueue.add(player);
            }
        }

        this.syncTracking.clear();
        this.syncTracking.addAll(updatedTracking);
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
    public Set<Player> getStartTracking() {
        return ImmutableSet.copyOf(startTracking);
    }

    @Override
    public Map<Player, CullType> getTracking() {
        return ImmutableMap.copyOf(asyncTracking);
    }

    @Override
    public Set<Player> getStopTracking() {
        return ImmutableSet.copyOf(stopTracking);
    }

    public int getRenderRadius() {
        return tracked.getBaseRange();
    }

    public void setRenderRadius(int radius) {
        tracked.setBaseRange(radius);
    }

}