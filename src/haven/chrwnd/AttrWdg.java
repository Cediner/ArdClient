package haven.chrwnd;

import haven.CharWnd;
import haven.Coord;
import haven.Glob;
import haven.ItemInfo;
import haven.Loading;
import haven.OwnerContext;
import haven.Resource;
import haven.Session;
import haven.Tex;
import haven.TexI;
import haven.Widget;

import java.util.List;

public abstract class AttrWdg extends Widget implements ItemInfo.Owner {
    public final String nm;
    public final Glob.CAttr attr;

    public AttrWdg(Coord sz, Glob glob, String attr) {
        super(sz);
        this.nm = attr;
        this.attr = glob.getcattr(attr);
    }

    private static final OwnerContext.ClassResolver<AttrWdg> ctxr = new OwnerContext.ClassResolver<AttrWdg>()
            .add(AttrWdg.class, wdg -> wdg)
            .add(CharWnd.class, wdg -> wdg.getparent(CharWnd.class))
            .add(Glob.CAttr.class, wdg -> wdg.attr)
            .add(Glob.class, wdg -> wdg.attr.glob)
            .add(Session.class, wdg -> wdg.ui.sess);

    public <T> T context(Class<T> cl) {return (ctxr.context(cl, this));}

    private ItemInfo.Raw rinfo = null;
    private List<ItemInfo> binfo = null;

    public List<ItemInfo> info() {
        if (attr.info != this.rinfo) {
            this.binfo = null;
            this.rinfo = attr.info;
        }
        if (this.binfo == null) {
            List<ItemInfo> binfo = ItemInfo.buildinfo(this, this.rinfo);
            Resource.Pagina pag = attr.res().get().layer(Resource.pagina);
            if (pag != null)
                binfo.add(new ItemInfo.Pagina(this, pag.text));
            if (!binfo.isEmpty())
                binfo.add(new ItemInfo.Name(this, attr.res().get().flayer(Resource.tooltip).t));
            this.binfo = binfo;
        }
        return (this.binfo);
    }

    private List<ItemInfo> tipinfo;
    private Tex tipimg = null;

    public Object tooltip(Coord c, Widget prev) {
        List<ItemInfo> info = info();
        if ((tipimg != null) && (info != tipinfo)) {
            tipimg.dispose();
            tipimg = null;
        }
        if (tipimg == null) {
            try {
                if (info.isEmpty())
                    return (null);
                tipimg = new TexI(ItemInfo.longtip(info));
                tipinfo = info;
            } catch (Loading l) {
                return ("...");
            }
        }
        return (tipimg);
    }
}