package fr.supermax_8.spawndecoration.blueprint;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import fr.supermax_8.spawndecoration.SpawnDecorationConfig;
import fr.supermax_8.spawndecoration.manager.AroundManager;
import fr.supermax_8.spawndecoration.utils.EntityIdProvider;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class Hologram {

    private static final AroundManager aroundManager = AroundManager.getInstance();

    @Getter
    private final int entityID = EntityIdProvider.provide();
    private final AroundManager.Around around;
    @Getter
    private final List<Player> viewers = new CopyOnWriteArrayList<>();
    @Getter
    @Setter
    private int bgAlpha = 0, bgRed = 0, bgGreen = 0, bgBlue = 0;
    @Getter
    @Setter
    private Display.Billboard billboard = Display.Billboard.FIXED;
    @Getter
    @Setter
    private int maxLineWidth = 200;
    @Getter
    @Setter
    private double viewRange = 1.0;
    @Setter
    @Getter
    private boolean shadow = false;
    @Getter
    @Setter
    private byte textOpacity = (byte) -1;
    @Getter
    @Setter
    private TextDisplay.TextAlignment alignment = TextDisplay.TextAlignment.CENTER;
    @Getter
    @Setter
    private boolean seeThroughBlocks = false;
    @Setter
    @Getter
    private int interpolationDurationRotation = 10;
    @Setter
    @Getter
    private int interpolationDurationTransformation = 10;
    @Setter
    @Getter
    private Vector3f translation = new Vector3f(0, 0, 0);
    @Setter
    @Getter
    private Vector3f scale = new Vector3f(1, 1, 1);
    @Setter
    @Getter
    private Component text = Component.text("Hologram");
    @Getter
    private boolean removed = false;
    private Location location;

    public Hologram(Location spawnLoc) {
        this(spawnLoc, null);
    }

    public Hologram(Location spawnLoc, Predicate<Player> detection) {
        location = spawnLoc;
        around = aroundManager.addAround(spawnLoc, SpawnDecorationConfig.getRenderRadius(), pEnter -> {
            viewers.add(pEnter);
            WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                    entityID, Optional.of(UUID.randomUUID()), EntityTypes.TEXT_DISPLAY,
                    new Vector3d(location.getX(), location.getY(), location.getZ()), location.getPitch(), location.getYaw(), 0f, 0, Optional.empty()
            );
            sendPacket(pEnter, spawnPacket, createMetaPacket(pEnter));
        }, pLeave -> {
            viewers.remove(pLeave);
            sendPacket(pLeave, new WrapperPlayServerDestroyEntities(entityID));
        }, detection);
    }

    public void setBackgroundColor(int alpha, int red, int green, int blue) {
        this.bgAlpha = alpha;
        this.bgRed = red;
        this.bgGreen = green;
        this.bgBlue = blue;
    }

    public void remove() {
        if (removed) return;
        removed = true;
        sendToViewers(createRemovePacket());
        aroundManager.removeAround(around);
    }

    public void update() {
        for (Player p : viewers)
            sendPacket(p, createMetaPacket(p));
    }

    public void teleport(Location loc) {
        if (location.equals(loc)) return;
        location = loc.clone();
        WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport(entityID, new com.github.retrooper.packetevents.protocol.world.Location(
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()
        ), false);
        sendToViewers(teleport);
    }

    private WrapperPlayServerDestroyEntities createRemovePacket() {
        return new WrapperPlayServerDestroyEntities(entityID);
    }

    private WrapperPlayServerEntityMetadata createMetaPacket(Player p) {
        return new WrapperPlayServerEntityMetadata(entityID, createMeta(p));
    }

    public boolean containsPlaceholder() {
        String json = AdventureSerializer.toJson(text);
        return PlaceholderAPI.containsPlaceholders(json);
    }

    private TextDisplayMeta createMeta(Player p) {
        TextDisplayMeta meta = (TextDisplayMeta) EntityMeta.createMeta(this.entityID, EntityTypes.TEXT_DISPLAY);
        String json = AdventureSerializer.toJson(text);
        meta.setText(AdventureSerializer.parseComponent(PlaceholderAPI.setPlaceholders(p, json)));
        meta.setInterpolationDelay(-1);
        meta.setTransformationInterpolationDuration(this.interpolationDurationTransformation);
        meta.setPositionRotationInterpolationDuration(this.interpolationDurationRotation);
        meta.setTranslation(this.translation);

        meta.setScale(this.scale);
        meta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.valueOf(this.billboard.name()));
        meta.setLineWidth(this.maxLineWidth);
        meta.setViewRange(1);
        meta.setBackgroundColor(convertARGBToDecimal(bgAlpha, bgRed, bgGreen, bgBlue));
        meta.setTextOpacity(this.textOpacity);
        meta.setShadow(this.shadow);
        meta.setSeeThrough(this.seeThroughBlocks);
        setMetaAlignment(meta);
        return meta;
    }

    private void setMetaAlignment(TextDisplayMeta meta) {
        switch (this.alignment) {
            case LEFT -> meta.setAlignLeft(true);
            case RIGHT -> meta.setAlignRight(true);
        }
    }


    /**
     * Converts ARGB components to a signed decimal integer (10 digits) using two's complement.
     *
     * @param alpha The alpha value (transparency).
     * @param red   The red value.
     * @param green The green value.
     * @param blue  The blue value.
     * @return The signed decimal value.
     */
    public static int convertARGBToDecimal(int alpha, int red, int green, int blue) {
        // Combine ARGB components into a 32-bit value
        int argb = (alpha << 24) | (red << 16) | (green << 8) | blue;

        // Apply two's complement if the value is negative
        long signedValue;
        if (argb >= 0) {
            signedValue = argb;
        } else {
            signedValue = (long) argb - (1L << 32);
        }

        return (int) signedValue;
    }

    public void sendToViewers(PacketWrapper<?>... packets) {
        for (Player player : viewers)
            sendPacket(player, packets);
    }

    public void sendPacket(Player p, PacketWrapper<?>... packets) {
        for (PacketWrapper<?> wrapper : packets)
            PacketEvents.getAPI().getPlayerManager().getUser(p).sendPacket(wrapper);
    }


}