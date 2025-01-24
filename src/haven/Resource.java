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

import dolda.xiphutil.VorbisStream;
import modification.configuration;
import modification.dev;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Resource implements Serializable {
    public static ThreadGroup loadergroup = null;
    private static Map<String, LayerFactory<?>> ltypes = new TreeMap<>();
    public static Class<Image> imgc = Image.class;
    public static Class<Neg> negc = Neg.class;
    public static Class<Props> props = Props.class;
    public static Class<Obstacle> obst = Obstacle.class;
    public static Class<Anim> animc = Anim.class;
    public static Class<Pagina> pagina = Pagina.class;
    public static Class<AButton> action = AButton.class;
    public static Class<Audio> audio = Audio.class;
    public static Class<Tooltip> tooltip = Tooltip.class;
    public static Class<Src> src = Src.class;

    public static final String language = Utils.getpref("language", "en");
    public static final String BUNDLE_TOOLTIP = "tooltip";
    public static final String BUNDLE_PAGINA = "pagina";
    public static final String BUNDLE_WINDOW = "window";
    public static final String BUNDLE_BUTTON = "button";
    public static final String BUNDLE_FLOWER = "flower";
    public static final String BUNDLE_MSG = "msg";
    public static final String BUNDLE_LABEL = "label";
    public static final String BUNDLE_ACTION = "action";
    public static final String BUNDLE_INGREDIENT = "ingredient";
    private final static Map<String, Map<String, String>> l10nBundleMap;
    public static final boolean L10N_DEBUG = System.getProperty("dumpstr") != null;


    protected Collection<Layer> layers = new LinkedList<>();
    public final String name;
    public int ver, realVer;
    public ResSource source;
    public final transient Pool pool;
    private boolean used = false;

    public abstract static class Named implements Indir<Resource>, Serializable {
        public final String name;
        public final int ver;

        public Named(String name, int ver) {
            this.name = name;
            this.ver = ver;
        }

        public boolean equals(Object other) {
            if (!(other instanceof Named))
                return (false);
            Named o = (Named) other;
            return (o.name.equals(this.name) && (o.ver == this.ver));
        }

        public int hashCode() {
            int ret = name.hashCode();
            ret = (ret * 31) + ver;
            return (ret);
        }

        public String name() {
            return name;
        }

        public String toString() {
            return (String.format("#<res-name %s v%d>", name, ver));
        }
    }

    public String toString2() {
        return (String.format("#<res-name %s v%d>", name, ver));
    }


    public static class Spec extends Named implements Serializable {
        public final transient Pool pool;

        public Spec(Pool pool, String name, int ver) {
            super(name, ver);
            this.pool = pool;
        }

        public Spec(Pool pool, String name) {
            this(pool, name, -1);
        }

        public Resource get(int prio) {
            return (pool.load(name, ver, prio).get());
        }

        @Override
        public Resource get() {
            return (get(0));
        }

        public static Resource loadsaved(Resource.Pool pool, Resource.Spec spec) {
            try {
                if (spec.pool == null)
                    return (pool.load(spec.name, spec.ver).get());
                return (spec.get());
            } catch (Loading l) {
                throw (l);
            } catch (Exception e) {
                return (pool.load(spec.name).get());
            }
        }

        public Resource loadsaved(Resource.Pool pool) {
            return (loadsaved(pool, this));
        }

        public Resource loadsaved() {
            return (loadsaved(this.pool));
        }
    }

    public interface Resolver {
        Indir<Resource> getres(int id);

        public default Indir<Resource> dynres(UID uid) {
            return (() -> {throw (new NoSuchResourceException(String.format("dyn/%x", uid.longValue()), 1, null));});
        }

        default Indir<Resource> getresv(Object desc) {
            if (desc == null)
                return (null);
            if (desc instanceof UID)
                return (dynres((UID) desc));
            if (desc instanceof Number) {
                int id = ((Number) desc).intValue();
                if (id < 0)
                    return (null);
                return (this.getres(id));
            }
            if (desc instanceof Resource)
                return (((Resource) desc).indir());
            if (desc instanceof Indir) {
                @SuppressWarnings("unchecked") Indir<Resource> ret = (Indir<Resource>) desc;
                return (ret);
            }
            throw (new ClassCastException("unknown type for resource id: " + desc));
        }

        class ResourceMap implements Resource.Resolver {
            public final Resource.Resolver bk;
            public final Map<Integer, Integer> map;

            public ResourceMap(Resource.Resolver bk, Map<Integer, Integer> map) {
                this.bk = bk;
                this.map = map;
            }

            public ResourceMap(Resource.Resolver bk, Message data) {
                this(bk, decode(data));
            }

            public ResourceMap(Resource.Resolver bk, Object[] args) {
                this(bk, decode(args));
            }

            public static Map<Integer, Integer> decode(Message sdt) {
                if (sdt.eom())
                    return (Collections.emptyMap());
                int n = sdt.uint8();
                Map<Integer, Integer> ret = new HashMap<>();
                for (int i = 0; i < n; i++)
                    ret.put(sdt.uint16(), sdt.uint16());
                return (ret);
            }

            public static Map<Integer, Integer> decode(Object[] args) {
                if (args.length == 0)
                    return (Collections.emptyMap());
                Map<Integer, Integer> ret = new HashMap<>();
                for (int a = 0; a < args.length; a += 2)
                    ret.put((Integer) args[a], (Integer) args[a + 1]);
                return (ret);
            }

            @Override
            public Indir<Resource> getres(int id) {
                return (bk.getres(map.get(id)));
            }

            public Indir<Resource> dynres(UID uid) {
                return (bk.dynres(uid));
            }

            public String toString() {
                return (map.toString());
            }
        }
    }

    private Resource(Pool pool, String name, int ver) {
        this(pool, name, ver, ver);
    }

    private Resource(Pool pool, String name, int ver, int realVer) {
        this.pool = pool;
        this.name = name;
        this.ver = ver;
        this.realVer = realVer;
    }

    public static class Virtual extends Resource {
        public Virtual(Pool pool, String name, int ver) {
            super(pool, name, ver);
        }

        public Virtual(String name, int ver) {
            this(remote(), name, ver);
        }

        public void add(Layer layer) {
            layers.add(layer);
        }
    }

    private void setRealVer(int ver) {
        this.realVer = ver;
    }

    public static void setcache(ResCache cache) {
        remote().add(new CacheSource(cache));
    }

    public String basename() {
        int p = name.lastIndexOf('/');
        if (p < 0)
            return (name);
        return (name.substring(p + 1));
    }

    public interface ResSource {
        InputStream get(String name) throws IOException;
    }

    public static abstract class TeeSource implements ResSource, Serializable {
        public ResSource back;

        public TeeSource(ResSource back) {
            this.back = back;
        }

        @Override
        public InputStream get(String name) throws IOException {
            StreamTee tee = new StreamTee(back.get(name));
            tee.setncwe();
            tee.attach(fork(name));
            return (tee);
        }

        public abstract OutputStream fork(String name) throws IOException;

        public String toString() {
            return ("forking source backed by " + back);
        }
    }

    public static class CacheSource implements ResSource, Serializable {
        public transient ResCache cache;

        public CacheSource(ResCache cache) {
            this.cache = cache;
        }

        @Override
        public InputStream get(String name) throws IOException {
            return (cache.fetch("res/" + name));
        }

        public String toString() {
            return ("cache source backed by " + cache);
        }
    }

    public static class FileSource implements ResSource, Serializable {
        public static final Collection<String> wintraps =
                new HashSet<>(Arrays.asList("con", "prn", "aux", "nul",
                        "com0", "com1", "com2", "com3", "com4",
                        "com5", "com6", "com7", "com8", "com9",
                        "lpt0", "lpt1", "lpt2", "lpt3", "lpt4",
                        "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"));
        public static final boolean windows = System.getProperty("os.name", "").startsWith("Windows");
        File base;

        public FileSource(File base) {
            this.base = base;
        }

        @Override
        public InputStream get(String name) throws IOException {
            File cur = base;
            String[] parts = name.split("/");
            for (int i = 0; i < parts.length - 1; i++)
                cur = new File(cur, parts[i]);
            cur = new File(cur, parts[parts.length - 1] + ".res");
            return (Files.newInputStream(cur.toPath()));
        }

        public String toString() {
            return ("filesystem res source (" + base + ")");
        }
    }

    public static class JarSource implements ResSource, Serializable {
        public final String base;

        public JarSource(String base) {
            this.base = base;
        }

        @Override
        public InputStream get(String name) throws FileNotFoundException {
            String full = "/" + base + "/" + name + ".res";
            if (name.equals("gfx/terobjs/cupboard") && !Config.flatcupboards)
                return null;
            InputStream s = Resource.class.getResourceAsStream(full);
            if (s == null)
                throw (new FileNotFoundException("Could not find resource locally: " + full));
            return (s);
        }

        public String toString() {
            return ("local res source (" + base + ")");
        }
    }

    public static class HttpSource implements ResSource, Serializable {
        public URI base;

        public HttpSource(URI base) {
            dev.simpleLog("Base URL: " + base);
            this.base = base;
        }

        private URI encodeuri(URI raw) throws IOException {
            /* This is kinda crazy, but it is, actually, how the Java
             * documentation recommends that it be done... */
            try {
                return (new URI(new URI(raw.getScheme(), raw.getUserInfo(), raw.getHost(), raw.getPort(), raw.getPath(), raw.getQuery(), raw.getFragment()).toASCIIString()));
            } catch (URISyntaxException e) {
                throw (new IOException(e));
            }
        }

        @Override
        public InputStream get(String name) throws IOException {
            return (Http.fetch(encodeuri(base.resolve(name + ".res")).toURL(), c -> {
                /* Apparently, some versions of Java Web Start has
                 * a bug in its internal cache where it refuses to
                 * reload a URL even when it has changed. */
                c.setUseCaches(false);
            }));
        }

        public String toString() {
            return ("HTTP res source (" + base + ")");
        }
    }

    public static class Loading extends haven.Loading {
        private final Pool.Queued res;

        private Loading(Pool.Queued res) {
            super("Waiting for resource " + res.name + "...");
            this.res = res;
        }

        public String toString() {
            return ("#<Resource " + res.name + ">");
        }

        @Override
        public void waitfor(Runnable callback, Consumer<Waiting> reg) {
            synchronized (res) {
                if (res.done) {
                    reg.accept(Waitable.Waiting.dummy);
                    callback.run();
                } else {
                    reg.accept(res.wq.add(callback));
                }
            }
        }

        public boolean boostprio(int prio) {
            res.boostprio(prio);
            return (true);
        }
    }

    public static class BadResourceException extends RuntimeException {
        public final String name;
        public final int ver;

        public BadResourceException(String name, int ver, String message, Throwable cause) {
            super(message, cause);
            this.name = name;
            this.ver = ver;
        }

        public BadResourceException(String name, int ver, String message) {
            this(name, ver, message, null);
        }

        public BadResourceException(String name, int ver, Throwable cause) {
            this(name, ver, null, cause);
        }

        public BadResourceException(String name, int ver) {
            this(name, ver, null, null);
        }
    }

    public static class LoadFailedException extends BadResourceException {
        public LoadFailedException(String name, int ver, LoadException cause) {
            super(name, ver, cause);
        }

        public String getMessage() {
            return (String.format("Failed to load resource %s (v%d)", name, ver));
        }
    }

    public static class NoSuchResourceException extends LoadFailedException {
        public NoSuchResourceException(String name, int ver, LoadException cause) {
            super(name, ver, cause);
        }
    }

    public static class BadVersionException extends BadResourceException {
        public final int curver;
        public final String cursrc;

        public BadVersionException(String name, int ver, int curver, ResSource cursrc) {
            super(name, ver);
            this.curver = curver;
            this.cursrc = (cursrc == null) ? null : String.valueOf(cursrc);
        }

        public String getMessage() {
            if (cursrc == null)
                return (String.format("Obsolete version %d of %s requested, loaded version is %d", ver, name, curver));
            else
                return (String.format("Obsolete version %d of %s requested, loaded version is %d, from %s", ver, name, curver, cursrc));
        }
    }

    public static class Pool {
        public int nloaders = 16;
        private final Collection<Loader> loaders = new LinkedList<>();
        private final List<ResSource> sources = new LinkedList<>();
        private final Map<String, Resource> cache = new CacheMap<>();
        private final PrioQueue<Queued> queue = new PrioQueue<>();
        private final Map<String, Queued> queued = new HashMap<>();
        private final Pool parent;

        public Pool(Pool parent, ResSource... sources) {
            this.parent = parent;
            for (ResSource source : sources)
                this.sources.add(source);
        }

        public Pool(ResSource... sources) {
            this(null, sources);
        }

        public void add(ResSource src) {
            sources.add(src);
        }

        private class Queued extends Named implements Prioritized, Serializable {
            transient final Collection<Queued> rdep = new LinkedList<>();
            final Waitable.Queue wq = new Waitable.Queue();
            volatile int prio;
            Queued awaiting;
            volatile boolean done = false;
            Resource res;
            LoadException error;

            Queued(String name, int ver, int prio) {
                super(name, ver);
                this.prio = prio;
            }

            @Override
            public int priority() {
                return (prio);
            }

            public void boostprio(int prio) {
                if (this.prio < prio)
                    this.prio = prio;
                Queued p = awaiting;
                if (p != null)
                    p.boostprio(prio);
            }

            private FakeResource fake;

            @Override
            public Resource get() {
                if (!done) {
                    boostprio(1);
                    throw (new Loading(this));
                }
                if (error != null)
                    if (dev.skipexceptions) {
                        dev.simpleLog("Delayed error in resource " + name + " (v" + ver + "), from " + error.src + " => " + error);
                    } else {
                        if (fake == null)
                            fake = new FakeResource(name, ver);
                        return (fake);
//                        throw (new RuntimeException("Delayed error in resource " + name + " (v" + ver + "), from " + error.src, error));
                    }
                return (res);
            }

            private void done() {
                synchronized (this) {
                    done = true;
                    for (Iterator<Queued> i = rdep.iterator(); i.hasNext(); ) {
                        Queued dq = i.next();
                        i.remove();
                        dq.prior(this);
                    }
                    wq.wnotify();
                }
                if (res != null) {
                    synchronized (cache) {
                        cache.put(name, res);
                    }
                    synchronized (queue) {
                        queued.remove(name);
                    }
                }
            }

            private void prior(Queued prior) {
                if ((res = prior.res) == null) {
                    error = prior.error;
                    synchronized (queue) {
                        queue.add(this);
                        queue.notify();
                    }
                    ckld();
                } else {
                    done();
                }
            }

            public String toString() {
                return (String.format("<q:%s(v%d)>", name, ver));
            }
        }

        private void handle(Queued res) {
            Map<Integer, Object[]> map = new HashMap<>();
            for (ResSource src : sources) {
                try {
                    try (InputStream in = src.get(res.name)) {
                        Resource ret = new Resource(this, res.name, res.ver);
                        ret.source = src;
                        Object[] objs = ret.load(in);
                        ret.setRealVer((Integer) objs[0]);
                        if (objs[1] != null) {
                            map.put((Integer) objs[0], new Object[]{src, ret, objs[2]});
                            throw ((LoadException) objs[1]);
                        }
                        ret.loadlayers((Message) objs[2]);
                        res.res = ret;
                        res.error = null;
                        break;
                    }
                } catch (Throwable t) {
                    LoadException error;
                    if (t instanceof LoadException)
                        error = (LoadException) t;
                    else
                        error = new LoadException(String.format("Load error in resource %s(v%d), from %s", res.name, res.ver, src), t, null);
                    error.src = src;
                    if (res.error != null) {
                        error.prev = res.error;
                        error.addSuppressed(res.error);
                    }
                    res.error = error;
                }
            }
            if (res.error != null && !map.isEmpty()) {
                if (!(res.error instanceof LoadException))
                    throw (res.error);
                map.keySet().stream().max(Comparator.comparingInt(Integer::intValue)).ifPresent(maxVer -> {
                    Object[] objs = map.get(maxVer);
                    Resource ret = (Resource) objs[1];
                    ret.source = (ResSource) objs[0];
                    if (!(ret.source instanceof JarSource)) {
                        ret.loadlayers((Message) objs[2]);
                        res.res = ret;
                        res.error = null;
                    }
                });
            }
            res.done();
        }

        public Named load(String name, int ver, int prio) {
            Queued ret;
            synchronized (cache) {
                Resource cur = cache.get(name);
                if (cur != null) {
                    if ((ver == -1) || (cur.ver == ver)) {
                        return (cur.indir());
                    } else if (ver < cur.ver) {
                        /* Throw LoadException rather than
                         * RuntimeException here, to make sure
                         * obsolete resources doing nested loading get
                         * properly handled. This could be the wrong
                         * way of going about it, however; I'm not
                         * sure. */
//                        throw (new LoadException(String.format("Weird version number on %s (%d > %d), loaded from %s", cur.name, cur.ver, ver, cur.source), cur));
                        dev.simpleLog(String.format("Weird version number on %s (%d > %d), loaded from %s %s", cur.name, cur.ver, ver, cur.source, cur));
                        return (cur.indir());
                    }
                }
                synchronized (queue) {
                    Queued cq = queued.get(name);
                    if (cq != null) {
                        if (ver != -1) {
                            if (ver < cq.ver) { //who cares, don't kill the client over this...
                                //throw(new LoadException(String.format("Weird version number on %s (%d > %d)", cq.name, cq.ver, ver), null));
                                dev.simpleLog(String.format("Weird version number on %s (%d > %d)", cq.name, cq.ver, ver));
                                cq.boostprio(prio);
                                return (cq);
                            } else if (ver == cq.ver) {
                                cq.boostprio(prio);
                                return (cq);
                            }
                        } else {
                            if (cq.done && (cq.error != null)) {
                                /* XXX: This is probably not the right way to handle this. */
                            } else {
                                cq.boostprio(prio);
                                return (cq);
                            }
                        }
                        queued.remove(name);
                        queue.removeid(cq);
                    }
                    Queued nq = new Queued(name, ver, prio);
                    queued.put(name, nq);
                    if (parent == null) {
                        queue.add(nq);
                        queue.notify();
                    } else {
                        Indir<Resource> pr = parent.load(name, ver, prio);
                        if (pr instanceof Queued) {
                            Queued pq = (Queued) pr;
                            synchronized (pq) {
                                if (pq.done) {
                                    nq.prior(pq);
                                } else {
                                    nq.awaiting = pq;
                                    pq.rdep.add(nq);
                                }
                            }
                        } else {
                            queued.remove(name);
                            nq.res = pr.get();
                            nq.done = true;
                        }
                    }
                    ret = nq;
                }
            }
            ckld();
            return (ret);
        }

        public Named load(String name, int ver) {
            return (load(name, ver, 0));
        }

        public Named load(String name) {
            return (load(name, -1));
        }

        public Indir<Resource> dynres(long id) {
            return (load(String.format("dyn/%x", id), 1));
        }

        public Indir<Resource> dynres(UID id) {
            return (dynres(id.bits));
        }

        private void ckld() {
            int qsz;
            synchronized (queue) {
                qsz = queue.size();
            }
            synchronized (loaders) {
                while (loaders.size() < Math.min(nloaders, qsz)) {
                    final Loader n = new Loader();
                    Thread th = AccessController.doPrivileged((PrivilegedAction<Thread>) () -> (new HackThread(loadergroup, n, "Haven resource loader")));
                    th.setDaemon(true);
                    th.start();
                    while (!n.added) {
                        try {
                            loaders.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        }

        public class Loader implements Runnable {
            private boolean added = false;

            @Override
            public void run() {
                synchronized (loaders) {
                    loaders.add(this);
                    added = true;
                    loaders.notifyAll();
                }
                boolean intd = false;
                try {
                    while (true) {
                        Queued cur;
                        synchronized (queue) {
                            long start = System.currentTimeMillis(), now = start;
                            while ((cur = queue.poll()) == null) {
                                queue.wait(10000 - (now - start));
                                now = System.currentTimeMillis();
                                if (now - start >= 10000)
                                    return;
                            }
                        }
                        handle(cur);
                        cur = null;
                    }
                } catch (InterruptedException e) {
                    intd = true;
                } finally {
                    synchronized (loaders) {
                        loaders.remove(this);
                    }
                    if (!intd)
                        ckld();
                }
            }
        }

        public int qdepth() {
            int ret = (parent == null) ? 0 : parent.qdepth();
            synchronized (queue) {
                ret += queue.size();
            }
            return (ret);
        }

        public int numloaded() {
            int ret = (parent == null) ? 0 : parent.numloaded();
            synchronized (cache) {
                ret += cache.size();
            }
            return (ret);
        }

        public Collection<Resource> cached() {
            Set<Resource> ret = new HashSet<>();
            if (parent != null)
                ret.addAll(parent.cached());
            synchronized (cache) {
                ret.addAll(cache.values());
            }
            return (ret);
        }

        public Collection<Resource> used() {
            Collection<Resource> ret = cached();
            for (Iterator<Resource> i = ret.iterator(); i.hasNext(); ) {
                Resource r = i.next();
                if (!r.used)
                    i.remove();
            }
            return (ret);
        }

        private final Set<Resource> loadwaited = new HashSet<>();

        public Collection<Resource> loadwaited() {
            Set<Resource> ret = new HashSet<>();
            if (parent != null)
                ret.addAll(parent.loadwaited());
            synchronized (loadwaited) {
                ret.addAll(loadwaited);
            }
            return (ret);
        }

        private Resource loadwaited(Resource res) {
            synchronized (loadwaited) {
                loadwaited.add(res);
            }
            return (res);
        }

        public Resource loadwaitint(String name, int ver) throws InterruptedException {
            return (loadwaited(Loading.waitforint(load(name, ver, 10))));
        }

        public Resource loadwaitint(String name) throws InterruptedException {
            return (loadwaitint(name, -1));
        }

        public Resource loadwait(String name, int ver) {
            return (loadwaited(Loading.waitfor(load(name, ver, 10))));
        }

        public Resource loadwait(String name) {
            return (loadwait(name, -1));
        }
    }

    private static Pool _local = null;

    public static Pool local() {
        if (_local == null) {
            synchronized (Resource.class) {
                if (_local == null) {
                    Pool local = new Pool();
                    try {
                        local.add(new JarSource("res"));
                        if (Config.resdir != null)
                            local.add(new FileSource(new File(Config.resdir)));
                    } catch (Exception e) {
                        /* Ignore these. We don't want to be crashing the client
                         * for users just because of errors in development
                         * aids. */
                    }
                    _local = local;
                }
            }
        }
        return (_local);
    }

    private static Pool _remote = null;

    public static Pool remote() {
        if (_remote == null) {
            synchronized (Resource.class) {
                if (_remote == null) {
                    Pool remote = new Pool(local());
                    remote.add(new JarSource("res-preload"));
//                    if (prscache != null)
//                        remote.add(new CacheSource(prscache));
                    _remote = remote;
                }
            }
        }
        return (_remote);
    }

    public static Pool remote(String uri) throws MalformedURLException {
        synchronized (Resource.class) {
            return new Pool(new HttpSource(URI.create(uri)));
        }
    }

    public static void addurl(URI url, ResCache cache) {
        ResSource src = new HttpSource(url);
        if (cache != null) {
            class Caching extends TeeSource {
                private final transient ResCache cache;

                Caching(ResSource bk, ResCache cache) {
                    super(bk);
                    this.cache = cache;
                }

                @Override
                public OutputStream fork(String name) throws IOException {
                    return (cache.store("res/" + name));
                }
            }
            src = new Caching(src, cache);
        }
        remote().add(src);
    }

    @Deprecated
    public static Resource load(String name, int ver) {
        return (remote().loadwait(name, ver));
    }

    @Deprecated
    public Resource loadwait() {
        return (this);
    }

    public static class LoadException extends RuntimeException {
        public Resource res;
        public ResSource src;
        public LoadException prev;

        public LoadException(String msg, Resource res) {
            super(msg);
            this.res = res;
        }

        public LoadException(String msg, Throwable cause, Resource res) {
            super(msg, cause);
            this.res = res;
        }

        public LoadException(Throwable cause, Resource res) {
            super("Load error in resource " + res.toString() + ", from " + res.source, cause);
            this.res = res;
        }
    }

    public static class LoadWarning extends Warning {
        public final Resource res;

        public LoadWarning(Resource res, String msg) {
            super(msg);
            this.res = res;
        }

        public LoadWarning(Resource res, String msg, Object... args) {
            this(res, String.format(msg, args));
        }
    }

    public static Coord cdec(Message buf) {
        return (new Coord(buf.int16(), buf.int16()));
    }

    public static Coord2d c2ddec(Message buf) {
        return (new Coord2d(buf.int16(), buf.int16()));
    }

    public static class PoolMapper implements Function<Object, Object> {
        public final Pool pool;

        public PoolMapper(Pool pool) {this.pool = pool;}

        public Object apply(Object obj) {
            if (obj instanceof Spec)
                return (new Spec(pool, ((Spec) obj).name, ((Spec) obj).ver));
            return (obj);
        }
    }

    public abstract class Layer implements Serializable {
        public abstract void init();

        public Resource getres() {
            return (Resource.this);
        }

        public String toString() {
            if (this instanceof IDLayer)
                return (String.format("#<%s (%s) in %s>", getClass().getSimpleName(), ((IDLayer) this).layerid(), Resource.this.name));
            else
                return (String.format("#<%s in %s>", getClass().getSimpleName(), Resource.this.name));
        }

        protected Function<Object, Object> resmapper() {
            return (new PoolMapper(Resource.this.pool));
        }
    }

    public interface LayerFactory<T extends Layer> {
        T cons(Resource res, Message buf);
    }

    public static class LayerConstructor<T extends Layer> implements LayerFactory<T> {
        public final Class<T> cl;
        private final Constructor<T> cons;

        public LayerConstructor(Class<T> cl) {
            this.cl = cl;
            try {
                this.cons = cl.getConstructor(Resource.class, Message.class);
            } catch (NoSuchMethodException e) {
                throw (new RuntimeException("No proper constructor found for layer type " + cl.getName(), e));
            }
        }

        @Override
        public T cons(Resource res, Message buf) {
            try {
                return (cons.newInstance(res, buf));
            } catch (InstantiationException | IllegalAccessException e) {
                throw (new LoadException(e, res));
            } catch (InvocationTargetException e) {
                Throwable c = e.getCause();
                if (c instanceof RuntimeException)
                    throw ((RuntimeException) c);
                else
                    throw (new LoadException(e, res));
            }
        }
    }

    public static void addltype(String name, LayerFactory<?> cons) {
        ltypes.put(name, cons);
    }

    public static <T extends Layer> void addltype(String name, Class<T> cl) {
        addltype(name, new LayerConstructor<>(cl));
    }

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LayerName {
        String value();
    }

    static {
        l10nBundleMap = new HashMap<String, Map<String, String>>(9) {{
            if (!language.equals("en") || Resource.L10N_DEBUG) {
                put(BUNDLE_TOOLTIP, l10n(BUNDLE_TOOLTIP, language));
                put(BUNDLE_PAGINA, l10n(BUNDLE_PAGINA, language));
                put(BUNDLE_WINDOW, l10n(BUNDLE_WINDOW, language));
                put(BUNDLE_BUTTON, l10n(BUNDLE_BUTTON, language));
                put(BUNDLE_FLOWER, l10n(BUNDLE_FLOWER, language));
                put(BUNDLE_MSG, l10n(BUNDLE_MSG, language));
                put(BUNDLE_LABEL, l10n(BUNDLE_LABEL, language));
                put(BUNDLE_ACTION, l10n(BUNDLE_ACTION, language));
                put(BUNDLE_INGREDIENT, l10n(BUNDLE_INGREDIENT, language));
            }
        }};

        for (Class<?> cl : dolda.jglob.Loader.get(LayerName.class).classes()) {
            String nm = cl.getAnnotation(LayerName.class).value();
            if (LayerFactory.class.isAssignableFrom(cl)) {
                addltype(nm, (LayerFactory<?>) Utils.construct(cl.asSubclass(LayerFactory.class)));
            } else if (Layer.class.isAssignableFrom(cl)) {
                addltype(nm, cl.asSubclass(Layer.class));
            } else {
                throw (new Error("Illegal resource layer class: " + cl));
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, String> l10n(String bundle, String langcode) {
//        Properties props = new Properties();

//        InputStream is = Config.class.getClassLoader().getResourceAsStream("l10n/" + bundle + "_" + langcode + ".properties");
        InputStream is = Config.class.getClassLoader().getResourceAsStream("l10n/" + langcode + "/" + bundle + ".json");
        if (is == null)
            return null;

//        InputStreamReader isr = null;
//        try {
//            isr = new InputStreamReader(is, "UTF-8");
//            props.load(isr);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (isr != null) {
//                try {
//                    isr.close();
//                } catch (IOException e) { // ignored
//                }
//            }
//        }

        Map<String, String> map = new HashMap<>();
        try {
            String result = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            JSONObject obj = new JSONObject(result);
            for (String key : obj.keySet()) {
                map.put(key, obj.getString(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        return props.size() > 0 ? new HashMap<>((Map) props) : null;
        return !map.isEmpty() ? map : null;
    }

    public interface IDLayer<T> {
        T layerid();
    }

    public static class ImageReadException extends IOException {
        public final String[] supported = ImageIO.getReaderMIMETypes();

        public ImageReadException() {
            super("Could not decode image data");
        }
    }

    public static BufferedImage readimage(InputStream fp) throws IOException {
        try {
            /* This can crash if not privileged due to ImageIO
             * creating tempfiles without doing that privileged
             * itself. It can very much be argued that this is a bug
             * in ImageIO. */
            return (AccessController.doPrivileged((PrivilegedExceptionAction<BufferedImage>) () -> {
                BufferedImage ret;
                ret = ImageIO.read(fp);
                if (ret == null)
                    throw (new ImageReadException());
                return (ret);
            }));
        } catch (PrivilegedActionException e) {
            Throwable c = e.getCause();
            if (c instanceof IOException)
                throw ((IOException) c);
            throw (new AssertionError(c));
        }
    }

    @LayerName("image")
    public class Image extends Layer implements Comparable<Image>, IDLayer<Integer> {
        public transient BufferedImage img, rawimage;
        private transient BufferedImage scaled;
        public final List<Pair<Double, BufferedImage>> scaleIList = new ArrayList<>();
        private transient TexI tex, rawtex;
        public final List<Pair<Double, TexI>> scaleTList = new ArrayList<>();
        public final int z, subz;
        public final boolean nooff;
        public final int id;
        public final Map<String, byte[]> kvdata;
        public float scale = 1;
        private int gay = -1;
        public Coord sz, o, so, rawtsz, tsz, ssz;

        public Image(Message buf) {
            z = buf.int16();
            subz = buf.int16();
            int fl = buf.uint8();
            /* Obsolete flag 1: Layered */
            nooff = (fl & 2) != 0;
            id = buf.int16();
            o = cdec(buf);
            so = UI.scale(o);
            Map<String, byte[]> kvdata = new HashMap<>();
            if ((fl & 4) != 0) {
                while (true) {
                    String key = buf.string();
                    if (key.equals(""))
                        break;
                    int len = buf.uint8();
                    if ((len & 0x80) != 0)
                        len = buf.int32();
                    byte[] data = buf.bytes(len);
                    Message val = new MessageBuf(data);
                    if (key.equals("tsz")) {
                        tsz = val.coord();
                    } else if (key.equals("scale")) {
                        scale = val.float32();
                    } else {
                        kvdata.put(key, data);
                    }
                }
            }
            this.kvdata = kvdata.isEmpty() ? Collections.emptyMap() : kvdata;
            try {
                img = readimage(new MessageInputStream(buf));
                rawimage = img;
                configuration.decodeimage(rawimage, Resource.this, "image", "");
            } catch (IOException e) {
                throw (new LoadException(e, Resource.this));
            }
            /*if (img == null)
                throw (new LoadException("Invalid image data in " + name, Resource.this));*/
            sz = Utils.imgsz(img);
            if (tsz == null)
                tsz = sz;
            rawtsz = tsz;
            ssz = UI.scale(sz.div(scale));
            if (scale != UI.getScale()) {
                img = scaled();
                sz = ssz;
            }
            tsz = sz;
            if (rawtsz != null) {
                /* This seems kind of ugly, but I'm not sure how to
                 * otherwise handle upwards rounding of both offset
                 * and size getting the image out of the intended
                 * area. */
                so = new Coord(Math.min(so.x, rawtsz.x - ssz.x), Math.min(so.y, sz.y - ssz.y));
            }
        }

        public BufferedImage scaled() {
            if (scaled == null) {
                synchronized (this) {
                    if (scaled == null)
                        scaled = PUtils.uiscale(img, ssz);
                }
            }
            return (scaled);
        }

        public BufferedImage scaled(double scale) {
            return (scaleIList.stream().filter(p -> p.a.equals(scale)).findFirst().orElseGet(() -> {
                Coord sz = Utils.imgsz(rawimage);
                Pair<Double, BufferedImage> pp = new Pair<>(scale, PUtils.uiscale(rawimage, Coord.of((int) (sz.x / this.scale * scale), (int) (sz.y / this.scale * scale))));
                scaleIList.add(pp);
                return (pp);
            }).b);
        }

        public Tex rawtex() {
            if (rawtex == null) {
                synchronized (this) {
                    if (rawtex == null) {
                        rawtex = new TexI(rawimage) {
                            public String toString() {
                                return ("TexI(" + Resource.this.name + ", " + id + ")");
                            }
                        };
                    }
                }
            }
            return (rawtex);
        }

        public Tex tex() {
            if (tex == null) {
                synchronized (this) {
                    if (tex == null) {
                        tex = new TexI(scaled()) {
                            public String toString() {
                                return ("TexI(" + Resource.this.name + ", " + id + ")");
                            }
                        };
                    }
                }
            }
            return (tex);
        }

        public Tex tex(double scale) {
            return (scaleTList.stream().filter(p -> p.a.equals(scale)).findFirst().orElseGet(() -> {
                Pair<Double, TexI> pp = new Pair<>(scale, new TexI(scaled(scale)) {
                    public String toString() {
                        return ("TexI(" + Resource.this.name + ", " + id + ")");
                    }
                });
                scaleTList.add(pp);
                return (pp);
            }).b);
        }

        public TexI texi() {
            if (tex == null) {
                synchronized (this) {
                    if (tex == null) {
                        tex = new TexI(scaled()) {
                            public String toString() {
                                return ("TexI(" + Resource.this.name + ", " + id + ")");
                            }
                        };
                    }
                }
            }
            return (tex);
        }

        private boolean detectgay() {
            for (int y = 0; y < sz.y; y++) {
                for (int x = 0; x < sz.x; x++) {
                    if ((img.getRGB(x, y) & 0x00ffffff) == 0x00ff0080)
                        return (true);
                }
            }
            return (false);
        }

        public boolean gayp() {
            if (gay == -1)
                gay = detectgay() ? 1 : 0;
            return (gay == 1);
        }

        @Override
        public int compareTo(Image other) {
            return (z - other.z);
        }

        @Override
        public Integer layerid() {
            return (id);
        }

        @Override
        public void init() {}
    }

    @LayerName("tooltip")
    public class Tooltip extends Layer {
        public final String t;
        public final String origt;

        public Tooltip(Message buf) {
            String text = new String(buf.bytes(), Utils.utf8);
            origt = text;
            Resource res = super.getres();
            String locText = getLocString(BUNDLE_TOOLTIP, res, text);

            if (!language.equals("en")) {
                if (locText.equals(text) || !res.name.startsWith("gfx/invobjs") ||
                        // exclude meat "conditions" since the tooltip is dynamically generated and it won't be in right order
                        text.contains("Raw ") || text.contains("Filet of ") || text.contains("Sizzling") ||
                        text.contains("Roast") || text.contains("Meat") || text.contains("Spitroast") ||
                        // exclude food conditions
                        res.name.startsWith("gfx/invobjs/food/")) {
                    this.t = locText;
                } else {
                    this.t = locText + " (" + text + ")";
                }
                return;
            }
            this.t = locText;
        }

        @Override
        public void init() {}
    }

    @LayerName("neg")
    public class Neg extends Layer {
        public Coord cc;
        public Coord bc, bs, sz;
        public Coord[][] ep;

        public Neg(Message buf) {
            cc = cdec(buf);
            bc = cdec(buf);
            bs = cdec(buf);
            sz = cdec(buf);
            //buf.skip(12);
            ep = new Coord[8][0];
            int en = buf.uint8();
            for (int i = 0; i < en; i++) {
                int epid = buf.uint8();
                int cn = buf.uint16();
                ep[epid] = new Coord[cn];
                for (int o = 0; o < cn; o++)
                    ep[epid][o] = cdec(buf);
            }
//            dev.resourceLog("neg", Resource.this, toString());
        }

        @Override
        public void init() {}

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("<Neg ");
            sb.append(cc);
            sb.append(bc);
            sb.append(bs);
            sb.append(sz);
            for (Coord[] ac : ep) {
                if (ac.length > 0) {
                    sb.append("[");
                    for (Coord c : ac) {
                        sb.append(c);
                    }
                    sb.append("]");
                }
            }
            sb.append(">");
            return sb.toString();
        }
    }

    @LayerName("props")
    public class Props extends Layer {
        public final Map<String, Object> props = new HashMap<>();

        public Props(Message buf) {
            int ver = buf.uint8();
            if (ver != 1)
                throw (new LoadException("Unknown property layer version: " + ver, getres()));
            Object[] raw = buf.list();
            for (int a = 0; a < raw.length - 1; a += 2)
                props.put((String) raw[a], raw[a + 1]);
        }

        public Object get(String nm) {
            return (props.get(nm));
        }

        public void init() {}
    }

    @LayerName("obst")
    public class Obstacle extends Layer implements IDLayer<String> {
        public int vc;
        public Coord2d[][] ep;
        public String id;

        public Obstacle(Message buf) {
            int ver = buf.uint8();
            if ((ver >= 1) && (ver <= 2)) {
                this.id = (ver >= 2) ? buf.string() : "";
                int points = buf.uint8();
                int[] boxes = new int[points];
                ep = new Coord2d[points][];
                for (int i = 0; i < points; i++) {
                    boxes[i] = buf.uint8();
                    ep[i] = new Coord2d[boxes[i]];
                }
                for (int i = 0; i < points; i++) {
                    for (int o = 0; o < boxes[i]; o++)
                        ep[i][o] = Coord2d.of(buf.float16(), buf.float16()).mul(MCache.tilesz);
                    vc += boxes[i];
                }
            } else {
                this.id = "#";
                this.ep = new Coord2d[0][];
            }
//            dev.resourceLog("Obst", name, vc, id, Arrays.toString(vertices));
        }

        @Override
        public void init() {}

        public String layerid() {return (id);}

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("<Obst ");
            sb.append(name).append(" ");
            sb.append(vc).append(" ");
            sb.append(id).append(" ");
            sb.append(Arrays.deepToString(ep));
            sb.append(">");
            return sb.toString();
        }
    }

    @LayerName("anim")
    public class Anim extends Layer {
        private int[] ids;
        public int id, d;
        public Image[][] f;

        public Anim(Message buf) {
            id = buf.int16();
            d = buf.uint16();
            ids = new int[buf.uint16()];
            for (int i = 0; i < ids.length; i++)
                ids[i] = buf.int16();
        }

        @Override
        public void init() {
            f = new Image[ids.length][];
            Image[] typeinfo = new Image[0];
            for (int i = 0; i < ids.length; i++) {
                LinkedList<Image> buf = new LinkedList<>();
                for (Image img : layers(Image.class)) {
                    if (img.id == ids[i])
                        buf.add(img);
                }
                f[i] = buf.toArray(typeinfo);
            }
        }
    }

    @LayerName("pagina")
    public class Pagina extends Layer {
        public final String text;

        public Pagina(Message buf) {
            String text = new String(buf.bytes(), Utils.utf8);
            this.text = Resource.getLocString(Resource.BUNDLE_PAGINA, super.getres(), text);
        }

        @Override
        public void init() {}
    }

    @LayerName("action")
    public class AButton extends Layer {
        public final String name;
        public final Named parent;
        public final char hk;
        public final String[] ad;

        public AButton(Named parent, String name) {
            this.name = name;
            this.parent = parent;
            ad = null;
            hk = '\0';
        }

        public AButton(Named parent, String name, char h) {
            this.name = name;
            this.parent = parent;
            ad = null;
            hk = h;
        }

        public AButton(Message buf) {
            String pr = buf.string();
            int pver = buf.uint16();
            if (pr.length() == 0) {
                parent = null;
            } else {
                Named n;
                try {
                    n = pool.load(pr, pver);
                } catch (RuntimeException e) {
//                    throw (new LoadException("Illegal resource dependency", e, Resource.this));
                    dev.simpleLog("Illegal resource dependency " + e + Resource.this);
                    try {
                        n = pool.load(pr);
                    } catch (Exception ex) {
                        n = Resource.remote().load("gfx/invobjs/missing");
                    }
                }
                parent = n;
            }

            name = buf.string();

            buf.string(); /* Prerequisite skill */
            hk = (char) buf.uint16();
            ad = new String[buf.uint16()];
            for (int i = 0; i < ad.length; i++)
                ad[i] = buf.string();
        }

        @Override
        public void init() {}
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface PublishedCode {
        String name();

        Class<? extends Instancer> instancer() default Instancer.class;

        interface Instancer<I> {
            I make(Class<?> cl, Resource res, Object... args);

            static <T, U extends T> T stdmake(Class<T> type, Class<U> cl, Resource ires, Object[] args) {
                try {
                    Function<Object[], T> make = Utils.smthfun(cl, "instantiate", type, Resource.class, Object[].class);
                    return (make.apply(new Object[]{ires, args}));
                } catch (NoSuchMethodException e) {}
                try {
                    Function<Object[], T> make = Utils.smthfun(cl, "instantiate", type, Object[].class);
                    return (make.apply(new Object[]{args}));
                } catch (NoSuchMethodException e) {}
                try {
                    Function<Object[], T> make = Utils.smthfun(cl, "instantiate", type, Resource.class);
                    return (make.apply(new Object[]{ires}));
                } catch (NoSuchMethodException e) {}
                try {
                    Constructor<U> cons = cl.getConstructor(Resource.class, Object[].class);
                    return (Utils.construct(cons, new Object[]{ires, args}));
                } catch (NoSuchMethodException e) {}
                try {
                    Constructor<U> cons = cl.getConstructor(Object[].class);
                    return (Utils.construct(cons, new Object[]{args}));
                } catch (NoSuchMethodException e) {}
                try {
                    Constructor<U> cons = cl.getConstructor(Resource.class);
                    return (Utils.construct(cons, new Object[]{ires}));
                } catch (NoSuchMethodException e) {}
                return (Utils.construct(cl));
            }

            class Direct<I> implements Instancer<I> {
                public final Class<I> type;

                public Direct(Class<I> type) {
                    this.type = type;
                }

                public I make(Class<?> cl, Resource res, Object... args) {
                    if (!type.isAssignableFrom(cl))
                        return (null);
                    return (stdmake(type, cl.asSubclass(type), res, args));
                }
            }

            class StaticCall<I, R> implements Instancer<I> {
                public final Class<I> type;
                public final String name;
                public final Class<R> rtype;
                public final Class<?>[] args;
                public final Function<Function<Object[], R>, I> maker;

                public StaticCall(Class<I> type, String name, Class<R> rtype, Class<?>[] args, Function<Function<Object[], R>, I> maker) {
                    this.type = type;
                    this.name = name;
                    this.rtype = rtype;
                    this.args = args;
                    this.maker = maker;
                }

                public I make(Class<?> cl, Resource res, Object... args) {
                    Function<Object[], R> make;
                    try {
                        make = Utils.smthfun(cl, name, rtype, this.args);
                    } catch (NoSuchMethodException e) {
                        return (null);
                    }
                    return (maker.apply(make));
                }
            }

            class Construct<I, R> implements Instancer<I> {
                public final Class<I> type;
                public final Class<R> rtype;
                public final Class<?>[] args;
                public final Function<Function<Object[], ? extends R>, I> maker;

                public Construct(Class<I> type, Class<R> rtype, Class<?>[] args, Function<Function<Object[], ? extends R>, I> maker) {
                    this.type = type;
                    this.rtype = rtype;
                    this.args = args;
                    this.maker = maker;
                }

                public I make(Class<?> cl, Resource res, Object... args) {
                    if (!rtype.isAssignableFrom(cl))
                        return (null);
                    Class<? extends R> scl = cl.asSubclass(rtype);
                    Function<Object[], ? extends R> cons;
                    try {
                        cons = Utils.consfun(scl, this.args);
                    } catch (NoSuchMethodException e) {
                        return (null);
                    }
                    return (maker.apply(cons));
                }
            }

            class Chain<I> implements Instancer<I> {
                public final Class<I> type;
                private final Collection<Instancer<? extends I>> sub = new ArrayList<>();

                public Chain(Class<I> type) {
                    this.type = type;
                }

                public void add(Instancer<? extends I> el) {
                    sub.add(el);
                }

                public I make(Class<?> cl, Resource res, Object... args) {
                    for (Instancer<? extends I> el : sub) {
                        I inst = type.cast(el.make(cl, res, args));
                        if (inst != null)
                            return (inst);
                    }
                    throw (new RuntimeException(String.format("Could not find any suitable constructor for %s in %s", type, cl)));
                }
            }

            Instancer<Object> simple = (cl, res, args) -> (stdmake(Object.class, cl, res, args));
        }

        Map<PublishedCode, Instancer> instancers = new WeakHashMap<>();
    }

    @LayerName("code")
    public class Code extends Layer {
        public final String name;
        public final transient byte[] data;

        public Code(Message buf) {
            name = buf.string();
            data = buf.bytes();
            if (dev.decodeCode) decode();
        }

        @Override
        public void init() {}

        public void decode() {
            File dir = new File("decode" + File.separator + Resource.this.toString().replace("/", File.separator));
            dir.mkdirs();
            String filename = name.substring(name.lastIndexOf('.') + 1) + ".class";
            File f = new File(dir, filename);
            if (!f.exists()) {
                Defer.later(() -> {
                    try {
                        FileOutputStream fout = new FileOutputStream(f);
                        fout.write(data);
                        fout.flush();
                        fout.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    dev.resourceLog("code", f.getPath(), "CREATED");
                    return (null);
                });
            }
        }
    }

    @LayerName("src")
    public class Src extends Layer {
        public final int type;
        public final String name;
        transient public final byte[] data;

        public Src(Message buf) {
            type = buf.uint8();
            name = buf.string();
            data = buf.bytes();
            if (dev.decodeCode) decode();
        }

        @Override
        public void init() {}

        public void decode() {
            Path path = Paths.get("decode" + File.separator + Resource.this.toString().replace("/", File.separator));
            if (!Files.exists(path.resolve(name))) {
                Defer.later(() -> {
                    try {
                        Files.createDirectories(path);
                        Files.write(path.resolve(name), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    dev.resourceLog("src", path.resolve(name), "CREATED");
                    return (null);
                });
            }
        }
    }

    private static final List<String> overridedResources = new ArrayList<>();
    protected static final Map<String, Resource> classResources = new HashMap<>();

    static { //don't work
        for (Class<?> cl : dolda.jglob.Loader.get(FromResource.class).classes()) {
            String nm = cl.getAnnotation(FromResource.class).name();
            overridedResources.add(nm);
        }
    }

    public class CustomClassLoader extends ClassLoader {
        public CustomClassLoader(ClassLoader parent) {
            super(parent);
        }

        public FromResource getsource(Class<?> cl) {
            for (; cl != null; cl = cl.getEnclosingClass()) {
                FromResource src = cl.getAnnotation(FromResource.class);
                if (src != null) return (src);
            }
            return (null);
        }

        public Resource getres() {
            return (Resource.this);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            char point = '.';
            int p = name.lastIndexOf(point);
            String res = getres().name.replace('/', point).replace("-", "_");
            String resname = res + point + (p < 0 ? name : name.substring(name.lastIndexOf(point) + 1));
            String text = "haven.res" + point + resname;
            Class<?> cl = super.loadClass(text);
            classResources.put(text, getres());
            try {
                FromResource src = getsource(cl);
                if (src != null && src.override()) {
                    if (src.version() != getres().ver) {
                        dev.simpleLog(String.format("New version of %s is available %d -> %d. Please inform the developer!", text, getres().ver, src.version()));
                    }
                }
                String fmt = "local copy of %s (%s) would be overridden by code from %s";

                Warning.warn(fmt, name, (src == null) ? "unannotated" : String.format("fetched from %s v%d", src.name(), src.version()), String.format("%s v%d", getres().name, getres().ver));
            } catch (Exception e) {
                dev.simpleLog(String.format("%s %s %s %s", name, text, res, e.getMessage()));
                classResources.remove(text);
                throw (e);
            }
            return (cl);
        }
    }

    public class ResClassLoader extends ClassLoader {
        public ResClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Resource getres() {
            return (Resource.this);
        }

        public String toString() {
            return ("cl:" + Resource.this.toString());
        }
    }

    public static Resource classres(final Class<?> cl) {
        return (AccessController.doPrivileged((PrivilegedAction<Resource>) () -> {
            ClassLoader l = cl.getClassLoader();
            if (l instanceof ResClassLoader)
                return (((ResClassLoader) l).getres());
            Resource res = classResources.get(cl.getName());
            if (res != null)
                return (res);
            throw (new RuntimeException("Cannot fetch resource of non-resloaded class " + cl));
        }));
    }

    public <T> T getcode(Class<T> cl, boolean fail) {
        CodeEntry e = layer(CodeEntry.class);
        if (e == null) {
            if (fail)
                throw (new RuntimeException("Tried to fetch non-present res-loaded class " + cl.getName() + " from " + Resource.this));
//            else
//                dev.simpleLog(new RuntimeException("Tried to fetch non-present res-loaded class " + cl.getName() + " from " + Resource.this.name));
            return (null);
        }
        return (e.get(cl, fail));
    }

    public static class LibClassLoader extends ClassLoader {
        private final ClassLoader[] classpath;

        public LibClassLoader(ClassLoader parent, Collection<ClassLoader> classpath) {
            super(parent);
            this.classpath = classpath.toArray(new ClassLoader[0]);
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            for (ClassLoader lib : classpath) {
                try {
                    return (lib.loadClass(name));
                } catch (ClassNotFoundException e) {}
            }
            throw (new ClassNotFoundException("Could not find " + name + " in any of " + Arrays.asList(classpath).toString()));
        }
    }

    public static class ResourceClassNotFoundException extends ClassNotFoundException {
        public final String clname;
        public final Resource res;

        public ResourceClassNotFoundException(String clname, Resource res) {
            super(String.format("Could not find class %s in resource %s", clname, res));
            this.clname = clname;
            this.res = res;
        }
    }

    @LayerName("codeentry")
    public class CodeEntry extends Layer {
        private final Map<String, Code> clmap = new HashMap<>();
        private final Map<String, String> pe = new HashMap<>();
        private final Map<String, Object[]> pa = new HashMap<>();
        private final Collection<Indir<Resource>> classpath = new ArrayList<>();
        transient private ClassLoader loader;
        transient private final Map<String, Class<?>> lpe = new HashMap<>();
        transient private final Map<String, Object> ipe = new HashMap<>();

        public CodeEntry(Message buf) {
            while (!buf.eom()) {
                int t = buf.uint8();
                if ((t == 1) || (t == 3)) {
                    while (true) {
                        String en = buf.string();
                        String cn = buf.string();
                        if (en.length() == 0)
                            break;
                        pe.put(en, cn);
                        if (t == 3)
                            pa.put(en, buf.list());
                    }
                } else if (t == 2) {
                    while (true) {
                        String ln = buf.string();
                        if (ln.length() == 0)
                            break;
                        int ver = buf.uint16();
                        classpath.add(pool.load(ln, ver));
                    }
                } else {
                    throw (new LoadException("Unknown codeentry data type: " + t, Resource.this));
                }
            }
        }

        @Override
        public void init() {
            for (Code c : layers(Code.class))
                clmap.put(c.name, c);
//            StringBuilder sb = new StringBuilder();
//            pe.forEach((k, v) -> sb.append(k).append("=").append(v).append('\n'));
//            pa.forEach((k, v) -> sb.append(k).append("=").append(Arrays.deepToString(v)).append('\n'));
//            sb.append('\n').append("[CODE]").append('\n');
//            clmap.forEach((k, v) -> sb.append(k).append('\n'));
//            System.out.printf("Resource %s -> %s%n", Resource.this, sb);
        }

        public ClassLoader loader() {
            synchronized (CodeEntry.this) {
                if (this.loader == null) {
                    this.loader = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
                        ClassLoader ret = Resource.class.getClassLoader();
                        if (overridedResources.contains(Resource.this.name))
                            ret = new CustomClassLoader(ret);
                        if (!classpath.isEmpty()) {
                            Collection<ClassLoader> loaders = new LinkedList<>();
                            for (Indir<Resource> res : classpath) {
                                CodeEntry ce = res.get().layer(CodeEntry.class);
                                if (ce != null)
                                    loaders.add(ce.loader());
                            }
                            ret = new LibClassLoader(ret, loaders);
                        }
                        if (!clmap.isEmpty()) {
                            ret = new ResClassLoader(ret) {
                                @Override
                                public Class<?> findClass(String name) throws ClassNotFoundException {
                                    Code c = clmap.get(name);
                                    if (c == null)
                                        throw (new ResourceClassNotFoundException(name, Resource.this));
                                    try {
                                        return (defineClass(name, c.data, 0, c.data.length));
                                    } catch (Throwable e) {
                                        String point = ".";
                                        String text = name;
                                        String res = getres().name.replace("/", point);
                                        int p = name.lastIndexOf(point);
                                        String resname = res + point + (p < 0 ? name : name.substring(p + 1));
                                        if (!name.startsWith("haven")) {
                                            text = (p < 0 ? String.format("haven%sres%s%s", point, point, resname) : name).replace("-", "_").replaceAll("[.](\\d)(.*)", "._$1$2");
                                        }
                                        return (defineClass(text, c.data, 0, c.data.length));
                                    }
                                }
                            };
                        }
                        return (ret);
                    });
                }
            }
            return (this.loader);
        }

        private Class<?> getentry(Class<?> cl, boolean fail) {
            PublishedCode entry = cl.getAnnotation(PublishedCode.class);
            if (entry == null)
                throw (new RuntimeException("Tried to fetch non-published res-loaded class " + cl.getName() + " from " + Resource.this.name));
            synchronized (CodeEntry.this) {
                Class<?> ret = lpe.get(entry.name());
                if (ret == null) {
                    String clnm = pe.get(entry.name());
                    if (clnm == null) {
                        if (fail)
                            throw (new RuntimeException("Tried to fetch non-present res-loaded class " + cl.getName() + "(" + entry.name() + ") from " + Resource.this.name));
//                        else
//                            dev.simpleLog(new RuntimeException("Tried to fetch non-present res-loaded class " + cl.getName() + " from " + Resource.this.name));
                        return (null);
                    }
                    try {
                        ret = loader().loadClass(clnm);
                    } catch (ClassNotFoundException e) {
                        throw (new LoadException(e, Resource.this));
                    }
                    lpe.put(entry.name(), ret);
                }
                return (ret);
            }
        }

        public <T> Class<? extends T> getcl(Class<T> cl, boolean fail) {
            Class<?> acl = getentry(cl, fail);
            if (acl == null)
                return (null);
            try {
                return (acl.asSubclass(cl));
            } catch (ClassCastException e) {
                throw (new RuntimeException(String.format("Illegal entry-point class specified for %s in %s", cl.getName(), Resource.this.name), e));
            }
        }

        public <T> Class<? extends T> getcl(Class<T> cl) {
            return (getcl(cl, true));
        }

        public <T> T get(Class<T> cl, boolean fail) {
            PublishedCode entry = cl.getAnnotation(PublishedCode.class);
            if (entry == null)
                throw (new RuntimeException("Tried to fetch non-published res-loaded class " + cl.getName() + " from " + Resource.this.name));
            synchronized (CodeEntry.this) {
                Object inst;
                if ((inst = ipe.get(entry.name())) == null) {
                    Class<?> acl = getentry(cl, fail);
                    if (acl == null)
                        return (null);
                    Object[] args = pa.getOrDefault(entry.name(), new Object[0]);
                    inst = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                        PublishedCode.Instancer mk;
                        synchronized (PublishedCode.instancers) {
                            mk = PublishedCode.instancers.computeIfAbsent(entry, k -> {
                                if (k.instancer() == PublishedCode.Instancer.class)
                                    return (PublishedCode.Instancer.simple);
                                else
                                    return (Utils.construct(k.instancer()));
                            });
                        }
                        return (mk.make(acl, Resource.this, args));
                    });
                    ipe.put(entry.name(), inst);
                }
                try {
                    return (cl.cast(inst));
                } catch (ClassCastException e) {
                    throw (new RuntimeException(String.format("Illegal entry-point class specified for %s in %s", entry.name(), Resource.this.name), e));
                }
            }
        }

        public <T> T get(Class<T> cl) {
            return (get(cl, true));
        }
    }

    @LayerName("audio")
    public class Audio extends Layer implements IDLayer<String> {
        transient public byte[] coded;
        public final String id;
        public double bvol = 1.0;

        public Audio(byte[] coded, String id) {
            this.coded = coded;
            this.id = id.intern();
            if (dev.decodeCode) decode();
        }

        public void decode() {
            Path path = Paths.get("decode").resolve(Resource.this.toString().replace("/", File.separator));
            String filename = name.substring(name.replace("/", File.separator).lastIndexOf(File.separator) + 1);
            Path file = path.resolve(String.format("%s_%s_%s.wav", filename, id, Arrays.hashCode(coded)));
            if (!Files.exists(file)) {
                Defer.later(() -> {
                    try {
                        Files.createDirectories(path);
                        Files.write(file, coded, StandardOpenOption.CREATE_NEW);
                        dev.resourceLog("audio", file.toString(), "CREATED");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return (null);
                });
            }
        }

        public Audio(Message buf) {
            this(buf.bytes(), "cl");
        }

        @Override
        public void init() {}

        public haven.Audio.CS stream() {
            try {
                return (new haven.Audio.VorbisClip(new VorbisStream(new ByteArrayInputStream(coded))));
            } catch (IOException e) {
                throw (new RuntimeException(e));
            }
        }

        @Override
        public String layerid() {
            return (id);
        }
    }

    @LayerName("audio2")
    public static class Audio2 implements LayerFactory<Audio> {
        @Override
        public Audio cons(Resource res, Message buf) {
            int ver = buf.uint8();
            if ((ver == 1) || (ver == 2)) {
                String id = buf.string();
                double bvol = 1.0;
                if (ver == 2)
                    bvol = buf.uint16() / 1000.0;
                Audio ret = res.new Audio(buf.bytes(), id);
                ret.bvol = bvol;
                return (ret);
            } else {
                throw (new LoadException("Unknown audio layer version: " + ver, res));
            }
        }
    }

    @LayerName("midi")
    public class Music extends Resource.Layer {
        transient Sequence seq;

        public Music(Message buf) {
            try {
                seq = MidiSystem.getSequence(new MessageInputStream(buf));
                if (dev.decodeCode) decode();
            } catch (InvalidMidiDataException e) {
                throw (new LoadException("Invalid MIDI data", Resource.this));
            } catch (IOException e) {
                throw (new LoadException(e, Resource.this));
            }
        }

        @Override
        public void init() {}

        public void decode() {
            File dir = new File("decode" + File.separator + Resource.this.toString().replace("/", File.separator));
            dir.mkdirs();
            if (seq == null) {
                dev.resourceLog("midi", dir.getPath(), "NULL");
                return;
            }
            int[] types = MidiSystem.getMidiFileTypes(seq);
            if (types.length == 0) {
                dev.resourceLog("midi", dir.getPath(), "FAIL TYPE");
                return;
            } else {
                for (int i : types) {
                    String filename = name.substring(name.lastIndexOf('/') + 1) + "_" + i + ".mid";
                    File outputfile = new File(dir, filename);
                    if (!outputfile.exists()) {
                        Defer.later(() -> {
                            try {
                                MidiSystem.write(seq, i, outputfile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            dev.resourceLog("midi", outputfile.getPath(), "CREATED");
                            return (null);
                        });
                    }
                }
            }

        }
    }

    @LayerName("font")
    public class Font extends Layer {
        public transient final java.awt.Font font;

        public Font(Message buf) {
            int ver = buf.uint8();
            if (ver == 1) {
                int type = buf.uint8();
                if (type == 0) {
                    try {
                        this.font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new MessageInputStream(buf));
                    } catch (Exception e) {
                        throw (new RuntimeException(e));
                    }
                } else {
                    throw (new LoadException("Unknown font type: " + type, Resource.this));
                }
            } else {
                throw (new LoadException("Unknown font layer version: " + ver, Resource.this));
            }
        }

        @Override
        public void init() {}
    }

    private void readall(InputStream in, byte[] buf) throws IOException {
        int ret, off = 0;
        while (off < buf.length) {
            ret = in.read(buf, off, buf.length - off);
            if (ret < 0)
                throw (new LoadException("Incomplete resource at " + name, this));
            off += ret;
        }
    }

    public Collection<Layer> layers() {
        return (layers);
    }

    public <L extends Layer> Collection<L> layers(final Class<L> cl) {
        used = true;
        return ((DefaultCollection<L>) () -> (Utils.filter(layers.iterator(), cl)));
    }

    public static class NoSuchLayerException extends NoSuchElementException {
        public NoSuchLayerException(String message) {
            super(message);
        }
    }

    public <L extends Layer> L layer(Class<L> cl) {
        used = true;
        for (Layer l : layers) {
            if (cl.isInstance(l))
                return (cl.cast(l));
        }
        return (null);
    }

    public <L extends Layer> L flayer(Class<L> cl) {
        L l = layer(cl);
        if (l == null) throw (new NoSuchLayerException("no " + cl + " in " + name + " " + ver + "(" + realVer + ")"));
        return (l);
    }

    public <I, L extends IDLayer<I>> L layer(Class<L> cl, I id) {
        used = true;
        for (Layer l : layers) {
            if (cl.isInstance(l)) {
                L ll = cl.cast(l);
                if (ll.layerid().equals(id))
                    return (ll);
            }
        }
        return (null);
    }

    public <I, L extends IDLayer<I>> L flayer(Class<L> cl, I id) {
        L l = layer(cl, id);
        if (l == null) throw (new NoSuchLayerException("no " + cl + " in " + name + " " + ver + "(" + realVer + ")" + " with id " + id));
        return (l);
    }

    public boolean equals(Object other) {
        if (!(other instanceof Resource))
            return (false);
        Resource o = (Resource) other;
        return (o.name.equals(this.name) && (o.ver == this.ver));
    }

    private static final List<String> depresList = Arrays.asList("gfx/hud/rosters/.*", "gfx/terobjs/cupboard", "gfx/terobjs/crate", "ui/inspect", "ui/croster", "lib/icave", "gfx/terobjs/", "gfx/terobjs/consobj.*", "ui/obj/buddy.*", "gfx/terobjs/peacebreaker.*", "lib/vmat", "gfx/terobjs/arch/upstairs", "lib/svaj", "gfx/terobjs/items/huntersquiver", "gfx/terobjs/items/huntersquiver-pounch", "gfx/terobjs/items/huntersbow", "gfx/terobjs/items/creel", "gfx/terobjs/items/leatherpurse", "gfx/terobjs/items/silkpurse", "gfx/terobjs/items/glassjug", "gfx/fx/eq");

    private Object[] load(InputStream st) throws IOException {
        LoadException exception = null;
        MessageBuf in = new MessageBuf(new StreamMessage(st));
        if (in.eom()) throw new Message.EOF("Empty stream of " + this);
        byte[] sig = "Haven Resource 1".getBytes(Utils.ascii);
        if (!Arrays.equals(sig, in.bytes(sig.length))) {
            dev.simpleLog(String.format("Invalid res signature %s", this));
            throw (new LoadException("Invalid res signature", this));
        }
        int ver = in.uint16();
        if (this.ver == -1)
            this.ver = ver;
        else if (ver == 0 || ver == -1)
            ver = this.ver;
        else if (ver != this.ver) {
            if (dev.reslog)
                dev.simpleLog(String.format("Wrong res version (%d != %d) %s", ver, this.ver, this));
            System.out.printf("Wrong res version (%d != %d) %s%n", ver, this.ver, this);
            if (depresList.stream().noneMatch(name::matches))
                exception = new LoadException("Wrong res version (" + ver + " != " + this.ver + ")", this);
//            if (depresList.stream().noneMatch(name::matches))
//                throw (exception);
        }
        configuration.saveResource(this, in.clone());
        //to load layers
        return (new Object[]{ver, exception, in});
    }

    private void loadlayers(Message in) {
        try {
            List<Layer> layers = new LinkedList<>();
            List<Pair<String, Integer>> layerList = new ArrayList<>();
            while (!in.eom()) {
                String title = in.string();
                LayerFactory<?> lc = ltypes.get(title);
                int len = in.int32();
                layerList.add(new Pair<>(title, len));
                if (lc == null) {
                    in.skip(len);
                    continue;
                }
                Message buf = new LimitMessage(in, len);
                layers.add(lc.cons(this, buf));
                buf.skip();
            }
            configuration.decodeLayers(this, layerList);
            this.layers = layers;
            for (Layer l : layers)
                l.init();
            used = false;
        } catch (Throwable e) {
            dev.simpleLog(String.format("Error: %s from %s:%s", this, pool, src));
            dev.simpleLog(e);
            throw (e);
        }
    }

    private transient Named indir = null;

    public Named indir() {
        if (indir != null)
            return (indir);
        class Ret extends Named implements Serializable {
            Ret(String name, int ver) {
                super(name, ver);
            }

            @Override
            public Resource get() {
                return (Resource.this);
            }

            public String toString() {
                return (String.format("<indir:%s(v%d)>", name, ver));
            }
        }
        indir = new Ret(name, ver);
        return (indir);
    }

    public static Image loadrimg(String name) {
        return (remote().loadwait(name).layer(imgc));
    }

    public static BufferedImage loadimgraw(String name) {
        return (remote().loadwait(name).layer(imgc).rawimage);
    }

    public static BufferedImage loadimg(String name) {
        return (remote().loadwait(name).layer(imgc).scaled());
    }

    public static BufferedImage loadimg(final String name, final int id) {
        final Resource res = remote().loadwait(name);
        final Collection<Image> imgs = res.layers(imgc);
        for (Image img : imgs) {
            if (img.id == id) {
                return img.img;
            }
        }
        throw new RuntimeException("Failed to find img for " + name + " - id: " + id);
    }

    public static BufferedImage loadsimg(String name) {
        return (remote().loadwait(name).layer(imgc).scaled());
    }

    public static Tex loadtex(final String name, final int id) {
        final Resource res = remote().loadwait(name);
        final Collection<Image> imgs = res.layers(imgc);
        for (Image img : imgs) {
            if (img.id == id) {
                return img.tex();
            }
        }
        throw new RuntimeException("Failed to find tex for " + name + " - id: " + id);
    }

    public static Tex loadtex(String name) {
        return (remote().loadwait(name).layer(imgc).tex());
    }

    public static Tex loadtex(Resource res) {
        return (remote().loadwaited(res).layer(imgc).tex());
    }

    public String toString() {
        return (name + "(v" + ver + ")");
    }

    public static void loadlist(Pool pool, InputStream list, int prio) throws IOException {
        if (list == null) return;
        BufferedReader in = new BufferedReader(new InputStreamReader(list, "us-ascii"));
        String ln;
        while ((ln = in.readLine()) != null) {
            int pos = ln.indexOf(':');
            if (pos < 0)
                continue;
            String nm = ln.substring(0, pos);
            int ver;
            try {
                ver = Integer.parseInt(ln.substring(pos + 1));
            } catch (NumberFormatException e) {
                continue;
            }
            try {
                pool.load(nm, ver, prio);
            } catch (RuntimeException e) {
            }
        }
        in.close();
    }

    public static void dumplist(Collection<Resource> list, Writer dest) {
        PrintWriter out = new PrintWriter(dest);
        List<Resource> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing(a -> a.name));
        for (Resource res : sorted)
            out.println(res.name + ":" + res.ver);
    }

    public static void updateloadlist(File file, File resdir) throws Exception {
        BufferedReader r = new BufferedReader(new FileReader(file));
        Map<String, Integer> orig = new HashMap<>();
        String ln;
        while ((ln = r.readLine()) != null) {
            int pos = ln.indexOf(':');
            if (pos < 0) {
                System.err.println("Weird line: " + ln);
                continue;
            }
            String nm = ln.substring(0, pos);
            int ver = Integer.parseInt(ln.substring(pos + 1));
            orig.put(nm, ver);
        }
        r.close();
        Pool pool = new Pool(new FileSource(resdir));
        for (String nm : orig.keySet())
            pool.load(nm);
        while (true) {
            int d = pool.qdepth();
            if (d == 0)
                break;
            System.out.print("\033[1GLoading... " + d + "\033[K");
            Thread.sleep(500);
        }
        System.out.println();
        Collection<Resource> cur = new LinkedList<>();
        for (Map.Entry<String, Integer> e : orig.entrySet()) {
            String nm = e.getKey();
            int ver = e.getValue();
            Resource res = Loading.waitfor(pool.load(nm));
            if (res.ver != ver)
                System.out.println(nm + ": " + ver + " -> " + res.ver);
            cur.add(res);
        }
        Writer w = new OutputStreamWriter(Files.newOutputStream(file.toPath()), "UTF-8");
        try {
            dumplist(cur, w);
        } finally {
            w.close();
        }
    }

    private static final String[] fmtLocStringsLabel = new String[]{
            "Health: %s",
            "Stamina: %s",
            "Energy: %s",
            "Pony Power: %s",
            "Hunger modifier: %s",
            "Food event bonus: %s",
            "Tell %s of your exploits",
            "Go laugh at %s",
            "Go rage at %s",
            "Go wave to %s",
            "Greet %s",
            "Visit %s",
            "Meeting %s",
            "%s's Biddings",
            "%s's Labors",
            "%s's Laundry List",
            "Believing in %s",
            "%s's Wild Hunt",
            "%s's Quest",
            "By %s's Command",
            "%s's Quarry",
            "Hunting for %s",
            "Grass, Stone, and %s",
            "%s's Tasks",
            "%s's Business",
            "%s Giving the Chase",
            "A Favor for %s",
            "%s's Laughter",
            "Blood for %s",
            "%s's Follies",
            "Under %s's Star",
            "%s's Story",
            "Errands for %s",
            "Affair with %s",
            "%s's Catch",
            "As %s Wishes",
            "Poaching for %s",
            "Crazy Old %s",
            "Flowers for %s",
            "Silly %s",
            "%s's Gathering",
            "In Name of %s",
            "%s's Dirty Laundry",
            "What %s Asked",
            "Tasked by %s",
            "What %s Asked",
            "One for %s",
            "Fair Game for %s",
            "Meditations on %s",
            "%s's Wild Harvest",
            "%s has invited you to join his party. Do you wish to do so?",
            "%s has requested to spar with you. Do you accept?",
            "Experience points gained: %s",
            "Here lies %s",
            "Create a level %d artifact"
    };

    private static final String[] fmtLocStringsFlower = new String[]{
            "Gild (%s%% chance)",
            //"Follow %s",
            //"Travel along %s",
            //"Connect %s",
            //	"Extend %s"
    };

    private static final String[] fmtLocStringsMsg = new String[]{
            "That land is owned by %s.",
            "The name of this charterstone is \"%s\".",
            "Will refill in %s days",
            "Will refill in %s hours",
            "Will refill in %s minutes",
            "Will refill in %s seconds",
            "Will refill in %s second",
            "Quality: %s",
            "%s%% grown",
            "The battering ram cannot be used until the glue has dried, in %s hours."
    };

    public static String getLocString(String bundle, String key) {
        Map<String, String> map = l10nBundleMap.get(bundle);
        if (map == null || key == null)
            return key;
        if (Resource.L10N_DEBUG)
            Resource.saveStrings(bundle, key, key);
        String ll = map.get(key);
        // strings which need to be formatted
        if (ll == null && bundle == BUNDLE_LABEL) {
            for (String s : fmtLocStringsLabel) {
                String llfmt = fmtLocString(map, key, s);
                if (llfmt != null)
                    return llfmt;
            }
        } else if (ll == null && bundle == BUNDLE_FLOWER) {
            for (String s : fmtLocStringsFlower) {
                String llfmt = fmtLocString(map, key, s);
                if (llfmt != null)
                    return llfmt;
            }
        } else if (ll == null && bundle == BUNDLE_MSG) {
            for (String s : fmtLocStringsMsg) {
                String llfmt = fmtLocString(map, key, s);
                if (llfmt != null)
                    return llfmt;
            }
        }
        return ll != null ? ll : key;
    }

    private static String fmtLocString(Map<String, String> map, String key, String s) {
        String ll = map.get(s);
        if (ll != null) {
            int vi = s.indexOf("%s");

            String sufix = s.substring(vi + 2);
            if (sufix.startsWith("%%"))                // fix for strings with escaped percentage sign
                sufix = sufix.substring(1);

            if (key.startsWith(s.substring(0, vi)) && key.endsWith(sufix))
                return String.format(ll, key.substring(vi, key.length() - sufix.length()));
        }
        return null;
    }

    public static String getLocString(String bundle, Resource key, String def) {
        Map<String, String> map = l10nBundleMap.get(bundle);
        if (map == null || key == null)
            return def;
        if (Resource.L10N_DEBUG)
            Resource.saveStrings(bundle, key.name, def);
        String ll = map.get(key.name);
        return ll != null ? ll : def;
    }

    public static String getLocContent(String str) {
        String loc;
        if ((loc = Resource.locContent(str, " l of ")) != null)
            return loc;
        if ((loc = Resource.locContent(str, " kg of ")) != null)
            return loc;
        if ((loc = Resource.locContent(str, " seeds of ")) != null)
            return loc;
        return str;
    }

    private static String locContent(String str, String type) {
        int i = str.indexOf(type);
        if (i > 0) {
            Map<String, String> map = l10nBundleMap.get(Resource.BUNDLE_LABEL);
            if (map == null)
                return str;
            String contName = str.substring(i);
            if (Resource.L10N_DEBUG)
                Resource.saveStrings(Resource.BUNDLE_LABEL, contName, contName);
            String locContName = map.get(contName);
            if (locContName != null)
                return str.substring(0, i) + locContName + " (" + str.substring(i + type.length()) + ")";
            return str;
        }
        return null;
    }

    private static void saveStrings(String bundle, String key, String val) {
        synchronized (Resource.class) {
            if (bundle.equals(BUNDLE_FLOWER)) {
                if (key.startsWith("Follow ") || key.startsWith("Travel along") ||
                        key.startsWith("Extend ") && key.startsWith("Connect "))
                    return;
            }

            Map<String, String> map = l10nBundleMap.get(bundle);

            if (bundle.equals(BUNDLE_TOOLTIP) &&
                    (key.startsWith("paginae/act") || key.startsWith("paginae/bld")
                            || key.startsWith("paginae/craft") || key.startsWith("paginae/gov")
                            || key.startsWith("paginae/pose") || key.startsWith("paginae/amber")
                            || key.startsWith("paginae/atk/ashoot") || key.startsWith("paginae/seid")))
                return;

            if (key == null || key.equals("") || val.equals(""))
                return;

            if (val.charAt(0) >= '0' && val.charAt(0) <= '9')
                return;

            if (key.startsWith("Village shield:") ||
                    key.endsWith("is ONLINE") || key.endsWith("is offline") ||
                    key.startsWith("Born to ") ||
                    key.equals("ui/r-enact"))
                return;

            if (bundle == BUNDLE_LABEL) {
                for (String s : fmtLocStringsLabel) {
                    if (fmtLocString(map, key, s) != null)
                        return;
                }
            } else if (bundle == BUNDLE_FLOWER) {
                for (String s : fmtLocStringsFlower) {
                    if (fmtLocString(map, key, s) != null)
                        return;
                }
            } else if (bundle == BUNDLE_MSG) {
                for (String s : fmtLocStringsMsg) {
                    if (fmtLocString(map, key, s) != null)
                        return;
                }
            }

            val = sanitizeVal(val);

            String valOld = map.get(key);
            if (valOld != null && sanitizeVal(valOld).equals(val))
                return;

            new File("l10n").mkdirs();

            CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
            encoder.onMalformedInput(CodingErrorAction.REPORT);
            encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            BufferedWriter out = null;
            try {
                map.put(key, val);
                key = key.replace(" ", "\\ ").replace(":", "\\:").replace("=", "\\=");
                if (key.startsWith("\\ "))
                    key = "\\u0020" + key.substring(2);
                if (key.endsWith("\\ "))
                    key = key.substring(0, key.length() - 2) + "\\u0020";
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("l10n/" + bundle + "_new.properties", true), encoder));
                out.write(key + " = " + val);
                out.newLine();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return;
        }
    }

    private static String sanitizeVal(String val) {
        val = val.replace("\\", "\\\\").replace("\n", "\\n").replace("\u0000", "");
        if (val.startsWith(" "))
            val = "\\u0020" + val.substring(1);
        if (val.endsWith(" "))
            val = val.substring(0, val.length() - 1) + "\\u0020";

        while (val.endsWith("\\n"))
            val = val.substring(0, val.length() - 2);

        return val;
    }

    public static void main(String[] args) throws Exception {
        String cmd = args[0].intern();
        if (cmd == "update") {
            updateloadlist(new File(args[1]), new File(args[2]));
        }
    }


    public static class FakeResource extends Resource {
        public final String realName;

        public FakeResource(String name, int ver) {
            super(null, "f:" + name, -1, ver);
            this.realName = name;

            layers.add(Resource.remote().loadwait("gfx/invobjs/missing").layer(imgc));
            MessageBuf tooltip = new MessageBuf();
            tooltip.addstring("f:" + realName);
            layers.add(new Tooltip(new MessageBuf(tooltip.wbuf)));
            dev.simpleLog("Fake resource: " + name + " v" + ver);
        }

        public FakeResource(Resource res) {
            this(res.name, res.ver);
        }

        @Override
        public String toString() {
            return ("f:" + realName + "(v" + ver + "(" + realVer + "))");
        }
    }
}
