package fr.supermax_8.spawndecoration.commands;

import com.github.retrooper.packetevents.util.MathUtil;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.assets.ItemModelData;
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import com.ticxo.modelengine.api.model.ModelRegistry;
import com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes;
import com.ticxo.modelengine.api.model.bone.behavior.BoneBehaviorType;
import com.ticxo.modelengine.api.utils.math.TMath;
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
import fr.supermax_8.spawndecoration.utils.PaginatedHotbarEditor;
import fr.supermax_8.spawndecoration.utils.SerializationMethods;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import revxrsal.commands.annotation.*;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.command.CommandActor;
import revxrsal.commands.node.ExecutionContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@CommandPermission("modelenginedecoration.use")
@Command({"modelenginedecoration", "mdec", "hendek"})
public class MegDecorationCommand {

    private final HashMap<UUID, List<StaticDecoList.StaticDeco>> purgeConfirm = new HashMap<>();
    private final HashMap<UUID, UUID> selectedMegDec = new HashMap<>();

    @CommandPlaceholder
    public void cmd(BukkitCommandActor actor) {
        sendHelp(actor.sender());
    }

    @Subcommand("select")
    public void select(Player p) {
        DecorationManager.getInstance().getStaticDecoMap().values().forEach(sd -> {
            sd.forEach(staticDecoration -> {
                getClosestDeco(p.getLocation());
            });
        });
    }

    @Subcommand({"setdefaultanimation", "setdanim"})
    public void setDefaultAnimation(Player p, @SuggestWith(ModelAnimation.class) String animation) {
        StaticDecoration closest = getClosestDeco(p.getLocation());
        if (closest == null) {
            p.sendMessage("§cThere is no close decoration !");
            return;
        }
        if (animation.equalsIgnoreCase("null")) animation = null;
        UUID uuid = closest.getUuid();
        String finalAnimation = animation;
        DecorationManager.getInstance().editStaticDecos(staticDecoList -> {
            for (StaticDecoList.StaticDeco deco : staticDecoList.getList()) {
                if (!deco.getId().equals(uuid))
                    continue;
                deco.setDefaultAnimation(finalAnimation);
                break;
            }
        });
    }

    public static final class ModelAnimation implements SuggestionProvider<BukkitCommandActor> {

        @Override
        public @NotNull List<String> getSuggestions(@NotNull ExecutionContext<BukkitCommandActor> context) {
            Player p = context.actor().asPlayer();
            StaticDecoration closest = getClosestDeco(p.getLocation());
            if (closest == null) {
                p.sendMessage("§cThere is no close decoration !");
                return List.of();
            }
            return new ArrayList<>(closest.getActiveModel().getBlueprint().getAnimations().keySet());
        }

    }

    private float roundFloat(float value, int precision) {
        float scale = (float) Math.pow(10, precision);
        return Math.round(value * scale) / scale;
    }

