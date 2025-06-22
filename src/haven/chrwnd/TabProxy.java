package haven.chrwnd;

import haven.AWidget;
import haven.CharWnd;
import haven.Utils;
import haven.Widget;

public class TabProxy extends AWidget {
    public final Class<? extends Widget> tcl;
    public final String id;
    private Widget tab = null;

    public TabProxy(Class<? extends Widget> tcl, String id) {
        this.tcl = tcl;
        this.id = id;
    }

    protected void added() {
        super.added();
        if (tab == null) {
            CharWnd chr = getparent(CharWnd.class);
            tab = chr.findchild(tcl);
            unlink();
            if (tab != null) {
                tab.addchild(this, id);
            } else {
                tab = Utils.construct(tcl);
                tab.addchild(this, id);
                chr.addchild(tab, "tab");
            }
        }
    }

    public void uimsg(String nm, Object... args) {
        tab.uimsg(nm, args);
    }
}