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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import haven.automation.Discord;
import haven.automation.ErrorSysMsgCallback;
import haven.automation.PickForageable;
import haven.automation.Traverse;
import haven.automation.TunnelerBot;
import haven.overlays.OverlayManager;
import haven.purus.DrinkWater;
import haven.purus.ItemClickCallback;
import haven.purus.PetalClickCallback;
import haven.purus.pbot.PBotScriptlist;
import haven.purus.pbot.PBotScriptlistOld;
import haven.purus.pbot.PBotUtils;
import haven.purus.pbot.PBotWindowAPI;
import haven.res.gfx.fx.msrad.MSRad;
import haven.resutil.BPRadSprite;
import haven.resutil.FoodInfo;
import haven.sloth.gob.AggroMark;
import haven.sloth.gob.Mark;
import haven.sloth.gui.DeletedManager;
import haven.sloth.gui.DowseWnd;
import haven.sloth.gui.ForageHelperWnd;
import haven.sloth.gui.ForageWizardWnd;
import haven.sloth.gui.HiddenManager;
import haven.sloth.gui.HighlightManager;
import haven.sloth.gui.SoundManager;
import haven.sloth.gui.Timer.TimersWnd;
import haven.sloth.gui.chr.SkillnCredoWnd;
import haven.sloth.gui.livestock.LivestockManager;
import haven.sloth.gui.script.ScriptManager;
import integrations.mapv4.MappingClient;
import modification.configuration;
import modification.dev;
import modification.newQuickSlotsWdg;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static haven.Action.TOGGLE_CHARACTER;
import static haven.Action.TOGGLE_EQUIPMENT;
import static haven.Action.TOGGLE_INVENTORY;
import static haven.Action.TOGGLE_KIN_LIST;
import static haven.Action.TOGGLE_OPTIONS;
import static haven.Action.TOGGLE_SEARCH;
import static haven.DefSettings.ANIMALDANGERCOLOR;
import static haven.DefSettings.BARTERCOLOR;
import static haven.DefSettings.BEEHIVECOLOR;
import static haven.DefSettings.MOUNDBEDCOLOR;
import static haven.DefSettings.SUPPORTDANGERCOLOR;
import static haven.DefSettings.TROUGHCOLOR;
import static haven.KeyBinder.KeyBind;

public class GameUI extends ConsoleHost implements Console.Directory, UI.MessageWidget {
    public static final Text.Foundry msgfoundry = new Text.Foundry(Text.dfont, Text.cfg.msg);
    private static final int blpw = 142;
    public final String chrid, genus;
    public final long plid;
    public final Hidepanel ulpanel, umpanel, urpanel, brpanel, menupanel;
    public Widget portrait;
    public Partyview partyview;
    public MenuGrid menu;
    public MenuSearch menuSearch;
    public PBotScriptlist PBotScriptlist;
    public PBotScriptlistOld PBotScriptlistold;
    public MapView map;
    public GobIcon.Settings iconconf;
    public Fightview fv;
    public Fightsess fs;
    private List<Widget> meters = new LinkedList<>();
    private List<Widget> cmeters = new LinkedList<>();
    private Text lastmsg;
    private double msgtime;
    public Window invwnd, equwnd;
    public Window iconwnd;
    public FilterWnd filter;
    public Inventory maininv;
    private Boolean temporarilyswimming = false;
    public CharWnd chrwdg;
    public Speedget speed;
    private ScriptManager scripts;
    public MapWnd mapfile;
    public Widget qqview;
    public QuestWnd questwnd;
    public boolean discordconnected = false;
    public BuddyWnd buddies;
    public Equipory equipory;
    private Zergwnd zerg;
    public final Collection<Polity> polities = new ArrayList<>();
    public HelpWnd help;
    public OptWnd opts;
    public Collection<DraggedItem> hand = new LinkedList<>();
    private Collection<DraggedItem> handSave = new LinkedList<>();
    private long DrinkTimer = 0, StarvationAlertDelay = 0, SwimTimer;
    public WItem vhand;
    public ChatUI chat;
    public ChatWnd chatwnd;
    //    private int saferadius = 1;
    private int dangerradius = 1;
    public ChatUI.Channel syslog;
    public ChatUI.Channel debuglog;
    public Window hidden, deleted, alerted, highlighted, overlayed, gobspawner;
    public double prog = -1;
    private boolean afk = false;
    @SuppressWarnings("unchecked")
    public BeltSlot[] belt = new BeltSlot[144];
    public final Map<Integer, String> polowners = new HashMap<Integer, String>();
    public Bufflist buffs;
    public LocalMiniMap mmap;
    private MinimapWnd mmapwnd;
    //    public haven.timers.TimersWnd timerswnd;
    public QuickSlotsWdg quickslots;
    public newQuickSlotsWdg newquickslots;
    public StatusWdg statuswindow;
    public boolean swimon = false;
    public boolean crimeon = false;
    public boolean trackon = false;
    public boolean partyperm = false;
    public boolean crimeautotgld = false;
    public boolean swimautotgld = false;
    public boolean trackautotgld = false;
    public CraftHistoryBelt histbelt;
    private ErrorSysMsgCallback errmsgcb;
    public StudyWnd studywnd;
    private haven.livestock.LivestockManager livestockwnd;
    public ItemClickCallback itemClickCallback;
    public PetalClickCallback petalClickCallback;
    public boolean drinkingWater, lastDrinkingSucessful;
    public CraftWindow makewnd;
    public BeltWnd fbelt, nbelt, npbelt;
    public FepMeter fepmeter;
    public HungerMeter hungermeter;
    public MapPointer pointer;
    public Cal cal;
    public QuestHelper questhelper;
    public Thread DrinkThread;
    public CraftDBWnd craftwnd = null;
    private TimersWnd timers;

    public TunnelerBot tunnelerBot;
    public Thread tunnelerBotThread;

    public final List<DowseWnd> dowsewnds = Collections.synchronizedList(new ArrayList<>());
    public ForageHelperWnd foragehelper;
    public ForageWizardWnd forageWzrWnd;
    public SkillnCredoWnd scwnd;
    public LivestockManager lm;
    //public long inspectedgobid = 0;//used for attaching inspected qualities to gobs.


    public static abstract class BeltSlot {
        public final int idx;

        public BeltSlot(int idx) {
            this.idx = idx;
        }

        public abstract void draw(GOut g);

        public abstract void use(MenuGrid.Interaction iact);

        public Object rendertt(boolean a) {
            return (null);
        }
    }

    private static final OwnerContext.ClassResolver<ResBeltSlot> beltctxr = new OwnerContext.ClassResolver<ResBeltSlot>()
            .add(GameUI.class, slot -> slot.wdg())
            .add(Glob.class, slot -> slot.wdg().ui.sess.glob)
            .add(Session.class, slot -> slot.wdg().ui.sess);

    public class ResBeltSlot extends BeltSlot implements GSprite.Owner {
        public final ResData rdt;

        public ResBeltSlot(int idx, ResData rdt) {
            super(idx);
            this.rdt = rdt;
        }

        private GSprite spr = null;

        public GSprite spr() {
            GSprite ret = this.spr;
            if (ret == null)
                ret = this.spr = GSprite.create(this, rdt.res.get(), new MessageBuf(rdt.sdt));
            return (ret);
        }

        public void draw(GOut g) {
            try {
                spr().draw(g);
            } catch (Loading l) {}
        }

        public void use(MenuGrid.Interaction iact) {
            Object[] args = {idx, iact.btn, iact.modflags};
            if (iact.mc != null) {
                args = Utils.extend(args, iact.mc.floor(OCache.posres));
                if (iact.click != null)
                    args = Utils.extend(args, MapView.gobclickargs(iact.click));
            }
            GameUI.this.wdgmsg("belt", args);
        }

        public Resource getres() {return (rdt.res.get());}

        public Random mkrandoom() {return (new Random(System.identityHashCode(this)));}

        public <T> T context(Class<T> cl) {return (beltctxr.context(cl, this));}

        private GameUI wdg() {return (GameUI.this);}

        @Override
        public Object rendertt(final boolean a) {
            return (Text.render(getres().layer(Resource.tooltip).t).tex());
        }
    }

    public static class PagBeltSlot extends BeltSlot {
        public final MenuGrid.Pagina pag;

        public PagBeltSlot(int idx, MenuGrid.Pagina pag) {
            super(idx);
            this.pag = pag;
        }

        public void draw(GOut g) {
            try {
                MenuGrid.PagButton btn = pag.button();
                btn.draw(g, btn.spr());
            } catch (Loading l) {
            }
        }

        public void use(MenuGrid.Interaction iact) {
            try {
                pag.scm.use(pag.button(), iact, false);
            } catch (Loading l) {
            }
        }

        public static MenuGrid.Pagina resolve(MenuGrid scm, Indir<Resource> resid) {
            Resource res = resid.get();
            Resource.AButton act = res.layer(Resource.action);
            /* XXX: This is quite a hack. Is there a better way? */
            if ((act != null) && (act.ad.length == 0))
                return (scm.paginafor(res.indir()));
            return (scm.paginafor(resid));
        }

        @Override
        public Object rendertt(final boolean a) {
            Object tt = null;
            if (pag.act() != null) {
                tt = pag.button().rendertt(a);
            } else {
                tt = Text.render(pag.res().layer(Resource.tooltip).t).tex();
            }
            return (tt);
        }
    }

    /* XXX: Remove me */
    public BeltSlot mkbeltslot(int idx, ResData rdt) {
        Resource res = rdt.res.get();
        Resource.AButton act = res.layer(Resource.action);
        if (act != null) {
            if (act.ad.length == 0)
                return (new PagBeltSlot(idx, menu.paginafor(res.indir())));
            return (new PagBeltSlot(idx, menu.paginafor(rdt.res)));
        }
        return (new ResBeltSlot(idx, rdt));
    }

    @RName("gameui")
    public static class $_ implements Factory {
        @Override
        public Widget create(UI ui, Object[] args) {
            String chrid = (String) args[0];
            int plid = (Integer) args[1];
            String genus = "";
            if (args.length > 2)
                genus = (String) args[2];
            return (new GameUI(chrid, plid, genus));
        }
    }

