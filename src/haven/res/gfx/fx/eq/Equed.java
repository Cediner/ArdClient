package haven.res.gfx.fx.eq;

import haven.Drawable;
import haven.EquipTarget;
import haven.FromResource;
import haven.GLState;
import haven.Gob;
import haven.Indir;
import haven.Message;
import haven.MessageBuf;
import haven.RenderLink;
import haven.RenderList;
import haven.Rendered;
import haven.Resource;
import haven.Skeleton;
import haven.Sprite;

import java.util.function.Function;

@FromResource(name = "gfx/fx/eq", version = 19, override = true)
public class Equed extends Sprite {
    private final Sprite espr;
    private final GLState eqd;

    public Equed(Owner owner, Resource res, Sprite espr, Skeleton.BoneOffset bo) {
        super(owner, res);
        this.espr = espr;
//        this.eqd = RUtils.StateTickNode.from(this.espr, bo.from(owner.fcontext(EquipTarget.class, false)));
        this.eqd = bo.from(owner.fcontext(EquipTarget.class, false));
    }

    public static Resource ctxres(Owner owner) {
        Gob gob = owner.context(Gob.class);
        if (gob == null)
            throw (new RuntimeException("no context resource for owner " + owner));
        Drawable d = gob.getattr(Drawable.class);
        if (d == null)
            throw (new RuntimeException("no drawable on object " + gob));
        return (d.getres());
    }

    public static Equed mksprite(Owner owner, Resource res, Message sdt) {
        Indir<Resource> eres = owner.context(Resource.Resolver.class).getres(sdt.uint16());
        int fl = sdt.uint8();
        String epn = sdt.string();
        Resource epres = ((fl & 1) == 0) ? (ctxres(owner)) : eres.get();
        Message sub = Message.nil;
        if ((fl & 2) != 0)
            sub = new MessageBuf(sdt.bytes(sdt.uint8()));
        Skeleton.BoneOffset bo = epres.layer(Skeleton.BoneOffset.class, epn);
        if (bo == null)
            throw (new RuntimeException("No such bone-offset in " + epres.name + ": " + epn));
        return (new Equed(owner, res, Sprite.create(owner, eres.get(), sub), bo));
    }

    public static RenderLink mkrlink(Resource res, Object... args) {
        String epn = (String) args[0];
        String fl = (String) args[1];
        Resource eres = res.pool.load((String) args[2], (Integer) args[3]).get();
        Function<Owner, Skeleton.BoneOffset> ep;
        if (fl.indexOf('l') >= 0)
            ep = owner -> eres.flayer(Skeleton.BoneOffset.class, epn);
        else if (fl.indexOf("c") >= 0)
            ep = owner -> ctxres(owner).flayer(Skeleton.BoneOffset.class, epn);
        else if (fl.indexOf("o") >= 0)
            ep = owner -> res.flayer(Skeleton.BoneOffset.class, epn);
        else
            ep = owner -> res.flayer(Skeleton.BoneOffset.class, epn);
        Mill<?> mill;
        if ((args.length > 4) && (args[4] instanceof byte[])) {
            mill = owner -> Sprite.create(owner, eres, new MessageBuf((byte[]) args[4]));
        } else if ((args.length > 4) && (args[4] instanceof Object[])) {
            RenderLink rl = eres.getcode(RenderLink.ArgLink.class, true).parse( res, (Object[]) args[4]);
            mill = owner -> {
                Rendered n = rl.make(owner);
                if (!(n instanceof Sprite))
                    throw (new ResourceException("Sublink returned non-sprite node " + String.valueOf(n), eres));
                return ((Sprite) n);
            };
        } else {
            mill = owner -> Sprite.create(owner, eres, Message.nil);
        }
        return (owner -> new Equed(owner, res, mill.create(owner), ep.apply(owner)));
    }

    public boolean setup(RenderList r) {
        r.add(espr, eqd);
        return (false);
    }

    public boolean tick(double dt) {
        espr.tick(dt);
        return (false);
    }

    public void age() {
        espr.age();
    }

    @Override
    public Object staticp() {
        return (null);
    }
}
