/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.overlays.OverlayData;
import haven.overlays.TextOverlay;
import haven.overlays.newPlantStageSprite;
import haven.purus.gobText;
import haven.res.gfx.fx.floatimg.DamageText;
import haven.res.lib.tree.Tree;
import haven.res.lib.tree.TreeScale;
import haven.res.lib.vmat.Materials;
import haven.res.lib.vmat.VarSprite;
import haven.res.ui.obj.buddy.Buddy;
import haven.res.ui.obj.buddy.Info;
import haven.resutil.BPRadSprite;
import haven.resutil.RectSprite;
import haven.resutil.WaterTile;
import haven.sloth.gfx.GobSpeedSprite;
import haven.sloth.gfx.HitboxMesh;
import haven.sloth.gfx.SnowFall;
import haven.sloth.gob.AggroMark;
import haven.sloth.gob.Alerted;
import haven.sloth.gob.Deleted;
import haven.sloth.gob.Halo;
import haven.sloth.gob.HeldBy;
import haven.sloth.gob.Hidden;
import haven.sloth.gob.Holding;
import haven.sloth.gob.Mark;
import haven.sloth.gob.Movable;
import haven.sloth.gob.Type;
import haven.sloth.io.HighlightData;
import haven.sloth.script.pathfinding.Hitbox;
import integrations.mapv4.MappingClient;
import modification.configuration;
import modification.resources;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;

public class Gob implements Rendered, Sprite.Owner, Skeleton.ModOwner, Skeleton.HasPose, EquipTarget {
    public int cropstgmaxval = 0;
    private Overlay bowvector = null;
    public static final Text.Foundry gobhpf = new Text.Foundry(Text.serif, UI.scale(14)).aa(true);
    private static final Material.Colors dframeEmpty = new Material.Colors(new Color(0, 255, 0, 200));
    public static Material.Colors cRackEmpty = new Material.Colors(DefSettings.CHEESERACKEMPTYCOLOR.get());
    public static Material.Colors cRackFull = new Material.Colors(DefSettings.CHEESERACKFULLCOLOR.get());
    public static Material.Colors cRackMissing = new Material.Colors(DefSettings.CHEESERACKMISSINGCOLOR.get());
    private static final Material.Colors coopMissing = new Material.Colors(new Color(255, 0, 255, 255));
    private static final Material.Colors dframeDone = new Material.Colors(new Color(255, 0, 0, 200));
    private static final AtomicReference<GLState> storagefullcolormaterial = new AtomicReference<>(new Material.Colors(getFullStorageColor()));
    private static final AtomicReference<GLState> storageemptycolormaterial = new AtomicReference<>(new Material.Colors(getEmptyStorageColor()));
    private static final AtomicReference<GLState> storagehalfcolormaterial = new AtomicReference<>(new Material.Colors(getHalfStorageColor()));
    private static final AtomicReference<GLState> barrelemptycolormaterial = new AtomicReference<>(new Material.Colors(getEmptyBarrelColor()));
    private static final AtomicReference<GLState> troughemptycolormaterial = new AtomicReference<>(new Material.Colors(getEmptyTroughColor()));
    private static final AtomicReference<GLState> troughhalfcolormaterial = new AtomicReference<>(new Material.Colors(getHalfTroughColor()));
    private static final AtomicReference<GLState> troughfullcolormaterial = new AtomicReference<>(new Material.Colors(getFullTroughColor()));
    private static final AtomicReference<GLState> beehivefullcolormaterial = new AtomicReference<>(new Material.Colors(getFullBeehiveColor()));
    private static final Material.Colors dframeWater = new Material.Colors(new Color(0, 0, 255, 200));
    private static final Material.Colors dframeBark = new Material.Colors(new Color(165, 42, 42, 200));
    public static Material.Colors potDOne = new Material.Colors(DefSettings.GARDENPOTDONECOLOR.get());
//    public static Gob.Overlay animalradius = new Gob.Overlay(new BPRadSprite(100.0F, -10.0F, BPRadSprite.smatDanger));
//    public static Gob.Overlay doubleanimalradius = new Gob.Overlay(new BPRadSprite(200.0F, -20.0F, BPRadSprite.smatDanger));

    public static Overlay createBPRadSprite(Gob gob, String name) {
        switch (name) {
            case "animalradius":
                return new Overlay(gob, BPRadSprite.getId(name), new BPRadSprite(gob, 100.0F, -10.0F, BPRadSprite.smatDanger));
            case "doubleanimalradius":
                return new Overlay(gob, BPRadSprite.getId(name), new BPRadSprite(gob, 200.0F, -20.0F, BPRadSprite.smatDanger));
            case "rovlsupport":
                return new Overlay(gob, BPRadSprite.getId(name), new BPRadSprite(gob, 100.03125F, 0, BPRadSprite.smatSupports));
            case "rovlcolumn":
                return new Overlay(gob, BPRadSprite.getId(name), new BPRadSprite(gob, 125.0F, 0, BPRadSprite.smatSupports));
            case "rovlbeam":
                return new Overlay(gob, BPRadSprite.getId(name), new BPRadSprite(gob, 149.96094F, 0, BPRadSprite.smatSupports));
            case "rovltrough":
                return new Overlay(gob, BPRadSprite.getId(name), new BPRadSprite(gob, 200.0625F, -10.0F, BPRadSprite.smatTrough));
            case "rovlbeehive":
                return new Overlay(gob, BPRadSprite.getId(name), new BPRadSprite(gob, 149.96094F, -10.0F, BPRadSprite.smatBeehive));
            case "rovlbarterhand": {
                return new Overlay(gob, BPRadSprite.getId(name), new BPRadSprite(gob, 55.0f, -10.0F, BPRadSprite.smatBarter));
            }
            case "rovlmoundbed": {
                return new Overlay(gob, BPRadSprite.getId(name), new BPRadSprite(gob, 225.5F, -10.0F, BPRadSprite.smatMoundbed));
            }
            default:
                return (null);
        }
    }

    public static class Overlay implements Rendered {
        public final int id;
        public Gob gob;
        public Indir<Resource> res;
        public MessageBuf sdt;
        public Sprite spr;
        public boolean delign = false;
        private boolean added = false;

        public Overlay(Gob gob, int id, Indir<Resource> res, Message sdt) {
            this.gob = gob;
            this.id = id;
            this.res = res;
            this.sdt = new MessageBuf(sdt);
            spr = null;
        }

        public Overlay(int id, Indir<Resource> res, Message sdt) {
            this(null, id, res, sdt);
        }

        public Overlay(Gob gob, int id, Sprite spr) {
            this.gob = gob;
            this.id = id;
            this.res = null;
            this.sdt = null;
            this.spr = spr;
        }

        public Overlay(int id, Sprite spr) {
            this(null, id, spr);
        }

        public Overlay(Gob gob, Sprite spr) {
            this.gob = gob;
            this.id = -1;
            this.res = null;
            this.sdt = null;
            this.spr = spr;
        }

        public Overlay(Sprite spr) {
            this(null, spr);
        }

        private void init() {
            if (spr == null) {
                spr = Sprite.create(gob, res.get(), new MessageBuf(sdt).clone());
            }
        }

        private void add0() {
            if (added)
                return;
            added = true;
        }

        public String name() {
            try {
                if (res != null)
                    return res.get().name;
                else if (spr != null && spr.res != null)
                    return spr.res.name;
                else
                    return "";
            } catch (Loading l) {
                return "";
            }
        }

        public interface CDel {
            void delete();
        }

        public interface CUpd {
            void update(Message sdt);
        }

        public interface SetupMod {
            void setupgob(GLState.Buffer buf);

            void setupmain(RenderList rl);
        }

        @Override
        public void draw(GOut g) {}

        private void remove0() {
            if (!added)
                return;
//            if(slots != null) {
//                RUtils.multirem(new ArrayList<>(slots));
//                slots = null;
//            }
//            if (spr instanceof SetupMod) {
//                if ((!delign || (spr instanceof Overlay.CDel)) && done)
//                    ols.remove(ol);
//            }
            added = false;
            if (spr != null)
                spr.dispose();
        }

        public void remove(boolean async) {
            if (async) {
                gob.defer(() -> remove(false));
                return;
            }
            remove0();
            gob.ols.remove(this);
        }

        public void remove() {
            remove(true);
        }

        public void remove(Gob gob) {
            this.gob = gob;
            remove(true);
        }

        private final AtomicReference<SavedMatches> matches = new AtomicReference<>();

        @Override
        public boolean setup(RenderList rl) {
            if (spr != null) {
                if (matches(matches, this::name,"gfx/terobjs/trees/yulestar-.*")) {
                    if (matches(matches, this::name,".*fir")) {
                        rl.prepc(Location.xlate(Coord3f.of((float) -0.655989, (float) 0.183716, (float) 48.3776)));
                    } else if (matches(matches, this::name,".*spruce")) {
                        rl.prepc(Location.xlate(Coord3f.of(0f, (float) -3.055197, (float) 62.988228)));
                    } else if (matches(matches, this::name,".*silverfir")) {
                        rl.prepc(Location.xlate(Coord3f.of((float) -0.649652, (float) -0.030299, (float) 92.28412)));
                    }
                    rl.prepc(Location.rot(Coord3f.of(0f, 1f, 0f), (float) 1.570796));
                }
                rl.add(spr, null);
            }
            return (false);
        }

        @Override
        public Object staticp() {
            return ((spr == null) ? null : spr.staticp());
        }
    }

    public interface Placer {
        /* XXX: *Quite* arguably, the distinction between getc and
         * getr should be abolished and a single transform matrix
         * should be used instead, but that requires first abolishing
         * the distinction between the gob/gobx location IDs. */
        Coord3f getc(Coord2d rc, double ra);

        Matrix4f getr(Coord2d rc, double ra);
    }

    public interface Placing {
        Placer placer();
    }

    public static class DefaultPlace implements Placer {
        public final MCache map;
        public final MCache.SurfaceID surf;

        public DefaultPlace(MCache map, MCache.SurfaceID surf) {
            this.map = map;
            this.surf = surf;
        }

        @Override
        public Coord3f getc(Coord2d rc, double ra) {
            return (map.getzp(surf, rc));
        }

        @Override
        public Matrix4f getr(Coord2d rc, double ra) {
            return (Transform.makerot(new Matrix4f(), Coord3f.zu, -(float) ra));
        }
    }

    public static class InclinePlace extends DefaultPlace {
        public InclinePlace(MCache map, MCache.SurfaceID surf) {
            super(map, surf);
        }

        @Override
        public Matrix4f getr(Coord2d rc, double ra) {
            Matrix4f ret = super.getr(rc, ra);
            Coord3f norm = map.getnorm(surf, rc);
            norm.y = -norm.y;
            Coord3f rot = Coord3f.zu.cmul(norm);
            float sin = rot.abs();
            if (sin > 0) {
                Matrix4f incl = Transform.makerot(new Matrix4f(), rot.mul(1 / sin), sin, (float) Math.sqrt(1 - (sin * sin)));
                ret = incl.mul(ret);
            }
            return (ret);
        }
    }

    public static class BasePlace extends DefaultPlace {
        public final Coord2d[][] obst;
        private Coord2d cc;
        private double ca;
        private int seq = -1;
        private float z;

        public BasePlace(MCache map, MCache.SurfaceID surf, Coord2d[][] obst) {
            super(map, surf);
            this.obst = obst;
        }

