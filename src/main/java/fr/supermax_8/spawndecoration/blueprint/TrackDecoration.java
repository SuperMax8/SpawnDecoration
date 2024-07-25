package fr.supermax_8.spawndecoration.blueprint;

import fr.supermax_8.spawndecoration.utils.PathUtils;
import org.bukkit.Location;

import java.util.Iterator;
import java.util.List;

public class TrackDecoration extends Decoration {

    private final List<Location> locs;
    private Iterator<Location> it;

    public TrackDecoration(List<Location> locs, String modelId, boolean smoothPath) {
        super(modelId, locs.get(0));
        this.locs = smoothPath ? PathUtils.smooth(locs) : locs;
        it = this.locs.iterator();
    }

    @Override
    public void tick() {
        if (!it.hasNext()) it = locs.iterator();
        Location loc = it.next();
        dummy.syncLocation(loc);
    }


}