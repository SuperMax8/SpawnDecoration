package fr.supermax_8.spawndecoration.blueprint;

import fr.supermax_8.spawndecoration.utils.SerializationMethods;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

@Getter
public class StaticDecoList {

    private final ArrayList<StaticDeco> list;

    public StaticDecoList(ArrayList<StaticDeco> list) {
        this.list = list;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class StaticDeco implements Cloneable {

        private UUID id;
        private String location;
        private String modelId;

        private String defaultAnimation;
        private double scale;
        private Quaternionf rotation;
        private int blockLight;
        private int skyLight;
        private Map<String, List<String>> texts;
        private Map<String, ModelTransformation> boneTransformations;

        public StaticDeco(Location location, String modelId) {
            id = UUID.randomUUID();
            this.location = SerializationMethods.serializedLocation(location);
            this.modelId = modelId;
            scale = 1.0;
            blockLight = -1;
            skyLight = -1;
        }

        public void setBukkitLocation(Location location) {
            this.location = SerializationMethods.serializedLocation(location);
        }

        public Location getBukkitLocation() {
            return SerializationMethods.deserializedLocation(location);
        }

        @Override
        public StaticDeco clone() {
            try {
                StaticDeco clone = (StaticDeco) super.clone();
                clone.id = UUID.randomUUID();
                clone.rotation = rotation != null ? new Quaternionf(rotation) : null;
                if (texts != null) {
                    Map<String, List<String>> clonedTexts = new HashMap<>();
                    for (Map.Entry<String, List<String>> entry : texts.entrySet())
                        clonedTexts.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                    clone.texts = clonedTexts;
                }

                if (boneTransformations != null) {
                    Map<String, ModelTransformation> clonedBones = new HashMap<>();
                    for (Map.Entry<String, ModelTransformation> entry : boneTransformations.entrySet())
                        clonedBones.put(entry.getKey(), entry.getValue().clone());
                    clone.boneTransformations = clonedBones;
                }
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }

        @Setter
        @Getter
        @AllArgsConstructor
        public static class ModelTransformation implements Cloneable {

            private boolean visible;
            private double scale;
            private Vector3f position;
            private Quaternionf rotation;

            public ModelTransformation() {
                visible = true;
                scale = 1.0;
                position = new Vector3f();
                rotation = new Quaternionf();
            }

            @Override
            public ModelTransformation clone() {
                try {
                    ModelTransformation clone = (ModelTransformation) super.clone();
                    clone.position = new Vector3f(position);
                    clone.rotation = new Quaternionf(rotation);
                    return clone;
                } catch (CloneNotSupportedException e) {
                    throw new AssertionError();
                }
            }

        }

    }

}