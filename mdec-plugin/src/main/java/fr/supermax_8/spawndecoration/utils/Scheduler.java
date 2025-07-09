package fr.supermax_8.spawndecoration.utils;

import fr.supermax_8.spawndecoration.ModelEngineDecorationPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

public class Scheduler {

    private static final BukkitScheduler scheduler = Bukkit.getScheduler();
    private static final Plugin main = ModelEngineDecorationPlugin.getInstance();


    public static void runSync(Consumer<BukkitTask> run) {
        scheduler.runTask(main, run);
    }

    public static BukkitTask runSync(Runnable run) {
        return scheduler.runTask(main, run);
    }

    public static BukkitTask runTimer(Runnable run, long delay, long period) {
        return scheduler.runTaskTimer(main, run, delay, period);
    }

    public static void runTimer(Consumer<BukkitTask> run, long delay, long period) {
        scheduler.runTaskTimer(main, run, delay, period);
    }

    public static void runLater(Consumer<BukkitTask> run, long delay) {
        scheduler.runTaskLater(main, run, delay);
    }

    public static BukkitTask runLater(Runnable run, long delay) {
        return scheduler.runTaskLater(main, run, delay);
    }

    public static void runAsync(Runnable run) {
        scheduler.runTaskAsynchronously(main, run);
    }

    public static void runAsync(Consumer<BukkitTask> run) {
        scheduler.runTaskAsynchronously(main, run);
    }


    public static BukkitTask runTimerAsync(Runnable run, long delay, long period) {
        return scheduler.runTaskTimerAsynchronously(main, run, delay, period);
    }

    public static void runTimerAsync(Consumer<BukkitTask> run, long delay, long period) {
        scheduler.runTaskTimerAsynchronously(main, run, delay, period);
    }

    public static BukkitTask runLaterAsync(Runnable run, long delay) {
        return scheduler.runTaskLaterAsynchronously(main, run, delay);
    }

    public static void runLaterAsync(Consumer<BukkitTask> run, long delay) {
        scheduler.runTaskLaterAsynchronously(main, run, delay);
    }

}