package haven.res.lib.dynspr;

import haven.FastMesh;
import haven.GLState;
import haven.Indir;
import haven.Material;
import haven.Message;
import haven.Rendered;
import haven.Resource;
import haven.Sprite;
import haven.TexGL;
import haven.TexR;
import haven.UID;

import java.util.Collection;

public class Dyntex implements Sprite.Factory {
    public int matid;
    public int meshid;
    public boolean pflag;

    public Dyntex(int matid, int meshid, boolean pflag) {
        this.matid = matid;
        this.meshid = meshid;
        this.pflag = pflag;
    }

    public Dyntex() {
        this(16, 0, true);
    }

    public Dyntex(Object... args) {
        this();
        for (Object argp : args) {
            Object[] arg = (Object[]) argp;
            switch ((String) arg[0]) {
                case "mat":
                    this.matid = ((Number) arg[1]).intValue();
                    break;
                case "mesh":
                    this.meshid = ((Number) arg[1]).intValue();
                    break;
                case "flagged":
                    pflag = ((Number) arg[1]).intValue() != 0;
                    break;
            }
        }
    }

    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
        Material base = res.layer(Material.Res.class, matid).get();
        FastMesh proj = res.layer(FastMesh.MeshRes.class, meshid).m;
        GLState.Wrapping banner;
        if (!sdt.eom() && (!pflag || (sdt.uint8() == 1))) {
            UID id = sdt.uniqid();
            Indir<Resource> dynres; try {dynres = owner.context(Resource.Resolver.class).dynres(id);} catch (NoSuchMethodError e) {dynres = res.pool.dynres(id);}
            TexGL tex = dynres.get().layer(TexR.class).tex();
            Material sym = new Material(base, tex.draw, tex.clip);
            banner = sym.apply(proj);
        } else {
            banner = null;
        }
        return (new haven.res.lib.vmat.VarSprite(owner, res, Message.nil) {
            public Collection<Rendered> iparts(int mask) {
                Collection<Rendered> parts = super.iparts(mask);
                if (banner != null)
                    parts.add(animwrap(banner));
                return (parts);
            }
        });
    }
}
