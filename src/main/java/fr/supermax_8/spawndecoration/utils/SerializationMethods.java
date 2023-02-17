package fr.supermax_8.spawndecoration.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class SerializationMethods {

    public static String serializedLocation(Location loc) { //Converts location -> String
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
    }

    public static Location deserializedLocation(String s) {//Converts String -> Location
        String[] parts = s.split(";");
        World w = Bukkit.getServer().getWorld(parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);
        return new Location(w, x, y, z, yaw, pitch); //can return null if the world no longer exists
    }


}