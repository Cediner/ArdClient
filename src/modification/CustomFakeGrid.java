package modification;

import haven.BGL;
import haven.Button;
import haven.CheckBox;
import haven.ColorPreview;
import haven.Coord;
import haven.Coord2d;
import haven.GOut;
import haven.GameUI;
import haven.Gob;
import haven.GobHitbox;
import haven.Label;
import haven.RenderList;
import haven.Scrollport;
import haven.Sprite;
import haven.States;
import haven.TextEntry;
import haven.Utils;
import haven.Widget;
import haven.WidgetVerticalAppender;
import haven.Window;
import haven.purus.pbot.PBotWindowAPI;
import org.json.JSONArray;

import javax.media.opengl.GL2;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class CustomFakeGrid extends Sprite {
    public static final String fileName = "FakeMouseBound.json";
    public static final List<Row> list = new ArrayList<>();
    public static final List<CustomFakeGrid> boxList = new ArrayList<>();
    public static boolean needUpdate = true;
    public static boolean needUpdateWindow = true;

    private Row row;
    private GobHitbox.BBox box;
    private int mode;
    private States.ColState color;
    private FloatBuffer[] buffers;

    public CustomFakeGrid(Gob gob, Coord2d size, Color clr, boolean fill) {
        super(gob, null);

        update(new Row(size, clr, fill));
    }

    public CustomFakeGrid(Gob gob, Row row) {
        super(gob, null);

        update(row);
    }

    public static void update(Gob gob) {
        if (list.isEmpty())
            loadList(loadJSON());
        boxList.clear();
        list.forEach(r -> boxList.add(new CustomFakeGrid(gob, r)));

        needUpdate = false;
    }

    public void update(Row row) {
        this.row = row;
        Coord2d size = StringtoC2d("(" + row.x.text() + ", " + row.y.text() + ")");
        Color clr = row.clr.getColor();
        boolean fill = row.mode.a;

        this.box = new GobHitbox.BBox(size);
        mode = fill ? GL2.GL_QUADS : GL2.GL_LINE_LOOP;
        color = new States.ColState(clr);

        buffers = new FloatBuffer[1];
        for (int i = 0; i < 1; i++) {
            buffers[0] = Utils.mkfbuf(box.points.length * 3);
            for (int p = 0; p < box.points.length; p++) {
                Coord2d point = box.points[p];
                int type = getType();
                buffers[i].put((float) point.x).put((float) ((type == 2 ? -1 : 1) * point.y)).put(1);
            }
            buffers[i].rewind();
        }
    }

    public int getType() {
        if (mode == GL2.GL_LINE_LOOP) {
            return (1);
        } else {
            return (2);
        }
    }

    public boolean setup(RenderList rl) {
        rl.prepo(color);
//        if (mode == GL2.GL_LINE_LOOP)
        rl.prepo(States.xray);
        return (true);
    }

    public void draw(GOut g) {
        try {
            g.apply();
            BGL gl = g.gl;
            if (getType() == 1)
                gl.glLineWidth(2.0F);
            for (int i = 0; i < 1; i++) {
                gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                gl.glVertexPointer(3, GL2.GL_FLOAT, 0, buffers[i]);
                gl.glDrawArrays(mode, 0, box.points.length);
                gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static Row defrow() {
        return (new Row(Coord2d.of(33, 33), Color.WHITE, false));
    }

    static class Row {
        TextEntry x = new TextEntry(50, "") {
            {
                autofocus = false;
            }

            String backup = text();

            @Override
            public boolean keydown(KeyEvent e) {
                backup = text();
                boolean b = super.keydown(e);
                try {
                    Double.parseDouble(text());
                } catch (Throwable ex) {
                    settext(backup);
                }
                return (b);
            }
        };
        Label pre = new Label("X");
        TextEntry y = new TextEntry(50, "") {
            {
                autofocus = false;
            }

            String backup = text();

            @Override
            public boolean keydown(KeyEvent e) {
                backup = text();
                boolean b = super.keydown(e);
                try {
                    Double.parseDouble(text());
                } catch (Throwable ex) {
                    settext(backup);
                }
                return (b);
            }
        };
        ColorPreview clr = new ColorPreview(Coord.of(20), Color.WHITE);
        CheckBox mode = new CheckBox("Solid");
        Button close = new Button("X", this::remove);

        Row(Coord2d size, Color clr, boolean fill) {
            x.settext(size.x + "");
            y.settext(size.y + "");
            this.clr.setColor(clr);
            mode.set(fill);
        }

        Row(String size, int clr, boolean fill) {
            this(StringtoC2d(size), new Color(clr, true), fill);
        }

        void create(WidgetVerticalAppender wva) {
            wva.addRow(x, pre, y, clr, mode, close);
        }

        void remove() {
            list.remove(this);
            saveFile(createJSON());

            needUpdate = true;
            needUpdateWindow = true;
        }
    }

    static Coord2d StringtoC2d(String c2d) {
        try {
            double x = Double.parseDouble(c2d.substring(c2d.indexOf("(") + 1, c2d.indexOf(",")));
            double y = Double.parseDouble(c2d.substring(c2d.indexOf(", ") + 1, c2d.indexOf(")")));
            return (Coord2d.of(x, y));
        } catch (Exception e) {
            e.printStackTrace();
            return (Coord2d.of(33, 33));
        }
    }

    static JSONArray loadJSON() {
        Path file = Paths.get(fileName);
        if (Files.exists(file) && !Files.isDirectory(file)) {
            StringBuilder sb = new StringBuilder();
            try {
                Files.readAllLines(file).forEach(sb::append);
                return (new JSONArray(sb.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        JSONArray array = createJSON();
        saveFile(array);
        return (array);
    }

    static JSONArray createJSON() {
        JSONArray ja = new JSONArray();
        if (list.isEmpty())
            list.add(defrow());
        list.forEach(r -> {
            JSONArray ra = new JSONArray();
            ra.put("(" + r.x.text() + ", " + r.y.text() + ")");
            ra.put(r.clr.getColor().getRGB());
            ra.put(r.mode.a);
            ja.put(ra);
        });
        return (ja);
    }

    static void loadList(JSONArray array) {
        list.clear();
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONArray ja = array.getJSONArray(i);
                list.add(new Row(ja.getString(0), ja.getInt(1), ja.getBoolean(2)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (list.isEmpty())
                list.add(defrow());
        }
    }

    static void saveFile(JSONArray array) {
        Path file = Paths.get(fileName);
        try {
            if (!Files.exists(file)) {
                if (Files.isDirectory(file))
                    Files.delete(file);
                Files.createFile(file);
            }
            Files.write(file, array.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void toggle(GameUI gui) {
        Runnable run = () -> {
            if (gui.map.fakeGob == null) {
                needUpdate = true;
                gui.map.fakeGob = gui.map.new Plob();
            } else {
                gui.map.fakeGob.ols.clear();
                gui.map.fakeGob = null;
            }
        };
        try {
            run.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void options(GameUI gui) {
        String windowName = "Mouse bound options";

        Window w = PBotWindowAPI.getWindow(gui.ui, windowName);
        if (w == null) {
            w = new Window(Coord.z, windowName, windowName) {
                @Override
                public void tick(double dt) {
                    if (needUpdateWindow)
                        update();
                    super.tick(dt);
                }

                final Coord size = Coord.of(300, 300);
                final Scrollport scroll = new Scrollport(size);

                final Button addplus = new Button("+", this::addrow);
                final Button finish = new Button("UPDATE", this::update);

                void addrow() {
                    list.add(defrow());
                    saveFile(createJSON());

                    needUpdate = true;
                    needUpdateWindow = true;
                }

                void update() {
                    saveFile(createJSON());

                    WidgetVerticalAppender wva = new WidgetVerticalAppender(scroll.cont);
                    wva.setHorizontalMargin(5);
                    wva.setVerticalMargin(5);
                    scroll.cont.children().forEach(Widget::reqdestroy);

                    if (list.isEmpty())
                        loadList(loadJSON());
                    if (list.isEmpty())
                        list.add(defrow());
                    list.forEach(r -> r.create(wva));

                    wva.add(addplus);
                    wva.add(finish);
                    pack();

                    setfocus(cbtn);

                    needUpdate = true;
                    needUpdateWindow = false;
                }

                {
                    add(scroll, new Coord(0, 0));
                }

                @Override
                public void close() {
                    super.close();
                    needUpdateWindow = true;
                }
            };

            gui.add(w);
        } else {
            w.close();
        }
    }
}
