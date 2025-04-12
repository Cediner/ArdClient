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

import haven.MapView.ClickInfo;
import haven.Skeleton.Pose;
import haven.Skeleton.PoseMod;
import haven.sloth.gob.Type;
import modification.configuration;
import modification.resources;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class Composited implements Rendered, MapView.Clickable, EquipTarget {
    public final Skeleton skel;
    public final Pose pose;
    private final PoseMorph morph;
    public Collection<Model> mod = new LinkedList<>();
    public Collection<Equ> equ = new LinkedList<>();
    public Poses poses = new Poses();
    public List<MD> nmod = null;
    public List<MD> cmod = new LinkedList<>();
    public List<ED> nequ = null;
    public List<ED> cequ = new LinkedList<>();
    public Sprite.Owner eqowner = null;

    public class Poses {
        public final PoseMod[] mods;
        Pose old;
        float ipold = 0.0f;
        float ipol = 0.0f;
        public float limit = -1.0f;
        public boolean stat;
        public boolean ldone;
        public boolean finished;
        private Random srnd = new Random();
        private float rsmod = (srnd.nextFloat() * 0.1f) + 0.95f;

        public Poses() {
            this.mods = new PoseMod[0];
        }

        public Poses(List<? extends PoseMod> mods) {
            this.mods = mods.toArray(new PoseMod[0]);
            stat = true;
            for (PoseMod mod : this.mods) {
                if (!mod.stat()) {
                    stat = false;
                    break;
                }
            }
        }

        private void rebuild() {
            pose.reset();
            for (PoseMod m : mods)
                m.apply(pose);
            if (ipold > 0.0f)
                pose.blend(old, ipold);
            pose.gbuild();
        }

        public void set(float ipol) {
            if ((this.ipol = ipol) > 0) {
                this.old = skel.new Pose(pose);
                this.ipold = 1.0f;
            }
            Composited.this.poses = this;
            rebuild();
        }

        public void tick(float dt) {
            rsmod = Utils.clip(rsmod + (srnd.nextFloat() * 0.005f) - 0.0025f, 0.90f, 1.10f);
            dt *= rsmod;
            boolean build = false;
            if (limit >= 0) {
                if ((limit -= dt) < 0)
                    ldone = true;
            }
            boolean done = ldone;
            for (PoseMod m : mods) {
                m.tick(dt);
                if (!m.done())
                    done = false;
            }
            if (!stat)
                build = true;
            if (ipold > 0.0f) {
                if ((ipold -= (dt / ipol)) < 0.0f) {
                    ipold = 0.0f;
                    old = null;
                }
                build = true;
            }
            if (build)
                rebuild();
            if (done)
                done();
            finished = done && ldone;
        }

        @Deprecated
        public void tick(float dt, double v) {
            tick(dt);
        }

        protected void done() {}

        /**
         * Poses are where most of the animation is.
         * Could be static when `done`, otherwise no.
         */
        public Object staticp() {
            for (PoseMod m : mods) {
                if (!m.done())
                    return null;
            }
            return Gob.STATIC;
        }
    }

    public Composited(Skeleton skel) {
        this.skel = skel;
        this.pose = skel.new Pose(skel.bindpose);
        this.morph = new PoseMorph(pose);
    }

    private static final Rendered.Order modorder = new Rendered.Order<Model.Layer>() {
        @Override
        public int mainz() {
            return (1);
        }

        private final Rendered.RComparator<Model.Layer> cmp = (a, b, sa, sb) -> {
            if (a.z1 != b.z1)
                return (a.z1 - b.z1);
            return (a.z2 - b.z2);
        };

        @Override
        public Rendered.RComparator<Model.Layer> cmp() {
            return (cmp);
        }
    };

    /**
     * MorphedMesh static or not? Lets try static
     */
    public class Model implements Rendered {
        public final MorphedMesh m;
        public final int id;
        public final List<Layer> lay = new ArrayList<>();
        int z = 0, lz = 0;

        public class Layer implements FRendered {
            private final Material mat;
            private final int z1, z2;

            private Layer(Material mat, int z1, int z2) {
                this.mat = mat;
                this.z1 = z1;
                this.z2 = z2;
            }

            @Override
            public void draw(GOut g) {
                m.draw(g);
            }

            @Override
            public void drawflat(GOut g) {
                if (z2 == 0)
                    m.drawflat(g);
            }

            @Override
            public boolean setup(RenderList r) {
                r.prepo(modorder);
                r.prepo(mat);
                return (true);
            }
        }

        private Model(FastMesh m, int id) {
            this.m = new MorphedMesh(m, morph);
            this.id = id;
        }

        private void addlay(Material mat) {
            lay.add(new Layer(mat, z, lz++));
        }

        @Override
        public void draw(GOut g) {}

        @Override
        public boolean setup(RenderList r) {
            m.setup(r);
            for (Layer lay : this.lay) {
                for (GLState gs : lay.mat.states) {
                    try {
                        if (gs instanceof TexGL.TexDraw) {
                            String s = gs.toString().substring("TexDraw(TexR(".length(), gs.toString().lastIndexOf(','));
                            if (resources.painedcloth.get(s) == null) {
                                resources.painedcloth.put(s, false);
                                Utils.saveCustomList(resources.painedcloth, "PaintedClothList");
                            }

                            if (resources.paintcloth) {
                                resources.paintcloth(s, r);
                            }
                        }
                    } catch (Throwable e) {}
                }
                r.add(lay, null);
            }
            return (false);
        }

        @Override
        public Object staticp() {
            return null;
        }
    }

    public class SpriteEqu extends Equ {
        private final Sprite spr;

        private SpriteEqu(ED ed) {
            super(ed);
            this.spr = Sprite.create(eqowner, ed.res.res.get(), ed.res.sdt.clone());
        }

        @Override
        public void draw(GOut g) {
        }

        @Override
        public boolean setup(RenderList rl) {
            rl.add(spr, null);
            return (false);
        }

        @Override
        public void tick(int dt) {
            spr.tick(dt);
        }

        @Override
        public Object staticp() {
            return spr.staticp();
        }
    }

    public class LightEqu extends Equ {
        private final Light l;

        private LightEqu(ED ed) {
            super(ed);
            this.l = ed.res.res.get().layer(Light.Res.class).make();
        }

        @Override
        public void draw(GOut g) {
        }

        @Override
        public boolean setup(RenderList rl) {
            rl.add(l, null);
            return (false);
        }

        @Override
        public Object staticp() {
            return l.staticp();
        }
    }

    /**
     * Equ are equipment ontop of the Skeleton or lighting for whatever reason. Always static at this level
     * Classes that extend this will likely override based off what they do
     */
    public abstract class Equ implements Rendered {
        private final GLState et;
        public final ED desc;
        public final int id;
        private boolean matched;

        private Equ(ED ed) {
            this.desc = ed.clone();
            this.id = desc.id;
            GLState bt = null;
            if (bt == null) {
                Skeleton.BoneOffset bo = ed.res.res.get().layer(Skeleton.BoneOffset.class, ed.at);
                if (bo != null)
                    bt = bo.forpose(pose);
            }
            if ((bt == null) && (skel instanceof Skeleton.ResourceSkeleton)) {
                Skeleton.BoneOffset bo = ((Skeleton.ResourceSkeleton) skel).res.layer(Skeleton.BoneOffset.class, ed.at);
                if (bo != null)
                    bt = bo.forpose(pose);
            }
            if (bt == null) {
                Skeleton.Bone bone = skel.bones.get(ed.at);
                if (bone != null)
                    bt = pose.bonetrans(bone.idx);
            }
            if ((bt == null) && !ed.at.equals(""))
                bt = new Material();
                /*throw (new RuntimeException("Transformation " + ed.at + " for equipment " + ed.res + " on skeleton " + skel + " could not be resolved"));*/
            if ((ed.off.x != 0.0f) || (ed.off.y != 0.0f) || (ed.off.z != 0.0f))
                this.et = GLState.compose(bt, Location.xlate(ed.off));
            else
                this.et = bt;
        }

        public void tick(int dt) {}

        @Override
        public Object staticp() {
            return Gob.STATIC;
        }
    }

    public static class MD implements Cloneable {
        public Indir<Resource> mod;
        public List<ResData> tex;
        public int id = -1;
        private Model real;

        public MD(Indir<Resource> mod, List<ResData> tex) {
            this.mod = mod;
            this.tex = tex;
        }

        public boolean equals(Object o) {
            if (!(o instanceof MD))
                return (false);
            MD m = (MD) o;
            return (mod.equals(m.mod) && tex.equals(m.tex));
        }

        @Override
        public MD clone() {
            try {
                MD ret = (MD) super.clone();
                ret.tex = new ArrayList<>(tex);
                ret.real = null;
                return (ret);
            } catch (CloneNotSupportedException e) {
                /* This is ridiculous. */
                throw (new RuntimeException(e));
            }
        }

        public String toString() {
            return (mod + "+" + tex);
        }
    }

    public static class ED implements Cloneable {
        public int t;
        public int id = -1;
        public String at;
        public ResData res;
        public Coord3f off;
        private Equ real;

        public ED(int t, String at, ResData res, Coord3f off) {
            this.t = t;
            this.at = at;
            this.res = res;
            this.off = off;
        }

        public boolean equals(Object o) {
            if (!(o instanceof ED))
                return (false);
            ED e = (ED) o;
            return ((t == e.t) && at.equals(e.at) && res.equals(e.res) && off.equals(e.off));
        }

        public boolean equals2(Object o) {
            if (!(o instanceof ED))
                return (false);
            ED e = (ED) o;
            return ((t == e.t) && at.equals(e.at) && res.res.equals(e.res.res) && off.equals(e.off));
        }

        @Override
        public ED clone() {
            try {
                ED ret = (ED) super.clone();
                ret.res = res.clone();
                ret.real = null;
                return (ret);
            } catch (CloneNotSupportedException e) {
                /* This is ridiculous. */
                throw (new RuntimeException(e));
            }
        }

        public String toString() {
            return (String.format("<ED: %d \"%s\" %s(%s) %s>", t, at, res.res, res.sdt, off));
        }
    }

    public static class Desc implements Cloneable {
        public Indir<Resource> base;
        public List<MD> mod = new ArrayList<>();
        public List<ED> equ = new ArrayList<>();

        public Desc() {
        }

        public Desc(Indir<Resource> base) {
            this.base = base;
        }

        public static Desc decode(Session sess, Object[] args) {
            Desc ret = new Desc();
	    ret.base = sess.getresv(args[0]);
            Object[] ma = (Object[]) args[1];
            for (int i = 0; i < ma.length; i += 2) {
                List<ResData> tex = new ArrayList<ResData>();
		Indir<Resource> mod = sess.getresv(ma[i]);
                Object[] ta = (Object[]) ma[i + 1];
                for (int o = 0; o < ta.length; o++) {
		    Indir<Resource> tr = sess.getresv(ta[o]);
                    Message sdt = Message.nil;
                    if ((ta.length > o + 1) && (ta[o + 1] instanceof byte[]))
                        sdt = new MessageBuf((byte[]) ta[++o]);
                    tex.add(new ResData(tr, sdt));
                }
                ret.mod.add(new MD(mod, tex));
            }
            Object[] ea = (Object[]) args[2];
            for (int i = 0; i < ea.length; i++) {
                Object[] qa = (Object[]) ea[i];
                int n = 0;
		int t = Utils.iv(qa[n++]);
                String at = (String) qa[n++];
		Indir<Resource> res = sess.getresv(qa[n++]);
                Message sdt = Message.nil;
                if (qa[n] instanceof byte[])
                    sdt = new MessageBuf((byte[]) qa[n++]);
		Coord3f off = new Coord3f(Utils.fv(qa[n + 0]), Utils.fv(qa[n + 1]), Utils.fv(qa[n + 2]));
                ret.equ.add(new ED(t, at, new ResData(res, sdt), off));
            }
            return (ret);
        }

        @Override
        public Desc clone() {
            Desc ret = new Desc(base);
            for (MD mod : this.mod)
                ret.mod.add(mod.clone());
            for (ED equ : this.equ)
                ret.equ.add(equ.clone());
            return (ret);
        }

        public String toString() {
            return (String.format("desc(%s, %s, %s)", base, mod, equ));
        }
    }

    private final Material.Owner matowner = new Material.Owner() {
        @Override
        public <T> T context(Class<T> cl) {
            if (eqowner == null)
                throw (new NoContext(cl));
            return (eqowner.context(cl));
        }
    };

    private void nmod(boolean nocatch) {
        for (Iterator<MD> i = nmod.iterator(); i.hasNext(); ) {
            MD md = i.next();
            try {
                if (md.real == null) {
                    FastMesh.MeshRes mr = md.mod.get().layer(FastMesh.MeshRes.class);
                    if (mr == null)
                        throw (new Sprite.ResourceException("Model resource contains no mesh", md.mod.get()));
                    md.real = new Model(mr.m, md.id);
                    if(mr.rdat.containsKey("cz"))
                        md.real.z = Integer.parseInt(mr.rdat.get("cz"));
                    /* This is really ugly, but I can't really think of
                     * anything less ugly right now. */
                    if (md.mod.get().name.equals("gfx/borka/male") || md.mod.get().name.equals("gfx/borka/female"))
                        md.real.z = -1;
                    this.mod.add(md.real);
                }
                for (Iterator<ResData> o = md.tex.iterator(); o.hasNext(); ) {
                    ResData res = o.next();
                    md.real.addlay(Material.fromres(matowner, res.res.get(), new MessageBuf(res.sdt)));
                    o.remove();
                }
                i.remove();
            } catch (Loading e) {
                if (nocatch)
                    throw (e);
            }
        }
        if (nmod.isEmpty())
            nmod = null;
    }

    private void nequ(boolean nocatch) {
        outer:
        for (Iterator<ED> i = nequ.iterator(); i.hasNext(); ) {
            ED ed = i.next();
            try {
                Equ prev = null;
                for (Equ equ : this.equ) {
                    if (equ.desc.equals(ed)) {
                        equ.matched = true;
                        i.remove();
                        continue outer;
                    } else if ((equ instanceof SpriteEqu) && (((SpriteEqu) equ).spr instanceof Gob.Overlay.CUpd || ((SpriteEqu) equ).spr instanceof Sprite.CUpd) && equ.desc.equals2(ed)) {
                        if (((SpriteEqu) equ).spr instanceof Sprite.CUpd)
                            ((Sprite.CUpd) ((SpriteEqu) equ).spr).update(ed.res.sdt.clone());
                        else if (((SpriteEqu) equ).spr instanceof Gob.Overlay.CUpd)
                            ((Gob.Overlay.CUpd) ((SpriteEqu) equ).spr).update(ed.res.sdt.clone());
                        equ.desc.res.sdt = ed.res.sdt;
                        equ.matched = true;
                        i.remove();
                        continue outer;
                    }
                }
                Equ ne;
                if (ed.t == 0)
                    ne = new SpriteEqu(ed);
                else if (ed.t == 1)
                    ne = new LightEqu(ed);
                else
                    throw (new RuntimeException("Invalid composite equ-type: " + ed.t));
                ne.matched = true;
                this.equ.add(ne);
                i.remove();
            } catch (Loading e) {
                if (nocatch)
                    throw (e);
            }
        }
        if (nequ.isEmpty()) {
            nequ = null;
            this.equ.removeIf(equ -> !equ.matched);
        }
    }

    public void changes(boolean nocatch) {
        if (nmod != null)
            nmod(nocatch);
        if (nequ != null)
            nequ(nocatch);
    }

    public void changes() {
        changes(false);
    }

    @Override
    public Object[] clickargs(ClickInfo inf) {
        Rendered[] st = inf.array();
        for (int g = 0; g < st.length; g++) {
            if (st[g] instanceof Gob) {
                Gob gob = (Gob) st[g];
                Object[] ret = {0, (int) gob.id, gob.rc.floor(OCache.posres), 0, 0};
                int id = 0;
                for (int i = g - 1; i >= 0; i--) {
                    if (st[i] instanceof Model) {
                        Model mod = (Model) st[i];
                        if (mod.id >= 0)
                            id = 0x01000000 | ((mod.id & 0xff) << 8);
                    } else if (st[i] instanceof Equ) {
                        Equ equ = (Equ) st[i];
                        if (equ.id >= 0)
                            id = 0x02000000 | ((equ.id & 0xff) << 16);
                    } else if (st[i] instanceof FastMesh.ResourceMesh) {
                        FastMesh.ResourceMesh rm = (FastMesh.ResourceMesh) st[i];
                        if ((id & 0xff000000) == 0x02000000)
                            id = (id & 0xffff0000) | (rm.id & 0xffff);
                    }
                }
                ret[4] = id;
                inf.gob = gob;
                return (ret);
            }
        }
        return (new Object[0]);
    }

    /*
    private static class CompositeClick extends ClickInfo {
	CompositeClick(ClickInfo prev, Integer id, Rendered r) {
	    super(prev, id, r);
        }

        public ClickInfo include(Rendered r) {
            int id = (this.id == null) ? 0 : this.id;
            if (r instanceof Model) {
                Model mod = (Model) r;
                if (mod.id >= 0)
		    return(new CompositeClick(this, 0x01000000 | ((mod.id & 0xff) << 8), r));
            } else if (r instanceof Equ) {
                Equ equ = (Equ) r;
                if (equ.id >= 0)
		    return(new CompositeClick(this, 0x02000000 | ((equ.id & 0xff) << 16), r));
            } else if (r instanceof FastMesh.ResourceMesh) {
                FastMesh.ResourceMesh rm = (FastMesh.ResourceMesh) r;
                if ((id & 0xff000000) == 2)
		    return(new CompositeClick(this, id & 0xffff0000 | (rm.id & 0xffff), r));
            }
            return (this);
        }
    }

    public ClickInfo clickinfo(Rendered self, ClickInfo prev) {
	return(new CompositeClick(prev, null, self));
    }
    */

    @Override
    public boolean setup(RenderList rl) {
//        try {
        changes();
        for (Model mod : this.mod)
            rl.add(mod, null);
        for (Equ equ : this.equ)
            rl.add(equ, equ.et);
//        } catch (Exception e) {}
        return (false);
    }

    @Override
    public GLState eqpoint(String nm, Message dat) {
        return(pose.eqpoint(nm, dat));
    }

    @Override
    public void draw(GOut g) {}

    private final AtomicLong ticktime = new AtomicLong(System.currentTimeMillis());
    private int buffertime = 0;

    public void tick(int dt) {
        if (configuration.allowAnim(ticktime)) {
            if (poses != null)
                poses.tick((buffertime + dt) / 1000.0f);
            for (Equ equ : this.equ)
                equ.tick(buffertime + dt);
            buffertime = 0;
        } else {
            buffertime += dt;
        }
    }

    @Override
    public Object staticp() {
        Gob compowner = null;
        if (eqowner instanceof Gob)
            compowner = (Gob) eqowner;
        if (eqowner instanceof Avaview.AvaOwner)
            return null;
        if (!configuration.disableAnimation(compowner) || (compowner != null && compowner.type == Type.HUMAN)) {
            Object stat = poses.staticp();
            if (stat != null) {
                for (Equ equ : this.equ) {
                    stat = equ.staticp();
                    if (stat != null)
                        continue;
                    break;
                }
            }
            return stat;
        } else {
            return Gob.STATIC;
        }
    }

    public void chmod(List<MD> mod) {
        if (mod.equals(cmod))
            return;
        this.mod = new LinkedList<>();
        nmod = new LinkedList<>();
        for (MD md : mod)
            nmod.add(md.clone());
        cmod = new ArrayList<>(mod);
    }

    public void chequ(List<ED> equ) {
        if (equ.equals(cequ))
            return;
        for (Equ oequ : this.equ)
            oequ.matched = false;
        nequ = new LinkedList<>();
        for (ED ed : equ)
            nequ.add(ed.clone());
        cequ = new ArrayList<>(equ);
    }
}
