package haven.sloth.gfx;

import com.google.common.flogger.FluentLogger;
import haven.Config;
import haven.Coord;
import haven.Coord3f;
import haven.FastMesh;
import haven.GOut;
import haven.MCache;
import haven.MapMesh;
import haven.Material;
import haven.RenderList;
import haven.Resource;
import haven.States;
import haven.Utils;
import haven.VertexBuf;

import javax.media.opengl.GL2;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static haven.MCache.tilesz;

public class GridMesh extends FastMesh {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private static final float[][] colors;

    static {
        colors = new float[256][3];
        int i = 0;
        try {
            BufferedImage im = Resource.loadimg("custom/gridgrad");
            Color c;
            for (i = 0; i < 256; ++i) {
                c = new Color(im.getRGB(i, 25));
                colors[i][0] = c.getRed() / 255f;
                colors[i][1] = c.getBlue() / 255f;
                colors[i][2] = c.getGreen() / 255f;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            logger.atSevere().withCause(e).log("Missing data/grad.png file for grid meshes");
            logger.atSevere().log("%s", i);
            System.exit(1);
        }
    }


    private GridMesh(VertexBuf buf, ShortBuffer sa) {
        super(buf, sa);
    }

    public boolean setup(RenderList rl) {
        rl.prepo(Material.nofacecull);
        rl.prepo(MapMesh.postmap);
        rl.prepo(States.vertexcolor);
        return super.setup(rl);
    }

    public void draw(GOut g) {
        g.gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
        super.draw(g);
        g.gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
    }

    private static Coord3f mapToScreen(Coord c, Coord ul, MCache map) {
        return new Coord3f((float) ((c.x - ul.x) * tilesz.x), (float) (-(c.y - ul.y) * tilesz.y), map.getz_safe(c));
    }

    public static FastMesh build(MCache mc, Coord ul, Coord sz) {
        Coord stop = ul.add(sz);
        Coord asz = sz.add(1, 1);
        int area = (asz.x) * (asz.y);
        FloatBuffer pa = Utils.mkfbuf(area * 3);
        FloatBuffer cl = Utils.mkfbuf(area * 4);
        ShortBuffer inds = Utils.mksbuf(area * 2 * 3);
        float zoff = 0.2f;

        int offset = Config.slothgridoffset;
        Coord c = new Coord();
        Coord3f me;
        int z;
        float mez;
        for (c.y = ul.y; c.y <= stop.y; ++c.y) {
            for (c.x = ul.x; c.x <= stop.x; ++c.x) {
                me = mapToScreen(c, ul, mc);
                mez = !Config.disableelev ? me.z : 0;
                pa.put(me.x).put(me.y).put(mez + zoff);
                z = ((int) me.z + 128 + offset) % 256;
//                if (((me.x % (11 * 100) == 0) && (c.x % 100 == 0)) || ((me.y % (11 * 100) == 0) && (c.y % 100 == 0))) {
//                    cl.put(255).put(255).put(255).put(0.5f);
//                } else {
                    cl.put(colors[z][0]).put(colors[z][1]).put(colors[z][2]).put(0.5f);
//                }
            }
        }

        short ind = 0;
        for (c.y = 0; c.y < sz.y; ++c.y, ++ind) {
            for (c.x = 0; c.x < sz.x; ++c.x, ++ind) {
                inds.put(ind).put((short) (ind + asz.x)).put((short) (ind + asz.x + 1));
                inds.put(ind).put((short) (ind + asz.x + 1)).put((short) (ind + 1));
            }
        }

        return new GridMesh(new VertexBuf(new VertexBuf.VertexArray(pa),
                new VertexBuf.ColorArray(cl)),
                inds);
    }
}

