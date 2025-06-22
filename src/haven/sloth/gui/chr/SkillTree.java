package haven.sloth.gui.chr;

import haven.Button;
import haven.Coord;
import haven.Coord2d;
import haven.GOut;
import haven.Indir;
import haven.Loading;
import haven.Resource;
import haven.RichText;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.Widget;
import haven.chrwnd.SkillWnd;
import haven.sloth.io.Storage;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill Tree is a 11.5 x 7 grid where half positions are allowed on the horizontal
 * Each row is 130px
 * Each full column is 90px
 */
public class SkillTree extends Widget {
    public static final RichText.Foundry ifnd = new RichText.Foundry(Resource.remote(),
            java.awt.font.TextAttribute.FAMILY, "SansSerif", java.awt.font.TextAttribute.SIZE, 9).aa(true);
    private static final Tex bought = Resource.loadtex("custom/skills/states", 0);
    private static final Tex unlocked = Resource.loadtex("custom/skills/states", 1);
    private static final Tex locked = Resource.loadtex("custom/skills/states", 2);
    private static final List<SkillData> skillData = new ArrayList<>();
    private static final Coord csz = new Coord(90, 130);

    public static void init(final Storage internal) {
        internal.ensure((sql) -> {
            try (final Statement stmt = sql.createStatement()) {
                final Map<Integer, SkillData> id2skill = new HashMap<>();
                try (final ResultSet res = stmt.executeQuery("SELECT id, res, expected_lp, loc_x, loc_y FROM skill_tree")) {
                    while (res.next()) {
                        final SkillData sd = new SkillData(res.getInt(1),
                                res.getString(2), res.getInt(3),
                                new Coord2d(res.getDouble(4), res.getDouble(5)));
                        id2skill.put(sd.id, sd);
                        skillData.add(sd);
                    }
                }
                try (final ResultSet res = stmt.executeQuery("SELECT skill_id, parent_id FROM skill_tree_rel")) {
                    while (res.next()) {
                        id2skill.get(res.getInt(1)).addParent(res.getInt(2));
                    }
                }
            }
        });
    }

    private static class SkillData {
        final int id;
        final String name;
        final Indir<Resource> res;
        final int expected_lp;
        final Coord2d loc;
        final List<Integer> parents = new ArrayList<>();
        final boolean shortcut;

        public SkillData(final int id, final String res, final int expected_lp, final Coord2d loc) {
            this.id = id;
            if (!res.startsWith("custom/skills/shortcut:")) {
                this.name = res;
                this.res = Resource.remote().load(res);
                shortcut = false;
            } else {
                this.name = res.substring(res.indexOf(":") + 1);
                this.res = Resource.remote().load(res.substring(0, res.indexOf(":")));
                shortcut = true;
            }
            this.expected_lp = expected_lp;
            this.loc = loc;
        }

        public void addParent(final int par) {
            parents.add(par);
        }
    }

    private interface SkillWidget {
        Coord c();

        Coord sz();

        SkillWnd.Skill skill();
    }

    private static class Skill extends Widget implements SkillWidget {
        private Button buy;
        final SkillData data;
        SkillWnd.Skill skill;
        private Tex name;
        private Tex cost;
        private Tex tt;

        public Skill(final SkillData data) {
            super(new Coord(110, 100));
            this.data = data;
            cost = Text.renderstroked("" + data.expected_lp).tex();
        }

        private void mkbuy() {
            if (buy == null) {
                buy = new Button(57, "buy", () -> ui.gui.chrwdg.wdgmsg("buy", skill.nm));
                add(buy, new Coord(27, 65));
            }
        }

        public void setSkill(final SkillWnd.Skill skill) {
            this.skill = skill;
            cost = Text.renderstroked("" + skill.cost).tex();
            if (!skill.has)
                mkbuy();
            else if (buy != null) {
                ui.destroy(buy);
                buy = null;
            }
        }

        @Override
        public SkillWnd.Skill skill() {
            return skill;
        }

        @Override
        public Coord c() {
            return c;
        }

        @Override
        public Coord sz() {
            return sz;
        }

        @Override
        public void draw(GOut g) {
            try {
                final Resource res = data.res.get();
                if (name == null) {
                    name = Text.renderstroked(res.layer(Resource.tooltip).t).tex();
                }
                //Draw name
                g.aimage(name, new Coord(sz.x / 2, 0), 0.5f, 0.0f);
                //Icon + Border
                g.aimage(res.layer(Resource.imgc).tex(), sz.add(0, name.sz().y).div(2), 0.5f, 0.5f);
                if (skill != null) {
                    if (skill.has) {
                        g.aimage(bought, sz.add(0, name.sz().y).div(2), 0.5f, 0.5f);
                    } else {
                        g.aimage(unlocked, sz.add(0, name.sz().y).div(2), 0.5f, 0.5f);
                    }
                } else {
                    g.aimage(locked, sz.add(0, name.sz().y).div(2), 0.5f, 0.5f);
                }
                //Cost
                g.aimage(cost, sz.add(0, name.sz().y + bought.sz().y).div(2), 0.5f, 0.5f);
                super.draw(g);
            } catch (Loading l) {
                //Skip until loaded
            }
        }

