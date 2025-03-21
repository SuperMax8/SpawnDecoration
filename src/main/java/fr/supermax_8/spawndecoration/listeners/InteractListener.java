package fr.supermax_8.spawndecoration.listeners;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.events.BaseEntityInteractEvent;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.mount.controller.impl.AbstractMountController;
import com.ticxo.modelengine.api.nms.entity.wrapper.MoveController;
import dev.triumphteam.gui.components.util.ItemNbt;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoration;
import fr.supermax_8.spawndecoration.events.InteractStaticDecorationEvent;
import fr.supermax_8.spawndecoration.manager.DecorationManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.UUID;

public class InteractListener implements Listener {

    private final HashMap<UUID, Long> cooldown = new HashMap<>();

    private void interact(Player player, StaticDecoration staticDecoration) {
        InteractStaticDecorationEvent interactEvent = new InteractStaticDecorationEvent(player, staticDecoration, InteractStaticDecorationEvent.InteractionType.INTERACT);
        Bukkit.getPluginManager().callEvent(interactEvent);

        if (!interactEvent.isCancelled())
            staticDecoration.getActiveModel().getMountManager().ifPresentOrElse(mountManager -> {
                tryDismountOld(player);
                mountManager.mountLeastOccupied(player, (entity, mount) -> new AbstractMountController(entity, mount) {
                    @Override
                    public void updateDriverMovement(MoveController controller, ActiveModel model) {
                    }

                    @Override
                    public void updatePassengerMovement(MoveController controller, ActiveModel model) {
                        if (input == null) return;
                        model.getMountManager().ifPresent(manager -> {
                            if (input.isSneak())
                                manager.dismountRider(player);
                        });
                    }
                }, mountController -> {
                    mountController.setCanInteractMount(true);
                    mountController.setCanDamageMount(true);
                });
            }, () -> staticDecoration.playAnimation("interact"));
    }

    private void hit(Player player, StaticDecoration staticDecoration) {
        InteractStaticDecorationEvent interactEvent = new InteractStaticDecorationEvent(player, staticDecoration, InteractStaticDecorationEvent.InteractionType.HIT);
        Bukkit.getPluginManager().callEvent(interactEvent);

        if (!interactEvent.isCancelled())
            staticDecoration.playAnimation("hit");
    }

    @EventHandler
    public void onMegInteract(BaseEntityInteractEvent event) {
        Object obj = event.getBaseEntity().getOriginal();
        if (!(obj instanceof StaticDecoration staticDecoration)) return;
        Player player = event.getPlayer();
        if (!canInteract(event.getPlayer())) return;
        switch (event.getAction()) {
            case INTERACT, INTERACT_ON -> {
                interact(player, staticDecoration);
            }
            case ATTACK -> {
                hit(player, staticDecoration);

                if (!player.hasPermission("modelenginedecoration.use")) return;
                ItemStack stack = player.getInventory().getItemInMainHand();
                String modelId = ItemNbt.getString(stack, "megdecoration_modelid");
                if (modelId == null) {
                    player.sendMessage("§cYou can't break a decoration without a decoration item in hand");
                    return;
                }
                DecorationManager.getInstance().removeStaticDeco(event.getBaseEntity().getLocation());
            }
        }
    }

    private void tryDismountOld(Player target) {
        ActiveModel model = ModelEngineAPI.getMountPairManager().getMountedPair(target.getUniqueId());
        if (model == null) return;
        model.getMountManager().ifPresent(mountManager -> {
            mountManager.dismountRider(target);
        });
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!canInteract(event.getPlayer())) return;
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK -> {
                StaticDecoration staticDecoration = StaticDecoration.getBarrierHitboxBlocks().get(event.getClickedBlock().getLocation());
                if (staticDecoration == null) return;
                event.setCancelled(true);
                hit(player, staticDecoration);

                if (!player.hasPermission("modelenginedecoration.use")) return;
                ItemStack stack = player.getInventory().getItemInMainHand();
                String modelId = ItemNbt.getString(stack, "megdecoration_modelid");
                if (modelId == null) return;
                event.setCancelled(true);

                DecorationManager.getInstance().removeStaticDeco(staticDecoration.getLocation());
            }
            case RIGHT_CLICK_BLOCK -> {
                StaticDecoration staticDecoration = StaticDecoration.getBarrierHitboxBlocks().get(event.getClickedBlock().getLocation());
                if (staticDecoration != null) {
                    interact(player, staticDecoration);
                    return;
                }

                if (!player.hasPermission("modelenginedecoration.use")) return;
                ItemStack stack = player.getInventory().getItemInMainHand();
                String modelId = ItemNbt.getString(stack, "megdecoration_modelid");
                if (modelId == null) return;

                float playerYaw = player.getLocation().getYaw();
                float yaw = Math.round(playerYaw / 45f) * 45f + 180;
                Location loc = event.getClickedBlock().getLocation().clone().add(0.5, event.getBlockFace() == BlockFace.DOWN ? -1 : 1, 0.5);
                loc.setYaw(yaw);
                if (DecorationManager.getInstance().getStaticDecoMap().values().stream().anyMatch(l ->
                        l.stream().anyMatch(s ->
                                s.getLocation().getBlock().getLocation().equals(loc.getBlock().getLocation())))) {
                    player.sendMessage("§cThere is already a decoration here !");
                    return;
                }
                DecorationManager.getInstance().addStaticDeco(loc, modelId, 1, new Quaternionf(), null);
            }
        }
    }

    private boolean canInteract(Player player) {
        Long l = cooldown.get(player.getUniqueId());
        if (l != null && System.currentTimeMillis() - l < 100) return false;
        cooldown.put(player.getUniqueId(), System.currentTimeMillis());
        return true;
    }

}