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

import haven.PUtils.BlurFurn;
import haven.PUtils.TexFurn;
import haven.chrwnd.BAttrWnd;
import haven.chrwnd.FoodMeter;
import haven.chrwnd.QuestWnd;
import haven.chrwnd.SAttrWnd;
import haven.chrwnd.SkillWnd;
import haven.chrwnd.TabProxy;

import java.awt.Color;
import java.awt.font.TextAttribute;
import java.util.stream.IntStream;

/* XXX: There starts to seem to be reason to split the while character
 * sheet into some more modular structure, as it is growing quite
 * large. */
public class CharWnd extends Window {
    public static final RichText.Foundry ifnd = new RichText.Foundry(Resource.remote(), TextAttribute.FAMILY, Text.cfg.font.get("sans"), TextAttribute.SIZE, Text.cfg.charWndBox).aa(true);
    public static final Text.Furnace catf = new BlurFurn(new TexFurn(new Text.Foundry(Text.sans, UI.scale(20)).aa(true), Window.ctex), 2, 2, new Color(96, 48, 0));
    public static final Text.Furnace failf = new BlurFurn(new TexFurn(new Text.Foundry(Text.sans, UI.scale(25)).aa(true), Resource.loadimg("gfx/hud/fontred")), 3, 2, new Color(96, 48, 0));
    public static final Text.Foundry attrf = Text.attrf;
    public static final Text.Foundry numfnd = new Text.Foundry(Text.sans, UI.scale(12));
    public static final Color debuff = new Color(255, 128, 128);
    public static final Color buff = new Color(128, 255, 128);
    public static final Color tbuff = new Color(128, 128, 255);
    public static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32);
    public static final int width = UI.scale(255);
    public static final int height = UI.scale(260);
    public static final int margin1 = UI.scale(5);
    public static final int margin2 = 2 * margin1;
    public static final int margin3 = 2 * margin2;
    public static final int fontsize = UI.scale(16);
    public static final int offy = UI.scale(35);
    public BAttrWnd battr;
    public SAttrWnd sattr;
    public SkillWnd skill;
    public FightWnd fight;
    public WoundWnd wound;
    public QuestWnd quest;
    public final Tabs.Tab battrtab, sattrtab, skilltab, fighttab, woundtab, questtab;
    public int exp, enc;
    private int scost;
    public int level;

    public static final int attrw = FoodMeter.frame.sz().x - wbox.bisz().x;

    public static final PUtils.Convolution iconfilter = new PUtils.Lanczos(3);

    @RName("chr")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            return (new CharWnd(ui.sess.glob));
        }
    }

    public static <T extends Widget> T settip(T wdg, String resnm) {
        wdg.tooltip = new Widget.PaginaTip(new Resource.Spec(Resource.remote(), resnm));
        return (wdg);
    }

    public CharWnd(Glob glob) {
        super(UI.scale(new Coord(300, 290)), "Character Sheet", "Character Sheet");

        final Tabs tabs = new Tabs(new Coord(15, 10), UI.scale(506, 315), this);
        battrtab = tabs.add();
        sattrtab = tabs.add();
        skilltab = tabs.add();
        fighttab = tabs.add();
        woundtab = tabs.add();
        questtab = tabs.add();

        {
            Widget prev;

            class TB extends IButton {
                final Tabs.Tab tab;

                TB(String nm, Tabs.Tab tab, String tip) {
                    super("gfx/hud/chr/" + nm, "u", "d", null);
                    this.tab = tab;
                    settip(tip);
                }

                public void click() {
                    tabs.showtab(tab);
                }

                protected void depress() {
                    Audio.play(Button.lbtdown.stream());
                }

                protected void unpress() {
                    Audio.play(Button.lbtup.stream());
                }
            }

            Composer composer = new Composer(this)
                    .hpad(tabs.c.x)
                    .vpad(tabs.c.y + tabs.sz.y + margin2);
            composer.addar(
                    tabs.sz.x,
                    new TB("battr", battrtab, Resource.getLocString(Resource.BUNDLE_TOOLTIP, "Base Attributes")),
                    new TB("sattr", sattrtab, Resource.getLocString(Resource.BUNDLE_TOOLTIP, "Abilities")),
                    new TB("skill", skilltab, Resource.getLocString(Resource.BUNDLE_TOOLTIP, "Lore & Skills")),
                    new TB("fgt", fighttab, Resource.getLocString(Resource.BUNDLE_TOOLTIP, "Martial Arts & Combat Schools")),
                    new TB("wound", woundtab, Resource.getLocString(Resource.BUNDLE_TOOLTIP, "Health & Wounds")),
                    new TB("quest", questtab, Resource.getLocString(Resource.BUNDLE_TOOLTIP, "Quest Log"))
            );
        }

        resize(contentsz().add(UI.scale(15, 10)));
    }

    public Glob.CAttr findattr(String name) {
        for (SAttrWnd.SAttr skill : sattr.skill) {
            if (name.equals(skill.attr.nm)) {
                return skill.attr;
            }
        }
        for (BAttrWnd.Attr stat : battr.base) {
            if (name.equals(stat.attr.nm)) {
                return stat.attr;
            }
        }
        return null;
    }

    public Glob.CAttr findattr(Resource res) {
        for (SAttrWnd.SAttr skill : sattr.skill) {
            if (res.name.equals(skill.attr.res().get().name)) {
                return skill.attr;
            }
        }
        for (BAttrWnd.Attr stat : battr.base) {
            if (res.name.equals(stat.attr.res().get().name)) {
                return stat.attr;
            }
        }
        return null;
    }

    public int statIndex(Resource res) {
        if (battr.base != null) {
            return IntStream.range(0, battr.base.size())
                    .filter(i -> battr.base.stream().equals(res))
                    .findFirst().orElse(Integer.MAX_VALUE);
        }
        return Integer.MAX_VALUE;
    }

    public int skillIndex(Resource res) {
        if (sattr.skill != null) {
            return IntStream.range(0, sattr.skill.size())
                    .filter(i -> sattr.skill.stream().equals(res))
                    .findFirst().orElse(Integer.MAX_VALUE);
        }
        return Integer.MAX_VALUE;
    }

    public int BY_PRIORITY(Resource r1, Resource r2) {
        int b1 = statIndex(r1);
        int b2 = statIndex(r2);

        if (b1 == b2) {
            b1 = skillIndex(r1);
            b2 = skillIndex(r2);
            if (b1 == b2) {
                return r1.name.compareTo(r2.name);
            } else {
                return Integer.compare(b1, b2);
            }
        } else {
            return Integer.compare(b1, b2);
        }
    }

    public void addchild(Widget child, Object... args) {
        String place = (args[0] instanceof String) ? (((String) args[0]).intern()) : null;
        if ((place == "tab") || /* XXX: Remove me! */ Utils.eq(args[0], Coord.of(47, 47))) {
            if (child instanceof BAttrWnd) {
                battr = battrtab.add((BAttrWnd) child, Coord.z);
                GameUI gui = getparent(GameUI.class);
                if (gui != null) {
                    gui.addcmeter(gui.hungermeter = new HungerMeter(battr.glut, "HungerMeter"));
                    gui.hungermeter.show(Config.hungermeter);
                    gui.addcmeter(gui.fepmeter = new FepMeter(battr.feps, "FepMeter"));
                    gui.fepmeter.show(Config.fepmeter);
                }
            } else if (child instanceof SAttrWnd) {
                sattr = sattrtab.add((SAttrWnd) child, Coord.z);
                GameUI gui = getparent(GameUI.class);
                if (gui != null) {
                    gui.studywnd = gui.add(new StudyWnd(), UI.scale(400, 100));
                    if (!Config.autowindows.get("Study").selected)
                        gui.studywnd.hide();
                }
            } else if (child instanceof SkillWnd) {
                skill = skilltab.add((SkillWnd) child, Coord.z);
            } else if (child instanceof FightWnd) {
                fight = fighttab.add((FightWnd) child, Coord.z);
            } else if (child instanceof WoundWnd) {
                wound = woundtab.add((WoundWnd) child, Coord.z);
            } else if (child instanceof QuestWnd) {
                quest = questtab.add((QuestWnd) child, Coord.z);
            } else if (child instanceof TabProxy) {
                add(child);
            } else {
                throw (new RuntimeException("unknown tab widget: " + child));
            }
            //resize(contentsz().add(UI.scale(15, 10)));
        } else if (place == "fmg") {
            /* XXX: Remove me! */
            fight = fighttab.add((FightWnd) child, 0, 0);
        } else {
            super.addchild(child, args);
        }
    }

    public void uimsg(String nm, Object... args) {
        if (nm == "attr") {
            int a = 0;
            while (a < args.length) {
                String attr = (String) args[a++];
                int base = Utils.iv(args[a++]);
                int comp = Utils.iv(args[a++]);
                ItemInfo.Raw info = ItemInfo.Raw.nil;
                if ((a < args.length) && (args[a] instanceof Object[]))
                    info = new ItemInfo.Raw((Object[]) args[a++]);
                ui.sess.glob.cattr(attr, base, comp, info);
            }
        } else if (nm == "exp") {
            exp = Utils.iv(args[0]);
        } else if (nm == "enc") {
            enc = Utils.iv(args[0]);
        } else {
            super.uimsg(nm, args);
        }
    }

    public static class LoadingTextBox extends RichTextBox {
        private Indir<String> text = null;

        public LoadingTextBox(Coord sz, String text, RichText.Foundry fnd) {
            super(sz, text, fnd);
        }

        public LoadingTextBox(Coord sz, String text, Object... attrs) {
            super(sz, text, attrs);
        }

        public void settext(Indir<String> text) {
            this.text = text;
        }

        public void draw(GOut g) {
            if (text != null) {
                try {
                    settext(text.get());
                    text = null;
                } catch (Loading l) {
                }
            }
            super.draw(g);
        }
    }
}
