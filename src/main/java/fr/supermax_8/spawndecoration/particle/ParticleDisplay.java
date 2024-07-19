package fr.supermax_8.spawndecoration.particle;

//
// Source code recreated from a .class file by Vineflower
//


import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cryptomorin.xseries.particles.XParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParticleDisplay implements Cloneable {
    private static final boolean ISFLAT;
    private static final boolean SUPPORTS_ALPHA_COLORS;
    public static final Color[] NOTE_COLORS;
    @NotNull
    private static final XParticle DEFAULT_PARTICLE;
    public int count = 1;
    public double extra;
    public boolean force;
    @NotNull
    private XParticle particle;
    @Nullable
    private Location location;
    @Nullable
    private Location lastLocation;
    @NotNull
    private Vector offset;
    @Nullable
    private Vector particleDirection;
    @NotNull
    private Vector direction;
    @NotNull
    public List<List<ParticleDisplay.Rotation>> rotations;
    @Nullable
    private List<ParticleDisplay.Quaternion> cachedFinalRotationQuaternions;
    @Nullable
    private ParticleDisplay.ParticleData data;
    @Nullable
    private Consumer<ParticleDisplay.CalculationContext> preCalculation;
    @Nullable
    private Consumer<ParticleDisplay.CalculationContext> postCalculation;
    @Nullable
    private Function<Double, Double> onAdvance;
    @Nullable
    private Set<Player> players;

    public ParticleDisplay() {
        this.particle = DEFAULT_PARTICLE;
        this.offset = new Vector();
        this.direction = new Vector(0, 1, 0);
        this.rotations = new ArrayList<>();
    }

    @NotNull
    @Deprecated
    public static ParticleDisplay colored(@Nullable Location location, int r, int g, int b, float size) {
        return of(XParticle.DUST).withLocation(location).withColor((float)r, (float)g, (float)b, size);
    }

    @Nullable
    public Set<Player> getPlayers() {
        return this.players;
    }

    public ParticleDisplay onlyVisibleTo(Collection<Player> players) {
        if (players.isEmpty()) {
            return this;
        } else {
            if (this.players == null) {
                this.players = Collections.newSetFromMap(new WeakHashMap<>());
            }

            this.players.addAll(players);
            return this;
        }
    }

    public ParticleDisplay onlyVisibleTo(Player... players) {
        if (players.length == 0) {
            return this;
        } else {
            if (this.players == null) {
                this.players = Collections.newSetFromMap(new WeakHashMap<>());
            }

            Collections.addAll(this.players, players);
            return this;
        }
    }

    @NotNull
    @Deprecated
    public static ParticleDisplay colored(Location location, @NotNull Color color, float size) {
        return of(XParticle.DUST).withLocation(location).withColor(color, size);
    }

    @NotNull
    @Deprecated
    public static ParticleDisplay simple(@Nullable Location location, @NotNull Particle particle) {
        Objects.requireNonNull(particle, "Cannot build ParticleDisplay with null particle");
        ParticleDisplay display = new ParticleDisplay();
        display.particle = XParticle.of(particle);
        display.location = location;
        return display;
    }

    @NotNull
    @Deprecated
    public static ParticleDisplay of(@NotNull Particle particle) {
        return of(XParticle.of(particle));
    }

    @NotNull
    public static ParticleDisplay of(@NotNull XParticle particle) {
        ParticleDisplay display = new ParticleDisplay();
        display.particle = particle;
        return display;
    }

    @NotNull
    @Deprecated
    public static ParticleDisplay display(@NotNull Location location, @NotNull Particle particle) {
        Objects.requireNonNull(location, "Cannot display particle in null location");
        ParticleDisplay display = simple(location, particle);
        display.spawn();
        return display;
    }

    public static ParticleDisplay fromConfig(@NotNull ConfigurationSection config) {
        return edit(new ParticleDisplay(), config);
    }

    private static int toInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException var2) {
            return 0;
        }
    }

    private static double toDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException var2) {
            return 0.0;
        }
    }

    private static List<String> split(@NotNull String str, char separatorChar) {
        List<String> list = new ArrayList<>(5);
        boolean match = false;
        boolean lastMatch = false;
        int len = str.length();
        int start = 0;

        for (int i = 0; i < len; i++) {
            if (str.charAt(i) == separatorChar) {
                if (match) {
                    list.add(str.substring(start, i));
                    match = false;
                    lastMatch = true;
                }

                start = i + 1;
            } else {
                lastMatch = false;
                match = true;
            }
        }

        if (match || lastMatch) {
            list.add(str.substring(start, len));
        }

        return list;
    }

    @NotNull
    public static ParticleDisplay edit(@NotNull ParticleDisplay display, @NotNull ConfigurationSection config) {
        Objects.requireNonNull(display, "Cannot edit a null particle display");
        Objects.requireNonNull(config, "Cannot parse ParticleDisplay from a null config section");
        String particleName = config.getString("particle");
        Optional<XParticle> particle = particleName == null ? Optional.empty() : XParticle.of(particleName);
        particle.ifPresent(xParticle -> display.particle = xParticle);
        if (config.isSet("count")) {
            display.withCount(config.getInt("count"));
        }

        if (config.isSet("extra")) {
            display.withExtra(config.getDouble("extra"));
        }

        if (config.isSet("force")) {
            display.forceSpawn(config.getBoolean("force"));
        }

        String offset = config.getString("offset");
        if (offset != null) {
            List<String> offsets = split(offset.replace(" ", ""), ',');
            if (offsets.size() >= 3) {
                double offsetx = toDouble(offsets.get(0));
                double offsety = toDouble(offsets.get(1));
                double offsetz = toDouble(offsets.get(2));
                display.offset(offsetx, offsety, offsetz);
            } else {
                double masterOffset = toDouble(offsets.get(0));
                display.offset(masterOffset);
            }
        }

        String particleDirection = config.getString("direction");
        if (particleDirection != null) {
            List<String> directions = split(particleDirection.replace(" ", ""), ',');
            if (directions.size() >= 3) {
                double directionx = toDouble(directions.get(0));
                double directiony = toDouble(directions.get(1));
                double directionz = toDouble(directions.get(2));
                display.particleDirection(directionx, directiony, directionz);
            }
        }

        ConfigurationSection rotations = config.getConfigurationSection("rotations");
        if (rotations != null) {
            for (String rotationGroupName : rotations.getKeys(false)) {
                ConfigurationSection rotationGroup = rotations.getConfigurationSection(rotationGroupName);
                List<ParticleDisplay.Rotation> grouped = new ArrayList<>();

                for (String rotationName : rotationGroup.getKeys(false)) {
                    ConfigurationSection rotation = rotationGroup.getConfigurationSection(rotationName);
                    double angle = rotation.getDouble("angle");
                    String axisStr = rotation.getString("vector").toUpperCase(Locale.ENGLISH).replace(" ", "");
                    Vector axis;
                    if (axisStr.length() == 1) {
                        axis = ParticleDisplay.Axis.valueOf(axisStr).vector;
                    } else {
                        String[] split = axisStr.split(",");
                        axis = new Vector(
                                Math.toRadians(Double.parseDouble(split[0])),
                                Math.toRadians(Double.parseDouble(split[1])),
                                Math.toRadians(Double.parseDouble(split[2]))
                        );
                    }

                    grouped.add(ParticleDisplay.Rotation.of(angle, axis));
                }

                display.rotations.add(grouped);
            }
        }

        String color = config.getString("color");
        String blockdata = config.getString("blockdata");
        String item = config.getString("itemstack");
        String materialdata = config.getString("materialdata");
        double size;
        if (config.isSet("size")) {
            size = config.getDouble("size");
            display.extra = size;
        } else {
            size = 1.0;
        }

        if (color != null) {
            List<String> colors = split(color.replace(" ", ""), ',');
            if (colors.size() <= 3 || colors.size() == 6) {
                Color parsedColor1 = Color.white;
                Color parsedColor2 = null;
                if (colors.size() <= 2) {
                    try {
                        parsedColor1 = Color.decode(colors.get(0));
                        if (colors.size() == 2) {
                            parsedColor2 = Color.decode(colors.get(1));
                        }
                    } catch (NumberFormatException var19) {
                    }
                } else {
                    parsedColor1 = new Color(toInt(colors.get(0)), toInt(colors.get(1)), toInt(colors.get(2)));
                    if (colors.size() == 6) {
                        parsedColor2 = new Color(toInt(colors.get(3)), toInt(colors.get(4)), toInt(colors.get(5)));
                    }
                }

                if (parsedColor2 != null) {
                    display.data = new ParticleDisplay.DustTransitionParticleColor(parsedColor1, parsedColor2, size);
                } else {
                    display.data = new ParticleDisplay.RGBParticleColor(parsedColor1);
                }
            }
        } else if (blockdata != null) {
            Material material = Material.getMaterial(blockdata);
            if (material != null && material.isBlock()) {
                display.data = new ParticleDisplay.ParticleBlockData(material.createBlockData());
            }
        } else if (item != null) {
            Material material = Material.getMaterial(item);
            if (material != null && material.isItem()) {
                display.data = new ParticleDisplay.ParticleItemData(new ItemStack(material, 1));
            }
        } else if (materialdata != null) {
            Material material = Material.getMaterial(materialdata);
            if (material != null && material.isBlock()) {
                display.data = new ParticleDisplay.ParticleMaterialData(material.getNewData((byte)0));
            }
        }

        return display;
    }

    public static void serialize(ParticleDisplay display, ConfigurationSection section) {
        section.set("particle", display.particle.name());
        if (display.count != 1) {
            section.set("count", display.count);
        }

        if (display.extra != 0.0) {
            section.set("extra", display.extra);
        }

        if (display.force) {
            section.set("force", true);
        }

        if (!isZero(display.offset)) {
            Vector offset = display.offset;
            section.set("offset", offset.getX() + ", " + offset.getY() + ", " + offset.getZ());
        }

        if (display.particleDirection != null) {
            Vector direction = display.particleDirection;
            section.set("direction", direction.getX() + ", " + direction.getY() + ", " + direction.getZ());
        }

        if (!display.rotations.isEmpty()) {
            ConfigurationSection rotations = section.createSection("rotations");
            int index = 1;

            for (List<ParticleDisplay.Rotation> rotationGroup : display.rotations) {
                ConfigurationSection rotationGroupSection = rotations.createSection("group-" + index++);
                int groupIndex = 1;

                for (ParticleDisplay.Rotation rotation : rotationGroup) {
                    ConfigurationSection rotationSection = rotationGroupSection.createSection(String.valueOf(groupIndex++));
                    rotationSection.set("angle", rotation.angle);
                    Vector axis = rotation.axis;
                    Optional<ParticleDisplay.Axis> mainAxis = Arrays.stream(ParticleDisplay.Axis.values()).filter(x -> x.vector.equals(axis)).findFirst();
                    if (mainAxis.isPresent()) {
                        rotationSection.set("axis", mainAxis.get().name());
                    } else {
                        rotationSection.set("axis", axis.getX() + ", " + axis.getY() + ", " + axis.getZ());
                    }
                }
            }
        }

        if (display.data != null) {
            display.data.serialize(section);
        }
    }

    public static Vector rotateAround(@NotNull Vector location, @NotNull ParticleDisplay.Axis axis, @NotNull Vector rotation) {
        Objects.requireNonNull(axis, "Cannot rotate around null axis");
        Objects.requireNonNull(rotation, "Rotation vector cannot be null");
        switch (axis) {
            case X:
                return rotateAround(location, axis, rotation.getX());
            case Y:
                return rotateAround(location, axis, rotation.getY());
            case Z:
                return rotateAround(location, axis, rotation.getZ());
            default:
                throw new AssertionError("Unknown rotation axis: " + axis);
        }
    }

    public static Vector rotateAround(@NotNull Vector location, double x, double y, double z) {
        rotateAround(location, ParticleDisplay.Axis.X, x);
        rotateAround(location, ParticleDisplay.Axis.Y, y);
        rotateAround(location, ParticleDisplay.Axis.Z, z);
        return location;
    }

    public static Vector rotateAround(@NotNull Vector location, @NotNull ParticleDisplay.Axis axis, double angle) {
        Objects.requireNonNull(location, "Cannot rotate a null location");
        Objects.requireNonNull(axis, "Cannot rotate around null axis");
        if (angle == 0.0) {
            return location;
        } else {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            switch (axis) {
                case X: {
                    double y = location.getY() * cos - location.getZ() * sin;
                    double z = location.getY() * sin + location.getZ() * cos;
                    return location.setY(y).setZ(z);
                }
                case Y: {
                    double x = location.getX() * cos + location.getZ() * sin;
                    double z = location.getX() * -sin + location.getZ() * cos;
                    return location.setX(x).setZ(z);
                }
                case Z: {
                    double x = location.getX() * cos - location.getY() * sin;
                    double y = location.getX() * sin + location.getY() * cos;
                    return location.setX(x).setY(y);
                }
                default:
                    throw new AssertionError("Unknown rotation axis: " + axis);
            }
        }
    }

    public ParticleDisplay preCalculation(@Nullable Consumer<ParticleDisplay.CalculationContext> preCalculation) {
        this.preCalculation = preCalculation;
        return this;
    }

    public ParticleDisplay postCalculation(@Nullable Consumer<ParticleDisplay.CalculationContext> postCalculation) {
        this.postCalculation = postCalculation;
        return this;
    }

    public ParticleDisplay onAdvance(@Nullable Function<Double, Double> onAdvance) {
        this.onAdvance = onAdvance;
        return this;
    }

    public ParticleDisplay withParticle(@NotNull Particle particle) {
        return this.withParticle(XParticle.of(Objects.requireNonNull(particle, "Particle cannot be null")));
    }

    public ParticleDisplay withParticle(@NotNull XParticle particle) {
        this.particle = Objects.requireNonNull(particle, "Particle cannot be null");
        return this;
    }

    @NotNull
    public Vector getDirection() {
        return this.direction;
    }

    public void advanceInDirection(double distance) {
        Objects.requireNonNull(this.direction, "Cannot advance with null direction");
        if (distance != 0.0) {
            if (this.onAdvance != null) {
                distance = this.onAdvance.apply(distance);
            }

            this.location.add(this.direction.clone().multiply(distance));
        }
    }

    public ParticleDisplay withDirection(@Nullable Vector direction) {
        this.direction = direction.clone().normalize();
        return this;
    }

    @NotNull
    public XParticle getParticle() {
        return this.particle;
    }

    public int getCount() {
        return this.count;
    }

    public double getExtra() {
        return this.extra;
    }

    @Nullable
    public ParticleDisplay.ParticleData getData() {
        return this.data;
    }

    public ParticleDisplay withData(ParticleDisplay.ParticleData data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return "ParticleDisplay:[Particle="
                + this.particle
                + ", Count="
                + this.count
                + ", Offset:{"
                + this.offset.getX()
                + ", "
                + this.offset.getY()
                + ", "
                + this.offset.getZ()
                + "}, "
                + (
                this.location != null
                        ? "Location:{"
                        + this.location.getWorld().getName()
                        + this.location.getX()
                        + ", "
                        + this.location.getY()
                        + ", "
                        + this.location.getZ()
                        + "}, "
                        : ""
        )
                + "Rotation:"
                + this.rotations
                + ", Extra="
                + this.extra
                + ", Force="
                + this.force
                + ", Data="
                + this.data;
    }

    @NotNull
    public ParticleDisplay withCount(int count) {
        this.count = count;
        return this;
    }

    @NotNull
    public ParticleDisplay withExtra(double extra) {
        this.extra = extra;
        return this;
    }

    @NotNull
    public ParticleDisplay forceSpawn(boolean force) {
        this.force = force;
        return this;
    }

    @NotNull
    public ParticleDisplay withColor(@NotNull Color color, float size) {
        return this.withColor((float)color.getRed(), (float)color.getGreen(), (float)color.getBlue(), size);
    }

    @NotNull
    public ParticleDisplay withColor(@NotNull Color color) {
        return this.withColor(color, 1.0F);
    }

    @NotNull
    public ParticleDisplay withNoteColor(int color) {
        this.data = new ParticleDisplay.NoteParticleColor(color);
        return this;
    }

    @NotNull
    public ParticleDisplay withNoteColor(Note note) {
        return this.withNoteColor(note.getId());
    }

    @NotNull
    @Deprecated
    public ParticleDisplay withColor(float red, float green, float blue, float size) {
        this.data = new ParticleDisplay.RGBParticleColor((int)red, (int)green, (int)blue);
        this.extra = (double)size;
        return this;
    }

    @NotNull
    public ParticleDisplay withTransitionColor(@NotNull Color fromColor, float size, @NotNull Color toColor) {
        this.data = new ParticleDisplay.DustTransitionParticleColor(fromColor, toColor, (double)size);
        this.extra = (double)size;
        return this;
    }

    @NotNull
    @Deprecated
    public ParticleDisplay withTransitionColor(float red1, float green1, float blue1, float size, float red2, float green2, float blue2) {
        return this.withTransitionColor(new Color((int)red1, (int)green1, (int)blue1), size, new Color((int)red2, (int)green2, (int)blue2));
    }

    @NotNull
    public ParticleDisplay withBlock(@NotNull BlockData blockData) {
        this.data = new ParticleDisplay.ParticleBlockData(blockData);
        return this;
    }

    @NotNull
    public ParticleDisplay withBlock(@NotNull MaterialData materialData) {
        this.data = new ParticleDisplay.ParticleMaterialData(materialData);
        return this;
    }

    @NotNull
    public ParticleDisplay withItem(@NotNull ItemStack item) {
        this.data = new ParticleDisplay.ParticleItemData(item);
        return this;
    }

    @NotNull
    public Vector getOffset() {
        return this.offset;
    }

    @NotNull
    public Vector getParticleDirection() {
        return this.direction;
    }

    @NotNull
    public ParticleDisplay withEntity(@NotNull Entity entity) {
        return this.withLocationCaller(entity::getLocation);
    }

    @NotNull
    public ParticleDisplay withLocationCaller(@Nullable Callable<Location> locationCaller) {
        this.preCalculation = context -> {
            try {
                context.location = locationCaller.call();
            } catch (Exception var3) {
                throw new RuntimeException(var3);
            }
        };
        return this;
    }

    @Nullable
    public Location getLocation() {
        return this.location;
    }

    public ParticleDisplay withLocation(@Nullable Location location) {
        this.location = location;
        return this;
    }

    @NotNull
    public ParticleDisplay face(@NotNull Entity entity) {
        return this.face(Objects.requireNonNull(entity, "Cannot face null entity").getLocation());
    }

    @NotNull
    public ParticleDisplay face(@NotNull Location location) {
        Objects.requireNonNull(location, "Cannot face null location");
        this.rotate(
                ParticleDisplay.Rotation.of(Math.toRadians((double)location.getYaw()), ParticleDisplay.Axis.Y),
                ParticleDisplay.Rotation.of(Math.toRadians((double)(-location.getPitch())), ParticleDisplay.Axis.X)
        );
        this.direction = location.getDirection().clone().normalize();
        return this;
    }

    @Nullable
    public Location cloneLocation(double x, double y, double z) {
        return this.location == null ? null : cloneLocation(this.location).add(x, y, z);
    }

    @NotNull
    private static Location cloneLocation(@NotNull Location location) {
        return new Location(location.getWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    private static boolean isZero(@NotNull Vector vector) {
        return vector.getX() == 0.0 && vector.getY() == 0.0 && vector.getZ() == 0.0;
    }

    @NotNull
    public ParticleDisplay cloneWithLocation(double x, double y, double z) {
        ParticleDisplay display = this.clone();
        if (this.location == null) {
            return display;
        } else {
            display.location.add(x, y, z);
            return display;
        }
    }

    @NotNull
    public ParticleDisplay clone() {
        ParticleDisplay display = of(this.particle)
                .withDirection(this.direction)
                .withCount(this.count)
                .offset(this.offset.clone())
                .forceSpawn(this.force)
                .preCalculation(this.preCalculation)
                .postCalculation(this.postCalculation);
        if (this.location != null) {
            display.location = cloneLocation(this.location);
        }

        if (!this.rotations.isEmpty()) {
            display.rotations = new ArrayList<>(this.rotations);
        }

        display.data = this.data;
        return display;
    }

    public static Vector getPrincipalAxesRotation(Location location) {
        return getPrincipalAxesRotation(location.getPitch(), location.getYaw(), 0.0F);
    }

    public static Vector getPrincipalAxesRotation(float pitch, float yaw, float roll) {
        return new Vector(Math.toRadians((double)(pitch + 90.0F)), Math.toRadians((double)(-yaw)), (double)roll);
    }

    public static float[] getYawPitch(Vector vector) {
        double _2PI = Math.PI * 2;
        double x = vector.getX();
        double z = vector.getZ();
        float pitch;
        float yaw;
        if (x == 0.0 && z == 0.0) {
            yaw = 0.0F;
            pitch = vector.getY() > 0.0 ? -90.0F : 90.0F;
        } else {
            double theta = Math.atan2(-x, z);
            yaw = (float)Math.toDegrees((theta + (Math.PI * 2)) % (Math.PI * 2));
            double x2 = NumberConversions.square(x);
            double z2 = NumberConversions.square(z);
            double xz = Math.sqrt(x2 + z2);
            pitch = (float)Math.toDegrees(Math.atan(-vector.getY() / xz));
        }

        return new float[]{yaw, pitch};
    }

    @NotNull
    public List<ParticleDisplay.Quaternion> getRotation(boolean forceUpdate) {
        if (this.rotations.isEmpty()) {
            return new ArrayList<>();
        } else {
            if (forceUpdate) {
                this.cachedFinalRotationQuaternions = null;
            }

            if (this.cachedFinalRotationQuaternions == null) {
                this.cachedFinalRotationQuaternions = new ArrayList<>();

                for (List<ParticleDisplay.Rotation> rotationGroup : this.rotations) {
                    ParticleDisplay.Quaternion groupedQuat = null;

                    for (ParticleDisplay.Rotation rotation : rotationGroup) {
                        ParticleDisplay.Quaternion q = ParticleDisplay.Quaternion.rotation(rotation.angle, rotation.axis);
                        if (groupedQuat == null) {
                            groupedQuat = q;
                        } else {
                            groupedQuat = groupedQuat.mul(q);
                        }
                    }

                    this.cachedFinalRotationQuaternions.add(groupedQuat);
                }
            }

            return this.cachedFinalRotationQuaternions;
        }
    }

    @NotNull
    public ParticleDisplay rotate(double x, double y, double z) {
        return this.rotate(
                ParticleDisplay.Rotation.of(x, ParticleDisplay.Axis.X),
                ParticleDisplay.Rotation.of(y, ParticleDisplay.Axis.Y),
                ParticleDisplay.Rotation.of(z, ParticleDisplay.Axis.Z)
        );
    }

    public ParticleDisplay rotate(ParticleDisplay.Rotation... rotations) {
        Objects.requireNonNull(rotations, "Null rotations");
        if (rotations.length != 0) {
            List<ParticleDisplay.Rotation> finalRots = Arrays.stream(rotations).filter(x -> x.angle != 0.0).collect(Collectors.toList());
            if (!finalRots.isEmpty()) {
                this.rotations.add(finalRots);
                if (this.cachedFinalRotationQuaternions != null) {
                    this.cachedFinalRotationQuaternions.clear();
                }
            }
        }

        return this;
    }

    public ParticleDisplay rotate(ParticleDisplay.Rotation rotation) {
        Objects.requireNonNull(rotation, "Null rotation");
        if (rotation.angle != 0.0) {
            this.rotations.add(Collections.singletonList(rotation));
            if (this.cachedFinalRotationQuaternions != null) {
                this.cachedFinalRotationQuaternions.clear();
            }
        }

        return this;
    }

    @Nullable
    public Location getLastLocation() {
        return this.lastLocation == null ? this.getLocation() : this.lastLocation;
    }

    @Nullable
    public Location finalizeLocation(@Nullable Vector local) {
        ParticleDisplay.CalculationContext preContext = new ParticleDisplay.CalculationContext(this.location, local);
        if (this.preCalculation != null) {
            this.preCalculation.accept(preContext);
        }

        if (!preContext.shouldSpawn) {
            return null;
        } else {
            Location location = preContext.location;
            if (location == null) {
                throw new IllegalStateException("Attempting to spawn particle when no location is set");
            } else {
                local = preContext.local;
                if (local != null && !this.rotations.isEmpty()) {
                    for (ParticleDisplay.Quaternion grouped : this.getRotation(false)) {
                        local = ParticleDisplay.Quaternion.rotate(local, grouped);
                    }
                }

                location = cloneLocation(location);
                if (local != null) {
                    location.add(local);
                }

                ParticleDisplay.CalculationContext postContext = new ParticleDisplay.CalculationContext(location, local);
                if (this.postCalculation != null) {
                    this.postCalculation.accept(postContext);
                }

                return !postContext.shouldSpawn ? null : location;
            }
        }
    }

    @NotNull
    public ParticleDisplay offset(double x, double y, double z) {
        return this.offset(new Vector(x, y, z));
    }

    @NotNull
    public ParticleDisplay offset(@NotNull Vector offset) {
        this.offset = Objects.requireNonNull(offset, "Particle offset cannot be null");
        return this;
    }

    @NotNull
    public ParticleDisplay offset(double offset) {
        return this.offset(offset, offset, offset);
    }

    @NotNull
    public ParticleDisplay particleDirection(double x, double y, double z) {
        return this.particleDirection(new Vector(x, y, z));
    }

    @NotNull
    public ParticleDisplay particleDirection(@Nullable Vector particleDirection) {
        this.particleDirection = particleDirection;
        if (particleDirection != null && this.extra == 0.0) {
            this.extra = 1.0;
        }

        return this;
    }

    @NotNull
    public ParticleDisplay directional() {
        this.particleDirection = new Vector();
        return this;
    }

    public boolean isDirectional() {
        return this.particleDirection != null;
    }

    @Nullable
    public Location spawn() {
        return this.spawn(this.finalizeLocation(null));
    }

    @Nullable
    public Location spawn(@Nullable Vector local) {
        return this.spawn(this.finalizeLocation(local));
    }

    @Nullable
    public Location spawn(double x, double y, double z) {
        return this.spawn(this.finalizeLocation(new Vector(x, y, z)));
    }

    @Nullable
    public Location spawn(Location loc) {
        if (loc == null) {
            return null;
        } else {
            this.lastLocation = loc;
            Particle particle = this.particle.get();
            Objects.requireNonNull(particle, () -> "Cannot spawn unsupported particle: " + particle);

            Object data = null;
            if (this.data != null) {
                this.data = this.data.transform(this);
                Vector offsetData = this.data.offsetValues(this);
                if (offsetData != null) {
                    this.spawnWithDataInOffset(particle, loc, offsetData, null);
                    return loc;
                }

                data = this.data.data(this);
                if (!particle.getDataType().isInstance(data)) {
                    data = null;
                }
            }

            if (this.particleDirection != null) {
                this.spawnWithDataInOffset(particle, loc, this.particleDirection, data);
                return loc;
            } else {
                this.spawnRaw(particle, loc, this.count, this.offset, data);
                return loc;
            }
        }
    }

    private void spawnWithDataInOffset(Particle particle, Location loc, Vector offsetData, Object data) {
        if (isZero(this.offset) && this.count < 2) {
            this.spawnRaw(particle, loc, 0, offsetData, data);
        } else {
            double offsetx = this.offset.getX();
            double offsety = this.offset.getY();
            double offsetz = this.offset.getZ();
            ThreadLocalRandom r = ThreadLocalRandom.current();

            for (int i = 0; i < this.count; i++) {
                double dx = offsetx == 0.0 ? 0.0 : r.nextGaussian() * 4.0 * offsetx;
                double dy = offsety == 0.0 ? 0.0 : r.nextGaussian() * 4.0 * offsety;
                double dz = offsetz == 0.0 ? 0.0 : r.nextGaussian() * 4.0 * offsetz;
                Location offsetLoc = cloneLocation(loc).add(dx, dy, dz);
                this.spawnRaw(particle, offsetLoc, 0, offsetData, data);
            }
        }
    }

    private void spawnRaw(Particle particle, Location loc, int count, Vector offset, Object data) {
        double dx = offset.getX();
        double dy = offset.getY();
        double dz = offset.getZ();
        double extra = this.particle == XParticle.DUST ? 1.0 : this.extra;
        if (this.players == null) {
            if (ISFLAT) {
                loc.getWorld().spawnParticle(particle, loc, count, dx, dy, dz, extra, data, this.force);
            } else {
                loc.getWorld().spawnParticle(particle, loc, count, dx, dy, dz, extra, data);
            }
        } else {
            for (Player player : this.players) {
                player.spawnParticle(particle, loc, count, dx, dy, dz, extra, data);
            }
        }
    }

    public static int findNearestNoteColor(Color color) {
        double best = colorDistanceSquared(color, NOTE_COLORS[0]);
        int bestIndex = 0;

        for (int i = 1; i < NOTE_COLORS.length; i++) {
            double distance = colorDistanceSquared(color, NOTE_COLORS[i]);
            if (distance < best) {
                best = distance;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    public static double colorDistanceSquared(Color c1, Color c2) {
        int red1 = c1.getRed();
        int red2 = c2.getRed();
        int rmean = red1 + red2 >> 1;
        int r = red1 - red2;
        int g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();
        return (double)(((512 + rmean) * r * r >> 8) + 4 * g * g + ((767 - rmean) * b * b >> 8));
    }

    static {
        boolean isFlat;
        try {
            World.class
                    .getDeclaredMethod(
                            "spawnParticle",
                            Particle.class,
                            Location.class,
                            int.class,
                            double.class,
                            double.class,
                            double.class,
                            double.class,
                            Object.class,
                            boolean.class
                    );
            isFlat = true;
        } catch (NoSuchMethodException var4) {
            isFlat = false;
        }

        ISFLAT = isFlat;

        boolean supportsAlphaColors;
        try {
            org.bukkit.Color.fromARGB(0);
            supportsAlphaColors = true;
        } catch (NoSuchMethodError var3) {
            supportsAlphaColors = false;
        }

        SUPPORTS_ALPHA_COLORS = supportsAlphaColors;
        NOTE_COLORS = new Color[]{
                new Color(7853824),
                new Color(9814016),
                new Color(11707648),
                new Color(13403648),
                new Color(14836992),
                new Color(15941888),
                new Color(16522752),
                new Color(16646159),
                new Color(16187443),
                new Color(15204442),
                new Color(13566083),
                new Color(11403433),
                new Color(8782028),
                new Color(5964007),
                new Color(2949369),
                new Color(133886),
                new Color(14326),
                new Color(26848),
                new Color(39612),
                new Color(50829),
                new Color(59736),
                new Color(64545),
                new Color(2096128),
                new Color(5892096),
                new Color(9748736)
        };
        DEFAULT_PARTICLE = XParticle.FLAME;
    }

    public static enum Axis {
        X(new Vector(1, 0, 0)),
        Y(new Vector(0, 1, 0)),
        Z(new Vector(0, 0, 1));

        private final Vector vector;

        private Axis(Vector vector) {
            this.vector = vector;
        }

        public Vector getVector() {
            return this.vector;
        }
    }

    public final class CalculationContext {
        private Location location;
        private Vector local;
        private boolean shouldSpawn = true;

        public CalculationContext(Location location, Vector local) {
            this.location = location;
            this.local = local;
        }

        @Nullable
        public Location getLocation() {
            return this.location;
        }

        @Nullable
        public Vector getLocal() {
            return this.local;
        }

        public void setLocal(Vector local) {
            this.local = local;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public void dontSpawn() {
            this.shouldSpawn = false;
        }

        public ParticleDisplay getDisplay() {
            return ParticleDisplay.this;
        }
    }

    public static class DustTransitionParticleColor implements ParticleDisplay.ParticleData {
        private final Particle.DustTransition dustTransition;

        public DustTransitionParticleColor(Color fromColor, Color toColor, double size) {
            this.dustTransition = new Particle.DustTransition(
                    org.bukkit.Color.fromRGB(fromColor.getRed(), fromColor.getGreen(), fromColor.getBlue()),
                    org.bukkit.Color.fromRGB(toColor.getRed(), toColor.getGreen(), toColor.getBlue()),
                    (float)size
            );
        }

        @Override
        public Object data(ParticleDisplay display) {
            return this.dustTransition;
        }

        @Override
        public void serialize(ConfigurationSection section) {
            StringJoiner colorJoiner = new StringJoiner(", ");
            org.bukkit.Color fromColor = this.dustTransition.getColor();
            org.bukkit.Color toColor = this.dustTransition.getToColor();
            colorJoiner.add(Integer.toString(fromColor.getRed()));
            colorJoiner.add(Integer.toString(fromColor.getGreen()));
            colorJoiner.add(Integer.toString(fromColor.getBlue()));
            colorJoiner.add(Integer.toString(toColor.getRed()));
            colorJoiner.add(Integer.toString(toColor.getGreen()));
            colorJoiner.add(Integer.toString(toColor.getBlue()));
            section.set("color", colorJoiner.toString());
        }
    }

    public static class NoteParticleColor implements ParticleDisplay.ParticleData {
        private final int note;

        public NoteParticleColor(int note) {
            this.note = note;
        }

        public NoteParticleColor(Note note) {
            this(note.getId());
        }

        @Override
        public Vector offsetValues(ParticleDisplay display) {
            return new Vector((double)this.note / 24.0, 0.0, 0.0);
        }

        @Override
        public Object data(ParticleDisplay display) {
            return null;
        }

        @Override
        public void serialize(ConfigurationSection section) {
            section.set("color", this.note);
        }

        @Override
        public ParticleDisplay.ParticleData transform(ParticleDisplay display) {
            return (ParticleDisplay.ParticleData)(display.particle == XParticle.NOTE
                    ? this
                    : new ParticleDisplay.RGBParticleColor(ParticleDisplay.NOTE_COLORS[this.note]));
        }
    }

    public static class ParticleBlockData implements ParticleDisplay.ParticleData {
        private final BlockData blockData;

        public ParticleBlockData(BlockData blockData) {
            this.blockData = blockData;
        }

        @Override
        public Object data(ParticleDisplay display) {
            return this.blockData;
        }

        @Override
        public void serialize(ConfigurationSection section) {
            section.set("blockdata", this.blockData.getMaterial().name());
        }
    }

    public interface ParticleData {
        default Vector offsetValues(ParticleDisplay display) {
            return null;
        }

        Object data(ParticleDisplay var1);

        void serialize(ConfigurationSection var1);

        default ParticleDisplay.ParticleData transform(ParticleDisplay display) {
            return this;
        }
    }

    public static class ParticleItemData implements ParticleDisplay.ParticleData {
        private final ItemStack item;

        public ParticleItemData(ItemStack item) {
            this.item = item;
        }

        @Override
        public Object data(ParticleDisplay display) {
            return this.item;
        }

        @Override
        public void serialize(ConfigurationSection section) {
            section.set("itemstack", this.item.getType());
        }
    }

    public static class ParticleMaterialData implements ParticleDisplay.ParticleData {
        private final MaterialData materialData;

        public ParticleMaterialData(MaterialData materialData) {
            this.materialData = materialData;
        }

        @Override
        public Object data(ParticleDisplay display) {
            return this.materialData;
        }

        @Override
        public void serialize(ConfigurationSection section) {
            section.set("materialdata", this.materialData.getItemType().name());
        }
    }

    public static class Quaternion implements Cloneable {
        public final double w;
        public final double x;
        public final double y;
        public final double z;

        public Quaternion(double w, double x, double y, double z) {
            this.w = w;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public ParticleDisplay.Quaternion clone() {
            return new ParticleDisplay.Quaternion(this.w, this.x, this.y, this.z);
        }

        public static Vector rotate(Vector vector, ParticleDisplay.Quaternion rotation) {
            return rotation.mul(from(vector)).mul(rotation.inverse()).toVector();
        }

        public static Vector rotate(Vector vector, Vector axis, double deg) {
            return rotate(vector, rotation(deg, axis));
        }

        public static ParticleDisplay.Quaternion from(Vector vector) {
            return new ParticleDisplay.Quaternion(0.0, vector.getX(), vector.getY(), vector.getZ());
        }

        public static ParticleDisplay.Quaternion rotation(double degrees, Vector vector) {
            vector = vector.normalize();
            degrees /= 2.0;
            double sin = Math.sin(degrees);
            return new ParticleDisplay.Quaternion(Math.cos(degrees), vector.getX() * sin, vector.getY() * sin, vector.getZ() * sin);
        }

        public String getInverseString() {
            double rads = Math.acos(this.w);
            double deg = Math.toDegrees(rads) * 2.0;
            double sin = Math.sin(rads);
            Vector axis = new Vector(this.x / sin, this.y / sin, this.z / sin);
            return deg + ", " + axis.getX() + ", " + axis.getY() + ", " + axis.getZ();
        }

        public Vector toVector() {
            return new Vector(this.x, this.y, this.z);
        }

        public ParticleDisplay.Quaternion inverse() {
            double l = this.w * this.w + this.x * this.x + this.y * this.y + this.z * this.z;
            return new ParticleDisplay.Quaternion(this.w / l, -this.x / l, -this.y / l, -this.z / l);
        }

        public ParticleDisplay.Quaternion conjugate() {
            return new ParticleDisplay.Quaternion(this.w, -this.x, -this.y, -this.z);
        }

        public ParticleDisplay.Quaternion mul(ParticleDisplay.Quaternion r) {
            double n0 = r.w * this.w - r.x * this.x - r.y * this.y - r.z * this.z;
            double n1 = r.w * this.x + r.x * this.w + r.y * this.z - r.z * this.y;
            double n2 = r.w * this.y - r.x * this.z + r.y * this.w + r.z * this.x;
            double n3 = r.w * this.z + r.x * this.y - r.y * this.x + r.z * this.w;
            return new ParticleDisplay.Quaternion(n0, n1, n2, n3);
        }

        public Vector mul(Vector point) {
            double x = this.x * 2.0;
            double y = this.y * 2.0;
            double z = this.z * 2.0;
            double xx = this.x * x;
            double yy = this.y * y;
            double zz = this.z * z;
            double xy = this.x * y;
            double xz = this.x * z;
            double yz = this.y * z;
            double wx = this.w * x;
            double wy = this.w * y;
            double wz = this.w * z;
            double vx = (1.0 - (yy + zz)) * point.getX() + (xy - wz) * point.getY() + (xz + wy) * point.getZ();
            double vy = (xy + wz) * point.getX() + (1.0 - (xx + zz)) * point.getY() + (yz - wx) * point.getZ();
            double vz = (xz - wy) * point.getX() + (yz + wx) * point.getY() + (1.0 - (xx + yy)) * point.getZ();
            return new Vector(vx, vy, vz);
        }
    }

    public static class RGBParticleColor implements ParticleDisplay.ParticleData {
        private final Color color;

        public RGBParticleColor(Color color) {
            this.color = color;
        }

        public RGBParticleColor(int r, int g, int b) {
            this(new Color(r, g, b));
        }

        @Override
        public Vector offsetValues(ParticleDisplay display) {
            if (!ParticleDisplay.ISFLAT
                    || display.particle == XParticle.ENTITY_EFFECT && display.particle.isSupported() && display.particle.get().getDataType() == Void.class) {
                double red = this.color.getRed() == 0 ? Float.MIN_VALUE : (double)this.color.getRed() / 255.0;
                return new Vector(red, (double)this.color.getGreen() / 255.0, (double)this.color.getBlue() / 255.0);
            } else {
                return null;
            }
        }

        @Override
        public Object data(ParticleDisplay display) {
            if (display.particle == XParticle.DUST) {
                return new DustOptions(org.bukkit.Color.fromRGB(this.color.getRed(), this.color.getGreen(), this.color.getBlue()), (float)display.extra);
            } else if (display.particle == XParticle.DUST_COLOR_TRANSITION) {
                org.bukkit.Color color = org.bukkit.Color.fromRGB(this.color.getRed(), this.color.getGreen(), this.color.getBlue());
                return new Particle.DustTransition(color, color, (float)display.extra);
            } else {
                return ParticleDisplay.SUPPORTS_ALPHA_COLORS
                        ? org.bukkit.Color.fromARGB(this.color.getAlpha(), this.color.getRed(), this.color.getGreen(), this.color.getBlue())
                        : org.bukkit.Color.fromRGB(this.color.getRed(), this.color.getGreen(), this.color.getBlue());
            }
        }

        @Override
        public void serialize(ConfigurationSection section) {
            StringJoiner colorJoiner = new StringJoiner(", ");
            colorJoiner.add(Integer.toString(this.color.getRed()));
            colorJoiner.add(Integer.toString(this.color.getGreen()));
            colorJoiner.add(Integer.toString(this.color.getBlue()));
            section.set("color", colorJoiner.toString());
        }

        @Override
        public ParticleDisplay.ParticleData transform(ParticleDisplay display) {
            return (ParticleDisplay.ParticleData)(display.particle == XParticle.NOTE
                    ? new ParticleDisplay.NoteParticleColor(ParticleDisplay.findNearestNoteColor(this.color))
                    : this);
        }
    }

    public static class Rotation implements Cloneable {
        public double angle;
        public Vector axis;

        public Rotation(double angle, Vector axis) {
            this.angle = angle;
            this.axis = axis;
        }

        @Override
        public Object clone() {
            return new ParticleDisplay.Rotation(this.angle, this.axis.clone());
        }

        public static ParticleDisplay.Rotation of(double angle, Vector axis) {
            return new ParticleDisplay.Rotation(angle, axis);
        }

        public static ParticleDisplay.Rotation of(double angle, ParticleDisplay.Axis axis) {
            return new ParticleDisplay.Rotation(angle, axis.vector);
        }
    }
}
