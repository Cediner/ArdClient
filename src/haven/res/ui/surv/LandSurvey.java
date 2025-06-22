package haven.res.ui.surv;

import haven.Area;
import haven.BGL;
import haven.Button;
import haven.Coord;
import haven.Coord3f;
import haven.FastMesh;
import haven.GLState;
import haven.GOut;
import haven.GameUI;
import haven.HSlider;
import haven.IButton;
import haven.Label;
import haven.Loading;
import haven.Location;
import haven.MCache;
import static haven.MCache.tilesz;
import haven.MapView;
import haven.RenderList;
import haven.Rendered;
import haven.States;
import haven.States.ColState;
import haven.States.DepthOffset;
import haven.States.PointSize;
import haven.Text;
import haven.Theme;
import haven.UI;
import haven.Utils;
import haven.VertexBuf;
import haven.VertexBuf.VertexArray;
import haven.Widget;
import haven.Window;
import static javax.media.opengl.GL.GL_FLOAT;
import static javax.media.opengl.GL.GL_POINTS;
import static javax.media.opengl.fixedfunc.GLPointerFunc.GL_COLOR_ARRAY;
import static javax.media.opengl.fixedfunc.GLPointerFunc.GL_VERTEX_ARRAY;
import modification.dev;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.stream.IntStream;

public class LandSurvey extends Window {
    public final Area area;
    public final Data data;
    private boolean inited = false;
    MapView mv;
    Display dsp;
    final FastMesh ol;
    final Location dloc;
    final Label albl;
    final Label zdlbl;
    final Label wlbl;
    final Label dlbl;
    final HSlider zset;
    int defz;

    public IButton plus;
    public IButton minus;
    public Label zvalue;
    public Label value;

    public LandSurvey(Area area, Data data) {
        super(Coord.z, "Land survey", "Land survey", true);
        this.area = area;
        this.data = data;
        this.dloc = Location.xlate(new Coord3f(area.ul.x * (float) tilesz.x, -area.ul.y * (float) tilesz.y, 0));
        VertexArray olv = new VertexArray(FloatBuffer.wrap(new float[]{
                0, 0, 0,
                (area.br.x - area.ul.x) * (float) tilesz.x, 0, 0,
                (area.br.x - area.ul.x) * (float) tilesz.x, -(area.br.y - area.ul.y) * (float)
                MCache.tilesz.y, 0, 0, -(area.br.y - area.ul.y) * (float) tilesz.y, 0,
        }));
        ol = new FastMesh(new VertexBuf(olv), ShortBuffer.wrap(new short[]{
                0, 3, 1,
                1, 3, 2,
        }));
        albl = add(new Label(String.format("Area: %d m\u00b2", area.area())), 0, 0);
        zdlbl = add(new Label("..."), UI.scale(0, 15));
        wlbl = add(new Label("..."), UI.scale(0, 30));
        dlbl = add(new Label("..."), UI.scale(0, 45));
        defz = data.dz[0];
        zset = add(new HSlider(UI.scale(225), -1, 1, data.dz[0]) {
            public void changed() {
                IntStream.range(0, data.dz.length).forEach(i -> data.dz[i] = val);
                changeVal(String.format("%s%d", (val > defz) ? "+" : "", val - defz));
                upd = true;
                send();
            }

            public Object tooltip(Coord c, Widget prev) {
                return Text.render(String.format("Z: %d, %s%d", val, (val > defz) ? "+" : "", val - defz)).tex();
            }
        }, UI.scale(0, 60));
        zvalue = add(new Label("Z"), UI.scale(0, 70));
        value = add(new Label("...") {
            public boolean mousewheel(Coord c, int amount) {
                final int v;
                if (ui.modshift)
                    v = amount * 10;
                else if (ui.modctrl)
                    v = amount * 5;
                else
                    v = amount;
                wheel(-v);
                return (true);
            }
        }, UI.scale(0, 70));
        plus = add(new IButton(Theme.fullres("buttons/circular/small/add"), this::plus), UI.scale(0, 70));
        minus = add(new IButton(Theme.fullres("buttons/circular/small/sub"), this::minus), UI.scale(0, 70));
        add(new Button(UI.scale(100), "Make level") {
            public void click() {
                LandSurvey.this.wdgmsg("lvl");
            }
        }, UI.scale(0, 90));
        add(new Button(UI.scale(100), "Remove") {
            public void click() {
                LandSurvey.this.wdgmsg("rm");
            }
        }, UI.scale(125, 90));
        pack();
    }

    public void changeVal(String text) {
        zvalue.settext("z: " + data.dz[0]);
        value.settext(text);
        value.move(UI.scale(new Coord(asz.x / 2, value.c.y)), 0.5, 0);
        plus.move(UI.scale(new Coord(value.c.x + value.sz.x + 5, value.c.y)));
        minus.move(UI.scale(new Coord(value.c.x - 5, value.c.y)), 1, 0);
    }

    public void plus() {
        zset.val++;
        zset.changed();
    }

    public void minus() {
        zset.val--;
        zset.changed();
    }

    public void wheel(int a) {
        zset.val += a;
        zset.changed();
    }

    public static Widget mkwidget(UI ui, Object... args) {
        Area area = Area.corn((Coord)args[0], (Coord)args[1]);
        float gran = ((Number)args[2]).floatValue() / 11;
        Data data = new Data(Area.corni(area.ul, area.br), gran);
        LandSurvey srv = new LandSurvey(area, data);
        if(args[3] != null) {
            data.decode(Utils.iv(args[3]), (byte[])args[4]);
            srv.inited = true;
        }
        return(srv);
    }

