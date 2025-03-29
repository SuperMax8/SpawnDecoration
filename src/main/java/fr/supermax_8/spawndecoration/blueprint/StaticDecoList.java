package fr.supermax_8.spawndecoration.blueprint;

import lombok.Getter;
import lombok.Setter;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class StaticDecoList {

    private final ArrayList<StaticDeco> list;

    public StaticDecoList(ArrayList<StaticDeco> list) {
        this.list = list;
    }

    @Getter
    @Setter
    public static class StaticDeco {
        private UUID id;
        private String location;
        private String modelId;
        private String defaultAnimation;
        private double scale;
        private Quaternionf rotation;
        private Map<String, List<String>> texts;

        public StaticDeco(UUID id, String location, String modelId, String defaultAnimation, double scale, Quaternionf rotation, Map<String, List<String>> texts) {
            this.id = id;
            this.location = location;
            this.modelId = modelId;
            this.defaultAnimation = defaultAnimation;
            this.scale = scale;
            this.rotation = rotation;
            this.texts = texts;
        }

    }

}