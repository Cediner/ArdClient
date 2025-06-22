package haven.chrwnd;

import haven.ItemInfo;
import haven.PUtils;
import haven.TexI;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;

public interface IconInfo {
    public void draw(BufferedImage img, Graphics g);

    public static BufferedImage render(BufferedImage base, List<ItemInfo> info) {
        BufferedImage ret = base;
        Graphics g = null;
        for (ItemInfo inf : info) {
            if (inf instanceof IconInfo) {
                if (g == null) {
                    BufferedImage buf = TexI.mkbuf(PUtils.imgsz(ret));
                    g = buf.getGraphics();
                    g.drawImage(ret, 0, 0, null);
                    ret = buf;
                }
                ((IconInfo) inf).draw(ret, g);
            }
        }
        if (g != null)
            g.dispose();
        return (ret);
    }
}