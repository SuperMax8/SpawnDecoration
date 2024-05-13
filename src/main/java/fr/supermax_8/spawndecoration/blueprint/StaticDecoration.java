package fr.supermax_8.spawndecoration.blueprint;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.Dummy;
import com.ticxo.modelengine.api.entity.Hitbox;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

import java.util.HashMap;

@Getter
public class StaticDecoration {

    @Getter
    private static final HashMap<Location, StaticDecoration> barrierHitboxBlocks = new HashMap<>();

    private String modelId;
    private Location location;
    private ActiveModel model;
    private Dummy<StaticDecoration> decorationDummy;

    public StaticDecoration(String modelId, Location location) {
        this.modelId = modelId;
        this.location = location;

        decorationDummy = new Dummy<>(this);

        decorationDummy.syncLocation(location);
        decorationDummy.getBodyRotationController().setYBodyRot(location.getYaw());

        model = ModelEngineAPI.createActiveModel(modelId);
        ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(decorationDummy);
        modeledEntity.addModel(model, true);
        modeledEntity.setBaseEntityVisible(false);

        forEachHitboxBlocks(block -> {
            if (!block.getType().isAir()) return;
            block.setType(Material.BARRIER, false);
            barrierHitboxBlocks.put(block.getLocation(), this);
        });
    }

    public void end() {
        decorationDummy.setRemoved(true);
        forEachHitboxBlocks(block -> {
            if (!block.getType().equals(Material.BARRIER)) return;
            block.setType(Material.AIR, false);
            barrierHitboxBlocks.remove(block.getLocation());
        });
    }

    private void forEachHitboxBlocks(Consumer<Block> consumer) {
        Hitbox hitbox = model.getBlueprint().getMainHitbox();
        Hitbox flooredHitbox = new Hitbox(
                Math.floor(hitbox.getWidth()),
                Math.floor(hitbox.getHeight()),
                Math.floor(hitbox.getDepth()),
                Math.floor(hitbox.getEyeHeight())
        );
        if (flooredHitbox.getDepth() == 0 || flooredHitbox.getWidth() == 0 || flooredHitbox.getHeight() == 0) return;
        BoundingBox box = flooredHitbox.createBoundingBox(new Vector(
                location.getX(), location.getY(), location.getZ()
        ));
        for (int x = (int) box.getMinX(); x <= box.getMaxX(); x++) {
            for (int y = (int) box.getMinY(); y <= box.getMaxY(); y++) {
                for (int z = (int) box.getMinZ(); z <= box.getMaxZ(); z++) {
                    Block block = location.getWorld().getBlockAt(x, y, z);
                    consumer.accept(block);
                }
            }
        }
    }

}