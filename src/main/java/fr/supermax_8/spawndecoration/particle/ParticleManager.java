package fr.supermax_8.spawndecoration.particle;

import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoration;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ParticleManager {

    @Getter
    private static ParticleManager instance = new ParticleManager();

    private final List<StaticDecoration> decorations = new CopyOnWriteArrayList<>();

    private ParticleManager() {
        instance = this;
        Bukkit.getScheduler().runTaskTimerAsynchronously(SpawnDecorationPlugin.getInstance(), () -> {
            for (StaticDecoration decoration : new LinkedList<>(decorations))
                for (ParticleSpot particle : decoration.getParticles()) particle.spawnParticle();
        }, 0, 0);
    }

    public void addDecoration(StaticDecoration decoration) {
        decorations.add(decoration);
    }

    public void removeDecoration(StaticDecoration decoration) {
        decorations.remove(decoration);
    }

    public void clear() {
        decorations.clear();
    }

}