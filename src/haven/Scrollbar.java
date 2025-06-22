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

public class Scrollbar extends Widget {
    static final Tex schain = Resource.loadtex("gfx/hud/schain"); //Just incase for hidden dependencies...
    private static final Tex schainb = Theme.tex("scroll/vertical", 0);
    private static final Tex schainm = Theme.tex("scroll/vertical", 1);
    private static final Tex schaint = Theme.tex("scroll/vertical", 2);
    public static final Tex sflarp = Theme.tex("scroll/vertical", 3);
    public static final int width = sflarp.sz().x;
    public Scrollable ctl;
    public int val, min, max;
    private UI.Grab drag = null;

    public Scrollbar(int h, Scrollable ctl) {
        this(h, 0, 0);
        this.ctl = ctl;
        this.min = ctl.scrollmin();
        this.max = ctl.scrollmax();
        this.val = ctl.scrollval();
    }

    public Scrollbar(int h, int min, int max) {
        super(new Coord(sflarp.sz().x, h));
        this.min = min;
        this.max = max;
        val = min;
        this.ctl = null;
    }

    public boolean vis() {
        return (max > min);
    }

    public void draw(GOut g) {
        if (ctl != null) {
            min = ctl.scrollmin();
            max = ctl.scrollmax();
            val = ctl.scrollval();
        }
        if (vis()) {
            g.chcolor(DefSettings.SLIDERCOL.get());
            //x offset incase sflarp.sz.x > schain.sz.x
            int cx = (sflarp.sz().x / 2) - (schaint.sz().x / 2);
            //Top
            g.image(schaint, new Coord(cx, 0));
            //middle
            for (int y = schainb.sz().y; y < sz.y - schaint.sz().y; y += schainm.sz().y)
                g.image(schainm, new Coord(cx, y));
            //bottom
            g.image(schainb, new Coord(cx, sz.y - schainb.sz().y));
            //slider
            double a = (double) val / (double) (max - min);
            int fy = (int) ((sz.y - sflarp.sz().y) * a);
            g.image(sflarp, new Coord(0, fy));
            g.chcolor();
        }
    }

    public boolean mousedown(Coord c, int button) {
        if (button != 1)
            return (false);
        if (!vis())
            return (false);
        drag = ui.grabmouse(this);
        mousemove(c);
        return (true);
    }

    public void mousemove(Coord c) {
        if (drag != null) {
            double a = (double) (c.y - (sflarp.sz().y / 2)) / (double) (sz.y - sflarp.sz().y);
            if (a < 0)
                a = 0;
            if (a > 1)
                a = 1;
            val = (int) Math.round(a * (max - min)) + min;
            changed();
        }
    }

    public boolean mouseup(Coord c, int button) {
        if (button != 1)
            return (false);
        if (drag == null)
            return (false);
        drag.remove();
        drag = null;
        return (true);
    }

    public void changed() {
    }

    public void ch(int a) {
        int val = this.val + a;
        if (val > max)
            val = max;
        if (val < min)
            val = min;
        if (this.val != val) {
            this.val = val;
            changed();
        }
    }

    public void reset() {
        ch(min - val);
    }

    public void resize(int h) {
        super.resize(new Coord(sflarp.sz().x, h));
    }

    public void move(Coord c) {
        this.c = c.add(-sflarp.sz().x, 0);
    }
}
