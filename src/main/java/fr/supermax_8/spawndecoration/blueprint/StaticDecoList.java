package fr.supermax_8.spawndecoration.blueprint;

import lombok.Getter;

import java.util.ArrayList;

@Getter
public class StaticDecoList {

    private ArrayList<StaticDeco> list;

    public StaticDecoList(ArrayList<StaticDeco> list) {
        this.list = list;
    }

    @Getter
    public static class StaticDeco {
        private String location;
        private String modelId;

        public StaticDeco(String location, String modelId) {
            this.location = location;
            this.modelId = modelId;
        }

    }

}