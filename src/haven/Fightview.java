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


import haven.chrwnd.SAttrWnd;
import haven.res.ui.obj.buddy.Buddy;
import haven.sloth.gfx.GobCombatSprite;
import haven.sloth.gob.AggroMark;
import haven.sloth.gob.Alerted;
import haven.sloth.gui.MovableWidget;
import haven.sloth.gui.fight.Attack;
import haven.sloth.gui.fight.Attacks;
import haven.sloth.gui.fight.Card;
import haven.sloth.gui.fight.Cards;
import haven.sloth.gui.fight.DefenseType;
import haven.sloth.gui.fight.Maneuver;
import modification.configuration;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Fightview extends MovableWidget {
    public static Tex bg = Theme.tex("bosq");
    public static int height = UI.scale(5);
    public static int ymarg = UI.scale(5);
    public static int width = UI.scale(165);
    static int oldblue, lastactopening, unarmed;
    private int damage = 0;
    public static Coord avasz = UI.scale(27, 27);
    public static Coord cavac = new Coord(width - Avaview.dasz.x - UI.scale(10), UI.scale(10));
    public static Coord cgivec = new Coord(cavac.x - UI.scale(35), cavac.y);
    public static Coord cpursc = new Coord(cavac.x - UI.scale(75), cgivec.y + UI.scale(35));
    public LinkedList<Relation> lsrel = new LinkedList<>();
    public final Map<Long, Widget> obinfo = new HashMap<>();
    public Relation current = null;
    public static String ttretaincur, ttretain;
    public List<Relation> notcurrent;
    public Indir<Resource> blk, batk, iatk;
    public double atkcs, atkct;
    public Indir<Resource> lastact = null;
    public double lastuse = 0;
    public boolean invalid = false;
    public double atkcd;
    public GiveButton curgive;
    //    private Avaview curava;
//    private Button curpurs;
    public final Bufflist buffs = add(new Bufflist());
    public Fightview.Relation lastTarget;

    private static final Gob.Overlay curol = new Gob.Overlay(new FightCurrentOpp());

    {
        buffs.hide();
    }

    private static final Color combatLogMeClr = new Color(86, 153, 191);
    private static final Color combatLogOpClr = new Color(234, 105, 105);

    public Maneuver maneuver;
    public double maneuvermeter;
    public final Map<DefenseType, Double> defweights = new HashMap<>();

    public class Relation {
        public final long gobid;
        public final Avaview ava;
        public final GiveButton give;
        public final Button purs;
        public final Bufflist buffs = add(new Bufflist());

        {
            buffs.hide();
        }

        public int ip, oip;
        public Indir<Resource> lastact = null;
        public double lastuse = 0;

        public Maneuver maneuver;
        public double maneuvermeter;
        public final Map<DefenseType, Double> preweights = new HashMap<>();
        public final Map<DefenseType, Double> defweights = new HashMap<>();
        public double estimatedBlockWeight = 0;

        public final Bufflist relbuffs = add(new Bufflist());

        {
            relbuffs.hide();
        }

        public boolean invalid = false;

        public Relation(long gobid) {
            this.gobid = gobid;
            add(this.ava = new Avaview(avasz, gobid, "fightcam")).canactivate = true;
            add(this.give = new GiveButton(0, UI.scale(30, 30)));
            add(this.purs = new Button(UI.scale(70), "Pursue"));
            for (DefenseType type : DefenseType.values()) {
                defweights.put(type, 0.0);
                preweights.put(type, 0.0);
            }
        }

        public void give(int state) {
            if (this == current)
                current.give.state = state;
            this.give.state = state;
        }

        public void show(boolean state) {
            ava.show(state);
            give.show(state);
            purs.show(state);
        }

        public void remove() {
            ui.destroy(ava);
            ui.destroy(give);
            ui.destroy(purs);
            ui.destroy(buffs);
            ui.destroy(relbuffs);
            invalid = true;
        }

        public void use(Indir<Resource> act) {
            try {
                lastact = act;
                lastuse = Utils.rtime();
                if (lastact != null)
                    if (lastact.get().basename().contains("cleave") && Config.cleavesound) {
                        try {
                            String aitem = "None";
                            if (!Utils.eq(aitem = Config.alarmsfxlist.get("alarmcleave"), "None")) {
                                Double vol = Config.alarmvollist.get("alarmcleave");
                                if (Alerted.customsort.get(aitem)) Audio.play(aitem, vol);
                                else Audio.play(Resource.local().load(aitem), vol);
                            }
                        } catch (Exception e) {
                        }//ignore because a crash here would prob get someone killed
                    }
                if (lastact != null && Config.logcombatactions) {
                    Resource res = lastact.get();
                    Resource.Tooltip tt = res.layer(Resource.tooltip);
                    ttretaincur = tt.t;
                    if (tt == null) {
                        ui.gui.syslog.append("Combat: WARNING! tooltip is missing for " + res.name + ". Notify Jorb/Loftar about this.", combatLogOpClr);
                        return;
                    }
                    OCache oc = ui.gui.map.glob.oc;
                    Gob gob = oc.getgob(gobid);
                    Buddy buddy = gob.getattr(Buddy.class);
                    if (buddy != null)
                        ui.gui.syslog.append(String.format("%s, %s, ip %d - %d", buddy.name(), tt.t, ip, oip), combatLogOpClr);
                    else if (gob.getres().basename().contains("body"))
                        ui.gui.syslog.append(String.format("Enemy Player, %s, ip %d - %d", tt.t, ip, oip), combatLogOpClr);
                    else
                        ui.gui.syslog.append(String.format("Enemy :%s, %s, ip %d - %d", gob.getres().basename(), tt.t, ip, oip), combatLogOpClr);
                }
            } catch (Loading | NullPointerException l) {
            }
        }

        private void updateDefWeights() {
            final Set<DefenseType> notfound = new HashSet<>(Arrays.asList(DefenseType.values()));
            for (Widget wdg = buffs.child; wdg != null; wdg = wdg.next) {
                if (wdg instanceof Buff) {
                    final Buff b = (Buff) wdg;
                    b.res().ifPresent(res -> {
                        final DefenseType type = DefenseType.lookup.getOrDefault(res.name, null);
                        if (type != null) {
                            preweights.put(type, defweights.get(type));
                            defweights.put(type, b.ameter() / 100.0);
                            notfound.remove(type);
                        } else if (Cards.lookup.get(res.layer(Resource.tooltip).origt) instanceof Maneuver) {
                            maneuver = (Maneuver) Cards.lookup.get(res.layer(Resource.tooltip).origt);
                            maneuvermeter = b.ameter() / 100.0;
                        }
                    });
                }
            }

            for (final DefenseType zero : notfound) {
                //no longer has this defense.
                defweights.put(zero, 0.0);
            }
        }

        public void tick() {
            updateDefWeights();
//            final Gob g = ui.sess.glob.oc.getgob(gobid);
//            if (g != null) {
//                if (DefSettings.COLORIZEAGGRO.get() && this.equals(current)) {
//                    if (g.findol(AggroMark.id) == null) {
//                        g.addol(new Gob.Overlay(AggroMark.id, new AggroMark()));
//                    }
//                } else {
//                    final Gob.Overlay ol = g.findol(AggroMark.id);
//                    final AggroMark mark = ol != null ? (AggroMark) ol.spr : null;
//                    if (mark != null) {
//                        mark.rem();
//                    }
//                }
//            }
        }

        public void destroy() {
            final Gob g = ui.sess.glob.oc.getgob(gobid);
            if (g != null) {
                final Gob.Overlay ol = g.findol(AggroMark.id);
                if (ol != null) {
                    final AggroMark am = (AggroMark) ol.spr;
                    if (am != null) {
                        am.rem();
                    }
                }
            }
        }

        void checkWeight() {
            final double SMOOTHED_ALPHA = 0.9;
            updateDefWeights();
            //Now use pre/post to determine block weight based off what we did to them
            try {
                if (Fightview.this.lastact != null) {
                    final Card c = Cards.lookup.getOrDefault(Fightview.this.lastact.get().layer(Resource.tooltip).origt, Cards.unknown);
                    final double blockweight;
                    if (c instanceof Attack || c == Cards.flex) {
                        final Attacks atk = (Attacks) c;
                        final int ua = ui.sess.glob.getcattr("unarmed").comp;
                        final int mc = ui.sess.glob.getcattr("melee").comp;
                        final int cards = ui.gui.chrwdg.fight.cards(Fightview.this.lastact.get().name);
                        if (maneuver == Cards.oakstance) {
                            final double atkweight = atk.getAttackweight(Fightview.this.maneuver, Fightview.this.maneuvermeter, ua, mc, cards);
                            final double estblockweight = estimatedBlockWeight == 0 ? atkweight : estimatedBlockWeight;
                            final Map<DefenseType, Double> expected = atk.calculateEnemyDefWeights(Fightview.this.maneuver, Fightview.this.maneuvermeter, ua, mc, cards, preweights, estblockweight);
                            DefenseType max = DefenseType.GREEN;
                            double maxv = 0;
                            for (DefenseType type : DefenseType.values()) {
                                if (expected.get(type) > maxv) {
                                    max = type;
                                    maxv = expected.get(type);
                                }
                            }

                            //Factor back in the 0.05% taken away
                            expected.put(DefenseType.GREEN, defweights.get(DefenseType.GREEN));
                            expected.put(DefenseType.BLUE, defweights.get(DefenseType.BLUE));
                            expected.put(DefenseType.YELLOW, defweights.get(DefenseType.YELLOW));
                            expected.put(DefenseType.RED, defweights.get(DefenseType.RED));
                            //Stats are no longer relevant for maneuvers, and the effects of maneuvers are always constant.
                            maxv = expected.get(max) + (expected.get(max) * 0.05);
                            expected.put(max, maxv);
                            //figuring our the weight from an oakstance hit that goes past 50% starts to cause issues and ruins the estimation
                            blockweight = maxv < 0.50 ? atk.guessEnemyBlockWeight(Fightview.this.maneuver, Fightview.this.maneuvermeter, ua, mc, cards, preweights, expected) : Double.POSITIVE_INFINITY;
                        } else {
                            blockweight = atk.guessEnemyBlockWeight(Fightview.this.maneuver, Fightview.this.maneuvermeter, ua, mc, cards, preweights, defweights);
                        }

                        if (!Double.isInfinite(blockweight)) {
                            estimatedBlockWeight = estimatedBlockWeight != 0 ? (SMOOTHED_ALPHA * estimatedBlockWeight) + ((1 - SMOOTHED_ALPHA) * blockweight) : blockweight;
                        }
                    }
                }
            } catch (Loading l) {
                //Ignore, but really should never hit here
            }
        }


        /*******************************************************************************
         * For Scripting API only
         */

        public void peace() {
            //if not peaced, peace
            if (give.state != 1) {
                give.wdgmsg("click", 1);
            }
        }

        /******************************************************************************/
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        List<Relation> lsrel;
        synchronized (this.lsrel) {
            lsrel = new ArrayList<>(this.lsrel);
        }
        for (Relation rel : lsrel) {
            rel.tick();
            Widget inf = obinfo(rel.gobid, false);
            if (inf != null)
                inf.tick(dt);
            final Gob gob = ui.sess.glob.oc.getgob(rel.gobid);
            if (gob != null) {
                final Gob.Overlay ol = gob.findol(GobCombatSprite.id);
                if (ol != null) {
                    if (Config.showothercombatinfo) {
                        if (rel.equals(current) && !configuration.showcurrentenemieinfo)
                            gob.ols.remove(ol);
                        else
                            ((GobCombatSprite) ol.spr).update(rel);
                    } else {
                        gob.ols.remove(ol);
                    }
                } else if (Config.showothercombatinfo && (configuration.showcurrentenemieinfo || !rel.equals(current))) {
                    gob.addol(new Gob.Overlay(gob, GobCombatSprite.id, new GobCombatSprite(gob, rel)));
                }
            }
        }

        final Set<DefenseType> notfound = new HashSet<>(Arrays.asList(DefenseType.values()));
        for (Widget wdg = buffs.child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof Buff) {
                final Buff b = (Buff) wdg;
                b.res().ifPresent(res -> {
                    final DefenseType type = DefenseType.lookup.getOrDefault(res.name, null);
                    if (type != null) {
                        defweights.put(type, b.ameter() / 100.0);
                        notfound.remove(type);
                    } else if (Cards.lookup.get(res.layer(Resource.tooltip).origt) instanceof Maneuver) {
                        maneuver = (Maneuver) Cards.lookup.get(res.layer(Resource.tooltip).origt);
                        maneuvermeter = b.ameter() / 100.0;
                    }
                });
            }
        }

        for (final DefenseType zero : notfound) {
            //no longer has this defense.
            defweights.put(zero, 0.0);
        }
    }

    public void use(Indir<Resource> act) {
        lastact = act;
        lastuse = Utils.rtime();
        if (lastact != null)
            if (lastact.get().basename().contains("cleave") && Config.cleavesound) {
                try {
                    String aitem = "None";
                    if (!Utils.eq(aitem = Config.alarmsfxlist.get("alarmcleave"), "None")) {
                        Double vol = Config.alarmvollist.get("alarmcleave");
                        if (Alerted.customsort.get(aitem)) Audio.play(aitem, vol);
                        else Audio.play(Resource.local().load(aitem), vol);
                    }
                } catch (Throwable e) {
                }//ignore because a crash here would prob get someone killed
            }
        if (lastact != null && Config.logcombatactions) {
            try {
                Resource res = lastact.get();
                Resource.Tooltip tt = res.layer(Resource.tooltip);
                ttretain = tt.t;
                if (tt == null) {
                    ui.gui.syslog.append("Combat: WARNING! tooltip is missing for " + res.name + ". Notify Jorb/Loftar about this.", combatLogMeClr);
                    return;
                }
                String cd = Utils.fmt1DecPlace(atkct - lastuse);
                double cdadvantage = checkcd(cd, tt);
                unarmed = getUnarmed();
                if (cdadvantage != 0.0)
                    ui.gui.syslog.append(String.format("me: %s, ip %d - %d, cd %ss, Agi Delta %s", tt.t, current.ip, current.oip, cd, cdadvantage), combatLogMeClr);
                else
                    ui.gui.syslog.append(String.format("me: %s, ip %d - %d, cd %ss", tt.t, current.ip, current.oip, cd), combatLogMeClr);
            } catch (Loading l) {
            }
        }
    }

    private int getUnarmed() {
        CharWnd chrwdg = null;
        int unarmedcombat = 0;
        try {
            chrwdg = ((GameUI) parent.parent).chrwdg;
        } catch (Throwable e) { // fail silently
        }
        if (chrwdg != null) {
            for (SAttrWnd.SAttr attr : chrwdg.sattr.skill) {
                if (attr.attr.nm.contains("unarmed")) {
                    unarmedcombat = attr.attr.comp;
                }
            }
        }
        return unarmedcombat;
    }

    private Double checkcd(String cd, Resource.Tooltip tt) {
        double base;
        if (tt.origt.contains("Flex"))
            base = 1.8;
        else if (tt.origt.contains("Knocks"))
            base = 2.7;
        else if (tt.origt.contains("Teeth"))
            base = 2.1;
        else
            return 0.0;

        double converted = Double.parseDouble(cd);
        double finalcd = converted - base;
        finalcd = finalcd / base * 100;
        if (finalcd < -16.0)
            finalcd = 100;
        else if (finalcd > -16.0 && finalcd <= -10.0)
            finalcd = 75;
        else if (finalcd > -10.0 && finalcd <= -6.0)
            finalcd = 50;
        else if (finalcd > -6.0 && finalcd <= -4.0)
            finalcd = 25;
        else if (finalcd > -1.0 && finalcd <= 1.0)
            finalcd = 0;
        else if (finalcd > 16.0)
            finalcd = -100;
        else if (finalcd < 16.0 && finalcd >= 10.0)
            finalcd = -75;
        else if (finalcd < 10.0 && finalcd >= 6.0)
            finalcd = -50;
        else if (finalcd < 6.0 && finalcd >= 4.0)
            finalcd = -25;

        return finalcd;
    }

    @RName("frv")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            return (new Fightview());
        }
    }

    public Fightview() {
        super(new Coord(width, (bg.sz().y + ymarg) * height), "FightView");
        for (DefenseType type : DefenseType.values())
            defweights.put(type, 0.0);
    }

    public void addchild(Widget child, Object... args) {
        if (args[0].equals("buff")) {
            Widget p;
            if (args[1] == null)
                p = buffs;
            else
                p = getrel((Integer) args[1]).buffs;
            p.addchild(child);
        } else if (args[0].equals("relbuff")) {
            getrel((Integer) args[1]).relbuffs.addchild(child);
        } else {
            super.addchild(child, args);
        }
    }

    /* XXX? It's a bit ugly that there's no trimming of obinfo, but
     * it's not obvious that one really ever wants it trimmed, and
     * it's really not like it uses a lot of memory. */
    public Widget obinfo(long gobid, boolean creat) {
        synchronized (obinfo) {
            Widget ret = obinfo.get(gobid);
            if ((ret == null) && creat)
                obinfo.put(gobid, ret = new AWidget());
            return (ret);
        }
    }

