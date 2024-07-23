package fr.supermax_8.spawndecoration.blueprint;

import com.cryptomorin.xseries.XMaterial;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.entity.Dummy;
import com.ticxo.modelengine.api.entity.Hitbox;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import fr.supermax_8.spawndecoration.SpawnDecorationConfig;
import fr.supermax_8.spawndecoration.particle.ParticleManager;
import fr.supermax_8.spawndecoration.particle.ParticleSpot;
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
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class StaticDecoration {

    @Getter
    private static final HashMap<Location, StaticDecoration> barrierHitboxBlocks = new HashMap<>();
    @Getter
    private static final HashMap<Location, StaticDecoration> lightBlocks = new HashMap<>();

    private String modelId;
    private Location location;
    private ActiveModel activeModel;
    private AnimationHandler animationHandler;
    private Dummy<StaticDecoration> decorationDummy;
    private List<Location> lights;
    private boolean end = false;
    private List<ParticleSpot> particles = null;

    public StaticDecoration(String modelId, Location location) {
        this.modelId = modelId;
        this.location = location;

        decorationDummy = new Dummy<>(this);
        decorationDummy.setRenderRadius(SpawnDecorationConfig.getRenderRadius());
        decorationDummy.syncLocation(location);
        decorationDummy.getBodyRotationController().setYBodyRot(location.getYaw());

        activeModel = ModelEngineAPI.createActiveModel(modelId);
        animationHandler = activeModel.getAnimationHandler();
        ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(decorationDummy);
        activeModel.getMountManager().ifPresent(mountManager -> {
            mountManager.setCanRide(true);
            mountManager.setCanDrive(true);
            modeledEntity.getMountData().setMainMountManager(mountManager);
        });
        modeledEntity.addModel(activeModel, true);
        modeledEntity.setBaseEntityVisible(false);

        activeModel.getMountManager().ifPresent(mountManager -> {
            mountManager.setCanRide(true);
            modeledEntity.getMountData().setMainMountManager(mountManager);
            DriverManager.addDriver(activeModel);
        });

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
                lightBlocks.put(loc, this);

                BlockData data = XMaterial.LIGHT.parseMaterial().createBlockData();
                Levelled levelled = (Levelled) data;
                levelled.setLevel(level);
                block.setBlockData(levelled);
                lights.add(loc);
            } else if (boneId.contains("particle")) {
                if (particles == null) particles = new ArrayList<>();

                List<String> spots = SpawnDecorationConfig.getBoneBehavior().get(boneId.split("_")[1]);
                for (String spot : spots)
                    particles.add(new ParticleSpot(spot, bone::getLocation));
            }
        }
        if (particles != null) ParticleManager.getInstance().addDecoration(this);

        forEachHitboxBlocks(block -> {
            if (!block.getType().isAir()) return;
            block.setType(Material.BARRIER, false);
            barrierHitboxBlocks.put(block.getLocation(), this);
        });
    }

    public void playAnimation(String animation) {
        animationHandler.playAnimation(animation, 0.1, 0.1, 1, true);
    }

    public void end() {
        end = true;
        if (particles != null)
            ParticleManager.getInstance().removeDecoration(this);
        decorationDummy.setRemoved(true);
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