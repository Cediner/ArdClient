/* Preprocessed source code */
package haven.res.lib.uspr;

import haven.FastMesh;
import haven.GLState;
import haven.Gob;
import haven.Loading;
import haven.MeshAnim;
import haven.Message;
import haven.MorphedMesh;
import haven.MorphedMesh.Morpher;
import haven.PoseMorph;
import haven.RenderLink;
import haven.RenderList;
import haven.Rendered;
import haven.Resource;
import haven.SkelSprite;
import haven.Skeleton;
import haven.Skeleton.Pose;
import haven.Skeleton.PoseMod;
import haven.Sprite;
import modification.configuration;
import modification.dev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/* >spr: haven.res.lib.uspr.UnivSprite */
public class UnivSprite extends Sprite implements Gob.Overlay.CUpd, Skeleton.HasPose {
    static {
        dev.checkFileVersion("lib/uspr", 16);
    }

    public static final float ipollen = 0.3f;
    public final Skeleton skel;
    public final Pose pose;
    public PoseMod[] mods = new PoseMod[0];
    public MeshAnim.Anim[] manims = new MeshAnim.Anim[0];
    private Morpher.Factory mmorph;
    private final PoseMorph pmorph;
    private Pose oldpose;
    private float ipold;
    private boolean stat = true;
    private Rendered[] parts;
    private int fl;
    private boolean loading = false;

    public UnivSprite(Owner owner, Resource res, Message sdt) {
        super(owner, res);
        Skeleton.Res sr = res.layer(Skeleton.Res.class);
        if (sr != null) {
            skel = sr.s;
            pose = skel.new Pose(skel.bindpose);
            pmorph = new PoseMorph(pose);
        } else {
            skel = null;
            pose = null;
            pmorph = null;
        }
        fl = sdt.eom() ? 0xffff0000 : decnum(sdt);
        update(true);
    }

    public Rendered animmesh(FastMesh mesh) {
        for (MeshAnim.Anim anim : manims) {
            if (anim.desc().animp(mesh)) {
                Rendered ret = new MorphedMesh(mesh, mmorph);
                if (SkelSprite.bonedb)
                    ret = SkelSprite.morphed.apply(ret);
                return (ret);
            }
        }
        Rendered ret;
        if (PoseMorph.boned(mesh)) {
            String bnm = PoseMorph.boneidp(mesh);
            if (bnm == null) {
                ret = new MorphedMesh(mesh, pmorph);
                if (SkelSprite.bonedb)
                    ret = SkelSprite.morphed.apply(ret);
            } else {
                ret = pose.bonetrans2(skel.bones.get(bnm).idx).apply(mesh);
                if (SkelSprite.bonedb)
                    ret = SkelSprite.rigid.apply(ret);
            }
        } else {
            ret = mesh;
            if (SkelSprite.bonedb)
                ret = SkelSprite.unboned.apply(ret);
        }
        return (ret);
    }

    public Rendered animwrap(Rendered r) {
        if (r instanceof FastMesh)
            return (animmesh((FastMesh) r));
        if (r instanceof GLState.Wrapping) {
            GLState.Wrapping wrap = (GLState.Wrapping) r;
            Rendered nr = animwrap(wrap.r);
            if (nr == wrap.r)
                return (wrap);
            return (wrap.st().apply(nr));
        }
        return (r);
    }

    public Collection<Rendered> iparts(int mask) {
        Collection<Rendered> rl = new ArrayList<Rendered>();
        for (FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
            try {
                if ((mr.mat != null) && ((mr.id < 0) || (((1 << mr.id) & mask) != 0)))
                    rl.add(mr.mat.get().apply(animmesh(mr.m)));
            } catch (Loading e) {
                throw (e);
            } catch (Throwable e) {
                dev.simpleLog(e);
            }
        }
        return (rl);
    }

    private void chparts(int mask) {
        Collection<Rendered> rl = new ArrayList<Rendered>();
        for (Rendered r : iparts(mask))
            rl.add(r);
        for (RenderLink.Res lr : res.layers(RenderLink.Res.class)) {
            try {
                if ((lr.id < 0) || (((1 << lr.id) & mask) != 0)) {
                    Rendered r = lr.l.make(owner);
                    if (r instanceof GLState.Wrapping)
                        r = animwrap(r);
                    rl.add(r);
                }
            } catch (Loading e) {
                throw (e);
            } catch (Throwable e) {
                dev.simpleLog(e);
            }
        }
        this.parts = rl.toArray(new Rendered[0]);
    }

