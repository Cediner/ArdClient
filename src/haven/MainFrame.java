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

import com.google.common.flogger.FluentLogger;
import haven.purus.pbot.PBotDiscord;
import haven.purus.pbot.Py4j;
import modification.configuration;

import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;


public class MainFrame extends java.awt.Frame implements Runnable, Console.Directory {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    public static MainFrame instance;
    public final HavenPanel p;
    private final ThreadGroup g;
    public final Thread mt;
    DisplayMode fsmode = null, prefs = null;

    public static String TITLE() {
        //System.out.println(encrypt(String.join("\n", death), "death"));
        List<String> death = Arrays.stream(decrypt(MainFrame.deathMessages, "death").split("\n")).collect(Collectors.toList());
        String pre = "ARD Deathlike I: ";
        String post = death.get((int) (Math.random() * death.size()));
        String ret = pre + post;
        return (ret);
    }

    public static void initawt() {
        try {
            System.setProperty("apple.awt.application.name", "Haven & Hearth");
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());

            // Since H&H IPs aren't likely to change (at least mid client run), and the client constantly needs to fetch
            // resources from the server, we enable "cache forever" policy so to overcome sporadic UnknownHostException
            // due to flaky DNS. Bad practice, but still better than forcing the user to modify hosts file.
            // NOTE: this needs to be done early as possible before InetAddressCachePolicy is initialized.
            java.security.Security.setProperty("networkaddress.cache.ttl", "-1");
        } catch (Exception e) {
        }
    }

    static {
        initawt();
    }

    DisplayMode findmode(int w, int h) {
        GraphicsDevice dev = getGraphicsConfiguration().getDevice();
        if (!dev.isFullScreenSupported())
            return (null);
        DisplayMode b = null;
        for (DisplayMode m : dev.getDisplayModes()) {
            int d = m.getBitDepth();
            if ((m.getWidth() == w) && (m.getHeight() == h) && ((d == 24) || (d == 32) || (d == DisplayMode.BIT_DEPTH_MULTI))) {
                if ((b == null) || (d > b.getBitDepth()) || ((d == b.getBitDepth()) && (m.getRefreshRate() > b.getRefreshRate())))
                    b = m;
            }
        }
        return (b);
    }

    public void setfs() {
        GraphicsDevice dev = getGraphicsConfiguration().getDevice();
        if (prefs != null)
            return;
        prefs = dev.getDisplayMode();
        try {
            setVisible(false);
            dispose();
            setUndecorated(true);
            setVisible(true);
            dev.setFullScreenWindow(this);
            dev.setDisplayMode(fsmode);
            pack();
        } catch (Exception e) {
            throw (new RuntimeException(e));
        }
    }

    public void setwnd() {
        GraphicsDevice dev = getGraphicsConfiguration().getDevice();
        if (prefs == null)
            return;
        try {
            dev.setDisplayMode(prefs);
            dev.setFullScreenWindow(null);
            setVisible(false);
            dispose();
            setUndecorated(false);
            setVisible(true);
        } catch (Exception e) {
            throw (new RuntimeException(e));
        }
        prefs = null;
    }

    public boolean hasfs() {
        return (prefs != null);
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();

    {
        cmdmap.put("sz", new Console.Command() {
            public void run(Console cons, String[] args) {
                if (args.length == 3) {
                    int w = Integer.parseInt(args[1]),
                            h = Integer.parseInt(args[2]);
                    p.setSize(w, h);
                    pack();
                    Utils.setprefc("wndsz", new Coord(w, h));
                } else if (args.length == 2) {
                    if (args[1].equals("dyn")) {
                        setResizable(true);
                        Utils.setprefb("wndlock", false);
                    } else if (args[1].equals("lock")) {
                        setResizable(false);
                        Utils.setprefb("wndlock", true);
                    }
                }
            }
        });
        cmdmap.put("fsmode", new Console.Command() {
            public void run(Console cons, String[] args) throws Exception {
                if (args.length == 3) {
                    DisplayMode mode = findmode(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                    if (mode == null)
                        throw (new Exception("No such mode is available"));
                    fsmode = mode;
                    Utils.setprefc("fsmode", new Coord(mode.getWidth(), mode.getHeight()));
                }
            }
        });
        cmdmap.put("fs", new Console.Command() {
            public void run(Console cons, String[] args) {
                if (args.length >= 2) {
                    Runnable r;
                    if (Utils.atoi(args[1]) != 0) {
                        r = new Runnable() {
                            public void run() {
                                setfs();
                            }
                        };
                    } else {
                        r = new Runnable() {
                            public void run() {
                                setwnd();
                            }
                        };
                    }
                    getToolkit().getSystemEventQueue().invokeLater(r);
                }
            }
        });
    }

    public Map<String, Console.Command> findcmds() {
        return (cmdmap);
    }

    private void seticon() {
        Image icon;
        try {
            InputStream data = MainFrame.class.getResourceAsStream("icon.png");
            icon = javax.imageio.ImageIO.read(data);
            data.close();
        } catch (IOException e) {
            throw (new Error(e));
        }
        setIconImage(icon);
    }

    public MainFrame(Coord isz) {
        super("Haven and Hearth");
        Coord sz;
        if (isz == null) {
            sz = Utils.getprefc("wndsz", new Coord(800, 600));
            if (sz.x < 640) sz.x = 640;
            if (sz.y < 480) sz.y = 480;
        } else {
            sz = isz;
        }
        this.g = new ThreadGroup(HackThread.tg(), "Haven client");
        this.mt = new HackThread(this.g, this, "Haven main thread");
        p = new HavenPanel(sz.x, sz.y);
        if (fsmode == null) {
            Coord pfm = Utils.getprefc("fsmode", null);
            if (pfm != null)
                fsmode = findmode(pfm.x, pfm.y);
        }
        if (fsmode == null) {
            DisplayMode cm = getGraphicsConfiguration().getDevice().getDisplayMode();
            fsmode = findmode(cm.getWidth(), cm.getHeight());
        }
        if (fsmode == null)
            fsmode = findmode(800, 600);
        add(p);
        pack();
        setResizable(!Utils.getprefb("wndlock", false));
        p.requestFocus();
        seticon();
        setVisible(true);
        p.init();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (Config.confirmclose) {
                    String ObjButtons[] = {"Yes", "No"};
                    int PromptResult = JOptionPane.showOptionDialog(null,
                            "Are you sure you want to exit?", "Confirm",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                            ObjButtons, ObjButtons[1]);
                    if (PromptResult == 0) {
                        g.interrupt();
                        mt.interrupt();
                        new Thread(() -> {
                            for (int i = 0, sleep = 10; !g.isDestroyed() || !mt.isInterrupted(); i += sleep) {
                                if (i >= 5000)
                                    System.exit(0);
                                try {
                                    Thread.sleep(sleep);
                                } catch (InterruptedException interruptedException) {
                                    interruptedException.printStackTrace();
                                }
                            }
                        }).start();
                    }
                } else {
                    g.interrupt();
                    mt.interrupt();
                    new Thread(() -> {
                        for (int i = 0, sleep = 10; !g.isDestroyed() || !mt.isInterrupted(); i += sleep) {
                            if (i >= 5000)
                                System.exit(0);
                            try {
                                Thread.sleep(sleep);
                            } catch (InterruptedException interruptedException) {
                                interruptedException.printStackTrace();
                            }
                        }
                    }).start();
                }
            }

            public void windowActivated(WindowEvent e) {
                p.bgmode = false;
            }

            public void windowDeactivated(WindowEvent e) {
                p.bgmode = true;
            }
        });
        if ((isz == null) && Utils.getprefb("wndmax", false))
            setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }

    private void savewndstate() {
        if (prefs == null) {
            if (getExtendedState() == NORMAL)
                /* Apparent, getSize attempts to return the "outer
                 * size" of the window, including WM decorations, even
                 * though setSize sets the "inner size" of the
                 * window. Therefore, use the Panel's size instead; it
                 * ought to correspond to the inner size at all
                 * times. */ {
                Dimension dim = p.getSize();
                Utils.setprefc("wndsz", new Coord(dim.width, dim.height));
            }
            Utils.setprefb("wndmax", (getExtendedState() & MAXIMIZED_BOTH) != 0);
        }
    }

    private final List<Thread> sessionThreads = new ArrayList<>();

    public void makeNewSession() {
        final Thread rui = new HackThread(() -> {
            final UI lui = p.newui(null);
            try {
                UI.Runner fun;
                //Login first
                Bootstrap bill = new Bootstrap(Config.defserv, Config.mainport);
                if ((Config.authuser != null) && (Config.authck != null)) {
                    bill.setinitcookie(Config.authuser, Config.authck);
                    Config.authck = null;
                }
                final Session sess = bill.run(lui);
                //reset UI and play game
                lui.reset(new Coord(p.w, p.h));
                lui.setSession(sess);
                new RemoteUI(sess).run(lui);
                //Remove this UI once done
                p.removeUI(lui);
//                lui.root.reqdestroy();
                lui.destroy();
            } catch (InterruptedException e) {
            } finally {
                p.removeUI(lui);
                synchronized (sessionThreads) {
                    sessionThreads.remove(Thread.currentThread());
                }
            }
        }, "Remote UI Session thread");
        rui.start();
        synchronized (sessionThreads) {
            sessionThreads.add(rui);
        }
    }


    public void run() {
        if (Thread.currentThread() != this.mt)
            throw (new RuntimeException("MainFrame is being run from an invalid context"));
        Thread ui = new HackThread(p, "Haven UI thread");
        ui.start();
        try {
            try {
                UI lui = p.newui(null);
                Session sess = null;
                while (true) {
                    UI.Runner fun;
                    lui.reset(new Coord(p.w, p.h));
                    if (sess == null) {
                        Bootstrap bill = new Bootstrap(Config.defserv, Config.mainport);
                        if ((Config.authuser != null) && (Config.authck != null)) {
                            bill.setinitcookie(Config.authuser, Config.authck);
                            Config.authck = null;
                        }
                        fun = bill;
                        if (configuration.customTitleBoolean)
                            setTitle(configuration.tittleCheck(sess));
                        else
                            setTitle(TITLE());
                    } else {
                        fun = new RemoteUI(sess);
                        lui.setSession(sess);
                        if (configuration.customTitleBoolean)
                            setTitle(configuration.tittleCheck(sess));
                        else
                            setTitle(/*sess.username + " \u2013 " + */TITLE());
                    }
                    sess = fun.run(lui);
//                    lui.root.reqdestroy();
                    lui.destroy();
                }
            } catch (InterruptedException e) {
            } finally {
                //p.removeUI(lui);
                p.removeUIS();
            }
            savewndstate();
        } finally {
            ui.interrupt();
            synchronized (sessionThreads) {
                for (final Thread thr : sessionThreads) {
                    thr.interrupt();
                }
            }
            dispose();
        }
    }


    public static void setupres() {
        ResCache cache = ResCache.getCache("resources");
        if (cache != null)
            Resource.setcache(cache);
        //if (Config.resurl != null)
        //    Resource.addurl(Config.resurl, cache);
        ResCache ncache = ResCache.getCache("resources_new");
        if (ncache != null)
            Resource.setcache(ncache);
        if (Config.newresurl != null)
            Resource.addurl(Config.newresurl, ncache);
        /*if (cache != null) {
            try {
                Resource.loadlist(Resource.remote(), cache.fetch("tmp/allused"), -10);
            } catch (IOException e) {}
        }*/
        if (!Config.nopreload) {
            try {
                InputStream pls;
                pls = Resource.class.getResourceAsStream("res-preload");
                if (pls != null)
                    Resource.loadlist(Resource.remote(), pls, -5);
                pls = Resource.class.getResourceAsStream("res-bgload");
                if (pls != null)
                    Resource.loadlist(Resource.remote(), pls, -10);
            } catch (IOException e) {
                throw (new Error(e));
            }
        }
    }

    static {
        WebBrowser.self = DesktopBrowser.create();
    }

    private static void javabughack() throws InterruptedException {
        /* Work around a stupid deadlock bug in AWT. */
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    PrintStream bitbucket = new PrintStream(new ByteArrayOutputStream());
                    bitbucket.print(LoginScreen.textf);
                    bitbucket.print(LoginScreen.textfs);
                }
            });
        } catch (java.lang.reflect.InvocationTargetException e) {
            /* Oh, how I love Swing! */
            throw (new Error(e));
        }
    }

    private static void main2(String[] args) {
        Config.cmdline(args);

        if (Config.playerposfile != null)
            new Thread(new PlayerPosStreamer(), "Player position thread").start();

        try {
            javabughack();
        } catch (InterruptedException e) {
            return;
        }
        setupres();
        DefSettings.init(); //init after res has been setup...
        try {
            if (Config.autoconnectdiscord)
                new Thread(PBotDiscord::initalize).start();
        } catch (Exception e) {
//            e.printStackTrace();
        }
        MainFrame f = (instance = new MainFrame(null));
        if (Utils.getprefb("fullscreen", false))
            f.setfs();
        f.mt.start();
        try {
            f.mt.join();
        } catch (InterruptedException e) {
            f.g.interrupt();
            return;
        }
        dumplist(Resource.remote().loadwaited(), null);
        dumplist(Resource.remote().cached(), null);
        if (ResCache.getCache("resources") != null) {
            try {
                Writer w = new OutputStreamWriter(ResCache.getCache("resources").store("tmp/allused"), "UTF-8");
                try {
                    Resource.dumplist(Resource.remote().used(), w);
                } finally {
                    w.close();
                }
            } catch (IOException e) {
            }
        }
        System.exit(0);
    }

    public static void main(final String[] args) {
        /* Set up the error handler as early as humanly possible. */
        final haven.error.ErrorHandler hg = new haven.error.ErrorHandler();
        hg.sethandler(new haven.error.ErrorGui(null) {
            public void errorsent() {
                hg.interrupt();
            }
        });
        ThreadGroup g = hg;
        new DeadlockWatchdog(hg).start();
        Py4j.start();
        Thread main = new HackThread(g, () -> main2(args), "Haven main thread");
        main.start();
    }

    private static void dumplist(Collection<Resource> list, String fn) {
        try {
            if (fn != null) {
                Writer w = new OutputStreamWriter(new FileOutputStream(fn), "UTF-8");
                try {
                    Resource.dumplist(list, w);
                } finally {
                    w.close();
                }
            }
        } catch (IOException e) {
            throw (new RuntimeException(e));
        }
    }


    //encrypted list for experience
    private static final String deathMessages = "MA0EVA8LBA1UBwJFABgERAkIEg1EDBJUDAEEFRxiKRASAEgKChVUCQgJQQAADQsGB0gFEUEAAAFFDRUbEEUDEUgXEgAYBAsSBBBIERVBHQZEAQQVHAxaayMNRAgIEwAQRQAHSBMADRhIAAwEVAkXRRUbSAMKQRsGRAkIAgEKAkEYAQ8AQQAADRZrOQ0JAA8AB0QIDgYBbikIAg0ARRYdHAwKFABIAgAABkREBA8QSAAMBBBIEwwVHAcREUESDQUXazkRRAQPFw0XEQ4GG0QEExFIFwgIGAEKAkEVHEQIBFpIJwQPVBELEEEHCR1FFRwNRBYAGQ1bbyYVBQFFJREeAQkOBAUBCxVOSCgMEgANAEU2Gx0KAWswDQURCVQBF0UPGxxEEQkRSAsVERsbDREEVAcCRQ0dDgFJQRYdEEUAVBgFFxVUBwJFCABiMApBAAABRRYRBAhIDgYPBQsIDg0ARQwdBgBJQRANBREJVAEXRQMBHEQRCRFICgAZAEgDFwQVHEQEBQINChEUBg1uMgRUCQgJQRABAUtBIAABRQYbCQhFCAcGhuX4AEgQCkEYARIAQRIHFgAXERpIRRUcDUQCDhUERAwSVBwLRQIGDQURBFQbCwgEAAANCwZUHAwEFVQfDQkNfiYLRQ4aDUQMElQJBxEUFQQIHEEQDQUBQQEGEAwNVBwMAEEGARQVDREbRBEJERFEBgABGwFFCBpIEA0EVB8LFw0QSAAMBFQJEwQYfiwBBBUcSA0WQRVICgQVARoFCUEECRYRQRsORAkIEg1KRTMRAgsMAhFIAgoTVBwMChIRSAUXDgEGAEUYGx1EEgkbSBAXABobAgoTGUgNCxUbSBANBFQuCxcCEWIwDQRUCgsQDxAJFgwEB0gTDQgXAEQBCAIBAABBOAECAEESGgsIQTANBREJVAkWAEEVHEQHBAccRBYJFQwLEhhUCQoBQQIJAxAE";

    //for encrypt your text
    /*public static String encrypt(String text, String key) {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[textBytes.length];

        for (int i = 0; i < textBytes.length; i++) {
            result[i] = (byte) (textBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        return (Base64.getEncoder().encodeToString(result));
    }*/

    public static String decrypt(String encryptedText, String key) {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[encryptedBytes.length];

        for (int i = 0; i < encryptedBytes.length; i++) {
            result[i] = (byte) (encryptedBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        return (new String(result, StandardCharsets.UTF_8));
    }
}
