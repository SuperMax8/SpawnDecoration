package fr.supermax_8.spawndecoration.listeners;

import com.ticxo.modelengine.api.events.BaseEntityInteractEvent;
import dev.triumphteam.gui.components.util.ItemNbt;
import fr.supermax_8.spawndecoration.SpawnDecorationConfig;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoList;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoration;
import fr.supermax_8.spawndecoration.manager.DecorationManager;
import fr.supermax_8.spawndecoration.utils.SerializationMethods;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class InteractListener implements Listener {

    HashMap<UUID, Long> cooldown = new HashMap<>();

    @EventHandler
    public void onMegInteract(BaseEntityInteractEvent event) {
        if (!event.getAction().equals(BaseEntityInteractEvent.Action.ATTACK)) return;
        Object obj = event.getBaseEntity().getOriginal();
        if (!(obj instanceof StaticDecoration staticDecoration)) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("modelenginedecoration.use")) return;
        if (!canInteract(event.getPlayer())) return;
        ItemStack stack = player.getInventory().getItemInMainHand();
        String modelId = ItemNbt.getString(stack, "megdecoration_modelid");
        if (modelId == null) {
            player.sendMessage("§cYou can't break a decoration without a decoration item in hand");
            return;
        }

        removeStaticDeco(event.getBaseEntity().getLocation());
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("modelenginedecoration.use")) return;
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK -> {
                StaticDecoration staticDecoration = StaticDecoration.getBarrierHitboxBlocks().get(event.getClickedBlock().getLocation());
                if (staticDecoration == null) return;
                ItemStack stack = player.getInventory().getItemInMainHand();
                String modelId = ItemNbt.getString(stack, "megdecoration_modelid");
                if (modelId == null) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou can't break a decoration without a decoration item in hand");
                    return;
                }

                if (!canInteract(player)) return;
                removeStaticDeco(staticDecoration.getLocation());
            }
            case RIGHT_CLICK_BLOCK -> {
                ItemStack stack = player.getInventory().getItemInMainHand();
                String modelId = ItemNbt.getString(stack, "megdecoration_modelid");
                if (modelId == null) return;
                if (!canInteract(player)) return;

                float playerYaw = player.getLocation().getYaw();
                float yaw = Math.round(playerYaw / 45f) * 45f + 180;
                Location loc = event.getClickedBlock().getLocation().clone().add(0.5, event.getBlockFace() == BlockFace.DOWN ? -1 : 1, 0.5);
                loc.setYaw(yaw);
                if (DecorationManager.staticDecoMap.values().stream().anyMatch(l ->
                        l.stream().anyMatch(s ->
                                s.getLocation().getBlock().getLocation().equals(loc.getBlock().getLocation())))) {
                    player.sendMessage("§cThere is already a decoration here !");
                    return;
                }
                String serializedLocation = SerializationMethods.serializedLocation(loc);

                StaticDecoList decoList = DecorationManager.readStaticDecos();
                decoList.getList().add(new StaticDecoList.StaticDeco(serializedLocation, modelId));
                DecorationManager.writeStaticDecos(decoList);

                SpawnDecorationConfig.reload();
            }
        }
    }

    private boolean canInteract(Player player) {
        Long l = cooldown.get(player.getUniqueId());
        if (l != null && System.currentTimeMillis() - l < 100) return false;
        cooldown.put(player.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    private void removeStaticDeco(Location location) {
        String loc = SerializationMethods.serializedLocation(location);

        StaticDecoList decoList = DecorationManager.readStaticDecos();
        StaticDecoList.StaticDeco toRemove = null;
        for (StaticDecoList.StaticDeco deco : decoList.getList()) {
            if (deco.getLocation().equals(loc)) toRemove = deco;
        }
        if (toRemove == null) return;
        decoList.getList().remove(toRemove);
        DecorationManager.writeStaticDecos(decoList);
        SpawnDecorationConfig.reload();
    }

}