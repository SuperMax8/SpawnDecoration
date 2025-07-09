package fr.supermax_8.spawndecoration.utils;

import fr.supermax_8.spawndecoration.ModelEngineDecorationPlugin;
import lombok.Builder;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PaginatedHotbarEditor {

    private final Player player;
    private final List<Item> items = new ArrayList<>();
    private final List<BukkitListener> listeners = new ArrayList<>();
    private final int[] displaySlots;
    private List<ItemStack> hotbar;
    private PaginatedHotbarEditor parent = null;
    private int currentPage = 0;
    @Setter
    private Runnable close = null;

    public PaginatedHotbarEditor(Player player) {
        this(player, new int[]{2, 3, 4, 5, 6, 7});
    }

    public PaginatedHotbarEditor(Player player, int[] displaySlots) {
        this.player = player;
        this.displaySlots = displaySlots;
    }

    public PaginatedHotbarEditor(Player player, PaginatedHotbarEditor parent, Consumer<PaginatedHotbarEditor> init) {
        this(player);
        this.parent = parent;
        if (init != null)
            init.accept(this);
        if (parent != null) {
            parent.unload();
            Bukkit.getScheduler().runTaskLater(ModelEngineDecorationPlugin.getInstance(), this::init, 2);
        } else
            init();
    }

    public PaginatedHotbarEditor addItem(ItemStack item, Consumer<Item.ItemBuilder> builder) {
        Item.ItemBuilder itemBuilder = Item.builder().item(item);
        builder.accept(itemBuilder);
        items.add(itemBuilder.build());
        return this;
    }

    public PaginatedHotbarEditor init() {
        hotbar = getHotbarItems();
        setItems();
        listeners.addAll(
                List.of(
                        BukkitListener.registerPlayerListener(player, PlayerQuitEvent.class, EventPriority.NORMAL, event -> {
                            end();
                        }),
                        BukkitListener.registerPlayerListener(player, PlayerInteractEvent.class, EventPriority.NORMAL, event -> {
                            if (event.getHand() == EquipmentSlot.OFF_HAND) return;
                            event.setCancelled(true);

                            int slot = player.getInventory().getHeldItemSlot();
                            if (slot == 0 && currentPage > 0) {
                                currentPage--;
                                setItems();
                                return;
                            }
                            if (slot == 1 && (currentPage + 1) * displaySlots.length < items.size()) {
                                currentPage++;
                                setItems();
                                return;
                            }
                            if (slot == 8) {
                                end();
                                return;
                            }

                            getHoldingItem(slot).ifPresent(item -> {
                                if (item.click != null) item.click.accept(event);
                                switch (event.getAction()) {
                                    case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK ->
                                            run(player.isSneaking() ? item.shiftRightClick : item.rightClick);
                                    case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK ->
                                            run(player.isSneaking() ? item.shiftLeftClick : item.leftClick);
                                }
                                setItems();
                            });
                        })
                )
        );
        return this;
    }

    private void setItems() {
        for (int i = 0; i < 8; i++)
            player.getInventory().setItem(i, null);
        int startIndex = currentPage * displaySlots.length;
        int endIndex = Math.min(startIndex + displaySlots.length, items.size());

        for (int i = startIndex, displayIndex = 0; i < endIndex && displayIndex < displaySlots.length; i++, displayIndex++) {
            Item item = items.get(i);
            player.getInventory().setItem(displaySlots[displayIndex],
                    item.itemSupplier == null ? item.item : item.itemSupplier.get());
        }

        // Slot 0: Previous page
        if (currentPage > 0) {
            ItemStack prev = createNamedItem(Material.ARROW, "§aPrevious Page");
            player.getInventory().setItem(0, prev);
        }

        // Slot 1: Next page
        if (endIndex < items.size()) {
            ItemStack next = createNamedItem(Material.ARROW, "§aNext Page");
            player.getInventory().setItem(1, next);
        }

        // Slot 8: End
        ItemStack end = createNamedItem(Material.BARRIER, "§cEnd edition");
        player.getInventory().setItem(8, end);
        player.updateInventory();
    }

    private ItemStack createNamedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<ItemStack> getHotbarItems() {
        List<ItemStack> snapshot = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack itm = player.getInventory().getItem(i);
            snapshot.add(itm != null ? itm.clone() : null);
        }
        return snapshot;
    }

    private void setHotbarItems(List<ItemStack> items) {
        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, items.get(i));
        }
        player.updateInventory();
    }

    private Optional<Item> getHoldingItem(int slot) {
        for (int i = 0; i < displaySlots.length; i++) {
            if (displaySlots[i] == slot) {
                int index = currentPage * displaySlots.length + i;
                if (index < items.size()) {
                    return Optional.of(items.get(index));
                }
            }
        }
        return Optional.empty();
    }

    private void run(Runnable action) {
        if (action != null) action.run();
    }

    public void unload() {
        setHotbarItems(hotbar);
        listeners.forEach(BukkitListener::unregister);
    }

    public void end() {
        setHotbarItems(hotbar);
        listeners.forEach(BukkitListener::unregister);
        player.sendMessage("§cEnd edition");
        if (close != null) close.run();
        if (parent != null)
            Bukkit.getScheduler().runTaskLater(ModelEngineDecorationPlugin.getInstance(), parent::init, 1);
    }

    @Builder
    public static class Item {
        private ItemStack item;
        private Supplier<ItemStack> itemSupplier;
        private Consumer<PlayerInteractEvent> click;
        private Runnable leftClick;
        private Runnable rightClick;
        private Runnable shiftLeftClick;
        private Runnable shiftRightClick;
    }

}