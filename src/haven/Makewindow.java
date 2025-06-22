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

import haven.chrwnd.BAttrWnd;
import haven.chrwnd.SAttrWnd;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import static haven.Inventory.invsq;

public class Makewindow extends Widget {
    Widget obtn, cbtn;
    public static final Tex qmodl = PUtils.strokeTex(Text.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Quality:")));
    public static final Tex tooll = PUtils.strokeTex(Text.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Tools:")));
    private static final Tex softcapl = PUtils.strokeTex(Text.render("Softcap:"));
    private Tex softcap;
    public List<Input> inputs = Collections.emptyList();
    public List<SpecWidget> outputs = Collections.emptyList();
    public List<Indir<Resource>> qmod = Collections.emptyList();
    public List<Indir<Resource>> tools = Collections.synchronizedList(new ArrayList<>());
    public int xoff = UI.scale(45), qmy = UI.scale(38), outy = UI.scale(65);
    public static final Text.Foundry nmf = new Text.Foundry(Text.serif, UI.scale(20)).aa(true);
    private long qModProduct = -1;

    @RName("make")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            return (new Makewindow((String) args[0]));
        }
    }

    private static final OwnerContext.ClassResolver<Makewindow> ctxr = new OwnerContext.ClassResolver<Makewindow>()
            .add(Makewindow.class, wdg -> wdg)
            .add(Glob.class, wdg -> wdg.ui.sess.glob)
            .add(Session.class, wdg -> wdg.ui.sess);

    public class Spec implements GSprite.Owner, ItemInfo.SpriteOwner {
        public Indir<Resource> res;
        public MessageBuf sdt;
        public Tex num;
        private GSprite spr;
        private Object[] rawinfo;
        private List<ItemInfo> info;

        public Spec(Indir<Resource> res, Message sdt, int num, Object[] info) {
            this.res = res;
            this.sdt = new MessageBuf(sdt);
            if (num >= 0)
                this.num = new TexI(Utils.outline2(Text.render(Integer.toString(num), Color.WHITE, Text.num10Fnd).img, Utils.contrast(Color.WHITE)));
            else
                this.num = null;
            this.rawinfo = info;
        }

        public GSprite sprite() {
            if (spr == null)
                spr = GSprite.create(this, res.get(), sdt.clone());
            return (spr);
        }

        public void draw(GOut g) {
            try {
                sprite().draw(g);
            } catch (Loading e) {
            }
            if (num != null)
                g.aimage(num, Inventory.sqsz, 1.0, 1.0);
        }

        private int opt = 0;

        public boolean opt() {
            if (opt == 0) {
                try {
                    opt = (ItemInfo.find(Optional.class, info()) != null) ? 1 : 2;
                } catch (Loading l) {
                    return (false);
                }
            }
            return (opt == 1);
        }

        public BufferedImage shorttip() {
            List<ItemInfo> info = info();
            BufferedImage img = null;
            if (!info.isEmpty())
                img = ItemInfo.shorttip(info);
            return (img);
        }

        public BufferedImage longtip() {
            List<ItemInfo> info = info();
            BufferedImage img = null;
            if (!info.isEmpty())
                img = ItemInfo.longtip(info);
            Resource.Pagina pg = res.get().layer(Resource.pagina);
            if (pg != null)
                img = ItemInfo.catimgs(0, img, RichText.render("\n" + pg.text, UI.scale(200)).img);
            return (img);
        }

        private Random rnd = null;

        public Random mkrandoom() {
            if (rnd == null)
                rnd = new Random();
            return (rnd);
        }

        public Resource getres() {
            return (res.get());
        }

        public <T> T context(Class<T> cl) {
            return (ctxr.context(cl, Makewindow.this));
        }

        @Deprecated
        public Glob glob() {
            return (ui.sess.glob);
        }

        public List<ItemInfo> info() {
            if (info == null) {
                Object[] rawinfo = this.rawinfo;
                if (rawinfo == null || rawinfo.length == 0)
                    rawinfo = new Object[]{new Object[]{Resource.remote().load("ui/tt/defn")}};
                info = ItemInfo.buildinfo(this, rawinfo);
            }
            return (info);
        }

        public Resource resource() {
            return (res.get());
        }
    }

    public static final KeyBinding kb_make = KeyBinding.get("make/one", KeyMatch.forcode(java.awt.event.KeyEvent.VK_ENTER, 0));
    public static final KeyBinding kb_makeall = KeyBinding.get("make/all", KeyMatch.forcode(java.awt.event.KeyEvent.VK_ENTER, KeyMatch.C));

