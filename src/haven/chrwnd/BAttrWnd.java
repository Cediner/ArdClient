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

import haven.CharWnd;
import haven.Composer;
import haven.Coord;
import haven.Frame;
import haven.GOut;
import haven.Glob;
import haven.Img;
import haven.Loading;
import haven.MessageBuf;
import haven.PUtils;
import haven.ResData;
import haven.Resource;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.UI;
import haven.Utils;
import haven.Widget;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static haven.CharWnd.attrf;
import static haven.CharWnd.attrw;
import static haven.CharWnd.buff;
import static haven.CharWnd.catf;
import static haven.CharWnd.debuff;
import static haven.CharWnd.every;
import static haven.CharWnd.iconfilter;
import static haven.CharWnd.margin1;
import static haven.CharWnd.offy;
import static haven.CharWnd.other;
import static haven.CharWnd.wbox;
import static haven.CharWnd.width;
import static haven.PUtils.convolve;

public class BAttrWnd extends Widget {
    public final List<Attr> base;
    public final FoodMeter feps;
    public final Constipations cons;
    public final GlutMeter glut;

    @RName("battr")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            return (new BAttrWnd(ui.sess.glob));
        }
    }

    public static class Attr extends AttrWdg {
        public final Tex rnm;
        public final Tex img;
        public final Color bg;
        private double lvlt = 0.0;
        private Tex ct;
        private int cbv = -1, ccv = -1;

        private Attr(Glob glob, String attr, Color bg) {
            super(Coord.of(attrw, attrf.height() + UI.scale(2)), glob, attr);
            Resource res = Loading.waitfor(this.attr.res());
            this.rnm = PUtils.strokeTex(attrf.render(res.flayer(Resource.tooltip).t));
            this.img = new TexI(convolve(res.flayer(Resource.imgc).img, new Coord(this.sz.y, this.sz.y), iconfilter));
            this.bg = bg;
        }

        public void tick(double dt) {
            if ((attr.base != cbv) || (attr.comp != ccv)) {
                cbv = attr.base;
                ccv = attr.comp;
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
                ct = PUtils.strokeTex(attrf.render(Integer.toString(ccv), c));
            }
            if ((lvlt > 0.0) && ((lvlt -= dt) < 0))
                lvlt = 0.0;
        }

        public void draw(GOut g) {
            if (lvlt != 0.0)
                g.chcolor(Utils.blendcol(bg, new Color(128, 255, 128, 128), lvlt));
            else
                g.chcolor(bg);
            g.frect(Coord.z, sz);
            g.chcolor();
            Coord cn = new Coord(0, sz.y / 2);
            g.aimage(img, cn.add(UI.scale(5), 0), UI.scale(20, 20), 0, 0.5);
            g.aimage(rnm, cn.add(UI.scale(20 + 10), 1), 0, 0.5);

            cbv = attr.base;
            ccv = attr.comp;
            if (ccv > cbv) {
                Tex buffed = PUtils.strokeTex(attrf.render(Integer.toString(ccv), buff));
                g.aimage(buffed, cn.add(sz.x - UI.scale(7), 1), 1, 0.5);
            } else if (ccv < cbv) {
                Tex debuffed = PUtils.strokeTex(attrf.render(Integer.toString(ccv), debuff));
                g.aimage(debuffed, cn.add(sz.x - UI.scale(7), 1), 1, 0.5);
            }

            Tex base = PUtils.strokeTex(attrf.render(Integer.toString(cbv), Color.WHITE));
            g.aimage(base, cn.add(sz.x - UI.scale(50), 1), 1, 0.5);
        }

        public void lvlup() {
            lvlt = 1.0;
        }
    }

    public BAttrWnd(Glob glob) {
        Widget left = new Widget.Temporary();
        {
            base = new ArrayList<>();
            base.add(new Attr(glob, "str", every));
            base.add(new Attr(glob, "agi", other));
            base.add(new Attr(glob, "int", every));
            base.add(new Attr(glob, "con", other));
            base.add(new Attr(glob, "prc", every));
            base.add(new Attr(glob, "csm", other));
            base.add(new Attr(glob, "dex", every));
            base.add(new Attr(glob, "wil", other));
            base.add(new Attr(glob, "psy", every));
            Composer composer = new Composer(left);
            left.add(CharWnd.settip(new Img(catf.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Base Attributes")).tex()), "gfx/hud/chr/tips/base"));
            composer.add(offy);
            composer.pad(wbox.btloff().add(margin1, 0));
            for (Attr v : base) {
                composer.add(v);
            }
            Frame.around(left, base);
            composer.add(UI.scale(16));
            composer.hpad(0);
            composer.add(CharWnd.settip(new Img(catf.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Food Event Points")).tex()), "gfx/hud/chr/tips/fep"));
            feps = new haven.chrwnd.FoodMeter();
            composer.add(feps);
        }
        left.pack();

        Widget right = new Widget.Temporary();
        {
            Composer composer = new Composer(right);
            right.add(CharWnd.settip(new Img(catf.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Food Satiations")).tex()), "gfx/hud/chr/tips/constip"));
            composer.add(offy);
            cons = new haven.chrwnd.Constipations(attrw, base.size());
            composer.pad(wbox.btloff().add(margin1, 0));
            composer.add(cons);
            Frame.around(right, Collections.singletonList(cons));
            composer.add(UI.scale(16));
            composer.hpad(0);
            composer.add(CharWnd.settip(new Img(catf.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Hunger Level")).tex()), "gfx/hud/chr/tips/hunger"));
            glut = new haven.chrwnd.GlutMeter();
            composer.add(glut);
        }
        right.pack();

        add(left);
        add(right, new Coord(width, 0));
        Widget.Temporary.optimize(this);

        pack();
    }

    public void uimsg(String nm, Object... args) {
        if (nm == "food") {
            feps.update(args);
        } else if (nm == "glut") {
            glut.update(args);
        } else if (nm == "ftrig") {
            feps.trig(ui.sess.getresv(args[0]));
        } else if (nm == "lvl") {
            for (Attr aw : base) {
                if (aw.nm.equals(args[0]))
                    aw.lvlup();
            }
        } else if (nm == "const") {
            int a = 0;
            while (a < args.length) {
                ResData t = new ResData(ui.sess.getresv(args[a++]), MessageBuf.nil);
                if (args[a] instanceof byte[])
                    t.sdt = new MessageBuf((byte[]) args[a++]);
                double m = Utils.dv(args[a++]);
                ui.sess.character.constipation.update(t, m);
                cons.update(t, m);
            }
        } else {
            super.uimsg(nm, args);
        }
    }
}
