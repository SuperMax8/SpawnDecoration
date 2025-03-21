package fr.supermax_8.spawndecoration.manager;

import com.fastasyncworldedit.core.wrappers.AsyncPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoList;
import fr.supermax_8.spawndecoration.utils.SerializationMethods;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WEClipboardManager {

    public static ConcurrentHashMap<UUID, List<StaticDecoList.StaticDeco>> clipboards = new ConcurrentHashMap<>();

    public static AsyncPlayer getAsyncPlayer(final Player player) {
        return AsyncPlayer.wrap(WorldEditPlugin.getInstance().wrapPlayer(player));
    }

    public static void copy(Player p, boolean cut) {
        Location pLoc = p.getLocation();
        AsyncPlayer asyncPlayer = getAsyncPlayer(p);
        Region region = asyncPlayer.getSelection();
        StaticDecoList decoList = DecorationManager.getInstance().readStaticDecos();
        ArrayList<StaticDecoList.StaticDeco> offsetedDecos = new ArrayList<>();
        ArrayList<StaticDecoList.StaticDeco> decos = new ArrayList<>();
        for (StaticDecoList.StaticDeco deco : decoList.getList()) {
            Location loc = SerializationMethods.deserializedLocation(deco.getLocation());
            if (!region.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) continue;
            Location offset = loc.subtract(pLoc.getBlockX(), pLoc.getBlockY(), pLoc.getBlockZ());
            offsetedDecos.add(new StaticDecoList.StaticDeco(UUID.randomUUID(), SerializationMethods.serializedLocation(offset), deco.getModelId(), deco.getScale(), deco.getRotation(), deco.getTexts()));
            decos.add(deco);
        }

        if (offsetedDecos.isEmpty()) {
            p.sendMessage("§cNo decos found in worldedit selection !");
            return;
        }
        clipboards.put(p.getUniqueId(), offsetedDecos);
        if (cut) {
            DecorationManager.getInstance().removeStaticDeco(decos);
            p.sendMessage("§e§l" + offsetedDecos.size() + " §2decos cut to the clipboard !");
        } else {
            p.sendMessage("§e§l" + offsetedDecos.size() + " §2decos copied to the clipboard !");
        }
    }


    public static void paste(Player p) {
        List<StaticDecoList.StaticDeco> clipboard = clipboards.get(p.getUniqueId());
        if (clipboard == null) {
            p.sendMessage("§cEmpty decos clipboard !");
            return;
        }
        Location pLoc = p.getLocation();
        LinkedList<StaticDecoList.StaticDeco> decos = new LinkedList<>();
        for (StaticDecoList.StaticDeco deco : clipboard) {
            Location offset = SerializationMethods.deserializedLocation(deco.getLocation());
            Location newLoc = offset.add(pLoc.getBlockX(), pLoc.getBlockY(), pLoc.getBlockZ());
            newLoc.setWorld(pLoc.getWorld());
            decos.add(new StaticDecoList.StaticDeco(UUID.randomUUID(), SerializationMethods.serializedLocation(newLoc), deco.getModelId(), deco.getScale(), deco.getRotation(), deco.getTexts()));
            p.sendMessage("§7Deco §e" + deco.getModelId() + " §7pasted: §e" + newLoc.getWorld().getName() + " " + newLoc.getBlockX() + " " + newLoc.getBlockY() + " " + newLoc.getBlockZ());
        }

        DecorationManager.getInstance().addStaticDecos(decos);
        p.sendMessage("§e§l" + decos.size() + " §2decos pasted at " + pLoc.getBlockX() + " " + pLoc.getBlockY() + " " + pLoc.getBlockZ());
    }

}