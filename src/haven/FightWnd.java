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

import haven.purus.pbot.PBotUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static haven.CharWnd.attrf;
import static haven.Inventory.invsq;
import static haven.Window.wbox;

public class FightWnd extends Widget {
    public final int nsave;
    public int maxact;
    public final Actions actlist;
    public List<Action> acts = new ArrayList<Action>();
    public final Action[] order;
    public int usesave;
    private final Text[] saves;
    private final ImageInfoBox info;
    private Tex count;
    private final Map<Indir<Resource>, Object[]> actrawinfo = new HashMap<>();
    public Dropbox<Pair<Text, Integer>> schoolsDropdown;

    private static final Set<String> attacks = new HashSet<>(Arrays.asList(
            "paginae/atk/pow",
            "paginae/atk/lefthook",
            "paginae/atk/lowblow",
            "paginae/atk/oppknock",
            "paginae/atk/ripapart",
            "paginae/atk/fullcircle",
            "paginae/atk/cleave",
            "paginae/atk/barrage",
            "paginae/atk/sideswipe",
            "paginae/atk/sting",
            "paginae/atk/sos",
            "paginae/atk/knockteeth",
            "paginae/atk/kick",
            "paginae/atk/haymaker",
            "paginae/atk/chop",
            "paginae/atk/gojug",
            "paginae/atk/uppercut",
            "paginae/atk/punchboth",
            "paginae/atk/stealthunder",
            "paginae/atk/ravenbite",
            "paginae/atk/takedown"
    ));
    private static final Set<String> restorations = new HashSet<>(Arrays.asList(
            "paginae/atk/regain",
            "paginae/atk/dash",
            "paginae/atk/zigzag",
            "paginae/atk/yieldground",
            "paginae/atk/watchmoves",
            "paginae/atk/sidestep",
            "paginae/atk/qdodge",
            "paginae/atk/jump",
            "paginae/atk/fdodge",
            "paginae/atk/artevade",
            "paginae/atk/flex"
    ));
    private static final Set<String> moves = new HashSet<>(Arrays.asList(
            "paginae/atk/think",
            "paginae/atk/takeaim"
    ));
    private static final Set<String> maneuvers = new HashSet<>(Arrays.asList(
            "paginae/atk/toarms",
            "paginae/atk/shield",
            "paginae/atk/parry",
            "paginae/atk/oakstance",
            "paginae/atk/dorg",
            "paginae/atk/chinup",
            "paginae/atk/bloodlust",
            "paginae/atk/combmed"
    ));
    private int filter = 0;


    public static interface IconInfo {
        public void draw(BufferedImage img, Graphics g);
    }

    public int cards(final String name) {
        for (final Action act : acts) {
            try {
                if (act.res.get().name.equals(name)) {
                    return act.u;
                }
            } catch (Loading l) {
                //ignore
            }
        }
        return 1;
    }

    private static final OwnerContext.ClassResolver<FightWnd> actxr = new OwnerContext.ClassResolver<FightWnd>()
            .add(FightWnd.class, wdg -> wdg)
            .add(Glob.class, wdg -> wdg.ui.sess.glob)
            .add(Session.class, wdg -> wdg.ui.sess);
    public static final Text.Foundry namef = new Text.Foundry(Text.serif.deriveFont(java.awt.Font.BOLD, UI.scale(16f))).aa(true);

    public class Action implements ItemInfo.ResOwner {
        public final Indir<Resource> res;
        private final int id;
        public int a, u;
        private String name;
        private Tex rnm, ru, ra;
        private Tex ri;

        public Action(Indir<Resource> res, int id, int a, int u) {
            this.res = res;
            this.id = id;
            this.a = a;
            this.u = u;
        }

        public String rendertext() {
            StringBuilder buf = new StringBuilder();
            Resource res = this.res.get();
            buf.append("$img[" + res.name + "]\n\n");
            buf.append("$b{$font[serif,16]{" + res.layer(Resource.tooltip).t + "}}\n\n");
            Resource.Pagina pag = res.layer(Resource.pagina);
            if (pag != null)
                buf.append(pag.text);
            return (buf.toString());
        }

        private void a(int a) {
            if (this.a != a) {
                this.a = a;
                this.ru = null;
                this.ra = null;
            }
        }

        private void u(int u) {
            if (this.u != u && u <= a) {
                this.u = u;
                this.ru = null;
                recount();
            }
        }

        public Resource resource() {
            return (res.get());
        }

        private List<ItemInfo> info = null;

