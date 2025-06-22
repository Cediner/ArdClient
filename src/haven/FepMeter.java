package haven;

import haven.chrwnd.FoodMeter;
import modification.configuration;

import java.awt.Color;

public class FepMeter extends IMeter {
    private final FoodMeter food;

    public FepMeter(FoodMeter food, String name) {
        super(Resource.local().load("hud/meter/fepmeter"), name);
        this.food = food;
    }

    @Override
    protected void drawMeters(GOut g) {
        boolean mini = configuration.minimalisticmeter;
        double x = 0;
        int w = !mini ? IMeter.msz.x : sz.x;
        for (FoodMeter.El el : food.els) {
            int l = (int) Math.floor((x / food.cap) * w);
            int r = (int) Math.floor(((x += el.a) / food.cap) * w);
            try {
                Color col = el.ev().col;
                g.chcolor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 255));
                if (!mini) {
                    g.frect(off.add(l, 0).mul(this.scale), Coord.of(r - l, msz.y).mul(this.scale));
                } else {
                    Coord off = miniOff.mul(this.scale);
                    g.frect(Coord.of(l, 0).mul(this.scale).add(off), Coord.of((int) ((r - l) * this.scale), sz.y).sub(off.mul(2)));
                }
            } catch (Loading e) {}
        }
        g.chcolor();
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        double sum = food.els.stream().mapToDouble(el -> el.a).sum();
        updatemeterinfo(String.format("%s/%s", Utils.odformat2(sum, 1), Utils.odformat(food.cap, 1)));
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        return (food.tooltip(c, prev));
    }
}