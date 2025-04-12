package haven;

import haven.purus.pbot.PBotUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GobSpawner extends Window {
    private List<String> shortvers = new ArrayList<>();

    public GobSpawner() {
        super(Coord.z, "Gob Spawner", "Gob Spawner");
        shortvers.addAll(defaultitems.values());
        shortvers.sort(String::compareTo);
        Coord c = new Coord(0, 0);
        c.y += add(new Listbox<String>(200, 20, 20) {
            @Override
            protected String listitem(int i) {
                return shortvers.get(i);
            }

            @Override
            protected int listitems() {
                return shortvers.size();
            }

            @Override
            public void change(String item) {
                if (sel != null && sel.equals(item)) {
                    defaultitems.forEach((k, v) -> {
                        if (v.equals(item)) {
                            try {
                                spawnGob(k);
                            } catch (Throwable e) {
                                PBotUtils.sysMsg(ui, "Exception in gob spawner.", Color.white);
                                e.printStackTrace();
                            }
                        }
                    });
                }
                super.change(item);
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, new Coord(5, 1));
            }
        }, c.copy()).sz.y + 5;
        CheckBox r = new CheckBox("");
        add(r, c.copy());
        c.y += add(new TextEntry(200, "") {
            @Override
            public void activate(String text) {
                try {
                    if (r.a)
                        drawable(text);
                    else
                        spawnGob(text);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, c.copy().add(r.c.x + r.sz.x, 0)).sz.y + 5;
        pack();
    }

    int next = -900000;

    public void spawnGob(final String res) {
        System.out.println("Spawn gob : " + res);
        if (!res.startsWith("gfx")) {
            System.out.println("has pose of : " + posemap.get(res));
            String type = res.substring(0, res.indexOf(" "));
            defaultitems.forEach((k, v) -> {
                if (v.equals(type)) {
                    final Gob g = ui.sess.glob.oc.createTempGob(next--);
                    ui.sess.glob.oc.move(g, PBotUtils.player(ui).rc, Math.toRadians(130));
                    ui.sess.glob.oc.composite(g, Resource.remote().load(k));
                    final List<ResData> data = new ArrayList<>();
                    data.add(new ResData(Resource.remote().load(k), Message.nil));
                    final List<Composited.MD> mds = new ArrayList<>();
                    mds.add(new Composited.MD(Resource.remote().load(k), data));
                    ui.sess.glob.oc.cmpmod(g, mds);
                    final List<ResData> pose = new ArrayList<>();
                    pose.add(new ResData(Resource.remote().load(posemap.get(res)), Message.nil));
                    ui.sess.glob.oc.cmppose(g, 11, pose, null, false, 0);
                }
            });
        } else {
            final Gob g = ui.sess.glob.oc.createTempGob(next--);
            ui.sess.glob.oc.move(g, PBotUtils.player(ui).rc, Math.toRadians(130));
            ui.sess.glob.oc.composite(g, Resource.remote().load(res));
            final List<ResData> data = new ArrayList<>();
            data.add(new ResData(Resource.remote().load(res), Message.nil));
            final List<Composited.MD> mds = new ArrayList<>();
            mds.add(new Composited.MD(Resource.remote().load(res), data));
            ui.sess.glob.oc.cmpmod(g, mds);
            final List<ResData> pose = new ArrayList<>();
            ui.sess.glob.oc.cmppose(g, 11, pose, null, false, 0);
        }
    }

    private void drawable(final String res) {
        final Gob g = ui.sess.glob.oc.createTempGob(next--);
        ui.sess.glob.oc.move(g, PBotUtils.player(ui).rc, Math.toRadians(130));
        ui.sess.glob.oc.cres(g, Resource.remote().load(res), Message.nil);
    }

    @Override
    public void close() {
        hide();
    }

    public final static HashMap<String, String> posemap = new HashMap<String, String>(14) {{
        put("Whale Knocked", "gfx/kritter/spermwhale/knock");
        put("Whale Dead", "gfx/kritter/spermwhale/waterdead");
        put("Whale Swimming", "gfx/kritter/spermwhale/swimming");

        put("Bear Knocked", "gfx/kritter/bear/knock");
        put("Bear Fight", "gfx/kritter/bear/fgtidle");
        put("Bear Idle", "gfx/kritter/bear/idle");

        put("Troll Knocked", "gfx/kritter/troll/knock");
        put("Troll Fight", "gfx/kritter/troll/fgtidle");
        put("Troll Idle", "gfx/kritter/troll/idle");

        put("Nidbane Fight", "gfx/kritter/nidbane/atk");
        put("Nidbane Idle", "gfx/kritter/nidbane/idle");

        put("Mammoth Knocked", "gfx/kritter/mammoth/knock");
        put("Mammoth Fight", "gfx/kritter/mammoth/fgtidle");
        put("Mammoth Idle", "gfx/kritter/mammoth/idle");
    }};

    public final static HashMap<String, String> defaultitems = new HashMap<String, String>(44) {{
        put("gfx/kritter/chicken/hen", "Chicken Hen");
        put("gfx/kritter/chicken/rooster", "Chicken Rooster");
        put("gfx/kritter/chicken/chick", "Chicken Chick");
        put("gfx/kritter/toad/toad", "Toad");
        put("gfx/kritter/frog/frog", "Frog");
        put("gfx/kritter/mallard/mallard", "Duck");
        put("gfx/kritter/hedgehog/hedgehog", "Hedgehog");
        put("gfx/kritter/squirrel/squirrel", "Squirrel");
        put("gfx/kritter/rabbit/rabbit", "Rabbit");
        put("gfx/kritter/crab/crab", "Crab");
        put("gfx/kritter/irrbloss/irrbloss", "Irrlight");
        put("gfx/kritter/opiumdragon/opiumdragon", "Opium Dragon");
        put("gfx/kritter/forestsnail/forestsnail", "Forest Snail");
        put("gfx/kritter/forestlizard/forestlizard", "Forest Lizard");
        put("gfx/kritter/bear/bear", "Bear");
        put("gfx/kritter/adder/adder", "Snake");
        put("gfx/kritter/lynx/lynx", "Lynx");
        put("gfx/kritter/walrus/walrus", "Walrus");
        put("gfx/kritter/greyseal/greyseal", "Seal");
        put("gfx/kritter/troll/troll", "Troll");
        put("gfx/kritter/mammoth/mammoth", "Mammoth");
        put("gfx/kritter/goldeneagle/goldeneagle", "Eagle");
        put("gfx/kritter/nidbane/nidbane", "Nidbane");
        put("gfx/kritter/horse/horse", "Wild Horse");
        put("gfx/kritter/moose/moose", "Moose");
        put("gfx/kritter/wolverine/wolverine", "Wolverine");
        put("gfx/kritter/badger/badger", "Badger");
        put("gfx/kritter/fox/fox", "Fox");
        put("gfx/kritter/wolf/wolf", "Wolves");
        put("gfx/kritter/mole/mole", "Moles");
        put("gfx/kritter/spermwhale/spermwhale", "Whale");
        put("gfx/kritter/orca/orca", "Orca");
        put("Whale Knocked", "Whale Knocked");
        put("Whale Dead", "Whale Dead");
        put("Whale Swimming", "Whale Swimming");
        put("Bear Knocked", "Bear Knocked");
        put("Bear Fight", "Bear Fight");
        put("Bear Idle", "Bear Idle");
        put("Troll Knocked", "Troll Knocked");
        put("Troll Fight", "Troll Fight");
        put("Troll Idle", "Troll Idle");
        put("Nidbane Fight", "Nidbane Fight");
        put("Nidbane Idle", "Nidbane Idle");
        put("Mammoth Knocked", "Mammoth Knocked");
        put("Mammoth Fight", "Mammoth Fight");
        put("Mammoth Idle", "Mammoth Idle");
    }};
}
