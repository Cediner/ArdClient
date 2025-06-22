import haven.Coord;
import haven.Indir;
import haven.ItemInfo;
import haven.ItemInfo.InfoFactory;
import haven.ItemInfo.Owner;
import haven.PUtils;
import haven.Resource;
import haven.RichText;
import haven.Text;
import haven.chrwnd.Constipations;

import java.awt.image.BufferedImage;

public class Satiate implements InfoFactory {
    public Satiate() {
    }

    public ItemInfo build(final Owner owner, Object... args) {
        final Indir icon = owner.glob().sess.getres(((Integer) args[1]).intValue());
        final double val = ((Number) args[2]).doubleValue();
        return new ItemInfo.Tip(owner) {
            public BufferedImage tipimg() {
                String satiateStr = Resource.getLocString(Resource.BUNDLE_LABEL, "Satiate ");
                BufferedImage satImg = Text.render(satiateStr).img;
                int satImgHeight = satImg.getHeight();
                BufferedImage var3x = PUtils.convolvedown((((Resource) icon.get()).layer(Resource.imgc)).img,
                        new Coord(satImgHeight, satImgHeight), Constipations.tflt);
                String byStr = Resource.getLocString(Resource.BUNDLE_LABEL, "%s by $col[255,128,128]{%d%%}");
                BufferedImage var4x = RichText.render(String.format(byStr,
                        new Object[]{(((Resource) icon.get()).layer(Resource.tooltip)).t, Integer.valueOf((int) Math.round((1.0D - val) * 100.0D))}), 0, new Object[0]).img;
                return catimgsh(0, new BufferedImage[]{satImg, var3x, var4x});
            }
        };
    }
}