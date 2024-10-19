package fr.supermax_8.spawndecoration.blueprint;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class StaticDecoList {

    private ArrayList<StaticDeco> list;

    public StaticDecoList(ArrayList<StaticDeco> list) {
        this.list = list;
    }

    @Getter
    @Setter
    public static class StaticDeco {
        private String location;
        private String modelId;
        private Map<String, List<String>> texts;

        public StaticDeco(String location, String modelId, Map<String, List<String>> texts) {
            this.location = location;
            this.modelId = modelId;
            this.texts = texts;
        }

    }

}