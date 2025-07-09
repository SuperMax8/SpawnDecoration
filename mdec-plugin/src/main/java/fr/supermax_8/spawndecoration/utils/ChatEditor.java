package fr.supermax_8.spawndecoration.utils;

import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ChatEditor {

    public static final Predicate<String> IS_INTEGER = s -> {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    };

    public static final Predicate<String> IS_DOUBLE = s -> {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    };

    private final Player p;
    private final List<BukkitListener> listeners = new ArrayList<>();
    private final List<AskVal> asks = new ArrayList<>();
    private final List<String> responses = new ArrayList<>();
    private final Consumer<List<String>> valid;
    private final Runnable failed;
    private int index = 0;
    private long timeOutMs = 1000 * 60;
    private BukkitTask timeOutTask;
    private boolean init = false;
    private long startTime;

    public ChatEditor(Player p, Consumer<List<String>> valid) {
        this(p, valid, null);
    }

    public ChatEditor(Player p, Consumer<List<String>> valid, Runnable failed) {
        this.p = p;
        this.valid = valid;
        this.failed = failed;
    }

    public void init() {
        if (init)
            throw new IllegalStateException("ChatEditor already initialized");
        if (asks.isEmpty())
            throw new IllegalStateException("ChatEditor asks empty");
        init = true;
        p.closeInventory();
        listeners.add(BukkitListener.registerPlayerListener(p, PlayerQuitEvent.class, e -> {
            fail();
        }));
        listeners.add(BukkitListener.registerPlayerListener(p, AsyncPlayerChatEvent.class, e -> {
            e.setCancelled(true);
            String msg = e.getMessage();
            if (msg.equalsIgnoreCase("canceleditor")) {
                p.sendMessage("§cCancelled chat input!");
                cancel();
                return;
            }
            AskVal askVal = asks.get(index);
            Predicate<String> validator = askVal.validator;
            if (validator != null && !validator.test(msg)) {
                p.sendMessage(askVal.askMessage);
                return;
            }

            responses.add(msg);
            // Correct response
            if (index == asks.size() - 1)
                valid();
            else {
                index++;
                p.sendMessage(asks.get(index).askMessage);
            }
        }));

        if (timeOutMs > 0) {
            startTime = System.currentTimeMillis();
            timeOutTask = Scheduler.runTimerAsync(() -> {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > timeOutMs) {
                    fail();
                    p.sendMessage("§cTook to long to respond!");
                }
            }, 0, 20);
        }
        p.sendMessage(asks.get(0).askMessage);
    }

    public ChatEditor setTimeOutMs(long timeOutMs) {
        this.timeOutMs = timeOutMs;
        return this;
    }

    public ChatEditor ask(String askMessage) {
        return ask(askMessage, null);
    }

    public ChatEditor ask(String askMessage, Predicate<String> validator) {
        if (init) throw new IllegalStateException("ChatEditor already initialized! Can't add ask");
        asks.add(new AskVal(askMessage, validator));
        return this;
    }

    public void cancel() {
        fail();
    }

    private void endclean() {
        listeners.forEach(BukkitListener::unregister);
        timeOutTask.cancel();
    }

    private void fail() {
        if (failed != null) failed.run();
        endclean();
    }

    private void valid() {
        endclean();
        Scheduler.runSync(() -> valid.accept(responses));
    }

    private record AskVal(String askMessage, Predicate<String> validator) {
    }

}
