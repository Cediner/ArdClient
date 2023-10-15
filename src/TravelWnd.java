import haven.Button;
import haven.Coord;
import haven.GOut;
import haven.Label;
import haven.Scrollbar;
import haven.Text;
import haven.TextEntry;
import haven.UI;
import haven.Widget;
import haven.Window;

import java.util.ArrayList;
import java.util.List;

public class TravelWnd extends Window {
    public static final int W = UI.scale(200);
    public final TextEntry txt;
    public final NameList ls;
    public final Button btn;

    public TravelWnd(String name) {
        super(Coord.z, name, true);
        Widget prev;
        prev = add(new Label("Enter a destination name:"), Coord.z);
        txt = add(new TextEntry(W, "") {
            @Override
            protected void changed() {
                ls.sel = null;
            }

            @Override
            public void activate(String text) {
                btn.click();
            }
        }, 0, UI.scale(20));
        prev = add(new Label("Or, choose a known destination:"), 0, UI.scale(50));
        ls = add(new NameList(new Coord(W, W)), 0, UI.scale(70));
        btn = add(new Button(W, "Travel!", () -> {
            if (ls.sel != null)
                TravelWnd.this.wdgmsg("act", ls.sel.text);
            else if (txt.text.length() > 0)
                TravelWnd.this.wdgmsg("act", txt.text);
        }), 0, UI.scale(280));
        pack();
    }

    public static Widget mkwidget(UI ui, Object... args) {
        return (new TravelWnd((String) args[0]));
    }

    private class NameList extends Widget {
        final int itemh = 20;
        List<Text> chrs = new ArrayList<Text>();
        Text sel;
        Scrollbar sb;

        private NameList(Coord sz) {
            super(sz);
            sb = adda(new Scrollbar(sz.y, 0, 0), sz.x, 0, 1, 0);
        }

        @Override
        public void draw(GOut g) {
            g.chcolor(0, 0, 0, 128);
            g.frect(Coord.z, sz);
            g.chcolor();
            synchronized (chrs) {
                for (int i = 0, y = 0; (y < sz.y) && (i + sb.val < chrs.size()); i++, y += itemh) {
                    Text c = chrs.get(i + sb.val);
                    if (c == sel) {
                        g.chcolor(255, 255, 0, 128);
                        g.frect(new Coord(0, y), new Coord(sz.x, itemh));
                        g.chcolor();
                    }
                    g.aimage(c.tex(), new Coord(0, y + (itemh / 2)), 0, 0.5);
                }
            }
            super.draw(g);
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            if (super.mousedown(c, button))
                return (true);
            int sel = (c.y / itemh) + sb.val;
            synchronized (chrs) {
                this.sel = (sel >= chrs.size()) ? null : chrs.get(sel);
                txt.settext("");
            }
            return (true);
        }

        @Override
        public boolean mousewheel(Coord c, int amount) {
            sb.ch(amount);
            return (true);
        }

        public void add(Text chr) {
            synchronized (chrs) {
                chrs.add(chr);
                sb.max = chrs.size();
            }
        }
    }

    @Override
    public void uimsg(String name, Object... args) {
        if (name == "add") {
            ls.add(Text.render((String) args[0]));
        } else {
            super.uimsg(name, args);
        }
    }
}
