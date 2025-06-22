package haven.chrwnd;

import haven.Coord;
import haven.GOut;
import haven.Resource;
import haven.RichText;
import haven.Tex;
import haven.UI;
import haven.Widget;

import java.awt.Color;

public class GlutMeter extends Widget {
    public static final Tex frame = Resource.loadtex("gfx/hud/chr/glutm");
    public static final Coord marg = UI.scale(5, 5);
    public Color fg = Color.WHITE;
    public Color bg = Color.WHITE;
    public double glut, lglut, gmod;
    public String lbl;

    public GlutMeter() {
        super(frame.sz());
    }

    public void draw(GOut g) {
        Coord isz = sz.sub(marg.mul(2));
        g.chcolor(bg);
        g.frect(marg, isz);
        g.chcolor(fg);
        g.frect(marg, new Coord((int) Math.round(isz.x * (glut - Math.floor(glut))), isz.y));
        g.chcolor();
        g.image(frame, Coord.z);
    }

    public void update(Object... args) {
        int a = 0;
        this.glut = ((Number) args[a++]).doubleValue();
        this.lglut = ((Number) args[a++]).doubleValue();
        this.gmod = ((Number) args[a++]).doubleValue();
        this.lbl = (String) args[a++];
        this.bg = (Color) args[a++];
        this.fg = (Color) args[a++];
        rtip = null;
    }

    private Tex rtip = null;

    public Object tooltip(Coord c, Widget prev) {
        if (rtip == null) {
            rtip = RichText.render(String.format("%s: %.1f\u2030\nFood efficacy: %d%%", lbl, glut * 1000, Math.round(gmod * 100)), -1).tex();
        }
        return (rtip);
    }
}
