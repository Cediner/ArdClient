import haven.CharWnd;
import haven.Coord;
import haven.GItem.NumberInfo;
import haven.GSprite;
import haven.ItemInfo;
import haven.ItemInfo.Tip;
import haven.Loading;
import static haven.PUtils.convolvedown;
import haven.ResData;
import haven.Resource;
import haven.RichText;
import haven.Text;
import haven.UI;
import haven.Utils;
import haven.res.lib.tspec.Spec;
import haven.res.ui.tt.attrmod.AttrMod;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ISlots extends Tip implements NumberInfo {
    public static final Text ch = Text.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Gilding:"));
    public final Collection<SItem> s = new ArrayList<>();
    public final int left;
    public final double pmin, pmax;
    public final Resource[] attrs;

    public ISlots(Owner owner, int left, double pmin, double pmax, Resource[] attrs) {
        super(owner);
        this.left = left;
        this.pmin = pmin;
        this.pmax = pmax;
        this.attrs = attrs;
    }

    public static final String chc = "192,192,255";

    public void layout(Layout l) {
        UI ui = owner.glob().ui.get();
        l.cmp.add(ch.img, new Coord(0, l.cmp.sz.y));
        if (attrs.length > 0) {
            String chanceStr = Resource.getLocString(Resource.BUNDLE_LABEL, "Chance: $col[%s]{%d%%} to $col[%s]{%d%%}");
            BufferedImage head = RichText.render(String.format(chanceStr, chc, Math.round(100 * pmin), chc, Math.round(100 * pmax)), 0).img;
            int h = head.getHeight();
            int x = 10, y = l.cmp.sz.y;
            l.cmp.add(head, new Coord(x, y));
            x += head.getWidth() + 10;
            for (int i = 0; i < attrs.length; i++) {
                BufferedImage icon = convolvedown(attrs[i].layer(Resource.imgc).img, new Coord(h, h), CharWnd.iconfilter);
                l.cmp.add(icon, new Coord(x, y));
                x += icon.getWidth() + 2;
            }
        } else {
            String chanceStr = Resource.getLocString(Resource.BUNDLE_LABEL, "Chance: $col[%s]{%d%%}");
            BufferedImage head = RichText.render(String.format(chanceStr, chc, (int) Math.round(100 * pmin)), 0).img;
            l.cmp.add(head, new Coord(10, l.cmp.sz.y));
        }

        Map<Resource, Integer> totalAttr = new HashMap<>();

//        for (ItemInfo ii : owner.info()) {
//            if (ii instanceof AttrMod) {
//                for (AttrMod.Mod mod : ((AttrMod) ii).mods) {
//                    boolean exist = false;
//                    for (Map.Entry<Resource, Integer> entry : totalAttr.entrySet()) {
//                        if (entry.getKey().equals(mod.attr)) {
//                            exist = true;
//                            entry.setValue(entry.getValue() + mod.mod);
//                            break;
//                        }
//                    }
//                    if (!exist)
//                        totalAttr.put(mod.attr, mod.mod);
//                }
//            }
//        }

        for (SItem si : s) {
            if (ui == null || ui.modflags() == UI.MOD_SHIFT)
                si.layout(l);

            for (ItemInfo ii : si.info) {
                if (ii instanceof AttrMod) {
                    for (haven.res.ui.tt.attrmod.Entry mod : ((AttrMod) ii).tab) {
                        boolean exist = false;
                        if (mod instanceof haven.res.ui.tt.attrmod.Mod) {
                            haven.res.ui.tt.attrmod.Mod m = (haven.res.ui.tt.attrmod.Mod) mod;
                            for (Map.Entry<Resource, Integer> entry : totalAttr.entrySet()) {
                                if (entry.getKey().equals(mod.attr)) {
                                    exist = true;
                                    entry.setValue((int) (entry.getValue() + m.mod));
                                    break;
                                }
                            }
                            if (mod.attr instanceof haven.res.ui.tt.attrmod.resattr) {
                                haven.res.ui.tt.attrmod.resattr r = (haven.res.ui.tt.attrmod.resattr) mod.attr;
                                if (!exist)
                                    totalAttr.put(r.res, (int) m.mod);
                            }
                        }
                    }
                }
            }
        }
        if (ui == null || ui.modflags() != UI.MOD_SHIFT)
            if (totalAttr.size() > 0) {
                List<haven.res.ui.tt.attrmod.Mod> lmods = new ArrayList<>();
                List<Map.Entry<Resource, Integer>> sortAttr = totalAttr.entrySet().stream().sorted(this::BY_PRIORITY).collect(Collectors.toList());
                for (Map.Entry<Resource, Integer> entry : sortAttr) {
                    lmods.add(new haven.res.ui.tt.attrmod.Mod(new haven.res.ui.tt.attrmod.intattr(entry.getKey()), entry.getValue()));
                }
                l.cmp.add(AttrMod.modimg(lmods), new Coord(10, l.cmp.sz.y));
            }

        if (left > 0) {
            String gildStr = Resource.getLocString(Resource.BUNDLE_LABEL, "Gildable \u00d7%d");
            String gild2Str = Resource.getLocString(Resource.BUNDLE_LABEL, "Gildable");
            l.cmp.add(Text.slotFnd.render(left > 1 ? String.format(gildStr, left) : gild2Str).img, new Coord(10, l.cmp.sz.y));
        }
    }

    private int BY_PRIORITY(Map.Entry<Resource, Integer> o1, Map.Entry<Resource, Integer> o2) {
        Resource r1 = o1.getKey();
        Resource r2 = o2.getKey();
        return r1.name.compareTo(r2.name);
    }

    public static final Object[] defn = new Object[]{Loading.waitfor(Resource.remote().load("ui/tt/defn"))};

    public class SItem {
        public final Resource res;
        public final GSprite spr;
        public final List<ItemInfo> info;
        public final String name;

        public SItem(ResData sdt, Object[] raw) {
            this.res = sdt.res.get();
            Spec spec1 = new Spec(sdt, owner, Utils.extend(new Object[]{defn}, raw));
            this.spr = spec1.spr();
            this.name = spec1.name();
            Spec spec2 = new Spec(sdt, owner, raw);
            this.info = spec2.info();
        }

        private BufferedImage img() {
            if (spr instanceof GSprite.ImageSprite)
                return (((GSprite.ImageSprite) spr).image());
            return (res.layer(Resource.imgc).img);
        }

        public List<ItemInfo> info() {
            return info;
        }

        public Resource resource() {
            return res;
        }

        public void layout(Layout l) {
            BufferedImage icon = convolvedown(img(), new Coord(16, 16), CharWnd.iconfilter);
            BufferedImage lbl = Text.render(name).img;
            BufferedImage sub = longtip(info);
            int x = 10, y = l.cmp.sz.y;
            l.cmp.add(icon, new Coord(x, y));
            l.cmp.add(lbl, new Coord(x + 16 + 3, y + ((16 - lbl.getHeight()) / 2)));
            if (sub != null)
                l.cmp.add(sub, new Coord(x + 16, y + 16));
        }
    }

    public int order() {
        return (200);
    }

    public int itemnum() {
        return (s.size());
    }

    public static final Color avail = new Color(25, 255, 255);

    public Color numcolor() {
        return ((left > 0) ? avail : Color.WHITE);
    }
}
