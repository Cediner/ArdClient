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

import modification.resources;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;

public class LoginScreen extends Widget {
    public static final Config.Variable<String> authmech = Config.Variable.prop("haven.authmech", "native");
    public static Text.Foundry textf = new Text.Foundry(Text.sans, UI.scale(16)).aa(true);
    public static Text.Foundry textfs = new Text.Foundry(Text.sans, UI.scale(14)).aa(true);
    public static Text.Foundry special = new Text.Foundry(Text.latin, UI.scale(14)).aa(true);
    boolean serverStatus;
    Login cur;
    Text error;
    IButton btn;
    Steambox steambox;
    Button statusbtn;
    Button optbtn;
    private TextEntry user;
    LoginList loginList;
    OptWnd opts;
    Img background;
    static Tex bg = null;

    static Tex bg() {
        if (bg == null)
            bg = resources.bgCheck();
        return (bg);
    }

    Text progress = null;
    private Window log;

    public final StatusLabel status;

    public LoginScreen() {
        super(bg().sz());
//        super(new Coord(800, 600));
        setfocustab(true);
        background = add(new Img(bg), Coord.z);
        optbtn = adda(new Button(UI.scale(100), "Options"), sz.x - UI.scale(10), UI.scale(40), 1, 1);
//        new UpdateChecker().start();
        loginList = adda(new LoginList(UI.scale(200), 29), UI.scale(10, sz.y / 2), 0, 0.5);
//        statusbtn = adda(new Button(200, "Initializing..."), sz.x - 210, 80, 0, 1);
//        StartUpdaterThread();
        status = adda(new StatusLabel(Config.Variable.prop("haven.defhost", "www.havenandhearth.com").get(), 0.5), Coord.of(sz.x - UI.scale(10), UI.scale(80)), 1, 1);
    }

    private static boolean changeLogShowed = false;

