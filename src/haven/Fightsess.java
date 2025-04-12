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
import haven.res.gfx.fx.floatimg.DamageText;
import haven.res.ui.tt.wpn.Armpen;
import haven.sloth.gob.Alerted;
import haven.sloth.gui.MovableWidget;
import haven.sloth.gui.fight.Attack;
import haven.sloth.gui.fight.Card;
import haven.sloth.gui.fight.Cards;
import haven.sloth.gui.fight.DefenseType;
import haven.sloth.gui.fight.Restoration;
import haven.sloth.gui.fight.Weapons;
import haven.sloth.gui.fight.WeightType;
import modification.configuration;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Fightsess extends Widget {
    public static final Tex cdframe = Resource.loadtex("gfx/hud/combat/cool");
    public static final Tex actframe = Buff.frame;
    public static final Coord actframeo = Buff.imgoff;
    public static final Tex indframe = Resource.loadtex("gfx/hud/combat/indframe");
    public static final Coord indframeo = (indframe.sz().sub(UI.scale(32, 32))).div(2);
    public static final Tex indbframe = Resource.loadtex("gfx/hud/combat/indbframe");
    public static final Coord indbframeo = (indframe.sz().sub(UI.scale(32, 32))).div(2);
    public static final Tex useframe = Resource.loadtex("gfx/hud/combat/lastframe");
    public static final Coord useframeo = (useframe.sz().sub(UI.scale(32, 32))).div(2);
    public static final int actpitch = UI.scale(75);
    public static final int smallactpitch = UI.scale(40);
    public float actionscale = Utils.getpreff("combatscale", 1);
    public static int blue = 0, red = 0, myblue = 0, myred = 0, oldblue = 0, oldred = 0, unarmedcombat;
    public static Double delta, expected, lastactopened;
    public final Action[] actions;
    public int use = -1, useb = -1;
    public Coord pcc, pcc2;
    public int pho;
    private double lastuse, now = 0;
    private Fightview fv;
    private final Tex[] keystex = new Tex[10];
    private final Tex[] keysftex = new Tex[10];
    private final Tex[] keysfftex = new Tex[10];
    private final Tex[] keysitex = new Tex[10];

    private static final Map<String, Color> openings = new HashMap<String, Color>(4) {{
        put("paginae/atk/dizzy", new Color(8, 103, 136));
        put("paginae/atk/offbalance", new Color(8, 103, 1));
        put("paginae/atk/cornered", new Color(221, 28, 26));
        put("paginae/atk/reeling", new Color(203, 168, 6));
    }};
    private Coord simpleOpeningSz = UI.scale(32, 32);
    private Coord smallerOpeningSz = UI.scale(24, 24);

    //    private Coord actionAnchor;
    private Coord enemyBuffAnchor;
    private Coord enemyIPAnchor;
    private Coord enemyLastMoveAnchor;
    private Coord buffAnchor;
    private Coord IPAnchor;
    private Coord lastMoveAnchor;
    private Coord cooldownAnchor;

    public MovableWidget actionWdg;
    public MovableWidget cooldownWdg;

    public static String colred = "[221,28,26]";    //"[255,105,180]";
    public static String colgreen = "[8,103,1]"; //"[0,255,0]";
    public static String colblue = "[8,103,136]";   //"[0,255,255]";
    public static String colyellow = "[203,168,6]";   //"[239,253,63]";
    public static String colgrey = "[60,60,60,180]";
    public static String ffont = "[mono]";
    public static Color grey = new Color(30, 30, 30);

    public static class Action {
        public final int id;
        public final Indir<Resource> res;
        public Card card;
        public int cards;
        public double cs, ct;
        private boolean discovered;

        public Action(Indir<Resource> res, final int id) {
            this.res = res;
            this.discovered = false;
            this.id = id;
        }

        public boolean isDiscovered() {
            return discovered;
        }

        void tick(final UI ui) {
            if (!discovered) {
                try {
                    card = Cards.lookup.getOrDefault(res.get().layer(Resource.tooltip).origt, Cards.unknown);
                    cards = ui.gui.chrwdg.fight.cards(res.get().name);
                    discovered = true;
                } catch (Loading l) {
                    //ignore
                }
            }
        }
    }

    @RName("fsess")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            int nact = (Integer) args[0];

            if (configuration.shieldnotify) {
                try {
                    WItem righthand = ui.gui.getequipory().quickslots[7];
                    WItem lefthand = ui.gui.getequipory().quickslots[6];

                    if (!((righthand != null && righthand.name.get().contains("shield")) || (lefthand != null && lefthand.name.get().contains("shield")))) {
                        PBotUtils.sysMsg(ui, "Shield not equipped!", Color.white);
                    }
                } catch (Throwable e) {
                }
            }

            String aitem = "None";
            if (!Utils.eq(aitem = Config.alarmsfxlist.get("alarmattacked"), "None")) {
                Double vol = Config.alarmvollist.get("alarmattacked");
                if (Alerted.customsort.get(aitem)) Audio.play(aitem, vol);
                else Audio.play(Resource.local().load(aitem), vol);
            }
            return (new Fightsess(nact));
        }
    }

    @SuppressWarnings("unchecked")
    public Fightsess(int nact) {
        pho = -40;
        this.actions = new Action[nact];

        for (int i = 0; i < 10; i++) {
            keystex[i] = Text.renderstroked(FightWnd.keys[i], grey, Color.WHITE, Text.num20Fnd).tex();
            if (i < 5)
                keysftex[i] = keystex[i];
            else
                keysftex[i] = Text.renderstroked(FightWnd.keysf[i - 5], grey, Color.WHITE, Text.num20Fnd).tex();
            keysfftex[i] = Text.renderstroked(FightWnd.keysf[i], grey, Color.WHITE, Text.num20Fnd).tex();
            keysitex[i] = Text.renderstroked(FightWnd.keysi[i], grey, Color.WHITE, Text.num20Fnd).tex();
        }
    }

    protected void added() {
        fv = parent.getparent(GameUI.class).fv;
        presize();

        Coord awc = UI.scale(400, 175);
        Coord awsz = awc.mul(actionscale);
        actionWdg = new MovableWidget(awsz, "FightSessActions") {
            @Override
            public void draw(GOut g) {
                //My cards
                final GItem weapon = weap();
                final int weapq;
                final int weapdmg;
                final double weappen;
                if (weapon != null) {
                    weapq = (int) weapon.quality().q;
                    weapdmg = Weapons.lookup.getOrDefault(weapon.name().orElse(""), 0);
                    weappen = weapon.getinfo(Armpen.class).orElse(Armpen.NOPEN).deg;
                } else {
                    weapq = weapdmg = 0;
                    weappen = 0.0;
                }

                for (int i = 0; i < actions.length; i++) {
//                    Coord ca = Config.altfightui ? actionAnchor.add(actc(i)) : pcc.add(actc(i));
                    float pitch = (configuration.showactioninfo ? actpitch : smallactpitch) * actionscale;
                    Coord ca = actionAnchor.get().add(actc(actionscale, i));
                    Action act = actions[i];
                    try {
                        if (act != null) {
                            Resource res = act.res.get();
                            Tex img = res.layer(Resource.imgc).tex();
                            Coord ic = ca.add((int) ((pitch - img.sz().mul(actionscale).x) / 2), (int) ((pitch - img.sz().mul(actionscale).y) / 2));
                            g.image(img, ic, img.sz().mul(actionscale));
                            if (now < act.ct) {
                                //This is from an era when moves had their own cooldown
                                double a = (now - act.cs) / (act.ct - act.cs);
                                g.chcolor(0, 0, 0, 128);
                                g.prect(ca, ic.sub(ca), ic.add(img.sz()).sub(ca), (1.0 - a) * Math.PI * 2);
                                g.chcolor();
                            }
                            if (i == use) {
                                g.image(indframe, ic.sub(indframeo.mul(actionscale)), indframe.sz().mul(actionscale));
                            } else if (i == useb) {
                                g.image(indbframe, ic.sub(indbframeo.mul(actionscale)), indbframe.sz().mul(actionscale));
                            } else {
                                g.image(actframe, ic.sub(actframeo.mul(actionscale)), actframe.sz().mul(actionscale));
                            }

                            if (configuration.showactioninfo) {
                                if (fv.current != null) {
                                    if (act.card instanceof Attack) {
                                        final Attack atk = (Attack) act.card;
                                        final Pair<Double, Double> dmg = atk.calculateDamage(weapdmg, weapq, weappen, str(), fv.current.defweights);
//                                    Tex tex = RichText.render("$b{$font" + ffont + "{$bg" + colgrey + "{$col" + colred + "{" + Math.round(dmg.a) + "} / $col" + colyellow + "{" + Math.round(dmg.b) + "}}}}", -1).tex();
                                        Tex tex = RichText.render(String.format("$bg%s{$b{$size[14]{$font%s{$col%s{%d}/$col%s{%d}}}}}", colgrey, ffont, colred, Math.round(dmg.a), colyellow, Math.round(dmg.b)), -1).tex();
//                                    g.chcolor(grey);
//                                    g.frect(ic.add(0, 35).sub(2, 2), tex.sz().add(2, 2));
//                                    g.chcolor();
                                        g.aimage(tex, ic.add(img.sz().mul(actionscale).x / 2, img.sz().mul(actionscale).y), 0.5, 0);
//                            FastText.printsf(g, ic.add(0, 35), "%d/%d", Math.round(dmg.a), Math.round(dmg.b));
                                        final int ua = ui.sess.glob.getcattr("unarmed").comp;
                                        final int mc = ui.sess.glob.getcattr("melee").comp;

                                        final Map<DefenseType, Double> newWeights = atk.calculateEnemyDefWeights(fv.maneuver, fv.maneuvermeter, ua, mc, act.cards, fv.current.defweights, fv.current.estimatedBlockWeight);
//                                    Tex texa = RichText.render("$b{$font" + ffont + "{$bg" + colgrey + "{$col" + colred + "{" + Math.round(newWeights.get(DefenseType.RED) * 100) + "} / $col" + colgreen + "{" + Math.round(newWeights.get(DefenseType.GREEN) * 100) + "} / $col" + colblue + "{" + Math.round(newWeights.get(DefenseType.BLUE) * 100) + "} / $col" + colyellow + "{" + Math.round(newWeights.get(DefenseType.YELLOW) * 100) + "}}}}", -1).tex();
                                        Tex texa = RichText.render(String.format("$bg%s{$b{$size[14]{$font%s{$col%s{%d}/$col%s{%d}/$col%s{%d}/$col%s{%d}}}}}", colgrey, ffont, colred, Math.round(newWeights.get(DefenseType.RED) * 100), colgreen, Math.round(newWeights.get(DefenseType.GREEN) * 100), colblue, Math.round(newWeights.get(DefenseType.BLUE) * 100), colyellow, Math.round(newWeights.get(DefenseType.YELLOW) * 100)), -1).tex();
//                                    g.chcolor(grey);
//                                    g.frect(ic.add(0, 45).sub(2, 2), tex.sz().add(2, 2));
//                                    g.chcolor();
                                        g.aimage(texa, ic.add(img.sz().mul(actionscale).x / 2, img.sz().mul(actionscale).y + tex.sz().y), 0.5, 0);
//                            FastText.printsf(g, ic.add(0, 45), "%d/%d/%d/%d",
//                                    Math.round(newWeights.get(DefenseType.RED) * 100),
//                                    Math.round(newWeights.get(DefenseType.GREEN) * 100),
//                                    Math.round(newWeights.get(DefenseType.BLUE) * 100),
//                                    Math.round(newWeights.get(DefenseType.YELLOW) * 100));
                                    } else if (act.card instanceof Restoration) {
                                        final Restoration restro = (Restoration) act.card;
                                        final Map<DefenseType, Double> newWeights = restro.getFutureWeights(act.cards, fv.defweights);
//                                    Tex tex = RichText.render("$b{$font" + ffont + "{$bg" + colgrey + "{$col" + colred + "{" + Math.round(newWeights.get(DefenseType.RED) * 100) + "} / $col" + colgreen + "{" + Math.round(newWeights.get(DefenseType.GREEN) * 100) + "} / $col" + colblue + "{" + Math.round(newWeights.get(DefenseType.BLUE) * 100) + "} / $col" + colyellow + "{" + Math.round(newWeights.get(DefenseType.YELLOW) * 100) + "}}}}", -1).tex();
                                        Tex tex = RichText.render(String.format("$bg%s{$b{$size[14]{$font%s{$col%s{%d}/$col%s{%d}/$col%s{%d}/$col%s{%d}}}}}", colgrey, ffont, colred, Math.round(newWeights.get(DefenseType.RED) * 100), colgreen, Math.round(newWeights.get(DefenseType.GREEN) * 100), colblue, Math.round(newWeights.get(DefenseType.BLUE) * 100), colyellow, Math.round(newWeights.get(DefenseType.YELLOW) * 100)), -1).tex();
//                                    g.chcolor(grey);
//                                    g.frect(ic.add(0, 35).sub(2, 2), tex.sz().add(2, 2));
//                                    g.chcolor();
                                        g.aimage(tex, ic.add(img.sz().mul(actionscale).x / 2, img.sz().mul(actionscale).y), 0.5, 0);
//                            FastText.printsf(g, ic.add(0, 35), "%d/%d/%d/%d",
//                                    Math.round(newWeights.get(DefenseType.RED) * 100),
//                                    Math.round(newWeights.get(DefenseType.GREEN) * 100),
//                                    Math.round(newWeights.get(DefenseType.BLUE) * 100),
//                                    Math.round(newWeights.get(DefenseType.YELLOW) * 100));
                                        if (act.card == Cards.flex) {
                                            final int ua = ui.sess.glob.getcattr("unarmed").comp;
                                            final int mc = ui.sess.glob.getcattr("melee").comp;

                                            final Map<DefenseType, Double> enemyWeights = restro.calculateEnemyDefWeights(fv.maneuver, fv.maneuvermeter, ua, mc, act.cards, fv.current.defweights, fv.current.estimatedBlockWeight);
//                                        Tex texa = RichText.render("$b{$font" + ffont + "{$bg" + colgrey + "{$col" + colred + "{" + Math.round(enemyWeights.get(DefenseType.RED) * 100) + "} / $col" + colgreen + "{" + Math.round(enemyWeights.get(DefenseType.GREEN) * 100) + "} / $col" + colblue + "{" + Math.round(enemyWeights.get(DefenseType.BLUE) * 100) + "} / $col" + colyellow + "{" + Math.round(enemyWeights.get(DefenseType.YELLOW) * 100) + "}}}}", -1).tex();
                                            Tex texa = RichText.render(String.format("$bg%s{$b{$size[14]{$font%s{$col%s{%d}/$col%s{%d}/$col%s{%d}/$col%s{%d}}}}}", colgrey, ffont, colred, Math.round(enemyWeights.get(DefenseType.RED) * 100), colgreen, Math.round(enemyWeights.get(DefenseType.GREEN) * 100), colblue, Math.round(enemyWeights.get(DefenseType.BLUE) * 100), colyellow, Math.round(enemyWeights.get(DefenseType.YELLOW) * 100)), -1).tex();
//                                        g.chcolor(grey);
//                                        g.frect(ic.add(0, 45).sub(2, 2), tex.sz().add(2, 2));
//                                        g.chcolor();
                                            g.aimage(texa, ic.add(img.sz().mul(actionscale).x / 2, img.sz().mul(actionscale).y + tex.sz().y), 0.5, 0);
//                                FastText.printsf(g, ic.add(0, 45), "%d/%d/%d/%d",
//                                        Math.round(enemyWeights.get(DefenseType.RED) * 100),
//                                        Math.round(enemyWeights.get(DefenseType.GREEN) * 100),
//                                        Math.round(enemyWeights.get(DefenseType.BLUE) * 100),
//                                        Math.round(enemyWeights.get(DefenseType.YELLOW) * 100));
                                        }
                                    }
                                }
                            }

                            if (Config.combshowkeys) {
                                Tex key;
                                if (Config.combatkeys == 0) {
                                    key = keystex[i];
                                } else if (Config.combatkeys == 1) {
                                    key = keysftex[i];
                                } else if (Config.combatkeys == 2) {
                                    key = keysfftex[i];
                                } else {
                                    key = keysitex[i];
                                }
                                g.aimage(key, ca.add((int) (pitch / 2), (int) (pitch / 2)), 0.5, 0.5);
                            }
                        }
                    } catch (Loading l) {
                    }
                }

                if (configuration.showcombatborder) {
                    g.chcolor(Color.WHITE);
                    g.rect(Coord.z, sz);
                    g.chcolor();
                }
                super.draw(g);
            }

            public boolean mousewheel(Coord coord, int amount) {
                if (ui.modflags() == (UI.MOD_CTRL | UI.MOD_META)) {
                    float scale = actionscale - (0.1f * amount);
                    float lastscale = actionscale;
                    if (scale > 3)
                        actionscale = 3;
                    else if (scale < 0.5)
                        actionscale = 0.5f;
                    else
                        actionscale = scale;

                    resize(new Coord(Math.round(awc.x * actionscale), Math.round(awc.y * actionscale)));
                    move(new Coord(Math.round(c.x - (sz.x - (awc.x * lastscale)) / 2), Math.round(c.y - (sz.y - (awc.y * lastscale)) / 2)));
                    return (true);
                }
                return (super.mousewheel(c, amount));
            }

            public Object tooltip(Coord c, Widget prev) {
                for (int i = 0; i < actions.length; i++) {
//                    Coord ca = Config.altfightui ? new Coord(cx - 18, ui.gui.sz.y - 250).add(actc(i)).add(16, 16) : pcc.add(actc(i));
//                    Coord ca = new Coord(cx - 18, sz.y - 250).add(actc(i)).add(16, 16);
                    float pitch = (configuration.showactioninfo ? actpitch : smallactpitch) * actionscale;
                    Coord ca = actionAnchor.get().add(actc(actionscale, i));
                    Indir<Resource> act = (actions[i] == null) ? null : actions[i].res;
                    try {
                        if (act != null) {
//                            Tex img = act.get().layer(Resource.imgc).tex();
                            if (c.isect(ca, new Coord(pitch, pitch))) {
                                String tip = act.get().layer(Resource.tooltip).t + " ($bg[60,60,60,168]{$b{$font" + ffont + "{$col[255,128,0]{" + keytips[i] + "}}}})";
                                if ((acttip == null) || !acttip.text.equals(tip))
                                    acttip = RichText.render(tip, -1);
                                return (acttip);
                            }
                        }
                    } catch (Loading l) {
                    }
                }
                return (null);
            }
        };

        cooldownWdg = new MovableWidget(UI.scale(600, 200), "FightSessCooldowns") {
            @Override
            public void draw(GOut g) {
                double now = Utils.rtime();
//                int gcx = ui.gui.sz.x / 2;
                int gcx = sz.x / 2;
                int gcy = sz.y / 2 - 50;
                for (Buff buff : fv.buffs.children(Buff.class)) {
//                    Coord bc = Config.altfightui ? new Coord(gcx - buff.c.x - Buff.cframe.sz().x - 80, 180) : pcc.add(-buff.c.x - Buff.cframe.sz().x - 20, buff.c.y + pho - Buff.cframe.sz().y);
                    Coord bc = new Coord(gcx - buff.c.x - Buff.cframe.sz().x - UI.scale(80), gcy);
                    drawOpening(g, buff, bc);
                }

                if (fv.current != null) {
                    for (Buff buff : fv.current.buffs.children(Buff.class)) {
                        Coord bc = new Coord(gcx + buff.c.x + UI.scale(80), gcy);
                        drawOpening(g, buff, bc);
                    }

                    g.aimage(ip.get().tex(), new Coord(gcx - UI.scale(45), gcy + UI.scale(20)), 0.5, 0.5);
                    g.aimage(oip.get().tex(), new Coord(gcx + UI.scale(45), gcy + UI.scale(20)), 0.5, 0.5);

                    if (fv.lsrel.size() > 1) {
                        fxon(fv.current.gobid, tgtfx);
//                        if (Config.showothercombatinfo) {
//                            for (int i = 0; i < fv.lsrel.size(); i++) {
//                                if (fv.current != fv.lsrel.get(i)) {
//                                    try {
//                                        Coord buffcoord = null;
//                                        for (Buff buff : fv.lsrel.get(i).buffs.children(Buff.class)) {
//                                            pcc2 = ui.gui.map.glob.oc.getgob(fv.lsrel.get(i).gobid).sc;
//                                            //  Coord cc = pcc2.add(buff.c.x / 32 * 24, -100);
//                                            Coord cc = ui.gui.map.glob.oc.getgob(fv.lsrel.get(i).gobid).sc.add(new Coord(ui.gui.map.glob.oc.getgob(fv.lsrel.get(i).gobid).sczu.mul(15)));
//                                            Coord finalcc = new Coord(cc.x, cc.y - 60);
//                                            if (buffcoord == null)
//                                                buffcoord = finalcc;
//                                            else
//                                                finalcc = buffcoord.add(buff.c.x / 32 * 24, 0);
//                                            drawOpeningofftarget(g, buff, finalcc, 24);
//                                        }
//                                        int itransfer = i;
//                                        Text.UText<?> oip2 = new Text.UText<Integer>(ipf2) {
//                                            public String text(Integer v) {
//                                                return ("IP: " + v);
//                                            }
//
//                                            public Integer value() {
//                                                return (fv.lsrel.get(itransfer).oip);
//                                            }
//                                        };
//                                        Text.UText<?> ip2 = new Text.UText<Integer>(ipf) {
//                                            public String text(Integer v) {
//                                                return ("IP: " + v);
//                                            }
//
//                                            public Integer value() {
//                                                return (fv.lsrel.get(itransfer).ip);
//                                            }
//                                        };
//                                        Coord cc = this.ui.gui.map.glob.oc.getgob(fv.lsrel.get(i).gobid).sc.add(new Coord(this.ui.gui.map.glob.oc.getgob(fv.lsrel.get(i).gobid).sczu.mul(15)));
//                                        Coord finalcc = new Coord(cc.x, cc.y - 50);
//                                        g.aimage(ip2.get().tex(), finalcc.add(-5, 0), 1, .5);
//                                        g.aimage(oip2.get().tex(), finalcc.add(-5, 20), 1, .5);
//                                    } catch (Exception idk) {
//                                    }
//                                }
//                            }
//                        }
                    }
                }

                {
//                    Coord cdc = Config.altfightui ? new Coord(gcx, 200) : pcc.sub(cmc);
                    Coord cdc = new Coord(gcx, gcy + UI.scale(20));
                    if (now < fv.atkct) {
                        double a = (now - fv.atkcs) / (fv.atkct - fv.atkcs);
                        g.chcolor(255, 0, 128, 224);
                        g.fellipse(cdc, UI.scale(24, 24), Math.PI / 2 - (Math.PI * 2 * Math.min(1.0 - a, 1.0)), Math.PI / 2);
                        g.chcolor();
                        if (Config.showcooldown)
                            g.atextstroked(Utils.fmt1DecPlace(fv.atkct - now), cdc, 0.5, 0.5, Color.WHITE, Color.BLACK, Text.num11Fnd);
                    }
                    g.image(cdframe, cdc.sub(cdframe.sz().div(2)));
                    if (configuration.showactioninfo) {
                        if (fv.current != null && fv.current.estimatedBlockWeight != 0) {
                            final int stat;
                            final WeightType type;
                            if (fv.current.maneuver != null) {
                                stat = (int) fv.current.maneuver.calculateStat(fv.current.estimatedBlockWeight);
                                type = fv.current.maneuver.type;
                            } else {
                                //An animal, just assume blockweight -> UA
                                type = WeightType.UA;
                                stat = (int) fv.current.estimatedBlockWeight;
                            }
//                        Tex tex = RichText.render("$b{$font" + ffont + "{$bg" + colgrey + "{$col" + colred + "{" + type + "} : $col" + colgreen + "{" + stat + "}}}}", -1).tex();
                            Tex tex = RichText.render(String.format("$bg%s{$b{$size[14]{$font%s{%s : %d}}}}", colgrey, ffont, type, stat), -1).tex();
//                        g.chcolor(grey);
//                        g.rect(cdc.add(-tex.sz().x / 2, -50).sub(2, 2), tex.sz().add(2, 2));
//                        g.chcolor();
                            g.aimage(tex, cdc.sub(0, cdframe.sz().y / 2), 0.5, 1);
//                        FastText.aprintsf(g, cdc.add(0, -50), 0.5, 0.0, "%s: %d", type, stat);
                        }
                    }
                }

                try {
                    Indir<Resource> lastact = fv.lastact;
                    if (lastact != Fightsess.this.lastact1) {
                        Fightsess.this.lastact1 = lastact;
                        Fightsess.this.lastacttip1 = null;
                    }
                    double lastuse = fv.lastuse;
                    if (lastact != null) {
                        Tex ut = lastact.get().layer(Resource.imgc).tex();
//                        Coord useul = Config.altfightui ? lastMoveAnchor.sub(ut.sz().x / 2, 0) : pcc.add(usec1).sub(ut.sz().div(2));
                        Coord useul = lastMoveAnchor.sub(ut.sz().x / 2, 0);
                        g.image(ut, useul);
                        g.image(useframe, useul.sub(useframeo));
                        double a = now - lastuse;
                        if (a < 1) {
                            Coord off = new Coord((int) (a * ut.sz().x / 2), (int) (a * ut.sz().y / 2));
                            g.chcolor(255, 255, 255, (int) (255 * (1 - a)));
                            g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
                            g.chcolor();
                        }
                    }
                } catch (Loading l) {
                }

                if (fv.current != null) {
                    try {
                        Indir<Resource> lastact = fv.current.lastact;
                        if (lastact != Fightsess.this.lastact2) {
                            Fightsess.this.lastact2 = lastact;
                            Fightsess.this.lastacttip2 = null;
                        }
                        double lastuse = fv.current.lastuse;
                        if (lastact != null) {
                            Tex ut = lastact.get().layer(Resource.imgc).tex();
//                            Coord useul = Config.altfightui ? enemyLastMoveAnchor.sub(ut.sz().x / 2, 0) : pcc.add(usec2).sub(ut.sz().div(2));
                            Coord useul = enemyLastMoveAnchor.sub(ut.sz().x / 2, 0);
                            g.image(ut, useul);
                            g.image(useframe, useul.sub(useframeo));
                            double a = now - lastuse;
                            if (a < 1) {
                                Coord off = new Coord((int) (a * ut.sz().x / 2), (int) (a * ut.sz().y / 2));
                                g.chcolor(255, 255, 255, (int) (255 * (1 - a)));
                                g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
                                g.chcolor();
                            }
                        }
                    } catch (Loading l) {
                    }
                }

                if (configuration.showcombatborder) {
                    g.chcolor(Color.WHITE);
                    g.rect(Coord.z, sz);
                    g.chcolor();
                }
                super.draw(g);
            }

            public Object tooltip(Coord c, Widget prev) {
                int cx = sz.x / 2;
                int cy = sz.y / 2 - UI.scale(50);
                for (Buff buff : fv.buffs.children(Buff.class)) {
//            Coord dc = Config.altfightui ? new Coord(cx - buff.c.x - Buff.cframe.sz().x - 80, 180) : pcc.add(-buff.c.x - Buff.cframe.sz().x - 20, buff.c.y + pho - Buff.cframe.sz().y);
                    Coord dc = new Coord(cx - buff.c.x - Buff.cframe.sz().x - UI.scale(80), cy);
                    if (c.isect(dc, buff.sz)) {
                        Object ret = buff.tooltip(c.sub(dc), prevtt);
                        if (ret != null) {
                            prevtt = buff;
                            return (ret);
                        }
                    }
                }

                if (fv.current != null) {
                    for (Buff buff : fv.current.buffs.children(Buff.class)) {
//                        Coord dc = Config.altfightui ? new Coord(cx + buff.c.x + 80, 180) : pcc.add(buff.c.x + 20, buff.c.y + pho - Buff.cframe.sz().y);
                        Coord dc = new Coord(cx + buff.c.x + UI.scale(80), cy);
                        if (c.isect(dc, buff.sz)) {
                            Object ret = buff.tooltip(c.sub(dc), prevtt);
                            if (ret != null) {
                                prevtt = buff;
                                return (ret);
                            }
                        }
                    }
                }
                try {
                    Indir<Resource> lastact = Fightsess.this.lastact1;
                    if (lastact != null) {
                        Coord usesz = lastact.get().layer(Resource.imgc).sz;
//                        Coord lac = Config.altfightui ? new Coord(cx - 69, 120).add(usesz.div(2)) : pcc.add(usec1);
//                        Coord lac = new Coord(cx - 69, 120).add(usesz.div(2));
                        Coord lac = lastMoveAnchor;
                        if (c.isect(lac, usesz)) {
                            if (lastacttip1 == null)
                                lastacttip1 = Text.render(lastact.get().layer(Resource.tooltip).t);
                            return (lastacttip1);
                        }
                    }
                } catch (Loading l) {
                }
                try {
                    Indir<Resource> lastact = Fightsess.this.lastact2;
                    if (lastact != null) {
                        Coord usesz = lastact.get().layer(Resource.imgc).sz;
//                        Coord lac = Config.altfightui ? new Coord(cx + 69 - usesz.x, 120).add(usesz.div(2)) : pcc.add(usec2);
//                        Coord lac = new Coord(cx + 69 - usesz.x, 120).add(usesz.div(2));
                        Coord lac = enemyLastMoveAnchor;
                        if (c.isect(lac.sub(usesz.x / 2, 0), usesz)) {
                            if (lastacttip2 == null)
                                lastacttip2 = Text.render(lastact.get().layer(Resource.tooltip).t);
                            return (lastacttip2);
                        }
                    }
                } catch (Loading l) {
                }
                return (null);
            }
        };

        final Coord center = sz.div(2);

        adda(actionWdg, center.add(0, center.y / 4), 0.5, 0.5);
        adda(cooldownWdg, center.sub(0, (int) (center.y / 1.5f)), 0.5, 0.5);

        anchors();
    }

    @Override
    protected void removed() {
        super.removed();
        ui.gui.fs = null;
        if (configuration.autocleardamage && ui.gui.map != null) {
            ui.sess.glob.gobmap.clear();
            ui.gui.map.removeCustomSprites(DamageText.id);
        }
    }

    public void presize() {
        resize(parent.sz);
        pcc = sz.div(2);

        if (actionWdg != null)
            actionWdg.move(new Coord(sz.x * (actionWdg.c.x + actionWdg.sz.x / 2) / oldsz.x, sz.y * (actionWdg.c.y + actionWdg.sz.y / 2) / oldsz.y), 0.5, 0.5);
        if (cooldownWdg != null)
            cooldownWdg.move(new Coord(sz.x * (cooldownWdg.c.x + cooldownWdg.sz.x / 2) / oldsz.x, sz.y * (cooldownWdg.c.y + cooldownWdg.sz.y / 2) / oldsz.y), 0.5, 0.5);
    }

    Supplier<Coord> actionAnchor = () -> actionWdg.sz.div(2);

    public void anchors() {
        final Coord center = sz.div(2);
        final Coord acenter = actionWdg.sz.div(2);
        final Coord ccenter = cooldownWdg.sz.div(2);
//        actionAnchor = center.add(0, center.y / 4);
//        cooldownAnchor = center.sub(0, (int) (center.y / 1.5f));
//        actionAnchor = acenter;
        cooldownAnchor = ccenter;
        enemyBuffAnchor = cooldownAnchor.add(UI.scale(50, 0));
        enemyIPAnchor = cooldownAnchor.add(UI.scale(75, 15));
        enemyLastMoveAnchor = cooldownAnchor.add(UI.scale(50, 25));
        buffAnchor = cooldownAnchor.sub(UI.scale(50, 0));
        IPAnchor = cooldownAnchor.add(UI.scale(-75, 15));
        lastMoveAnchor = cooldownAnchor.add(UI.scale(-50, 25));
    }

    private void updatepos() {
        MapView map;
        Gob pl;
        if (((map = getparent(GameUI.class).map) == null) || ((pl = map.player()) == null) || (pl.sc == null))
            return;
        pcc = pl.sc;
        pho = (int) (pl.sczu.mul(20f).y) - UI.scale(20);
    }

    private static final Resource tgtfx = Resource.remote().loadwait("gfx/hud/combat/trgtarw");
    private final Map<Pair<Long, Resource>, Sprite> cfx = new CacheMap<Pair<Long, Resource>, Sprite>();
    private final Collection<Sprite> curfx = new ArrayList<Sprite>();

    private void fxon(long gobid, Resource fx) {
        MapView map = getparent(GameUI.class).map;
        Gob gob = ui.sess.glob.oc.getgob(gobid);
        if ((map == null) || (gob == null))
            return;
        Pair<Long, Resource> id = new Pair<>(gobid, fx);
        Sprite spr = cfx.get(id);
        if (spr == null)
            cfx.put(id, spr = Sprite.create(null, fx, Message.nil));
        map.drawadd(gob.loc.apply(spr));
        curfx.add(spr);
    }

    public void tick(double dt) {
        for (Sprite spr : curfx)
            spr.tick((int) (dt * 1000));
        for (int i = 0; i < actions.length; ++i) {
            if (actions[i] != null) {
                actions[i].tick(ui);
            }
        }
        curfx.clear();
    }


    private static final Text.Furnace ipf = new PUtils.BlurFurn(new Text.Foundry(Text.serif, UI.scale(18), new Color(128, 128, 255)).aa(true), 1, 1, new Color(48, 48, 96));
    private static final Text.Furnace ipf2 = new PUtils.BlurFurn(new Text.Foundry(Text.serif, UI.scale(18), new Color(255, 0, 0)).aa(true), 1, 1, new Color(48, 48, 96));
    //private static final Text.Furnace ipf3 = new PUtils.BlurFurn(new Text.Foundry(Text.serif, 18, new Color(128, 128, 255)).aa(true), 1, 1, new Color(48, 48, 96));
    private final Text.UText<?> ip = new Text.UText<Integer>(ipf) {
        public String text(Integer v) {
//            return (Config.altfightui ? v.toString() : "IP: " + v);
            return v.toString();
        }

        public Integer value() {
            return (fv.current.ip);
        }

    };
    private final Text.UText<?> oip = new Text.UText<Integer>(ipf) {
        public String text(Integer v) {
//            return (Config.altfightui ? v.toString() : "IP: " + v);
            return v.toString();
        }

        public Integer value() {
            return (fv.current.oip);
        }
    };

    private static Coord actc(int i) {
        int rl = 5;

        int row = i / rl;
        if (Config.combatkeys == 1)
            row ^= 1;
        int pitch = configuration.showactioninfo ? actpitch : smallactpitch;

        return (new Coord((pitch * (i % rl)) - (((rl - 1) * pitch) / 2), (row * pitch)));
    }

    private static Coord actc(float scale, int i) {
        int rl = 5;

        int row = i / rl;
        if (Config.combatkeys == 1)
            row ^= 1;
        float pitch = (configuration.showactioninfo ? actpitch : smallactpitch) * scale;

        return (new Coord((pitch * (i % rl)) - (((rl - 1) * pitch) / 2), (row * pitch)).sub(pitch / 2, pitch));
    }

    private static final Coord cmc = new Coord(0, 67);
    private static final Coord usec1 = new Coord(-65, 67);
    private static final Coord usec2 = new Coord(65, 67);
    private Indir<Resource> lastact1 = null, lastact2 = null;
    private Text lastacttip1 = null, lastacttip2 = null;


    public void draw(GOut g) {
        updatepos();
        try {
            if (parent.focused != this) {
                raise();
                if (actionWdg != null)
                    actionWdg.raise();
                if (cooldownWdg != null)
                    cooldownWdg.raise();
                if (Config.forcefightfocusharsh)
                    parent.setfocus(this);
                else if (!ui.gui.chat.hasfocus && Config.forcefightfocus)
                    parent.setfocus(this);
            }
        } catch (Throwable e) {
        }
        super.draw(g);
    }

    private void drawOpening(GOut g, Buff buff, Coord bc) {
        if (Config.combaltopenings) {
            try {
                Resource res = buff.res.get();
                Color clr = openings.get(res.name);
                if (clr == null) {
                    buff.draw(g.reclip(bc, buff.sz));
                    return;
                }

                Double ameter = (buff.ameter >= 0) ? Double.valueOf(buff.ameter / 100.0) : buff.ameteri.get();
                if (ameter != null) {
                    g.image(Buff.cframe, bc);
                    g.chcolor(Color.BLACK);
                    g.frect(bc.add(Buff.ameteroff), Buff.ametersz);
                    g.chcolor(Color.WHITE);
                    g.frect(bc.add(Buff.ameteroff), new Coord((int) Math.floor(ameter * Buff.ametersz.x), Buff.ametersz.y));
                } else {
                    g.image(Buff.frame, bc);
                }

                bc.x += UI.scale(3);
                bc.y += UI.scale(3);

                g.chcolor(clr);
                g.frect(bc, simpleOpeningSz);

                g.chcolor(Color.WHITE);
                if (buff.atex == null)
                    buff.atex = Text.renderstroked((ameter != null ? (int) (ameter * 100) : "") + "", Color.WHITE, Color.BLACK, Text.num12boldFnd).tex();
                Tex atex = buff.atex;
                bc.x = bc.x + simpleOpeningSz.x / 2 - atex.sz().x / 2;
                bc.y = bc.y + simpleOpeningSz.y / 2 - atex.sz().y / 2;
                g.image(atex, bc);
                g.chcolor();
            } catch (Loading l) {
            }
        } else {
            buff.draw(g.reclip(bc, buff.sz));
        }
    }

    private void drawOpeningofftarget(GOut g, Buff buff, Coord bc, int size) {
        try {
            Coord smalSz = new Coord(size, size);
            Coord metSz = new Coord(size, UI.scale(3));
            Resource res = buff.res.get();
            Color clr = openings.get(res.name);
            if (clr == null) {
                buff.draw(g.reclip(bc, smalSz.add(0, UI.scale(10))), size);
                return;
            }

            Double ameter = (buff.ameter >= 0) ? Double.valueOf(buff.ameter / 100.0) : buff.ameteri.get();
            if (ameter != null) {
                g.chcolor(Color.BLACK);
                g.frect(bc.add(Buff.ameteroff), metSz);
                g.chcolor(Color.WHITE);
                g.frect(bc.add(Buff.ameteroff), new Coord(buff.ameter * metSz.x / 100, metSz.y));
            }

            bc.x += UI.scale(3);
            bc.y += UI.scale(3);
            g.chcolor(clr);
            g.frect(bc, smalSz);
            g.chcolor(Color.WHITE);
            if (buff.atex == null) {
                buff.atex = Text.renderstroked((ameter != null ? (int) (ameter * 100) : "") + "", Color.WHITE, Color.BLACK, Text.num12boldFnd).tex();
            }

            Tex atex = buff.atex;
            bc.x = bc.x + smalSz.x / 2 - atex.sz().x / 2;
            bc.y = bc.y + smalSz.y / 2 - atex.sz().y / 2;
            g.image(atex, bc);
            g.chcolor();
        } catch (Loading l) {
        }
    }

    private GItem weap() {
        return ui.gui.equipory != null ? ui.gui.equipory.getWeapon() : null;
    }

    private int str() {
        final Glob.CAttr strattr = ui.sess.glob.getcattr("str");
        return strattr.comp;
    }

    private Widget prevtt = null;
    private Text acttip = null;
    public static final String[] keytips = {"1", "2", "3", "4", "5", "Shift+1", "Shift+2", "Shift+3", "Shift+4", "Shift+5"};

    public void uimsg(String msg, Object... args) {
        if (msg == "act") {
            try {
                int n = (Integer) args[0];
                if (args.length > 1) {
                    Indir<Resource> res = ui.sess.getres((Integer) args[1]);
                    actions[n] = new Action(res, n);
                } else {
                    actions[n] = null;
                }
            } catch (Loading e) {
            }
        } else if (msg == "acool") {
            int n = (Integer) args[0];
            double now = Utils.rtime();
            actions[n].cs = now;
            actions[n].ct = now + (((Number) args[1]).doubleValue() * 0.06);
        } else if (msg == "use") {
            this.use = (Integer) args[0];
            this.useb = (args.length > 1) ? ((Integer) args[1]) : -1;
        } else if (msg == "used") {
        } else {
            super.uimsg(msg, args);
        }
    }


    private int last_button = -1;
    private long last_sent = System.currentTimeMillis();

    /* XXX: This is a bit ugly, but release message do need to be
     * properly sequenced with use messages in some way. */
    private class Release implements MapView.Delayed, BGL.Request {
        final int n;

        Release(int n) {
            this.n = n;
        }

        public void run(GOut g) {
            g.gl.bglSubmit(this);
        }

        public void run(javax.media.opengl.GL2 gl) {
            wdgmsg("rel", n);
        }
    }

    private UI.Grab holdgrab = null;
    private int held = -1;

    public boolean globtype(char key, KeyEvent ev) {
        int n = -1;
        if (Config.combatkeys == 0) {
            if ((ev.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) == 0) {
                switch (ev.getKeyCode()) {
                    case KeyEvent.VK_1:
                        n = 0;
                        break;
                    case KeyEvent.VK_2:
                        n = 1;
                        break;
                    case KeyEvent.VK_3:
                        n = 2;
                        break;
                    case KeyEvent.VK_4:
                        n = 3;
                        break;
                    case KeyEvent.VK_5:
                        n = 4;
                        break;
                }
                if ((n >= 0) && ((ev.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0))
                    n += 5;
            }
        } else if (Config.combatkeys == 1) { // F1-F5
            switch (ev.getKeyCode()) {
                case KeyEvent.VK_1:
                    n = 0;
                    break;
                case KeyEvent.VK_2:
                    n = 1;
                    break;
                case KeyEvent.VK_3:
                    n = 2;
                    break;
                case KeyEvent.VK_4:
                    n = 3;
                    break;
                case KeyEvent.VK_5:
                    n = 4;
                    break;
                case KeyEvent.VK_F1:
                    n = 5;
                    break;
                case KeyEvent.VK_F2:
                    n = 6;
                    break;
                case KeyEvent.VK_F3:
                    n = 7;
                    break;
                case KeyEvent.VK_F4:
                    n = 8;
                    break;
                case KeyEvent.VK_F5:
                    n = 9;
                    break;
            }
        } else if (Config.combatkeys == 2) { // F1-F10
            switch (ev.getKeyCode()) {
                case KeyEvent.VK_F1:
                    n = 0;
                    break;
                case KeyEvent.VK_F2:
                    n = 1;
                    break;
                case KeyEvent.VK_F3:
                    n = 2;
                    break;
                case KeyEvent.VK_F4:
                    n = 3;
                    break;
                case KeyEvent.VK_F5:
                    n = 4;
                    break;
                case KeyEvent.VK_F6:
                    n = 5;
                    break;
                case KeyEvent.VK_F7:
                    n = 6;
                    break;
                case KeyEvent.VK_F8:
                    n = 7;
                    break;
                case KeyEvent.VK_F9:
                    n = 8;
                    break;
                case KeyEvent.VK_F10:
                    n = 9;
                    break;
            }
        } else { // 1-10
            switch (ev.getKeyCode()) {
                case KeyEvent.VK_1:
                    n = 0;
                    break;
                case KeyEvent.VK_2:
                    n = 1;
                    break;
                case KeyEvent.VK_3:
                    n = 2;
                    break;
                case KeyEvent.VK_4:
                    n = 3;
                    break;
                case KeyEvent.VK_5:
                    n = 4;
                    break;
                case KeyEvent.VK_6:
                    n = 5;
                    break;
                case KeyEvent.VK_7:
                    n = 6;
                    break;
                case KeyEvent.VK_8:
                    n = 7;
                    break;
                case KeyEvent.VK_9:
                    n = 8;
                    break;
                case KeyEvent.VK_0:
                    n = 9;
                    break;
            }
        }

        int fn = n;
        if ((n >= 0) && (n < actions.length) && (last_button != fn || (System.currentTimeMillis() - last_sent) >= 150)) {
            wdgmsg("use", fn, 1, ui.modflags());
            last_button = fn;
            last_sent = System.currentTimeMillis();
            return (true);
        } else if ((n >= 0) && (n < actions.length))
            return (true);
        else
            return (super.globtype(key, ev));
    }
}
