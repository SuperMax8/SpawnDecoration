package fr.supermax_8.spawndecoration.blueprint;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.Dummy;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.nms.entity.wrapper.TrackedEntity;
import fr.supermax_8.spawndecoration.SpawnDecorationConfig;
import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.List;

public class TrackDecoration extends BukkitRunnable {

    private final List<Location> locs;

    private Iterator<Location> it;
    private Dummy<TrackDecoration> decorationDummy;

    public TrackDecoration(List<Location> locs, String modelId) {
        this.locs = locs;
        it = locs.iterator();
        decorationDummy = new Dummy<>();
        decorationDummy.setRenderRadius(SpawnDecorationConfig.getRenderRadius());

        Location loc = locs.get(0);
        decorationDummy.setLocation(loc);
        ActiveModel model = ModelEngineAPI.createActiveModel(modelId);
        ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(decorationDummy);
        modeledEntity.addModel(model, true);
        modeledEntity.setBaseEntityVisible(false);

        runTaskTimerAsynchronously(SpawnDecorationPlugin.getInstance(), 0, 0);
    }

    @Override
    public void run() {
        if (!it.hasNext()) it = locs.iterator();
        Location loc = it.next();
        decorationDummy.syncLocation(loc);
    }

    public void end() {
        cancel();
        decorationDummy.setRemoved(true);
    }

}