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

package haven.chrwnd;

import haven.Area;
import haven.Buff;
import haven.Bufflist;
import haven.Button;
import haven.CharWnd;
import haven.CheckBox;
import haven.Composer;
import haven.Config;
import haven.Coord;
import haven.Frame;
import haven.GItem;
import haven.GOut;
import haven.GameUI;
import haven.Glob;
import haven.IButton;
import haven.Img;
import haven.Inventory;
import haven.ItemInfo;
import haven.Label;
import haven.Loading;
import haven.PUtils;
import haven.Resource;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.UI;
import haven.Utils;
import haven.Widget;
import haven.resutil.Curiosity;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static haven.CharWnd.attrf;
import static haven.CharWnd.attrw;
import static haven.CharWnd.buff;
import static haven.CharWnd.catf;
import static haven.CharWnd.debuff;
import static haven.CharWnd.every;
import static haven.CharWnd.iconfilter;
import static haven.CharWnd.margin1;
import static haven.CharWnd.margin2;
import static haven.CharWnd.margin3;
import static haven.CharWnd.numfnd;
import static haven.CharWnd.offy;
import static haven.CharWnd.other;
import static haven.CharWnd.tbuff;
import static haven.CharWnd.wbox;
import static haven.CharWnd.width;
import static haven.PUtils.convolve;

public class SAttrWnd extends Widget {
    public final Collection<SAttr> skill;
    private final Coord studyc;
    private CharWnd chr;
    private int scost;

