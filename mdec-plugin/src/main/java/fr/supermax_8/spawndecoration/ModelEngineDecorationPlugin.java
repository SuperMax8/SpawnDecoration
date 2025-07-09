package fr.supermax_8.spawndecoration;

import fr.supermax_8.spawndecoration.jarloader.LibLoader;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Used as a wrapper of ModelEngineDecoration to load libs
 */
public final class ModelEngineDecorationPlugin extends JavaPlugin {

    @Getter
    private static ModelEngineDecorationPlugin instance;
    private ModelEngineDecoration modelEngineDecoration;

    @Override
    public void onLoad() {
        instance = this;
        LibLoader.loadLibs(getDataFolder());
        modelEngineDecoration = new ModelEngineDecoration(this);
        modelEngineDecoration.onLoad();
    }

    @Override
    public void onEnable() {
        modelEngineDecoration.onEnable();
    }

    @Override
    public void onDisable() {
        modelEngineDecoration.onDisable();
    }

}