        private String rendertt() {
            final StringBuilder buf = new StringBuilder();
            final Resource res = data.res.get();
            buf.append("$b{$font[serif,16]{");
            buf.append(res.layer(Resource.tooltip).t);
            buf.append("}}\n\n\n");
            buf.append("Cost: ");
            buf.append((skill != null ? skill.cost : data.expected_lp));
            buf.append("\n\n");
            buf.append(res.layer(Resource.pagina).text);
            return (buf.toString());
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            if (tt != null)
                return tt;
            try {
                tt = ifnd.render(rendertt(), 200).tex();
                return tt;
            } catch (Loading l) {
                //Skip until loaded
                return super.tooltip(c, prev);
            }
        }
    }

    private static class Shortcut extends Widget implements SkillWidget {
        final SkillData data;
        final Skill myself;

        public Shortcut(final SkillData data, final Skill myself) {
            super(new Coord(110, 100));
            this.data = data;
            this.myself = myself;
        }

        @Override
        public void draw(GOut g) {
            try {
                final Color col = myself.skill() == null ? Color.RED : !myself.skill().has ? Color.ORANGE : Color.GREEN;
                g.chcolor(col);
                g.aimage(data.res.get().layer(Resource.imgc).tex(), sz.div(2.0, 1.25), 0.5f, 0.5f);
                g.chcolor();
                if (myself.name != null) {
                    g.aimage(myself.name, sz.div(2.0, 1.25), 0.5f, 0.5f);
                }
            } catch (Loading l) {
                //Skip until loaded
            }
        }

        @Override
        public SkillWnd.Skill skill() {
            return myself.skill;
        }

        @Override
        public Coord sz() {
            return sz;
        }

        @Override
        public Coord c() {
            return c;
        }
    }

    private final Map<Integer, SkillWidget> id2skill = new HashMap<>();
    private final Map<String, Skill> name2skill = new HashMap<>();
    private Tex bg = null;

    public SkillTree() {
        super(csz.mul(11.5f, 7.0f));
        for (final SkillData data : skillData) {
            if (!data.shortcut) {
                final Skill sk = new Skill(data);
                id2skill.put(data.id, sk);
                name2skill.put(data.name.substring(data.name.lastIndexOf('/') + 1), sk);
                add(sk, csz.mul(data.loc).floor());
            } else {
                final Skill sk = name2skill.get(data.name);
                final Shortcut sc = new Shortcut(data, sk);
                id2skill.put(data.id, sc);
                add(sc, csz.mul(data.loc).floor());
            }
        }
        pack();
    }

    private void buildBG() {
        final BufferedImage img = new BufferedImage(sz.x, sz.y, BufferedImage.TYPE_INT_ARGB);
        final Graphics g = img.getGraphics();
        final int mid = 17;
        for (final Skill sk : children(Skill.class)) {
            if (sk.data.parents.size() > 0) {
                final List<Integer> pars = new ArrayList<>(sk.data.parents);
                pars.sort((l, r) -> {
                    final SkillWidget lsk = id2skill.get(l);
                    final SkillWidget rsk = id2skill.get(r);
                    if ((rsk.skill() == null && lsk.skill() == null) ||
                            (rsk.skill() != null && lsk.skill() != null && rsk.skill().has == lsk.skill().has)) {
                        return 0;
                    } else if (lsk.skill() != null && lsk.skill().has) {
                        return -1;
                    } else {
                        return 1;
                    }
                });

                for (final int pid : pars) {
                    final SkillWidget parent = id2skill.get(pid);
                    final Color col = parent.skill() == null ? Color.RED : !parent.skill().has ? Color.ORANGE : Color.GREEN;
                    g.setColor(col);
                    //Draw line up from our skill
                    g.drawLine(sk.c.x + sk.sz.x / 2, sk.c.y, sk.c.x + sk.sz.x / 2 - 3, sk.c.y - 5);
                    g.drawLine(sk.c.x + sk.sz.x / 2, sk.c.y, sk.c.x + sk.sz.x / 2 + 3, sk.c.y - 5);
                    g.drawLine(sk.c.x + sk.sz.x / 2, sk.c.y, sk.c.x + sk.sz.x / 2, sk.c.y - mid);
                    //Draw line over to the right column center
                    g.drawLine(sk.c.x + sk.sz.x / 2, sk.c.y - mid, parent.c().x + parent.sz().x / 2, sk.c.y - mid);
                    //Draw up to parent
                    g.drawLine(parent.c().x + parent.sz().x / 2, sk.c.y - mid,
                            parent.c().x + parent.sz().x / 2, parent.c().y + parent.sz().y);
                }
            }
        }
        g.dispose();
        bg = new TexI(img);
    }

    public void update(List<SkillWnd.Skill> sks) {
        sks.forEach((skill) -> {
            if (name2skill.containsKey(skill.nm)) {
                name2skill.get(skill.nm).setSkill(skill);
            }
        });
        buildBG();
    }

    @Override
    public void draw(GOut g) {
        if (bg != null)
            g.image(bg, Coord.z);
        super.draw(g);
    }
}