    protected void attached() {
        super.attached();
        this.mv = getparent(GameUI.class).map;
        this.dsp = new Display();
        //s_dsp = mv.drawadd(dsp);
        //select(area);
        //mode(new Idle());
    }

    class Display implements Rendered {
        final GLState ptsz = new PointSize(3);
        final MCache map;
        final FloatBuffer cposb;
        final FloatBuffer ccolb;
        final int area;

        Display() {
            map = mv.ui.sess.glob.map;
            area = (LandSurvey.this.area.br.x - LandSurvey.this.area.ul.x + 1) * (LandSurvey.this.area.br.y - LandSurvey.this.area.ul.y + 1);
            cposb = Utils.mkfbuf(area * 3);
            ccolb = Utils.mkfbuf(area * 4);
            update();
        }

        public void draw(GOut g) {
            g.apply();
            BGL bg = g.gl;
            this.cposb.rewind();
            this.ccolb.rewind();
            bg.glEnableClientState(GL_VERTEX_ARRAY);
            bg.glVertexPointer(3, GL_FLOAT, 0, cposb);
            bg.glEnableClientState(GL_COLOR_ARRAY);
            bg.glColorPointer(4, GL_FLOAT, 0, ccolb);
            bg.glDrawArrays(GL_POINTS, 0, area);
            bg.glDisableClientState(GL_VERTEX_ARRAY);
            bg.glDisableClientState(GL_COLOR_ARRAY);
        }

        void update() {
            float E = 0.001f;
            cposb.rewind();
            ccolb.rewind();
            Coord c = new Coord();
            float tz = LandSurvey.this.data.dz[0] / data.gran;
            for (c.y = LandSurvey.this.area.ul.y; c.y <= LandSurvey.this.area.br.y; c.y++) {
                for (c.x = LandSurvey.this.area.ul.x; c.x <= LandSurvey.this.area.br.x; c.x++) {
                    float z = (float) map.getfz(c);
                    cposb.put((c.x - LandSurvey.this.area.ul.x) * (float) tilesz.x).put(-(c.y - LandSurvey.this.area.ul.y) * (float) tilesz.y).put(tz);
                    if (Math.abs(tz - z) < E) {
                        ccolb.put(0).put(1).put(0).put(1);
                    } else if (tz < z) {
                        ccolb.put(1).put(0).put(1).put(1);
                    } else {
                        ccolb.put(0).put(0.5f).put(1).put(1);
                    }
                }
            }

        }

        public boolean setup(RenderList rls) {
            rls.prepo(dloc);
            rls.prepo(ptsz);
            rls.prepo(States.ndepthtest);
            rls.prepo(last);
            rls.prepo(States.vertexcolor);
            return (true);
        }
    }

    private void initsurf() {
        MCache map = mv.ui.sess.glob.map;
        for (Coord vc : data.varea)
            data.wz[data.varea.ridx(vc)] = data.dz[data.varea.ridx(vc)] = (int) Math.round(map.getfz(vc) * data.gran);
        data.seq++;
        upd = true;
    }

    private void initplane() {
        MCache map = mv.ui.sess.glob.map;
        double zs = 0;
        int nv = 0;
        for (Coord vc : data.varea) {
            zs += map.getfz(vc);
            nv++;
        }
        int z = Math.round((float) (zs / nv) * data.gran);
        for (int i = 0; i < data.wz.length; i++)
            data.wz[i] = data.dz[i] = z;
        data.seq++;
        upd = true;
    }

    private void updmap() {
        MCache map = mv.ui.sess.glob.map;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        int sd = 0, hn = 0;
        for (Coord vc : data.varea) {
            int vz = Math.round((float) map.getfz(vc) * data.gran);
            int tz = data.dz[data.varea.ridx(vc)];
            min = Math.min(min, vz); max = Math.max(max, vz);
            sd += tz - vz;
            if (vz > tz)
                hn += vz - tz;
        }
        zdlbl.settext(String.format("Peak to trough: %.1f m", (max - min) / 10.0));
        if (sd >= 0)
            wlbl.settext(String.format("Units of soil required: %d", sd));
        else
            wlbl.settext(String.format("Units of soil left over: %d", -sd));
        dlbl.settext(String.format("Units of soil to dig: %d", hn));
    }

    private void send() {
        wdgmsg("data", data.encode());
    }

    private boolean upd = true;
    private int mapseq = -1;
    private static final GLState olmat = GLState.compose(new ColState(new Color(255, 0, 0, 64)), Rendered.eyesort, new DepthOffset(-2, -2));
    private int olseq = -1;

    public void tick(double dt) {
        if (!inited) {
            try {
                initplane();
                send();
                olseq = mv.ui.sess.glob.map.olseq;
                inited = true;
            } catch (Loading l) {}
        }
        if (inited) {
            if (upd || (mapseq != mv.ui.sess.glob.map.chseq) || (olseq != mv.ui.sess.glob.map.olseq)) {
                try {
                    updmap();
                    mapseq = mv.ui.sess.glob.map.chseq;
                    olseq = mv.ui.sess.glob.map.olseq;
                    upd = false;
                } catch (Loading l) {
                }
            }

            if (olseq != -1) {
                mv.drawadd(dsp);
                mv.drawadd(GLState.compose(olmat, Location.xlate(new Coord3f(area.ul.x * (float) tilesz.x, -area.ul.y * (float) tilesz.y, data.dz[0]))).apply(ol));
            }
        }
        super.tick(dt);
    }

    public void uimsg(String name, Object... args) {
        if (name == "tz") {
            data.decode(Utils.iv(args[0]), (byte[]) args[1]);
            upd = true;
        } else {
            super.uimsg(name, args);
        }
    }

    public void destroy() {
        //mode(null);
        //if (s_dsp != null)
        //    s_dsp.remove();
        super.destroy();
    }
}
