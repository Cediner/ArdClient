package haven.res.lib.itemtex;

import haven.CacheMap;
import haven.Coord;
import haven.GSprite;
import haven.Indir;
import haven.Message;
import haven.MessageBuf;
import haven.Mipmapper;
import haven.OwnerContext;
import haven.PUtils;
import haven.Resource;
import haven.Tex;
import haven.TexI;
import haven.TexL;
import haven.res.lib.mapres.ResourceMap;

import javax.media.opengl.GL;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Random;

public class ItemTex {
    public static class Icon implements GSprite.Owner, Resource.Resolver {
        public final Resource res;
        final Resource.Resolver pool;

        Icon(Resource res, Resource.Resolver pool) {
            this.res = res;
            this.pool = pool;
        }

        public Indir<Resource> getres(int id) {
            return (null);
        }

        static final ClassResolver<Icon> rsv = new ClassResolver<Icon>()
                .add(Resource.Resolver.class, ico -> ico.pool);

        public <C> C context(Class<C> cl) {return (rsv.context(cl, this));}

        public Resource getres() {return (res);}

        public Random mkrandoom() {return (new Random());}
    }

    public static Resource res(OwnerContext owner, Message osdt) {
        MessageBuf sdt = new MessageBuf(osdt.bytes()).clone();
        int resid = sdt.uint16();
        if ((resid & 0x8000) != 0) {
            resid &= ~0x8000;
        }
        Resource ires = owner.context(Resource.Resolver.class).getres(resid).get();
        return (ires);
    }

    public static GSprite mkspr(OwnerContext owner, Message sdt) {
        int resid = sdt.uint16();
        Message isdt = Message.nil;
        if ((resid & 0x8000) != 0) {
            resid &= ~0x8000;
            isdt = new MessageBuf(sdt.bytes(sdt.uint8()));
        }
        Resource ires = owner.context(Resource.Resolver.class).getres(resid).get();
        GSprite.Owner ctx = new Icon(ires, new ResourceMap(owner.context(Resource.Resolver.class), sdt));
        return (GSprite.create(ctx, ires, isdt));
    }

    public static BufferedImage sprimg(GSprite spr) {
        if (spr instanceof GSprite.ImageSprite)
            return (((GSprite.ImageSprite) spr).image());
        return (spr.owner.getres().layer(Resource.imgc).img);
    }

    public static final Map<MessageBuf, BufferedImage> made = new CacheMap<>();

    public static BufferedImage create(OwnerContext owner, Message osdt) {
        MessageBuf copy = new MessageBuf(osdt.bytes());
        synchronized (made) {
            BufferedImage ret = made.get(copy);
            if (ret == null)
                made.put(copy, ret = sprimg(mkspr(owner, copy.clone())));
            return (ret);
        }
    }

    public static BufferedImage fixsz(BufferedImage img) {
        Coord sz = PUtils.imgsz(img);
        int msz = Math.max(sz.x, sz.y);
        int nsz = Math.max((int) Math.round(Math.pow(2, Math.round(Math.log(sz.x) / Math.log(2)))),
                (int) Math.round(Math.pow(2, Math.round(Math.log(sz.y) / Math.log(2)))));
        BufferedImage ret = TexI.mkbuf(new Coord(nsz, nsz));
        java.awt.Graphics g = ret.getGraphics();
        int w = (sz.x * nsz) / msz, h = (sz.y * nsz) / msz;
        g.drawImage(img, (nsz - w) / 2, (nsz - h) / 2, (nsz + w) / 2, (nsz + h) / 2, 0, 0, sz.x, sz.y, null);
        g.dispose();
        return (ret);
    }

    public static final Map<BufferedImage, TexL> fixed = new CacheMap<>();

    public static TexL fixup(final BufferedImage img) {
        TexL tex;
        synchronized (fixed) {
            tex = fixed.get(img);
            if (tex == null) {
                BufferedImage fimg = img;
                Coord sz = PUtils.imgsz(fimg);
                if ((sz.x != sz.y) || (sz.x != Tex.nextp2(sz.x)) || (sz.y != Tex.nextp2(sz.y))) {
                    fimg = fixsz(fimg);
                    sz = PUtils.imgsz(fimg);
                }
                final BufferedImage timg = fimg;
                tex = new TexL(sz) {
                    public BufferedImage fill() {
                        return (timg);
                    }
                };
                tex.mipmap(Mipmapper.dav);
                tex.magfilter(GL.GL_LINEAR);
                tex.minfilter(GL.GL_LINEAR);
//                tex.mipfilter(Texture.Filter.LINEAR);
                fixed.put(img, tex);
            }
        }
        return (tex);
    }
}