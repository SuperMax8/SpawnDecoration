package fr.supermax_8.spawndecoration.blueprint;

import fr.supermax_8.spawndecoration.utils.PathUtils;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class TrackDecoration extends Decoration {

    public TrackDecoration(List<Location> locs, String modelId, boolean smoothPath) {
        super(modelId, locs.get(0), new Supplier<>() {
            final List<Location> locations = smoothPath ? PathUtils.smooth(locs) : locs;
            Iterator<Location> it = locations.iterator();

            @Override
            public Location get() {
                if (!it.hasNext()) it = this.locations.iterator();
                return it.next();
            }
        }, null);
    }

    @Override
    public void tick() {
    }

}