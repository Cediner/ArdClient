package modification;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import haven.Callback;
import haven.CheckBox;
import haven.CheckListboxItem;
import haven.ColorPreview;
import haven.Config;
import haven.Coord;
import haven.Coord2d;
import haven.Defer;
import haven.Glob;
import haven.Gob;
import haven.GobHitbox;
import haven.HSliderListboxItem;
import haven.HSliderNamed;
import haven.LimitMessage;
import haven.MainFrame;
import haven.MapFile;
import haven.Matrix4f;
import haven.Message;
import haven.MessageBuf;
import haven.OCache;
import haven.PUtils;
import haven.Pair;
import haven.RadioGroup;
import haven.Resource;
import haven.Session;
import haven.Sprite;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.TextEntry;
import haven.UI;
import haven.Utils;
import haven.Widget;
import haven.WidgetVerticalAppender;
import haven.Window;
import haven.purus.pbot.PBotUtils;
import haven.sloth.gfx.SnowFall;
import haven.sloth.gob.Hidden;
import haven.sloth.util.ObservableCollection;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class configuration {
    public static String modificationPath = "modification";
    public static String soundPath = modificationPath + "/sound";
    public static String picturePath = modificationPath + "/picture";
    public static String errorPath = "errors";
    public static String pbotErrorPath = "pboterrors";

    public static boolean customTitleBoolean = Utils.getprefb("custom-title-bol", false);

    public static String defaultCustomTitleName(String name) {
        return "♂" + name + "♂: ♂right version♂";
    }

    //public static String defaultTitle = MainFrame.TITLE();
    public static String defaultCustomTitle = "https://youtu.be/dQw4w9WgXcQ";
    public static String defaultUtilsCustomTitle = Utils.getpref("custom-title", defaultCustomTitle);

    public static String tittleCheck(Session sess) {
        String name = "", title;

        /*if (sess == null)
            name = "";
        else
            name = sess.username + " \u2013 ";*/

        if (configuration.customTitleBoolean)
            title = configuration.defaultUtilsCustomTitle;
        else
            title = MainFrame.TITLE();

        return name + title;
    }

    public static Coord savedHavenPanelSize = Utils.getprefc("havpansz", new Coord(800, 600));

    public static boolean autoclick = Utils.getprefb("autoclick", false);
    public static boolean puruspfignoreridge = Utils.getprefb("puruspfignoreridge", false);

    public static boolean statustooltip = Utils.getprefb("statustooltip", false);

    public static boolean newCropStageOverlay = Utils.getprefb("newCropStageOverlay", false);

    public static boolean newQuickSlotWdg = Utils.getprefb("newQuickSlotWdg", false);
    public static boolean newgildingwindow = Utils.getprefb("newgildingwindow", true);

    public static boolean scaletree = Utils.getprefb("scaletree", false);
    public static int scaletreeint = Utils.getprefi("scaletreeint", 25);

    public static boolean instflmopening = Utils.getprefb("instflmopening", true);
    public static boolean instflmchosen = Utils.getprefb("instflmchosen", false);
    public static boolean instflmcancel = Utils.getprefb("instflmcancel", true);

    //    public static boolean proximityspecial = Utils.getprefb("proximityspecial", false);
    public static boolean customquality = Utils.getprefb("customquality", false);
    public static String qualitypos = Utils.getpref("qualitypos", "Left-Bottom");
    public static boolean shownumeric = Utils.getprefb("shownumeric", true);
    public static String numericpos = Utils.getpref("numericpos", "Right-Top");
    public static boolean showstudytime = Utils.getprefb("showstudytime", true);
    public static String studytimepos = Utils.getpref("studytimepos", "Left-Top");
    public static boolean studytimereal = Utils.getprefb("studytimereal", false);
    public static boolean tooltipapproximatert = Utils.getprefb("tooltipapproximatert", false);

    public static boolean rightclickproximity = Utils.getprefb("rightclickproximity", false);
    public static int rightclickproximityradius = Utils.getprefi("rightclickproximityradius", 5);
    public static int attackproximityradius = Utils.getprefi("attackproximityradius", 5);

    public static Coord infopos(String pos, Coord parsz, Coord tsz) {
        switch (pos) {
            case "Right-Center":
                return new Coord(parsz.x - tsz.x, parsz.y / 2 - tsz.y / 2);
            case "Left-Center":
                return new Coord(0, parsz.y / 2 - tsz.y / 2);
            case "Top-Center":
                return new Coord(parsz.x / 2 - tsz.x / 2, 0);
            case "Bottom-Center":
                return new Coord(parsz.x / 2 - tsz.x / 2, parsz.y - tsz.y);
            case "Right-Top":
                return new Coord(parsz.x - tsz.x, 0);
            case "Right-Bottom":
                return new Coord(parsz.x - tsz.x, parsz.y - tsz.y);
            case "Center":
                return new Coord(parsz.x / 2 - tsz.x / 2, parsz.y / 2 - tsz.y / 2);
            case "Left-Bottom":
                return new Coord(0, parsz.y - tsz.y);
            case "Left-Top":
            default:
                return new Coord(0, 0);
        }
    }

    public static boolean focusrectangle = Utils.getprefb("focusrectangle", false);
    public static boolean focusrectanglesolid = Utils.getprefb("focusrectanglesolid", false);
    public static int focusrectanglecolor = Utils.getprefi("focusrectanglecolor", new Color(255, 255, 255, 128).hashCode());

    public static boolean showpolownersinfo = Utils.getprefb("showpolownersinfo", false);
    public static boolean oldmountbar = Utils.getprefb("oldmountbar", false);
    public static boolean newmountbar = Utils.getprefb("newmountbar", true);
    public static boolean showcountinfo = Utils.getprefb("showcountinfo", true);
    public static boolean showtroughstatus = Utils.getprefb("showtroughstatus", false);
    public static boolean showbeehivestatus = Utils.getprefb("showbeehivestatus", false);
    public static boolean showtreeberry = Utils.getprefb("showtreeberry", false);
    public static boolean playerbordersprite = Utils.getprefb("playerbordersprite", false);
    public static int playerbordercolor = Utils.getprefi("playerbordercolor", new Color(192, 0, 0, 128).hashCode());
    public static boolean playerboxsprite = Utils.getprefb("playerbordersprite", false);
    public static int playerboxcolor = Utils.getprefi("playerboxcolor", new Color(192, 0, 0, 128).hashCode());
    public static boolean gridboxsprite = Utils.getprefb("gridboxsprite", false);
    public static int gridboxcolor = Utils.getprefi("gridboxcolor", new Color(192, 0, 0, 128).hashCode());
    public static float radiusheight = Utils.getpreff("radiusheight", 0.5f);
    public static int minimumtreestage = Utils.getprefi("minimumtreestage", 101);
    public static int showtreeberryamb = Utils.getprefi("showtreeberryamb", Color.WHITE.hashCode());
    public static int showtreeberrydif = Utils.getprefi("showtreeberrydif", Color.BLUE.hashCode());
    public static int showtreeberryspc = Utils.getprefi("showtreeberryspc", Color.RED.hashCode());
    public static int showtreeberryemi = Utils.getprefi("showtreeberryemi", Color.BLACK.hashCode());
    public static boolean morethanquility = Utils.getprefb("morethanquility", false);
    public static int morethancolor = Utils.getprefi("morethancolor", -1);
    public static int morethancoloroutline = Utils.getprefi("morethancoloroutline", Color.RED.hashCode());
    public static boolean showpointdist = Utils.getprefb("showpointdist", false);
    public static boolean straightridges = Utils.getprefb("straightridges", false);
    public static boolean gobspeedsprite = Utils.getprefb("gobspeedsprite", false);
    public static boolean kinid = Utils.getprefb("kinid", false);
    public static boolean forcelivestock = Utils.getprefb("forcelivestock", false);
    public static boolean forcelivestockopen = Utils.getprefb("forcelivestockopen", false);
    public static boolean resizableworld = Utils.getprefb("resizableworld", false);
    public static double worldsize = Utils.getprefd("worldsize", 1f);
    public static boolean rotateworld = Utils.getprefb("rotateworld", false);
    public static double rotateworldvalx = Utils.getprefd("rotateworldvalx", 0);
    public static double rotateworldvaly = Utils.getprefd("rotateworldvaly", 0);
    public static double rotateworldvalz = Utils.getprefd("rotateworldvalz", 0);
    public static boolean transparencyworld = Utils.getprefb("transparencyworld", false);
    public static boolean shieldnotify = Utils.getprefb("shieldnotify", false);
    public static int quickradius = Utils.getprefi("quickradius", 20);
    public static boolean quickactionauto = Utils.getprefb("quickactionauto", false);

    public static boolean privatechatalerts = Utils.getprefb("privatechatalerts", true);
    public static boolean ignorepm = Utils.getprefb("ignorepm", false);
    public static boolean autoselectchat = Utils.getprefb("autoselectchat", true);

    public static List<String> liquids = new ArrayList<String>(Arrays.asList("Water", "Milk", "Aurochs Milk", "Cowsmilk", "Sheepsmilk", "Goatsmilk", "Piping Hot Tea", "Tea", "Applejuice", "Pearjuice", "Grapejuice", "Stale grapejuice", "Cider", "Perry", "Wine", "Beer", "Weißbier", "Mead", "Spring Water")) {{
        sort(String::compareTo);
    }};
    public static String autoDrinkLiquid = Utils.getpref("autoDrinkLiquid", "Water");
    public static boolean drinkorsip = Utils.getprefb("drinkorsip", false);
    public static boolean autodrinkosip = Utils.getprefb("autodrinkosip", false);
    public static int autosipthreshold = Utils.getprefi("autosipthreshold", 100);
    public static boolean autoDrinkWhatever = Utils.getprefb("autoDrinkWhatever", false);
    public static boolean siponce = Utils.getprefb("siponce", false);
    public static int sipwaiting = Utils.getprefi("sipwaiting", 2000);
    public static boolean drinkmessage = Utils.getprefb("drinkmessage", false);
    public static boolean autocleardamage = Utils.getprefb("autocleardamage", false);
    public static boolean showcombatborder = Utils.getprefb("showcombatborder", false);
    public static boolean showcurrentenemieinfo = Utils.getprefb("showcurrentenemieinfo", false);
    public static boolean showactioninfo = Utils.getprefb("showactioninfo", false);
    public static boolean showinvnumber = Utils.getprefb("showinvnumber", false);
    public static boolean moredetails = Utils.getprefb("moredetails", false);
    public static boolean showhiddenoverlay = Utils.getprefb("showhiddenoverlay", true);
    public static boolean showaccgridlines = Utils.getprefb("showaccgridlines", false);
    public static boolean showaccboundingboxes = Utils.getprefb("showaccboundingboxes", false);
    public static int animationfrequency = Utils.getprefi("animationfrequency", 0);

    public static boolean allowAnim(AtomicLong time) {
        if (animationfrequency == 0)
            return (true);
        if (animationfrequency == 5000)
            return (false);
        if (System.currentTimeMillis() - time.get() >= animationfrequency) {
            time.set(System.currentTimeMillis());
            return (true);
        }
        return (false);
    }

    public static boolean checkDisableAnimation1(long id) {
        if (animategobsid.entrySet().stream().anyMatch(e -> e.getKey() == id && e.getValue().disable())) {
            return (true);
        }
        return (false);
    }

    public static boolean checkDisableAnimation(String name) {
        if (animategobsstring.entrySet().stream().anyMatch(e -> e.getKey().equalsIgnoreCase(name) && e.getValue().disable())) {
            return (true);
        }
        return (false);
    }

    public static boolean disableAnimation(Sprite.Owner owner) {
        if (Config.disableAllAnimations) return (true);
        if (owner instanceof Gob) {
            Gob gob = (Gob) owner;
            if (gob.disableAnimation1) return (true);
            if (gob.disableAnimation) return (true);
        }
        return (false);
    }

    public static boolean scalingmarks = Utils.getprefb("scalingmarks", false);
    public static boolean bigmapshowgrid = Utils.getprefb("bigmapshowgrid", false);
    public static boolean bigmapshowviewdist = Utils.getprefb("bigmapshowviewdist", false);
    public static boolean bigmaphidemarks = Utils.getprefb("bigmapshowmarks", false);
    public static boolean allowtexturemap = Utils.getprefb("allowtexturemap", true);
    public static boolean allowoutlinemap = Utils.getprefb("allowoutlinemap", true);
    public static boolean anotheroutlinemap = Utils.getprefb("anotheroutlinemap", false);
    public static boolean allowridgesmap = Utils.getprefb("allowridgesmap", true);
    public static int mapoutlinetransparency = Utils.getprefi("mapoutlinetransparency", 255);
    public static float simplelmapintens = Utils.getpreff("simplelmapintens", 0.75f);
    public static boolean cavetileonmap = Utils.getprefb("cavetileonmap", false);
    public static boolean tempmarks = Utils.getprefb("tempmarks", false);
    public static boolean tempmarksall = Utils.getprefb("tempmarksall", false);
    public static int tempmarkstime = Utils.getprefi("tempmarkstime", 300);
    public static int tempmarksfrequency = Utils.getprefi("tempmarksfrequency", 500);
    public static boolean bouldersmine = Utils.getprefb("bouldersmine", true);

    public static float badcamdistdefault = Utils.getpreff("badcamdistdefault", 50.0f);
    public static float badcamdistminimaldefault = Utils.getpreff("badcamdistminimaldefault", 5.0f);
    public static float badcamelevdefault = Utils.getpreff("badcamelevdefault", (float) Math.PI / 4.0f);
    public static float badcamangldefault = Utils.getpreff("badcamangldefault", 0.0f);
    public static boolean badcamelevlock = Utils.getprefb("badcamelevlock", false);

    public static int pfcolor = Utils.getprefi("pfcolor", Color.MAGENTA.hashCode());
    public static int dowsecolor = Utils.getprefi("dowsecolor", Color.MAGENTA.hashCode());
    public static int questlinecolor = Utils.getprefi("questlinecolor", Color.MAGENTA.hashCode());
    public static int distanceviewcolor = Utils.getprefi("distanceviewcolor", new Color(10, 200, 200).hashCode());
    public static int outlinecolor = Utils.getprefi("outlinecolor", Color.BLACK.hashCode());
    public static int outlineh = Utils.getprefi("outlineh", 1);
    public static boolean colorizeridge = Utils.getprefb("colorizeridge", false);
    public static int colorizeridgecolor = Utils.getprefi("colorizeridgecolor", Color.RED.getRGB());

    public static boolean showgobsoldfags = Utils.getprefb("showgobsoldfags", true);
    public static boolean showgobssemifags = Utils.getprefb("showgobssemifags", true);
    public static boolean showgobssemistat = Utils.getprefb("showgobssemistat", true);
    public static boolean showgobsnewfags = Utils.getprefb("showgobsnewfags", true);
    public static boolean showgobsdynamic = Utils.getprefb("showgobsdynamic", true);
    public static boolean enablegobticks = Utils.getprefb("enablegobticks", true);
    public static boolean enablegobcticks = Utils.getprefb("enablegobcticks", true);
    public static boolean autodroponlyplayer = Utils.getprefb("autodroponlyplayer", false);
    public static boolean pointplacing = Utils.getprefb("pointplacing", false);
    public static boolean placinginfo = Utils.getprefb("placinginfo", false);
    public static boolean minimalisticmeter = Utils.getprefb("minimalisticmeter", false);
    public static boolean showdrinkinfo = Utils.getprefb("showdrinkinfo", true);
    public static int imetertransparency = Utils.getprefi("imetertransparency", 128);
    public static boolean boostspeedbox = Utils.getprefb("boostspeedbox", true);
    public static boolean showlinmove = Utils.getprefb("showlinmove", true);
    public static boolean simpledraging = Utils.getprefb("simpledraging", true);
    public static boolean showUniconedItemsIcon = Utils.getprefb("showUniconedItemsIcon", true);

    public static boolean disablepavingoutlineonmap = Utils.getprefb("disablepavingoutlineonmap", false);

    public static boolean nocursor = Utils.getprefb("nocursor", false);
    public static int crosterresid = Utils.getprefi("crosterresid", -1);

    public static boolean cachedGem = Utils.getprefb("cachedGem", false);

    public static boolean showlocationinfo = Utils.getprefb("showlocationinfo", true);
    public static boolean showweatherinfo = Utils.getprefb("showweatherinfo", true);
    public static boolean keyboardkeys = Utils.getprefb("keyboardkeys", false);

    public static String[] customMenuGrid = new String[]{Utils.getpref("customMenuGrid0", "6"), Utils.getpref("customMenuGrid1", "4")};

    public static Coord getMenuGrid() {
        return new Coord(Integer.parseInt(configuration.customMenuGrid[0]), Integer.parseInt(configuration.customMenuGrid[1]));
    }

    public static Coord getAutoSize(int w, int h) {
        Coord minSize = new Coord(800, 600);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Coord maxSize = new Coord(screenSize);
        Coord chosenSize = new Coord(w, h);

        if ((w < minSize.x && h > maxSize.y) || (w > maxSize.x && h < minSize.y) || (w < minSize.x && h < minSize.y)) {
            return minSize;
        }

        if (w < minSize.x) {
            chosenSize = new Coord(minSize.x, h * minSize.x / w);
            if (chosenSize.y < minSize.y) {
                chosenSize = new Coord(chosenSize.x * minSize.y / chosenSize.y, minSize.y);
                if (chosenSize.x > maxSize.x) {
                    chosenSize = new Coord(maxSize.x, chosenSize.y * maxSize.x / chosenSize.x);
                    if (chosenSize.y > maxSize.y)
                        chosenSize = new Coord(chosenSize.x * maxSize.y / chosenSize.y, maxSize.y);
                }
            }
        }
        if (h < minSize.y) {
            chosenSize = new Coord(w * minSize.y / h, minSize.y);
            if (chosenSize.x > maxSize.x) {
                chosenSize = new Coord(maxSize.x, chosenSize.y * maxSize.x / chosenSize.x);
                if (chosenSize.y > maxSize.y)
                    chosenSize = new Coord(chosenSize.x * maxSize.y / chosenSize.y, maxSize.y);
            }
        }
        if (w > maxSize.x) {
            chosenSize = new Coord(maxSize.x, h * maxSize.x / w);
            if (chosenSize.y > maxSize.y)
                chosenSize = new Coord(chosenSize.x * maxSize.y / chosenSize.y, maxSize.y);
        }
        if (h > maxSize.y)
            chosenSize = new Coord(w * maxSize.y / h, maxSize.y);

        return chosenSize;
    }

    public static BufferedImage scaleImage(BufferedImage before) {
        try {
            int w = before.getWidth();
            int h = before.getHeight();

            Coord chosenSize = new Coord(w, h);

            double scale1 = (double) chosenSize.x / w;
            double scale2 = (double) chosenSize.y / h;

            return scalingImage(before, chosenSize, scale1, scale2);
        } catch (Exception e) {
            e.printStackTrace();
            return before;
        }
    }

    public static BufferedImage scaleImage(BufferedImage before, Coord chosenSize) {
        try {
            int w = before.getWidth();
            int h = before.getHeight();

            double scale1 = (double) chosenSize.x / w;
            double scale2 = (double) chosenSize.y / h;

            return scalingImage(before, chosenSize, scale1, scale2);
        } catch (Exception e) {
            e.printStackTrace();
            return before;
        }
    }

    public static BufferedImage scaleImage(String name) throws Exception {
        File img = new File(name);
        BufferedImage in = ImageIO.read(img);

        return scaleImage(in);
    }

    public static BufferedImage scaleImage(String name, Coord chosenSize) throws Exception {
        File img = new File(name);
        BufferedImage in = ImageIO.read(img);

        return scaleImage(in, chosenSize);
    }

    public static BufferedImage scaleImage(BufferedImage before, boolean autoSize) {
        try {
            int w = before.getWidth();
            int h = before.getHeight();

            Coord chosenSize;
            if (autoSize) chosenSize = getAutoSize(w, h);
            else chosenSize = new Coord(w, h);

            // Create a new image of the proper size
            int w2 = chosenSize.x;
            int h2 = chosenSize.y;
            double scale1 = (double) w2 / w;
            double scale2 = (double) h2 / h;
            BufferedImage after = TexI.mkbuf(Coord.of(w2, h2));
            AffineTransform scaleInstance = AffineTransform.getScaleInstance(scale1, scale2);
            AffineTransformOp scaleOp = new AffineTransformOp(scaleInstance, AffineTransformOp.TYPE_BILINEAR);

            scaleOp.filter(before, after);
            return after;
        } catch (Exception e) {
            e.printStackTrace();
            return before;
        }
    }

    public static BufferedImage scaleImage(BufferedImage before, int scale) {
        try {
            int w = before.getWidth();
            int h = before.getHeight();
            // Create a new image of the proper size
            int w2 = (int) (w * scale);
            int h2 = (int) (h * scale);
            BufferedImage after = TexI.mkbuf(Coord.of(w2, h2));
            AffineTransform scaleInstance = AffineTransform.getScaleInstance(scale, scale);
            AffineTransformOp scaleOp = new AffineTransformOp(scaleInstance, AffineTransformOp.TYPE_BILINEAR);

            scaleOp.filter(before, after);
            return after;
        } catch (Exception e) {
            e.printStackTrace();
            return before;
        }
    }

    public static BufferedImage scaleImage(String name, BufferedImage defaultImage) {
        try {
            return scaleImage(name);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultImage;
        }
    }

    public static BufferedImage scaleImage(String name, Coord chosenSize, BufferedImage defaultImage) {
        try {
            return scaleImage(name, chosenSize);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultImage;
        }
    }

    public static BufferedImage scalingImage(BufferedImage before, Coord chosenSize, double scale1, double scale2) {
        BufferedImage after = TexI.mkbuf(chosenSize);
        AffineTransform scaleInstance = AffineTransform.getScaleInstance(scale1, scale2);
        AffineTransformOp scaleOp = new AffineTransformOp(scaleInstance, AffineTransformOp.TYPE_BILINEAR);

        scaleOp.filter(before, after);
        return after;
    }

    public static Tex getTex(String name, Coord chosenSize, boolean autoSize) throws IOException {
        BufferedImage in;
        File img = new File(name);
        in = ImageIO.read(img);

        BufferedImage newImage = TexI.mkbuf(Coord.of(in.getWidth(), in.getHeight()));

        Graphics2D g = newImage.createGraphics();
        g.drawImage(in, 0, 0, null);
        g.dispose();

        Tex tex;
        if (autoSize)
            tex = new TexI(scaleImage(newImage, true));
        else if (chosenSize != null)
            tex = new TexI(scaleImage(newImage, chosenSize));
        else
            tex = new TexI(newImage);

        return tex;
    }

    public static Tex getTex(String name) throws IOException {
        return getTex(name, null, false);
    }

    public static Tex getTex(String name, Coord chosenSize) throws IOException {
        return getTex(name, chosenSize, false);
    }

    public static Tex getTex(String name, boolean autoSize) throws IOException {
        return getTex(name, null, autoSize);
    }

    public static Tex imageToTex(String name, boolean autoSize, Coord chosenSize, Tex defaultTex) {
        try {
            if (autoSize)
                return getTex(name, autoSize);
            else if (chosenSize == null)
                return getTex(name);
            else
                return getTex(name, chosenSize);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(name);
            if (defaultTex == null)
                return null;
            else
                return defaultTex;
        }
    }

    public static Tex imageToTex(String name) {
        return imageToTex(name, false, null, null);
    }

    public static Tex imageToTex(String name, Coord chosenSize) {
        return imageToTex(name, false, chosenSize, null);
    }

    public static Tex imageToTex(String name, boolean autoSize) {
        return imageToTex(name, autoSize, null, null);
    }

    public static Tex imageToTex(String name, Tex defaultTex) {
        return imageToTex(name, false, null, defaultTex);
    }

    public static Tex imageToTex(String name, Coord chosenSize, Tex defaultTex) {
        return imageToTex(name, false, chosenSize, defaultTex);
    }

    public static Tex imageToTex(String name, boolean autoSize, Tex defaultTex) {
        return imageToTex(name, autoSize, null, defaultTex);
    }

    public static BufferedImage rotate(BufferedImage image, double angle) {
        AffineTransform transform = new AffineTransform();
        transform.rotate(angle, image.getWidth() / 2, image.getHeight() / 2);
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        return image = op.filter(image, null);
    }

    public static TexI monochrome(TexI texI, Color color) {
        BufferedImage bimg = PUtils.monochromize(texI.back, color);
        return new TexI(bimg);
    }

    public static ArrayList<String> findFiles(String dir, List<String> exts) {
        try {
            File file = new File(dir);

            ArrayList<String> list = new ArrayList<String>();
            if (!file.exists()) System.out.println(dir + " folder not exists");
            for (String ext : exts) {
                File[] listFiles = file.listFiles(new MyFileNameFilter(ext));
                if (listFiles.length == 0) {
                    //System.out.println(dir + " не содержит файлов с расширением " + ext);
                } else {
                    for (File f : listFiles) {
                        list.add(dir + File.separator + f.getName());
                        //System.out.println("File: " + dir + File.separator + f.getName());
                    }
                }
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class MyFileNameFilter implements FilenameFilter {

        private String ext;

        public MyFileNameFilter(String ext) {
            this.ext = ext.toLowerCase();
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(ext);
        }
    }

    public static String getShortName(String name) {
        if (name.contains("/")) {
            int p = name.lastIndexOf('/');
            if (p < 0) return (name);
            return (name.substring(p + 1, p + 2).toUpperCase() + name.substring(p + 2));
        } else
            return (name.substring(0, 1).toUpperCase());
    }


    public static boolean snowfalloverlay = Utils.getprefb("snowfalloverlay", false);
    public static boolean blizzardoverlay = Utils.getprefb("blizzardoverlay", false);

    public static int blizzarddensity = Utils.getprefi("blizzarddensity", 5);
    public static int currentsnow = 0;

    public synchronized static int getCurrentsnow(OCache oc) {
        int count = 0;
        for (final Gob g : oc.getallgobs()) {
            if (g.isplayer()) continue;
            if (g.findol(-4921) != null)
                count++;
        }
        return currentsnow = count;
    }

    public synchronized static void addsnow(OCache oc) {
        ArrayList<Gob> gobs = new ArrayList<>();
        oc.forEach(gobs::add);

        while (configuration.getCurrentsnow(oc) < configuration.blizzarddensity) {
            Gob g = getRandom(gobs);
            if (g.findol(-4921) != null || g.isplayer()) continue;
            g.addol(new Gob.Overlay(g, -4921, new SnowFall(g)));
        }
    }

    public synchronized static void deleteAllSnow(OCache oc) {
        for (final Gob g : oc) {
            if (g.isplayer()) continue;
            Gob.Overlay snow = g.findol(-4921);
            if (snow != null)
                g.ols.remove(snow);
        }
    }

    public synchronized static void deleteSnow(OCache oc) {
        ArrayList<Gob> gobs = new ArrayList<>();
        for (final Gob g : oc) {
            if (g.isplayer()) continue;
            Gob.Overlay snow = g.findol(-4921);
            if (snow != null)
                gobs.add(g);
        }

        while (configuration.getCurrentsnow(oc) > configuration.blizzarddensity) {
            Gob g = getRandom(gobs);
            Gob.Overlay snow = g.findol(-4921);
            if (snow != null)
                g.ols.remove(snow);
        }
    }

    public static Gob getRandom(ArrayList<Gob> array) {
        int rnd = new Random().nextInt(array.size());
        return array.get(rnd);
    }

    public static class SnowThread extends Thread {
        private final AtomicBoolean running = new AtomicBoolean(false);
        final OCache oc;

        public SnowThread(OCache oc) {
            super("Snowfall");
            this.oc = oc;
        }

        @Override
        public void run() {
            running.set(true);
            while (running.get() && configuration.blizzardoverlay) {
                configuration.addsnow(oc);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void kill() {
            running.set(false);
            interrupt();
            configuration.deleteAllSnow(oc);
            configuration.snowThread = null;
        }
    }

    public static SnowThread snowThread;

    public static boolean autoflower = Utils.getprefb("autoflower", true);
    public static List<String> exclusion = new ArrayList<>(Arrays.asList("Gild", "Meditate", "Sing"));

    public static void addPetal(String name) {
        for (String item : exclusion) {
            if (name.contains(item)) {
                name = item;
                break;
            }
        }
        if (Config.flowermenus.get(name) == null) {
            CheckListboxItem ci = new CheckListboxItem(name);
            Config.flowermenus.put(name, ci);
            Utils.setcollection("petalcol", Config.flowermenus.keySet());
        }
    }

    public static int addSFX(String name) {
        return (addSFX(name, 1.0));
    }

    public static int addSFX(String name, double val) {
        HSliderListboxItem i = resources.sfxmenus.computeIfAbsent(list -> {
            for (HSliderListboxItem item : list)
                if (item.name.equals(name))
                    return (item);
            return (null);
        }, () -> {
            HSliderListboxItem item = new HSliderListboxItem(name, (int) (val * 100));
            Defer.later(() -> {
                Utils.setprefsliderlst("customsfxvol", resources.sfxmenus.items());
                return (null);
            });
            return (item);
        });
        return (i.val);
    }

    public static HSliderNamed createSFXSlider(HSliderListboxItem item) {
        return (new HSliderNamed(item, UI.scale(180), 0, 100, () -> {
            Defer.later(() -> {
                Utils.setprefsliderlst("customsfxvol", resources.sfxmenus.items());
                return (null);
            });
        }) {
            @Override
            public Object tooltip(Coord c, Widget prev) {
                return (Text.render(item.name + " volume " + item.val));
            }
        });
    }


    public static boolean insect(Coord2d[] polygon1, Coord2d[] polygon2) {
        for (int i1 = 0; i1 < polygon1.length; i1++)
            for (int i2 = 0; i2 < polygon2.length; i2++)
                if (crossing(polygon1[i1], polygon1[i1 + 1 == polygon1.length ? 0 : i1 + 1], polygon2[i2], polygon2[i2 + 1 == polygon2.length ? 0 : i2 + 1]))
                    return (true);
        return (false);
    }

    public static boolean insect(Coord2d[] polygon1, Coord2d[] polygon2, Coord2d gobc1, Coord2d gobc2) {
        for (int i1 = 0; i1 < polygon1.length; i1++)
            for (int i2 = 0; i2 < polygon2.length; i2++)
                if (crossing(polygon1[i1].add(gobc1), polygon1[i1 + 1 == polygon1.length ? 0 : i1 + 1].add(gobc1),
                        polygon2[i2].add(gobc2), polygon2[i2 + 1 == polygon2.length ? 0 : i2 + 1].add(gobc2)))
                    return (true);
        return (false);
    }

    public static boolean insect(Coord2d[] polygon1, Coord2d[] polygon2, Gob gob1, Gob gob2) {
        Coord2d gobc1 = gob1.rc, gobc2 = gob2.rc;
        Coord2d[] p1 = new Coord2d[polygon1.length], p2 = new Coord2d[polygon2.length];
        for (int i = 0; i < polygon1.length; i++)
            p1[i] = polygon1[i].rotate((float) gob1.a).add(gobc1);
        for (int i = 0; i < polygon2.length; i++)
            p2[i] = polygon2[i].rotate((float) gob2.a).add(gobc2);
        for (int i1 = 0; i1 < polygon1.length; i1++)
            for (int i2 = 0; i2 < polygon2.length; i2++)
                if (crossing(p1[i1], p1[i1 + 1 == p1.length ? 0 : i1 + 1], p2[i2], p2[i2 + 1 == p2.length ? 0 : i2 + 1]))
                    return (true);
        return (false);
    }

    public static double vectormul(double ax, double ay, double bx, double by) {
        return (ax * by - ay * bx);
    }

    public static boolean crossing(Coord2d c1, Coord2d c2, Coord2d c3, Coord2d c4) {
//        int v1 = (int) Math.round(vectormul(c4.x - c3.x, c4.y - c3.y, c3.x - c1.x, c3.y - c1.y));
//        int v2 = (int) Math.round(vectormul(c4.x - c3.x, c4.y - c3.y, c3.x - c1.x, c3.y - c1.y));
//        int v3 = (int) Math.round(vectormul(c2.x - c1.x, c2.y - c1.y, c1.x - c3.x, c1.y - c3.y));
//        int v4 = (int) Math.round(vectormul(c2.x - c1.x, c2.y - c1.y, c1.x - c4.x, c1.y - c4.y));
        Line2D l1 = new Line2D.Double(c1.x, c1.y, c2.x, c2.y);
        Line2D l2 = new Line2D.Double(c3.x, c3.y, c4.x, c4.y);
        return l1.intersectsLine(l2);
//        return ((v1 * v2) < 0 && (v3 * v4) < 0);
    }

    public static Coord2d abs(Coord2d c, double adding) {
        return (new Coord2d(c.x + (c.x / Math.abs(c.x) * adding), c.y + (c.y / Math.abs(c.y) * adding)));
    }

    public static Coord2d[] abs(Coord2d[] c, double adding) {
        Coord2d[] c2 = new Coord2d[c.length];
        for (int i = 0; i < c.length; i++)
            c2[i] = abs(c[i], adding);
        return (c2);
    }

    public static JSONObject loadjson(String filename) {
        String result = "";
        BufferedReader br = null;
        try {
            File file = new File(filename);
            if (file.exists() && !file.isDirectory()) {
                FileReader fr = new FileReader(filename);
                br = new BufferedReader(fr);
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
                result = sb.toString();
            } else {
                FileWriter jsonWriter = null;
                try {
                    jsonWriter = new FileWriter(filename);
                    jsonWriter.write(new JSONObject().toString());
                    return (new JSONObject());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (jsonWriter != null) {
                            jsonWriter.flush();
                            jsonWriter.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (new JSONObject(result));
    }

    public static void savejson(String filename, JSONObject jsonObject) {
        FileWriter jsonWriter = null;
        try {
            jsonWriter = new FileWriter(filename);
            jsonWriter.write(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (jsonWriter != null) {
                    jsonWriter.flush();
                    jsonWriter.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[] createBytes(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(object);
            out.flush();
            return (bos.toByteArray());
        } finally {
            try {
                bos.close();
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    public static Object readBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            return (in.readObject());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    public static byte[] imageToBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    public static BufferedImage bytesToImage(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return ImageIO.read(bais);
    }

    public static byte[] JSONArrayToBytes(JSONArray jsonArray) {
        byte[] bytes = new byte[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            bytes[i] = (byte) (((int) jsonArray.get(i)) & 0xFF);
        }
        return (bytes);
    }

    public static void decodeimage(BufferedImage bi, Resource res, String type, String n) {
        if (dev.decodeCode) {
            String hash = "";
            try {
                hash = Arrays.hashCode(configuration.imageToBytes(bi)) + "";
            } catch (IOException e) {
                e.printStackTrace();
            }
            File dir = new File("decode" + File.separator + res.toString().replace("/", File.separator));
            dir.mkdirs();
            String filename = res.name.substring(res.name.lastIndexOf('/') + 1) + "_" + n + hash + ".png";
            File outputfile = new File(dir, filename);
            if (!outputfile.exists()) {
                new Thread(() -> {
                    if (bi == null) {
                        dev.resourceLog(type, outputfile.getPath(), "NULL");
                        return;
                    }
                    try {
                        ImageIO.write(bi, "png", outputfile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    dev.resourceLog(type, outputfile.getPath(), "CREATED");
                }, "decode " + type + " " + outputfile.getPath()).start();
            }
        }
    }

    public static void decodeLayers(Resource res, List<Pair<String, Integer>> data) {
        if (dev.decodeCode) {
            int hash = data.hashCode();
            Path dir = Paths.get("decode" + File.separator + res.toString().replace("/", File.separator));
            String filename = res.name.substring(res.name.lastIndexOf('/') + 1) + "_layers_" + hash + ".txt";
            Path file = dir.resolve(filename);
            if (!Files.exists(file)) {
                new Thread(() -> {
                    try {
                        StringBuilder sb = new StringBuilder();
                        data.forEach(p -> sb.append(p.a).append(" = ").append(p.b).append('\n'));

                        Files.createDirectories(dir);
                        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                        dev.resourceLog("layers", file, "CREATED");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }, "decode " + " layers " + file).start();
            }
        }
    }

    public static void saveResource(Resource res, MessageBuf in) {
        if (dev.decodeCode) {
            Message resMsg = in.clone();
            Message layersMsg = in.clone();
            int hash = resMsg.hashCode();
            Path dir = Paths.get("decode" + File.separator + res.toString().replace("/", File.separator));
            String filename = res.name.substring(res.name.lastIndexOf('/') + 1) + "_" + hash + ".res";
            Path file = dir.resolve(filename);
            if (!Files.exists(file)) {
                new Thread(() -> {
                    try {
                        Files.createDirectories(dir);
                        Files.write(file, resMsg.bytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                        byte[] sig = "Haven Resource 1".getBytes(Utils.ascii);
                        layersMsg.bytes(sig.length);
                        layersMsg.uint16();

                        while (!layersMsg.eom()) {
                            String title = layersMsg.string();
                            int len = layersMsg.int32();

                            Message buf = new LimitMessage(layersMsg, len);
                            int lhash = buf.hashCode();
                            String lfilename = res.name.substring(res.name.lastIndexOf('/') + 1) + "_" + lhash + "." + title;
                            Path lfile = dir.resolve(lfilename);

                            Files.write(lfile, buf.bytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                            dev.resourceLog("resource", lfile, "CREATED");

                            buf.skip();
                        }

                        dev.resourceLog("resource", file, "CREATED");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, "save " + " res " + file).start();
            }
        }
    }

//    public static String loadToken(String username) {
//        try {
//            String loginsjson = Utils.getpref("maptokens", null);
//            if (loginsjson == null)
//                return (null);
//            JSONArray ja = new JSONArray(loginsjson);
//            for (int i = 0; i < ja.length(); i++) {
//                JSONObject jo = ja.getJSONObject(i);
//                if (jo.getString("name").equals(username)) {
//                    return (jo.getString("token"));
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return (null);
//    }
//
//    public static void saveToken(String username, String token) {
//        try {
//            String loginsjson = Utils.getpref("maptokens", null);
//            JSONArray ja;
//            if (loginsjson == null) {
//                ja = new JSONArray();
//                JSONObject jo = new JSONObject();
//                jo.put("name", username);
//                jo.put("token", token);
//                ja.put(jo);
//            } else {
//                boolean isExist = false;
//                ja = new JSONArray(loginsjson);
//                for (int i = 0; i < ja.length(); i++) {
//                    JSONObject jo = ja.getJSONObject(i);
//                    if (jo.getString("name").equals(username)) {
//                        jo.put("token", token);
//                        isExist = true;
//                        break;
//                    }
//                }
//                if (!isExist) {
//                    JSONObject jo = new JSONObject();
//                    jo.put("name", username);
//                    jo.put("token", token);
//                    ja.put(jo);
//                }
//            }
//            Utils.setpref("maptokens", ja.toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static String endpoint = Utils.getpref("vendan-mapv4-endpoint", "");
    private static final Map<String, Boolean> mapSettings = Collections.synchronizedMap(new WeakHashMap<>());

    public static boolean loadMapSetting(String username, String type) {
        Boolean ret = mapSettings.get(username + type);
        if (ret != null)
            return (ret);
        try {
            String loginsjson = Utils.getpref("json-vendan-mapv4", null);
            if (loginsjson == null) {
                JSONArray ja = new JSONArray();
                JSONObject jo = new JSONObject();
                jo.put("name", username);
                jo.put(type, true);
                ja.put(jo);
                mapSettings.put(username + type, true);
                Utils.setpref("json-vendan-mapv4", ja.toString());
                return (true);
            }
            JSONArray ja = new JSONArray(loginsjson);
            for (int i = 0; i < ja.length(); i++) {
                JSONObject jo = ja.getJSONObject(i);
                if (jo.getString("name").equals(username)) {
                    boolean bol = true;
                    try {
                        bol = jo.get(type).toString().equals("true");
                        mapSettings.put(username + type, bol);
                    } catch (Exception ignored) {
                        configuration.saveMapSetting(username, true, type);
                    }
                    return (bol);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mapSettings.put(username + type, true);
        return (true);
    }

    public static void saveMapSetting(String username, boolean bol, String type) {
        mapSettings.put(username + type, bol);
        try {
            String loginsjson = Utils.getpref("json-vendan-mapv4", null);
            JSONArray ja;
            if (loginsjson == null) {
                ja = new JSONArray();
                JSONObject jo = new JSONObject();
                jo.put("name", username);
                jo.put(type, bol);
                ja.put(jo);
            } else {
                boolean isExist = false;
                ja = new JSONArray(loginsjson);
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    if (jo.getString("name").equals(username)) {
                        jo.put(type, bol);
                        isExist = true;
                        break;
                    }
                }
                if (!isExist) {
                    JSONObject jo = new JSONObject();
                    jo.put("name", username);
                    jo.put(type, bol);
                    ja.put(jo);
                }
            }
            Utils.setpref("json-vendan-mapv4", ja.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void classMaker(Runnable run) {
        try {
            run.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] elfpreffix = new String[]{"Ael", "Aer", "Af", "Ah", "Al", "Am", "Ama", "An", "Ang", "Ansr", "Ar", "Arм", "Arn", "Aza", "Bael", "Bes", "Cael", "Cal", "Cas", "Cla", "Cor", "Cy", "Dae", "Dho", "Dre", "Du", "Eil", "Eir", "El", "Er", "Ev", "Fera", "Fi", "Fir", "Fis", "Gael", "Gar", "Gil", "Ha", "Hu", "Ia", "Il", "Ja", "Jar", "Ka", "Kan", "Ker", "Keth", "Koeh", "Kor", "Ky", "La", "Laf", "Lam", "Lue", "Ly", "Mai", "Mal", "Mara", "My", "Na", "Nai", "Nim", "Nu", "Ny", "Py", "Raer", "Re", "Ren", "Rhy", "Ru", "Rua", "Rum", "Rid", "Sae", "Seh", "Sel", "Sha", "She", "Si", "Sim", "Sol", "Sum", "Syl", "Ta", "Tahl", "Tha", "Tho", "Ther", "Thro", "Tia", "Tra", "Ty", "Uth", "Ver", "Vil", "Von", "Ya", "Za", "Zy"};
    public static String[] elfsuffix = new String[]{"ae", "nae", "ael", "aer", "aera", "aias", "aia", "ah", "aha", "aith", "aira", "al", "ala", "la", "lae", "llae", "ali", "am", "ama", "an", "ana", "a", "ani", "uanna", "ar", "ara", "ra", "ari", "ri", "aro", "ro", "as", "ash", "sah", "ath", "avel", "brar", "abrar", "ibrar", "dar", "adar", "odar", "deth", "eath", "eth", "dre", "drim", "drimme", "udrim", "dul", "ean", "el", "ele", "ela", "emar", "en", "er", "erl", "ern", "ess", "esti", "evar", "fel", "afel", "efel", "hal", "ahal", "ihal", "har", "ihar", "uhar", "hel", "ahel", "ihel", "ian", "ianna", "ia", "ii", "ion", "iat", "ik", "il", "iel", "ila", "lie", "im", "in", "inar", "ine", "ir", "ira", "ire", "is", "iss", "ist", "ith", "lath", "lith", "lyth", "kash", "ashk", "okash", "ki", "lan", "lanna", "lean", "olan", "ola", "lam", "ilam", "ulam", "lar", "lirr", "las", "lian", "lia", "lis", "elis", "lys", "lon", "ellon", "lyn", "llinn", "lihn", "mah", "ma", "mahs", "mil", "imil", "umil", "mus", "nal", "inal", "onal", "nes", "nin", "nine", "nyn", "nis", "anis", "on", "onna", "or", "oro", "oth", "othi", "que", "quis", "rah", "rae", "raee", "rad", "rahd", "rail", "ria", "aral", "ral", "ryl", "ran", "re", "reen", "reth", "rath", "ro", "ri", "ron", "ruil", "aruil", "eruil", "sal", "isal", "sali", "san", "sar", "asar", "isar", "sel", "asel", "isel", "sha", "she", "shor", "spar", "tae", "itae", "tas", "itas", "ten", "iten", "thal", "tha", "ethal", "etha", "thar", "ethar", "ithar", "ther", "ather", "thir", "thi", "ethil", "thil", "thus", "thas", "aethus", "aethas", "ti", "eti", "til", "tril", "tria", "atri", "atril", "atria", "ual", "lua", "uath", "luth", "uth", "us", "ua", "van", "vanna", "var", "vara", "avar", "avara", "vain", "avain", "via", "avia", "vin", "avin", "wyn", "ya", "yr", "yn", "yth", "zair", "zara", "azair", "ezara"};

    public static String randomNick() {
        return (elfpreffix[new Random().nextInt(elfpreffix.length)] + elfsuffix[new Random().nextInt(elfsuffix.length)]);
    }

    public static boolean resizegob = Utils.getprefb("resizegob", false);
    public static final Map<Long, GobScale> resizablegobsid = Collections.synchronizedMap(new HashMap<>());
    public static JSONObject resizablegobjson = configuration.loadjson("GobResize.json");
    public static final Map<String, GobScale> resizablegobsstring = getGobMapResize(resizablegobjson);

    public static final Map<Long, GobAnimate> animategobsid = Collections.synchronizedMap(new HashMap<>());
    public static JSONObject animategobjson = configuration.loadjson("GobAnimate.json");
    public static final Map<String, GobAnimate> animategobsstring = getGobMapAnimate(animategobjson);

    public static final Map<Long, GobColors> colorsgobsid = Collections.synchronizedMap(new HashMap<>());
    public static JSONObject colorsgobjson = configuration.loadjson("GobColors.json");
    public static final Map<String, GobColors> colorsgobsstring = getGobMapColors(colorsgobjson);

    public static Map<String, GobScale> getGobMapResize(JSONObject jo) {
        Map<String, GobScale> map = Collections.synchronizedMap(new HashMap<>());
        try {
            for (String name : jo.keySet()) {
                map.put(name, new GobScale(jo.getJSONObject(name)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (map);
    }

    public static Map<String, GobAnimate> getGobMapAnimate(JSONObject jo) {
        Map<String, GobAnimate> map = Collections.synchronizedMap(new HashMap<>());
        try {
            for (String name : jo.keySet()) {
                map.put(name, new GobAnimate(jo.getJSONObject(name)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (map);
    }

    public static Map<String, GobColors> getGobMapColors(JSONObject jo) {
        Map<String, GobColors> map = Collections.synchronizedMap(new HashMap<>());
        try {
            for (String name : jo.keySet()) {
                map.put(name, new GobColors(jo.getJSONObject(name)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (map);
    }

    public static GobScale getItem(String name) {
        for (String n : resizablegobsstring.keySet())
            if (name.equalsIgnoreCase(n))
                return (resizablegobsstring.get(n));
        return (resizablegobsstring.get(name));
    }

    public static class GobScale {
        private final float[][] scale = new float[][]{{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}};
        private boolean enable = false;

        public GobScale() {}

        public GobScale(JSONObject jo) {
            try {
                enable = jo.getBoolean("enable");
                for (int i = 0; i < scale.length; i++) {
                    for (int j = 0; j < scale[i].length; j++) {
                        scale[i][j] = (float) jo.getDouble(Integer.toString(i * scale.length + j));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public boolean setScale1(int row, int column, float val, Gob gob) {
            if (row > 3 || row < 0 || column > 3 || column < 0)
                return (false);
            scale[row][column] = val;
            reloadGob(gob);
            save();
            return (true);
        }

        public boolean setScale(int row, int column, float val, Gob gob, String resname) {
            if (row > 3 || row < 0 || column > 3 || column < 0)
                return (false);
            scale[row][column] = val;
            reloadGobs(gob, resname);
            save();
            return (true);
        }

        public void onoff1(boolean a, Gob gob) {
            this.enable = a;
            reloadGob(gob);
            save();
        }

        public void onoff(boolean a, Gob gob, String resname) {
            this.enable = a;
            reloadGobs(gob, resname);
            save();
        }

        public boolean enable() {
            return (enable);
        }

        private void reloadGob(Gob gob) {
            gob.scaleMatrix1 = enable() ? getMatrix() : null;
        }

        private void reloadGobs(Gob gob, String resname) {
            OCache oc = gob.context(Glob.class).oc;
            synchronized (oc) {
                for (Gob g : oc) {
                    g.resname().ifPresent(nm -> {
                        if (nm.equalsIgnoreCase(resname)) {
                            g.scaleMatrix = enable() ? getMatrix() : null;
                        }
                    });
                }
            }
        }

        public float[][] getScale() {
            return (scale);
        }

        public Matrix4f getMatrix() {
            return (new Matrix4f(
                    scale[0][0], scale[0][1], scale[0][2], scale[0][3],
                    scale[1][0], scale[1][1], scale[1][2], scale[1][3],
                    scale[2][0], scale[2][1], scale[2][2], scale[2][3],
                    scale[3][0], scale[3][1], scale[3][2], scale[3][3])
            );
        }

        public void createjson() {
            JSONObject nall = new JSONObject();
            for (Map.Entry<String, GobScale> entry : resizablegobsstring.entrySet()) {
                JSONObject o = new JSONObject();
                o.put("enable", entry.getValue().enable());
                for (int i = 0; i < entry.getValue().getScale().length; i++) {
                    for (int j = 0; j < entry.getValue().getScale()[i].length; j++) {
                        o.put(Integer.toString(i * entry.getValue().getScale().length + j), entry.getValue().getScale()[i][j]);
                    }
                }
                nall.put(entry.getKey(), o);
            }
            resizablegobjson = nall;
        }

        public void save() {
            createjson();
            try (FileWriter jsonWriter = new FileWriter("GobResize.json")) {
                jsonWriter.write(resizablegobjson.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class GobAnimate {
        private boolean disable = false;

        public GobAnimate() {}

        public GobAnimate(JSONObject jo) {
            try {
                disable = jo.getBoolean("disable");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onoff1(boolean a, Gob gob) {
            this.disable = a;
            reloadGob(gob);
            save();
        }

        public void onoff(boolean a, Gob gob, String resname) {
            this.disable = a;
            reloadGobs(gob, resname);
            save();
        }

        private void reloadGob(Gob gob) {
            gob.disableAnimation1 = this.disable();
        }

        private void reloadGobs(Gob gob, String resname) {
            OCache oc = gob.context(Glob.class).oc;
            synchronized (oc) {
                for (Gob g : oc) {
                    g.resname().ifPresent(nm -> {
                        if (nm.equalsIgnoreCase(resname)) {
                            g.disableAnimation = this.disable();
                        }
                    });
                }
            }
        }

        public boolean disable() {
            return (disable);
        }

        public void createjson() {
            JSONObject nall = new JSONObject();
            for (Map.Entry<String, GobAnimate> entry : animategobsstring.entrySet()) {
                JSONObject o = new JSONObject();
                o.put("disable", entry.getValue().disable());
                nall.put(entry.getKey(), o);
            }
            animategobjson = nall;
        }

        public void save() {
            createjson();
            try (FileWriter jsonWriter = new FileWriter("GobAnimate.json")) {
                jsonWriter.write(animategobjson.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class GobColors {
        private boolean enable = false;
        private Color color = Color.WHITE;

        public GobColors() {}

        public GobColors(JSONObject jo) {
            try {
                enable = jo.getBoolean("enable");
                color = new Color(jo.getInt("color"), true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onoff1(boolean a, Gob gob) {
            this.enable = a;
            reloadGob(gob);
            save();
        }

        public void onoff(boolean a, Gob gob) {
            this.enable = a;
            reloadGobs(gob);
            save();
        }


        public boolean enable() {
            return (enable);
        }

        public final Consumer<Color> setColor(Gob gob) {
            return (c -> {
                color = c;
                reloadGobs(gob);
                save();
            });
        }

        private void reloadGob(Gob gob) {
            Hidden h = gob.getattr(Hidden.class);
            if (h != null) {
                gob.delattr(Hidden.class);
                gob.setattr(new Hidden(gob));
            }

            Gob.Overlay bh = gob.findol(GobHitbox.olid);
            if (bh != null) {
                gob.remol(bh);
            }

            Gob.Overlay bh2 = gob.findol(GobHitbox.olid_solid);
            if (bh2 != null) {
                gob.remol(bh2);
            }
        }

        private void reloadGobs(Gob gob) {
            OCache oc = gob.context(Glob.class).oc;
            synchronized (oc) {
//                oc.changeAllGobs();
                for (Gob g : oc) {
                    reloadGob(g);
                }
            }
        }

        public void createjson() {
            JSONObject nall = new JSONObject();
            for (Map.Entry<String, GobColors> entry : colorsgobsstring.entrySet()) {
                JSONObject o = new JSONObject();
                o.put("enable", entry.getValue().enable());
                o.put("color", entry.getValue().color.getRGB());
                nall.put(entry.getKey(), o);
            }
            colorsgobjson = nall;
        }

        public void save() {
            createjson();
            try (FileWriter jsonWriter = new FileWriter("GobColors.json")) {
                jsonWriter.write(colorsgobjson.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Color canCustomColor(Gob gob) {
        Optional<GobColors> entry = colorsgobsid.entrySet().stream().filter(e -> e.getKey() == gob.id && e.getValue().enable()).map(Map.Entry::getValue).findFirst();
        if (entry.isPresent())
            return (entry.get().color);
        try {
            Resource gres = gob.res().orElse(null);
            if (gres != null) {
                entry = colorsgobsstring.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(gres.name) && e.getValue().enable()).map(Map.Entry::getValue).findFirst();
                if (entry.isPresent())
                    return (entry.get().color);
            }
        } catch (Exception e) {
        }
        return (null);
    }

    public static Window gobScaleWindow(Gob gob) {
        String resname = gob.getres().name;

        Window window = new Window(Coord.z, "Gob Changer");
        Widget resizeWdg = new Widget();
        {
            WidgetVerticalAppender wva1 = new WidgetVerticalAppender(resizeWdg);
            WidgetVerticalAppender wva2 = new WidgetVerticalAppender(resizeWdg);
            wva1.setY(UI.scale(20));
            wva2.setY(UI.scale(20));
            wva2.setX(UI.scale(220));

            final GobScale single = resizablegobsid.computeIfAbsent(gob.id, k1 -> new GobScale());
            float[][] sscale = single.getScale();

            wva1.add(new CheckBox("Only current: " + gob.id, val -> single.onoff1(val, gob), single.enable()));

            TextEntry[][] singlescale = new TextEntry[sscale.length][sscale[0].length];
            for (int i = 0; i < singlescale.length; i++) {
                final List<TextEntry> list = new ArrayList<>();
                for (int j = 0; j < singlescale[i].length; j++) {
                    int row = i;
                    int column = j;
                    singlescale[i][j] = new TextEntry(UI.scale(50), Float.toString(sscale[row][column])) {
                        String backup = text();

                        @Override
                        public boolean keydown(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                                try {
                                    float val = text().isEmpty() ? 0 : Float.parseFloat(text().replace(',', '.'));
                                    single.setScale1(row, column, val, gob);
                                    return (true);
                                } catch (NumberFormatException ex) {
                                }
                            }
                            backup = text();
                            boolean b = super.keydown(e);
                            try {
                                if (!text().isEmpty())
                                    Float.parseFloat(text().replace(',', '.'));
                            } catch (Exception ex) {
                                settext(backup);
                            }
                            return (b);
                        }
                    };
                    list.add(singlescale[i][j]);
                }
                wva1.addRow(list);
            }

            final GobScale multi = resizablegobsstring.computeIfAbsent(resname, k -> new GobScale());
            float[][] mscale = multi.getScale();

            wva2.add(new CheckBox("Everyone: " + resname, val -> multi.onoff(val, gob, resname), multi.enable()));

            TextEntry[][] multiscale = new TextEntry[mscale.length][mscale[0].length];
            for (int i = 0; i < multiscale.length; i++) {
                final List<TextEntry> list = new ArrayList<>();
                for (int j = 0; j < multiscale[i].length; j++) {
                    int row = i;
                    int column = j;
                    multiscale[i][j] = new TextEntry(UI.scale(50), Float.toString(mscale[row][column])) {
                        String backup = text();

                        @Override
                        public boolean keydown(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                                try {
                                    float val = text().isEmpty() ? 0 : Float.parseFloat(text().replace(',', '.'));
                                    multi.setScale(row, column, val, gob, resname);
                                    return (true);
                                } catch (NumberFormatException ex) {
                                }
                            }
                            backup = text();
                            boolean b = super.keydown(e);
                            try {
                                if (!text().isEmpty())
                                    Float.parseFloat(text().replace(',', '.'));
                            } catch (Exception ex) {
                                settext(backup);
                            }
                            return (b);
                        }
                    };
                    list.add(multiscale[i][j]);
                }
                wva2.addRow(list);
            }

            resizeWdg.pack();
            resizeWdg.adda(new CheckBox("Gob resizer", val -> Utils.setprefb("resizegob", resizegob = val), resizegob), new Coord(resizeWdg.sz.x / 2, 0), 0.5, 0);
            resizeWdg.hide();
        }

        Widget animationWdg = new Widget();
        {
            WidgetVerticalAppender wva = new WidgetVerticalAppender(animationWdg);
            final GobAnimate single = animategobsid.computeIfAbsent(gob.id, k1 -> new GobAnimate());
            wva.add(new CheckBox("Only current: " + gob.id, val -> single.onoff1(val, gob), single.disable()));
            final GobAnimate multi = animategobsstring.computeIfAbsent(resname, k -> new GobAnimate());
            wva.add(new CheckBox("Everyone: " + resname, val -> multi.onoff(val, gob, resname), multi.disable()));

            animationWdg.pack();
            animationWdg.hide();
        }

        Widget colorsWdg = new Widget();
        {
            WidgetVerticalAppender wva = new WidgetVerticalAppender(colorsWdg);
            final GobColors single = colorsgobsid.computeIfAbsent(gob.id, k1 -> new GobColors());
            wva.addRow(new CheckBox("Only current: " + gob.id, val -> single.onoff1(val, gob), single.enable()), new ColorPreview(UI.scale(16, 16), single.color, single.setColor(gob)));
            final GobColors multi = colorsgobsstring.computeIfAbsent(resname, k -> new GobColors());
            wva.addRow(new CheckBox("Everyone: " + resname, val -> multi.onoff(val, gob), multi.enable()), new ColorPreview(UI.scale(16, 16), multi.color, multi.setColor(gob)));

            colorsWdg.pack();
            colorsWdg.hide();
        }

        Widget changerWdg = new Widget();
        RadioGroup group = new RadioGroup(changerWdg) {
            @Override
            public void changed(final int btn, final String lbl) {
                super.changed(btn, lbl);
                if (lbl.equals("Resizer")) {
                    resizeWdg.show();
                    animationWdg.hide();
                    colorsWdg.hide();
                } else if (lbl.equals("Animation")) {
                    resizeWdg.hide();
                    animationWdg.show();
                    colorsWdg.hide();
                } else if (lbl.equals("Hitboxes")) {
                    animationWdg.hide();
                    resizeWdg.hide();
                    colorsWdg.show();
                }
                window.pack();
            }
        };
        RadioGroup.RadioButton p = group.add("Resizer", new Coord());
        p = group.add("Animation", p.pos("bl"));
        p = group.add("Hitboxes", p.pos("bl"));
        changerWdg.pack();

        window.add(changerWdg);
        window.pack();

        window.add(resizeWdg, window.pos("ur"));
        window.add(animationWdg, window.pos("ur"));
        window.add(colorsWdg, window.pos("ur"));
        window.pack();

        group.check("Resizer");
        return (window);
    }

    public static long namehash(long h, String name) {
        for (int i = 0; i < name.length(); i++)
            h = (h * 31) + name.charAt(i);
        return (h);
    }

    public static Executor executor = Executors.newSingleThreadExecutor();

    public static void waitfor(Callable<Boolean> call, Callback<Boolean> back, int timeout) {
        executor.execute(() -> back.done(waitfor(call, timeout)));
    }

    public static boolean waitfor(Callable<Boolean> call, int timeout) {
        for (int i = 0, sleep = 100; ; i += sleep) {
            if (i >= timeout)
                break;
            try {
                if (call.call())
                    return (true);
            } catch (Exception ignore) {
            }
            PBotUtils.sleep(sleep);
        }
        return (false);
    }

    public static final ObservableCollection<String> tilesCollection = new ObservableCollection<>(Utils.loadcollection("tilescollection"));

    public static void addTile(final String tileName) {
        synchronized (tilesCollection) {
            final AtomicBoolean dirty = new AtomicBoolean();
            if (!tilesCollection.contains(tileName)) {
                tilesCollection.add(tileName);
            }
            if (dirty.get()) {
                Utils.setcollection("tilescollection", tilesCollection.items());
            }
        }
    }

    public static void addTiles(final MapFile.TileInfo[] tileSets) {
        synchronized (tilesCollection) {
            final AtomicBoolean dirty = new AtomicBoolean();
            Arrays.stream(tileSets).filter(Objects::nonNull).forEach(t -> {
                String tileName = t.res.name();
                if (!tilesCollection.contains(tileName)) {
                    tilesCollection.add(tileName);
                    dirty.set(true);
                }
            });
            if (dirty.get()) {
                Utils.setcollection("tilescollection", tilesCollection.items());
            }
        }
    }

    public static int highlightTilePeriod = Utils.getprefi("highlightTilePeriod", 2000);

    public static <KEY, VALUE> Map<KEY, VALUE> copyMap(final Map<KEY, VALUE> original) {
        synchronized (original) {
            return (new HashMap<>(original));
        }
    }

    public static boolean autorunscriptsenable = Utils.getprefb("autorunscriptsenable", false);
    public static final ObservableCollection<String> autorunscripts = new ObservableCollection<>(Utils.loadcollection("autorunscripts"));

    public static boolean savingFogOfWar = Utils.getprefb("fogofwar", true);
    public static int fogOfWarColor = Utils.getprefi("fogofwarcolor", new Color(255, 255, 0, 100).getRGB());
    public static int fogOfWarColorTemp = Utils.getprefi("fogofwarcolorTemp", new Color(0, 255, 0, 100).getRGB());
    public static int mapgridcolor = Utils.getprefi("mapgridcolor", new Color(255, 100, 100, 150).getRGB());

    public static boolean showcolortiles = Utils.getprefb("showcolortiles", false);
    public static final JsonElement customTiles = Utils.loadCustomElement("CustomColorTiles");

    public static Integer getTileColor(String tileResName) {
        synchronized (customTiles) {
            JsonObject obj = customTiles.getAsJsonObject();
            JsonElement tileEl = obj.get(tileResName);
            if (tileEl != null) {
                JsonObject tileObj = tileEl.getAsJsonObject();
                boolean a = tileObj.get("a").getAsBoolean();
                int c = tileObj.get("c").getAsInt();
                if (a) {
                    return (c);
                }
            }
            return (null);
        }
    }

    //new JsonParser().parse("{}")

    public static boolean openStacksOnAlt = Utils.getprefb("altstacks", false);

    public static final Map<Long, String> treesMap = Collections.synchronizedMap(new HashMap<>());

    public static List<String> customEnabledMarks = Utils.loadcollection("CustomEnabledMarks");
}
