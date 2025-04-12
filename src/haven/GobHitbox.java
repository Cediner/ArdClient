package haven;

import haven.res.lib.obst.Obstacle;
import haven.sloth.gob.Type;
import haven.sloth.util.ResHashMap;
import modification.configuration;

import javax.media.opengl.GL2;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GobHitbox extends Sprite {
    public static final int olid_solid = "overlay_hitbox_solid_id".hashCode();
    public static final int olid = "overlay_hitbox_id".hashCode();
    public static States.ColState fillclrstate = new States.ColState(DefSettings.HIDDENCOLOR.get());
    public static States.ColState bbclrstate = new States.ColState(DefSettings.HITBOXCOLOR.get());
    private BBox[] b;
    private int mode;
    private States.ColState clrstate;
    private boolean wall = false;
    private final FloatBuffer[] buffers;

    public GobHitbox(Gob gob, BBox[] b, boolean fill) {
        super(gob, null);

        Color cc = configuration.canCustomColor(gob);
        if (fill) {
            mode = GL2.GL_QUADS;
            clrstate = cc == null ? fillclrstate : new States.ColState(cc);
        } else if (gob.type != Type.WALLSEG) {
            mode = GL2.GL_LINE_LOOP;
            clrstate = cc == null ? bbclrstate : new States.ColState(cc);
        } else {
            if (Config.flatwalls)
                wall = true;
            mode = GL2.GL_LINE_LOOP;
            clrstate = cc == null ? bbclrstate : new States.ColState(cc);
        }

//        a = new Coordf(ac.x, ac.y);
//        b = new Coordf(ac.x, bc.y);
//        c = new Coordf(bc.x, bc.y);
//        d = new Coordf(bc.x, ac.y);
        this.b = b;

        int type = getType();
        buffers = new FloatBuffer[b.length];
        for (int i = 0; i < b.length; i++) {
            buffers[i] = Utils.mkfbuf(b[i].points.length * 3);
            for (int p = 0; p < b[i].points.length; p++) {
                Coord2d point = b[i].points[p];
                buffers[i].put((float) point.x).put((float) ((type == 2 ? -1 : 1) * point.y)).put((float) (type == 3 ? 11 : 1));
            }
            buffers[i].rewind();
        }
    }

    public int getType() {
        if (mode == GL2.GL_LINE_LOOP && !wall) {
            return (1);
        } else if (!wall) {
            return (2);
        } else {
            return (3);
        }
    }

    public boolean setup(RenderList rl) {
        Location.goback(rl.state(), "gob");
        rl.prepo(clrstate);
        if (mode == GL2.GL_LINE_LOOP && !configuration.showaccboundingboxes)
            rl.prepo(States.xray);
        return true;
    }

    public void draw(GOut g) {
        try {
            g.apply();
            BGL gl = g.gl;
            if (getType() == 1)
                gl.glLineWidth(2.0F);
            for (int i = 0; i < b.length; i++) {
                gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                gl.glVertexPointer(3, GL2.GL_FLOAT, 0, buffers[i]);
                gl.glDrawArrays(mode, 0, b[i].points.length);
                gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static class BBox {
        public final Coord2d[] points;

        public BBox(Coord2d[] points) {
            this.points = new Coord2d[points.length];
            for (int i = 0; i < points.length; i++) {
                this.points[i] = new Coord2d(points[i].x, -points[i].y);
            }
        }

        public BBox(Coord ac, Coord bc) {
            this.points = new Coord2d[]{new Coord2d(ac.x, -ac.y), new Coord2d(bc.x, -ac.y), new Coord2d(bc.x, -bc.y), new Coord2d(ac.x, -bc.y)};
        }

        public BBox(Coord2d ac) {
            double x = ac.x / 2.0;
            double y = ac.y / 2.0;
            this.points = new Coord2d[]{new Coord2d(x, -y), new Coord2d(-x, -y), new Coord2d(-x, y), new Coord2d(x, y)};
        }
    }

    private static final BBox[] bboxCalf = new BBox[]{new BBox(new Coord(-9, -3), new Coord(9, 3))};
    private static final BBox[] bboxLamb = new BBox[]{new BBox(new Coord(-4, -2), new Coord(5, 2))};
    private static final BBox[] bboxGoat = new BBox[]{new BBox(new Coord(-3, -2), new Coord(4, 2))};
    private static final BBox[] bboxPig = new BBox[]{new BBox(new Coord(-6, -3), new Coord(6, 3))};
    private static final BBox[] bboxBoost = new BBox[]{new BBox(new Coord(-4, -4), new Coord(4, 4))};
    //    private static final BBox[] bboxCattle = new BBox[]{new BBox(new Coord(-12, -4), new Coord(12, 4))};
    private static final BBox[] bboxHorse = new BBox[]{new BBox(new Coord(-8, -4), new Coord(8, 4))};
    private static final BBox[] bboxTDeer = new BBox[]{new BBox(new Coord(-6, -2), new Coord(12, 2))};
//    private static final BBox[] bboxSmelter = new BBox[]{new BBox(new Coord(-12, -12), new Coord(12, 20))};
//    private static final BBox[] bboxWallseg = new BBox[]{new BBox(new Coord(-5, -6), new Coord(6, 5))};
//    private static final BBox[] bboxHwall = new BBox[]{new BBox(new Coord(-1, 0), new Coord(0, 11))};
//    private static final BBox[] bboxCupboard = new BBox[]{new BBox(new Coord(-5, -5), new Coord(5, 5))};

    public static BBox[] getBBox(Gob gob) throws Loading {
        Resource res = gob.getres();
        if (res == null)
            return (null);

        String name = res.name;

        // calves, lambs, cattle, goat
        if (name.equals("gfx/kritter/cattle/calf"))
            return bboxCalf;
        else if (name.equals("gfx/kritter/sheep/lamb"))
            return bboxLamb;
//        else if (name.equals("gfx/kritter/cattle/cattle"))
//            return bboxCattle;
        else if (name.startsWith("gfx/kritter/horse/"))
            return bboxHorse;
        else if (name.startsWith("gfx/kritter/goat/"))
            return bboxGoat;
        else if (name.startsWith("gfx/kritter/pig/"))
            return bboxPig;
        else if (name.startsWith("gfx/terobjs/boostspeed"))
            return bboxBoost;
        else if (name.matches("gfx/kritter/reindeer/teimdeer(bull|cow)"))
            return bboxTDeer;

        // dual state gobs
//        if (name.endsWith("gate") && name.startsWith("gfx/terobjs/arch")) {
//            GAttrib rd = gob.getattr(ResDrawable.class);
//            if (rd == null)     // shouldn't happen
//                return null;
//            int state = ((ResDrawable) rd).sdt.peekrbuf(0);
//            if (state == 1)     // open gate
//                return null;
//        } else if (name.endsWith("/pow")) {
//            GAttrib rd = gob.getattr(ResDrawable.class);
//            if (rd == null)     // shouldn't happen
//                return null;
//            int state = ((ResDrawable) rd).sdt.peekrbuf(0);
//            if (state == 17 || state == 33) // hf
//                return null;
//        }


//        if (name.endsWith("/smelter"))
//            return bboxSmelter;
//        else if (name.endsWith("brickwallseg") || name.endsWith("brickwallcp") ||
//                name.endsWith("palisadeseg") || name.endsWith("palisadecp") ||
//                name.endsWith("poleseg") || name.endsWith("polecp") ||
//                name.endsWith("drystonewallseg") || name.endsWith("drystonewallcp"))
//            return bboxWallseg;
//        else if (name.endsWith("/hwall"))
//            return bboxHwall;

        if (name.endsWith("/consobj")) {
            ResDrawable rd = gob.getattr(ResDrawable.class);
            if (rd != null && rd.sdt.rbuf.length >= 4) {
                MessageBuf buf = rd.sdt.clone();
                Obstacle obst = Obstacle.parse(buf);
//                return (new BBox[]{new BBox(new Coord(buf.rbuf[0], buf.rbuf[1]), new Coord(buf.rbuf[2], buf.rbuf[3]))});
                return (Arrays.stream(obst.p).map(BBox::new).toArray(BBox[]::new));
            }
        }

        if (res.name.endsWith("/trellis")) {
            Optional<BBox[]> hitbox = hitboxes.getc(res.name);
            if (hitbox.isPresent()) {
                return (hitbox.get());
            }
        } else {
            Optional<BBox[]> hitbox = hitboxes.get(res.name);
            if (hitbox.isPresent()) {
                return (hitbox.get());
            }
        }

//        try {
        List<Resource.Neg> negs = new ArrayList<>(res.layers(Resource.Neg.class));
        List<Resource.Obstacle> obsts = new ArrayList<>(res.layers(Resource.Obstacle.class));
        for (RenderLink.Res link : res.layers(RenderLink.Res.class)) {
            RenderLink l = link.l;
            if (l instanceof RenderLink.MeshMat) {
                RenderLink.MeshMat mm = (RenderLink.MeshMat) l;
                addIf(negs, getLayer(Resource.Neg.class, mm.srcres.indir(), mm.mesh));
                addIf(obsts, getLayer(Resource.Obstacle.class, mm.srcres.indir(), mm.mesh));
            }
            if (l instanceof RenderLink.AmbientLink) {
                RenderLink.AmbientLink al = (RenderLink.AmbientLink) l;
                addIf(negs, getLayer(Resource.Neg.class, al.res));
                addIf(obsts, getLayer(Resource.Obstacle.class, al.res));
            }
            if (l instanceof RenderLink.Collect) {
                RenderLink.Collect cl = (RenderLink.Collect) l;
                addIf(negs, getLayer(Resource.Neg.class, cl.from));
                addIf(obsts, getLayer(Resource.Obstacle.class, cl.from));
            }
            if (l instanceof RenderLink.Parameters) {
                RenderLink.Parameters pl = (RenderLink.Parameters) l;
                addIf(negs, getLayer(Resource.Neg.class, pl.res));
                addIf(obsts, getLayer(Resource.Obstacle.class, pl.res));
            }
        }

        final List<BBox> hitlist = new ArrayList<>();
        for (Resource.Obstacle o : obsts) {
            for (int i = 0; i < o.ep.length; i++) {
                hitlist.add(new BBox(o.ep[i]));
            }
        }
        for (Resource.Neg o : negs) {
            hitlist.add(new BBox(o.bs, o.bc));
        }
        if (!hitlist.isEmpty()) {
            BBox[] boxes = hitlist.toArray(new BBox[0]);
            hitboxes.put(res.name, boxes);
            return (boxes);
        }
//        } catch (Exception ignore) {
//            ignore.printStackTrace();
//        }
        return (null);
    }

    private static final ResHashMap<BBox[]> hitboxes = new ResHashMap<>();

    @SafeVarargs
    public static <T extends Resource.Layer> List<T> getLayer(final Class<T> layer, Indir<Resource>... reses) {
        final List<T> list = new ArrayList<>();
        for (Indir<Resource> ires : reses) {
            if (ires != null) {
                Resource res = ires.get();
                if (res != null) {
                    T l = res.layer(layer);
                    if (l != null)
                        list.add(layer.cast(l));
                }
            }
        }
        return (list);
    }

    public static <T extends Resource.Layer> void addIf(final List<T> end, final List<T> start) {
        for (T l : start)
            if (!end.contains(l))
                end.add(l);
    }
}
