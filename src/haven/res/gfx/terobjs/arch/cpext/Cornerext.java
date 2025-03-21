package haven.res.gfx.terobjs.arch.cpext;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MapView;
import haven.MapView.Plob;
import haven.Message;
import haven.OCache;
import haven.Resource;
import haven.Sprite;

import static haven.MCache.tilesz;

@haven.FromResource(name = "gfx/terobjs/arch/cpext", version = 3, override = true)
public class Cornerext extends Sprite implements MapView.PlobAdjust {
    public final Coord2d sc;
    public final int dirs, gaps;

    public Cornerext(Owner owner, Resource res, Message sdt) {
        super(owner, res);
        sc = new Coord(sdt.int32(), sdt.int32()).mul(OCache.posres);
        dirs = sdt.uint8();
        gaps = sdt.uint8();
        Gob gob = owner.context(Gob.class);
        if (gob instanceof Plob) {
            ((Plob) gob).adjust = this;
        }
    }

    @Override
    public void adjust(Plob plob, Coord pc, Coord2d mc, int modflags) {
        Coord2d cl = null;
        for (int dir = 0; dir < 4; dir++) {
            if ((dirs & (1 << dir)) == 0)
                continue;
            for (int gap = 0; gap < 8; gap++) {
                if ((gaps & (1 << gap)) == 0)
                    continue;
                Coord2d dc = sc.add(Coord2d.sc((dir - 1) * Math.PI / 2, tilesz.x * (gap + 2)));
                if ((cl == null) || (dc.dist(mc) < cl.dist(mc)))
                    cl = dc;
            }
        }
        plob.move(cl, 0);
    }

    @Override
    public boolean rotate(Plob plob, int amount, int modflags) {
        return (false);
    }
}