    public GameUI(String chrid, long plid, String genus) {
        this.chrid = chrid;
        this.plid = plid;
        this.genus = genus;
        setcanfocus(true);
        setfocusctl(true);
        ulpanel = add(new Hidepanel("gui-ul", null, new Coord(-1, -1)));
        umpanel = add(new Hidepanel("gui-um", null, new Coord(0, -1)) {
            @Override
            public Coord base() {
                if (base != null)
                    return base.get();
                return new Coord(parent.sz.x / 2 - this.sz.x / 2, 0);
            }
        });
        urpanel = add(new Hidepanel("gui-ur", null, new Coord(1, -1)));
        brpanel = add(new Hidepanel("gui-br", null, new Coord(1, 1)) {
            @Override
            public void move(double a) {
                super.move(a);
                menupanel.move();
            }
        });
        menupanel = add(new Hidepanel("menu", () -> {
//                return (new Coord(GameUI.this.sz.x, Math.min(brpanel.c.y/* - menupanel.sz.y*/ + 1, GameUI.this.sz.y - menupanel.sz.y)));
            return (new Coord(GameUI.this.sz.x, brpanel.c.y));
        }, new Coord(1, 1)));

        // brpanel.add(new Img(Resource.loadtex("gfx/hud/brframe")), 0, 0);
        if (Config.lockedmainmenu)
            menupanel.add(new MainMenu(), 0, 0);
        portrait = ulpanel.add(Frame.with(new Avaview(Avaview.dasz, plid, "avacam") {
            @Override
            public boolean mousedown(Coord c, int button) {
                return (true);
            }
        }, false));
        cal = umpanel.add(new Cal(), UI.scale(0, 10));
        if (Config.hidecalendar)
            cal.hide();
//        add(new Widget(new Coord(360, 40)) {
//            @Override
//            public void draw(GOut g) {
//                if (Config.showservertime) {
//                    Tex time = ui.sess.glob.servertimetex;
//                    if (time != null)
//                        g.image(time, new Coord(360 / 2 - time.sz().x / 2, 0));
//                }
//            }
//        }, new Coord(HavenPanel.w / 2 - 360 / 2, umpanel.sz.y));

        add(new Widget(new Coord(UI.scale(360), umpanel.sz.y)) {
            @Override
            public void draw(GOut g) {
                if (c.x != umpanel.c.x - UI.scale(360))
                    c.x = umpanel.c.x - UI.scale(360);
                if (Config.showservertime) {
                    int y = UI.scale(10);
                    int x = UI.scale(5);
                    Text ttime = ui.sess.glob.tservertimetex.get();
                    Text dtime = ui.sess.glob.dservertimetex.get();
                    Text ytime = ui.sess.glob.yservertimetex.get();
                    Text mtime = ui.sess.glob.mservertimetex.get();
                    Text btime = ui.sess.glob.bservertimetex.get();
                    if (ttime != null) {
                        g.aimage(ttime.tex(), new Coord(sz.x - x, y), 1, 0);
                        y += ttime.sz().y;
                    }
                    if (dtime != null) {
                        g.aimage(dtime.tex(), new Coord(sz.x - x, y), 1, 0);
                        y += dtime.sz().y;
                    }
                    if (ytime != null) {
                        g.aimage(ytime.tex(), new Coord(sz.x - x, y), 1, 0);
                        y += ytime.sz().y;
                    }
                    if (mtime != null) {
                        g.aimage(mtime.tex(), new Coord(sz.x - x, y), 1, 0);
                        y += mtime.sz().y;
                    }
                    if (btime != null) {
                        g.aimage(btime.tex(), new Coord(sz.x - x, y), 1, 0);
                        y += btime.sz().y;
                    }

                    Text pinfo = provincetex.get();
                    if (configuration.showlocationinfo && pinfo != null) {
                        g.aimage(pinfo.tex(), new Coord(sz.x - x, y), 1, 0);
                        y += pinfo.sz().y;
                    }

                    Text winfo = ui.sess.glob.weathertimetex.get();
                    if (configuration.showweatherinfo && winfo != null) {
                        g.aimage(winfo.tex(), new Coord(sz.x - x, y), 1, 0);
                        y += winfo.sz().y;
                    }
                    if (sz.y != y) resize(sz.x, y);
                }
            }
        }, new Coord(umpanel.c.x - UI.scale(360), 0));

        opts = add(new OptWnd());
        opts.hide();

//        PBotAPI.gui = this;
//        if (Config.showTroughrad && Config.showBeehiverad)
//            saferadius = 4;
//        else if (!Config.showTroughrad && Config.showBeehiverad)
//            saferadius = 3;
//        else if (Config.showTroughrad && !Config.showBeehiverad)
//            saferadius = 2;
//        else if (!Config.showTroughrad && !Config.showBeehiverad)
//            saferadius = 1;
        fixAlarms();
    }

    @Override
    protected void added() {
        ui.gui = this;
        resize(parent.sz);
        ui.cons.out = new java.io.PrintWriter(new java.io.Writer() {
            StringBuilder buf = new StringBuilder();

            @Override
            public void write(char[] src, int off, int len) {
                List<String> lines = new ArrayList<>();
                synchronized (this) {
                    buf.append(src, off, len);
                    int p;
                    while ((p = buf.indexOf("\n")) >= 0) {
                        lines.add(buf.substring(0, p));
                        buf.delete(0, p + 1);
                    }
                }
                for (String ln : lines)
                    debuglog.append(ln, Color.WHITE);
            }

            @Override
            public void close() {
            }

            @Override
            public void flush() {
            }
        });
        buffs = add(new Bufflist(), new Coord(95, 85));
        quickslots = new QuickSlotsWdg();
        newquickslots = new modification.newQuickSlotsWdg();

        if (!Config.quickslots) {
            quickslots.hide();
            newquickslots.hide();
        } else if (configuration.newQuickSlotWdg)
            quickslots.hide();
        else newquickslots.hide();

        add(quickslots, new Coord(430, 160));
        add(newquickslots, new Coord(450, 160));

        if (Config.statuswdgvisible) {
            statuswindow = new StatusWdg();
            add(statuswindow, umpanel.c.add(umpanel.sz.x + UI.scale(10), UI.scale(10)));
        }

        histbelt = new CraftHistoryBelt(Utils.getprefb("histbelt_vertical", true));
        add(histbelt, new Coord(70, 200));
        if (!Config.histbelt)
            histbelt.hide();
        if (!chrid.isEmpty()) {
            Utils.loadprefchklist("boulderssel_" + chrid, Config.boulders);
            Utils.loadprefchklist("bushessel_" + chrid, Config.bushes);
            Utils.loadprefchklist("treessel_" + chrid, Config.trees);
            Utils.loadprefchklist("iconssel_" + chrid, Config.icons);
            opts.setMapSettings();
//            Config.discordchat = Utils.getprefb("discordchat_" + chrid, false);
//            opts.discordcheckbox.a = Config.discordchat;
        }

        zerg = add(new Zergwnd(), new Coord(187, 50));
        if (!Config.autowindows.get("Kith & Kin").selected)
            zerg.hide();
        questwnd = add(new QuestWnd(), new Coord(0, sz.y - 200));
        chatwnd = add(new ChatWnd(chat = new ChatUI(600, 150)), new Coord(20, sz.y - 200));
        if (Config.autowindows.get("Chat") != null && Config.autowindows.get("Chat").selected)
            chatwnd.hide();
        syslog = chat.add(new ChatUI.Log("System"));
        debuglog = chat.add(new ChatUI.DebugChat());
        //Debug.log = ui.cons.out;
        Debug.add(ui.cons.out);
        opts.c = sz.sub(opts.sz).div(2);
        pointer = add(new MapPointer());
        foragehelper = add(new ForageHelperWnd());
        foragehelper.hide();
        timers = add(new TimersWnd(ui.sess.glob));
        timers.hide();
        if (!Config.autowindows.get("Timers").selected)
            timers.hide();
        scwnd = add(new SkillnCredoWnd());
        scwnd.hide();
        lm = add(new haven.sloth.gui.livestock.LivestockManager());
        lm.hide();
        scripts = add(new ScriptManager());
        scripts.hide();
        questhelper = add(new QuestHelper(), new Coord(0, sz.y - UI.scale(200)));
        questhelper.hide();
        hidden = add(new HiddenManager());
        hidden.hide();
        deleted = add(new DeletedManager());
        deleted.hide();
        alerted = add(new SoundManager());
        alerted.hide();
        gobspawner = add(new GobSpawner());
        gobspawner.hide();
        highlighted = add(new HighlightManager());
        highlighted.hide();
        overlayed = add(new OverlayManager());
        overlayed.hide();

        PBotScriptlist pblist = ui.root.findchild(PBotScriptlist.class);
        if (pblist == null) pblist = new PBotScriptlist();
        else pblist.unlink();

        PBotScriptlist = add(pblist);
        PBotScriptlist.hide();
        PBotScriptlistold = add(new PBotScriptlistOld());
        PBotScriptlistold.hide();
//        Glob.timersThread = new haven.timers.TimersThread();
//        Glob.timersThread.start();
//        timerswnd = add(new haven.timers.TimersWnd(this));
//        if (!Config.autowindows.get("Timers").selected)
//            timerswnd.hide();
        ui.root.sessionDisplay.unlink();
        if (Config.sessiondisplay) {
            add(ui.root.sessionDisplay);
        }

        if (!chrid.isEmpty() && ui.sess != null) {
            String endpoint = configuration.endpoint;
            Glob glob = ui.sess.glob;
            String username = ui.sess.username + "/" + chrid;
            if (glob != null && !endpoint.isEmpty()) {
                glob.addReference(username);
                if (configuration.loadMapSetting(username, "mapper")) {
                    MappingClient map = MappingClient.getInstance(username);
                    map.SetEndpoint(endpoint);
                    map.EnableGridUploads(true);
                    map.SetPlayerName(chrid);
                }
            }
        }

        synchronized (crutchList) {
            crutchList.forEach(Runnable::run);
            crutchList.clear();
        }

        if (configuration.autorunscriptsenable) {
            configuration.autorunscripts.forEach(name -> PBotScriptlist.getScript(name.replace(":", File.separator)).runScript());
        }
    }

    public haven.livestock.LivestockManager livestockwnd() {
        if (livestockwnd == null) {
            livestockwnd = add(new haven.livestock.LivestockManager(), new Coord(0, sz.y - 200));
            livestockwnd.hide();
        }
        return (livestockwnd);
    }

    @Override
    protected void attached() {
        if (configuration.blizzardoverlay) {
            configuration.snowThread = new configuration.SnowThread(ui.sess.glob.oc);
            configuration.snowThread.start();
        }
        iconconf = loadiconconf();
        super.attached();
    }

    @Override
    public void destroy() {
        Debug.remove(ui.cons.out);
        if (statuswindow != null) {//seems to be a bug that occasionally keeps the status window thread alive.
            statuswindow.reqdestroy();
        }
        ui.root.sessionDisplay.unlink();
        if (Config.sessiondisplay) {
            ui.root.add(ui.root.sessionDisplay);
        }

        if (configuration.snowThread != null && configuration.snowThread.isAlive())
            configuration.snowThread.kill();
        super.destroy();
        ui.gui = null;

        if (!chrid.isEmpty() && ui.sess != null) {
            Glob glob = ui.sess.glob;
            String username = ui.sess.username + "/" + chrid;
            if (glob != null) {
                glob.removeReference(username);
                MappingClient map = MappingClient.getInstance2(username);
                if (map != null) {
                    map.SetEndpoint("");
                    map.EnableGridUploads(false);
                    map.EnableTracking(false);
                    MappingClient.removeInstance(username);
                }
            }
        }
    }

    public void beltPageSwitch1() {
        nbelt.page = 0;
        nbelt.upd_page();
    }

    public void beltPageSwitch2() {
        nbelt.page = 1;
        nbelt.upd_page();
    }

    public void beltPageSwitch3() {
        nbelt.page = 2;
        nbelt.upd_page();
    }

    public void beltPageSwitch4() {
        nbelt.page = 3;
        nbelt.upd_page();
    }

    public void beltPageSwitch5() {
        nbelt.page = 4;
        nbelt.upd_page();
    }

    public void toggleDebug() {
        Config.dbtext = !Config.dbtext;
    }


    public void toggleCraftDB() {
        if (craftwnd == null) {
            craftwnd = add(new CraftDBWnd());
        } else {
            craftwnd.close();
        }
    }

    public void toggleMenuSettings() {
        if (!opts.visible()) {
            opts.show();
            opts.raise();
            fitwdg(opts);
            setfocus(opts);
            opts.chpanel(opts.flowermenus);
        } else {
            opts.show(false);
        }
    }

    public void toggleOverlaySettings() {
        if (!opts.visible()) {
            opts.show();
            opts.raise();
            fitwdg(opts);
            setfocus(opts);
            opts.chpanel(opts.hidesettings);
        } else {
            opts.show(false);
        }
    }

    public void toggleMapSettings() {
        if (!opts.visible()) {
            opts.show();
            opts.raise();
            fitwdg(opts);
            setfocus(opts);
            opts.chpanel(opts.mapPanel);
        } else {
            opts.show(false);
        }
    }

    public void toggleWaterSettings() {
        if (!opts.visible()) {
            opts.show();
            opts.raise();
            fitwdg(opts);
            setfocus(opts);
            opts.chpanel(opts.waterPanel);
        } else {
            opts.show(false);
        }
    }

    public void toggleGridLines() {
        int sum = (Config.showgridlines ? 1 : 0) + (configuration.showaccgridlines ? 1 : 0);
        Utils.setprefb("showgridlines", Config.showgridlines = Config.slothgrid ? !Config.showgridlines : sum == 0 || sum == 1);
        Utils.setprefb("showaccgridlines", configuration.showaccgridlines = !Config.slothgrid && sum == 1);
        if (map != null)
            map.togglegrid();
    }

