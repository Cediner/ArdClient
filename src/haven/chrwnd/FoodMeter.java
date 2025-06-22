package haven.chrwnd;

import haven.Coord;
import haven.GOut;
import haven.Indir;
import haven.ItemInfo;
import haven.Loading;
import haven.Message;
import haven.Resource;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.UI;
import haven.Utils;
import haven.Widget;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static haven.PUtils.imgblur;

public class FoodMeter extends Widget {
    public static final Tex frame = Resource.loadtex("gfx/hud/chr/foodm");
    public static final Coord marg = UI.scale(5, 5), trmg = UI.scale(10, 10);
    public double cap;
    public List<El> els = new LinkedList<El>();
    private List<El> enew = null, etr = null;
    private Indir<Resource> trev = null;
    private Tex trol;
    private double trtm = 0;

    @Resource.LayerName("foodev")
    public static class Event extends Resource.Layer {
        public final Color col;
        public final String nm;
        public final String orignm;
        public final int sort;

        public Event(Resource res, Message buf) {
            res.super();
            int ver = buf.uint8();
            if (ver == 1) {
                col = new Color(buf.uint8(), buf.uint8(), buf.uint8(), buf.uint8());
                orignm = buf.string();
                nm = Resource.getLocString(Resource.BUNDLE_TOOLTIP, res, orignm);
                sort = buf.int16();
            } else {
                throw (new Resource.LoadException("unknown foodev version: " + ver, res));
            }
        }

        public void init() {
        }
    }

    public static class El {
        public final Indir<Resource> res;
        public double a;

        public El(Indir<Resource> res, double a) {
            this.res = res;
            this.a = a;
        }

        private Event ev = null;

        public Event ev() {
            if (ev == null)
                ev = res.get().layer(Event.class);
            return (ev);
        }
    }

    public static final Comparator<El> dcmp = new Comparator<El>() {
        public int compare(El a, El b) {
            int c;
            if ((c = (a.ev().sort - b.ev().sort)) != 0)
                return (c);
            return (a.ev().nm.compareTo(b.ev().nm));
        }
    };

    public FoodMeter() {
        super(frame.sz());
    }

    private BufferedImage mktrol(List<El> els, Indir<Resource> trev) {
        BufferedImage buf = TexI.mkbuf(sz.add(trmg.mul(2)));
        Coord marg2 = marg.add(trmg);
        Graphics g = buf.getGraphics();
        double x = 0;
        int w = sz.x - (marg.x * 2);
        for (El el : els) {
            int l = (int) Math.floor((x / cap) * w);
            int r = (int) Math.floor(((x += el.a) / cap) * w);
            if (el.res == trev) {
                g.setColor(Utils.blendcol(el.ev().col, Color.WHITE, 0.5));
                g.fillRect(marg2.x - (trmg.x / 2) + l, marg2.y - (trmg.y / 2), r - l + trmg.x, sz.y - (marg.y * 2) + trmg.y);
            }
        }
        imgblur(buf.getRaster(), trmg.x, trmg.y);
        return (buf);
    }

    private void drawels(GOut g, List<El> els, int alpha) {
        double x = 0;
        int w = sz.x - (marg.x * 2);
        for (El el : els) {
            int l = (int) Math.floor((x / cap) * w);
            int r = (int) Math.floor(((x += el.a) / cap) * w);
            try {
                Color col = el.ev().col;
                g.chcolor(new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha));
                g.frect(new Coord(marg.x + l, marg.y), new Coord(r - l, sz.y - (marg.y * 2)));
            } catch (Loading e) {
            }
        }
    }

    public void tick(double dt) {
        if (enew != null) {
            try {
                Collections.sort(enew, dcmp);
                els = enew;
                rtip = null;
            } catch (Loading l) {
            }
            enew = null;
        }
        if (trev != null) {
            try {
                Collections.sort(etr, dcmp);
                if (ui.gui != null)
                    ui.gui.msg("You gained " + Loading.waitfor(trev).layer(Event.class).orignm, Color.WHITE);
                trol = new TexI(mktrol(etr, trev));
                trtm = Utils.rtime();
                trev = null;
            } catch (Loading l) {
            }
        }
    }

    public void draw(GOut g) {
        double d = (trtm > 0) ? (Utils.rtime() - trtm) : Double.POSITIVE_INFINITY;
        g.chcolor(0, 0, 0, 255);
        g.frect(marg, sz.sub(marg.mul(2)));
        drawels(g, els, 255);
        if (d < 1.0)
            drawels(g, etr, (int) (255 - (d * 255)));
        g.chcolor();
        g.image(frame, Coord.z);
        if (d < 2.5) {
            GOut g2 = g.reclipl(trmg.inv(), sz.add(trmg.mul(2)));
            g2.chcolor(255, 255, 255, (int) (255 - ((d * 255) * (1.0 / 2.5))));
            g2.image(trol, Coord.z);
        } else {
            trtm = 0;
        }
    }

    public void update(Object... args) {
        int n = 0;
        this.cap = (Float) args[n++];
        List<El> enew = new LinkedList<El>();
        while (n < args.length) {
            Indir<Resource> res = ui.sess.getres((Integer) args[n++]);
            double a = (Float) args[n++];
            enew.add(new El(res, a));
        }
        this.enew = enew;
    }

    public void trig(Indir<Resource> ev) {
        etr = (enew != null) ? enew : els;
        trev = ev;
    }

    private Tex rtip = null;

    public Object tooltip(Coord c, Widget prev) {
        if (rtip == null) {
            List<El> els = this.els;
            BufferedImage cur = null;
            double sum = 0.0;
            for (El el : els) {
                Event ev = el.res.get().layer(Event.class);
                Color col = Utils.blendcol(ev.col, Color.WHITE, 0.5);
                BufferedImage ln = Text.render(String.format("%s: %s", ev.nm, Utils.odformat2(el.a, 2)), col).img;
                Resource.Image icon = el.res.get().layer(Resource.imgc);
                if (icon != null)
                    ln = ItemInfo.catimgsh(5, icon.img, ln);
                cur = ItemInfo.catimgs(0, cur, ln);
                sum += el.a;
            }
            cur = ItemInfo.catimgs(0, cur, Text.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Total: %s/%s"), Utils.odformat2(sum, 2), Utils.odformat(cap, 2))).img);
            rtip = new TexI(cur);
        }
        return (rtip);
    }
}
