package haven.chrwnd;

import haven.Coord;
import haven.GOut;
import haven.Indir;
import haven.Loading;
import haven.RichText;
import haven.RichTextBox;

public class LoadingTextBox extends RichTextBox {
    private Indir<String> text = null;

    public LoadingTextBox(Coord sz, String text, RichText.Foundry fnd) {
        super(sz, text, fnd);
    }

    public LoadingTextBox(Coord sz, String text, Object... attrs) {
        super(sz, text, attrs);
    }

    public void settext(Indir<String> text) {
        this.text = text;
    }

    public void draw(GOut g) {
        if (text != null) {
            try {
                settext(text.get());
                text = null;
            } catch (Loading l) {
            }
        }
        super.draw(g);
    }
}
