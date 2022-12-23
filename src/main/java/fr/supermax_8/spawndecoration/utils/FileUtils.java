package fr.supermax_8.spawndecoration.utils;

import java.io.File;
import java.util.LinkedList;

public class FileUtils {

    public static LinkedList<File> getFilesRecursively(File directory) {
        LinkedList<File> files = new LinkedList<>();
        for (File f : directory.listFiles()) {
            if (f.isDirectory()) files.addAll(getFilesRecursively(f));
            else files.add(f);
        }
        return files;
    }


}