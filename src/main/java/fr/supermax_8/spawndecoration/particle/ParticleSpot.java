package fr.supermax_8.spawndecoration.particle;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.particles.XParticle;
import fr.supermax_8.spawndecoration.utils.EquationParser;
import org.bukkit.Location;

import java.util.function.Supplier;

public class ParticleSpot {

    private final EquationParser parser = new EquationParser();
    private final Supplier<Location> getLocation;
    private XParticle particle;
    private ParticleDisplay.ParticleData data;
    private int from = 0;
    private int to = 1;
    private int t = 0;

    public ParticleSpot(String spot, Supplier<Location> getLocation) {
        this.getLocation = getLocation;
        try {
            String[] split = spot.split("__");

            String particleName;
            String particlesplit = split[0];
            if (particlesplit.contains(";")) {
                String[] particleSplit = particlesplit.split(";", 2);
                particleName = particleSplit[0];
                String[] dataSplit = particleSplit[1].split("=", 2);
                switch (dataSplit[0].toLowerCase()) {
                    case "item" ->
                            data = new ParticleDisplay.ParticleItemData(XMaterial.valueOf(dataSplit[1].toUpperCase()).parseItem());
                    case "color" -> {
                        String[] color = dataSplit[1].split(";", 3);
                        data = new ParticleDisplay.RGBParticleColor(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2]));
                    }
                }
            } else particleName = particlesplit;

            particle = XParticle.of(particleName.toUpperCase()).get();

            String[] expressions = split[1].split(";");

            for (String expression : expressions) {
                String[] parts = expression.split("=");
                if (expression.startsWith("fori")) {
                    String[] fori = parts[1].split(",");
                    from = Integer.parseInt(fori[0]);
                    to = Integer.parseInt(fori[1]);
                    continue;
                }
                parser.addEquation(parts[0], parts[1]);
            }

            /*System.out.println("Loaded particle spot");
            System.out.println("--Particle " + particle.name());
            System.out.println("--from " + from);
            System.out.println("--to " + to);
            System.out.println("--data " + data);*/
        } catch (Exception e) {
            System.out.println("ERROR WHILE LOAD PARTICLE SPOT: " + spot);
            e.printStackTrace();
        }
    }

    public void spawnParticle() {
        spawnParticle(getLocation.get());
    }

    public void spawnParticle(Location loc) {
        if (particle == null) return;

        if (t == 101) t = 0;
        for (int i = from; i < to; i++) {
            double deltaX = parser.evaluate("x", t, i);
            double deltaY = parser.evaluate("y", t, i);
            double deltaZ = parser.evaluate("z", t, i);

            double offsetX = parser.evaluate("offsetX", t, i);
            double offsetY = parser.evaluate("offsetY", t, i);
            double offsetZ = parser.evaluate("offsetZ", t, i);

            int count = (int) parser.evaluate("count", t, i);
            double extra = parser.evaluate("extra", t, i);

            Location spawnLoc = loc.clone().add(deltaX, deltaY, deltaZ);
            ParticleDisplay display = ParticleDisplay.of(particle)
                    .withCount(count)
                    .withExtra(extra)
                    .withLocation(spawnLoc)
                    .offset(offsetX, offsetY, offsetZ);
            if (data != null) display.withData(data);
            display.spawn();
        }
        t++;
    }


}