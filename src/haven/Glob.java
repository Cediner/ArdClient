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

//import haven.sloth.script.pathfinding.GobHitmap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import haven.res.gfx.fx.floatimg.DamageText;
import haven.sloth.script.pathfinding.GobHitmap;
import modification.configuration;
import modification.dev;
import org.apache.commons.collections4.list.TreeList;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static haven.MCache.tilesz;

public class Glob {
    //TODO: Glob should honestly make the ui, not have the UI attach onto it.
    public WeakReference<UI> ui;
    public final GobHitmap gobhitmap;
    @Deprecated
    public static final double SERVER_TIME_RATIO = 3.29d;
    public double gtime;
    public double sgtime;
    public double epoch = Utils.rtime();
    public final Loader loader = new Loader();
    public Astronomy ast;
    public OCache oc = new OCache(this);
    public MCache map;
    public Session sess;
    public Party party;
    public Color lightamb = null, lightdif = null, lightspc = null;
    public Color olightamb = null, olightdif = null, olightspc = null;
    public Color tlightamb = null, tlightdif = null, tlightspc = null;
    public static Color dlightamb = new Color(200, 200, 200);
    public static Color dlightdif = new Color(200, 200, 200);
    public static Color dlightspc = new Color(255, 255, 255);
    public double lightang = 0.0, lightelev = 0.0;
    public double olightang = 0.0, olightelev = 0.0;
    public double tlightang = 0.0, tlightelev = 0.0;
    public long lchange = -1;
    public Indir<Resource> sky1 = null, sky2 = null;
    public double skyblend = 0.0;
    public final Map<String, CAttr> cattr = new HashMap<>();
    private Map<Indir<Resource>, Object> wmap = new HashMap<>();
    //    public static haven.timers.TimersThread timersThread;
    public String servertime;
    public Tex servertimetex;
    public int moonid = 0;
    public String tservertime;
    public String dservertime;
    public String yservertime;
    public String mservertime;
    public String bservertime;
    public final List<String> weatherinfo = Collections.synchronizedList(new ArrayList<>());
    public final AtomicReference<Text> tservertimetex = new AtomicReference<>(null);
    public final AtomicReference<Text> dservertimetex = new AtomicReference<>(null);
    public final AtomicReference<Text> yservertimetex = new AtomicReference<>(null);
    public final AtomicReference<Text> mservertimetex = new AtomicReference<>(null);
    public final AtomicReference<Text> bservertimetex = new AtomicReference<>(null);
    public final AtomicReference<Text> weathertimetex = new AtomicReference<>(null);
    public final Map<Long, DamageText.Numbers> gobmap = new ConcurrentHashMap<>();

    private static Map<String, Glob> reference = new HashMap<>();

    public static Glob getByReference(String name) {
        return reference.get(name);
    }

    public Glob(Session sess) {
        this.sess = sess;
        gobhitmap = new GobHitmap();
        map = new MCache(sess);
        party = new Party(this);
    }

    public void addReference(String username) {
        reference.put(username, this);
    }

    public void removeReference(String username) {
        reference.remove(username, this);
    }

    @Resource.PublishedCode(name = "wtr")
    public interface Weather {
        void gsetup(RenderList rl);

        void update(Object... args);

        boolean tick(int dt);
    }

    public static class CAttr extends Observable {
        public static final Text.Foundry fnd = new Text.Foundry(Text.sans, UI.scale(12));
        public final Glob glob;
        public final String nm;
        public int base, comp;
        public ItemInfo.Raw info;
        private Text.Line compLine = null;
        public Tex comptex;

        public CAttr(Glob glob, String nm, int base, int comp, ItemInfo.Raw info) {
            this.glob = glob;
            this.nm = nm.intern();
            this.base = base;
            this.comp = comp;
            this.info = info;
            compLine = null;
            this.comptex = Text.renderstroked(comp + "", Color.WHITE, Color.BLACK, Text.num12boldFnd).tex();
        }

