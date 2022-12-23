package fr.supermax_8.spawndecoration.blueprint;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.List;

public class TrackDecoration extends BukkitRunnable {

    private final List<Location> locs;
    private final ArmorStand stand;

    private Iterator<Location> it;

    public TrackDecoration(List<Location> locs, String modelId) {
        this.locs = locs;
        it = locs.iterator();

        Location loc = locs.get(0);
        stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.setInvulnerable(true);
        stand.setPersistent(false);
        ActiveModel model = ModelEngineAPI.createActiveModel(modelId);
        ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(stand);
        modeledEntity.addModel(model, true);
        modeledEntity.setBaseEntityVisible(false);

        runTaskTimer(SpawnDecorationPlugin.getInstance(), 0, 0);
    }

    @Override
    public void run() {
        if (!it.hasNext()) it = locs.iterator();
        Location loc = it.next();
        stand.teleport(loc);
    }

    public void end() {
        cancel();
        stand.remove();
    }

}