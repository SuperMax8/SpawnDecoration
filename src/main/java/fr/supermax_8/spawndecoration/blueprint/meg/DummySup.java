package fr.supermax_8.spawndecoration.blueprint.meg;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.BaseEntity;
import com.ticxo.modelengine.api.entity.data.IEntityData;
import com.ticxo.modelengine.api.nms.entity.EntityHandler;
import com.ticxo.modelengine.api.nms.entity.wrapper.BodyRotationController;
import com.ticxo.modelengine.api.nms.entity.wrapper.LookController;
import com.ticxo.modelengine.api.nms.entity.wrapper.MoveController;
import com.ticxo.modelengine.api.nms.impl.DefaultBodyRotationController;
import com.ticxo.modelengine.api.nms.impl.EmptyLookController;
import com.ticxo.modelengine.api.nms.impl.EmptyMoveController;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

@Getter
@Setter
public class DummySup<O> implements BaseEntity<O> {

    private Supplier<Location> sup;
    protected final DummyDataSup data;

    protected final BodyRotationController bodyRotationController;
    protected final MoveController moveController;
    protected final LookController lookController;

    protected boolean detectingPlayers = true;
    protected boolean isRemoved;
    protected boolean isWalking;
    protected boolean isStrafing;
    protected boolean isJumping;
    protected boolean isFlying;

    protected O original;
    protected UUID uuid;

    public DummySup() {
        this(null, null);
    }

    public DummySup(O original, Supplier<Location> sup) {
        this(original, sup, UUID.randomUUID());
    }

    public DummySup(O original, Supplier<Location> sup, UUID id) {
        this.sup = sup;
        this.original = original;
        data = new DummyDataSup(this);
        bodyRotationController = new DefaultBodyRotationController(this);
        moveController = new EmptyMoveController();
        lookController = new EmptyLookController();
        uuid = id;
    }

    @Override
    public boolean isAlive() {
        return !isRemoved;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    public void setLocation(Location loc) {
        data.setLocation(loc);
    }

    @Override
    public Location getLocation() {
        return data.location;
    }

    @Override
    public int getRenderRadius() {
        return data.getRenderRadius();
    }

    @Override
    public void setRenderRadius(int radius) {
        data.setRenderRadius(radius);
    }

    @Override
    public float getYRot() {
        return getLocation().getYaw();
    }

    public void setForceViewing(Player player, boolean flag) {
        if (flag) {
            setForceHidden(player, false);
            data.getTracked().addForcedPairing(player);
        } else {
            data.getTracked().removeForcedPairing(player);
        }
    }

    public void setForceHidden(Player player, boolean flag) {
        if (flag) {
            setForceViewing(player, false);
            data.getTracked().addForcedHidden(player);
        } else {
            data.getTracked().removeForcedHidden(player);
        }
    }

    @Override
    public O getOriginal() {
        return original;
    }

    @Override
    public IEntityData getData() {
        return data;
    }

    @Override
    public void registerData() {
        ModelEngineAPI.getAPI().getDataTrackers().execute(this.getUUID(), (uuid, tracker) -> tracker.putEntityData(uuid, this.data));
    }

    //region Unused
    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public void setVisible(boolean flag) {

    }

    @Override
    public boolean isRemoved() {
        return isRemoved;
    }

    @Override
    public boolean isForcedAlive() {
        return false;
    }

    @Override
    public void setForcedAlive(boolean flag) {

    }

    @Override
    public int getEntityId() {
        return 0;
    }

    @Override
    public double getMaxStepHeight() {
        return 0;
    }

    @Override
    public void setMaxStepHeight(double stepHeight) {

    }

    @Override
    public void setCollidableWith(org.bukkit.entity.Entity entity, boolean flag) {

    }

    @Override
    public BodyRotationController getBodyRotationController() {
        return bodyRotationController;
    }

    @Override
    public MoveController getMoveController() {
        return moveController;
    }

    @Override
    public LookController getLookController() {
        return lookController;
    }

    @Override
    public boolean isGlowing() {
        return false;
    }

    @Override
    public int getGlowColor() {
        return 0;
    }

    @Override
    public boolean hurt(@Nullable HumanEntity player, Object nmsDamageCause, float damage) {
        return false;
    }

    @Override
    public EntityHandler.InteractionResult interact(HumanEntity player, EquipmentSlot slot) {
        return null;
    }

    @Override
    public float getYBodyRot() {
        return getLocation().getYaw();
    }

    @Override
    public float getYHeadRot() {
        return getLocation().getYaw();
    }

    @Override
    public float getXHeadRot() {
        return getLocation().getPitch();
    }

    @Override
    public boolean isWalking() {
        return isWalking;
    }

    @Override
    public boolean isStrafing() {
        return isStrafing;
    }

    @Override
    public boolean isJumping() {
        return isJumping;
    }

    @Override
    public boolean isFlying() {
        return isFlying;
    }

    @Override
    public float getHealth() {
        return 20;
    }

    @Override
    public float getMaxHealth() {
        return 20;
    }

}