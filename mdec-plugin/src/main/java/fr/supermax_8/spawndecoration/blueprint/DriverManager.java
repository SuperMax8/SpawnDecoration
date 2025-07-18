package fr.supermax_8.spawndecoration.blueprint;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.mount.controller.MountController;
import com.ticxo.modelengine.api.nms.impl.EmptyMoveController;
import fr.supermax_8.spawndecoration.ModelEngineDecorationPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

public class DriverManager {

    @Getter
    private static final DriverManager instance = new DriverManager();
    private final ArrayList<ActiveModel> activeModels = new ArrayList<>();

    public DriverManager() {
    }

    public void initTask() {
        Bukkit.getScheduler().runTaskTimer(ModelEngineDecorationPlugin.getInstance(), () -> {
            for (ActiveModel model : new LinkedList<>(activeModels)) {
                if (model.isRemoved() || model.isDestroyed()) activeModels.remove(model);
                model.getMountManager().ifPresent(mountManager -> {
                    for (Entity passenger : mountManager.getPassengerSeatMap().keySet()) {
                        MountController mountController = getController(passenger.getUniqueId());
                        if (mountController == null) continue;
                        mountController.updateRiderPosition(new EmptyMoveController());
                        mountController.updatePassengerMovement(null, model);
                    }
                });
            }
        }, 0, 0);
    }

    private MountController getController(UUID uuid) {
        return ModelEngineAPI.getMountPairManager().getController(uuid);
    }


    public static void addDriver(ActiveModel model) {
        getInstance().activeModels.add(model);
    }

    public static void clear() {
        getInstance().activeModels.clear();
    }

}