        public BasePlace(MCache map, MCache.SurfaceID surf, Resource res, String id) {
            this(map, surf, res.flayer(Resource.obst, id).ep);
        }

        public BasePlace(MCache map, MCache.SurfaceID surf, Resource res) {
            this(map, surf, res, "");
        }

        private float getz(Coord2d rc, double ra) {
            Coord2d[][] no = this.obst, ro = new Coord2d[no.length][];
            {
                double s = Math.sin(ra), c = Math.cos(ra);
                for (int i = 0; i < no.length; i++) {
                    ro[i] = new Coord2d[no[i].length];
                    for (int o = 0; o < ro[i].length; o++)
                        ro[i][o] = Coord2d.of((no[i][o].x * c) - (no[i][o].y * s), (no[i][o].y * c) + (no[i][o].x * s)).add(rc);
                }
            }
            float ret = Float.NaN;
            for (int i = 0; i < no.length; i++) {
                for (int o = 0; o < ro[i].length; o++) {
                    Coord2d a = ro[i][o], b = ro[i][(o + 1) % ro[i].length];
                    for (Coord2d c : new Line2d.GridIsect(a, b, MCache.tilesz, false)) {
                        double z = map.getz(surf, c);
                        if (Float.isNaN(ret) || (z < ret))
                            ret = (float) z;
                    }
                }
            }
            return (ret);
        }

        @Override
        public Coord3f getc(Coord2d rc, double ra) {
            int mseq = map.chseq;
            if ((mseq != this.seq) || !Utils.eq(rc, cc) || (ra != ca)) {
                this.z = getz(rc, ra);
                this.seq = mseq;
                this.cc = rc;
                this.ca = ra;
            }
            return (Coord3f.of((float) rc.x, (float) rc.y, this.z));
        }
    }

    public static class LinePlace extends DefaultPlace {
        public final double max, min;
        public final Coord2d k;
        private Coord3f c;
        private Matrix4f r = Matrix4f.id;
        private int seq = -1;
        private Coord2d cc;
        private double ca;

        public LinePlace(MCache map, MCache.SurfaceID surf, Coord2d[][] points, Coord2d k) {
            super(map, surf);
            Line2d l = Line2d.from(Coord2d.z, k);
            double max = 0, min = 0;
            for (int i = 0; i < points.length; i++) {
                for (int o = 0; o < points[i].length; o++) {
                    int p = (o + 1) % points[i].length;
                    Line2d edge = Line2d.twixt(points[i][o], points[i][p]);
                    Coord2d t = l.cross(edge);
                    if ((t.y >= 0) && (t.y <= 1)) {
                        max = Math.max(t.x, max);
                        min = Math.min(t.x, min);
                    }
                }
            }
            if ((max == 0) || (min == 0))
                throw (new RuntimeException("illegal bounds for LinePlace"));
            this.k = k;
            this.max = max;
            this.min = min;
        }

        public LinePlace(MCache map, MCache.SurfaceID surf, Resource res, String id, Coord2d k) {
            this(map, surf, res.flayer(Resource.obst, id).ep, k);
        }

        public LinePlace(MCache map, MCache.SurfaceID surf, Resource res, Coord2d k) {
            this(map, surf, res, "", k);
        }

        private void recalc(Coord2d rc, double ra) {
            Coord2d rk = k.rot(ra);
            double maxz = map.getz(surf, rc.add(rk.mul(max)));
            double minz = map.getz(surf, rc.add(rk.mul(min)));
            Coord3f rax = Coord3f.of((float) -rk.y, (float) -rk.x, 0);
            float dz = (float) (maxz - minz);
            float dx = (float) (max - min);
            float hyp = (float) Math.sqrt((dx * dx) + (dz * dz));
            float sin = dz / hyp, cos = dx / hyp;
            c = Coord3f.of((float) rc.x, (float) rc.y, (float) minz + (dz * (float) (-min / (max - min))));
            r = Transform.makerot(new Matrix4f(), rax, sin, cos).mul(super.getr(rc, ra));
        }

        private void check(Coord2d rc, double ra) {
            int mseq = map.chseq;
            if ((mseq != this.seq) || !Utils.eq(rc, cc) || (ra != ca)) {
                recalc(rc, ra);
                this.seq = mseq;
                this.cc = rc;
                this.ca = ra;
            }
        }

        @Override
        public Coord3f getc(Coord2d rc, double ra) {
            check(rc, ra);
            return (c);
        }

        @Override
        public Matrix4f getr(Coord2d rc, double ra) {
            check(rc, ra);
            return (r);
        }
    }

    public static class PlanePlace extends DefaultPlace {
        public final Coord2d[] points;
        private Coord3f c;
        private Matrix4f r = Matrix4f.id;
        private int seq = -1;
        private Coord2d cc;
        private double ca;

        public static Coord2d[] flatten(Coord2d[][] points) {
            int n = 0;
            for (int i = 0; i < points.length; i++)
                n += points[i].length;
            Coord2d[] ret = new Coord2d[n];
            for (int i = 0, o = 0; i < points.length; o += points[i++].length)
                System.arraycopy(points[i], 0, ret, o, points[i].length);
            return (ret);
        }

        public PlanePlace(MCache map, MCache.SurfaceID surf, Coord2d[] points) {
            super(map, surf);
            this.points = points;
        }

        public PlanePlace(MCache map, MCache.SurfaceID surf, Coord2d[][] points) {
            this(map, surf, flatten(points));
        }

        public PlanePlace(MCache map, MCache.SurfaceID surf, Resource res, String id) {
            this(map, surf, res.flayer(Resource.obst, id).ep);
        }

        public PlanePlace(MCache map, MCache.SurfaceID surf, Resource res) {
            this(map, surf, res, "");
        }

        private void recalc(Coord2d rc, double ra) {
            double s = Math.sin(ra), c = Math.cos(ra);
            Coord3f[] pp = new Coord3f[points.length];
            for (int i = 0; i < pp.length; i++) {
                Coord2d rv = Coord2d.of((points[i].x * c) - (points[i].y * s), (points[i].y * c) + (points[i].x * s));
                pp[i] = map.getzp(surf, rv.add(rc));
            }
            int I = 0, O = 1, U = 2;
            Coord3f mn = Coord3f.zu;
            double ma = 0;
            for (int i = 0; i < pp.length - 2; i++) {
                for (int o = i + 1; o < pp.length - 1; o++) {
                    plane:
                    for (int u = o + 1; u < pp.length; u++) {
                        Coord3f n = pp[o].sub(pp[i]).cmul(pp[u].sub(pp[i])).norm();
                        for (int p = 0; p < pp.length; p++) {
                            if ((p == i) || (p == o) || (p == u))
                                continue;
                            float pz = (((n.x * (pp[i].x - pp[p].x)) + (n.y * (pp[i].y - pp[p].y))) / n.z) + pp[i].z;
                            if (pz < pp[p].z - 0.01)
                                continue plane;
                        }
                        double a = n.cmul(Coord3f.zu).abs();
                        if (a > ma) {
                            mn = n;
                            ma = a;
                            I = i;
                            O = o;
                            U = u;
                        }
                    }
                }
            }
            this.c = Coord3f.of((float) rc.x, (float) rc.y, (((mn.x * (pp[I].x - (float) rc.x)) + (mn.y * (pp[I].y - (float) rc.y))) / mn.z) + pp[I].z);
            this.r = Transform.makerot(new Matrix4f(), Coord3f.zu, -(float) ra);
            mn.y = -mn.y;
            Coord3f rot = Coord3f.zu.cmul(mn);
            float sin = rot.abs();
            if (sin > 0) {
                Matrix4f incl = Transform.makerot(new Matrix4f(), rot.mul(1 / sin), sin, (float) Math.sqrt(1 - (sin * sin)));
                this.r = incl.mul(this.r);
            }
        }

        private void check(Coord2d rc, double ra) {
            int mseq = map.chseq;
            if ((mseq != this.seq) || !Utils.eq(rc, cc) || (ra != ca)) {
                recalc(rc, ra);
                this.seq = mseq;
                this.cc = rc;
                this.ca = ra;
            }
        }

        @Override
        public Coord3f getc(Coord2d rc, double ra) {
            check(rc, ra);
            return (this.c);
        }

        @Override
        public Matrix4f getr(Coord2d rc, double ra) {
            return (this.r);
        }
    }

    /* XXX: This whole thing didn't turn out quite as nice as I had
     * hoped, but hopefully it can at least serve as a source of
     * inspiration to redo attributes properly in the future. There
     * have already long been arguments for remaking GAttribs as
     * well. */
    public static class ResAttr {
        public boolean update(Message dat) {
            return (false);
        }

        public void dispose() {
        }

        public static class Cell<T extends ResAttr> {
            final Class<T> clsid;
            Indir<Resource> resid = null;
            MessageBuf odat;
            public T attr = null;

            public Cell(Class<T> clsid) {
                this.clsid = clsid;
            }

            public void set(ResAttr attr) {
                if (this.attr != null)
                    this.attr.dispose();
                this.attr = clsid.cast(attr);
            }
        }

        private static class Load {
            final Indir<Resource> resid;
            final MessageBuf dat;

            Load(Indir<Resource> resid, Message dat) {
                this.resid = resid;
                this.dat = new MessageBuf(dat);
            }
        }

        @Resource.PublishedCode(name = "gattr", instancer = FactMaker.class)
        public interface Factory {
            ResAttr mkattr(Gob gob, Message dat);
        }

        public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<Factory> {
            public FactMaker() {
                super(Factory.class);
                add(new Direct<>(Factory.class));
                add(new Construct<>(Factory.class, ResAttr.class, new Class<?>[]{Gob.class, Message.class}, (cons) -> (gob, dat) -> cons.apply(new Object[]{gob, dat})));
            }
        }
    }


    public interface ANotif<T extends GAttrib> {
        void ch(T n);
    }

    public class Save extends GLState.Abstract {
        public Matrix4f cam = new Matrix4f(), wxf = new Matrix4f(),
                mv = new Matrix4f();
        public Projection proj = null;
        boolean debug = false;

        @Override
        public void prep(Buffer buf) {
            mv.load(cam.load(buf.get(PView.cam).fin(Matrix4f.id))).mul1(wxf.load(buf.get(PView.loc).fin(Matrix4f.id)));
            Projection proj = buf.get(PView.proj);
            PView.RenderState wnd = buf.get(PView.wnd);
            Coord3f s = proj.toscreen(mv.mul4(Coord3f.o), wnd.sz());
            Gob.this.sc = new Coord(s);
            Gob.this.sczu = proj.toscreen(mv.mul4(Coord3f.zu), wnd.sz()).sub(s);
            this.proj = proj;
        }
    }

    public class GobLocation extends GLState.Abstract {
        private Coord3f c = null;
        private double a = 0.0;
        private Matrix4f update = null;
        private final Location xl = new Location(Matrix4f.id, "gobx"), rot = new Location(Matrix4f.id, "gob");


