package haven.res.ui.plob_fdir;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MapView;
import haven.MapView.Plob;
import haven.Message;
import haven.Resource;
import haven.Sprite;
import haven.UI;

import static haven.MCache.tilesz;

@haven.FromResource(name = "ui/plob-fdir", version = 4, override = true)
public class Fixedplob extends Sprite implements MapView.PlobAdjust {
    public final double a;

    public Fixedplob(Owner owner, Resource res, Message sdt) {
        super(owner, res);
        this.a = Math.PI * 2 * (sdt.uint8() / 180.0);
        //Gob gob = owner.context(Gob.class);
        if (owner instanceof Plob) {
            ((Plob) owner).adjust = this;
        }
    }

    @Override
    public void adjust(Plob plob, Coord pc, Coord2d mc, int modflags) {
        Coord2d nc;
        if ((modflags & UI.MOD_SHIFT) == 0)
            nc = mc.floor(tilesz).mul(tilesz).add(tilesz.div(2));
        else
            nc = mc;
        plob.move(nc, a);
    }

    @Override
    public boolean rotate(Plob plob, int amount, int modflags) {
        return (false);
    }
}
