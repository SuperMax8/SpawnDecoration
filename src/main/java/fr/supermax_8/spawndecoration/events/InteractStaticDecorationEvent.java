package fr.supermax_8.spawndecoration.events;

import fr.supermax_8.spawndecoration.blueprint.StaticDecoration;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class InteractStaticDecorationEvent extends PlayerEvent implements Cancellable {

    private static HandlerList list = new HandlerList();

    @Getter
    @Setter
    private boolean cancelled;
    @Getter
    private final StaticDecoration decoration;
    private final InteractionType interactionType;

    public InteractStaticDecorationEvent(@NotNull Player who, StaticDecoration decoration, InteractionType interactionType) {
        super(who);
        this.decoration = decoration;
        this.interactionType = interactionType;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return list;
    }

    public static @NotNull HandlerList getHandlerList() {
        return list;
    }

    public enum InteractionType {
        INTERACT,
        HIT
    }


}