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
import haven.purus.pbot.PBotUtils;
import haven.purus.pbot.PBotWindowAPI;
import haven.res.ui.tt.Wear;
import haven.resutil.Curiosity;
import haven.sloth.gui.MovableWidget;
import haven.sloth.io.HiddenWndData;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static haven.DefSettings.CURIOHIGH;
import static haven.DefSettings.CURIOLOW;
import static haven.DefSettings.CURIOTARGET;
import static haven.DefSettings.HUDTHEME;
import static haven.PUtils.blurmask2;
import static haven.PUtils.rasterimg;
import static haven.Resource.cdec;

public class
Window extends MovableWidget implements DTarget {
    @Resource.LayerName("windowconfig")
    public static class WindowConfig extends Resource.Layer {
        final Coord tlc;
        final Coord brc;
        final Coord capc;
        final Coord btnc;

        public WindowConfig(Resource res, Message buf) {
            res.super();
            tlc = cdec(buf);
            brc = cdec(buf);
            capc = cdec(buf);
            btnc = cdec(buf);
        }

        public void init() {
        }
    }

    // 0 = bg, 1 = bgl, 2 = bgr
    // 3 = capl, 4 = capm, 5 = capr
    // 6 = bl, 7 = br
    // 8 = l, 9 = r, 10 = b
    private static final Resource res = Theme.res("window");

    //bg, left bg, right bg
    public static final TexI bg = res.layer(Resource.imgc, 0).texi();
    public static final TexI bgl = res.layer(Resource.imgc, 1).texi();
    public static final TexI bgr = res.layer(Resource.imgc, 2).texi();
    //caption left, mid, right
    public static final TexI cl = res.layer(Resource.imgc, 3).texi();
    public static final TexI cm = res.layer(Resource.imgc, 4).texi();
    public static final TexI cr = res.layer(Resource.imgc, 5).texi();
    // bottom left, right
    public static final TexI bl = res.layer(Resource.imgc, 6).texi();
    public static final TexI br = res.layer(Resource.imgc, 7).texi();
    //left, right, bottom
    public static final TexI lm = res.layer(Resource.imgc, 8).texi();
    public static final TexI rm = res.layer(Resource.imgc, 9).texi();
    public static final TexI bm = res.layer(Resource.imgc, 10).texi();

    //top left corner, bottom right corner, caption position
    public static final WindowConfig cfg = res.layer(WindowConfig.class);
    public static boolean CurioReport = false;
    Collection Curios = new ArrayList();
    public Button checkcurios;
    public Label curiosliderlabel, studyhours;
    private Widget curiotarget, curiohigh, curiolow;
    public HSlider curioslider;
    public int curiocount = 0;
    public boolean justclose = false;
    //Large margin vs small margin
    public static final Coord dlmrgn = UI.scale(23, 14), dsmrgn = UI.scale(3, 3);
    //caption foundry
    public static final BufferedImage ctex = Resource.loadimg("gfx/hud/fonttex");
    public static final Text.Furnace cf = new Text.Imager(new PUtils.TexFurn(new Text.Foundry(Text.sans, UI.scale(15)).aa(true), ctex)) {
        protected BufferedImage proc(Text text) {
            return (rasterimg(blurmask2(text.img.getRaster(), 1, 1, Color.BLACK)));
        }
    };
    //Basic frame box
    public static final IBox wbox = new IBox(Theme.fullres("frame")) {
        final Coord co = UI.scale(3, 3), bo = UI.scale(2, 2);

        public Coord btloff() {
            return (super.btloff().sub(bo));
        }

        public Coord bbroff() {return (super.bbroff().sub(bo));}

        public Coord ctloff() {
            return (super.ctloff().sub(co));
        }

        public Coord cbroff() {return (super.cbroff().sub(co));}

        public Coord bisz() {
            return (super.bisz().sub(bo.mul(2)));
        }

        public Coord cisz() {
            return (super.cisz().sub(co.mul(2)));
        }
    };

    //margin based off large or not
    public final Coord mrgn;
    //close button
    public final IButton cbtn, lbtn;
    private IButton hbtn, mbtn;
    private final BufferedImage on, off;
    public final ArrayList<IButton> btns = new ArrayList<>();

    public boolean dt = false;
    //Caption
    public Text cap;
    public String origcap;
    //Window size, usable space top left, usable space size
    public Coord wsz, atl, asz;
    //close position, close size
    public Coord ctl, csz;
    private boolean hidable = false, hidden;
    private boolean minimized;
    private Coord fullsz;
    private Runnable destroyHook = null;
    private final Collection<Widget> twdgs = new LinkedList<>();

    @RName("wnd")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            Coord sz = (Coord) args[0];
            String cap = (args.length > 1) ? (String) args[1] : null;
            boolean lg = (args.length > 2) && ((Integer) args[2] != 0);
            if (!Config.stackwindows && cap != null && ui.gui != null && PBotWindowAPI.getWindow(ui, cap) != null) {
                return (new Window(sz, cap, cap, false, lg, Coord.z, Coord.z));
            }
            return (new Window(sz, cap, cap, lg, Coord.z, Coord.z));
        }
    }


    public Window(Coord sz, String cap, boolean lg, Coord tlo, Coord rbo) {
        this.mrgn = lg ? dlmrgn : dsmrgn;
        cbtn = add(new IButton(Theme.fullres("buttons/close"), null, this::close));
        lbtn = null;
        makeMinimizable();
        on = off = null;
        origcap = cap;

        chcap(Resource.getLocString(Resource.BUNDLE_WINDOW, cap));
        resize2(sz);
        setfocustab(true);
    }

    public static final List<String> hideableNames = Arrays.asList("Belt", "Inventory", "Equipment", "Study", "Chat", "Character Sheet", "Timers", "Basket", "Creel", "Quiver");

    public Window(Coord sz, String cap, final String moveKey, boolean lg, Coord tlo, Coord rbo) {
        super(moveKey);
        this.mrgn = lg ? dlmrgn : dsmrgn;
        cbtn = add(new IButton(Theme.fullres("buttons/close"), null, this::close));
        lbtn = add(new IButton(Theme.fullres("buttons/lock"), null, this::toggleLock));
        makeMinimizable();
        on = lbtn.hover;
        off = lbtn.up;
        origcap = cap;
        if (hideableNames.contains(origcap))
            makeHidable();
        chcap(Resource.getLocString(Resource.BUNDLE_WINDOW, cap));
        resize2(sz);

        setfocustab(true);
    }

    public Window(Coord sz, String cap, final String moveKey, boolean loadPosition, boolean lg, Coord tlo, Coord rbo) {
        this(sz, cap, moveKey, lg, tlo, rbo);
        this.loadPosition = loadPosition;
    }

    public Window(Coord sz, String cap, boolean lg) {
        this(sz, cap, lg, Coord.z, Coord.z);
    }

    public Window(Coord sz, String cap, final String moveKey, boolean loadPosition, boolean lg) {
        this(sz, cap, moveKey, loadPosition, lg, Coord.z, Coord.z);
    }

    public Window(Coord sz, String cap, final String moveKey, boolean lg) {
        this(sz, cap, moveKey, lg, Coord.z, Coord.z);
    }

    public Window(Coord sz, String cap) {
        this(sz, cap, false);
    }

    public Window(final Coord sz, final String cap, final String moveKey) {
        this(sz, cap, moveKey, false);
    }

    protected void added() {
        parent.setfocus(this);
        super.added();
        if (lbtn != null && locked()) {
            lbtn.up = on;
            lbtn.hover = off;
        }
        if (this.cap.text.equals(Resource.getLocString(Resource.BUNDLE_WINDOW, "Table"))) {
            adda(new Button(UI.scale(60), "Eat All") {
                public void click() {
                    Resource curs = ui.root.getcurs(c);
                    if (curs.name.equals("gfx/hud/curs/eat")) {
                        Map idk = getStats();
                        synchronized (ui.root.lchild) {
                            try {
                                for (Widget q = ui.root.lchild; q != null; q = q.rnext()) {
                                    if (q instanceof Inventory) {
                                        if (q.parent instanceof Window)
                                            if (!((Window) q.parent).cap.text.equals("Study")) {
                                                List<WItem> foods = getfoods((Inventory) q);
                                                for (WItem item : foods) {
                                                    if (!item.item.getname().contains("Corn")) {
                                                        GItem food = item.item;
                                                        food.wdgmsg("iact", Coord.z, -1);
                                                    }
                                                }
                                            }
                                    }
                                }

                            } catch (Exception q) {
                            }
                        }
                        PBotUtils.sleep(1000);
                        Map idk2 = getStats();
                        idk2.forEach((k, v) -> {
                            if ((Integer) idk2.get(k) - (Integer) idk.get(k) > 0) {
                                // System.out.println("Bulk Stats gained : " + k + " value : " + ((Integer) idk2.get(k) - (Integer) idk.get(k)));
                                PBotUtils.sysLogAppend(ui, "Bulk Stats gained : " + k + " value : " + ((Integer) idk2.get(k) - (Integer) idk.get(k)), "green");
                            }
                            // else
                            // System.out.println("Old : "+idk.get(k)+" new : "+v);
                        });
                    } else {
                        ui.gui.msg("Click Feast First!", Color.white);
                    }
                }

                @Override
                public void tick(final double dt) {
                    super.tick(dt);
                    if (!visible && Window.this.children().stream().filter(Inventory.class::isInstance).count() == 3)
                        show();
                }

                @Override
                public void presize() {
                    super.presize();
                    move(asz.sub(0, UI.scale(25)), 1, 1);
                }
            }, asz.sub(0, UI.scale(25)), 1, 1).hide();
        }
    }

    public void makeHidable() {
        //  hbtn = add(new IButton("custom/hud/sloth/buttons/hide", "Toggle Transparency", this::toggleHide));
        // hbtn = addBtn("buttons/hide", null, this::toggleHide);
        hbtn = addBtn_other("custom/hud/sloth/buttons/hide", "Toggle Transparency", this::toggleHide);
        if (origcap != null) {
            hidable = HiddenWndData.shouldHide(origcap);

            hidden = false;
            if (hidable) {
                final BufferedImage tmp = hbtn.down;
                hbtn.down = hbtn.up;
                hbtn.up = tmp;
            }
        }
    }

    public void toggleHide() {
        hidable = !hidable;
        hidden = false;
        final BufferedImage tmp = hbtn.down;
        hbtn.down = hbtn.up;
        hbtn.up = tmp;
        if (cap != null) {
            HiddenWndData.saveHide(cap.text, hidable);
        }
    }

    public void makeMinimizable() {
        mbtn = addBtn_other("custom/hud/sloth/buttons/minimize", "Toggle Minimize", this::toggleMinimize);
    }

    public void toggleMinimize() {
        minimized = !minimized;
        final BufferedImage tmp = mbtn.down;
        mbtn.down = mbtn.up;
        mbtn.up = tmp;

        if (minimized) {
            fullsz = asz;
            resize(Math.min(cap.sz().x + 100, asz.x), -mrgn.mul(2).y);
        } else {
            resize(fullsz);
        }
    }

    public boolean minimized() {
        return minimized;
    }


    public IButton addBtn(final String res, final String tt, final Runnable action) {
        final IButton btn = add(new IButton("res/" + Theme.fullres(res), tt, action));
        btns.add(btn);
        return btn;
    }

    public IButton addBtn_other(final String res, final String tt, final Runnable action) {
        final IButton btn = add(new IButton(res, tt, action));
        btns.add(btn);
        return btn;
    }


    public void addBtn_base(final String res, final String tt, final Runnable action) {
        btns.add(add(new IButton(res, tt, action)));
    }

    @Override
    public void toggleLock() {
        if (locked()) {
            lbtn.up = off;
            lbtn.hover = on;
        } else {
            lbtn.up = on;
            lbtn.hover = off;
        }
        super.toggleLock();
    }

    public void chcap(String cap) {
        if (cap == null)
            this.cap = null;
        else
            this.cap = cf.render(cap);
    }

    public class PButton extends Button {
        public final OptWnd.Panel tgt;
        public final int key;

        public PButton(int w, String title, int key, OptWnd.Panel tgt) {
            super(w, title);
            this.tgt = tgt;
            this.key = key;
        }

        public void click() {
            // main(tgt);
        }

        public boolean type(char key, java.awt.event.KeyEvent ev) {
            if ((this.key != -1) && (key == this.key)) {
                click();
                return (true);
            }
            return (false);
        }
    }


    public void cdraw(GOut g) {
    }

    // Input time as minutes
    String sensibleTimeFormat(Double time) {
        double rtime = time / ui.sess.glob.getTimeFac();
        StringBuilder sb = new StringBuilder();
        int days = new Double(time / 1440).intValue();
        int rdays = new Double(rtime / 1440).intValue();
        time -= days * 1440;
        rtime -= rdays * 1440;
        int hours = new Double(time / 60).intValue();
        int rhours = new Double(rtime / 60).intValue();
        time -= hours * 60;
        rtime -= rhours * 60;
        int minutes = time.intValue();
        int rminutes = (int) Math.round(rtime);

        String ts = "";
        if (days > 0) ts += days + "d";
        if (hours > 0) {
            if (!ts.isEmpty()) ts += " ";
            ts += hours + "h";
        }
        if (minutes > 0) {
            if (!ts.isEmpty()) ts += " ";
            ts += minutes + "m";
        }

        String rts = "";
        if (rdays > 0) rts += rdays + "d";
        if (rhours > 0) {
            if (!rts.isEmpty()) rts += " ";
            rts += rhours + "h";
        }
        if (rminutes > 0) {
            if (!rts.isEmpty()) rts += " ";
            rts += rminutes + "m";
        }

        sb.append(String.format("%s (%s)", ts, rts));
        return sb.toString();
    }

    String sensibleLPFormat(int LP) {
        StringBuilder sb = new StringBuilder();
        int thousands = new Double(LP / 1000).intValue();

        if (thousands > 0) {
            sb.append(thousands + "k LP");
        } else
            sb.append(LP + " LP");
        return sb.toString();
    }

    private List<WItem> getfoods(Inventory inv) {
        List<WItem> getfoods = inv.getItemsPartial("");
        return getfoods;
    }


    private static HashMap<String, Long> recentlyTakenCutlery = new HashMap<>();

    protected void drawframe(GOut g) {
        if (!HUDTHEME.get().equals("ardclient")) {
            g.chcolor(DefSettings.WNDCOL.get());
            //corners
            g.image(cl, Coord.z);
            g.image(bl, new Coord(0, sz.y - bl.sz().y));
            g.image(br, sz.sub(br.sz()));
            g.image(cr, new Coord(sz.x - cr.sz().x, 0));

            //draw background
            g.rimagev(bgl, ctl, csz.y);
            g.rimagev(bgr, ctl.add(csz.x - bgr.sz().x, 0), csz.y);
            g.rimage(bg, ctl.add(bgl.sz().x, 0), csz.sub(bgl.sz().x + bgr.sz().x, 0));

            //horizontal and vertical tiling of the long parts
            g.rimagev(lm, new Coord(0, cl.sz().y), sz.y - bl.sz().y - cl.sz().y);
            g.rimagev(rm, new Coord(sz.x - rm.sz().x, cr.sz().y), sz.y - br.sz().y - cr.sz().y);
            g.rimageh(bm, new Coord(bl.sz().x, sz.y - bm.sz().y), sz.x - br.sz().x - bl.sz().x);
            g.rimageh(cm, new Coord(cl.sz().x, 0), sz.x - cl.sz().x - cr.sz().x);
            g.chcolor();
        } else {
            g.chcolor(DefSettings.WNDCOL.get());

            //draw background
            g.rimagev(bgl, ctl, csz.y);
            g.rimagev(bgr, ctl.add(csz.x - bgr.sz().x, 0), csz.y);
            g.rimage(bg, ctl.add(bgl.sz().x, 0), csz.sub(bgl.sz().x + bgr.sz().x, 0));

            //corners
            g.image(cl, Coord.z);
            g.image(bl, new Coord(0, sz.y - bl.sz().y));
            g.image(br, sz.sub(br.sz()));
            g.image(cr, new Coord(sz.x - cr.sz().x, 0));

            //horizontal and vertical tiling of the long parts
            g.rimagev(lm, new Coord(0, cl.sz().y), sz.y - bl.sz().y - cl.sz().y);
            g.rimagev(rm, new Coord(sz.x - rm.sz().x, cr.sz().y), sz.y - br.sz().y - cr.sz().y);
            g.rimageh(bm, new Coord(bl.sz().x, sz.y - bm.sz().y), sz.x - br.sz().x - bl.sz().x);
            g.rimageh(cm, new Coord(cl.sz().x, 0), sz.x - cl.sz().x - cr.sz().x);
            g.chcolor();
        }


        try {
            if (this.cap.text.equals(Resource.getLocString(Resource.BUNDLE_WINDOW, "Table"))) {
                if (Config.savecutlery) {
                    for (Widget w = this.lchild; w != null; w = w.prev) {
                        if (w instanceof Inventory) {
                            for (WItem item : ((Inventory) w).wmap.values()) {
                                for (ItemInfo ii : item.item.info())
                                    if (ii instanceof Wear) {
                                        Wear wr = (Wear) ii;
                                        if (wr.d == wr.m - 1 && item.item.getres() != null && (!recentlyTakenCutlery.containsKey(item.item.getres().name) || System.currentTimeMillis() - recentlyTakenCutlery.get(item.item.getres().name) > 1000 * 60)) { // About to break
                                            item.item.wdgmsg("transfer", Coord.z);
                                            ui.gui.msg("Detected cutlery that is about to break! Taking to inventory! You may want to polish it.", Color.yellow);
                                            recentlyTakenCutlery.put(item.item.getres().name, System.currentTimeMillis());
                                        }
                                    }
                            }
                        }
                    }
                }
            }

            if (this.cap.text.equals(Resource.getLocString(Resource.BUNDLE_WINDOW, "Study Desk"))) {
                // Study Table total LP and durations of curiosities
                Collection GetCurios = new ArrayList(); //add curios from tables to this before parsing
                Collection FinalCurios = new ArrayList(); //parsed out list for checking against the curios you should be studying from Config.curiolist
                Collection CurioCounter = new ArrayList(); //used to see if the number of curios on the table changes to redraw the addons

                int offX = UI.scale(5);
                int sizeY = UI.scale(36 * 7 + 5);
                int totalLP = 0;
                int totalAttn = 0;
                Map<String, String> names = new HashMap<>();
                HashMap<String, Double> studyTimes = new HashMap<String, Double>();
                HashMap<String, Integer> AttnTotal = new HashMap<String, Integer>();
                List<Curio> curiolist = new ArrayList<>();
                for (Widget wdg = this.lchild; wdg != null; wdg = wdg.prev) {
                    if (wdg instanceof Inventory) {
                        for (WItem item : ((Inventory) wdg).wmap.values()) {
                            try {
                                Curiosity ci = ItemInfo.find(Curiosity.class, item.item.info());
                                totalLP += ci.exp;
                                names.put(item.item.getres().name, item.item.getname());
                                curiolist.add(new Curio(item.item.getres().name, studyTimes.get(item.item.getres().name) == null ? item.item.studytime : studyTimes.get(item.item.getres().name) + item.item.studytime, ci.exp));
                                studyTimes.put(item.item.getres().name, studyTimes.get(item.item.getres().name) == null ? item.item.studytime : studyTimes.get(item.item.getres().name) + item.item.studytime);
                                AttnTotal.put(item.item.getres().name, AttnTotal.get(item.item.getres().name) == null ? ci.mw : AttnTotal.get(item.item.getres().name));
                            } catch (NullPointerException qq) {
                            }
                        }
                    }
                }

                List<Map.Entry<String, Integer>> lst2 = AttnTotal.entrySet().stream().sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue())).collect(Collectors.toList());
                for (Map.Entry<String, Integer> entry : lst2) {
                    totalAttn += entry.getValue();
                }
                g.image(Text.labelFnd.render("Total Attention: " + String.format("%,d", totalAttn)).tex(), new Coord(offX, sizeY));
                sizeY += UI.scale(14);

                g.image(Text.labelFnd.render("Total LP: " + String.format("%,d", totalLP)).tex(), new Coord(offX, sizeY));
                sizeY += UI.scale(14);
                //iterates the curio list to only print out total study times for unique curios
                List<Map.Entry<String, Double>> lst = studyTimes.entrySet().stream().sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue())).collect(Collectors.toList());
                for (Map.Entry<String, Double> entry : lst) {
                    CurioCounter.add(entry.getKey());
                    int LP = 0;
                    for (Curio c : curiolist) {
                        if (c.CurioName.equals(entry.getKey()))
                            LP += c.LPGain;
                    }
                    if (entry.getValue() > Config.curiotimetarget * 3) {
                        g.image(Text.labelFnd.render(names.get(entry.getKey()) + ": " + sensibleTimeFormat(entry.getValue()) + " - " + sensibleLPFormat(LP), CURIOHIGH.get()).tex(), new Coord(offX, sizeY));
                        sizeY += UI.scale(15);
                        for (int i = 0; i < Curios.size(); i++) {
                            if (Curios.contains(entry.getKey())) {
                                FinalCurios.add(entry.getKey());
                            }
                        }
                    } else if (entry.getValue() < Config.curiotimetarget) {
                        g.image(Text.labelFnd.render(names.get(entry.getKey()) + ": " + sensibleTimeFormat(entry.getValue()) + " - " + sensibleLPFormat(LP), CURIOLOW.get()).tex(), new Coord(offX, sizeY));
                        sizeY += UI.scale(15);
                        for (int i = 0; i < Curios.size(); i++) {
                            if (Curios.contains(entry.getKey())) {
                                FinalCurios.add(entry.getKey());
                            }
                        }

                    } else {
                        g.image(Text.labelFnd.render(names.get(entry.getKey()) + ": " + sensibleTimeFormat(entry.getValue()) + " - " + sensibleLPFormat(LP), CURIOTARGET.get()).tex(), new Coord(offX, sizeY));
                        sizeY += UI.scale(15);
                        for (int i = 0; i < Curios.size(); i++) {
                            if (Curios.contains(entry.getKey())) {
                                FinalCurios.add(entry.getKey());
                            }
                        }
                    }
                    GetCurios.add(entry.getKey());
                }

                if (curiocount != CurioCounter.size()) {
                    //messy as fuck, if curio number changes redraw everything so it's in the right place.
                    if (checkcurios != null)
                        checkcurios.destroy();
                    if (curiotarget != null)
                        curiotarget.destroy();
                    if (curiohigh != null)
                        curiohigh.destroy();
                    if (curiolow != null)
                        curiolow.destroy();
                    if (studyhours != null)
                        studyhours.destroy();
                    if (curiosliderlabel != null)
                        curiosliderlabel.destroy();
                    if (curioslider != null)
                        curioslider.destroy();
                    curiotarget = add(ColorPreWithLabel("Target Color", CURIOTARGET), new Coord(0, sizeY - UI.scale(5)));
                    curiohigh = add(ColorPreWithLabel("High Color", CURIOHIGH), new Coord(0, sizeY + UI.scale(15)));
                    curiolow = add(ColorPreWithLabel("Low Color", CURIOLOW), new Coord(0, sizeY + UI.scale(35)));
                    studyhours = add(new Label(""), new Coord(UI.scale(140), sizeY + UI.scale(40)));
                    curiosliderlabel = add(new Label("Curio Time Target:"), new Coord(0, sizeY + UI.scale(50)));
                    curioslider = add(new HSlider(UI.scale(130), 0, 10080, Config.curiotimetarget) {
                        public void added() {
                            super.added();
                            val = (Config.curiotimetarget);
                            updateLabel();
                        }

                        public void changed() {
                            Utils.setprefi("curiotimetarget", val);
                            Config.curiotimetarget = val;
                            updateLabel();
                        }

                        private void updateLabel() {
                            studyhours.settext(String.format("%d Hours", val / 60));
                        }

                        @Override
                        public Object tooltip(final Coord c, final Widget prev) {
                            return (RichText.render(String.format("%s(%s)", val / 60, val / 60 / ui.sess.glob.getTimeFac())));
                        }
                    }, new Coord(UI.scale(105), sizeY + UI.scale(55)));
                    checkcurios = add(new Button(UI.scale(110), "Check Curios") {
                        public void click() {
                            CurioReport = true;
                        }
                    }, new Coord(UI.scale(90), sizeY - UI.scale(5)));
                }
                sizeY += UI.scale(80);
                resize(UI.scale(265), sizeY);
                if (CurioReport) {
                    CurioReport = false;
                    Curios.clear();
                    for (String itm : Config.curioslist.keySet())
                        if (itm != null && Config.curioslist.get(itm))
                            Curios.add(itm);
                    Curios.removeAll(GetCurios);
                    if (!Curios.isEmpty()) {
                        PBotUtils.sysMsg(ui, "Missing Curios : " + Curios, Color.WHITE);
                    } else
                        PBotUtils.sysMsg(ui, "No Curios missing! GJ bro", Color.WHITE);
                }
                curiocount = CurioCounter.size(); //set this so we can only trigger the button/label redraw when the value changes.
            }
        } catch (Loading l) {
        }
        //caption if applies
        if (cap != null) {
            g.image(cap.tex(), UI.scale(cfg.capc));
        }
    }

    public class Curio {
        private String CurioName;
        private double StudyTime;
        private int LPGain;

        public Curio(String CurioName, double StudyTime, int LPGain) {
            this.CurioName = CurioName;
            this.StudyTime = StudyTime;
            this.LPGain = LPGain;
        }
    }

    public void draw(GOut g) {
        if (!hidden)
            drawframe(g);
        cdraw(g.reclip(atl, asz));
        super.draw(g);
    }

    private Widget ColorPreWithLabel(final String text, final IndirSetting<Color> cl) {
        final Widget container = new Widget();
        final Label lbl = new Label(text);
        final IndirColorPreview pre = new IndirColorPreview(new Coord(16, 16), cl);
        final int height = Math.max(lbl.sz.y, pre.sz.y) / 2;
        container.add(lbl, new Coord(0, height - lbl.sz.y / 2));
        container.add(pre, new Coord(lbl.sz.x, height - pre.sz.y / 2));
        container.pack();
        return container;
    }

    public Coord contentsz() {
        Coord max = new Coord(0, 0);
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg == cbtn || wdg == lbtn || wdg == mbtn || wdg == hbtn)
                continue;
            if (!wdg.visible())
                continue;
            Coord br = wdg.c.add(wdg.sz);
            if (br.x > max.x)
                max.x = br.x;
            if (br.y > max.y)
                max.y = br.y;
        }
        return (max);
    }

    public void addtwdg(Widget wdg) {
        twdgs.add(wdg);
        placetwdgs();
    }

    protected void placetwdgs() {
        int x = sz.x - 20;
        for (Widget ch : twdgs) {
            if (ch.visible) {
                ch.c = xlate(new Coord(x -= ch.sz.x + 5, ctl.y - ch.sz.y / 2), false);
            }
        }
    }

    private void placecbtn() {
        cbtn.c = new Coord(sz.x - cbtn.sz.x - atl.x - UI.scale(cfg.btnc).x, -atl.y + UI.scale(cfg.btnc).y);
        final Coord c;
        if (lbtn != null) {
            lbtn.c = cbtn.c.sub(lbtn.sz.x + 5, 0);
            c = new Coord(lbtn.c.x - (lbtn.sz.x + 5), lbtn.c.y);
        } else {
            c = new Coord(cbtn.c.x - (cbtn.sz.x + 5), cbtn.c.y);
        }
        for (final IButton btn : btns) {
            btn.c = c.copy();
            c.x -= btn.sz.x + 5;
        }
    }

    private void resize2(Coord sz) {
        asz = sz; //usable size for content
        csz = asz.add(mrgn.mul(2)); //add margin around usable size
        wsz = csz.add(UI.scale(cfg.tlc)).add(UI.scale(cfg.brc)); //usable size + margin + frame size
        //tlo, rbo = top left offset, bottom right offset usually 0 always...
        //Basically same job as tlc, brc
        this.sz = wsz;
        //top left coordinate of inner content area
        ctl = UI.scale(cfg.tlc);
        //Top left coordinate of where usable space starts after accounting for margin
        atl = ctl.add(mrgn);
        //Where the close button goes
        cbtn.c = new Coord(sz.x - UI.scale(cfg.btnc).x - cbtn.sz.x, UI.scale(cfg.btnc).y);
        for (Widget ch = child; ch != null; ch = ch.next)
            ch.presize();
        placecbtn();
    }

    public void resize(Coord sz) {
        resize2(sz);
    }

    public void uimsg(String msg, Object... args) {
        switch (msg) {
            case "pack":
                pack();
                break;
            case "dt":
                dt = (Integer) args[0] != 0;
                break;
            case "cap":
                String cap = (String) args[0];
                chcap(cap.equals("") ? null : cap);
                break;
            default:
                super.uimsg(msg, args);
                break;
        }
    }

    public Coord xlate(Coord c, boolean in) {
        if (in)
            return (c.add(atl));
        else
            return (c.sub(atl));
    }

    @Override
    protected boolean moveHit(Coord c, int btn) {
        Coord cpc = c.sub(cl.sz().x, 0);
        Coord cprc = c.sub(sz.x - cr.sz().x, 0);
        //content size
        return ui.modflags() == 0 && btn == 1 && (c.isect(ctl, csz) ||
                //or left caption
                (c.isect(Coord.z, cl.sz()) && cl.back.getRaster().getSample(c.x, c.y, 3) >= 128) ||
                //or right caption
                (c.isect(new Coord(sz.x - cr.sz().x, 0), cr.sz()) &&
                        cr.back.getRaster().getSample(cprc.x % cr.back.getWidth(), cprc.y, 3) >= 128) ||
                //or mid caption
                (c.isect(new Coord(cl.sz().x, 0), new Coord(sz.x - cr.sz().x - cl.sz().x, cm.sz().y)) &&
                        (cm.back.getRaster().getSample(cpc.x % cm.back.getWidth(), cpc.y, 3) >= 128)));
    }

    public boolean mousedown(Coord c, int button) {
        if (button == 4 || button == 5) //ignore these because why allow every mousedown to move the window?
            return false;
        if (super.mousedown(c, button)) {
            if (parent != null) {
                parent.setfocus(this);
                raise();
            }
            return (true);
        }
        if ((button == 1 && ui.modflags() != 0) || (button == 3 && ui.modflags() != UI.MOD_META)) { //miss click
            return (true);
        }
        if (button == 3 && ui.modflags() == UI.MOD_META && showSpecialMenu()) { //need move to inventory widget
            return (true);
        }
        return (false);
    }

    public boolean mouseup(Coord c, int button) {
        super.mouseup(c, button);
        return (true);
    }

    public void mousemove(Coord c) {
        if (hidable) {
            if (c.isect(Coord.z, sz) || moving()) {
                hidden = false;
                cbtn.visible = true;
                if (lbtn != null)
                    lbtn.visible = true;
                btns.forEach(btn -> btn.visible = true);
            } else {
                hidden = true;
                cbtn.visible = false;
                if (lbtn != null)
                    lbtn.visible = false;
                btns.forEach(btn -> btn.visible = false);
            }
        }
        super.mousemove(c);
    }

    public boolean mousehover(Coord c, boolean b) {
        return (super.mousehover(c, b));
    }

    public void setDestroyHook(final Runnable r) {
        this.destroyHook = r;
    }

    @Override
    public void destroy() {
        if (destroyHook != null)
            destroyHook.run();
        super.destroy();
    }

    public void close() {
        wdgmsg("close");
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn) {
            close();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public static final List<String> excludesCloseWnd = new ArrayList<>(Arrays.asList("Chat", "Minimap", "Map"));

    @Override
    public boolean type(char key, KeyEvent ev) {
        return (true);
    }

    @Override
    public boolean keydown(KeyEvent ev) {
        if (super.keydown(ev))
            return (true);
        if (key_esc.match(ev) && Config.escclosewindows) {
            if (!excludesCloseWnd.contains(this.origcap)) {
                close();
                return (true);
            }
        }
        return (false);
    }

    public boolean drop(Coord cc, Coord ul) {
        if (dt) {
            wdgmsg("drop", cc);
            return (true);
        }
        return (false);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        return (false);
    }

    public Object tooltip(Coord c, Widget prev) {
        Object ret = super.tooltip(c, prev);
        if (ret != null)
            return (ret);
        else
            return ("");
    }

    private Map<String, Integer> getStats() {
        CharWnd chrwdg = null;
        Map<String, Integer> statmap = new HashMap<String, Integer>();
        try {
            chrwdg = ((GameUI) parent).chrwdg;
        } catch (Exception e) { // fail silently
        }
        if (chrwdg != null) {
            for (BAttrWnd.Attr attr2 : chrwdg.battr.base) {
                //  System.out.println("name : "+attr2.attr.nm);
                if (attr2.attr.nm.contains("str")) {
                    statmap.put("str", attr2.attr.comp);
                }
                if (attr2.attr.nm.contains("agi")) {
                    statmap.put("agi", attr2.attr.comp);
                }
                if (attr2.attr.nm.contains("int")) {
                    statmap.put("int", attr2.attr.comp);
                }
                if (attr2.attr.nm.contains("con")) {
                    statmap.put("con", attr2.attr.comp);
                }
                if (attr2.attr.nm.contains("prc")) {
                    statmap.put("prc", attr2.attr.comp);
                }
                if (attr2.attr.nm.contains("csm")) {
                    statmap.put("csm", attr2.attr.comp);
                }
                if (attr2.attr.nm.contains("dex")) {
                    statmap.put("dex", attr2.attr.comp);
                }
                if (attr2.attr.nm.contains("wil")) {
                    statmap.put("wil", attr2.attr.comp);
                }
                if (attr2.attr.nm.contains("psy")) {
                    statmap.put("psy", attr2.attr.comp);
                }
            }
        }

        //  statmap.forEach((k, v) -> System.out.println("Key : "+k+" value : "+v));
        return statmap;
    }

    /**
     * Special menu for windows
     * using: list.put("Name", runnable);
     */

    public boolean showSpecialMenu() {
        List<Pair<String, Runnable>> list = new ArrayList<>();

        Set<Inventory> invs = children(Inventory.class);
        if (!invs.isEmpty()) {
            Runnable run = () -> {
                for (Inventory inv : invs) {
                    ui.root.add(inv.sortingWindow(), inv.parentpos(ui.root));
                }
            };
            list.add(new Pair<>("Sort", run));
            list.add(new Pair<>("Open Stacks", () -> invs.forEach(Inventory::openStacks)));
            list.add(new Pair<>("Close Stacks", () -> invs.forEach(Inventory::closeStacks)));
            list.add(new Pair<>("Toggle Inventory", () -> invs.forEach(Inventory::toggleAltInventory)));
//            list.put("Stack", () -> {});
//            list.put("Unstack", () -> {});
        }
        if (!list.isEmpty()) {
            String[] options = new String[list.size()];
            Iterator<Pair<String, Runnable>> it = list.iterator();
            for (int i = 0; i < options.length; i++) {
                options[i] = it.next().a;
            }
            Consumer<Integer> callback = selection -> {
                if (selection == -1)
                    return;
                Iterator<Pair<String, Runnable>> iterator = list.iterator();
                Pair<String, Runnable> entry = iterator.next();
                for (int i = 0; i < selection; i++) {
                    if (iterator.hasNext())
                        entry = iterator.next();
                    else
                        return;
                }
                try {
                    entry.b.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            final FlowerMenu modmenu = new FlowerMenu(callback, options);
            ui.root.getchilds(FlowerMenu.class).forEach(wdg -> wdg.choose(null));
            ui.root.add(modmenu, ui.mc);
            return (true);
        } else {
            return (false);
        }
    }

    public void info() {
        System.out.println(this);
        PBotUtils.sysMsg(ui, this.toString());
    }
}
