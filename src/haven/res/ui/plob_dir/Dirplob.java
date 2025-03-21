package haven.res.ui.plob_dir;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MapView;
import haven.MapView.Plob;
import haven.Message;
import haven.OCache;
import haven.Resource;
import haven.Sprite;
import haven.Utils;

@haven.FromResource(name = "ui/plob-dir", version = 3, override = true)
public class Dirplob extends Sprite implements MapView.PlobAdjust {
    public final Coord2d sc;
    public final int dirs;

    public Dirplob(Owner owner, Resource res, Message sdt) {
        super(owner, res);
        sc = new Coord(sdt.int32(), sdt.int32()).mul(OCache.posres);
        dirs = sdt.uint8();
        //Gob gob = owner.context(Gob.class);
        if (owner instanceof Plob) {
            Plob po = (Plob) owner;
            po.adjust = this;
            po.move(sc);
        }
    }

    @Override
    public void adjust(Plob plob, Coord pc, Coord2d mc, int modflags) {
        double cl = Double.NaN, md = Double.POSITIVE_INFINITY;
        double mdir = sc.angle(mc);
        for (int dir = 0; dir < 4; dir++) {
            if ((dirs & (1 << dir)) == 0)
                continue;
            double ca = (dir - 1) * Math.PI / 2;
            double cd = Math.abs(Utils.cangle(mdir - ca));
            if (Double.isNaN(cl) || cd < md) {
                cl = ca;
                md = cd;
            }
        }
        plob.move(cl);
    }

    @Override
    public boolean rotate(Plob plob, int amount, int modflags) {
        return (false);
    }
}