        public List<ItemInfo> info() {
            if (info == null) {
                Object[] rawinfo = actrawinfo.get(this.res);
                if (rawinfo != null)
                    info = ItemInfo.buildinfo(this, rawinfo);
                else
                    info = Arrays.asList(new ItemInfo.Name(this, res.get().layer(Resource.tooltip).origt, res.get().layer(Resource.tooltip).t));
            }
            return (info);
        }

        public <T> T context(Class<T> cl) {
            return (actxr.context(cl, FightWnd.this));
        }

        public BufferedImage rendericon() {
            BufferedImage ret = res.get().layer(Resource.imgc).img;
            Graphics g = null;
            for (ItemInfo inf : info()) {
                if (inf instanceof IconInfo) {
                    if (g == null) {
                        BufferedImage buf = TexI.mkbuf(PUtils.imgsz(ret));
                        g = buf.getGraphics();
                        ret = buf;
                    }
                    ((IconInfo) inf).draw(ret, g);
                }
            }
            if (g != null)
                g.dispose();
            return (ret);
        }

        private Tex icon = null;

        public Tex icon() {
            if (icon == null)
                icon = new TexI(rendericon());
            return (icon);
        }

        private Pattern ezFix = Pattern.compile("(.*\\$img\\[)([A-Za-z\\/]+)(].*)");

        public BufferedImage renderinfo(int width) {
            ItemInfo.Layout l = new ItemInfo.Layout(this);
            l.width = width;
            List<ItemInfo> info = info();
            l.cmp.add(rendericon(), Coord.z);
            ItemInfo.Name nm = ItemInfo.find(ItemInfo.Name.class, info);
            l.cmp.add(namef.render(nm.str.text).img, new Coord(0, l.cmp.sz.y + UI.scale(10)));
            l.cmp.sz = l.cmp.sz.add(0, UI.scale(10));
            for (ItemInfo inf : info) {
                if ((inf != nm) && (inf instanceof ItemInfo.Tip)) {
                    l.add((ItemInfo.Tip) inf);
                }
            }
            Resource.Pagina pag = res.get().layer(Resource.pagina);
            if (pag != null) {
                String text = ezFix.matcher(pag.text).find() ? pag.text.replaceAll(ezFix.pattern(), "$1" + "$2" + ",h=1ln" + "$3") : pag.text;
                l.add(new ItemInfo.Pagina(this, text));
            }
            return (l.render());
        }
    }

    private void recount() {
        int u = 0;
        for (Action act : acts)
            u += act.u;
        count = PUtils.strokeTex(Text.num12boldFnd.render(String.format("= %d/%d", u, maxact), (u > maxact) ? Color.RED : Color.WHITE));
    }

    public static class ImageInfoBox extends Widget {
        private Tex img;
        private Indir<Tex> loading;
        private final Scrollbar sb;

        public ImageInfoBox(Coord sz) {
            super(sz);
            sb = adda(new Scrollbar(sz.y, 0, 1), sz.x, 0, 1, 0);
        }

        public void drawbg(GOut g) {
            g.chcolor(0, 0, 0, 128);
            g.frect(Coord.z, sz);
            g.chcolor();
        }

        public Coord marg() {
            return (UI.scale(10, 10));
        }

        public void tick(double dt) {
            if (loading != null) {
                try {
                    set(loading.get());
                    loading = null;
                } catch (Loading l) {
                }
            }
            super.tick(dt);
        }

        public void draw(GOut g) {
            drawbg(g);
            if (img != null)
                g.image(img, marg().sub(0, sb.val));
            super.draw(g);
        }

        public void set(Tex img) {
            this.img = img;
            if (img != null) {
                sb.max = img.sz().y + (marg().y * 2) - sz.y;
                sb.val = 0;
            } else {
                sb.max = sb.val = 0;
            }
        }

        public void set(Indir<Tex> loading) {
            this.loading = loading;
        }

        public boolean mousewheel(Coord c, int amount) {
            sb.ch(amount * 20);
            return (true);
        }

        public void resize(Coord sz) {
            super.resize(sz);
            sb.c = new Coord(sz.x - sb.sz.x, 0);
            sb.resize(sz.y);
            set(img);
        }
    }

    private static final Tex[] add = {Resource.loadtex("gfx/hud/buttons/addu"),
            Resource.loadtex("gfx/hud/buttons/addd")};
    private static final Tex[] sub = {Resource.loadtex("gfx/hud/buttons/subu"),
            Resource.loadtex("gfx/hud/buttons/subd")};

    public class Actions extends Listbox<Action> implements DTarget {
        private boolean loading = false;
        private int da = -1, ds = -1;
        UI.Grab d = null;
        Action drag = null;
        Coord dp;

