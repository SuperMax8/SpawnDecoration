package fr.supermax_8.spawndecoration.commands;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import com.ticxo.modelengine.api.model.ModelRegistry;
import com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes;
import com.ticxo.modelengine.api.model.bone.behavior.BoneBehaviorType;
import com.ticxo.modelengine.api.utils.config.ConfigProperty;
import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import fr.supermax_8.spawndecoration.SpawnDecorationConfig;
import fr.supermax_8.spawndecoration.SpawnDecorationPlugin;
import fr.supermax_8.spawndecoration.manager.DecorationManager;
import fr.supermax_8.spawndecoration.manager.RecordLocationManager;
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

import java.util.List;
import java.util.Optional;

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
                    for (String name : registry.getKeys()) {
                        if (name.startsWith("d_") || name.contains("deco")) {
                            ModelBlueprint blueprint = registry.get(name);
                            Optional<BlueprintBone> opt = blueprint.getBones().values().stream().filter(bb -> bb.getCachedBehaviorProvider().containsKey(BoneBehaviorTypes.HEAD) ||
                                    bb.getCachedBehaviorProvider().containsKey(BoneBehaviorTypes.ITEM)
                            ).findFirst();
                            if (opt.isEmpty()) continue;
                            BlueprintBone bone = opt.get();
                            ItemStack stack = ConfigProperty.ITEM_MODEL.getBaseItem().create(Color.WHITE, bone.getDataId());
                            GuiItem itm = ItemBuilder.from(stack).setNbt("megdecoration_modelid", name).flags(ItemFlag.values()).name(Component.text(blueprint.getName())).lore(List.of()).asGuiItem(event -> {
                                event.setCancelled(false);
                            });
                            paginatedGui.addItem(itm);
                        }
                    }

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
            }
        } catch (Exception e) {
            sendHelp(sender);
        }
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(new String[]{
                "§8[§6ModelEngineDecoration§8]",
                "§7Version: " + SpawnDecorationPlugin.version,
                "§7Records: " + RecordLocationManager.records.size(),
                "§7TrackedDecorations: " + DecorationManager.trackedDecoMap.size(),
                "§7StaticDecorations: " + DecorationManager.staticDecoMap.size(),
                "§f- §7/megdecoration record <newRecordName>",
                "§f- §7/megdecoration reload"
        });
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of("record", "reload", "deco");
    }
}