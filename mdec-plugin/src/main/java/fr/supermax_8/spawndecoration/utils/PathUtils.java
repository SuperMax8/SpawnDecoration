package fr.supermax_8.spawndecoration.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class PathUtils {

    public static List<Location> smooth(List<Location> locs) {
        ArrayList<Location> smoothed = new ArrayList<>();
        Location lastLoc = null;
        int i = 0;
        for (Location loc : locs) {
            if (lastLoc == null) {
                lastLoc = loc;
                continue;
            }

            Vector direction = loc.toVector().subtract(lastLoc.toVector());
            if (!direction.isZero()) direction.normalize();
            Vector delta = direction.multiply(avrSectionSpeed(locs, i, 40));
            Location smoothedLoc = lastLoc.clone();
            smoothedLoc.add(delta);
            smoothed.add(smoothedLoc);

            lastLoc = smoothedLoc;
            i++;
        }

        return smoothed;
    }

    public static double avrSectionSpeed(List<Location> locs, int i, int sectionDistance) {
        int half = sectionDistance / 2;
        LinkedList<Location> section = new LinkedList<>();
        for (int j = Math.max(0, i - half); j <= Math.min(locs.size() - 1, i + half); j++)
            section.add(locs.get(j));
        return avrSpeed(section);
    }

    public static double avrSpeed(Collection<Location> locs) {
        ArrayList<Double> lengths = new ArrayList<>();
        Location lastLoc = null;
        for (Location loc : locs) {
            if (lastLoc == null) {
                lastLoc = loc;
                continue;
            }
            Vector v = loc.toVector().subtract(lastLoc.toVector());
            double l = v.length();
            lengths.add(l);
            lastLoc = loc;
        }

        double avr = 0;
        for (double length : lengths) avr += length;
        avr /= Math.max(lengths.size(), 1);
        return avr;
    }


}