        public void tick() {
            try {
                Coord3f c = getc();
                if (Config.disableelev)
                    c.z = 0;
                if (type == Type.WALLSEG && Config.flatwalls) {
                    c.z = c.z - 10;
                }
                c.y = -c.y;
                if (type == Type.ANIMAL || type == Type.DANGANIMAL) {
                    Tiler tl = glob.map.tiler(glob.map.gettile_safe(rc.floor(MCache.tilesz)));
                    if (tl instanceof WaterTile)
                        c.z += 5;
                }
                if ((this.c == null) || !c.equals(this.c))
                    xl.update(Transform.makexlate(new Matrix4f(), this.c = c));
                if (this.a != Gob.this.a)
                    rot.update(Transform.makerot(new Matrix4f(), Coord3f.zu, (float) -(this.a = Gob.this.a)));
            } catch (Loading l) {
            }
        }

        @Override
        public void prep(Buffer buf) {
            xl.prep(buf);
            rot.prep(buf);
        }
    }

    public static class Static {
    }

    public static final Static STATIC = new Static();

    public static class SemiStatic {
    }

    public static final SemiStatic SEMISTATIC = new SemiStatic();

    public final Save save = new Save();
    public final GobLocation loc = new GobLocation();
    public final GLState olmod = new GLState() {
        @Override
        public void apply(GOut g) {
        }

        @Override
        public void unapply(GOut g) {
        }

        @Override
        public void prep(Buffer buf) {
            for (Overlay ol : new ArrayList<>(ols)) {
                if (ol.spr instanceof Overlay.SetupMod) {
                    ((Overlay.SetupMod) ol.spr).setupgob(buf);
                }
            }

            Collection<GAttrib> attr = new ArrayList<>(Gob.this.attr.values());
            for (GAttrib a : attr)
                if (a instanceof Overlay.SetupMod)
                    ((Overlay.SetupMod) a).setupgob(buf);
        }
    };

    public Coord2d rc;
    public Coord sc;
    public Coord3f sczu;
    public double a;
    public boolean virtual = false;
    int clprio = 0;
    public boolean removed = false;
    public long id;
    public int frame;
    public final Glob glob;
    public int quality = 0;
    public final Map<Class<? extends GAttrib>, GAttrib> attr = Collections.synchronizedMap(new HashMap<>());
    private final Set<haven.Rendered> renderedattrs = Collections.synchronizedSet(new HashSet<>());
    public final Collection<Overlay> ols = Collections.synchronizedCollection(new LinkedList<Overlay>() {
        @Override
        public boolean add(Overlay item) {
            /* XXX: Remove me once local code is changed to use addol(). */
            if (glob.oc.getgob(id) != null) {
                // FIXME: extend ols with a method for adding sprites without triggering changed.
                if (item.id != Sprite.GROWTH_STAGE_ID && item != findol(BPRadSprite.getId("animalradius")) && item != findol(BPRadSprite.getId("doubleanimalradius")))
                    glob.oc.changed(Gob.this);
            }
            return (super.add(item));
        }
    });
    private final List<Overlay> dols = Collections.synchronizedList(new ArrayList<>());
    private final List<Pair<GAttrib, Consumer<Gob>>> dattrs = Collections.synchronizedList(new ArrayList<>());

    private final Collection<ResAttr.Cell<?>> rdata = Collections.synchronizedCollection(new LinkedList<>());
    private final Collection<ResAttr.Load> lrdata = Collections.synchronizedCollection(new LinkedList<>());
    private HitboxMesh hitboxmesh[];
    private boolean pathfinding_blackout = false;
    private List<Coord> hitboxcoords;

    private boolean discovered = false;
    public Type type;

    public Gob(Glob glob, Coord2d c, long id) {
        this.glob = glob;
        this.rc = c;
        this.id = id;
        if (id < 0)
            virtual = true;
    }

    public Gob(Glob glob, Coord2d c) {
        this(glob, c, -1);
    }

