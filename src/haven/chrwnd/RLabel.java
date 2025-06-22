package haven.chrwnd;

import haven.Coord;
import haven.Label;
import haven.Utils;

import java.awt.Color;
import java.util.function.Function;
import java.util.function.Supplier;

public class RLabel<V> extends Label {
    private final Supplier<V> val;
    private final Function<V, String> fmt;
    private final Function<V, Color> col;
    private Coord oc;
    private Color lc;
    private V lv;

    private RLabel(Supplier<V> val, Function<V, String> fmt, Function<V, Color> col, V ival) {
        super(ival == null ? "" : fmt.apply(ival));
        this.val = val;
        this.fmt = fmt;
        this.col = col;
        this.lv = ival;
        this.oc = oc;
        if ((col != null) && (ival != null))
            setcolor(lc = col.apply(ival));
    }

    public RLabel(Supplier<V> val, Function<V, String> fmt, Function<V, Color> col) {
        this(val, fmt, col, null);
    }

    public RLabel(Supplier<V> val, Function<V, String> fmt, Color col) {
        this(val, fmt, (Function<V, Color>) null);
        setcolor(col);
    }

    private void update() {
        V v = val.get();
        if (!Utils.eq(v, lv)) {
            settext(fmt.apply(v));
            lv = v;
            if (col != null) {
                Color c = col.apply(v);
                if (!Utils.eq(c, lc)) {
                    setcolor(c);
                    lc = c;
                }
            }
        }
    }

    protected void attached() {
        super.attached();
        if (oc == null)
            oc = new Coord(c.x + sz.x, c.y);
        if (lv == null)
            update();
    }

    public void settext(String text) {
        super.settext(text);
        if (oc != null)
            move(oc.add(-sz.x, 0));
    }

    public void tick(double dt) {
        update();
    }
}
