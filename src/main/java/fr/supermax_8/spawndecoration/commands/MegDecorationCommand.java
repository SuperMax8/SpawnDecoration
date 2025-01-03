package fr.supermax_8.spawndecoration.commands;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import com.ticxo.modelengine.api.model.ModelRegistry;
import com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes;
import com.ticxo.modelengine.api.model.bone.behavior.BoneBehaviorType;
import com.ticxo.modelengine.api.utils.config.ConfigProperty;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import fr.supermax_8.spawndecoration.SpawnDecorationConfig;
import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoList;
import fr.supermax_8.spawndecoration.blueprint.StaticDecoration;
import fr.supermax_8.spawndecoration.manager.DecorationManager;
import fr.supermax_8.spawndecoration.manager.RecordLocationManager;
import fr.supermax_8.spawndecoration.manager.WEClipboardManager;
import fr.supermax_8.spawndecoration.utils.SerializationMethods;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.command.CommandActor;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@CommandPermission("modelenginedecoration.use")
@Command({"modelenginedecoration", "mdec", "hendek"})
public class MegDecorationCommand {

    private HashMap<UUID, List<StaticDecoList.StaticDeco>> purgeConfirm = new HashMap<>();

    @CommandPlaceholder
    public void cmd(BukkitCommandActor actor) {
        sendHelp(actor.sender());
    }

    @Subcommand("record")
    public void record(Player p, String record) {
        RecordLocationManager.startRecord(p, record);
    }

    @Subcommand("reload")
    public void reload(CommandActor actor) {
        actor.reply("§6MegDecoration Reload...");
        SpawnDecorationConfig.reload();
        actor.reply("§6MegDecoration Reload Done");
    }

    @Subcommand("near")
    public void near(Player actor, double radius) {
        near(actor, radius, null);
    }

    @Subcommand("near")
    public void near(Player actor, double radius, String modelId) {
        actor.sendMessage("§7Near decorations:");

        Location pLoc = actor.getLocation();
        StaticDecoList list = DecorationManager.getInstance().readStaticDecos();
        int count = 0;
        for (StaticDecoList.StaticDeco st : list.getList()) {
            Location decLoc = SerializationMethods.deserializedLocation(st.getLocation());
            if (decLoc.getWorld() != pLoc.getWorld() || decLoc.distance(pLoc) > radius ||
                    (modelId != null && !st.getModelId().toLowerCase().contains(modelId.toLowerCase()))
            ) continue;

            sendDeco(actor, st);
            count++;
        }
        actor.sendMessage("§7Deco Count: §6" + count);
    }

    @Subcommand("deco")
    public void deco(Player p) {
        PaginatedGui paginatedGui = Gui.paginated()
                .title(Component.text("Decorations"))
                .rows(6)
                .pageSize(45)
                .create();

        ModelRegistry registry = ModelEngineAPI.getAPI().getModelRegistry();
        List<GuiItem> stacks = new ArrayList<>();
        for (String name : registry.getKeys()) {
            if (name.startsWith("d_") || name.contains("deco")) {
                ModelBlueprint blueprint = registry.get(name);

                int cmd = getRelevantDataFromBlueprint(blueprint);
                ItemStack stack = ConfigProperty.ITEM_MODEL.getBaseItem().create(Color.WHITE, cmd);
                GuiItem itm = ItemBuilder.from(stack)
                        .setNbt("megdecoration_modelid", name)
                        .flags(ItemFlag.values())
                        .name(Component.text(blueprint.getName()))
                        .lore(List.of(Component.text("§7Preview CustomModelData: §f" + cmd)))
                        .asGuiItem(event -> event.setCancelled(false));
                stacks.add(itm);
            }
        }

        stacks.sort((g1, g2) -> {
            String s1 = "", s2 = "";
            if (g1.getItemStack().hasItemMeta() && g1.getItemStack().getItemMeta().hasDisplayName())
                s1 = g1.getItemStack().getItemMeta().getDisplayName();
            if (g2.getItemStack().hasItemMeta() && g2.getItemStack().getItemMeta().hasDisplayName())
                s2 = g2.getItemStack().getItemMeta().getDisplayName();
            return s1.compareTo(s2);
        });

        stacks.forEach(paginatedGui::addItem);

        paginatedGui.setItem(6, 3, ItemBuilder.from(Material.PAPER).setName("Previous").asGuiItem(event -> {
            paginatedGui.previous();
            event.setCancelled(true);
        }));
        paginatedGui.setItem(6, 7, ItemBuilder.from(Material.PAPER).setName("Next").asGuiItem(event -> {
            paginatedGui.next();
            event.setCancelled(true);
        }));

        paginatedGui.open(p);
    }