    @RName("sattr")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            return (new SAttrWnd(ui.sess.glob));
        }
    }

    public class SAttr extends AttrWdg {
        public final Tex rnm;
        public final Tex img;
        public final Color bg;
        public int tbv, tcv, cost;
        private final IButton add, sub;
        private Tex ct;
        private int cbv, ccv;

        private SAttr(Glob glob, String attr, Color bg) {
            super(Coord.of(attrw, attrf.height() + UI.scale(2)), glob, attr);
            Resource res = Loading.waitfor(this.attr.res());
            this.img = new TexI(convolve(res.flayer(Resource.imgc).img, new Coord(this.sz.y, this.sz.y), iconfilter));
            this.rnm = PUtils.strokeTex(attrf.render(res.flayer(Resource.tooltip).t));
            this.bg = bg;
            add = adda(new IButton("gfx/hud/buttons/add", "u", "d", "h").action(() -> adj(1)),
                    sz.x - margin1, sz.y / 2, 1, 0.5);
            sub = adda(new IButton("gfx/hud/buttons/sub", "u", "d", "h").action(() -> adj(-1)),
                    sz.x - margin3, sz.y / 2, 1, 0.5);
        }

        public void tick(double dt) {
            if ((attr.base != cbv) ||
                    (attr.comp != ccv)) {
                cbv = attr.base;
            }
            if (attr.comp != ccv) {
                ccv = attr.comp;
                if (tbv <= cbv) {
                    tbv = cbv;
                    tcv = ccv;
                    updcost();
                }
                Color c = Color.WHITE;
                if (ccv > cbv) {
                    c = buff;
                    tooltip = Text.render(String.format("%d + %d", cbv, ccv - cbv));
                } else if (ccv < cbv) {
                    c = debuff;
                    tooltip = Text.render(String.format("%d - %d", cbv, cbv - ccv));
                } else {
                    tooltip = null;
                }
                if (tcv > ccv)
                    c = tbuff;
                ct = PUtils.strokeTex(attrf.render(Integer.toString(tcv), c));
                cbv = tcv;
            }
        }

        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, sz);
            g.chcolor();
            super.draw(g);
            Coord cn = new Coord(0, sz.y / 2);
            g.aimage(img, cn.add(UI.scale(5), 0), UI.scale(20, 20), 0, 0.5);
            g.aimage(rnm, cn.add(UI.scale(20 + 10), 1), 0, 0.5);
            if (!Config.splitskills) {
                g.aimage(ct, cn.add(sz.x - UI.scale(40), 1), 1, 0.5);
            } else {
                cbv = attr.base;
                ccv = attr.comp;

//                ccv + " " + tbv + " " + cbv    260 205 200
                if (ccv > cbv) {
                    Tex buffed;
                    if (tbv > cbv) {
//                        buffed = attrf.render(Integer.toString(tbv + (ccv - cbv)), tbuff);
                        buffed = PUtils.strokeTex(attrf.render(Integer.toString(ccv + (tbv - cbv)), tbuff));
                    } else {
                        buffed = PUtils.strokeTex(attrf.render(Integer.toString(ccv), buff));
                    }
                    g.aimage(buffed, cn.add(sz.x - UI.scale(35), 1), 1, 0.5);
                } else if (ccv < cbv) {
                    if (tbv > cbv) {
//                        Text buffed = attrf.render(Integer.toString(tbv + (cbv - ccv)), tbuff);
                        Tex buffed = PUtils.strokeTex(attrf.render(Integer.toString(ccv + (tbv - cbv)), tbuff));
                        g.aimage(buffed, cn.add(sz.x - UI.scale(35), 1), 1, 0.5);
                    } else {
                        Tex debuffed = PUtils.strokeTex(attrf.render(Integer.toString(ccv), debuff));
                        g.aimage(debuffed, cn.add(sz.x - UI.scale(35), 1), 1, 0.5);
                    }
                }

                Tex base;
                if (tbv > cbv) {
                    base = PUtils.strokeTex(attrf.render(Integer.toString(tbv), tbuff));
                } else {
                    base = PUtils.strokeTex(attrf.render(Integer.toString(cbv), Color.WHITE));
                }
                g.aimage(base, cn.add(sz.x - UI.scale(65), 1), 1, 0.5);
            }
        }

        private void updcost() {
            int cost = 100 * ((tbv + (tbv * tbv)) - (attr.base + (attr.base * attr.base))) / 2;
            scost += cost - this.cost;
            this.cost = cost;
        }

        public void adj(int a) {
            if (tbv + a < attr.base) a = attr.base - tbv;
            tbv += a;
            tcv += a;
            cbv = ccv = 0;
            updcost();
        }

        public void reset() {
            tbv = attr.base;
            tcv = attr.comp;
            cbv = ccv = 0;
            updcost();
        }

        public boolean mousewheel(Coord c, int amount) {
            int b = amount * Config.statgainsize;
            adj(-b);
            return (true);
        }
    }

    public RLabel<?> explabel() {
        return (new RLabel<Integer>(() -> chr.exp, Utils::thformat, new Color(192, 192, 255)));
    }

    public RLabel<?> enclabel() {
        return (new RLabel<Integer>(() -> chr.enc, Utils::thformat, new Color(255, 255, 192)));
    }

    protected void attached() {
        this.chr = getparent(CharWnd.class);
        super.attached();
    }

    public static class StudyInfo extends Widget {
        public Widget study;
        public int texp, tw, tenc;
        public double tlph;
        private final Text.UTex<?> texpt = new Text.UTex<>(() -> texp, s -> PUtils.strokeTex(numfnd.render(Utils.thformat(s))));
        private final Text.UTex<?> twt = new Text.UTex<>(() -> tw + "/" + ui.sess.glob.getcattr("int").comp, s -> PUtils.strokeTex(numfnd.render(s)));
        private final Text.UTex<?> tenct = new Text.UTex<>(() -> tenc, s -> PUtils.strokeTex(numfnd.render(Integer.toString(tenc))));
        private final DecimalFormat f = new DecimalFormat("##.##");
        private final Text.UTex<?> tlpht = new Text.UTex<>(() -> tlph, s -> PUtils.strokeTex(Text.std.render(String.format("%s", !Utils.getprefb("tooltipapproximatert", false) ? f.format(tlph) : f.format(tlph * ui.sess.glob.getTimeFac())))));

        private StudyInfo(Coord sz, Widget study) {
            super(sz);
            this.study = study;
            add(new Label("Attention:"), UI.scale(2, 2));
            add(new Label("Experience cost:"), UI.scale(2, 32));
            add(new Label("LP/H"), UI.scale(2), sz.y - UI.scale(64));
            add(new Label("Learning points:"), UI.scale(2), sz.y - UI.scale(32));

            if (Config.studybuff && ((Inventory) study).getFreeSpace() > 0) {
                Buff tgl = study.ui.gui.buffs.gettoggle("brain");
                if (tgl == null)
                    study.ui.gui.buffs.addchild(new Buff(Bufflist.buffbrain.indir()));
            }
        }

        private void upd() {
            int texp = 0, tw = 0, tenc = 0;
            double tlph = 0;
            for (GItem item : study.children(GItem.class)) {
                try {
                    Curiosity ci = ItemInfo.find(Curiosity.class, item.info());
                    if (ci != null) {
                        texp += ci.exp;
                        tw += ci.mw;
                        tenc += ci.enc;
                        tlph += (ci.exp / (ci.time / 60));
                    }
                } catch (Loading l) {
                }
            }
            this.texp = texp;
            this.tw = tw;
            this.tenc = tenc;
            this.tlph = tlph;
        }

        public void tick(double dt) {
            upd();
            super.tick(dt);
        }

        public void draw(GOut g) {
            super.draw(g);
            g.chcolor(255, 192, 255, 255);
            g.aimage(twt.get(), new Coord(sz.x - UI.scale(4), UI.scale(17)), 1.0, 0.0);
            g.chcolor(255, 255, 192, 255);
            g.aimage(tenct.get(), new Coord(sz.x - UI.scale(4), UI.scale(47)), 1.0, 0.0);
            g.chcolor(192, 192, 255, 255);
            g.aimage(texpt.get(), sz.add(UI.scale(-4, -15)), 1.0, 0.0);
            g.chcolor(192, 192, 255, 255);
            g.aimage(tlpht.get(), sz.add(UI.scale(-4, -49)), 1.0, 0.0);
        }
    }

    private void buy() {
        ArrayList<Object> args = new ArrayList<>();
        for (SAttr attr : skill) {
            if (attr.tbv > 0) {
                args.add(attr.attr.nm);
                args.add(attr.attr.base + attr.tbv);
            }
        }
        wdgmsg("sattr", args.toArray(new Object[0]));
    }

    private void reset() {
        for (SAttr attr : skill)
            attr.reset();
    }

    public SAttrWnd(Glob glob) {
        Widget left = new Widget.Temporary();
        int bottom;
        {
            skill = new ArrayList<>();
            skill.add(new SAttr(glob, "unarmed", every));
            skill.add(new SAttr(glob, "melee", other));
            skill.add(new SAttr(glob, "ranged", every));
            skill.add(new SAttr(glob, "explore", other));
            skill.add(new SAttr(glob, "stealth", every));
            skill.add(new SAttr(glob, "sewing", other));
            skill.add(new SAttr(glob, "smithing", every));
            skill.add(new SAttr(glob, "masonry", other));
            skill.add(new SAttr(glob, "carpentry", every));
            skill.add(new SAttr(glob, "cooking", other));
            skill.add(new SAttr(glob, "farming", every));
            skill.add(new SAttr(glob, "survive", other));
            skill.add(new SAttr(glob, "lore", every));
            Composer composer = new Composer(left);
            left.add(CharWnd.settip(new Img(catf.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Abilities")).tex()), "gfx/hud/chr/tips/sattr"));
            composer.add(offy);
            composer.pad(wbox.btloff().add(margin1, 0));
            for (SAttr v : skill) {
                composer.add(v);
            }
            Frame frame = Frame.around(left, skill);
            bottom = frame.c.y + frame.sz.y;
        }
        left.pack();

        Widget right = new Widget.Temporary();
        {
            Composer composer = new Composer(right);
            studyc = right.add(CharWnd.settip(new Img(catf.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Study Report")).tex()), "gfx/hud/chr/tips/study")).c.add(5, 0);
            composer.add(offy + UI.scale(151));
            int fy = composer.y();
            composer.add(margin1);
            composer.vmrgn(margin1).hpad(UI.scale(15));
            int rx = attrw - margin2;
            composer.addrf(rx, new Label(Resource.getLocString(Resource.BUNDLE_LABEL, "Experience points:")), enclabel());
            composer.addrf(rx, new Label(Resource.getLocString(Resource.BUNDLE_LABEL, "Learning points:")), explabel());
            composer.addrf(rx,
                    new Label(Resource.getLocString(Resource.BUNDLE_LABEL, "Learning cost:")),
                    new RLabel<Integer>(() -> scost, Utils::thformat, n -> (n > chr.exp) ? debuff : Color.WHITE)
            );
            composer.hpad(rx - UI.scale(160))
                    .hmrgn(margin2)
                    .vmrgn(0);
            composer.addr(
                    new Button(UI.scale(75), Resource.getLocString(Resource.BUNDLE_BUTTON, "Reset")).action(this::reset),
                    new Button(UI.scale(75), Resource.getLocString(Resource.BUNDLE_BUTTON, "Buy")).action(this::buy)
            );
            Frame.around(right, Area.sized(new Coord(margin1, fy).add(wbox.btloff()), new Coord(attrw, bottom - fy - 2 * wbox.btloff().y)));
        }
        right.pack();

        add(left);
        add(right, new Coord(width, 0));
        Widget.Temporary.optimize(this);

        pack();
    }

    public void addchild(Widget child, Object... args) {
        String place = (args[0] instanceof String) ? (((String) args[0]).intern()) : null;
        if (place == "study") {
            add(child, new Coord(width + margin1, offy).add(wbox.btloff()));
            Frame.around(this, Collections.singletonList(child));
            Widget inf = add(new StudyInfo(new Coord(attrw - UI.scale(150), child.sz.y), child), new Coord(width + margin1 + UI.scale(150), child.c.y).add(wbox.btloff().x, 0));
            add(new CheckBox(Resource.getLocString(Resource.BUNDLE_LABEL, "Lock")) {
                {
                    a = Config.studylock;
                }

                public void set(boolean val) {
                    Utils.setprefb("studylock", val);
                    Config.studylock = val;
                    a = val;
                }
            }, UI.scale(407, 10));
            add(new CheckBox(Resource.getLocString(Resource.BUNDLE_LABEL, "Auto")) {
                {
                    a = Config.autostudy;
                }

                public void set(boolean val) {
                    Utils.setprefb("autostudy", val);
                    Config.autostudy = val;
                    a = val;
                }
            }, UI.scale(465, 10));
            Frame.around(this, Collections.singletonList(inf));
            getparent(GameUI.class).studywnd.setStudy((Inventory) child);
        } else {
            super.addchild(child, args);
        }
    }
}
