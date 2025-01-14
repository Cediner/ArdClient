package haven.res.gfx.kritter.rat.ratking;

import haven.Composite;
import haven.Composited;
import haven.Composited.MD;
import haven.Coord3f;
import haven.GLState;
import haven.GOut;
import haven.Indir;
import haven.Loading;
import haven.Location;
import haven.Matrix4f;
import haven.Message;
import haven.RenderList;
import haven.Rendered;
import haven.ResData;
import haven.Resource;
import haven.Skeleton;
import haven.Transform;
import haven.Utils;

import java.util.Collections;

public class Rat implements Rendered {
    public static final double ao = Math.PI / 10;
    public static final Indir<Resource> p_idle = Resource.classres(Rat.class).pool.load("gfx/kritter/rat/idle", 1);
    public static final Indir<Resource> p_fidle = Resource.classres(Rat.class).pool.load("gfx/kritter/rat/fgtidle-ratking", 1);
    public static final Indir<Resource> p_walking = Resource.classres(Rat.class).pool.load("gfx/kritter/rat/walking", 3);
    public static final Indir<Resource> p_resist = Resource.classres(Rat.class).pool.load("gfx/kritter/rat/clawrooted", 1);
    public static final Indir<Resource> p_knock = Resource.classres(Rat.class).pool.load("gfx/kritter/rat/knock", 1);
    public final Ratking king;
    public final Indir<Resource> base;
    public final Composited comp;
    public final GLState off;
    public double a, ta;
    public Indir<Resource> npose;
    public int state;

    public Rat(Ratking king, Indir<Resource> base, MD desc, double a) {
        this.king = king;
        this.base = base;
        this.comp = new Composited(base.get().flayer(Skeleton.Res.class).s);
        if (king.state == 0) {
            setpose(p_fidle, false);
            state = 0;
        } else if (king.state == 1) {
            setpose(p_knock, false);
            state = 3;
        }
        this.off = base.get().flayer(Skeleton.BoneOffset.class, "tail").from(this.comp.pose);
        this.comp.chmod(Collections.singletonList(desc));
        this.a = this.ta = Utils.cangle(a);
    }

    private void setpose(Indir<Resource> pose, boolean interp) {
        this.comp.new Poses(Composite.loadposes(Collections.singletonList(new ResData(pose, Message.nil)), king.gob, this.comp.skel, true)).set(interp ? 0.2f : 0);
    }

    private Location invert(GLState off) {
        return (new Location(Transform.rxinvert(((Location) off).fin(Matrix4f.id))));
    }

    private GLState loc() {
        Location l = Location.rot(Coord3f.zu, -(float) a);
        if (off instanceof Location)
            return (GLState.compose(l, invert(off)));
        return (l);
    }

    private void ticka(double dt) {
        double d = Utils.cangle(ta - a);
        if ((state == 1) || (state == 2)) {
            double ar = (state == 1) ? 50 : 10;
            double c = d * (1.0 - Math.pow(ar, -dt));
            a += c;
        } else if (state == 0) {
            double av = 1.5;
            double c = Math.signum(d) * Math.min(dt * av, Math.abs(d));
            a += c;
        } else {
        }
        a = Utils.cangle(a);
    }

    public void tick(double dt) {
        comp.tick((int) (dt * 1000.0));
        ticka(dt);
        if (npose != null) {
            try {
                setpose(npose, true);
                npose = null;
            } catch (Loading l) {}
        }
    }

    @Override
    public boolean setup(final RenderList r) {
        r.add(comp, loc());
        return (false);
    }

    @Override
    public void draw(final GOut g) {

    }
}
