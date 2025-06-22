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
 * Credo Tree is a
 */
public class CredoTree extends Widget {
    public static final RichText.Foundry ifnd = new RichText.Foundry(Resource.remote(),
            java.awt.font.TextAttribute.FAMILY, "SansSerif", java.awt.font.TextAttribute.SIZE, 9).aa(true);
    private static final Tex bought = Resource.loadtex("custom/credos/states", 0);
    private static final Tex unlocked = Resource.loadtex("custom/credos/states", 1);
    private static final Tex locked = Resource.loadtex("custom/credos/states", 2);
    private static final List<CredoData> credoData = new ArrayList<>();
    private static final Coord csz = new Coord(90, 130);

    public static void init(final Storage internal) {
        internal.ensure((sql) -> {
            try (final Statement stmt = sql.createStatement()) {
                final Map<Integer, CredoData> id2credo = new HashMap<>();
                try (final ResultSet res = stmt.executeQuery("SELECT id, res, loc_x, loc_y FROM credo_tree")) {
                    while (res.next()) {
                        final CredoData sd = new CredoData(res.getInt(1),
                                res.getString(2),
                                new Coord2d(res.getDouble(3), res.getDouble(4)));
                        id2credo.put(sd.id, sd);
                        credoData.add(sd);
                    }
                }
                try (final ResultSet res = stmt.executeQuery("SELECT credo_id, parent_id FROM credo_tree_rel")) {
                    while (res.next()) {
                        id2credo.get(res.getInt(1)).addParent(res.getInt(2));
                    }
                }
            }
        });
    }

    private static class CredoData {
        final int id;
        final String name;
        final Indir<Resource> res;
        final Coord2d loc;
        final List<Integer> parents = new ArrayList<>();

        public CredoData(final int id, final String res, final Coord2d loc) {
            this.id = id;
            this.name = res;
            this.res = Resource.remote().load(res);
            this.loc = loc;
        }

        public void addParent(final int par) {
            parents.add(par);
        }
    }


    private interface CredoWidget {
        Coord c();

        Coord sz();

        SkillWnd.Credo credo();
    }

    private static class Credo extends Widget implements CredoWidget {
        private Button buy;
        final CredoData data;
        SkillWnd.Credo credo;
        private Tex name;
        private Tex tt;
        private Tex lvl;
        private Tex quests;

        public Credo(final CredoData data) {
            super(new Coord(110, 100));
            this.data = data;
        }

        private void mkbuy() {
            if (buy == null) {
                buy = new Button(58, "pursue", () -> ui.gui.chrwdg.wdgmsg("crpursue", credo.nm));
                add(buy, new Coord(26, 74));
            }
        }

        public void setSkill(final SkillWnd.Credo credo) {
            this.credo = credo;
            if (credo.on) {
                lvl = Text.renderstroked(String.format("L %d/%d", credo.crl, credo.crlt)).tex();
                quests = Text.renderstroked(String.format("Q %d/%d", credo.crql, credo.crqlt)).tex();
            } else {
                if (lvl != null) {
                    lvl.dispose();
                    quests.dispose();
                }
                lvl = null;
                quests = null;
            }

            if (!credo.has && !credo.on)
                mkbuy();
            else if (buy != null) {
                ui.destroy(buy);
                buy = null;
            }
        }