    /**
     * This method is called once as soon as its res name is accessible
     *
     * @param name The res name
     */
    private void discovered(final String name) {
        if (Deleted.isDeleted(name)) {
            //We don't care about these gobs, tell OCache to start the removal process
            //dispose();
            //glob.oc.remove(this);
            return;
        }
        disableAnimation1 = configuration.checkDisableAnimation1(id);
        disableAnimation = configuration.checkDisableAnimation(name);
        {
            configuration.GobScale rmulti = configuration.getItem(name);
            if (rmulti != null && rmulti.enable()) {
                scaleMatrix = rmulti.getMatrix();
            }

            configuration.GobScale rsingle = configuration.resizablegobsid.get(this.id);
            if (rsingle != null && rsingle.enable()) {
                scaleMatrix1 = rsingle.getMatrix();
            }
        }

        //Don't try to discover anything until we know who the plgob is.
        final UI ui = glob.ui.get();
        if (ui != null && ui.gui != null && ui.gui.map != null && ui.gui.map.plgob != -1) {
            if (ui.gui.mapfile != null && resources.customMarkObj) {
                for (Map.Entry<String, Boolean> entry : resources.customMarks.entrySet()) {
                    String key = entry.getKey();
                    if (name.equals(key)) {
                        if (Boolean.TRUE.equals(entry.getValue())) {
                            ui.gui.mapfile.markobj(id, this, resources.getDefaultTextName(key), configuration.customEnabledMarks.contains(key));
                            break;
                        }
                    }
                }
            }

            //Before we do anything make sure we care about this
            if (!Deleted.isDeleted(name)) {
                //Gobs we care about
                //Figure out our type first
                type = Type.getType(name);
                //checks for mannequins and changes their type to prevent unknown alarms
                if (type == Type.HUMAN && getattr(GobHealth.class) != null)
                    type = Type.UNKNOWN;

                if (configuration.gobspeedsprite && (type == Type.HUMAN || type == Type.ANIMAL || name.startsWith("gfx/kritter/")) && !isDead()) {
                    addol(new Overlay(this, GobSpeedSprite.id, new GobSpeedSprite(this)));
                    //if(id == ui.gui.map.rlplgob) {
                    //    addol(new Overlay(-4921, new SnowFall(this)));
                    //}
                }

                if (configuration.snowfalloverlay && type == Type.HUMAN && isplayer()) {
                    if (findol(-4921) == null)
                        addol(new Overlay(this, -4921, new SnowFall(this)));
                }
                if (type == Type.UNKNOWN && matches(matches, () -> name,"gfx/terobjs/trees/[a-zA-Z\\-]+stump$"))
                    type = Type.STUMP;
                if (type == Type.UNKNOWN && matches(matches, () -> name,"gfx/terobjs/trees/[a-zA-Z\\-]+log$"))
                    type = Type.LOG;
                if (type == Type.UNKNOWN && matches(matches, () -> name,"gfx/terobjs/trees/[a-zA-Z\\-]+$"))
                    type = Type.TREE;
                if (type == Type.UNKNOWN && matches(matches, () -> name,"gfx/terobjs/plants/(?!trellis).*"))
                    type = Type.PLANT;

                String customIcon = null;
                if ((type == Type.TREE || type == Type.BUSH || type == Type.STUMP || type == Type.LOG) && (!matches(matches, () -> name,".*trees/old(stump|trunk)"))) {
                    String fistname1 = name.substring(0, name.lastIndexOf('/'));
                    String fistname = fistname1.substring(0, fistname1.lastIndexOf('/'));
                    String lastname = name.replace(fistname, "");
                    if (lastname.endsWith("stump"))
                        lastname = lastname.substring(0, lastname.length() - "stump".length());
                    if (lastname.endsWith("log"))
                        lastname = lastname.substring(0, lastname.length() - "log".length());

                    customIcon = fistname + "/mm" + lastname;
                } else if (type == Type.BOULDER) {
                    customIcon = name.substring(0, name.length() - 1).replace("terobjs/bumlings", "invobjs");
                } else if (name.equals("gfx/terobjs/map/cavepubble")) {
                    customIcon = "gfx/invobjs/clay-cave";
                } else if (name.equals("gfx/terobjs/map/dustpile")) {
                    customIcon = "gfx/invobjs/cavedust";
                } else if (type == Type.DUNGEONDOOR) {
                    customIcon = "gfx/icons/door";
                }

                Indir<Tex> ctex = Config.additonalicons.get(name);
                if (customIcon != null || ctex != null) {
                    String finalCustomIcon = customIcon;
                    glob.loader.defer(() -> {
                        if (getattr(GobIcon.class) == null) {
                            if (ctex != null) {
                                setattr(new GobIcon.CustomGobIcon(Gob.this, name, ctex));
                                return;
                            }
                            Resource res = null;
                            try {
                                res = Resource.remote().loadwait(finalCustomIcon);
                                setattr(new GobIcon(Gob.this, res.indir()));
                            } catch (Loading l) {
                                if (res != null) {
                                    Resource finalRes = res;
                                    l.waitfor(() -> setattr(new GobIcon(Gob.this, finalRes.indir())), waiting -> {
                                    });
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }, null);
                }

                if (type == Type.UNKNOWN && name.startsWith("gfx/terobjs/bumlings/"))
                    type = Type.BOULDER;

                //Check for any special attributes we should attach
                Alerted.checkAlert(name, this);

                if (Hidden.isHidden(name)) {
                    setattr(new Hidden(this));
                }
                if (HighlightData.isHighlighted(name)) {
                    mark(-1);
                }
                if (OverlayData.isTexted(name)) {
                    addol(new Overlay(Sprite.GOB_TEXT_ID, new TextOverlay(this)));
                }
                if (type == Type.HUMAN) {
                    setattr(new Halo(this));
                }

                res().ifPresent((res) -> { //should always be present once name is discovered
                    final Hitbox[] hitbox = Hitbox.hbfor(this, true);
                    if (hitbox != null) {
                        hitboxmesh = HitboxMesh.makehb(this, hitbox);
                        updateHitmap();
                    }
                });
            } else {
                //We don't care about these gobs, tell OCache to start the removal process
                dispose();
                glob.oc.remove(this);
            }
            discovered = true;
        }

        try {
            Overlay ol = findol(DamageText.id);
            if (ol == null) {
                DamageText.Numbers numbers = glob().gobmap.get(id);
                if (numbers != null) {
                    DamageText text = new DamageText(this, Resource.remote().loadwait("gfx/fx/floatimg"), numbers);
                    daddol(DamageText.id, text);
                    text.remake();
                }
            }
        } catch (Exception ignore) {
        }
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public void updateHitmap() {
        synchronized (glob.gobhitmap) {
            if (hitboxcoords != null) {
                glob.gobhitmap.rem(this, hitboxcoords);
                hitboxcoords = null;
            }
            //don't want objects being held to be on the hitmap
            final UI ui = glob.ui.get();
            if (getattr(HeldBy.class) == null &&
                    (getattr(Holding.class) == null || ui == null || ui.gui == null || getattr(Holding.class).held != ui.gui.map.plgob) &&
                    !pathfinding_blackout) {
                hitboxcoords = glob.gobhitmap.add(this);
            }
        }
    }

    public void updatePathfindingBlackout(final boolean val) {
        this.pathfinding_blackout = val;
        updateHitmap();
    }

    public void mark(final int life) {
        if (findol(Mark.id) == null) {
            daddol(Mark.id, new Mark(life));
        } else {
            ((Mark) (findol(Mark.id).spr)).setLife(life);
        }
    }

    public void aggromark(final int life) {
        if (findol(AggroMark.id) == null) {
            daddol(AggroMark.id, new AggroMark(life));
        } else {
            ((AggroMark) (findol(AggroMark.id).spr)).setLife(life);
        }
    }

    public void unmark() {
        if (findol(Mark.id) != null) {
            ((Mark) (findol(Mark.id).spr)).revoke();
        }
    }

    public void ctick(int dt) {
        if (Deleted.isDeleted(name())) return;
        final Hidden hid = getattr(Hidden.class);
        if (!(hid != null && Config.hideuniquegobs) || configuration.showhiddenoverlay) {
            if (!discovered) {
                resname().ifPresent(this::discovered);
            }

            for (GAttrib a : new ArrayList<>(this.attr.values()))
                a.ctick(dt);

            List<Pair<GAttrib, Consumer<Gob>>> dattrs;
            synchronized (this.dattrs) {
                dattrs = new ArrayList<>(this.dattrs);
                this.dattrs.clear();
            }
            for (Pair<GAttrib, Consumer<Gob>> pair : dattrs) {
                setattr(pair.a);
                pair.a.ctick(dt);
                pair.b.accept(this);
            }

            for (Overlay ol : new ArrayList<>(ols)) {
                if (ol.spr == null) {
//                    try {
//                        ol.spr = Sprite.create(this, ol.res.get(), ol.sdt.clone());
//                    } catch (Loading e) {
//                    }
                } else {
                    boolean done = ol.spr.tick(dt);
                    done |= ol.spr.tick(dt / 1000.0);
                    if ((!ol.delign || (ol.spr instanceof Overlay.CDel) || (ol.spr instanceof Sprite.CDel)) && done)
                        ol.remove(this);
                }
            }

            for (Overlay ol : new ArrayList<>(dols)) {
                ols.add(ol);
                dols.remove(ol);
            }
            if (virtual && ols.isEmpty())
                glob.oc.remove(this);
        }
    }

    public String details() {
        StringBuilder sb = new StringBuilder();
        sb.append("Res: ");
        if (res().isPresent()) sb.append(getres());
        sb.append(" [").append(id).append("][").append(frame).append("]\n");
        final GobIcon icon = getattr(GobIcon.class);
        if (icon != null) {
            sb.append("Icon: ").append(icon.res.get()).append(Arrays.toString(icon.sdt)).append("\n");
        }
        sb.append("Type: ").append(type).append("\n");
        sb.append("staticp: ").append(staticp() != null ? "static" : "dynamic").append("\n");
        final Holding holding = getattr(Holding.class);
        if (holding != null) {
            sb.append("Holding: ").append(holding.held).append(" - ").append(holding.tgt().resname().orElse("Unknown")).append("\n");
        }
        final HeldBy heldby = getattr(HeldBy.class);
        if (heldby != null) {
            sb.append("Held By: ").append(heldby.holder).append(" - ").append(heldby.tgt().resname().orElse("Unknown")).append("\n");
        }
//        sb.append(attr.entrySet()).append("\n");
        ResDrawable dw = getattr(ResDrawable.class);
        if (dw != null) {
            sb.append("ResDraw: ").append(Arrays.toString(dw.sdt.rbuf));
            if (dw.spr != null) {
                sb.append(", ").append("[").append(dw.spr.getClass().getName()).append("]");
                if (dw.spr instanceof VarSprite) {
                    VarSprite varSprite = (VarSprite) dw.spr;
//                    sb.append("\n").append("[").append(varSprite.mats()).append("]");
                    if (varSprite.mats() instanceof Materials) {
                        Materials materials = (Materials) varSprite.mats();
                        for (Map.Entry<Integer, Material> entry : materials.mats.entrySet()) {
                            sb.append("\n").append("[").append(entry.getKey()).append(":");
                            for (GLState gl : entry.getValue().states) {
//                            sb.append(":[").append(gl).append("]");
                                if (gl instanceof TexGL.TexDraw) {
                                    TexGL.TexDraw texDraw = (TexGL.TexDraw) gl;
                                    sb.append(texDraw.tex);
                                }
                            }
                            sb.append("]");
                        }
                    }
                }
                if (dw.spr instanceof Tree) {
                    Tree treeSprite = (Tree) dw.spr;
                    sb.append(", ").append(treeSprite.fscale);
                }
            }
            sb.append("\n");
            sb.append("sdt: ").append(dw.sdtnum()).append("\n");
        } else {
            Composite comp = getattr(Composite.class);
            if (comp != null) {
                sb.append(eq()).append("\n");
            }
        }
        if (!ols.isEmpty()) {
            sb.append("Overlays: ").append(ols.size()).append("\n");
            for (Overlay ol : ols) {
                if (ol != null) {
                    sb.append("ol: ").append("[id:").append(ol.id).append("]");
                    if (ol.res != null && ol.res.get() != null) sb.append("[r:").append(ol.res.get()).append("]");
                    if (ol.spr != null) sb.append("[s:").append(ol.spr).append("]");
//                    if (ol.sdt != null) sb.append(", d").append(Arrays.toString(ol.sdt.rbuf));
                    sb.append("\n");
                }
            }
        }

        Map<Class<? extends GAttrib>, GAttrib> attr;
        synchronized (this.attr) {
            attr = new HashMap<>(this.attr);
        }
        if (attr.size() > 0) {
            sb.append("GAttribs: ").append(attr.size()).append("\n");
            for (GAttrib ga : attr.values()) {
                if (ga != null) {
                    if (ga instanceof TreeScale)
                        sb.append("ga: ").append("[TreeScale: ").append(((TreeScale) ga).scale).append("]");
                    else
                        sb.append("ga: ").append("[").append(ga).append("]");
                    sb.append("\n");
                }
            }
        }

        sb.append("Angle: ").append(Math.toDegrees(a)).append("\n");
        sb.append("Position: ").append(String.format("(%.3f, %.3f, %.3f)", getc().x, getc().y, getc().z)).append("\n");
        DoubleUnaryOperator offset = (s) -> {
            int n = 100 * 11;
            return (s % n < 0 ? s % n + n : s % n);
        };
        double ox = offset.applyAsDouble(getc().x);
        double oy = offset.applyAsDouble(getc().y);
        sb.append("Offset: ").append(String.format("(%.3f x %.3f) (%.0f x %.0f)", ox, oy, Math.floor(ox / 11.0), Math.floor(oy / 11.0))).append("\n");
        if (configuration.moredetails) {
            sb.append("Layers: ").append("\n");
            for (Resource.Layer l : getres().layers()) {
                sb.append("--").append(l).append("\n");
            }
        }
        return sb.toString();
    }

    public String rnm(Indir<Resource> r) {
        try {
            if (r != null && r.get() != null)
                return r.get().name;
            else
                return "";
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isDead() {
        Drawable d = getattr(Drawable.class);
        if (d instanceof Composite) {
            Composite comp = (Composite) d;
            if (comp.oldposes != null) {
                for (ResData res : comp.oldposes) {
                    final String nm = rnm(res.res).toLowerCase();
                    final String last = nm.contains("/") ? nm.substring(nm.lastIndexOf("/")) : "";
                    if (nm.endsWith("knock") || nm.endsWith("dead") || last.contains("knock")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String eq() {
        Drawable d = getattr(Drawable.class);
        if (d instanceof Composite) {
            Composite comp = (Composite) d;

            final StringBuilder sb = new StringBuilder();
            sb.append("Equipment:");
            if (comp.lastnequ != null)
                for (Composited.ED eq : comp.lastnequ) {
                    sb.append("\nEqu: ");
                    sb.append(rnm(eq.res.res));
                    sb.append(" @ ");
                    sb.append(eq.at);
                }

            if (comp.nmod != null)
                for (Composited.MD md : comp.nmod) {
                    sb.append("\nMod: ");
                    sb.append(rnm(md.mod));
                    for (ResData rd : md.tex) {
                        sb.append("\n  Tex: ");
                        sb.append(rnm(rd.res));
                    }
                }

            sb.append("\nPoses:");
            if (comp.oldposes != null) {
                for (ResData res : comp.oldposes) {
                    sb.append("\nPose: ");
                    sb.append(rnm(res.res));
                }
            }
            if (comp.oldtposes != null) {
                for (ResData res : comp.oldtposes) {
                    sb.append("\nTPose: ");
                    sb.append(rnm(res.res));
                }
            }
            return sb.toString();
        }
        return "";
    }

    private final LinkedList<Runnable> deferred = new LinkedList<>();
    private Loader.Future<?> deferral = null;

    private void deferred() {
        while (true) {
            Runnable task;
            synchronized (deferred) {
                task = deferred.peek();
                if (task == null) {
                    deferral = null;
                    return;
                }
            }
            synchronized (this) {
                if (!removed)
                    task.run();
            }
            if (task instanceof Disposable)
                ((Disposable) task).dispose();
            synchronized (deferred) {
                if (deferred.poll() != task)
                    throw (new RuntimeException());
            }
        }
    }

    public void defer(Runnable task) {
        synchronized (deferred) {
            deferred.add(task);
            if (deferral == null)
                deferral = glob.loader.defer(this::deferred, null);
        }
    }

    /* Intended for local code. Server changes are handled via OCache. */
    public void addol(Overlay ol, boolean async) {
        if (async) {
            defer(() -> addol(ol, false));
            return;
        }
        ol.init();
        ol.add0();
        ols.add(ol);
    }

    public void addol(Overlay ol) {
        addol(ol, false);
    }

    public void addol(Sprite ol) {
        addol(new Overlay(this, ol));
    }

    public void addol(Indir<Resource> res, Message sdt) {
        addol(new Overlay(this, -1, res, sdt));
    }

    public boolean hasOverlay(Class<?> cl) {
        synchronized (ols) {
            for (Overlay ol : ols) {
                if (cl.isInstance(ol) || cl.isInstance(ol.spr))
                    return (true);
            }
        }
        return (false);
    }

    public void remol(Overlay ol) {
        if (ol == null) return;
        ol.remove(this);
        /*synchronized (ols) {
            ols.remove(ol);
        }*/
    }

    public Overlay daddol(final Overlay ol) {
        dols.add(ol);
        return ol;
    }

    public Overlay daddol(int id, Sprite spr) {
        final Overlay ol = new Overlay(this, id, spr);
        daddol(ol);
        return ol;
    }

    public Overlay findol(int id) {
        synchronized (ols) {
            for (Overlay ol : ols) {
                if (ol.id == id)
                    return (ol);
            }
            for (Overlay ol : dols) {
                if (ol.id == id)
                    return ol;
            }
        }
        return (null);
    }

    public void tick() {
        if (Deleted.isDeleted(name())) return;
        synchronized (attr) {
            for (GAttrib a : attr.values())
                a.tick();
        }
        loadrattr();
    }

    public void dispose() {
        if (hitboxcoords != null) {
            synchronized (glob.gobhitmap) {
                glob.gobhitmap.rem(this, hitboxcoords);
                hitboxcoords = null;
            }
        }
        synchronized (attr) {
            for (GAttrib a : attr.values())
                a.dispose();
        }
        for (ResAttr.Cell rd : rdata) {
            if (rd.attr != null)
                rd.attr.dispose();
        }
    }

    public void updsdt() {
        resname().ifPresent(name -> {
            if (name.endsWith("gate") || name.endsWith("/pow")) {
                updateHitmap();
            }
        });
    }

    public boolean moving() {
        return getattr(Moving.class) != null;
    }

    public void move(Coord2d c, double a) {
        Moving m = getattr(Moving.class);
        if (m != null)
            m.move(c);
        synchronized (glob.gobhitmap) {
            if (hitboxcoords != null) {
                glob.gobhitmap.rem(this, hitboxcoords);
                hitboxcoords = null;
            }
            this.rc = c;

            if (isplayer()) {
                if (glob.ui != null) {
                    UI ui = glob.ui.get();
                    if (!configuration.endpoint.isEmpty() && ui != null && ui.sess != null && ui.sess.alive() && ui.sess.username != null && ui.gui != null) {
                        if (!ui.gui.chrid.isEmpty()) {
                            String username = ui.sess.username + "/" + ui.gui.chrid;
                            if (configuration.loadMapSetting(username, "mapper")) {
                                MappingClient map = MappingClient.getInstance(username);
                                map.CheckGridCoord(c);
                            }
                        }
                    }
                }

                if (configuration.savingFogOfWar)
                    glob.addFOW(c);
            }
            this.a = a;
            if (glob.ui != null) {
                final UI ui = glob.ui.get();
                if (discovered) {
                    if (getattr(HeldBy.class) == null &&
                            (getattr(Holding.class) == null || ui == null || (ui.gui != null && ui.gui.map != null && getattr(Holding.class).held != ui.gui.map.plgob)) &&
                            !pathfinding_blackout) {
                        hitboxcoords = glob.gobhitmap.add(this);
                    }
                }
            }
        }
    }

    public void move(Coord2d c) {
        move(c, this.a);
    }

    public void move(double a) {
        move(this.rc, a);
    }

    public Coord3f getc() {
        Moving m = getattr(Moving.class);
        Coord3f ret = (m != null) ? m.getc() : getrc();
        DrawOffset df = getattr(DrawOffset.class);
        if (df != null)
            ret = ret.add(df.off);
        return (ret);
    }

    public Coord3f getc_old() {
        Moving m = getattr(Moving.class);
        Coord3f ret = (m != null) ? m.getc() : getrc_old();
        DrawOffset df = getattr(DrawOffset.class);
        if (df != null)
            ret = ret.add(df.off);
        return (ret);
    }

    public Coord3f getrc() {
        return (glob.map.getzp(rc));
    }

    public Coord3f getrc_old() {
        return (glob.map.getzp_old(rc));
    }//only exists because follow cam hates the new getz


    public double geta() {
        return a;
    }

    private Class<? extends GAttrib> attrclass(Class<? extends GAttrib> cl) {
        while (true) {
            Class<?> p = cl.getSuperclass();
            if (p == GAttrib.class)
                return (cl);
            cl = p.asSubclass(GAttrib.class);
        }
    }

    public void setattr(GAttrib a) {
//        if (a instanceof haven.Rendered)
//            renderedattrs.add((haven.Rendered) a);
        Class<? extends GAttrib> ac = attrclass(a.getClass());
        GAttrib at = attr.put(ac, a);
        if (at != null)
            at.dispose();
//        if (DefSettings.SHOWPLAYERPATH.get() && gobpath == null && a instanceof LinMove) {
//            final UI ui = glob.ui.get();
//            if (ui != null) {
//                try {
//                    Gob pl = glob.oc.getgob(ui.gui.map.plgob);
//                    if (pl != null) {
//                        Following follow = pl.getattr(Following.class);
//                        if (pl == this ||
//                                (follow != null && follow.tgt() == this)) {
//                            gobpath = new Overlay(new GobPath(this));
//                            ols.add(gobpath);
//                        }
//                    }
//                } catch (Exception e) {
//                }//ignore, this is just a draw a line on player movement. Not critical.
//            }
//        }
    }

    public void delayedsetattr(GAttrib a, Consumer<Gob> cb) {
        dattrs.add(new Pair<>(a, cb));
    }

    public <C extends GAttrib> C getattr(Class<C> c) {
        GAttrib attr = this.attr.get(attrclass(c));
        if (!c.isInstance(attr))
            return (null);
        return (c.cast(attr));
    }

    public void delattr(Class<? extends GAttrib> c) {
        GAttrib at = attr.remove(attrclass(c));
        if (at != null)
            at.dispose();
    }

    private Class<? extends ResAttr> rattrclass(Class<? extends ResAttr> cl) {
        while (true) {
            Class<?> p = cl.getSuperclass();
            if (p == ResAttr.class)
                return (cl);
            cl = p.asSubclass(ResAttr.class);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends ResAttr> ResAttr.Cell<T> getrattr(Class<T> c) {
        for (ResAttr.Cell<?> rd : new ArrayList<>(rdata)) {
            if (rd.clsid == c)
                return ((ResAttr.Cell<T>) rd);
        }
        ResAttr.Cell<T> rd = new ResAttr.Cell<>(c);
        rdata.add(rd);
        return (rd);
    }

    public static <T extends ResAttr> ResAttr.Cell<T> getrattr(Object obj, Class<T> c) {
        if (!(obj instanceof Gob))
            return (new ResAttr.Cell<>(c));
        return (((Gob) obj).getrattr(c));
    }

    private void loadrattr() {
        boolean upd = false;
        for (ResAttr.Load rd : new ArrayList<>(lrdata)) {
            task:
            {
                ResAttr attr;
                try {
                    attr = rd.resid.get().getcode(ResAttr.Factory.class, true).mkattr(this, rd.dat.clone());
                } catch (Loading l) {
                    continue;
                } catch (Exception e) {
                    break task;
                }
                ResAttr.Cell<?> rc = getrattr(rattrclass(attr.getClass()));
                if (rc.resid == null)
                    rc.resid = rd.resid;
                else if (rc.resid != rd.resid)
                    throw (new RuntimeException("Conflicting resattr resource IDs on " + rc.clsid + ": " + rc.resid + " -> " + rd.resid));
                rc.odat = rd.dat;
                rc.set(attr);
            }
            lrdata.remove(rd);
            upd = true;
        }
        if (upd) {
            if (glob.oc.getgob(id) != null)
                glob.oc.changed(this);
        }
    }

    public void setrattr(Indir<Resource> resid, Message dat) {
        for (ResAttr.Cell<?> rd : new ArrayList<>(rdata)) {
            if (rd.resid == resid) {
                if (dat.equals(rd.odat))
                    return;
                if ((rd.attr != null) && rd.attr.update(dat))
                    return;
                break;
            }
        }
        for (ResAttr.Load rd : new ArrayList<>(lrdata)) {
            if (rd.resid == resid) {
                lrdata.remove(rd);
                break;
            }
        }
        lrdata.add(new ResAttr.Load(resid, dat));
        loadrattr();
    }

    public void delrattr(Indir<Resource> resid) {
        for (ResAttr.Cell<?> rd : new ArrayList<>(rdata)) {
            if (rd.resid == resid) {
                rdata.remove(rd);
                rd.attr.dispose();
                break;
            }
        }
        for (ResAttr.Load rd : new ArrayList<>(lrdata)) {
            if (rd.resid == resid) {
                lrdata.remove(rd);
                break;
            }
        }
    }

    @Override
    public GLState eqpoint(String nm, Message dat) {
        for (GAttrib attr : this.attr.values()) {
            if (attr instanceof EquipTarget) {
                GLState ret = ((EquipTarget) attr).eqpoint(nm, dat);
                if (ret != null)
                    return (ret);
            }
        }
        return (null);
    }

    public int sdt() {
        ResDrawable dw = getattr(ResDrawable.class);
        if (dw != null)
            return dw.sdtnum();
        return 0;
    }

    @Override
    public void draw(GOut g) {}

    public boolean disableAnimation1;
    public boolean disableAnimation;
    public Matrix4f scaleMatrix1;
    public Matrix4f scaleMatrix;
    private gobText barreltext;
    private gobText treetext;
    private gobText signtext;

//    private static final Map<Object, WeakReference<SavedMatches>> matchesMap = Collections.synchronizedMap(new HashMap<>());
    private final AtomicReference<SavedMatches> matches = new AtomicReference<>();
    static class SavedMatches {
        String name;
        Map<String, Boolean> matches = Collections.synchronizedMap(new HashMap<>());
    }

    static boolean matches(AtomicReference<SavedMatches> matches, Supplier<String> getter, String regex) {
        synchronized (matches) {
            SavedMatches saved = matches.get();
            String name = getter.get();
            if (saved == null) {
                saved = new SavedMatches();
                saved.name = name;
                boolean result = name.matches(regex);
                saved.matches.put(regex, result);
                matches.set(saved);
                return (result);
            } else {
                if (!Objects.equals(name, saved.name)) {
                    saved = new SavedMatches();
                    saved.name = name;
                    boolean result = name.matches(regex);
                    saved.matches.put(regex, result);
                    matches.set(saved);
                    return (result);
                } else {
                    return (saved.matches.computeIfAbsent(regex, name::matches));
                }
            }
        }
    }

    @Override
    public boolean setup(RenderList rl) {
        if (Deleted.isDeleted(name())) return (false);
        loc.tick();
        try {
            Collection<Location.Chain> ret = new ArrayList<>();
            Location.Chain loc = rl.state().get(PView.loc);
            if (loc != null)
                ret.add(loc);
            lastChain = ret;//Collections.singleton(Location.goback(rl.state(), "gobx")); //FIXME may need change to GobLocation loc
        } catch (Throwable e) {}
        final Hidden hid = getattr(Hidden.class);
        if (hid != null && Config.hideuniquegobs) {
            if (Config.showoverlay) {
                hid.setup(rl);
            }
        }
        if (!(hid != null && Config.hideuniquegobs) || configuration.showhiddenoverlay) {
            if (configuration.rotateworld) {
                rl.prepc(Location.rot(new Coord3f(1, 0, 0), (float) Math.toRadians(configuration.rotateworldvalx)));
                rl.prepc(Location.rot(new Coord3f(0, 1, 0), (float) Math.toRadians(configuration.rotateworldvaly)));
                rl.prepc(Location.rot(new Coord3f(0, 0, 1), (float) Math.toRadians(configuration.rotateworldvalz)));
            }
            if (configuration.transparencyworld) {
                rl.prepc(WaterTile.surfmat);
                rl.prepc(States.xray);
            }
            if (configuration.resizegob) {
                Matrix4f scaleMatrix1 = this.scaleMatrix1;
                if (scaleMatrix1 != null) {
                    rl.prepc(new Location(scaleMatrix1));
                } else {
                    Matrix4f scaleMatrix = this.scaleMatrix;
                    if (scaleMatrix != null) {
                        rl.prepc(new Location(scaleMatrix));
                    }
                }
            }
            List<Overlay> ols = new ArrayList<>(this.ols);
            String barrelText = null;
            String signText = null;
            boolean barrelRet = true;
            for (Overlay ol : ols) {
                String olname = ol.name();
                if (matches(ol.matches, () -> olname,"gfx/terobjs/trees/yulestar-.*")) {
                    if (ol.spr == null || ol.spr.res == null || matches(ol.matches, () -> olname,"gfx/terobjs/trees/yulestar-.*"))
                        ol.spr = Sprite.create(this, Resource.remote().loadwait("gfx/terobjs/items/yulestar"), ol.sdt);
                }
                if (matches(matches, this::name,"gfx/terobjs/barrel")) {
                    if (Config.showbarreltext && barrelText == null && matches(ol.matches, () -> olname,"gfx/terobjs/barrel-.*")) {
                        barrelText = olname.substring(olname.lastIndexOf("-") + 1);
                    }
                    if (Config.showbarrelstatus && barrelRet && matches(ol.matches, () -> olname,"gfx/terobjs/barrel-.*")) {
                        barrelRet = false;
                    }
                }

                rl.add(ol, null);

                if (ol.spr instanceof Overlay.SetupMod)
                    ((Overlay.SetupMod) ol.spr).setupmain(rl);
            }
            if (Config.showbarreltext && matches(matches, this::name,"gfx/terobjs/barrel") && !ols.isEmpty()) {
                if (barrelText != null) {
                    if (barreltext == null) barreltext = new gobText(this, barrelText, Color.GREEN, 50);
                    else barreltext.update(barrelText);
                    rl.add(barreltext, null);
                } else {
                    if (barreltext != null) barreltext = null;
                }
            } else {
                if (barreltext != null) barreltext = null;
            }
            if (Config.showsigntext && matches(matches, this::name,"gfx/terobjs/iconsign")) {
                if (signText == null) {
                    try {
                        ResDrawable resdraw = getattr(ResDrawable.class);
                        MessageBuf sdt = resdraw.sdt.clone();

                        int resid = sdt.uint16();
                        Message isdt = Message.nil;
                        if((resid & 0x8000) != 0) {
                            resid &= ~0x8000;
                            isdt = new MessageBuf(sdt.bytes(sdt.uint8()));
                        }
                        Resource ires = this.context(Resource.Resolver.class).getres(resid).get();//meat???
                        signText = ires.basename();
                    } catch (Throwable e) {}
                }
                if (signText != null) {
                    if (signtext == null) signtext = new gobText(this, signText, Color.GREEN, 50);
                    else signtext.update(signText);
                    rl.add(signtext, null);
                } else {
                    if (signtext != null) signtext = null;
                }
            } else {
                if (signtext != null) signtext = null;
            }
            if (Config.showbarrelstatus && matches(matches, this::name,"gfx/terobjs/barrel")) {
                if (barrelRet) rl.prepc(barrelemptycolormaterial.get());
            }
            for (Map.Entry<Long, String> entry : configuration.treesMap.entrySet()) {
                if (entry.getKey() == id) {
                    String text = entry.getValue();
                    if (text != null) {
                        if (treetext == null) treetext = new gobText(this, text, Color.ORANGE, 50);
                        rl.add(treetext, null);
                    }
                    if (!hasOverlay(PartyMemberOutline.class))
                        addol(new PartyMemberOutline(this, Color.ORANGE));
                    break;
                }
            }
            if ((!Config.showbarreltext || ols.isEmpty()) && barreltext != null) barreltext = null;
            if (!Config.showsigntext && signtext != null) signtext = null;

            Collection<GAttrib> attr = new ArrayList<>(this.attr.values());
            for (GAttrib a : attr)
                if (a instanceof Overlay.SetupMod)
                    ((Overlay.SetupMod) a).setupmain(rl);

            if (type == Type.HUMAN || type == Type.VEHICLE || type == Type.WATERVEHICLE || type == Type.ANIMAL || type == Type.SMALLANIMAL || type == Type.TAMEDANIMAL || type == Type.DANGANIMAL) {
//                    if (Movable.isMovable(name)) {}
                if (isMoving() && getattr(Movable.class) == null)
                    setattr(new Movable(this));
            }

            if (configuration.resizableworld) {
                float scale = (float) configuration.worldsize;
                rl.prepc(new Location(new Matrix4f(
                        scale, 0, 0, 0,
                        0, scale, 0, 0,
                        0, 0, scale, 0,
                        0, 0, 0, 1)));
            }
//            final GobHealth hlt = getattr(GobHealth.class);
//            if (hlt != null)
//                rl.prepc(hlt.getfx());

//            final GobQuality qlty = getattr(GobQuality.class);
//            if (qlty != null)
//                rl.prepc(qlty.getfx());

            if (Config.showrackstatus && type == Type.CHEESERACK) {
                final List<Overlay> eqOls = new ArrayList<>();
                for (Overlay ol : ols)
                    if (ol.name().contains("gfx/fx/eq"))
                        eqOls.add(ol);
                if (eqOls.isEmpty())
                    rl.prepc(cRackEmpty);
                else if (eqOls.size() == 3)
                    rl.prepc(cRackFull);
                else if (eqOls.size() < 3 && Config.cRackmissing)
                    rl.prepc(cRackMissing);
            }
            if (Config.showcupboardstatus) {
                resname().flatMap(Gob::findStorageMaterial).ifPresent(sm -> {
                    int stage = getattr(ResDrawable.class).sdt.peekrbuf(0);
                    sm.getColor(stage).ifPresent(rl::prepc);
                });
            }
            if (configuration.showtreeberry && (type == Type.TREE || type == Type.BUSH)) {
                ResDrawable mb = getattr(ResDrawable.class);
                if (mb != null) {
                    int stage = mb.sdt.peekrbuf(0);
                    if (stage == 16 || stage == 32 || stage == 48) {
                        rl.prepc(Rendered.eyesort);
//                    rl.prepc(Rendered.deflt);
//                    rl.prepc(Rendered.first);
//                    rl.prepc(Rendered.last);
//                    rl.prepc(Rendered.postfx);
                        rl.prepc(Rendered.postpfx);
//                    rl.prepc(States.vertexcolor);
//                    rl.prepc(WaterTile.surfmat);
//                    rl.prepc(Light.vlights); //plights vlights
//                    rl.prepc(WaterTile.wfog);
                        rl.prepc(new Material.Colors(
                                new Color(configuration.showtreeberryamb, true),
                                new Color(configuration.showtreeberrydif, true),
                                new Color(configuration.showtreeberryspc, true),
                                new Color(configuration.showtreeberryemi, true)
                        ));
                    }
                }
            }
            if (Config.showshedstatus && type == Type.SHED) {
                int stage = getattr(ResDrawable.class).sdt.peekrbuf(0);

                if (stage == 30 || stage == 29)
                    rl.prepc(storagefullcolormaterial.get());
                if (stage == 1 || stage == 2)
                    rl.prepc(storageemptycolormaterial.get());
                //while open : empty == 1, 1 item to half items = 5, half full = 13, full 29
                //while closed : empty = 2, 1 item to half items = 6, half full= 14, full 30
            }

            if (MapView.markedGobs.contains(id))
                rl.prepc(MapView.markedFx);

            if (Config.showdframestatus && type == Type.TANTUB) {
                int stage = getattr(ResDrawable.class).sdt.peekrbuf(0);
                // BotUtils.sysLogAppend("Sprite num : "+stage,"white");
                if (stage == 2)
                    rl.prepc(dframeEmpty);
                if (stage == 10 || stage == 9 || stage == 8)
                    rl.prepc(dframeDone);
                if (stage == 0 || stage == 1 || stage == 4 || stage == 5)
                    rl.prepc(dframeWater);
            }
            if (Config.showcoopstatus && type == Type.COOP) {
                int stage = getattr(ResDrawable.class).sdt.peekrbuf(0);
                if (stage == 0)
                    rl.prepc(cRackFull);
                if (stage == 1)
                    rl.prepc(coopMissing);
                if (stage == 2)
                    rl.prepc(dframeWater);
            }
            if (Config.showhutchstatus && type == Type.HUTCH) {
          /*  no rabbits -stage 2 = no food or water
            stage 1  = no food or water doors open
            stage 6  = water no food
            stage 5 = water no food doors open
            stage 62 = food water doors closed
            stage 61 = food water doors open
            stage 58 = food no water
            stage 57 = food no water door open

            1-11 rabbits
            stage 125 food and water doors open
            stage 126 food and water doors closed
            stage 69 no food water doors open
            stage 70 no food water doors closed
            stage 66 no food no water doors closed
            stage 65 no food no water doors open
            stage 122 food no water doors closed
            stage 121 food no water doors open

            12 rabbits - stage -2 = food and water doors closed
            stage -3  = food and water doors open
            stage -6 = food no water doors closed
            stage -7 = food no water doors open
            stage -62 no food no water doors closed
            stage -63 no food no water doors open
            stage -58 no food water doors closed
            stage -59 no food water doors open*/
                int stage = getattr(ResDrawable.class).sdt.peekrbuf(0);
                if (stage == 2 || stage == 1 || stage == -62 || stage == -63 || stage == 66 || stage == 65)
                    rl.prepc(cRackFull);
                if (stage == 6 || stage == 5 || stage == -58 || stage == -59 || stage == 69 || stage == 70 || stage == -51 || stage == -50)
                    rl.prepc(coopMissing);
                if (stage == -38 || stage == 58 || stage == 57 || stage == -6 || stage == -7 || stage == 122 || stage == 121)
                    rl.prepc(dframeWater);
            }

            if (configuration.showtroughstatus && type == Type.TROUGH) {
                int stage = getattr(ResDrawable.class).sdt.peekrbuf(0);

                if (stage == 1)
                    rl.prepc(troughhalfcolormaterial.get());
                if (stage == 0)
                    rl.prepc(troughemptycolormaterial.get());
                if (stage == 7)
                    rl.prepc(troughfullcolormaterial.get());
            }

            if (configuration.showbeehivestatus && type == Type.BEEHIVE) {
                int stage = getattr(ResDrawable.class).sdt.peekrbuf(0);

                if (stage == 5 || stage == 6 || stage == 7 || stage == 9 || stage == 15)
                    rl.prepc(beehivefullcolormaterial.get());
            }

            if (OverlayData.isHighlighted(name())) {
                OverlayData.OverlayGob og = OverlayData.get(name());
                if (og != null) rl.prepc(new Material.Colors(og.highlightColor));
            }

            if (Config.showdframestatus && type == Type.DFRAME) {
                boolean done = true;
                boolean empty = true;
                for (Overlay ol : ols) {
                    try {
                        Indir<Resource> olires = ol.res;
                        if (olires != null) {
                            empty = false;
                            Resource olres = olires.get();
                            if (olres != null) {
                                if (olres.name.endsWith("-blood") || olres.name.endsWith("-windweed") || olres.name.endsWith("fishraw")) {
                                    done = false;
                                    break;
                                }
                            }
                        }
                    } catch (Loading l) {
                    }
                }
                if (done && !empty)
                    rl.prepc(dframeDone);
                else if (empty)
                    rl.prepc(dframeEmpty);
            }


            if (Config.highlightpots && (type == Type.GARDENPOT || name().equalsIgnoreCase("gfx/terobjs/moundbed"))) {
                final List<Overlay> eqOls = new ArrayList<>();
                for (Overlay ol : ols)
                    if (ol.name().contains("gfx/fx/eq"))
                        eqOls.add(ol);
                if (eqOls.size() == 2)
                    rl.prepc(potDOne);
            }


//            for (final haven.Rendered rattr : new ArrayList<>(renderedattrs)) {
//                rattr.setup(rl);
//            }

            GobHighlight highlight = getattr(GobHighlight.class);
            if (highlight != null) {
                if (highlight.cycle <= 0)
                    delattr(GobHighlight.class);
                else
                    rl.prepc(highlight.getfx());
            }


            Drawable d = getattr(Drawable.class);
            try {
                if (d != null) {
                    if (!(hid != null && Config.hideuniquegobs)) {
                        Overlay ol = findol(GobHitbox.olid_solid);
                        if (Config.hidegobs && ((type == Type.TREE && Config.hideTrees) || (type == Type.LOG && Config.hideLogs) || (type == Type.STUMP && Config.hideStumps) || (type == Type.BUSH && Config.hideBushes) || (type == Type.BOULDER && Config.hideboulders) || (configuration.boostspeedbox && name().equalsIgnoreCase("gfx/terobjs/boostspeed")))) {
                            if (Config.showoverlay) {
                                if (ol == null) {
                                    try {
                                        GobHitbox.BBox[] bbox = GobHitbox.getBBox(this);
                                        if (bbox != null)
                                            addol(new Overlay(this, GobHitbox.olid_solid, new GobHitbox(this, bbox, true)));
                                    } catch (Loading l) {}
                                }
                            } else if (ol != null)
                                remol(ol);
                            if (name().equalsIgnoreCase("gfx/terobjs/boostspeed")) {
                                d.setup(rl);
                            }
                        } else {
                            if (ol != null)
                                remol(ol);
                            d.setup(rl);
                        }
                    }
                    if (Config.showarchvector && type == Type.HUMAN && d instanceof Composite) {
                        boolean targetting = false;
                        Gob followGob = null;
                        Moving moving = getattr(Moving.class);
                        if (moving instanceof Following)
                            followGob = ((Following) moving).tgt();
                        for (Composited.ED ed : ((Composite) d).comp.cequ) {
                            try {
                                Resource res = ed.res.res.get();
                                if (res != null && (res.name.endsWith("huntersbow") || res.name.endsWith("rangersbow")) && ed.res.sdt.peekrbuf(0) == 5) {
                                    targetting = true;
                                    if (bowvector == null) {
                                        bowvector = new Overlay(new GobArcheryVector(this, followGob));
                                        addol(bowvector);
                                    }
                                    break;
                                }
                            } catch (Loading l) {
                            }
                        }
                        if (!targetting && bowvector != null) {
                            remol(bowvector);
                            bowvector = null;
                        }
                    }
                }
            } catch (Throwable e) {
                //TODO: This is a weird issue that can pop up on startup, need to look into it
                return false;
            }
            Overlay hitboxOl = findol(GobHitbox.olid);
            if (Config.showboundingboxes) {
                if (hitboxOl == null) {
                    try {
                        GobHitbox.BBox[] bbox = GobHitbox.getBBox(this);
                        if (bbox != null) {
                            addol(new Overlay(this, GobHitbox.olid, new GobHitbox(this, bbox, false)));
                        }
                    } catch (Loading l) {}
                }
            } else if (hitboxOl != null) {
                remol(hitboxOl);
            }
            Overlay plantOl = findol(Sprite.GROWTH_STAGE_ID);
            plants:
            if (Config.showplantgrowstage) {
                Resource res;
                try {
                    res = getres();
                } catch (Throwable e) {
                    break plants;
                }
                if (res == null) break plants;
                if (type == Type.PLANT || type == Type.MULTISTAGE_PLANT) {
                    int stage = getattr(ResDrawable.class).sdt.peekrbuf(0);
                    if (cropstgmaxval == 0) {
                        for (FastMesh.MeshRes layer : res.layers(FastMesh.MeshRes.class)) {
                            int stg = layer.id / 10;
                            if (stg > cropstgmaxval)
                                cropstgmaxval = stg;
                        }
                    }
                    if (plantOl == null && (stage == cropstgmaxval || (Config.showfreshcropstage ? stage >= 0 : stage > 0) && stage < 6)) {
                        if (configuration.newCropStageOverlay)
                            addol(new Gob.Overlay(this, Sprite.GROWTH_STAGE_ID, new newPlantStageSprite(stage, cropstgmaxval, type == Type.MULTISTAGE_PLANT, (res.basename().contains("turnip") || res.basename().contains("leek") || res.basename().contains("carrot")))));
                        else
                            addol(new Gob.Overlay(this, Sprite.GROWTH_STAGE_ID, new PlantStageSprite(stage, cropstgmaxval, type == Type.MULTISTAGE_PLANT, (res.basename().contains("turnip") || res.basename().contains("leek")))));
                    } else if (plantOl != null && !Config.showfreshcropstage && stage == 0 || (stage != cropstgmaxval && stage >= 6)) {
                        remol(plantOl);
                    } else if (plantOl != null && configuration.newCropStageOverlay && plantOl.spr instanceof newPlantStageSprite && ((newPlantStageSprite) plantOl.spr).stg != stage) {
                        ((newPlantStageSprite) plantOl.spr).update(stage, cropstgmaxval);
                    } else if (plantOl != null && plantOl.spr instanceof PlantStageSprite && ((PlantStageSprite) plantOl.spr).stg != stage) {
                        ((PlantStageSprite) plantOl.spr).update(stage, cropstgmaxval);
                    }
                }
            } else if (plantOl != null)
                remol(plantOl);
            Overlay treeOl = findol(Sprite.GROWTH_STAGE_ID2);
            trees:
            if (Config.showtreegrowstage) {
                if (type == Type.TREE || type == Type.BUSH) {
                    ResDrawable rd = getattr(ResDrawable.class);
                    if (rd != null) {
                        int fscale = rd.sdt.peekrbuf(1);
                        if (fscale != -1) {
                            /*
                            if (ol == null) {
                                addol(new Gob.Overlay(Sprite.GROWTH_STAGE_ID, new TreeStageSprite(fscale)));
                            } else if (((TreeStageSprite) ol.spr).val != fscale) {
                                ((TreeStageSprite) ol.spr).update(fscale);
                            }
                            */
                            fscale = (fscale + 255) % 255;
                            int minStage = (type == Type.TREE ? 10 : 30);
                            int growPercents = (int) Math.ceil((float) (fscale - minStage) / (float) (100 - minStage) * 100f);
                            if (treeOl == null) {
                                if (growPercents < 100 || growPercents >= configuration.minimumtreestage)
                                    addol(new Gob.Overlay(this, Sprite.GROWTH_STAGE_ID2, new TreeStageSprite(growPercents)));
                            } else if (((TreeStageSprite) treeOl.spr).val != growPercents) {
                                if (growPercents < 100 || growPercents >= configuration.minimumtreestage)
                                    ((TreeStageSprite) treeOl.spr).update(growPercents);
                                else
                                    remol(treeOl);
                            }
                        }
                    }
                }
            } else if (treeOl != null)
                remol(treeOl);
            if (Config.stranglevinecircle && type == Type.STRANGLEVINE) {
                if (!ols.isEmpty())
                    return (false);
                else
                    addol(new Gob.Overlay(new PartyMemberOutline(this, new Color(0, 255, 0, 255))));
            }
            if (Config.showanimalrad && !Config.doubleradius && type == Type.DANGANIMAL) {
                Overlay ol = findol(BPRadSprite.getId("animalradius"));
                if (!isDead() && ol == null)
                    addol(createBPRadSprite(this, "animalradius"));
                else if (isDead() && ol != null)
                    remol(ol);
            } else if (Config.showanimalrad && Config.doubleradius && type == Type.DANGANIMAL) {
                Overlay ol = findol(BPRadSprite.getId("doubleanimalradius"));
                if (!isDead() && ol == null)
                    addol(createBPRadSprite(this, "doubleanimalradius"));
                else if (isDead() && ol != null)
                    remol(ol);
            }

            for (GAttrib a : attr) {
                if (a instanceof PView.Render2D) {
                    rl.add((Rendered) a, null);
                } else if (a instanceof Rendered) {
                    ((Rendered) a).setup(rl);
                }
            }

            if (DefSettings.SHOWHITBOX.get() && hitboxmesh != null && hitboxmesh.length != 0) {
                for (HitboxMesh mesh : hitboxmesh)
                    rl.add(mesh, null);
            }

            /*if (type == Type.TAMEDANIMAL) {
                CattleId cattleId = getattr(CattleId.class);
                if (cattleId != null) {
                    Overlay co = findol(CattleIdSprite.id);
                    if (co == null) {
                        CattleIdSprite sprite = new CattleIdSprite(cattleId);
                        addol(new Overlay(CattleIdSprite.id, sprite));
                        cattleId.sprite = sprite;
                    } else if (cattleId.sprite == null) {
                        cattleId.sprite = (CattleIdSprite) co.spr;
                    }
                }
            }*/

            if (name().equals("gfx/borka/body") && isplayer()) {
                int borderhash = Arrays.hashCode("playerborder".getBytes());
                int boxhash = Arrays.hashCode("playerbox".getBytes());
                int gridhash = Arrays.hashCode("gridbox".getBytes());
                Overlay border = findol(borderhash);
                Overlay box = findol(boxhash);
                Overlay grid = findol(gridhash);
                if (border == null && configuration.playerbordersprite)
                    addol(new Overlay(this, borderhash, new RectSprite(this, new Coord2d(MCache.cmaps.mul(9)), () -> new Color(configuration.playerbordercolor, true), new Coord2d(MCache.cmaps))));
                else if (border != null && !configuration.playerbordersprite)
                    remol(border);
                if (box == null && configuration.playerboxsprite)
                    addol(new Overlay(this, boxhash, new RectSprite(this, new Coord2d(MCache.cmaps), () -> new Color(configuration.playerboxcolor, true), new Coord2d(MCache.cmaps))));
                else if (box != null && !configuration.playerboxsprite)
                    remol(box);
                if (grid == null && configuration.gridboxsprite)
                    addol(new Overlay(this, gridhash, new RectSprite(this, new Coord2d(MCache.cmaps.mul(11)), () -> new Color(configuration.gridboxcolor, true), new Coord2d(MCache.cmaps.mul(11)))));
                else if (grid != null && !configuration.gridboxsprite)
                    remol(grid);
            }
        }

        return (false);
    }

    private Collection<Location.Chain> lastChain = null;


    private static final Object DYNAMIC = new Object();
    private Object seq = null;

    @Override
    public Object staticp() {
        if (type == Type.HUMAN)
            seq = DYNAMIC;

        if (seq != null) {
            return ((seq == DYNAMIC) ? null : seq);
        } else if (getattr(Hidden.class) == null) {
            int rs = 0;
            for (GAttrib attr : new ArrayList<>(attr.values())) {
                Object as = attr.staticp();
                if (as == Rendered.CONSTANS) {
                } else if (as instanceof Static) {
                } else if (as == SemiStatic.class) {
                    rs = Math.max(rs, 1);
                } else {
                    rs = 2;
                    break;
                }
            }
            for (Overlay ol : new ArrayList<>(ols)) {
                Object os = ol.staticp();
                if (os == Rendered.CONSTANS) {
                } else if (os instanceof Static) {
                } else if (os == SemiStatic.class) {
                    rs = Math.max(rs, 1);
                } else {
                    rs = 2;
                    break;
                }
            }
            if (getattr(Info.class) != null) {
                rs = 2; //I want to see the names above fires/players without it being screwed up
            }
            switch (rs) {
                case 0:
                    seq = new Static();
                    break;
                case 1:
                    seq = new SemiStatic();
                    break;
                default:
                    seq = null;
                    break;
            }
            return ((seq == DYNAMIC) ? null : seq);
        } else {
            //New hidden gob
            return seq = getattr(Moving.class) == null ? STATIC : DYNAMIC;
        }
    }

    void changed() {
        seq = null;
    }

    @Override
    public Random mkrandoom() {
        return (Utils.mkrandoom(id));
    }

    public Optional<String> resname() {
        return res().map((res) -> res.name);
    }

    public String name() {
        return resname().orElse("");
    }

    public Optional<Resource> res() {
        Resource res = null;
        try {
            res = getres();
        } catch (Loading e) {
        }
        if (res == null)
            return Optional.empty();
        return Optional.of(res);
    }

    @Override
    public Resource getres() {
        Drawable d = getattr(Drawable.class);
        if (d != null)
            return (d.getres());
        return (null);
    }

    @Override
    public Skeleton.Pose getpose() {
        Drawable d = getattr(Drawable.class);
        if (d != null)
            return (d.getpose());
        return (null);
    }

    private static final ClassResolver<Gob> ctxr = new ClassResolver<Gob>()
            .add(Gob.class, g -> g)
            .add(Glob.class, g -> g.glob)
            .add(OCache.class, g -> g.glob.oc)
            .add(Session.class, g -> g.glob.sess);

    @Override
    public <T> T context(Class<T> cl) {
        return (ctxr.context(cl, this));
    }

    @Override
    @Deprecated
    public Glob glob() {
        return (context(Glob.class));
    }

    /* Because generic functions are too nice a thing for Java. */
    @Override
    public double getv() {
        Moving m = getattr(Moving.class);
        if (m == null)
            return (0);
        return (m.getv());
    }

    @Override
    public Collection<Location.Chain> getloc() {
        return (lastChain);
    }

    public boolean isplayer() {
        try { //not clean but when multi-sessioning client can crash here when second client is booting.
            final UI ui = glob.ui.get();
            return ui.gui.map.plgob == id;
        } catch (Throwable e) {
            return false;
        }
    }

    public boolean isMoving() {
//        if (getattr(LinMove.class) != null)
//            return (true);
//
//        Following follow = getattr(Following.class);
//        if (follow != null && follow.tgt() != null && follow.tgt().getattr(LinMove.class) != null)
//            return (true);
//
//        Homing homing = getattr(Homing.class);
//        if (homing != null && homing.tgt() != null && homing.tgt().getattr(LinMove.class) != null)
//            return (true);

        Moving m = getattr(Moving.class);
        return (m != null && m.isMoving());
        // when player is riding a horse, player is only moving if the ridden horse is also moving
        //if (getattr(Moving.class) != null) {
        //    Gob riddenGob = findRiddenGob();
        //    return riddenGob == null || riddenGob.isMoving();
        //}
    }

    public Gob findFollowingGob() {
        HeldBy hb = getattr(HeldBy.class);
        return (hb != null ? hb.tgt() : null);
    }

    public Gob findFollowedGob() {
        Holding h = getattr(Holding.class);
        return (h != null ? h.tgt() : null);
    }

    public Collection<Gob> findChainedGobs() {
        /*Following follow = getattr(Following.class);
        if (follow == null || follow.tgt() == null || follow.tgt().rc.dist((rc)) >= 0.1) {
            return null;
        } else {
            return follow.tgt();
        }*/
        Set<Gob> set = new HashSet<>();
        Gob hb = findFollowingGob();
        if (hb != null) set.add(hb);
        Gob h = findFollowedGob();
        if (h != null) set.add(h);
        return (set);
    }

    public LinMove getLinMove() {
        LinMove lm = getattr(LinMove.class);
        if (lm != null)
            return (lm);

        Following follow = getattr(Following.class);
        if (follow != null)
            return (follow.tgt().getattr(LinMove.class));

        Homing homing = getattr(Homing.class);
        if (homing != null)
            return (homing.tgt().getattr(LinMove.class));

        return (null);
    }

    public boolean isFriend() {
        synchronized (glob.party.memb) {
            for (Party.Member m : glob.party.memb.values()) {
                if (m.gobid == id)
                    return true;
            }
        }

        Buddy buddy = getattr(Buddy.class);
        if (buddy == null || buddy.group() == 2 /*red*/)
            return false;

        return true;
    }

    public int getStage() {
        try {
            Resource res = getres();
            if (res != null && res.name.startsWith("gfx/terobjs/plants") && !res.name.endsWith("trellis")) {
                GAttrib rd = getattr(ResDrawable.class);
                final int stage = ((ResDrawable) rd).sdt.peekrbuf(0);
                return stage;
            } else
                return -1;
        } catch (Loading l) {
            return -1;
        }
    }


    public static void setFullStorageColor(final Color color) {
        Utils.setprefi("storagefullcolor", color.getRGB());
        storagefullcolormaterial.set(new Material.Colors(color));
    }

    public static void setEmptyStorageColor(final Color color) {
        Utils.setprefi("storageemptycolor", color.getRGB());
        storageemptycolormaterial.set(new Material.Colors(color));
    }

    public static void setHalfStorageColor(final Color color) {
        Utils.setprefi("storagehalfcolor", color.getRGB());
        storagehalfcolormaterial.set(new Material.Colors(color));
    }

    public static void setEmptyBarrelColor(final Color color) {
        Utils.setprefi("barrelemptycolor", color.getRGB());
        barrelemptycolormaterial.set(new Material.Colors(color));
    }

    public static void setEmptyTroughColor(final Color color) {
        Utils.setprefi("troughemptycolor", color.getRGB());
        troughemptycolormaterial.set(new Material.Colors(color));
    }

    public static void setHalfTroughColor(final Color color) {
        Utils.setprefi("troughhalfcolor", color.getRGB());
        troughhalfcolormaterial.set(new Material.Colors(color));
    }

    public static void setFullTroughColor(final Color color) {
        Utils.setprefi("troughfullcolor", color.getRGB());
        troughfullcolormaterial.set(new Material.Colors(color));
    }

    public static void setFullBeehiveColor(final Color color) {
        Utils.setprefi("beehivefullcolor", color.getRGB());
        beehivefullcolormaterial.set(new Material.Colors(color));
    }

    public static Color getFullStorageColor() {
        return (new Color(Utils.getprefi("storagefullcolor", new Color(255, 0, 0, 230).getRGB()), true));
    }

    public static Color getEmptyStorageColor() {
        return (new Color(Utils.getprefi("storageemptycolor", new Color(0, 255, 0, 230).getRGB()), true));
    }

    public static Color getEmptyBarrelColor() {
        return (new Color(Utils.getprefi("barrelemptycolor", new Color(0, 255, 0, 230).getRGB()), true));
    }

    public static Color getHalfStorageColor() {
        return (new Color(Utils.getprefi("storagehalfcolor", new Color(255, 255, 0, 230).getRGB()), true));
    }

    public static Color getEmptyTroughColor() {
        return (new Color(Utils.getprefi("troughemptycolor", new Color(255, 0, 0, 230).getRGB()), true));
    }

    public static Color getHalfTroughColor() {
        return (new Color(Utils.getprefi("troughhalfcolor", new Color(255, 0, 255, 230).getRGB()), true));
    }

    public static Color getFullTroughColor() {
        return (new Color(Utils.getprefi("troughfullcolor", new Color(0, 255, 0, 230).getRGB()), true));
    }

    public static Color getFullBeehiveColor() {
        return (new Color(Utils.getprefi("beehivefullcolor", new Color(0, 255, 0, 230).getRGB()), true));
    }

    private static final List<StorageMaterial> storageMaterialList = new ArrayList<>();

    static {
        storageMaterialList.add(new StorageMaterial("/jotunclam", new Pair<>(113, storagefullcolormaterial), new Pair<>(114, storagefullcolormaterial), new Pair<>(1, storageemptycolormaterial), new Pair<>(2, storageemptycolormaterial), new Pair<>(-1, storagehalfcolormaterial)));
        storageMaterialList.add(new StorageMaterial("/cupboard", new Pair<>(29, storagefullcolormaterial), new Pair<>(30, storagefullcolormaterial), new Pair<>(1, storageemptycolormaterial), new Pair<>(2, storageemptycolormaterial), new Pair<>(-1, storagehalfcolormaterial)));
        storageMaterialList.add(new StorageMaterial("/metalcabinet", new Pair<>(65, storagefullcolormaterial), new Pair<>(66, storagefullcolormaterial), new Pair<>(1, storageemptycolormaterial), new Pair<>(2, storageemptycolormaterial), new Pair<>(-1, storagehalfcolormaterial)));
        storageMaterialList.add(new StorageMaterial("/chest", new Pair<>(29, storagefullcolormaterial), new Pair<>(30, storagefullcolormaterial), new Pair<>(1, storageemptycolormaterial), new Pair<>(2, storageemptycolormaterial), new Pair<>(-1, storagehalfcolormaterial)));
        storageMaterialList.add(new StorageMaterial("/exquisitechest", new Pair<>(29, storagefullcolormaterial), new Pair<>(30, storagefullcolormaterial), new Pair<>(1, storageemptycolormaterial), new Pair<>(2, storageemptycolormaterial), new Pair<>(-1, storagehalfcolormaterial)));
        storageMaterialList.add(new StorageMaterial("/crate", new Pair<>(16, storagefullcolormaterial), new Pair<>(0, storageemptycolormaterial), new Pair<>(-1, storagehalfcolormaterial)));
        storageMaterialList.add(new StorageMaterial("/largechest", new Pair<>(17, storagefullcolormaterial), new Pair<>(18, storagefullcolormaterial), new Pair<>(1, storageemptycolormaterial), new Pair<>(2, storageemptycolormaterial), new Pair<>(-1, storagehalfcolormaterial)));
    }

    private static Optional<StorageMaterial> findStorageMaterial(final String resname) {
        return (storageMaterialList.stream().filter(sm -> resname.endsWith(sm.resname)).findFirst());
    }

    private static class StorageMaterial {
        private final String resname;
        private final Map<Integer, AtomicReference<GLState>> map = new HashMap<>();

        private StorageMaterial(final String resname, Pair<Integer, AtomicReference<GLState>>... entries) {
            this.resname = resname;
            Arrays.stream(entries).forEach(e -> map.put(e.a, e.b));
        }

        private Optional<GLState> getColor(final int sdt) {
            AtomicReference<GLState> mc = map.get(sdt);
            if (mc == null && Config.showpartialstoragestatus)
                mc = map.get(-1);
            return (mc == null ? Optional.empty() : Optional.ofNullable(mc.get()));
        }
    }

    public static class DataLoading extends Loading {
        public final transient Gob gob;
        public final int updseq;

        /* It would be assumed that the caller has synchronized on gob
         * while creating this exception. */
        public DataLoading(Gob gob, String message) {
            super(message);
            this.gob = gob;
            this.updseq = gob.updateseq;
        }

        @Override
        public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
            synchronized (gob) {
                if (gob.updateseq != this.updseq) {
                    reg.accept(Waitable.Waiting.dummy);
                    callback.run();
                } else {
                    gob.updwait(callback, reg);
                }
            }
        }
    }

    public int updateseq = 0;
    private Waitable.Queue updwait = null;

    void updated() {
        synchronized (this) {
            updateseq++;
            if (updwait != null)
                updwait.wnotify();
        }
    }

    public void updwait(Runnable callback, Consumer<Waitable.Waiting> reg) {
        /* Caller should probably synchronize on this already for a
         * call like this to even be meaningful, but just in case. */
        synchronized (this) {
            if (updwait == null)
                updwait = new Waitable.Queue();
            reg.accept(updwait.add(callback));
        }
    }

    void removed() {
        removed = true;
    }
}
