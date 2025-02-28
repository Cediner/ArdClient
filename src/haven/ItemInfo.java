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

import haven.res.gfx.hud.buffs.travelwar.Weariness;
import haven.res.lib.tspec.Spec;
import haven.res.ui.tt.ArmorFactory;
import haven.res.ui.tt.WearFactory;
import haven.res.ui.tt.attrmod.AttrMod;
import haven.res.ui.tt.slot.Slotted;
import haven.res.ui.tt.wpn.Damage;
import modification.dev;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class ItemInfo {
    public static final Resource armor_hard = Resource.local().loadwait("gfx/hud/chr/custom/ahard");
    public static final Resource armor_soft = Resource.local().loadwait("gfx/hud/chr/custom/asoft");
    public static BufferedImage emptyTooltip = Text.render("...").img;
    static final Pattern count_pattern = Pattern.compile("(?:^|[\\s])([0-9]*\\.?[0-9]+\\s*%?)");
    public final Owner owner;

    public static ItemInfo make(Session sess, String resname, Object... args) {
        Resource res = Resource.remote().load(resname).get();
        InfoFactory f = res.layer(Resource.CodeEntry.class).get(InfoFactory.class);
        return f.build(new SessOwner(sess), args);
    }

    public interface Owner extends OwnerContext {
        @Deprecated
        public default Glob glob() {
            return (context(Glob.class));
        }

        public List<ItemInfo> info();
    }

    private static class SessOwner implements ItemInfo.Owner {
        private final OwnerContext.ClassResolver<SessOwner> ctxr;

        public SessOwner(Session sess) {
            ctxr = new OwnerContext.ClassResolver<SessOwner>()
                    .add(Glob.class, x -> sess.glob)
                    .add(Session.class, x -> sess);
        }

        @Override
        public List<ItemInfo> info() {
            return null;
        }

        @Override
        public <T> T context(Class<T> cl) {
            return (ctxr.context(cl, this));
        }
    }

    public interface ResOwner extends Owner {
        public Resource resource();
    }

    public interface SpriteOwner extends ResOwner {
        public GSprite sprite();
    }

    public static class Raw {
        public static final Raw nil = new Raw(new Object[0], 0);
        public final Object[] data;
        public final double time;

        public Raw(Object[] data, double time) {
            this.data = data;
            this.time = time;
        }

        public Raw(Object[] data) {
            this(data, Utils.rtime());
        }
    }

    @Resource.PublishedCode(name = "tt", instancer = FactMaker.class)
    public static interface InfoFactory {
        public default ItemInfo build(Owner owner, Raw raw, Object... args) {
            return (build(owner, args));
        }

        @Deprecated
        public default ItemInfo build(Owner owner, Object... args) {
            throw (new AbstractMethodError("info factory missing either build bmethod"));
        }
    }

    public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<InfoFactory> {
        public FactMaker() {super(InfoFactory.class);}

        {
            add(new Direct<>(InfoFactory.class));
            add(new StaticCall<>(InfoFactory.class, "mkinfo", ItemInfo.class, new Class<?>[]{Owner.class, Object[].class}, (make) -> new InfoFactory() {
                        public ItemInfo build(Owner owner, Raw raw, Object... args) {
                            return (make.apply(new Object[]{owner, args}));
                        }
                    }));
            add(new StaticCall<>(InfoFactory.class, "mkinfo", ItemInfo.class, new Class<?>[]{Owner.class, Raw.class, Object[].class}, (make) -> new InfoFactory() {
                        public ItemInfo build(Owner owner, Raw raw, Object... args) {
                            return (make.apply(new Object[]{owner, raw, args}));
                        }
                    }));
            add(new Construct<>(InfoFactory.class, ItemInfo.class, new Class<?>[]{Owner.class, Object[].class}, (cons) -> new InfoFactory() {
                        public ItemInfo build(Owner owner, Raw raw, Object... args) {
                            return (cons.apply(new Object[]{owner, args}));
                        }
                    }));
            add(new Construct<>(InfoFactory.class, ItemInfo.class, new Class<?>[]{Owner.class, Raw.class, Object[].class}, (cons) -> new InfoFactory() {
                        public ItemInfo build(Owner owner, Raw raw, Object... args) {
                            return (cons.apply(new Object[]{owner, raw, args}));
                        }
                    }));
        }
    }

    public ItemInfo(Owner owner) {
        this.owner = owner;
    }

    public static class Layout {
        public final Owner owner;
        private final List<Tip> tips = new ArrayList<>();
        private final Map<TipID, Tip> itab = new HashMap<>();
        public final CompImage cmp = new CompImage();
        public int width = 0;

        public Layout(Owner owner) {
            this.owner = owner;
        }

        public interface TipID<T extends Tip> {
            public T make(Owner owner);
        }

        @Deprecated
        public interface ID<T extends Tip> extends TipID<T> {
            public T make();
            public default T make(Owner owner) {return(make());}
        }

        @SuppressWarnings("unchecked")
        public <T extends Tip> T intern(TipID<T> id) {
            T ret = (T)itab.get(id);
            if(ret == null) {
                itab.put(id, ret = id.make(owner));
                add(ret);
            }
            return(ret);
        }

        public <T extends Tip> T intern(ID<T> id) {
            return(intern((TipID<T>)id));
        }

        public void add(Tip tip) {
            tips.add(tip);
            tip.prepare(this);
        }

        public BufferedImage render() {
            tips.sort(Comparator.comparingInt(Tip::order));
            for (Tip tip : tips)
                tip.layout(this);
            return (cmp.compose());
        }
    }

    public static abstract class Tip extends ItemInfo {
        public Tip(Owner owner) {
            super(owner);
        }

        public BufferedImage tipimg() {
            return (null);
        }

        public BufferedImage tipimg(int w) {
            return (tipimg());
        }

        public Tip shortvar() {
            return (null);
        }

        public void prepare(Layout l) {}

        public void layout(Layout l) {
            try {
                BufferedImage t = tipimg(l.width);
                if (t != null)
                    l.cmp.add(t, new Coord(0, l.cmp.sz.y));
            } catch (Exception e) {
                dev.simpleLog(e);
            }
        }

        public int order() {
            return (100);
        }
    }

    public static class AdHoc extends Tip {
        public final Text str;

        public AdHoc(Owner owner, String str) {
            super(owner);
            this.str = Text.render(str);
        }

        public AdHoc(Owner owner, String str, Color color) {
            super(owner);
            this.str = Text.render(str, color);
        }

        public BufferedImage tipimg() {
            return (str.img);
        }
    }

    public static class Name extends Tip {
        public final Text str;
        public final Text ostr;

        public Name(Owner owner, Text ostr, Text str) {
            super(owner);
            this.str = str;
            this.ostr = ostr;
        }

        public Name(Owner owner, Text str) {
            this(owner, str, str);
        }

        public Name(Owner owner, String str) {
            this(owner, Text.render(str));
        }

        public Name(Owner owner, String origin, String str) {
            this(owner, Text.render(origin), Text.render(str));
        }

        public BufferedImage tipimg() {
            return (str.img);
        }

        public int order() {
            return (0);
        }

        public Tip shortvar() {
            return (new Tip(owner) {
                public BufferedImage tipimg() {
                    return (str.img);
                }

                public int order() {
                    return (0);
                }
            });
        }

        public interface Dynamic {
            public String name();
        }

        public static class Default implements InfoFactory {
            public static String get(Owner owner) {
                if (owner instanceof SpriteOwner) {
                    GSprite spr = ((SpriteOwner) owner).sprite();
                    if (spr instanceof Dynamic)
                        return (((Dynamic) spr).name());
                }
                if (!(owner instanceof ResOwner))
                    return (null);
                Resource res = ((ResOwner) owner).resource();
                Resource.Tooltip tt = res.layer(Resource.tooltip);
                if (tt == null)
                    throw (new RuntimeException("Item resource " + res + " is missing default tooltip"));
                return (tt.t);
            }

            @Override
            public ItemInfo build(Owner owner, Raw raw, Object... args) {
                String nm = get(owner);
                return ((nm == null) ? null : new Name(owner, nm));
            }
        }
    }

    public static class Pagina extends Tip {
        public final String str;

        public Pagina(Owner owner, String str) {
            super(owner);
            this.str = str;
        }

        public BufferedImage tipimg(int w) {
            return (RichText.render(str, w).img);
        }

        public void layout(Layout l) {
            BufferedImage t = tipimg((l.width == 0) ? UI.scale(200) : l.width);
            if (t != null)
                l.cmp.add(t, new Coord(0, l.cmp.sz.y + 10));
        }

        public int order() {
            return (10000);
        }
    }

    public static class Contents extends Tip {
        public final List<ItemInfo> sub;
        private static final Text.Line ch = Text.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Contents:"));
        public double content = 0;
        public boolean isseeds;
        public boolean iscurds;

        public Contents(Owner owner, List<ItemInfo> sub) {
            super(owner);
            this.sub = sub;

            for (ItemInfo info : sub) {
                if (info instanceof ItemInfo.Name) {
                    ItemInfo.Name name = (ItemInfo.Name) info;
                    if (name.str != null) {
                        // determine whether we are dealing with seeds by testing for
                        // the absence of decimal separator (this will work irregardless of current localization)
                        int amountend = name.str.text.indexOf(' ');
                        isseeds = name.str.text.lastIndexOf('.', amountend) < 0;
                        //determines if its a tray partially full of curds, maybe.
                        iscurds = name.str.text.lastIndexOf('/', amountend) > 0;
                        if (amountend > 0) {
                            try {
                                content = Double.parseDouble(name.str.text.substring(0, amountend));
                                break;
                            } catch (NumberFormatException nfe) {
                            }
                        }
                    }
                }
            }
        }

        public BufferedImage tipimg() {
            BufferedImage stip = longtip(sub);
            BufferedImage img = TexI.mkbuf(new Coord(stip.getWidth() + 10, stip.getHeight() + 15));
            Graphics g = img.getGraphics();
            g.drawImage(ch.img, 0, 0, null);
            g.drawImage(stip, 10, 15, null);
            g.dispose();
            return (img);
        }

        public Tip shortvar() {
            return (new Tip(owner) {
                public BufferedImage tipimg() {
                    return (shorttip(sub));
                }

                public int order() {
                    return (100);
                }
            });
        }
    }

    public static BufferedImage catimgs(int margin, BufferedImage... imgs) {
        return catimgs(margin, false, imgs);
    }

    public static BufferedImage catimgs(int margin, boolean right, BufferedImage... imgs) {
        int w = 0, h = -margin;
        for (BufferedImage img : imgs) {
            if (img == null)
                continue;
            if (img.getWidth() > w)
                w = img.getWidth();
            h += img.getHeight() + margin;
        }
        BufferedImage ret = TexI.mkbuf(new Coord(w, h));
        Graphics g = ret.getGraphics();
        int y = 0;
        for (BufferedImage img : imgs) {
            if (img == null)
                continue;
            g.drawImage(img, right ? w - img.getWidth() : 0, y, null);
            y += img.getHeight() + margin;
        }
        g.dispose();
        return (ret);
    }

    public static BufferedImage catimgs_center(int margin, BufferedImage... imgs) {
        int w = 0, h = -margin;
        for (BufferedImage img : imgs) {
            if (img == null)
                continue;
            if (img.getWidth() > w)
                w = img.getWidth();
            h += img.getHeight() + margin;
        }
        BufferedImage ret = TexI.mkbuf(new Coord(w, h));
        Graphics g = ret.getGraphics();
        int y = 0;
        for (BufferedImage img : imgs) {
            if (img == null)
                continue;
            g.drawImage(img, (w - img.getWidth()) / 2, y, null);
            y += img.getHeight() + margin;
        }
        g.dispose();
        return (ret);
    }

    public static BufferedImage catimgsh(int margin, BufferedImage... imgs) {
        int w = -margin, h = 0;
        for (BufferedImage img : imgs) {
            if (img == null)
                continue;
            if (img.getHeight() > h)
                h = img.getHeight();
            w += img.getWidth() + margin;
        }
        BufferedImage ret = TexI.mkbuf(new Coord(w, h));
        Graphics g = ret.getGraphics();
        int x = 0;
        for (BufferedImage img : imgs) {
            if (img == null)
                continue;
            g.drawImage(img, x, (h - img.getHeight()) / 2, null);
            x += img.getWidth() + margin;
        }
        g.dispose();
        return (ret);
    }

    public static BufferedImage longtip(List<? extends ItemInfo> info) {
        if(info.isEmpty())
            return(null);
        Layout l = new Layout(info.get(0).owner);
        synchronized (Collections.unmodifiableList(info)) {
            for (ItemInfo ii : info) {
                if (ii instanceof Tip) {
                    Tip tip = (Tip) ii;
                    l.add(tip);
                }
            }
        }
        if (l.tips.size() < 1)
            return (emptyTooltip);
        return (l.render());
    }

    public static BufferedImage shorttip(List<? extends ItemInfo> info) {
        Layout l = new Layout(info.get(0).owner);
        synchronized (Collections.unmodifiableList(info)) {
            for (ItemInfo ii : info) {
                if (ii instanceof Tip) {
                    Tip tip = ((Tip) ii).shortvar();
                    if (tip != null)
                        l.add(tip);
                }
            }
        }
        if (l.tips.isEmpty())
            return (emptyTooltip);
        return (l.render());
    }

    public static <T> T find(Class<T> cl, List<ItemInfo> il) {
        for (ItemInfo inf : il) {
            if (cl.isInstance(inf))
                return (cl.cast(inf));
        }
        return (null);
    }

    public static <T extends ItemInfo> List<T> findall(Class<T> cl, List<ItemInfo> il) {
        List<T> ret = new LinkedList<>();
        for (ItemInfo inf : il) {
            if (cl.isInstance(inf))
                ret.add(cl.cast(inf));
        }
        return ret;
    }

    public static List<ItemInfo> findall(String cl, List<ItemInfo> infos) {
        return infos.stream()
                .filter(inf -> Reflect.is(inf, cl))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private static final Map<String, ItemInfo.InfoFactory> customFactories = new HashMap<>();

    static {
//        customFactories.put("paginae/gov/enact/backwater", new BackwaterFactory());
//        customFactories.put("paginae/gov/enact/bullmyth", new BullmythFactory());
//        customFactories.put("paginae/gov/enact/centeroflearning", new CenteroflearningFactory());
//        customFactories.put("paginae/gov/enact/fecundearth", new FecundearthFactory());
//        customFactories.put("paginae/gov/enact/foundingmythos", new FoundingmythosFactory());
//        customFactories.put("paginae/gov/enact/gamekeeping", new GamekeepingFactory());
//        customFactories.put("paginae/gov/enact/guardedmarches", new GuardedmarchesFactory());
//        customFactories.put("paginae/gov/enact/heraldicswan", new HeraldicswanFactory());
//        customFactories.put("paginae/gov/enact/localcuisine", new LocalcuisineFactory());
//        customFactories.put("paginae/gov/enact/mountaintradition", new MountaintraditionFactory());
//        customFactories.put("paginae/gov/enact/seamarriage", new SeamarriageFactory());
//        customFactories.put("paginae/gov/enact/woodlandrealm", new WoodlandrealmFactory());

        customFactories.put("ui/tt/armor", new ArmorFactory());
        customFactories.put("ui/tt/wear", new WearFactory());
        customFactories.put("ui/tt/attrmod", new AttrMod.Fac());
        customFactories.put("ui/tt/wpn/dmg", new Damage.Fac());
        customFactories.put("gfx/hud/buffs/travelwear", new ItemInfo.InfoFactory() {
            @Override
            public ItemInfo build(final Owner owner, final Object... args) {
                return (Weariness.mkinfo(owner, args));
            }
        });
    }

    public static List<ItemInfo> buildinfo(Owner owner, Raw raw) {
        List<ItemInfo> ret = new ArrayList<>();
        if (raw != null) {
            for (Object o : raw.data) {
                if (o instanceof Object[]) {
                    Object[] a = (Object[]) o;
                    Resource ttres = null;
                    InfoFactory f = null;
                    if (a[0] instanceof InfoFactory) {
                        f = (InfoFactory) a[0];
                    } else if (a[0] instanceof Integer) {
                        Resource.Resolver rr = owner.context(Resource.Resolver.class);
                        ttres = rr.getres((Integer) a[0]).get();
                    } else if (a[0] instanceof Resource) {
                        ttres = (Resource) a[0];
                    } else if (a[0] instanceof Indir) {
                        ttres = (Resource) ((Indir) a[0]).get();
                    } else  {
                        throw (new ClassCastException("Unexpected info specification " + a[0].getClass()));
                    }

                    if (ttres != null) {
                        if (f == null)
                            f = customFactories.get(ttres.name);
                        if (f == null)
                            f = ttres.getcode(InfoFactory.class, true);
                    }
                    if (f != null) {
                        ItemInfo inf = f.build(owner, raw, a);
                        if (inf != null)
                            ret.add(inf);
                    }
                    if (ttres == null || f == null)
                        System.err.printf("ItemInfo for %s %s %s %s %s failed!%n", ttres, f, owner, raw, Arrays.toString(a));
                } else if (o instanceof String) {
                    ret.add(new AdHoc(owner, (String) o));
                } else {
                    throw (new ClassCastException("Unexpected object type " + o.getClass() + " in item info array."));
                }
            }
            final List<String> skipList = new ArrayList<>(Arrays.asList("TipLabel", "haven.VMeter"));
            String s = null;
            try {
                if (owner instanceof ResOwner)
                    s = ((ResOwner) owner).resource().name;
                else if (owner instanceof MenuGrid.PagButton)
                    s = ((MenuGrid.PagButton) owner).res.name;
                else if (owner instanceof MenuGrid.Pagina)
                    s = ((MenuGrid.Pagina) owner).res().name;
                else if (skipList.contains(owner.getClass().getCanonicalName())) {
                } else
                    s = owner.toString();
            } catch (Exception e) {
                s = owner.toString();
            }
            if (s != null)
                ret.add(new AdHoc(owner, "\n" + s, Color.GRAY));
        }
        return (ret);
    }

    public static List<ItemInfo> buildinfo(Owner owner, Object[] rawinfo) {
        return (buildinfo(owner, new Raw(rawinfo)));
    }

    public static String getCount(List<ItemInfo> infos) {
        String res = null;
        for (ItemInfo info : infos) {
            if (info instanceof Contents) {
                Contents cnt = (Contents) info;
                res = getCount(cnt.sub);
            } else if (info instanceof AdHoc) {
                AdHoc ah = (AdHoc) info;
                try {
                    Matcher m = count_pattern.matcher(ah.str.text);
                    if (m.find()) {
                        res = m.group(1);
                    }
                } catch (Exception ignored) {
                }
            } else if (info instanceof Name) {
                Name name = (Name) info;
                try {
                    Matcher m = count_pattern.matcher(name.str.text);
                    if (m.find()) {
                        res = m.group(1);
                    }
                } catch (Exception ignored) {
                }
            }
            if (res != null) {
                return res.trim();
            }
        }
        return null;
    }

    public static Pair<Integer, Integer> getWear(List<ItemInfo> infos) {
        infos = findall("Wear", infos);
        for (ItemInfo info : infos) {
            if (Reflect.hasField(info, "m") && Reflect.hasField(info, "d")) {
                return new Pair<>(Reflect.getFieldValueInt(info, "d"), Reflect.getFieldValueInt(info, "m"));
            }
        }
        return null;
    }

    public static Pair<Integer, Integer> getArmor(List<ItemInfo> infos) {
        //loftar is wonderful sunshine and has same class name for wear and armor tooltips even though
        //they are different classes with different fields :)
        infos = findall("Wear", infos);
        for (ItemInfo info : infos) {
            if (Reflect.hasField(info, "hard") && Reflect.hasField(info, "soft")) {
                return new Pair<>(Reflect.getFieldValueInt(info, "hard"), Reflect.getFieldValueInt(info, "soft"));
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Map<Resource, Integer> getBonuses(List<ItemInfo> infos) {
        List<ItemInfo> slotInfos = ItemInfo.findall("ISlots", infos);
        List<Slotted> gilding = ItemInfo.findall(Slotted.class, infos);
        Map<Resource, Integer> bonuses = new HashMap<>();
        try {
            for (ItemInfo islots : slotInfos) {
                Resource[] attrs = (Resource[]) Reflect.getFieldValue(islots, "attrs");
                if (attrs != null) for (Resource attr : attrs) {
                    if (!bonuses.containsKey(attr)) bonuses.put(attr, 0);
                }
                List<Object> slots = (List<Object>) Reflect.getFieldValue(islots, "s");
                for (Object slot : slots) {
                    parseAttrMods(bonuses, (List<ItemInfo>) Reflect.getFieldValue(slot, "info"));
                }
            }
            for (Slotted info : gilding) {
                List<ItemInfo> slots = info.sub;
                parseAttrMods(bonuses, slots);
            }
            parseAttrMods(bonuses, ItemInfo.findall(AttrMod.class, infos));
        } catch (Exception ignored) {
        }
        Pair<Integer, Integer> wear = ItemInfo.getArmor(infos);
        if (wear != null) {
            bonuses.put(armor_hard, wear.a);
            bonuses.put(armor_soft, wear.b);
        }
        return bonuses;
    }

    public static Map<Resource, Integer> getInputs(List<ItemInfo> infos) {
        List<ItemInfo> inputInfos = ItemInfo.findall("Inputs", infos);
        Map<Resource, Integer> inputs = new HashMap<>();
        try {
            for (ItemInfo islots : inputInfos) {
                Object[] slots = (Object[]) Reflect.getFieldValue(islots, "inputs");
                for (Object slot : slots) {
                    Spec spec = (Spec) Reflect.getFieldValue(slot, "spec");
                    int num = Reflect.getFieldValueInt(slot, "num");
                    inputs.put(spec.getres(), num);
                }
            }
        } catch (Exception ignored) {
        }

        return inputs;
    }


    @SuppressWarnings("unchecked")
    public static void parseAttrMods(Map<Resource, Integer> bonuses, List infos) {
        for (Object inf : infos) {
            List<Object> mods = (List<Object>) Reflect.getFieldValue(inf, "mods");
            if (mods != null) {
                for (Object mod : mods) {
                    Resource attr = (Resource) Reflect.getFieldValue(mod, "attr");
                    int value = Reflect.getFieldValueInt(mod, "mod");
                    if (bonuses.containsKey(attr)) {
                        bonuses.put(attr, bonuses.get(attr) + value);
                    } else {
                        bonuses.put(attr, value);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Resource, Integer> parseAttrMods2(List infos) {
        Map<Resource, Integer> bonuses = new HashMap<>();
        for (Object inf : infos) {
            List<Object> mods = (List<Object>) Reflect.getFieldValue(inf, "mods");
            for (Object mod : mods) {
                Resource attr = (Resource) Reflect.getFieldValue(mod, "attr");
                int value = Reflect.getFieldValueInt(mod, "mod");
                if (bonuses.containsKey(attr)) {
                    bonuses.put(attr, bonuses.get(attr) + value);
                } else {
                    bonuses.put(attr, value);
                }
            }
        }
        return bonuses;
    }

    private static String dump(Object arg) {
        if (arg instanceof Object[]) {
            StringBuilder buf = new StringBuilder();
            buf.append("[");
            boolean f = true;
            for (Object a : (Object[]) arg) {
                if (!f)
                    buf.append(", ");
                buf.append(dump(a));
                f = false;
            }
            buf.append("]");
            return (buf.toString());
        } else {
            return (arg.toString());
        }
    }

    public static class AttrCache<R> implements Indir<R> {
        private final Supplier<List<ItemInfo>> from;
        private final Function<List<ItemInfo>, Supplier<R>> data;
        private List<ItemInfo> forinfo = null;
        private Supplier<R> save;

        public AttrCache(Supplier<List<ItemInfo>> from, Function<List<ItemInfo>, Supplier<R>> data) {
            this.from = from;
            this.data = data;
        }

        public R get() {
            try {
                List<ItemInfo> info = from.get();
                if (info != forinfo) {
                    save = data.apply(info);
                    forinfo = info;
                }
                return (save.get());
            } catch (Loading l) {
                return (null);
            }
        }

        public static <I, R> Function<List<ItemInfo>, Supplier<R>> map1(Class<I> icl, Function<I, Supplier<R>> data) {
            return (info -> {
                I inf = find(icl, info);
                if (inf == null)
                    return (() -> null);
                return (data.apply(inf));
            });
        }

        public static <I, R> Function<List<ItemInfo>, Supplier<R>> map1s(Class<I> icl, Function<I, R> data) {
            return (info -> {
                I inf = find(icl, info);
                if (inf == null)
                    return (() -> null);
                R ret = data.apply(inf);
                return (() -> ret);
            });
        }

        public static <R> Function<List<ItemInfo>, Supplier<R>> cache(Function<List<ItemInfo>, R> data) {
            return (info -> {
                R result = data.apply(info);
                return (() -> result);
            });
        }
    }

    public static interface InfoTip {
        public List<ItemInfo> info();
    }
}