        @Override
        public SkillWnd.Credo credo() {
            return credo;
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
                final Tex tex = res.layer(Resource.imgc).tex();
                g.image(tex, sz.add(0, name.sz().y).div(2).sub(tex.sz().div(4)), tex.sz().div(2));
                if (credo != null) {
                    if (credo.has) {
                        g.image(bought, sz.add(0, name.sz().y).div(2).sub(tex.sz().div(4)));
                    } else {
                        g.image(unlocked, sz.add(0, name.sz().y).div(2).sub(tex.sz().div(4)));
                    }
                } else {
                    g.image(locked, sz.add(0, name.sz().y).div(2).sub(tex.sz().div(4)));
                }
                //If  has levels/quests draw them
                if (lvl != null) {
                    g.chcolor(new Color(192, 192, 192, 128));
                    g.frect(sz.add(0, name.sz().y).div(2), tex.sz().div(4));
                    g.chcolor();
                    g.aimage(lvl, sz.add(0, name.sz().y).div(2), 0.0f, -0.5f);
                    g.aimage(quests, sz.add(0, name.sz().y).div(2), 0.0f, -1.5f);
                }
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
            buf.append(res.layer(Resource.pagina).text);
            return (buf.toString());
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            if (tt != null)
                return tt;
            try {
                tt = ifnd.render(rendertt(), 300).tex();
                return tt;
            } catch (Loading l) {
                //Skip until loaded
                return super.tooltip(c, prev);
            }
        }
    }

    private final Map<Integer, CredoWidget> id2credo = new HashMap<>();
    private final Map<String, Credo> name2credo = new HashMap<>();
    private Tex bg = null;

    public CredoTree() {
        super(csz.mul(11.5f, 7.0f));
        for (final CredoData data : credoData) {
            final Credo sk = new Credo(data);
            id2credo.put(data.id, sk);
            name2credo.put(data.name.substring(data.name.lastIndexOf('/') + 1), sk);
            add(sk, csz.mul(data.loc).floor());
        }
        pack();
    }

    private void buildBG() {
        final BufferedImage img = new BufferedImage(sz.x, sz.y, BufferedImage.TYPE_INT_ARGB);
        final Graphics g = img.getGraphics();
        final int mid = 17;
        for (final Credo sk : children(Credo.class)) {
            if (sk.data.parents.size() > 0) {
                final List<Integer> pars = new ArrayList<>(sk.data.parents);
                pars.sort((l, r) -> {
                    final CredoWidget lsk = id2credo.get(l);
                    final CredoWidget rsk = id2credo.get(r);
                    if ((rsk.credo() == null && lsk.credo() == null) ||
                            (rsk.credo() != null && lsk.credo() != null && rsk.credo().has == lsk.credo().has)) {
                        return 0;
                    } else if (lsk.credo() != null && lsk.credo().has) {
                        return -1;
                    } else {
                        return 1;
                    }
                });

                int sub = 0;
                for (final int pid : pars) {
                    final CredoWidget parent = id2credo.get(pid);
                    final Color col = parent.credo() == null ? Color.RED : parent.credo().on ? Color.ORANGE : parent.credo().has ? Color.GREEN : Color.RED;
                    g.setColor(col);
                    //Draw line up from our skill
                    g.drawLine(sk.c.x + sk.sz.x / 2 + sub, sk.c.y, sk.c.x + sk.sz.x / 2 + sub - 3, sk.c.y - 5);
                    g.drawLine(sk.c.x + sk.sz.x / 2 + sub, sk.c.y, sk.c.x + sk.sz.x / 2 + sub + 3, sk.c.y - 5);
                    g.drawLine(sk.c.x + sk.sz.x / 2 + sub, sk.c.y, sk.c.x + sk.sz.x / 2 + sub, sk.c.y - (mid - sub));
                    //Draw line over to the right column center
                    g.drawLine(sk.c.x + sk.sz.x / 2 + sub, sk.c.y - (mid - sub), parent.c().x + parent.sz().x / 2 + sub, sk.c.y - (mid - sub));
                    //Draw up to parent
                    g.drawLine(parent.c().x + parent.sz().x / 2 + sub, sk.c.y - (mid - sub),
                            parent.c().x + parent.sz().x / 2 + sub, parent.c().y + parent.sz().y);
                    sub -= 3;
                }
            }
        }
        g.dispose();
        bg = new TexI(img);
    }

    public void update(List<SkillWnd.Credo> sks) {
        sks.forEach((skill) -> {
            if (name2credo.containsKey(skill.nm)) {
                name2credo.get(skill.nm).setSkill(skill);
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