    private ItemStack itm(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    private void editdeco(Player editor, UUID uuid, Consumer<StaticDecoList.StaticDeco> edition) {
        DecorationManager.getInstance().editStaticDecos(staticDecoList -> {
            for (StaticDecoList.StaticDeco deco : staticDecoList.getList()) {
                if (!deco.getId().equals(uuid))
                    continue;
                edition.accept(deco);
                Location loc = deco.getBukkitLocation();
                Vector3f angle = new Vector3f();
                deco.getRotation().getEulerAnglesXYZ(angle);
                editor.sendMessage("Edit: ");
                editor.sendMessage("§8- §7Position XYZ: §6" + loc.getX() + " " + loc.getY() + " " + loc.getZ());
                editor.sendMessage("§8- §7Rotation XYZ: §6" + Math.toDegrees(angle.x) + " " + Math.toDegrees(angle.y) + " " + Math.toDegrees(angle.z));
                editor.sendMessage("§8- §7Scale: §6" + deco.getScale());
                editor.sendMessage("§8- §7BlockLight/SkyLight: §6" + deco.getBlockLight() + " " + deco.getSkyLight());
                break;
            }
        });
    }

    private void editmove(Player editor, UUID uuid, double dx, double dy, double dz) {
        editdeco(editor, uuid, deco -> {
            Location loc = deco.getBukkitLocation();
            loc.add(dx, dy, dz);
            deco.setBukkitLocation(loc);
        });
    }

    private void editrotate(Player editor, UUID uuid, double drx, double dry, double drz) {
        editdeco(editor, uuid, deco -> {
            deco.getRotation().rotateXYZ(
                    (float) Math.toRadians(drx),
                    (float) Math.toRadians(dry),
                    (float) Math.toRadians(drz)
            );
        });
    }

    @Subcommand({"moverotate", "mr"})
    public void moverotate(Player p) {
        StaticDecoration closest = getClosestDeco(p.getLocation());
        if (closest == null) {
            p.sendMessage("§cThere is no close decoration !");
            return;
        }
        UUID id = closest.getUuid();
        PaginatedHotbarEditor editor = new PaginatedHotbarEditor(p)
                .addItem(itm(Material.RED_WOOL, "MOVE X"), itm -> {
                    itm.click(e -> {
                        double delta = e.getAction().name().contains("LEFT") ? 0.1 : -0.1;
                        editmove(p, id, delta, 0, 0);
                    });
                })
                .addItem(itm(Material.ORANGE_WOOL, "MOVE Y"), itm -> {
                    itm.click(e -> {
                        double delta = e.getAction().name().contains("LEFT") ? 0.1 : -0.1;
                        editmove(p, id, 0, delta, 0);
                    });
                })
                .addItem(itm(Material.YELLOW_WOOL, "MOVE Z"), itm -> {
                    itm.click(e -> {
                        double delta = e.getAction().name().contains("LEFT") ? 0.1 : -0.1;
                        editmove(p, id, 0, 0, delta);
                    });
                })
                .addItem(itm(Material.MAGENTA_GLAZED_TERRACOTTA, "SCALE"), itm -> {
                    itm.click(e -> {
                        double delta = e.getAction().name().contains("LEFT") ? 0.1 : -0.1;
                        editdeco(p, id, deco -> {
                            deco.setScale(deco.getScale() + delta);
                        });
                    });
                })
                .addItem(itm(Material.PURPLE_WOOL, "ROTATE X"), itm -> {
                    itm.click(e -> {
                        double delta = e.getAction().name().contains("LEFT") ? 11.25 : -11.25;
                        editrotate(p, id, delta, 0, 0);
                    });
                })
                .addItem(itm(Material.MAGENTA_WOOL, "ROTATE Y"), itm -> {
                    itm.click(e -> {
                        double delta = e.getAction().name().contains("LEFT") ? 11.25 : -11.25;
                        editrotate(p, id, 0, delta, 0);
                    });
                })
                .addItem(itm(Material.PINK_WOOL, "ROTATE Z"), itm -> {
                    itm.click(e -> {
                        double delta = e.getAction().name().contains("LEFT") ? 11.25 : -11.25;
                        editrotate(p, id, 0, 0, delta);
                    });
                })
                .addItem(itm(Material.BLACK_WOOL, "RESET ROTATE"), itm -> {
                    itm.click(e -> {
                        editdeco(p, id, deco -> {
                            deco.getRotation().set(new Quaternionf());
                        });
                    });
                })
                .addItem(itm(Material.REDSTONE_LAMP, "BLOCK LIGHT"), itm -> {
                    itm.click(e -> {
                        int delta = e.getAction().name().contains("LEFT") ? 1 : -1;
                        editdeco(p, id, deco -> {
                            deco.setBlockLight(MathUtil.clamp(deco.getBlockLight() + delta, 0, 15));
                        });
                    });
                })
                .addItem(itm(Material.SEA_LANTERN, "SKY LIGHT"), itm -> {
                    itm.click(e -> {
                        int delta = e.getAction().name().contains("LEFT") ? 1 : -1;
                        editdeco(p, id, deco -> {
                            deco.setSkyLight(MathUtil.clamp(deco.getSkyLight() + delta, 0, 15));
                        });
                    });
                })
                .init();
    }

    private void editbonebone(Player editor, UUID decoId, String boneId, float dx, float dy, float dz, double dscale, float drx, float dry, float drz, boolean resetPosition, boolean resetRotate, int visible) {
        DecorationManager.getInstance().editStaticDecos(staticDecoList -> {
            for (StaticDecoList.StaticDeco deco : staticDecoList.getList()) {
                if (!deco.getId().equals(decoId))
                    continue;
                Map<String, StaticDecoList.StaticDeco.ModelTransformation> bones = deco.getBoneTransformations();
                if (bones == null) bones = new HashMap<>();
                StaticDecoList.StaticDeco.ModelTransformation transformation = bones.getOrDefault(boneId, new StaticDecoList.StaticDeco.ModelTransformation());
                Vector3f pos = transformation.getPosition();
                if (resetPosition)
                    pos.set(new Vector3f());
                else {
                    pos.add(dx, dy, dz);
                    pos.x = roundFloat(pos.x, 3);
                    pos.y = roundFloat(pos.y, 3);
                    pos.z = roundFloat(pos.z, 3);
                }
                transformation.setScale(roundFloat((float) (transformation.getScale() + dscale), 3));
                Quaternionf transRot = transformation.getRotation();
                if (resetRotate)
                    transRot.set(new Quaternionf());
                else {
                    transRot.rotateXYZ(
                            (float) Math.toRadians(drx),
                            (float) Math.toRadians(dry),
                            (float) Math.toRadians(drz)
                    );
                    transRot.x = roundFloat(transRot.x, 5);
                    transRot.y = roundFloat(transRot.y, 5);
                    transRot.z = roundFloat(transRot.z, 5);
                    transRot.w = roundFloat(transRot.w, 5);
                }
                if (visible != 0)
                    transformation.setVisible(visible == 2);

                Vector3f angle = new Vector3f();
                transformation.getRotation().getEulerAnglesXYZ(angle);
                editor.sendMessage("Edit Bone: §e§l" + boneId);
                editor.sendMessage("§8- §7Position XYZ: §6" + pos.x + " " + pos.y + " " + pos.z);
                editor.sendMessage("§8- §7Rotation XYZ: §6" + Math.toDegrees(angle.x) + " " + Math.toDegrees(angle.y) + " " + Math.toDegrees(angle.z));
                editor.sendMessage("§8- §7Scale: §6" + transformation.getScale());
                editor.sendMessage("§8- §7Visible: §6" + transformation.isVisible());
                bones.put(boneId, transformation);
                deco.setBoneTransformations(bones);
                break;
            }
        });
    }

    private void editBone(Player p, StaticDecoration staticDecoration, UUID decoId, String boneId) {
        new PaginatedHotbarEditor(p)
                .addItem(itm(Material.RED_WOOL, "MOVE DIRECTION"), itm -> {
                    itm.leftClick(() -> {
                        Vector direction = p.getLocation().getDirection();
                        Vector3f moveVec = new Vector3f((float) direction.getX(), (float) direction.getY(), (float) direction.getZ());
                        float yaw = staticDecoration.getLocation().getYaw();
                        Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(TMath.wrapDegree(yaw + 180)));
                        rotation.transform(moveVec);

                        float dx = Math.round(moveVec.x()) * 0.1f;
                        float dy = Math.round(moveVec.y()) * 0.1f;
                        float dz = Math.round(moveVec.z()) * 0.1f;

                        editbonebone(p, decoId, boneId, dx, dy, dz, 0, 0, 0, 0, false, false, 0);
                    });
                    itm.rightClick(() -> {
                        Vector direction = p.getLocation().getDirection();
                        Vector3f moveVec = new Vector3f((float) direction.getX(), (float) direction.getY(), (float) direction.getZ());
                        float yaw = staticDecoration.getLocation().getYaw();
                        Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(TMath.wrapDegree(yaw)));
                        rotation.transform(moveVec);

                        float dx = Math.round(moveVec.x()) * 0.1f;
                        float dy = Math.round(moveVec.y()) * 0.1f;
                        float dz = Math.round(moveVec.z()) * 0.1f;

                        editbonebone(p, decoId, boneId, dx, dy, dz, 0, 0, 0, 0, false, false, 0);
                    });
                })
                .addItem(itm(Material.GRAY_WOOL, "RESET POS"), itm -> {
                    itm.leftClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0, 0, 0, 0, true, false, 0);
                    }).rightClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0, 0, 0, 0, true, false, 0);
                    });
                })
                .addItem(itm(Material.REDSTONE_LAMP, "IS VISIBLE"), itm -> {
                    itm.leftClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0, 0, 0, 0, false, false, 2);
                    }).rightClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0, 0, 0, 0, false, false, 1);
                    });
                })
                .addItem(itm(Material.MAGENTA_GLAZED_TERRACOTTA, "SCALE"), itm -> {
                    itm.leftClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0.1, 0, 0, 0, false, false, 0);
                    }).rightClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, -0.1, 0, 0, 0, false, false, 0);
                    });
                })
                .addItem(itm(Material.PURPLE_WOOL, "ROTATE X"), itm -> {
                    itm.leftClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0, 11.25f, 0, 0, false, false, 0);
                    }).rightClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0, -11.25f, 0, 0, false, false, 0);
                    });
                })
                .addItem(itm(Material.MAGENTA_WOOL, "ROTATE Y"), itm -> {
                    itm.leftClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0, 0, 11.25f, 0, false, false, 0);
                    }).rightClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0, 0, -11.25f, 0, false, false, 0);
                    });
                })
                .addItem(itm(Material.PINK_WOOL, "ROTATE Z"), itm -> {
                    itm.leftClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0, 0, 0, 11.25f, false, false, 0);
                    }).rightClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0, 0, 0, -11.25f, false, false, 0);
                    });
                })
                .addItem(itm(Material.BLACK_WOOL, "RESET ROTATE"), itm -> {
                    itm.leftClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0, 0, 0, 0, false, true, 0);
                    }).rightClick(() -> {
                        editbonebone(p, decoId, boneId, 0, 0, 0, 0, 0, 0, 0, false, true, 0);
                    });
                })
                .init();
    }

    @Subcommand({"editbone"})
    public void boneTransformations(Player p) {
        StaticDecoration closest = getClosestDeco(p.getLocation());
        if (closest == null) {
            p.sendMessage("§cThere is no close decoration !");
            return;
        }
        ModelBlueprint blueprint = ModelEngineAPI.getBlueprint(closest.getModelId());
        PaginatedGui paginatedGui = Gui.paginated()
                .title(Component.text("Decorations"))
                .rows(6)
                .pageSize(45)
                .create();
        blueprintBonesForeach(blueprint.getBones(), (s, b) -> {
            paginatedGui.addItem(ItemBuilder.from(Material.BONE).name(Component.text(s)).asGuiItem(click -> {
                click.setCancelled(true);
                p.closeInventory();
                editBone(p, closest, closest.getUuid(), s);
                p.sendMessage(s);
            }));
        });
        paginatedGui.open(p);
    }


    private void blueprintBonesForeach(Map<String, BlueprintBone> bones, BiConsumer<String, BlueprintBone> cons) {
        bones.forEach((s, b) -> {
            cons.accept(s, b);
            blueprintBonesForeach(b.getChildren(), cons);
        });
    }

    @CommandPermission("mdec.record")
    @Subcommand("record")
    public void record(Player p, String record) {
        RecordLocationManager.startRecord(p, record);
    }

    @CommandPermission("mdec.reload")
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
        deco(p, 0, 0);
    }

    @Subcommand("deco")
    public void deco(Player p, int xspace, int yspace) {
        PaginatedGui paginatedGui = Gui.paginated()
                .title(Component.text("Decorations"))
                .rows(6)
                .pageSize(45)
                .create();

        ModelRegistry registry = ModelEngineAPI.getAPI().getModelRegistry();
        List<GuiItem> stacks = new ArrayList<>();
        ArrayList<String> list = new ArrayList<>(registry.getKeys());
        list.sort((s1, s2) -> {
            s1 = s1.replace("d_", "").replace("deco_", "");
            s2 = s2.replace("d_", "").replace("deco_", "");
            return s1.compareToIgnoreCase(s2);
        });
        int count = 0;
        for (String name : list) {
            if (name.startsWith("d_") || name.contains("deco")) {
                if (count >= 9) {
                    count = 0;
                    for (int i = 0; i < yspace; i++)
                        for (int j = 0; j < 8; j++)
                            stacks.add(new GuiItem(Material.AIR));
                }
                ModelBlueprint blueprint = registry.get(name);

                ItemModelData modelData = getRelevantDataFromBlueprint(blueprint);
                ItemStack stack = createItem(modelData);
                GuiItem itm = ItemBuilder.from(stack)
                        .setNbt("megdecoration_modelid", name)
                        .flags(ItemFlag.values())
                        .name(Component.text(blueprint.getName()))
                        .asGuiItem(event -> event.setCancelled(false));
                stacks.add(itm);
                count++;
                for (int i = 0; i < xspace; i++) {
                    stacks.add(new GuiItem(Material.AIR));
                    count++;
                }
            }
        }

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

    @CommandPermission("mdec.purge")
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

    @CommandPermission("mdec.purge")
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

    @CommandPermission("mdec.teleport")
    @Subcommand("teleport")
    public void teleport(Player p, World world, double x, double y, double z) {
        p.teleport(new Location(world, x, y, z));
    }

    @CommandPermission("mdec.text")
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

    @CommandPermission("mdec.text")
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
                if (!deco.getBukkitLocation().equals(closest.getLocation()))
                    continue;
                if (deco.getTexts() == null) deco.setTexts(new HashMap<>());
                deco.getTexts().computeIfAbsent(textId, k -> new ArrayList<>()).add(text);
                break;
            }
        });
        p.sendMessage("§6New line added: §r" + text);
    }

    @CommandPermission("mdec.text")
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
                if (!deco.getBukkitLocation().equals(closest.getLocation()))
                    continue;
                if (deco.getTexts() == null) deco.setTexts(new HashMap<>());
                deco.getTexts().remove(textId);
                break;
            }
        });
        p.sendMessage("§6Lines cleared for text §r" + textId);
    }

    private static StaticDecoration getClosestDeco(Location loc) {
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
        Location loc = st.getBukkitLocation();
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

    private ItemModelData getRelevantDataFromBlueprint(ModelBlueprint blueprint) {
        if (SpawnDecorationConfig.isMegcombined()) {
            ItemModelData modelData = new ItemModelData();
            modelData.setSingleComposite(new ItemModelData.SingleComposite(NamespacedKey.fromString("modelengine:" + blueprint.getName() + "/megcombined")));
            return modelData;
        }

        AtomicReference<BlueprintBone> head = new AtomicReference<>(null);
        List<BlueprintBone> bones = blueprint.getBones().values().stream()
                .filter(bb -> {
                            Map<BoneBehaviorType<?>, BoneBehaviorType.CachedProvider<?>> types = bb.getCachedBehaviorProvider();
                            if (bb.isRenderByDefault() && types.containsKey(BoneBehaviorTypes.HEAD) && types.containsKey(BoneBehaviorTypes.ITEM)) {
                                head.set(bb);
                                return false;
                            }
                            return true;
                        }
                ).toList();
        ItemModelData modelData = new ItemModelData();
        if (head.get() != null) {
            modelData = head.get().getModelData();
        } else if (!bones.isEmpty()) {
            for (BlueprintBone bone : bones) {
                if (bone.isRenderByDefault())
                    return bone.getModelData();
            }
        }

        return modelData;
    }

    private ItemStack createItem(ItemModelData imd) {
        ItemStack stack = new ItemStack(Material.BONE);
        try {
            NamespacedKey itemModel;
            if (imd.getSingleComposite() != null) {
                itemModel = imd.getSingleComposite().model();
            } else {
                //System.out.println("" + imd.getMultiModels().getSubModels().stream().findFirst().get().getId());
                itemModel = NamespacedKey.fromString(imd.getMultiModels().getSubModels().stream().findFirst().get().getId());
            }
            ItemMeta meta = stack.getItemMeta();
            meta.setItemModel(itemModel);
            stack.setItemMeta(meta);
        } catch (Exception e) {
        }
        return stack;
    }

}