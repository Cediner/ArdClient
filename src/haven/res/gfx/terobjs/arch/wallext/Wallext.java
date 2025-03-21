package haven.res.gfx.terobjs.arch.wallext;

import haven.Coord;
import haven.Coord2d;
import haven.Coord3f;
import haven.Drawable;
import haven.Location;
import haven.MapView;
import haven.MapView.Plob;
import haven.Message;
import haven.OCache;
import haven.RenderList;
import haven.Resource;
import haven.Sprite;
import haven.Utils;

import static haven.MCache.tilesz;

@haven.FromResource(name = "gfx/terobjs/arch/wallext", version = 16, override = true)
public class Wallext extends Sprite implements MapView.PlobAdjust {
    public final Coord2d sc;
    public final int dirs;
    public final int max;
    public final Sprite extsp;
    public final int ang;
    private Location exts[] = {};

    public Wallext(Owner owner, Resource res, Message sdt) {
        super(owner, res);
        sc = new Coord(sdt.int32(), sdt.int32()).mul(OCache.posres);
        dirs = sdt.uint8();
        max = sdt.uint8();
        ang = sdt.int8();
        //Gob gob = owner.context(Gob.class);
        if (owner instanceof Plob) {
            ((Plob) owner).adjust = this;
            extsp = Sprite.create(owner, ((Plob) owner).getattr(Drawable.class).getres(), Message.nil);
        } else {
            extsp = null;
        }
    }

    @Override
    public boolean setup(RenderList slot) {
        for (Location ext : exts)
            slot.add(extsp, ext);

        return (false);
    }

    @Override
    public void adjust(Plob plob, Coord pc, Coord2d mc, int modflags) {
        Coord2d cl = null;
        int cnum = 0;
        if ((dirs & 1) != 0) {
            int num = Math.min(Math.max((int) Math.round((sc.y - mc.y) / tilesz.y), 1), max);
            Coord2d dc = new Coord2d(sc.x, sc.y - (num * tilesz.y));
            if ((cl == null) || (dc.dist(mc) < cl.dist(mc))) {
                cl = dc;
                cnum = num;
            }
        }
        if ((dirs & 2) != 0) {
            int num = Math.min(Math.max((int) Math.round((mc.x - sc.x) / tilesz.x), 1), max);
            Coord2d dc = new Coord2d(sc.x + (num * tilesz.x), sc.y);
            if ((cl == null) || (dc.dist(mc) < cl.dist(mc))) {
                cl = dc;
                cnum = num;
            }
        }
        if ((dirs & 4) != 0) {
            int num = Math.min(Math.max((int) Math.round((mc.y - sc.y) / tilesz.y), 1), max);
            Coord2d dc = new Coord2d(sc.x, sc.y + (num * tilesz.y));
            if ((cl == null) || (dc.dist(mc) < cl.dist(mc))) {
                cl = dc;
                cnum = num;
            }
        }
        if ((dirs & 8) != 0) {
            int num = Math.min(Math.max((int) Math.round((sc.x - mc.x) / tilesz.x), 1), max);
            Coord2d dc = new Coord2d(sc.x - (num * tilesz.x), sc.y);
            if ((cl == null) || (dc.dist(mc) < cl.dist(mc))) {
                cl = dc;
                cnum = num;
            }
        }
        plob.move(cl);
        double dir = sc.angle(cl);
        int edir;
        if (ang < 0) {
            if (Utils.cangle(cl.angle(mc) - dir) < 0) {
                plob.move(dir - Math.PI / 2); edir = 1;
            } else {
                plob.move(dir + Math.PI / 2); edir = -1;
            }
        } else {
            plob.move(ang * Math.PI / 2);
            edir = ((Utils.cangle(plob.a - dir)) < 0) ? 1 : -1;
        }
        Location[] pexts = this.exts;
        Location exts[] = new Location[Math.max(cnum - 1, 0)];
        for (int i = 0; i < exts.length; i++)
            exts[i] = Location.xlate(new Coord3f(0, edir * (i + 1) * (float) tilesz.x, 0));
        this.exts = exts;
    }

    @Override
    public boolean rotate(Plob plob, int amount, int modflags) {
        return (false);
    }
}
