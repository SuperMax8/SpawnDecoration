package fr.supermax_8.spawndecoration.blueprint;

import fr.supermax_8.spawndecoration.utils.SerializationMethods;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
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
        private double defaultAnimationSpeed;
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
            defaultAnimationSpeed = 1.0;
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
            private ModelItem modelItem;

            public ModelTransformation() {
                visible = true;
                scale = 1.0;
                position = new Vector3f();
                rotation = new Quaternionf();
                modelItem = null;
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

            @AllArgsConstructor
            public static class ModelItem {

                private final Material material;
                private final String itemModel;
                private final boolean enchanted;
                private final CustomModelDataWrapper customModelData;

                public ModelItem(ItemStack fromStack) {
                    material = fromStack.getType();
                    ItemMeta itemMeta = fromStack.getItemMeta();
                    itemModel = itemMeta.getItemModel() != null ? itemMeta.getItemModel().toString() : null;
                    enchanted = !fromStack.getEnchantments().isEmpty();
                    customModelData = new CustomModelDataWrapper(itemMeta.getCustomModelDataComponent());
                }

                public ItemStack createStack() {
                    ItemStack itemStack = new ItemStack(material);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if (itemModel != null) itemMeta.setItemModel(NamespacedKey.fromString(itemModel));
                    if (enchanted) itemMeta.addEnchant(Enchantment.SHARPNESS, 1, true);
                    customModelData.fill(itemMeta.getCustomModelDataComponent());
                    itemStack.setItemMeta(itemMeta);
                    return itemStack;
                }

                @Override
                public String toString() {
                    return "ModelItem{" +
                            "material=" + material +
                            ", itemModel='" + itemModel + '\'' +
                            ", enchanted=" + enchanted +
                            ", customModelData=" + customModelData +
                            '}';
                }

                private class CustomModelDataWrapper {

                    private List<Float> floats;
                    private List<Boolean> flags;
                    private List<String> strings;
                    private List<Color> colors;

                    public CustomModelDataWrapper(CustomModelDataComponent component) {
                        floats = component.getFloats();
                        flags = component.getFlags();
                        strings = component.getStrings();
                        colors = component.getColors();
                    }

                    public void fill(CustomModelDataComponent component) {
                        component.setFloats(floats);
                        component.setFlags(flags);
                        component.setStrings(strings);
                        component.setColors(colors);
                    }

                    @Override
                    public String toString() {
                        return "CustomModelDataWrapper{" +
                                "floats=" + floats +
                                ", flags=" + flags +
                                ", strings=" + strings +
                                ", colors=" + colors +
                                '}';
                    }
                }

            }

        }

    }

}