    private void showChangeLog() {
        changeLogShowed = true;
        log = new Window(UI.scale(50, 50), "Information");
        log.justclose = true;
        Textlog txt = log.add(new Textlog(UI.scale(450, 200)));
        txt.quote = false;
        int maxlines = txt.maxLines = 200;
        log.pack();
        txt.append("Client is now very unstable!");
        txt.append("Dropped, as announced for a few years or whatever now, support for the old pre-render-rework game resources. Yay!");
        txt.append("Use another client!");
        txt.append("More about 'Haven and Hearth' can be found here: https://discord.gg/24Tq35uwBD");
        txt.append("\n");
        try {
            String git = "";
            {
                try (InputStream in = ClassLoader.getSystemResourceAsStream("buildinfo")) {
                    if (in != null) {
                        Properties info = new Properties();
                        info.load(in);
                        String ver = info.getProperty("version");
                        git = info.getProperty("git-rev");
                        String commit = info.getProperty("commit");
                        txt.append(String.format("Current version: %s", ver));
                        txt.append(String.format("Current git: %s", git));
                        txt.append(String.format("%s", commit.replaceAll("/;", "\n")));
                    }
                } catch (IOException e) {
                }
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("CHANGELOG.txt"), StandardCharsets.UTF_8))) {
                try (FileOutputStream out = new FileOutputStream(Config.getFile("CHANGELOG.txt"))) {
                    String strLine;
                    int count = 0;
                    while ((count < maxlines) && (strLine = br.readLine()) != null) {
                        txt.append(strLine.contains("%s") ? String.format(strLine, git) : strLine);
                        out.write((strLine + Config.LINE_SEPARATOR).getBytes());
                        count++;
                    }
                }
            }
        } catch (IOException ignored) {
        }
        txt.setprog(0);

        //log.add(new Button(log.sz.x, "REPAIR PREFERENCES").action(() -> {
        //    try {
        //        ui.cons.run("sqliteexport");
        //    } catch (Exception e) {}
        //}), log.pos("cbl")).settip("After exporting client will turn off");
        log.pack();

        ui.root.adda(log, ui.root.pos("cmid").div(1, 2), 0.5, 0.5);
    }

    private static abstract class Login extends Widget {
        abstract Object[] data();

        abstract boolean enter();
    }

    private class Pwbox extends Login {
        TextEntry pass;

        private Pwbox(String username, boolean save) {
            setfocustab(true);
            add(new Label("User name", textf) {{
                setstroked(Color.BLACK);
            }}, Coord.z);
            add(user = new TextEntry(UI.scale(150), username), UI.scale(0, 20));
            add(new Label("Password", textf) {{
                setstroked(Color.BLACK);
            }}, UI.scale(0, 50));
            add(pass = new TextEntry(UI.scale(150), ""), UI.scale(0, 70));
            pass.pw = true;
            if (user.text().equals(""))
                setfocus(user);
            else
                setfocus(pass);
            resize(UI.scale(150, 150));
            LoginScreen.this.adda(this, new Coord(LoginScreen.this.sz.x / 2, LoginScreen.this.sz.y / 2), 0.5, 0);
        }

        public void wdgmsg(Widget sender, String name, Object... args) {
        }

        Object[] data() {
            return (new Object[]{new AuthClient.NativeCred(user.text(), pass.text()), false});
        }

        boolean enter() {
            if (user.text().equals("")) {
                setfocus(user);
                return (false);
            } else if (pass.text().equals("")) {
                setfocus(pass);
                return (false);
            } else {
                return (true);
            }
        }

        public boolean globtype(char k, KeyEvent ev) {
            if ((k == 'r') && ((ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0)) {
                return (true);
            }
            return (false);
        }
    }

    private class Tokenbox extends Login {
        Text label;
        Button btn;

        private Tokenbox(String username) {
            label = textfs.render("Identity is saved for " + username, java.awt.Color.WHITE);
            add(btn = new Button(UI.scale(100), "Forget me"), UI.scale(75, 30));
            resize(UI.scale(250, 100));
            LoginScreen.this.add(this, UI.scale(295, 330));
        }

        Object[] data() {
            return (new Object[0]);
        }

        boolean enter() {
            return (true);
        }

        public void wdgmsg(Widget sender, String name, Object... args) {
            if (sender == btn) {
                LoginScreen.this.wdgmsg("forget");
                return;
            }
            super.wdgmsg(sender, name, args);
        }

        public void draw(GOut g) {
            g.image(label.tex(), new Coord((sz.x / 2) - (label.sz().x / 2), 0));
            super.draw(g);
        }

        public boolean globtype(char k, KeyEvent ev) {
            if ((k == 'f') && ((ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0)) {
                LoginScreen.this.wdgmsg("forget");
                return (true);
            }
            return (false);
        }
    }


    public static class LoginList extends Listbox<LoginData> {
        private static final int ITEM_HEIGHT = UI.scale(20);
        private static final Tex xicon = Text.render("✖", Color.WHITE, special).tex();
        private static final Tex upicon;
        private static final Tex downicon;

        static {
            Text uptext = Text.render("▲", Color.WHITE, special);
            upicon = new TexI(PUtils.uiscale(uptext.img, uptext.sz().div(2)));
            Text downtext = Text.render("▼", Color.WHITE, special);
            downicon = new TexI(PUtils.uiscale(downtext.img, downtext.sz().div(2)));
        }

        private int hover = -1;
        private int xhover = -1;
        private int uphover = -1;
        private int downhover = -1;
        private Coord lastMouseDown = Coord.z;

        public LoginList(int w, int h) {
            super(w, h, ITEM_HEIGHT);
        }

        @Override
        protected void drawbg(GOut g) {
            g.chcolor(0, 0, 0, 120);
            g.frect(Coord.z, sz);
            g.chcolor();
        }

        @Override
        protected void drawsel(GOut g) {
        }

        @Override
        protected LoginData listitem(int i) {
            synchronized (Config.logins) {
                return Config.logins.get(i);
            }
        }

        @Override
        protected int listitems() {
            synchronized (Config.logins) {
                return Config.logins.size();
            }
        }

        @Override
        public void mousemove(Coord c) {
            setHoverItem(c);
            super.mousemove(c);
        }

        @Override
        public boolean mousewheel(Coord c, int amount) {
            setHoverItem(c);
            return super.mousewheel(c, amount);
        }

        private void setHoverItem(Coord c) {
            if (c.x > 0 && c.x < sz.x && c.y > 0 && c.y < listitems() * ITEM_HEIGHT)
                hover = c.y / ITEM_HEIGHT + sb.val;
            else
                hover = -1;

            int xpos = sz.x - xicon.sz().x - UI.scale(5);
            int cpos = sz.x - xicon.sz().x - UI.scale(5) - upicon.sz().x - UI.scale(5);
            if (c.x >= xpos && c.x <= xpos + xicon.sz().x)
                xhover = hover;
            else
                xhover = -1;
            if (c.x >= cpos && c.x <= cpos + upicon.sz().x) {
                if (c.y % ITEM_HEIGHT > ITEM_HEIGHT / 2)
                    downhover = hover;
                else
                    downhover = -1;
                if (c.y % ITEM_HEIGHT < ITEM_HEIGHT / 2)
                    uphover = hover;
                else
                    uphover = -1;
            } else {
                uphover = -1;
                downhover = -1;
            }
        }

        private final Map<String, Tex> cached = Collections.synchronizedMap(new HashMap<>());

        @Override
        protected void drawitem(GOut g, LoginData item, int i) {
            if (hover == i) {
                g.chcolor(96, 96, 96, 255);
                g.frect(Coord.z, g.sz);
                g.chcolor();
            }
            g.chcolor(68, 68, 68, 128);
            g.rect(Coord.z, g.sz);
            g.chcolor();
            Tex tex = cached.computeIfAbsent(item.name, (text) -> Text.render(text, Color.WHITE, textfs).tex());
            g.image(tex, new Coord(UI.scale(5), (ITEM_HEIGHT - tex.sz().y) / 2));
            g.chcolor(xhover == i ? Color.YELLOW : Color.RED);
            g.aimage(xicon, new Coord(sz.x - UI.scale(5), (ITEM_HEIGHT - xicon.sz().y) / 2), 1, 0);
            g.chcolor(uphover == i ? Color.YELLOW : Color.RED);
            g.aimage(upicon, new Coord(sz.x - xicon.sz().x - UI.scale(5) - UI.scale(5), 0), 1, 0);
            g.chcolor(downhover == i ? Color.YELLOW : Color.RED);
            g.aimage(downicon, new Coord(sz.x - xicon.sz().x - UI.scale(5) - UI.scale(5), ITEM_HEIGHT), 1, 1);
            g.chcolor();
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            lastMouseDown = c;
            return super.mousedown(c, button);
        }

        @Override
        protected void itemclick(LoginData itm, int button) {
            if (button == 1) {
                int xpos = sz.x - xicon.sz().x - UI.scale(5);
                int cpos = sz.x - xicon.sz().x - UI.scale(5) - upicon.sz().x - UI.scale(5);
                if (lastMouseDown.x >= xpos && lastMouseDown.x <= xpos + xicon.sz().x) {
                    synchronized (Config.logins) {
                        Config.logins.remove(itm);
                        Config.saveLogins();
                    }
                } else if (lastMouseDown.x >= cpos && lastMouseDown.x <= cpos + upicon.sz().x) {
                    if (lastMouseDown.y % ITEM_HEIGHT > ITEM_HEIGHT / 2) {
                        moveData(itm, 1);
                    } else if (lastMouseDown.y % ITEM_HEIGHT < ITEM_HEIGHT / 2) {
                        moveData(itm, -1);
                    }
                } else {
                    parent.wdgmsg("forget");
                    parent.wdgmsg("login", new AuthClient.NativeCred(itm.name, itm.pass), false);
                    Context.accname = itm.name;
                }
                super.itemclick(itm, button);
            }
        }

        private void moveData(LoginData itm, int amount) {
            synchronized (Config.logins) {
                int idx = Config.logins.indexOf(itm);
                if (idx != -1) {
                    int f = idx + amount;
                    if (f >= 0 && f < Config.logins.size()) {
                        Config.logins.remove(itm);
                        Config.logins.add(f, itm);
                        Config.saveLogins();
                    }
                }
            }
        }
    }

    private static boolean steam_autologin = false;

    public class Steambox extends Widget {

        private Steambox() {
            super(UI.scale(200, 150));
            Widget prev = adda(new Label("Logging in with Steam", textf), sz.x / 2, 0, 0.5, 0);
            adda(new IButton("gfx/hud/buttons/login", "u", "d", "o") {
                     protected void depress() {ui.sfx(Button.lbtdown.stream());}

                     protected void unpress() {ui.sfx(Button.lbtup.stream());}

                     public void click() {enter();}
                 },
                    prev.pos("bl").adds(0, 10).x(sz.x / 2), 0.5, 0.0);
        }

        private AuthClient.Credentials creds() throws java.io.IOException {
            return (new SteamCreds());
        }

        private void enter() {
            try {
                LoginScreen.this.wdgmsg("login", creds(), false);
            } catch (java.io.IOException e) {
                error(e.getMessage());
            }
        }

        public void tick(double dt) {
            super.tick(dt);
            if (steam_autologin) {
                enter();
                steam_autologin = false;
            }
        }
    }

    private void mklogin() {
        synchronized (ui) {
            adda(btn = new IButton("gfx/hud/buttons/login", "u", "d", "o") {
                protected void depress() {
                    Audio.play(Button.lbtdown.stream());
                }

                protected void unpress() {
                    Audio.play(Button.lbtup.stream());
                }
            }, LoginScreen.this.sz.x / 2, LoginScreen.this.sz.y / 2 + UI.scale(100), 0.5, 0);
            adda(steambox = new Steambox(), UI.scale(10), LoginScreen.this.sz.y, 0, 1);
            progress(null);
        }
    }

    private void error(String error) {
        synchronized (ui) {
            if (this.error != null)
                this.error = null;
            if (error != null)
                this.error = textf.renderstroked(error, java.awt.Color.RED, Color.BLACK);
        }
    }

    private void progress(String p) {
        synchronized (ui) {
            if (progress != null)
                progress = null;
            if (p != null)
                progress = textf.renderstroked(p, java.awt.Color.WHITE, Color.BLACK);
        }
    }

    private void clear() {
        if (cur != null) {
            ui.destroy(cur);
            cur = null;
            ui.destroy(btn);
            btn = null;
        }
        progress(null);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == btn) {
            if (cur.enter()) {
                Context.accname = user.text();
                super.wdgmsg("login", cur.data());
            }
            return;
        } else if (sender == optbtn) {
            if (opts == null) {
                opts = adda(new OptWnd(false) {
                    public void hide() {
                        /* XXX */
                        reqdestroy();
                    }
                }, sz.div(2), 0.5, 0.5);
            } else {
                opts.reqdestroy();
                opts = null;
            }
            return;
        } else if (sender == opts) {
            opts.reqdestroy();
            opts = null;
        } else if (sender == statusbtn || sender == status) {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI("http://www.havenandhearth.com/portal/"));
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        } else
            super.wdgmsg(sender, msg, args);
    }

    public void cdestroy(Widget ch) {
        if (ch == opts) {
            opts = null;
        }
    }

    public void uimsg(String msg, Object... args) {
        synchronized (ui) {
            if (msg == "passwd") {
                clear();
                cur = new Pwbox((String) args[0], (Boolean) args[1]);
                mklogin();
            } else if (msg == "token") {
                clear();
                cur = new Tokenbox((String) args[0]);
                mklogin();
            } else if (msg == "error") {
                error((String) args[0]);
            } else if (msg == "prg") {
                error(null);
                clear();
                progress((String) args[0]);
            } else if (msg == "bg") {
                if (ui == null || ui.gui == null || ui.sess == null || !ui.sess.alive()) {
                    background.img = bg;
                    background.resize(bg.sz());
                    resize(bg.sz());
                }
            }
        }
    }

    public void presize() {
        c = parent.sz.div(2).sub(sz.div(2));
    }

    protected void added() {
        lower();
        presize();
        parent.setfocus(this);
        if (true && !changeLogShowed) {
            showChangeLog();
        }
    }

    public void resize(Coord sz) {
        super.resize(sz);
        if (opts != null && opts.visible)
            opts.move(new Coord(sz.x * (opts.c.x + opts.sz.x / 2) / oldsz.x, sz.y * (opts.c.y + opts.sz.y / 2) / oldsz.y), 0.5, 0.5);
    }

    @Override
    public void reqdestroy() {
        status.dispose();
        super.reqdestroy();
    }


    public void draw(GOut g) {
        super.draw(g);
        int szx = Math.min(MainFrame.instance.p.getSize().width, sz.x);
        int szy = Math.min(MainFrame.instance.p.getSize().height, sz.y);
        int zerox = MainFrame.instance.p.getSize().width > sz.x ? 0 : sz.x / 2 - MainFrame.instance.p.getSize().width / 2;
        int zeroy = MainFrame.instance.p.getSize().height > sz.y ? 0 : sz.y / 2 - MainFrame.instance.p.getSize().height / 2;

        optbtn.move(new Coord(zerox + szx - UI.scale(10), zeroy + UI.scale(40)), 1, 1);
        loginList.move(new Coord(zerox + UI.scale(10), zeroy + szy / 2), 0, 0.5);
//        statusbtn.move(new Coord(zerox + szx - 210, zeroy + 80), 0, 1);
        status.move(new Coord(zerox + szx - UI.scale(10), zeroy + UI.scale(80)), 1, 1);
        if (cur != null)
            cur.move(new Coord(zerox + szx / 2, zeroy + szy / 2), 0.5, 0);
        if (btn != null)
            btn.move(new Coord(zerox + szx / 2, zeroy + szy / 2 + UI.scale(100)), 0.5, -1);
        move(new Coord(MainFrame.instance.p.getSize().width / 2, MainFrame.instance.p.getSize().height / 2), 0.5, 0.5);

        if (error != null)
            g.aimage(error.tex(), new Coord(LoginScreen.this.sz.x / 2, LoginScreen.this.sz.y / 2 + UI.scale(100)), 0.5, -1);
        if (progress != null)
            g.aimage(progress.tex(), new Coord(LoginScreen.this.sz.x / 2, LoginScreen.this.sz.y / 2), 0.5, -1);
    }

    public boolean type(char k, KeyEvent ev) {
        if (k == 10) {
            if ((cur != null) && cur.enter()) {
                Context.accname = user.text();
                wdgmsg("login", cur.data());
            }
            return (true);
        }
        return (super.type(k, ev));
    }

    private void StartUpdaterThread() {
        Thread statusupdaterthread = new Thread(new Runnable() {
            public void run() {
                try {
                    URL url = new URL("http://www.havenandhearth.com/portal/index/status");
                    while (true) {
                        try (InputStream is = url.openStream()) {
                            try (Scanner scan = new Scanner(is)) {
                                while (scan.hasNextLine()) {
                                    String line = scan.nextLine();
                                    if (line.contains("h2")) {
                                        statusbtn.change(line.substring(line.indexOf("<h2>") + 4, line.indexOf("</h2>")), Color.WHITE);
                                    }
                                }

                                if (ui != null && ui.gui != null && ui.sess != null && ui.sess.alive()) {
                                    break;
                                }
                                Thread.sleep(5000);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        });
        statusupdaterthread.start();
    }

    public static class StatusLabel extends Widget {
        public final HttpStatus stat;
        public final double ax;

        public StatusLabel(String host, double ax) {
            super(new Coord(UI.scale(150), FastText.h * 2));
            this.stat = new HttpStatus(host);
            this.ax = ax;
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            wdgmsg("activate");
            return (true);
        }

        public void draw(GOut g) {
            drawbg(g);
            int x = (int) Math.round(sz.x * ax);
            synchronized (stat) {
                if (!stat.syn || (Objects.equals(stat.status, "")))
                    return;
                if (Objects.equals(stat.status, "up")) {
                    FastText.aprintf(g, new Coord(x, FastText.h * 0), ax, 0, "Server status: Up");
                    FastText.aprintf(g, new Coord(x, FastText.h * 1), ax, 0, "Hearthlings playing: %,d", stat.users);
                } else if (Objects.equals(stat.status, "down")) {
                    FastText.aprintf(g, new Coord(x, FastText.h * 0), ax, 0, "Server status: Down");
                } else if (Objects.equals(stat.status, "shutdown")) {
                    FastText.aprintf(g, new Coord(x, FastText.h * 0), ax, 0, "Server status: Shutting down");
                } else if (Objects.equals(stat.status, "crashed")) {
                    FastText.aprintf(g, new Coord(x, FastText.h * 0), ax, 0, "Server status: Crashed");
                }
            }
        }

        protected void drawbg(GOut g) {
            g.chcolor(0, 0, 0, 120);
            g.frect(Coord.z, sz);
            g.chcolor();
        }

        protected void added() {
            stat.start();
        }

        public void dispose() {
            stat.quit();
        }
    }
}
