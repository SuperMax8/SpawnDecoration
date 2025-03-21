package fr.supermax_8.spawndecoration.utils;

import lombok.Builder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HotbarEditor {

    private final Player player;
    private final List<Item> items = new ArrayList<>();
    private final List<TemporaryListener> listeners = new ArrayList<>();
    private boolean init = false, ended = false;
    private List<ItemStack> hotbar;

    public HotbarEditor(Player player) {
        this.player = player;
    }

    public HotbarEditor(Player player, Consumer<HotbarEditor> init) {
        this.player = player;
        init.accept(this);
        init();
    }

    public HotbarEditor addItem(int slot, ItemStack itm, Consumer<Item.ItemBuilder> builder) {
        if (init)
            throw new RuntimeException("You idiot! You can't add an item to the hotbar after init!");
        Item.ItemBuilder builder1 = Item.builder().item(itm).slot(slot);
        builder.accept(builder1);
        items.add(builder1.build());
        return this;
    }

    private void setItems() {
        for (Item item : items)
            player.getInventory().setItem(item.slot, item.itemSupplier == null ? item.item : item.itemSupplier.get());
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName("§cEnd edition");
        stack.setItemMeta(meta);
        player.getInventory().setItem(8, stack);
    }

    private List<ItemStack> getHotbarItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack itm = player.getInventory().getItem(i);
            if (itm != null) itm = itm.clone();
            items.add(itm);
        }
        return items;
    }

    private void setHotbarItems(List<ItemStack> items) {
        for (int i = 0; i < 9; i++)
            player.getInventory().setItem(i, items.get(i));
    }

    public HotbarEditor init() {
        if (init) return this;
        init = true;
        hotbar = getHotbarItems();
        setItems();
        listeners.addAll(
                List.of(
                        new TemporaryListener<>(PlayerQuitEvent.class, EventPriority.NORMAL, event -> {
                            end();
                        }),
                        new TemporaryListener<>(PlayerInteractEvent.class, EventPriority.NORMAL, event -> {
                            event.setCancelled(true);
                            if (player.getInventory().getHeldItemSlot() == 8) {
                                end();
                                return;
                            }
                            Optional<Item> optitm = getHoldingItem();
                            if (optitm.isEmpty()) return;
                            Item item = optitm.get();
                            switch (event.getAction()) {
                                case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                                    run(player.isSneaking() ? item.shiftRightClick : item.rightClick);
                                }
                                case LEFT_CLICK_BLOCK, LEFT_CLICK_AIR -> {
                                    run(player.isSneaking() ? item.shiftLeftClick : item.leftClick);
                                }
                            }
                            setItems();
                        })
                )
        );
        return this;
    }

    private void run(Runnable runnable) {
        if (runnable != null) runnable.run();
    }

    private Optional<Item> getHoldingItem() {
        return items.stream().filter(item -> item.slot == player.getInventory().getHeldItemSlot()).findFirst();
    }

    public void end() {
        if (!init || ended) return;
        ended = true;
        setHotbarItems(hotbar);
        listeners.forEach(TemporaryListener::unregister);
        player.sendMessage("§cEnd edition");
    }

    @Builder
    public static class Item {

        private int slot;
        private ItemStack item;
        private Supplier<ItemStack> itemSupplier;
        private Runnable leftClick;
        private Runnable rightClick;
        private Runnable shiftLeftClick;
        private Runnable shiftRightClick;

    }

}