    private void rebuild() {
        if (skel != null) {
            pose.reset();
            for (PoseMod m : mods)
                m.apply(pose);
            if (ipold > 0) {
                float f = ipold * ipold * (3 - (2 * ipold));
                pose.blend(oldpose, f);
            }
            pose.gbuild();
        }
    }

    private void chmanims(int mask) {
        Collection<MeshAnim.Anim> anims = new LinkedList<MeshAnim.Anim>();
        for (MeshAnim.Res ar : res.layers(MeshAnim.Res.class)) {
            try {
                if ((ar.id < 0) || (((1 << ar.id) & mask) != 0))
                    anims.add(ar.make());
            } catch (Loading e) {
                throw (e);
            } catch (Throwable e) {
                dev.simpleLog(e);
            }
        }
        this.manims = anims.toArray(new MeshAnim.Anim[0]);
        this.mmorph = MorphedMesh.combine(this.manims);
    }

    private Map<Skeleton.ResPose, PoseMod> modids = new HashMap<Skeleton.ResPose, PoseMod>();

    private void chposes(int mask, boolean old) {
        if (!old) {
            this.oldpose = skel.new Pose(pose);
            this.ipold = 1.0f;
        }
        Collection<PoseMod> poses = new LinkedList<PoseMod>();
        stat = true;
        Skeleton.ModOwner mo = (owner instanceof Skeleton.ModOwner) ? (Skeleton.ModOwner) owner : Skeleton.ModOwner.nil;
        Map<Skeleton.ResPose, PoseMod> newids = new HashMap<Skeleton.ResPose, PoseMod>();
        for (Skeleton.ResPose p : res.layers(Skeleton.ResPose.class)) {
            if ((p.id < 0) || ((mask & (1 << p.id)) != 0)) {
                PoseMod mod;
                if ((mod = modids.get(p)) == null) {
                    mod = p.forskel(mo, skel, p.defmode);
                    if (old)
                        mod.age();
                }
                if (p.id >= 0)
                    newids.put(p, mod);
                if (!mod.stat())
                    stat = false;
                poses.add(mod);
            }
        }
        this.mods = poses.toArray(new PoseMod[0]);
        this.modids = newids;
        rebuild();
    }

    private void update(boolean old) {
        chmanims(fl);
        if (skel != null)
            chposes(fl, old);
        chparts(fl);
        constant = new Gob.Static();
    }

    public void update() {
        try {
            update(false);
        } catch (Loading l) {
            loading = true;
        }
    }

    public void update(int fl) {
        this.fl = fl;
        update();
    }

    public void update(Message sdt) {
        fl = sdt.eom() ? 0xffff0000 : decnum(sdt);
        update();
    }

    public boolean setup(RenderList rl) {
        for (Rendered p : parts)
            rl.add(p, null);
        /* rl.add(pose.debug, null); */
        return (false);
    }

    public boolean tick(int idt) {
        float dt = idt / 1000.0f;
        if (loading) {
            loading = false;
            update();
        }
        if (!stat || (ipold > 0)) {
            boolean done = true;
            for (PoseMod m : mods) {
                m.tick(dt);
                done = done && m.done();
            }
            if (done)
                stat = true;
            if (ipold > 0) {
                if ((ipold -= (dt / ipollen)) < 0) {
                    ipold = 0;
                    oldpose = null;
                }
            }
            rebuild();
        }
        for (MeshAnim.Anim anim : manims)
            anim.tick(dt);
        return (false);
    }

    private static final Object semistat;

    static {
        Object ss;
        try {
            ss = Gob.SemiStatic.class;
        } catch (NoClassDefFoundError e) {
            ss = CONSTANS;
        }
        semistat = ss;
    }

    private Object constant = new Gob.Static();

    public Object staticp() {
        if (!configuration.disableAnimation(owner)) {
            if (!stat || (manims.length > 0) || (ipold > 0)) {
                return (null);
            } else {
                return ((skel == null) ? constant : semistat);
            }
        } else {
            return constant;
        }
    }

    public Pose getpose() {
        return (pose);
    }
}
