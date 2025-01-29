package haven.res.gfx.terobjs.items.decal;

import haven.Config;
import haven.Coord3f;
import haven.FastMesh;
import haven.GLState;
import haven.GSprite;
import haven.Gob;
import haven.Location;
import haven.MCache;
import haven.Material;
import haven.Message;
import haven.MessageBuf;
import haven.RenderList;
import haven.Rendered;
import haven.Resource;
import haven.Skeleton;
import haven.Sprite;
import haven.States;
import haven.StaticSprite;
import haven.TexL;
import haven.Utils;
import haven.res.lib.itemtex.ItemTex;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

public class Decal implements Sprite.Factory {
    private static boolean xrayVal = getXray();

    public static boolean getXray() {
        return (Utils.getprefb("dacalsxray", false));
    }

    public static void setXray(final boolean val) {
        Utils.setprefb("dacalsxray", xrayVal = val);
    }

    @Override
    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
        GLState eq = null;
        Gob gob = owner.ocontext(Gob.class).orElse(null);
        if (gob != null) {
            Resource ores = gob.getres();
            if (ores != null) {
                Skeleton.BoneOffset bo = ores.layer(Skeleton.BoneOffset.class, "decal");
                if (bo != null)
                    eq = bo.forpose(Skeleton.getpose(gob));
            }
        }
        Material base = res.layer(Material.Res.class, 16).get();
        FastMesh proj = res.layer(FastMesh.MeshRes.class, 0).m;
        Coord3f pc;
        if (sdt.eom()) {
            pc = Coord3f.o;
        } else {
            pc = new Coord3f((float) (sdt.float16() * MCache.tilesz.x), -(float) (sdt.float16() * MCache.tilesz.y), 0);
        }
        if (owner.getres() != null && owner.getres().toString().contains("gfx/terobjs/cupboard") && Config.flatcupboards)
            pc = Coord3f.of(0, 0, 1);
        Location offset = null;
        if (eq == null)
            offset = Location.xlate(pc);
        Material sym = null;
        final AtomicReference<Resource> iconResource = new AtomicReference<>();
        final AtomicReference<GSprite> iconSprite = new AtomicReference<>();
        if (!sdt.eom()) {
            MessageBuf copy = new MessageBuf(sdt).clone();
            iconResource.set(ItemTex.res(owner, copy));
            GSprite sprite = ItemTex.createg(owner, sdt);
            iconSprite.set(sprite);
            BufferedImage img = ItemTex.sprimg(sprite);
            if (img != null) {
                TexL tex = ItemTex.fixup(img);
                sym = new Material(base, tex.draw, tex.clip);
            }
        }
        Rendered[] parts = StaticSprite.lsparts(res, Message.nil);
        if (sym != null)
            parts = Utils.extend(parts, sym.apply(proj));
        Location cpoffset = offset;
        GLState cpeq = eq;
        return (new DecalSprite(owner, res, parts, iconResource.get(), iconSprite.get(), cpoffset, cpeq));
    }

    public static class DecalSprite extends StaticSprite {
        public final Resource iconResource;
        public final GSprite sprite;
        public final Location cpoffset;
        public final GLState cpeq;
        GLState normal;
        GLState xray;

        public DecalSprite(final Owner owner, final Resource res, final Rendered[] parts, final Resource icon, final GSprite sprite, final Location cpoffset, final GLState cpeq) {
            super(owner, res, parts);
            this.iconResource = icon;
            this.sprite = sprite;
            this.cpoffset = cpoffset;
            this.cpeq = cpeq;
            normal = cpeq != null ? cpeq : cpoffset;
            xray = GLState.compose(normal, States.xray);
        }

        @Override
        public boolean setup(RenderList rl) {
            for (int i = 0; i < parts.length; i++) {
                rl.add(parts[i], (i == parts.length - 1 && xrayVal) ? xray : normal);
            }
            return (false);
        }
    }
}
