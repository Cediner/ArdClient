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

import modification.dev;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GobIcon extends GAttrib {
    private static final int size = UI.scale(20);
    public static final PUtils.Convolution filter = new PUtils.Hanning(1);
    public final Indir<Resource> res;
    public final byte[] sdt;
    protected Image img;

    public GobIcon(Gob g) {
        super(g);
        this.res = null;
        this.sdt = new byte[0];
    }

    public GobIcon(Gob g, Indir<Resource> res) {
        super(g);
        this.res = res;
        this.sdt = new byte[0];
    }

    public GobIcon(Gob g, Indir<Resource> res, byte[] sdt) {
        super(g);
        this.res = res;
        this.sdt = sdt;
    }

    public abstract static class Icon {
        public static final Object[] nilid = new Object[0];
        public final OwnerContext owner;
        public final Resource res;

        public Icon(OwnerContext owner, Resource res) {
            this.owner = owner;
            this.res = res;
        }

        public enum Markable {
            UNMARKABLE, NONDEFAULT, DEFAULT
        }

        public abstract String name();

        public abstract BufferedImage image();

        public abstract Image img();

        public abstract void draw(GOut g, Coord cc);

        public abstract boolean checkhit(Coord c);

        public Object[] id() {return (nilid);}

        public int z() {return (0);}

        public Markable markable() {return (Markable.UNMARKABLE);}

        @Resource.PublishedCode(name = "mapicon")
        public interface Factory {
            Icon create(OwnerContext owner, Resource res, Message sdt);

            Collection<? extends Icon> enumerate(OwnerContext owner, Resource res, Message sdt);
        }
    }

    public TexI tex() {
        return (img().tex());
    }

    public TexI texgrey() {
        return (img().texgrey());
    }

    public static class Image {
        private static final Map<Resource, Image> cache = new WeakHashMap<>();
        protected Resource.Image rimg;
        protected TexI tex, texgrey;
        public Coord cc;
        public boolean rot;
        public double ao;
        public int z;

        public Image() {}

        public Image(Resource res) {
            this(res.layer(Resource.imgc));
        }

        public Image(Resource.Image rimg) {
            this.rimg = rimg;
            BufferedImage buf = PUtils.copy(rimg.img);
            if ((buf.getWidth() > size) || (buf.getHeight() > size)) {
                buf = PUtils.convolve(buf, UI.scale(20, 20), filter);
            }
            buf = Utils.outline2(buf, Color.GRAY);
            TexI tex = new TexI(buf);
            this.tex = tex;
            this.cc = tex.sz().div(2);
            byte[] data = rimg.kvdata.get("mm/rot");
            if (data != null) {
                this.rot = true;
                this.ao = Utils.float32d(data, 0) * (Math.PI / 180f);
            }
            this.z = rimg.z;
            data = rimg.kvdata.get("mm/z");
            if (data != null)
                this.z = Utils.intvard(data, 0);
        }

        public TexI texgrey() {
            if (texgrey == null) {
                BufferedImage bimg = PUtils.monochromize(rimg.img, Color.WHITE);
                if ((bimg.getWidth() > size) && (bimg.getHeight() > size)) {
                    bimg = PUtils.convolvedown(bimg, UI.scale(20, 20), filter);
                }
                texgrey = new TexI(bimg);
            }
            return (texgrey);
        }

        public TexI tex(Coord sz, boolean isdead) {
            BufferedImage bimg = isdead ? PUtils.monochromize(rimg.img, Color.WHITE) : rimg.img;
            if ((bimg.getWidth() > sz.x) && (bimg.getHeight() > sz.y)) {
                bimg = PUtils.convolvedown(bimg, sz, filter);
            }
            return (new TexI(bimg));
        }

        public TexI tex() {
            return tex;
        }

        public static Image get(Resource res) {
            synchronized (cache) {
                Image img = cache.get(res);
                if (img == null) {
                    img = new Image(res);
                    cache.put(res, img);
                }
                return (img);
            }
        }
    }

    public static class ImageIcon extends Icon {
        public final Image img;
        private final Gob gob = owner.fcontext(Gob.class, false);

        public ImageIcon(OwnerContext owner, Resource res, Image img) {
            super(owner, res);
            this.img = img;
        }

        public String name() {
            Resource.Tooltip name = res.layer(Resource.tooltip);
            return (name == null ? "???" : name.t);
        }

        public BufferedImage image() {
            return (img().rimg.img);//res.flayer(Resource.imgc).img
        }

        public Image img() {
            return (img);
        }

        public void draw(GOut g, Coord cc) {
            if (!img.rot)
                g.image(img.tex, cc.sub(img.cc));
            else
                g.rotimage(img.tex, cc, img.cc, ((gob == null) ? 0 : -gob.a) + img.ao);
        }

        public boolean checkhit(Coord c) {
            Coord oc = c.add(img.cc);
            if (!oc.isect(Coord.z, PUtils.imgsz(img.rimg.img)))
                return (false);
            if (img.rimg.img.getRaster().getNumBands() < 4)
                return (true);
            return (img.rimg.img.getRaster().getSample(oc.x, oc.y, 3) >= 128);
        }

        public int z() {
            return (img.z);
        }

        private int markdata() {
            byte[] data = res.flayer(Resource.imgc).kvdata.get("mm/mark");
            if (data == null)
                return (0);
            return (Utils.intvard(data, 0));
        }

        public Markable markable() {
            switch (markdata()) {
                case 1:
                    return (Markable.NONDEFAULT);
                case 2:
                    return (Markable.DEFAULT);
                default:
                    return (Markable.UNMARKABLE);
            }
        }

        public static final Factory factory = new Factory() {
            public ImageIcon create(OwnerContext owner, Resource res, Message sdt) {
                return (new ImageIcon(owner, res, Image.get(res)));
            }

            public Collection<ImageIcon> enumerate(OwnerContext owner, Resource res, Message sdt) {
                return (Collections.singletonList(new ImageIcon(owner, res, Image.get(res))));
            }
        };
    }

    public static Icon.Factory getfac(Resource res) {
        try {
            Icon.Factory fac = res.getcode(Icon.Factory.class, false);
            if (fac != null)
                return (fac);
        } catch (Throwable e) {
            dev.simpleLog(e);
        }
        return (ImageIcon.factory);
    }

    public Image img() {
        if (this.img == null) {
            Resource res = this.res.get();
            this.img = getfac(res).create(gob, res, new MessageBuf(sdt)).img();
        }
        return (this.img);
    }

    private static Consumer<UI> resnotif(String nm) {
        return (ui -> {
            Indir<Resource> resid = Resource.local().load(nm);
            ui.sess.glob.loader.defer(() -> {
                Resource res;
                try {
                    res = resid.get();
                } catch (Loading l) {
                    throw (l);
                } catch (RuntimeException e) {
                    ui.error("Could not play " + nm);
                    return;
                }
                Audio.CS clip = Audio.fromres(res);
                ui.sfx(clip);
            }, null);
        });
    }

    private static final Map<Object, Double> lastnotifs = new HashMap<>();

    private static Consumer<UI> notiflimit(Consumer<UI> bk, Object id) {
        return (ui -> {
            double now = Utils.rtime();
            synchronized (lastnotifs) {
                Double last = lastnotifs.get(id);
                if ((last != null) && (now - last < 0.5))
                    return;
                lastnotifs.put(id, now);
            }
            bk.accept(ui);
        });
    }

    public static class Setting implements Serializable {
        public Resource.Spec res;
        public boolean show, defshow;

        public Setting(Resource.Spec res) {
            this.res = res;
        }
    }

    public static class CustomSetting extends Setting {
        public final Indir<Tex> tex;
        public final Indir<String> name;

        public CustomSetting(final Resource.Spec res, final Indir<Tex> tex) {
            super(res);
            this.tex = tex;
            this.name = () -> pretty(res.name); //res.basename().replaceAll("(^[a-z])(.*)", "$1".toUpperCase() + "$2"))
            show = true;
            defshow = true;
        }

        private static String pretty(String name) {
            int k = name.lastIndexOf("/");
            name = name.substring(k + 1);
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            return name;
        }
    }

    public static class Settings implements Serializable {
        public static final byte[] sig = "Icons".getBytes(Utils.ascii);
        public Map<String, Setting> settings = new HashMap<>();
        public int tag = -1;
        public boolean notify = false;

        public Setting get(Resource.Named res) {
            Setting ret = settings.get(res.name);
            if ((ret != null) && (ret.res.ver < res.ver))
                ret.res = new Resource.Spec(null, res.name, res.ver);
            return (ret);
        }

        public Setting get(Resource res) {
            return (get(res.indir()));
        }

        public void receive(int tag, Setting[] conf) {
            Map<String, Setting> nset = new HashMap<>(settings);
            for (int i = 0; i < conf.length; i++) {
                String nm = conf[i].res.name;
                Setting prev = nset.get(nm);
                if (prev == null)
                    nset.put(nm, conf[i]);
                else if (prev.res.ver < conf[i].res.ver)
                    prev.res = conf[i].res;
            }
            this.settings = nset;
            this.tag = tag;
        }

        public void save(Message buf) {
            buf.addbytes(sig);
            buf.adduint8(2);
            buf.addint32(tag);
            buf.adduint8(notify ? 1 : 0);
            for (Setting set : settings.values()) {
                buf.addstring(set.res.name);
                buf.adduint16(set instanceof CustomSetting ? -1 : set.res.ver);
                if (set instanceof CustomSetting) {
                    buf.adduint8((byte) 'c');
                }
                buf.adduint8((byte) 's');
                buf.adduint8(set.show ? 1 : 0);
                buf.adduint8((byte) 'd');
                buf.adduint8(set.defshow ? 1 : 0);
                buf.adduint8(0);
            }
            buf.addstring("");
        }

        public static Settings load(Message buf) {
            if (!Arrays.equals(buf.bytes(sig.length), sig))
                throw (new Message.FormatError("Invalid signature"));
            int ver = buf.uint8();
            if ((ver < 1) || (ver > 2))
                throw (new Message.FormatError("Unknown version: " + ver));
            Settings ret = new Settings();
            ret.tag = buf.int32();
            if (ver >= 2)
                ret.notify = (buf.uint8() != 0);
            while (true) {
                String resnm = buf.string();
                if (resnm.equals(""))
                    break;
                int resver = buf.uint16();
                Resource.Spec res = new Resource.Spec(null, resnm, resver);
                Setting set = new Setting(res);
                boolean setdef = false;
                boolean failed = false;
                data:
                while (true) {
                    int datum = buf.uint8();
                    switch (datum) {
                        case (int) 'c':
                            Indir<Tex> ctex = Config.additonalicons.get(resnm);
                            set = new CustomSetting(res, ctex);
                            if (ctex == null) {
                                failed = true;
                                Debug.println(String.format("Failed load icon for %s(%s)", resnm, resver));
                            }
                            break;
                        case (int) 's':
                            set.show = (buf.uint8() != 0);
                            break;
                        case (int) 'd':
                            set.defshow = (buf.uint8() != 0);
                            setdef = true;
                            break;
                        case 0:
                            break data;
                        default:
                            throw (new Message.FormatError("Unknown datum: " + datum));
                    }
                }
                if (!setdef)
                    set.defshow = set.show;
                if (!failed)
                    ret.settings.put(res.name, set);
            }
            return (ret);
        }
    }

    public static class SettingsWindow extends Window {
        public final Settings conf;
        private final Runnable save;

        public static class Icon {
            public final Setting conf;
            public Text name = null;

            public Icon(Setting conf) {
                this.conf = conf;
            }

            protected Tex img = null;

            public Tex img() {
                if (this.img == null) {
                    BufferedImage img = conf.res.loadsaved(Resource.remote()).layer(Resource.imgc).img;
                    Coord tsz;
                    if (img.getWidth() > img.getHeight())
                        tsz = new Coord(elh, (elh * img.getHeight()) / img.getWidth());
                    else
                        tsz = new Coord((elh * img.getWidth()) / img.getHeight(), elh);
                    this.img = new TexI(PUtils.convolve(img, tsz, filter));
                }
                return (this.img);
            }
        }

        public static class CustomIcon extends Icon {
            public Indir<String> iname;
            public Indir<Tex> itex;

            public CustomIcon(final CustomSetting conf) {
                super(conf);
                this.itex = conf.tex;
                this.iname = conf.name;
            }

            public Tex img() {
                if (this.img == null) {
                    if (itex != null) {
                        this.img = itex.get();
                    }
                }
                return (this.img);
            }
        }

        private static final Text.Foundry elf = Text.attrf;
        private static final int elh = elf.height() + UI.scale(2);
        private static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32), found = new Color(255, 255, 0, 32);

        public class IconList extends Searchbox<Icon> {
            private Coord showc;
            private List<Icon> all = Collections.emptyList();
            private List<Icon> ordered = Collections.emptyList();
            private Map<String, Setting> cur = null;
            private boolean reorder = false;
            private Filter filter = Filter.ALL;

            private IconList(int w, int h) {
                super(w, h, elh);
                this.showc = showc();
            }

            private Coord showc() {
                return (new Coord(sz.x - (sb.vis() ? sb.sz.x : 0) - ((elh - CheckBox.sbox.sz().y) / 2) - CheckBox.sbox.sz().x,
                        ((elh - CheckBox.sbox.sz().y) / 2)));
            }

            public void tick(double dt) {
                Map<String, Setting> cur = this.cur;
                if (cur != conf.settings) {
                    cur = conf.settings;
                    ArrayList<Icon> ordered = new ArrayList<>();
                    try {
                        for (Setting set : cur.values()) {
                            if (set instanceof CustomSetting) {
                                CustomSetting cs = (CustomSetting) set;
                                ordered.add(new CustomIcon(cs));
                            } else
                                ordered.add(new Icon(set));
                        }
                    } catch (Loading l) {
                        return;
                    }
                    this.cur = cur;
                    this.all = ordered;
                    reorder = true;
                }
                if (reorder) {
                    reorder = false;
                    fixScrollbar();
                    this.ordered = all.stream().filter(filter.predicate).collect(Collectors.toList());
                    for (Icon icon : ordered.stream().filter(filter.predicate).collect(Collectors.toList())) {
                        if (icon.name == null) {
                            try {
                                if (icon instanceof CustomIcon) {
                                    CustomIcon cicon = (CustomIcon) icon;
                                    icon.name = elf.render(cicon.iname.get());
                                } else {
                                    Resource.Tooltip name = icon.conf.res.loadsaved(Resource.remote()).layer(Resource.tooltip);
                                    icon.name = elf.render((name == null) ? "???" : name.t);
                                }
                            } catch (Throwable l) {
                                reorder = true;
                            }
                        }
                    }
                    ordered.sort((a, b) -> {
                        if ((a.name == null) && (b.name == null))
                            return (0);
                        if (a.name == null)
                            return (1);
                        if (b.name == null)
                            return (-1);
                        return (a.name.text.compareTo(b.name.text));
                    });
                }
            }

            public Icon listitem(int idx) {
                return (ordered.get(idx));
            }

            public int listitems() {
                return (ordered.size());
            }

            public boolean searchmatch(int idx, String txt) {
                Icon icon = listitem(idx);
                if (icon.name == null)
                    return (false);
                return (icon.name.text.toLowerCase().contains(txt.toLowerCase()));
            }

            public void draw(GOut g) {
                this.showc = showc();
                super.draw(g);
            }

            protected void drawbg(GOut g) {}

            public void drawitem(GOut g, Icon icon, int idx) {
                if (soughtitem(idx)) {
                    g.chcolor(found);
                    g.frect(Coord.z, g.sz());
                }
                g.chcolor(((idx % 2) == 0) ? every : other);
                g.frect(Coord.z, g.sz());
                g.chcolor();
                try {
                    g.aimage(icon.img(), new Coord(0, elh / 2), 0.0, 0.5);
                } catch (Loading l) {
                }
                if (icon.name != null)
                    g.aimage(icon.name.tex(), new Coord(elh + UI.scale(5), elh / 2), 0.0, 0.5);
                g.image(CheckBox.sbox, showc);
                if (icon.conf.show)
                    g.image(CheckBox.smark, showc);
            }

            public boolean mousedown(Coord c, int button) {
                int idx = idxat(c);
                if ((idx >= 0) && (idx < listitems())) {
                    Icon icon = listitem(idx);
                    Coord ic = c.sub(idxc(idx));
                    if (ic.isect(showc, CheckBox.sbox.sz())) {
                        icon.conf.show = !icon.conf.show;
                        if (save != null)
                            save.run();
                        return (true);
                    }
                }
                return (super.mousedown(c, button));
            }

            public boolean keydown(java.awt.event.KeyEvent ev) {
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    if (sel != null) {
                        sel.conf.show = !sel.conf.show;
                        if (save != null)
                            save.run();
                    }
                    return (true);
                }
                return (super.keydown(ev));
            }

            public void showall() {
                for (Icon icon : ordered)
                    icon.conf.show = true;
                if (save != null)
                    save.run();
            }

            public void hideall() {
                for (Icon icon : ordered)
                    icon.conf.show = false;
                if (save != null)
                    save.run();
            }

            public void reset() {
                for (Icon icon : ordered)
                    icon.conf.show = icon.conf.defshow;
                if (save != null)
                    save.run();
            }

            public boolean filter(final Filter filter) {
                Filter old = this.filter;
                boolean ret = old == filter;
                this.filter = ret ? Filter.ALL : filter;
                reorder = true;
                return (ret);
            }
        }

        enum Filter {
            ALL(i -> true),
            TREE(predicate("(gfx/terobjs/mm/trees/.*)")),
            BUSH(predicate("(gfx/terobjs/mm/bushes/.*)")),
            BOULDER(predicate("(gfx/invobjs/alabaster)|(gfx/invobjs/apatite)|(gfx/invobjs/arkose)|(gfx/invobjs/basalt)|(gfx/invobjs/blackcoal)|(gfx/invobjs/magnetite)|(gfx/invobjs/hematite)|(gfx/invobjs/breccia)|(gfx/invobjs/cassiterite)|(gfx/invobjs/chalcopyrite)|(gfx/invobjs/chert)|(gfx/invobjs/cinnabar)|(gfx/invobjs/diabase)|(gfx/invobjs/diorite)|(gfx/invobjs/dolomite)|(gfx/invobjs/eclogite)|(gfx/invobjs/feldspar)|(gfx/invobjs/flint)|(gfx/invobjs/fluorospar)|(gfx/invobjs/gabbro)|(gfx/invobjs/galena)|(gfx/invobjs/gneiss)|(gfx/invobjs/granite)|(gfx/invobjs/graywacke)|(gfx/invobjs/greenschist)|(gfx/invobjs/ilmenite)|(gfx/invobjs/hornsilver)|(gfx/invobjs/hornblende)|(gfx/invobjs/limonite)|(gfx/invobjs/limestone)|(gfx/invobjs/jasper)|(gfx/invobjs/corund)|(gfx/invobjs/kyanite)|(gfx/invobjs/leadglance)|(gfx/invobjs/nagyagite)|(gfx/invobjs/malachite)|(gfx/invobjs/marble)|(gfx/invobjs/mica)|(gfx/invobjs/microlite)|(gfx/invobjs/olivine)|(gfx/invobjs/orthoclase)|(gfx/invobjs/peacockore)|(gfx/invobjs/pegmatite)|(gfx/invobjs/porphyry)|(gfx/invobjs/pumice)|(gfx/invobjs/quartz)|(gfx/invobjs/rhyolite)|(gfx/invobjs/sandstone)|(gfx/invobjs/schist)|(gfx/invobjs/serpentine)|(gfx/invobjs/argentite)|(gfx/invobjs/slate)|(gfx/invobjs/soapstone)|(gfx/invobjs/sodalite)|(gfx/invobjs/sunstone)|(gfx/invobjs/cuprite)|(gfx/invobjs/zincspar)")),
            HERBS(predicate("(gfx/invobjs/herbs/.*)|(gfx/invobjs/small/tangledbramble)|(gfx/invobjs/small/thornythistle)|(gfx/invobjs/small/bladderwrack)|(gfx/invobjs/champignon-small)|(gfx/invobjs/clay-cave)|(gfx/invobjs/clay-gray)|(gfx/invobjs/small/yulelights)|(gfx/invobjs/small/yulestar)|(gfx/invobjs/whirlingsnowflake)")),
            KRITTER(predicate("(gfx/kritter/.*)|(gfx/invobjs/bayshrimp)|(gfx/invobjs/bogturtle)|(gfx/invobjs/brimstonebutterfly)|(gfx/invobjs/bunny)|(gfx/invobjs/cavecentipede)|(gfx/invobjs/cavemoth)|(gfx/invobjs/rooster)|(gfx/invobjs/crab)|(gfx/invobjs/earthworm)|(gfx/invobjs/dragonfly-emerald)|(gfx/invobjs/silkmoth-f)|(gfx/invobjs/firefly)|(gfx/invobjs/forestlizard)|(gfx/invobjs/forestsnail)|(gfx/invobjs/frog)|(gfx/invobjs/grasshopper)|(gfx/invobjs/grub)|(gfx/invobjs/hen)|(gfx/invobjs/irrbloss)|(gfx/invobjs/jellyfish)|(gfx/invobjs/ladybug)|(gfx/invobjs/lobster)|(gfx/invobjs/magpie)|(gfx/invobjs/mallard-m)|(gfx/invobjs/mallard-f)|(gfx/invobjs/mole)|(gfx/invobjs/monarchbutterfly)|(gfx/invobjs/moonmoth)|(gfx/invobjs/ptarmigan)|(gfx/invobjs/quail)|(gfx/invobjs/rabbit)|(gfx/invobjs/rabbit-doe)|(gfx/invobjs/rat)|(gfx/invobjs/rockdove)|(gfx/invobjs/sandflea)|(gfx/invobjs/seagull)|(gfx/invobjs/springbumblebee)|(gfx/invobjs/stagbeetle)|(gfx/invobjs/swan)|(gfx/invobjs/toad)|(gfx/invobjs/small/snapdragon)|(gfx/invobjs/waterstrider)|(gfx/invobjs/woodgrouse-m)|(gfx/invobjs/woodgrouse-f)|(gfx/invobjs/woodworm)")),
            MARK(predicate("(gfx/terobjs/mm/[a-z]+)|(gfx/hud/mmap/.*)|(gfx/invobjs/clue-.*)"));

            final Predicate<Icon> predicate;

            Filter(final Predicate<Icon> predicate) {
                this.predicate = predicate;
            }

            public boolean check(final Icon icon) {
                return (predicate.test(icon));
            }

            private static Predicate<Icon> predicate(final String name) {
                return (i -> i.conf.res.name().matches(name));
            }
        }

        public SettingsWindow(Settings conf, Runnable save) {
            super(Coord.z, "Icon settings", "Icon settings");
            this.conf = conf;
            this.save = save;
            makeHidable();
            Composer composer = new Composer(this).vmrgn(UI.scale(5)).hmrgn(UI.scale(5));
            IconList list = new IconList(UI.scale(250), 21);
            List<ImageButton> blist = new ArrayList<>();
            blist.addAll(Arrays.asList(
                    new ImageButton("gfx/hud/rosters/growth", blist, () -> list.filter(Filter.TREE)),
                    new ImageButton("gfx/hud/rosters/alive", blist, () -> list.filter(Filter.BUSH)),
                    new ImageButton("gfx/invobjs/limestone", blist, () -> list.filter(Filter.BOULDER)),
                    new ImageButton("gfx/hud/rosters/metabolism", blist, () -> list.filter(Filter.HERBS)),
                    new ImageButton("gfx/invobjs/rabbit-doe", blist, () -> list.filter(Filter.KRITTER)),
                    new ImageButton("gfx/hud/curs/flag", blist, () -> list.filter(Filter.MARK))
            ));
            composer.addar(UI.scale(230), blist.toArray(new ImageButton[0]));
            composer.add(list);
            composer.hpad(UI.scale(5));
            composer.add(new CheckBox("Notification on newly seen icons") {
                {
                    this.a = conf.notify;
                }

                @Override
                public void changed(boolean val) {
                    conf.notify = val;
                    if (save != null)
                        save.run();
                }
            });
            composer.hpad(UI.scale(10));
            composer.addar(UI.scale(230),
                    new Button("All", list::showall),
                    new Button("None", list::hideall),
                    new Button("Reset", list::reset)
            );
            pack();
        }

        private static class ImageButton extends IButton {
            private final String name;
            private final List<ImageButton> list;
            private final BooleanSupplier action;
            private boolean enable;
            private static Color dcol = new Color(87, 82, 82, 128);
            private static Color ecol = new Color(218, 101, 101, 128);

            public ImageButton(final String name, final List<ImageButton> list, final BooleanSupplier action) {
                super(img(name));
                this.name = name;
                this.list = list;
                this.action = action;
            }

            private static BufferedImage img(String name) {
                BufferedImage img = Resource.remote().loadwait(name).layer(Resource.imgc).img;
                Coord tsz;
                if (img.getWidth() > img.getHeight())
                    tsz = new Coord(elh, (elh * img.getHeight()) / img.getWidth());
                else
                    tsz = new Coord((elh * img.getWidth()) / img.getHeight(), elh);
                img = PUtils.convolve(img, tsz, filter);
                return (img);
            }

            @Override
            public void draw(BufferedImage buf) {
                Graphics g = buf.getGraphics();
                if (a)
                    g.drawImage(down, 1, 1, null);
                else if (h)
                    g.drawImage(hover, 0, 1, null);
                else
                    g.drawImage(up, 0, 0, null);
                g.dispose();
            }

            @Override
            public void draw(GOut g) {
                Color col;
                if (enable) col = ecol;
                else if (a) col = found;
                else if (h) col = other;
                else col = every;
                g.chcolor(col);
                g.frect(Coord.z, sz);
                g.chcolor();
                g.chcolor(dcol);
                g.rect(Coord.z, sz);
                g.chcolor();
                super.draw(g);
            }

            @Override
            public void click() {
                list.forEach(b -> b.enable = false);
                enable = !action.getAsBoolean();
            }

            @Override
            public boolean checkhit(final Coord c) {
                return (c.isect(Coord.z, sz));
            }
        }
    }

    public static class CustomGobIcon extends GobIcon {
        public CustomGobIcon(final Gob g, final String name, final Indir<Tex> itex) {
            super(g, Resource.remote().load(name));
            this.img = new CustomImage(itex);
        }

        public class CustomImage extends Image {
            public final Indir<Tex> itex;

            public CustomImage(final Indir<Tex> itex) {
                super();
                this.itex = itex;
                this.cc = tex().sz().div(2);
            }

            public TexI texgrey() {
                if (texgrey == null) {
                    BufferedImage bimg = PUtils.monochromize(tex().back, Color.WHITE);
                    if ((bimg.getWidth() > size) && (bimg.getHeight() > size)) {
                        bimg = PUtils.convolvedown(bimg, UI.scale(20, 20), filter);
                    }
                    texgrey = new TexI(bimg);
                }
                return (texgrey);
            }

            public TexI tex() {
                if (tex == null) {
                    BufferedImage bimg = ((TexI) itex.get()).back;
                    if ((bimg.getWidth() > size) && (bimg.getHeight() > size)) {
                        bimg = PUtils.convolvedown(bimg, UI.scale(20, 20), filter);
                    }
                    tex = new TexI(bimg);
                }
                return (tex);
            }
        }
    }

    @OCache.DeltaType(OCache.OD_ICON)
    public static class $icon implements OCache.Delta {
        @Override
        public void apply(Gob g, OCache.AttrDelta msg) {
            int resid = msg.uint16();
            Indir<Resource> res;
            if (resid == 65535) {
                g.delattr(GobIcon.class);
            } else {
                int ifl = msg.uint8();
                byte[] sdt = msg.bytes();
                g.setattr(new GobIcon(g, OCache.Delta.getres(g, resid), sdt));
            }
        }
    }
}
