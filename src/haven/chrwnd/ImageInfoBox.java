package haven.chrwnd;

import haven.Coord;
import haven.GOut;
import haven.Indir;
import haven.Loading;
import haven.Scrollbar;
import haven.Tex;
import haven.UI;
import haven.Widget;

public class ImageInfoBox extends Widget {
    private Tex img;
    private Indir<Tex> loading;
    private final Scrollbar sb;

    public ImageInfoBox(Coord sz) {
        super(sz);
        sb = adda(new Scrollbar(sz.y, 0, 1), sz.x, 0, 1, 0);
    }

    public void drawbg(GOut g) {
        g.chcolor(0, 0, 0, 128);
        g.frect(Coord.z, sz);
        g.chcolor();
    }

    public Coord marg() {return (UI.scale(10, 10));}

    public void tick(double dt) {
        if (loading != null) {
            try {
                set(loading.get());
                loading = null;
            } catch (Loading l) {
            }
        }
        super.tick(dt);
    }

    public void draw(GOut g) {
        drawbg(g);
        if (img != null)
            g.image(img, marg().sub(0, sb.val));
        super.draw(g);
    }

    public void set(Tex img) {
        this.img = img;
        if (img != null) {
            sb.max = img.sz().y + (marg().y * 2) - sz.y;
            sb.val = 0;
        } else {
            sb.max = sb.val = 0;
        }
    }

    public void set(Indir<Tex> loading) {
        this.loading = loading;
    }

    public boolean mousewheel(Coord c, int amount) {
        sb.ch(amount * 20);
        return (true);
    }

    public void resize(Coord sz) {
        super.resize(sz);
        sb.c = new Coord(sz.x - sb.sz.x, 0);
        sb.resize(sz.y);
        set(img);
    }
}