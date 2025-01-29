package haven.res.lib.layspr;

import haven.Coord;
import haven.GOut;
import haven.GSprite;
import haven.Indir;
import haven.Message;
import haven.Resource;
import haven.TexI;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Layered extends GSprite implements GSprite.ImageSprite {
    final Layer[] lay;
    final Coord sz;
    public final List<Resource> layers = new CopyOnWriteArrayList<>();

    public static List<Indir<Resource>> decode(Resource.Resolver rr, Message sdt) {
        List<Indir<Resource>> ret = new ArrayList<>();
        while (!sdt.eom())
            ret.add(rr.getres(sdt.uint16()));
        return (ret);
    }

    public Layered(Owner owner, Collection<Indir<Resource>> lres) {
        super(owner);
        List<Layer> lay = new ArrayList<>(lres.size());
        for (Indir<Resource> res : lres) {
            layers.add(res.get());
            boolean f = false;
            for (Resource.Anim anim : res.get().layers(Resource.animc)) {
                f = true;
                lay.add(new Animation(anim));
            }
            if (f)
                continue;
            for (Resource.Image img : res.get().layers(Resource.imgc))
                lay.add(new Image(img));
        }
        lay.sort(Comparator.comparingInt(a -> a.z));
        this.lay = lay.toArray(new Layer[0]);
        sz = new Coord();
        for (Layer l : this.lay) {
            sz.x = Math.max(sz.x, l.sz.x);
            sz.y = Math.max(sz.y, l.sz.y);
        }
        tick(Math.random() * 10);
    }

    public void draw(GOut g) {
        for (Layer l : lay)
            l.draw(g);
    }

    public void draw(GOut g, Coord sz) {
        for (Layer l : lay)
            l.draw(g, sz);
    }

    public Coord sz() {
        return (sz);
    }

    public void tick(double dt) {
        for (Layer l : lay) {
            if (l instanceof Animation)
                ((Animation) l).tick(dt);
        }
    }

    public BufferedImage image() {
        Coord sz = sz();
        if ((sz.x < 1) || (sz.y < 1))
            return (null);
        BufferedImage img = TexI.mkbuf(sz);
        Graphics g = img.getGraphics();
        for (Layer l : lay) {
            if (l instanceof Image) {
                Image il = (Image) l;
                g.drawImage(il.img.img, il.img.o.x, il.img.o.y, null);
            }
        }
        return (img);
    }
}
