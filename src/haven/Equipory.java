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
import haven.res.ui.tt.Armor;
import haven.res.ui.tt.wpn.Damage;
import modification.configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static haven.DefSettings.SYMMETRICOUTLINES;
import static haven.Inventory.invsq;

public class Equipory extends Widget implements DTarget {
    private static final Resource.Image bgi = Resource.loadrimg("gfx/hud/equip/bg");
    private static final int yo = Inventory.sqsz.y, sh = 10;
    private static final Tex bg = new TexI(PUtils.uiscale(bgi.img, Coord.of((sh * yo * bgi.sz.x) / bgi.sz.y, sh * yo)));
    private static final int rx = invsq.sz().x + bg.sz().x;
    private static final int acx = invsq.sz().x + bg.sz().x / 2;
    private static final Text.Foundry acf = new Text.Foundry(Text.sans, Text.cfg.def).aa(true);
    private Tex armorclass = null;
    private Tex percexp = null;
    private Tex intste = null;
    private List<GItem> checkForDrop = new LinkedList<GItem>();
    public static final Coord ecoords[] = {
            new Coord(0, 0 * yo),
            new Coord(0, 1 * yo),
            new Coord(0, 2 * yo),
            new Coord(rx, 2 * yo),
            new Coord(0, 3 * yo),
            new Coord(rx, 3 * yo),
            new Coord(0, 4 * yo),
            new Coord(rx, 4 * yo),
            new Coord(0, 5 * yo),
            new Coord(rx, 5 * yo),
            new Coord(0, 6 * yo),
            new Coord(rx, 6 * yo),
            new Coord(0, 8 * yo),
            new Coord(rx, 8 * yo),
            new Coord(0, 9 * yo),
            new Coord(rx, 9 * yo),
            new Coord(invsq.sz().x, 0 * yo),
            new Coord(rx, 0 * yo),
            new Coord(rx, 1 * yo),
            new Coord(0, 7 * yo),
            new Coord(rx, 7 * yo),
    };
    public static final Tex[] ebgs = new Tex[ecoords.length];
    public static final Text[] etts = new Text[ecoords.length];
    static Coord isz;

    static {
        isz = new Coord();
        for (Coord ec : ecoords) {
            if (ec.x + invsq.sz().x > isz.x)
                isz.x = ec.x + invsq.sz().x;
            if (ec.y + invsq.sz().y > isz.y)
                isz.y = ec.y + invsq.sz().y;
        }
        for (int i = 0; i < ebgs.length; i++) {
            Resource bgres = Resource.remote().loadwait("gfx/hud/equip/ep" + i);
            Resource.Image img = bgres.layer(Resource.imgc);
            if (img != null) {
                ebgs[i] = bgres.layer(Resource.imgc).tex();
                etts[i] = Text.render(bgres.layer(Resource.tooltip).t);
            }
        }
    }


    Map<GItem, WItem[]> wmap = new HashMap<GItem, WItem[]>();
    private final Avaview ava;
    AttrBonusesWdg bonuses;
    public WItem[] quickslots = new WItem[ecoords.length];
    public WItem[] slots = new WItem[ecoords.length];
    public QuickSlot[] quicks = new QuickSlot[ecoords.length];

