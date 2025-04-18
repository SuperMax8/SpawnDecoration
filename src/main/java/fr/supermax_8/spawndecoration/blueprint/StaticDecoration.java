package fr.supermax_8.spawndecoration.blueprint;

import com.cryptomorin.xseries.XMaterial;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.entity.Hitbox;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import com.ticxo.modelengine.api.model.bone.SimpleManualAnimator;
import fr.supermax_8.spawndecoration.utils.StringUtils;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Getter
public class StaticDecoration extends Decoration {

    @Getter
    private static final ConcurrentHashMap<Location, StaticDecoration> barrierHitboxBlocks = new ConcurrentHashMap<>();
    private final Quaternionf modelRotation;
    private List<Location> lights;

    public StaticDecoration(StaticDecoList.StaticDeco deco) {
        this(deco.getId(), deco.getModelId(), deco.getDefaultAnimation(), deco.getBukkitLocation(), deco.getScale(), deco.getBlockLight(), deco.getSkyLight(), deco.getRotation(), deco.getTexts(), deco.getBoneTransformations());
    }

    public StaticDecoration(UUID uuid, String modelId, String defaultAnimation, Location location, double scale, int blockLight, int skyLight, Quaternionf rotation, Map<String, List<String>> texts, Map<String, StaticDecoList.StaticDeco.ModelTransformation> boneTransformations) {
        super(uuid, modelId, location, null, texts);

        modelRotation = new Quaternionf(rotation);
        activeModel.setScale(scale);
        if (blockLight > 0) activeModel.setBlockLight(blockLight);
        if (skyLight > 0) activeModel.setSkyLight(skyLight);
        activeModel.getBones().values().stream().filter(md -> md.getParent() == null).findFirst().ifPresent(bone -> {
            SimpleManualAnimator anim = new SimpleManualAnimator();
            anim.getRotation().set(rotation);
            bone.setManualAnimator(anim);
            bone.tick();
        });

        if (boneTransformations != null)
            boneTransformations.forEach((k, v) -> {
                activeModel.getBone(k).ifPresent(bone -> {
                    SimpleManualAnimator anim = new SimpleManualAnimator();
                    anim.getRotation().set(v.getRotation());
                    anim.getPosition().set(v.getPosition());
                    anim.getScale().set(v.getScale());
                    bone.setManualAnimator(anim);
                    bone.tick();
                    bone.setVisible(v.isVisible());
                });
            });

        if (defaultAnimation != null)
            activeModel.getAnimationHandler().setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE, defaultAnimation, 0.1, 0.1, 1));

        for (ModelBone bone : activeModel.getBones().values()) {
            String boneId = bone.getBoneId();
            bone.tick();
            if (boneId.contains("light")) {
                if (lights == null) lights = new ArrayList<>();
                int level = StringUtils.extractAndParseDigits(boneId);
                if (XMaterial.LIGHT.isSupported()) {
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
        Hitbox hit = activeModel.getBlueprint().getMainHitbox();
        float w = (float) hit.getWidth(), h = (float) hit.getHeight(), d = (float) hit.getDepth();
        if (w <= 0 || h <= 0 || d <= 0) return;

        Location loc = getLocation();
        int cx = loc.getBlockX(), cy = loc.getBlockY(), cz = loc.getBlockZ();

        BoundingBox bb = hit.createBoundingBox(new Vector());
        Vector bc = bb.getCenter();
        Vector3f center = new Vector3f((float) bc.getX(), (float) bc.getY(), (float) bc.getZ());

        float hx = w / 2f, hy = h / 2f, hz = d / 2f;
        int R = (int) Math.ceil(Math.max(Math.max(hx, hy), hz));

        Quaternionf invYaw = new Quaternionf().rotateY((float) Math.toRadians(loc.getYaw()));

        World world = loc.getWorld();
        Vector3f p = new Vector3f();
        for (int dx = -R; dx <= R; dx++)
            for (int dy = -R; dy <= R; dy++)
                for (int dz = -R; dz <= R; dz++) {
                    invYaw.transform(p.set(dx + 0.5f, dy + 0.5f, dz + 0.5f)).sub(center);
                    if (Math.abs(p.x) <= hx && Math.abs(p.y) <= hy && Math.abs(p.z) <= hz)
                        consumer.accept(world.getBlockAt(cx + dx, cy + dy, cz + dz));
                }
    }

}