        public void update(int base, int comp, ItemInfo.Raw info) {
            if ((base == this.base) && (comp == this.comp))
                return;
            this.base = base;
            this.comp = comp;
            this.info = info;
            compLine = null;
            setChanged();
            notifyObservers(null);
            this.comptex = Text.renderstroked(comp + "", Color.WHITE, Color.BLACK, Text.num12boldFnd).tex();
        }

        public Text.Line compline() {
            if (compLine == null) {
                Color c = Color.WHITE;
                if (comp > base) {
                    c = CharWnd.buff;
                } else if (comp < base) {
                    c = CharWnd.debuff;
                }
                compLine = Text.renderstroked(Integer.toString(comp), c, Color.BLACK, fnd);
            }
            return compLine;
        }

        public Indir<Resource> res() {
            return (Resource.local().load("gfx/hud/chr/" + nm));
        }
    }

    private static Color colstep(Color o, Color t, double a) {
        int or = o.getRed(), og = o.getGreen(), ob = o.getBlue(), oa = o.getAlpha();
        int tr = t.getRed(), tg = t.getGreen(), tb = t.getBlue(), ta = t.getAlpha();
        return (new Color(or + (int) ((tr - or) * a),
                og + (int) ((tg - og) * a),
                ob + (int) ((tb - ob) * a),
                oa + (int) ((ta - oa) * a)));
    }

    private void ticklight(int dt) {
        if (lchange >= 0) {
            lchange += dt;
            if (lchange > 2000) {
                lchange = -1;
                lightamb = tlightamb;
                lightdif = tlightdif;
                lightspc = tlightspc;
                lightang = tlightang;
                lightelev = tlightelev;
            } else {
                double a = lchange / 2000.0;
                lightamb = colstep(olightamb, tlightamb, a);
                lightdif = colstep(olightdif, tlightdif, a);
                lightspc = colstep(olightspc, tlightspc, a);
                lightang = olightang + a * Utils.cangle(tlightang - olightang);
                lightelev = olightelev + a * Utils.cangle(tlightelev - olightelev);
            }
        }
    }

    private long lastctick = 0;

    public void ctick() {
        long now = System.currentTimeMillis();
        int dt;
        if (lastctick == 0)
            dt = 0;
        else
            dt = (int) (now - lastctick);
        dt = Math.max(dt, 0);

        synchronized (this) {
            ticklight(dt);
            for (Object o : wmap.values()) {
                if (o instanceof Weather)
                    ((Weather) o).tick(dt);
            }
        }

        tickgtime(now, dt);
        oc.ctick(dt);
        map.ctick(dt);

        if (Config.showservertime)
            servertimecalc();

        lastctick = now;
    }

    private static final double itimefac = 3.0;
    private double stimefac = itimefac, ctimefac = itimefac;

    private void tickgtime(double now, double dt) {
        double sgtime = this.sgtime + ((now - epoch) * stimefac);
        gtime += dt * ctimefac;
        if ((sgtime > gtime) && (ctimefac / stimefac < 1.1))
            ctimefac += Math.min((sgtime - gtime) * 0.001, 0.02) * dt;
        else if ((sgtime < gtime) && (stimefac / ctimefac < 1.1))
            ctimefac -= Math.min((gtime - sgtime) * 0.001, 0.02) * dt;
        ctimefac += Math.signum(stimefac - ctimefac) * 0.002 * dt;
    }

    public double getTimeFac() {
        return (stimefac);
    }

    private void updgtime(double sgtime, boolean inc) {
        double now = Utils.rtime();
        double delta = now - epoch;
        epoch = now;
        if ((this.sgtime == 0) || !inc || (Math.abs(sgtime - this.sgtime) > 500)) {
            this.gtime = this.sgtime = sgtime;
            return;
        }
        if ((sgtime - this.sgtime) > 1) {
            double utimefac = (sgtime - this.sgtime) / delta;
            double f = Math.min(delta * 0.01, 0.5);
            stimefac = (stimefac * (1 - f)) + (utimefac * f);
        }
        this.sgtime = sgtime;
    }