        public Actions(int w, int h) {
            super(w, h, attrf.height() + UI.scale(2));
        }

        protected Action listitem(int n) {
            Set<String> filterSet = null;
            switch (filter) {
                case 1:
                    filterSet = attacks;
                    break;
                case 2:
                    filterSet = restorations;
                    break;
                case 3:
                    filterSet = maneuvers;
                    break;
                case 4:
                    filterSet = moves;
                    break;
            }
            if (filterSet == null)
                return acts.get(n);

            int num = 0;
            for (int i = 0; i < acts.size(); i++) {
                try {
                    if (filterSet.contains(acts.get(i).res.get().name) && num++ == n)
                        return acts.get(i);
                } catch (Loading l) {
                }
            }

            return (acts.get(n));
        }

        protected int listitems() {
            Set<String> filterSet = null;
            switch (filter) {
                case 1:
                    filterSet = attacks;
                    break;
                case 2:
                    filterSet = restorations;
                    break;
                case 3:
                    filterSet = maneuvers;
                    break;
                case 4:
                    filterSet = moves;
                    break;
            }

            if (filterSet == null)
                return acts.size();

            int num = 0;
            for (int i = 0; i < acts.size(); i++) {
                try {
                    if (filterSet.contains(acts.get(i).res.get().name))
                        num++;
                } catch (Loading l) {
                }
            }
            return num;
        }

        protected void drawbg(GOut g) {
        }

        protected void drawitem(GOut g, Action act, int idx) {
            g.chcolor((idx % 2 == 0) ? CharWnd.every : CharWnd.other);
            g.frect(Coord.z, g.sz);
            g.chcolor();
            if (act.ru == null)
                act.ru = PUtils.strokeTex(attrf.render(String.format("%d/%d", act.u, act.a)));

            try {
                if (act.ri == null)
                    act.ri = new TexI(PUtils.convolvedown(act.rendericon(), new Coord(itemh, itemh), CharWnd.iconfilter));
                g.image(act.ri, Coord.z);
            } catch (Loading l) {
                g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, new Coord(itemh, itemh));
            }
            int ty = (itemh - act.rnm.sz().y) / 2;
            g.image(act.rnm, new Coord(itemh + UI.scale(2), ty));

            if (act.ra == null)
                act.ra = PUtils.strokeTex(Text.num12boldFnd.render(String.valueOf(act.a)));
            g.aimage(act.ra, new Coord(sz.x - UI.scale(15), ty), 1.0, 0.0);
            g.aimage(act.ru, new Coord(sz.x - UI.scale(45), ty), 1.0, 0.0);
            g.aimage(add[da == idx ? 1 : 0], new Coord(sz.x - UI.scale(10), itemh / 2), 1.0, 0.5);
            g.aimage(sub[ds == idx ? 1 : 0], new Coord(sz.x - UI.scale(25), itemh / 2), 1.0, 0.5);
        }

        public void change(final Action act) {
            if (act != null)
                info.set(() -> new TexI(act.renderinfo(info.sz.x - 20)));
            else if (sel != null)
                info.set((Tex) null);
            super.change(act);
        }

        public boolean mousewheel(Coord c, int am) {
            if (ui.modshift) {
                Action act = itemat(c);
                if (act != null)
                    setu(act, act.u - am);
                return (true);
            }
            return (super.mousewheel(c, am));
        }

        public void draw(GOut g) {
            if (loading) {
                loading = false;
                for (Action act : acts) {
                    try {
                        Resource res = act.res.get();
                        act.name = res.layer(Resource.tooltip).t;
                        act.rnm = PUtils.strokeTex(attrf.render(act.name));
                    } catch (Loading l) {
                        act.name = "...";
                        act.rnm = attrf.render(act.name).tex();
                        loading = true;
                    }
                }

                Collections.sort(acts, new Comparator<Action>() {
                    public int compare(Action a, Action b) {
                        int ret = a.name.compareTo(b.name);
                        return (ret);
                    }
                });
            }
            if ((drag != null) && (dp == null)) {
                try {
                    final Tex dt = drag.res.get().layer(Resource.imgc).tex();
                    ui.drawafter(new UI.AfterDraw() {
                        public void draw(GOut g) {
                            g.image(dt, ui.mc.add(dt.sz().div(2).inv()));
                        }
                    });
                } catch (Loading l) {
                }
            }
            super.draw(g);
        }

        private boolean onadd(Coord c, int idx) {
            Coord ic = c.sub(0, (idx - sb.val) * itemh);
            int by = (itemh - add[0].sz().y) / 2;
            return (ic.isect(new Coord(sz.x - UI.scale(10) - add[0].sz().x, by), add[0].sz()));
        }

