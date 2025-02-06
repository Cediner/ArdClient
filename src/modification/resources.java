package modification;

import haven.HSliderListboxItem;
import haven.Indir;
import haven.Light;
import haven.Material;
import haven.RenderList;
import haven.Rendered;
import haven.Resource;
import haven.States;
import haven.Tex;
import haven.Text;
import haven.UI;
import haven.Utils;
import haven.resutil.WaterTile;
import haven.sloth.util.ObservableCollection;
import haven.sloth.util.ObservableMap;
import org.apache.commons.collections4.list.TreeList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class resources {
    public static String picturePath = "modification/picture";

    public static boolean defaultUtilsCustomLoginScreenBgBoolean = Utils.getprefb("custom-login-background-bol", false);
    public static String defaultCustomLoginScreenBg = picturePath + "/loginscr_death.png";
    public static String defaultUtilsCustomLoginScreenBg = Utils.getpref("custom-login-background", defaultCustomLoginScreenBg);

    public static Tex bgCheck() {
        Tex bg = null;
        if (defaultUtilsCustomLoginScreenBgBoolean)
            bg = configuration.imageToTex(defaultUtilsCustomLoginScreenBg, true);
        if (bg == null)
            bg = configuration.imageToTex(defaultCustomLoginScreenBg, true);
        if (bg == null)
            bg = Resource.local().loadwait("gfx/loginscr").layer(Resource.imgc).tex(UI.getScale());
        return bg;
    }

    public static Tex invsq = Resource.loadtex("gfx/hud/invsq");

    public static boolean customMarkObj = Utils.getprefb("customMarkObj", false);
    public static List<String> customMarkObjs = new ArrayList<>(Arrays.asList(
            "gfx/tiles/ridges/cavein",
            "gfx/tiles/ridges/caveout",
            "gfx/terobjs/beaverdamdoor",
            "gfx/terobjs/dng/batcave",
            "gfx/terobjs/dng/antdungeon",
            "gfx/terobjs/wonders/tarpit",
            "gfx/terobjs/dng/antdoor",
            "gfx/terobjs/beaverdoor"
    ));
    public static List<String> hatslist = new ArrayList<>(Arrays.asList("gfx/terobjs/items/hats/mooncap", "gfx/terobjs/items/hats/evileyehat"));
    public static List<String> normalhatslist = new TreeList<>(Arrays.asList(
            "gfx/terobjs/items/linkhat",
            "gfx/terobjs/items/hats/jesterscap",
            "gfx/terobjs/items/hats/blackguardsmanscap",
            "gfx/terobjs/items/hats/deputyshat",
            "gfx/terobjs/items/hats/redcorsairshat",
            "gfx/terobjs/items/hats/inquisitorshat",
            "gfx/terobjs/items/hats/yulebell",
            "gfx/terobjs/items/hats/christmusketeershat",
            "gfx/terobjs/items/hats/magicrown",
            "gfx/terobjs/items/hats/evileyehat",
            "gfx/terobjs/items/hats/highwaymanhat",
            "gfx/terobjs/items/hats/exotichat",
            "gfx/terobjs/items/hats/spiderfarmershat",
            "gfx/terobjs/items/hats/flagshipcaptain",
            "gfx/terobjs/items/hats/merrygreenhat",
            "gfx/terobjs/items/sprucecap",
            "gfx/terobjs/largechest"
    ));
    public static ObservableMap<String, Boolean> customMarks = Utils.loadCustomList(customMarkObjs, "CustomMarks");
    public static ObservableMap<String, Boolean> customHats = Utils.loadCustomList(normalhatslist, "CustomHats");
    public static String defaultbrokenhat = "gfx/terobjs/items/sprucecap";
    public static String hatreplace = Utils.getpref("hatreplace", defaultbrokenhat);

    public static boolean paintcloth = Utils.getprefb("paintcloth", false);
    public static ObservableMap<String, Boolean> painedcloth = Utils.loadCustomList(new ArrayList<>(), "PaintedClothList");
    public static String[] clothfilters = new String[]{"Rendered.eyesort", "Rendered.deflt", "Rendered.first", "Rendered.last", "Rendered.postfx", "Rendered.postpfx", "States.vertexcolor", "WaterTile.surfmat", "Light.vlights", "WaterTile.wfog"};
    public static String clothcol = "Material.Colors";
    public static JSONObject painedclothjson = configuration.loadjson("PaintedCloth.json");

    public static void paintcloth(String res, RenderList r) {
        for (String ps : resources.painedcloth.keySet()) {
            if (res.contains(ps)) {
                boolean check = resources.painedcloth.get(ps);
                if (check) {
                    JSONObject o = new JSONObject();
                    try {
                        o = resources.painedclothjson.getJSONObject(ps);
                    } catch (JSONException ignored) {
                    }
                    if (o.length() > 0) {
                        for (String n : o.keySet()) {
                            if (n.equals(resources.clothcol)) {
                                JSONArray ar = new JSONArray();
                                try {
                                    ar = o.getJSONArray(resources.clothcol);
                                } catch (JSONException ignored) {
                                }
                                if (ar.length() > 0) {
                                    boolean f = false;
                                    int ac = -1, dc = -1, sc = -1, ec = -1, shine = 0;
                                    try {
                                        f = ar.getBoolean(0);
                                    } catch (JSONException ignored) {
                                    }
                                    JSONObject colorj = new JSONObject();
                                    try {
                                        colorj = ar.getJSONObject(1);
                                    } catch (JSONException ignored) {
                                    }
                                    if (colorj.length() > 0) {
                                        try {
                                            ac = colorj.getInt("Ambient");
                                        } catch (JSONException ignored) {
                                        }
                                        try {
                                            dc = colorj.getInt("Diffuse");
                                        } catch (JSONException ignored) {
                                        }
                                        try {
                                            sc = colorj.getInt("Specular");
                                        } catch (JSONException ignored) {
                                        }
                                        try {
                                            ec = colorj.getInt("Emission");
                                        } catch (JSONException ignored) {
                                        }
                                        try {
                                            shine = colorj.getInt("Shine");
                                        } catch (JSONException ignored) {
                                        }
                                    }

                                    if (f)
                                        r.prepc(new Material.Colors(
                                                new Color(ac, true),
                                                new Color(dc, true),
                                                new Color(sc, true),
                                                new Color(ec, true),
                                                shine / 100f
                                        ));
                                }
                            } else {
                                boolean f = false;
                                try {
                                    f = o.getBoolean(n);
                                } catch (JSONException ignored) {
                                }
                                if (f)
                                    switch (n) {
                                        case "Rendered.eyesort":
                                            r.prepc(Rendered.eyesort);
                                            break;
                                        case "Rendered.deflt":
                                            r.prepc(Rendered.deflt);
                                            break;
                                        case "Rendered.first":
                                            r.prepc(Rendered.first);
                                            break;
                                        case "Rendered.last":
                                            r.prepc(Rendered.last);
                                            break;
                                        case "Rendered.postfx":
                                            r.prepc(Rendered.postfx);
                                            break;
                                        case "Rendered.postpfx":
                                            r.prepc(Rendered.postpfx);
                                            break;
                                        case "States.vertexcolor":
                                            r.prepc(States.vertexcolor);
                                            break;
                                        case "WaterTile.surfmat":
                                            r.prepc(WaterTile.surfmat);
                                            break;
                                        case "Light.vlights":
                                            r.prepc(Light.vlights);
                                            break;
                                        case "WaterTile.wfog":
                                            r.prepc(WaterTile.wfog);
                                            break;
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

    public static String getDefaultTextName(String gobname) {
        if (gobname.contains("/")) {
            int p = gobname.lastIndexOf('/');
            if (p < 0) return (gobname);
            return gobname.substring(p + 1, p + 2).toUpperCase() + gobname.substring(p + 2);
        } else return gobname;
    }

    public static class IndirResource implements Indir<Resource> {
        public Resource res;
        public String resnm;

        public IndirResource(String resnm) {
            this.resnm = resnm;
        }

        public Resource get() {
            try {
                if (res == null)
                    res = Resource.remote().load(resnm).get();
            } catch (Exception e) {
            }
            return (res);
        }

        public String toString() {
            return ("<Resource Icon " + res + ">");
        }

        private void reset() {
            res = null;
        }
    }

    public static Map<Kit, Tex> cachedTex = new HashMap<>();
    public static Map<String, IndirResource> cachedRes = new HashMap<>();

    public static Tex getCachedTex(Kit kit) {
        for (Map.Entry<Kit, Tex> entry : cachedTex.entrySet())
            if (entry.getKey().equals(kit))
                return (entry.getValue());
        return (null);
    }

    public static Tex getCachedTex(String text) {
        Kit kit = new Kit(text);
        Tex tex = getCachedTex(kit);
        if (tex == null) {
            tex = Text.render(text).tex();
            cachedTex.put(kit, tex);
        }
        return (tex);
    }

    public static Tex getCachedTex(String text, Color color, Color bgcol, Text.Foundry fnd) {
        Kit kit = new Kit(text, color, bgcol, fnd);
        Tex tex = getCachedTex(kit);
        if (tex == null) {
            tex = Text.renderstroked(text, color, bgcol, fnd).tex();
            cachedTex.put(kit, tex);
        }
        return (tex);
    }

    public static class Kit {
        public String text;
        public Color color;
        public Color bgcol;
        public Text.Foundry fnd;

        public Kit(String text, Color color, Color bgcol, Text.Foundry fnd) {
            this.text = text;
            this.color = color;
            this.bgcol = bgcol;
            this.fnd = fnd;
        }

        public Kit(String text) {
            this(text, null, null, null);
        }

        public boolean equals(Kit kit) {
            if (!text.equals(kit.text)) return (false);

            if (color != null && kit.color != null && color.equals(kit.color)) return (false);
            if (bgcol != null && kit.bgcol != null && bgcol.equals(kit.bgcol)) return (false);
            if (fnd != null && kit.fnd != null && fnd.equals(kit.fnd)) return (false);

            if ((color != null && kit.color == null) || (color == null && kit.color != null)) return (false);
            if ((bgcol != null && kit.bgcol == null) || (bgcol == null && kit.bgcol != null)) return (false);
            if ((fnd != null && kit.fnd == null) || (fnd == null && kit.fnd != null)) return (false);

            return (true);
        }
    }

    public static IndirResource getCachedRes(String text) {
        IndirResource res = cachedRes.get(text);
        if (res == null) {
            res = new resources.IndirResource(text);
            cachedRes.put(text, res);
        }
        return (res);
    }

    public static List<String> defaultQuickActions = new ArrayList<>(Arrays.asList(
            ".*cart",
            "gfx/terobjs/herbs.*",
            "gfx/kritter/.*"
    ));
    public static ObservableMap<String, Boolean> customQuickActions = Utils.loadCustomList(defaultQuickActions, "QuickPattern", true);

    public static final ObservableCollection<HSliderListboxItem> sfxmenus = new ObservableCollection<>(Collections.synchronizedList(new TreeList<>()));

    public static ObservableMap<String, Boolean> customSendMarks = Utils.loadCustomList(Collections.emptyList(), "CustomSendMarks");

    public static JSONObject areaTasksJson = configuration.loadjson("AreaPickerTasks.json");
}
