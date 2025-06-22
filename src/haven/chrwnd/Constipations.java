package haven.chrwnd;

import haven.CharWnd;
import haven.Coord;
import haven.GOut;
import haven.ItemInfo;
import haven.ItemSpec;
import haven.Listbox;
import haven.Loading;
import haven.OwnerContext;
import haven.PUtils;
import haven.ResData;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.UI;
import haven.Utils;
import haven.resutil.FoodInfo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static haven.PUtils.convolvedown;

public class Constipations extends Listbox<Constipations.El> {
    public static final Color hilit = new Color(255, 255, 0, 48);
    public static final Text.Foundry elf = CharWnd.attrf;
    public static final int elh = elf.height() + 2;
    public static final PUtils.Convolution tflt = new PUtils.Hanning(1);
    public static final Color buffed = new Color(160, 255, 160), full = new Color(250, 230, 64), none = new Color(250, 19, 43);
    public final List<El> els = new ArrayList<El>();
    private Integer[] order = {};

    public static Color color(double a) {
        return (a > 1.0) ? buffed : Utils.blendcol(none, full, a);
    }

    public class El {
        public final ResData t;
        public double a;
        private Tex tt, at;
        private BufferedImage tip;
        private boolean hl;

        public El(ResData t, double a) {
            this.t = t;
            this.a = a;
        }

        public void update(double a) {
            this.a = a;
            at = null;
        }

        public Tex tt() {
            if (tt == null) {
                ItemSpec spec = new ItemSpec(OwnerContext.uictx.curry(ui), t, null);
                BufferedImage img = spec.image();
                String nm = spec.name();
                TexI rnm = PUtils.strokeTex(elf.render(nm));
                BufferedImage buf = TexI.mkbuf(new Coord(elh + UI.scale(5) + rnm.sz().x, elh));
                Graphics g = buf.getGraphics();
                g.drawImage(convolvedown(img, new Coord(elh, elh), tflt), 0, 0, null);
                g.drawImage(rnm.back, elh + 5, ((elh - rnm.sz().y) / 2) + 1, null);
                g.dispose();
                tt = new TexI(buf);
            }
            return (tt);
        }

        public Tex at() {
            if (at == null) {
                Color c = (a > 1.0) ? buffed : Utils.blendcol(none, full, a);
                at = PUtils.strokeTex(elf.render(String.format("%d%%", Math.max((int) Math.round((1.0 - a) * 100), 1)), c));
            }
            return (at);
        }
    }

    private ItemInfo.InfoTip lasttip = null;

    public void draw(GOut g) {
        ItemInfo.InfoTip tip = null;
        if (ui.lasttip instanceof ItemInfo.InfoTip)
            tip = (ItemInfo.InfoTip) ui.lasttip;
        if (tip != lasttip) {
            for (El el : els)
                el.hl = false;
            FoodInfo finf;
            try {
                finf = (tip == null) ? null : ItemInfo.find(FoodInfo.class, tip.info());
            } catch (Loading l) {
                finf = null;
            }
            if (finf != null) {
                for (int i = 0; i < els.size(); i++) {
                    El el = els.get(i);
                    for (int o = 0; o < finf.types.length; o++) {
                        if (finf.types[o] == i) {
                            el.hl = true;
                            break;
                        }
                    }
                }
            }
            lasttip = tip;
        }
        super.draw(g);
    }


    public static final Comparator<El> ecmp = new Comparator<El>() {
        public int compare(El a, El b) {
            if (a.a < b.a)
                return (-1);
            else if (a.a > b.a)
                return (1);
            return (0);
        }
    };

    public Constipations(int w, int h) {
        super(w, h, elh);
    }

    protected void drawbg(GOut g) {
    }

    protected El listitem(int i) {
        Integer ii = order[i];
        return (ii == null ? null : els.get(ii));
    }

    protected int listitems() {
        return (order.length);
    }

    protected void drawitem(GOut g, El el, int idx) {
        g.chcolor(el.hl ? hilit : (((idx % 2) == 0) ? CharWnd.every : CharWnd.other));
        g.frect(Coord.z, g.sz);
        g.chcolor();
        try {
            g.image(el.tt(), Coord.z);
        } catch (Loading e) {
        }
        Tex at = el.at();
        g.image(at, new Coord(sz.x - at.sz().x - sb.sz.x, (elh - at.sz().y) / 2));
    }


    private void order() {
        int n = els.size();
        order = new Integer[n];
        for (int i = 0; i < n; i++)
            order[i] = i;
        Arrays.sort(order, new Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                return (ecmp.compare(els.get(a), els.get(b)));
            }
        });
    }

    public void update(ResData t, double a) {
        prev:
        {
            for (Iterator<El> i = els.iterator(); i.hasNext(); ) {
                El el = i.next();
                if (!Utils.eq(el.t, t))
                    continue;
                if (a == 1.0)
                    i.remove();
                else
                    el.update(a);
                break prev;
            }
            els.add(new El(t, a));
        }
        order();
    }


    protected void itemclick(El item, int button) {
    }
}
