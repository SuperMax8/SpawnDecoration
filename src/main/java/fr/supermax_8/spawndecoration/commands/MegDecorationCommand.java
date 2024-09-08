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
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class MegDecorationCommand implements CommandExecutor, TabCompleter {

    private static HashMap<UUID, List<StaticDecoList.StaticDeco>> purgeConfirm = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        try {
            if (!sender.hasPermission("modelenginedecoration.use")) return false;
            switch (args[0]) {
                case "record" -> {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage("§cYou are not a player !");
                        return false;
                    }
                    RecordLocationManager.startRecord(p, args[1]);
                }
                case "reload" -> {
                    sender.sendMessage("§6MegDecoration Reload...");
                    SpawnDecorationConfig.reload();
                    sender.sendMessage("§6MegDecoration Reload Done");
                }
                case "deco" -> {
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

                    paginatedGui.open((HumanEntity) sender);
                }
                case "copy" -> {
                    WEClipboardManager.copy((Player) sender, false);
                }
                case "cut" -> {
                    WEClipboardManager.copy((Player) sender, true);
                }
                case "paste" -> {
                    WEClipboardManager.paste((Player) sender);
                }
                case "list" -> {
                    Player p = (Player) sender;
                    StaticDecoList list = DecorationManager.getInstance().readStaticDecos();
                    p.sendMessage("§7List:");
                    list.getList().forEach(st -> {
                        if (args.length >= 2 && !args[1].equals(st.getModelId())) {
                            return;
                        }
                        sendDeco(p, st);
                    });
                }
                case "purge" -> {
                    Player p = (Player) sender;
                    StaticDecoList list = DecorationManager.getInstance().readStaticDecos();
                    ArrayList<StaticDecoList.StaticDeco> toPurge = new ArrayList<>();
                    list.getList().forEach(staticDeco -> {
                        if (staticDeco.getModelId().equals(args[1])) {
                            toPurge.add(staticDeco);
                        }
                    });
                    purgeConfirm.put(p.getUniqueId(), toPurge);
                    p.sendMessage("§7List:");
                    for (StaticDecoList.StaticDeco staticDeco : toPurge) {
                        sendDeco(p, staticDeco);
                    }
                    p.sendMessage("§c§lAre you sure you want to purge all theses decorations?");
                    p.sendMessage("§4/mdec confirmpurge yesImSure");
                }
                case "confirmpurge" -> {
                    Player p = (Player) sender;
                    if (!purgeConfirm.containsKey(p.getUniqueId())) {
                        p.sendMessage("§cNothing to purge, /mdec purge <modelId>");
                        return false;
                    }
                    if (!args[1].equals("yesImSure")) {
                        sender.sendMessage("§cWrong yesImSure !");
                        return false;
                    }
                    DecorationManager.getInstance().removeStaticDeco(purgeConfirm.remove(p.getUniqueId()));
                }
                case "teleport" -> {
                    Player p = (Player) sender;
                    p.teleport(new Location(Bukkit.getWorld(args[1]), Float.parseFloat(args[2]), Float.parseFloat(args[3]), Float.parseFloat(args[4])));
                }
            }
        } catch (Exception e) {
            sendHelp(sender);
        }
        return false;
    }

    private void sendDeco(Player p, StaticDecoList.StaticDeco st) {
        Location loc = SerializationMethods.deserializedLocation(st.getLocation());
        TextComponent textComponent = Component.text("§6§l" + st.getModelId() + " §8: §7" + st.getLocation())
                .hoverEvent(HoverEvent.showText(Component.text("Click to teleport")))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/mdec teleport " + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
        BukkitAudiences.builder(SpawnDecorationPlugin.getInstance()).build().player(p).sendMessage(textComponent);
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

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of("record", "reload", "deco", "copy", "cut", "paste", "purge", "list");
    }


}