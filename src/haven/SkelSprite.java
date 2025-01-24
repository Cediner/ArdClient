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

import haven.MorphedMesh.Morpher;
import haven.Skeleton.Pose;
import haven.Skeleton.PoseMod;
import modification.configuration;
import modification.dev;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SkelSprite extends Sprite implements Gob.Overlay.CUpd, Skeleton.HasPose, Sprite.CUpd {
    public static final GLState
            rigid = new Material.Colors(java.awt.Color.GREEN),
            morphed = new Material.Colors(java.awt.Color.RED),
            unboned = new Material.Colors(java.awt.Color.YELLOW);
    public static boolean bonedb = false;
    public static final float ipollen = 0.3f;
    public final Skeleton skel;
    public final Pose pose;
    public PoseMod[] mods = new PoseMod[0];
    public MeshAnim.Anim[] manims = new MeshAnim.Anim[0];
    public int curfl;
    private Morpher.Factory mmorph;
    private final PoseMorph pmorph;
    private Pose oldpose;
    private float ipold;
    private boolean stat = true;
    private Rendered[] parts;

    public static final Factory fact = new Factory() {
        @Override
        public Sprite create(Owner owner, Resource res, Message sdt) {
            if (res.layer(Skeleton.Res.class) == null)
                return (null);
            return (new SkelSprite(owner, res, sdt));
        }
    };

    public SkelSprite(Owner owner, Resource res, int fl) {
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
        update(fl, true);
    }

    public SkelSprite(Owner owner, Resource res) {
        this(owner, res, 0xffff0000);
    }

    public SkelSprite(Owner owner, Resource res, Message sdt) {
        this(owner, res, sdt.eom() ? 0xffff0000 : decnum(sdt));
    }

    /* XXX: It's ugly to snoop inside a wrapping, but I can't think of
     * a better way to apply morphing to renderlinks right now. */
    private Rendered animwrap(GLState.Wrapping wrap) {
        if (!(wrap.r instanceof FastMesh))
            return (wrap);
        FastMesh m = (FastMesh) wrap.r;
        for (MeshAnim.Anim anim : manims) {
            if (anim.desc().animp(m)) {
                Rendered ret = wrap.st().apply(new MorphedMesh(m, mmorph));
                if (bonedb)
                    ret = morphed.apply(ret);
                return (ret);
            }
        }
        Rendered ret;
        if (PoseMorph.boned(m)) {
            String bnm = PoseMorph.boneidp(m);
            if (bnm == null) {
                ret = wrap.st().apply(new MorphedMesh(m, pmorph));
                if (bonedb)
                    ret = morphed.apply(ret);
            } else {
                ret = pose.bonetrans2(skel.bones.get(bnm).idx).apply(wrap);
                if (bonedb)
                    ret = rigid.apply(ret);
            }
        } else {
            ret = wrap;
            if (bonedb)
                ret = unboned.apply(ret);
        }
        return (ret);
    }

    private void chparts(int mask) {
        Collection<Rendered> rl = new LinkedList<>();
        for (FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
            try {
                if ((mr.mat != null) && ((mr.id < 0) || (((1 << mr.id) & mask) != 0)))
                    rl.add(animwrap(mr.mat.get().apply(mr.m)));
            } catch (Loading e) {
                throw (e);
            } catch (Throwable e) {
                dev.simpleLog(e);
            }
        }
        for (RenderLink.Res lr : res.layers(RenderLink.Res.class)) {
            try {
                if ((lr.id < 0) || (((1 << lr.id) & mask) != 0)) {
                    Rendered r = lr.l.make();
                    if (r instanceof GLState.Wrapping)
                        r = animwrap((GLState.Wrapping) r);
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
        pose.reset();
        for (PoseMod m : mods)
            m.apply(pose);
        if (ipold > 0) {
            float f = ipold * ipold * (3 - (2 * ipold));
            pose.blend(oldpose, f);
        }
        pose.gbuild();
    }

    private void chmanims(int mask) {
        Collection<MeshAnim.Anim> anims = new LinkedList<>();
        for (MeshAnim.Res ar : res.layers(MeshAnim.Res.class)) {
            if ((ar.id < 0) || (((1 << ar.id) & mask) != 0))
                anims.add(ar.make());
        }
        this.manims = anims.toArray(new MeshAnim.Anim[0]);
        this.mmorph = MorphedMesh.combine(this.manims);
    }

    private static final Map<Skeleton.ResPose, PoseMod> initmodids = new HashMap<>();
    private Map<Skeleton.ResPose, PoseMod> modids = initmodids;

    private void chposes(int mask, boolean old) {
        chmanims(mask);
        if (!old) {
            this.oldpose = skel.new Pose(pose);
            this.ipold = 1.0f;
        }
        Collection<PoseMod> poses = new LinkedList<>();
        stat = true;
        Skeleton.ModOwner mo = (owner instanceof Skeleton.ModOwner) ? (Skeleton.ModOwner) owner : Skeleton.ModOwner.nil;
        Map<Skeleton.ResPose, PoseMod> newids = new HashMap<>();
        for (Skeleton.ResPose p : res.layers(Skeleton.ResPose.class)) {
            try {
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
            } catch (Loading e) {
                throw (e);
            } catch (Throwable e) {
                dev.simpleLog(e);
            }
        }
        this.mods = poses.toArray(new PoseMod[0]);
        if ((modids != initmodids) && !modids.equals(newids)) {
            this.oldpose = skel.new Pose(pose);
            this.ipold = 1.0f;
        }
        this.modids = newids;
        rebuild();
    }

    public void update(int fl, boolean old) {
        chmanims(fl);
        if (skel != null)
            chposes(fl, old);
        chparts(fl);
        this.curfl = fl;
    }

    public void update(int fl) {
        update(fl, false);
    }

    public void update() {
        update(curfl);
    }

    @Override
    public void update(Message sdt) {
        int fl = sdt.eom() ? 0xffff0000 : decnum(sdt);
        update(fl);
    }

    @Override
    public boolean setup(RenderList rl) {
        for (Rendered p : parts)
            rl.add(p, null);
        /* rl.add(pose.debug, null); */
        return (false);
    }

    public void age() {
        for (PoseMod mod : mods)
            mod.age();
        for (MeshAnim.Anim anim : manims)
            anim.age();
        this.ipold = 0.0f;
        this.oldpose = null;
    }

    @Override
    public boolean tick(int idt) {
        float dt = idt / 1000.0f;
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

    @Override
    public Object staticp() {
        if (!configuration.disableAnimation(owner)) {
            if (!stat || (manims.length > 0) || (ipold > 0)) {
                return (null);
            } else {
                return (Gob.SemiStatic.class);
            }
        } else {
            return Gob.STATIC;
        }
    }

    @Override
    public Pose getpose() {
        return (pose);
    }

    static {
        Console.setscmd("bonedb", new Console.Command() {
            @Override
            public void run(Console cons, String[] args) {
                bonedb = Utils.parsebool(args[1], false);
            }
        });
    }
}
