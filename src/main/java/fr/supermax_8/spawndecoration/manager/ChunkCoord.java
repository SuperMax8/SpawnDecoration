package fr.supermax_8.spawndecoration.manager;


import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.LinkedList;
import java.util.Objects;
import java.util.UUID;

public class ChunkCoord implements Cloneable {
    public final UUID worldUUID;
    public int x;
    public int z;

    public ChunkCoord(Chunk chunk) {
        this(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    public ChunkCoord(Location loc) {
        this(loc.getWorld().getUID(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    public ChunkCoord(UUID worldUUID, int x, int z) {
        this.x = x;
        this.z = z;
        this.worldUUID = worldUUID;
    }

    public boolean equals(Object obj) {
        if (this == obj || !(obj instanceof ChunkCoord other)) return true;

        if (this.worldUUID == null) {
            if (other.worldUUID != null)
                return false;
        } else if (!this.worldUUID.equals(other.worldUUID))
            return false;

        return this.x == other.x && this.z == other.z;
    }

    public Chunk getChunk() {
        World world = Bukkit.getWorld(this.worldUUID);
        return world != null ? world.getChunkAt(this.x, this.z) : null;
    }


    public int hashCode() {
        return 31 * (31 * (31 + (this.worldUUID == null ? 0 : this.worldUUID.hashCode())) + this.x) + this.z;
    }

    public String toString() {
        return "[" + this.x + "," + this.z + "]";
    }

    public ChunkCoord add(int x, int z) {
        this.x += x;
        this.z += z;
        return this;
    }

    public ChunkCoord substract(int x, int z) {
        this.x -= x;
        this.z -= z;
        return this;
    }

    public ChunkCoord set(int x, int z) {
        this.x = x;
        this.z = z;
        return this;
    }

    @Override
    public ChunkCoord clone() {
        try {
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return (ChunkCoord) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

}