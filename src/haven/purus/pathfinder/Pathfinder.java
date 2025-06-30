package haven.purus.pathfinder;

import haven.Coord;
import haven.Coord2d;
import haven.GameUI;
import haven.Gob;
import haven.Resource;
import haven.Tiler;
import haven.resutil.TerrainTile;
import haven.sloth.script.pathfinding.Hitbox;
import modification.configuration;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import static haven.MCache.tilesz2;
import static haven.OCache.posres;

public class Pathfinder extends Thread {

    private int button, mod, meshid;
    private GameUI gui;
    private Coord2d dest;
    private Gob destGob;
    private String action;
    private boolean stop;
    public static boolean DEBUG = false;

    // Move to center of the location tile
    public Pathfinder(GameUI gui, Coord2d dest, String action) {
        this.gui = gui;
        this.dest = dest;
        this.action = action;
    }

    // Move close to the gob and right click it
    public Pathfinder(GameUI gui, Gob destGob, int button, int mod, int meshid, String action) {
        this.gui = gui;
        this.dest = destGob.rc;
        this.destGob = destGob;
        this.button = button;
        this.mod = mod;
        this.meshid = meshid;
        this.action = action;
    }

    public static Line segmentToLine(Coord2d a, Coord2d b) {
        if (a.y == b.y) { // Horizontal
            if (a.x == b.x)
                throw new Error("Segment must have different start and end points!");
            else
                return new Line(a.y, 0);
        } else if (a.x == b.x) { // Vertical
            return new Line(a.x, Double.POSITIVE_INFINITY);
        } else {
            double deltaX = a.x - b.x;
            double deltaY = a.y - b.y;
            double slope = deltaY / deltaX;
            double constant = a.y - slope * a.x;
            return new Line(constant, deltaY / deltaX);
        }
    }

    Set<String> inaccessibleTiles = new HashSet<String>() {{
        add("gfx/tiles/nil");
        add("gfx/tiles/cave");
        add("gfx/tiles/rocks/.*");
    }};

    Set<String> waterTiles = new HashSet<String>() {{
        add("gfx/tiles/deep");
        add("gfx/tiles/odeep");
    }};

    Set<String> water2Tiles = new HashSet<String>() {{
        add("gfx/tiles/odeeper");
    }};

    Set<String> bogTiles = new HashSet<String>() {{
        add("gfx/tiles/bog");
        add("gfx/tiles/bogwater");
        add("gfx/tiles/fen");
        add("gfx/tiles/fenwater");
        add("gfx/tiles/swamp");
        add("gfx/tiles/swampwater");
    }};

    Set<String> whitelistedGobs = new HashSet<>();

    Set<String> boatGobs = new HashSet<String>() {{
        add("gfx/terobjs/vehicle/dugout");
        add("gfx/terobjs/vehicle/coracle");
    }};

    Set<String> boat1Gobs = new HashSet<String>() {{
        add("gfx/terobjs/vehicle/rowboat");
    }};

    Set<String> boat2Gobs = new HashSet<String>() {{
        add("gfx/terobjs/vehicle/snekkja");
        add("gfx/terobjs/vehicle/knarr");
    }};

    private Coord coordToTile(Coord2d c) {
        return c.floor(tilesz);
    }

    // True if tile is within the given matrix and its not inaccessible
    private boolean isAccessible(Coord c, int[][] accessibilityMatrix) {
        if (c.x > 0 && c.x < accessibilityMatrix.length && c.y > 0 && c.y < accessibilityMatrix[0].length && accessibilityMatrix[c.x][c.y] <= 0)
            return true;
        else
            return false;
    }

    // Click tile at its center point
    private void clickTile(Coord tile, Coord2d origin) {
        gui.map.clearmovequeue();
        gui.map.queuemove(origin.add(tile.x * 11, tile.y * 11).add(11 / 2.0, 11 / 2.0));
//        gui.map.wdgmsg("click", PBotUtils.getCenterScreenCoord(gui.ui), origin.add(tile.x * 11, tile.y * 11).add(11 / 2.0, 11 / 2.0).floor(posres), 1, 0);
//        gui.map.pllastcc = origin.add(tile.x * 11, tile.y * 11).add(11 / 2.0, 11 / 2.0);
    }

