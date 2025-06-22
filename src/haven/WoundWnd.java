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

import haven.chrwnd.IconInfo;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static haven.CharWnd.attrf;
import static haven.CharWnd.attrw;
import static haven.CharWnd.catf;
import static haven.CharWnd.every;
import static haven.CharWnd.iconfilter;
import static haven.CharWnd.ifnd;
import static haven.CharWnd.margin1;
import static haven.CharWnd.offy;
import static haven.CharWnd.other;
import static haven.CharWnd.wbox;
import static haven.CharWnd.width;

public class WoundWnd extends Widget {
    public static final Text.Foundry namef = new Text.Foundry(Text.serif.deriveFont(java.awt.Font.BOLD), 16).aa(true);
    public final Widget woundbox;
    public final WoundList wounds;
    public Wound.Info wound;

    @RName("wounds")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            return (new WoundWnd());
        }
    }

    public static interface QuickInfo {
        public default Widget qwdg(int h) {
            if (qstr() == null)
                return (null);
            return (new Label(qstr(), attrf));
        }

        public default String qstr() {
            return (null);
        }
    }

    public static class WoundPagina extends ItemInfo.Tip {
        public final String str;

        public WoundPagina(Owner owner, String str) {
            super(owner);
            this.str = str;
        }

        public void layout(Layout l) {
            BufferedImage t = ifnd.render(str, l.width).img;
            if (t != null) {
                l.cmp.add(t, Coord.of(0, l.cmp.sz.y));
                l.cmp.sz = l.cmp.sz.add(0, UI.scale(10));
            }
        }

        public int order() {return (10);}
    }

    public static class Wound implements ItemInfo.ResOwner {
        public final Glob glob;
        public final int id, parentid;
        public Indir<Resource> res;
        public Object qdata;
        public int level;
        public ItemInfo.Raw rawinfo;
        private String sortkey = "\uffff";
        private Tex small;
        private int namew;
        private final Text.UTex<?> rnm = new Text.UTex<>(() -> {
            try {
                return (res.get().layer(Resource.tooltip).t);
            } catch (Loading l) {
                return ("...");
            }
        }, s -> PUtils.strokeTex(CharWnd.attrf.render(s)));
        /*public Text render(String text) {
            Text.Foundry fnd = (Text.Foundry) this.fnd;
            Text.Line full = fnd.render(text);
            if (full.sz().x <= namew)
                return (full);
            int ew = fnd.strsize("...").x;
            for (int i = full.text.length() - 1; i > 0; i--) {
                if ((full.advance(i) + ew) < namew)
                    return (fnd.render(text.substring(0, i) + "..."));
            }
            return (full);
        }*/

        /*
        public Text render(String text) {
        Text.Foundry fnd = (Text.Foundry)this.fnd;
        Text.Line ret = fnd.render(text);
        while(ret.sz().x > namew) {
            fnd = new Text.Foundry(fnd.font, fnd.font.getSize() - 1, fnd.defcol).aa(true);
            ret = fnd.render(text);
        }
        return(ret);
        }
        */
//        };
        private final Text.UTex<?> rqd = new Text.UTex<>(() -> qdata, s -> PUtils.strokeTex(CharWnd.attrf.render(Objects.toString(s))));

        public Wound(Glob glob, int id, Indir<Resource> res, int parentid) {
            this.glob = glob;
            this.id = id;
            this.res = res;
            this.parentid = parentid;
        }

        private static final OwnerContext.ClassResolver<Wound> ctxr = new OwnerContext.ClassResolver<Wound>()
                .add(Wound.class, wnd -> wnd)
                .add(Glob.class, wnd -> wnd.glob)
                .add(Session.class, wnd -> wnd.glob.sess);

        public <T> T context(Class<T> cl) {return (ctxr.context(cl, this));}

        public Resource resource() {return (res.get());}

        private List<ItemInfo> info;

        public List<ItemInfo> info() {
            if (info == null) {
                List<ItemInfo> info = ItemInfo.buildinfo(this, rawinfo);
                Resource.Pagina pag = res.get().layer(Resource.pagina);
                if (pag != null)
                    info.add(new WoundPagina(this, pag.text));
                this.info = info;
            }
            return (info);
        }

        public BufferedImage icon() {
            return (IconInfo.render(res.get().flayer(Resource.imgc).scaled(), info()));
        }

        public String name() {
            return (ItemInfo.find(ItemInfo.Name.class, info()).str.text);
        }

        public interface Info {
            public int woundid();
        }
    }

    public static class WoundBox extends haven.chrwnd.ImageInfoBox implements Wound.Info {
        public final int id;
        private List<ItemInfo> info;

        public WoundBox(int id) {
            super(Coord.z);
            this.id = id;
        }

        protected void added() {
            resize(parent.sz);
        }

        public Wound wound() {
            return (getparent(WoundWnd.class).wounds.get(id));
        }

        public void tick(double dt) {
            super.tick(dt);
            try {
                if (this.info != wound().info())
                    set(() -> new TexI(renderinfo(sz.x - Scrollbar.width - (marg().x * 2))));
            } catch (Loading l) {}
        }

        public void drawbg(GOut g) {}

        public BufferedImage renderinfo(int width) {
            Wound wnd = wound();
            ItemInfo.Layout l = new ItemInfo.Layout(wnd);
            l.width = width;
            List<ItemInfo> info = wnd.info();
            l.cmp.add(wnd.icon(), Coord.z);
            ItemInfo.Name nm = ItemInfo.find(ItemInfo.Name.class, info);
            l.cmp.add(namef.render(nm.str.text).img, Coord.of(0, l.cmp.sz.y + UI.scale(10)));
            l.cmp.sz = l.cmp.sz.add(0, UI.scale(10));
            for (ItemInfo inf : info) {
                if ((inf != nm) && (inf instanceof ItemInfo.Tip))
                    l.add((ItemInfo.Tip) inf);
            }
            this.info = info;
            return (l.render());
        }

        public int woundid() {return (id);}
    }

    @RName("wound")
    public static class $wound implements Factory {
        public Widget create(UI ui, Object[] args) {
            int id = Utils.iv(args[0]);
            return (new WoundBox(id));
        }
    }

    public class WoundList extends Listbox<Wound> implements DTarget {
        public List<Wound> wounds = Collections.synchronizedList(new ArrayList<Wound>());
        private boolean loading = false;
        private final Comparator<Wound> wcomp = new Comparator<Wound>() {
            public int compare(Wound a, Wound b) {
                return (a.sortkey.compareTo(b.sortkey));
            }
        };

        private WoundList(int w, int h) {
            super(w, h, attrf.height() + UI.scale(2));
        }

        private List<Wound> treesort(List<Wound> from, int pid, int level) {
            List<Wound> direct = Collections.synchronizedList(new ArrayList<>(from.size()));
            for (Wound w : from) {
                if (w.parentid == pid) {
                    w.level = level;
                    direct.add(w);
                }
            }
            Collections.sort(direct, wcomp);
            List<Wound> ret = Collections.synchronizedList(new ArrayList<>(from.size()));
            for (Wound w : direct) {
                ret.add(w);
                ret.addAll(treesort(from, w.id, level + 1));
            }
            return (ret);
        }

        public void tick(double dt) {
            if (loading) {
                loading = false;
                for (Wound w : wounds) {
                    try {
                        w.sortkey = w.res.get().layer(Resource.tooltip).t;
                    } catch (Loading l) {
                        w.sortkey = "\uffff";
                        loading = true;
                    }
                }
                wounds = treesort(wounds, -1, 0);
            }
        }

        protected Wound listitem(int idx) {
            return (wounds.get(idx));
        }

        protected int listitems() {
            return (wounds.size());
        }

        protected void drawbg(GOut g) {
        }

        protected void drawitem(GOut g, Wound w, int idx) {
            if ((wound != null) && (wound.woundid() == w.id))
                drawsel(g);
            g.chcolor((idx % 2 == 0) ? every : other);
            g.frect(Coord.z, g.sz);
            g.chcolor();
            int x = w.level * itemh;
            try {
                if (w.small == null)
                    w.small = new TexI(PUtils.convolvedown(w.res.get().layer(Resource.imgc).img, new Coord(itemh, itemh), iconfilter));
                g.image(w.small, new Coord(x, 0));
                x += itemh + margin1;
            } catch (Loading e) {
                g.image(WItem.missing.layer(Resource.imgc).tex(), new Coord(x, 0), new Coord(itemh, itemh));
                x += itemh + margin1;
            }
            g.aimage(w.rnm.get(), new Coord(x, itemh / 2), 0, 0.5);
            Tex qd = w.rqd.get();
            if (qd != null)
                g.aimage(qd, new Coord(sz.x - UI.scale(15), itemh / 2), 1.0, 0.5);
        }

        protected void itemclick(Wound item, int button) {
            if (button == 3) {
                WoundWnd.this.wdgmsg("wclick", item.id, button, ui.modflags());
            } else {
                super.itemclick(item, button);
            }
        }

        public boolean drop(Coord cc, Coord ul) {
            return (false);
        }

        public boolean iteminteract(Coord cc, Coord ul) {
            Wound w = itemat(cc);
            if (w != null)
                WoundWnd.this.wdgmsg("wiact", w.id, ui.modflags());
            return (true);
        }

        public void change(Wound w) {
            if (w == null)
                WoundWnd.this.wdgmsg("wsel", (Object) null);
            else
                WoundWnd.this.wdgmsg("wsel", w.id);
        }

        public Wound get(int id) {
            for (Wound w : wounds) {
                if (w.id == id)
                    return (w);
            }
            return (null);
        }

        public void add(Wound w) {
            wounds.add(w);
        }

        public Wound remove(int id) {
            for (Iterator<Wound> i = wounds.iterator(); i.hasNext(); ) {
                Wound w = i.next();
                if (w.id == id) {
                    i.remove();
                    return (w);
                }
            }
            return (null);
        }
    }

    public WoundWnd() {
        add(CharWnd.settip(new Img(catf.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Health & Wounds")).tex()), "gfx/hud/chr/tips/wounds"), new Coord(0, 0));
        this.wounds = add(new WoundList(attrw, 12), new Coord(width + margin1, offy).add(wbox.btloff()));
        Frame.around(this, Collections.singletonList(this.wounds));
        woundbox = add(new Widget(new Coord(attrw, this.wounds.sz.y)) {
            public void draw(GOut g) {
                g.chcolor(0, 0, 0, 128);
                g.frect(Coord.z, sz);
                g.chcolor();
                super.draw(g);
            }

            public void cdestroy(Widget w) {
                if (w == wound)
                    wound = null;
            }
        }, new Coord(margin1, offy).add(wbox.btloff()));
        Frame.around(this, Collections.singletonList(woundbox));
        pack();
    }

    public void addchild(Widget child, Object... args) {
        String place = (args[0] instanceof String) ? (((String) args[0]).intern()) : null;
        if (place == "wound") {
            this.wound = (Wound.Info) child;
            woundbox.add(child, Coord.z);
        } else {
            super.addchild(child, args);
        }
    }

    private void decwound(Object[] args, int a, int len) {
        int id = (Integer) args[a];
        Indir<Resource> res = (args[a + 1] == null) ? null : ui.sess.getres((Integer) args[a + 1]);
        if (res != null) {
            int parentid = (len > 3) ? ((args[a + 3] == null) ? -1 : Utils.iv(args[a + 3])) : -1;
            Wound w = wounds.get(id);
            if (w == null) {
                wounds.add(w = new Wound(ui.sess.glob, id, res, parentid));
            } else {
                w.res = res;
            }
            w.rawinfo = new ItemInfo.Raw(Utils.splice(args, a + 4, len - 4));
            w.info = null;
            wounds.loading = true;
        } else {
            wounds.remove(id);
        }
    }

    public void uimsg(String nm, Object... args) {
        if (nm == "wounds") {
            if (args.length > 0) {
                if (args[0] instanceof Object[]) {
                    for (int i = 0; i < args.length; i++)
                        decwound((Object[]) args[i], 0, ((Object[]) args[i]).length);
                } else {
                    for (int i = 0; i < args.length; i += 3)
                        decwound(args, i, 3);
                }
            }
          /*  for (int i = 0; i < args.length; i += 3) {
                int id = (Integer) args[i];
                Indir<Resource> res = (args[i + 1] == null) ? null : ui.sess.getres((Integer) args[i + 1]);
                Object qdata = args[i + 2];
                if (res != null) {
                    Object qdata = args[a + 2];
                    int parentid = (len > 3) ? ((args[a + 3] == null) ? -1 : (Integer)args[a + 3]) : -1;
                    Wound w = wounds.get(id);
                    if (w == null) {
                        wounds.add(new Wound(id,res,qdata,parentid));
                    } else {
                        w.res = res;
                        w.qdata = qdata;
                    }
                    wounds.loading = true;
                } else {
                    wounds.remove(id);
                }
            }*/
        } else {
            super.uimsg(nm, args);
        }
    }
}