    @RName("epry")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            long gobid;
            if (args.length < 1)
                gobid = -2;
            else if (args[0] == null)
                gobid = -1;
            else
                gobid = Utils.uint32((Integer) args[0]);
            return (new Equipory(gobid));
        }
    }

    protected void added() {
        if (ava.avagob == -2)
            ava.avagob = getparent(GameUI.class).plid;
        super.added();
    }

    private final IButton plus = new IButton(Theme.fullres("buttons/circular/small/add"), () -> showBonuses(true));
    private final IButton minus = new IButton(Theme.fullres("buttons/circular/small/sub"), () -> showBonuses(false));

    public void showBonuses(boolean show) {
        plus.show(!show);
        bonuses.show(show);
        minus.show(show);
    }

    public Equipory(long gobid) {
        super(isz);
        ava = add(new Avaview(bg.sz(), gobid, "equcam") {
            public boolean mousedown(Coord c, int button) {
                return (false);
            }

            public void draw(GOut g) {
                g.image(bg, Coord.z);
                super.draw(g);
            }

            Outlines outlines = new Outlines(SYMMETRICOUTLINES);

            protected void setup(RenderList rl) {
                super.setup(rl);
//                rl.add(outlines, null);
            }

            protected FColor clearcolor() {
                return (null);
            }
        }, new Coord(invsq.sz().x, 0));
        ava.color = null;
        bonuses = add(new AttrBonusesWdg(bg.sz().sub(plus.sz.x * 2, invsq.sz().y + 20)), invsq.sz().add(plus.sz.x, 5));
        adda(plus, Coord.of(rx, 0), 1, 0);
        adda(minus, Coord.of(rx, 0), 1, 0);
        plus.hide();

        for (int i = 0; i < ecoords.length; i++) {
            Coord c = ecoords[i].add(ecoords[i].x != rx ? invsq.sz().x : 0, invsq.sz().y / 2);
            quicks[i] = adda(new QuickSlot(i), c, ecoords[i].x != rx ? 0 : 1, 0.5);
            quicks[i].z(1);
            if (!(Config.quickslots && configuration.newQuickSlotWdg)) {
                quicks[i].hide();
            }
        }
    }

    public static class QuickSlot extends IButton {
        private static final List<Integer> defs = Arrays.asList(6, 7, 5, 14);
        private final int slot;
        private final String prefName;
        private boolean state;

        public QuickSlot(final int slot) {
            super(Theme.fullres("buttons/circular/small/add"), null);
            this.slot = slot;
            this.prefName = "quickslot." + slot;
            this.state = Utils.getprefb(prefName, defs.contains(slot));

            if (state) {
                String res = Theme.fullres("buttons/circular/small/sub");
                this.up = load(res, 0);
                this.down = load(res, 1);
                this.hover = load(res, 2);
            }

            action(this::click);
        }

        public boolean state() {
            return (state);
        }

        public void click() {
            boolean state = this.state;
            Utils.setprefb(prefName, this.state = !state);
            String res = Theme.fullres("buttons/circular/small/" + (state ? "add" : "sub"));
            this.up = load(res, 0);
            this.down = load(res, 1);
            this.hover = load(res, 2);
        }
    }

    @Override
    public void tick(double dt) {
        if (Config.quickbelt && ui.beltWndId == -1 && ((Window) parent).cap.toString().equals("Equipment")) {
            for (WItem itm[] : wmap.values()) {
                try {
                    if (itm.length > 0 && itm[0].item.res.get().name.endsWith("belt"))
                        itm[0].mousedown(Coord.z, 3);
                } catch (Loading l) {
                }
            }
        }
        super.tick(dt);
        try {
            if (!checkForDrop.isEmpty()) {
                GItem g = checkForDrop.get(0);
                if (g.resource().name.equals("gfx/invobjs/leech") || g.resource().name.equals("gfx/invobjs/tick")) {
                    g.drop = true;
                    //ui.gui.map.wdgmsg("drop", Coord.z);
                }
                checkForDrop.remove(0);
            }
        } catch (Resource.Loading ignore) {
        }
    }

    public static interface SlotInfo {
        public int slots();
    }

    private GItem lweap, rweap;

    public GItem getWeapon() {
        if (lweap != null && lweap.getinfo(Damage.class).isPresent()) {
            return lweap;
        } else if (rweap != null && rweap.getinfo(Damage.class).isPresent()) {
            return rweap;
        } else {
            return null;
        }
    }

    public GItem leftHand() {
        return lweap;
    }

    public GItem rightHand() {
        return rweap;
    }

    public void addchild(Widget child, Object... args) {
        if (child instanceof GItem) {
            add(child);
            GItem g = (GItem) child;
            WItem[] v = new WItem[args.length];
            for (int i = 0; i < args.length; i++) {
                int ep = (Integer) args[i];
                v[i] = quickslots[ep] = slots[ep] = add(new WItem(g), ecoords[ep].add(1, 1));
                //v[i] = add(new WItem(g), ecoords[ep].add(1, 1));
                //slots[ep] = v[i];
                //quickslots[ep] = v[i];
                switch (ep) {
                    case 6:
                        lweap = g;
                        break;
                    case 7:
                        rweap = g;
                        break;
                }
            }
            g.sendttupdate = true;
            wmap.put(g, v);
            if (Config.leechdrop)
                checkForDrop.add(g);
            if (armorclass != null) {
                armorclass.dispose();
                armorclass = null;
            }
            if (percexp != null) {
                percexp.dispose();
                percexp = null;
            }
            if (intste != null) {
                intste.dispose();
                intste = null;
            }
        } else {
            super.addchild(child, args);
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender instanceof GItem && wmap.containsKey(sender) && msg.equals("ttupdate")) {
            bonuses.update(slots);
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void cdestroy(Widget w) {
        super.cdestroy(w);
        if (w instanceof GItem) {
            GItem i = (GItem) w;
            final WItem[] witms = wmap.remove(i);
            for (WItem v : witms) {
                ui.destroy(v);
                for (int s = 0; s < slots.length; ++s) {
                    if (slots[s] == v)
                        slots[s] = null;
                    if (quickslots[s] == v)
                        quickslots[s] = null;
                }
            }
            if (lweap == i) {
                lweap = null;
            } else if (rweap == i) {
                rweap = null;
            }
            if (armorclass != null) {
                armorclass.dispose();
                armorclass = null;
            }
            if (percexp != null) {
                percexp.dispose();
                percexp = null;
            }
            if (intste != null) {
                intste.dispose();
                intste = null;
            }
            bonuses.update(slots);
        }
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "pop") {
            ava.avadesc = Composited.Desc.decode(ui.sess, args);
        } else {
            super.uimsg(msg, args);
        }
    }

    public int epat(Coord c) {
        for (int i = 0; i < ecoords.length; i++) {
            if (c.isect(ecoords[i], invsq.sz()))
                return (i);
        }
        return (-1);
    }

    public boolean drop(Coord cc, Coord ul) {
        wdgmsg("drop", epat(cc));
        return (true);
    }

    public void drawslots(GOut g) {
        int slots = 0;
        if ((ui.gui != null) && (ui.gui.vhand != null)) {
            try {
                SlotInfo si = ItemInfo.find(SlotInfo.class, ui.gui.vhand.item.info());
                if (si != null)
                    slots = si.slots();
            } catch (Loading l) {
            }
        }
        for (int i = 0; i < ecoords.length; i++) {
            if ((slots & (1 << i)) != 0) {
                g.chcolor(255, 255, 0, 64);
                g.frect(ecoords[i].add(1, 1), invsq.sz().sub(2, 2));
                g.chcolor();
            }
            g.image(invsq, ecoords[i]);
            if (ebgs[i] != null)
                g.image(ebgs[i], ecoords[i]);
        }
    }

    public Object tooltip(Coord c, Widget prev) {
        Object tt = super.tooltip(c, prev);
        if (tt != null)
            return (tt);
        int sl = epat(c);
        if (sl >= 0)
            return (etts[sl]);
        return (null);
    }

    public void draw(GOut g) {
        drawslots(g);
        super.draw(g);
        if (armorclass == null) {
            int h = 0, s = 0;
            try {
                for (int i = 0; i < quickslots.length; i++) {
                    WItem itm = quickslots[i];
                    if (itm != null) {
                        for (ItemInfo info : itm.item.info()) {
                            if (info instanceof Armor) {
                                h += ((Armor) info).hard;
                                s += ((Armor) info).soft;
                                break;
                            }
                        }
                    }
                }
                armorclass = PUtils.strokeTex(acf.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Armor Class: ") + h + "/" + s));
            } catch (Exception e) { // fail silently
            }
        }
        if (armorclass != null)
            g.aimage(armorclass, new Coord(rx - UI.scale(10), bg.sz().y - UI.scale(10)), 1, 1);
        if (percexp == null) {
            int h = 0, s = 0, x;
            CharWnd chrwdg = null;
            try {
                chrwdg = ui.gui.chrwdg;
                for (BAttrWnd.Attr attr : chrwdg.battr.base) {
                    if (attr.attr.nm.contains("prc"))
                        h = attr.attr.comp;
                }
                for (SAttrWnd.SAttr attr : chrwdg.sattr.skill)
                    if (attr.attr.nm.contains("exp"))
                        s = attr.attr.comp;
                x = h * s;
                percexp = PUtils.strokeTex(acf.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Perc*Exp: ") + x));
            } catch (Exception e) { // fail silently
            }
        }
        if (percexp != null)
            g.aimage(percexp, new Coord(rx - UI.scale(10), bg.sz().y - UI.scale(20)), 1, 1);
        if (intste == null) {
            int h = 0, s = 0, x;
            CharWnd chrwdg = null;
            try {
                chrwdg = ui.gui.chrwdg;
                for (BAttrWnd.Attr attr : chrwdg.battr.base) {
                    if (attr.attr.nm.contains("int"))
                        h = attr.attr.comp;
                }
                for (SAttrWnd.SAttr attr : chrwdg.sattr.skill)
                    if (attr.attr.nm.contains("ste"))
                        s = attr.attr.comp;
                x = h * s;
                intste = PUtils.strokeTex(acf.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Int*Ste: ") + x));
            } catch (Exception e) { // fail silently
            }
        }
        if (intste != null)
            g.aimage(intste, new Coord(rx - UI.scale(10), bg.sz().y - UI.scale(30)), 1, 1);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        return (false);
    }
}
