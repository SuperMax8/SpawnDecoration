package fr.supermax_8.spawndecoration.utils;

import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class TemporaryListener<E extends Event> implements Listener {

    private boolean isUnregister = false;

    public TemporaryListener(Class<? extends E> cz, EventPriority priority, TemporaryEvent<E> event) {
        Bukkit.getServer().getPluginManager().registerEvent(cz, this, priority,
                (ignored, ev) -> {
                    try {
                        event.use((E) ev);
                    } catch (ClassCastException e) {
                    }
                },

                SpawnDecorationPlugin.getInstance());
    }

    public TemporaryListener(Class<? extends Event> cz, EventPriority priority, TemporaryEventAutoUnregister<E> event) {
        Bukkit.getServer().getPluginManager().registerEvent(cz, this, priority,
                (ignored, ev) -> {
                    try {
                        unregister(event.use((E) ev));
                    } catch (ClassCastException e) {
                    }
                }, SpawnDecorationPlugin.getInstance());
    }

    public void unregister(boolean bool) {
        if (bool) unregister();
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        isUnregister = true;
    }

    public boolean isUnregister() {
        return isUnregister;
    }

    public interface TemporaryEvent<E> {

        void use(E event);

    }

    public interface TemporaryEventAutoUnregister<E> {

        /**
         * Return true if you want to remove the listener
         */
        boolean use(E event);

    }


}