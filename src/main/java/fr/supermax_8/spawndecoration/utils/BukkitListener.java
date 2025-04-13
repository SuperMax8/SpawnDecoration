package fr.supermax_8.spawndecoration.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.Plugin;

/**
 * V1
 * @author Max
 */
public class BukkitListener implements Listener {

    private static Plugin plugin;

    private boolean isUnregister = false;

    public static void init(Plugin plugin) {
        BukkitListener.plugin = plugin;
    }

    private BukkitListener() {
    }

    public static <E extends Event> BukkitListener registerListener(Class<E> cz, EventListener<E> eventListener) {
        return registerListener(cz, EventPriority.NORMAL, eventListener);
    }

    public static <E extends Event> BukkitListener registerListener(Class<E> cz, EventPriority priority, EventListener<E> eventListener) {
        BukkitListener listener = new BukkitListener();
        Bukkit.getServer().getPluginManager().registerEvent(cz, listener, priority,
                (ignored, ev) -> {
                    try {
                        eventListener.use((E) ev);
                    } catch (ClassCastException ignored1) {
                    }
                }, plugin);
        return listener;
    }

    public static <E extends Event> BukkitListener registerListener(Class<E> cz, AutoUnregisterEventListener<E> eventListener) {
        return registerListener(cz, EventPriority.NORMAL, eventListener);
    }

    public static <E extends Event> BukkitListener registerListener(Class<E> cz, EventPriority priority, AutoUnregisterEventListener<E> eventListener) {
        BukkitListener listener = new BukkitListener();
        Bukkit.getServer().getPluginManager().registerEvent(cz, listener, priority,
                (ignored, ev) -> {
                    try {
                        listener.unregister(eventListener.use((E) ev));
                    } catch (ClassCastException ignored1) {
                    }
                }, plugin);
        return listener;
    }

    public static <E extends PlayerEvent> BukkitListener registerPlayerListener(Player player, Class<E> cz, EventListener<E> eventListener) {
        return registerPlayerListener(player, cz, EventPriority.NORMAL, eventListener);
    }

    public static <E extends PlayerEvent> BukkitListener registerPlayerListener(Player player, Class<E> cz, EventPriority priority, EventListener<E> eventListener) {
        BukkitListener listener = new BukkitListener();
        Bukkit.getServer().getPluginManager().registerEvent(cz, listener, priority,
                (ignored, ev) -> {
                    try {
                        E e = (E) ev;
                        if (e.getPlayer().equals(player))
                            eventListener.use(e);
                    } catch (ClassCastException ignored1) {
                    }
                }, plugin);
        return listener;
    }

    public static <E extends PlayerEvent> BukkitListener registerPlayerListener(Player player, Class<E> cz, AutoUnregisterEventListener<E> eventListener) {
        return registerPlayerListener(player, cz, EventPriority.NORMAL, eventListener);
    }

    public static <E extends PlayerEvent> BukkitListener registerPlayerListener(Player player, Class<E> cz, EventPriority priority, AutoUnregisterEventListener<E> eventListener) {
        BukkitListener listener = new BukkitListener();
        Bukkit.getServer().getPluginManager().registerEvent(cz, listener, priority,
                (ignored, ev) -> {
                    try {
                        E e = (E) ev;
                        if (e.getPlayer().equals(player))
                            listener.unregister(eventListener.use(e));
                    } catch (ClassCastException ignored1) {
                    }
                }, plugin);
        return listener;
    }

    public void unregister(boolean bool) {
        if (bool) unregister();
    }

    public void unregister() {
        if (isUnregister) return;
        HandlerList.unregisterAll(this);
        isUnregister = true;
    }

    public boolean isUnregister() {
        return isUnregister;
    }

    public interface EventListener<E> {

        void use(E event);

    }

    public interface AutoUnregisterEventListener<E> {

        /**
         * Return true if you want to remove the listener
         */
        boolean use(E event);

    }

}