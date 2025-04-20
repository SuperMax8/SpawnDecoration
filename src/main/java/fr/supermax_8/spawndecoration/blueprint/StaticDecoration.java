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
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.joml.Quaternionf;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Getter
public class StaticDecoration extends Decoration {

    private static final Quaternionf ZERO = new Quaternionf();

    @Getter
    private static final ConcurrentHashMap<Location, StaticDecoration> barrierHitboxBlocks = new ConcurrentHashMap<>();
    private List<Location> lights;

    public StaticDecoration(StaticDecoList.StaticDeco deco) {
        this(deco.getId(), deco.getModelId(), deco.getDefaultAnimation(), deco.getDefaultAnimationSpeed(), deco.getBukkitLocation(), deco.getScale(), deco.getBlockLight(), deco.getSkyLight(), deco.getRotation(), deco.getTexts(), deco.getBoneTransformations());
    }

    public StaticDecoration(UUID uuid, String modelId, String defaultAnimation, double defaultAnimationSpeed, Location location, double scale, int blockLight, int skyLight, Quaternionf rotation, Map<String, List<String>> texts, Map<String, StaticDecoList.StaticDeco.ModelTransformation> boneTransformations) {
        super(uuid, modelId, location, null, texts);

        activeModel.setScale(scale);
        if (!rotation.equals(ZERO)) {
            // Find the parent bone
            activeModel.getBones().values().stream().filter(md -> md.getParent() == null).findFirst().ifPresent(bone -> {
                SimpleManualAnimator anim = new SimpleManualAnimator();
                anim.getRotation().set(rotation);
                bone.setManualAnimator(anim);
                bone.tick();
            });
        }
        if (blockLight > 0) activeModel.setBlockLight(blockLight);
        if (skyLight > 0) activeModel.setSkyLight(skyLight);

        if (boneTransformations != null)
            boneTransformations.forEach((k, v) -> {
                activeModel.getBone(k).ifPresent(bone -> {
                    SimpleManualAnimator anim = new SimpleManualAnimator();
                    anim.getRotation().set(v.getRotation());
                    anim.getPosition().set(v.getPosition());
                    anim.getScale().set(v.getScale());
                    bone.setManualAnimator(anim);
                    bone.setVisible(v.isVisible());
                    if (v.getModelItem() != null) bone.setModel(v.getModelItem().createStack());
                    bone.tick();
                });
            });

        String defAnim = defaultAnimation != null ? defaultAnimation : "idle";
        animationHandler.setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE, defAnim, 0.1, 0.1, defaultAnimationSpeed));

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
        Hitbox hitbox = activeModel.getBlueprint().getMainHitbox();
        if (hitbox.getWidth() == 0 || hitbox.getDepth() == 0 || hitbox.getHeight() == 0) return;

        Location loc = getLocation();
        double yawRad = Math.toRadians(loc.getYaw());
        double sin = Math.sin(-yawRad);
        double cos = Math.cos(-yawRad);

        double width = hitbox.getWidth();
        double height = hitbox.getHeight();
        double depth = hitbox.getDepth();

        if (width < 1 || height < 1 || depth < 1) return;

        int minX = (int) Math.floor(loc.getX() - width / 2 - 1);
        int maxX = (int) Math.ceil(loc.getX() + width / 2 + 1);
        int minY = (int) Math.floor(loc.getY());
        int maxY = (int) Math.ceil(loc.getY() + height + 1);
        int minZ = (int) Math.floor(loc.getZ() - depth / 2 - 1);
        int maxZ = (int) Math.ceil(loc.getZ() + depth / 2 + 1);

        Set<Long> visited = new HashSet<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double cx = x + 0.5 - loc.getX();
                    double cy = y + 0.5 - loc.getY();
                    double cz = z + 0.5 - loc.getZ();

                    double lx = cx * cos - cz * sin;
                    double lz = cx * sin + cz * cos;

                    if (Math.abs(lx) <= width / 2 && Math.abs(cy) <= height && Math.abs(lz) <= depth / 2) {
                        long key = (((long) x & 0x3FFFFFFL) << 38) | (((long) y & 0xFFFL) << 26) | ((long) z & 0x3FFFFFFL);
                        if (visited.add(key)) {
                            Block block = loc.getWorld().getBlockAt(x, y, z);
                            consumer.accept(block);
                        }
                    }
                }
            }
        }
    }

}