//    public void tick(double dt) {
//        super.tick(dt);
//        for (Relation rel : lsrel) {
//            Widget inf = obinfo(rel.gobid, false);
//            if (inf != null)
//                inf.tick(dt);
//        }
//    }

    public <T extends Widget> T obinfo(long gobid, Class<T> cl, boolean creat) {
        Widget cnt = obinfo(gobid, creat);
        if (cnt == null)
            return (null);
        T ret = cnt.getchild(cl);
        if ((ret == null) && creat) {
            try {
                ret = Utils.construct(cl.getConstructor());
            } catch (NoSuchMethodException e) {
                throw (new RuntimeException(e));
            }
            cnt.add(ret);
        }
        return (ret);
    }

    public static interface ObInfo {
        public default int prio() {
            return (1000);
        }

        public default Coord2d grav() {
            return (new Coord2d(0, 1));
        }
    }

    private void setcur(Relation rel) {
//        if ((current == null) && (rel != null)) {
//            add(curgive = new GiveButton(0), cgivec);
//            add(curava = new Avaview(Avaview.dasz, rel.gobid, "avacam"), cavac).canactivate = true;
//            add(curpurs = new Button(70, "Chase"), cpursc);
//            curgive.state = rel.give.state;
//        } else if ((current != null) && (rel == null)) {
//            ui.destroy(curgive);
//            ui.destroy(curava);
//            ui.destroy(curpurs);
//            curgive = null;
//            curava = null;
//            curpurs = null;
//        } else if ((current != null) && (rel != null)) {
//            curgive.state = rel.give.state;
//            curava.avagob = rel.gobid;
//        }
        current = rel;

        if (Config.hlightcuropp) {
            if (current != null) {
                Gob curgob = ui.sess.glob.oc.getgob(current.gobid);
                if (curgob != null && !curgob.ols.contains(curol))
                    curgob.ols.add(curol);
            }
            for (Relation r : lsrel) {
                Gob relgob = ui.sess.glob.oc.getgob(r.gobid);
                if (relgob != null && r != rel)
                    relgob.ols.remove(curol);
            }
        }
    }

    public void scroll(final int amount) {
        if (current != null) {
            final int idx = lsrel.indexOf(current);
            final Relation rel;
            if (idx + amount < 0)
                rel = lsrel.get(lsrel.size() - 1);
            else
                rel = lsrel.get((idx + amount) % lsrel.size());

            if (rel != null) {
                wdgmsg("bump", (int) rel.gobid);
            }
        }
    }

    public void destroy() {
        setcur(null);
        super.destroy();
    }

    public void draw(GOut g) {
        try {
            int y = UI.scale(10);
//        if (curava != null)
//            y = curava.c.y + curava.sz.y + 10;
            int x = width - bg.sz().x - UI.scale(10);
            for (Relation rel : lsrel) {
                if (rel == current) {
                    g.chcolor(Color.YELLOW);
                    g.image(bg, new Coord(x, y));
                    g.chcolor();
                } else {
                    g.image(bg, new Coord(x, y));
                }

                rel.ava.c = new Coord(UI.scale(12), y + UI.scale(3));
                rel.purs.c = new Coord(rel.ava.c.x + rel.ava.sz.x + UI.scale(2), y + UI.scale(3));
                rel.give.c = new Coord(rel.purs.c.x + rel.purs.sz.x + UI.scale(2), y + UI.scale(3));
                rel.show(true);
                g.chcolor(Color.GREEN);
                FastText.printf(g, new Coord(UI.scale(12), y + UI.scale(30)), "IP %d", rel.ip);
                g.chcolor(Color.RED);
                FastText.printf(g, new Coord(UI.scale(12), y + UI.scale(42)), "IP %d", rel.oip);
                final Coord c = new Coord(UI.scale(40), y + UI.scale(30));
                for (Widget wdg = rel.buffs.child; wdg != null; wdg = wdg.next) {
                    if (!(wdg instanceof Buff))
                        continue;
                    final Buff buf = (Buff) wdg;
                    Double ameter = (buf.ameter >= 0) ? Double.valueOf(buf.ameter / 100.0) : buf.ameteri.get();
                    if (ameter != null && buf.isOpening()) {
                        buf.fightdraw(g.reclip(c.copy(), Buff.scframe.sz()));
                        c.x += Buff.scframe.sz().x + UI.scale(2);
                    }
                }
                final Gob gob = ui.sess.glob.oc.getgob(rel.gobid);
                if (gob != null) {
                    g.chcolor(new Color(200, 200, 200));
//                    FastText.printf(g, new Coord(12, y + 64), "Speed: %f", gob.getv());
//                    FastText.printf(g, new Coord(12, y + 72), "Distance: %f", gob.getc().dist(ui.sess.glob.oc.getgob(ui.gui.map.plgob).getc()) / 11.0);

                    g.image(RichText.render(String.format("$bg[35,35,35,192]{$b{Speed: %.1f : Distance: %.2f}}", gob.getv(), gob.getc().dist(ui.sess.glob.oc.getgob(ui.gui.map.plgob).getc()) / 11.0), -1).tex(), new Coord(UI.scale(12), y + UI.scale(68)));
                }
                g.chcolor();
                y += bg.sz().y + ymarg + UI.scale(16);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        super.draw(g);
    }


    public static class Notfound extends RuntimeException {
        public final long id;

        public Notfound(long id) {
            super("No relation for Gob ID " + id + " found");
            this.id = id;
        }
    }

    public Relation getrel2(final long gobid) {
        for (Relation rel : lsrel) {
            if (rel.gobid == gobid)
                return (rel);
        }
        return null;
    }

    public Relation getrel(long gobid) {
        for (Relation rel : lsrel) {
            if (rel.gobid == gobid)
                return (rel);
        }
        throw (new Notfound(gobid));
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
//        if (sender == curava) {
//            wdgmsg("click", (int) current.gobid, args[0]);
//            return;
//        } else if (sender == curgive) {
//            wdgmsg("give", (int) current.gobid, args[0]);
//            return;
//        } else if (sender == curpurs) {
//            wdgmsg("prs", (int) current.gobid);
//            return;
//        }
        for (Relation rel : lsrel) {
            if (sender == rel.ava) {
                wdgmsg("click", (int) rel.gobid, args[0]);
                return;
            } else if (sender == rel.give) {
                wdgmsg("give", (int) rel.gobid, args[0]);
                return;
            } else if (sender == rel.purs) {
                wdgmsg("prs", (int) rel.gobid);
                return;
            }
        }
        super.wdgmsg(sender, msg, args);
    }

    @Override
    public boolean mousedown(final Coord mc, final int button) {
        if (super.mousedown(mc, button)) {
            return (true);
        }
        if (!visible())
            return (false);
        int y = UI.scale(10);
        int x = width - bg.sz().x - UI.scale(10);
        for (Relation rel : lsrel) {
            if (mc.isect(Coord.of(x, y), bg.sz())) {
                wdgmsg("click", (int) rel.gobid, button);
                return (true);
            }
            y += bg.sz().y + ymarg + UI.scale(16);
        }
        return (false);
    }

    private Indir<Resource> n2r(int num) {
        if (num < 0)
            return (null);
        return (ui.sess.getres(num));
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "new") {
            Relation rel = new Relation((Integer) args[0]);
            rel.give((Integer) args[1]);
            rel.ip = (Integer) args[2];
            rel.oip = (Integer) args[3];
            lsrel.addFirst(rel);
            ui.sess.glob.oc.isfight = true;
            return;
        } else if (msg == "del") {
            Relation rel = getrel((Integer) args[0]);
            OCache oc = ui.sess.glob.oc;
            oc.removedmgoverlay(rel.gobid);
            if (Config.hlightcuropp) {
                Gob relgob = ui.sess.glob.oc.getgob(rel.gobid);
                if (relgob != null)
                    relgob.ols.remove(curol);
            }
            rel.remove();
            rel.destroy();
            lsrel.remove(rel);
            if (lsrel.size() == 0) {
                oc.removedmgoverlay(ui.gui.map.plgob);
                oc.isfight = false;
            }
            if (rel == current)
                setcur(null);
            final Gob g = ui.sess.glob.oc.getgob(rel.gobid);
            if (g != null) {
                final Gob.Overlay ol = g.findol(GobCombatSprite.id);
                if (ol != null) {
                    ((GobCombatSprite) ol.spr).update(null);
                }
            }
            return;
        } else if (msg == "upd") {
            Relation rel = getrel((Integer) args[0]);
            rel.give((Integer) args[1]);
            rel.ip = (Integer) args[2];
            rel.oip = (Integer) args[3];
            if (rel != current)
                return;
        } else if (msg == "used") {
            use((args[0] == null) ? null : ui.sess.getres((Integer) args[0]));
            if (current != null)
                current.checkWeight();
            return;
        } else if (msg == "ruse") {
            Relation rel = getrel((Integer) args[0]);
            rel.use((args[1] == null) ? null : ui.sess.getres((Integer) args[1]));
            return;
        } else if (msg == "cur") {
            try {
                Relation rel = getrel((Integer) args[0]);
                lsrel.remove(rel);
                lsrel.addFirst(rel);
                setcur(rel);
            } catch (Notfound e) {
                setcur(null);
            }
            return;
        } else if (msg == "atkc") {
            atkcd = ((Number) args[0]).doubleValue();
            atkcs = Utils.rtime();
            atkct = atkcs + (atkcd * 0.06);
            return;
        } else if (msg == "blk") {
            blk = n2r((Integer) args[0]);
            return;
        } else if (msg == "atk") {
            batk = n2r((Integer) args[0]);
            iatk = n2r((Integer) args[1]);
            return;
        }
        super.uimsg(msg, args);
    }

    /*******************************************************************************
     * For Scripting API only
     */

    public Relation[] getrelations() {
        return lsrel.toArray(new Relation[0]);
    }

    /******************************************************************************/

    public void targetClosestCombat() {
        try {
            Fightview.Relation closest = null;
            Gob closestGob = null;
            Gob rGob = null;
            double distanceRange = 0.0D;
            Gob player = this.ui.sess.glob.oc.getgob(ui.gui.map.plgob);

            Iterator var7 = this.lsrel.iterator();

            while (var7.hasNext()) {
                Fightview.Relation r = (Fightview.Relation) var7.next();
                if (r != null) {
                    rGob = this.ui.sess.glob.oc.getgob(r.gobid);

                    if (closestGob == null && rGob != null) {
                        closestGob = rGob;
                    }

                    if (closestGob != null && rGob != null) {
                        double closestDist = (double) player.getc().dist(closestGob.getc());
                        double rDist = (double) player.getc().dist(rGob.getc());
                        if (rDist < closestDist) {
                            closestGob = rGob;
                        }
                    }
                }
            }

            if (closestGob != null) {
                if (this.current.gobid != closestGob.id) {
                    this.lastTarget = this.current;
                }

                this.wdgmsg("bump", new Object[]{(int) closestGob.id});
            }
        } catch (NullPointerException var15) {
            System.out.println(var15.toString());
        }

    }
}
