package haven.sloth.gfx;

import haven.Buff;
import haven.Coord;
import haven.Fightview;
import haven.GOut;
import haven.Gob;
import haven.PUtils;
import haven.RenderList;
import haven.Resource;
import haven.RichText;
import haven.Sprite;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.UI;
import haven.Utils;
import haven.Widget;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GobCombatSprite extends Sprite {
    public static final int id = -244942;
    private Fightview.Relation rel;

    public GobCombatSprite(final Gob g, final Fightview.Relation relation) {
        super(g, null);
        this.rel = relation;
    }

    public void draw(GOut g) {
        if (rel != null) {
            final Gob gob = (Gob) owner;
            if (gob.sc == null) {
                return;
            }
            float scale = 0.8f;
//            final Coord c = new Coord(sc.add(sczu.mul(16))).sub(0, UI.scale(40));
            final Coord c1 = gob.sc.add(new Coord(gob.sczu.mul(15))).sub(0, UI.scale(25));
            final Coord c = c1.sub(0, UI.scale(25)).sub(0, Buff.scframe.sz().y * scale);
            final Coord bc = c.copy();
            final Coord sc = c.copy();

            //Draw Buffs
            int count = 0;
            for (Widget wdg = rel.buffs.child; wdg != null; wdg = wdg.next) {
                if (!(wdg instanceof Buff))
                    continue;
                //final Buff buf = (Buff) wdg;
                //Double ameter = (buf.ameter >= 0) ? Double.valueOf(buf.ameter / 100.0) : buf.ameteri.get();
                //if (ameter != null && buf.isOpening()) {
                count++;
                //}
            }
            bc.x -= (int) (((Buff.scframe.sz().x * scale) + 2) * count / 2);
            for (Widget wdg = rel.buffs.child; wdg != null; wdg = wdg.next) {
                if (!(wdg instanceof Buff))
                    continue;
                final Buff buf = (Buff) wdg;
                //Double ameter = (buf.ameter >= 0) ? Double.valueOf(buf.ameter / 100.0) : buf.ameteri.get();
                if (/*ameter != null && */buf.isOpening()) {
                    buf.fightdraw(g.reclip(bc.copy(), Buff.scframe.sz().mul(scale)), scale);
                } else {
                    buf.stancedraw(g.reclip(bc.copy(), Buff.scframe.sz().mul(scale)), scale);
                }
                bc.x += (int) (Buff.scframe.sz().x * scale) + 2;
            }

            /*for (Widget wdg = rel.buffs.child; wdg != null; wdg = wdg.next) {
                if (!(wdg instanceof Buff))
                    continue;
                final Buff buf = (Buff) wdg;
                if (!buf.isOpening()) {
                    buf.stancedraw(g.reclip(sc.copy().add(-(int) (Buff.scframe.sz().x * scale / 2.0), (int) (Buff.scframe.sz().y * scale)), Buff.scframe.sz()), scale);
                }
            }*/

            if (rel.lastact != null) {
                Coord lc = c.sub(0, Buff.scframe.sz().y * scale);

                double max = 10;
                try {
                    Resource lastres = rel.lastact.get();
                    if (lastres != null) {
                        double lastuse = rel.lastuse;
                        Double savedt = cooldowns.get(lastres.name);
                        if (savedt != null) {
                            int delay = 0;
                            if (lastres.name.contains("takeaim"))
                                delay = 1;
                            if (lastres.name.contains("think"))
                                delay = 2;
                            max = delay == 0 ? savedt : savedt * (1 + 0.2 * (rel.oip - delay));
                        }
                        double time = Utils.rtime() - lastuse;
                        boolean verylong = time > max;
                        Tex lasttex = texMap.computeIfAbsent(lastres.name + (verylong ? "_grey" : ""), t -> {
                            Resource.Image lastimg = lastres.layer(Resource.imgc);
                            BufferedImage bimg = verylong ? PUtils.monochromize(lastimg.img, Color.WHITE) : PUtils.copy(lastimg.img);
                            if (lastres.name.contains("cleave") && !verylong)
                                bimg = PUtils.rasterimg(PUtils.blurmask2(bimg.getRaster(), 2, 2, Color.RED));
                            else
                                bimg = PUtils.rasterimg(PUtils.blurmask2(bimg.getRaster(), 1, 1, Color.BLACK));
                            bimg = PUtils.convolvedown(bimg, Coord.of((int) (bimg.getWidth() * scale), (int) (bimg.getHeight() * scale)), new PUtils.Hanning(1));
                            return (new TexI(bimg));
                        });
                        g.aimage(lasttex, lc, 0.5, 0.5);
                        if (!verylong) {
                            Tex timetex = texMap.computeIfAbsent(String.format("%.1f", max - time), t -> Text.renderstroked(t, new Color(30, 30, 30), Color.WHITE, Text.num14boldFnd).tex());
                            g.aimage(timetex, lc, 0.5, 0.5);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            Tex render = texMap.computeIfAbsent(String.format("$bg[35,35,35,192]{$size[14]{$b{$col[0,255,0]{%d} : $col[255,0,0]{%d}}}}", rel.ip, rel.oip), t -> RichText.render(t, -1).tex());
            g.aimage(render, c1, 0.5, 1.01);
            g.chcolor();
        }
    }

    private String lastn;
    private double lastt;

    public final Map<String, Tex> texMap = new HashMap<>();
    public final Map<String, Double> cooldowns = new ConcurrentHashMap<>();

    public void update(final Fightview.Relation rel) {
        this.rel = rel;
    }

    public boolean setup(RenderList rl) {
        rl.prepo(last);
        return true;
    }

    @Override
    public boolean tick(int dt) {
        if (rel != null && rel.lastact != null) {
            try {
                Resource lastres = rel.lastact.get();
                if (lastres != null) {
                    double max = 10;
                    double lastuse = rel.lastuse;
                    if (!lastres.name.equals(lastn) || lastuse != lastt) {
                        double timem = Utils.rtime() - lastt;
                        if (lastn != null && timem < max) {
                            int delay = 0;
                            if (lastres.name.contains("takeaim"))
                                delay = 1;
                            if (lastres.name.contains("think"))
                                delay = 2;
                            timem = delay == 0 ? timem : timem / (1 + 0.2 * (rel.oip - delay));
                            final double finalTimem = timem;
                            cooldowns.compute(lastn, (s, d) -> d == null ? finalTimem : Math.min(d, finalTimem));
                        }
                        if (!lastres.name.equals(lastn))
                            lastn = lastres.name;
                        if (lastuse != lastt)
                            lastt = lastuse;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return rel == null;
    }

    public Object staticp() {
        return Gob.STATIC;
    }
}
