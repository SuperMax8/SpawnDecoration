package fr.supermax_8.spawndecoration.blueprint;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.Dummy;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.nms.entity.wrapper.TrackedEntity;
import fr.supermax_8.spawndecoration.SpawnDecorationConfig;
import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import fr.supermax_8.spawndecoration.utils.PathUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import javax.swing.text.html.StyleSheet;
import java.util.Iterator;
import java.util.List;

public class TrackDecoration extends BukkitRunnable {

    private final List<Location> locs;

    private Iterator<Location> it;
    private Dummy<TrackDecoration> decorationDummy;
    private Location lastLoc;

    public TrackDecoration(List<Location> locs, String modelId, boolean smoothPath) {
        this.locs = smoothPath ? PathUtils.smooth(locs) : locs;
        decorationDummy = new Dummy<>();
        it = this.locs.iterator();
        decorationDummy.setRenderRadius(SpawnDecorationConfig.getRenderRadius());

        Location loc = this.locs.get(0);
        decorationDummy.syncLocation(loc);
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
        lastLoc = loc.clone();
    }

    public void end() {
        cancel();
        decorationDummy.setRemoved(true);
    }

}