    private boolean moveToTileAndWait(Coord tile, Coord2d origin) {
        if (coordToTile(gui.map.player().rc.sub(origin)).equals(tile))
            return (true);
        clickTile(tile, origin);
        for (int i = 0, sleep = 25; (gui.map.player().isMoving() || !coordToTile(gui.map.player().rc.sub(origin)).equals(tile)) && !stop; ) { // For now lets assume that player starts from different tile so we only have to check that he has moved to correct tile and is not walking anymore
            if (!gui.map.player().isMoving()) {
                if (i > 1000) {
                    return (false);
                } else {
                    i += sleep;
                }
            } else {
                i = 0;
            }
            sleep(sleep);
            if (stop)
                return (true);
        }
        return (true);
    }

    @Override
    public void run() {
        run:
        while (!stop) {
            try {
                BufferedImage bMap = null;
                Graphics g = null;
                if (DEBUG) {
                    bMap = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
                    g = bMap.getGraphics();
                }
                int xmatrix = (int) (cmaps.x * 9 / tilesz.x);
                int ymatrix = (int) (cmaps.y * 9 / tilesz.y);

                int[][] accessMatrix = new int[xmatrix + 2][ymatrix + 2]; //  0 <= accessible, 0 < inaccessible, 1 = object blocking, 2 = innaccessible tile, -1 = destinationgob
                Coord2d origintile = new Coord2d(gui.map.player().rc.floor(tilesz).mul(tilesz));
                // Get rid of negative coordinates
                Coord2d originview = new Coord2d(gui.map.player().rc.floor(cmaps).mul(cmaps));
                Coord2d originmap = originview.sub(cmaps.mul(4));
                Coord2d origin = originmap.floor(tilesz).mul(tilesz);
                if (origin.x < originmap.x) origin.x += tilesz.x;
                if (origin.y < originmap.y) origin.y += tilesz.y;

                Predicate<String> boatCheck = s -> {
                    for (String act : boatGobs) {
                        Pattern pattern = Pattern.compile(act);
                        if (pattern.matcher(s).matches())
                            return (true);
                    }
                    return (false);
                };
                Predicate<String> boat1Check = s -> {
                    for (String act : boat1Gobs) {
                        Pattern pattern = Pattern.compile(act);
                        if (pattern.matcher(s).matches())
                            return (true);
                    }
                    return (false);
                };
                Predicate<String> boat2Check = s -> {
                    for (String act : boat2Gobs) {
                        Pattern pattern = Pattern.compile(act);
                        if (pattern.matcher(s).matches())
                            return (true);
                    }
                    return (false);
                };

                //There are more than 1 objects, so it’s better to initially make a filter and add a simple check inside the loop
                Set<Long> ignoreList = new HashSet<>();
                Gob player = gui.map.player();
                final boolean[] isOnBoat = {false, false, false};
                if (player != null) {
                    ignoreList.add(player.id);
                    player.findChainedGobs().stream().distinct().filter(Objects::nonNull).map(cg -> cg.id).forEach(ignoreList::add);
                    Gob heldBy = player.findFollowingGob();
                    if (heldBy != null)
                        heldBy.resname().ifPresent(nm -> {
                            isOnBoat[0] = boatCheck.test(nm);
                            isOnBoat[1] = boat1Check.test(nm);
                            isOnBoat[2] = boat2Check.test(nm);
                        });
                }

                Predicate<String> inaccess = s -> {
                    for (String act : inaccessibleTiles) {
                        Pattern pattern = Pattern.compile(act);
                        if (pattern.matcher(s).matches())
                            return (true);
                    }
                    boolean bog = false;
                    boolean deeper = false;
                    boolean boat = false;
                    if (isOnBoat[2]) {
                        boat = true;
                        deeper = true;
                    } else if (isOnBoat[1]) {
                        boat = true;
                    } else if (isOnBoat[0]) {
                        boat = true;
                        bog = true;
                    } else {
                        bog = true;
                    }
                    if (!bog) {
                        for (String act : bogTiles) {
                            Pattern pattern = Pattern.compile(act);
                            if (pattern.matcher(s).matches())
                                return (true);
                        }
                    }
                    if (!boat) {
                        for (String act : waterTiles) {
                            Pattern pattern = Pattern.compile(act);
                            if (pattern.matcher(s).matches())
                                return (true);
                        }
                    }
                    if (!deeper) {
                        for (String act : water2Tiles) {
                            Pattern pattern = Pattern.compile(act);
                            if (pattern.matcher(s).matches())
                                return (true);
                        }
                    }

                    return (false);
                };

                for (int i = 0; i < accessMatrix.length; i++) {
                    for (int j = 0; j < accessMatrix[i].length; j++) {
                        Coord2d current = Coord2d.of(origin.floor(tilesz).add(i, j));
                        if (current.mul(tilesz).x < originmap.x || current.mul(tilesz).y < originmap.y) {
                            accessMatrix[i][j] = 2;
                            continue;
                        }
                        if (current.mul(tilesz).x + tilesz.x > originmap.x + (cmaps.x * 9) || current.mul(tilesz).y + tilesz.y > originmap.y + (cmaps.y * 9)) {
                            accessMatrix[i][j] = 2;
                            continue;
                        }

                        int t = gui.map.glob.map.gettile(current.floor());

                        boolean inac = false;
                        final Tiler tiler = gui.map.glob.map.tiler(t);
                        if (configuration.puruspfignoreridge && tiler instanceof TerrainTile.RidgeTile) {
                            double max = Double.NEGATIVE_INFINITY;
                            double min = Double.POSITIVE_INFINITY;
                            for (Coord2d off : new Coord2d[]{Coord2d.z, Coord2d.of(tilesz.x, 0), Coord2d.of(tilesz), Coord2d.of(0, tilesz.y)}) {
                                double z = gui.map.glob.map.getcz(current.mul(tilesz).add(off));
                                max = Double.max(max, z);
                                min = Double.min(min, z);
                            }

                            if (max - min >= ((TerrainTile.RidgeTile) tiler).breakz()) inac = true;
                        }

                        Resource res = gui.map.glob.map.tilesetr(t);

                        if ((res != null && inaccess.test(res.name)) || inac) {
                            accessMatrix[i][j] = 2;
                        }
                    }
                }

                long start = System.currentTimeMillis();
                for (Gob gob : gui.ui.sess.glob.oc.getallgobs()) {
                    if (ignoreList.contains(gob.id))
                        continue;
                    Hitbox[] box = Hitbox.hbfor(gob);
                    if (box == null) continue;
                    for (Hitbox hitbox : box) {
                        if (!whitelistedGobs.contains(gob.getres().name) && hitbox.ishitable()) {//FIXME
                            Coord2d rel = gob.rc.sub(origin);

                            Coord2d[] points = new Coord2d[hitbox.points.length];

                            for (int i = 0; i < points.length; i++)
                                points[i] = hitbox.points[i].rotate(gob.a).add(rel);

                            double maxY = Double.NEGATIVE_INFINITY;
                            double minY = Double.POSITIVE_INFINITY;

                            for (Coord2d point : points) {
                                maxY = Double.max(maxY, point.y);
                                minY = Double.min(minY, point.y);
                            }

                            for (int i = (int) Math.floor(minY); i <= (int) Math.ceil(maxY); i++) {
                                List<Double> plist = new ArrayList<>();
                                for (int j = 0; j < points.length; j++) {
                                    if (Math.min(points[j].y, points[(j + 1) % points.length].y) <= i && i <= Math.max(points[j].y, points[(j + 1) % points.length].y)) {
                                        try {
                                            Line l = segmentToLine(points[j], points[(j + 1) % points.length]);
                                            if (l.isVertical())
                                                plist.add(l.constant);
                                            else if (l.isHorizontal()) {
                                                plist.add(Math.min(points[j].x, points[(j + 1) % points.length].x));
                                                plist.add(Math.max(points[j].x, points[(j + 1) % points.length].x));
                                            } else {
                                                plist.add((l.xAtY(i)));
                                            }
                                        } catch (final Throwable ignored) {}
                                    }
                                }
                                Collections.sort(plist);
                                for (int j = 1; j < plist.size(); j += 2) {
                                    int left = (int) Math.ceil(plist.get(j - 1));
                                    int right = (int) Math.floor(plist.get(j));
                                    for (int k = left; k <= right; k++) {
                                        if (i < 0 || k < 0 || i / 11 > ymatrix || k / 11 > xmatrix)
                                            continue;
                                        try {
                                            if (destGob != null && gob.id == destGob.id)
                                                accessMatrix[(k) / 11][(i) / 11] = -1;
                                            else
                                                accessMatrix[(k) / 11][(i) / 11] = 1;
                                        } catch (ArrayIndexOutOfBoundsException a) {
                                            a.printStackTrace();
                                        }
                                    }
                                    if (left == right) {
                                        j--;
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                }
                Coord startTile = coordToTile(gui.map.player().rc.sub(origin));
                Coord destTile = coordToTile(dest.sub(origin));

                if (DEBUG) {
                    g.setColor(Color.orange);
                    g.setColor(new Color(104, 0, 98));
                    g.fillRect(startTile.x * 11, startTile.y * 11, 11, 11);

                    g.setColor(Color.WHITE);
                    g.fillRect(destTile.x * 11, destTile.y * 11, 11, 11);
                }
                // Only pathfind within 37 tiles to every distance around player TODO: Find the precise distance where objects are loaded
                Coord playerTile = coordToTile(gui.map.player().rc.sub(origin));
//                for (int i = -38; i <= 38; i++) {
//                    accessMatrix[playerTile.x + 38][playerTile.y + i] = 2;
//                    accessMatrix[playerTile.x - 38][playerTile.y + i] = 2;
//                    accessMatrix[playerTile.x + i][playerTile.y + 38] = 2;
//                    accessMatrix[playerTile.x + i][playerTile.y - 38] = 2;
//                }
                // Color tiles
                if (DEBUG) {
                    for (int i = 0; i < xmatrix; i++) {
                        for (int j = 0; j < ymatrix; j++) {
                            if (accessMatrix[i][j] == 0)
                                continue;
                            else if (accessMatrix[i][j] == -1)
                                g.setColor(new Color(255, 116, 0));
                            else if (accessMatrix[i][j] == 1)
                                g.setColor(Color.cyan);
                            else if (accessMatrix[i][j] == 2)
                                g.setColor(Color.orange);
                            g.fillRect((i) * 11, (j) * 11, 11, 11);
                        }
                    }
                }

                // While player is in tile that is inaccessible, try to move player to accessible tile
                Random rng = new Random(1337);
                int timeout = 10000;
                long startTime = System.currentTimeMillis();

                /** Unstack cycle
                 * while player is standing on inaccessible tile do a random click within 11 pixels
                 */
                while (true) {
                    // Move player to tile that is accessible
                    playerTile = coordToTile(gui.map.player().rc.sub(origin));
                    int t = gui.map.glob.map.gettile(origin.floor(tilesz).add(playerTile.x, playerTile.y));
                    Resource res = gui.map.glob.map.tilesetr(t);

                    boolean inac = false;
                    final Tiler tiler = gui.map.glob.map.tiler(t);
                    if (configuration.puruspfignoreridge && tiler instanceof TerrainTile.RidgeTile) {
                        double max = Double.NEGATIVE_INFINITY;
                        double min = Double.POSITIVE_INFINITY;
                        for (Coord off : new Coord[]{Coord.z, Coord.of(tilesz2.x, 0), Coord.of(tilesz2), Coord.of(0, tilesz2.y)}) {
                            double z = gui.map.glob.map.getcz(playerTile.mul(tilesz).add(off));
                            max = Double.max(max, z);
                            min = Double.min(min, z);
                        }

                        if (max - min >= ((TerrainTile.RidgeTile) tiler).breakz()) inac = true;
                    }

                    if ((res != null && inaccess.test(res.name)) || inac) {
                        accessMatrix[playerTile.x][playerTile.y] = 2;
                    }

                    if (accessMatrix[playerTile.x][playerTile.y] > 0) {
                        if (System.currentTimeMillis() - startTime > timeout) {
                            // Timeout after 10 seconds
                            return;
                        }
                        // Randomly click around and hope that player moves to correct position, timeout after few
                        gui.map.clearmovequeue();
                        gui.map.queuemove(gui.map.player().rc.add(rng.nextInt() % 11, rng.nextInt() % 11));
//                        gui.map.wdgmsg("click", PBotUtils.getCenterScreenCoord(gui.ui), gui.map.player().rc.add(rng.nextInt() % 11, rng.nextInt() % 11).floor(posres), 1, 0);
//                        gui.map.pllastcc = gui.map.player().rc.add(rng.nextInt() % 11, rng.nextInt() % 11);
                        do {
                            sleep(250);
                            if (stop)
                                return;
                        } while (gui.map.player().getv() != 0);
                    } else {
                        break;
                    }
                }
                clickTile(playerTile, origin);
                // Find route
                if (!playerTile.equals(destTile)) { // If player is already in the destination tile, skip this
                    Coord route[];
                    Coord[][] toHere = new Coord[accessMatrix.length][accessMatrix[0].length];
                    int dist[][] = new int[accessMatrix.length][accessMatrix[0].length];
                    LinkedList<Coord> q = new LinkedList<>();
                    q.addFirst(new Coord(coordToTile(gui.map.player().rc.sub(origin))));
                    while (!q.isEmpty()) {
                        Coord cur = q.pollLast();
                        if (cur.equals(destTile))
                            break;
                        Coord[] possibleMoves = new Coord[4];
                        possibleMoves[0] = new Coord(cur.x + 1, cur.y);
                        possibleMoves[1] = new Coord(cur.x - 1, cur.y);
                        possibleMoves[2] = new Coord(cur.x, cur.y + 1);
                        possibleMoves[3] = new Coord(cur.x, cur.y - 1);
                        for (int i = 0; i < possibleMoves.length; i++) {
                            if (isAccessible(possibleMoves[i], accessMatrix) && toHere[possibleMoves[i].x][possibleMoves[i].y] == null) {
                                toHere[possibleMoves[i].x][possibleMoves[i].y] = new Coord(cur);
                                q.addFirst(possibleMoves[i]);
                                dist[possibleMoves[i].x][possibleMoves[i].y] = dist[cur.x][cur.y] + 1;
                            }
                        }
                    }
                    if (toHere[destTile.x][destTile.y] == null) { // A route does not exist!
                        synchronized (gui.map) {
                            gui.map.foundPath = false;
                        }
                        if (DEBUG) {
                            g.setColor(Color.WHITE);
                            g.fillRect(destTile.x * 11+3, destTile.y * 11+3, 11-6, 11-6);
                            try {
                                ImageIO.write(bMap, "png", new File(System.currentTimeMillis() + "fail.png"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return;
                    }

                    route = new Coord[dist[destTile.x][destTile.y]];
                    Coord cur = playerTile;
                    route[route.length - 1] = destTile;

                    if (DEBUG)
                        g.setColor(Color.green);
                    for (int i = route.length - 2; i >= 0; i--) {
                        route[i] = toHere[route[i + 1].x][route[i + 1].y];
                        if (DEBUG)
                            g.fillRect((route[i].x) * 11, (route[i].y) * 11, 11, 11);
                    }

                    System.out.println("Time taken for finding the route: " + (System.currentTimeMillis() - start));
                    synchronized (gui.map) {
                        gui.map.foundPath = true;
                    }
                    // Walk the route through
                    Coord clickDest = playerTile;
                    for (int i = 0; i < route.length; i++) {
                        if (accessMatrix[route[i].x][route[i].y] == -1)
                            break;
                        if (playerTile.x == route[i].x || playerTile.y == route[i].y) {
                            clickDest = clickDest.add(route[i].sub(clickDest));
                        } else {
                            if (!moveToTileAndWait(clickDest, origin))
                                continue run;
                            if (stop)
                                return;
                            playerTile = coordToTile(gui.map.player().rc.sub(origin));
                            clickDest = route[i];
                        }
                    }
                    if (destGob == null && action != null && action.length() > 0)
                        gui.act(action);
                    if (!moveToTileAndWait(clickDest, origin))
                        continue run;
                    if (stop)
                        return;
                }

                if (destGob != null) {
                    if (action != null && action.length() > 0)
                        gui.act(action);
                    if (destGob != null && destGob.rc != null) {
                        gui.map.wdgmsg("click", Coord.z, destGob.rc.floor(posres), button, mod, 0, (int) destGob.id, destGob.rc.floor(posres), 0, meshid);
                        gui.map.pllastcc = destGob.rc;
                    }
                    //	gui.map.wdgmsg("click", gob.sc, mc, clickb, modflags, 0, (int) gob.id, gob.rc.floor(posres), 0, meshid);
                }

                if (DEBUG) {
                    try {
                        ImageIO.write(bMap, "png", new File(System.currentTimeMillis() + ".png"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            break;
        }
    }

    private void sleep(int timeInMillis) {
        try {
            Thread.sleep(timeInMillis);
        } catch (InterruptedException ie) {
            stop = true;
        }
    }
}