    public String gtimestats() {
        double sgtime = this.sgtime + ((Utils.rtime() - epoch) * stimefac);
        return (String.format("%.2f %.2f %.2f %.2f %.2f %.2f %.2f", gtime, this.sgtime, epoch, sgtime, sgtime - gtime, ctimefac, stimefac));
    }

    public double globtime() {
        return (this.sgtime + ((Utils.rtime() - epoch) * stimefac));
    }

    public double newglobtime() {
        return (gtime);
    }

    private static final long secinday = 60 * 60 * 24;
    private static final long dewyladysmantletimemin = 4 * 60 * 60 + 45 * 60;
    private static final long dewyladysmantletimemax = 7 * 60 * 60 + 15 * 60;
    private static final String[] seasonNames = {"Spring", "Summer", "Autumn", "Winter"};
    private static final String[] mPhaseNames = {
            "New",
            "Waxing Crescent",
            "First Quarter",
            "Waxing Gibbous",
            "Full",
            "Waning Gibbous",
            "Last Quarter",
            "Waning Crescent"
    };

    private void servertimecalc1() {
        Astronomy ast = this.ast;
        if (ast == null) return;

        long secs = (long) globtime();
        long day = secs / secinday;
        long secintoday = secs % secinday;
        long hours = secintoday / 3600;
        long mins = (secintoday % 3600) / 60;
        int nextseason = (int) Math.ceil((1 - ast.sp) * (ast.is == 1 ? 30 : ast.is == 3 ? 5 : 10));

        String fmt;
        switch (ast.is) {
            case 0:
                fmt = nextseason == 1 ? "Day %d, %02d:%02d. Spring (%d RL day left)." : "Day %d, %02d:%02d. Spring (%d RL days left).";
                break;
            case 1:
                fmt = nextseason == 1 ? "Day %d, %02d:%02d. Summer (%d RL day left)." : "Day %d, %02d:%02d. Summer (%d RL days left).";
                break;
            case 2:
                fmt = nextseason == 1 ? "Day %d, %02d:%02d. Autumn (%d RL day left)." : "Day %d, %02d:%02d. Autumn (%d RL days left).";
                break;
            case 3:
                fmt = nextseason == 1 ? "Day %d, %02d:%02d. Winter (%d RL day left)." : "Day %d, %02d:%02d. Winter (%d RL days left).";
                break;
            default:
                fmt = "Unknown Season";
        }

        servertime = String.format(Resource.getLocString(Resource.BUNDLE_LABEL, fmt), day, hours, mins, nextseason);

        if (secintoday >= dewyladysmantletimemin && secintoday <= dewyladysmantletimemax)
            servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Dewy Lady's Mantle)");
        if (ast.night) {
            if (moonid == 128)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (New Moon)");
            else if (moonid == 129)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Waxing Crescent)");
            else if (moonid == 130)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (First Quarter)");
            else if (moonid == 131)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Waxing Gibbous)");
            else if (moonid == 132)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Full Moon)");
            else if (moonid == 133)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Waning Gibbous)");
            else if (moonid == 134)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Last Quarter)");
            else if (moonid == 135)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Waning Crescent)");
        } else
            servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Daytime)");
        servertimetex = Text.render(servertime).tex();
    }

    private void servertimecalc() {
        long secs = (long) (globtime());
        long day = (secs / secinday) + 1;
        long secintoday = secs % secinday;
        long hours = secintoday / 3600;
        long mins = (secintoday % 3600) / 60;
        long seconds = secintoday % 60;

        String dayOfMonth = "";
        String season = "";
        String month = "";
        String year = "";
        String phaseOfMoon = " ";
        String dayTime = "";
        Astronomy ast = this.ast;
        if (ast != null) {
//            int nextseason = (int) Math.ceil((1 - ast.sp) * (ast.is == 1 ? 35 : ast.is == 3 ? 5 : 10));

//            int sdt = (ast.is == 1 ? 105 : ast.is == 3 ? 15 : 30); //days of season total
//            int sdp = (int) (ast.sp * (sdt)); //days of season passed
            int sdt = (int) (Math.round(ast.md / (ast.ym % 1))); //days of season total
            int sdp = (int) (Math.floor(ast.md) + 1); //days of season passed
//            int sdl = (int) Math.floor((1 - ast.sp) * (sdt));
//            if (sdl >= 1)
//                dayOfMonth = Resource.getLocString(Resource.BUNDLE_LABEL, seasonNames[ast.is]) + String.format(" %d (%d ", (sdp + 1), sdl) + Resource.getLocString(Resource.BUNDLE_LABEL, "left") + String.format(" (%d RL))", nextseason);
            season = Resource.getLocString(Resource.BUNDLE_LABEL, seasonNames[ast.is]) + " " + String.format("(%s%%)", Math.round(ast.sp * 100));
            dayOfMonth = String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Day") + " %d/%d", sdp, sdt);
//            else
//                dayOfMonth = Resource.getLocString(Resource.BUNDLE_LABEL, String.format("Last day of %s", seasonNames[ast.is]));
            int mp = (int) Math.round(ast.mp * mPhaseNames.length) % mPhaseNames.length;
            phaseOfMoon = String.format("$col[%s,%s,%s]{%s}", ast.mc.getRed(), ast.mc.getGreen(), ast.mc.getBlue(), Resource.getLocString(Resource.BUNDLE_LABEL, mPhaseNames[mp] + " Moon"));
            month = Resource.getLocString(Resource.BUNDLE_LABEL, "Month") + " " + String.format("%s/%s", (int) (Math.floor(ast.ym) + 1), Math.round(ast.ym / ast.yt));
            year = Resource.getLocString(Resource.BUNDLE_LABEL, "Year") + " " + String.format("%s (%s%%)", ((int) Math.floor(ast.years) + 1), Math.round(ast.yt * 100));
            dayTime = Resource.getLocString(Resource.BUNDLE_LABEL, ast.dt >= 0.2 && ast.dt < 0.3 ? "Dawn" : (ast.night ? "Night" : "Day"));
        }

        tservertime = String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Time") + " %02d:%02d:%02d", hours, mins, seconds) + (dayTime.isEmpty() ? "" : ", " + dayTime);
        dservertime = String.format("%s (%s). %s", dayOfMonth, day, month);
        yservertime = String.format("%s. %s", season, year);
        mservertime = phaseOfMoon;
        if (secintoday >= dewyladysmantletimemin && secintoday <= dewyladysmantletimemax)
            bservertime = Resource.getLocString(Resource.BUNDLE_LABEL, "(Dewy Lady's Mantle)");
        /*
        if(night) {
            if (moonid == 128)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (New Moon)");
            else if (moonid == 129)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Waxing Crescent)");
            else if (moonid == 130)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (First Quarter)");
            else if (moonid == 131)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Waxing Gibbous)");
            else if (moonid == 132)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Full Moon)");
            else if (moonid == 133)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Waning Gibbous)");
            else if (moonid == 134)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Last Quarter)");
            else if (moonid == 135)
                servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Waning Crescent)");
        }else
            servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Daytime)");
        */
        infoUpdate(tservertimetex, tservertime);
        infoUpdate(dservertimetex, dservertime);
        infoUpdate(yservertimetex, yservertime);
        infoUpdate(mservertimetex, mservertime);
        infoUpdate(bservertimetex, bservertime);

        if (configuration.showweatherinfo) {
            List<String> tempw;
            synchronized (weatherinfo) {
                tempw = new ArrayList<>(weatherinfo);
            }

            String text = String.join("", tempw.toArray(new String[0]));
            infoUpdate(weathertimetex, text, () -> Text.create(text, ItemInfo.catimgs(0, true, tempw.stream().map(in -> PUtils.strokeImg(Text.render(in))).toArray(BufferedImage[]::new))));
        }
    }

    private void infoUpdate(AtomicReference<Text> t, String text) {
        infoUpdate(t, text, () -> Text.create(text, PUtils.strokeImg(RichText.render(text, -1))));
    }

    private void infoUpdate(AtomicReference<Text> t, String text, Supplier<Text> getter) {
        if (text != null && (t.get() == null || !t.get().text.equals(text)))
            t.set(text.isEmpty() ? null : getter.get());
    }

    public void blob(Message msg) {
        boolean inc = msg.uint8() != 0;
        while (!msg.eom()) {
            String t = msg.string().intern();
            Object[] a = msg.list();
            int n = 0;
            if (t == "tm") {
                updgtime(((Number) a[n++]).doubleValue(), inc);
            } else if (t == "astro") {
                double dt = ((Number) a[n++]).doubleValue();//current day time [0, 1]
                double mp = ((Number) a[n++]).doubleValue();//current moon phase [0, 1]
                double yt = ((Number) a[n++]).doubleValue();//current year time [0, 1]
                boolean night = (Integer) a[n++] != 0;//night sprite
                Color mc = (Color) a[n++];//moon color
                int is = (n < a.length) ? ((Number) a[n++]).intValue() : 1;//season {0, 1, 2, 3}
                double sp = (n < a.length) ? ((Number) a[n++]).doubleValue() : 0.5;//season period [0, 1]
                double sd = (n < a.length) ? ((Number) a[n++]).doubleValue() : 0.5;//season days [1, 0]???
                double years = (n < a.length) ? ((Number) a[n++]).doubleValue() : 0.5;//current year [0, ->]
                double ym = (n < a.length) ? ((Number) a[n++]).doubleValue() : 0.5;//current month in year [0, max month]
                double md = (n < a.length) ? ((Number) a[n++]).doubleValue() : 0.5;//current day in month [0, max days]
                ast = new Astronomy(dt, mp, yt, night, mc, is, sp, sd, years, ym, md);
                //dev.simpleLog(String.format("Glob Astronomy: day:%s moon:%s year:%s night:%s mc:%s season:%s sp:%s sd:%s years:%s ym:%s md:%s", dt, mp, yt, night, mc, is, sp, sd, years, ym, md));
                //Glob Astronomy: day:0.21407407522201538 moon:0.9404691457748413 year:0.8234115242958069 night:true mc:java.awt.Color[r=175,g=225,b=250] season:2 sp:0.4404691457748413 sd:0.9654287695884705 years:7.823411464691162 ym:4.940469264984131 md:28.214073181152344
                //Glob Astronomy: day:0.700960636138916 moon:0.023365354165434837 year:0.8372275829315186 night:false mc:java.awt.Color[r=175,g=225,b=250] season:2 sp:0.5233653783798218 sd:0.9946214556694031 years:7.8372273445129395 ym:5.023365497589111 md:0.700960636138916
                //Glob Astronomy: day:0.38636574149131775 moon:0.14621219038963318 year:0.8577020168304443 night:false mc:java.awt.Color[r=175,g=225,b=250] season:2 sp:0.6462122201919556 sd:0.8034355640411377 years:7.857702255249023 ym:5.146212100982666 md:4.38636589050293
            } else if (t == "light") {
                synchronized (this) {
                    tlightamb = (Color) a[n++];
                    tlightdif = (Color) a[n++];
                    tlightspc = (Color) a[n++];
                    tlightang = ((Number) a[n++]).doubleValue();
                    tlightelev = ((Number) a[n++]).doubleValue();
                    if (inc) {
                        olightamb = lightamb;
                        olightdif = lightdif;
                        olightspc = lightspc;
                        olightang = lightang;
                        olightelev = lightelev;
                        lchange = 0;
                    } else {
                        lightamb = tlightamb;
                        lightdif = tlightdif;
                        lightspc = tlightspc;
                        lightang = tlightang;
                        lightelev = tlightelev;
                        lchange = -1;
                    }
                }
            } else if (t == "sky") {
                synchronized (this) {
                    if (a.length < 1) {
                        sky1 = sky2 = null;
                        skyblend = 0.0;
                    } else {
                        sky1 = sess.getres(((Number) a[n++]).intValue());
                        if (a.length < 2) {
                            sky2 = null;
                            skyblend = 0.0;
                        } else {
                            sky2 = sess.getres(((Number) a[n++]).intValue());
                            skyblend = ((Number) a[n++]).doubleValue();
                        }
                    }
                }
            } else if (t == "wth") {
                synchronized (this) {
                    if (!inc)
                        wmap.clear();
                    Collection<Object> old = new LinkedList<Object>(wmap.keySet());
                    List<String> tempw = new ArrayList<>();
                    while (n < a.length) {
                        Indir<Resource> res = sess.getres(((Number) a[n++]).intValue());
                        Object[] args = (Object[]) a[n++];
                        Object curv = wmap.get(res);
                        tempw.add(resToHuman(res.toString()) + " " + Arrays.deepToString(args));
                        if (curv instanceof Weather) {
                            Weather cur = (Weather) curv;
                            cur.update(args);
                        } else {
                            wmap.put(res, args);
                        }
                        old.remove(res);
                    }
                    synchronized (weatherinfo) {
                        weatherinfo.clear();
                        weatherinfo.addAll(tempw);
                    }
                    for (Object p : old)
                        wmap.remove(p);
                }
            } else {
                System.err.println("Unknown globlob type: " + t);
            }
        }
    }

    private String resToHuman(final String text) {
        if (text.matches(".*gfx/fx/rain.*")) return ("Rainy");
        if (text.matches(".*gfx/fx/wet.*")) return ("Wet");
        if (text.matches(".*gfx/fx/snow.*")) return ("Snowy");
        if (text.matches(".*gfx/fx/seasonmap.*")) return ("Season Effect");
        if (text.matches(".*gfx/fx/clouds.*")) return ("Cloudy");
        return (text);
    }

    public final Iterable<Weather> weather = new Iterable<Weather>() {
        public Iterator<Weather> iterator() {
            return (new Iterator<Weather>() {
                Iterator<Map.Entry<Indir<Resource>, Object>> bk = wmap.entrySet().iterator();
                Weather n = null;

                public boolean hasNext() {
                    if (n == null) {
                        while (true) {
                            if (!bk.hasNext())
                                return (false);
                            Map.Entry<Indir<Resource>, Object> cur = bk.next();
                            Indir<Resource> ir = cur.getKey();
                            Object v = cur.getValue();
                            if (v instanceof Weather) {
                                n = (Weather) v;
                                break;
                            }
                            Resource r = ir.get();
                            if (r instanceof Resource.FakeResource) continue;
                            Weather w;
                            try {
                                Class<? extends Weather> cl = r.layer(Resource.CodeEntry.class).getcl(Weather.class);
                                w = Utils.construct(cl.getConstructor(Object[].class), new Object[]{v});
                            } catch (Loading e) {
                                throw (e);
                            } catch (Throwable e) {
                                dev.simpleLog(e);
                                w = new Weather() {
                                    @Override
                                    public void gsetup(final RenderList rl) {}

                                    @Override
                                    public void update(final Object... args) {}

                                    @Override
                                    public boolean tick(final int dt) {
                                        return (false);
                                    }
                                };
                            }
                            cur.setValue(n = w);
                        }
                    }
                    return (true);
                }

                public Weather next() {
                    if (!hasNext())
                        throw (new NoSuchElementException());
                    Weather ret = n;
                    n = null;
                    return (ret);
                }

                public void remove() {
                    throw (new UnsupportedOperationException());
                }
            });
        }
    };

    /* XXX: This is actually quite ugly and there should be a better
     * way, but until I can think of such a way, have this as a known
     * entry-point to be forwards-compatible with compiled
     * resources. */
    public static DirLight amblight(RenderList rl) {
        return (((MapView) ((PView.WidgetContext) rl.state().get(PView.ctx)).widget()).amb);
    }

    /*public CAttr getcattr(String nm) {
        synchronized (cattr) {
            CAttr a = cattr.get(nm);
            if (a == null) {
                a = new CAttr(nm, 0, 0);
                cattr.put(nm, a);
            }
            return (a);
        }
    }*/

    public CAttr getcattr(String nm) {
        synchronized (cattr) {
            CAttr a = cattr.get(nm);
            if (a == null) {
                a = new CAttr(this, nm, 0, 0, ItemInfo.Raw.nil);
                cattr.put(nm, a);
            }
            return (a);
        }
    }

    /*public void cattr(String nm, int base, int comp) {
        synchronized (cattr) {
            CAttr a = cattr.get(nm);
            if (a == null) {
                a = new CAttr(nm, base, comp);
                cattr.put(nm, a);
            } else {
                a.update(base, comp);
            }
        }
    }*/

    public void cattr(String nm, int base, int comp, ItemInfo.Raw info) {
        synchronized (cattr) {
            CAttr a = cattr.get(nm);
            if (a == null) {
                a = new CAttr(this, nm, base, comp, info);
                cattr.put(nm, a);
            } else {
                a.update(base, comp, info);
            }
        }
    }

    /*public void cattr(Message msg) {
        synchronized (cattr) {
            while (!msg.eom()) {
                String nm = msg.string();
                int base = msg.int32();
                int comp = msg.int32();
                cattr(nm, base, comp);
            }
        }
    }*/

    //Map
    private static final String mapFileName = "fogofwar.v2";
    private static final String mapFileNameTemp = "fogofwartemp.v2";
    private static final AtomicBoolean mapDirty = new AtomicBoolean();
    private static final AtomicBoolean mapDirtyTemp = new AtomicBoolean();
    public static final Map<Long, List<Integer>> mapList = loadCustomList(mapFileName);
    public static final Map<Long, List<Integer>> mapListTemp = loadCustomList(mapFileNameTemp);
    private static final ExecutorService mapUploader = Executors.newSingleThreadExecutor();
    private static final ScheduledExecutorService mapScheduler = Executors.newSingleThreadScheduledExecutor();

    static {
        mapScheduler.scheduleWithFixedDelay(() -> mapUploader.submit(() -> {
            try {
                if (mapDirty.get()) {
                    Utils.saveCustomList(mapList, mapFileName);
                    mapDirty.set(false);
                }
                if (mapDirtyTemp.get()) {
                    Utils.saveCustomList(mapListTemp, mapFileNameTemp);
                    mapDirtyTemp.set(false);
                }
            } catch (Exception e) {
                dev.simpleLog(e);
            }
        }), 0, 5, TimeUnit.SECONDS);
    }

    private static Map<Long, List<Integer>> loadCustomList(final String name) {
        final String json = Config.loadFile(name + ".json");
        final Map<Long, List<Integer>> list = Collections.synchronizedMap(new HashMap<>());
        if (json != null) {
            try {
                Gson gson = (new GsonBuilder()).create();
                Type collectionType = new TypeToken<HashMap<Long, TreeList<Integer>>>() {
                }.getType();
                list.putAll(gson.fromJson(json, collectionType));
            } catch (Exception ignored) {
            }
        }
        return (list);
    }

    private volatile Pair<Coord, Coord> lastSQRT = null;
    private volatile Pair<Coord, Coord> lastSQRTTemp = null;

    private double offset(final double s) {
        int n = 100 * 11;
        return (s % n < 0 ? s % n + n : s % n);
    }

    public final List<Long> updatableMap = Collections.synchronizedList(new ArrayList<>());
    public final List<Long> updatableMapTemp = Collections.synchronizedList(new ArrayList<>());

    public void clearTemp() {
        mapUploader.submit(() -> {
            final List<Long> temp = new ArrayList<>(mapListTemp.keySet());
            mapListTemp.clear();
            Utils.saveCustomList(mapListTemp, mapFileNameTemp);
            temp.forEach(id -> {
                if (updatableMapTemp.stream().noneMatch(l -> l.equals(id))) updatableMapTemp.add(id);
            });
            lastSQRTTemp = null;
        });
    }

    private boolean updateFogMap(final MCache.Grid fid, final int bit, final Map<Long, List<Integer>> map, final List<Long> uList) {
        boolean dirty = false;
        final List<Integer> points = map.computeIfAbsent(fid.id, k -> new TreeList<>());
        if (points.size() >= 121) {
            points.clear();
            points.add(-1);
        } else if (!(points.contains(-1) || points.contains(bit))) {
            if (points.size() + 1 >= 121) {
                points.clear();
                points.add(-1);
            } else points.add(bit);
            dirty = true;
            if (uList.stream().noneMatch(l -> l.equals(fid.id))) uList.add(fid.id);
        }
        return (dirty);
    }

    public void addFOW(final Coord2d mc) {
        final Coord gc = Coord.of(Math.floorDiv((int) mc.x, 1100), Math.floorDiv((int) mc.y, 1100));
        final Coord cc = Coord.of((int) Math.floor(offset(mc.x) / 100.0), (int) Math.floor(offset(mc.y) / 100.0));

        final Pair<Coord, Coord> lastSQRT = this.lastSQRT;
        final boolean updateMap = lastSQRT == null || !lastSQRT.a.equals(gc) || !lastSQRT.b.equals(cc);
        if (updateMap) this.lastSQRT = new Pair<>(gc, cc);

        final Pair<Coord, Coord> lastSQRTTemp = this.lastSQRTTemp;
        final boolean updateMapTemp = lastSQRTTemp == null || !lastSQRTTemp.a.equals(gc) || !lastSQRTTemp.b.equals(cc);
        if (updateMapTemp) this.lastSQRTTemp = new Pair<>(gc, cc);

        if (!updateMap && !updateMapTemp) return;
        mapUploader.submit(() -> {
            try {
                boolean dirty = false;
                boolean dirtyTemp = false;
                for (int x = 0; x < 9; x++) {
                    for (int y = 0; y < 9; y++) {
                        final Coord ccc = cc.sub(4, 4).add(x, y);
                        final MCache.Grid fid;
                        final Coord fc;

                        if (ccc.x < 0 || ccc.y < 0 || ccc.x > 10 || ccc.y > 10) {
                            final Coord2d nc = mc.add((x - 4) * 100.0, (y - 4) * 100.0);
                            fid = map.getgrid(nc.floor(tilesz).div(MCache.cmaps));
                            fc = Coord.of((int) Math.floor(offset(nc.x) / 100.0), (int) Math.floor(offset(nc.y) / 100.0));
                        } else {
                            fid = map.getgrid(mc.floor(tilesz).div(MCache.cmaps));
                            fc = ccc;
                        }

                        final int bit = fc.y * 11 + fc.x;

                        if (updateMap && updateFogMap(fid, bit, mapList, updatableMap)) dirty = true;

                        if (updateMapTemp && updateFogMap(fid, bit, mapListTemp, updatableMapTemp)) dirtyTemp = true;
                    }
                }
                if (dirty) mapDirty.set(true);
                if (dirtyTemp) mapDirtyTemp.set(true);
            } catch (Exception e) {
                dev.simpleLog(e);
            }
        });
    }
}
