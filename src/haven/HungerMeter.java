package haven;

import haven.chrwnd.GlutMeter;
import modification.configuration;

import java.awt.Color;

public class HungerMeter extends IMeter {
    private final GlutMeter glut;

    public HungerMeter(GlutMeter glut, final String name) {
        super(Resource.local().load("hud/meter/hungermeter"), name);
        this.glut = glut;
    }

    @Override
    protected void drawBg(GOut g) {
        if (glut.bg == null) return;
        boolean mini = configuration.minimalisticmeter;
        g.chcolor(glut.bg);
        if (!mini) {
            g.frect(off.mul(this.scale), msz.mul(this.scale));
        } else {
            Coord off = miniOff.mul(this.scale);
            g.frect(off, sz.sub(off.mul(2)));
        }
        g.chcolor();
    }

    @Override
    protected void drawMeters(GOut g) {
        boolean mini = configuration.minimalisticmeter;
        Color col = glut.fg;
        if (col == null) return;
        g.chcolor(col.darker());
        if (!mini) {
            g.frect(off.mul(this.scale), Coord.of((int) Math.round(msz.x * (glut.glut - Math.floor(glut.glut))), msz.y).mul(this.scale));
        } else {
            Coord off = miniOff.mul(this.scale);
            g.frect(off, sz.sub(off.mul(2)));
        }
        g.chcolor();
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        updatemeterinfo(String.format("%s‰:%s%%", Utils.odformat2(glut.lglut * 1000, 1), Math.round(glut.gmod * 100)));
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        return (glut.tooltip(c, prev));
    }
}