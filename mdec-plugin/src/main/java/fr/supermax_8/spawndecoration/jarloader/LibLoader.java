package fr.supermax_8.spawndecoration.jarloader;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

import static fr.supermax_8.spawndecoration.ModelEngineDecoration.log;

public class LibLoader {

    private static final String[] libsLink = {
            "https://github.com/SuperMax8/SpawnDecoration/releases/download/1.5.3/mdec-libs-1.5.3.jar"
    };

    public static void loadExternalLibs(File dataFolder) throws IOException {
        log("Loading external libs");
        long ts = System.currentTimeMillis();
        File libs = new File(dataFolder, "libs");
        libs.mkdirs();
        URLClassLoader loader = (URLClassLoader) LibLoader.class.getClassLoader();

        File AIO = new File(libs, "AIO.jar");
        if (AIO.exists()) {
            fr.supermax_8.spawndecoration.jarloader.JarDependency dependency = new fr.supermax_8.spawndecoration.jarloader.JarDependency(AIO.toPath());
            fr.supermax_8.spawndecoration.jarloader.JarLoader.load(dependency, loader);
            long ts2 = System.currentTimeMillis();
            log("Libs loaded in " + (ts2 - ts) + " ms");
            return;
        }

        log("If you have problems loading libs, you can download them directly and put it in plugins/BoostedAudio/libs");
        for (String link : libsLink) {
            log(link);
            File lib = new File(libs, link.substring(link.lastIndexOf('/') + 1));
            Path libsPath = Paths.get(lib.getAbsolutePath());

            fr.supermax_8.spawndecoration.jarloader.JarDependency dependency = new fr.supermax_8.spawndecoration.jarloader.JarDependency(link, libsPath);
            fr.supermax_8.spawndecoration.jarloader.JarLoader.downloadIfNotExists(dependency);
            fr.supermax_8.spawndecoration.jarloader.JarLoader.load(dependency, loader);
        }
        long ts2 = System.currentTimeMillis();
        log("Libs loaded in " + (ts2 - ts) + " ms");
    }

    public static long loadLibs(File dataFolder) {
        long ts = System.currentTimeMillis();
        try {
            File libs = new File(dataFolder, "libs");
            libs.mkdirs();

            URLClassLoader loader = (URLClassLoader) LibLoader.class.getClassLoader();

            // Load the AIO
            File AIO = new File(libs, "AIO.jar");
            if (AIO.exists()) {
                fr.supermax_8.spawndecoration.jarloader.JarDependency dependency = new fr.supermax_8.spawndecoration.jarloader.JarDependency(AIO.toPath());
                fr.supermax_8.spawndecoration.jarloader.JarLoader.load(dependency, loader);
                long ts2 = System.currentTimeMillis();
                return ts2 - ts;
            }

            // Classic libs loading
            for (String link : libsLink) {
                System.out.println("Loading " + link);
                //BoostedAudioAPI.getAPI().info(link);
                File lib = new File(libs, link.substring(link.lastIndexOf('/') + 1));
                Path libsPath = Paths.get(lib.getAbsolutePath());

                fr.supermax_8.spawndecoration.jarloader.JarDependency dependency = new fr.supermax_8.spawndecoration.jarloader.JarDependency(link, libsPath);
                fr.supermax_8.spawndecoration.jarloader.JarLoader.downloadIfNotExists(dependency);
                fr.supermax_8.spawndecoration.jarloader.JarLoader.load(dependency, loader);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        long ts2 = System.currentTimeMillis();
        return ts2 - ts;
    }

}