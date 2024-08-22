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
import fr.supermax_8.spawndecoration.blueprint.StaticDecoration;
import fr.supermax_8.spawndecoration.manager.DecorationManager;
import fr.supermax_8.spawndecoration.manager.RecordLocationManager;
import fr.supermax_8.spawndecoration.manager.WEClipboardManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MegDecorationCommand implements CommandExecutor, TabCompleter {


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
            }
        } catch (Exception e) {
            sendHelp(sender);
        }
        return false;
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
        return List.of("record", "reload", "deco", "copy", "cut", "paste");
    }


}