    @Subcommand("copy")
    public void copy(Player p) {
        WEClipboardManager.copy(p, false);
    }

    @Subcommand("cut")
    public void cut(Player p) {
        WEClipboardManager.copy(p, true);
    }

    @Subcommand("paste")
    public void paste(Player p) {
        WEClipboardManager.paste(p);
    }

    @Subcommand("list")
    public void list(BukkitCommandActor actor, @Optional String modelId) {
        StaticDecoList list = DecorationManager.getInstance().readStaticDecos();
        actor.reply("§7List:");
        int count = 0;
        for (StaticDecoList.StaticDeco st : list.getList()) {
            if (modelId != null && !st.getModelId().toLowerCase().contains(modelId.toLowerCase())) {
                continue;
            }

            sendDeco(actor.sender(), st);
            count++;
        }
        actor.reply("§7Deco Count: §6" + count);
    }

    @Subcommand("purge")
    public void purge(Player p, String modelId) {
        StaticDecoList list = DecorationManager.getInstance().readStaticDecos();
        ArrayList<StaticDecoList.StaticDeco> toPurge = new ArrayList<>();
        list.getList().forEach(staticDeco -> {
            if (staticDeco.getModelId().equals(modelId)) {
                toPurge.add(staticDeco);
            }
        });
        purgeConfirm.put(p.getUniqueId(), toPurge);
        p.sendMessage("§7List:");
        for (StaticDecoList.StaticDeco staticDeco : toPurge) {
            sendDeco(p, staticDeco);
        }
        p.sendMessage("§c§lAre you sure you want to purge all theses decorations?");
        p.sendMessage("§4/mdec confirmpurge true");
    }

    @Subcommand("confirmpurge")
    public void confirmPurge(Player p, boolean imSure) {
        if (!purgeConfirm.containsKey(p.getUniqueId())) {
            p.sendMessage("§cNothing to purge, /mdec purge <modelId>");
            return;
        }
        List<StaticDecoList.StaticDeco> list = purgeConfirm.remove(p.getUniqueId());
        if (!imSure) {
            p.sendMessage("§cWrong yesImSure !");
            return;
        }
        DecorationManager.getInstance().removeStaticDeco(list);
    }

    @Subcommand("teleport")
    public void teleport(Player p, World world, double x, double y, double z) {
        p.teleport(new Location(world, x, y, z));
    }

    @Subcommand("text")
    public void text(Player p) {
        StaticDecoration closest = getClosestDeco(p.getLocation());
        if (closest == null) {
            p.sendMessage("§cThere is no close decoration !");
            return;
        }

        if (closest.getTexts() == null) {
            if (closest.getHolograms() == null) {
                p.sendMessage("§cThe model of the decoration can't have text!");
                return;
            }
            p.sendMessage("§6The decoration has no texts!");
            return;
        }
        p.sendMessage("§7Text of decoration: §6" + closest.getModelId() + " §e" + closest.getLocation());
        closest.getTexts().forEach((id, l) -> {
            p.sendMessage("§6" + id);
            for (String text : l) {
                p.sendMessage("§8- §r" + text);
            }
        });
    }

    @Subcommand("text add")
    public void textAdd(Player p, String textId, String text) {
        StaticDecoration closest = getClosestDeco(p.getLocation());
        if (closest == null) {
            p.sendMessage("§cThere is no close decoration !");
            return;
        }
        if (closest.getHolograms() == null) {
            p.sendMessage("§cThe model of the decoration can't have text!");
            return;
        }
        DecorationManager.getInstance().editStaticDecos(staticDecoList -> {
            for (StaticDecoList.StaticDeco deco : staticDecoList.getList()) {
                if (!SerializationMethods.deserializedLocation(deco.getLocation()).equals(closest.getLocation()))
                    continue;
                if (deco.getTexts() == null) deco.setTexts(new HashMap<>());
                deco.getTexts().computeIfAbsent(textId, k -> new ArrayList<>()).add(text);
                break;
            }
        });
        p.sendMessage("§6New line added: §r" + text);
    }