        private boolean onsub(Coord c, int idx) {
            Coord ic = c.sub(0, (idx - sb.val) * itemh);
            int by = (itemh - sub[0].sz().y) / 2;
            return (ic.isect(new Coord(sz.x - UI.scale(25) - add[0].sz().x, by), add[0].sz()));
        }

        public void drag(Action act) {
            if (d == null)
                d = ui.grabmouse(this);
            drag = act;
            dp = null;
        }

        public boolean mousedown(Coord c, int button) {
            if (button == 1) {
                int idx = (c.y / itemh) + sb.val;
                if (idx < listitems()) {
                    if (onadd(c, idx)) {
                        da = idx;
                        d = ui.grabmouse(this);
                        return (true);
                    } else if (onsub(c, idx)) {
                        ds = idx;
                        d = ui.grabmouse(this);
                        return (true);
                    }
                }
                super.mousedown(c, button);
                if ((sel != null) && (c.x < sb.c.x)) {
                    d = ui.grabmouse(this);
                    drag = sel;
                    dp = c;
                }
                return (true);
            }
            return (super.mousedown(c, button));
        }

        public void mousemove(Coord c) {
            super.mousemove(c);
            if ((drag != null) && (dp != null)) {
                if (c.dist(dp) > 5)
                    dp = null;
            }
        }

        private boolean setu(Action act, int u) {
            u = Utils.clip(u, 0, act.a);
            int s;
            for (s = 0; s < order.length; s++) {
                if (order[s] == act)
                    break;
            }
            if (u > 0) {
                if (s == order.length) {
                    for (s = 0; s < order.length; s++) {
                        if (order[s] == null)
                            break;
                    }
                    if (s == order.length)
                        return (false);
                    order[s] = act;
                }
            } else {
                if (s < order.length)
                    order[s] = null;
            }
            act.u(u);
            return (true);
        }

        public boolean mouseup(Coord c, int button) {
            if ((d != null) && (button == 1)) {
                d.remove();
                d = null;
                if (drag != null) {
                    if (dp == null)
                        ui.dropthing(ui.root, c.add(rootpos()), drag);
                    drag = null;
                }
                if (da >= 0) {
                    if (onadd(c, da)) {
                        Action act = listitem(da);
                        setu(act, act.u + 1);
                    }
                    da = -1;
                } else if (ds >= 0) {
                    if (onsub(c, ds)) {
                        Action act = listitem(ds);
                        setu(act, act.u - 1);
                    }
                    ds = -1;
                }
                return (true);
            }
            return (super.mouseup(c, button));
        }

        public boolean drop(Coord cc, Coord ul) {
            return (false);
        }