    public Makewindow(String rcpnm) {
        Label lblIn = new Label("Input:");
        Label lblOut = new Label("Result:");

        xoff = qmodl.sz().x;
        if (lblIn.sz.x > xoff)
            xoff = lblIn.sz.x;
        if (lblOut.sz.x > xoff)
            xoff = lblOut.sz.x;
        xoff += 8;

        add(lblIn, new Coord(0, 8));
        add(lblOut, new Coord(0, outy + 8));
        obtn = add(new Button(UI.scale(85), "Craft"), UI.scale(new Coord(265, 75))).action(() -> wdgmsg("make", 0));
        cbtn = add(new Button(UI.scale(85), "Craft All"), UI.scale(new Coord(360, 75))).action(() -> wdgmsg("make", 1));
        pack();
        adda(new Label(rcpnm, nmf), sz.x, 0, 1, 0);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "inpop") {
            List<Spec> inputs = new ArrayList<>();
            for (int i = 0; i < args.length; ) {
                int resid = (Integer) args[i++];
                Message sdt = (args[i] instanceof byte[]) ? new MessageBuf((byte[]) args[i++]) : MessageBuf.nil;
                int num = (Integer) args[i++];
                Object[] info = {};
                if ((i < args.length) && (args[i] instanceof Object[]))
                    info = (Object[]) args[i++];
                inputs.add(new Spec(ui.sess.getres(resid), sdt, num, info));
            }
            ui.sess.glob.loader.defer(() -> {
                List<Input> wdgs = new ArrayList<>();
                int idx = 0;
                for (Spec spec : inputs)
                    wdgs.add(new Input(spec, idx++));
                synchronized (ui) {
                    for (Widget w : this.inputs)
                        w.destroy();
                    Position pos = new Position(xoff, 0);
                    SpecWidget prev = null;
                    for (Input wdg : wdgs) {
                        if ((prev != null) && (wdg.opt != false))
                            pos = pos.adds(10, 0);
                        add(wdg, pos);
                        pos = pos.add(Inventory.sqsz.x, 0);
                        prev = wdg;
                    }
                    this.inputs = wdgs;
                }
            }, null);
        } else if (msg == "opop") {
            List<Spec> outputs = new ArrayList<Spec>();
            for (int i = 0; i < args.length; ) {
                int resid = (Integer) args[i++];
                Message sdt = (args[i] instanceof byte[]) ? new MessageBuf((byte[]) args[i++]) : MessageBuf.nil;
                int num = (Integer) args[i++];
                Object[] info = {};
                if ((i < args.length) && (args[i] instanceof Object[]))
                    info = (Object[]) args[i++];
                outputs.add(new Spec(ui.sess.getres(resid), sdt, num, info));
            }
            ui.sess.glob.loader.defer(() -> {
                List<SpecWidget> wdgs = new ArrayList<>();
                for (Spec spec : outputs)
                    wdgs.add(new SpecWidget(spec));
                synchronized (ui) {
                    for (Widget w : this.outputs)
                        w.destroy();
                    Position pos = new Position(xoff, outy);
                    SpecWidget prev = null;
                    for (SpecWidget wdg : wdgs) {
                        if ((prev != null) && (wdg.opt != prev.opt))
                            pos = pos.adds(10, 0);
                        add(wdg, pos);
                        pos = pos.add(Inventory.sqsz.x, 0);
                        prev = wdg;
                    }
                    this.outputs = wdgs;
                }
            }, null);
        } else if (msg == "qmod") {
            List<Indir<Resource>> qmod = new ArrayList<Indir<Resource>>();
            for (Object arg : args)
                qmod.add(ui.sess.getres((Integer) arg));
            this.qmod = qmod;
        } else if (msg == "tool") {
            tools.add(ui.sess.getres((Integer) args[0]));
        } else if (msg == "inprcps") {
            int idx = (Integer) args[0];
            Collection<MenuGrid.Pagina> rcps = new ArrayList<>();
            GameUI gui = getparent(GameUI.class);
            if ((gui != null) && (gui.menu != null)) {
                for (int a = 1; a < args.length; a++)
                    rcps.add(gui.menu.paginafor(ui.sess.getres((Integer) args[a])));
            }
            inputs.get(idx).recipes(rcps);
            ;
        } else {
            super.uimsg(msg, args);
        }
    }

    public static final Coord qmodsz = UI.scale(20, 20);
    private static final Map<Indir<Resource>, Tex> qmicons = new WeakHashMap<>();

    private static Tex qmicon(Indir<Resource> qm) {
        return (qmicons.computeIfAbsent(qm, res -> new TexI(PUtils.convolve(res.get().flayer(Resource.imgc).img, qmodsz, CharWnd.iconfilter))));
    }

    public static class SpecWidget extends Widget {
        public final Spec spec;
        public final boolean opt;

        public SpecWidget(Spec spec) {
            super(invsq.sz());
            this.spec = spec;
            opt = spec.opt();
        }

        public void draw(GOut g) {
            if (opt) {
                g.chcolor(0, 255, 0, 255);
                g.image(invsq, Coord.z);
                g.chcolor();
            } else {
                g.image(invsq, Coord.z);
            }
            spec.draw(g);
        }

        private double hoverstart;
        Indir<Object> stip, ltip;

        public Object tooltip(Coord c, Widget prev) {
            double now = Utils.rtime();
            if (prev == this) {
            } else if (prev instanceof SpecWidget) {
                double ps = ((SpecWidget) prev).hoverstart;
                hoverstart = (now - ps < 1.0) ? now : ps;
            } else {
                hoverstart = now;
            }
            if (now - hoverstart < 1.0) {
                if (stip == null) {
                    BufferedImage tip = spec.shorttip();
                    Tex tt = (tip == null) ? null : new TexI(tip);
                    stip = () -> tt;
                }
                return (stip);
            } else {
                if (ltip == null) {
                    BufferedImage tip = spec.longtip();
                    Tex tt = (tip == null) ? null : new TexI(tip);
                    ltip = () -> tt;
                }
                return (ltip);
            }
        }

        public void tick(double dt) {
            super.tick(dt);
            if (spec.spr != null)
                spec.spr.tick(dt);
        }
    }

    public class Input extends SpecWidget {
        public final int idx;
        private Collection<MenuGrid.Pagina> rpag = null;
        private Coord cc = null;

        public Input(Spec spec, int idx) {
            super(spec);
            this.idx = idx;
        }

        public boolean mousedown(Coord c, int button) {
            if (button == 1) {
                if (rpag == null)
                    Makewindow.this.wdgmsg("findrcps", idx);
                this.cc = c;
                return (true);
            }
            return (super.mousedown(c, button));
        }

        public void tick(double dt) {
            super.tick(dt);
            if ((cc != null) && (rpag != null)) {
                if (!rpag.isEmpty()) {
                    try {
                        List<MenuGrid.PagButton> btns = new ArrayList<>();
                        for (MenuGrid.Pagina pag : rpag)
                            btns.add(pag.button());
                        new SListMenu<MenuGrid.PagButton, Widget>(UI.scale(250, 120), CharWnd.attrf.height()) {
                            public List<MenuGrid.PagButton> items() {return (btns);}

                            public Widget makeitem(MenuGrid.PagButton btn, int idx, Coord sz) {
                                return (SListWidget.IconText.of(sz, btn::img, btn::name));
                            }

                            public void choice(MenuGrid.PagButton btn) {
                                if (btn != null)
                                    btn.pag.scm.use(btn, false);
                                destroy();
                            }
                        }.addat(this, cc.add(UI.scale(5, 5))).tick(dt);
                    } catch (Loading l) {
                        return;
                    }
                }
                cc = null;
            }
        }

        public void recipes(Collection<MenuGrid.Pagina> pag) {
            rpag = pag;
        }
    }

    public void draw(GOut g) {
        {
            int x = 0;
            if (!qmod.isEmpty()) {
                g.aimage(qmodl, new Coord(x, qmy + (qmodsz.y / 2)), 0, 0.5);
                x += qmodl.sz().x + UI.scale(5);
                x = Math.max(x, xoff);
                qmx = x;

                CharWnd chrwdg = null;
                try {
                    chrwdg = ((GameUI) parent.parent).chrwdg;
                } catch (Exception e) { // fail silently
                }

                List<Integer> qmodValues = new ArrayList<>(3);

                for (Indir<Resource> qm : qmod) {
                    try {
                        Tex t = qmicon(qm);
                        g.image(t, new Coord(x, qmy));
                        x += t.sz().x + UI.scale(1);

                        if (Config.showcraftcap && chrwdg != null) {
                            String name = qm.get().basename();
                            for (SAttrWnd.SAttr attr : chrwdg.sattr.skill) {
                                if (name.equals(attr.attr.nm)) {
                                    g.aimage(attr.attr.comptex, new Coord(x, qmy + (qmodsz.y / 2)), 0, 0.5);
                                    x += attr.attr.comptex.sz().x;
                                    qmodValues.add(attr.attr.comp);
                                    break;
                                }
                            }
                            for (BAttrWnd.Attr attr : chrwdg.battr.base) {
                                if (name.equals(attr.attr.nm)) {
                                    g.aimage(attr.attr.comptex, new Coord(x, qmy + (qmodsz.y / 2)), 0, 0.5);
                                    x += attr.attr.comptex.sz().x;
                                    qmodValues.add(attr.attr.comp);
                                    break;
                                }
                            }
                        }
                    } catch (Exception l) {
                    }
                }
                x += UI.scale(25);

                if (Config.showcraftcap && !qmodValues.isEmpty()) {
                    long product = 1;
                    for (long cap : qmodValues)
                        product *= cap;

                    if (product != qModProduct) {
                        qModProduct = product;
                        softcap = PUtils.strokeTex(Text.num12boldFnd.render("" + (int) Math.pow(product, 1.0 / qmodValues.size()),
                                Color.WHITE));
                    }

                    Coord sz = softcap.sz();
                    Coord szl = softcapl.sz();
                    g.image(softcapl, this.sz.sub(sz.x + szl.x + 113, (this.sz.y / 2 + szl.y / 2) - 15));
                    g.image(softcap, this.sz.sub(sz.x + 105, (this.sz.y / 2 + sz.y / 2) - 15));
                }
            }

            if (!tools.isEmpty()) {
                g.aimage(tooll, new Coord(x, qmy + (qmodsz.y / 2)), 0, 0.5);
                x += tooll.sz().x + UI.scale(5);
                x = Math.max(x, xoff);
                toolx = x;
                for (Indir<Resource> tool : tools) {
                    try {
                        Tex t = qmicon(tool);
                        g.image(t, new Coord(x, qmy));
                        x += t.sz().x + UI.scale(1);
                    } catch (Loading l) {
                    }
                }
                x += UI.scale(25);
            }
        }
        super.draw(g);
    }

    private int qmx, toolx;

    public Object tooltip(Coord mc, Widget prev) {
        String name = null;
        Spec tspec = null;
        Coord c;
        if (!qmod.isEmpty()) {
            c = new Coord(qmx, qmy);
            try {
                CharWnd chrwdg = null;
                Tex tvalue = null;
                if (Config.showcraftcap) {
                    try {
                        chrwdg = ((GameUI) parent.parent).chrwdg;
                    } catch (Exception e) { // fail silently
                    }

                }
                for (Indir<Resource> qm : qmod) {
                    Coord tsz = qmicon(qm).sz();
                    if (mc.isect(c, tsz))
                        return (qm.get().flayer(Resource.tooltip).t);
                    if (Config.showcraftcap && chrwdg != null) {
                        String value = qm.get().basename();
                        for (SAttrWnd.SAttr attr : chrwdg.sattr.skill) {
                            if (value.equals(attr.attr.nm)) {
                                tvalue = attr.attr.comptex;
                                break;
                            }
                        }
                        for (BAttrWnd.Attr attr : chrwdg.battr.base) {
                            if (value.equals(attr.attr.nm)) {
                                tvalue = attr.attr.comptex;
                                break;
                            }
                        }
                    }
                    c = c.add(tsz.x + (tvalue == null ? 0 : tvalue.sz().x), 0);
                }
            } catch (Loading l) {
            }
        }
        if (!tools.isEmpty()) {
            c = new Coord(toolx, qmy);
            try {
                for (Indir<Resource> tool : tools) {
                    Coord tsz = qmicon(tool).sz();
                    if (mc.isect(c, tsz))
                        return (tool.get().flayer(Resource.tooltip).t);
                    c = c.add(tsz.x + UI.scale(1), 0);
                }
            } catch (Loading l) {
            }
        }
        return (super.tooltip(mc, prev));
    }

    private static String getDynamicName(GSprite spr) {
        if (spr != null) {
            Class<? extends GSprite> sprClass = spr.getClass();
            if (Reflect.hasInterface("haven.res.ui.tt.defn.DynName", sprClass)) {
                return (String) Reflect.invoke(spr, "name");
            }
        }
        return null;
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == obtn) {
            if (msg == "activate")
                wdgmsg("make", 0);
            return;
        }
        if (sender == cbtn) {
            if (msg == "activate")
                wdgmsg("make", 1);
            return;
        }
        super.wdgmsg(sender, msg, args);
    }

    public boolean globtype(char ch, KeyEvent ev) {
        if (ch == '\n') {
            wdgmsg("make", ui.modctrl ? 1 : 0);
            return (true);
        }
        return (super.globtype(ch, ev));
    }

    public static class Optional extends ItemInfo.Tip {
        public static final Text text = RichText.render("$i{Optional}", 0);

        public Optional(Owner owner) {
            super(owner);
        }

        public BufferedImage tipimg() {
            return (text.img);
        }

        public Tip shortvar() {
            return (this);
        }
    }

    public static class MakePrep extends ItemInfo implements GItem.ColorInfo, GItem.ContentsInfo {
        private final static Color olcol = new Color(0, 255, 0, 64);

        public MakePrep(Owner owner) {
            super(owner);
        }

        public Color olcol() {
            return (olcol);
        }

        public void propagate(List<ItemInfo> buf, Owner outer) {
            if (ItemInfo.find(MakePrep.class, buf) == null)
                buf.add(new MakePrep(outer));
        }
    }
}