    @Subcommand("text clear")
    public void textClear(Player p, String textId) {
        StaticDecoration closest = getClosestDeco(p.getLocation());
        if (closest == null) {
            p.sendMessage("§cThere is no close decoration !");
            return;
        }
        if (closest.getHolograms() == null) {
            p.sendMessage("§cThe model of the decoration can't have text!");
            return;
        }
        DecorationManager.getInstance().editStaticDecos(staticDecoList -> {
            for (StaticDecoList.StaticDeco deco : staticDecoList.getList()) {
                if (!SerializationMethods.deserializedLocation(deco.getLocation()).equals(closest.getLocation()))
                    continue;
                if (deco.getTexts() == null) deco.setTexts(new HashMap<>());
                deco.getTexts().remove(textId);
                break;
            }
        });
        p.sendMessage("§6Lines cleared for text §r" + textId);
    }

    private StaticDecoration getClosestDeco(Location loc) {
        StaticDecoration deco = null;
        double distance = Double.MAX_VALUE;

        for (List<StaticDecoration> d : DecorationManager.getInstance().getStaticDecoMap().values()) {
            for (StaticDecoration dec : d) {
                Location l = dec.getLocation();
                if (!l.getWorld().equals(loc.getWorld())) continue;
                double dsq = l.distanceSquared(loc);
                if (dsq <= 5 * 5 && dsq < distance) {
                    distance = dsq;
                    deco = dec;
                }
            }
        }

        return deco;
    }

    private void sendDeco(CommandSender p, StaticDecoList.StaticDeco st) {
        Location loc = SerializationMethods.deserializedLocation(st.getLocation());
        TextComponent textComponent = Component.text("§6§l" + st.getModelId() + " §8: §7" + st.getLocation())
                .hoverEvent(HoverEvent.showText(Component.text("Click to teleport")))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/mdec teleport " + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
        BukkitAudiences.builder(SpawnDecorationPlugin.getInstance()).build().sender(p).sendMessage(textComponent);
    }

    private void sendHelp(CommandSender sender) {
        int count = 0;
        for (List<StaticDecoration> l : DecorationManager.getInstance().getStaticDecoMap().values())
            count += l.size();
        sender.sendMessage(new String[]{
                "§8[§6ModelEngineDecoration§8]",
                "§7Version: " + SpawnDecorationPlugin.version,
                "§7Records: " + RecordLocationManager.records.size(),
                "§7TrackedDecorations: " + DecorationManager.getInstance().getTrackedDecoMap().size(),
                "§7StaticDecorations: " + count,
                "§f- §7/megdecoration record <newRecordName>",
                "§f- §7/megdecoration deco §fShow GUI to get static decos",
                "§f- §7/megdecoration reload §fReload decos",
                "§f- §7/megdecoration copy §fCopy static decos from Fawe selection",
                "§f- §7/megdecoration cut §fCut (copy and remove) static decos from Fawe selection",
                "§f- §7/megdecoration paste §fPaste static decos from deco clipboard",
                "§f- §7/megdecoration list <modelId(optional)> §fList all deco",
                "§f- §7/megdecoration purge <modelId> §fPurge static decos from all the worls",
        });
    }

    private int getRelevantDataFromBlueprint(ModelBlueprint blueprint) {
        AtomicReference<BlueprintBone> head = new AtomicReference<>(null);
        List<BlueprintBone> bones = blueprint.getBones().values().stream()
                .filter(bb -> {
                            Map<BoneBehaviorType<?>, BoneBehaviorType.CachedProvider<?>> types = bb.getCachedBehaviorProvider();
                            if (bb.getDataId() == 0) return false;
                            if (types.containsKey(BoneBehaviorTypes.HEAD)) {
                                head.set(bb);
                                return false;
                            }
                            return true;
                        }
                ).toList();
        int id = 0;
        if (head.get() != null)
            id = head.get().getDataId();
        else if (!bones.isEmpty()) {
            BlueprintBone bestOne = null;
            for (BlueprintBone bone : bones) {
                if (bestOne == null) bestOne = bone;
                double bestOneScale = bestOne.getModelScale().length();
                double boneScale = bestOne.getModelScale().length();
                if (boneScale > 0 && boneScale < bestOneScale) bestOne = bone;
            }

            if (bestOne == null) bestOne = bones.get(0);
            id = bestOne.getDataId();
        }

        return id;
    }


}