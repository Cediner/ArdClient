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

import haven.purus.pbot.PBotInventory;
import haven.purus.pbot.PBotItem;
import haven.purus.pbot.PBotUtils;
import haven.res.ui.tt.q.qbuff.QBuff;
import modification.InventoryListener;
import modification.ItemObserver;
import modification.configuration;

import java.awt.Color;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class Inventory extends Widget implements DTarget2, ItemObserver, InventoryListener {
    public static final Coord sqsz = UI.scale(33, 33);
    public static final Tex invsq/* = Resource.loadtex("gfx/hud/invsq")*/;
    private static final Set<String> PLAYER_INVENTORY_NAMES = new HashSet<>(Arrays.asList("Inventory", "Belt", "Equipment", "Character Sheet", "Study"));
    public boolean dropul = true;
    public Coord isz;
    public boolean[] sqmask = null;
    public static final Comparator<WItem> ITEM_COMPARATOR_ASC = (o1, o2) -> {
        QualityList ql1 = o1.itemq.get();
        double q1 = (ql1 != null && !ql1.isEmpty()) ? ql1.single(QualityList.SingleType.Average).value : 0;

        QualityList ql2 = o2.itemq.get();
        double q2 = (ql2 != null && !ql2.isEmpty()) ? ql2.single(QualityList.SingleType.Average).value : 0;

        return Double.compare(q1, q2);
    };
    public static final Comparator<WItem> ITEM_COMPARATOR_DESC = (o1, o2) -> ITEM_COMPARATOR_ASC.compare(o2, o1);

    public boolean locked = false;
    public Map<GItem, WItem> wmap = Collections.synchronizedMap(new HashMap<>());

    static {
        Coord sz = sqsz.add(1, 1);
        WritableRaster buf = PUtils.imgraster(sz);
        for (int i = 1, y = sz.y - 1; i < sz.x - 1; i++) {
            buf.setSample(i, 0, 0, 20);
            buf.setSample(i, 0, 1, 28);
            buf.setSample(i, 0, 2, 21);
            buf.setSample(i, 0, 3, 167);
            buf.setSample(i, y, 0, 20);
            buf.setSample(i, y, 1, 28);
            buf.setSample(i, y, 2, 21);
            buf.setSample(i, y, 3, 167);
        }
        for (int i = 1, x = sz.x - 1; i < sz.y - 1; i++) {
            buf.setSample(0, i, 0, 20);
            buf.setSample(0, i, 1, 28);
            buf.setSample(0, i, 2, 21);
            buf.setSample(0, i, 3, 167);
            buf.setSample(x, i, 0, 20);
            buf.setSample(x, i, 1, 28);
            buf.setSample(x, i, 2, 21);
            buf.setSample(x, i, 3, 167);
        }
        for (int y = 1; y < sz.y - 1; y++) {
            for (int x = 1; x < sz.x - 1; x++) {
                buf.setSample(x, y, 0, 36);
                buf.setSample(x, y, 1, 52);
                buf.setSample(x, y, 2, 38);
                buf.setSample(x, y, 3, 125);
            }
        }
        invsq = new TexI(PUtils.rasterimg(buf));
    }

    @RName("inv")
    public static class $_ implements Factory {
        @Override
        public Widget create(UI ui, Object[] args) {
            return (new Inventory((Coord) args[0]));
        }
    }

    public final Map<String, Tex> cached = new HashMap<>();

    @Override
    public void draw(GOut g) {
        Coord c = new Coord();
        int mo = 0;
        int cell = 0;
        for (c.y = 0; c.y < isz.y; c.y++) {
            for (c.x = 0; c.x < isz.x; c.x++) {
                if ((sqmask != null) && sqmask[mo++])
                    continue;
                g.image(invsq, c.mul(sqsz));
                if (configuration.showinvnumber)
                    g.aimage(cached.computeIfAbsent(Integer.toString(++cell), s -> Text.render(s, new Color(255, 255, 255, 100)).tex()), c.mul(sqsz).add(invsq.sz().div(2)), 0.5, 0.5);
            }
        }
        super.draw(g);
    }

    public final AltInventory ainv;

    public Inventory(Coord sz) {
//        super(invsq.sz().add(new Coord(-1, -1)).mul(sz).add(new Coord(1, 1)));
        super(sqsz.mul(sz).add(1, 1));
        isz = sz;
        ainv = new AltInventory(this);
        add(ainv, Coord.of(this.sz.x, 0));
        ainv.hide();
    }

    @Override
    protected void added() {
        super.added();
        if (ainv.visible()) {
            Coord max = sqsz.mul(isz).add(1, 1);
            max.x = Math.max(max.x, ainv.c.x + ainv.sz.x);
            max.y = Math.max(max.y, ainv.c.y + ainv.sz.y);
            resize(max);
        }
        listeners.add(this);
    }

    @Override
    public void reqdestroy() {
        super.reqdestroy();
        listeners.remove(this);
    }

    @Override
    public void resize(final Coord sz) {
        super.resize(sz);
    }

    @Override
    public void pack() {
//        super.pack();
        Coord invsz = sqsz.mul(isz).add(1, 1);
        Coord max = invsz;
        if (ainv.visible()) {
            ainv.list.resizeh(invsz.y);
            ainv.move(Coord.of(invsz.x, 0));
            max.x = Math.max(max.x, ainv.c.x + ainv.sz.x);
            max.y = Math.max(max.y, ainv.c.y + ainv.sz.y);
        }
        resize(max);
        parent.pack();
    }

    @Override
    public boolean mousewheel(Coord c, int amount) {
        Coord invsz = sqsz.mul(isz).add(1, 1);
        if (c.isect(Coord.z, invsz)) {
            if (ui.modshift) {
                Inventory minv = getparent(GameUI.class).maininv;
                if (minv != this) {
                    if (amount < 0)
                        wdgmsg("invxf", minv.wdgid(), 1);
                    else if (amount > 0)
                        minv.wdgmsg("invxf", this.wdgid(), 1);
                }
            }
            return (true);
        }
        return (super.mousewheel(c, amount));
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        return !locked && super.mousedown(c, button);
    }

    @Override
    public void addchild(Widget child, Object... args) {
        add(child);
        Coord c = (Coord) args[0];
        if (child instanceof GItem) {
            GItem i = (GItem) child;
            WItem w = new WItem(i);
            wmap.put(i, add(w, c.mul(sqsz).add(1, 1)));
            i.addListeners(listeners());
            observers().forEach(InventoryListener::dirty);
        }
    }

    @Override
    public void cdestroy(Widget w) {
        super.cdestroy(w);
        if (w instanceof GItem) {
            GItem i = (GItem) w;
            WItem wItem = wmap.remove(i);
            ui.destroy(wItem);
            i.removeListeners(listeners());
            observers().forEach(InventoryListener::dirty);
        }
    }

    @Override
    public boolean drop(WItem target, Coord cc, Coord ul) {
        for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
            if (wdg.visible()) {
                if (wdg instanceof DTarget) {
                    Coord ccc = cc.sub(wdg.c);
                    Coord ulc = ul.sub(wdg.c);
                    if (ccc.isect(Coord.z, wdg.sz)) {
                        if (((DTarget) wdg).drop(ccc, ulc))
                            return (true);
                    }
                } else if (wdg instanceof DTarget2) {
                    Coord ccc = cc.sub(wdg.c);
                    Coord ulc = ul.sub(wdg.c);
                    if (ccc.isect(Coord.z, wdg.sz)) {
                        if (((DTarget2) wdg).drop(target, ccc, ulc))
                            return (true);
                    }
                }
            }
        }
        Coord dc = dropul ? ul.add(sqsz.div(2)).div(sqsz) : cc.div(sqsz);
        wdgmsg("drop", dc);
        return (true);
    }

    @Override
    public boolean iteminteract(WItem target, Coord cc, Coord ul) {
        for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
            if (wdg.visible()) {
                if (wdg instanceof DTarget) {
                    Coord ccc = cc.sub(wdg.c);
                    Coord ulc = ul.sub(wdg.c);
                    if (ccc.isect(Coord.z, wdg.sz)) {
                        if (((DTarget) wdg).iteminteract(ccc, ulc))
                            break;
                    }
                } else if (wdg instanceof DTarget2) {
                    Coord ccc = cc.sub(wdg.c);
                    Coord ulc = ul.sub(wdg.c);
                    if (ccc.isect(Coord.z, wdg.sz)) {
                        if (((DTarget2) wdg).iteminteract(target, ccc, ulc))
                            break;
                    }
                }
            }
        }
        return (true);
    }

    @Override
    public void uimsg(String msg, Object... args) {
        if (msg == "sz") {
            isz = (Coord) args[0];
            pack();
            sqmask = null;
        } else if (msg == "mask") {
            boolean[] nmask;
            if (args[0] == null) {
                nmask = null;
            } else {
                nmask = new boolean[isz.x * isz.y];
                byte[] raw = (byte[]) args[0];
                for (int i = 0; i < isz.x * isz.y; i++)
                    nmask[i] = (raw[i >> 3] & (1 << (i & 7))) != 0;
            }
            this.sqmask = nmask;
        } else if (msg == "mode") {
            dropul = (((Integer) args[0]) == 0);
        } else {
            super.uimsg(msg, args);
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        final boolean check = args.length > 0 && args[0] instanceof GItem;
        if (check && msg.equals("drop-identical")) {
            GItem gitem = (GItem) args[0];
            Color colorIdentical = null;
            for (WItem item : getIdenticalItems(gitem, false)) {
                try {
                    if (Config.dropcolor) {
                        if (gitem.quality() != null) {
                            if (gitem.quality().color != null) {
                                if (colorIdentical == null) {
                                    colorIdentical = gitem.quality().color;
                                }
                                if (!item.qq.color.equals(colorIdentical)) {
                                    continue;
                                }
                            }
                        }
                    }
                    item.item.wdgmsg("drop", Coord.z);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } else if (check && msg.startsWith("transfer-identical")) {
//            Window stockpile = ui.gui.getwnd("Stockpile");
//            Window smelter = ui.gui.getwnd("Ore Smelter");
//            Window kiln = ui.gui.getwnd("Kiln");
//            if (stockpile == null || smelter != null || kiln != null) {
            GItem gitem = (GItem) args[0];
            boolean eq = msg.endsWith("eq");
            List<WItem> items = getIdenticalItems(gitem, eq);
            if (!eq) {
                int asc = msg.endsWith("asc") ? 1 : -1;
                try {
                    items.sort((a, b) -> {
                        QBuff aq = a.item.quality();
                        QBuff bq = b.item.quality();
                        if (aq == null || bq == null) {
                            if (aq == null && bq == null) return (0);
                            else if (bq == null) return (asc);
                            else return (-asc);
                        } else {
                            if (aq.q == bq.q) return (0);
                            else if (aq.q > bq.q) return (asc);
                            else return (-asc);
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }

            Color colorIdentical = null;

            Object[] invxf2Args = tryBuildInvxf2Args();

            for (WItem item : items) {
                try {
                    if (Config.transfercolor) {
                        if (gitem.quality() != null) {
                            if (gitem.quality().color != null) {
                                if (colorIdentical == null) {
                                    colorIdentical = gitem.quality().color;
                                }
                                if (item.qq == null || !item.qq.color.equals(colorIdentical)) {
                                    continue;
                                }
                            }
                        } else {
                            if (item.qq != null) {
                                continue;
                            }
                        }
                    }
                    item.item.wdgmsg("transfer", Coord.z);
                    PBotItem pbItem = new PBotItem(item);
                    if (pbItem.isStack()) {
                        invxf2Args[1] = pbItem.getStackContents().size();
                        pbItem.getStackContents().get(0).gitem.wdgmsg("invxf2", invxf2Args);
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
//            } else {
//                for (Widget w = stockpile.lchild; w != null; w = w.prev) {
//                    if (w instanceof ISBox) {
//                        ISBox isb = (ISBox) w;
//                        int freespace = isb.getfreespace();
//                        for (WItem item : getIdenticalItems((GItem) args[0])) {
//                            if (freespace-- <= 0)
//                                break;
//                            item.item.wdgmsg("take", new Coord(item.sz.x / 2, item.sz.y / 2));
//                            isb.drop(null, null);
//                        }
//                        break;
//                    }
//                }
//            }
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    private Object[] tryBuildInvxf2Args() {
        List<Integer> transferTargetIds = getTransferTargetWidgetIds();
        if (transferTargetIds.isEmpty()) {
            return null;
        }

        Object[] xf2args = new Object[2 + transferTargetIds.size()];
        xf2args[0] = 0;
        xf2args[1] = 1;
        for (int i = 0; i < transferTargetIds.size(); i++) {
            xf2args[i+2] = transferTargetIds.get(i);
        }
        return xf2args;
    }

    private List<Integer> getTransferTargetWidgetIds() {
        List<Integer> externalInventoryIds = PBotUtils.getAllInventories(ui).stream().filter(i -> {
                    Window window = i.inv.getparent(Window.class);
                    if (window == null || window.cap == null || window.cap.text == null) {
                        return false;
                    }

                    return !PLAYER_INVENTORY_NAMES.contains(window.cap.text);
                }).map(i -> i.inv.wdgid())
                .collect(Collectors.toList());

        List<Integer> stockpiles = ui.widgets.values().stream().filter(i -> i instanceof ISBox).map(Widget::wdgid).collect(Collectors.toList());

        externalInventoryIds.addAll(stockpiles);
        return externalInventoryIds;
    }

    public List<WItem> getIdenticalItems(GItem item) {
        List<WItem> items = new ArrayList<WItem>();
        GSprite sprite = item.spr();
        if (sprite != null) {
            String name = sprite.getname();
            String resname = item.resource().name;
            for (Widget wdg = child; wdg != null; wdg = wdg.next) {
                if (wdg instanceof WItem) {
                    if (!((WItem) wdg).locked()) {
                        sprite = ((WItem) wdg).item.spr();
                        if (sprite != null) {
                            Resource res = ((WItem) wdg).item.resource();
                            if (res != null && res.name.equals(resname) && (name == null || name.equals(sprite.getname())))
                                items.add((WItem) wdg);
                        }
                    }
                }
            }
        }
        return items;
    }

    static Pattern stackPattern = Pattern.compile("(.*)(, stack of)?");

    private boolean identicalStack(String name1, String name2) {
        return (name1.replaceAll(stackPattern.pattern(), "$1").equalsIgnoreCase(name2.replaceAll(stackPattern.pattern(), "$1")));
    }

    public List<WItem> getIdenticalItems(GItem item, boolean quality) {
        List<WItem> items = new ArrayList<WItem>();
        double q0 = 0;
        if (quality) {
            QBuff aq = item.quality();
            if (aq != null)
                q0 = aq.q;
        }
        GSprite sprite = item.spr();
        if (sprite != null) {
            String name = sprite.getname();
            String resname = item.resource().name;
            for (Widget wdg = child; wdg != null; wdg = wdg.next) {
                if (wdg instanceof WItem) {
                    if (!((WItem) wdg).locked()) {
                        GItem it = ((WItem) wdg).item;
                        sprite = it.spr();
                        if (sprite != null) {
                            Resource res = it.resource();
                            if (res != null && res.name.equals(resname) && (name == null || identicalStack(name, sprite.getname()))) {
                                if (quality) {
                                    QBuff bq = it.quality();
                                    if (bq != null) {
                                        double q1 = bq.q - q0;
                                        if (q1 < 0.1 && q1 > -0.1)
                                            items.add((WItem) wdg);
                                    }
                                } else {
                                    items.add((WItem) wdg);
                                }
                            }
                        }
                    }
                }
            }
        }
        return items;
    }

    /* Following getItem* methods do partial matching of the name *on purpose*.
       Because when localization is turned on, original English name will be in the brackets
       next to the translation
    */
    public List<WItem> getItemsPartial(String... names) {
        List<WItem> items = new ArrayList<WItem>();
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem) wdg).item.getname();
                for (String name : names) {
                    if (wdgname.contains(name)) {
                        items.add((WItem) wdg);
                        break;
                    }
                }
            }
        }
        return items;
    }

    public WItem getItemPartial(String name) {
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem) wdg).item.getname();
                if (wdgname.contains(name))
                    return (WItem) wdg;
            }
        }
        return null;
    }

    public WItem getItemPartialTrays(String name) {
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem) wdg).item.getname();
                if (wdgname.contains(name))
                    return (WItem) wdg;
            }
        }
        return null;
    }

    public WItem getItemPartialDrink(String name) {
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem) wdg).item.getname();
                if (wdgname.contains(name))
                    if (!PBotUtils.canDrinkFrom((WItem) wdg))
                        return null;
                if (PBotUtils.canDrinkFrom((WItem) wdg)) {
                    return (WItem) wdg;
                }
            }
        }
        return null;
    }

    public int getItemPartialCount(String name) {
        int count = 0;
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem) wdg).item.getname();
                if (wdgname.contains(name))
                    count++;
            }
        }
        return count;
    }

    public int getFreeSpace() {
        int freespace = getMaxSlots();
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem)
                freespace -= (wdg.sz.x * wdg.sz.y) / (sqsz.x * sqsz.y);
        }
        return freespace;
    }

    // Null if no free slots found
    public Coord getFreeSlot() {
        int[][] invTable = new int[isz.x][isz.y];
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem item = (WItem) wdg;
                for (int i = 0; i < item.sz.div(sqsz).y; i++)
                    for (int j = 0; j < item.sz.div(sqsz).x; j++)
                        invTable[item.c.div(sqsz).x + j][item.c.div(sqsz).y + i] = 1;
            }
        }
        int mo = 0;
        for (int i = 0; i < isz.y; i++) {
            for (int j = 0; j < isz.x; j++) {
                if ((sqmask != null) && sqmask[mo++]) continue;
                if (invTable[j][i] == 0)
                    return (new Coord(j, i));
            }
        }
        return (null);
    }

    public List<Coord> getFreeSlots() {
        List<Coord> cordlist = new ArrayList<>();
        int[][] invTable = new int[isz.x][isz.y];
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem item = (WItem) wdg;
                for (int i = 0; i < item.sz.div(sqsz).y; i++)
                    for (int j = 0; j < item.sz.div(sqsz).x; j++)
                        invTable[item.c.div(sqsz).x + j][item.c.div(sqsz).y + i] = 1;
            }
        }
        int mo = 0;
        for (int i = 0; i < isz.y; i++) {
            for (int j = 0; j < isz.x; j++) {
                if ((sqmask != null) && sqmask[mo++]) continue;
                if (invTable[j][i] == 0) cordlist.add(new Coord(j, i));
            }
        }
        return (cordlist);
    }

    public int getMaxSlots() {
        Coord c = new Coord();
        int mo = 0;
        int max = 0;
        for (c.y = 0; c.y < isz.y; c.y++) {
            for (c.x = 0; c.x < isz.x; c.x++) {
                if (sqmask == null || !sqmask[mo++]) max++;
            }
        }
        return (max);
    }

    public boolean drink(int threshold) {
        IMeter.Meter stam = ui.gui.getmeter("stam", 0);
        if (stam == null || stam.a > threshold)
            return (false);

        List<WItem> containers = getItemsPartial("Waterskin", "Waterflask", "Kuksa");
        for (WItem wi : containers) {
            ItemInfo.Contents cont = wi.item.getcontents();
            if (cont != null) {
                FlowerMenu.setNextSelection("Drink");
                ui.lcc = wi.rootpos();
                wi.item.wdgmsg("iact", wi.c, 0);
                return (true);
            }
        }

        return (false);
    }

    public static Coord invsz(Coord sz) {
        return invsq.sz().add(new Coord(-1, -1)).mul(sz).add(new Coord(1, 1));
    }

    public static Coord sqroff(Coord c) {
        return c.div(invsq.sz());
    }

    public static Coord sqoff(Coord c) {
        return c.mul(invsq.sz());
    }

    public void stack() {
        Set<String> ignore = new HashSet<>();
        PBotInventory inv = new PBotInventory(this);
        while (true) {
            try {
                List<PBotItem> items = inv.getInventoryContents().stream().filter(p -> !p.isStack()).filter(p -> !ignore.contains(p.getResname())).collect(Collectors.toList());
                if (!items.isEmpty()) {
                    PBotItem item = items.get(0);
                    String name = item.getResname();
                    List<PBotItem> resItems = inv.getInventoryItemsByResnames(name).stream().filter(p -> !p.isStack()).filter(p -> !p.coord().equals(item.coord())).collect(Collectors.toList());
                    if (!resItems.isEmpty()) {
                        if (item.takeItem(10)) {
                            PBotUtils.sleep(250);
                            resItems.get(0).itemact(3);
                            if (!waitHandOut(250)) {
                                if (!inv.dropItemToInventory(item.coord(), 250)) {
                                    PBotUtils.sysMsg(ui, "Stack broken");
                                }
                            }
                        }
                    }
                    ignore.add(name);
                } else break;
            } catch (Loading l) {}
            PBotUtils.sleep(50);
        }
        PBotUtils.sysMsg(ui, "Stack resolved");
    }

    public void unstack() {
        PBotInventory inv = new PBotInventory(this);
        while (true) {
            try {
                List<PBotItem> stacks = inv.getInventoryContents().stream().filter(p -> p.isStack()).collect(Collectors.toList());
                if (!stacks.isEmpty()) {
                    PBotItem stack = stacks.get(0);
                    if (inv.freeSpaceForItem(stack) != null) {
                        stack.activateItem();
                    } else break;
                } else break;
            } catch (Loading l) {}
            PBotUtils.sleep(50);
        }
    }

    private boolean waitHandOut(int time) {
        for (int i = 0; i < time && PBotUtils.getItemAtHand(ui) != null; i += 25) PBotUtils.sleep(25);
        return (PBotUtils.getItemAtHand(ui) == null);
    }

    /**
     * Sorting system
     */
    public Window sortingWindow() {
        Window window = new Window(Coord.z, "Sorting");
        final String[] t = {"reverse", "none", "normal"};
        Label ql = new Label("Quality ");
        Label qt = new Label("normal    ");
        HSlider quality = new HSlider(UI.scale(50), 0, 2, 2) {
            @Override
            public void changed() {
                qt.settext(t[val]);
            }
        };
        Label rl = new Label("ResName ");
        Label rt = new Label("normal    ");
        HSlider resname = new HSlider(UI.scale(50), 0, 2, 2) {
            @Override
            public void changed() {
                rt.settext(t[val]);
            }
        };
        Label nl = new Label("Name ");
        Label nt = new Label("normal    ");
        HSlider name = new HSlider(UI.scale(50), 0, 2, 2) {
            @Override
            public void changed() {
                nt.settext(t[val]);
            }
        };
        Button sort = new Button("Sorting") {
            @Override
            public void click() {
                Defer.later(() -> {
                    try {
                        String s = "";
                        if (quality.val == 2)
                            s = s.concat("q");
                        else if (quality.val == 0)
                            s = s.concat("!q");
                        if (resname.val == 2)
                            s = s.concat("r");
                        else if (resname.val == 0)
                            s = s.concat("!r");
                        if (name.val == 2)
                            s = s.concat("n");
                        else if (name.val == 0)
                            s = s.concat("!n");
                        sort(s);
                    } catch (Exception e) {
                        e.printStackTrace();
                        PBotUtils.sysMsg(ui, "Sorting Error!");
                    }
                    return (null);
                });
                window.reqdestroy();
            }
        };
        WidgetVerticalAppender wva = new WidgetVerticalAppender(window);
        wva.addRow(ql, qt, quality);
        wva.addRow(rl, rt, resname);
        wva.addRow(nl, nt, name);
        wva.add(sort);
        window.pack();
        return (window);
    }

    public void sort(String s) { //string with q n r
        PBotUtils.sysMsg(ui, "Sorting! Please don't move!");
        List<InvItem> items = new ArrayList<>();
        List<Integer> ignoreSlots = new ArrayList<>();
        Coord c1 = Coord.of(1);
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                InvItem item = new InvItem((WItem) wdg);
                Coord sz = item.getSize();
                if (!sz.equals(c1)) {
                    Coord slot = item.getSlot();
                    for (int y = 0; y < sz.y; y++) {
                        for (int x = 0; x < sz.x; x++) {
                            ignoreSlots.add(coordToSloti(slot.add(x, y)));
                        }
                    }
//                    PBotUtils.sysMsg(ui, "Not support large items! " + item.getName());
//                    return;
                    continue;
                }
                items.add(item);
            }
        }

        items.sort(Comparator.comparing(InvItem::getSloti));
        if (s.contains("!n"))
            items.sort((o1, o2) -> o2.getName().compareTo(o1.getName()));
        else if (s.contains("n"))
            items.sort(Comparator.comparing(InvItem::getName));
        if (s.contains("!q"))
            items.sort((o1, o2) -> {
                if (o1.getQuality() == null && o2.getQuality() == null) return (0);
                if (o1.getQuality() == null && o2.getQuality() != null) return (-1);
                if (o1.getQuality() != null && o2.getQuality() == null) return (1);
                return Objects.requireNonNull(o2.getQuality()).compareTo(o1.getQuality());
            });
        else if (s.contains("q"))
            items.sort((o1, o2) -> {
                if (o1.getQuality() == null && o2.getQuality() == null) return (0);
                if (o1.getQuality() == null && o2.getQuality() != null) return (1);
                if (o1.getQuality() != null && o2.getQuality() == null) return (-1);
                return Objects.requireNonNull(o1.getQuality()).compareTo(o2.getQuality());
            });

        if (s.contains("!r"))
            items.sort((o1, o2) -> o2.getResname().compareTo(o1.getResname()));
        else if (s.contains("r"))
            items.sort(Comparator.comparing(InvItem::getResname));

        sort:
        for (int i = 0; i < items.size(); i++) {
            InvItem invItem = items.get(i);
            InvItem iItem = getItem(invItem.getSlot());
            AtomicInteger targetSloti = new AtomicInteger(i);
            ignoreSlots.stream().filter(sl -> sl <= targetSloti.get()).forEach(sl -> targetSloti.getAndIncrement());
            if (invItem.equals(iItem) && invItem.getSloti() != targetSloti.get()) {
                if (invItem.take() == null)
                    break;
                InvItem item = getItem(slotiToCoord(targetSloti.get()));
                if (!drop(slotiToCoord(targetSloti.get())))
                    break;
                while (item != null && ui.gui.vhand != null) {
                    Integer in = getInt(items, item, ignoreSlots);
                    if (in == null)
                        break;
                    item = getItem(slotiToCoord(in));
                    if (!drop(slotiToCoord(in)))
                        break sort;
                }
            }
        }
        PBotUtils.sysMsg(ui, "Sorting finished!");
    }

    public Integer getInt(List<InvItem> items, InvItem item, List<Integer> ignoreSlots) {
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).equals(item)) {
                AtomicInteger targetSloti = new AtomicInteger(i);
                ignoreSlots.stream().filter(sl -> sl <= targetSloti.get()).forEach(sl -> targetSloti.getAndIncrement());
                return (targetSloti.get());
            }
        return (null);
    }

    public InvItem getItem(Coord slot) {
        InvItem item = null;
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem w = (WItem) wdg;
                if (w.c.sub(1, 1).div(sqsz.x, sqsz.y).equals(slot)) {
                    item = new InvItem(w);
                    break;
                }
            }
        }
        return (item);
    }

    public boolean drop(Coord slot) {
        InvItem item = getItem(slot);
        wdgmsg("drop", slot);
        InvItem nitem = getItem(slot);
        for (int sleep = 10; nitem == null || nitem == item; ) {
            nitem = getItem(slot);
            PBotUtils.sleep(sleep);
        }
        return (true);
    }

    public Coord slotiToCoord(int slot) {
        Coord c = new Coord();
        int mo = 0;
        int max = 0;
        for (c.y = 0; c.y < isz.y; c.y++) {
            for (c.x = 0; c.x < isz.x; c.x++) {
                if (sqmask == null || !sqmask[mo++]) {
                    if (slot == max) return (c);
                    else max++;
                }
            }
        }
        return (null);
    }

    public Integer coordToSloti(Coord slot) {
        Coord c = new Coord();
        int mo = 0;
        int max = 0;
        for (c.y = 0; c.y < isz.y; c.y++) {
            for (c.x = 0; c.x < isz.x; c.x++) {
                if (sqmask == null || !sqmask[mo++]) {
                    if (slot.x == c.x && slot.y == c.y) return (max);
                    else max++;
                }
            }
        }
        return (null);
    }

    public class InvItem {
        private final WItem wItem;
        private GItem gItem;
        private String resname;
        private String name;
        private boolean qinit = false;
        private Double quality;
        private Coord slot;
        private Integer sloti;
        private Coord size;

        public InvItem(WItem wItem) {
            this.wItem = wItem;
        }

        public WItem getWItem() {
            return (this.wItem);
        }

        public GItem getGItem() {
            if (this.gItem == null)
                this.gItem = getWItem().item;
            return (this.gItem);
        }

        public String getResname() {
            if (this.resname == null) {
                Resource res = null;
                for (int sleep = 10; res == null; ) {
                    res = getGItem().resource();
                    PBotUtils.sleep(sleep);
                }
                this.resname = res.name;
            }
            return (this.resname);
        }

        public String getName() {
            if (this.name == null) {
                Optional<String> n = getGItem().name();
                for (int sleep = 10; !n.isPresent(); ) {
                    n = getGItem().name();
                    PBotUtils.sleep(sleep);
                }
                this.name = n.get();
            }
            return (this.name);
        }

        public Double getQuality() {
            if (!this.qinit) {
                QBuff qBuff = getGItem().quality();
                this.quality = qBuff == null ? null : qBuff.q;
                this.qinit = true;
            }
            return (this.quality);
        }

        public Coord getSlot() {
            if (this.slot == null) {
                this.slot = getWItem().c.sub(1, 1).div(sqsz.x, sqsz.y);
            }
            return (this.slot);
        }

        public Integer getSloti() {
            if (this.sloti == null) {
                this.sloti = coordToSloti(getSlot());
            }
            return (this.sloti);
        }

        public Coord getSize() {
            if (this.size == null) {
                GSprite spr = null;
                for (int sleep = 10; spr == null; ) {
                    spr = getGItem().spr();
                    PBotUtils.sleep(sleep);
                }
                this.size = spr.sz().div(UI.scale(30));
            }
            return (this.size);
        }

        public WItem take() {
            for (int i = 0; i < 5; i++) {
                getGItem().wdgmsg("take", Coord.z);

                for (int t = 0, sleep = 10; ui.gui.vhand == null; t += sleep) {
                    if (t >= 1000)
                        break;
                    else
                        PBotUtils.sleep(sleep);
                }
                if (ui.gui.vhand != null)
                    break;
            }
            return (ui.gui.vhand);
        }

        @Override
        public String toString() {
            return "InvItem{" +
                    "wItem=" + wItem +
                    ", resname='" + resname + '\'' +
                    ", name='" + name + '\'' +
                    ", quality=" + quality +
                    ", slot=" + slot +
                    ", sloti=" + sloti +
                    ", slotrollback=" + slotiToCoord(sloti) +
                    ", size=" + size +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InvItem)) return false;
            InvItem invItem = (InvItem) o;
            return getWItem().equals(invItem.getWItem());
        }

        @Override
        public int hashCode() {
            return Objects.hash(wItem);
        }
    }

    public void openStacks() {
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem w = (WItem) wdg;
                if (w.item.contents != null) w.item.showcontwnd(true);
            }
        }
    }

    public void closeStacks() {
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem w = (WItem) wdg;
                if (w.item.contents != null) w.item.showcontwnd(false);
            }
        }
    }

    public static class AltInventory extends Widget implements DTarget2 {
        private static final Color even = new Color(255, 255, 255, 16);
        private static final Color odd = new Color(255, 255, 255, 32);

        private final Inventory inv;
        public final ItemGroupList list;
        public Dropbox<Grouping> dropGroup = new Dropbox<Grouping>(UI.scale(60), 16, UI.scale(16)) {
            @Override
            protected Grouping listitem(final int i) {
                return (Grouping.values()[i]);
            }

            @Override
            protected int listitems() {
                return (Grouping.values().length);
            }

            @Override
            protected void drawitem(final GOut g, final Grouping item, final int i) {
                Tex tex = Text.render(item.name).tex();
                g.image(tex, Coord.of(0, (itemh - tex.sz().y) / 2));
            }

            @Override
            public void change(final int index) {
                super.change(index);
                inv.dirty();
            }
        };
        public Dropbox<Sorting> dropSort = new Dropbox<Sorting>(UI.scale(60), 16, UI.scale(16)) {
            @Override
            protected Sorting listitem(final int i) {
                return (Sorting.values()[i]);
            }

            @Override
            protected int listitems() {
                return (Sorting.values().length);
            }

            @Override
            protected void drawitem(final GOut g, final Sorting item, final int i) {
                Tex tex = Text.render(item.name).tex();
                g.image(tex, Coord.of(0, (itemh - tex.sz().y) / 2));
            }

            @Override
            public void change(final int index) {
                super.change(index);
                inv.dirty();
            }
        };

        public AltInventory(final Inventory inv) {
            this.inv = inv;
            add(dropSort, Coord.of(0, 0)).settip("List Sorting");
            add(dropGroup, dropSort.pos("ur").adds(5, 0)).settip("Item Picking On Quality");
            list = add(new ItemGroupList(inv, this, UI.scale(150), 16, UI.scale(16)), dropSort.pos("bl"));
            list.resizeh(inv.sz.y);
            dropGroup.change(0);
            dropSort.change(0);
            super.pack();
        }

        @Override
        public void draw(final GOut g) {
            super.draw(g);
            g.chcolor(even);
            g.rect(Coord.z, g.sz());
            g.chcolor();
        }

        @Override
        public void tick(final double dt) {
            if (!visible()) return;
            super.tick(dt);
        }

        @Override
        public boolean drop(final WItem target, final Coord cc, final Coord ul) {
            for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
                if (wdg.visible()) {
                    if (wdg instanceof DTarget) {
                        Coord ccc = cc.sub(wdg.c);
                        Coord ulc = ul.sub(wdg.c);
                        if (ccc.isect(Coord.z, wdg.sz)) {
                            if (((DTarget) wdg).drop(ccc, ulc))
                                return (true);
                        }
                    } else if (wdg instanceof DTarget2) {
                        Coord ccc = cc.sub(wdg.c);
                        Coord ulc = ul.sub(wdg.c);
                        if (ccc.isect(Coord.z, wdg.sz)) {
                            if (((DTarget2) wdg).drop(target, ccc, ulc))
                                return (true);
                        }
                    }
                }
            }
            return (false);
        }

        @Override
        public boolean iteminteract(final WItem target, final Coord cc, final Coord ul) {
            for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
                if (wdg.visible()) {
                    if (wdg instanceof DTarget) {
                        Coord ccc = cc.sub(wdg.c);
                        Coord ulc = ul.sub(wdg.c);
                        if (ccc.isect(Coord.z, wdg.sz)) {
                            if (((DTarget) wdg).iteminteract(ccc, ulc))
                                return (true); ;
                        }
                    } else if (wdg instanceof DTarget2) {
                        Coord ccc = cc.sub(wdg.c);
                        Coord ulc = ul.sub(wdg.c);
                        if (ccc.isect(Coord.z, wdg.sz)) {
                            if (((DTarget2) wdg).iteminteract(target, ccc, ulc))
                                return (true);
                        }
                    }
                }
            }
            return (false);
        }

        public enum Grouping {
            NORMAL("Ascending", Comparator.comparingDouble(o -> {
                QBuff qb = ItemInfo.find(QBuff.class, o.item.info());
                return (qb == null ? 0.0 : qb.q);
            })),
            REVERSED("Descending", NORMAL.cmp.reversed());

            private final String name;
            private final Comparator<WItem> cmp;

            Grouping(String name, Comparator<WItem> cmp) {
                this.name = name;
                this.cmp = cmp;
            }
        }

        public enum Sorting {
            NONE("None", Comparator.comparingInt(g -> 0)),
            COUNT("Count", Comparator.<Group>comparingInt(g -> g.count).reversed()),
            NAME("Name", Comparator.comparing(g -> g.name)),
            RESNAME("ResName", Comparator.comparing(g -> g.resname)),
            Q("Quality", Comparator.<Group>comparingDouble(g -> g.q).reversed());

            private final String name;
            private final Comparator<Group> cmp;

            Sorting(String name, Comparator<Group> cmp) {
                this.name = name;
                this.cmp = cmp;
            }
        }

        public static class Group extends Widget {
            private final String name;
            private final String resname;
            private int count;
            private double q;
            private boolean nq;
            private GSprite spr;
            private final List<WItem> items = Collections.synchronizedList(new ArrayList<>());
            private static final Tex qimg = Resource.remote().loadwait("ui/tt/q/quality").layer(Resource.imgc, 0).tex();

            public Group(final String name, final String resname) {
                this.name = name;
                this.resname = resname;
            }

            public void addItem(final WItem item) {
                items.add(item);
                count++;
                if (spr == null)
                    spr = item.item.spr();
            }

            public void calcQuality() {
                List<Double> stream = items.stream().mapToDouble(i -> {
                    QBuff qb = ItemInfo.find(QBuff.class, i.item.info());
                    return (qb == null ? 0 : qb.q);
                }).boxed().collect(Collectors.toList());
                double sum = stream.stream().mapToDouble(d -> d).sum();
                q = sum / items.size();
                nq = stream.stream().allMatch(d -> d != q);
            }

            private WItem takeFirst() {
                return (items.get(0));
            }

            @Override
            public boolean mousedown(final Coord c, final int button) {
                return (takeFirst().mousedown(c, button));
            }

            @Override
            public boolean mouseup(final Coord c, final int button) {
                return (takeFirst().mouseup(c, button));
            }

            @Override
            public void mousemove(final Coord c) {
                takeFirst().mousemove(c);
            }

            @Override
            public boolean mousewheel(final Coord c, final int amount) {
                return (takeFirst().mousewheel(c, amount));
            }

            private static final Map<String, Tex> TEX = Collections.synchronizedMap(new WeakHashMap<>());
            private static Tex create(String text) {
                return (TEX.computeIfAbsent(text, s -> Text.render(s).tex()));
            }

            private final Text.UTex<String> ucount = new Text.UTex<>(() -> "x" + count, s -> Text.render(s).tex());
            private final Text.UTex<String> uq = new Text.UTex<>(() -> (nq ? "~" : "") + Utils.odformat2(q, 2), s -> Text.render(s).tex());

            public void draw(final GOut g, final Coord sz) {
                int x = 0;
                GSprite spr = this.spr;
                if (spr != null) {
                    Coord ssz = spr.sz();
                    double sy = 1.0 * ssz.y / sz.y;
                    ssz = ssz.div(sy);
                    spr.draw(g, ssz);
                    x += ssz.x;
                }
                int count = this.count;
                if (count > 0) {
                    Tex tex = ucount.get();
                    g.image(tex, Coord.of(x, (sz.y - tex.sz().y) / 2));
                    x += tex.sz().x;
                }
                int right = 0;
                int w = sz.x;
                double q = this.q;
                if (q > 0) {
                    Tex tex = uq.get();
                    g.image(tex, Coord.of(w - tex.sz().x, (sz.y - tex.sz().y) / 2));
                    g.aimage(qimg, Coord.of(w - tex.sz().x, (sz.y - tex.sz().y) / 2), 1, 0.05);
                    right = tex.sz().x + qimg.sz().x;
                }
                String name = this.name;
                if (!name.isEmpty()) {
                    x += 5;
                    int max = w - x - right - 5;
                    for (int j = 0; j < name.length(); j++) {
                        if (j != 0) {
                            name = name.substring(0, name.length() - 1 - j).concat("...");
                        }
                        if (Text.std.strsize(name).x <= max)
                            break;
                    }
                    Tex tex = create(name);
                    g.image(tex, Coord.of(x, (sz.y - tex.sz().y) / 2));
                    x += tex.sz().x;
                }
            }
        }

        public static class ItemGroupList extends Listbox<Group> implements DTarget2 {
            private final Inventory inv;
            private final AltInventory ainv;
            private List<Group> wlist = Collections.emptyList();

            public ItemGroupList(Inventory inv, AltInventory ainv, int w, int h, int itemh) {
                super(w, h, itemh);
                this.inv = inv;
                this.ainv = ainv;
            }

            @Override
            protected Group listitem(int i) {
                return (wlist.get(i));
            }

            @Override
            protected int listitems() {
                return (wlist.size());
            }

            @Override
            protected void drawitem(GOut g, Group item, int i) {
                g.chcolor(((i % 2) == 0) ? even : odd);
                g.frect(Coord.z, g.sz());
                g.chcolor();
                item.draw(g, Coord.of(sz.x - (sb.vis() ? sb.sz.x : 0), itemh));
            }

            @Override
            public void dispose() {
                super.dispose();
            }

            @Override
            public void tick(double dt) {
                super.tick(dt);
                if (inv.dirty) {
                    inv.dirty = false;
                    try {
                        List<WItem> wlist = new ArrayList<>();
                        for (WItem wItem : inv.wmap.values()) {
                            Widget cont = wItem.item.contents;
                            if (cont != null) {
                                wlist.addAll(cont.getchilds(WItem.class));
                            } else {
                                wlist.add(wItem);
                            }
                        }
                        Map<String, Group> wmap = new HashMap<>();
                        for (WItem wItem : wlist) {
                            ItemInfo.Name ninfo = ItemInfo.find(ItemInfo.Name.class, wItem.item.info());
                            if (ninfo == null) continue;
                            String name = ninfo.ostr.text;
                            String resname = wItem.item.resource().name;
                            Group gr = wmap.computeIfAbsent(name + resname, n -> new Group(name, resname));
                            gr.addItem(wItem);
                        }
                        for (Group g : wmap.values()) {
                            g.calcQuality();

                            g.items.sort(ainv.dropGroup.sel.cmp);
                        }
                        List<Group> list = new ArrayList<>(wmap.values());
                        list.sort(ainv.dropSort.sel.cmp);
                        this.wlist = list;
                    } catch (Loading l) {
                        inv.dirty = true;
                    }
                }
            }

            @Override
            protected void itemclick(final Group item, final int button) {
                item.mousedown(Coord.z, button);
            }

            @Override
            protected void drawbg(GOut g) {}

            @Override
            public boolean drop(final WItem target, final Coord cc, final Coord ul) {
                Coord slot = inv.getFreeSlot();
                if (slot != null)
                    inv.wdgmsg("drop", slot);
                return (true);
            }

            @Override
            public boolean iteminteract(final WItem target, final Coord cc, final Coord ul) {
                int idx = idxat(cc);
                WItem item = null;
                if (idx >= 0 && idx < listitems())
                    item = listitem(idx).takeFirst();
                if (item != null) {
                    item.iteminteract(target, Coord.z, Coord.z);
                }
                return (true);
            }

            @Override
            public boolean mousewheel(final Coord c, final int amount) {
                //if(ui.modshift) {
                //	    Inventory minv = getparent(GameUI.class).maininv;
                //	    if(amount < 0)
                //		wdgmsg("invxf", minv.wdgid(), 1);
                //	    else if(amount > 0)
                //		minv.wdgmsg("invxf", this.wdgid(), 1);
                //	}
                //	return(true);
                return super.mousewheel(c, amount);
            }

            @Override
            public Object tooltip(Coord c, Widget prev) {
                int idx = idxat(c);
                WItem item = null;
                if (idx >= 0 && idx < listitems())
                    item = listitem(idx).takeFirst();
                if (item != null) {
                    return item.tooltip(Coord.z, prev);
                }
                return super.tooltip(c, prev);
            }
        }
    }

    public void showAltInventory(boolean show) {
        ainv.show(show);
        pack();
    }

    public void toggleAltInventory() {
        ainv.show(!ainv.visible());
        pack();
    }

    private boolean dirty = true;

    @Override
    public void dirty() {
        dirty = true;
    }

    private final List<InventoryListener> listeners = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void initListeners(final List<InventoryListener> listeners) {
        this.listeners.addAll(listeners);
    }

    @Override
    public List<InventoryListener> listeners() {
        return (listeners);
    }

    private final List<InventoryListener> listeners2 = Collections.synchronizedList(new ArrayList<>());

    @Override
    public List<InventoryListener> observers() {
        return (listeners2);
    }

    @Override
    public void addListeners(final List<InventoryListener> listeners) {
        this.listeners2.addAll(listeners);
    }

    @Override
    public void removeListeners(final List<InventoryListener> listeners) {
        this.listeners2.removeAll(listeners);
    }
}
