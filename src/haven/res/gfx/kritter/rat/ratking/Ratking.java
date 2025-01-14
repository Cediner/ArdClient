package haven.res.gfx.kritter.rat.ratking;

import haven.Composited.MD;
import haven.Coord3f;
import haven.Drawable;
import haven.GLState;
import haven.Gob;
import haven.Indir;
import haven.Location;
import haven.Message;
import haven.MessageBuf;
import haven.Moving;
import haven.RenderList;
import haven.ResData;
import haven.Resource;
import haven.Skeleton;
import haven.Sprite;
import haven.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Ratking extends Drawable {
    public final Indir<Resource> tail = Resource.classres(Ratking.class).pool.load("gfx/kritter/rat/ratacle", 2);
    public final Random rnd = new Random();
    public final Sprite knot, crown;
    public final GLState croff;
    public List<Sprite> tails = null;
    public Rat[] rats = {};
    public Double mva = null;
    public List<Double> tgta = Collections.emptyList();
    public int state = 0;

    public Ratking(Gob gob) {
        super(gob);
        knot = Sprite.create(gob, Resource.classres(Ratking.class).pool.load("gfx/kritter/rat/tailknot", 2).get(), Message.nil);
        crown = Sprite.create(gob, Resource.classres(Ratking.class).pool.load("gfx/terobjs/items/ratcrown", 1).get(), Message.nil);
        croff = crown.res.flayer(Skeleton.BoneOffset.class, "rk").from(gob);
    }

    public Resource getres() {return (Resource.classres(Ratking.class));}

    private void assignmva(double a) {
        int n = 0;
        Rat[] p = new Rat[rats.length];
        for (Rat rat : rats) {
            if (rat != null)
                p[n++] = rat;
        }
	/*
	int minr = (n > 1) ? (int)Math.ceil(n / 5.0) : 1;
	int maxr = Math.max(minr, (int)Math.floor(n / 2.0));
	int r = minr + Math.round(rnd.nextFloat() * (maxr - minr));
	*/
        int minp = (n > 1) ? (int) Math.ceil(n / 3.0) : 1;
        int maxp = Math.max(minp, (int) Math.floor(n / 2.0));
        int r = n - (minp + Math.round(rnd.nextFloat() * (maxp - minp)));
        Arrays.sort(p, Comparator.comparing(rat -> Math.abs(Utils.cangle(rat.a - a))));
        Arrays.sort(p, 0, n - r, Comparator.comparing(rat -> Utils.cangle(rat.a - a)));
        Arrays.sort(p, n - r, n, Comparator.comparing(rat -> Utils.cangle(rat.a - a + Math.PI)));
        for (int i = 0, o = 0; i < n - r; i++, o++) {
            p[o].ta = Utils.cangle(a + (((i * 1.0) - ((n - r - 1) * 0.5)) * Rat.ao));
            p[o].npose = Rat.p_walking;
            p[o].state = 1;
        }
        for (int i = 0, o = n - r; i < r; i++, o++) {
            p[o].ta = Utils.cangle(a + Math.PI + (((i * 1.0) - ((r - 1) * 0.5)) * Rat.ao));
            p[o].npose = Rat.p_resist;
            p[o].state = 2;
        }
    }

    private void assignidle() {
        int n = 0;
        Rat[] p = new Rat[rats.length];
        for (Rat rat : rats) {
            if (rat != null)
                p[n++] = rat;
        }
        Arrays.sort(p, Comparator.comparing(rat -> Utils.cangle(rat.a)));
        if (tgta.size() > 0) {
            int rpt = Math.min((int) Math.ceil(n / tgta.size()), 4);
            int[] tc = new int[tgta.size()];
            for (int i = 0; i < tgta.size(); i++) {
                double mina = 0;
                int minr = -1;
                for (int o = 0; o < n; o++) {
                    double ad = Math.abs(Utils.cangle(tgta.get(i) - p[o].a));
                    if ((minr < 0) || (ad < mina)) {
                        mina = ad;
                        minr = o;
                    }
                }
                for (int o = (minr - (rpt / 2) + p.length) % p.length, u = 0; u < rpt; o = (o + 1) % p.length, u++) {
                    // p[i].ta = tgta.get(i);
                }
            }
        } else {
            for (int i = 0; i < n; i++) {
                p[i].ta = Utils.cangle(Utils.clip(p[i].a + (rnd.nextDouble() - 0.5) * 1.0,
                        p[i].a + Utils.cangle(p[(i + n - 1) % n].a + Rat.ao - p[i].a),
                        p[i].a + Utils.cangle(p[(i + 1) % n].a - Rat.ao - p[i].a)));
            }
        }
        for (Rat rat : rats) {
            if (rat != null) {
                rat.npose = Rat.p_fidle;
                rat.state = 0;
            }
        }
    }

    public void ctick(double dt) {
        knot.tick(dt);
        crown.tick(dt);
        if (tails != null) {
            for (Sprite spr : tails)
                spr.tick(dt);
        }
        if (state == 0) {
            Moving mv = gob.getattr(Moving.class);
            double v = (mv == null) ? 0 : mv.getv();
            if (v > 0) {
                if ((mva == null) || (mva != gob.a)) {
                    assignmva(gob.a);
                    mva = gob.a;
                }
            } else {
                if (mva != null)
                    assignidle();
                mva = null;
            }
        }
        for (Rat rat : rats) {
            if (rat != null)
                rat.tick(dt);
        }
    }

    private void parts(RenderList slot) {
        slot.add(knot, null);
        slot.add(croff.apply(crown), null);
        for (int i = 0; i < tails.size(); i++)
            slot.add(GLState.compose(Location.rot(Coord3f.zu, ((float) i / (float) tails.size()) * 2 * (float) Math.PI),
                            Location.rot(Coord3f.yu, -0.1f + (rnd.nextFloat() * 0.6f)),
                            Location.xlate(Coord3f.of(3, 0, 7)),
                            Location.scale(0.5f + (rnd.nextFloat() * 1.0f)))
                    .apply(tails.get(i)), null);
        for (Rat rat : rats) {
            if (rat != null)
                slot.add(rat, null);
        }
    }

    @Override
    public void setup(final RenderList rl) {
        if (tails == null) {
            tails = new ArrayList<>();
            for (int i = 0; i < rats.length; i++) {
                if (rats[i] != null) {
                    Sprite spr = Sprite.create(gob, tail.get(), new MessageBuf(new byte[]{(byte) ((state == 0) ? 1 : 2)}));
                    spr.age();
                    tails.add(spr);
                }
            }
        }

        rl.prepo(Location.goback("gobx"));
        parts(rl);
    }

    public double randang(Rat[] cur) {
        int n = 0;
        double[] a = new double[cur.length];
        for (int i = 0; i < cur.length; i++) {
            if (cur[i] != null)
                a[n++] = Utils.floormod(cur[i].a, 2 * Math.PI);
        }
        if (n == 0)
            return (rnd.nextDouble() * 2 * Math.PI);
        Arrays.sort(a, 0, n);
        double[] d = new double[n];
        for (int i = 0; i < n - 1; i++)
            d[i] = a[i + 1] - a[i];
        d[n - 1] = a[0] + (2 * Math.PI) - a[n - 1];
        double md = 0;
        int mi = 0;
        for (int i = 0; i < n; i++) {
            if (d[i] > md)
                md = d[mi = i];
        }
        return (a[mi] + (d[mi] * (0.25 + (rnd.nextDouble() * 0.5))));
    }

    private void update(Message msg) {
        List<Indir<Resource>> rats = new ArrayList<Indir<Resource>>();
        while (!msg.eom()) {
            int id = msg.uint16();
            if (id == 0xffff)
                rats.add(null);
            else
                rats.add(gob.glob.sess.getres(id));
        }
        Rat[] nrats = new Rat[rats.size()];
        for (int i = 0; i < nrats.length; i++) {
            Indir<Resource> res = rats.get(i);
            Rat crat = (i < this.rats.length) ? this.rats[i] : null;
            if ((crat != null) && (crat.base == res))
                nrats[i] = crat;
        }
        for (int i = 0; i < nrats.length; i++) {
            Indir<Resource> res = rats.get(i);
            if ((nrats[i] == null) && (res != null)) {
                nrats[i] = new Rat(this, res, new MD(res, Collections.singletonList(new ResData(res, Message.nil))), randang(nrats));
            }
        }
        Rat[] prats = this.rats;
        this.rats = nrats;
    }

    public void state(Message msg) {
        int nstate = msg.uint8();
        if (nstate == state)
            return;
        state = nstate;
        if (state == 1) {
            for (Rat rat : rats) {
                if (rat != null) {
                    rat.npose = Rat.p_knock;
                    rat.state = 3;
                }
            }
            ((Sprite.CUpd) knot).update(new MessageBuf(new byte[]{2}));
            if (tails != null) {
                for (Sprite tail : tails)
                    ((Sprite.CUpd) tail).update(new MessageBuf(new byte[]{2}));
            }
        }
    }

    public void targets(Message msg) {
        tgta.clear();
        while (!msg.eom())
            tgta.add(msg.mnorm8() * 2 * Math.PI);
    }

    public static void parse(Gob gob, Message msg) {
        Ratking king = gob.getattr(Ratking.class);
        if (king == null)
            gob.setattr(king = new Ratking(gob));
        king.update(msg);
    }
}
