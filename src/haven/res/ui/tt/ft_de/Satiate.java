/* Preprocessed source code */
package haven.res.ui.tt.ft_de;

import haven.Coord;
import haven.Indir;
import haven.ItemInfo;
import haven.PUtils;
import haven.Resource;
import haven.RichText;
import haven.Text;
import haven.chrwnd.Constipations;

import java.awt.image.BufferedImage;

/* >tt: Satiate */
@haven.FromResource(name = "ui/tt/ft-de", version = 7, override = true)
public class Satiate implements ItemInfo.InfoFactory {
    public ItemInfo build(ItemInfo.Owner owner, ItemInfo.Raw raw, Object... args) {
        final Indir<Resource> res = owner.context(Resource.Resolver.class).getres((Integer) args[1]);
        final double f = ((Number) args[2]).doubleValue();
        return (new ItemInfo.Tip(owner) {
            public BufferedImage tipimg() {
                BufferedImage t1 = Text.render("Satiate ").img;
                int h = t1.getHeight();
                BufferedImage icon = PUtils.convolvedown(res.get().layer(Resource.imgc).img, new Coord(h, h), Constipations.tflt);
                BufferedImage t2 = RichText.render(String.format("%s by $col[255,128,128]{%d%%}", res.get().layer(Resource.tooltip).t, (int) Math.round((1.0 - f) * 100)), 0).img;
                return (catimgsh(0, t1, icon, t2));
            }
        });
    }
}