        public boolean iteminteract(Coord cc, Coord ul) {
            Action act = itemat(cc);
            if (act != null)
                FightWnd.this.wdgmsg("itemact", act.id, ui.modflags());
            return (true);
        }
    }

    public int findorder(Action a) {
        for (int i = 0; i < order.length; i++) {
            if (order[i] == a)
                return (i);
        }
        return (-1);
    }

    public static final String[] keys = {"1", "2", "3", "4", "5", "\u21e71", "\u21e72", "\u21e73", "\u21e74", "\u21e75"};
    public static final String[] keysf = {"F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10"};
    public static final String[] keysi = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};

    public class BView extends Widget implements DropTarget {
        private int subp = -1;
        private int addp = -1;
        private final int subOffX = UI.scale(3);
        private final int addOffX = UI.scale(16);
        private final int subOffY = invsq.sz().y + UI.scale(10 + 10);
        private UI.Grab d = null;
        private Action drag = null;
        private Coord dp;
        private final Coord[] animoff = new Coord[order.length];
        private final double[] animpr = new double[order.length];
        private boolean anim = false;

        private BView() {
            super(new Coord(((invsq.sz().x + UI.scale(2)) * (order.length - 1)) + (UI.scale(10) * ((order.length - 1) / 5)) + UI.scale(60), 0).add(invsq.sz().x, invsq.sz().y + UI.scale(35)));
        }

        private Coord itemc(int i) {
            return (new Coord(((invsq.sz().x + UI.scale(2)) * i) + (UI.scale(10) * (i / 5)), 0));
        }

        private int citem(Coord c) {
            for (int i = 0; i < order.length; i++) {
                if (c.isect(itemc(i), invsq.sz()))
                    return (i);
            }
            return (-1);
        }

        private int csub(Coord c) {
            for (int i = 0; i < order.length; i++) {
                if (c.isect(itemc(i).add(subOffX, subOffY), sub[0].sz()))
                    return (i);
            }
            return (-1);
        }

        private int cadd(Coord c) {
            for (int i = 0; i < order.length; i++) {
                if (c.isect(itemc(i).add(addOffX, subOffY), add[0].sz()))
                    return (i);
            }
            return (-1);
        }

        final Tex[] keystex = new Tex[10];
        final Tex[] keysftex = new Tex[10];

        {
            for (int i = 0; i < 10; i++) {
                keystex[i] = Text.render(FightWnd.keys[i]).tex();
                if (i < 5)
                    keysftex[i] = keystex[i];
                else
                    keysftex[i] = Text.render(FightWnd.keysf[i - 5]).tex();
            }
        }

        final Tex[] keysfftex = new Tex[10];
        final Tex[] keysitex = new Tex[10];

        {
            for (int i = 0; i < 10; i++) {
                keysfftex[i] = Text.render(FightWnd.keysf[i]).tex();
                keysitex[i] = Text.render(FightWnd.keysi[i]).tex();
            }
        }

        public void draw(GOut g) {
            int pcy = invsq.sz().y + UI.scale(4);

            int[] reo;
            if (anim) {
                reo = new int[order.length];
                for (int i = 0, a = 0, b = order.length - 1; i < order.length; i++) {
                    if (animoff[i] == null)
                        reo[a++] = i;
                    else
                        reo[b--] = i;
                }
            }

            for (int i = 0; i < order.length; i++) {
                Coord c = itemc(i);
                g.image(invsq, c);
                Action act = order[i];
                try {
                    if (act != null) {
                        Coord ic = c.add(1, 1);
                        if (animoff[i] != null)
                            ic = ic.add(animoff[i].mul(Math.pow(1.0 - animpr[i], 3)));

                        g.image(act.res.get().layer(Resource.imgc).tex(), ic);

                        if (act.ru == null)
                            act.ru = PUtils.strokeTex(Text.num12boldFnd.render(String.format("%d/%d", act.u, act.a)));

                        g.image(act.ru, c.add(invsq.sz().x / 2 - act.ru.sz().x / 2, pcy));
                        g.chcolor();

                        g.image(sub[subp == i ? 1 : 0], c.add(subOffX, subOffY));
                        g.image(add[addp == i ? 1 : 0], c.add(addOffX, subOffY));
                    }
                } catch (Loading l) {
                }
                g.chcolor(156, 180, 158, 255);

                Tex keytex;
                if (Config.combatkeys == 0) {
                    keytex = keystex[i];
                } else if (Config.combatkeys == 1) {
                    keytex = keystex[i];
                } else if (Config.combatkeys == 2) {
                    keytex = keysfftex[i];
                } else {
                    keytex = keysitex[i];
                }

                g.aimage(keytex, c.add(invsq.sz().sub(2, 0)), 1, 1);
                g.chcolor();
            }

            g.image(count, new Coord(UI.scale(370), pcy));

            if ((drag != null) && (dp == null)) {
                try {
                    final Tex dt = drag.res.get().layer(Resource.imgc).tex();
                    ui.drawafter(new UI.AfterDraw() {
                        public void draw(GOut g) {
                            g.image(dt, ui.mc.add(dt.sz().div(2).inv()));
                        }
                    });
                } catch (Loading l) {
                }
            }
        }

        public boolean mousedown(Coord c, int button) {
            int s = citem(c);

            if (button == 3) {
                if (s >= 0) {
                    if (order[s] != null)
                        order[s].u(0);
                    order[s] = null;
                    return (true);
                }
            } else if (button == 1) {
                int acti = csub(c);
                if (acti >= 0) {
                    subp = acti;
                    return true;
                }
                acti = cadd(c);
                if (acti >= 0) {
                    addp = acti;
                    return true;
                }

                if (s >= 0) {
                    Action act = order[s];
                    actlist.change(act);
                    actlist.display();

                    d = ui.grabmouse(this);
                    drag = order[s];
                    dp = c;
                    return true;
                }
            }
            return (super.mousedown(c, button));
        }

        public void mousemove(Coord c) {
            super.mousemove(c);
            if (drag != null && dp != null) {
                if (c.dist(dp) > 5)
                    dp = null;
            }
        }

        public boolean mouseup(Coord c, int button) {
            subp = -1;
            addp = -1;

            int s = csub(c);
            if (s >= 0) {
                Action act = order[s];
                if (act != null) {
                    if (act.u == 1) {
                        if (order[s] != null)
                            order[s].u(0);
                        order[s] = null;
                    } else {
                        act.u(act.u - 1);
                    }
                    return true;
                }
            }

            s = cadd(c);
            if (s >= 0) {
                Action act = order[s];
                if (act != null) {
                    act.u(act.u + 1);
                    return true;
                }
            }

            if (d != null && button == 1) {
                d.remove();
                d = null;
                if (drag != null) {
                    if (dp == null)
                        ui.dropthing(ui.root, c.add(rootpos()), drag);
                    drag = null;
                }
                return true;
            }

            return (super.mouseup(c, button));
        }

        private void animate(int s, Coord off) {
            animoff[s] = off;
            animpr[s] = 0.0;
            anim = true;
        }

        public boolean dropthing(Coord c, Object thing) {
            if (thing instanceof Action) {
                Action act = (Action) thing;
                int s = citem(c);
                if (s < 0)
                    return (false);
                if (order[s] != act) {
                    int cp = findorder(act);
                    if (cp >= 0)
                        order[cp] = order[s];
                    if (order[s] != null) {
                        if (cp >= 0) {
                            animate(cp, itemc(s).sub(itemc(cp)));
                        } else {
                            order[s].u(0);
                        }
                    }
                    order[s] = act;
                    if (act.u < 1)
                        act.u(1);
                }
                return (true);
            }
            return (false);
        }

        public void tick(double dt) {
            if (anim) {
                boolean na = false;
                for (int i = 0; i < order.length; i++) {
                    if (animoff[i] != null) {
                        if ((animpr[i] += (dt * 3)) > 1.0)
                            animoff[i] = null;
                        else
                            na = true;
                    }
                }
                anim = na;
            }
        }
    }

    public class Savelist extends Listbox<Integer> implements ReadLine.Owner {
        private int edit = -1;
        private Text.Line redit = null;
        private ReadLine nmed;
        private double focusstart;

        public Savelist(int w, int h) {
            super(w, h, attrf.height() + 2);
            setcanfocus(true);
            sel = Integer.valueOf(0);
        }

        protected Integer listitem(int idx) {
            return (idx);
        }

        protected int listitems() {
            return (nsave);
        }

        protected void drawbg(GOut g) {
        }

        protected void drawitem(GOut g, Integer save, int n) {
            g.chcolor((n % 2 == 0) ? CharWnd.every : CharWnd.other);
            g.frect(Coord.z, g.sz);
            g.chcolor();
            if (n == edit) {
                if (redit == null)
                    redit = attrf.render(nmed.line());
                g.aimage(redit.tex(), new Coord(UI.scale(20), itemh / 2), 0.0, 0.5);
                if (hasfocus && (((Utils.rtime() - focusstart) % 1.0) < 0.5)) {
                    int cx = redit.advance(nmed.point());
                    g.chcolor(255, 255, 255, 255);
                    Coord co = new Coord(UI.scale(20) + cx + UI.scale(1), (g.sz.y - redit.sz().y) / 2);
                    g.line(co, co.add(0, redit.sz().y), 1);
                    g.chcolor();
                }
            } else {
                g.aimage(saves[n].tex(), new Coord(UI.scale(20), itemh / 2), 0.0, 0.5);
            }
            if (n == usesave)
                g.aimage(Theme.timg("chkbox/small", 1), new Coord(itemh / 2, itemh / 2), 0.5, 0.5);
        }

        private Coord lc = null;
        private double lt = 0;

        public boolean mousedown(Coord c, int button) {
            boolean ret = super.mousedown(c, button);
            if (ret && (button == 1)) {
                double now = Utils.rtime();
                if (((now - lt) < 0.5) && (c.dist(lc) < UI.scale(10)) && (sel != null) && (saves[sel] != unused)) {
                    if (sel == usesave) {
                        edit = sel;
                        nmed = ReadLine.make(this, saves[sel].text);
                        redit = null;
                        parent.setfocus(this);
                        focusstart = now;
                    } else {
                        load(sel);
                        use(sel);
                    }
                } else {
                    lt = now;
                    lc = c;
                }
            }
            return (ret);
        }

        public void done(ReadLine buf) {
            saves[edit] = attrf.render(buf.line());
            edit = -1;
            nmed = null;
        }

        public void changed(ReadLine buf) {
            redit = null;
        }

        public void change(Integer sel) {
            super.change(sel);
            if ((edit != -1) && (edit != sel)) {
                edit = -1;
                redit = null;
                nmed = null;
            }
        }

        public boolean keydown(KeyEvent ev) {
            if (edit != -1) {
                if (ev.getKeyChar() == 27) {
                    edit = -1;
                    redit = null;
                    nmed = null;
                    return (true);
                } else {
                    return (nmed.key(ev));
                }
            }
            return (super.keydown(ev));
        }
    }

    @RName("fmg")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            return (new FightWnd((Integer) args[0], (Integer) args[1], (Integer) args[2]));
            //  return(new FightWndEx((Integer)args[0], (Integer)args[1], (Integer)args[2]));
        }
    }

    public void load(int n) {
        wdgmsg("load", n);
    }

    public void save(int n) {
        List<Object> args = new LinkedList<Object>();
        args.add(n);
        if (saves[n] != unused)
            args.add(saves[n].text);

        for (int i = 0; i < order.length; i++) {
            if (order[i] == null) {
                args.add(null);
            } else {
                args.add(order[i].id);
                args.add(order[i].u);
            }
        }
        wdgmsg("save", args.toArray(new Object[0]));
    }

    public void use(int n) {
        wdgmsg("use", n);
    }

    private final Pair[] filterDropdownVals = new Pair[]{
            new Pair<>("- All -", 0),
            new Pair<>("Attacks", 1),
            new Pair<>("Restorations", 2),
            new Pair<>("Maneuvers", 3),
            new Pair<>("Moves", 4)
    };

    @SuppressWarnings("unchecked")
    private Dropbox<Pair<String, Integer>> getFilterDropdown() {
        List<String> values = Arrays.stream(filterDropdownVals).map(x -> x.a.toString()).collect(Collectors.toList());
        Dropbox<Pair<String, Integer>> filterDropdown = new Dropbox<Pair<String, Integer>>(filterDropdownVals.length, values) {
            @Override
            protected Pair<String, Integer> listitem(int i) {
                return filterDropdownVals[i];
            }

            @Override
            protected int listitems() {
                return filterDropdownVals.length;
            }

            @Override
            protected void drawitem(GOut g, Pair<String, Integer> item, int i) {
                g.text(item.a, Coord.z);
            }

            @Override
            public void change(Pair<String, Integer> item) {
                super.change(item);
                filter = item.b;
                if (actlist != null)
                    actlist.sb.val = 0;
            }
        };
        filterDropdown.change(filterDropdownVals[0]);
        return filterDropdown;
    }

    public FightWnd(int nsave, int nact, int max) {
        super(Coord.z);
        this.nsave = nsave;
        this.maxact = max;
        this.order = new Action[nact];
        this.saves = new Text[nsave];
        for (int i = 0; i < nsave; i++)
            saves[i] = unused;

        Dropbox<Pair<String, Integer>> filterDropdown = getFilterDropdown();
        add(filterDropdown, new Coord(UI.scale(270 + 230) - filterDropdown.sz.x, UI.scale(15)));
        Frame.around(this, Collections.singletonList(filterDropdown));

        schoolsDropdown = new Dropbox<Pair<Text, Integer>>(UI.scale(250), saves.length, saves[0].sz().y) {
            @Override
            protected Pair<Text, Integer> listitem(int i) {
                return new Pair<>(saves[i], i);
            }

            @Override
            protected int listitems() {
                return saves.length;
            }

            @Override
            protected void drawitem(GOut g, Pair<Text, Integer> item, int i) {
                g.image(item.a.tex(), Coord.z);
            }

            @Override
            public void change2(Pair<Text, Integer> item) {
                super.change2(item);
            }

            @Override
            public void change(Pair<Text, Integer> item) {
                super.change2(item);
                load(item.b);
                use(item.b);
            }
        };


        info = add(new ImageInfoBox(UI.scale(223, 152)), UI.scale(5, 35).add(wbox.btloff()));
        Frame.around(this, Collections.singletonList(info));

        add(CharWnd.settip(new Img(CharWnd.catf.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Martial Arts & Combat Schools")).tex()), "gfx/hud/chr/tips/combat"), 0, 0);
        actlist = add(new Actions(UI.scale(235), actionsListHeight()), UI.scale(265, 35).add(wbox.btloff()));
        Frame.around(this, Collections.singletonList(actlist));
        Widget p = add(new BView(), UI.scale(77, 200));

        add(schoolsDropdown, UI.scale(10, 280));
        Frame.around(this, Collections.singletonList(schoolsDropdown));

        add(new Button(UI.scale(110), "Save", false) {
            public void click() {
                Pair<Text, Integer> sel = schoolsDropdown.sel;
                if (sel != null) {
                    save(sel.b);
                    use(sel.b);
                }
            }
        }, UI.scale(280, 277));
        add(new Button(UI.scale(110), "Rename", false) {
            public void click() {
                Pair<Text, Integer> sel = schoolsDropdown.sel;
                if (sel == null || sel.a.text.equals(Resource.getLocString(Resource.BUNDLE_LABEL, "unused save")))
                    return;

                Window renwnd = new Window(UI.scale(225, 100), "Rename School") {
                    {
                        final TextEntry txtname = new TextEntry(UI.scale(200), sel.a.text);
                        add(txtname, UI.scale(15, 20));

                        Button add = new Button(UI.scale(60), "Save") {
                            @Override
                            public void click() {
                                saves[sel.b] = attrf.render(txtname.text());
                                schoolsDropdown.sel = new Pair<>(saves[sel.b], sel.b);
                                save(sel.b);
                                parent.reqdestroy();
                            }
                        };
                        add(add, UI.scale(15, 60));

                        Button cancel = new Button(UI.scale(60), "Cancel") {
                            @Override
                            public void click() {
                                parent.reqdestroy();
                            }
                        };
                        add(cancel, UI.scale(155, 60));
                    }

                    @Override
                    public void wdgmsg(Widget sender, String msg, Object... args) {
                        if (sender == cbtn)
                            reqdestroy();
                        else
                            super.wdgmsg(sender, msg, args);
                    }

                    @Override
                    public boolean type(char key, KeyEvent ev) {
                        if (key == 27) {
                            reqdestroy();
                            return true;
                        }
                        return super.type(key, ev);
                    }
                };
                ui.gui.add(renwnd, new Coord(ui.gui.sz.x / 2 - UI.scale(200), ui.gui.sz.y / 2 - UI.scale(200)));
                renwnd.show();
            }
        }, UI.scale(395, 277));

        pack();
    }

    public void changebutton(Integer index) {
        try {
            if (!saves[index].text.equals("unused save")) {
                schoolsDropdown.change(new Pair(saves[index], index));
                PBotUtils.sysMsg(ui, "Switched to deck : " + saves[index].text, Color.white);
            } else
                PBotUtils.sysMsg(ui, "This is not a saved deck, not switching.", Color.white);
        } catch (Exception e) {
            PBotUtils.sysLogAppend(ui, "Exception switching combat decks, exception ignored to avoid crash.", "white");
        }
    }

    private static int actionsListHeight() {
        switch (Resource.language) {
            default:
            case "en":
                return Config.iswindows ? 7 : 8;
            case "ru":
                return 8;
            case "zh":
                return Config.iswindows ? 7 : 8;
            case "ko":
                return 7;
        }
    }

    public Action findact(int resid) {
        for (Action act : acts) {
            if (act.id == resid)
                return (act);
        }
        return (null);
    }

    private final Text unused = new Text.Foundry(Text.sans.deriveFont(Font.ITALIC, UI.scale(14))).aa(true).render(Resource.getLocString(Resource.BUNDLE_LABEL, "unused save"));

    public void uimsg(String nm, Object... args) {
        if (nm == "avail") {
            List<Action> acts = new ArrayList<Action>();
            int a = 0;
            while (true) {
                int resid = (Integer) args[a++];
                if (resid < 0)
                    break;
                int av = (Integer) args[a++];
                Action pact = findact(resid);
                if (pact == null) {
                    acts.add(new Action(ui.sess.getres(resid), resid, av, 0));
                } else {
                    acts.add(pact);
                    pact.a(av);
                }
            }
            this.acts = acts;
            actlist.loading = true;
        } else if (nm == "tt") {
            Indir<Resource> res = ui.sess.getres((Integer) args[0]);
            Object[] rawinfo = (Object[]) args[1];
            actrawinfo.put(res, rawinfo);
        } else if (nm == "used") {
            int a = 0;
            for (Action act : acts)
                act.u(0);
            for (int i = 0; i < order.length; i++) {
                int resid = (Integer) args[a++];
                if (resid < 0) {
                    order[i] = null;
                    continue;
                }
                int us = (Integer) args[a++];
                (order[i] = findact(resid)).u(us);
            }
        } else if (nm == "saved") {
            int fl = (Integer) args[0];
            for (int i = 0; i < nsave; i++) {
                if ((fl & (1 << i)) != 0) {
                    if (args[i + 1] instanceof String)
                        saves[i] = attrf.render((String) args[i + 1]);
                    else
                        saves[i] = attrf.render(String.format("Saved school %d", i + 1));
                } else {
                    saves[i] = unused;
                }
            }
        } else if (nm == "use") {
            int i = (int) args[0];
            if (i >= 0 && i < saves.length)
                schoolsDropdown.change2(new Pair<>(saves[i], i));
        } else if (nm == "max") {
            maxact = (Integer) args[0];
            recount();
        } else {
            super.uimsg(nm, args);
        }
    }
}
