package fr.supermax_8.spawndecoration.blueprint;

import com.cryptomorin.xseries.XMaterial;
import com.ticxo.modelengine.api.entity.Hitbox;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import fr.supermax_8.spawndecoration.utils.StringUtils;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Getter
public class StaticDecoration extends Decoration {

    @Getter
    private static final ConcurrentHashMap<Location, StaticDecoration> barrierHitboxBlocks = new ConcurrentHashMap<>();
    private List<Location> lights;

    public StaticDecoration(String modelId, Location location) {
        super(modelId, location, null);

        for (ModelBone bone : activeModel.getBones().values()) {
            String boneId = bone.getBoneId();
            bone.tick();
            if (boneId.contains("light")) {
                if (lights == null) lights = new ArrayList<>();
                int level = StringUtils.extractAndParseDigits(boneId);
                if (!XMaterial.LIGHT.isSupported()) continue;
                Location loc = bone.getLocation().clone();

                Block block = loc.getBlock();
                if (!block.getType().isAir()) return;

                BlockData data = XMaterial.LIGHT.parseMaterial().createBlockData();
                Levelled levelled = (Levelled) data;
                levelled.setLevel(level);
                block.setBlockData(levelled);
                lights.add(loc);
            }
        }

        forEachHitboxBlocks(block -> {
            if (!block.getType().isAir()) return;
            block.setType(Material.BARRIER, false);
            barrierHitboxBlocks.put(block.getLocation(), this);
        });
    }

    @Override
    public void tick() {
        super.tick();
    }


    @Override
    public void remove() {
        super.remove();
        forEachHitboxBlocks(block -> {
            if (!block.getType().equals(Material.BARRIER)) return;
            block.setType(Material.AIR, false);
            barrierHitboxBlocks.remove(block.getLocation());
        });
        if (lights != null) {
            for (Location location : lights) {
                Block block = location.getBlock();
                if (block.getType() != XMaterial.LIGHT.parseMaterial()) continue;
                block.setType(Material.AIR);
            }
        }
    }

    private void forEachHitboxBlocks(Consumer<Block> consumer) {
        Hitbox hitbox = activeModel.getBlueprint().getMainHitbox();
        if (Math.floor(hitbox.getHeight()) == 0 || Math.floor(hitbox.getDepth()) == 0 || Math.floor(hitbox.getWidth()) == 0)
            return;
        Location location = getLocation();
        BoundingBox box = hitbox.createBoundingBox(new Vector(
                location.getX() - 0.5, location.getY() - 0.5, location.getZ() - 0.5
        ));
        for (int x = (int) Math.ceil(box.getMinX()); x <= Math.floor(box.getMaxX()); x++) {
            for (int y = (int) Math.ceil(box.getMinY()); y <= Math.floor(box.getMaxY()); y++) {
                for (int z = (int) Math.ceil(box.getMinZ()); z <= Math.floor(box.getMaxZ()); z++) {
                    Block block = location.getWorld().getBlockAt(x, y, z);
                    consumer.accept(block);
                }
            }
        }
    }

}