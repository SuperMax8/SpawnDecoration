package fr.supermax_8.spawndecoration.blueprint;

import com.github.retrooper.packetevents.util.Quaternion4f;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import fr.supermax_8.spawndecoration.SpawnDecorationConfig;
import fr.supermax_8.spawndecoration.blueprint.meg.DummySup;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Getter
public abstract class Decoration {

    protected final DummySup dummy;
    protected final ActiveModel activeModel;
    protected final AnimationHandler animationHandler;
    protected final ModeledEntity modeledEntity;
    protected final String modelId;
    protected ArrayList<ParticleSpot> particles;
    protected ArrayList<Holo> holograms;
    protected boolean removed = false;
    private short tickHologram = 0;
    // TextId : Lines<Text>
    private Map<String, List<String>> texts;

    public Decoration(String modelId, Location spawnLoc, Supplier<Location> locCalculator, Map<String, List<String>> texts) {
        this.modelId = modelId;
        this.texts = texts;

        dummy = new DummySup<>(this, locCalculator);
        dummy.setRenderRadius(SpawnDecorationConfig.getRenderRadius());
        dummy.setLocation(spawnLoc);
        dummy.getBodyRotationController().setYBodyRot(spawnLoc.getYaw());
        dummy.getBodyRotationController().setRotationDelay(0);

        activeModel = ModelEngineAPI.createActiveModel(modelId);
        animationHandler = activeModel.getAnimationHandler();

        modeledEntity = ModelEngineAPI.createModeledEntity(dummy);
        modeledEntity.addModel(activeModel, true);

        activeModel.getMountManager().ifPresent(mountManager -> {
            mountManager.setCanRide(true);
            modeledEntity.getMountData().setMainMountManager(mountManager);
            DriverManager.addDriver(activeModel);
        });

        for (ModelBone bone : activeModel.getBones().values()) {
            String boneId = bone.getBoneId();
            bone.tick();
            if (boneId.startsWith("particle")) {
                if (particles == null) particles = new ArrayList<>();

                List<String> spots = SpawnDecorationConfig.getParticle().get(boneId.split("_")[1]);
                for (String spot : spots)
                    particles.add(new ParticleSpot(spot, bone::getLocation));
            } else if (boneId.startsWith("text")) {
                if (holograms == null) holograms = new ArrayList<>();
                Location holoSpawn = bone.getLocation();
                Vector3f rotation = bone.getParent().getCachedLeftRotation();
                holoSpawn.setYaw((float) Math.toDegrees(rotation.y));
                holoSpawn.setPitch((float) Math.toDegrees(rotation.x));
                String[] split = boneId.split("_", 2);
                String textId = split[1];
                SpawnDecorationConfig.Text confText = SpawnDecorationConfig.getText().get(textId);
                if (confText == null) {
                    System.out.println("Conf text doesn't exist !! textId: " + textId);
                    continue;
                }
                List<String> lines = texts != null && texts.containsKey(textId) ? texts.get(textId) : confText.lines();
                if (lines == null) lines = new ArrayList<>();
                MiniMessage mm = MiniMessage.miniMessage();
                Component text = Component.text("");
                int i = 0;
                for (String line : lines) {
                    text = text.append(mm.deserialize(line));
                    i++;
                    if (i < lines.size())
                        text = text.appendNewline();
                }

                Hologram hologram = new Hologram(holoSpawn);
                hologram.setInterpolationDurationRotation(1);
                hologram.setInterpolationDurationTransformation(1);
                hologram.setText(text);
                hologram.setAlignment(confText.alignment());
                hologram.setBillboard(confText.billboard());
                hologram.setShadow(confText.shadow());
                hologram.setSeeThroughBlocks(confText.seeThroughBlocks());
                int[] bgColor = confText.bgArgb();
                hologram.setBackgroundColor(bgColor[0], bgColor[1], bgColor[2], bgColor[3]);
                float scale = confText.scale();
                hologram.setScale(new com.github.retrooper.packetevents.util.Vector3f(scale, scale, scale));

                holograms.add(new Holo(hologram, bone, hologram.containsPlaceholder()));
            }
        }
    }

    public void playAnimation(String animation) {
        animationHandler.playAnimation(animation, 0.1, 0.1, 1, true);
    }

    public void tick() {
        if (particles != null)
            for (ParticleSpot particle : particles) particle.spawnParticle();
        if (holograms != null) {
            for (Holo holo : holograms) {
                ModelBone bone = holo.bone;
                Location loc = holo.bone.getLocation();
                Quaternion4f left = convertQuaternionJomlToPE(bone.getGlobalLeftRotation());
                Quaternion4f right = convertQuaternionJomlToPE(bone.getGlobalRightRotation());
                holo.hologram.setLeftRotation(left);
                holo.hologram.setRightRotation(right);
                holo.hologram.update();
                loc.setYaw(dummy.getLocation().getYaw());
                holo.hologram.teleport(loc);
            }
        }
    }

    private Quaternion4f convertQuaternionJomlToPE(Quaternionf quaternionf) {
        return new Quaternion4f(quaternionf.x, quaternionf.y, quaternionf.z, quaternionf.w);
    }

    public void remove() {
        if (removed) return;
        if (holograms != null)
            for (Holo hologram : holograms)
                hologram.hologram.remove();
        dummy.setRemoved(true);
        removed = true;
    }

    public Location getLocation() {
        return dummy.getLocation().clone();
    }


    public record Holo(Hologram hologram, ModelBone bone, boolean containsPlaceholders) {
    }

}