    public void markTarget() {
        try {
            Gob g = null;
            if (fv != null && fv.current != null)
                g = map.glob.oc.getgob(fv.current.gobid);
            if (g != null) {
                synchronized (fv.lsrel) {
                    for (Fightview.Relation rel : fv.lsrel) {
                        final Gob gob = ui.sess.glob.oc.getgob(rel.gobid);
                        if (gob != null) {
                            final Gob.Overlay ol = gob.findol(AggroMark.id);
                            if (ol != null) {
                                gob.ols.remove(ol);
                            }
                        }
                    }
                }
                g.aggromark(10000);
                for (Widget wdg = chat.lchild; wdg != null; wdg = wdg.prev) {
                    if (wdg instanceof ChatUI.PartyChat) {
                        final ChatUI.PartyChat chat = (ChatUI.PartyChat) wdg;
                        chat.send(String.format(Mark.CHAT_FMT, g.id, 10000));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closestTarget() {
        try {
            fv.targetClosestCombat();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void peaceCurrent() {
        try {
            if (fv != null && fv.current != null && fv.current.give != null) {
                fv.current.give.wdgmsg("click", 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toggleInventory() {
        if ((invwnd != null) && invwnd.show(!invwnd.visible())) {
            invwnd.raise();
            fitwdg(invwnd);
        }
    }

    public void toggleEquipment() {
        if ((equwnd != null) && equwnd.show(!equwnd.visible())) {
            equwnd.raise();
            fitwdg(equwnd);
        }
    }

    public void nextSess() {
        HashMap<Integer, UI> sessmap = new HashMap<Integer, UI>();
        int sess = 0;
        int activesess = 1;
        int sesscount = MainFrame.instance.p.sessionCount();
        try {
            if (sesscount > 1) {
                for (UI uiwdg : MainFrame.instance.p.sessions) {
                    sess++;
                    if (uiwdg == ui) {
                        activesess = sess;
                    }
                    sessmap.put(sess, uiwdg);
                }
                System.out.println("active : " + activesess + " count : " + sesscount + " sess : " + sess);
                if (activesess == sess) {//if we're the last sess in the list, loop around
                    MainFrame.instance.p.setActiveUI(sessmap.get(1));
                } else {
                    MainFrame.instance.p.setActiveUI(sessmap.get(activesess + 1));
                }
            } else {
                msg("There appears to only be 1 active session currently, cannot switch.", Color.WHITE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sessmap = null;
            msg("Error trying to switch to next session.", Color.WHITE);
        }
        sessmap = null;
    }

    public void prevSess() {
        HashMap<Integer, UI> sessmap = new HashMap<Integer, UI>();
        int sess = 0;
        int activesess = 1;
        int sesscount = MainFrame.instance.p.sessionCount();
        try {
            if (sesscount > 1) {
                for (UI uiwdg : MainFrame.instance.p.sessions) {
                    sess++;
                    if (uiwdg == ui) {
                        activesess = sess;
                    }
                    sessmap.put(sess, uiwdg);
                }
                System.out.println("active : " + activesess + " count : " + sesscount + " sess : " + sess);
                if (activesess == 1) {//if we're the first sess in the list, loop around
                    MainFrame.instance.p.setActiveUI(sessmap.get(sesscount));
                } else {
                    MainFrame.instance.p.setActiveUI(sessmap.get(activesess - 1));
                }
            } else {
                msg("There appears to only be 1 active session currently, cannot switch.", Color.WHITE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sessmap = null;
            msg("Error trying to switch to next session.", Color.WHITE);
        }
        sessmap = null;
    }

    public void pauseSess() {
        DefSettings.PAUSED.set(!DefSettings.PAUSED.get());
    }

    public void fixClient() {
        PBotUtils.sysMsg(ui, String.format("Before: %d%s keys. %d%s mouses.", ui.keygrab.size(), ui.keygrab.stream().map(g -> g.wdg).collect(Collectors.toList()), ui.mousegrab.size(), ui.mousegrab.stream().map(g -> g.wdg).collect(Collectors.toList())));
        ui.keygrab.clear();
        ui.mousegrab.clear();
        setfocus(map);
        PBotUtils.sysMsg(ui, String.format("After: %d keys. %d mouses. Game focused!", ui.keygrab.size(), ui.mousegrab.size()));
    }

    public void toggleCharacter() {
        if ((chrwdg != null) && chrwdg.show(!chrwdg.visible())) {
            chrwdg.raise();
            fitwdg(chrwdg);
            setfocus(chrwdg);
        }
    }

    public void toggleKinList() {
        if (zerg.show(!zerg.visible())) {
            zerg.raise();
            fitwdg(zerg);
            //  setfocus(zerg);
        }
    }

    public void toggleQuestHelper() {
        if (questhelper.visible()) {
            questhelper.show(false);
            questhelper.active = false;
        } else {
            questhelper.show(true);
            questhelper.active = true;
            questhelper.raise();
            questhelper.refresh();
        }
    }

    public void toggleOptions() {
        if (opts.show(!opts.visible())) {
            opts.raise();
            fitwdg(opts);
            setfocus(opts);
        }
    }

    public void toggleMap() {
        if ((mapfile != null) && mapfile.show(!mapfile.visible())) {
            mapfile.raise();
            fitwdg(mapfile);
            setfocus(mapfile);
        }
    }

    public class Hidepanel extends Widget {
        public final String id;
        public final Coord g;
        public final Indir<Coord> base;
        public boolean tvis;
        private double cur;

        public Hidepanel(String id, Indir<Coord> base, Coord g) {
            this.id = id;
            this.base = base;
            this.g = g;
            cur = show(tvis = true) ? 0 : 1;
        }

        @Override
        public <T extends Widget> T add(T child) {
            super.add(child);
            pack();
            if (parent != null)
                move();
            return (child);
        }

        public Coord base() {
            if (base != null) return (base.get());
            /*
            return (new Coord((g.x > 0) ? parent.sz.x : (g.x < 0) ? 0 : (parent.sz.x / 2),
                    (g.y > 0) ? parent.sz.y : (g.y < 0) ? 0 : (parent.sz.y / 2)));
                    */
            return (new Coord((g.x > 0) ? parent.sz.x : (g.x < 0) ? 0 : ((parent.sz.x - this.sz.x) / 2),
                    (g.y > 0) ? parent.sz.y : (g.y < 0) ? 0 : ((parent.sz.y - this.sz.y) / 2)));
        }

        public void move(double a) {
            cur = a;
            Coord c = new Coord(base());
            if (g.x < 0)
                c.x -= (int) (sz.x * a);
            else if (g.x > 0)
                c.x -= (int) (sz.x * (1 - a));
            if (g.y < 0)
                c.y -= (int) (sz.y * a);
            else if (g.y > 0)
                c.y -= (int) (sz.y * (1 - a));
            this.c = c;
        }

        public void move() {
            move(cur);
        }

        @Override
        public void presize() {
            move();
        }

        public boolean mshow(final boolean vis) {
            clearanims(Anim.class);
            if (vis)
                show();
            new NormAnim(0.25) {
                final double st = cur, f = vis ? 0 : 1;

                @Override
                public void ntick(double a) {
                    if ((a == 1.0) && !vis)
                        hide();
                    move(st + (Utils.smoothstep(a) * (f - st)));
                }
            };
            tvis = vis;
            return (vis);
        }

        public boolean mshow() {
            return (mshow(Utils.getprefb(id + "-visible", true)));
        }

        public boolean cshow(boolean vis) {
            Utils.setprefb(id + "-visible", vis);
            if (vis != tvis)
                mshow(vis);
            return (vis);
        }

        @Override
        public void cdestroy(Widget w) {
            parent.cdestroy(w);
        }
    }

    public static class Hidewnd extends Window {
        Hidewnd(Coord sz, String cap, boolean lg) {
            super(sz, cap, cap, lg);
        }

        public Hidewnd(Coord sz, String cap) {
            super(sz, cap, cap, false);
        }

        @Override
        public void wdgmsg(Widget sender, String msg, Object... args) {
            if ((sender == this) && msg.equals("close")) {
                this.hide();
                return;
            }
            super.wdgmsg(sender, msg, args);
        }
    }

    static class Zergwnd extends Hidewnd {
        Tabs tabs = new Tabs(Coord.z, Coord.z, this);
        final TButton kin, pol, pol2;

        class TButton extends IButton {
            Tabs.Tab tab = null;
            final Tex inv;

            TButton(String nm, boolean g) {
                super(Resource.loadimg("gfx/hud/buttons/" + nm + "u"), Resource.loadimg("gfx/hud/buttons/" + nm + "d"));
                if (g)
                    inv = Resource.loadtex("gfx/hud/buttons/" + nm + "g");
                else
                    inv = null;
            }

            @Override
            public void draw(GOut g) {
                if ((tab == null) && (inv != null))
                    g.image(inv, Coord.z);
                else
                    super.draw(g);
            }

            @Override
            public void click() {
                if (tab != null) {
                    tabs.showtab(tab);
                    repack();
                }
            }
        }

        Zergwnd() {
            super(Coord.z, "Kith & Kin", true);
            kin = add(new TButton("kin", false));
            kin.tooltip = Text.render("Kin");
            pol = add(new TButton("pol", true));
            pol2 = add(new TButton("rlm", true));
        }

        private void repack() {
            tabs.indpack();
            kin.c = new Coord(0, tabs.curtab.contentsz().y + 20);
            pol.c = new Coord(kin.c.x + kin.sz.x + 10, kin.c.y);
            pol2.c = new Coord(pol.c.x + pol.sz.x + 10, pol.c.y);
            this.pack();
        }

        Tabs.Tab ntab(Widget ch, TButton btn) {
            Tabs.Tab tab = add(tabs.new Tab() {
                @Override
                public void cresize(Widget ch) {
                    repack();
                }
            }, tabs.c);
            tab.add(ch, Coord.z);
            btn.tab = tab;
            repack();
            return (tab);
        }

        void dtab(TButton btn) {
            btn.tab.destroy();
            btn.tab = null;
            repack();
        }

        void addpol(Polity p) {
            /* This isn't very nice. :( */
            TButton btn = p.cap.equals("Village") ? pol : pol2;
            ntab(p, btn);
            btn.tooltip = Text.render(p.cap);
        }
    }

    public static class DraggedItem {
        public final GItem item;
        final Coord dc;

        DraggedItem(GItem item, Coord dc) {
            this.item = item;
            this.dc = dc;
        }
    }

    private void updhand() {
        if ((hand.isEmpty() && (vhand != null)) || ((vhand != null) && !hand.contains(vhand.item))) {
            ui.destroy(vhand);
            vhand = null;
        }
        if (!hand.isEmpty() && (vhand == null)) {
            DraggedItem fi = hand.iterator().next();
            vhand = add(new ItemDrag(fi.dc, fi.item));
            if (map.lastItemactClickArgs != null)
                map.iteminteractreplay();
        }
    }

    public void DiscordToggle() {
        if (Discord.jdalogin != null) {
            msg("Discord Disconnected", Color.white);
            discordconnected = false;
            Discord.jdalogin.shutdownNow();
            Discord.jdalogin = null;
            for (int i = 0; i < 15; i++) {
                for (Widget w = chat.lchild; w != null; w = w.prev) {
                    if (w instanceof ChatUI.DiscordChat)
                        w.destroy();
                }
            }
        }
    }

    private String mapfilename() {
        StringBuilder buf = new StringBuilder();
        buf.append(genus);
        String chrid = Utils.getpref("mapfile/" + this.chrid, "");
        if (!chrid.equals("")) {
            if (buf.length() > 0) buf.append('/');
            buf.append(chrid);
        }
        return (buf.toString());
    }

    public void addcmeter(Widget meter) {
        Coord metc = new Coord(0, portrait.c.y + portrait.sz.y + 50);
        for (Widget m : cmeters) {
            metc = metc.add(new Coord(0, IMeter.fsz.y));
        }
        add(meter, metc);
        // ulpanel.add(meter);
        cmeters.add(meter);
        updcmeters();
    }

    public void toggleHand() {
        if (hand.isEmpty()) {
            hand.addAll(handSave);
            handSave.clear();
            updhand();
        } else {
            handSave.addAll(hand);
            hand.clear();
            updhand();
        }
    }

    public void toggleGridBinds() {
        opts.menugridcheckbox.set(!opts.menugridcheckbox.a);
        if (opts.menugridcheckbox.a)
            msg("Menugrid keybinds are now disabled!", Color.white);
        else
            msg("Menugrid keybinds are now enabled!", Color.white);
    }

    public void toggleStudy() {
        studywnd.show(!studywnd.visible());
    }

    public void takeScreenshot() {
        if (Config.screenurl != null) {
            Screenshooter.take(this, Config.screenurl);
        }
    }

    public void localScreenshot() {
        if (Config.screenurl != null)
            HavenPanel.needtotakescreenshot = true;
    }

    public <T extends Widget> void delcmeter(Class<T> cl) {
        Widget widget = null;
        for (Widget meter : cmeters) {
            if (cl.isAssignableFrom(meter.getClass())) {
                widget = meter;
                break;
            }
        }
        if (widget != null) {
            cmeters.remove(widget);
            widget.destroy();
            updcmeters();
        }
    }


    private Coord getMeterPos(int x, int y) {
        return new Coord(portrait.c.x + portrait.sz.x + UI.scale(10) + x * (IMeter.fsz.x + UI.scale(5)), portrait.c.y + y * (IMeter.fsz.y + UI.scale(2)));
    }

    public void addMeterAt(Widget m, int x, int y) {
        ulpanel.add(m, getMeterPos(x, y));
        ulpanel.pack();
    }


    private void updcmeters() {
        int i = 0;
        for (Widget meter : cmeters) {
            int x = ((meters.size() + i) % 3) * (IMeter.fsz.x + UI.scale(5));
            int y = ((meters.size() + i) / 3) * (IMeter.fsz.y + UI.scale(2));
//            meter.c = new Coord(portrait.c.x + portrait.sz.x + 10 + x, portrait.c.y + y);
            i++;
        }
    }

    public Coord optplacement(Widget child, Coord org) {
        Set<Window> closed = new HashSet<>();
        Set<Coord> open = new HashSet<>();

        //Hook for window placement
        if (child instanceof Window) {
            String name = ((Window) child).origcap;
            Window wnd = PBotWindowAPI.getWindow(ui, name);
            if (wnd != null)
                org = wnd.c;
        }

        open.add(org);
        Coord opt = null;
        double optscore = Double.NEGATIVE_INFINITY;
        Coord plc = null;
        {
            Gob pl = map.player();
            if (pl != null)
                plc = pl.sc;
        }
        Area parea = Area.sized(Coord.z, sz);
        while (!open.isEmpty()) {
            Coord cur = Utils.take(open);
            double score = 0;
            Area tarea = Area.sized(cur, child.sz);
            if (parea.isects(tarea)) {
                double outside = 1.0 - (((double) parea.overlap(tarea).area()) / ((double) tarea.area()));
                if ((outside > 0.75) && !cur.equals(org))
                    continue;
                score -= Math.pow(outside, 2) * 100;
            } else {
                if (!cur.equals(org))
                    continue;
                score -= 100;
            }
            {
                boolean any = false;
                for (Widget wdg = this.child; wdg != null; wdg = wdg.next) {
                    if (!(wdg instanceof Window))
                        continue;
                    Window wnd = (Window) wdg;
                    if (!wnd.visible())
                        continue;
                    Area warea = wnd.parentarea(this);
                    if (warea.isects(tarea)) {
                        any = true;
                        score -= ((double) warea.overlap(tarea).area()) / ((double) tarea.area());
                        if (!closed.contains(wnd)) {
                            open.add(new Coord(wnd.c.x - child.sz.x, cur.y));
                            open.add(new Coord(cur.x, wnd.c.y - child.sz.y));
                            open.add(new Coord(wnd.c.x + wnd.sz.x, cur.y));
                            open.add(new Coord(cur.x, wnd.c.y + wnd.sz.y));
                            closed.add(wnd);
                        }
                    }
                }
                if (!any)
                    score += 10;
            }
            if (plc != null) {
                if (tarea.contains(plc))
                    score -= 100;
                else
                    score -= (1 - Math.pow(tarea.closest(plc).dist(plc) / sz.dist(Coord.z), 2)) * 1.5;
            }
            score -= (cur.dist(org) / sz.dist(Coord.z)) * 0.75;
            if (score > optscore) {
                optscore = score;
                opt = cur;
            }
        }
        return (opt);
    }

//    private void savewndpos() {
//        if (mapfile != null) {
//            Utils.setprefc("wndsz-map", mapfile.asz);
//        }
//    }

    private final List<Runnable> crutchList = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void addchild(Widget child, Object... args) {
        Runnable run = () -> addchild2(child, args);
        if (parent == null) {
            synchronized (crutchList) {
                crutchList.add(run);
            }
        } else {
            run.run();
        }
    }

    public void addchild2(Widget child, Object... args) {
        String place = ((String) args[0]).intern();
        if (place == "mapview") {
            child.resize(sz);
            map = add((MapView) child, Coord.z);
            map.lower();
            if (mmap != null)
                ui.destroy(mmap);
            if (mapfile != null) {
                ui.destroy(mapfile);
                mapfile = null;
            }
            ResCache cache = ResCache.getCache("map");
            if (cache != null) {
                MapFile file;
                try {
                    file = MapFile.load(cache, mapfilename());
                } catch (java.io.IOException e) {
                    /* XXX: Not quite sure what to do here. It's
                     * certainly not obvious that overwriting the
                     * existing mapfile with a new one is better. */
                    throw (new RuntimeException("failed to load mapfile", e));
                }

                mapfile = new MapWnd(file, map, Utils.getprefc("wndsz-map", UI.scale(700, 500)), "Map");
                mapfile.hide();
                add(mapfile, UI.scale(50, 50));

                mmap = new LocalMiniMap(UI.scale(133, 133), map);
                mmapwnd = adda(new MinimapWnd(mmap), new Coord(sz.x, 0), 1, 0);
                mmap.save(file);

                mmapwnd.mapfile = mapfile;
//                mapfile.uploadMarks();
            }

            if (trackon) {
                buffs.addchild(new Buff(Bufflist.bufftrack.indir()));
                msgnosfx(Resource.getLocString(Resource.BUNDLE_MSG, "Tracking is now turned on."));
            }
            if (crimeon) {
                buffs.addchild(new Buff(Bufflist.buffcrime.indir()));
                msgnosfx(Resource.getLocString(Resource.BUNDLE_MSG, "Criminal acts are now turned on."));
            }
            if (swimon) {
                buffs.addchild(new Buff(Bufflist.buffswim.indir()));
                msgnosfx(Resource.getLocString(Resource.BUNDLE_MSG, "Swimming is now turned on."));
            }
            if (partyperm) {
                buffs.addchild(new Buff(Bufflist.partyperm.indir()));
                msgnosfx(Resource.getLocString(Resource.BUNDLE_MSG, "Party permissions are now turned on."));
            }
        } else if (place == "menu") {
            menu = (MenuGrid) brpanel.add(child);
            try {
                final BeltData data = new BeltData(ui.sess.username + "::" + chrid);
                fbelt = add(new BeltWnd("fk", data, KeyEvent.VK_F1, KeyEvent.VK_F10, 5, 50), UI.scale(0, 50));
                npbelt = add(new BeltWnd("np", data, KeyEvent.VK_NUMPAD0, KeyEvent.VK_NUMPAD9, 4, 100), UI.scale(0, 100));
                nbelt = add(new BeltWnd("n", data, KeyEvent.VK_0, KeyEvent.VK_9, 5, 0), UI.scale(0, 150));
            } catch (Exception e) {
                dev.simpleLog(e);
            }
            menuSearch = add(new MenuSearch("Search..."));
            if (!Config.autowindows.get("Search...").selected)
                menuSearch.hide();
            filter = add(new FilterWnd("Filter"));
            filter.hide();
        } else if (place == "fight") {
//            fv = urpanel.add((Fightview) child, 0, 0);
            fv = adda((Fightview) child, sz.x, 0, 1, 0);
        } else if (place == "fsess") {
            fs = add((Fightsess) child, Coord.z);
        } else if (place == "inv") {
            invwnd = new Hidewnd(Coord.z, "Inventory") {
                @Override
                public void cresize(Widget ch) {
//                    pack();
                }
            };
            invwnd.add(maininv = (Inventory) child, Coord.z);
            invwnd.pack();
            invwnd.show(Config.autowindows.get("Inventory").selected);
            add(invwnd, UI.scale(100, 100));
        } else if (place == "equ") {
            equwnd = new Hidewnd(Coord.z, "Equipment");
            equipory = equwnd.add((Equipory) child, Coord.z);
            equwnd.pack();
            equwnd.hide();
            add(equwnd, UI.scale(400, 10));
            equwnd.show(Config.autowindows.get("Equipment").selected);
        } else if (place == "hand") {
            GItem g = add((GItem) child);
            Coord lc = (Coord) args[1];
            hand.add(new DraggedItem(g, lc));
            updhand();
        } else if (place == "chr") {
            chrwdg = add((CharWnd) child, UI.scale(300, 50));
            if (!Config.autowindows.get("Character Sheet").selected)
                chrwdg.hide();
        } else if (place == "craft") {
            final Widget mkwdg = child;
            if (craftwnd != null) {
                craftwnd.setMakewindow(mkwdg);
            } else {
                if (makewnd == null) {
                    makewnd = add(new CraftWindow() {
                        @Override
                        public void reqdestroy() {
                            super.reqdestroy();
                            makewnd = null;
                        }
                    }, UI.scale(400, 200));
                }
                makewnd.add(child);
                makewnd.pack();
                makewnd.raise();
                makewnd.show();
            }
        } else if (place == "buddy") {
            zerg.ntab(buddies = (BuddyWnd) child, zerg.kin);
        } else if (place == "pol") {
            Polity p = (Polity) child;
            polities.add(p);
            zerg.addpol(p);
        } else if (place == "chat") {
            chat.addchild(child);
        } else if (place == "party") {
            partyview = add((Partyview) child, UI.scale(10, 95));
        } else if (place == "meter") {
            int x = (meters.size() % 3) * (IMeter.fsz.x + UI.scale(5));
            int y = (meters.size() / 3) * (IMeter.fsz.y + UI.scale(2));
            add(child, portrait.c.x + portrait.sz.x + UI.scale(10) + x, portrait.c.y + y);
            meters.add(child);
            updcmeters();
        } else if (place == "buff") {
            buffs.addchild(child);
        } else if (place == "qq") {
            if (qqview != null) {
                qqview.reqdestroy();
            }
            qqview = child;
            questwnd.add(child, Coord.z);
        } else if (place == "misc") {
            Coord c;
            if (child instanceof Window && ((Window) child).cap != null &&
                    haven.sloth.gui.livestock.LivestockManager.animals.contains(Resource.getLocString(Resource.BUNDLE_WINDOW, ((Window) child).cap.text))) {
                if (configuration.forcelivestock) {
                    lm.addAnimal((Window) child);
                    return;
                }
            }
            if (args[1] instanceof Coord) {
                c = (Coord) args[1];
            } else if (args[1] instanceof Coord2d) {
                c = ((Coord2d) args[1]).mul(new Coord2d(this.sz.sub(child.sz))).round();
                c = optplacement(child, c);
            } else if (args[1] instanceof String) {
                c = relpos((String) args[1], child, (args.length > 2) ? ((Object[]) args[2]) : new Object[]{}, 0);
            } else {
                throw (new UI.UIException("Illegal gameui child", place, args));
            }
            add(child, c);
        } else if (place == "abt") {
            add(child, Coord.z);
        } else {
            throw (new UI.UIException("Illegal gameui child", place, args));
        }
    }

    @Override
    public void cdestroy(Widget w) {
        if (w instanceof GItem) {
            for (Iterator<DraggedItem> i = hand.iterator(); i.hasNext(); ) {
                DraggedItem di = i.next();
                if (di.item == w) {
                    i.remove();
                    updhand();
                }
            }
        } else if (w instanceof Polity && polities.contains(w)) {
            polities.remove(w);
            zerg.dtab(zerg.pol);
        } else if (w == chrwdg) {
            chrwdg = null;
        }
        if (meters.remove(w))
            updcmeters();
        cmeters.remove(w);
    }

    private static final Resource.Anim progt = Resource.remote().loadwait("gfx/hud/prog").layer(Resource.animc);
    private Tex curprog = null;
    private int curprogf, curprogb;

    private void drawprog(GOut g, double prog) {
        int fr = Utils.clip((int) Math.floor(prog * progt.f.length), 0, progt.f.length - 2);
        int bf = Utils.clip((int) (((prog * progt.f.length) - fr) * 255), 0, 255);
        if ((curprog == null) || (curprogf != fr) || (curprogb != bf)) {
            if (curprog != null)
                curprog.dispose();
            WritableRaster buf = PUtils.imgraster(progt.f[fr][0].sz);
            PUtils.blit(buf, progt.f[fr][0].img.getRaster(), Coord.z);
            PUtils.blendblit(buf, progt.f[fr + 1][0].img.getRaster(), Coord.z, bf);
            curprog = new TexI(PUtils.rasterimg(buf));
            curprogf = fr;
            curprogb = bf;
        }
        Coord hgc = new Coord(sz.x / 2, (sz.y * 4) / 10);
        g.aimage(curprog, hgc, 0.5, 0.5);
        if (Config.showprogressperc)
            g.atextstroked(Utils.odformat2(prog * 100, 2) + "%", hgc, 0.5, 2.5, Color.WHITE, Color.BLACK, Text.num12boldFnd);
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        if (prog >= 0)
            drawprog(g, prog);
        int bx = blpw;
        int by = sz.y;
        if (chatwnd.visible()) {
            bx = Math.max(bx, chatwnd.c.x);
            by = Math.min(by, chatwnd.c.y);
        }
        if (cmdline != null) {
            drawcmd(g, new Coord(bx + 10, by -= 20));
        } else if (lastmsg != null) {
            if ((Utils.rtime() - msgtime) > 3.0) {
                lastmsg = null;
            } else {
                g.chcolor(0, 0, 0, 192);
                g.frect(new Coord(bx + 8, by - 22), lastmsg.sz().add(4, 4));
                g.chcolor();
                g.image(lastmsg.tex(), new Coord(bx + 10, by -= 20));
            }
        }
    }

    private String iconconfname() {
        StringBuilder buf = new StringBuilder();
        buf.append("data/mm-icons");
        if (genus != null)
            buf.append("/" + genus);
        if (ui.sess != null)
            buf.append("/" + ui.sess.username);
        return (buf.toString());
    }

    private GobIcon.Settings loadiconconf() {
        if (ResCache.getCache("map") == null)
            return (new GobIcon.Settings());
        try {
            try (StreamMessage fp = new StreamMessage(ResCache.getCache("map").fetch(iconconfname()))) {
                return (GobIcon.Settings.load(fp));
            }
        } catch (java.io.FileNotFoundException e) {
            return (new GobIcon.Settings());
        } catch (Exception e) {
            new Warning(e, "failed to load icon-conf").issue();
            return (new GobIcon.Settings());
        }
    }

    public void saveiconconf() {
        if (ResCache.getCache("map") == null)
            return;
        try {
            try (StreamMessage fp = new StreamMessage(ResCache.getCache("map").store(iconconfname()))) {
                iconconf.save(fp);
            }
        } catch (Exception e) {
            new Warning(e, "failed to store icon-conf").issue();
        }
    }

    private double lastwndsave = 0;

    @Override
    public void tick(double dt) {
        super.tick(dt);
        double now = Utils.rtime();
        try {
            IMeter.Meter stam = getmeter("stam", 0);
            if (Config.temporaryswimming && temporarilyswimming) {
                if (System.currentTimeMillis() - SwimTimer >= 30000) {
                    SwimTimer = 0;
                    temporarilyswimming = false;
                    PBotUtils.doAct(ui, "swim");
                }
            }
            if (!drinkingWater && Config.autodrink && (DrinkThread == null || !DrinkThread.isAlive()) && stam.a < Config.autodrinkthreshold) {
                if (System.currentTimeMillis() - DrinkTimer >= Config.autodrinktime * 1000) {
                    DrinkTimer = System.currentTimeMillis();
                    Drink();
                }
            }
            int energy = getmeter("nrj", 0).a;
            if (energy < 21 && System.currentTimeMillis() - StarvationAlertDelay > 10000 && Config.StarveAlert) {
                StarvationAlertDelay = System.currentTimeMillis();
                PBotUtils.sysMsg(ui, "You are Starving!", Color.white);
            }
        } catch (Exception e) {
        }//exceptions doing these two things aren't critical, ignore
        if (now - lastwndsave > 60) {
//            savewndpos();
            lastwndsave = now;
        }
        double idle = Utils.rtime() - ui.lastevent;
        if (!afk && (idle > 300)) {
            afk = true;
            wdgmsg("afk");
            if (Config.afklogouttime != 0) {
                if (idle > Config.afklogouttime * 60)
                    logoutChar();
            }
        } else if (afk && (idle <= 300)) {
            afk = false;
        }
    }

    private void togglebuff(String err, Resource res) {
        String name = res.basename();
        if (err.endsWith("on.") && buffs.gettoggle(name) == null) {
            buffs.addchild(new Buff(res.indir()));
            if (name.equals("swim"))
                swimon = true;
            else if (name.equals("crime"))
                crimeon = true;
            else if (name.equals("tracking"))
                trackon = true;
        } else if (err.endsWith("off.")) {
            Buff tgl = buffs.gettoggle(name);
            if (tgl != null)
                tgl.reqdestroy();
            if (name.equals("swim"))
                swimon = false;
            else if (name.equals("crime"))
                crimeon = false;
            else if (name.equals("tracking"))
                trackon = false;
        }
    }

    private void updateSlot(int slot) {
        if (slot <= 49)
            nbelt.update(slot);
        else if (slot <= 99)
            fbelt.update(slot);
        else
            npbelt.update(slot);
    }

    @Override
    public void uimsg(String msg, Object... args) {
        if (msg == "err") {
            error((String) args[0]);
        } else if (msg == "prog") {
            if (args.length > 0)
                prog = ((Number) args[0]).doubleValue() / 100.0;
            else
                prog = -1;
        } else if (msg == "setbelt") {
            int slot = Utils.iv(args[0]);
            if (args.length < 2) {
                belt[slot] = null;
                updateSlot(slot);
            } else {
                Indir<Resource> res = ui.sess.getresv(args[1]);
                Message sdt = Message.nil;
                if (args.length > 2)
                    sdt = new MessageBuf((byte[]) args[2]);
                ResData rdt = new ResData(res, sdt);
                ui.sess.glob.loader.defer(() -> {
                    belt[slot] = mkbeltslot(slot, rdt);
                    updateSlot(slot);
                }, null);
            }
        } else if (msg == "setbelt2") {
            int slot = Utils.iv(args[0]);
            if (args.length < 2) {
                belt[slot] = null;
                updateSlot(slot);
            } else {
                switch ((String) args[1]) {
                    case "p": {
                        Object id = args[2];
                        belt[slot] = new PagBeltSlot(slot, menu.paginafor(id, null));
                        updateSlot(slot);
                        break;
                    }
                    case "r": {
                        Indir<Resource> res = ui.sess.getresv(args[2]);
                        ui.sess.glob.loader.defer(() -> {
                            belt[slot] = new PagBeltSlot(slot, PagBeltSlot.resolve(menu, res));
                            updateSlot(slot);
                        }, null);
                        break;
                    }
                    case "d": {
                        Indir<Resource> res = ui.sess.getresv(args[2]);
                        Message sdt = Message.nil;
                        if (args.length > 2)
                            sdt = new MessageBuf((byte[]) args[3]);
                        belt[slot] = new ResBeltSlot(slot, new ResData(res, sdt));
                        updateSlot(slot);
                        break;
                    }
                }
            }
        } else if (msg == "polowner") {
            int id = (Integer) args[0];
            String o = (String) args[1];
            boolean n = ((Integer) args[2]) != 0;
            if (o != null)
                o = o.intern();
            String cur = polowners.get(id);
            if (map != null) {
                if ((o != null) && (cur == null)) {
                    if (Config.DivertPolityMessages)
                        PBotUtils.sysMsg(ui, "Entering " + o, Color.GREEN);
                    else
                        map.setpoltext(id, "Entering " + o);
                } else if ((o == null) && (cur != null)) {
                    if (Config.DivertPolityMessages)
                        PBotUtils.sysMsg(ui, "Leaving " + cur, Color.GREEN);
                    else
                        map.setpoltext(id, "Leaving " + cur);
                }
            }
            polowners.put(id, o);
        } else if (msg == "showhelp") {
            Indir<Resource> res = ui.sess.getresv(args[0]);
            if (help == null)
                help = adda(new HelpWnd(res), 0.5, 0.25);
            else
                help.res = res;
        } else if (msg == "map-mark") {
            dev.sysPrintStackTrace("map-mark");
            dev.sysLog("map-mark", this, -1, msg, args);
            long gobid = Utils.uiv(args[0]);
            long oid = ((Number) args[1]).longValue();
            Indir<Resource> res = ui.sess.getresv(args[2]);
            String nm = (String) args[3];
            if (mapfile != null)
                mapfile.markobj(gobid, oid, res, nm);
        } else if (msg == "map-icons") {
            GobIcon.Settings conf = this.iconconf;
            int tag = Utils.iv(args[0]);
            if (args.length < 2) {
                if (conf.tag != tag)
                    wdgmsg("map-icons", conf.tag);
            } else if (args[1] instanceof String) {
                Resource.Spec res = new Resource.Spec(null, (String) args[1], (Integer) args[2]);
                GobIcon.Setting cset = new GobIcon.Setting(res);
                boolean has = conf.settings.containsKey(res.name);
                cset.show = cset.defshow = ((Integer) args[3]) != 0;
                conf.receive(tag, new GobIcon.Setting[]{cset});
                saveiconconf();
                if (!has && conf.notify) {
                    ui.sess.glob.loader.defer(() -> {
                        Resource lres = Resource.remote().load(res.name, res.ver).get();
                        Resource.Tooltip tip = lres.layer(Resource.tooltip);
                        if (tip != null)
                            msg(String.format("%s added to list of seen icons.", tip.t));
                    }, null);
                }
            } else if (args[1] instanceof Object[]) {
                Object[] sub = (Object[]) args[1];
                int a = 0;
                Collection<GobIcon.Setting> csets = new ArrayList<>();
                while (a < sub.length) {
                    String resnm = (String) sub[a++];
                    int resver = (Integer) sub[a++];
                    int fl = (Integer) sub[a++];
                    Resource.Spec res = new Resource.Spec(null, resnm, resver);
                    GobIcon.Setting cset = new GobIcon.Setting(res);
                    cset.show = cset.defshow = ((fl & 1) != 0);
                    csets.add(cset);
                }
                conf.receive(tag, csets.toArray(new GobIcon.Setting[0]));
                saveiconconf();
            }
        } else {
            super.uimsg(msg, args);
        }
    }

    public final AtomicReference<Text> provincetex = new AtomicReference<>(null);

    public void notifyProvince(Object... cargs) {
        if (map != null) {
            try {
                String name = (String) cargs[0];
                int id = 0;
                String realmName = "";
                if (cargs.length == 4) {
                    id = (int) cargs[2];
                    realmName = (String) cargs[3];
                }
                String text = "Entering ";

                String finish = text + name + (!realmName.isEmpty() ? " of " + realmName : "");
                if (Config.DivertPolityMessages)
                    PBotUtils.sysMsg(ui, finish, Color.GREEN);
                else
                    map.setpoltext(id, finish);
                String ptext = "Province '" + name + "'";
                if (provincetex.get() == null || !provincetex.get().text.equals(ptext))
                    provincetex.set(Text.create(ptext, PUtils.strokeImg(RichText.render(ptext, -1))));
            } catch (Exception e) {
                dev.simpleLog(e);
            }
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        //   System.out.println("############");if(!sender.toString().contains("Camera")) System.out.println(sender);System.out.println(msg);for(Object o :args) System.out.println(o);
        if (msg.equals("close")) {
            if (sender == chrwdg) {
                chrwdg.hide();
                return;
            } else if (sender == mapfile) {
                mapfile.hide();
                return;
            } else if (sender == help) {
                ui.destroy(help);
                help = null;
                return;
            } else if ((polities.contains(sender)) && (msg == "close")) {
                sender.hide();
            } else if (sender == iconwnd) {
                iconwnd.hide();
                return;
            }
        }
        super.wdgmsg(sender, msg, args);
    }

    public void fitwdg(Widget wdg) {
        if (wdg.c.x < 0)
            wdg.c.x = 0;
        if (wdg.c.y < 0)
            wdg.c.y = 0;
        if (wdg.c.x + wdg.sz.x > sz.x)
            wdg.c.x = sz.x - wdg.sz.x;
        if (wdg.c.y + wdg.sz.y > sz.y)
            wdg.c.y = sz.y - wdg.sz.y;
    }

    private static final Tex menubg = Resource.loadtex("gfx/hud/mainmenu/rbtn-bg");

    public class MenuButton2 extends IButton {
        private final Action action;
        private final String tip;

        MenuButton2(String base, String tooltip, Action action) {
            super("gfx/hud/mainmenu/" + base, "", "-d", "-h");
            this.action = action;
            this.tip = tooltip;
            // KeyBinder.add(code, mods, action);
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            if (!checkhit(c)) {
                return null;
            }
            KeyBinder.KeyBind bind = KeyBinder.get(action);
            String tt = tip;
            if (bind != null && !bind.isEmpty()) {
                tt = String.format("%s ($col[255,255,0]{%s})", tip, bind.shortcut());
            }
            return RichText.render(tt, 0);
        }

        @Override
        public void click() {
            action.run(GameUI.this);
        }
    }

    public class MainMenu extends Widget {
        public MainMenu() {
            super(menubg.sz());
            add(new MenuButton2("rbtn-src", "Menu Search", TOGGLE_SEARCH), UI.scale(1, 1));
            add(new MenuButton2("rbtn-inv", "Inventory", TOGGLE_INVENTORY), UI.scale(34, 1));
            add(new MenuButton2("rbtn-equ", "Equipment", TOGGLE_EQUIPMENT), UI.scale(67, 1));
            add(new MenuButton2("rbtn-chr", "Character Sheet", TOGGLE_CHARACTER), UI.scale(100, 1));
            add(new MenuButton2("rbtn-bud", "Kith & Kin", TOGGLE_KIN_LIST), UI.scale(133, 1));
            add(new MenuButton2("rbtn-opt", "Options", TOGGLE_OPTIONS), UI.scale(166, 1));
            pack();
        }

        @Override
        public void draw(GOut g) {
            g.image(menubg, Coord.z);
            super.draw(g);
        }
    }

    public void SwitchTargets() {
        Fightview.Relation cur = fv.current;
        if (cur != null) {
            fv.lsrel.remove(cur);
            fv.lsrel.addLast(cur);
            fv.wdgmsg("bump", (int) fv.lsrel.get(0).gobid);
        }
    }

    public void switchmarkedtarget() {
        Fightview.Relation cur = fv.current;
        if (cur != null) {
            synchronized (fv.lsrel) {
                for (Fightview.Relation rel : fv.lsrel) {
                    final Gob gob = ui.sess.glob.oc.getgob(rel.gobid);
                    if (gob != null) {
                        final Gob.Overlay ol = gob.findol(AggroMark.id);
                        if (ol != null) {
                            fv.lsrel.remove(cur);
                            fv.lsrel.addLast(cur);
                            fv.wdgmsg("bump", (int) gob.id);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void toggleGobs() {
        int sum = (Config.showboundingboxes ? 1 : 0) + (configuration.showaccboundingboxes ? 1 : 0);
        Utils.setprefb("showboundingboxes", Config.showboundingboxes = (sum == 0 || sum == 1));
        Utils.setprefb("showaccboundingboxes", configuration.showaccboundingboxes = sum == 1);
        if (map != null)
            map.refreshGobsAll();
    }

    void toggleTimers() {
        if (timers != null && timers.show(!timers.visible())) {
            timers.raise();
            fitwdg(timers);
            setfocus(timers);
        }
    }

    void toggleScripts() {
        if (scripts != null && scripts.show(!scripts.visible())) {
            scripts.raise();
            fitwdg(scripts);
            setfocus(scripts);
        }
    }

    void toggleHighlight() {
        if (highlighted != null && highlighted.show(!highlighted.visible())) {
            highlighted.raise();
            fitwdg(highlighted);
            setfocus(highlighted);
        }
    }

    void toggleOverlay() {
        if (overlayed != null && overlayed.show(!overlayed.visible())) {
            overlayed.raise();
            fitwdg(overlayed);
            setfocus(overlayed);
        }
    }

    void toggleHidden() {
        if (hidden != null && hidden.show(!hidden.visible())) {
            hidden.raise();
            fitwdg(hidden);
            setfocus(hidden);
        }
    }

    public void OpenChat() {
        if (chatwnd != null && chatwnd.show(!chatwnd.visible())) {
            chatwnd.raise();
            fitwdg(chatwnd);
            setfocus(chatwnd);
        }
    }

    void toggleAlerted() {
        if (alerted != null && alerted.show(!alerted.visible())) {
            alerted.raise();
            fitwdg(alerted);
            setfocus(alerted);
        }
    }

    void toggleGobSpawner() {
        if (gobspawner != null && gobspawner.show(!gobspawner.visible())) {
            gobspawner.raise();
            fitwdg(gobspawner);
            setfocus(gobspawner);
        }
    }

    public void toggleKin() {
        if (zerg.show(!zerg.visible())) {
            zerg.raise();
            fitwdg(zerg);
            setfocus(zerg);
        }
    }

    void toggleInv() {
        if ((invwnd != null) && invwnd.show(!invwnd.visible())) {
            invwnd.raise();
            fitwdg(invwnd);
            setfocus(invwnd);
        }
    }


    void toggleCharWnd() {
        if ((chrwdg != null) && chrwdg.show(!chrwdg.visible())) {
            chrwdg.raise();
            fitwdg(chrwdg);
            setfocus(chrwdg);
        }
    }

    public void toggleMapfile() {
        if ((mapfile != null) && mapfile.show(!mapfile.visible())) {
            mapfile.raise();
            fitwdg(mapfile);
            setfocus(mapfile);
        }
    }

    public void toggleForageHelper() {
        if (foragehelper != null && foragehelper.show(!foragehelper.visible())) {
            foragehelper.raise();
            fitwdg(foragehelper);
            setfocus(foragehelper);
        }
    }

    public void toggleChat() {
        if (chatwnd.visible() && !chat.hasfocus) {
            setfocus(chat);
        } else if (chatwnd.visible() && chat.hasfocus) {
            // OpenChat();
            setfocus(maininv);
        } else {
            if (!chatwnd.visible()) {
                OpenChat();
            } else {
                setfocus(chat);
            }
        }
    }

    public void toggleMinimap() {
        if (mmapwnd != null && mmapwnd.show(!mmapwnd.visible())) {
            mmapwnd.raise();
            fitwdg(mmapwnd);
            setfocus(mmapwnd);
        }
    }

    public void toggleMapGrid() {
        Config.mapshowgrid = !Config.mapshowgrid;
        Utils.setprefb("mapshowgrid", Config.mapshowgrid);
    }

    public void toggleMapViewDist() {
        Config.mapshowviewdist = !Config.mapshowviewdist;
        Utils.setprefb("mapshowviewdist", Config.mapshowviewdist);
    }

    public void toggleMute() {
        if (Audio.volume > 0) {
            PBotUtils.sysMsg(ui, "Audio muted.", Color.white);
            Audio.volume = 0;
        } else {
            Audio.volume = Double.parseDouble(Utils.getpref("sfxvol", "1.0"));
            PBotUtils.sysMsg(ui, "Audio un-muted.", Color.white);
        }
    }

    public void logout() {
        if (Discord.jdalogin != null)
            DiscordToggle();
        //  act("lo");
        ui.sess.close();
    }

    public void logoutChar() {
        if (Discord.jdalogin != null)
            DiscordToggle();
        act("lo", "cs");
    }

    public void toggleTreeStage() {
        /*Config.showplantgrowstage = !Config.showplantgrowstage;
        Utils.setprefb("showplantgrowstage", Config.showplantgrowstage);
        if (!Config.showplantgrowstage && map != null)
            map.removeCustomSprites(Sprite.GROWTH_STAGE_ID);
        if (map != null)
            map.refreshGobsGrowthStages();*/

        List<Window> wnds = PBotWindowAPI.getWindows(ui, "Growth Stage");
        if (!wnds.isEmpty()) {
            wnds.forEach(Window::close);
            return;
        }
        Window w = new Window(Coord.z, "Growth Stage");
        WidgetVerticalAppender wva = new WidgetVerticalAppender(w);
        wva.addRow(new CheckBox("Show Plant Grow Stage", val -> {
            Utils.setprefb("showplantgrowstage", Config.showplantgrowstage = val);
            if (!val && map != null)
                map.removeCustomSprites(Sprite.GROWTH_STAGE_ID);
            if (map != null)
                map.refreshGobsGrowthStages();
        }, Config.showplantgrowstage));
        wva.add(new CheckBox("New overlay for plant stage") {
            {
                a = configuration.newCropStageOverlay;
            }

            public void set(boolean val) {
                Utils.setprefb("newCropStageOverlay", val);
                configuration.newCropStageOverlay = val;
                a = val;
            }
        });
        wva.add(new CheckBox("Display stage 1 (fresh planted) crops when crop stage overlay enabled.") {
            {
                a = Config.showfreshcropstage;
            }

            public void set(boolean val) {
                Utils.setprefb("showfreshcropstage", val);
                Config.showfreshcropstage = val;
                a = val;
            }
        });
        wva.addRow(new CheckBox("Show Tree Grow Stage", val -> {
            Utils.setprefb("showtreegrowstage", Config.showtreegrowstage = val);
            if (!val && map != null)
                map.removeCustomSprites(Sprite.GROWTH_STAGE_ID2);
            if (map != null)
                map.refreshGobsGrowthStages2();
        }, Config.showtreegrowstage));
        Label text = new Label(configuration.minimumtreestage + "");
        wva.addRow(new Label("Tree minimum:"), new HSlider(UI.scale(100), 100, 200, (int) (configuration.minimumtreestage)) {
            @Override
            public void changed() {
                Utils.setprefi("minimumtreestage", configuration.minimumtreestage = val);
                text.settext(configuration.minimumtreestage + "");
            }

            @Override
            public Object tooltip(final Coord c, final Widget prev) {
                return (configuration.minimumtreestage + "");
            }
        }, text);
        w.pack();

        adda(w, sz.div(2), 0.5, 0.5);
    }

    public void toggleSearch() {
        KeyBind k = KeyBinder.get(TOGGLE_SEARCH);
        if (menuSearch.show(!menuSearch.visible())) {
            if (menuSearch.visible()) {
                menuSearch.raise();
                fitwdg(menuSearch);
                setfocus(menuSearch);
                if (k.mods == 4)
                    menuSearch.ignoreinit = true;
            }
        }
    }

    public void doNothing() {
        //ugly hack to stop unbound keybinds from being triggered
    }

    public void toggleDaylight() {
        DefSettings.NIGHTVISION.set(!DefSettings.NIGHTVISION.get());
    }

    public void toggleFilter() {
        if (filter.show(!filter.visible())) {
            filter.raise();
            fitwdg(filter);
        }
    }

    public void toggleUI() {
        TexGL.disableall = !TexGL.disableall;
    }

    public void harvestForageable() {
        Thread t = new Thread(new PickForageable(this), "PickForageable");
        t.start();
    }

    public void harvestMForageable() {
        if (map != null) {
            map.takemc(ui.mc, mc -> {
                Thread t = new Thread(new PickForageable(this, mc), "PickMForageable");
                t.start();
            });
        }
    }

    public void traverse() {
        Thread t = new Thread(new Traverse(this), "Traverse");
        t.start();
    }

    public void toggleDangerRadius() {
        toggleSafeRadius();
//        Config.showminerad = !Config.showminerad;
//        msg("Mine support radii are now : " + Config.showminerad, Color.white);
//        Utils.setprefb("showminerad", Config.showminerad);
    }

    public void toggleSafeRadius() {
        List<Window> wnds = PBotWindowAPI.getWindows(ui, "Radius Configurator");
        if (!wnds.isEmpty()) {
            wnds.forEach(Window::close);
            return;
        }
        Window w = new Window(Coord.z, "Radius Configurator");
        WidgetVerticalAppender wva = new WidgetVerticalAppender(w);
        wva.addRow(new CheckBox("Show Animal Radius", val -> {
            Utils.setprefb("showanimalrad", Config.showanimalrad = val);
            msg("Animal Radius " + (val ? "on" : "off"), Color.white);
        }, Config.showanimalrad), new ColorPreview(UI.scale(20, 20), ANIMALDANGERCOLOR.get(), val -> {
            BPRadSprite.smatDanger = new States.ColState(val);
            ANIMALDANGERCOLOR.set(val);
            if (ui.gui.map != null) ui.gui.map.refreshGobsAll();
        }, "Dangerous animal radius color"));
        wva.addRow(new CheckBox("Double animal radius size", val -> {
            Utils.setprefb("doubleradius", Config.doubleradius = val);
            msg("Double Radius " + (val ? "on" : "off"), Color.white);
        }, Config.doubleradius));
        wva.addRow(new CheckBox("Show Mine Support", val -> {
            Utils.setprefb("showminerad", Config.showminerad = val);
            msg("Mine support " + (val ? "on" : "off"), Color.white);
        }, Config.showminerad), new ColorPreview(UI.scale(20, 20), SUPPORTDANGERCOLOR.get(), val -> {
            BPRadSprite.smatSupports = new States.ColState(val);
            SUPPORTDANGERCOLOR.set(val);
            if (map != null) map.refreshGobsAll();
        }, "Radius mine radius color"), new ColorPreview(UI.scale(20, 20), MSRad.getColor1(), MSRad::changeColor1, "Vanilla mine radius first color"), new ColorPreview(UI.scale(20, 20), MSRad.getColor2(), MSRad::changeColor2, "Vanilla mine radius second color"));
        wva.addRow(new CheckBox("Show Trough Radius", val -> {
            Utils.setprefb("showTroughrad", Config.showTroughrad = val);
            msg("Troughs " + (val ? "on" : "off"), Color.white);
        }, Config.showTroughrad), new ColorPreview(UI.scale(20, 20), TROUGHCOLOR.get(), val -> {
            BPRadSprite.smatTrough = new States.ColState(val);
            TROUGHCOLOR.set(val);
            if (map != null) map.refreshGobsAll();
        }));
        wva.addRow(new CheckBox("Show Beehive Radius", val -> {
            Utils.setprefb("showBeehiverad", Config.showBeehiverad = val);
            msg("Beehives " + (val ? "on" : "off"), Color.white);
        }, Config.showBeehiverad), new ColorPreview(UI.scale(20, 20), BEEHIVECOLOR.get(), val -> {
            BPRadSprite.smatBeehive = new States.ColState(val);
            BEEHIVECOLOR.set(val);
            if (map != null) map.refreshGobsAll();
        }));
        wva.addRow(new CheckBox("Show Mound Bed Radius", val -> {
            Utils.setprefb("showMoundbedrad", Config.showMoundbedrad = val);
            msg("Mound Beds " + (val ? "on" : "off"), Color.white);
        }, Config.showMoundbedrad), new ColorPreview(UI.scale(20, 20), MOUNDBEDCOLOR.get(), val -> {
            BPRadSprite.smatMoundbed = new States.ColState(val);
            MOUNDBEDCOLOR.set(val);
            if (map != null) map.refreshGobsAll();
        }));
        wva.addRow(new CheckBox("Show Barter Hand Radius", val -> {
            Utils.setprefb("showBarterrad", Config.showBarterrad = val);
            msg("Barter Hands " + (val ? "on" : "off"), Color.white);
        }, Config.showBarterrad), new ColorPreview(UI.scale(20, 20), BARTERCOLOR.get(), val -> {
            BPRadSprite.smatBarter = new States.ColState(val);
            BARTERCOLOR.set(val);
            if (map != null) map.refreshGobsAll();
        }));
        wva.addRow(new CheckBox("Show player distance border", val -> Utils.setprefb("playerbordersprite", configuration.playerbordersprite = val), configuration.playerbordersprite), new ColorPreview(UI.scale(20, 20), new Color(configuration.playerbordercolor, true), val -> {
            configuration.playerbordercolor = val.hashCode();
            Utils.setprefi("playerbordercolor", val.hashCode());
        }));
        wva.addRow(new CheckBox("Show player distance box", val -> Utils.setprefb("playerboxsprite", configuration.playerboxsprite = val), configuration.playerboxsprite), new ColorPreview(UI.scale(20, 20), new Color(configuration.playerboxcolor, true), val -> {
            configuration.playerboxcolor = val.hashCode();
            Utils.setprefi("playerboxcolor", val.hashCode());
        }));
        wva.addRow(new CheckBox("Show grid box", val -> Utils.setprefb("gridboxsprite", configuration.gridboxsprite = val), configuration.gridboxsprite), new ColorPreview(UI.scale(20, 20), new Color(configuration.gridboxcolor, true), val -> {
            configuration.gridboxcolor = val.hashCode();
            Utils.setprefi("gridboxcolor", val.hashCode());
        }));
        wva.addRow(new CheckBox("Show color tiles", val -> {
            Utils.setprefb("showcolortiles", configuration.showcolortiles = val);
            if (ui.sess != null) {
                ui.sess.glob.map.invalidateAll();
            }
        }, configuration.showcolortiles), new Button("Configure", () -> {
            new Object() {
                Coord itemsz = UI.scale(180, 20);
                final JsonElement element = configuration.customTiles;

                Widget addNew(String name, WidgetList<Widget> list) {
                    Widget item = new Widget(itemsz);
                    WidgetVerticalAppender itemwva = new WidgetVerticalAppender(item);
                    itemwva.setHorizontalMargin(3);
                    itemwva.addRow(new CheckBox(name, val -> edit(name, val), false), new ColorPreview(UI.scale(20, 20), Color.WHITE, val -> edit(name, val)), new Button("X", () -> {
                        list.removeitem(item, true);
                        remove(name);
                    }));
                    return (item);
                }

                Widget loadWidget(String name, JsonObject obj, WidgetList<Widget> list) {
                    Widget item = new Widget(itemsz);
                    WidgetVerticalAppender itemwva = new WidgetVerticalAppender(item);
                    itemwva.setHorizontalMargin(3);
                    itemwva.addRow(new CheckBox(name, val -> edit(name, val), obj.get("a").getAsBoolean()), new ColorPreview(UI.scale(20, 20), new Color(obj.get("c").getAsInt(), true), val -> edit(name, val)), new Button("X", () -> {
                        list.removeitem(item, true);
                        remove(name);
                    }));
                    return (item);
                }

                List<Widget> load(WidgetList<Widget> list) {
                    JsonObject obj;
                    synchronized (element) {
                        obj = element.getAsJsonObject();
                    }
                    return (obj.keySet().stream().map(item -> loadWidget(item, obj.get(item).getAsJsonObject(), list)).collect(Collectors.toList()));
                }

                void createNew(String name) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("a", false);
                    obj.addProperty("c", Color.WHITE.getRGB());
                    synchronized (element) {
                        element.getAsJsonObject().add(name, obj);
                        save();
                    }
                }

                JsonObject object(String item) {
                    synchronized (element) {
                        JsonElement itemEl = element.getAsJsonObject().get(item);
                        if (itemEl != null) {
                            return (itemEl.getAsJsonObject());
                        }
                        return (null);
                    }
                }

                void save() {
                    synchronized (element) {
                        Utils.saveCustomElement(element, "CustomColorTiles");
                    }
                }

                void remove(String item) {
                    JsonElement itemEl = object(item);
                    if (itemEl != null) {
                        element.getAsJsonObject().remove(item);
                        save();
                        invalidate();
                    }
                }

                void edit(String item, boolean a) {
                    boolean yes = false;
                    synchronized (element) {
                        JsonElement itemEl = object(item);
                        if (itemEl != null) {
                            JsonObject itemObj = itemEl.getAsJsonObject();
                            boolean old = itemObj.get("a").getAsBoolean();
                            if (old != a) {
                                itemObj.addProperty("a", a);
                                save();
                                yes = true;
                            }
                        }
                    }
                    if (yes) invalidate();
                }

                void edit(String item, Color c) {
                    boolean yes = false;
                    synchronized (element) {
                        JsonElement itemEl = object(item);
                        if (itemEl != null) {
                            JsonObject itemObj = itemEl.getAsJsonObject();
                            int old = itemObj.get("c").getAsInt();
                            int n = c.getRGB();
                            if (old != n) {
                                itemObj.addProperty("c", n);
                                save();
                                yes = true;
                            }
                        }
                    }
                    if (yes) invalidate();
                }

                void invalidate() {
                    if (ui.sess != null) {
                        ui.sess.glob.map.invalidateAll();
                    }
                }

                {
                    Window w = new Window(Coord.z, "Tiles Highlighting Configure");
                    WidgetVerticalAppender wva = new WidgetVerticalAppender(w);
                    final WidgetList<Widget> list = new WidgetList<>(itemsz, 10);
                    load(list).forEach(list::additem);
                    final TextEntry value = new TextEntry(UI.scale(150), "") {
                        @Override
                        public void activate(String text) {
                            if (!text.isEmpty()) {
                                if (object(text) == null) {
                                    list.additem(addNew(text, list));
                                    createNew(text);
                                }
                                settext("");
                            }
                        }
                    };
                    wva.add(list);
                    wva.addRow(value, new Button("Add", () -> value.activate(value.text())));
                    w.pack();

                    ui.root.adda(w, ui.root.sz.div(2), 0.5, 0.5);
                }
            };
        }));
        Label text = new Label(configuration.radiusheight + "");
        wva.addRow(new Label("Height:"), new HSlider(100, 0, 5000, (int) (configuration.radiusheight * 100f)) {
            @Override
            public void changed() {
                Utils.setpreff("radiusheight", configuration.radiusheight = val / 100f);
                text.settext(configuration.radiusheight + "");
            }

            @Override
            public Object tooltip(final Coord c, final Widget prev) {
                return (configuration.radiusheight + "");
            }
        }, text);
        w.pack();

        adda(w, sz.div(2), 0.5, 0.5);
    }

    public void toggleStatusWidget() {
        if (Config.statuswdgvisible) {
            if (statuswindow != null)
                statuswindow.reqdestroy();
            Config.statuswdgvisible = false;
            Utils.setprefb("statuswdgvisible", false);
        } else {
            statuswindow = new StatusWdg();
            add(statuswindow, umpanel.c.add(umpanel.sz.x + UI.scale(10), UI.scale(10)));
            Config.statuswdgvisible = true;
            Utils.setprefb("statuswdgvisible", true);
        }
    }

    void toggleDeleted() {
        if (deleted != null && deleted.show(!deleted.visible())) {
            deleted.raise();
            fitwdg(deleted);
            setfocus(deleted);
        }
    }

    public void toggleHide() {
        Config.hidegobs = !Config.hidegobs;
        Utils.setprefb("hidegobs", Config.hidegobs);
        if (map != null)
            map.refreshGobsAll();
        if (Config.hidegobs)
            msg("Gobs are now hidden.", Color.white);
        else
            msg("Gobs are now NOT hidden.", Color.white);
    }

    public void toggleHiddenGobs() {
        Config.hideuniquegobs = !Config.hideuniquegobs;
        Utils.setprefb("hideuniquegobs", Config.hideuniquegobs);
        if (map != null)
            map.refreshGobsAll();
        if (Config.hideuniquegobs)
            msg("Unique gobs are now hidden.", Color.white);
        else
            msg("Unique gobs are now NOT hidden.", Color.white);
    }

    public void toggleGridCentering() {
        Config.tilecenter = !Config.tilecenter;
        Utils.setprefb("tilecenter", Config.tilecenter);
        msg("Tile centering is now turned " + (Config.tilecenter ? "on." : "off."), Color.WHITE);
    }


    public void togglePathfinding() {
        boolean p1 = Config.pf;
        boolean p2 = Config.pf2;
        if (p1) {
            Config.pf = false;
            Config.pf2 = true;
        } else if (p2) {
            Config.pf2 = false;
        } else {
            Config.pf = true;
        }
        msg("Pathfinding is now turned " + (Config.pf || Config.pf2 ? "on " : "off") + (Config.pf ? "purus" : (Config.pf2 ? "sloth" : "")), Color.WHITE);
    }

    public void aggroClosest() {
        if (map != null)
            map.aggroclosest();
    }

    public void attack() {
        try {
            this.act("aggro");
        } catch (Exception e) {}
    }

    public void shoot() {
        try {
            this.act("shoot");
        } catch (Exception e) {}
    }

    public void push() {
        try {
            this.act("push");
        } catch (Exception e) {}
    }


    public void rightHand() {
        Equipory e = equipory;
        if (configuration.newQuickSlotWdg) {
            newquickslots.drop(7);
            newquickslots.simulateclick(7);
        } else {
            quickslots.drop(QuickSlotsWdg.lc, Coord.z);
            quickslots.simulateclick(QuickSlotsWdg.lc);
        }
    }

    public void changeDecks(int deck) {
        FightWnd fightwdg = ui.fightwnd.get();
        if (fightwdg != null)
            fightwdg.changebutton(deck);
    }

    public void leftHand() {
        if (configuration.newQuickSlotWdg) {
            newquickslots.drop(6);
            newquickslots.simulateclick(6);
        } else {
            quickslots.drop(QuickSlotsWdg.rc, Coord.z);
            quickslots.simulateclick(QuickSlotsWdg.rc);
        }
    }

    public void Drink() {
        new Thread(new DrinkWater(this)).start();
    }


    public void crawlSpeed() {
        if (speed != null)
            speed.set(0);
    }

    public void walkSpeed() {
        if (speed != null)
            speed.set(1);
    }

    public void runSpeed() {
        if (speed != null)
            speed.set(2);
    }

    public void sprintSpeed() {
        if (speed != null)
            speed.set(3);
    }

    public void cycleSpeed() {
        if (speed != null) {
            if (speed.max >= 0) {
                int n;
                if (speed.cur > speed.max)
                    n = 0;
                else
                    n = (speed.cur + 1) % (speed.max + 1);
                speed.set(n);
            }
        }
    }

    public void fixAlarms() { //this is to fix me being a retard and relabeling previously boolean values as strings
        /*if (Config.alarmunknownplayer.toLowerCase().equals("true") || Config.alarmunknownplayer.toLowerCase().equals("false")) {
            Utils.setpref("alarmunknownplayer", "sfx/OhShitItsAGuy");
            Config.alarmunknownplayer = "sfx/OhShitItsAGuy";
        }
        if (Config.alarmredplayer.toLowerCase().equals("true") || Config.alarmredplayer.toLowerCase().equals("false")) {
            Utils.setpref("alarmredplayer", "sfx/Siren");
            Config.alarmredplayer = "sfx/Siren";
        }
        if (Config.alarmstudy.toLowerCase().equals("true") || Config.alarmstudy.toLowerCase().equals("false")) {
            Utils.setpref("alarmstudy", "sfx/Study");
            Config.alarmstudy = "sfx/Study";
        }
        if (Config.cleavesfx.toLowerCase().equals("true") || Config.cleavesfx.toLowerCase().equals("false")) {
            Utils.setpref("cleavesfx", "sfx/oof");
            Config.cleavesfx = "sfx/oof";
        }*/
    }

    public void toggleres() {
        Config.resinfo = !Config.resinfo;
        Utils.setprefb("resinfo", Config.resinfo);
        map.tooltip = null;
        msg("Resource info on shift/shift+ctrl is now turned " + (Config.resinfo ? "on" : "off"), Color.WHITE);
    }

    @Override
    public boolean globtype(char key, KeyEvent ev) {
        if (key == ':') {
            entercmd();
            return true;
        } else if ((key == 27) && (map != null) && !map.hasfocus) {
            setfocus(map);
            return true;
        } else if (chatfocused()) {
            return true;
        } else {
            return (KeyBinder.handle(ui, ev)) || (super.globtype(key, ev));
        }
    }

    public boolean chatfocused() {
        boolean isfocused = false;
        for (Widget w = chat.lchild; w != null; w = w.prev) {
            if (w instanceof TextEntry)
                if (w.hasfocus)
                    isfocused = true;
        }

        return isfocused;
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        return (super.mousedown(c, button));
    }

    @Override
    public boolean mousewheel(Coord c, int amount) {
        if (fs != null && fv != null && ui.modflags() == UI.MOD_CTRL) {
            fv.scroll(amount);
            return (true);
        }
        return (super.mousewheel(c, amount));
    }

    public DowseWnd makeDowseWnd(final Coord2d startc, final double a1, final double a2, final Consumer<Color> changeCol, final Runnable onClose) {
        DowseWnd dwnd = new DowseWnd(startc, a1, a2, changeCol, onClose);
        dowsewnds.add(add(dwnd));
        return (dwnd);
    }

    public void remDowseWnd(final DowseWnd wnd) {
        dowsewnds.removeIf(wdg -> wdg == wnd);
    }

    @Override
    public void resize(Coord sz) {
        super.resize(sz);
        if (map != null)
            map.resize(sz);
        if (statuswindow != null)
            statuswindow.c = umpanel.c.add(umpanel.sz.x + UI.scale(10), UI.scale(10));
    }

    @Override
    public void presize() {
        resize(parent.sz);
    }

    public void msg(String msg, Color color, Color logcol, Audio.CS sfx) {
        /*if (msg.startsWith(charterMsg)) //TravelWnd
            CharterList.addCharter(msg.substring(charterMsg.length(), msg.length() - 2));*/
        if (msg.startsWith("Swimming is now turned")) {
            togglebuff(msg, Bufflist.buffswim);
            if (swimautotgld) {
                //msgnosfx(msg);
                swimautotgld = false;
                //return;
            }
        } else if (msg.startsWith("Tracking is now turned")) {
            togglebuff(msg, Bufflist.bufftrack);
            if (trackautotgld) {
                //msgnosfx(msg);
                trackautotgld = false;
                //return;
            }
        } else if (msg.startsWith("Criminal acts are now turned")) {
            togglebuff(msg, Bufflist.buffcrime);
            if (crimeautotgld) {
                //msgnosfx(msg);
                crimeautotgld = false;
                //return;
            }
        } else if (msg.startsWith("Party permissions are now")) {
            togglebuff(msg, Bufflist.partyperm);
        }

        msgtime = Utils.rtime();
        if (Config.temporaryswimming && msg.equals("Swimming is now turned on.")) { //grab it here before we localize the message
            temporarilyswimming = true;
            SwimTimer = System.currentTimeMillis();
        }
        if (Config.temporaryswimming && temporarilyswimming && msg.equals("Swimming is now turned off.")) {//swimming manually toggled back off before the auto-off trigger fired, reset the auto-toggle flags.
            temporarilyswimming = false;
            SwimTimer = 0;
        }
        msg = Resource.getLocString(Resource.BUNDLE_MSG, msg);
        lastmsg = msgfoundry.render(msg, color);
        syslog.append(msg, logcol);
        if (sfx != null && color == Color.WHITE)
            Audio.play(sfx);
    }

    public void msg(String msg, Color color, Color logcol) {
        msg(msg, color, logcol, (color == Color.WHITE) ? Audio.fromres(msgsfx) : UI.InfoMessage.nosfx);
    }

    public void msg(String msg, Color color) {
        msg(msg, color, color);
    }

    public void debugmsg(String msg, Color color, Color logcol) {
        msgtime = Utils.rtime();
        lastmsg = msgfoundry.render(msg, color);
        debuglog.append(msg, logcol);
        Audio.play(msgsfx);
    }

    public void debugmsg(String msg, Color color) {
        debugmsg(msg, color, color);
    }

    private static final Resource errsfx = Resource.remote().loadwait("sfx/error");
    private static final Resource msgsfx = Resource.remote().loadwait("sfx/msg");

    private double lasterrsfx = 0;

    @Override
    public void error(String msg) {
        msg(msg, DefSettings.ERRORTEXTCOLOR.get(), new Color(255, 0, 0));
        if (errmsgcb != null)
            errmsgcb.notifyErrMsg(msg);
        double now = Utils.rtime();
        if (now - lasterrsfx > 0.1 && Config.errorsounds) {
            Audio.play(errsfx);
            lasterrsfx = now;
        }

    }

    public void msgnosfx(String msg) {
        msg(msg, new Color(255, 255, 254), Color.WHITE);
    }

    private static final String charterMsg = "The name of this charterstone is \"";

    @Override
    public void msg(String msg) {
        msg(msg, Color.WHITE, Color.WHITE);
    }

    @Override
    public void msg(UI.Notice msg) {
        msg(msg.message(), msg.color(), msg.color(), msg.sfx());
    }

    public void act(String... args) {
        wdgmsg("act", (Object[]) args);
    }

    public void act(int mods, Coord mc, Gob gob, String... args) {
        int n = args.length;
        Object[] al = new Object[n];
        System.arraycopy(args, 0, al, 0, n);
        if (mc != null) {
            al = Utils.extend(al, al.length + 2);
            al[n++] = mods;
            al[n++] = mc;
            if (gob != null) {
                al = Utils.extend(al, al.length + 2);
                al[n++] = (int) gob.id;
                al[n++] = gob.rc;
            }
        }
        wdgmsg("act", al);
    }

    public Window getwnd(String cap) {
        for (Widget w = lchild; w != null; w = w.prev) {
            if (w instanceof Window) {
                Window wnd = (Window) w;
                if (wnd.cap != null && cap.equals(wnd.origcap))
                    return wnd;
            }
        }
        return null;
    }

    public Window getwnd(Window wnd) {
        for (Widget w = lchild; w != null; w = w.prev) {
            if (w instanceof Window) {
                if (w.equals(wnd))
                    return (Window) w;
            }
        }
        return null;
    }


    private static final int WND_WAIT_SLEEP = 8;

    public Window waitfForWnd(String cap, int timeout) {
        int t = 0;
        while (t < timeout) {
            Window wnd = getwnd(cap);
            if (wnd != null)
                return wnd;
            t += WND_WAIT_SLEEP;
            try {
                Thread.sleep(WND_WAIT_SLEEP);
            } catch (InterruptedException e) {
                return null;
            }
        }
        return null;
    }

    public List<IMeter.Meter> getmeters(String name) {
        for (Widget meter : meters) {
            if (meter instanceof IMeter) {
                IMeter im = (IMeter) meter;
                try {
                    Resource res = im.bg.get();
                    if (res != null && res.basename().equals(name))
                        return im.meters;
                } catch (Loading l) {
                }
            }
        }
        return null;
    }

    public IMeter.Meter getmeter(String name, int midx) {
        List<IMeter.Meter> meters = getmeters(name);
        if (meters != null && midx < meters.size())
            return meters.get(midx);
        return null;
    }

    public Equipory getequipory() {
        if (equwnd != null) {
            for (Widget w = equwnd.lchild; w != null; w = w.prev) {
                if (w instanceof Equipory)
                    return (Equipory) w;
            }
        }
        return null;
    }

    private static final Tex nkeybg = Resource.loadtex("gfx/hud/hb-main");

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();

    {
        cmdmap.put("afk", (cons, args) -> {
            afk = true;
            wdgmsg("afk");
        });
        cmdmap.put("act", (cons, args) -> {
            Object[] ad = new Object[args.length - 1];
            System.arraycopy(args, 1, ad, 0, ad.length);
            wdgmsg("act", ad);
        });
        cmdmap.put("chrmap", new Console.Command() {
            @Override
            public void run(Console cons, String[] args) {
                Utils.setpref("mapfile/" + chrid, args[1]);
            }
        });
        cmdmap.put("tool", (cons, args) -> add(gettype(args[1]).create(ui, new Object[0]), 200, 200));
        cmdmap.put("help", (cons, args) -> {
            Debug.println("Available console commands:");
            cons.findcmds().forEach((s, cmd) -> Debug.println(s));
        });
        cmdmap.put("savemap", (cons, args) -> {
            new Thread(() -> mapfile.view.dumpTiles(), "MapDumper").start();
        });
        cmdmap.put("baseq", (cons, args) -> {
            FoodInfo.showbaseq = Utils.parsebool(args[1]);
            msg("q10 FEP values in tooltips are now " + (FoodInfo.showbaseq ? "enabled" : "disabled"));
        });
    }

    public void registerItemCallback(ItemClickCallback itemClickCallback) {
        this.itemClickCallback = itemClickCallback;
    }

    public void unregisterItemCallback() {
        this.itemClickCallback = null;
    }

    public void registerPetalCallback(PetalClickCallback petalClickCallback) {
        this.petalClickCallback = petalClickCallback;
    }

    public void unregisterPetalCallback() {
        this.petalClickCallback = null;
    }

    @Override
    public Map<String, Console.Command> findcmds() {
        return (cmdmap);
    }

    public void registerErrMsg(ErrorSysMsgCallback callback) {
        this.errmsgcb = callback;
    }
}
